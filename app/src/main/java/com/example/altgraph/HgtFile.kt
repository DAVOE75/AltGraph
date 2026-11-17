package com.example.altgraph

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Fuente de altitud MDT a partir de archivos SRTM/Skadi .hgt (cuadrícula de 1x1 grado).
 * Se mapea en memoria virtual (Memory-Mapped ByteBuffer) para lectura ultrarrápida sin consumo de RAM.
 */
class HgtFile(file: File) {
    val south: Int
    val west: Int
    private val n: Int
    private val grid: ByteBuffer

    init {
        val m = NAME_REGEX.find(file.name) ?: throw IllegalArgumentException("Nombre de archivo HGT no válido: ${file.name}")
        south = m.groupValues[2].toInt() * (if (m.groupValues[1].uppercase() == "S") -1 else 1)
        west = m.groupValues[4].toInt() * (if (m.groupValues[3].uppercase() == "W") -1 else 1)
        n = sqrt(file.length() / 2.0).roundToInt()
        require(n >= 2 && n.toLong() * n * 2 == file.length()) { "${file.name} no es una cuadrícula HGT cuadrada válida" }
        grid = RandomAccessFile(file, "r").use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length()) }
    }

    private fun cell(row: Int, col: Int): Double? {
        val v = grid.getShort((row * n + col) * 2).toInt()
        return if (v == VOID) null else v.toDouble()
    }

    /**
     * Altura interpolada bilinealmente en la coordenada lat/lng dada.
     */
    fun getElevation(lat: Double, lng: Double): Double? {
        val row = (south + 1 - lat) * (n - 1)
        val col = (lng - west) * (n - 1)
        if (row < 0 || col < 0 || row > n - 1 || col > n - 1) return null
        val r0 = floor(row).toInt().coerceAtMost(n - 2)
        val c0 = floor(col).toInt().coerceAtMost(n - 2)
        val fr = row - r0
        val fc = col - c0
        val a = cell(r0, c0) ?: return null
        val b = cell(r0, c0 + 1) ?: return null
        val c = cell(r0 + 1, c0) ?: return null
        val d = cell(r0 + 1, c0 + 1) ?: return null
        return (a * (1 - fc) + b * fc) * (1 - fr) + (c * (1 - fc) + d * fc) * fr
    }

    companion object {
        private const val VOID = -32768
        private val NAME_REGEX = Regex("^([NSns])(\\d{2})([EWew])(\\d{3})\\.hgt$")

        fun nameFor(lat: Double, lng: Double): String {
            val fLat = floor(lat).toInt()
            val fLng = floor(lng).toInt()
            return "%s%02d%s%03d.hgt".format(
                if (fLat < 0) "S" else "N", Math.abs(fLat),
                if (fLng < 0) "W" else "E", Math.abs(fLng)
            )
        }
    }
}
