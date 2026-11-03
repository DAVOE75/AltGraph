package com.example.altgraph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView

/**
 * Imagen con sombra paralela (desplazada y difuminada) que sigue la silueta del dibujo, no su rectángulo.
 * Se dibuja por software para que la sombra funcione igual en cualquier versión de Android.
 */
class ShadowImageView(context: Context) : ImageView(context) {
    private val density = resources.displayMetrics.density
    private val blur = 5f * density
    private val dx = 3f * density
    private val dy = 4f * density
    private val dest = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = Color.argb(200, 0, 0, 0)
    }
    private val shadowDest = RectF()
    private var cached: Bitmap? = null
    private var cachedFrom: Any? = null

    // Silueta (solo canal alfa) del dibujo ya desenfocada; se recalcula solo si cambia la escala
    private var shadowAlpha: Bitmap? = null
    private var shadowOffset = IntArray(2)
    private var shadowScale = -1f
    private var shadowSource: Any? = null

    private fun bitmapOf(): Bitmap? {
        val d = drawable ?: return null
        if (cachedFrom === d && cached != null) return cached
        val bmp = (d as? BitmapDrawable)?.bitmap ?: run {
            val w = d.intrinsicWidth.coerceAtLeast(1)
            val h = d.intrinsicHeight.coerceAtLeast(1)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { d.setBounds(0, 0, w, h); d.draw(Canvas(it)) }
        }
        cached = bmp
        cachedFrom = d
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmapOf() ?: return super.onDraw(canvas)
        // Deja sitio dentro de la vista para que la sombra no se recorte
        val padL = paddingLeft + blur
        val padT = paddingTop + blur
        val padR = paddingRight + blur + dx
        val padB = paddingBottom + blur + dy
        val availW = width - padL - padR
        val availH = height - padT - padB
        if (availW <= 0f || availH <= 0f) return
        val scale = minOf(availW / bmp.width, availH / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        val left = padL + (availW - w) / 2f
        val top = padT + (availH - h) / 2f
        dest.set(left, top, left + w, top + h)

        // Sombra paralela: la silueta del logo desenfocada, desplazada y dibujada detrás
        if (shadowAlpha == null || shadowScale != scale || cachedFrom !== shadowSource) {
            val blurPaint = Paint().apply { maskFilter = BlurMaskFilter((blur / scale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL) }
            shadowAlpha = bmp.extractAlpha(blurPaint, shadowOffset)
            shadowScale = scale
            shadowSource = cachedFrom
        }
        shadowAlpha?.let { sa ->
            shadowDest.set(
                dest.left + shadowOffset[0] * scale + dx,
                dest.top + shadowOffset[1] * scale + dy,
                dest.left + (shadowOffset[0] + sa.width) * scale + dx,
                dest.top + (shadowOffset[1] + sa.height) * scale + dy
            )
            canvas.drawBitmap(sa, null, shadowDest, shadowPaint)
        }
        canvas.drawBitmap(bmp, null, dest, paint)
    }
}
