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
    private var mapReader: KarooMapReader? = null
    private var lastMapCheckMs = 0L
    private var anchorLat = 0.0
    private var anchorLng = 0.0
    private var hasMoved = false
    private var lastRoadKey: String? = null
    private val STICKY_BONUS_M = 15.0

    data class HorizonResult(
        val elevations: List<Float>,
        val grades: List<Float>,
        val coordinates: List<Pair<Double, Double>>,
        val roadName: String?,
        val totalDistanceMeters: Double
    )

    private fun ensureMapReader(): KarooMapReader? {
        val now = System.currentTimeMillis()
        if (mapReader == null || now - lastMapCheckMs > 60_000L) {
            lastMapCheckMs = now
            val mapDir = File("/sdcard/offline/maps")
            val maps = mapDir.listFiles { f -> f.name.endsWith(".map") }?.toList().orEmpty()
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
        headingDeg: Double,
        distanceMeters: Double = 3000.0,
        sampleStepMeters: Double = 25.0
    ): HorizonResult? {
        // Disparar en segundo plano la descarga si estamos en Wi-Fi
        elevationManager.considerDownload(lat, lng)

        val reader = ensureMapReader()
        if (reader == null) { android.util.Log.w("RoadHorizon", "no map reader (permission/maps?)"); return null }
        val currentPoint = GeoPoint(lat, lng)
        if (anchorLat == 0.0) { anchorLat = lat; anchorLng = lng }
        if (!hasMoved && GeoPoint(anchorLat, anchorLng).distanceTo(currentPoint) > 15.0) hasMoved = true
        val stationary = !hasMoved
        val nearbyRoads = reader.getRoadsNear(currentPoint, radiusMeters = 80.0)
        android.util.Log.i("RoadHorizon", "roads near=${nearbyRoads.size} heading=$headingDeg")
        if (nearbyRoads.isEmpty()) return null

        // 1. Snapping: Encontrar el tramo de carretera más alineado con nuestro rumbo
        var bestRoad: RoadSegment? = null
        var bestIndex = 0
        var bestScore = Double.MAX_VALUE

        for (road in nearbyRoads) {
            for (i in 0 until road.points.size - 1) {
                val p1 = road.points[i]
                val p2 = road.points[i + 1]
                val segmentBearing = p1.bearingTo(p2)
                val bearingDiff = if (stationary) 0.0 else abs(angleDifference(headingDeg, segmentBearing))

                // Solo consideramos tramos que van más o menos en nuestra dirección (< 60 grados)
                if (bearingDiff < 60.0) {
                    val distToSeg = distanceToSegment(currentPoint, p1, p2)
                    val sticky = if (roadKey(road) == lastRoadKey) STICKY_BONUS_M else 0.0
                    // Parado el rumbo no es fiable: gana la vía más cercana (la longitud solo desempata)
                    val score = if (stationary) distToSeg - sticky - road.lengthMeters * 1e-4
                                else distToSeg + bearingDiff * 0.3 - sticky
                    if (distToSeg < 30.0 && score < bestScore) {
                        bestScore = score
                        bestRoad = road
                        bestIndex = i
                    }
                }
            }
        }

        if (bestRoad == null) { android.util.Log.w("RoadHorizon", "no road aligned with heading"); return null }
        lastRoadKey = roadKey(bestRoad)

        // El trazado parte del punto proyectado sobre la vía (no del GPS crudo) para no quedar en el carril contiguo
        val snapped = projectOnSegment(currentPoint, bestRoad.points[bestIndex], bestRoad.points[bestIndex + 1])
        val snapDist = currentPoint.distanceTo(snapped)

        // 2. Extender camino hacia adelante (Road Following)
        val projectedPoints = mutableListOf<GeoPoint>()
        projectedPoints.add(if (snapDist < 30.0) snapped else currentPoint)

        var currentRoad = bestRoad
        var currentIndex = bestIndex + 1
        var accumulatedDist = 0.0
        val visitedRoads = mutableSetOf<String>()
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
            val currentBearing = if (projectedPoints.size >= 2) {
                projectedPoints[projectedPoints.size - 2].bearingTo(junctionPoint)
            } else headingDeg

            for (tol in doubleArrayOf(25.0, 90.0)) {
            for (cand in candidateRoads) {
                // Verificar si empieza cerca del junction (con huecos en los datos del mapa se amplía la tolerancia)
                val fromStart = cand.points.first().distanceTo(junctionPoint) < tol
                val fromEnd = !cand.oneway && cand.points.last().distanceTo(junctionPoint) < tol

                if (fromStart || fromEnd) {
                    val nextPt = if (fromStart) cand.points[1] else cand.points[cand.points.size - 2]
                    val nextBearing = junctionPoint.bearingTo(nextPt)
                    val signedTurn = angleDifference(currentBearing, nextBearing)
                    val turnAngle = abs(signedTurn)
                    val sameName = (currentRoad.name != null && currentRoad.name == cand.name)

                    // Prioridad ante la duda: izquierda, recto, derecha (a igualdad de clase, mismo nombre y menor giro)
                    val klass = when {
                        signedTurn < -25.0 -> 0
                        turnAngle <= 25.0 -> 1
                        else -> 2
                    }
                    val score = klass * 1000.0 + (if (sameName) 0.0 else 300.0) + turnAngle
                    val revisits = projectedPoints.any { it.distanceTo(nextPt) < 8.0 }
                    if (!revisits && turnAngle < 135.0 && score < bestNextAngle) {
                        bestNextAngle = score
                        bestNextRoad = if (fromStart) cand else RoadSegment(
                            cand.points.reversed(), cand.name, cand.ref, cand.kind, cand.oneway,
                            cand.isRoundabout, cand.isBridge, cand.isTunnel
                        )
                    }
                }
            }
            if (bestNextRoad != null) break
            }

            if (bestNextRoad != null) {
                currentRoad = bestNextRoad
                currentIndex = 1
            } else {
                val near = candidateRoads.map { minOf(it.points.first().distanceTo(junctionPoint), it.points.last().distanceTo(junctionPoint)) to it }.sortedBy { it.first }.take(3)
                android.util.Log.i("RoadHorizon", "path ends at ${junctionPoint.lat},${junctionPoint.lng} after ${accumulatedDist.toInt()} m, candidates=${candidateRoads.size} road=${currentRoad.name} pts=${currentRoad.points.size} nearest=" + near.joinToString { "%.0fm/%s/oneway=%s/pts=%d".format(it.first, it.second.name, it.second.oneway, it.second.points.size) })
                break // Fin de la vía o no hay continuación transitable
            }
        }

        if (projectedPoints.size < 2 || accumulatedDist < 100.0) return null
        android.util.Log.i("RoadHorizon", "path=" + projectedPoints.filterIndexed { i, _ -> i % 3 == 0 }.joinToString(";") { "%.5f,%.5f".format(it.lat, it.lng) })

        // 3. Muestrear elevación del MDT a lo largo de la polilínea proyectada
        val samplePoints = resamplePath(projectedPoints, sampleStepMeters, distanceMeters)
        val elevations = mutableListOf<Float>()

        for (p in samplePoints) {
            val elev = elevationManager.getElevation(p.lat, p.lng)
            elevations.add(elev?.toFloat() ?: -999f)
        }

        // Si faltan algunas cotas puntuales, interpolar
        fixMissingElevations(elevations)
        smoothElevations(elevations, sampleStepMeters)
        android.util.Log.i("RoadHorizon", "road=$mainRoadName pts=${samplePoints.size} step=$sampleStepMeters dist=$accumulatedDist elev=${elevations.minOrNull()}..${elevations.maxOrNull()} first=${samplePoints.first().lat},${samplePoints.first().lng} last=${samplePoints.last().lat},${samplePoints.last().lng}")

        // Calcular pendientes (%)
        val grades = mutableListOf<Float>()
        for (i in 0 until elevations.size - 1) {
            val dE = (elevations[i + 1] - elevations[i]).toDouble()
            val grade = (dE / sampleStepMeters) * 100.0
            grades.add(grade.coerceIn(-30.0, 30.0).toFloat())
        }

        return HorizonResult(
            elevations = elevations,
            grades = grades,
            coordinates = samplePoints.map { Pair(it.lat, it.lng) },
            roadName = mainRoadName,
            totalDistanceMeters = (elevations.size - 1) * sampleStepMeters
        )
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

    // El MDT es de 1 m de resolución vertical y recoge edificios/arbolado: sin suavizar salen rampas falsas en llano
    private fun smoothElevations(e: MutableList<Float>, stepM: Double) {
        if (e.size < 3) return
        val radius = Math.ceil(60.0 / stepM).toInt().coerceAtLeast(1)
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
