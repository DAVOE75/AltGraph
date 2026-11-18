package com.example.altgraph

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.*

/**
 * Motor de predicción de horizonte de carretera para AltGraph (Road Horizon 3D).
 * Determina el camino más probable que seguirá el ciclista en los próximos metros sin ruta precargada,
 * consulta la elevación real del terreno (.hgt) y genera el perfil altimétrico anticipado.
 */
class RoadHorizonPredictor(private val context: Context) {

    private val elevationManager = ElevationManager.getInstance(context)
    private val appPrefs = AppPreferences.getInstance(context)
    private var lastPosSaveMs = 0L
    @Volatile var lastDiagnostic: String = ""
        private set
    @Volatile var mapFileCount: Int = 0
        private set
    private var mapReader: KarooMapReader? = null
    private var lastMapCheckMs = 0L
    private var anchorLat = 0.0
    private var anchorLng = 0.0
    private var hasMoved = false
    private var lastRoadKey: String? = null
    private val STICKY_BONUS_M = 15.0
    private val BEARING_LOOK_M = 15.0
    private val STRAIGHT_ANGLE = 30.0
    private val MAX_TURN = 110.0
    private val SNAP_MAX_ANGLE = 50.0
    private val MINOR_COST_M = 8.0
    private val MINOR_KINDS = setOf("service", "track", "path", "footway", "pedestrian", "steps", "bridleway", "living_street")

    // Estado de la última predicción, para mostrar un mensaje claro en vez de un perfil plano engañoso
    enum class Status { OK, NO_MAP, NO_ROAD, NO_TERRAIN }
    @Volatile var lastStatus: Status = Status.OK
        private set
    @Volatile var lastRoadName: String? = null
        private set

    // Histéresis al cambiar de calle: la nueva debe ganar varias lecturas seguidas
    private var pendingRoadKey: String? = null
    private var pendingCount = 0
    private val SWITCH_READINGS = 2
    private val SWITCH_NOW_MARGIN = 12.0
    private val FOOT_KINDS = setOf("footway", "path", "steps", "pedestrian", "track", "bridleway")

    // ---------- Rapidez: cálculo en segundo plano y precarga ----------
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "RoadHorizonCalc").apply { isDaemon = true } }
    private val computing = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private class Cached(val result: HorizonResult?, val lat: Double, val lng: Double)
    private val cachedMap = java.util.concurrent.ConcurrentHashMap<String, Cached>()

    /**
     * Pide el cálculo en segundo plano y devuelve el último resultado válido cerca de esta posición (o null si aún no hay),
     * para que el hilo principal nunca espere al mapa ni al terreno. La caché es por distancia pedida (radar y 3D piden distintas).
     */
    fun predictAsync(
        lat: Double, lng: Double, heading: Double, dist: Double, step: Double, speedMps: Double,
        behindCount: Int = 0, fresh: Boolean = false
    ): HorizonResult? {
        val key = "${dist.toInt()}_${step.toInt()}_$behindCount"
        // fresh: se descarta el resultado antiguo para no devolver un trazado de otra posición o de otra calle
        if (fresh && !computing.contains(key)) cachedMap.remove(key)
        if (computing.add(key)) {
            executor.execute {
                try {
                    val r = predictHorizon(lat, lng, heading, dist, step, speedMps, behindCount)
                    cachedMap[key] = Cached(r, lat, lng)
                } catch (e: Exception) {
                    android.util.Log.e("RoadHorizon", "predict failed", e)
                } finally {
                    computing.remove(key)
                }
            }
        }
        val c = cachedMap[key] ?: return null
        return if (GeoPoint(c.lat, c.lng).distanceTo(GeoPoint(lat, lng)) < 60.0) c.result else null
    }

    /** Precarga el lector de mapas, las calles de la última posición y el archivo de terreno al arrancar. */
    fun warmUp() {
        executor.execute {
            try {
                val reader = ensureMapReader() ?: return@execute
                val lat = appPrefs.horizonLastLat.toDouble(); val lng = appPrefs.horizonLastLng.toDouble()
                if (lat != 0.0 || lng != 0.0) {
                    reader.getRoadsNear(GeoPoint(lat, lng), radiusMeters = 150.0)
                    elevationManager.getElevation(lat, lng)
                }
            } catch (e: Exception) { }
        }
    }

    // ---------- Precisión de calle: trayectoria reciente ----------
    private val trail = ArrayList<GeoPoint>()
    private val trailLong = ArrayList<GeoPoint>() // últimos ~1,2 km recorridos: el 20 % de atrás del perfil sale de aquí

    /** Guarda las últimas posiciones (150 m de recorrido) para emparejar la trayectoria con las calles. */
    fun recordFix(lat: Double, lng: Double) {
        synchronized(trail) {
            val p = GeoPoint(lat, lng)
            if (trail.isEmpty() || trail.last().distanceTo(p) >= 8.0) {
                trail.add(p)
                var len = 0.0
                for (i in 1 until trail.size) len += trail[i - 1].distanceTo(trail[i])
                while (len > 150.0 && trail.size > 2) { len -= trail[0].distanceTo(trail[1]); trail.removeAt(0) }
            }
            if (trailLong.isEmpty() || trailLong.last().distanceTo(p) >= 10.0) {
                trailLong.add(p)
                var len = 0.0
                for (i in 1 until trailLong.size) len += trailLong[i - 1].distanceTo(trailLong[i])
                while (len > 1200.0 && trailLong.size > 2) { len -= trailLong[0].distanceTo(trailLong[1]); trailLong.removeAt(0) }
            }
        }
    }

    private fun trailSnapshot(): List<GeoPoint> = synchronized(trail) { ArrayList(trail) }
    private fun trailLongSnapshot(): List<GeoPoint> = synchronized(trail) { ArrayList(trailLong) }

    private fun offsetPoint(p: GeoPoint, bearingDeg: Double, meters: Double): GeoPoint {
        val rad = Math.toRadians(bearingDeg)
        return GeoPoint(
            p.lat + meters * Math.cos(rad) / 111320.0,
            p.lng + meters * Math.sin(rad) / (111320.0 * Math.cos(Math.toRadians(p.lat)))
        )
    }

    /**
     * Puntos de la trayectoria ya recorrida, cada [step] m hacia atrás desde [cur], de más antiguo a más reciente
     * (el último está a [step] m). Si la trayectoria guardada es más corta, se prolonga en línea recta.
     */
    private fun behindSamples(cur: GeoPoint, heading: Double, count: Int, step: Double): List<GeoPoint> {
        val tr = trailLongSnapshot()
        val back = ArrayList<GeoPoint>()
        back.add(cur)
        for (i in tr.indices.reversed()) if (back.last().distanceTo(tr[i]) > 0.5) back.add(tr[i])
        val need = count * step
        var len = 0.0
        for (i in 1 until back.size) len += back[i - 1].distanceTo(back[i])
        if (len < need) {
            val dir = if (back.size >= 2) back[back.size - 2].bearingTo(back.last()) else (heading + 180.0) % 360.0
            back.add(offsetPoint(back.last(), dir, need - len + step))
        }
        val out = ArrayList<GeoPoint>()
        for (k in count downTo 1) {
            val target = k * step
            var acc = 0.0
            var pt = back.last()
            for (i in 1 until back.size) {
                val d = back[i - 1].distanceTo(back[i])
                if (acc + d >= target) {
                    val f = if (d <= 0.0) 0.0 else (target - acc) / d
                    pt = GeoPoint(back[i - 1].lat + (back[i].lat - back[i - 1].lat) * f, back[i - 1].lng + (back[i].lng - back[i - 1].lng) * f)
                    break
                }
                acc += d
            }
            out.add(pt)
        }
        return out
    }

    // Rumbo medido sobre la trayectoria real (más de 25 m atrás): más estable que el del GPS
    private fun trailHeading(cur: GeoPoint, t: List<GeoPoint>): Double? {
        for (i in t.indices.reversed()) if (t[i].distanceTo(cur) >= 25.0) return t[i].bearingTo(cur)
        return null
    }

    // Distancia media de la trayectoria reciente a la calle (limitada a 40 m): una calle paralela encaja peor que la real
    private fun trailCostFor(road: RoadSegment, t: List<GeoPoint>): Double {
        if (t.size < 3) return 0.0
        var sum = 0.0
        for (p in t) {
            var best = 40.0
            for (i in 0 until road.points.size - 1) {
                val d = distanceToSegment(p, road.points[i], road.points[i + 1])
                if (d < best) best = d
            }
            sum += best
        }
        return sum / t.size
    }

    // ---------- Precisión de cota: calibración con pendiente y aprendizaje propio ----------
    @Volatile var calibOffset = 0.0
        private set
    @Volatile var calibSlope = 0.0 // metros de corrección por metro recorrido, acotada
        private set
    @Volatile var calibValid = false
        private set
    private val xs = ArrayDeque<Double>()
    private var cumDist = 0.0
    private val corr = java.util.concurrent.ConcurrentHashMap<Long, Float>() // corrección aprendida por celda de ~12 m
    @Volatile private var corrDirty = false
    private var lastCorrSaveMs = 0L
    private val corrFile = File(context.filesDir, "dem_corr.bin")

    init {
        try {
            if (corrFile.isFile) java.io.DataInputStream(corrFile.inputStream().buffered()).use { input ->
                val n = input.readInt()
                for (i in 0 until n.coerceAtMost(400_000)) corr[input.readLong()] = input.readFloat()
            }
        } catch (e: Exception) { corr.clear() }
    }

    private fun cellKey(lat: Double, lng: Double): Long =
        Math.round(lat * 9000.0) * 4_000_000L + Math.round((lng + 180.0) * 9000.0)

    private fun maybeSaveCorr() {
        val now = System.currentTimeMillis()
        if (!corrDirty || now - lastCorrSaveMs < 60_000L) return
        lastCorrSaveMs = now
        corrDirty = false
        executor.execute {
            try {
                java.io.DataOutputStream(corrFile.outputStream().buffered()).use { out ->
                    val snap = corr.entries.toList().take(400_000)
                    out.writeInt(snap.size)
                    for (e in snap) { out.writeLong(e.key); out.writeFloat(e.value) }
                }
            } catch (e: Exception) { }
        }
    }

    private fun updateCalibration() {
        val n = minOf(20, diffs.size)
        if (n < 8) return
        val d = diffs.toList().takeLast(n)
        val x = xs.toList().takeLast(n)
        calibOffset = d.sorted()[n / 2]
        val mx = x.average(); val my = d.average()
        var num = 0.0; var den = 0.0
        for (i in 0 until n) { num += (x[i] - mx) * (d[i] - my); den += (x[i] - mx) * (x[i] - mx) }
        val slope = if (den > 1.0 && x.last() - x.first() >= 60.0) num / den else 0.0
        calibSlope = slope.coerceIn(-0.005, 0.005)
        calibValid = true
    }

    private fun learn(lat: Double, lng: Double, diff: Double) {
        val resid = (diff - calibOffset).coerceIn(-6.0, 6.0).toFloat()
        val k = cellKey(lat, lng)
        val old = corr[k]
        corr[k] = if (old == null) resid * 0.5f else old * 0.6f + resid * 0.4f
        corrDirty = true
        maybeSaveCorr()
    }

    data class HorizonResult(
        val elevations: List<Float>,
        val grades: List<Float>,
        val coordinates: List<Pair<Double, Double>>,
        val roadName: String?,
        val totalDistanceMeters: Double,
        val guessFromMeters: Double = -1.0 // distancia desde la que el trazado es una suposición en un cruce (-1 = ninguna)
    )

    fun invalidate() { cachedMap.clear() }

    private fun ensureMapReader(): KarooMapReader? {
        val now = System.currentTimeMillis()
        if (mapReader == null || now - lastMapCheckMs > 60_000L) {
            lastMapCheckMs = now
            val mapDir = File("/sdcard/offline/maps")
            val maps = mapDir.listFiles { f -> f.name.endsWith(".map") }?.toList().orEmpty()
            mapFileCount = maps.size
            android.util.Log.i("RoadHorizon", "maps dir exists=${mapDir.exists()} canRead=${mapDir.canRead()} maps=${maps.size}")
            if (maps.isNotEmpty()) {
                if (mapReader == null) {
                    mapReader = KarooMapReader(maps)
                }
            }
        }
        return mapReader
    }

    /**
     * Proyecta la carretera hacia adelante desde la posición actual y rumbo.
     * Retorna el horizonte predicho con sus cotas interpoladas desde el MDT.
     */
    fun predictHorizon(
        lat: Double,
        lng: Double,
        headingIn: Double,
        distanceMeters: Double = 3000.0,
        sampleStepMeters: Double = 25.0,
        speedMps: Double = -1.0,
        behindCount: Int = 0
    ): HorizonResult? {
        // Disparar en segundo plano la descarga si estamos en Wi-Fi
        elevationManager.considerDownload(lat, lng)

        val reader = ensureMapReader()
        if (reader == null) { android.util.Log.w("RoadHorizon", "no map reader (permission/maps?)"); lastStatus = Status.NO_MAP; return null }
        val currentPoint = GeoPoint(lat, lng)
        val trailPts = trailSnapshot()
        // Rumbo de la trayectoria real si la hay; si no, el que llega del GPS
        val headingDeg = trailHeading(currentPoint, trailPts) ?: headingIn
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastPosSaveMs > 30_000L) {
            lastPosSaveMs = nowMs
            appPrefs.horizonLastLat = lat.toFloat(); appPrefs.horizonLastLng = lng.toFloat()
        }
        if (anchorLat == 0.0) { anchorLat = lat; anchorLng = lng }
        if (!hasMoved && GeoPoint(anchorLat, anchorLng).distanceTo(currentPoint) > 6.0) hasMoved = true
        val stationary = !hasMoved && !(appPrefs.horizonHeadingMode == 1 && headingDeg != 0.0)
        val nearbyRoads = reader.getRoadsNear(currentPoint, radiusMeters = 80.0)
        android.util.Log.i("RoadHorizon", "roads near=${nearbyRoads.size} heading=$headingDeg")
        if (nearbyRoads.isEmpty()) { lastStatus = Status.NO_ROAD; return null }

        // 1. Snapping: la vía más cercana que va en nuestro sentido, en cualquiera de los dos sentidos de digitalización
        var bestRoad: RoadSegment? = null
        var bestIndex = 0
        var bestForward = true
        var bestScore = Double.MAX_VALUE
        // Mejor tramo de la calle que ya seguíamos, por si la nueva no gana de forma sostenida
        var prevRoad: RoadSegment? = null
        var prevIndex = 0
        var prevForward = true
        var prevScore = Double.MAX_VALUE
        val cyclewayBonus = if (appPrefs.horizonPreferCycleway) 8.0 else 0.0

        for (road in nearbyRoads) {
            // Caminos, pistas y accesos pierden frente a una calle al lado
            val minorCost = (if (road.kind in MINOR_KINDS) MINOR_COST_M else 0.0) +
                (if (speedMps > 3.3 && road.kind in FOOT_KINDS) 25.0 else 0.0) // a más de ~12 km/h no se va por un sendero
            val trailCost = if (stationary) 0.0 else trailCostFor(road, trailPts) * 0.5
            val cycleBonus = if (road.kind == "cycleway") cyclewayBonus else 0.0
            val isPrev = roadKey(road) == lastRoadKey
            for (i in 0 until road.points.size - 1) {
                val p1 = road.points[i]
                val p2 = road.points[i + 1]
                val segmentBearing = p1.bearingTo(p2)
                val off = if (stationary) 0.0 else abs(angleDifference(headingDeg, segmentBearing))
                val forward = off <= 90.0
                if (!forward && road.oneway) continue
                val bearingDiff = if (forward) off else 180.0 - off

                if (bearingDiff < SNAP_MAX_ANGLE) {
                    val distToSeg = distanceToSegment(currentPoint, p1, p2)
                    // Parado el rumbo no es fiable: gana la vía más cercana (la longitud solo desempata)
                    val score = (if (stationary) distToSeg - road.lengthMeters * 1e-4
                                 else distToSeg + bearingDiff * 0.3) + minorCost + trailCost - cycleBonus
                    if (distToSeg < 30.0 && score < bestScore) {
                        bestScore = score
                        bestRoad = road
                        bestIndex = i
                        bestForward = forward
                    }
                    if (isPrev && distToSeg < 35.0 && score < prevScore) {
                        prevScore = score
                        prevRoad = road
                        prevIndex = i
                        prevForward = forward
                    }
                }
            }
        }

        if (bestRoad == null) { android.util.Log.w("RoadHorizon", "no road aligned with heading"); lastStatus = Status.NO_ROAD; return null }

        // Histéresis: cambiar de calle exige que la nueva gane varias lecturas seguidas (evita saltar al carril o calle paralela)
        val bestKey = roadKey(bestRoad)
        if (prevRoad != null && bestKey != lastRoadKey) {
            if (pendingRoadKey == bestKey) pendingCount++ else { pendingRoadKey = bestKey; pendingCount = 1 }
            if (pendingCount < SWITCH_READINGS && prevScore - bestScore < SWITCH_NOW_MARGIN) {
                bestRoad = prevRoad; bestIndex = prevIndex; bestForward = prevForward
            } else { pendingRoadKey = null; pendingCount = 0 }
        } else { pendingRoadKey = null; pendingCount = 0 }
        lastRoadKey = roadKey(bestRoad)
        lastRoadName = bestRoad.name ?: bestRoad.ref
        // El trazado parte del punto proyectado sobre la vía (no del GPS crudo) para no quedar en el carril contiguo
        val snapped = projectOnSegment(currentPoint, bestRoad.points[bestIndex], bestRoad.points[bestIndex + 1])
        val snapDist = currentPoint.distanceTo(snapped)

        // Si se circula contra el sentido en que está dibujada la vía, se sigue por la copia invertida
        if (!bestForward) {
            val n = bestRoad.points.size
            bestIndex = n - 2 - bestIndex
            bestRoad = RoadSegment(
                bestRoad.points.reversed(), bestRoad.name, bestRoad.ref, bestRoad.kind, bestRoad.oneway,
                bestRoad.isRoundabout, bestRoad.isBridge, bestRoad.isTunnel
            )
        }
        // 2. Extender camino hacia adelante (Road Following)
        val projectedPoints = mutableListOf<GeoPoint>()
        projectedPoints.add(if (snapDist < 30.0) snapped else currentPoint)
        val flatFlags = mutableListOf(false) // punto i llegado por un puente o túnel
        var ringEntryBearing: Double? = null
        var ringEntryName: String? = null

        var currentRoad = bestRoad
        var currentIndex = bestIndex + 1
        var accumulatedDist = 0.0
        val visitedRoads = mutableSetOf<String>()
        var guessFrom = -1.0
        var mainRoadName = bestRoad.name ?: bestRoad.ref

        while (accumulatedDist < distanceMeters && currentRoad != null) {
            val key = "${currentRoad.name}_${currentRoad.lengthMeters}"
            if (!visitedRoads.add(key) && visitedRoads.size > 20) break // Prevenir bucles infinitos

            var prevPt = projectedPoints.last()
            while (currentIndex < currentRoad.points.size && accumulatedDist < distanceMeters) {
                val pt = currentRoad.points[currentIndex]
                val d = prevPt.distanceTo(pt)
                accumulatedDist += d
                projectedPoints.add(pt)
                flatFlags.add(currentRoad.isBridge || currentRoad.isTunnel)
                prevPt = pt
                currentIndex++
            }

            if (accumulatedDist >= distanceMeters) break

            // Cruce / Bifurcación: Buscar la siguiente carretera en el extremo
            val junctionPoint = projectedPoints.last()
            val candidateRoads = reader.getRoadsNear(junctionPoint, radiusMeters = 30.0)
                .filter { it !== currentRoad }

            var bestNextRoad: RoadSegment? = null
            var bestNextAngle = Double.MAX_VALUE
            var bestIsContinuation = false
            // Rumbo de llegada leído sobre los últimos ~15 m, para que un segmento corto y torcido no decida
            val currentBearing = bearingBack(projectedPoints, BEARING_LOOK_M) ?: headingDeg

            var eligibleCount = 0
            for (tol in doubleArrayOf(25.0, 90.0)) {
            for (cand in candidateRoads) {
                // Verificar si empieza cerca del junction (con huecos en los datos del mapa se amplía la tolerancia)
                val fromStart = cand.points.first().distanceTo(junctionPoint) < tol
                val fromEnd = !cand.oneway && cand.points.last().distanceTo(junctionPoint) < tol

                if (fromStart || fromEnd) {
                    val oriented = if (fromStart) cand.points else cand.points.reversed()
                    val nextPt = oriented[1]
                    val nextBearing = bearingForward(junctionPoint, oriented, BEARING_LOOK_M)
                    val signedTurn = angleDifference(currentBearing, nextBearing)
                    val turnAngle = abs(signedTurn)
                    val sameName = (currentRoad.name != null && currentRoad.name == cand.name)
                    // Seguir por la misma calle o por la que sigue recta no es una suposición: solo hay duda en bifurcaciones y T
                    val continuation = (sameName && turnAngle <= MAX_TURN) || turnAngle <= STRAIGHT_ANGLE

                    val baseScore = when (appPrefs.horizonJunctionMode) {
                        1 -> (if (sameName) 0.0 else 300.0) + turnAngle // misma calle por nombre, luego menor giro
                        2 -> turnAngle // menor giro
                        else -> { // izquierda, recto, derecha (a igualdad de clase, mismo nombre y menor giro)
                            val klass = when {
                                signedTurn < -25.0 -> 0
                                turnAngle <= 25.0 -> 1
                                else -> 2
                            }
                            klass * 1000.0 + (if (sameName) 0.0 else 300.0) + turnAngle
                        }
                    }
                    val onRing = currentRoad.isRoundabout
                    val score = when {
                        // Rotonda: se sale por la calle que sigue el rumbo de entrada (mismo nombre o menor desvío); si no, se sigue en el anillo
                        onRing && cand.isRoundabout -> 5_000.0
                        onRing -> {
                            val d = abs(angleDifference(ringEntryBearing ?: currentBearing, nextBearing))
                            if (d <= 70.0) (if (cand.name != null && cand.name == ringEntryName) 0.0 else 60.0) + d else 10_000.0 + d
                        }
                        continuation -> (if (sameName) 0.0 else 500.0) + turnAngle
                        else -> 100_000.0 + baseScore
                    }
                    val isContinuation = continuation || (onRing && score < 10_000.0)
                    val revisits = projectedPoints.any { it.distanceTo(nextPt) < 8.0 }
                    if (!revisits && turnAngle < MAX_TURN) eligibleCount++
                    if (!revisits && turnAngle < MAX_TURN && score < bestNextAngle) {
                        bestNextAngle = score
                        bestIsContinuation = isContinuation
                        bestNextRoad = if (fromStart) cand else RoadSegment(
                            cand.points.reversed(), cand.name, cand.ref, cand.kind, cand.oneway,
                            cand.isRoundabout, cand.isBridge, cand.isTunnel
                        )
                    }
                }
            }
            if (bestNextRoad != null) break
            }

            if (bestNextRoad != null && !bestIsContinuation && eligibleCount >= 1 && guessFrom < 0.0) guessFrom = accumulatedDist
            if (bestNextRoad != null) {
                if (!currentRoad.isRoundabout && bestNextRoad.isRoundabout) {
                    ringEntryBearing = currentBearing
                    ringEntryName = currentRoad.name
                }
                currentRoad = bestNextRoad
                currentIndex = 1
            } else {
                break // Fin de la vía o no hay continuación transitable
            }
        }

        if (projectedPoints.size < 2 || accumulatedDist < 100.0) { lastStatus = Status.NO_ROAD; return null }

        // 3. Muestrear elevación del MDT a lo largo de la polilínea proyectada
        val samplePoints = resamplePath(projectedPoints, sampleStepMeters, distanceMeters)
        val elevations = mutableListOf<Float>()

        for (p in samplePoints) {
            val elev = elevationManager.getElevation(p.lat, p.lng)
            // La corrección aprendida de tus propias pasadas se suma a la cota del terreno
            val learned = corr[cellKey(p.lat, p.lng)] ?: 0f
            elevations.add(if (elev != null) elev.toFloat() + learned else -999f)
        }
        if (elevations.all { it <= -900f }) { lastStatus = Status.NO_TERRAIN; return null }

        // Puentes y túneles: el MDT mide el suelo, así que se une con una recta la cota de entrada y la de salida
        flattenBridges(elevations, projectedPoints, flatFlags, sampleStepMeters)

        // El 20 % de atrás sale de la trayectoria real recorrida; va delante del trazado y se suaviza junto con él
        var allPoints: List<GeoPoint> = samplePoints
        if (behindCount > 0) {
            val behind = behindSamples(currentPoint, headingDeg, behindCount, sampleStepMeters)
            val behindElev = behind.map { p ->
                val e = elevationManager.getElevation(p.lat, p.lng)
                if (e != null) e.toFloat() + (corr[cellKey(p.lat, p.lng)] ?: 0f) else -999f
            }
            elevations.addAll(0, behindElev)
            allPoints = behind + samplePoints
        }

        // Si faltan algunas cotas puntuales, interpolar
        fixMissingElevations(elevations)
        smoothElevations(elevations, sampleStepMeters)
        lastStatus = Status.OK
        val maxGrade = appPrefs.horizonMaxGradePct.toDouble()
        android.util.Log.i("RoadHorizon", "road=$mainRoadName terrain=${elevationManager.lastSource} pts=${samplePoints.size} step=$sampleStepMeters dist=$accumulatedDist elev=${elevations.minOrNull()}..${elevations.maxOrNull()}")

        // Calcular pendientes (%)
        val grades = mutableListOf<Float>()
        for (i in 0 until elevations.size - 1) {
            val dE = (elevations[i + 1] - elevations[i]).toDouble()
            val grade = (dE / sampleStepMeters) * 100.0
            grades.add(grade.coerceIn(-maxGrade, maxGrade).toFloat())
        }

        return HorizonResult(
            elevations = elevations,
            grades = grades,
            coordinates = allPoints.map { Pair(it.lat, it.lng) },
            roadName = mainRoadName,
            totalDistanceMeters = (elevations.size - 1) * sampleStepMeters,
            guessFromMeters = if (guessFrom >= 0.0) guessFrom + behindCount * sampleStepMeters else -1.0
        ).also {
            lastDiagnostic = "road=$mainRoadName dist=${accumulatedDist.toInt()}m step=${sampleStepMeters.toInt()}m " +
                "elev=${"%.0f".format(elevations.minOrNull() ?: 0f)}..${"%.0f".format(elevations.maxOrNull() ?: 0f)} " +
                "pos=${"%.5f".format(lat)},${"%.5f".format(lng)} heading=${headingDeg.toInt()} maps=$mapFileCount guess=${guessFrom.toInt()} terrain=${elevationManager.lastSource}"
        }
    }

    private fun resamplePath(points: List<GeoPoint>, stepMeters: Double, maxDistMeters: Double): List<GeoPoint> {
        val result = mutableListOf<GeoPoint>()
        result.add(points.first())

        var accumulated = 0.0
        var targetDist = stepMeters
        var currentIdx = 0

        while (currentIdx < points.size - 1 && targetDist <= maxDistMeters) {
            val p1 = points[currentIdx]
            val p2 = points[currentIdx + 1]
            val segmentLen = p1.distanceTo(p2)

            if (accumulated + segmentLen >= targetDist) {
                val remain = targetDist - accumulated
                val frac = (remain / segmentLen).coerceIn(0.0, 1.0)
                val lat = p1.lat + (p2.lat - p1.lat) * frac
                val lng = p1.lng + (p2.lng - p1.lng) * frac
                result.add(GeoPoint(lat, lng))
                targetDist += stepMeters
            } else {
                accumulated += segmentLen
                currentIdx++
            }
        }
        return result
    }

    private fun fixMissingElevations(elevs: MutableList<Float>) {
        var lastValid = elevs.firstOrNull { it != -999f } ?: 0f
        for (i in elevs.indices) {
            if (elevs[i] == -999f) {
                elevs[i] = lastValid
            } else {
                lastValid = elevs[i]
            }
        }
    }

    // Puentes y túneles: se sustituye la cota del suelo por una recta entre la cota de entrada y la de salida
    private fun flattenBridges(e: MutableList<Float>, path: List<GeoPoint>, flags: List<Boolean>, step: Double) {
        if (flags.none { it } || e.size < 3) return
        val cum = DoubleArray(path.size)
        for (i in 1 until path.size) cum[i] = cum[i - 1] + path[i - 1].distanceTo(path[i])
        fun flatAt(k: Int): Boolean {
            val d = k * step
            var j = 1
            while (j < cum.size - 1 && cum[j] < d) j++
            return flags.getOrElse(j) { false }
        }
        var k = 1
        while (k < e.size - 1) {
            if (flatAt(k)) {
                val a = k - 1
                var b = k
                while (b < e.size - 1 && flatAt(b)) b++
                if (e[a] > -900f && e[b] > -900f) {
                    for (m in a + 1 until b) e[m] = e[a] + (e[b] - e[a]) * (m - a) / (b - a).toFloat()
                }
                k = b
            } else k++
        }
    }

    // El MDT recoge edificios y arbolado: una mediana quita los picos sueltos y la media suaviza lo que queda
    private fun smoothElevations(e: MutableList<Float>, stepM: Double) {
        if (e.size < 3) return
        val med = e.toList()
        for (i in 1 until med.size - 1) {
            e[i] = listOf(med[i - 1], med[i], med[i + 1]).sorted()[1]
        }
        val radius = Math.ceil(when (appPrefs.horizonSmoothing) { 0 -> 30.0; 2 -> 120.0; else -> 60.0 } / stepM).toInt().coerceAtLeast(1)
        val src = e.toList()
        for (i in src.indices) {
            val r = minOf(radius, i, src.size - 1 - i)
            var sum = 0f
            for (k in i - r..i + r) sum += src[k]
            e[i] = sum / (2 * r + 1)
        }
        e[0] = src[0]
        e[e.size - 1] = src[src.size - 1]
    }

    // Rumbo con el que se llega al final del trazado, medido sobre los últimos lookM metros
    private fun bearingBack(pts: List<GeoPoint>, lookM: Double): Double? {
        if (pts.size < 2) return null
        val end = pts.last()
        var i = pts.size - 2
        while (i > 0 && pts[i].distanceTo(end) < lookM) i--
        return if (pts[i].distanceTo(end) < 1.0) null else pts[i].bearingTo(end)
    }

    // Rumbo con el que sale una vía de un punto, medido sobre los primeros lookM metros
    private fun bearingForward(from: GeoPoint, pts: List<GeoPoint>, lookM: Double): Double {
        var target = pts[1]
        for (k in 1 until pts.size) {
            target = pts[k]
            if (from.distanceTo(pts[k]) >= lookM) break
        }
        return from.bearingTo(target)
    }

    // Confianza del terreno: se compara la altitud real con la del MDT mientras se avanza (0 = desconocida, 1 baja, 2 media, 3 alta)
    private val diffs = ArrayDeque<Double>()
    private var lastSampleLat = 0.0
    private var lastSampleLng = 0.0
    @Volatile var confidence: Int = 0
        private set

    fun demAt(lat: Double, lng: Double): Double? = elevationManager.getElevation(lat, lng)

    /** Registra la altitud real frente al MDT y devuelve el desfase (real - MDT), o null si no hay terreno aquí. */
    fun recordActual(lat: Double, lng: Double, actual: Double): Double? {
        val dem = demAt(lat, lng) ?: return null
        val diff = actual - dem
        if (lastSampleLat == 0.0 || GeoPoint(lastSampleLat, lastSampleLng).distanceTo(GeoPoint(lat, lng)) >= 5.0) {
            if (lastSampleLat != 0.0) cumDist += GeoPoint(lastSampleLat, lastSampleLng).distanceTo(GeoPoint(lat, lng))
            lastSampleLat = lat; lastSampleLng = lng
            xs.addLast(cumDist)
            diffs.addLast(diff)
            if (diffs.size > 60) { diffs.removeFirst(); xs.removeFirst() }
            if (diffs.size >= 12) {
                val mean = diffs.average()
                val std = Math.sqrt(diffs.sumOf { (it - mean) * (it - mean) } / diffs.size)
                confidence = if (std < 2.5) 3 else if (std < 5.0) 2 else 1
                updateCalibration()
                if (confidence >= 2) learn(lat, lng, diff)
            }
        }
        return diff
    }

    private fun roadKey(r: RoadSegment) = "${r.name}_${r.lengthMeters.toInt()}_${r.points.first().lat}_${r.points.first().lng}"

    private fun projectOnSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): GeoPoint {
        val kLng = Math.cos(Math.toRadians(p.lat))
        val dx = (b.lng - a.lng) * kLng
        val dy = b.lat - a.lat
        val l2 = dx * dx + dy * dy
        if (l2 == 0.0) return a
        val t = ((((p.lng - a.lng) * kLng) * dx + (p.lat - a.lat) * dy) / l2).coerceIn(0.0, 1.0)
        return GeoPoint(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
    }

    private fun angleDifference(a: Double, b: Double): Double {
        var diff = (b - a) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    private fun distanceToSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val l2 = a.distanceTo(b).pow(2)
        if (l2 == 0.0) return p.distanceTo(a)
        val dLat = b.lat - a.lat
        val dLng = b.lng - a.lng
        val t = (((p.lat - a.lat) * dLat + (p.lng - a.lng) * dLng) / (dLat * dLat + dLng * dLng)).coerceIn(0.0, 1.0)
        val proj = GeoPoint(a.lat + t * dLat, a.lng + t * dLng)
        return p.distanceTo(proj)
    }

    companion object {
        @Volatile
        private var INSTANCE: RoadHorizonPredictor? = null

        fun getInstance(context: Context): RoadHorizonPredictor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoadHorizonPredictor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
