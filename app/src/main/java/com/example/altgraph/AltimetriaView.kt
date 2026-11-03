package com.example.altgraph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.min

class AltimetriaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var remainingDistance: Double = 8500.0
    private var timeToSummit: Long = 1620L
    private var avgGradeRemaining: Double = 7.2
    private var zoneColor: String = "VERDE"
    private var nextBlocks: List<Float> = listOf(3.5f, 5.0f, 7.5f, 11.2f, 12.8f, 9.0f, 6.5f, 8.2f, 10.5f, 4.0f)
    private var attackAlert: Boolean = false
    private var blockSizeMeters: Double = 100.0
    private var showBlockPercentages: Boolean = true
    private var visibleBlocksCount: Int = 5

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 65f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val blockTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val bgHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val alertBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val alertStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val alertRect = RectF()
    private val blockRect = RectF()

    fun updateStrategyData(
        remainingDistance: Double,
        timeToSummit: Long,
        avgGrade: Double,
        currentZoneColor: String,
        nextBlocks: List<Float>,
        attackAlert: Boolean,
        blockSizeMeters: Double,
        showBlockPct: Boolean = true,
        visibleBlocksCount: Int = 5,
        fontFamilyKey: String = "sans-serif-condensed"
    ) {
        this.remainingDistance = remainingDistance
        this.timeToSummit = timeToSummit
        this.avgGradeRemaining = avgGrade
        this.zoneColor = currentZoneColor
        if (nextBlocks.isNotEmpty()) {
            this.nextBlocks = nextBlocks
        }
        this.attackAlert = attackAlert
        this.blockSizeMeters = blockSizeMeters
        this.showBlockPercentages = showBlockPct
        this.visibleBlocksCount = visibleBlocksCount.coerceIn(1, 10)

        val tf = FontHelper.getTypeface(fontFamilyKey)
        textPaint.typeface = tf
        labelPaint.typeface = tf
        blockTextPaint.typeface = tf

        postInvalidate()
    }

    private fun getColorForGrade(grade: Float): Int {
        return when {
            grade > 12f -> Color.parseColor("#9C27B0")
            grade > 8f -> Color.parseColor("#F44336")
            grade > 5f -> Color.parseColor("#FF9800")
            grade > 3f -> Color.parseColor("#FFEB3B")
            else -> Color.parseColor("#4CAF50")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        canvas.drawColor(Color.BLACK)

        val headerColor = when (zoneColor) {
            "AMARILLO" -> Color.parseColor("#FFEB3B")
            "NARANJA" -> Color.parseColor("#FF9800")
            "ROJO" -> Color.parseColor("#F44336")
            "VIOLETA" -> Color.parseColor("#9C27B0")
            else -> Color.parseColor("#4CAF50")
        }
        bgHeaderPaint.color = headerColor
        canvas.drawRect(0f, 0f, w, h * 0.45f, bgHeaderPaint)

        val isLightHeader = zoneColor == "AMARILLO"
        val headerTextColor = if (isLightHeader) Color.BLACK else Color.WHITE
        val headerLabelColor = if (isLightHeader) Color.DKGRAY else Color.LTGRAY

        val centerX = w / 2f

        labelPaint.color = headerLabelColor
        labelPaint.textSize = (h * 0.09f).coerceAtLeast(20f)
        canvas.drawText(context.getString(R.string.header_climb_strategist), centerX, h * 0.12f, labelPaint)

        val distanceKm = remainingDistance / 1000.0
        val distanceText = if (Units.imperial(context)) Units.distImperial(remainingDistance) else String.format(Locale.getDefault(), "%.1f km", distanceKm)
        val minutes = timeToSummit / 60
        val seconds = timeToSummit % 60
        val timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        textPaint.color = headerTextColor
        textPaint.textSize = (h * 0.16f).coerceAtLeast(28f)
        canvas.drawText(distanceText, centerX, h * 0.27f, textPaint)

        labelPaint.textSize = (h * 0.075f).coerceAtLeast(18f)
        canvas.drawText(context.getString(R.string.label_dist_remaining), centerX, h * 0.35f, labelPaint)

        labelPaint.color = Color.LTGRAY
        textPaint.color = Color.WHITE
        textPaint.textSize = (h * 0.14f).coerceAtLeast(26f)
        canvas.drawText(timeText, centerX, h * 0.58f, textPaint)

        labelPaint.textSize = (h * 0.07f).coerceAtLeast(14f)
        canvas.drawText(context.getString(R.string.label_time_to_summit), centerX, h * 0.66f, labelPaint)

        // Bloques
        val barY = h * 0.74f
        val barHeight = h * 0.18f
        val count = min(visibleBlocksCount, nextBlocks.size)
        val totalMeters = (count * blockSizeMeters).toInt()

        labelPaint.textSize = (h * 0.065f).coerceAtLeast(16f)
        val blocksTitle = if (Units.imperial(context)) {
            context.getString(R.string.label_next_blocks_value, Units.distImperial(totalMeters.toDouble(), 2), Units.distImperial(blockSizeMeters, 2))
        } else {
            context.getString(R.string.label_next_blocks, totalMeters, blockSizeMeters.toInt())
        }
        canvas.drawText(blocksTitle, centerX, barY - 10f, labelPaint)

        if (count > 0) {
            val blockWidth = (w - 30f) / count.toFloat()

            for (i in 0 until count) {
                val left = 15f + i * blockWidth
                val top = barY
                val right = left + blockWidth - 4f
                val bottom = top + barHeight

                blockRect.set(left, top, right, bottom)
                val gradeVal = nextBlocks[i]
                blockPaint.color = getColorForGrade(gradeVal)
                canvas.drawRoundRect(blockRect, 8f, 8f, blockPaint)

                // Regla: Bloques de 50m muestran decimales; de 100m en adelante sin decimales (enteros)
                if (showBlockPercentages) {
                    val gradeText = if (blockSizeMeters <= 50.0) {
                        if (gradeVal % 1.0f == 0.0f) "%.0f%%".format(gradeVal) else "%.1f%%".format(gradeVal)
                    } else {
                        "%.0f%%".format(gradeVal)
                    }
                    blockTextPaint.textSize = (barHeight * 0.68f).coerceIn(14f, 26f)
                    blockTextPaint.color = if (gradeVal > 3f && gradeVal <= 5f) Color.BLACK else Color.WHITE

                    val txtX = blockRect.centerX()
                    val txtY = blockRect.centerY() + (blockTextPaint.textSize * 0.35f)
                    canvas.drawText(gradeText, txtX, txtY, blockTextPaint)
                }
            }
        }

        // Alerta
        if (attackAlert) {
            alertRect.set(w * 0.1f, h * 0.25f, w * 0.9f, h * 0.65f)

            alertBgPaint.color = Color.RED
            alertBgPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(alertRect, 24f, 24f, alertBgPaint)

            alertStrokePaint.color = Color.WHITE
            alertStrokePaint.style = Paint.Style.STROKE
            alertStrokePaint.strokeWidth = 6f
            canvas.drawRoundRect(alertRect, 24f, 24f, alertStrokePaint)

            textPaint.color = Color.WHITE
            textPaint.textSize = (h * 0.14f).coerceAtLeast(26f)
            canvas.drawText(context.getString(R.string.alert_attack), centerX, h * 0.42f, textPaint)

            labelPaint.color = Color.WHITE
            labelPaint.textSize = (h * 0.075f).coerceAtLeast(14f)
            canvas.drawText(context.getString(R.string.alert_attack_sub), centerX, h * 0.54f, labelPaint)
        }
    }
}