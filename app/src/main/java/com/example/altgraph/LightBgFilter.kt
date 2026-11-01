package com.example.altgraph

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Filtro del "fondo blanco" de las gráficas: invierte la luminosidad conservando el matiz
 * (fondo negro -> blanco; texto, cotas y km claros -> casi negros) y refuerza la saturación.
 *
 * Invertir aclara los colores oscuros y saturados (rojos, granates), justo lo contrario de lo que se quiere sobre
 * blanco. Por eso los colores saturados se precompensan con la inversa del filtro ([precompensate]) y salen
 * intensos y algo más oscuros; los neutros se dejan pasar y los invierte el propio filtro.
 */
object LightBgFilter {
    private const val SATURATION = 1.35f
    // Contraste alrededor del gris medio: los grises secundarios (km, leyendas) quedan casi negros
    private const val CONTRAST = 1.45f
    // Luminancias de ColorMatrix.setSaturation
    private const val LR = 0.213f
    private const val LG = 0.715f
    private const val LB = 0.072f

    // Matriz hue-rotate 180º (como en CSS)
    private val hue = arrayOf(
        doubleArrayOf(-0.574, 1.430, 0.144),
        doubleArrayOf(0.426, 0.430, 0.144),
        doubleArrayOf(0.426, 1.430, -0.856)
    )

    private val sat: Array<DoubleArray> by lazy {
        val s = SATURATION.toDouble()
        arrayOf(
            doubleArrayOf(LR + (1 - LR) * s, LG * (1 - s), LB * (1 - s)),
            doubleArrayOf(LR * (1 - s), LG + (1 - LG) * s, LB * (1 - s)),
            doubleArrayOf(LR * (1 - s), LG * (1 - s), LB + (1 - LB) * s)
        )
    }

    // y = m·x + t  con  m = -k·(S·H)  y  t = k·255·(S·H)·1 + 128·(1-k)   (k = contraste)
    private val sh: Array<DoubleArray> by lazy { mul(sat, hue) }
    private val m: Array<DoubleArray> by lazy { Array(3) { r -> DoubleArray(3) { c -> -CONTRAST * sh[r][c] } } }
    private val t: DoubleArray by lazy { DoubleArray(3) { r -> CONTRAST * 255.0 * (sh[r][0] + sh[r][1] + sh[r][2]) + 128.0 * (1 - CONTRAST) } }
    private val mInv: Array<DoubleArray> by lazy { inverse(m) }

    val paint: Paint by lazy {
        val invert = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        val hue180 = ColorMatrix(floatArrayOf(
            hue[0][0].toFloat(), hue[0][1].toFloat(), hue[0][2].toFloat(), 0f, 0f,
            hue[1][0].toFloat(), hue[1][1].toFloat(), hue[1][2].toFloat(), 0f, 0f,
            hue[2][0].toFloat(), hue[2][1].toFloat(), hue[2][2].toFloat(), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        val boost = ColorMatrix().apply { setSaturation(SATURATION) }
        val contrast = ColorMatrix(floatArrayOf(
            CONTRAST, 0f, 0f, 0f, 128f * (1 - CONTRAST),
            0f, CONTRAST, 0f, 0f, 128f * (1 - CONTRAST),
            0f, 0f, CONTRAST, 0f, 128f * (1 - CONTRAST),
            0f, 0f, 0f, 1f, 0f
        ))
        invert.postConcat(hue180)
        invert.postConcat(boost)
        invert.postConcat(contrast)
        Paint().apply { colorFilter = ColorMatrixColorFilter(invert) }
    }

    /**
     * Color a dibujar para que, tras el filtro, se vea un color intenso sobre blanco.
     * Los neutros (blanco, grises, negro) no se tocan: el filtro ya los invierte.
     */
    fun precompensate(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        if (hsv[1] < 0.18f) return color
        // Objetivo: algo más saturado y oscuro para que contraste sobre blanco
        hsv[1] = (hsv[1] * 1.08f).coerceAtMost(1f)
        hsv[2] = hsv[2] * 0.88f
        val target = Color.HSVToColor(Color.alpha(color), hsv)
        val y = doubleArrayOf(Color.red(target).toDouble(), Color.green(target).toDouble(), Color.blue(target).toDouble())
        val d = DoubleArray(3) { y[it] - t[it] }
        val x = DoubleArray(3) { r -> mInv[r][0] * d[0] + mInv[r][1] * d[1] + mInv[r][2] * d[2] }
        return Color.argb(
            Color.alpha(color),
            x[0].toInt().coerceIn(0, 255),
            x[1].toInt().coerceIn(0, 255),
            x[2].toInt().coerceIn(0, 255)
        )
    }

    private fun mul(a: Array<DoubleArray>, b: Array<DoubleArray>) =
        Array(3) { r -> DoubleArray(3) { c -> a[r][0] * b[0][c] + a[r][1] * b[1][c] + a[r][2] * b[2][c] } }

    private fun inverse(a: Array<DoubleArray>): Array<DoubleArray> {
        val det = a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1]) -
            a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0]) +
            a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0])
        return arrayOf(
            doubleArrayOf(
                (a[1][1] * a[2][2] - a[1][2] * a[2][1]) / det,
                (a[0][2] * a[2][1] - a[0][1] * a[2][2]) / det,
                (a[0][1] * a[1][2] - a[0][2] * a[1][1]) / det
            ),
            doubleArrayOf(
                (a[1][2] * a[2][0] - a[1][0] * a[2][2]) / det,
                (a[0][0] * a[2][2] - a[0][2] * a[2][0]) / det,
                (a[0][2] * a[1][0] - a[0][0] * a[1][2]) / det
            ),
            doubleArrayOf(
                (a[1][0] * a[2][1] - a[1][1] * a[2][0]) / det,
                (a[0][1] * a[2][0] - a[0][0] * a[2][1]) / det,
                (a[0][0] * a[1][1] - a[0][1] * a[1][0]) / det
            )
        )
    }
}
