package com.example.altgraph

import android.content.Context
import io.hammerhead.karooext.models.Symbol
import android.location.Location
import android.util.Log
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val distance: Double
)

enum class PoiType { TOWN, WATER, VIEWPOINT, SUMMIT }

data class Poi(val relativeDistance: Double, val icon: String, val name: String, val type: PoiType)

enum class SurfaceType { ASPHALT, GRAVEL, TRAIL }

data class SurfaceSegment(
    val startDistance: Double,
    val endDistance: Double,
    val type: SurfaceType
) {
    val length: Double get() = (endDistance - startDistance).coerceAtLeast(0.0)
}

data class HairpinInfo(
    val distance: Double,
    val isRight: Boolean
)

data class RouteClimb(
    val startDistance: Double,
    val endDistance: Double,
    val length: Double,
    val totalElevation: Double,
    val avgGrade: Double,
    var apm: Double = 0.0,
    var category: String = "",
    val name: String? = null,
    val isCustom: Boolean = false
)

fun List<RouteClimb>.deduplicateOverlappingClimbs(): List<RouteClimb> {
    if (this.size <= 1) return this

    // Si hay puertos personalizados definidos por el usuario, estos tienen PREVALENCIA TOTAL
    // sobre los puertos automáticos que se solapen con ellos o que formen parte de su ascensión.
    val sortedClimbs = this.sortedWith(
        compareByDescending<RouteClimb> { it.isCustom }
            .thenByDescending { it.length }
    )
    val result = mutableListOf<RouteClimb>()

    for (climb in sortedClimbs) {
        val overlaps = result.any { existing ->
            // Caso 1: Si existing es un puerto personalizado (isCustom = true) y climb es automático,
            // cualquier puerto automático que toque, esté contenido o solape con el puerto personalizado queda eliminado.
            if (existing.isCustom && !climb.isCustom) {
                val expandedStart = existing.startDistance - 300.0
                val expandedEnd = existing.endDistance + 300.0
                val overlapStart = maxOf(existing.startDistance, climb.startDistance)
                val overlapEnd = minOf(existing.endDistance, climb.endDistance)
                val overlap = overlapEnd - overlapStart

                // Si solapa en cualquier tramo (> 10m) o sus extremos caen dentro de la zona de subida del puerto personalizado
                if (overlap > 10.0 || (climb.startDistance in expandedStart..expandedEnd) || (climb.endDistance in expandedStart..expandedEnd)) {
                    return@any true
                }
            }

            // Caso 2: Solapamiento estándar entre dos puertos del mismo tipo
            val overlapStart = maxOf(existing.startDistance, climb.startDistance)
            val overlapEnd = minOf(existing.endDistance, climb.endDistance)
            val overlap = overlapEnd - overlapStart
            val minLen = minOf(existing.length, climb.length)

            // Considerar solapado si:
            // 1) La distancia solapada es significativa (> 100m o > 20% del puerto más corto)
            // 2) O ambas cimas están muy cerca (< 300m) y los inicios están cerca (< 500m)
            overlap > 100.0 ||
                (minLen > 0 && overlap / minLen > 0.2) ||
                (Math.abs(existing.endDistance - climb.endDistance) < 300.0 && Math.abs(existing.startDistance - climb.startDistance) < 500.0)
        }

        if (!overlaps) {
            result.add(climb)
        }
    }

    return result.sortedBy { it.startDistance }
}

data class StrategyData(
    val remainingDistance: Double,
    val timeToSummit: Long,
    val avgGrade: Double,
    val nextBlocks: List<Float>,
    val attackAlert: Boolean,
    val blockSizeMeters: Double,
    val totalFatigueGrade: Int,
    val hairpins: List<Double>,
    val hairpinsInfo: List<HairpinInfo> = emptyList(),
    val pois: List<Poi>,
    val curvatureOffsets: List<Float> = emptyList(),
    val riderProgress: Float = 0.0f,
    val windowStartMeters: Double = 0.0,
    val windowStartElevation: Double = 0.0,
    val subBlocks: List<Float> = emptyList(),
    val subBlocksMax: List<Float> = emptyList(),
    val windowLengthMeters: Double = 400.0,
    val subBlockSizeMeters: Double = 50.0,
    val majorBlockSizeMeters: Double = 100.0,
    val profileElevations: List<Float> = emptyList(),
    val activeClimbs: List<RouteClimb> = emptyList(),
    val visibleAvgGrade: Double = 0.0,
    val visibleMaxGrade: Double = 0.0,
    val routeName: String? = null,
    val routeTotalLength: Double = 0.0,
    // PRO Features
    val oasisDistanceToNextCrucible: Double? = null,
    val virtualPacerRelativeDistance: Double? = null,
    val energyBatteryLevel: Double = 100.0,
    val currentPower: Double = 0.0,
    val currentCadence: Double = 0.0,
    // ELITE Features
    val stravaSegmentDistance: Double? = null,
    val stravaPrGhostDistance: Double? = null,
    val windEffectIntensity: Double? = null, // -1 to 1 (-1 headwind, 1 tailwind)
    val routeCoords: List<Pair<Double, Double>> = emptyList(), // Added for Global Isometric GPS
    // Pendientes de los bloques posteriores a subBlocks: reserva a la derecha de la RouteBar (solo la RouteBar la usa)
    val rightBufferBlocks: List<Float> = emptyList(),
    // Tramos de superficie (Asfalto / Grava / Sendero-MTB)
    val surfaceSegments: List<SurfaceSegment> = emptyList()
)

class AltimetriaStrategyCalculator {
    private fun getVisibleRouteCoords(startIdx: Int, points: List<RoutePoint>, rangeMeters: Int): List<Pair<Double, Double>> {
        if (points.isEmpty() || startIdx >= points.size) return emptyList()
        val coords = mutableListOf<Pair<Double, Double>>()
        val startDist = points[startIdx].distance
        val endDist = startDist + rangeMeters
        for (i in startIdx until points.size) {
            val pt = points[i]
            if (pt.distance > endDist) {
                // Add the last point just to close the shape correctly
                if (pt.latitude != 0.0 || pt.longitude != 0.0) {
                    coords.add(Pair(pt.latitude, pt.longitude))
                }
                break
            }
            // Skip points with no valid GPS coordinates (0.0, 0.0) which might occur if decoding fails partially
            if (pt.latitude != 0.0 || pt.longitude != 0.0) {
                coords.add(Pair(pt.latitude, pt.longitude))
            }
        }
        return coords
    }


    var currentLatitude = 0.0
    var currentLongitude = 0.0
    var currentSpeed = 0.0
    var currentElevation = 350.0
    var viewWidth = 480

    var nearestIndex = 0
    var isNavigatingRoute = false
    var activeRouteName: String? = null
    var routePoints: List<RoutePoint> = emptyList()

    // Historial y buffer de altitud barométrica instantánea en tiempo real
    private val liveElevationHistory = mutableListOf<Double>()
    private var lastElevationSample = 350.0
    private var liveDistanceAccumulated = 0.0
    private var lastIntegrationMs = 0L
    private val freeRideHistory = mutableListOf<Pair<Double, Double>>() // Distancia, Elevación
    var instantBarometricGrade = 0.0
    
    // PRO Feature States
    private var smartLookahead: Double = 350.0
    private var virtualPacerDistance: Double = 0.0
    private var energyBatteryLevel: Double = 100.0
    private var currentPower: Double = 0.0
    private var currentCadence: Double = 0.0
    
    // ELITE Feature States
    private var isStravaSegmentActive = false
    private var stravaSegmentStartDist = 0.0
    private var stravaPrGhostDist = 0.0
    private var lastWindCheckTime = 0L

    private fun getFreeRideElevationAt(dist: Double): Double {
        if (freeRideHistory.isEmpty()) return currentElevation
        if (dist <= freeRideHistory.first().first) return freeRideHistory.first().second
        if (dist >= freeRideHistory.last().first) return freeRideHistory.last().second

        var low = 0
        var high = freeRideHistory.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val p = freeRideHistory[mid]
            if (p.first < dist) low = mid + 1
            else if (p.first > dist) high = mid - 1
            else return p.second
        }
        val p1 = freeRideHistory[low - 1]
        val p2 = freeRideHistory[low]
        val t = (dist - p1.first) / (p2.first - p1.first).coerceAtLeast(0.01)
        return p1.second + t * (p2.second - p1.second)
    }

    fun setPower(power: Double) {
        currentPower = power
    }

    fun setCadence(cadence: Double) {
        currentCadence = cadence
    }

    // ── Datos del Climber nativo de Karoo (DISTANCE_TO_TOP, ELEVATION_TO_TOP, …) ──
    // El SDK expone estos valores calculados directamente desde el archivo de ruta (GPX/FIT).
    // Son la fuente de verdad para la altimetría real del climb activo.
    var distanceToTop: Double = 0.0      // metros hasta la cima del climb actual
    var elevationToTop: Double = 0.0     // metros de desnivel hasta la cima
    var distanceFromBottom: Double = 0.0 // metros desde la base del climb
    var elevationFromBottom: Double = 0.0 // metros de desnivel desde la base
    var elevationRemaining: Double = 0.0 // desnivel restante total de la ruta
    
    // Distancia exacta reportada por Karoo en la ruta
    var currentRouteDistance: Double = 0.0

    // Distancia exacta restante reportada por Karoo en la ruta (útil si no hay puntos GPS)
    var fallbackRemainingDistance: Double = 0.0
    var globalRouteOffset: Double = 0.0

    // GPX Elevation Profile Completo (SDK 1.1.7+)
    var routeElevationProfile: List<ElevationPolylineDecoder.ElevationPoint> = emptyList()

    // Caches para evitar decodificación intensiva en cada emisión del SDK
    private var lastRoutePolyline: String = ""
    private var lastElevationPolyline: String = ""
    private var polylineScaleFactor: Double = 1.0

    // Lista de puertos (climbs) de la ruta para marcar Mountain Gates en el 3D
    var routeClimbs: List<RouteClimb> = emptyList()
    private var routeKeyForClimbs: String? = null

    // Curvas de herradura (distancias absolutas detectadas e información de dirección)
    private var absoluteHairpins = mutableListOf<Double>()
    private var absoluteHairpinInfos = mutableListOf<HairpinInfo>()
    private val routePois = mutableListOf<Poi>()
    var routeSurfaceSegments = mutableListOf<SurfaceSegment>()
    
    var currentHeading: Double = 0.0
    var mapZoomLevel: Double? = null
    var manualPanOffsetMeters: Double = 0.0
    var appContext: Context? = null

    fun updateLiveGrade(grade: Double) {
        this.instantBarometricGrade = grade
    }

    /**
     * Actualiza los datos del climb activo recibidos de los streams nativos del SDK:
     * DISTANCE_TO_TOP, ELEVATION_TO_TOP, DISTANCE_FROM_BOTTOM, ELEVATION_FROM_BOTTOM.
     * Estos reflejan la altimetría real del archivo de ruta cargado (GPX/FIT),
     * exactamente como lo hace el módulo Climber de Hammerhead internamente.
     */
    fun updateClimbData(
        distToTop: Double = distanceToTop,
        elevToTop: Double = elevationToTop,
        distFromBottom: Double = distanceFromBottom,
        elevFromBottom: Double = elevationFromBottom,
        elevRemaining: Double = elevationRemaining
    ) {
        var changed = false
        if (distToTop >= 0.0 && this.distanceToTop != distToTop) { this.distanceToTop = distToTop; changed = true }
        if (elevToTop >= 0.0 && this.elevationToTop != elevToTop) { this.elevationToTop = elevToTop; changed = true }
        if (distFromBottom >= 0.0 && this.distanceFromBottom != distFromBottom) { this.distanceFromBottom = distFromBottom; changed = true }
        if (elevFromBottom >= 0.0 && this.elevationFromBottom != elevFromBottom) { this.elevationFromBottom = elevFromBottom; changed = true }
        if (elevRemaining >= 0.0 && this.elevationRemaining != elevRemaining) { this.elevationRemaining = elevRemaining; changed = true }
        
        if (changed) {
            Log.d("AltiCalc", "ClimbData Updated: distToTop=$distanceToTop, elevToTop=$elevationToTop, distFromBottom=$distanceFromBottom, elevFromBottom=$elevationFromBottom, elevRemaining=$elevationRemaining")
        }
    }

    fun updateLiveElevation(elev: Double) {
        if (elev <= 0.0) return

        // Calcular la pendiente barométrica instantánea real (deltaE / deltaD)
        if (lastElevationSample > 0.0 && currentSpeed > 0.1) {
            val deltaE = elev - lastElevationSample
            val deltaD = (currentSpeed * 1.0).coerceAtLeast(0.5) // Medido cada segundo
            val calcGrade = (deltaE / deltaD) * 100.0

            // Suavizado por media móvil exponencial (EMA) para eliminar ruido del altímetro
            instantBarometricGrade = (0.3 * calcGrade) + (0.7 * instantBarometricGrade)
        }
        lastElevationSample = elev
        this.currentElevation = elev

        if (!isNavigatingRoute && routePoints.isEmpty()) {
            if (liveElevationHistory.size < 20) {
                liveElevationHistory.add(elev)
            } else {
                liveElevationHistory.removeAt(0)
                liveElevationHistory.add(elev)
            }
        }

        // En modo ruta: recalibrar el perfil con la altitud barométrica real
        if (isNavigatingRoute && routePoints.isNotEmpty()) {
            recalibrateElevationProfile(elev)
        }
    }

    fun getSubBlockSize(lookaheadDist: Double, customResolution: Int = 0): Double {
        if (customResolution > 0) return customResolution.toDouble()
        return when {
            lookaheadDist <= 500.0 -> 50.0
            lookaheadDist <= 5000.0 -> 100.0
            lookaheadDist <= 20000.0 -> 500.0
            lookaheadDist <= 50000.0 -> 1000.0
            lookaheadDist <= 100000.0 -> 1000.0
            lookaheadDist <= 200000.0 -> 1000.0
            else -> 1000.0
        }
    }

    fun getMajorBlockSize(lookaheadDist: Double): Double {
        return when {
            lookaheadDist <= 500.0 -> 100.0
            lookaheadDist <= 5000.0 -> 500.0
            lookaheadDist <= 20000.0 -> 2000.0
            lookaheadDist <= 50000.0 -> 5000.0
            lookaheadDist <= 100000.0 -> 10000.0
            lookaheadDist <= 200000.0 -> 20000.0
            else -> 20000.0
        }
    }

    fun getElevationAtDistance(dist: Double): Double {
        if (routeElevationProfile.isNotEmpty()) {
            return getTrueElevationAtDistance(dist)
        }
        
        if (routePoints.isEmpty()) return currentElevation
        if (dist <= routePoints.first().distance) return routePoints.first().elevation
        if (dist >= routePoints.last().distance) return routePoints.last().elevation

        var low = 0
        var high = routePoints.size - 1
        while (low <= high) {
            val mid = (low + high).ushr(1)
            val p = routePoints[mid]
            if (p.distance < dist) {
                low = mid + 1
            } else if (p.distance > dist) {
                high = mid - 1
            } else {
                return p.elevation
            }
        }
        val p0 = routePoints[high.coerceIn(0, routePoints.size - 1)]
        val p1 = routePoints[low.coerceIn(0, routePoints.size - 1)]
        val span = (p1.distance - p0.distance).coerceAtLeast(0.01)
        val frac = ((dist - p0.distance) / span).coerceIn(0.0, 1.0)
        return p0.elevation + frac * (p1.elevation - p0.elevation)
    }

    fun updateCurrentLocation(lat: Double, lng: Double) {
        if (lat == 0.0 && lng == 0.0) return
        if (this.currentLatitude != 0.0 && this.currentLongitude != 0.0) {
            val dist = Math.hypot(lat - this.currentLatitude, lng - this.currentLongitude)
            if (dist > 0.00002) { // aprox 2 metros
                this.currentHeading = bearing(this.currentLatitude, this.currentLongitude, lat, lng)
            }
        }
        this.currentLatitude = lat
        this.currentLongitude = lng
        
        if (routePoints.isNotEmpty()) {
            var minEffectiveDist = Double.MAX_VALUE
            var realMinDistance = Double.MAX_VALUE
            
            val isLost = (nearestIndex != 0 && routePoints.isNotEmpty() && Math.hypot(lat - routePoints[nearestIndex].latitude, lng - routePoints[nearestIndex].longitude) > 0.005) // approx 500m
            val searchStart = if (nearestIndex == 0 || isLost) 0 else (nearestIndex - 20).coerceAtLeast(0)
            val searchEnd = if (nearestIndex == 0 || isLost) routePoints.size else (nearestIndex + 500).coerceAtMost(routePoints.size)
            
            var newNearestIdx = nearestIndex
            for (i in searchStart until searchEnd) {
                val pt = routePoints[i]
                val dist = planarDistM(lat to lng, pt.latitude to pt.longitude)
                
                // Penalizar saltos largos en el índice para evitar que en rutas circulares 
                // salte al final de la ruta por un metro de diferencia en el GPS.
                val indexDiff = Math.abs(i - nearestIndex)
                val penalty = if (nearestIndex == 0) 0.0 else (if (indexDiff > 500) 500.0 else (indexDiff * 0.1))
                
                val effectiveDist = dist + penalty
                
                if (effectiveDist < minEffectiveDist) {
                    minEffectiveDist = effectiveDist
                    realMinDistance = dist
                    newNearestIdx = i
                }
            }
            nearestIndex = newNearestIdx
            
            val computedDist = routePoints[nearestIndex].distance
            this.currentRouteDistance = computedDist
        }
    }

    /**
     * [karooRouteDistance] es la longitud TOTAL de la ruta según Karoo (NavigatingRoute.routeDistance),
     * no la distancia restante. Solo sirve para calibrar la polilínea; la posición del ciclista
     * (currentRouteDistance) sale siempre de su proyección sobre la ruta, nunca de este valor.
     */
    fun syncRouteDistance(karooRouteDistance: Double) {
        this.fallbackRemainingDistance = karooRouteDistance

        if (routePoints.isEmpty()) return

        // Calibrar la polilínea (distancias WGS84 comprimidas) a la longitud real de Karoo
        if (polylineScaleFactor == 1.0 && globalRouteOffset == 0.0 && karooRouteDistance > 500.0) {
            val rawPolylineTotal = routePoints.last().distance
            if (rawPolylineTotal > 500.0) {
                val factor = karooRouteDistance / rawPolylineTotal
                if (factor in 0.8..1.2) {
                    polylineScaleFactor = factor
                    routePoints = routePoints.map { it.copy(distance = it.distance * factor) }
                    if (absoluteHairpins.isNotEmpty()) {
                        absoluteHairpins = absoluteHairpins.map { it * factor }.toMutableList()
                    }
                    if (absoluteHairpinInfos.isNotEmpty()) {
                        absoluteHairpinInfos = absoluteHairpinInfos.map { it.copy(distance = it.distance * factor) }.toMutableList()
                    }
                    // El perfil de elevación ya viene en distancia real de Karoo: no se escala
                    if (routeElevationProfile.isNotEmpty()) applyTrueElevationsToRoutePoints()
                    android.util.Log.d("AltiCalc", "Calibrated distances. Factor: $polylineScaleFactor")
                }
            }
        }
    }
    fun setRoutePois(symbols: List<io.hammerhead.karooext.models.Symbol.POI>) {
        if (routePoints.isEmpty() || symbols.isEmpty()) {
            this.routePois.clear()
            return
        }
        routePois.clear()
        for (sym in symbols) {
            var nearestDist = 0.0
            var minDist = Double.MAX_VALUE
            for (pt in routePoints) {
                val d = hypot(pt.latitude - sym.lat, pt.longitude - sym.lng)
                if (d < minDist) {
                    minDist = d
                    nearestDist = pt.distance
                }
            }
            var name = sym.name ?: "Hito"
            // Limpiamos cualquier "XXX m" previo y le añadimos la altitud real
            name = name.replace(Regex("\\b\\d+\\s*m\\b", RegexOption.IGNORE_CASE), "").trim()
            val realElevation = getTrueElevationAtDistance(nearestDist).toInt()
            name = "$name ${realElevation}m"
            val icon = when {
                name.contains("agua", ignoreCase = true) || name.contains("font", ignoreCase = true) || name.contains("fuente", ignoreCase = true) -> "💧"
                name.contains("mirador", ignoreCase = true) || name.contains("vista", ignoreCase = true) -> "📸"
                name.contains("puerto", ignoreCase = true) || name.contains("cima", ignoreCase = true) || name.contains("alto", ignoreCase = true) || name.contains("col", ignoreCase = true) -> "📡"
                else -> "☕"
            }
            val type = when (icon) {
                "💧" -> PoiType.WATER
                "📸" -> PoiType.VIEWPOINT
                "📡" -> PoiType.SUMMIT
                else -> PoiType.TOWN
            }
            routePois.add(Poi(nearestDist, icon, name, type))
        }

        // ELITE: Detectar puertos de montaña definidos manualmente mediante POIs por el usuario
        detectUserDefinedClimbsFromPois(symbols)

        // ELITE: Detectar tramos de superficie (Gravel / Trail / Asfalto)
        detectSurfaceSegmentsFromPois(symbols)
    }

    private fun detectSurfaceSegmentsFromPois(symbols: List<io.hammerhead.karooext.models.Symbol.POI>) {
        routeSurfaceSegments.clear()
        if (routePoints.isEmpty()) return

        // Extraer POIs con etiquetas de superficie
        val surfaceMarkers = mutableListOf<Pair<Double, SurfaceType>>()
        for (sym in symbols) {
            val text = sym.name.orEmpty().lowercase()
            val type = when {
                text.contains("gravel") || text.contains("grava") || text.contains("ripio") || text.contains("sterrato") || text.contains("kies") -> SurfaceType.GRAVEL
                text.contains("trail") || text.contains("sendero") || text.contains("mtb") || text.contains("singletrack") || text.contains("pista") -> SurfaceType.TRAIL
                text.contains("asfalto") || text.contains("asphalt") || text.contains("road") || text.contains("pavé") || text.contains("paved") -> SurfaceType.ASPHALT
                else -> null
            }
            if (type != null) {
                var nearestDist = 0.0
                var minDist = Double.MAX_VALUE
                for (pt in routePoints) {
                    val d = hypot(pt.latitude - sym.lat, pt.longitude - sym.lng)
                    if (d < minDist) {
                        minDist = d
                        nearestDist = pt.distance
                    }
                }
                surfaceMarkers.add(Pair(nearestDist, type))
            }
        }

        surfaceMarkers.sortBy { it.first }
        val totalLen = routePoints.last().distance

        if (surfaceMarkers.isNotEmpty()) {
            for (i in surfaceMarkers.indices) {
                val start = surfaceMarkers[i].first
                val end = if (i + 1 < surfaceMarkers.size) surfaceMarkers[i + 1].first else totalLen
                if (end > start + 30.0) {
                    routeSurfaceSegments.add(SurfaceSegment(start, end, surfaceMarkers[i].second))
                }
            }
        }
    }

    /**
     * Extrae una categoría explícita si el usuario la incluyó en el nombre o descripción del POI.
     * Soporta: [ESP], [HC], [1C], [2C], [3C], [4C], [1ª], [2ª], etc.
     */
    private fun parseCategoryFromName(text: String): String? {
        val upper = text.uppercase()
        val bracketMatch = Regex("\\[(ESP|HC|H\\.C\\.?|1C|2C|3C|4C|1ª|2ª|3ª|4ª|CAT\\s*[1-4]|CAT\\s*ESP)]").find(upper)
        if (bracketMatch != null) {
            val token = bracketMatch.groupValues[1].replace(" ", "").replace(".", "")
            return when {
                token.contains("ESP") || token.contains("HC") -> FatigueGradeCalculator.getClimbCategoryName(250.0)
                token.contains("1") -> "1ª Cat"
                token.contains("2") -> "2ª Cat"
                token.contains("3") -> "3ª Cat"
                token.contains("4") -> "4ª Cat"
                else -> null
            }
        }
        return null
    }

    // Quitar únicamente prefijos técnicos de marcación como "INICIO:", "CIMA:", "FINAL:", "FIN:"
    val technicalPrefixes = listOf(
        "INICIO DE PUERTO:", "INICIO DE PUERTO", "INICIO DEL PUERTO:", "INICIO DEL PUERTO",
        "INICIO PUERTO:", "INICIO PUERTO", "INICIO PTO:", "INICIO PTO", "INICIO:", "INICIO -", "INICIO",
        "START OF CLIMB:", "START CLIMB:", "START:", "START -", "START",
        "COMIENZO DE PUERTO:", "COMIENZO DE PUERTO", "COMIENZO PUERTO:", "COMIENZO PUERTO", "COMIENZO:", "COMIENZO",
        "PIE DE PUERTO:", "PIE DE PUERTO", "PIE:", "PIE",
        "FINAL DE PUERTO:", "FINAL DE PUERTO", "FINAL DEL PUERTO:", "FINAL DEL PUERTO",
        "FINAL PUERTO:", "FINAL PUERTO", "FINAL PTO:", "FINAL PTO", "FINAL:", "FINAL -", "FINAL",
        "FIN DE PUERTO:", "FIN DE PUERTO", "FIN DEL PUERTO:", "FIN DEL PUERTO",
        "FIN PUERTO:", "FIN PUERTO", "FIN PTO:", "FIN PTO", "FIN:", "FIN -", "FIN",
        "END OF CLIMB:", "END CLIMB:", "END:", "END -", "END",
        "CIMA DE PUERTO:", "CIMA DE PUERTO", "CIMA DEL PUERTO:", "CIMA DEL PUERTO",
        "CIMA PUERTO:", "CIMA PUERTO", "CIMA PTO:", "CIMA PTO", "CIMA:", "CIMA -", "CIMA",
        "SUMMIT OF CLIMB:", "SUMMIT:", "SUMMIT -", "SUMMIT",
        "CUMBRE DE PUERTO:", "CUMBRE DE PUERTO", "CUMBRE DEL PUERTO:", "CUMBRE DEL PUERTO",
        "CUMBRE PUERTO:", "CUMBRE PUERTO", "CUMBRE PTO:", "CUMBRE PTO", "CUMBRE:", "CUMBRE -", "CUMBRE"
    ).sortedByDescending { it.length }

    fun cleanClimbName(raw: String): String {
        var n = raw.replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\b\\d+\\s*m\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[☕📡💧📸]"), "")
            .trim()
        for (pfx in technicalPrefixes) {
            if (n.startsWith(pfx, ignoreCase = true)) {
                val candidate = n.substring(pfx.length).trim()
                if (candidate.startsWith(":") || candidate.startsWith("-") || candidate.startsWith("de ") || candidate.startsWith("del ")) {
                    val cleaned = candidate.replaceFirst(Regex("^[:\\-]|^(de\\s+|del\\s+)", RegexOption.IGNORE_CASE), "").trim()
                    if (cleaned.isNotBlank()) n = cleaned
                } else if (candidate.isNotBlank()) {
                    n = candidate
                }
                break
            }
        }
        return n.trim()
    }

    /**
     * Detecta puertos de montaña definidos por el usuario mediante pares de POIs (Inicio y Cima)
     * o POIs individuales de Cima.
     * Si la preferencia eliteCustomClimbsEnabled está activa (por defecto true en ELITE),
     * estos puertos prevalecen sobre cualquier puerto detectado automáticamente.
     */
    fun detectUserDefinedClimbsFromPois(symbols: List<io.hammerhead.karooext.models.Symbol.POI>? = null) {
        val prefs = appContext?.let { AppPreferences.getInstance(it) }
        val isEnabled = prefs?.eliteCustomClimbsEnabled ?: true
        if (!isEnabled || routePoints.isEmpty()) return

        // 1. Extraer POIs con distancia real a lo largo de la ruta
        data class PoiWithDist(
            val name: String,
            var distance: Double,
            var elevation: Double,
            val type: String = "",
            val lat: Double = 0.0,
            val lng: Double = 0.0
        )
        val poisWithDist = mutableListOf<PoiWithDist>()

        fun projectPoiToRoute(
            lat: Double,
            lng: Double,
            minAllowedDist: Double = 0.0,
            maxAllowedDist: Double = Double.MAX_VALUE
        ): Double {
            var bestDist = 0.0
            var bestMetricDist = Double.MAX_VALUE
            for (pt in routePoints) {
                if (pt.distance < minAllowedDist || pt.distance > maxAllowedDist) continue
                val d = hypot(pt.latitude - lat, pt.longitude - lng)
                if (d < bestMetricDist) {
                    bestMetricDist = d
                    bestDist = pt.distance
                }
            }
            return if (bestMetricDist < Double.MAX_VALUE) bestDist else {
                var fallbackDist = 0.0
                var fallbackMin = Double.MAX_VALUE
                for (pt in routePoints) {
                    val d = hypot(pt.latitude - lat, pt.longitude - lng)
                    if (d < fallbackMin) {
                        fallbackMin = d
                        fallbackDist = pt.distance
                    }
                }
                fallbackDist
            }
        }

        if (symbols != null && symbols.isNotEmpty()) {
            for (sym in symbols) {
                val sName = (sym.name?.takeIf { it.isNotBlank() } ?: sym.type)?.trim() ?: continue
                var minDist = Double.MAX_VALUE
                var nearestDist = 0.0
                for (pt in routePoints) {
                    val d = hypot(pt.latitude - sym.lat, pt.longitude - sym.lng)
                    if (d < minDist) {
                        minDist = d
                        nearestDist = pt.distance
                    }
                }
                val elev = getTrueElevationAtDistance(nearestDist)
                poisWithDist.add(PoiWithDist(sName.trim(), nearestDist, elev, sym.type ?: "", sym.lat, sym.lng))
            }
        } else {
            for (poi in routePois) {
                val cleanName = poi.name.replace(Regex("\\b\\d+\\s*m\\b", RegexOption.IGNORE_CASE), "").trim()
                poisWithDist.add(PoiWithDist(cleanName, poi.relativeDistance, getTrueElevationAtDistance(poi.relativeDistance)))
            }
        }

        if (poisWithDist.isEmpty()) return

        val detectedClimbs = mutableListOf<RouteClimb>()

        fun removeDiacritics(str: String): String {
            return str.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace("ü", "u").replace("Ü", "U").replace("ñ", "n").replace("Ñ", "N")
        }

        fun normalizeKey(name: String): String {
            return cleanClimbName(name).lowercase().replace(Regex("[^a-z0-9]"), "")
        }

        fun isStartPoi(name: String, type: String = ""): Boolean {
            val upperType = type.uppercase()
            if (upperType == "START" || upperType == "INICIO") return true
            val s = removeDiacritics(name).uppercase()
            return s.startsWith("INICIO") || s.contains(" INICIO") ||
                   s.startsWith("START") || s.contains(" START") ||
                   s.startsWith("COMIENZO") || s.contains(" COMIENZO") ||
                   s.startsWith("PIE") || s.contains(" PIE") ||
                   s.startsWith("DEBUT") || s.contains(" DEBUT") ||
                   s.startsWith("INIZIO") || s.contains(" INIZIO") ||
                   s.startsWith("ARRANQUE") || s.contains(" ARRANQUE")
        }

        fun isWaterOrFood(name: String): Boolean {
            val s = removeDiacritics(name).uppercase()
            return s.contains("AGUA") || s.contains("WATER") || s.contains("FUENTE") || s.contains("FONT") ||
                   s.contains("CAFE") || s.contains("COFFEE") || s.contains("COMIDA") || s.contains("FOOD")
        }

        fun isEndPoi(name: String, type: String = ""): Boolean {
            if (isStartPoi(name, type) || isWaterOrFood(name)) return false // Un inicio de puerto nunca es una cima
            val upperType = type.uppercase()
            if (upperType == "SUMMIT" || upperType == "CUMBRE" || upperType == "PEAK") return true
            val s = removeDiacritics(name).uppercase()
            return s.startsWith("FIN") || s.contains(" FIN") ||
                   s.startsWith("FINAL") || s.contains(" FINAL") ||
                   s.startsWith("END") || s.contains(" END") ||
                   s.contains("CIMA") || s.contains("CUMBRE") || s.contains("SUMMIT") || s.contains("SOMMET") ||
                   s.contains("PUERTO") || s.contains("PTO") || s.contains("ALTO") || s.contains("PASO") ||
                   s.contains("PASSO") || s.contains("COL ") || s.contains("COLL ") || s.contains("COLLADO") ||
                   s.contains("CORONA") || s.contains("PORT ")
        }

        val startPois = poisWithDist.filter { isStartPoi(it.name, it.type) }
        val endPois = poisWithDist.filter { isEndPoi(it.name, it.type) }

        val sortedStarts = startPois.sortedBy { it.distance }
        val sortedEnds = endPois.sortedBy { it.distance }
        val pairedStarts = mutableSetOf<PoiWithDist>()
        val pairedEnds = mutableSetOf<PoiWithDist>()

        // A) Emparejar INICIO con CIMA / FINAL secuencialmente a lo largo de la ruta
        for (i in sortedStarts.indices) {
            val start = sortedStarts[i]
            val nextStart = sortedStarts.getOrNull(i + 1)
            val startKey = normalizeKey(start.name)

            // Buscar la cima correspondiente:
            // 1) Si startKey no es vacía, buscar coincidencia por nombre en cimas explícitas
            var matchEnd = if (startKey.isNotEmpty()) {
                sortedEnds.firstOrNull { end ->
                    if (end in pairedEnds) return@firstOrNull false
                    // Si por coincidencia de ruta circular start.distance >= end.distance, re-proyectar
                    var effStartDist = start.distance
                    var effEndDist = end.distance
                    if (effEndDist <= effStartDist + 200.0 && (start.lat != 0.0 || end.lat != 0.0)) {
                        val altStartDist = if (start.lat != 0.0) projectPoiToRoute(start.lat, start.lng, 0.0, effEndDist - 200.0) else effStartDist
                        val altEndDist = if (end.lat != 0.0) projectPoiToRoute(end.lat, end.lng, effStartDist + 200.0, Double.MAX_VALUE) else effEndDist
                        if (altEndDist > altStartDist + 200.0) {
                            start.distance = altStartDist
                            start.elevation = getTrueElevationAtDistance(altStartDist)
                            end.distance = altEndDist
                            end.elevation = getTrueElevationAtDistance(altEndDist)
                            effStartDist = altStartDist
                            effEndDist = altEndDist
                        }
                    }
                    if (effEndDist <= effStartDist + 200.0) return@firstOrNull false
                    val endKey = normalizeKey(end.name)
                    endKey.isNotEmpty() && (startKey == endKey || startKey.contains(endKey) || endKey.contains(startKey))
                }
            } else null

            // 2) Buscar la primera cima libre antes del siguiente inicio
            if (matchEnd == null) {
                matchEnd = sortedEnds.firstOrNull { end ->
                    if (end in pairedEnds || end.distance <= start.distance + 200.0) return@firstOrNull false
                    nextStart == null || end.distance < nextStart.distance
                }
            }

            // 3) Si no se marcó una cima explícita con palabras clave (ej: el usuario llamó al POI de la cima
            // simplemente "Busot", "Tudons", etc.), tomar cualquier POI posterior antes del siguiente inicio
            if (matchEnd == null) {
                matchEnd = poisWithDist.firstOrNull { poi ->
                    poi !in pairedEnds && poi.distance > start.distance + 400.0 &&
                    (nextStart == null || poi.distance < nextStart.distance) &&
                    !isStartPoi(poi.name, poi.type) && !isWaterOrFood(poi.name)
                }
            }

            if (matchEnd != null) {
                pairedStarts.add(start)
                pairedEnds.add(matchEnd)
                val len = matchEnd.distance - start.distance
                val elevGain = (matchEnd.elevation - start.elevation).coerceAtLeast(0.0)
                val avgGrade = if (len > 0) (elevGain / len) * 100.0 else 0.0

                var displayName = cleanClimbName(matchEnd.name)
                val isGenericEndName = displayName.isBlank() ||
                    displayName.equals("Puerto", ignoreCase = true) ||
                    displayName.equals("Cima", ignoreCase = true) ||
                    displayName.equals("Cumbre", ignoreCase = true) ||
                    displayName.equals("Fin", ignoreCase = true) ||
                    displayName.equals("Final", ignoreCase = true) ||
                    displayName.equals("End", ignoreCase = true)

                if (isGenericEndName) {
                    val sName = cleanClimbName(start.name)
                    if (sName.isNotBlank() && !sName.equals("Inicio", ignoreCase = true) && !sName.equals("Start", ignoreCase = true)) {
                        displayName = sName
                    }
                }
                if (displayName.isBlank() || isGenericEndName && displayName.equals("Puerto", ignoreCase = true)) {
                    displayName = "Puerto ${detectedClimbs.size + 1}"
                }

                val forcedCategory = parseCategoryFromName(matchEnd.name) ?: parseCategoryFromName(start.name)

                var climb = RouteClimb(
                    startDistance = start.distance,
                    endDistance = matchEnd.distance,
                    length = len,
                    totalElevation = elevGain,
                    avgGrade = avgGrade,
                    name = displayName,
                    isCustom = true
                )
                climb = calculateClimbCategory(climb)
                if (forcedCategory != null) {
                    climb.category = forcedCategory
                }
                detectedClimbs.add(climb)
            }
        }

        // B) Si una CIMA no tenía INICIO explícito pero tiene formato de puerto ("CIMA: Morcuera", "PUERTO: ...", "Pto de Busot", etc.)
        for (end in endPois) {
            if (end in pairedEnds) continue
            val hasExplicitPrefix = technicalPrefixes.any { end.name.startsWith(it, ignoreCase = true) }
            val hasClimbIndicator = hasExplicitPrefix ||
                end.name.contains("puerto", ignoreCase = true) ||
                end.name.contains("pto", ignoreCase = true) ||
                end.name.contains("cima", ignoreCase = true) ||
                end.name.contains("cumbre", ignoreCase = true) ||
                end.name.contains("passo", ignoreCase = true) ||
                end.name.contains("alto de", ignoreCase = true) ||
                end.name.contains("col de", ignoreCase = true) ||
                end.name.contains("col du", ignoreCase = true)

            if (hasClimbIndicator && end.distance > 500.0) {
                // Retroceder en el perfil de elevación o ruta para encontrar el pie de puerto natural
                var bestStartDist = (end.distance - 1000.0).coerceAtLeast(0.0)
                var minElevSeen = end.elevation
                val maxLookback = 35000.0 // Hasta 35 km atrás

                // 1) Si ya tenemos climbs calculados/proporcionados por Karoo que culminan cerca de esta cima,
                // tomamos el inicio del primer tramo continuo que asciende a esta cima para unificarlo por completo.
                val existingClimbsLeadingToSummit = this.routeClimbs.filter {
                    it.endDistance in (end.distance - 800.0)..(end.distance + 800.0) ||
                    (it.startDistance < end.distance && it.endDistance >= end.distance - 200.0)
                }
                if (existingClimbsLeadingToSummit.isNotEmpty()) {
                    val earliestStart = existingClimbsLeadingToSummit.minOf { it.startDistance }
                    if (earliestStart < end.distance - 500.0) {
                        bestStartDist = earliestStart
                        minElevSeen = getTrueElevationAtDistance(bestStartDist)
                    }
                }

                // 2) Si hay perfil de elevación detallado, buscar el valle / pie de puerto real
                if (routeElevationProfile.isNotEmpty()) {
                    val pts = routeElevationProfile.filter { it.distance in (end.distance - maxLookback)..end.distance }.sortedByDescending { it.distance }
                    var profileMinElev = end.elevation
                    var profileBestDist = bestStartDist
                    for (pt in pts) {
                        if (pt.elevation < profileMinElev) {
                            profileMinElev = pt.elevation
                            profileBestDist = pt.distance
                        } else if (pt.elevation > profileMinElev + 35.0) {
                            // Subió más de 35m antes de empezar: hemos llegado al valle anterior
                            break
                        }
                    }
                    if (end.distance - profileBestDist >= 500.0 && profileBestDist <= bestStartDist) {
                        bestStartDist = profileBestDist
                        minElevSeen = profileMinElev
                    }
                } else if (routePoints.isNotEmpty() && existingClimbsLeadingToSummit.isEmpty()) {
                    val pts = routePoints.filter { it.distance in (end.distance - maxLookback)..end.distance }.sortedByDescending { it.distance }
                    var routeMinElev = end.elevation
                    var routeBestDist = bestStartDist
                    for (pt in pts) {
                        if (pt.elevation < routeMinElev) {
                            routeMinElev = pt.elevation
                            routeBestDist = pt.distance
                        } else if (pt.elevation > routeMinElev + 35.0) {
                            break
                        }
                    }
                    if (end.distance - routeBestDist >= 500.0) {
                        bestStartDist = routeBestDist
                        minElevSeen = routeMinElev
                    }
                }

                val len = end.distance - bestStartDist
                if (len >= 500.0) {
                    val elevGain = (end.elevation - minElevSeen).coerceAtLeast(0.0)
                    val avgGrade = if (len > 0) (elevGain / len) * 100.0 else 0.0

                    var displayName = cleanClimbName(end.name)
                    if (displayName.isBlank()) {
                        displayName = end.name.replace(Regex("\\[.*?\\]"), "").replace(Regex("\\b\\d+\\s*m\\b", RegexOption.IGNORE_CASE), "").trim()
                    }
                    if (displayName.isBlank()) {
                        displayName = "Puerto"
                    }

                    val forcedCategory = parseCategoryFromName(end.name)

                    var climb = RouteClimb(
                        startDistance = bestStartDist,
                        endDistance = end.distance,
                        length = len,
                        totalElevation = elevGain,
                        avgGrade = avgGrade,
                        name = if (displayName.isNotBlank()) displayName else null,
                        isCustom = true
                    )
                    climb = calculateClimbCategory(climb)
                    if (forcedCategory != null) {
                        climb.category = forcedCategory
                    }
                    detectedClimbs.add(climb)
                }
            }
        }

        // C) Si un INICIO no tenía CIMA explícita emparejada (ej. el usuario solo marcó INICIO, o el pin de CIMA
        // se guardó como Pin Global en vez de PDI de ruta y Karoo no lo envió en la lista de pois de la ruta)
        for (start in sortedStarts) {
            if (start in pairedStarts) continue
            val hasExplicitPrefix = technicalPrefixes.any { start.name.startsWith(it, ignoreCase = true) }
            val hasClimbIndicator = hasExplicitPrefix ||
                start.name.contains("puerto", ignoreCase = true) ||
                start.name.contains("pto", ignoreCase = true) ||
                start.name.contains("subida", ignoreCase = true) ||
                start.name.contains("col", ignoreCase = true) ||
                isStartPoi(start.name, start.type)

            if (hasClimbIndicator) {
                // Avanzar en el perfil de elevación o ruta para encontrar la cima natural
                var bestEndDist = (start.distance + 1500.0).coerceAtMost(routePoints.lastOrNull()?.distance ?: (start.distance + 1500.0))
                var maxElevSeen = start.elevation
                val maxLookahead = 35000.0 // Hasta 35 km adelante

                // 1) Si ya tenemos climbs calculados/proporcionados por Karoo que comienzan cerca de este inicio,
                // tomamos el final del tramo continuo que asciende desde este inicio.
                val existingClimbsStartingNear = this.routeClimbs.filter {
                    it.startDistance in (start.distance - 800.0)..(start.distance + 800.0) ||
                    (it.startDistance >= start.distance - 200.0 && it.endDistance > start.distance + 500.0)
                }
                if (existingClimbsStartingNear.isNotEmpty()) {
                    val latestEnd = existingClimbsStartingNear.maxOf { it.endDistance }
                    if (latestEnd > start.distance + 500.0) {
                        bestEndDist = latestEnd
                        maxElevSeen = getTrueElevationAtDistance(bestEndDist)
                    }
                }

                // 2) Si hay perfil de elevación detallado, buscar la cima real (pico más alto antes de un descenso significativo)
                if (routeElevationProfile.isNotEmpty()) {
                    val pts = routeElevationProfile.filter { it.distance in start.distance..(start.distance + maxLookahead) }.sortedBy { it.distance }
                    var profileMaxElev = start.elevation
                    var profileBestDist = bestEndDist
                    for (pt in pts) {
                        if (pt.elevation > profileMaxElev) {
                            profileMaxElev = pt.elevation
                            profileBestDist = pt.distance
                        } else if (pt.elevation < profileMaxElev - 35.0) {
                            // Descendió más de 35m tras coronar: hemos pasado la cima y empezado la bajada
                            break
                        }
                    }
                    if (profileBestDist - start.distance >= 500.0) {
                        bestEndDist = profileBestDist
                        maxElevSeen = profileMaxElev
                    }
                } else if (routePoints.isNotEmpty() && existingClimbsStartingNear.isEmpty()) {
                    val pts = routePoints.filter { it.distance in start.distance..(start.distance + maxLookahead) }.sortedBy { it.distance }
                    var routeMaxElev = start.elevation
                    var routeBestDist = bestEndDist
                    for (pt in pts) {
                        if (pt.elevation > routeMaxElev) {
                            routeMaxElev = pt.elevation
                            routeBestDist = pt.distance
                        } else if (pt.elevation < routeMaxElev - 35.0) {
                            break
                        }
                    }
                    if (routeBestDist - start.distance >= 500.0) {
                        bestEndDist = routeBestDist
                        maxElevSeen = routeMaxElev
                    }
                }

                val len = bestEndDist - start.distance
                if (len >= 500.0) {
                    val elevGain = (maxElevSeen - start.elevation).coerceAtLeast(0.0)
                    val avgGrade = if (len > 0) (elevGain / len) * 100.0 else 0.0

                    var displayName = cleanClimbName(start.name)
                    if (displayName.isBlank() || displayName.equals("Inicio", ignoreCase = true) || displayName.equals("Start", ignoreCase = true)) {
                        displayName = "Puerto ${detectedClimbs.size + 1}"
                    }

                    val forcedCategory = parseCategoryFromName(start.name)

                    var climb = RouteClimb(
                        startDistance = start.distance,
                        endDistance = bestEndDist,
                        length = len,
                        totalElevation = elevGain,
                        avgGrade = avgGrade,
                        name = displayName,
                        isCustom = true
                    )
                    climb = calculateClimbCategory(climb)
                    if (forcedCategory != null) {
                        climb.category = forcedCategory
                    }
                    detectedClimbs.add(climb)
                }
            }
        }

        if (detectedClimbs.isNotEmpty()) {
            // Si el usuario ha definido puertos propios por POIs, éstos tienen preferencia absoluta.
            // No dejamos que los automáticos vuelvan a poblar la ruta si el usuario ha simplificado a sus propios puertos.
            this.routeClimbs = detectedClimbs.sortedBy { it.startDistance }
            Log.d("AltiCalc", "ELITE: Aplicados exclusivamente ${detectedClimbs.size} puertos definidos por POIs de usuario.")

            // Guardar de forma persistente en almacenamiento de Ruta y Catálogo Global (GPS)
            appContext?.let { ctx ->
                val currentRouteKey = activeRouteName ?: routeKeyForClimbs
                val storedList = detectedClimbs.mapNotNull { climb ->
                    val startPt = routePoints.minByOrNull { Math.abs(it.distance - climb.startDistance) }
                    val endPt = routePoints.minByOrNull { Math.abs(it.distance - climb.endDistance) }
                    if (startPt != null && endPt != null && (startPt.latitude != 0.0 || startPt.longitude != 0.0)) {
                        val climbName = climb.name ?: "Puerto"
                        val climbId = "${climbName.lowercase().replace(" ", "_")}_${String.format("%.4f", startPt.latitude)}_${String.format("%.4f", endPt.latitude)}"
                        StoredClimb(
                            id = climbId,
                            name = climbName,
                            category = climb.category,
                            startLat = startPt.latitude,
                            startLng = startPt.longitude,
                            startElev = startPt.elevation,
                            endLat = endPt.latitude,
                            endLng = endPt.longitude,
                            endElev = endPt.elevation,
                            lengthMeters = climb.length,
                            avgGrade = climb.avgGrade,
                            routeKey = currentRouteKey
                        )
                    } else null
                }
                if (storedList.isNotEmpty()) {
                    if (!currentRouteKey.isNullOrBlank()) {
                        CustomClimbStorage.saveRouteClimbs(ctx, currentRouteKey, storedList)
                    }
                    CustomClimbStorage.saveGlobalClimbs(ctx, storedList)
                }
            }
        }
    }

    /**
     * Comprueba si hay puertos de montaña personalizados guardados (por ruta o en el catálogo global GPS)
     * y los aplica a la ruta actual.
     */
    fun restoreSavedClimbsForRoute() {
        val ctx = appContext ?: return
        val prefs = AppPreferences.getInstance(ctx)
        if (!prefs.eliteCustomClimbsEnabled || routePoints.isEmpty()) return

        val currentRouteKey = activeRouteName ?: routeKeyForClimbs
        val matchedClimbs = CustomClimbStorage.matchClimbsForRoute(ctx, currentRouteKey, routePoints)
        if (matchedClimbs.isNotEmpty()) {
            val categorized = matchedClimbs.map { calculateClimbCategory(it) }
            val existingNonCustom = this.routeClimbs.filter { !it.isCustom }
            val existingCustom = this.routeClimbs.filter { it.isCustom }
            this.routeClimbs = (categorized + existingCustom + existingNonCustom).deduplicateOverlappingClimbs()
            Log.d("AltiCalc", "Restaurados ${matchedClimbs.size} puertos guardados (Ruta/Global GPS). Total activos: ${this.routeClimbs.size}")
        }
    }

    /**
     * Sincroniza la lista de climbs. Como Karoo puede borrar los climbs superados de la lista,
     * hacemos un merge para mantenerlos (y poder verlos al hacer re-ride).
     * Además, cataloga los puertos calculando su APM.
     * Los puertos definidos por el usuario (isCustom = true) prevalecen sobre los incoming de Karoo.
     */
    fun syncRouteClimbs(routeKey: String, incoming: List<RouteClimb>) {
        val existingCustom = routeClimbs.filter { it.isCustom }
        if (existingCustom.isNotEmpty()) {
            // Si el usuario ha definido puertos propios (manualmente por POIs o guardados),
            // estos tienen PREVALENCIA Y EXCLUSIVIDAD ABSOLUTA. Descartamos por completo
            // los puertos automáticos del Climber de Karoo.
            routeClimbs = existingCustom.sortedBy { it.startDistance }
            return
        }

        if (routeKeyForClimbs != routeKey) {
            routeKeyForClimbs = routeKey
            if (incoming.isNotEmpty()) {
                val validIncoming = incoming.filter { climb ->
                    (climb.totalElevation >= 50.0 || (climb.length >= 800.0 && climb.avgGrade >= 6.0)) && climb.length >= 600.0
                }
                val categorized = validIncoming.map { calculateClimbCategory(it) }.filter { it.category.isNotBlank() }
                // Asignar nombres de cimas o inicios POI si existen en la ruta
                val named = categorized.map { climb ->
                    if (climb.name.isNullOrBlank()) {
                        val summitPoi = routePois.filter { poi ->
                            val s = poi.name.uppercase()
                            !s.contains("AGUA") && !s.contains("WATER") && !s.contains("FUENTE") && !s.contains("FONT") &&
                            !s.contains("CAFE") && !s.contains("COFFEE") && !s.contains("COMIDA") && !s.contains("FOOD") &&
                            !s.startsWith("INICIO") && !s.contains(" INICIO") &&
                            poi.relativeDistance in (climb.startDistance + climb.length * 0.4)..(climb.endDistance + 2500.0)
                        }.minByOrNull { Math.abs(it.relativeDistance - climb.endDistance) }

                        val startPoi = if (summitPoi == null) {
                            routePois.filter { poi ->
                                val s = poi.name.uppercase()
                                !s.contains("AGUA") && !s.contains("WATER") && !s.contains("FUENTE") && !s.contains("FONT") &&
                                !s.contains("CAFE") && !s.contains("COFFEE") && !s.contains("COMIDA") && !s.contains("FOOD") &&
                                poi.relativeDistance in (climb.startDistance - 1500.0)..(climb.startDistance + climb.length * 0.5)
                            }.minByOrNull { Math.abs(it.relativeDistance - climb.startDistance) }
                        } else null

                        val namePoi = summitPoi ?: startPoi
                        if (namePoi != null) {
                            climb.copy(name = cleanClimbName(namePoi.name))
                        } else climb
                    } else climb
                }
                routeClimbs = named.deduplicateOverlappingClimbs()
                Log.d("AltiCalc", "syncRouteClimbs: Nueva ruta '$routeKey'. Inicializados ${routeClimbs.size} puertos categorizados.")
            }
        } else if (incoming.isNotEmpty()) {
            val validIncoming = incoming.filter { climb ->
                (climb.totalElevation >= 50.0 || (climb.length >= 800.0 && climb.avgGrade >= 6.0)) && climb.length >= 600.0
            }
            val categorizedIncoming = validIncoming.map { calculateClimbCategory(it) }.filter { it.category.isNotBlank() }
            val named = categorizedIncoming.map { climb ->
                if (climb.name.isNullOrBlank()) {
                    val summitPoi = routePois.filter { poi ->
                        val s = poi.name.uppercase()
                        !s.contains("AGUA") && !s.contains("WATER") && !s.contains("FUENTE") && !s.contains("FONT") &&
                        !s.contains("CAFE") && !s.contains("COFFEE") && !s.contains("COMIDA") && !s.contains("FOOD") &&
                        !s.startsWith("INICIO") && !s.contains(" INICIO") &&
                        poi.relativeDistance in (climb.startDistance + climb.length * 0.4)..(climb.endDistance + 2500.0)
                    }.minByOrNull { Math.abs(it.relativeDistance - climb.endDistance) }

                    val startPoi = if (summitPoi == null) {
                        routePois.filter { poi ->
                            val s = poi.name.uppercase()
                            !s.contains("AGUA") && !s.contains("WATER") && !s.contains("FUENTE") && !s.contains("FONT") &&
                            !s.contains("CAFE") && !s.contains("COFFEE") && !s.contains("COMIDA") && !s.contains("FOOD") &&
                            poi.relativeDistance in (climb.startDistance - 1500.0)..(climb.startDistance + climb.length * 0.5)
                        }.minByOrNull { Math.abs(it.relativeDistance - climb.startDistance) }
                    } else null

                    val namePoi = summitPoi ?: startPoi
                    if (namePoi != null) {
                        climb.copy(name = cleanClimbName(namePoi.name))
                    } else climb
                } else climb
            }
            routeClimbs = (routeClimbs + named).deduplicateOverlappingClimbs()
            Log.d("AltiCalc", "syncRouteClimbs: Actualizados ${routeClimbs.size} puertos en '$routeKey'.")
        }

        // Si tenemos POIs de usuario presentes, re-evaluamos para garantizar que absorban y sustituyan
        // cualquier puerto automático de Karoo que se haya recibido posteriormente
        if (routePois.isNotEmpty()) {
            detectUserDefinedClimbsFromPois()
        }
    }

    private fun calculateClimbCategory(climb: RouteClimb): RouteClimb {
        var accumulatedHardness = 0.0
        var maxRamp100m = climb.avgGrade
        var maxGrade500m = climb.avgGrade
        var maxGrade1000m = climb.avgGrade
        var metersOver8Pct = 0.0
        var metersOver10Pct = 0.0
        val defaultAsphaltFactor = 0.5 // Standard TA
        val climbLen = climb.length.coerceAtLeast(100.0)

        val hasProfile = routeElevationProfile.isNotEmpty() || routePoints.isNotEmpty()
        if (hasProfile) {
            val step = 100.0 // Tramos de 100m para medir dureza acumulada con máxima precisión
            var curDist = climb.startDistance
            while (curDist < climb.endDistance) {
                val nextDist = minOf(curDist + step, climb.endDistance)
                val dDist = nextDist - curDist
                if (dDist >= 30.0) {
                    val e1 = getTrueElevationAtDistance(curDist)
                    val e2 = getTrueElevationAtDistance(nextDist)
                    val rawGrade = ((e2 - e1) / dDist) * 100.0
                    val segGrade = rawGrade.coerceIn(0.0, 30.0)
                    accumulatedHardness += FatigueGradeCalculator.calculateSegmentHardness(segGrade, dDist / 1000.0)
                    if (segGrade >= 8.0) metersOver8Pct += dDist
                    if (segGrade >= 10.0) metersOver10Pct += dDist
                    if (rawGrade > maxRamp100m) {
                        maxRamp100m = rawGrade.coerceAtMost(32.0)
                    }
                }
                curDist = nextDist
            }

            // Ventanas móviles de 500m y 1000m para detectar tramos duros continuados
            var testDist = climb.startDistance
            while (testDist + 500.0 <= climb.endDistance) {
                val e1 = getTrueElevationAtDistance(testDist)
                val e2 = getTrueElevationAtDistance(testDist + 500.0)
                val g500 = ((e2 - e1) / 500.0) * 100.0
                if (g500 > maxGrade500m) maxGrade500m = g500.coerceAtMost(30.0)
                testDist += 100.0
            }

            testDist = climb.startDistance
            while (testDist + 1000.0 <= climb.endDistance) {
                val e1 = getTrueElevationAtDistance(testDist)
                val e2 = getTrueElevationAtDistance(testDist + 1000.0)
                val g1000 = ((e2 - e1) / 1000.0) * 100.0
                if (g1000 > maxGrade1000m) maxGrade1000m = g1000.coerceAtMost(28.0)
                testDist += 100.0
            }
        } else {
            accumulatedHardness = FatigueGradeCalculator.calculateSegmentHardness(climb.avgGrade.coerceAtLeast(0.0), climbLen / 1000.0)
            maxRamp100m = (climb.avgGrade * 1.3).coerceAtLeast(climb.avgGrade)
            maxGrade500m = (climb.avgGrade * 1.15).coerceAtLeast(climb.avgGrade)
            maxGrade1000m = climb.avgGrade
        }

        // Concentración de dureza y rampas sostenidas (criterio oficial Vuelta a España / UCI / APM):
        // En puertos como 6.7 km al 5.8% con 1 km al 9% y 500m al 9%, la fatiga fisiológica real
        // es notablemente superior a una subida regular continua.
        var sustainedSteepBonus = 0.0
        if (maxGrade1000m >= 7.5) {
            sustainedSteepBonus += (maxGrade1000m - 7.0) * 4.0
        }
        if (maxGrade500m >= 8.5) {
            sustainedSteepBonus += (maxGrade500m - 8.0) * 3.0
        }
        if (metersOver8Pct >= 500.0) {
            sustainedSteepBonus += (metersOver8Pct / 1000.0) * 3.5
        }
        if (metersOver10Pct >= 300.0) {
            sustainedSteepBonus += (metersOver10Pct / 1000.0) * 5.0
        }

        val apm = FatigueGradeCalculator.calculateTotalFatigueGrade(
            accumulatedHardness + sustainedSteepBonus,
            defaultAsphaltFactor,
            maxRamp100m
        )

        // Criterios de puntuabilidad:
        // Repechos breves con desnivel < 50m y pendientes moderadas NO son puertos puntuables
        val isNonCategorized = (climb.totalElevation < 50.0 && climb.avgGrade < 8.0 && maxGrade500m < 9.0) ||
                               (climbLen < 800.0 && climb.totalElevation < 50.0) ||
                               apm < 18.0

        val category = if (isNonCategorized && !climb.isCustom) {
            ""
        } else if (climb.category.isNotBlank() && climb.isCustom) {
            climb.category
        } else {
            val baseCat = FatigueGradeCalculator.getClimbCategoryName(apm)
            val hasHardSustainedRamps = (maxGrade1000m >= 8.5 || maxGrade500m >= 9.0 || (maxRamp100m >= 9.0 && metersOver8Pct >= 800.0))

            // A) Si un puerto tiene longitud >= 5 km y tramos duros sostenidos (ej. 6.7 km al 5.8% con 1km al 9% y 500m al 9%):
            // Clasificado como 2ª Categoría
            if (baseCat == "3ª Cat" && climbLen >= 5000.0 && climb.totalElevation >= 320.0 && hasHardSustainedRamps) {
                "2ª Cat"
            }
            // B) Puerto largo tendido (ej: 11.9 km al 3.1%, desnivel ~370m, sin rampas duras continuadas):
            // Clasificado como 3ª Categoría (nunca 2ª Cat)
            else if (baseCat == "2ª Cat" && (climb.avgGrade < 4.2 && !hasHardSustainedRamps && climb.totalElevation < 500.0)) {
                "3ª Cat"
            }
            // C) Subida larga de pendiente tendida con desnivel considerable (ej: 8-12 km al 3% con +350m):
            // Pasa de 4ª a 3ª Categoría
            else if (baseCat == "4ª Cat" && climbLen >= 7000.0 && climb.totalElevation >= 280.0) {
                "3ª Cat"
            } else {
                baseCat
            }
        }

        return climb.copy(apm = apm, category = category)
    }

    fun setRouteElevationProfile(encoded: String?, routeDistance: Double? = null) {
        if (routeCropped && routeElevationProfile.isNotEmpty()) return
        val safeEncoded = encoded ?: ""
        if (safeEncoded == lastElevationPolyline) return
        lastElevationPolyline = safeEncoded
        
        Log.e("AltiCalc", "setRouteElevationProfile called, encoded length=${encoded?.length}")
        
        // Use true route distance from system if available! Otherwise fallback to geometric length.
        val expectedLength = routeDistance ?: ((routePoints.lastOrNull()?.distance ?: 0.0) - globalRouteOffset)
        
        val result = ElevationPolylineDecoder.decodeSafe(encoded, expectedLength.coerceAtLeast(0.0))
        if (result is ElevationPolylineDecoder.DecodeResult.Success) {
            var pts = ElevationPolylineDecoder.smooth(result.points)
            if (globalRouteOffset > 0.0) {
                pts = pts.map { it.copy(distance = it.distance + globalRouteOffset) }
            }
            this.routeElevationProfile = pts
            Log.e("AltiCalc", "Route elevation decoded successfully, points=${routeElevationProfile.size}")
            applyTrueElevationsToRoutePoints()
            detectCustomClimbsFromProfile()
        } else {
            Log.e("AltiCalc", "Route elevation decode failed!")
            this.routeElevationProfile = emptyList()
        }
    }

    private fun detectCustomClimbsFromProfile() {
        if (routeElevationProfile.isEmpty()) return
        
        val customClimbs = mutableListOf<RouteClimb>()
        
        val minClimbDistance = 500.0
        val minClimbElevation = 25.0
        
        var climbStartIndex = -1
        var localMaxIndex = -1
        
        var i = 0
        while (i < routeElevationProfile.size - 1) {
            val pt = routeElevationProfile[i]
            
            if (climbStartIndex == -1) {
                // Look for a solid start (next 200m averages >= 2.5%)
                val lookaheadDist = 200.0
                var j = i + 1
                while (j < routeElevationProfile.size && routeElevationProfile[j].distance - pt.distance < lookaheadDist) {
                    j++
                }
                if (j < routeElevationProfile.size) {
                    val endPt = routeElevationProfile[j]
                    val dE = endPt.elevation - pt.elevation
                    val dD = endPt.distance - pt.distance
                    val grade = if (dD > 0) (dE / dD) * 100.0 else 0.0
                    
                    if (grade >= 2.5) {
                        climbStartIndex = i
                        localMaxIndex = i
                    } else {
                        // Skip forward to avoid micro-checking flats
                        i = (i + 5).coerceAtMost(routeElevationProfile.size - 2)
                        continue
                    }
                } else {
                    break
                }
            } else {
                if (pt.elevation > routeElevationProfile[localMaxIndex].elevation) {
                    localMaxIndex = i
                }
                
                val drop = routeElevationProfile[localMaxIndex].elevation - pt.elevation
                val distanceSinceMax = pt.distance - routeElevationProfile[localMaxIndex].distance
                val isFlat = distanceSinceMax > 500.0 // 500m without establishing a new peak
                val isEndOfRoute = i == routeElevationProfile.size - 2
                
                if (drop > 20.0 || isFlat || isEndOfRoute) {
                    val startPt = routeElevationProfile[climbStartIndex]
                    val maxPt = routeElevationProfile[localMaxIndex]
                    
                    val climbDist = maxPt.distance - startPt.distance
                    val climbElev = maxPt.elevation - startPt.elevation
                    
                    if (climbDist >= 500.0) {
                        val avgGrade = (climbElev / climbDist) * 100.0
                        
                        val potentialClimb = RouteClimb(
                            startDistance = startPt.distance,
                            endDistance = maxPt.distance,
                            length = climbDist,
                            totalElevation = climbElev,
                            avgGrade = avgGrade
                        )
                        val categorizedClimb = calculateClimbCategory(potentialClimb)
                        
                        val isStandardClimb = climbDist >= 3000.0 && avgGrade >= 3.0
                        val isMuroClimb = climbDist >= 500.0 && avgGrade >= 10.0
                        val isVueltaCategoria = categorizedClimb.apm >= 20.0
                        
                        if (isStandardClimb || isMuroClimb || isVueltaCategoria) {
                            customClimbs.add(categorizedClimb)
                        }
                    }
                    
                    i = localMaxIndex
                    climbStartIndex = -1
                    localMaxIndex = -1
                }
            }
            i++
        }
        
        if (customClimbs.isNotEmpty()) {
            val existingCustom = this.routeClimbs.filter { it.isCustom }
            if (existingCustom.isNotEmpty()) {
                this.routeClimbs = (existingCustom + customClimbs).deduplicateOverlappingClimbs()
            } else if (this.routeClimbs.isEmpty()) {
                this.routeClimbs = customClimbs.deduplicateOverlappingClimbs()
                Log.e("AltiCalc", "Detected ${this.routeClimbs.size} custom climbs!")
            }
        }
        // Re-evaluar puertos definidos por POIs ahora que tenemos el perfil de elevación real
        detectUserDefinedClimbsFromPois()
        // Restaurar puertos guardados persistentes (Ruta y Global GPS)
        restoreSavedClimbsForRoute()
    }

    private fun applyTrueElevationsToRoutePoints() {
        if (routePoints.isEmpty() || routeElevationProfile.isEmpty()) return

        val newPoints = routePoints.map { pt ->
            // Interpolate true elevation from routeElevationProfile based on distance
            val trueElev = getTrueElevationAtDistance(pt.distance)
            pt.copy(elevation = trueElev)
        }
        this.routePoints = newPoints
    }

    private fun getTrueElevationAtDistance(dist: Double): Double {
        if (routeElevationProfile.isNotEmpty()) {
            if (dist <= routeElevationProfile.first().distance) return routeElevationProfile.first().elevation
            if (dist >= routeElevationProfile.last().distance) return routeElevationProfile.last().elevation

            var low = 0
            var high = routeElevationProfile.size - 1

            while (low <= high) {
                val mid = (low + high) ushr 1
                val midVal = routeElevationProfile[mid].distance
                if (midVal < dist) low = mid + 1
                else if (midVal > dist) high = mid - 1
                else return routeElevationProfile[mid].elevation
            }

            if (low == 0) return routeElevationProfile.first().elevation
            if (low >= routeElevationProfile.size) return routeElevationProfile.last().elevation

            val p1 = routeElevationProfile[low - 1]
            val p2 = routeElevationProfile[low]

            val t = (dist - p1.distance) / (p2.distance - p1.distance)
            return p1.elevation + t * (p2.elevation - p1.elevation)
        }

        if (routePoints.isNotEmpty()) {
            val pt = routePoints.minByOrNull { Math.abs(it.distance - dist) }
            return pt?.elevation ?: 0.0
        }

        return 0.0
    }

    private var lastRouteNameForReset: String? = null

    // Ruta base (completa) sobre la que se mide el km; los recortes de Karoo (puertos) se ignoran
    private var fullRoutePolyline: String = ""
    private var fullRouteGeomLength: Double = 0.0
    private var routeCropped: Boolean = false

    private fun geomLengthM(pts: List<Pair<Double, Double>>): Double {
        var total = 0.0
        for (i in 1 until pts.size) total += planarDistM(pts[i - 1], pts[i])
        return total
    }

    private fun planarDistM(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val dLat = (b.first - a.first) * 111320.0
        val dLon = (b.second - a.second) * 111320.0 * Math.cos(Math.toRadians((a.first + b.first) / 2.0))
        return Math.hypot(dLat, dLon)
    }

    private fun distToSegmentM(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val cosLat = Math.cos(Math.toRadians(p.first))
        val ax = (a.second - p.second) * 111320.0 * cosLat
        val ay = (a.first - p.first) * 111320.0
        val bx = (b.second - p.second) * 111320.0 * cosLat
        val by = (b.first - p.first) * 111320.0
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 > 0.0) (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0) else 0.0
        return Math.hypot(ax + t * dx, ay + t * dy)
    }

    /** true si una muestra de puntos de [inner] (incluidos extremos) cae sobre el trazado [outer]. */
    private fun isSubPath(inner: List<Pair<Double, Double>>, outer: List<Pair<Double, Double>>): Boolean {
        if (inner.isEmpty() || outer.size < 2) return false
        val samples = 12
        for (s in 0 until samples) {
            val p = inner[(s * (inner.size - 1)) / (samples - 1)]
            var near = false
            for (i in 1 until outer.size) {
                if (distToSegmentM(p, outer[i - 1], outer[i]) <= 40.0) { near = true; break }
            }
            if (!near) return false
        }
        return true
    }

    fun setRouteFromPolyline(polyline: String, routeName: String? = null) {
        if (polyline == lastRoutePolyline) return
        lastRoutePolyline = polyline
        
        var isNewRouteName = false
        if (routeName != null && routeName != lastRouteNameForReset) {
            polylineScaleFactor = 1.0
            lastRouteNameForReset = routeName
            isNewRouteName = true
        }

        
        val points = decodePolyline(polyline)
        if (points.isEmpty()) {
            routePoints = emptyList()
            activeRouteName = null
            lastRoutePolyline = ""
            return
        }

        var isSuperset = false
        if (!isNewRouteName && fullRoutePolyline.isNotEmpty() && routePoints.isNotEmpty()) {
            if (polyline == fullRoutePolyline) {
                routeCropped = false
                return
            }
            val newLen = geomLengthM(points)
            val base = routePoints.map { it.latitude to it.longitude }
            if (newLen < fullRouteGeomLength * 0.97 && isSubPath(points, base)) {
                // Recorte de Karoo (puerto): se conserva la ruta completa para no reiniciar el km
                routeCropped = true
                return
            }
            if (newLen > fullRouteGeomLength * 1.03 && isSubPath(base, points)) isSuperset = true
        }

        var isContinuation = false
        if (!isNewRouteName && !isSuperset && routePoints.isNotEmpty()) {
            val oldStart = routePoints.first()
            val newStart = points.first()
            
            // Approximation for distance in meters
            val dLatStart = Math.abs(oldStart.latitude - newStart.first) * 111320.0
            val dLonStart = Math.abs(oldStart.longitude - newStart.second) * (111320.0 * Math.cos(Math.toRadians(oldStart.latitude)))
            val distBetweenStarts = Math.hypot(dLatStart, dLonStart)
            
            // If it's the SAME route name, but the polyline starts somewhere else (>100m away),
            // Karoo cropped it (e.g. at the start of a climb or a recalculation).
            // We treat this as a continuation of the same route to preserve distance.
            if (distBetweenStarts > 100.0) {
                isContinuation = true
            }
        }
        
        if (!isContinuation) {
            polylineScaleFactor = 1.0
        }

        val shiftOffset = if (isContinuation) {
            if (currentRouteDistance > 0.0) currentRouteDistance else liveDistanceAccumulated
        } else 0.0
        this.globalRouteOffset = shiftOffset
        
        var accumulatedDist = shiftOffset
        val result = mutableListOf<RoutePoint>()

        // First pass: compute accumulated distances using accurate Haversine/Android Location
        val rawDistances = DoubleArray(points.size)
        val results = FloatArray(1)
        for (i in points.indices) {
            if (i > 0) {
                val prev = points[i - 1]
                val pt  = points[i]
                android.location.Location.distanceBetween(prev.first, prev.second, pt.first, pt.second, results)
                accumulatedDist += results[0] * polylineScaleFactor
            }
            rawDistances[i] = accumulatedDist
        }

        // Second pass: assign elevation anchored at the current barometric altitude.
        // We project forward using the live grade (m of climb per metre of distance).
        // If we have no live grade yet we assume a flat road — far better than fake sines.
        val anchorElev = if (currentElevation > 0.0) currentElevation else 100.0
        // gradePerMetre: e.g. 8% → 0.08; negative for descents
        val gradePerMetre = instantBarometricGrade / 100.0

        for (i in points.indices) {
            val pt = points[i]
            val dist = rawDistances[i]
            // Simple linear projection from anchor at the rider's current position.
            // The delta distance from dist=0 (start of polyline) could be huge if the
            // rider is already mid-route, so we keep it relative to current position.
            // updateCurrentLocation() + nearestIndex logic corrects this at render time.
            val projectedElev = anchorElev + dist * gradePerMetre
            result.add(RoutePoint(pt.first, pt.second, projectedElev, dist))
        }

        this.routePoints = result
        this.nearestIndex = 0
        this.isNavigatingRoute = true
        this.fullRoutePolyline = polyline
        this.fullRouteGeomLength = geomLengthM(points)
        this.routeCropped = false
        
        // If we already received the elevation profile, apply it now
        if (routeElevationProfile.isNotEmpty()) {
            applyTrueElevationsToRoutePoints()
        }

        detectHairpins()
        restoreSavedClimbsForRoute()
    }

    /**
     * Called whenever we get a fresh barometric altitude reading.
     * In route mode we recalibrate the elevation profile so that the point nearest to
     * the rider's current GPS position matches the real barometric altitude, and all
     * surrounding points are shifted by the same delta. This keeps the profile truthful.
     */
    fun recalibrateElevationProfile(realElev: Double) {
        if (routePoints.isEmpty() || realElev <= 0.0) return
        // Find the nearest point to the current GPS position
        var nearestIndex = 0
        var minDist = Double.MAX_VALUE
        routePoints.forEachIndexed { index, point ->
            val d = hypot(point.latitude - currentLatitude, point.longitude - currentLongitude)
            if (d < minDist) { minDist = d; nearestIndex = index }
        }
        val currentProfileElev = routePoints[nearestIndex].elevation
        val delta = realElev - currentProfileElev
        if (abs(delta) < 0.5) return  // negligible correction
        // Shift all points by the same delta (rigid translation keeps relative shape)
        this.routePoints = routePoints.map { it.copy(elevation = it.elevation + delta) }
    }

    private fun detectHairpins() {
        absoluteHairpins.clear()
        absoluteHairpinInfos.clear()
        if (routePoints.size < 3) return
        
        var lastP = routePoints[0]
        for (i in 1 until routePoints.size - 1) {
            val p1 = routePoints[i]
            if (p1.distance - lastP.distance < 15.0) continue
            
            var p2 = routePoints[i+1]
            for (j in i+1 until routePoints.size) {
                if (routePoints[j].distance - p1.distance >= 15.0) {
                    p2 = routePoints[j]
                    break
                }
            }
            
            val b1 = bearing(lastP.latitude, lastP.longitude, p1.latitude, p1.longitude)
            val b2 = bearing(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            
            var signedDiff = b2 - b1
            while (signedDiff > 180.0) signedDiff -= 360.0
            while (signedDiff < -180.0) signedDiff += 360.0
            val absDiff = abs(signedDiff)
            
            if (absDiff > 130.0) {
                if (absoluteHairpins.isEmpty() || (p1.distance - absoluteHairpins.last() > 50.0)) {
                    val isRight = signedDiff > 0.0
                    absoluteHairpins.add(p1.distance)
                    absoluteHairpinInfos.add(HairpinInfo(p1.distance, isRight))
                }
            }
            lastP = p1
        }
    }

    private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLon) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon)

        val brng = Math.toDegrees(Math.atan2(y, x))
        return (brng + 360.0) % 360.0
    }

    fun clearRoute() {
        this.routePoints = emptyList()
        this.routeElevationProfile = emptyList()
        this.routeClimbs = emptyList()
        this.routeKeyForClimbs = null
        this.absoluteHairpins.clear()
        this.absoluteHairpinInfos.clear()
        this.routePois.clear()
        this.isNavigatingRoute = false
        this.activeRouteName = null
        // Resetear datos del climb al salir de la navegación
        this.distanceToTop = 0.0
        this.elevationToTop = 0.0
        this.distanceFromBottom = 0.0
        this.elevationFromBottom = 0.0
        this.elevationRemaining = 0.0
        this.globalRouteOffset = 0.0
        this.lastRoutePolyline = ""
        this.lastElevationPolyline = ""
        this.polylineScaleFactor = 1.0
        this.fullRoutePolyline = ""
        this.fullRouteGeomLength = 0.0
        this.routeCropped = false
    }

    // advance=false: calcula la estrategia con el estado actual SIN mutarlo (EMA, integración, clima, Strava, APM)
    fun calculateStrategy(context: Context? = null, advance: Boolean = true): StrategyData {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        val effectiveCtx = context ?: appContext
        val prefs = effectiveCtx?.let { AppPreferences.getInstance(it) }

        val blockSize = prefs?.blockSizeMeters ?: 100.0
        val baseLookahead = (prefs?.lookaheadMeters3d ?: 350).toDouble()
        val thresholdAttack = prefs?.thresholdAttackPct ?: 10.0
        val attackAlertsEnabled = prefs?.attackAlertEnabled ?: true
        val asphaltFactor = prefs?.asphaltFactor ?: 0.5
        val useTopographicCalculation = prefs?.useTopographicCalculation ?: false

        // PRO Features Settings
        val smartZoomEnabled = prefs?.smartZoomEnabled ?: true
        val oasisTrackingEnabled = prefs?.oasisTrackingEnabled ?: true
        val virtualPacerEnabled = prefs?.virtualPacerEnabled ?: true
        val energyManagementEnabled = prefs?.energyManagementEnabled ?: true

        // SMART ZOOM LOGIC
        var lookaheadDist = baseLookahead
        
        // MAP ZOOM OVERRIDE
        if (mapZoomLevel != null) {
            // El mapa dibuja 480 píxeles a pantalla completa con esta resolución.
            // Para que la escala encaje en nuestro view (que puede ser 480, 240, etc.),
            // calculamos los metros por píxel y los multiplicamos por el ancho real del view.
            val mapMetersPerPixel = 119911.96 / Math.pow(2.0, mapZoomLevel!!)
            val actualViewWidth = if (viewWidth > 0) viewWidth.toDouble() else 480.0
            // La pantalla de Karoo tiene doble densidad de píxeles (2x), por lo que 1 píxel físico
            // equivale a medio píxel lógico de Mapbox. Por tanto, dividimos entre 2 el factor.
            val calibrationFactor = 0.416
            lookaheadDist = (actualViewWidth * mapMetersPerPixel * calibrationFactor).coerceIn(50.0, 100000.0)
        } else if (smartZoomEnabled && baseLookahead < 100000.0) {
            val targetLookahead = when {
                instantBarometricGrade >= 8.0 -> baseLookahead.coerceAtMost(350.0) // Crucible
                instantBarometricGrade >= 4.0 -> baseLookahead.coerceAtLeast(1000.0) // Normal climb
                else -> baseLookahead.coerceAtLeast(3000.0) // Flat or descent
            }
            if (advance) smartLookahead = smartLookahead + (targetLookahead - smartLookahead) * 0.05
            lookaheadDist = smartLookahead
        } else {
            if (advance) smartLookahead = baseLookahead
        }

        val isNavigating = isNavigatingRoute && (routePoints.isNotEmpty() || routeElevationProfile.isNotEmpty())

        // MODO LIBRE / ENTRENAMIENTO REAL SIN RUTA PRECARGADA:
        // Genera el perfil 3D con el historial acumulado en tiempo real en lugar de proyectar al futuro
        if (!isNavigating) {
            val baseGrade = instantBarometricGrade
            // Integrar por tiempo transcurrido, no "1 s por llamada": con varias vistas o
            // streams del mismo campo, calculateStrategy() se llama más de una vez por segundo.
            // Nota: tope de 2 s tras un hueco (vista fuera de pantalla), como el 1 s fijo anterior
            if (advance) {
                val nowMs = System.currentTimeMillis()
                val dt = if (lastIntegrationMs == 0L) 1.0 else ((nowMs - lastIntegrationMs) / 1000.0).coerceIn(0.0, 2.0)
                lastIntegrationMs = nowMs
                if (currentSpeed > 0.1) {
                    liveDistanceAccumulated += currentSpeed * dt
                }

                // Acumular el punto actual en el historial si avanzamos más de 5 metros
                if (freeRideHistory.isEmpty() || liveDistanceAccumulated - freeRideHistory.last().first >= 5.0) {
                    freeRideHistory.add(Pair(liveDistanceAccumulated, currentElevation))
                }
            }

            // La ventana ahora mira hacia atrás desde la posición actual
            val windowStartDist = (liveDistanceAccumulated - lookaheadDist).coerceAtLeast(0.0)
            
            // El ciclista avanza hacia la derecha al empezar, y luego se queda en el borde derecho
            val riderDistInWindow = (liveDistanceAccumulated - windowStartDist).coerceAtLeast(0.0)
            val riderProgress = (riderDistInWindow / lookaheadDist).toFloat().coerceIn(0f, 1f)

            val customResolution = prefs?.eliteRadarResolution ?: 0
            val subBlockSize = getSubBlockSize(lookaheadDist, customResolution)
            val majorBlockSize = getMajorBlockSize(lookaheadDist)
            val numSubBlocks = (lookaheadDist / subBlockSize).toInt().coerceIn(2, 60)

            val freeSubBlocks = mutableListOf<Float>()
            val freeSubBlocksMax = mutableListOf<Float>()
            val freeElevations = mutableListOf<Float>()

            // Remuestrear el historial en los sub-bloques de la ventana
            for (idx in 0..numSubBlocks) {
                val ptDist = windowStartDist + (idx * subBlockSize)
                val ptElev = getFreeRideElevationAt(ptDist)
                freeElevations.add(ptElev.toFloat())
                
                if (idx > 0) {
                    val prevElev = freeElevations[idx - 1]
                    val dE = ptElev - prevElev
                    val dD = subBlockSize
                    val grade = if (dD > 0) (dE / dD) * 100.0 else 0.0
                    freeSubBlocks.add(grade.toFloat())
                    
                    // In free ride, we don't have finer data than the history array.
                    // We'll just set max grade equal to average grade for now, or sample it if we had finer resolution.
                    // Since history is recorded at 1Hz, we could sample it, but it's simpler to approximate.
                    freeSubBlocksMax.add(grade.toFloat())
                }
            }

            val windowStartElevation = freeElevations.first().toDouble()

            val hasAttack = attackAlertsEnabled && freeSubBlocks.any { it > thresholdAttack }
            val liveFatigue = (freeSubBlocks.average() * 8.5 + asphaltFactor * 10).roundToInt().coerceIn(10, 250)

            if (advance) ClimbStateManager.updateApm(liveFatigue)

            val liveHairpins = emptyList<Double>()
            val livePois = emptyList<Poi>()

            val liveCurvatures = List(50) { idx ->
                val distAlongWindow = idx * (lookaheadDist / 49.0)
                (sin((windowStartDist + distAlongWindow) / 75.0) * 0.75 + sin((windowStartDist + distAlongWindow) / 200.0) * 0.25).toFloat()
            }

        return StrategyData(
                remainingDistance = 0.0,
                timeToSummit = 0L,
                avgGrade = baseGrade,
                nextBlocks = freeSubBlocks.take(10),
                attackAlert = hasAttack,
                blockSizeMeters = subBlockSize,
                totalFatigueGrade = liveFatigue,
                hairpins = liveHairpins,
                pois = livePois,
                curvatureOffsets = liveCurvatures,
                riderProgress = riderProgress,
                windowStartMeters = windowStartDist,
                windowStartElevation = windowStartElevation,
                subBlocks = freeSubBlocks,
                subBlocksMax = freeSubBlocksMax,
                windowLengthMeters = lookaheadDist,
                subBlockSizeMeters = subBlockSize,
                majorBlockSizeMeters = majorBlockSize,
                profileElevations = freeElevations,
                activeClimbs = emptyList(),
                routeName = activeRouteName,
                routeTotalLength = 0.0
            )
        }

        // 1. Encuentra el índice más cercano (movido a updateCurrentLocation para que sea continuo)

        // 1. Usar la distancia reportada y suavizada
        val currentRiderDistance = currentRouteDistance

        // 2. Ventana deslizante en bloques cuánticos de 50 metros (avance gradual y continuo)
        val quantumMeters = 50.0
        // Desplazamos la ventana 1/4 hacia atrás para que el ciclista aparezca al 25% de la pantalla SIEMPRE
        val autoStart = currentRiderDistance - (lookaheadDist / 4.0)
        
        // Restauramos el coerceAtLeast(0.0) a petición del usuario para que la ruta empiece en 0m y no en negativo.
        var windowStartDist = (Math.floor(autoStart / quantumMeters).toLong() * quantumMeters).toDouble().coerceAtLeast(0.0)
        
        // Aplicamos el pan manual DESPUÉS del cálculo base para que funcione instantáneamente
        windowStartDist = (windowStartDist + manualPanOffsetMeters).coerceAtLeast(0.0)
        
        var actualLookahead = lookaheadDist
        val totalRouteLength = routeElevationProfile.lastOrNull()?.distance ?: (routePoints.lastOrNull()?.distance ?: 0.0)

        // Limitar desplazamiento manual (pan) para no arrastrar más allá del final de la ruta (evitar bloques al 0% irreales)
        if (totalRouteLength > 0.0 && manualPanOffsetMeters > 0.0) {
            val maxWindowStart = (totalRouteLength - actualLookahead).coerceAtLeast(0.0)
            windowStartDist = windowStartDist.coerceAtMost(maxWindowStart)
        }

        // ADAPTACIÓN DE ZOOM A LA RUTA:
        // Si tenemos una ruta activa y el zoom solicitado es mayor o igual a la longitud total,
        // limitamos la ventana a la ruta completa empezando en el km 0 para no mostrar áreas planas más allá del final de ruta.
        if (totalRouteLength > 500.0 && (lookaheadDist >= totalRouteLength || lookaheadDist >= 100000.0)) {
            windowStartDist = 0.0
            actualLookahead = totalRouteLength
        }
        
        val windowEndDist = windowStartDist + actualLookahead

        val riderOffsetInWindow = (currentRiderDistance - windowStartDist).coerceAtLeast(0.0)
        val riderProgress = (riderOffsetInWindow / actualLookahead).toFloat().coerceIn(0f, 1f)

        // 3. Localizar el punto de ruta correspondiente al inicio de la ventana (windowStartDist)
        var windowStartIndex = nearestIndex
        while (windowStartIndex > 0 && routePoints[windowStartIndex].distance > windowStartDist) {
            windowStartIndex--
        }
        while (windowStartIndex < routePoints.size - 1 && routePoints[windowStartIndex + 1].distance <= windowStartDist) {
            windowStartIndex++
        }
        val windowStartElevation = getElevationAtDistance(windowStartDist)

        // 4. Distancia y desnivel restantes
        val totalDistanceRemaining: Double = if (routePoints.isNotEmpty()) {
            (routePoints.last().distance - currentRiderDistance).coerceAtLeast(0.0)
        } else {
            fallbackRemainingDistance
        }
        
        val elevationGainRemaining: Double
        if (distanceToTop > 0.0 && elevationToTop > 0.0) {
            // ✅ Usar elevación real del SDK si estamos en un climb, pero NO machacar totalDistanceRemaining
            elevationGainRemaining = elevationToTop
        } else {
            val endElevation = if (routePoints.isNotEmpty()) routePoints.last().elevation else currentElevation
            elevationGainRemaining = (endElevation - currentElevation).coerceAtLeast(0.0)
        }

        val avgGradeRemaining = if (totalDistanceRemaining > 0) {
            if (useTopographicCalculation) {
                val distRealSq = totalDistanceRemaining * totalDistanceRemaining
                val elevSq = elevationGainRemaining * elevationGainRemaining
                val horizontalDist = if (distRealSq > elevSq) sqrt(distRealSq - elevSq) else totalDistanceRemaining
                (elevationGainRemaining / horizontalDist) * 100.0
            } else {
                (elevationGainRemaining / totalDistanceRemaining) * 100.0
            }
        } else {
            0.0
        }

        val secondsRemaining = if (currentSpeed > 0.1) (totalDistanceRemaining / currentSpeed).toLong() else 0L

        // 5. Cálculo de resolución adaptativa según escala (Lookahead)
        val customResolution = prefs?.eliteRadarResolution ?: 0
        var subBlockSize = getSubBlockSize(actualLookahead, customResolution)
        
        // Cap blocks to prevent OOM or lag if lookahead is huge but resolution is small
        if (actualLookahead / subBlockSize > 500.0) {
            subBlockSize = actualLookahead / 500.0
        }
        val majorBlockSize = getMajorBlockSize(actualLookahead)

        // Add extra blocks (+ 2) to buffer the right edge during local rolling window. 
        // When viewing the entire route (actualLookahead == totalRouteLength), cap exactly at route end so no flat ground appears.
        val isFullRouteView = totalRouteLength > 500.0 && actualLookahead >= (totalRouteLength - 1.0)
        val numSubBlocks = if (isFullRouteView) {
            (actualLookahead / subBlockSize).roundToInt().coerceIn(2, 2000)
        } else {
            (actualLookahead / subBlockSize).roundToInt().coerceIn(2, 2000) + 2
        }
        val routeSubBlocks = mutableListOf<Float>()
        val routeSubBlocksMax = mutableListOf<Float>()
        val routeElevations = mutableListOf<Float>()
        routeElevations.add(windowStartElevation.toFloat())

        var attack = false
        var accumulatedHardness = 0.0
        var maxRampPct = 0.0

        // ── Estrategia de construcción del perfil ──────────────────────────────────
        // Si tenemos datos nativos del climb (del SDK, fuente directa del archivo GPX/FIT):
        //   → Usamos la interpolación a lo largo del climb PERO si además tenemos el
        //     PERFIL DE ELEVACIÓN COMPLETO (routeElevationProfile), entonces usamos
        //     esos datos con precisión punto a punto.
        // ──────────────────────────────────────────────────────────────────────────
        val useClimberData = distanceToTop > 50.0 && elevationToTop > 0.5
        val summitElevation = if (useClimberData) currentElevation + elevationToTop else 0.0
        val hasTrueProfile = routeElevationProfile.isNotEmpty()

        for (j in 0 until numSubBlocks) {
            val sDistStart = windowStartDist + (j * subBlockSize)
            val sDistEnd = windowStartDist + ((j + 1) * subBlockSize)

            val sElevStart: Double
            val sElevEnd: Double

            if (hasTrueProfile) {
                // Si tenemos el perfil real, leemos la altitud exacta!
                sElevStart = getElevationAtDistance(sDistStart)
                sElevEnd   = getElevationAtDistance(sDistEnd)
            } else if (useClimberData) {
                // Fallback a interpolación lineal si no hay perfil detallado
                val fracStart = (sDistStart - windowStartDist).coerceAtLeast(0.0) / distanceToTop.coerceAtLeast(1.0)
                val fracEnd   = (sDistEnd   - windowStartDist).coerceAtLeast(0.0) / distanceToTop.coerceAtLeast(1.0)
                sElevStart = currentElevation + fracStart.coerceIn(0.0, 1.0) * elevationToTop
                sElevEnd   = currentElevation + fracEnd.coerceIn(0.0, 1.0)   * elevationToTop
            } else {
                // Fallback a polilínea 2D calibrada barométricamente
                sElevStart = getElevationAtDistance(sDistStart)
                sElevEnd   = getElevationAtDistance(sDistEnd)
            }

            val sElevDiff = sElevEnd - sElevStart
            val sDist = (sDistEnd - sDistStart).coerceAtLeast(1.0)
            val grade = if (sDistEnd <= 0.0) {
                -999.0
            } else if (useTopographicCalculation) {
                val distRealSq = sDist * sDist
                val elevSq = sElevDiff * sElevDiff
                val horizontalDist = if (distRealSq > elevSq) sqrt(distRealSq - elevSq) else sDist
                (sElevDiff / horizontalDist) * 100.0
            } else {
                (sElevDiff / sDist) * 100.0
            }
            
            // Calculate max inner grade by sampling every 10 meters (if we have true profile)
            var maxInnerGrade = grade
            if (hasTrueProfile) {
                var innerDist = sDistStart
                val step = 10.0
                while (innerDist < sDistEnd) {
                    val nextDist = (innerDist + step).coerceAtMost(sDistEnd)
                    val e1 = getElevationAtDistance(innerDist)
                    val e2 = getElevationAtDistance(nextDist)
                    val dD = nextDist - innerDist
                    if (dD > 0) {
                        val innerG = ((e2 - e1) / dD) * 100.0
                        if (innerG > maxInnerGrade) maxInnerGrade = innerG
                    }
                    innerDist += step
                }
            }

            routeSubBlocks.add(grade.toFloat())
            routeSubBlocksMax.add(maxInnerGrade.toFloat())
            routeElevations.add(sElevEnd.toFloat())
        }

        // Reserva a la derecha medida en distancia (no en nº de bloques): cubre el desfase de la ventana
        // cuantizada a 50 m y la inclinación del efecto 3D, para que nunca asome un hueco sin datos.
        // En vista de ruta completa no añadimos buffer a la derecha para no mostrar tramos planos irreales.
        val rightBufferMeters = if (isFullRouteView) 0.0 else (quantumMeters + actualLookahead * 0.4)
        val rightBufferCount = if (isFullRouteView) 0 else Math.ceil(rightBufferMeters / subBlockSize).toInt().coerceIn(2, 500)
        val rightBufferBlocks = ArrayList<Float>(rightBufferCount)
        if (!isFullRouteView) {
            for (j in numSubBlocks until numSubBlocks + rightBufferCount) {
                val bStart = windowStartDist + (j * subBlockSize)
                val bEnd = bStart + subBlockSize
                val bElevDiff = getElevationAtDistance(bEnd) - getElevationAtDistance(bStart)
                val bGrade = if (useTopographicCalculation) {
                    val distRealSq = subBlockSize * subBlockSize
                    val elevSq = bElevDiff * bElevDiff
                    val horizontalDist = if (distRealSq > elevSq) sqrt(distRealSq - elevSq) else subBlockSize
                    (bElevDiff / horizontalDist) * 100.0
                } else {
                    (bElevDiff / subBlockSize) * 100.0
                }
                rightBufferBlocks.add(bGrade.toFloat())
            }
        }

        // Construcción de bloques mayores para telemetría y rótulos
        val numMajorBlocks = (actualLookahead / majorBlockSize).roundToInt().coerceIn(1, 200)
        val routeMajorBlocks = mutableListOf<Float>()
        for (m in 0 until numMajorBlocks) {
            val mDistStart = windowStartDist + (m * majorBlockSize)
            val mDistEnd = windowStartDist + ((m + 1) * majorBlockSize)
            val mElevStart = getElevationAtDistance(mDistStart)
            val mElevEnd = getElevationAtDistance(mDistEnd)
            val mElevDiff = mElevEnd - mElevStart
            val mDist = (mDistEnd - mDistStart).coerceAtLeast(1.0)
            val mGrade = if (useTopographicCalculation) {
                val distRealSq = mDist * mDist
                val elevSq = mElevDiff * mElevDiff
                val horizontalDist = if (distRealSq > elevSq) sqrt(distRealSq - elevSq) else mDist
                (mElevDiff / horizontalDist) * 100.0
            } else {
                (mElevDiff / mDist) * 100.0
            }
            routeMajorBlocks.add(mGrade.toFloat())
        }


        val visibleHairpins = absoluteHairpins.toList() // copia: la lista se modifica en el sitio
            .map { it - windowStartDist }
            .filter { it in 0.0..actualLookahead }

        val visibleHairpinsInfo = absoluteHairpinInfos.toList()
            .map { it.copy(distance = it.distance - windowStartDist) }
            .filter { it.distance in 0.0..actualLookahead }

        val showPoiTowns = prefs?.showPoiTowns ?: true
        val showPoiWater = prefs?.showPoiWater ?: true
        val showPoiViewpoints = prefs?.showPoiViewpoints ?: true
        val showPoiSummits = prefs?.showPoiSummits ?: true

        // Solo mostrar POIs reales de la ruta SDK. Si la ruta no tiene POIs no fabricamos ninguno.
        val rawPois = if (routePois.isNotEmpty()) {
            routePois.map { Poi(it.relativeDistance - windowStartDist, it.icon, it.name, it.type) }
        } else {
            emptyList()
        }

        val visiblePois = rawPois.filter { poi ->
            val matchesCategory = when (poi.type) {
                PoiType.TOWN -> showPoiTowns
                PoiType.WATER -> showPoiWater
                PoiType.VIEWPOINT -> showPoiViewpoints
                PoiType.SUMMIT -> showPoiSummits
            }
            matchesCategory && poi.relativeDistance in 0.0..actualLookahead
        }

        // Cálculo de curvatura real de la carretera GPS por orientación vectorial muestreado a 50 puntos
        val rawOffsets = mutableListOf<Pair<Double, Double>>()
        rawOffsets.add(Pair(windowStartDist, 0.0))

        if (routePoints.size > windowStartIndex + 1) {
            val startP = routePoints[windowStartIndex]
            var initialBearing = 0.0
            if (windowStartIndex < routePoints.size - 1) {
                val p1 = routePoints[windowStartIndex + 1]
                initialBearing = bearing(startP.latitude, startP.longitude, p1.latitude, p1.longitude)
            }

            var cumOffset = 0.0
            for (i in windowStartIndex + 1 until routePoints.size) {
                val prevP = routePoints[i - 1]
                val currP = routePoints[i]
                val segDist = (currP.distance - prevP.distance).coerceAtLeast(1.0)

                val curBearing = bearing(prevP.latitude, prevP.longitude, currP.latitude, currP.longitude)
                var angleDiff = curBearing - initialBearing
                while (angleDiff > 180.0) angleDiff -= 360.0
                while (angleDiff < -180.0) angleDiff += 360.0

                val lateralMeters = sin(Math.toRadians(angleDiff)) * (segDist / 25.0)
                cumOffset += lateralMeters
                rawOffsets.add(Pair(currP.distance, cumOffset))

                if (currP.distance >= windowEndDist) {
                    break
                }
            }
        }

        // Muestreo uniforme en 50 puntos a lo largo de la ventana [windowStartDist, windowEndDist]
        val curvatureOffsets = List(50) { idx ->
            val targetDist = windowStartDist + idx * (actualLookahead / 49.0)
            if (rawOffsets.size >= 2) {
                val nextIdx = rawOffsets.indexOfFirst { it.first >= targetDist }
                if (nextIdx == -1) {
                    rawOffsets.last().second.coerceIn(-1.0, 1.0).toFloat()
                } else if (nextIdx == 0) {
                    rawOffsets.first().second.coerceIn(-1.0, 1.0).toFloat()
                } else {
                    val p0 = rawOffsets[nextIdx - 1]
                    val p1 = rawOffsets[nextIdx]
                    val span = (p1.first - p0.first).coerceAtLeast(0.1)
                    val frac = ((targetDist - p0.first) / span).coerceIn(0.0, 1.0)
                    (p0.second + frac * (p1.second - p0.second)).coerceIn(-1.0, 1.0).toFloat()
                }
            } else {
                0.0f
            }
        }

        // ── Cálculo de Pendiente Máxima y Media REAL del Tramo Visible ────────────────
        val rawSource = if (routeElevationProfile.isNotEmpty()) routeElevationProfile else routePoints.map { ElevationPolylineDecoder.ElevationPoint(it.distance, it.elevation) }

        var trueMaxGrade = 0.0
        var visibleElevationGain = 0.0
        var visibleAvgGrade = avgGradeRemaining // Fallback
        
        var trueAccumulatedHardness = 0.0
        var attackAlert = false

        if (rawSource.isNotEmpty()) {
            var lastPt: ElevationPolylineDecoder.ElevationPoint? = null
            var firstPt: ElevationPolylineDecoder.ElevationPoint? = null
            var endPt: ElevationPolylineDecoder.ElevationPoint? = null

            var totalAscent = 0.0
            var ascentDistance = 0.0

            for (pt in rawSource) {
                if (pt.distance < windowStartDist) {
                    lastPt = pt
                    continue
                }
                if (firstPt == null) firstPt = pt

                if (lastPt != null) {
                    val dDist = pt.distance - lastPt.distance
                    if (dDist > 5.0) { // Ignorar distancias microscópicas para evitar ruido
                        val dElev = pt.elevation - lastPt.elevation
                        val grade = (dElev / dDist) * 100.0
                        if (grade > trueMaxGrade) trueMaxGrade = grade
                        
                        if (grade > 0.0) {
                            trueAccumulatedHardness += FatigueGradeCalculator.calculateSegmentHardness(grade, dDist / 1000.0)
                        }
                        if (attackAlertsEnabled && grade > thresholdAttack) attackAlert = true
                        
                        if (dElev > 0.0) {
                            totalAscent += dElev
                            ascentDistance += dDist
                        }
                        lastPt = pt
                    }
                } else {
                    lastPt = pt
                }
                endPt = pt

                if (pt.distance > windowEndDist) {
                    break
                }
            }
            
            if (ascentDistance > 0.0) {
                visibleAvgGrade = (totalAscent / ascentDistance) * 100.0
            } else {
                visibleAvgGrade = 0.0
            }
        }

        val totalGf = FatigueGradeCalculator.calculateTotalFatigueGrade(
            totalHardness = trueAccumulatedHardness,
            asphaltFactor = asphaltFactor,
            maxRampPct = trueMaxGrade
        ).roundToInt()

        if (advance) ClimbStateManager.updateApm(totalGf)

        // 12. Filtrar puertos visibles en la ventana 3D actual
        val visibleClimbs = routeClimbs.filter { climb ->
            climb.startDistance < windowEndDist && climb.endDistance > windowStartDist
        }

        // OASIS TRACKING LOGIC
        var oasisDistance: Double? = null
        if (oasisTrackingEnabled && instantBarometricGrade < 3.0) {
            var distToCrucible = 0.0
            for (i in (windowStartIndex..routePoints.size-2)) {
                val pt1 = routePoints[i]
                val pt2 = routePoints[i+1]
                val dist = pt2.distance - pt1.distance
                if (dist > 0) {
                    val grade = ((pt2.elevation - pt1.elevation) / dist) * 100.0
                    if (grade >= 8.0) {
                        distToCrucible = (pt1.distance - currentRiderDistance).coerceAtLeast(0.0)
                        break
                    }
                }
            }
            if (distToCrucible > 50.0) {
                oasisDistance = distToCrucible
            }
        }

        // Virtual pacer init logic for route mode
        if (advance && virtualPacerDistance == 0.0) virtualPacerDistance = currentRiderDistance

        // ELITE FEATURE: Wind Overlay
        var windEffect: Double? = null
        var stravaRelDist: Double? = null
        var stravaGhostRelDist: Double? = null
        
        context?.let { ctx ->
            val prefs = AppPreferences.getInstance(ctx)
            if (prefs.weatherOverlayEnabled) {
                val currentTime = System.currentTimeMillis()
                if (advance && currentTime - lastWindCheckTime > 5 * 60 * 1000) { // Check every 5 mins
                    if (routePoints.isNotEmpty()) {
                        val pt = routePoints[windowStartIndex]
                        WeatherService.fetchWeatherData(pt.latitude, pt.longitude)
                    }
                    lastWindCheckTime = currentTime
                }
                if (WeatherService.isDataFresh && windowStartIndex < routePoints.size - 10) {
                    val pt1 = routePoints[windowStartIndex]
                    val pt2 = routePoints[windowStartIndex + 10]
                    val dLon = (pt2.longitude - pt1.longitude)
                    val y = Math.sin(Math.toRadians(dLon)) * Math.cos(Math.toRadians(pt2.latitude))
                    val x = Math.cos(Math.toRadians(pt1.latitude)) * Math.sin(Math.toRadians(pt2.latitude)) - Math.sin(Math.toRadians(pt1.latitude)) * Math.cos(Math.toRadians(pt2.latitude)) * Math.cos(Math.toRadians(dLon))
                    val heading = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360
                    
                    val windDir = WeatherService.currentWindDirectionDegrees.toDouble()
                    val tailwindComponent = -Math.cos(Math.toRadians(heading - windDir))
                    windEffect = tailwindComponent * (WeatherService.currentWindSpeedKmh / 50.0).coerceIn(0.0, 1.0)
                }
            }

            // ELITE FEATURE: Strava Live Segments Mock
            if (prefs.stravaSegmentsEnabled) {
                if (advance && !isStravaSegmentActive && instantBarometricGrade > 7.0 && currentRiderDistance > stravaSegmentStartDist + 3000) {
                    isStravaSegmentActive = true
                    stravaSegmentStartDist = currentRiderDistance
                    stravaPrGhostDist = currentRiderDistance
                }
                
                if (isStravaSegmentActive) {
                    val distInSegment = currentRiderDistance - stravaSegmentStartDist
                    if (distInSegment > 2000.0) {
                        if (advance) isStravaSegmentActive = false // Segment finished
                    } else {
                        stravaRelDist = distInSegment
                        if (advance) stravaPrGhostDist += (5.5 * 1.0) // Ghost runs at 1300 VAM (approx 5.5 m/s)
                        stravaGhostRelDist = stravaPrGhostDist - currentRiderDistance
                    }
                }
            }
        }

        return StrategyData(
            remainingDistance = totalDistanceRemaining,
            timeToSummit = secondsRemaining,
            avgGrade = avgGradeRemaining,
            nextBlocks = routeMajorBlocks,
            attackAlert = attackAlert,
            blockSizeMeters = majorBlockSize,
            totalFatigueGrade = totalGf,
            hairpins = visibleHairpins,
            hairpinsInfo = visibleHairpinsInfo,
            pois = visiblePois,
            curvatureOffsets = curvatureOffsets,
            riderProgress = riderProgress,
            windowStartMeters = windowStartDist,
            windowStartElevation = windowStartElevation,
            subBlocks = routeSubBlocks,
            subBlocksMax = routeSubBlocksMax,
            rightBufferBlocks = rightBufferBlocks,
            windowLengthMeters = actualLookahead,
            subBlockSizeMeters = subBlockSize,
            majorBlockSizeMeters = majorBlockSize,
            profileElevations = routeElevations,
            activeClimbs = routeClimbs,
            visibleAvgGrade = visibleAvgGrade,
            visibleMaxGrade = trueMaxGrade,
            routeName = activeRouteName,
            routeTotalLength = if (routePoints.isNotEmpty()) routePoints.last().distance else 0.0,
            oasisDistanceToNextCrucible = oasisDistance,
            virtualPacerRelativeDistance = if (virtualPacerEnabled) virtualPacerDistance - currentRiderDistance else null,
            energyBatteryLevel = energyBatteryLevel,
            currentPower = currentPower,
            currentCadence = currentCadence,
            stravaSegmentDistance = stravaRelDist,
            stravaPrGhostDistance = stravaGhostRelDist,
            windEffectIntensity = windEffect,
            routeCoords = getVisibleRouteCoords(windowStartIndex, routePoints, actualLookahead.toInt()),
            surfaceSegments = routeSurfaceSegments
        )
    }

    fun getZoneColor(currentElevation: Double): String {
        return when {
            currentElevation > 2000 -> "VIOLETA"
            currentElevation > 1500 -> "ROJO"
            currentElevation > 1000 -> "NARANJA"
            currentElevation > 500 -> "AMARILLO"
            else -> "VERDE"
        }
    }

    private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        return try {
            val poly = ArrayList<Pair<Double, Double>>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0

            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng

                val pLat = lat.toDouble() / 1E5
                val pLng = lng.toDouble() / 1E5
                poly.add(Pair(pLat, pLng))
            }
            poly
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getStrategyDataForClimb(climb: RouteClimb, customStartDist: Double? = null, customLength: Double? = null): StrategyData {
        val windowLength = customLength ?: climb.length
        val majorBlock = getMajorBlockSize(windowLength)
        val subBlock = getSubBlockSize(windowLength)
        
        val subBlocks = mutableListOf<Float>()
        val profileElevs = mutableListOf<Float>()
        
        val startDist = customStartDist ?: climb.startDistance
        val endDist = (startDist + windowLength).coerceAtMost(climb.endDistance)
        var currentDist = startDist
        
        var minElev = Double.MAX_VALUE
        var maxElev = Double.MIN_VALUE
        var trueMaxGrade = 0.0
        var totalAscent = 0.0
        var ascentDistance = 0.0
        
        // Sampling loop for the static climb
        while (currentDist < endDist) {
            val chunkEnd = (currentDist + subBlock).coerceAtMost(endDist)
            val e1 = getElevationAtDistance(currentDist)
            val e2 = getElevationAtDistance(chunkEnd)
            val dDist = chunkEnd - currentDist
            
            var grade = 0.0
            if (dDist > 0.0) {
                grade = ((e2 - e1) / dDist) * 100.0
                if (grade > trueMaxGrade) trueMaxGrade = grade
                if (grade > 0.0) {
                    totalAscent += (e2 - e1)
                    ascentDistance += dDist
                }
            }
            
            subBlocks.add(grade.toFloat())
            profileElevs.add(e1.toFloat())
            
            if (e1 < minElev) minElev = e1
            if (e2 > maxElev) maxElev = e2
            
            currentDist += subBlock
        }
        
        // Add final elevation point to close the profile
        val finalElev = getElevationAtDistance(endDist)
        profileElevs.add(finalElev.toFloat())
        if (finalElev < minElev) minElev = finalElev
        if (finalElev > maxElev) maxElev = finalElev
        
        val avgGrade = if (ascentDistance > 0) (totalAscent / ascentDistance) * 100.0 else climb.avgGrade
        
        val actualRiderDist = currentRouteDistance
        val progress = if (windowLength > 0.0) ((actualRiderDist - startDist) / windowLength).toFloat().coerceIn(0f, 1f) else 0f

        val visibleHairpins = absoluteHairpins.toList()
            .filter { it in startDist..endDist }
            .map { it - startDist }

        val visibleHairpinsInfo = absoluteHairpinInfos.toList()
            .filter { it.distance in startDist..endDist }
            .map { it.copy(distance = it.distance - startDist) }

        return StrategyData(
            remainingDistance = climb.length,
            timeToSummit = 0L,
            avgGrade = avgGrade,
            nextBlocks = emptyList(), // Not used in this static view
            attackAlert = trueMaxGrade >= 12.0,
            blockSizeMeters = majorBlock,
            totalFatigueGrade = climb.apm.roundToInt(),
            hairpins = visibleHairpins,
            hairpinsInfo = visibleHairpinsInfo,
            pois = emptyList(),
            curvatureOffsets = emptyList(),
            riderProgress = progress,
            windowStartMeters = startDist,
            windowStartElevation = minElev,
            subBlocks = subBlocks,
            windowLengthMeters = windowLength,
            subBlockSizeMeters = subBlock,
            majorBlockSizeMeters = majorBlock,
            profileElevations = profileElevs,
            activeClimbs = listOf(climb),
            visibleAvgGrade = avgGrade,
            visibleMaxGrade = trueMaxGrade,
            routeName = climb.category, // Passing category as routeName to display in the header
            routeTotalLength = climb.endDistance,
            routeCoords = getVisibleRouteCoords(routePoints.indexOfFirst { it.distance >= startDist }.coerceAtLeast(0), routePoints, (endDist - startDist).toInt())
        )
    }
}