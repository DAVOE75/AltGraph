package com.example.altgraph

import android.content.Context
import android.graphics.Color
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.HidePolyline
import io.hammerhead.karooext.models.MapEffect
import io.hammerhead.karooext.models.OnNavigationState
import io.hammerhead.karooext.models.ShowPolyline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

class MapOverlayManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val karooSystem: KarooSystemService
) {
    private val prefs = AppPreferences(context)
    private val calculator = AltimetriaStrategyCalculator()
    private var lastRouteId: String? = null
    private var activePolylines = mutableSetOf<String>()

    data class GradeSegment(val startDist: Double, val endDist: Double, val grade: Double)

    private var consumerId: String? = null
    // Decodificar y recortar la ruta es pesado: fuera del hilo principal y de uno en uno
    @OptIn(ExperimentalCoroutinesApi::class)
    private val worker = Dispatchers.Default.limitedParallelism(1)
    // Trabajo de este manager: se cancela en stop() para que nada encolado siga emitiendo
    private val job = SupervisorJob(scope.coroutineContext[Job])
    private val stopped get() = !job.isActive
    // stop() y cada emisión van bajo el mismo lock: tras stop() no sale ningún efecto más
    private val emitLock = Any()

    private fun emit(emitter: Emitter<MapEffect>, effect: MapEffect): Boolean = synchronized(emitLock) {
        if (stopped) return false
        emitter.onNext(effect)
        true
    }

    fun start(emitter: Emitter<MapEffect>) {
        consumerId = karooSystem.addConsumer<OnNavigationState> { navEvent ->
            scope.launch(worker + job) { updateOverlay(navEvent, emitter) }
        }
        scope.launch(worker + job) {
            while (isActive) {
                delay(1500)
                try { drawHorizon(emitter) } catch (e: Exception) { Log.w("MapOverlayManager", "horizon overlay failed", e) }
            }
        }
    }

    // Sin ruta cargada: el trazado previsto del Road Horizon se pinta en el mapa con el color de la pendiente
    @Volatile private var routeOnMap = false
    private var horizonSig: String? = null

    private fun drawHorizon(emitter: Emitter<MapEffect>) {
        if (stopped) return
        val snap = HorizonOverlayBus.latest
        val mode = prefs.eliteMapOverlayMode
        val fresh = snap != null && System.currentTimeMillis() - snap.timeMs < 6000L
        if (routeOnMap || !prefs.horizonMapLine || !prefs.roadHorizonEnabled || mode == "off" || !prefs.isEliteUnlocked || !fresh || snap == null) {
            if (horizonSig != null && !routeOnMap) { clearPolylines(emitter); horizonSig = null }
            return
        }
        val i0 = Math.floor(snap.riderIndex).toInt().coerceIn(0, snap.coords.size - 2)
        val coords = snap.coords.subList(i0, snap.coords.size)
        if (coords.size < 2) return
        val grades = snap.grades
        val sig = "${"%.5f".format(coords.first().first)},${"%.5f".format(coords.first().second)}|${coords.size}|${"%.5f".format(coords.last().first)}|${prefs.eliteRadarResolution}|${prefs.horizonMapSmooth}"
        if (sig == horizonSig) return

        val step = snap.stepMeters
        val segs = ArrayList<GradeSegment>()
        for (k in 0 until coords.size - 1) {
            val g = grades.getOrNull(i0 + k)?.toDouble() ?: 0.0
            segs.add(GradeSegment(k * step, (k + 1) * step, g))
        }
        val minSeg = if (prefs.eliteRadarResolution > 0) prefs.eliteRadarResolution.toDouble() else 100.0
        val merged = mergeShortSegments(segs, minSeg)

        if (stopped) return
        clearPolylines(emitter)
        horizonSig = sig
        var n = 0
        val smooth = prefs.horizonMapSmooth
        // Puntos del trazado entre dos distancias (interpolando los extremos)
        fun pointAt(d: Double): Pair<Double, Double> {
            val f = (d / step).coerceIn(0.0, (coords.size - 1).toDouble())
            val i = Math.floor(f).toInt().coerceAtMost(coords.size - 2)
            val t = f - i
            return Pair(coords[i].first + (coords[i + 1].first - coords[i].first) * t, coords[i].second + (coords[i + 1].second - coords[i].second) * t)
        }
        fun sub(d0: Double, d1: Double): List<Pair<Double, Double>> {
            val out = ArrayList<Pair<Double, Double>>()
            out.add(pointAt(d0))
            var k = Math.floor(d0 / step).toInt() + 1
            while (k * step < d1 && k < coords.size) { out.add(coords[k]); k++ }
            out.add(pointAt(d1))
            return out
        }
        fun show(pts: List<Pair<Double, Double>>, color: Int): Boolean {
            if (pts.size < 2) return true
            val id = "altgraph-grade-${n++}"
            if (!emit(emitter, ShowPolyline(id = id, encodedPolyline = PolylineCodec.encode(pts, 1e5), color = color, width = 8))) return false
            activePolylines.add(id)
            return true
        }
        for ((idx, seg) in merged.withIndex()) {
            val c1 = Color.parseColor(GradeColorScale.getColorHex(seg.grade))
            val next = merged.getOrNull(idx + 1)
            val len = seg.endDist - seg.startDist
            if (smooth && next != null && next.startDist == seg.endDist && len >= 80.0) {
                // Igual que el coloreado de ruta de Elite: tramo base y zona de difuminado en 3 pasos hacia el color siguiente
                val c2 = Color.parseColor(GradeColorScale.getColorHex(next.grade))
                val blend = minOf(60.0, len * 0.4)
                val solidEnd = seg.endDist - blend
                if (!show(sub(seg.startDist, solidEnd), c1)) return
                for (s in 0 until 3) {
                    val p0 = solidEnd + s * blend / 3
                    val p1 = solidEnd + (s + 1) * blend / 3
                    if (!show(sub(p0, p1), blendColors(c1, c2, (s + 0.5f) / 3f))) return
                }
            } else if (!show(sub(seg.startDist, seg.endDist), c1)) return
        }
        if (n > highWaterPolylines) highWaterPolylines = n
        val hw = context.getSharedPreferences(HW_PREFS, Context.MODE_PRIVATE)
        if (n > hw.getInt(HW_KEY, 0) || !hw.contains(HW_KEY)) hw.edit().putInt(HW_KEY, n).apply()
    }

    fun stop() {
        synchronized(emitLock) { job.cancel() }
        consumerId?.let { karooSystem.removeConsumer(it) }
        consumerId = null
    }

    private fun updateOverlay(navEvent: OnNavigationState, emitter: Emitter<MapEffect>) {
        if (stopped) return
        val nav = navEvent.state
        val route = nav as? OnNavigationState.NavigationState.NavigatingRoute
        val dest = nav as? OnNavigationState.NavigationState.NavigatingToDestination
        val polyEnc: String? = route?.routePolyline ?: dest?.polyline
        routeOnMap = !polyEnc.isNullOrEmpty()
        val elevEnc0: String? = route?.routeElevationPolyline ?: dest?.elevationPolyline
        val routeDist: Double = route?.routeDistance ?: 0.0
        val state = if (polyEnc != null) nav else null

        val mode = prefs.eliteMapOverlayMode
        Log.d("MapOverlayManager", "updateOverlay: mode=$mode, unlocked=${prefs.isEliteUnlocked}, state=${state != null}, type=${nav.javaClass.simpleName}")
        if (mode == "off" || !prefs.isEliteUnlocked || state == null) {
            clearPolylines(emitter)
            return
        }

        val routeId = polyEnc!!.take(30) +
            "|$mode|${prefs.eliteSmoothMapOverlayEnabled}|${prefs.eliteRadarResolution}"
        Log.d("MapOverlayManager", "updateOverlay: routeId=$routeId, last=$lastRouteId")
        if (polyEnc.isEmpty()) {
            clearPolylines(emitter)
            lastRouteId = null
            return
        }

        if (routeId == lastRouteId) return
        lastRouteId = routeId

        var elevEncoded = elevEnc0
        if (elevEncoded.isNullOrEmpty() && route != null) {
            elevEncoded = (route.javaClass.methods.find { it.name == "getElevationPolyline" }?.invoke(route) as? String)
        }
        val pathEncoded = polyEnc
        Log.d("MapOverlayManager", "updateOverlay: elevLength=${elevEncoded?.length}, pathLength=${pathEncoded?.length}")

        if (elevEncoded.isNullOrEmpty() || pathEncoded.isNullOrEmpty()) {
            clearPolylines(emitter)
            return
        }

        try {
            val safeRouteDist = routeDist
            val elevResult = ElevationPolylineDecoder.decodeSafe(elevEncoded, safeRouteDist)
            if (elevResult !is ElevationPolylineDecoder.DecodeResult.Success) return

            val elevPoints = ElevationPolylineDecoder.smooth(elevResult.points, 10)
            val segments = createGradeSegments(elevPoints)
            
            // Filter by climbs if needed
            val filteredSegments = if (mode == "climbs") {
                calculator.setRouteElevationProfile(elevEncoded, safeRouteDist)
                val climbs = calculator.routeClimbs
                segments.filter { seg ->
                    climbs.any { climb -> seg.startDist >= climb.startDistance && seg.endDist <= climb.endDistance }
                }
            } else {
                segments // Do not filter out flats for 'entire route' mode
            }
            
            val customResolution = prefs.eliteRadarResolution
            val minSegmentM = if (customResolution > 0) customResolution.toDouble() else 300.0
            val finalSegments = mergeShortSegments(filteredSegments, minSegmentM)
            val smoothOverlay = prefs.eliteSmoothMapOverlayEnabled

            // Escalar a la distancia del perfil: los tramos se miden sobre él
            val path = RoutePath.fromPolyline(pathEncoded, elevPoints.lastOrNull()?.distance) ?: return

            Log.d("MapOverlayManager", "updateOverlay: created ${finalSegments.size} segments (smooth=$smoothOverlay)")
            if (stopped) return // el decode puede tardar: no emitir si ya se paró
            clearPolylines(emitter)

            var polylineCount = 0
            // Carretera repetida (ida y vuelta): cada sentido se dibuja desplazado a su derecha para que se vean los dos colores
            fun offsetPath(a: Double, b: Double) = path.subPathOffset(a, b, 5.0)
            for (i in finalSegments.indices) {
                if (stopped) return
                val seg = finalSegments[i]
                val nextSeg = finalSegments.getOrNull(i + 1)
                val c1 = Color.parseColor(GradeColorScale.getColorHex(seg.grade))

                val segLen = seg.endDist - seg.startDist
                val canSmoothTransition = smoothOverlay && nextSeg != null && nextSeg.startDist == seg.endDist && segLen >= 80.0

                if (canSmoothTransition && nextSeg != null) {
                    val c2 = Color.parseColor(GradeColorScale.getColorHex(nextSeg.grade))
                    val blendZoneM = minOf(60.0, segLen * 0.4)
                    val solidEnd = seg.endDist - blendZoneM

                    // Tramo principal con color base
                    val mainSubPath = offsetPath(seg.startDist, solidEnd)
                    if (mainSubPath.size >= 2) {
                        val encoded = PolylineCodec.encode(mainSubPath, 1e5)
                        val polylineId = "altgraph-grade-${polylineCount++}"
                        if (!emit(emitter, ShowPolyline(id = polylineId, encodedPolyline = encoded, color = c1, width = 8))) return
                        activePolylines.add(polylineId)
                    }

                    // Zona de difuminado dividida en 3 micropasos interpolados
                    val steps = 3
                    val stepDist = blendZoneM / steps
                    for (s in 0 until steps) {
                        if (stopped) return
                        val pStart = solidEnd + s * stepDist
                        val pEnd = solidEnd + (s + 1) * stepDist
                        val t = (s + 0.5f) / steps.toFloat()
                        val blendedColor = blendColors(c1, c2, t)
                        val blendSubPath = offsetPath(pStart, pEnd)
                        if (blendSubPath.size >= 2) {
                            val encoded = PolylineCodec.encode(blendSubPath, 1e5)
                            val polylineId = "altgraph-grade-${polylineCount++}"
                            if (!emit(emitter, ShowPolyline(id = polylineId, encodedPolyline = encoded, color = blendedColor, width = 8))) return
                            activePolylines.add(polylineId)
                        }
                    }
                } else {
                    val subPath = offsetPath(seg.startDist, seg.endDist)
                    if (subPath.size >= 2) {
                        val encoded = PolylineCodec.encode(subPath, 1e5)
                        val polylineId = "altgraph-grade-${polylineCount++}"
                        if (!emit(emitter, ShowPolyline(id = polylineId, encodedPolyline = encoded, color = c1, width = 8))) return
                        activePolylines.add(polylineId)
                    }
                }
            }
            Log.d("MapOverlayManager", "updateOverlay: Emitted ${activePolylines.size} polylines!")
            if (polylineCount > highWaterPolylines) highWaterPolylines = polylineCount
            val hwPrefs = context.getSharedPreferences(HW_PREFS, Context.MODE_PRIVATE)
            if (polylineCount > hwPrefs.getInt(HW_KEY, 0) || !hwPrefs.contains(HW_KEY)) {
                hwPrefs.edit().putInt(HW_KEY, polylineCount).apply()
            }
        } catch (e: Exception) {
            Log.e("MapOverlayManager", "Error processing overlay", e)
        }
    }

    private fun clearPolylines(emitter: Emitter<MapEffect>) {
        try {
            // También los ids de instancias o procesos anteriores: el mapa del host conserva sus líneas
            val ids = LinkedHashSet<String>(activePolylines)
            val known = maxOf(highWaterPolylines, context.getSharedPreferences(HW_PREFS, Context.MODE_PRIVATE).getInt(HW_KEY, DEFAULT_HIGH_WATER))
            for (n in 0 until known) ids.add("altgraph-grade-$n")
            for (id in ids) {
                if (!emit(emitter, HidePolyline(id))) break
            }
        } catch (e: Exception) {
            // El mapa del host puede haber desaparecido (DeadObjectException)
            Log.w("MapOverlayManager", "clearPolylines failed", e)
        }
        activePolylines.clear()
        lastRouteId = null
        horizonSig = null
    }

    private fun getElevationAt(points: List<ElevationPolylineDecoder.ElevationPoint>, distance: Double): Double {
        if (points.isEmpty()) return 0.0
        val d = distance.coerceIn(points.first().distance, points.last().distance)
        var lo = 0
        var hi = points.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (points[mid].distance <= d) lo = mid else hi = mid
        }
        val p1 = points[lo]
        val p2 = points[hi]
        if (p2.distance == p1.distance) return p1.elevation
        val t = (d - p1.distance) / (p2.distance - p1.distance)
        return p1.elevation + t * (p2.elevation - p1.elevation)
    }

    private fun createGradeSegments(points: List<ElevationPolylineDecoder.ElevationPoint>): List<GradeSegment> {
        val segments = mutableListOf<GradeSegment>()
        if (points.isEmpty()) return segments

        val totalDist = points.last().distance
        val step = 10.0

        var currentStartDist = -1.0
        var currentEndDist = -1.0
        var currentColorHex = ""
        var currentGradeSum = 0.0
        var currentWeight = 0.0

        var d = 0.0
        while (d < totalDist - 1.0) {
            val endD = minOf(d + step, totalDist)
            val dist = endD - d
            if (dist < 5.0) {
                d = endD
                continue
            }
            
            val e1 = getElevationAt(points, d)
            val e2 = getElevationAt(points, endD)
            val grade = ((e2 - e1) / dist) * 100.0
            val colorHex = GradeColorScale.getColorHex(grade)

            if (currentStartDist < 0) {
                currentStartDist = d
                currentEndDist = endD
                currentColorHex = colorHex
                currentGradeSum = grade * dist
                currentWeight = dist
            } else if (colorHex == currentColorHex) {
                currentEndDist = endD
                currentGradeSum += grade * dist
                currentWeight += dist
            } else {
                val avgGrade = if (currentWeight > 0) currentGradeSum / currentWeight else 0.0
                segments.add(GradeSegment(currentStartDist, currentEndDist, avgGrade))
                
                currentStartDist = d
                currentEndDist = endD
                currentColorHex = colorHex
                currentGradeSum = grade * dist
                currentWeight = dist
            }
            d = endD
        }
        
        if (currentStartDist >= 0) {
            val avgGrade = if (currentWeight > 0) currentGradeSum / currentWeight else 0.0
            segments.add(GradeSegment(currentStartDist, currentEndDist, avgGrade))
        }

        return segments
    }

    companion object {
        @Volatile private var highWaterPolylines = 0
        private const val HW_PREFS = "altgraph_map_overlay"
        private const val HW_KEY = "polyline_high_water"
        // Sin dato guardado (versiones previas) se ocultan hasta 1500 ids una vez
        private const val DEFAULT_HIGH_WATER = 1500

        // Nota: umbral fijo; si hiciera falta más detalle en puertos cortos, bajarlo
        private const val MIN_SEGMENT_M = 300.0

        /**
         * Con pasos de 10 m salen cientos o miles de tramos en una ruta larga, y cada uno
         * es una polilínea en el mapa del Karoo. Une entre sí los tramos cortos contiguos
         * hasta MIN_SEGMENT_M; un tramo largo nunca se une a nada, así no se diluye una
         * subida o bajada larga en la media de un tramo corto.
         * Nota: los cortos agrupados se promedian aunque mezclen subida y bajada (son < 300 m)
         */
        internal fun mergeShortSegments(segments: List<GradeSegment>, minSegmentM: Double): List<GradeSegment> {
            val merged = mutableListOf<GradeSegment>()
            for (seg in segments) {
                val prev = merged.lastOrNull()
                if (prev != null && prev.endDist == seg.startDist &&
                    length(prev) < minSegmentM && length(seg) < minSegmentM) {
                    merged[merged.lastIndex] = combine(prev, seg)
                } else {
                    merged.add(seg)
                }
            }
            return merged
        }

        private fun length(s: GradeSegment) = s.endDist - s.startDist

        private fun combine(a: GradeSegment, b: GradeSegment): GradeSegment {
            val la = length(a)
            val lb = length(b)
            val grade = if (la + lb > 0) (a.grade * la + b.grade * lb) / (la + lb) else a.grade
            return GradeSegment(a.startDist, b.endDist, grade)
        }

        private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRatio = 1f - ratio
            val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt().coerceIn(0, 255)
            val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt().coerceIn(0, 255)
            val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt().coerceIn(0, 255)
            return Color.rgb(r, g, b)
        }
    }

    data class PathPoint(val lat: Double, val lng: Double, val distance: Double)

    private fun assignDistancesToPath(path: List<Pair<Double, Double>>): List<PathPoint> {
        val result = mutableListOf<PathPoint>()
        var totalDist = 0.0
        
        if (path.isEmpty()) return result
        result.add(PathPoint(path[0].first, path[0].second, 0.0))
        
        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]
            val dist = haversineDistance(prev.first, prev.second, curr.first, curr.second)
            totalDist += dist
            result.add(PathPoint(curr.first, curr.second, totalDist))
        }
        return result
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val phi1 = lat1 * PI / 180.0
        val phi2 = lat2 * PI / 180.0
        val deltaPhi = (lat2 - lat1) * PI / 180.0
        val deltaLambda = (lon2 - lon1) * PI / 180.0

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
