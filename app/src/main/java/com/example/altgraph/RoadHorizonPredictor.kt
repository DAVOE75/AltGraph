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

        val reader = ensureMapReader() ?: return null
        val currentPoint = GeoPoint(lat, lng)
        val nearbyRoads = reader.getRoadsNear(currentPoint, radiusMeters = 80.0)
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
                val bearingDiff = abs(angleDifference(headingDeg, segmentBearing))

                // Solo consideramos tramos que van más o menos en nuestra dirección (< 75 grados)
                if (bearingDiff < 75.0) {
                    val distToSeg = distanceToSegment(currentPoint, p1, p2)
                    if (distToSeg < 35.0 && distToSeg < bestScore) {
                        bestScore = distToSeg
                        bestRoad = road
                        bestIndex = i
                    }
                }
            }
        }

        if (bestRoad == null) return null

        // 2. Extender camino hacia adelante (Road Following)
        val projectedPoints = mutableListOf<GeoPoint>()
        projectedPoints.add(currentPoint)

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

            for (cand in candidateRoads) {
                // Verificar si empieza cerca del junction
                val fromStart = cand.points.first().distanceTo(junctionPoint) < 25.0
                val fromEnd = !cand.oneway && cand.points.last().distanceTo(junctionPoint) < 25.0

                if (fromStart || fromEnd) {
                    val nextPt = if (fromStart) cand.points[1] else cand.points[cand.points.size - 2]
                    val nextBearing = junctionPoint.bearingTo(nextPt)
                    val turnAngle = abs(angleDifference(currentBearing, nextBearing))

                    // Prioridad absoluta: Si conserva el mismo nombre de vía
                    val sameName = (currentRoad.name != null && currentRoad.name == cand.name)

                    val score = if (sameName) turnAngle * 0.5 else turnAngle
                    if (turnAngle < 110.0 && score < bestNextAngle) {
                        bestNextAngle = score
                        bestNextRoad = if (fromStart) cand else RoadSegment(
                            cand.points.reversed(), cand.name, cand.ref, cand.kind, cand.oneway,
                            cand.isRoundabout, cand.isBridge, cand.isTunnel
                        )
                    }
                }
            }

            if (bestNextRoad != null) {
                currentRoad = bestNextRoad
                currentIndex = 1
            } else {
                break // Fin de la vía o no hay continuación transitable
            }
        }

        if (projectedPoints.size < 2 || accumulatedDist < 100.0) return null

        // 3. Muestrear elevación del MDT a lo largo de la polilínea proyectada
        val samplePoints = resamplePath(projectedPoints, sampleStepMeters, distanceMeters)
        val elevations = mutableListOf<Float>()

        for (p in samplePoints) {
            val elev = elevationManager.getElevation(p.lat, p.lng)
            elevations.add(elev?.toFloat() ?: -999f)
        }

        // Si faltan algunas cotas puntuales, interpolar
        fixMissingElevations(elevations)

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
