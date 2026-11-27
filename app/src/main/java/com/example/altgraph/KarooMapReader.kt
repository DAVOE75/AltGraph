package com.example.altgraph

import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.map.reader.MapFile
import java.io.File
import kotlin.math.*

/**
 * Coordenada geográfica básica con utilidades de geodesia.
 */
data class GeoPoint(val lat: Double, val lng: Double) {
    fun distanceTo(other: GeoPoint): Double {
        val r = 6371000.0
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(other.lat)
        val dPhi = Math.toRadians(other.lat - lat)
        val dLam = Math.toRadians(other.lng - lng)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLam / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun bearingTo(other: GeoPoint): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(other.lat)
        val dLam = Math.toRadians(other.lng - lng)
        val y = sin(dLam) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLam)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }
}

/**
 * Tramo de carretera extraído del mapa vectorial de Karoo.
 */
class RoadSegment(
    val points: List<GeoPoint>,
    val name: String?,
    val ref: String?,
    val kind: String,
    val oneway: Boolean,
    val isRoundabout: Boolean,
    val isBridge: Boolean,
    val isTunnel: Boolean
) {
    val lengthMeters: Double by lazy {
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += points[i].distanceTo(points[i + 1])
        }
        total
    }
}

/**
 * Lector de mapas vectoriales Mapsforge (.map) del Karoo ubicados en /sdcard/offline/maps.
 */
class KarooMapReader(private val mapFiles: List<File>, private val zoomLevel: Int = 14) {

    private val openMaps: List<Pair<String, MapFile>> = mapFiles.mapNotNull { f ->
        runCatching { f.name to MapFile(f) }.getOrNull()
    }

    private val tileCache = object : LinkedHashMap<Long, List<RoadSegment>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, List<RoadSegment>>?) = size > 48
    }

    private val n = 1 shl zoomLevel

    private fun tileX(lng: Double) = floor((lng + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    private fun tileY(lat: Double): Int {
        val r = Math.toRadians(lat.coerceIn(-85.0, 85.0))
        return floor((1.0 - ln(tan(r) + 1.0 / cos(r)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
    }

    @Synchronized
    fun getRoadsNear(center: GeoPoint, radiusMeters: Double = 600.0): List<RoadSegment> {
        val dLat = radiusMeters / 111320.0
        val dLng = dLat / cos(Math.toRadians(center.lat)).coerceAtLeast(0.01)

        val roads = mutableListOf<RoadSegment>()
        val minX = tileX(center.lng - dLng)
        val maxX = tileX(center.lng + dLng)
        val minY = tileY(center.lat + dLat)
        val maxY = tileY(center.lat - dLat)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                roads.addAll(loadTileRoads(x, y))
            }
        }
        return roads
    }

    private fun loadTileRoads(x: Int, y: Int): List<RoadSegment> {
        val key = x.toLong() * n + y
        tileCache[key]?.let { return it }

        val tileRoads = mutableListOf<RoadSegment>()
        val tile = Tile(x, y, zoomLevel.toByte(), 256)

        for ((_, map) in openMaps) {
            val data = runCatching {
                if (map.supportsTile(tile)) map.readMapData(tile) else null
            }.getOrNull() ?: continue

            for (way in data.ways) {
                for (coords in way.latLongs) {
                    if (coords.size < 2) continue
                    val seg = parseWay(way.tags, coords.map { GeoPoint(it.latitude, it.longitude) })
                    if (seg != null) {
                        tileRoads.add(seg)
                    }
                }
            }
            if (tileRoads.isNotEmpty()) break
        }

        tileCache[key] = tileRoads
        return tileRoads
    }

    private fun parseWay(tags: List<Tag>, points: List<GeoPoint>): RoadSegment? {
        var kind: String? = null
        var name: String? = null
        var ref: String? = null
        var oneway = false
        var reversed = false
        var roundabout = false
        var bridge = false
        var tunnel = false

        var onewayNo = false
        var roundaboutJunction = false
        for (t in tags) {
            when (t.key) {
                "highway" -> kind = t.value
                "name" -> name = t.value?.takeIf { it.isNotBlank() }
                "ref" -> ref = t.value?.takeIf { it.isNotBlank() }
                "oneway" -> when (t.value) {
                    "yes", "true", "1" -> oneway = true
                    "-1" -> { oneway = true; reversed = true }
                    "no", "false", "0" -> onewayNo = true
                }
                "junction" -> {
                    roundabout = t.value == "roundabout" || t.value == "circular"
                    roundaboutJunction = t.value == "roundabout"
                }
                "bridge" -> bridge = t.value != "no"
                "tunnel" -> tunnel = t.value != "no"
                "area" -> if (t.value == "yes") return null
            }
        }
        // Una rotonda es de sentido único aunque el mapa no lo etiquete (regla de OpenStreetMap): sin esto, en el cruce entre dos vías
        // del anillo el trazado podía seguir en contra de la circulación
        if (roundaboutJunction && !onewayNo) oneway = true

        if (kind == null || kind !in RIDEABLE_HIGHWAYS) return null
        val finalPts = if (reversed) points.reversed() else points
        return RoadSegment(finalPts, name, ref, kind, oneway, roundabout, bridge, tunnel)
    }

    fun close() {
        openMaps.forEach { runCatching { it.second.close() } }
        tileCache.clear()
    }

    companion object {
        val RIDEABLE_HIGHWAYS = setOf(
            "primary", "secondary", "tertiary", "unclassified", "residential",
            "primary_link", "secondary_link", "tertiary_link", "living_street",
            "cycleway", "path", "track", "service", "road"
        )
    }
}
