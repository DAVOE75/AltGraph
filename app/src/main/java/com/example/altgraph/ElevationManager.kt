package com.example.altgraph

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Gestor de almacenamiento y descarga de archivos de elevación MDT (.hgt)
 * Guarda las cuadrículas en /sdcard/altgraph/elevation o en el almacenamiento interno de la app.
 * Fuente: AWS Open Data Terrain Tiles (Skadi / SRTM global libre).
 */
class ElevationManager(private val context: Context) {

    private val baseDir: File by lazy {
        val sdcard = File("/sdcard/altgraph/elevation")
        if (sdcard.exists() || sdcard.mkdirs()) sdcard
        else File(context.filesDir, "elevation").apply { mkdirs() }
    }

    private val openFiles = ConcurrentHashMap<String, HgtFile?>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isFetching = AtomicBoolean(false)
    private var lastCheckMs = 0L

    fun getElevation(lat: Double, lng: Double): Double? {
        val name = HgtFile.nameFor(lat, lng)
        val hgt = openFiles.getOrPut(name) {
            val f = File(baseDir, name)
            if (f.isFile && f.length() > 0) {
                runCatching { HgtFile(f) }.getOrNull()
            } else null
        }
        return hgt?.getElevation(lat, lng)
    }

    fun hasCoverageFor(lat: Double, lng: Double): Boolean {
        val name = HgtFile.nameFor(lat, lng)
        return File(baseDir, name).isFile
    }

    fun getHeldFiles(): List<File> =
        baseDir.listFiles { f -> f.name.endsWith(".hgt") }?.sortedBy { it.name }.orEmpty()

    fun considerDownload(lat: Double, lng: Double, radiusKm: Int = 30) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCheckMs < 60_000L) return
        lastCheckMs = now

        val neededFiles = findNeededTileNames(lat, lng, radiusKm)
        val missing = neededFiles.filter { !File(baseDir, it).isFile && !isKnownMissing(it) }
        if (missing.isEmpty() || !isWifiConnected()) return

        if (!isFetching.compareAndSet(false, true)) return
        scope.launch {
            try {
                for (name in missing) {
                    fetchHgt(name)
                }
            } finally {
                isFetching.set(false)
                openFiles.clear()
            }
        }
    }

    private fun findNeededTileNames(lat: Double, lng: Double, radiusKm: Int): List<String> {
        val out = mutableListOf<String>()
        val lat0 = floor(lat).toInt()
        val lng0 = floor(lng).toInt()
        for (dLat in -1..1) {
            for (dLng in -1..1) {
                val s = (lat0 + dLat).toDouble()
                val w = (lng0 + dLng).toDouble()
                val nearestLat = lat.coerceIn(s, s + 1.0)
                val nearestLng = lng.coerceIn(w, w + 1.0)
                if (distanceMeters(lat, lng, nearestLat, nearestLng) <= radiusKm * 1000.0) {
                    out.add(HgtFile.nameFor(s + 0.5, w + 0.5))
                }
            }
        }
        return out.distinct()
    }

    private fun isKnownMissing(name: String): Boolean {
        val mark = File(baseDir, "$name.none")
        return mark.isFile && (System.currentTimeMillis() - mark.lastModified() < 30L * 86_400_000L)
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun fetchHgt(name: String) {
        val prefix = name.substring(0, 3) // p. ej. N38
        val urlStr = "https://elevation-tiles-prod.s3.amazonaws.com/skadi/$prefix/$name.gz"
        val partFile = File(baseDir, "$name.part")
        val targetFile = File(baseDir, name)

        try {
            Log.i("ElevationManager", "Descargando MDT: $urlStr")
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept-Encoding", "identity")

            when (conn.responseCode) {
                200 -> {
                    GZIPInputStream(conn.inputStream).use { input ->
                        partFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val n = sqrt(partFile.length() / 2.0).roundToInt()
                    if (n >= 2 && n.toLong() * n * 2 == partFile.length()) {
                        partFile.renameTo(targetFile)
                        Log.i("ElevationManager", "MDT guardado: $name (${targetFile.length() / 1024} KB)")
                    } else {
                        partFile.delete()
                    }
                }
                404, 403 -> {
                    // Zona de mar abierto o sin datos: marcar para no reintentar continuamente
                    File(baseDir, "$name.none").createNewFile()
                }
                else -> {
                    Log.w("ElevationManager", "Error HTTP ${conn.responseCode} al descargar $name")
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("ElevationManager", "Fallo descargando $name", e)
            partFile.delete()
        }
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(deltaPhi / 2) * kotlin.math.sin(deltaPhi / 2) +
                kotlin.math.cos(phi1) * kotlin.math.cos(phi2) *
                kotlin.math.sin(deltaLambda / 2) * kotlin.math.sin(deltaLambda / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    companion object {
        @Volatile
        private var INSTANCE: ElevationManager? = null

        fun getInstance(context: Context): ElevationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ElevationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
