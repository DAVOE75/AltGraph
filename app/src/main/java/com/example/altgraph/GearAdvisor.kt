package com.example.altgraph

import kotlin.math.abs
import kotlin.math.ln

/** Recomienda plato x piñón según la pendiente y la transmisión configurada por el usuario. */
object GearAdvisor {
    const val DEFAULT_CHAINRINGS = "50,34"
    const val DEFAULT_CASSETTE = "11,12,13,14,15,17,19,21,24,27,30,34"

    // Relación objetivo (plato/piñón) por pendiente, equivalente a la tabla clásica 50x16 ... 34x34
    private val targets = listOf(
        15.0 to 1.00, 12.0 to 1.06, 9.0 to 1.21, 6.0 to 1.42, 3.0 to 1.62, 0.0 to 2.38, -100.0 to 3.10
    )

    fun parseTeeth(text: String): List<Int> =
        text.split(',', ';', ' ', '-', '/')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 9..70 }
            .distinct()
            .sorted()

    fun targetRatio(grade: Double): Double {
        for (i in targets.indices) {
            if (grade >= targets[i].first) {
                if (i == 0) return targets[0].second
                val (g0, r0) = targets[i - 1]
                val (g1, r1) = targets[i]
                val f = (grade - g1) / (g0 - g1)
                return r1 + (r0 - r1) * f
            }
        }
        return targets.last().second
    }

    fun recommend(grade: Double, chainringsText: String, cassetteText: String): String =
        pickGear(targetRatio(grade), chainringsText, cassetteText)

    // ── Marcha según esfuerzo: cadencia objetivo por terreno + velocidad esperada con el FTP del ciclista ──

    // Cadencia objetivo (rpm): subida 70-85, llano 85-95, bajada 90-100, con transición suave entre terrenos
    fun targetCadence(grade: Double): Double = interp(grade, listOf(-3.0 to 95.0, 0.0 to 90.0, 1.5 to 90.0, 4.0 to 77.5))

    // Fracción del FTP que se espera sostener según el terreno
    fun intensity(grade: Double): Double = interp(grade, listOf(-3.0 to 0.45, 0.0 to 0.70, 1.5 to 0.70, 4.0 to 0.88))

    // La FC ajusta el esfuerzo: muy alta pide aliviar, muy baja permite apretar un poco
    fun hrFactor(hrFraction: Double?): Double = when {
        hrFraction == null || hrFraction <= 0.0 -> 1.0
        hrFraction > 0.92 -> 0.88
        hrFraction > 0.85 -> 0.95
        hrFraction < 0.65 -> 1.05
        else -> 1.0
    }

    private fun interp(x: Double, pts: List<Pair<Double, Double>>): Double {
        if (x <= pts.first().first) return pts.first().second
        if (x >= pts.last().first) return pts.last().second
        for (i in 1 until pts.size) {
            if (x <= pts[i].first) {
                val (x0, y0) = pts[i - 1]
                val (x1, y1) = pts[i]
                return y0 + (y1 - y0) * (x - x0) / (x1 - x0)
            }
        }
        return pts.last().second
    }

    // Velocidad (m/s) que da la potencia en esa pendiente: equilibrio de gravedad, rodadura y aire
    fun expectedSpeed(powerW: Double, grade: Double, riderKg: Double): Double {
        val mass = riderKg + BIKE_KG
        val theta = Math.atan(grade / 100.0)
        val resist = mass * 9.81 * (Math.sin(theta) + CRR * Math.cos(theta))
        fun balance(v: Double) = powerW * DRIVETRAIN_EFF - (resist * v + 0.5 * AIR_DENSITY * CDA * v * v * v)
        var lo = 0.3
        var hi = MAX_SPEED_MS
        if (balance(hi) > 0.0) return hi
        repeat(40) {
            val mid = (lo + hi) / 2.0
            if (balance(mid) > 0.0) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    fun recommendForEffort(
        grade: Double, ftp: Int, riderKg: Double, hrFraction: Double?,
        chainringsText: String, cassetteText: String
    ): String {
        val power = ftp * intensity(grade) * hrFactor(hrFraction)
        val speed = expectedSpeed(power, grade, riderKg)
        val cadence = targetCadence(grade)
        val ratio = speed * 60.0 / (cadence * WHEEL_CIRC_M)
        return pickGear(ratio, chainringsText, cassetteText)
    }

    private const val BIKE_KG = 9.0
    private const val CRR = 0.005
    private const val CDA = 0.32
    private const val AIR_DENSITY = 1.2
    private const val DRIVETRAIN_EFF = 0.97
    private const val MAX_SPEED_MS = 17.0
    private const val WHEEL_CIRC_M = 2.1

    private fun pickGear(target: Double, chainringsText: String, cassetteText: String): String {
        val rings = parseTeeth(chainringsText).ifEmpty { parseTeeth(DEFAULT_CHAINRINGS) }
        val cogs = parseTeeth(cassetteText).ifEmpty { parseTeeth(DEFAULT_CASSETTE) }
        var best = rings.first() to cogs.first()
        var bestCost = Double.MAX_VALUE
        for (ring in rings) {
            for ((idx, cog) in cogs.withIndex()) {
                var cost = abs(ln(ring.toDouble() / cog / target))
                // Evita cruzar cadena: plato grande con los piñones más grandes y plato pequeño con los más pequeños
                if (rings.size > 1) {
                    if (ring == rings.last() && idx >= cogs.size - 2) cost += 0.08
                    if (ring == rings.first() && idx <= 1) cost += 0.08
                }
                if (cost < bestCost) {
                    bestCost = cost
                    best = ring to cog
                }
            }
        }
        return "${best.first}x${best.second}"
    }
}
