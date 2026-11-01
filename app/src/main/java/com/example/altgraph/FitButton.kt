package com.example.altgraph

import android.content.Context
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import kotlin.math.abs

/**
 * Botón del panel que ajusta su tamaño de letra al ancho real que le toca (cualquier idioma):
 * baja de [maxSp] a [minSp] hasta que ninguna palabra se parta y el texto quepa en [maxLines] líneas.
 * Si ni con [minSp] cabe y [stackWhenTight] está activo, la fila (LinearLayout horizontal) pasa a vertical.
 */
class FitButton(
    context: Context,
    private val maxSp: Float = PanelTheme.BUTTON,
    private val minSp: Float = 10f,
    private val maxLines: Int = 2,
    private val stackWhenTight: Boolean = true
) : Button(context) {

    private var appliedSp = -1f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY && w > 0 && text.isNotEmpty()) fitTo(w)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun fitTo(w: Int) {
        val avail = w - paddingLeft - paddingRight
        if (avail <= 0) return
        val shown = (transformationMethod?.getTransformation(text, this) ?: text).toString()
        val words = shown.split(' ', '\n').filter { it.isNotEmpty() }
        val scaled = resources.displayMetrics.scaledDensity
        var sp = maxSp
        var fits = false
        while (true) {
            val p = TextPaint(paint).apply { textSize = sp * scaled }
            val widest = words.maxOfOrNull { p.measureText(it) } ?: 0f
            val lines = if (widest > avail) Int.MAX_VALUE
            else StaticLayout.Builder.obtain(shown, 0, shown.length, p, avail).build().lineCount
            if (lines <= maxLines) { fits = true; break }
            if (sp - 0.5f < minSp) break
            sp -= 0.5f
        }
        if (!fits) {
            sp = minSp
            if (stackWhenTight) stackParent()
        }
        if (abs(appliedSp - sp) > 0.01f) {
            appliedSp = sp
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        }
    }

    private fun stackParent() {
        val row = parent as? LinearLayout ?: return
        if (row.orientation == LinearLayout.VERTICAL) return
        row.post {
            row.orientation = LinearLayout.VERTICAL
            for (i in 0 until row.childCount) {
                val c = row.getChildAt(i)
                val lp = (c.layoutParams as? LinearLayout.LayoutParams) ?: continue
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.weight = 0f
                lp.topMargin = 3
                lp.bottomMargin = 3
                c.layoutParams = lp
            }
            row.requestLayout()
        }
    }
}
