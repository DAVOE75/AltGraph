package com.example.altgraph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import kotlin.math.roundToInt

class RouteBarView(context: Context) : View(context) {

    private companion object {
        // Tamaño fijo de la regla: no depende de la escala de letra del panel de estilo
        const val RULER_TEXT_SIZE = 25f
    }

    var strategyData: StrategyData? = null

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val headerOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val pastOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(155, 0, 0, 0) // Oscurece el tramo ya recorrido a la izquierda de la flecha con mayor contraste
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 200
    }

    private val percentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val checkeredPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val size = 20
        val bitmap = android.graphics.Bitmap.createBitmap(size * 2, size * 2, android.graphics.Bitmap.Config.ARGB_8888)
        val cvs = android.graphics.Canvas(bitmap)
        val pWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        val pBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        cvs.drawRect(0f, 0f, size.toFloat(), size.toFloat(), pWhite)
        cvs.drawRect(size.toFloat(), size.toFloat(), size * 2f, size * 2f, pWhite)
        cvs.drawRect(size.toFloat(), 0f, size * 2f, size.toFloat(), pBlack)
        cvs.drawRect(0f, size.toFloat(), size.toFloat(), size * 2f, pBlack)
        shader = android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
    }

    private val tickTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 2f, Color.BLACK)
    }

    // Paint for max-grade-per-block label (top-left of each band, only at 50m scale)
    private val maxGradeBlockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val maxGradeArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 2f, Color.BLACK)
    }

    private val cyclistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FBC02D")
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val cyclistOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    data class ColorBand(
        val startIndex: Int,
        var endIndex: Int,
        val colorHex: String,
        var sumGrade: Double
    )

    // ── ELITE feature state (set by RouteBarDataField each frame) ────────────
    var ghostGap: GhostGap? = null   // hueco con KGhost (+ por delante / - por detrás)
    var ghostStyle: String = "classic" // "classic", "velon", "pursuit", "tissot"
    var showEnergyBar: Boolean = false
    var showPowerBar: Boolean = false
    var showRadarAlerts: Boolean = true
    var showPowerGuide: Boolean = false
    var userFtp: Int = 250
    var showPoiRuler: Boolean = false
    var showHistogram: Boolean = false
    var showRadar3d: Boolean = true
    var radarTheme: String = "Estándar"

    // Texto del hueco con KGhost (reutilizado en cada frame)
    private val ghostTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    // POI icon paint
    private val poiTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 2f, Color.BLACK)
    }

    // Energy bar paints
    private val energyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val histPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
        setShadowLayer(2f, 0f, 1f, Color.BLACK)
    }

    // KGhost Widget Paints (sin sombras)
    private val ghostWidgetStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val ghostWidgetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ghostWidgetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    /**
     * Ciclista con relleno claro y fino borde interior. Dos pasadas: la primera pinta la silueta completa
     * en el color del borde; la segunda pinta el relleno por dentro, con los trazos más finos y un
     * contorno interior recortado a cada forma rellena.
     */
    private fun drawCyclistVector(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val s = size / 24f
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val isRed = (hsv[0] <= 15f || hsv[0] >= 345f) && hsv[1] > 0.5f && hsv[2] > 0.5f
        val fillColor = if (isRed) Color.parseColor("#FF3B3B") else Color.WHITE
        val borderColor = if (isRed) Color.parseColor("#B30000") else Color.BLACK
        val bw = 0.9f * s
        renderCyclistVector(canvas, cx, cy, size, borderColor, borderColor, 0f)
        renderCyclistVector(canvas, cx, cy, size, fillColor, borderColor, bw)
        ghostWidgetFillPaint.style = Paint.Style.FILL
    }

    private val cyclistBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    private fun renderCyclistVector(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, borderColor: Int, bw: Float) {
        val s = size / 24f // Cuadrícula 24x24; silueta de ciclista de carretera en posición aerodinámica
        ghostWidgetStrokePaint.color = color
        ghostWidgetFillPaint.color = color
        ghostWidgetFillPaint.style = Paint.Style.FILL
        fun px(x: Float) = cx + x * s
        fun py(y: Float) = cy + y * s
        // Grosor del trazo: en la pasada de relleno se reduce para dejar visible el borde interior
        fun sw(w: Float) = if (bw > 0f) (w * s - 2f * bw).coerceAtLeast(0.4f) else w * s
        fun fillShape(path: Path) {
            canvas.drawPath(path, ghostWidgetFillPaint)
            if (bw > 0f) {
                cyclistBorderPaint.color = borderColor
                cyclistBorderPaint.strokeWidth = 2f * bw
                canvas.save()
                canvas.clipPath(path)
                canvas.drawPath(path, cyclistBorderPaint)
                canvas.restore()
            }
        }
        fun fillCircle(x: Float, y: Float, r: Float) = fillShape(Path().apply { addCircle(px(x), py(y), r * s, Path.Direction.CW) })

        // Ruedas
        val rWheel = 4.7f
        val wheelY = 4.6f
        ghostWidgetStrokePaint.strokeWidth = sw(1.5f)
        canvas.drawCircle(px(-7.4f), py(wheelY), rWheel * s, ghostWidgetStrokePaint)
        canvas.drawCircle(px(7.4f), py(wheelY), rWheel * s, ghostWidgetStrokePaint)
        fillCircle(-7.4f, wheelY, 1.0f)
        fillCircle(7.4f, wheelY, 1.0f)

        // Cuadro
        val bbX = -0.2f; val bbY = 4.6f
        val seatX = -3.2f; val seatY = -2.6f
        val headX = 5.4f; val headY = -0.8f
        ghostWidgetStrokePaint.strokeWidth = sw(1.3f)
        canvas.drawLine(px(-7.4f), py(wheelY), px(bbX), py(bbY), ghostWidgetStrokePaint)
        canvas.drawLine(px(-7.4f), py(wheelY), px(seatX), py(seatY), ghostWidgetStrokePaint)
        canvas.drawLine(px(seatX), py(seatY), px(bbX), py(bbY), ghostWidgetStrokePaint)
        canvas.drawLine(px(bbX), py(bbY), px(headX), py(headY), ghostWidgetStrokePaint)
        canvas.drawLine(px(seatX), py(seatY), px(headX), py(headY), ghostWidgetStrokePaint)
        canvas.drawLine(px(headX), py(headY), px(7.4f), py(wheelY), ghostWidgetStrokePaint)
        // Plato
        fillCircle(bbX, bbY, 1.5f)

        // Sillín
        ghostWidgetStrokePaint.strokeWidth = sw(1.6f)
        canvas.drawLine(px(-4.6f), py(-3.1f), px(-1.4f), py(-2.9f), ghostWidgetStrokePaint)

        // Torso macizo inclinado hacia delante
        val torso = Path().apply {
            moveTo(px(-5.2f), py(-3.6f))
            cubicTo(px(-4.6f), py(-7.6f), px(0.2f), py(-8.4f), px(3.2f), py(-6.6f))
            lineTo(px(2.4f), py(-4.4f))
            cubicTo(px(0.2f), py(-3.6f), px(-1.4f), py(-1.6f), px(-3.0f), py(-1.4f))
            close()
        }
        fillShape(torso)

        // Brazo hasta el manillar y gancho de las manetas
        ghostWidgetStrokePaint.strokeWidth = sw(2.0f)
        val arm = Path().apply {
            moveTo(px(2.8f), py(-5.6f))
            lineTo(px(4.6f), py(-2.6f))
            lineTo(px(6.4f), py(-1.6f))
        }
        canvas.drawPath(arm, ghostWidgetStrokePaint)
        ghostWidgetStrokePaint.strokeWidth = sw(1.4f)
        val drop = Path().apply {
            moveTo(px(5.6f), py(-2.8f))
            lineTo(px(7.4f), py(-2.8f))
            lineTo(px(7.6f), py(-1.0f))
        }
        canvas.drawPath(drop, ghostWidgetStrokePaint)

        // Pierna: muslo, espinilla y pie
        val kneeX = 2.6f; val kneeY = -0.6f
        ghostWidgetStrokePaint.strokeWidth = sw(3.0f)
        canvas.drawLine(px(-3.6f), py(-2.6f), px(kneeX), py(kneeY), ghostWidgetStrokePaint)
        ghostWidgetStrokePaint.strokeWidth = sw(2.3f)
        canvas.drawLine(px(kneeX), py(kneeY), px(0.6f), py(4.2f), ghostWidgetStrokePaint)
        fillShape(Path().apply { addOval(android.graphics.RectF(px(-0.8f), py(3.9f), px(2.6f), py(5.6f)), Path.Direction.CW) })

        // Casco
        val helmet = Path().apply {
            moveTo(px(3.6f), py(-6.8f))
            cubicTo(px(4.4f), py(-9.0f), px(7.6f), py(-9.2f), px(8.4f), py(-7.0f))
            cubicTo(px(8.8f), py(-5.8f), px(7.6f), py(-5.0f), px(6.2f), py(-5.2f))
            cubicTo(px(5.0f), py(-5.4f), px(4.0f), py(-5.8f), px(3.6f), py(-6.8f))
            close()
        }
        fillShape(helmet)

        // Líneas de velocidad
        ghostWidgetStrokePaint.strokeWidth = sw(1.1f)
        canvas.drawLine(px(-12.0f), py(-6.8f), px(-8.2f), py(-6.8f), ghostWidgetStrokePaint)
        canvas.drawLine(px(-12.8f), py(-4.6f), px(-7.6f), py(-4.6f), ghostWidgetStrokePaint)
        canvas.drawLine(px(-12.0f), py(-2.4f), px(-8.2f), py(-2.4f), ghostWidgetStrokePaint)
    }

    private fun drawThemedBand(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        colorHex: String,
        theme: String,
        isHorizontal: Boolean
    ) {
        val baseColor = Color.parseColor(colorHex)
        val w = right - left
        val h = bottom - top

        when (theme.lowercase(java.util.Locale.getDefault())) {
            "oasis" -> {
                // Smooth gradient + shiny top
                val hsv = FloatArray(3)
                Color.colorToHSV(baseColor, hsv)
                hsv[2] = Math.min(1.0f, hsv[2] * 1.3f)
                val lightColor = Color.HSVToColor(hsv)
                hsv[2] = Math.max(0.0f, hsv[2] * 0.5f) // darken from original (1.3 * 0.5 = 0.65)
                val darkColor = Color.HSVToColor(hsv)

                val grad = android.graphics.LinearGradient(
                    0f, top, 0f, bottom,
                    intArrayOf(lightColor, baseColor, darkColor),
                    floatArrayOf(0f, 0.4f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                segmentPaint.shader = grad
                canvas.drawRect(left, top, right + 1f, bottom, segmentPaint)
                segmentPaint.shader = null

                // Shiny highlight
                val shinePaint = Paint().apply {
                    color = Color.argb(40, 255, 255, 255)
                    style = Paint.Style.FILL
                }
                if (isHorizontal) {
                    canvas.drawRect(left, top, right + 1f, top + h * 0.2f, shinePaint)
                }
            }
            "crisoles" -> {
                // Inner dark glow, bright center
                val hsv = FloatArray(3)
                Color.colorToHSV(baseColor, hsv)
                hsv[2] = Math.max(0.0f, hsv[2] * 0.5f)
                val darkColor = Color.HSVToColor(hsv)
                hsv[2] = Math.min(1.0f, hsv[2] * 2.4f)
                val lightColor = Color.HSVToColor(hsv)

                val grad = android.graphics.LinearGradient(
                    0f, top, 0f, bottom,
                    intArrayOf(darkColor, lightColor, darkColor),
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )
                segmentPaint.shader = grad
                canvas.drawRect(left, top, right + 1f, bottom, segmentPaint)
                segmentPaint.shader = null

                // Add inner shadow
                val innerShadow = Paint().apply {
                    color = Color.argb(100, 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawRect(left, top, right, bottom, innerShadow)
            }
            "bruma" -> {
                // Glassmorphism: Desaturated color with high transparency and white borders
                val glassColor = Color.argb(
                    180,
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                )
                segmentPaint.color = glassColor
                canvas.drawRect(left, top, right + 1f, bottom, segmentPaint)

                val glassGlow = android.graphics.LinearGradient(
                    left, top, right, bottom,
                    intArrayOf(Color.argb(80, 255, 255, 255), Color.TRANSPARENT),
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )
                val glowPaint = Paint().apply {
                    shader = glassGlow
                }
                canvas.drawRect(left, top, right + 1f, bottom, glowPaint)

                // Glass border
                val borderPaint = Paint().apply {
                    color = Color.argb(60, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRect(left, top, right, bottom, borderPaint)
            }
            "campo de fuerza" -> {
                // Neon Grid
                val hsv = FloatArray(3)
                Color.colorToHSV(baseColor, hsv)
                hsv[2] = Math.max(0.0f, hsv[2] * 0.8f)
                val darkColor = Color.HSVToColor(hsv)
                hsv[2] = Math.min(1.0f, hsv[2] * 1.8f)
                val neonColor = Color.HSVToColor(hsv)

                segmentPaint.color = darkColor
                canvas.drawRect(left, top, right + 1f, bottom, segmentPaint)

                val neonBorder = Paint().apply {
                    color = neonColor
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    setShadowLayer(8f, 0f, 0f, color)
                }
                canvas.drawRect(left, top, right, bottom, neonBorder)

                // Draw some grid lines
                val gridPaint = Paint().apply {
                    color = Color.argb(50, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                if (isHorizontal && w > 10f) {
                    canvas.drawLine(left + w / 2, top, left + w / 2, bottom, gridPaint)
                }
            }
            else -> { // Estándar
                segmentPaint.color = baseColor
                canvas.drawRect(left, top, right + 1f, bottom, segmentPaint)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        // Aplicar fuente del usuario a los textos
        val fontFamilyKey = AppPreferences.getInstance(context).fontFamilyKey
        FontHelper.applyFontToPaint(percentTextPaint, fontFamilyKey, Typeface.BOLD)
        FontHelper.applyFontToPaint(tickTextPaint, fontFamilyKey, Typeface.NORMAL)
        FontHelper.applyFontToPaint(ghostTextPaint, fontFamilyKey, Typeface.BOLD)
        FontHelper.applyFontToPaint(maxGradeArrowPaint, fontFamilyKey, Typeface.BOLD)
        FontHelper.applyFontToPaint(histPaint, fontFamilyKey, Typeface.BOLD)
        // Escala de letra del panel de estilo: los textos crecen/decrecen y la franja se reparte en consecuencia
        val fontScale = AppPreferences.getInstance(context).fontSize3dScale

        val strat = strategyData ?: return

        val w = width.toFloat()
        val h = height.toFloat()
        val blocks = strat.subBlocks + strat.rightBufferBlocks
        if (blocks.isEmpty()) return

        val numBlocks = blocks.size
        val isHorizontal = w > h

        // Agrupar por colores para calcular la pendiente media de cada franja visual
        val bands = mutableListOf<ColorBand>()
        val firstGrade = blocks[0].toDouble()
        var currentColor = if (firstGrade <= -900.0) "#9E9E9E" else GradeColorScale.getColorHex(firstGrade)
        var currentBand = ColorBand(0, 0, currentColor, blocks[0].toDouble())
        bands.add(currentBand)

        for (i in 1 until numBlocks) {
            val grade = blocks[i].toDouble()
            val color = if (grade <= -900.0) "#9E9E9E" else GradeColorScale.getColorHex(grade)
            if (color == currentColor) {
                currentBand.endIndex = i
                currentBand.sumGrade += grade
            } else {
                currentColor = color
                currentBand = ColorBand(i, i, currentColor, grade)
                bands.add(currentBand)
            }
        }

        val ruleBackgroundPaint = Paint().apply { color = Color.parseColor("#151515"); style = Paint.Style.FILL }

        if (isHorizontal) {
            // HORIZONTAL MODE
            // Reparto vertical según lo activo: barras inferiores, alertas, regla y franja de pendiente
            val bottomBarH = 12f
            val barsH = (if (showEnergyBar) bottomBarH else 0f) + (if (showPowerBar) bottomBarH else 0f)
            val nonRadarH = if (showRadarAlerts) h * 0.40f else barsH
            val radarH = h - nonRadarH
            val alertH = if (showRadarAlerts) nonRadarH - barsH else 0f
            val rulerH = (15f + 2f + RULER_TEXT_SIZE / 0.85f).coerceIn(radarH * 0.25f, radarH * 0.42f)
            val headerHeight = radarH - rulerH
            // 1. Dibujar franjas de color con ligera perspectiva isométrica
            if (showRadar3d) {
                canvas.save()
                val skewFactor = -0.35f // controla la inclinación para efecto 3D
                canvas.skew(skewFactor, 0f)
            }
            val expectedBlocks = (strat.windowLengthMeters / strat.subBlockSizeMeters).toFloat()
            val blockWidth = w / expectedBlocks
            val cWidth = 30f
            val arrowX = (w * 0.25f).coerceAtLeast(cWidth / 2f)
            val progressOffset = (w * strat.riderProgress) - arrowX

            for (i in bands.indices) {
                val band = bands[i]
                val left = band.startIndex * blockWidth - progressOffset
                val right = (band.endIndex + 1) * blockWidth - progressOffset
                drawThemedBand(canvas, left, 0f, right, headerHeight, band.colorHex, radarTheme, true)
            }
            
            // Draw gradients in a second pass so they don't get overdrawn by subsequent bands
            val themeLower = radarTheme.lowercase(java.util.Locale.getDefault())
            val isClassicTheme = themeLower == "estándar" || themeLower == "estandar" || themeLower == "clásico" || themeLower == "clasico"
            if (AppPreferences.getInstance(context).eliteSmoothGradientsEnabled && isClassicTheme) {
                for (i in 0 until bands.size - 1) {
                    val band = bands[i]
                    val nextBand = bands[i+1]
                    val boundary = (band.endIndex + 1) * blockWidth - progressOffset
                    val blendW = blockWidth * 1.5f // Aumentamos el ancho del difuminado para que sea mucho más suave
                    
                    if (boundary > -blendW && boundary < w + blendW) {
                        val color1 = Color.parseColor(band.colorHex)
                        val color2 = Color.parseColor(nextBand.colorHex)
                        val gradient = android.graphics.LinearGradient(
                            boundary - blendW/2f, 0f, boundary + blendW/2f, 0f,
                            color1, color2, android.graphics.Shader.TileMode.CLAMP
                        )
                        val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = gradient
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(boundary - blendW/2f, 0f, boundary + blendW/2f, headerHeight, blendPaint)
                    }
                }
            }
            if (showRadar3d) {
                canvas.restore()
            }

            // Draw checkered start pattern if we are before the actual route starts
            val totalLookahead = strat.windowLengthMeters
            val startX = w * (0.0 - strat.windowStartMeters).toFloat() / totalLookahead.toFloat() - progressOffset
            if (startX > 0f) {
                canvas.drawRect(0f, 0f, startX, headerHeight, checkeredPaint)
            }

            // Draw checkered end pattern if the route finishes within the window
            var endX = w.toFloat() + Math.abs(progressOffset)
            if (strat.routeTotalLength > 0.0) {
                endX = w * (strat.routeTotalLength - strat.windowStartMeters).toFloat() / totalLookahead.toFloat() - progressOffset
                if (endX < w) {
                    canvas.drawRect(endX, 0f, w.toFloat(), headerHeight, checkeredPaint)
                }
            }

            // 2. Dibujar overlay oscuro (pasado) a la izquierda de la flecha estática
            if (arrowX > startX) {
                // Oscurecer lo que queda atrás (solo en la franja superior donde hay colores)
                // Hacemos que el overlay empiece desde startX para no oscurecer la bandera de cuadros
                canvas.drawRect(Math.max(0f, startX), 0f, arrowX, headerHeight, pastOverlayPaint)
            }

            // 3. Textos de porcentaje medio en la franja superior (sin el fondo oscurecido general)
            percentTextPaint.setShadowLayer(4f, 0f, 2f, Color.BLACK) // Sombra fuerte para legibilidad sin fondo oscuro
            FontHelper.applyFontToPaint(maxGradeBlockPaint, fontFamilyKey, Typeface.BOLD)

            // DYNAMIC SCALING FOR HEIGHT COMPRESSION
            val currentMaxGradeTextSize = Math.min(24f * fontScale, headerHeight * 0.45f).coerceAtLeast(10f)
            maxGradeBlockPaint.textSize = currentMaxGradeTextSize
            maxGradeArrowPaint.textSize = currentMaxGradeTextSize
            percentTextPaint.textSize = Math.min(54f * fontScale, headerHeight * 0.9f).coerceAtLeast(12f)

            val cyPercent = (headerHeight / 2f) + (percentTextPaint.textSize / 3f)
            // getSubBlockSize only returns 50.0 or 100.0 for typical route lookaheads
            val showMaxGrade = strat.subBlockSizeMeters <= 100.0 && strat.subBlocksMax.isNotEmpty()
            for (band in bands) {
                val left = band.startIndex * blockWidth - progressOffset
                val right = (band.endIndex + 1) * blockWidth - progressOffset
                val bandWidth = right - left

                // Si la banda está fuera de la pantalla no dibujar su %
                if (right <= 0f || left >= w) continue

                if (bandWidth > percentTextPaint.textSize * 1.6f) {
                    val count = band.endIndex - band.startIndex + 1
                    val avgGrade = (Math.round(band.sumGrade / count)).toInt()
                    // Adjust drawing to not overlap with checkered start/end if it crosses them
                    val actualLeft = Math.max(left, startX)
                    val actualRight = Math.min(right, endX)
                    val drawCenter = actualLeft + (actualRight - actualLeft) / 2f
                    drawGradeLabel(canvas, avgGrade, drawCenter, cyPercent, percentTextPaint.textSize)

                    // Max grade label (top-left corner) — computed as extreme across all subBlocks in the band
                    if (showMaxGrade) {
                        val blockGrades = (band.startIndex..band.endIndex)
                            .mapNotNull { strat.subBlocksMax.getOrNull(it) }
                        val maxG = blockGrades.maxOrNull()
                        val minG = blockGrades.minOrNull()
                        // Pick the most extreme value: if avg is negative, look at the most negative peak
                        val extremeG = if (avgGrade < 0 && minG != null && minG < avgGrade - 1f) minG
                                       else if (maxG != null && maxG > avgGrade + 1f) maxG
                                       else null
                        if (extremeG != null) {
                            val arrow = if (extremeG < 0) "▼" else "▲"
                            val maxLabel = "${Math.abs(Math.round(extremeG))}%"

                            val labelX = actualLeft + currentMaxGradeTextSize // Center relative to text
                            val arrowY = maxGradeArrowPaint.textSize + 4f
                            val textY = arrowY + maxGradeBlockPaint.textSize + 2f

                            val baseColor = Color.parseColor(band.colorHex)
                            val hsv = FloatArray(3)
                            Color.colorToHSV(baseColor, hsv)
                            hsv[1] = Math.min(1.0f, hsv[1] * 1.2f) // Increase saturation a bit
                            hsv[2] *= 0.5f // Decrease brightness for a darker tone
                            maxGradeArrowPaint.color = Color.HSVToColor(hsv)

                            canvas.drawText(arrow, labelX, arrowY, maxGradeArrowPaint)
                            canvas.drawText(maxLabel, labelX, textY, maxGradeBlockPaint)
                        }
                    }
                }
            }

            // 4. Franja Intermedia (Regla / Ruler)
            if (showRadar3d) {
                canvas.save()
                canvas.skew(-0.35f, 0f)
                canvas.drawRect(-w, headerHeight, w * 2, radarH, ruleBackgroundPaint)
            } else {
                canvas.drawRect(0f, headerHeight, w, radarH, ruleBackgroundPaint)
            }
            // Línea divisoria blanca
            canvas.drawLine(-w, headerHeight, w * 2, headerHeight, dividerPaint)

            // Ticks de distancia (Escala dinámica)
            val lookahead = strat.windowLengthMeters
            val tickInterval = when {
                lookahead <= 350 -> 50.0
                lookahead <= 700 -> 100.0
                lookahead <= 1000 -> 250.0
                lookahead <= 2000 -> 500.0
                lookahead <= 5000 -> 1000.0
                else -> 2000.0
            }
            val startTick = Math.ceil((strat.windowStartMeters - lookahead) / tickInterval) * tickInterval
            var tickDist = startTick
            tickTextPaint.textAlign = Paint.Align.CENTER
            while (tickDist < strat.windowStartMeters + lookahead * 2) {
                val progress = (tickDist - strat.windowStartMeters) / lookahead
                val px = (w * progress).toFloat() - progressOffset
                if (px in -50f..(w + 50f)) {
                    // Dynamic scaling for horizontal ruler
                    val rulerSpace = radarH - headerHeight
                    val dynamicTickLen = Math.min(15f, rulerSpace * 0.25f)
                    val maxTextHeight = rulerSpace - dynamicTickLen - 2f
                    val currentTextSize = Math.min(RULER_TEXT_SIZE, maxTextHeight * 0.85f).coerceAtLeast(10f)
                    tickTextPaint.textSize = currentTextSize

                    // Línea de marca hacia abajo
                    canvas.drawLine(px, headerHeight, px, headerHeight + dynamicTickLen, dividerPaint)
                    // Texto (un-skew if 3D)
                    val kmLabel = if (tickDist >= 1000) {
                        val km = tickDist / 1000.0
                        val kmStr = String.format(java.util.Locale.US, "%.2f", km).trimEnd('0').trimEnd('.')
                        "${kmStr}km"
                    } else {
                        "${tickDist.toInt()}m"
                    }
                    // Baseline alignment avoiding overlap with alert bar
                    val cyTick = headerHeight + dynamicTickLen + 2f + tickTextPaint.textSize * 0.8f
                    if (showRadar3d) {
                        canvas.save()
                        canvas.translate(px, cyTick)
                        canvas.skew(0.35f, 0f) // unskew
                        canvas.drawText(kmLabel, 0f, 0f, tickTextPaint)
                        canvas.restore()
                    } else {
                        canvas.drawText(kmLabel, px, cyTick, tickTextPaint)
                    }
                }
                tickDist += tickInterval
            }
            if (showRadar3d) {
                canvas.restore()
            }

            // 5. Indicador de posición (Ciclista)
            val path = Path()
            val cHeight = headerHeight * 0.8f
            path.moveTo(arrowX, headerHeight) // Punta abajo (tocando la divisoria)
            path.lineTo(arrowX - (cWidth / 2f), 0f) // Esquina superior izq
            path.lineTo(arrowX, 10f) // Centro arriba
            path.lineTo(arrowX + (cWidth / 2f), 0f) // Esquina superior der
            path.close()
            canvas.drawPath(path, cyclistPaint)
            canvas.drawPath(path, cyclistOutline)

            // 5c. POI icons on ruler (ELITE)
            if (showPoiRuler && strat.pois.isNotEmpty()) {
                if (showRadar3d) {
                    canvas.save()
                    canvas.skew(-0.35f, 0f)
                }
                for (poi in strat.pois) {
                    val poiProgress = poi.relativeDistance / lookahead
                    val poiX = (w * poiProgress).toFloat() - progressOffset
                    if (poiX < startX || poiX > endX) continue
                    if (poiX in -50f..(w + 50f)) {
                        val icon = poi.icon.ifEmpty {
                            when (poi.type) {
                                PoiType.WATER -> "💧"
                                PoiType.TOWN -> "🏘"
                                PoiType.SUMMIT -> "🔺"
                                PoiType.VIEWPOINT -> "📷"
                            }
                        }
                        // Draw a small vertical tick at ruler level and the icon above
                        canvas.drawLine(poiX, headerHeight, poiX, headerHeight + 22f, dividerPaint)

                        // Draw unskewed icon
                        if (showRadar3d) {
                            canvas.save()
                            canvas.translate(poiX, headerHeight + 42f)
                            canvas.skew(0.35f, 0f)
                            canvas.drawText(icon, 0f, 0f, poiTextPaint)
                            canvas.restore()
                        } else {
                            canvas.drawText(icon, poiX, headerHeight + 42f, poiTextPaint)
                        }
                    }
                }
                if (showRadar3d) {
                    canvas.restore()
                }
            }

            // 6. Alertas dinámicas
            if (showRadarAlerts) {
                drawAlerts(canvas, strat, isHorizontal = true, 0f, radarH, w, radarH + alertH, fontScale)
            }

            // 7. Energy / Power Bars (ELITE) — thin horizontal bars below the alert strip
            var bottomOffset = 0f
            if (showEnergyBar) {
                val barTop = h - bottomOffset - 12f
                val barHeight = 12f
                val energyLevel = strat.energyBatteryLevel.coerceIn(0.0, 100.0).toFloat() / 100f
                canvas.drawRect(0f, barTop, w, barTop + barHeight, energyBgPaint)
                val barColor = when {
                    energyLevel > 0.6f -> Color.parseColor("#4CAF50")
                    energyLevel > 0.3f -> Color.parseColor("#FFC107")
                    else -> Color.parseColor("#F44336")
                }
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColor; style = Paint.Style.FILL }
                canvas.drawRect(0f, barTop, w * energyLevel, barTop + barHeight, barPaint)
                bottomOffset += 12f
            }

            if (showPowerBar) {
                val barTop = h - bottomOffset - 12f
                val barHeight = 12f
                canvas.drawRect(0f, barTop, w, barTop + barHeight, energyBgPaint)

                val currentPwr = strat.currentPower.toFloat()
                val ftp = userFtp.toFloat().coerceAtLeast(1f)
                val percentFtp = currentPwr / ftp

                val barColor = when {
                    percentFtp < 0.55f -> Color.parseColor("#808080") // Z1: Active Recovery (Gray)
                    percentFtp < 0.75f -> Color.parseColor("#4CAF50") // Z2: Endurance (Green)
                    percentFtp < 0.90f -> Color.parseColor("#FFEB3B") // Z3: Tempo (Yellow)
                    percentFtp < 1.05f -> Color.parseColor("#FF9800") // Z4: Threshold (Orange)
                    percentFtp < 1.20f -> Color.parseColor("#F44336") // Z5: VO2 Max (Red)
                    percentFtp < 1.50f -> Color.parseColor("#9C27B0") // Z6: Anaerobic Capacity (Purple)
                    else -> Color.parseColor("#FFEB3B") // Z7: Neuromuscular (Yellow/White - let's use White or bright yellow)
                }
                if (percentFtp >= 1.50f) {
                    val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                    canvas.drawRect(0f, barTop, w, barTop + barHeight, p2)
                } else {
                    val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColor; style = Paint.Style.FILL }
                    // Scale it so that Z1-Z6 covers the width linearly, capping at 1.5x FTP
                    val widthScale = (percentFtp / 1.5f).coerceIn(0f, 1f)
                    canvas.drawRect(0f, barTop, w * widthScale, barTop + barHeight, p2)
                }
                bottomOffset += 12f
            }

            // 8. Grade Histogram (ELITE) — mini panel on the right side of the ruler strip
            if (showHistogram && strat.subBlocks.isNotEmpty()) {
                drawGradeHistogram(canvas, strat, headerHeight, radarH, w)
            }

        } else {
            // VERTICAL MODE
            val sideBarW = 12f
            val barsW = (if (showEnergyBar) sideBarW else 0f) + (if (showPowerBar) sideBarW else 0f)
            val nonRadarW = if (showRadarAlerts) w * 0.40f else barsW
            val radarW = w - nonRadarW
            val alertW = if (showRadarAlerts) nonRadarW - barsW else 0f
            tickTextPaint.textSize = RULER_TEXT_SIZE
            val rulerW = (tickTextPaint.measureText("8888m") + 21f).coerceIn(radarW * 0.25f, radarW * 0.5f)
            val headerWidth = radarW - rulerW
            val blockHeight = h * (strat.subBlockSizeMeters / strat.windowLengthMeters).toFloat()

            // 1. Dibujar franjas de color con ligera perspectiva isométrica (vertical mode)
            if (showRadar3d) {
                canvas.save()
                val skewFactorV = -0.35f // inclinación vertical para efecto 3D
                canvas.skew(0f, skewFactorV)
            }
            for (i in bands.indices) {
                val band = bands[i]
                val topBandY = h - ((band.endIndex + 1) * blockHeight)
                val bottomBandY = h - (band.startIndex * blockHeight)
                drawThemedBand(canvas, 0f, topBandY - 1f, headerWidth, bottomBandY, band.colorHex, radarTheme, false)
            }
            
            // Draw gradients in a second pass
            val themeLowerV = radarTheme.lowercase(java.util.Locale.getDefault())
            val isClassicThemeV = themeLowerV == "estándar" || themeLowerV == "estandar" || themeLowerV == "clásico" || themeLowerV == "clasico"
            if (AppPreferences.getInstance(context).eliteSmoothGradientsEnabled && isClassicThemeV) {
                for (i in 0 until bands.size - 1) {
                    val band = bands[i]
                    val nextBand = bands[i+1]
                    val boundary = h - ((band.endIndex + 1) * blockHeight)
                    val blendH = blockHeight * 1.5f // Aumentamos la altura del difuminado para que sea más suave
                    
                    if (boundary > -blendH && boundary < h + blendH) {
                        val color1 = Color.parseColor(band.colorHex)
                        val color2 = Color.parseColor(nextBand.colorHex)
                        val gradient = android.graphics.LinearGradient(
                            0f, boundary + blendH/2f, 0f, boundary - blendH/2f,
                            color1, color2, android.graphics.Shader.TileMode.CLAMP
                        )
                        val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = gradient
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(0f, boundary - blendH/2f, headerWidth, boundary + blendH/2f, blendPaint)
                    }
                }
            }
            if (showRadar3d) {
                canvas.restore()
            }

            // Draw checkered start pattern if before actual route starts
            val totalLookahead = strat.windowLengthMeters
            val startY = h - (h * (0.0 - strat.windowStartMeters).toFloat() / totalLookahead.toFloat())
            if (startY < h) {
                canvas.drawRect(0f, startY, headerWidth, h, checkeredPaint)
            }

            // Draw checkered end pattern if route finishes within window
            var endY = 0f
            if (strat.routeTotalLength > 0.0) {
                endY = h - (h * (strat.routeTotalLength - strat.windowStartMeters).toFloat() / totalLookahead.toFloat())
                if (endY > 0f) {
                    canvas.drawRect(0f, 0f, headerWidth, endY, checkeredPaint)
                }
            }

            // 2. Oscurecer lo que queda atrás (abajo)
            val cHeight = 30f
            var riderY = (h - (h * strat.riderProgress)).coerceAtLeast(cHeight / 2f).coerceAtMost(h - (cHeight / 2f))

            if (startY < h) {
                val distMeters = (startY - riderY) * totalLookahead / h
                if (distMeters in 0f..25f) {
                    riderY = startY
                }
            }

            if (riderY < h - (cHeight / 2f)) {
                // Overlay termina en startY para no oscurecer la bandera
                val limitY = Math.min(h.toFloat(), startY)
                canvas.drawRect(0f, riderY, headerWidth, limitY, pastOverlayPaint)
            }

            // 3. Textos de %
            percentTextPaint.setShadowLayer(4f, 0f, 2f, Color.BLACK)
            // DYNAMIC SCALING FOR WIDTH COMPRESSION IN VERTICAL MODE
            percentTextPaint.textSize = Math.min(54f * fontScale, headerWidth * 0.9f).coerceAtLeast(12f)
            val cxPercent = headerWidth / 2f
            for (band in bands) {
                val top = h - ((band.endIndex + 1) * blockHeight)
                val bottom = h - (band.startIndex * blockHeight)
                val bandHeight = bottom - top

                // Si la banda está debajo de startY (pre-ruta), no dibujar
                if (top >= startY) continue
                // Si la banda está arriba de endY (meta), tampoco dibujar
                if (bottom <= endY) continue

                if (bandHeight > percentTextPaint.textSize * 1.25f) {
                    val count = band.endIndex - band.startIndex + 1
                    val avgGrade = (Math.round(band.sumGrade / count)).toInt()
                    // Adjust drawing to not overlap with checkered start/end
                    val actualBottom = Math.min(bottom, startY)
                    val actualTop = Math.max(top, endY)
                    val cyPercent = actualTop + ((actualBottom - actualTop) / 2f) + (percentTextPaint.textSize / 3f)
                    drawGradeLabel(canvas, avgGrade, cxPercent, cyPercent, percentTextPaint.textSize)
                }
            }

            // 4. Franja Intermedia (Regla)
            if (showRadar3d) {
                canvas.save()
                canvas.skew(0f, -0.35f)
                canvas.drawRect(headerWidth, -h, radarW, h * 2, ruleBackgroundPaint)
            } else {
                canvas.drawRect(headerWidth, 0f, radarW, h, ruleBackgroundPaint)
            }
            canvas.drawLine(headerWidth, -h, headerWidth, h * 2, dividerPaint)

            // Ticks de distancia
            val lookahead = strat.windowLengthMeters
            val tickInterval = when {
                lookahead <= 350 -> 50.0
                lookahead <= 700 -> 100.0
                lookahead <= 1000 -> 250.0
                lookahead <= 2000 -> 500.0
                lookahead <= 5000 -> 1000.0
                else -> 2000.0
            }
            val startTick = Math.ceil(strat.windowStartMeters / tickInterval) * tickInterval
            var tickDist = startTick
            tickTextPaint.textAlign = Paint.Align.LEFT
            while (tickDist < strat.windowStartMeters + lookahead) {
                val progress = (tickDist - strat.windowStartMeters) / lookahead
                val py = h - (h * progress).toFloat()
                // Dynamic scaling for vertical ruler
                val rulerSpace = radarW - headerWidth
                val dynamicTickLen = Math.min(15f, rulerSpace * 0.25f)

                canvas.drawLine(headerWidth, py, headerWidth + dynamicTickLen, py, dividerPaint)
                val kmLabel = if (tickDist >= 1000) "${(tickDist / 1000).toInt()}km" else "${tickDist.toInt()}m"

                // Scale down if it's too wide
                tickTextPaint.textSize = RULER_TEXT_SIZE
                var currentTextSize = RULER_TEXT_SIZE
                val maxTextWidth = rulerSpace - dynamicTickLen - 4f
                if (tickTextPaint.measureText(kmLabel) > maxTextWidth) {
                    currentTextSize = currentTextSize * (maxTextWidth / tickTextPaint.measureText(kmLabel))
                    tickTextPaint.textSize = currentTextSize.coerceAtLeast(10f)
                }

                val cyTick = py + (tickTextPaint.textSize / 3f)
                if (showRadar3d) {
                    canvas.save()
                    canvas.translate(headerWidth + dynamicTickLen + 2f, cyTick)
                    canvas.skew(0f, 0.35f)
                    canvas.drawText(kmLabel, 0f, 0f, tickTextPaint)
                    canvas.restore()
                } else {
                    canvas.drawText(kmLabel, headerWidth + dynamicTickLen + 2f, cyTick, tickTextPaint)
                }
                tickDist += tickInterval
            }
            if (showRadar3d) {
                canvas.restore()
            }

            // 5. Indicador de posición (Ciclista)
            val path = Path()
            val cWidth = headerWidth * 0.8f
            path.moveTo(headerWidth, riderY) // Punta izq (tocando la divisoria)
            path.lineTo(headerWidth - cWidth, riderY + (cHeight / 2f)) // Esquina abajo
            path.lineTo(headerWidth - cWidth + 10f, riderY) // Centro izq (muesca)
            path.lineTo(headerWidth - cWidth, riderY - (cHeight / 2f)) // Esquina arriba
            path.close()
            canvas.drawPath(path, cyclistPaint)
            canvas.drawPath(path, cyclistOutline)

            // 7. Energy / Power Bars (ELITE) — thin vertical bars to the right of the radar
            var leftOffset = radarW
            if (showEnergyBar) {
                val barLeft = leftOffset
                val barWidth = sideBarW
                val energyLevel = strat.energyBatteryLevel.coerceIn(0.0, 100.0).toFloat() / 100f
                canvas.drawRect(barLeft, 0f, barLeft + barWidth, h, energyBgPaint)
                val barColor = when {
                    energyLevel > 0.6f -> Color.parseColor("#4CAF50")
                    energyLevel > 0.3f -> Color.parseColor("#FFC107")
                    else -> Color.parseColor("#F44336")
                }
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColor; style = Paint.Style.FILL }
                canvas.drawRect(barLeft, h * (1f - energyLevel), barLeft + barWidth, h, barPaint)
                leftOffset += sideBarW
            }

            if (showPowerBar) {
                val barLeft = leftOffset
                val barWidth = sideBarW
                canvas.drawRect(barLeft, 0f, barLeft + barWidth, h, energyBgPaint)

                val currentPwr = strat.currentPower.toFloat()
                val ftp = userFtp.toFloat().coerceAtLeast(1f)
                val percentFtp = currentPwr / ftp

                val barColor = when {
                    percentFtp < 0.55f -> Color.parseColor("#808080")
                    percentFtp < 0.75f -> Color.parseColor("#4CAF50")
                    percentFtp < 0.90f -> Color.parseColor("#FFEB3B")
                    percentFtp < 1.05f -> Color.parseColor("#FF9800")
                    percentFtp < 1.20f -> Color.parseColor("#F44336")
                    percentFtp < 1.50f -> Color.parseColor("#9C27B0")
                    else -> Color.parseColor("#FFEB3B")
                }
                if (percentFtp >= 1.50f) {
                    val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                    canvas.drawRect(barLeft, 0f, barLeft + barWidth, h, p2)
                } else {
                    val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barColor; style = Paint.Style.FILL }
                    val heightScale = (percentFtp / 1.5f).coerceIn(0f, 1f)
                    canvas.drawRect(barLeft, h * (1f - heightScale), barLeft + barWidth, h, p2)
                }
                leftOffset += sideBarW
            }

            // 6. Alertas dinámicas
            if (showRadarAlerts) {
                drawAlerts(canvas, strat, isHorizontal = false, w - alertW, 0f, w, h, fontScale)
            }
        }
    }

    // Número grande y símbolo % más pequeño, centrados como un único texto
    private fun drawGradeLabel(canvas: Canvas, grade: Int, cx: Float, cy: Float, numberSize: Float) {
        val number = grade.toString()
        val signSize = numberSize * 0.6f
        percentTextPaint.textSize = numberSize
        val numberW = percentTextPaint.measureText(number)
        percentTextPaint.textSize = signSize
        val signW = percentTextPaint.measureText("%")
        val startX = cx - (numberW + signW) / 2f

        percentTextPaint.textAlign = Paint.Align.LEFT
        percentTextPaint.textSize = numberSize
        canvas.drawText(number, startX, cy, percentTextPaint)
        percentTextPaint.textSize = signSize
        // El % va arriba, alineado con la parte alta del número (como el 1ª)
        canvas.drawText("%", startX + numberW, cy - (numberSize - signSize) * 0.72f, percentTextPaint)
        percentTextPaint.textSize = numberSize
        percentTextPaint.textAlign = Paint.Align.CENTER
    }

    private fun formatDist(m: Double): String {
        return when {
            m < 1000 -> "${Math.round(m)}m"
            m < 10000 -> String.format(java.util.Locale.getDefault(), "%.2fkm", m / 1000.0)
            else -> String.format(java.util.Locale.getDefault(), "%.1fkm", m / 1000.0)
        }
    }

    private fun drawAlerts(
        canvas: Canvas,
        strat: StrategyData,
        isHorizontal: Boolean,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        fontScale: Float
    ) {
        // Usar la distancia exacta del ciclista en lugar de la relativa a la ventana
        val trueRiderDist = strat.routeTotalLength - strat.remainingDistance
        
        var alertText = ""
        var alertColorHex = "#212121" // Default dark gray
        
        val currentBlockIdxSafe = (strat.riderProgress * strat.windowLengthMeters / strat.subBlockSizeMeters).toInt().coerceIn(0, strat.subBlocks.size - 1)
        val currentGrade = if (strat.subBlocks.isNotEmpty()) strat.subBlocks[currentBlockIdxSafe].toDouble() else 0.0

        val getTargetPowerStr = { grade: Double ->
            val pct = when {
                grade < 3.0 -> 0.75
                grade < 5.0 -> 0.85
                grade < 8.0 -> 0.95
                grade < 10.0 -> 1.05
                else -> 1.15
            }
            "${(userFtp * pct).toInt()}W"
        }

        val activeClimb = strat.activeClimbs.find { trueRiderDist >= it.startDistance && trueRiderDist < it.endDistance }
        if (activeClimb != null) {
            val currentBlockIdx = (strat.riderProgress * strat.windowLengthMeters / strat.subBlockSizeMeters).toInt()
            var steepDist: Double? = null
            var steepGrade: Float? = null

            for (i in currentBlockIdx until strat.subBlocks.size) {
                val grade = strat.subBlocks[i]
                if (grade >= 12f) {
                    val distToBlock = strat.windowStartMeters + (i * strat.subBlockSizeMeters) - trueRiderDist
                    if (distToBlock > 0 && distToBlock < activeClimb.endDistance - trueRiderDist) {
                        steepDist = distToBlock
                        steepGrade = grade
                        break
                    }
                }
            }

            if (steepDist != null && steepGrade != null && steepDist < 5000) {
                val wallText = context.getString(R.string.alert_wall, steepGrade.toInt().toString(), formatDist(steepDist))
                if (showPowerGuide) {
                    alertText = context.getString(R.string.alert_power_push, getTargetPowerStr(steepGrade.toDouble()), wallText)
                } else {
                    alertText = wallText
                }
                alertColorHex = GradeColorScale.getColorHex(steepGrade.toDouble())
            } else {
                val distToTop = activeClimb.endDistance - trueRiderDist
                val summitText = context.getString(R.string.alert_summit, formatDist(distToTop))
                if (showPowerGuide) {
                    alertText = context.getString(R.string.alert_power_target, getTargetPowerStr(currentGrade), summitText)
                    alertColorHex = GradeColorScale.getColorHex(currentGrade)
                } else {
                    alertText = summitText
                    alertColorHex = "#4CAF50" // Green
                }
            }
        } else {
            val nextClimb = strat.activeClimbs.filter { it.startDistance > trueRiderDist }.minByOrNull { it.startDistance }
            if (nextClimb != null) {
                val distToStart = nextClimb.startDistance - trueRiderDist
                val gradeStr = String.format(java.util.Locale.getDefault(), "%.1f", nextClimb.avgGrade)
                val climbText = context.getString(R.string.alert_climb, formatDist(distToStart), formatDist(nextClimb.length), gradeStr)
                if (showPowerGuide) {
                    alertText = context.getString(R.string.alert_power_prepare, getTargetPowerStr(nextClimb.avgGrade), climbText)
                } else {
                    alertText = climbText
                }
                alertColorHex = GradeColorScale.getColorHex(nextClimb.avgGrade)
            }
        }

        if (alertText.isEmpty()) {
            // Si no hay alerta, el color de la franja debe coincidir con la pendiente actual del ciclista
            alertColorHex = GradeColorScale.getColorHex(currentGrade)
        }

        // Sin más puertos por delante, el último aviso deja paso a la distancia a meta
        if (alertText.isEmpty() && strat.routeTotalLength > 0.0 && strat.remainingDistance > 0.0) {
            alertText = context.getString(R.string.alert_finish, formatDist(strat.remainingDistance))
        }

        // Fondo de la franja: mezclamos el color de alerta con un poco de transparencia/blanco para que sea "más claro"
        val baseColor = Color.parseColor(alertColorHex)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val r = Color.red(baseColor)
            val g = Color.green(baseColor)
            val b = Color.blue(baseColor)
            val mix = 0.4f
            color = Color.rgb(
                (r + (255 - r) * mix).toInt(),
                (g + (255 - g) * mix).toInt(),
                (b + (255 - b) * mix).toInt()
            )
            style = Paint.Style.FILL
        }

        val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK // Texto negro sobre fondo claro
            textSize = 38f // Reduced text size as requested
            textAlign = Paint.Align.CENTER
            setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            FontHelper.applyFontToPaint(this, AppPreferences.getInstance(context).fontFamilyKey, android.graphics.Typeface.BOLD)
        }

        if (isHorizontal) {
            // Franja inferior, justo debajo de la regla
            canvas.drawRect(android.graphics.RectF(left, top, right, bottom), bgPaint)

            val currentGap = ghostGap
            val hasGhost = currentGap != null && (currentGap.timeS != null || currentGap.distM != null)
            val hasAlert = alertText.isNotEmpty()

            // Alternancia: si ambos están disponibles, cicla cada 4 segundos (4000ms Ghost / 4000ms Aviso)
            val showGhostNow = when {
                hasGhost && hasAlert -> ((System.currentTimeMillis() / 4000L) % 2L) == 0L
                hasGhost -> true
                else -> false
            }

            if (showGhostNow && currentGap != null) {
                // Widget KGhost visual profesional: ciclista real vs ciclista ghost
                val barW = right - left
                val barH = bottom - top
                val cy = top + barH / 2f

                // Valores reales
                val diffDist = currentGap.distM ?: 0.0
                val diffTime = currentGap.timeS ?: 0.0
                // true = ciclista real por delante (aparece a la derecha)
                val realAhead = diffDist >= 0.0 || (diffDist == 0.0 && diffTime >= 0.0)

                val distStr = distGapText(diffDist)
                val timeStr = timeGapText(diffTime)
                val gapLabel = "$timeStr / $distStr"

                when (ghostStyle) {
                    "velon" -> {
                        // ── ESTILO VELON LIVE TV (Cápsulas gemelas de retransmisión televisiva) ──
                        val pillH = barH * 0.72f
                        val pillTop = cy - pillH / 2f
                        val pillBottom = cy + pillH / 2f
                        val pillW = barW * 0.30f
                        val cornerR = pillH * 0.28f

                        val leftPillL = left + barW * 0.04f
                        val leftPillR = leftPillL + pillW
                        val rightPillR = right - barW * 0.04f
                        val rightPillL = rightPillR - pillW

                        // Colores de cápsulas según quién va por delante
                        val leadBg = Color.parseColor("#0F172A") // Navy oscuro premium
                        val chaseBg = Color.parseColor("#1E293B")
                        val accentAhead = Color.parseColor("#10B981") // Verde esmeralda WT
                        val accentBehind = Color.parseColor("#EF4444") // Rojo WT

                        // 1. Cápsula Izquierda
                        val leftIsReal = !realAhead
                        val leftBgColor = if (leftIsReal) leadBg else chaseBg
                        val leftAccentColor = if (leftIsReal) (if (realAhead) accentAhead else accentBehind) else Color.parseColor("#94A3B8")
                        val leftTitle = if (leftIsReal) "YOU" else "GHOST"
                        
                        ghostWidgetFillPaint.color = leftBgColor
                        canvas.drawRoundRect(RectF(leftPillL, pillTop, leftPillR, pillBottom), cornerR, cornerR, ghostWidgetFillPaint)
                        // Borde acentuado
                        ghostWidgetStrokePaint.color = leftAccentColor
                        ghostWidgetStrokePaint.strokeWidth = 2.0f
                        canvas.drawRoundRect(RectF(leftPillL, pillTop, leftPillR, pillBottom), cornerR, cornerR, ghostWidgetStrokePaint)

                        // Icono y texto en cápsula izquierda
                        val iconSizeLeft = pillH * 0.58f
                        drawCyclistVector(canvas, leftPillL + iconSizeLeft * 0.75f, cy, iconSizeLeft, leftAccentColor)
                        ghostWidgetTextPaint.textSize = (pillH * 0.38f * fontScale).coerceIn(11f, 20f)
                        ghostWidgetTextPaint.color = Color.WHITE
                        ghostWidgetTextPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText(leftTitle, (leftPillL + iconSizeLeft * 1.3f + leftPillR) / 2f, cy + ghostWidgetTextPaint.textSize * 0.35f, ghostWidgetTextPaint)

                        // 2. Cápsula Derecha (delante si realAhead)
                        val rightIsReal = realAhead
                        val rightBgColor = if (rightIsReal) leadBg else chaseBg
                        val rightAccentColor = if (rightIsReal) (if (realAhead) accentAhead else accentBehind) else Color.parseColor("#94A3B8")
                        val rightTitle = if (rightIsReal) "YOU" else "GHOST"

                        ghostWidgetFillPaint.color = rightBgColor
                        canvas.drawRoundRect(RectF(rightPillL, pillTop, rightPillR, pillBottom), cornerR, cornerR, ghostWidgetFillPaint)
                        ghostWidgetStrokePaint.color = rightAccentColor
                        ghostWidgetStrokePaint.strokeWidth = 2.0f
                        canvas.drawRoundRect(RectF(rightPillL, pillTop, rightPillR, pillBottom), cornerR, cornerR, ghostWidgetStrokePaint)

                        val iconSizeRight = pillH * 0.58f
                        drawCyclistVector(canvas, rightPillL + iconSizeRight * 0.75f, cy, iconSizeRight, rightAccentColor)
                        ghostWidgetTextPaint.textSize = (pillH * 0.38f * fontScale).coerceIn(11f, 20f)
                        ghostWidgetTextPaint.color = Color.WHITE
                        canvas.drawText(rightTitle, (rightPillL + iconSizeRight * 1.3f + rightPillR) / 2f, cy + ghostWidgetTextPaint.textSize * 0.35f, ghostWidgetTextPaint)

                        // 3. Pastilla central flotante de GAP (Amarillo Tour / Blanco con alto contraste)
                        val gapW = rightPillL - leftPillR - 12f
                        if (gapW > 30f) {
                            val gapL = (left + right) / 2f - gapW / 2f
                            val gapR = gapL + gapW
                            val gapH = pillH * 0.88f
                            val gapTop = cy - gapH / 2f
                            val gapBottom = cy + gapH / 2f

                            val badgeBg = if (realAhead) Color.parseColor("#FACC15") else Color.parseColor("#DC2626") // Amarillo Tour líder / Rojo persecución
                            ghostWidgetFillPaint.color = badgeBg
                            canvas.drawRoundRect(RectF(gapL, gapTop, gapR, gapBottom), 6f, 6f, ghostWidgetFillPaint)

                            val badgeTextColor = if (realAhead) Color.BLACK else Color.WHITE
                            ghostWidgetTextPaint.color = badgeTextColor
                            ghostWidgetTextPaint.textSize = (gapH * 0.42f * fontScale).coerceIn(12f, 22f)
                            canvas.drawText(timeStr, (left + right) / 2f, cy - 2f, ghostWidgetTextPaint)
                            ghostWidgetTextPaint.textSize = (gapH * 0.30f * fontScale).coerceIn(9f, 16f)
                            canvas.drawText(distStr, (left + right) / 2f, cy + ghostWidgetTextPaint.textSize + 2f, ghostWidgetTextPaint)
                        }
                    }
                    "pursuit" -> {
                        // ── ESTILO PURSUIT TRACKER (Pista interactiva relativa en tiempo real) ──
                        val trackH = 4f
                        val trackL = left + barW * 0.08f
                        val trackR = right - barW * 0.08f
                        val trackW = trackR - trackL

                        // Carril de asfalto y línea discontinua
                        ghostWidgetFillPaint.color = Color.parseColor("#475569")
                        canvas.drawRoundRect(RectF(trackL, cy - trackH / 2f, trackR, cy + trackH / 2f), 2f, 2f, ghostWidgetFillPaint)

                        // Distancia visual: mapear la diferencia en la pista
                        val maxGapM = 300.0 // +/- 300 metros rango dinámico visual
                        val normDiff = (diffDist / maxGapM).coerceIn(-0.40, 0.40).toFloat()
                        
                        val realCyclistX = trackL + trackW * (0.50f + normDiff)
                        val ghostCyclistX = trackL + trackW * 0.50f // Ghost en punto de referencia central

                        // Rayo de vector relativo entre ambos
                        val beamColor = if (realAhead) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
                        ghostWidgetStrokePaint.color = beamColor
                        ghostWidgetStrokePaint.strokeWidth = 3.5f
                        val minX = Math.min(realCyclistX, ghostCyclistX)
                        val maxX = Math.max(realCyclistX, ghostCyclistX)
                        if (maxX - minX > 8f) {
                            canvas.drawLine(minX, cy, maxX, cy, ghostWidgetStrokePaint)
                        }

                        // Dibujar ambos ciclistas
                        val cSize = (barH * 0.76f).coerceIn(28f, 50f)
                        val realC = Color.parseColor("#E11D48") // Tú (magenta / carmesí potente)
                        val ghostC = Color.parseColor("#64748B") // Ghost (gris acero)

                        drawCyclistVector(canvas, ghostCyclistX, cy + barH * 0.02f, cSize * 0.88f, ghostC)
                        drawCyclistVector(canvas, realCyclistX, cy + barH * 0.02f, cSize, realC)

                        // Badge superior con el Gap
                        ghostWidgetTextPaint.color = Color.BLACK
                        ghostWidgetTextPaint.textSize = (barH * 0.34f * fontScale).coerceIn(12f, 24f)
                        ghostWidgetTextPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText(gapLabel, left + barW / 2f, top + barH * 0.30f, ghostWidgetTextPaint)
                    }
                    "tissot" -> {
                        // ── ESTILO TISSOT SPLIT (Parcial de crono oficial WT) ──
                        val splitBg = if (realAhead) Color.parseColor("#065F46") else Color.parseColor("#991B1B") // Verde oscuro / Rojo oscuro
                        val splitAccent = if (realAhead) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
                        
                        val splitRect = RectF(left + barW * 0.03f, top + barH * 0.12f, right - barW * 0.03f, bottom - barH * 0.12f)
                        ghostWidgetFillPaint.color = splitBg
                        canvas.drawRoundRect(splitRect, 8f, 8f, ghostWidgetFillPaint)

                        // Borde de competición
                        ghostWidgetStrokePaint.color = splitAccent
                        ghostWidgetStrokePaint.strokeWidth = 2.5f
                        canvas.drawRoundRect(splitRect, 8f, 8f, ghostWidgetStrokePaint)

                        // Mini bandera de estado a la izquierda
                        val bannerW = barW * 0.22f
                        val bannerRect = RectF(splitRect.left, splitRect.top, splitRect.left + bannerW, splitRect.bottom)
                        ghostWidgetFillPaint.color = splitAccent
                        canvas.drawRoundRect(bannerRect, 6f, 6f, ghostWidgetFillPaint)

                        ghostWidgetTextPaint.color = Color.WHITE
                        ghostWidgetTextPaint.textSize = (barH * 0.30f * fontScale).coerceIn(10f, 18f)
                        ghostWidgetTextPaint.textAlign = Paint.Align.CENTER
                        val statusText = if (realAhead) "LEADER" else "CHASE"
                        canvas.drawText(statusText, bannerRect.centerX(), cy + ghostWidgetTextPaint.textSize * 0.35f, ghostWidgetTextPaint)

                        // Tiempo digital grande monoespaciado en el centro
                        ghostWidgetTextPaint.color = Color.WHITE
                        ghostWidgetTextPaint.textSize = (barH * 0.48f * fontScale).coerceIn(16f, 32f)
                        val centerDisplay = "$timeStr  ($distStr)"
                        val textCenterX = (bannerRect.right + splitRect.right) * 0.5f
                        canvas.drawText(centerDisplay, textCenterX, cy + ghostWidgetTextPaint.textSize * 0.36f, ghostWidgetTextPaint)
                    }
                    else -> {
                        // ── ESTILO CLÁSICO VECTORIAL (Ciclistas vectoriales estilizados + vector de flecha) ──
                        val cyclistSize = (barH * 0.90f).coerceIn(34f, 68f)
                        val realColor = Color.parseColor("#B91C1C") // Rojo burdeos profesional
                        val ghostColor = Color.parseColor("#94A3B8") // Gris claro metalizado

                        val leftCyclistX = left + barW * 0.10f + cyclistSize * 0.5f
                        val rightCyclistX = right - barW * 0.10f - cyclistSize * 0.5f

                        val leftColor = if (realAhead) ghostColor else realColor
                        val rightColor = if (realAhead) realColor else ghostColor

                        drawCyclistVector(canvas, leftCyclistX, cy, cyclistSize, leftColor)
                        drawCyclistVector(canvas, rightCyclistX, cy, cyclistSize, rightColor)

                        // Flecha superior que conecta a ambos ciclistas
                        val arrowY = top + barH * 0.22f
                        val arrowStartX = leftCyclistX + cyclistSize * 0.8f
                        val arrowEndX = rightCyclistX - cyclistSize * 0.8f

                        if (arrowEndX > arrowStartX + 20f) {
                            ghostWidgetStrokePaint.color = Color.BLACK
                            ghostWidgetStrokePaint.strokeWidth = 2.0f
                            canvas.drawLine(arrowStartX, arrowY, arrowEndX, arrowY, ghostWidgetStrokePaint)

                            val arrowHeadSize = (barH * 0.16f).coerceIn(7f, 13f)
                            val arrowHead = Path().apply {
                                moveTo(arrowEndX, arrowY)
                                lineTo(arrowEndX - arrowHeadSize, arrowY - arrowHeadSize * 0.48f)
                                lineTo(arrowEndX - arrowHeadSize, arrowY + arrowHeadSize * 0.48f)
                                close()
                            }
                            ghostWidgetFillPaint.color = Color.BLACK
                            canvas.drawPath(arrowHead, ghostWidgetFillPaint)
                        }

                        // Texto métrico centrado (+0:12s / +135m)
                        ghostWidgetTextPaint.textSize = (barH * 0.40f * fontScale).coerceIn(15f, 32f)
                        val textY = top + barH * 0.78f
                        canvas.drawText(gapLabel, left + barW / 2f, textY, ghostWidgetTextPaint)
                    }
                }

                // Si alterna con aviso, refrescar pantalla para el cambio de ciclo
                if (hasAlert) {
                    postInvalidateDelayed(500)
                }
            } else if (alertText.isNotEmpty()) {
                // En horizontal aprovechamos todo el alto (40%)
                alertPaint.textSize = Math.min(38f * fontScale, (bottom - top) * 0.6f).coerceAtLeast(16f)
                val cy = top + (bottom - top) / 2f + (alertPaint.textSize / 3f)
                val textW = alertPaint.measureText(alertText)
                val viewW = right - left

                if (textW > viewW * 0.95f) {
                    // Modo pancarta publicitaria (Marquee)
                    val gap = 80f
                    val totalW = textW + gap
                    val speed = 0.08f // Velocidad de desplazamiento
                    val timeMod = System.currentTimeMillis() % ((totalW / speed).toLong())
                    val xOffset = -(timeMod * speed)

                    alertPaint.textAlign = Paint.Align.LEFT
                    canvas.save()
                    canvas.clipRect(left, top, right, bottom)
                    var drawX = left + xOffset
                    while (drawX < right) {
                        canvas.drawText(alertText, drawX, cy, alertPaint)
                        drawX += totalW
                    }
                    canvas.restore()
                    postInvalidateDelayed(40) // ~25 FPS
                } else {
                    alertPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(alertText, left + viewW / 2f, cy, alertPaint)
                    if (hasGhost) {
                        postInvalidateDelayed(500)
                    }
                }
            }
        } else {
            // Franja derecha (vertical mode)
            canvas.drawRect(android.graphics.RectF(left, top, right, bottom), bgPaint)

            val currentGap = ghostGap
            val hasGhost = currentGap != null && (currentGap.timeS != null || currentGap.distM != null)
            val hasAlert = alertText.isNotEmpty()

            // Alternancia: si ambos están disponibles, cicla cada 4 segundos
            val showGhostNow = when {
                hasGhost && hasAlert -> ((System.currentTimeMillis() / 4000L) % 2L) == 0L
                hasGhost -> true
                else -> false
            }

            if (showGhostNow && currentGap != null) {
                val barW = right - left
                val barH = bottom - top
                val cx = left + barW / 2f

                val diffDist = currentGap.distM ?: 0.0
                val diffTime = currentGap.timeS ?: 0.0
                val realAhead = diffDist >= 0.0 || (diffDist == 0.0 && diffTime >= 0.0)

                val distStr = distGapText(diffDist)
                val timeStr = timeGapText(diffTime)

                when (ghostStyle) {
                    "tissot" -> {
                        val splitBg = if (realAhead) Color.parseColor("#065F46") else Color.parseColor("#991B1B")
                        val splitAccent = if (realAhead) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
                        val splitRect = RectF(left + barW * 0.05f, top + barH * 0.08f, right - barW * 0.05f, bottom - barH * 0.08f)
                        ghostWidgetFillPaint.color = splitBg
                        canvas.drawRoundRect(splitRect, 6f, 6f, ghostWidgetFillPaint)

                        ghostWidgetStrokePaint.color = splitAccent
                        ghostWidgetStrokePaint.strokeWidth = 2.0f
                        canvas.drawRoundRect(splitRect, 6f, 6f, ghostWidgetStrokePaint)

                        // Cabecera status
                        val statusH = barH * 0.22f
                        val statusRect = RectF(splitRect.left, splitRect.top, splitRect.right, splitRect.top + statusH)
                        ghostWidgetFillPaint.color = splitAccent
                        canvas.drawRoundRect(statusRect, 5f, 5f, ghostWidgetFillPaint)

                        ghostWidgetTextPaint.color = Color.WHITE
                        ghostWidgetTextPaint.textSize = (barW * 0.26f * fontScale).coerceIn(10f, 18f)
                        ghostWidgetTextPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText(if (realAhead) "LEAD" else "CHASE", cx, statusRect.centerY() + ghostWidgetTextPaint.textSize * 0.35f, ghostWidgetTextPaint)

                        // Textos en vertical
                        val midY = top + barH / 2f + statusH * 0.3f
                        ghostWidgetTextPaint.textSize = (barW * 0.28f * fontScale).coerceIn(12f, 22f)
                        canvas.drawText(timeStr, cx, midY - 2f, ghostWidgetTextPaint)
                        ghostWidgetTextPaint.textSize = (barW * 0.20f * fontScale).coerceIn(9f, 15f)
                        canvas.drawText(distStr, cx, midY + ghostWidgetTextPaint.textSize + 4f, ghostWidgetTextPaint)
                    }
                    else -> {
                        // Modo clásico / velon / pursuit en vertical con ciclistas nítidos
                        val cyclistSize = (barW * 0.88f).coerceIn(32f, 64f)
                        val realColor = if (realAhead) Color.parseColor("#10B981") else Color.parseColor("#B91C1C")
                        val ghostColor = Color.parseColor("#94A3B8")

                        // En vertical: el que va por delante aparece arriba
                        val topCyclistY = top + barH * 0.11f + cyclistSize * 0.5f
                        val bottomCyclistY = bottom - barH * 0.11f - cyclistSize * 0.5f

                        val topColor = if (realAhead) realColor else ghostColor
                        val bottomColor = if (realAhead) ghostColor else realColor

                        drawCyclistVector(canvas, cx, bottomCyclistY, cyclistSize, bottomColor)
                        drawCyclistVector(canvas, cx, topCyclistY, cyclistSize, topColor)

                        // Flecha vertical ascendente
                        val arrowStartY = bottomCyclistY - cyclistSize * 0.85f
                        val arrowEndY = topCyclistY + cyclistSize * 0.85f

                        if (arrowStartY > arrowEndY + 20f) {
                            ghostWidgetStrokePaint.color = Color.BLACK
                            ghostWidgetStrokePaint.strokeWidth = 2.0f
                            canvas.drawLine(cx, arrowStartY, cx, arrowEndY, ghostWidgetStrokePaint)

                            val arrowHeadSize = (barW * 0.16f).coerceIn(6f, 10f)
                            val arrowHead = Path().apply {
                                moveTo(cx, arrowEndY)
                                lineTo(cx - arrowHeadSize * 0.48f, arrowEndY + arrowHeadSize)
                                lineTo(cx + arrowHeadSize * 0.48f, arrowEndY + arrowHeadSize)
                                close()
                            }
                            ghostWidgetFillPaint.color = Color.BLACK
                            canvas.drawPath(arrowHead, ghostWidgetFillPaint)
                        }

                        // Textos métricos en el centro
                        val midY = top + barH / 2f
                        ghostWidgetTextPaint.color = Color.BLACK
                        ghostWidgetTextPaint.textSize = (barW * 0.22f * fontScale).coerceIn(12f, 20f)
                        canvas.drawText(timeStr, cx, midY - 4f, ghostWidgetTextPaint)
                        canvas.drawText(distStr, cx, midY + ghostWidgetTextPaint.textSize + 2f, ghostWidgetTextPaint)
                    }
                }

                if (hasAlert) {
                    postInvalidateDelayed(500)
                }
            } else if (alertText.isNotEmpty()) {
                // En vertical el texto necesita caber o rotar
                alertPaint.textSize = Math.min(35f * fontScale, (bottom - top) * 0.8f).coerceAtLeast(16f)
                val cy = top + (bottom - top) / 2f + (alertPaint.textSize / 3f)
                val textW = alertPaint.measureText(alertText)
                val viewW = right - left

                if (textW > viewW * 0.95f) {
                    val gap = 60f
                    val totalW = textW + gap
                    val speed = 0.08f
                    val timeMod = System.currentTimeMillis() % ((totalW / speed).toLong())
                    val xOffset = -(timeMod * speed)

                    alertPaint.textAlign = Paint.Align.LEFT
                    canvas.save()
                    canvas.clipRect(left, top, right, bottom)
                    var drawX = left + xOffset
                    while (drawX < right) {
                        canvas.drawText(alertText, drawX, cy, alertPaint)
                        drawX += totalW
                    }
                    canvas.restore()
                    postInvalidateDelayed(40)
                } else {
                    alertPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(alertText, left + viewW / 2f, cy, alertPaint)
                    if (hasGhost) {
                        postInvalidateDelayed(500)
                    }
                }
            }
        }
    }

    /**
     * Draws a compact grade histogram on the right side of the ruler strip.
     * Buckets: 0-2%, 2-4%, 4-6%, 6-8%, 8-10%, >10%
     */
    private fun drawGradeHistogram(canvas: Canvas, strat: StrategyData, top: Float, bottom: Float, w: Float) {
        val buckets = intArrayOf(0, 0, 0, 0, 0, 0)
        val bucketColors = arrayOf("#43A047", "#7CB342", "#FDD835", "#FB8C00", "#E53935", "#B71C1C")
        val labels = arrayOf("<2", "2-4", "4-6", "6-8", "8-10", ">10")

        for (g in strat.subBlocks) {
            val gi = g.toInt()
            when {
                gi < 2  -> buckets[0]++
                gi < 4  -> buckets[1]++
                gi < 6  -> buckets[2]++
                gi < 8  -> buckets[3]++
                gi < 10 -> buckets[4]++
                else    -> buckets[5]++
            }
        }

        val total = strat.subBlocks.size.coerceAtLeast(1)
        val panelW = w * 0.30f // right 30% of width
        val panelLeft = w - panelW
        val panelHeight = bottom - top
        val bucketW = panelW / buckets.size

        // Background
        canvas.drawRect(panelLeft, top, w, bottom, energyBgPaint)

        for (i in buckets.indices) {
            val fraction = buckets[i].toFloat() / total
            val barH = fraction * panelHeight * 0.85f
            val barLeft = panelLeft + i * bucketW + 2f
            val barRight = barLeft + bucketW - 4f
            val barBottom = bottom - 16f
            val barTop = barBottom - barH

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(bucketColors[i])
                style = Paint.Style.FILL
            }
            canvas.drawRect(barLeft, barTop.coerceAtMost(barBottom), barRight, barBottom, barPaint)

            // Label under bar
            histPaint.textSize = 16f
            canvas.drawText(labels[i], barLeft + bucketW / 2f - 2f, bottom - 2f, histPaint)
        }

        // Title
        histPaint.textSize = 17f
        histPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.label_dist_format, "%"), panelLeft + panelW / 2f, top + 16f, histPaint)
    }
}
