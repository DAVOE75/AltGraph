package com.example.altgraph

import android.content.Context
import java.io.File

/**
 * Prueba del horizonte con una salida grabada: reproduce un GPX punto a punto, predice el camino por delante
 * en cada punto y lo compara con lo que de verdad se recorrió. Sirve para medir con datos reales cuánto acierta.
 * Entrada: /sdcard/altgraph/replay.gpx. Salida: /sdcard/altgraph/replay_report.txt
 */
object RoadHorizonReplay {

    private class Fix(val p: GeoPoint, val ele: Double?)

    private val TRKPT = Regex("<trkpt[^>]*?lat=\"([-0-9.]+)\"[^>]*?lon=\"([-0-9.]+)\"[^>]*?>(.*?)</trkpt>", RegexOption.DOT_MATCHES_ALL)
    private val TRKPT_LON_FIRST = Regex("<trkpt[^>]*?lon=\"([-0-9.]+)\"[^>]*?lat=\"([-0-9.]+)\"[^>]*?>(.*?)</trkpt>", RegexOption.DOT_MATCHES_ALL)
    private val ELE = Regex("<ele>([-0-9.]+)</ele>")

    fun gpxFile(): File = File("/sdcard/altgraph/replay.gpx")

    private fun parse(text: String): List<Fix> {
        val out = ArrayList<Fix>()
        for (m in TRKPT.findAll(text)) {
            val ele = ELE.find(m.groupValues[3])?.groupValues?.get(1)?.toDoubleOrNull()
            out += Fix(GeoPoint(m.groupValues[1].toDouble(), m.groupValues[2].toDouble()), ele)
        }
        if (out.isEmpty()) {
            for (m in TRKPT_LON_FIRST.findAll(text)) {
                val ele = ELE.find(m.groupValues[3])?.groupValues?.get(1)?.toDoubleOrNull()
                out += Fix(GeoPoint(m.groupValues[2].toDouble(), m.groupValues[1].toDouble()), ele)
            }
        }
        return out
    }

    /** Devuelve el informe en texto, o null si no hay GPX. */
    fun run(context: Context, stepM: Double = 25.0, horizonM: Double = 500.0): String? {
        val file = gpxFile()
        if (!file.isFile) return null
        val raw = parse(file.readText())
        if (raw.size < 10) return "GPX demasiado corto / too short (${raw.size} pts)"

        // Se adelgaza el recorrido a un punto cada ~stepM metros
        val fixes = ArrayList<Fix>()
        val cum = ArrayList<Double>()
        var total = 0.0
        for (f in raw) {
            if (fixes.isEmpty()) { fixes += f; cum += 0.0; continue }
            val d = fixes.last().p.distanceTo(f.p)
            if (d >= stepM) { total += d; fixes += f; cum += total }
        }
        val predictor = RoadHorizonPredictor(context)
        val ahead = Math.ceil(horizonM / stepM).toInt()

        var tested = 0; var detected = 0; var good = 0
        var matchSum = 0.0
        val err250 = ArrayList<Double>(); val err500 = ArrayList<Double>()
        val hasEle = fixes.count { it.ele != null } > fixes.size / 2

        var i = 2
        while (i < fixes.size - ahead) {
            tested++
            val cur = fixes[i].p
            val back = fixes[i - 2].p
            val heading = back.bearingTo(cur)
            val res = predictor.predictHorizon(cur.lat, cur.lng, heading, horizonM, stepM)
            if (res != null && res.coordinates.size >= 3) {
                detected++
                var ok = 0
                val n = minOf(res.coordinates.size, ahead + 1)
                for (k in 0 until n) {
                    val c = GeoPoint(res.coordinates[k].first, res.coordinates[k].second)
                    var best = Double.MAX_VALUE
                    for (j in i..minOf(fixes.size - 1, i + ahead + 2)) {
                        val d = c.distanceTo(fixes[j].p)
                        if (d < best) best = d
                    }
                    if (best <= 35.0) ok++
                }
                val frac = ok.toDouble() / n
                matchSum += frac
                if (frac >= 0.9) good++
                if (hasEle) {
                    val e0a = fixes[i].ele; val e0p = res.elevations[0]
                    if (e0a != null) {
                        for ((kk, list) in listOf(10 to err250, 20 to err500)) {
                            val fa = fixes.getOrNull(i + kk)?.ele
                            if (fa != null && kk < res.elevations.size) {
                                list += Math.abs((res.elevations[kk] - e0p) - (fa - e0a))
                            }
                        }
                    }
                }
            }
            i += 2
        }

        fun median(l: List<Double>) = if (l.isEmpty()) Double.NaN else l.sorted()[l.size / 2]
        val sb = StringBuilder()
        sb.appendLine("AltGraph Road Horizon replay")
        sb.appendLine("GPX points: ${raw.size}, used: ${fixes.size}, route: ${"%.1f".format(total / 1000.0)} km")
        sb.appendLine("Tested positions: $tested")
        sb.appendLine("Prediction available: ${if (tested == 0) 0 else 100 * detected / tested} %")
        sb.appendLine("Path matches the real track (avg): ${if (detected == 0) 0 else Math.round(100 * matchSum / detected)} %")
        sb.appendLine("Positions with >= 90 % path match: ${if (detected == 0) 0 else 100 * good / detected} %")
        if (hasEle) {
            sb.appendLine("Median relative height error at 250 m: ${"%.1f".format(median(err250))} m")
            sb.appendLine("Median relative height error at 500 m: ${"%.1f".format(median(err500))} m")
        } else sb.appendLine("GPX without elevations: height error not measured")
        val report = sb.toString()
        runCatching { File("/sdcard/altgraph/replay_report.txt").writeText(report) }
        return report
    }
}
