package com.example.altgraph

import android.util.Log
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnNavigationState

/**
 * Único punto que traduce eventos de navegación y streams de karoo-ext
 * a llamadas sobre el calculador compartido.
 */
object NavigationSync {

    private const val TAG = "NavigationSync"

    /** Streams que el repositorio debe suscribir. */
    val STREAM_TYPES: List<String> = listOf(
        DataType.Type.SPEED,
        DataType.Type.PRESSURE_ELEVATION_CORRECTION,
        DataType.Type.ELEVATION_GRADE,
        DataType.Type.POWER,
        DataType.Type.CADENCE,
        DataType.Type.HEART_RATE,
        DataType.Type.DISTANCE_TO_TOP,
        DataType.Type.ELEVATION_TO_TOP,
        DataType.Type.DISTANCE_FROM_BOTTOM,
        DataType.Type.ELEVATION_FROM_BOTTOM,
        DataType.Type.ELEVATION_REMAINING
    )

    // Secuencia copiada del consumer de OnNavigationState del campo 3D
    fun applyNavigation(calc: AltimetriaStrategyCalculator, state: OnNavigationState.NavigationState) {
        try {
            if (state is OnNavigationState.NavigationState.NavigatingRoute) {
                Log.d(TAG, "NAV: ruta='${state.name}' dist=${state.routeDistance}m pois=${state.pois.size}")
                calc.isNavigatingRoute = true
                calc.activeRouteName = state.name
                calc.syncRouteDistance(state.routeDistance)
                calc.setRouteFromPolyline(state.routePolyline, state.name)

                var elevPoly = state.routeElevationPolyline
                if (elevPoly.isNullOrEmpty()) {
                    elevPoly = (state.javaClass.methods.find { it.name == "getElevationPolyline" }?.invoke(state) as? String)
                }
                calc.setRouteElevationProfile(elevPoly)

                calc.setRoutePois(state.pois)

                // Sincronizar lista de puertos (Mountain Gates)
                val routeKey = "route:${state.name}"
                val routeClimbs = state.climbs.map { climb ->
                    RouteClimb(
                        startDistance = climb.startDistance,
                        endDistance = climb.startDistance + climb.length,
                        length = climb.length,
                        totalElevation = climb.totalElevation,
                        avgGrade = climb.grade
                    )
                }
                calc.syncRouteClimbs(routeKey, routeClimbs)

                // Usar routeDistance como fallback si no tenemos puntos GPS
                calc.syncRouteDistance(state.routeDistance)

            } else if (state is OnNavigationState.NavigationState.NavigatingToDestination) {
                val dist = (state.javaClass.methods.find { it.name == "getDestinationDistance" || it.name == "getDistance" }?.invoke(state) as? Double) ?: 0.0
                Log.d(TAG, "NAV: Destino dinámico detectado, dist=${dist}m")
                calc.isNavigatingRoute = true
                // Rutas a destino no suelen tener routePolyline, pero sí elevationPolyline
                calc.setRouteElevationProfile(state.elevationPolyline)
                calc.syncRouteDistance(dist)

            } else if (state.javaClass.simpleName == "Idle" || state is OnNavigationState.NavigationState.Idle) {
                Log.d(TAG, "NAV: Idle (sin ruta cargada)")
                calc.clearRoute()
            }
        } catch (e: Exception) {
            Log.e(TAG, "NAV: error aplicando navegación", e)
        }
    }

    // Claves y fallbacks idénticos a los consumers del campo 3D (POWER, del RouteBar)
    fun applyStream(calc: AltimetriaStrategyCalculator, type: String, values: Map<String, Double>) {
        val single = values[DataType.Field.SINGLE]
        
        when (type) {
            DataType.Type.SPEED -> {
                val speed = values[DataType.Field.SPEED] ?: single ?: 0.0
                // Log.v(TAG, "STREAM SPEED: $speed m/s") // Opcional, puede generar mucho ruido
                calc.currentSpeed = speed
            }
            DataType.Type.PRESSURE_ELEVATION_CORRECTION -> {
                val elev = values[DataType.Field.PRESSURE_ELEVATION] ?: single ?: 0.0
                Log.d(TAG, "STREAM ELEVATION: $elev m")
                calc.updateLiveElevation(elev)
            }
            DataType.Type.ELEVATION_GRADE -> {
                val grade = values[DataType.Field.ELEVATION_GRADE] ?: single ?: 0.0
                // Log.v(TAG, "STREAM GRADE: $grade %") // Opcional, puede generar mucho ruido
                calc.updateLiveGrade(grade)
            }
            DataType.Type.POWER -> {
                val pwr = single ?: 0.0
                calc.setPower(pwr)
            }
            DataType.Type.CADENCE -> {
                val cad = values[DataType.Field.CADENCE] ?: single ?: 0.0
                calc.setCadence(cad)
            }
            DataType.Type.HEART_RATE -> {
                val hr = values[DataType.Field.HEART_RATE] ?: single ?: 0.0
                calc.setHeartRate(hr)
            }
            DataType.Type.DISTANCE_TO_TOP -> {
                val dist = values[DataType.Field.DISTANCE_TO_TOP] ?: single ?: 0.0
                Log.d(TAG, "STREAM DIST_TO_TOP: $dist m")
                calc.updateClimbData(distToTop = dist)
            }
            DataType.Type.ELEVATION_TO_TOP -> {
                val elevTop = values[DataType.Field.ELEVATION_TO_TOP] ?: single ?: 0.0
                Log.d(TAG, "STREAM ELEV_TO_TOP: $elevTop m")
                calc.updateClimbData(elevToTop = elevTop)
            }
            // Bug conocido (no se corrige): FROM_BOTTOM lee las claves TO_TOP, como el campo 3D
            DataType.Type.DISTANCE_FROM_BOTTOM -> {
                val distBot = values[DataType.Field.DISTANCE_TO_TOP] ?: single ?: 0.0
                Log.d(TAG, "STREAM DIST_FROM_BOTTOM: $distBot m")
                calc.updateClimbData(distFromBottom = distBot)
            }
            DataType.Type.ELEVATION_FROM_BOTTOM -> {
                val elevBot = values[DataType.Field.ELEVATION_TO_TOP] ?: single ?: 0.0
                Log.d(TAG, "STREAM ELEV_FROM_BOTTOM: $elevBot m")
                calc.updateClimbData(elevFromBottom = elevBot)
            }
            DataType.Type.ELEVATION_REMAINING -> {
                val elevRem = values[DataType.Field.ASCENT_REMAINING] ?: single ?: 0.0
                Log.d(TAG, "STREAM ELEV_REMAINING: $elevRem m")
                calc.updateClimbData(elevRemaining = elevRem)
            }
        }
    }
}
