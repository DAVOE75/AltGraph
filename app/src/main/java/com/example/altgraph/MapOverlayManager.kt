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
    }

    fun stop() {
        synchronized(emitLock) { job.cancel() }
        consumerId?.let { karooSystem.removeConsumer(it) }
        consumerId = null
    }

    private fun updateOverlay(navEvent: OnNavigationState, emitter: Emitter<MapEffect>) {
        if (stopped) return
        val state = navEvent.state as? OnNavigationState.NavigationState.NavigatingRoute
        
        val mode = prefs.eliteMapOverlayMode
        Log.d("MapOverlayManager", "updateOverlay: mode=$mode, unlocked=${prefs.isEliteUnlocked}, state=${state != null}")
        if (mode == "off" || !prefs.isEliteUnlocked || state == null) {
            clearPolylines(emitter)
            return
        }

        // Just use polyline hash as routeId since NavigatingRoute doesn't expose routeId natively here
        // La clave incluye los ajustes: al cambiarlos hay que redibujar la ruta aunque sea la misma
        val routeId = state.routePolyline.take(30) +
            "|$mode|${prefs.eliteSmoothMapOverlayEnabled}|${prefs.eliteRadarResolution}"
        Log.d("MapOverlayManager", "updateOverlay: routeId=$routeId, last=$lastRouteId")
        if (state.routePolyline.isEmpty()) {
            clearPolylines(emitter)
            lastRouteId = null
            return
        }

        if (routeId == lastRouteId) return
        lastRouteId = routeId

        var elevEncoded = state.routeElevationPolyline
        if (elevEncoded.isNullOrEmpty()) {
            elevEncoded = (state.javaClass.methods.find { it.name == "getElevationPolyline" }?.invoke(state) as? String)
        }
        val pathEncoded = state.routePolyline
        Log.d("MapOverlayManager", "updateOverlay: elevLength=${elevEncoded?.length}, pathLength=${pathEncoded?.length}")

        if (elevEncoded.isNullOrEmpty() || pathEncoded.isNullOrEmpty()) {
            clearPolylines(emitter)
            return
        }

        try {
            val safeRouteDist = state.routeDistance ?: 0.0
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
        } catch (e: Exception) {
            Log.e("MapOverlayManager", "Error processing overlay", e)
        }
    }

    private fun clearPolylines(emitter: Emitter<MapEffect>) {
        try {
            for (id in activePolylines) {
                if (!emit(emitter, HidePolyline(id))) break
            }
        } catch (e: Exception) {
            // El mapa del host puede haber desaparecido (DeadObjectException)
            Log.w("MapOverlayManager", "clearPolylines failed", e)
        }
        activePolylines.clear()
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
