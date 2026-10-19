package com.example.altgraph

import java.util.Locale
import kotlin.math.pow

object FatigueGradeCalculator {

    enum class AsphaltQuality(
        val factor: Double,
        val labelEs: String,
        val labelEn: String,
        val labelFr: String,
        val labelIt: String,
        val labelDe: String
    ) {
        VERY_GOOD(0.1, "Muy Bueno", "Very Good", "Très Bon", "Molto Buono", "Sehr Gut"),
        GOOD(0.5, "Bueno", "Good", "Bon", "Buono", "Gut"),
        REGULAR(1.2, "Regular", "Regular", "Moyen", "Regolare", "Mittel"),
        POOR(1.7, "Malo / Gravilla", "Poor / Gravel", "Mauvais / Gravier", "Pessimo / Ghiaia", "Schlecht / Kies");

        fun getLocalizedLabel(): String {
            val lang = Locale.getDefault().language.lowercase()
            return when {
                lang.startsWith("es") -> labelEs
                lang.startsWith("fr") -> labelFr
                lang.startsWith("it") -> labelIt
                lang.startsWith("de") -> labelDe
                else -> labelEn
            }
        }
    }

    /**
     * Calcula la Dureza (DU) para un tramo con pendiente media (gradientPct) y distancia (distanceKm).
     *
     * Fórmula del estudio "El Grado de Fatiga":
     * - 0% a 3%: DU = (Gradient%)^2 / 3.25 * DistanciaKm
     * - 3.1% a 9%: DU = (Gradient%)^2 / 3.00 * DistanciaKm
     * - 9.1% a 20%: DU = (Gradient%)^2 / 2.75 * DistanciaKm
     * - > 20%: DU = (Gradient%)^2 / 2.50 * DistanciaKm
     */
    fun calculateSegmentHardness(gradientPct: Double, distanceKm: Double): Double {
        if (gradientPct <= 0.0 || distanceKm <= 0.0) return 0.0

        val factor = when {
            gradientPct <= 3.0 -> 3.25
            gradientPct <= 9.0 -> 3.00
            gradientPct <= 20.0 -> 2.75
            else -> 2.50
        }

        return (gradientPct.pow(2) / factor) * distanceKm
    }

    /**
     * Calcula el Grado de Fatiga (GF) total:
     * GF = SUMA(DU) + TA + (PMx / 5)
     *
     * @param totalHardness Suma de DU de todos los kilómetros
     * @param asphaltFactor Factor del tipo de asfalto (TA)
     * @param maxRampPct Pendiente máxima alcanzada (PMx)
     */
    fun calculateTotalFatigueGrade(
        totalHardness: Double,
        asphaltFactor: Double = 0.5,
        maxRampPct: Double = 0.0
    ): Double {
        if (totalHardness <= 0.0) return 0.0
        val maxRampTerm = (maxRampPct / 5.0).coerceAtLeast(0.0)
        return totalHardness + asphaltFactor + maxRampTerm
    }

    /**
     * Clasifica el puerto según su Grado de Fatiga (GF) siguiendo la lógica oficial de La Vuelta a España / UCI / APM.
     * Si GF < 18.0, no es un puerto puntuable (repecho no categorizado).
     */
    fun getClimbCategoryName(gf: Double): String {
        val lang = Locale.getDefault().language.lowercase()
        return when {
            gf < 18.0 -> "" // No puntuable / Repecho no categorizado
            gf <= 39.9 -> "4ª Cat"
            gf <= 74.9 -> "3ª Cat"
            gf <= 144.9 -> "2ª Cat"
            gf <= 239.9 -> "1ª Cat"
            else -> when {
                lang.startsWith("es") -> "Especial (C.E.)"
                lang.startsWith("fr") -> "Hors Catégorie (HC)"
                lang.startsWith("it") -> "Fuori Categoria (HC)"
                lang.startsWith("de") -> "Ehrenkategorie (HC)"
                else -> "Special (HC)"
            }
        }
    }
}