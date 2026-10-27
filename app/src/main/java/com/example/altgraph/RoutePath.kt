package com.example.altgraph

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RoutePath private constructor(
    private val points: List<Pair<Double, Double>>,
    private val cumulative: DoubleArray,
) {
    private val total: Double get() = cumulative.last()

    fun pointAt(distance: Double): Pair<Double, Double> {
        val d = distance.coerceIn(0.0, total)
        val i = segmentIndex(d)
        return interpolate(i, d)
    }

    fun subPath(from: Double, to: Double): List<Pair<Double, Double>> {
        val f = from.coerceIn(0.0, total)
        val t = to.coerceIn(0.0, total)
        if (t <= f) return listOf(pointAt(f))
        val result = mutableListOf(pointAt(f))
        var i = segmentIndex(f) + 1
        while (i < points.size && cumulative[i] < t) {
            result.add(points[i])
            i++
        }
        result.add(pointAt(t))
        return result
    }

    // Puntos por los que la ruta pasa más de una vez (misma carretera de ida y vuelta o repetida)
    private val shared: BooleanArray by lazy { computeShared() }

    private fun computeShared(): BooleanArray {
        val result = BooleanArray(points.size)
        val cell = 0.0002
        val grid = HashMap<Long, MutableList<Int>>()
        fun key(la: Int, lo: Int) = la.toLong() * 4_000_000L + lo.toLong()
        for (i in points.indices) {
            val la = Math.floor(points[i].first / cell).toInt()
            val lo = Math.floor(points[i].second / cell).toInt()
            grid.getOrPut(key(la, lo)) { mutableListOf() }.add(i)
        }
        for (i in points.indices) {
            val (lat, lng) = points[i]
            val mPerLng = 111320.0 * cos(Math.toRadians(lat))
            val la = Math.floor(lat / cell).toInt()
            val lo = Math.floor(lng / cell).toInt()
            loop@ for (dla in -1..1) for (dlo in -1..1) {
                val list = grid[key(la + dla, lo + dlo)] ?: continue
                for (j in list) {
                    if (Math.abs(cumulative[j] - cumulative[i]) < SHARED_MIN_ROUTE_GAP_M) continue
                    val dy = (points[j].first - lat) * 111320.0
                    val dx = (points[j].second - lng) * mPerLng
                    if (dx * dx + dy * dy < SHARED_RADIUS_M * SHARED_RADIUS_M) { result[i] = true; break@loop }
                }
            }
        }
        return result
    }

    /**
     * Igual que subPath, pero los puntos de carretera repetida se desplazan offsetMeters hacia la derecha del sentido
     * de marcha: la ida y la vuelta por la misma carretera quedan una a cada lado en vez de pintarse una encima de otra.
     */
    fun subPathOffset(from: Double, to: Double, offsetMeters: Double): List<Pair<Double, Double>> {
        val f = from.coerceIn(0.0, total)
        val t = to.coerceIn(0.0, total)
        if (t <= f) return listOf(pointAt(f))
        val pts = mutableListOf(pointAt(f))
        val flags = mutableListOf(shared[segmentIndex(f)])
        var i = segmentIndex(f) + 1
        while (i < points.size && cumulative[i] < t) {
            pts.add(points[i])
            flags.add(shared[i])
            i++
        }
        pts.add(pointAt(t))
        flags.add(flags.last())
        if (pts.size < 2 || flags.none { it }) return pts

        // El desplazamiento entra y sale con suavidad (media con los vecinos) para no dejar escalones
        val weight = DoubleArray(pts.size) { k ->
            val a = if (flags[k]) 1.0 else 0.0
            val b = if (k > 0 && flags[k - 1]) 1.0 else 0.0
            val c = if (k < pts.size - 1 && flags[k + 1]) 1.0 else 0.0
            (a * 2 + b + c) / 4.0
        }
        return pts.mapIndexed { k, p ->
            val prev = pts[maxOf(k - 1, 0)]
            val next = pts[minOf(k + 1, pts.size - 1)]
            val mPerLng = 111320.0 * cos(Math.toRadians(p.first))
            val n = (next.first - prev.first) * 111320.0
            val e = (next.second - prev.second) * mPerLng
            val len = sqrt(n * n + e * e)
            if (len < 0.01 || weight[k] == 0.0) p
            else {
                val off = offsetMeters * weight[k]
                val rightN = -e / len
                val rightE = n / len
                (p.first + rightN * off / 111320.0) to (p.second + rightE * off / mPerLng)
            }
        }
    }

    private fun segmentIndex(d: Double): Int {
        var lo = 0
        var hi = points.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (cumulative[mid] <= d) lo = mid else hi = mid
        }
        return lo
    }

    private fun interpolate(i: Int, d: Double): Pair<Double, Double> {
        if (i >= points.size - 1) return points.last()
        val span = cumulative[i + 1] - cumulative[i]
        if (span <= 0.0) return points[i]
        val t = (d - cumulative[i]) / span
        val (lat1, lng1) = points[i]
        val (lat2, lng2) = points[i + 1]
        return lat1 + t * (lat2 - lat1) to lng1 + t * (lng2 - lng1)
    }

    companion object {
        // Dos puntos de la ruta cuentan como "misma carretera" si están a menos de 12 m y a más de 60 m de ruta de distancia
        private const val SHARED_RADIUS_M = 12.0
        private const val SHARED_MIN_ROUTE_GAP_M = 60.0
        private const val EARTH_RADIUS = 6371000.0
        private const val ROUTE_POLYLINE_FACTOR = 1e5

        fun fromPolyline(encoded: String?, routeDistance: Double?): RoutePath? {
            if (encoded == null) return null
            val points = PolylineCodec.decode(encoded, ROUTE_POLYLINE_FACTOR)
            if (points.size < 2) return null
            val cumulative = DoubleArray(points.size)
            for (i in 1 until points.size) {
                cumulative[i] = cumulative[i - 1] + haversine(points[i - 1], points[i])
            }
            val geometricTotal = cumulative.last()
            if (geometricTotal <= 0.0) return null
            val safeRouteDistance = routeDistance ?: -1.0
            if (safeRouteDistance > 0.0) {
                val scale = safeRouteDistance / geometricTotal
                for (i in cumulative.indices) cumulative[i] *= scale
            }
            return RoutePath(points, cumulative)
        }

        private fun haversine(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
            val la1 = Math.toRadians(p1.first)
            val la2 = Math.toRadians(p2.first)
            val dLat = Math.toRadians(p2.first - p1.first)
            val dLng = Math.toRadians(p2.second - p1.second)
            val a = sin(dLat / 2) * sin(dLat / 2) + cos(la1) * cos(la2) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * EARTH_RADIUS * asin(sqrt(a))
        }
    }
}
