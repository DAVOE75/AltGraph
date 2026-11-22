package com.example.altgraph

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.text.Collator
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

/**
 * Paquetes de terreno de 5 m por provincia (obra derivada del MDT05 del CNIG, CC-BY 4.0).
 * La lista de provincias sale de un índice JSON; cada paquete es un .zip de trozos DEM5 que se descomprime al vuelo
 * (no se guarda el zip), así que el pico de espacio es el de los propios trozos.
 */
class TerrainPacks(private val context: Context) {

    data class Province(val id: String, val name: String, val url: String, val bytes: Long, val tiles: Int)

    private val prefs = AppPreferences.getInstance(context)
    private val elevation = ElevationManager.getInstance(context)
    val cancel = AtomicBoolean(false)

    private val packsDir: File get() = File(elevation.dem5Directory(), "_packs").apply { mkdirs() }

    fun indexUrl(): String = prefs.terrainIndexUrl.ifBlank { DEFAULT_INDEX_URL }

    private fun open(url: String): URLConnection = URL(url).openConnection().apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        setRequestProperty("Pragma", "no-cache")
    }

    /** Descarga la lista de provincias (bloqueante: llamar fuera del hilo principal). Sin red usa la última copia o la incluida en la app. */
    fun fetchIndex(): List<Province> {
        val base = indexUrl()
        val cache = File(packsDir, "_index.json")
        val text = try {
            open(base).getInputStream().bufferedReader().use { it.readText() }.also { runCatching { cache.writeText(it) } }
        } catch (e: Exception) {
            android.util.Log.w("TerrainPacks", "index offline, using fallback: ${e.message}")
            if (cache.isFile) cache.readText()
            else context.assets.open("provincias.json").bufferedReader().use { it.readText() }
        }
        val arr = JSONObject(text).getJSONArray("provinces")
        val provinces = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val u = o.getString("url")
            val abs = if (u.contains("://")) u else base.substringBefore('?').substringBeforeLast('/') + "/" + u
            Province(o.getString("id"), o.getString("name"), abs, o.optLong("bytes"), o.optInt("tiles"))
        }
        val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
        return provinces.sortedWith(compareBy(collator) { it.name })
    }

    fun installedIds(): Set<String> =
        packsDir.listFiles { f -> f.name.endsWith(".txt") }?.map { it.nameWithoutExtension }?.toSet().orEmpty()

    fun freeBytes(): Long = elevation.dem5Directory().usableSpace

    /** Instala una provincia. [onProgress] recibe los bytes descargados. Devuelve true si terminó bien. */
    fun install(p: Province, onProgress: (Long) -> Unit): Boolean {
        cancel.set(false)
        val dir = elevation.dem5Directory()
        val written = ArrayList<String>()
        try {
            val counting = CountingStream(open(p.url).getInputStream().buffered(), onProgress)
            ZipInputStream(counting).use { zin ->
                var e = zin.nextEntry
                while (e != null) {
                    if (cancel.get()) throw IOException("cancelled")
                    val name = File(e.name).name
                    if (!e.isDirectory && name.startsWith("dem5_") && name.endsWith(".bin") && name.contains("@${p.id}.")) {
                        val tmp = File(dir, "$name.part")
                        tmp.outputStream().buffered().use { out -> zin.copyTo(out) }
                        val target = File(dir, name)
                        if (target.exists()) target.delete()
                        if (!tmp.renameTo(target)) throw IOException("rename failed")
                        written += name
                    }
                    e = zin.nextEntry
                }
            }
            if (written.isEmpty()) return false
            File(packsDir, "${p.id}.txt").writeText(written.joinToString("\n"))
            elevation.refreshDem5()
            return true
        } catch (e: Exception) {
            android.util.Log.w("TerrainPacks", "install ${p.id} failed: ${e.message}")
            written.forEach { File(dir, it).delete() }
            dir.listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
            elevation.refreshDem5()
            return false
        }
    }

    fun delete(id: String) {
        val dir = elevation.dem5Directory()
        val manifest = File(packsDir, "$id.txt")
        if (manifest.isFile) manifest.readLines().filter { it.isNotBlank() }.forEach { File(dir, it).delete() }
        manifest.delete()
        elevation.refreshDem5()
    }

    private class CountingStream(input: InputStream, private val onProgress: (Long) -> Unit) : FilterInputStream(input) {
        private var total = 0L
        private var lastReport = 0L

        private fun add(n: Int) {
            if (n <= 0) return
            total += n
            val now = System.currentTimeMillis()
            if (now - lastReport > 400L) { lastReport = now; onProgress(total) }
        }

        override fun read(): Int = super.read().also { if (it >= 0) add(1) }
        override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { add(it) }
    }

    companion object {
        const val DEFAULT_INDEX_URL = "https://github.com/DAVOE75/ALTGRAPH/releases/download/terrain-v1/provincias.json?v=3.0.30"
    }
}
