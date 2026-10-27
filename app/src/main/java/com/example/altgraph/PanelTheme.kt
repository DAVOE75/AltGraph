package com.example.altgraph

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import kotlin.math.abs

/**
 * Tema (oscuro / claro) y escala tipográfica única del panel de ajustes.
 * Los colores salen de [colors]; los tamaños de letra se normalizan en [apply] para que todo el panel
 * use los mismos pasos: título, sección, valor, cuerpo, pie y botón.
 */
object PanelTheme {
    const val DARK = "dark"
    const val LIGHT = "light"

    /** Vistas con colores propios (p. ej. un botón azul con texto blanco): no se tocan. */
    const val KEEP = "panel_keep_colors"

    class Palette(
        val bg: Int,
        val card: Int,
        val stroke: Int,
        val text: Int,
        val textSecondary: Int,
        val textMuted: Int,
        val hint: Int,
        val accent: Int,
        val onAccent: Int,
        val chip: Int,
        val onChip: Int,
        val save: Int,
        val onSave: Int
    )

    private val dark = Palette(
        bg = Color.parseColor("#09090B"),
        card = Color.parseColor("#18181B"),
        stroke = Color.parseColor("#27272A"),
        text = Color.WHITE,
        textSecondary = Color.LTGRAY,
        textMuted = Color.parseColor("#94A3B8"),
        hint = Color.parseColor("#71717A"),
        accent = Color.parseColor("#38BDF8"),
        onAccent = Color.BLACK,
        chip = Color.parseColor("#27272A"),
        onChip = Color.WHITE,
        save = Color.parseColor("#22C55E"),
        onSave = Color.BLACK
    )

    private val light = Palette(
        bg = Color.parseColor("#F4F4F5"),
        card = Color.WHITE,
        stroke = Color.parseColor("#E4E4E7"),
        text = Color.parseColor("#18181B"),
        textSecondary = Color.parseColor("#52525B"),
        textMuted = Color.parseColor("#71717A"),
        hint = Color.parseColor("#A1A1AA"),
        accent = Color.parseColor("#0369A1"),
        onAccent = Color.WHITE,
        chip = Color.parseColor("#E4E4E7"),
        onChip = Color.parseColor("#18181B"),
        save = Color.parseColor("#16A34A"),
        onSave = Color.WHITE
    )

    var colors: Palette = dark
        private set
    var isLight: Boolean = false
        private set

    // Escala tipográfica (sp)
    const val TITLE = 24f
    const val VALUE = 16f
    const val SAVE = 16f
    const val BODY = 15f
    const val SECTION = 13f
    const val CAPTION = 12f
    const val BUTTON = 12f

    fun init(context: Context, mode: String) {
        isLight = mode == LIGHT
        colors = if (isLight) light else dark
    }

    /** Recorre el árbol y unifica tamaños de letra y colores según el tema activo. */
    fun apply(root: View) {
        when (root) {
            is Spinner -> themeSpinner(root)
            is TextView -> style(root)
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) apply(root.getChildAt(i))
        }
    }

    private fun style(tv: TextView) {
        if (tv.tag == KEEP) return
        val sp = tv.textSize / tv.resources.displayMetrics.scaledDensity
        val bold = tv.typeface?.isBold == true
        val target = when {
            tv is CompoundButton || tv is EditText -> BODY
            tv is Button -> if (sp >= 15.5f) sp else BUTTON
            sp >= 22f -> sp
            bold && sp >= 15.75f -> VALUE
            sp < 12.75f -> CAPTION
            bold && sp <= 13.5f -> SECTION
            else -> BODY
        }
        if (abs(sp - target) > 0.01f) tv.textSize = target

        if (tv is EditText) tv.setHintTextColor(colors.hint)
        // Los botones ya salen con los colores del tema desde sus ayudantes
        if (tv is Button && tv !is CompoundButton) return
        tv.setTextColor(mapColor(tv.currentTextColor))
    }

    private fun mapColor(c: Int): Int = when (c) {
        Color.WHITE -> colors.text
        Color.LTGRAY -> colors.textSecondary
        Color.parseColor("#94A3B8"), Color.parseColor("#A1A1AA") -> colors.textMuted
        Color.parseColor("#38BDF8") -> colors.accent
        else -> c
    }

    private fun themeSpinner(sp: Spinner) {
        val current = sp.adapter as? ArrayAdapter<*> ?: return
        if (current is ThemedAdapter) return
        val items = List(current.count) { current.getItem(it)?.toString() ?: "" }
        val pos = sp.selectedItemPosition
        sp.adapter = ThemedAdapter(sp.context, items)
        if (pos >= 0) sp.setSelection(pos)
    }

    private class ThemedAdapter(context: Context, items: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        private fun tint(v: View, dropDown: Boolean): View {
            (v as? TextView)?.apply {
                setTextColor(colors.text)
                textSize = BODY
                typeface = Typeface.DEFAULT
                if (dropDown) setBackgroundColor(colors.card)
            }
            return v
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            tint(super.getView(position, convertView, parent), false)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            tint(super.getDropDownView(position, convertView, parent), true)
    }
}
