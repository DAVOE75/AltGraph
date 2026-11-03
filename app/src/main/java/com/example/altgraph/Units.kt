package com.example.altgraph

import android.content.Context
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Conversión de unidades para los textos de las gráficas. El cálculo interno es siempre métrico; solo cambia lo que se
 * escribe. Con las unidades métricas cada punto de uso conserva su formato de siempre.
 */
object Units {
    private const val M_TO_FT = 3.28084
    private const val M_PER_MI = 1609.344
    private const val KMH_TO_MPH = 0.621371
    // Por debajo de ~1000 ft las distancias se dan en pies
    private const val SHORT_LIMIT_M = 305.0

    // Copia en memoria del ajuste para el código sin Context (lo mantiene AppPreferences)
    @Volatile var imperialFlag: Boolean = false

    fun imperial(ctx: Context): Boolean = AppPreferences.getInstance(ctx).useImperialUnits

    // Altitud o desnivel: "123m" / "404ft". rounded = redondear en vez de truncar (solo afecta a métricas)
    fun elev(ctx: Context, meters: Double, rounded: Boolean = false): String =
        if (imperial(ctx)) "${(meters * M_TO_FT).roundToInt()}ft"
        else "${if (rounded) meters.roundToInt() else meters.toInt()}m"

    // Distancia en unidades imperiales: pies hasta ~1000 ft y millas a partir de ahí ("0.62mi", "12.4mi")
    fun distImperial(meters: Double, decimals: Int = 1): String {
        if (meters < SHORT_LIMIT_M) return "${(meters * M_TO_FT).roundToInt()}ft"
        val mi = meters / M_PER_MI
        val d = if (mi >= 100.0) 0 else decimals
        val s = String.format(Locale.US, "%.${d}f", mi)
        return (if (d > 0) s.trimEnd('0').trimEnd('.') else s) + "mi"
    }

    // Ritmo de desnivel: "+420 m/h" / "+1380 ft/h" (en pies, a la decena más cercana)
    fun climbRate(ctx: Context, metersPerHour: Int): String =
        if (imperial(ctx)) "+${(Math.round(metersPerHour * M_TO_FT / 10.0) * 10L)} ft/h" else "+$metersPerHour m/h"

    private val NUMBER_PER_HOUR = Regex("(\\d+(?:[.,]\\d+)?)(\\s?)m/h")
    private val NUMBER_UNIT = Regex("(\\d+(?:[.,]\\d+)?)(\\s?)(km|m)\\b(?![/\\w])")
    private val PER_HOUR = Regex("\\bm/h\\b")
    private val UNIT_WORD = Regex("\\((Meters|Meter|Metros|Mètres|Metri)\\)")

    // VAM / ritmo de desnivel: "450 m/h" / "1480 ft/h" (en pies, a la decena más cercana)
    fun vam(ctx: Context, metersPerHour: Int): String =
        if (imperial(ctx)) "${Math.round(metersPerHour * M_TO_FT / 10.0) * 10L} ft/h" else "$metersPerHour m/h"

    // Convierte las cifras en m / km y "m/h" de un texto del panel a pies y millas cuando se usan unidades imperiales.
    // Pensado para descripciones fijas traducidas ("por debajo de 30 m"); en métrico devuelve el texto sin tocar.
    fun localize(ctx: Context, text: String): String = if (imperial(ctx)) localizeImperial(text) else text

    // Parte pura (sin Context) de localize, para poder probarla
    fun localizeImperial(text: String): String {
        // Primero las cifras con "m/h" (se convierten); luego m y km; al final los "m/h" sueltos y la palabra "(Metros)"
        var out = NUMBER_PER_HOUR.replace(text) { m ->
            val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@replace m.value
            "${Math.round(value * M_TO_FT / 10.0) * 10L}${m.groupValues[2]}ft/h"
        }
        out = NUMBER_UNIT.replace(out) { m ->
            val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@replace m.value
            val space = m.groupValues[2]
            if (m.groupValues[3] == "km") {
                String.format(Locale.US, "%.1f", value * 1000.0 / M_PER_MI).trimEnd('0').trimEnd('.') + space + "mi"
            } else {
                val ft = value * M_TO_FT
                val rounded = if (ft >= 1000.0) Math.round(ft / 10.0) * 10L else Math.round(ft / 5.0) * 5L
                "$rounded${space}ft"
            }
        }
        out = PER_HOUR.replace(out, "ft/h")
        return UNIT_WORD.replace(out, "(ft / mi)")
    }

    // Convierte las cifras con unidad de una frase hablada ("5 kilómetros", "250 metros por hora", "20 km/h") a millas, pies y
    // millas por hora usando las palabras del idioma del usuario. Función pura (sin Context) para poder probarla.
    fun convertSpoken(
        phrase: String, kmWords: List<String>, meterWords: List<String>,
        mile: String, miles: String, foot: String, feet: String, mph: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val metres = meterWords + "Höhenmeter"
        val alternatives = (listOf("km/h") + kmWords + metres).distinct().sortedByDescending { it.length }
        val regex = Regex("(\\d+(?:[.,]\\d+)?)\\s+(" + alternatives.joinToString("|") { Regex.escape(it) } + ")(?!\\p{L})", RegexOption.IGNORE_CASE)
        return regex.replace(phrase) { m ->
            val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@replace m.value
            val unit = m.groupValues[2]
            if (metres.any { it.equals(unit, ignoreCase = true) }) {
                val ft = value * M_TO_FT
                val rounded = when {
                    ft >= 1000.0 -> Math.round(ft / 50.0) * 50L
                    ft >= 100.0 -> Math.round(ft / 10.0) * 10L
                    else -> Math.round(ft / 5.0) * 5L
                }
                "$rounded ${if (rounded == 1L) foot else feet}"
            } else {
                val mi = Math.round(value * KMH_TO_MPH * 10.0) / 10.0
                val text = if (mi % 1.0 == 0.0) mi.toInt().toString() else String.format(locale, "%.1f", mi)
                val word = if (unit.equals("km/h", ignoreCase = true)) mph else if (mi <= 1.05) mile else miles
                "$text $word"
            }
        }
    }

    fun speed(ctx: Context, kmh: Double): String =
        if (imperial(ctx)) String.format(Locale.US, "%.1f mph", kmh * KMH_TO_MPH) else String.format(Locale.US, "%.1f km/h", kmh)
}