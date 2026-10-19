package com.example.altgraph

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Representa un puerto de montaña guardado de forma persistente.
 * Puede estar asociado a una ruta concreta (routeKey) o en la librería global
 * para ser detectado en cualquier ruta cuyas coordenadas coincidan con el inicio y fin.
 */
data class StoredClimb(
    val id: String,
    val name: String,
    val category: String,
    val startLat: Double,
    val startLng: Double,
    val startElev: Double,
    val endLat: Double,
    val endLng: Double,
    val endElev: Double,
    val lengthMeters: Double,
    val avgGrade: Double = 0.0,
    val routeKey: String? = null // null para puertos del catálogo global
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("category", category)
        json.put("startLat", startLat)
        json.put("startLng", startLng)
        json.put("startElev", startElev)
        json.put("endLat", endLat)
        json.put("endLng", endLng)
        json.put("endElev", endElev)
        json.put("lengthMeters", lengthMeters)
        json.put("avgGrade", avgGrade)
        if (routeKey != null) {
            json.put("routeKey", routeKey)
        }
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): StoredClimb? {
            return try {
                StoredClimb(
                    id = json.optString("id", "${json.optDouble("startLat")}_${json.optDouble("endLat")}"),
                    name = json.optString("name", "Puerto"),
                    category = json.optString("category", "3C"),
                    startLat = json.getDouble("startLat"),
                    startLng = json.getDouble("startLng"),
                    startElev = json.optDouble("startElev", 0.0),
                    endLat = json.getDouble("endLat"),
                    endLng = json.getDouble("endLng"),
                    endElev = json.optDouble("endElev", 0.0),
                    lengthMeters = json.optDouble("lengthMeters", 0.0),
                    avgGrade = json.optDouble("avgGrade", 0.0),
                    routeKey = if (json.has("routeKey")) json.optString("routeKey") else null
                )
            } catch (e: Exception) {
                Log.e("CustomClimbStorage", "Error parseando StoredClimb: ${e.message}")
                null
            }
        }
    }
}

/**
 * Gestor de persistencia de puertos de montaña definidos por el usuario:
 * 1. A nivel de Ruta (se guarda con clave de nombre/ID de la ruta).
 * 2. A nivel Global (catálogo GPS global estilo Hammerhead Dashboard / Karoo CLIMBER).
 */
object CustomClimbStorage {
    private const val PREFS_NAME = "altgraph_custom_climbs_storage"
    private const val KEY_GLOBAL_CLIMBS = "global_climbs_catalog"
    private const val PREFIX_ROUTE_CLIMB = "route_climbs_"
    private const val GPS_MATCH_THRESHOLD_METERS = 120.0 // Margen de proximidad GPS para reconocer inicio y cima

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==========================================
    // PERSISTENCIA POR RUTA
    // ==========================================

    @Synchronized
    fun saveRouteClimbs(context: Context, routeKey: String, climbs: List<StoredClimb>) {
        if (routeKey.isBlank()) return
        try {
            val array = JSONArray()
            climbs.forEach { array.put(it.toJsonObject()) }
            getPrefs(context).edit().putString("$PREFIX_ROUTE_CLIMB$routeKey", array.toString()).apply()
            Log.d("CustomClimbStorage", "Guardados ${climbs.size} puertos para la ruta '$routeKey'")
        } catch (e: Exception) {
            Log.e("CustomClimbStorage", "Error al guardar puertos de ruta: ${e.message}")
        }
    }

    @Synchronized
    fun getRouteClimbs(context: Context, routeKey: String): List<StoredClimb> {
        if (routeKey.isBlank()) return emptyList()
        val jsonStr = getPrefs(context).getString("$PREFIX_ROUTE_CLIMB$routeKey", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<StoredClimb>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                StoredClimb.fromJsonObject(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.e("CustomClimbStorage", "Error leyendo puertos de ruta: ${e.message}")
            emptyList()
        }
    }

    // ==========================================
    // CATÁLOGO GLOBAL (GPS)
    // ==========================================

    @Synchronized
    fun saveGlobalClimbs(context: Context, climbs: List<StoredClimb>) {
        try {
            val existing = getGlobalClimbs(context).toMutableList()
            for (newClimb in climbs) {
                // Comprobar si ya existe un puerto global idéntico o muy cercano (<100m entre inicios y cimas)
                val idx = existing.indexOfFirst {
                    it.id == newClimb.id ||
                    (haversineDistance(it.startLat, it.startLng, newClimb.startLat, newClimb.startLng) < 100.0 &&
                     haversineDistance(it.endLat, it.endLng, newClimb.endLat, newClimb.endLng) < 100.0)
                }
                if (idx >= 0) {
                    existing[idx] = newClimb.copy(routeKey = null)
                } else {
                    existing.add(newClimb.copy(routeKey = null))
                }
            }

            val array = JSONArray()
            existing.forEach { array.put(it.toJsonObject()) }
            getPrefs(context).edit().putString(KEY_GLOBAL_CLIMBS, array.toString()).apply()
            Log.d("CustomClimbStorage", "Catálogo global actualizado. Total puertos: ${existing.size}")
        } catch (e: Exception) {
            Log.e("CustomClimbStorage", "Error guardando catálogo global: ${e.message}")
        }
    }

    @Synchronized
    fun getGlobalClimbs(context: Context): List<StoredClimb> {
        val jsonStr = getPrefs(context).getString(KEY_GLOBAL_CLIMBS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<StoredClimb>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                StoredClimb.fromJsonObject(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.e("CustomClimbStorage", "Error leyendo catálogo global: ${e.message}")
            emptyList()
        }
    }

    /**
     * Compara los puntos GPS de la ruta actual contra el catálogo global y el almacén de ruta.
     * Si la ruta pasa por el inicio y luego por la cima de alguno de los puertos guardados,
     * genera los objetos RouteClimb correspondientes con `isCustom = true`.
     */
    fun matchClimbsForRoute(
        context: Context,
        routeKey: String?,
        routePoints: List<RoutePoint>
    ): List<RouteClimb> {
        if (routePoints.isEmpty()) return emptyList()

        val candidateClimbs = mutableListOf<StoredClimb>()
        
        // 1. Añadir los de la ruta específica si existe clave
        if (!routeKey.isNullOrBlank()) {
            candidateClimbs.addAll(getRouteClimbs(context, routeKey))
        }

        // 2. Añadir los del catálogo global (evitando duplicar IDs)
        val globalClimbs = getGlobalClimbs(context)
        for (gc in globalClimbs) {
            if (candidateClimbs.none { it.id == gc.id || it.name.equals(gc.name, ignoreCase = true) }) {
                candidateClimbs.add(gc)
            }
        }

        if (candidateClimbs.isEmpty()) return emptyList()

        val matchedRouteClimbs = mutableListOf<RouteClimb>()

        for (candidate in candidateClimbs) {
            // Buscar punto más cercano al inicio del puerto
            var bestStartIndex = -1
            var bestStartDist = Double.MAX_VALUE

            for (i in routePoints.indices) {
                val pt = routePoints[i]
                val d = haversineDistance(pt.latitude, pt.longitude, candidate.startLat, candidate.startLng)
                if (d < bestStartDist) {
                    bestStartDist = d
                    bestStartIndex = i
                }
            }

            if (bestStartIndex < 0 || bestStartDist > GPS_MATCH_THRESHOLD_METERS) {
                continue // El inicio no está en la ruta
            }

            // Buscar la cima después del punto de inicio
            var bestEndIndex = -1
            var bestEndDist = Double.MAX_VALUE

            for (i in (bestStartIndex + 1) until routePoints.size) {
                val pt = routePoints[i]
                val d = haversineDistance(pt.latitude, pt.longitude, candidate.endLat, candidate.endLng)
                if (d < bestEndDist) {
                    bestEndDist = d
                    bestEndIndex = i
                }
            }

            if (bestEndIndex > bestStartIndex && bestEndDist <= GPS_MATCH_THRESHOLD_METERS) {
                val startPt = routePoints[bestStartIndex]
                val endPt = routePoints[bestEndIndex]
                val length = endPt.distance - startPt.distance

                if (length >= 300.0) { // Al menos 300 metros
                    val elevGain = (endPt.elevation - startPt.elevation).coerceAtLeast(0.0)
                    val grade = if (length > 0) (elevGain / length) * 100.0 else 0.0

                    matchedRouteClimbs.add(
                        RouteClimb(
                            startDistance = startPt.distance,
                            endDistance = endPt.distance,
                            length = length,
                            totalElevation = elevGain,
                            avgGrade = grade,
                            category = candidate.category,
                            name = candidate.name,
                            isCustom = true
                        )
                    )
                    Log.d("CustomClimbStorage", "Coincidencia GPS para puerto '${candidate.name}' [${candidate.category}]: km ${startPt.distance / 1000.0} -> km ${endPt.distance / 1000.0}")
                }
            }
        }

        return matchedRouteClimbs
    }

    /**
     * Cálculo de distancia Haversine en metros entre dos coordenadas.
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
