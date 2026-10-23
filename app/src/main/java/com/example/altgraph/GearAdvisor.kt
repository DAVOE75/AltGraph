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

    fun recommend(grade: Double, chainringsText: String, cassetteText: String): String {
        val rings = parseTeeth(chainringsText).ifEmpty { parseTeeth(DEFAULT_CHAINRINGS) }
        val cogs = parseTeeth(cassetteText).ifEmpty { parseTeeth(DEFAULT_CASSETTE) }
        val target = targetRatio(grade)
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
