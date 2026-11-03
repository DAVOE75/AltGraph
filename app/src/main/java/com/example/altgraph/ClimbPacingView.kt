package com.example.altgraph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class ClimbPacingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentVam: Int = 850
    private var targetVam: Int = 900
    private var targetSpeedKmh: Double = 12.5
    private var status: ClimbPacingCalculator.PacingStatus = ClimbPacingCalculator.PacingStatus.ON_PACE

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#09090B")
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val vamValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val speedLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A1A1AA")
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val speedValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAB308")
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val badgeRect = RectF()

    fun updatePacingData(result: ClimbPacingCalculator.PacingResult) {
        this.currentVam = result.currentVam
        this.targetVam = result.targetVam
        this.targetSpeedKmh = result.targetSpeedKmh
        this.status = result.status
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val padX = 20f

        // 1. Título VAM
        titlePaint.textSize = (h * 0.08f).coerceIn(14f, 20f)
        val titleText = if (Units.imperial(context)) {
            context.getString(R.string.label_target_vam_format_value, Units.vam(context, targetVam))
        } else {
            context.getString(R.string.label_target_vam_format, targetVam)
        }
        canvas.drawText(titleText, padX, h * 0.16f, titlePaint)

        // 2. Valor VAM Principal
        vamValuePaint.textSize = (h * 0.28f).coerceIn(28f, 56f)
        val vamStr = Units.vam(context, currentVam)
        canvas.drawText(vamStr, padX, h * 0.46f, vamValuePaint)

        // 3. Etiqueta y Valor de Velocidad Objetivo en km/h
        speedLabelPaint.textSize = (h * 0.07f).coerceIn(12f, 18f)
        canvas.drawText(context.getString(R.string.label_target_speed), padX, h * 0.62f, speedLabelPaint)

        speedValuePaint.textSize = (h * 0.22f).coerceIn(22f, 42f)
        val speedStr = if (targetSpeedKmh > 0.0) Units.speed(context, targetSpeedKmh) else if (Units.imperial(context)) "--.- mph" else "--.- km/h"
        canvas.drawText(speedStr, padX, h * 0.88f, speedValuePaint)

        // 4. Insinia de Estado
        val (badgeText, badgeColor) = when (status) {
            ClimbPacingCalculator.PacingStatus.ON_PACE -> context.getString(R.string.label_pacing_on_pace) to "#22C55E"
            ClimbPacingCalculator.PacingStatus.OVERPACING -> context.getString(R.string.label_pacing_overpacing) to "#EF4444"
            ClimbPacingCalculator.PacingStatus.UNDERPACING -> context.getString(R.string.label_pacing_underpacing) to "#38BDF8"
        }

        badgeBgPaint.color = Color.parseColor(badgeColor)
        badgeTextPaint.textSize = (h * 0.065f).coerceIn(12f, 16f)

        val textWidth = badgeTextPaint.measureText(badgeText)
        val rectRight = w - padX
        val rectLeft = rectRight - textWidth - 24f
        val rectTop = h * 0.68f
        val rectBottom = rectTop + (h * 0.18f)

        badgeRect.set(rectLeft, rectTop, rectRight, rectBottom)
        canvas.drawRoundRect(badgeRect, 12f, 12f, badgeBgPaint)

        canvas.drawText(badgeText, rectLeft + 12f, rectTop + (badgeRect.height() * 0.68f), badgeTextPaint)
    }
}