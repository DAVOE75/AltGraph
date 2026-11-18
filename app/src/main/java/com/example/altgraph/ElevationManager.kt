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
import java.util.Optional
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

    private val openFiles = ConcurrentHashMap<String, Optional<HgtFile>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isFetching = AtomicBoolean(false)
    private var lastCheckMs = 0L

    fun getElevation(lat: Double, lng: Double): Double? {
        val name = HgtFile.nameFor(lat, lng)
        val cached = openFiles[name]
        val hgt = if (cached != null) cached.orElse(null) else {
            val f = File(baseDir, name)
            val opened = if (f.isFile && f.length() > 0) runCatching { HgtFile(f) }.getOrNull() else null
            // Solo se recuerda el archivo si existe; si falta se vuelve a mirar (puede descargarse después)
            if (opened != null) openFiles[name] = Optional.of(opened)
            opened
        }
        return try { hgt?.getElevation(lat, lng) } catch (e: Exception) { null }
    }

    fun hasCoverageFor(lat: Double, lng: Double): Boolean {
        val name = HgtFile.nameFor(lat, lng)
        return File(baseDir, name).isFile
    }

    fun getHeldFiles(): List<File> =
        baseDir.listFiles { f -> f.name.endsWith(".hgt") }?.sortedBy { it.name }.orEmpty()

    fun considerDownload(lat: Double, lng: Double, radiusKm: Int = AppPreferences.getInstance(context).horizonDownloadRadiusKm) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCheckMs < 60_000L) return
        lastCheckMs = now
        startDownload(lat, lng, radiusKm, null)
    }

    /** Descarga inmediata desde el panel. Devuelve false si no hay red permitida o ya hay una descarga en curso. */
    fun downloadNow(lat: Double, lng: Double, onDone: (Int) -> Unit): Int {
        val radius = AppPreferences.getInstance(context).horizonDownloadRadiusKm
        if (!isNetworkAllowed()) return DL_NO_NETWORK
        return if (startDownload(lat, lng, radius, onDone)) DL_STARTED else DL_NOTHING
    }

    private fun startDownload(lat: Double, lng: Double, radiusKm: Int, onDone: ((Int) -> Unit)?): Boolean {
        val neededFiles = findNeededTileNames(lat, lng, radiusKm)
        val missing = neededFiles.filter { !File(baseDir, it).isFile && (onDone != null || !isKnownMissing(it)) }
        if (missing.isEmpty() || !isNetworkAllowed()) return false

        if (!isFetching.compareAndSet(false, true)) return false
        scope.launch {
            var ok = 0
            try {
                for (name in missing) {
                    if (fetchHgt(name)) ok++
                }
            } finally {
                isFetching.set(false)
                openFiles.clear()
                onDone?.invoke(ok)
            }
        }
        return true
    }

    fun isDownloading(): Boolean = isFetching.get()

    /** true si ya están todas las cuadrículas necesarias alrededor de la posición (o no existen datos para ellas). */
    fun isUpToDate(lat: Double, lng: Double): Boolean {
        val radius = AppPreferences.getInstance(context).horizonDownloadRadiusKm
        return findNeededTileNames(lat, lng, radius).all { File(baseDir, it).isFile || isKnownMissing(it) }
    }

    fun heldSummary(): Pair<Int, Long> {
        val files = getHeldFiles()
        return files.size to files.sumOf { it.length() }
    }

    private fun findNeededTileNames(lat: Double, lng: Double, radiusKm: Int): List<String> {
        val out = mutableListOf<String>()
        val lat0 = floor(lat).toInt()
        val lng0 = floor(lng).toInt()
        val span = Math.ceil(radiusKm / 70.0).toInt().coerceIn(1, 4)
        for (dLat in -span..span) {
            for (dLng in -span..span) {
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

    private fun isNetworkAllowed(): Boolean {
        if (isWifiConnected()) return true
        if (!AppPreferences.getInstance(context).horizonMobileData) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun fetchHgt(name: String): Boolean {
        var saved = false
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
                        saved = true
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
        return saved
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
        const val DL_STARTED = 0
        const val DL_NO_NETWORK = 1
        const val DL_NOTHING = 2

        @Volatile
        private var INSTANCE: ElevationManager? = null

        fun getInstance(context: Context): ElevationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ElevationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
