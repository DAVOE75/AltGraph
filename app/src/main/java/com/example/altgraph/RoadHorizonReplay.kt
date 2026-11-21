package com.example.altgraph

import android.content.Context
import java.io.File

/**
 * Prueba del horizonte con una salida grabada: reproduce un GPX punto a punto, predice el camino por delante
 * en cada punto y lo compara con lo que de verdad se recorrió. Sirve para medir con datos reales cuánto acierta.
 * Entrada: Android/data/com.example.altgraph/files/replay.gpx. Salida: replay_report.txt en la misma carpeta
 */
object RoadHorizonReplay {

    private class Fix(val p: GeoPoint, val ele: Double?)

    private val TRKPT = Regex("<trkpt[^>]*?lat=\"([-0-9.]+)\"[^>]*?lon=\"([-0-9.]+)\"[^>]*?>(.*?)</trkpt>", RegexOption.DOT_MATCHES_ALL)
    private val TRKPT_LON_FIRST = Regex("<trkpt[^>]*?lon=\"([-0-9.]+)\"[^>]*?lat=\"([-0-9.]+)\"[^>]*?>(.*?)</trkpt>", RegexOption.DOT_MATCHES_ALL)
    private val ELE = Regex("<ele>([-0-9.]+)</ele>")

    fun gpxFile(context: Context): File {
        val own = File(context.getExternalFilesDir(null), "replay.gpx")
        if (own.isFile && own.canRead()) return own
        val legacy = File("/sdcard/altgraph/replay.gpx")
        return if (legacy.isFile && legacy.canRead()) legacy else own
    }

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

    /** Reproduce un tramo del GPX fix a fix (1 Hz, sin adelgazar) y registra en logcat el rumbo y la vía elegida en cada lectura. */
    fun runLive(context: Context, from: Int, to: Int) {
        val file = gpxFile(context)
        if (!file.isFile) return
        val raw = parse(file.readText())
        val predictor = RoadHorizonPredictor(context).also { it.ignoreMissingTerrain = true }
        var wrong = 0; var n = 0
        for (i in maxOf(3, from) until minOf(to, raw.size - 10)) {
            val cur = raw[i].p
            predictor.recordFix(cur.lat, cur.lng)
            if (i % 2 != 0) continue
            val gpsHeading = raw[i - 3].p.bearingTo(cur)
            val res = predictor.predictHorizon(cur.lat, cur.lng, gpsHeading, 300.0, 25.0, 1.7)
            val truth = cur.bearingTo(raw[i + 8].p)
            var predDir = Double.NaN
            if (res != null && res.coordinates.size >= 5) {
                predDir = GeoPoint(res.coordinates[0].first, res.coordinates[0].second)
                    .bearingTo(GeoPoint(res.coordinates[4].first, res.coordinates[4].second))
            }
            val diff = if (predDir.isNaN()) -1.0 else Math.abs((predDir - truth + 540.0) % 360.0 - 180.0)
            n++; if (diff > 90.0) wrong++
            android.util.Log.i("ReplayLive", "i=$i gps=${gpsHeading.toInt()} truth=${truth.toInt()} pred=${if (predDir.isNaN()) -1 else predDir.toInt()} diff=${diff.toInt()} road=${predictor.lastRoadName} kind=${predictor.lastRoadKind} snap=${predictor.lastSnapDist.toInt()}${if (diff > 90.0) " REVERSED" else ""}")
        }
        android.util.Log.i("ReplayLive", "done reversed=$wrong of $n")
    }

    /** Devuelve el informe en texto, o null si no hay GPX. */
    fun run(context: Context, stepM: Double = 25.0, horizonM: Double = 500.0): String? {
        val file = gpxFile(context)
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
        val predictor = RoadHorizonPredictor(context).also { it.ignoreMissingTerrain = true }
        val ahead = Math.ceil(horizonM / stepM).toInt()

        var tested = 0; var detected = 0; var good = 0
        val fails = HashMap<String, Int>()
        var wrongNow = 0; var wrongLater = 0; var shortPaths = 0
        var matchSum = 0.0
        val err250 = ArrayList<Double>(); val err500 = ArrayList<Double>()
        val hasEle = fixes.count { it.ele != null } > fixes.size / 2

        var i = 2
        while (i < fixes.size - ahead) {
            tested++
            val cur = fixes[i].p
            val back = fixes[i - 2].p
            val heading = back.bearingTo(cur)
            predictor.recordFix(fixes[i - 1].p.lat, fixes[i - 1].p.lng)
            predictor.recordFix(cur.lat, cur.lng)
            val res = predictor.predictHorizon(cur.lat, cur.lng, heading, horizonM, stepM)
            if (res == null) fails.merge(predictor.lastFail.ifEmpty { predictor.lastStatus.name }, 1, Int::plus)
            if (res != null && res.coordinates.size >= 3) {
                detected++
                var ok = 0
                var firstBad = -1
                val n = minOf(res.coordinates.size, ahead + 1)
                for (k in 0 until n) {
                    val c = GeoPoint(res.coordinates[k].first, res.coordinates[k].second)
                    var best = Double.MAX_VALUE
                    for (j in i..minOf(fixes.size - 1, i + ahead + 2)) {
                        val d = c.distanceTo(fixes[j].p)
                        if (d < best) best = d
                    }
                    if (best <= 35.0) ok++ else if (firstBad < 0) firstBad = k
                }
                val frac = ok.toDouble() / n
                matchSum += frac
                if (frac >= 0.9) good++
                else if (firstBad in 0..4) { wrongNow++; if (wrongNow <= 40) android.util.Log.i("ReplayBad", "i=$i pos=${"%.5f".format(cur.lat)},${"%.5f".format(cur.lng)} hd=${heading.toInt()} road=${predictor.lastRoadName} kind=${predictor.lastRoadKind} snap=${predictor.lastSnapDist.toInt()} first=${"%.5f".format(res.coordinates[0].first)},${"%.5f".format(res.coordinates[0].second)} ok=$ok/$n firstBad=$firstBad") } // la calle en la que estás ya está mal (primeros 100 m)
                else wrongLater++ // la calle es buena pero el cruce se adivinó mal
                if (res.coordinates.size < ahead + 1) shortPaths++
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
        for ((k, v) in fails) sb.appendLine("  no prediction: $k = $v")
        sb.appendLine("Path matches the real track (avg): ${if (detected == 0) 0 else Math.round(100 * matchSum / detected)} %")
        sb.appendLine("Positions with >= 90 % path match: ${if (detected == 0) 0 else 100 * good / detected} %")
        sb.appendLine("Failures - road under you wrong (first 100 m): ${if (detected == 0) 0 else 100 * wrongNow / detected} %")
        sb.appendLine("Failures - junction guessed wrong (later): ${if (detected == 0) 0 else 100 * wrongLater / detected} %")
        sb.appendLine("Predictions shorter than asked (dead end): ${if (detected == 0) 0 else 100 * shortPaths / detected} %")
        if (hasEle) {
            sb.appendLine("Median relative height error at 250 m: ${"%.1f".format(median(err250))} m")
            sb.appendLine("Median relative height error at 500 m: ${"%.1f".format(median(err500))} m")
        } else sb.appendLine("GPX without elevations: height error not measured")
        val report = sb.toString()
        runCatching { File(context.getExternalFilesDir(null), "replay_report.txt").writeText(report) }
        return report
    }
}
