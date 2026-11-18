package com.example.altgraph

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater
import kotlin.math.floor

/**
 * Trozo de terreno de alta resolución (~5 m) generado a partir del MDT05 del CNIG.
 * Formato: 'DEM5' | rows int32 | cols int32 | latTop f64 | lngLeft f64 | stepLat f64 | stepLng f64 | zlib(int16 decímetros).
 * Se descomprime una sola vez al abrirlo y se guarda como ShortArray (unos 8 MB por trozo en memoria).
 */
class Dem5Tile private constructor(
    private val rows: Int,
    private val cols: Int,
    private val latTop: Double,
    private val lngLeft: Double,
    private val stepLat: Double,
    private val stepLng: Double,
    private val data: ShortArray
) {
    fun contains(lat: Double, lng: Double): Boolean {
        val row = (latTop - lat) / stepLat
        val col = (lng - lngLeft) / stepLng
        return row >= 0 && col >= 0 && row < rows - 1 && col < cols - 1
    }

    /** Altura interpolada bilinealmente, o null si cae fuera o en un hueco. */
    fun elevation(lat: Double, lng: Double): Double? {
        val row = (latTop - lat) / stepLat
        val col = (lng - lngLeft) / stepLng
        if (row < 0 || col < 0 || row >= rows - 1 || col >= cols - 1) return null
        val r0 = floor(row).toInt()
        val c0 = floor(col).toInt()
        val fr = row - r0
        val fc = col - c0
        val a = data[r0 * cols + c0].toInt()
        val b = data[r0 * cols + c0 + 1].toInt()
        val c = data[(r0 + 1) * cols + c0].toInt()
        val d = data[(r0 + 1) * cols + c0 + 1].toInt()
        if (a == VOID || b == VOID || c == VOID || d == VOID) return null
        return ((a * (1 - fc) + b * fc) * (1 - fr) + (c * (1 - fc) + d * fc) * fr) / 10.0
    }

    companion object {
        private const val VOID = -32768

        /** Nombre de archivo del trozo que contiene la coordenada (trozos de 0,1°). */
        fun nameFor(lat: Double, lng: Double): String {
            fun f(v: Double): String {
                val n = Math.round(Math.abs(v) * 10.0).toInt()
                return (if (v < 0) "m" else "p") + "%03d".format(n)
            }
            val la = floor(lat * 10.0 + 1e-9) / 10.0
            val ln = floor(lng * 10.0 + 1e-9) / 10.0
            return "dem5_${f(la)}_${f(ln)}.bin"
        }

        fun open(file: File): Dem5Tile? = try {
            RandomAccessFile(file, "r").use { raf ->
                val head = ByteArray(44)
                raf.readFully(head)
                if (String(head, 0, 4, Charsets.US_ASCII) != "DEM5") return null
                val bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
                bb.position(4)
                val rows = bb.int
                val cols = bb.int
                val latTop = bb.double
                val lngLeft = bb.double
                val stepLat = bb.double
                val stepLng = bb.double
                if (rows <= 1 || cols <= 1 || rows.toLong() * cols > 40_000_000L) return null
                val comp = ByteArray((raf.length() - 44).toInt())
                raf.readFully(comp)
                val raw = ByteArray(rows * cols * 2)
                val inf = Inflater()
                inf.setInput(comp)
                var off = 0
                while (off < raw.size && !inf.finished()) {
                    val n = inf.inflate(raw, off, raw.size - off)
                    if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
                    off += n
                }
                inf.end()
                if (off != raw.size) return null
                val shorts = ShortArray(rows * cols)
                ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                Dem5Tile(rows, cols, latTop, lngLeft, stepLat, stepLng, shorts)
            }
        } catch (e: Exception) {
            null
        }
    }
}
