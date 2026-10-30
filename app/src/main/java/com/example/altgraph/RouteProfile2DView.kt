package com.example.altgraph

import android.content.Context
import android.graphics.*
import android.view.View

class RouteProfile2DView(context: Context) : View(context) {

    var profileElevations: FloatArray = FloatArray(0)
    var totalDistance: Double = 0.0
    var currentDistance: Double = 0.0
    var isRotated: Boolean = false
    var routeName: String = ""
    var routeCoords: List<Pair<Double, Double>> = emptyList()
    var altitudeGain: Double = 0.0
    var altitudeGainText: String = ""
    var fontFamilyKey: String = "sans-serif-condensed"
    var fontSizeScale: Float = 1.0f
    var activeClimbs: List<RouteClimb> = emptyList()
    var tvColor: String = "green"
    var highlightClimbs: Boolean = false
    var showClimbNames: Boolean = true
    
    private fun climbMaxGrade(climb: RouteClimb): Double {
        val n = profileElevations.size
        if (n < 2 || totalDistance <= 0) return climb.avgGrade
        val step = totalDistance / (n - 1)
        val window = (100.0 / step).toInt().coerceAtLeast(1)
        val s = (climb.startDistance / step).toInt().coerceIn(0, n - 1)
        val e = (climb.endDistance / step).toInt().coerceIn(0, n - 1)
        var max = climb.avgGrade
        var i = s
        while (i + window <= e) {
            val g = (profileElevations[i + window] - profileElevations[i]) / (window * step) * 100.0
            if (g > max) max = g
            i++
        }
        return max
    }

    private fun climbDetailText(climb: RouteClimb): String =
        String.format("%.1f km | %.1f%% | max %.1f%%", climb.length / 1000.0, climb.avgGrade, climbMaxGrade(climb))

    private val pathLine = Path()
    private val pathFill = Path()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Bright green
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val futureLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0") // High visibility white / very light grey under bright sun
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
    }
    private val climbListPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val climbListCatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val kmDoneTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Bright green
        textAlign = Paint.Align.CENTER
    }
    private val kmRemainTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val elevDoneTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        textAlign = Paint.Align.CENTER
    }
    private val elevRemainTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val dashLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }
    private val peakTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B") // Amber color for category
        textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22FFFFFF") // Very faint white
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val gridTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FFFFFF") // Faint white
        textAlign = Paint.Align.LEFT
    }

    private fun drawFlag(canvas: Canvas, cx: Float, cy: Float, colorStr: String, scale: Float = 1.0f) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor(colorStr) }
        val stick = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f * scale; color = Color.WHITE }
        canvas.drawLine(cx, cy, cx, cy - 30f * scale, stick)
        val path = Path()
        path.moveTo(cx, cy - 30f * scale)
        path.lineTo(cx + 15f * scale, cy - 22f * scale)
        path.lineTo(cx, cy - 15f * scale)
        path.close()
        canvas.drawPath(path, p)
        canvas.drawCircle(cx, cy, 4f * scale, p)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (profileElevations.isEmpty() || totalDistance <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        canvas.save()
        var drawW = w
        var drawH = h
        if (isRotated) {
            canvas.translate(w, 0f)
            canvas.rotate(90f)
            drawW = h
            drawH = w
        }

        // Layout adaptation based on whether the view is rendered in vertical/tall layout
        val isVerticalLayout = !isRotated && (drawH > drawW * 1.15f)

        // Apply fonts dynamically
        FontHelper.applyFontToPaint(textPaint, fontFamilyKey, Typeface.BOLD)
        textPaint.textSize = (if (isVerticalLayout) 21f else 24f) * fontSizeScale
        
        FontHelper.applyFontToPaint(subTextPaint, fontFamilyKey, Typeface.NORMAL)
        subTextPaint.textSize = (if (isVerticalLayout) 15f else 18f) * fontSizeScale
        
        FontHelper.applyFontToPaint(climbListPaint, fontFamilyKey, Typeface.NORMAL)
        climbListPaint.textSize = (if (isVerticalLayout) 16f else 20f) * fontSizeScale
        
        FontHelper.applyFontToPaint(climbListCatPaint, fontFamilyKey, Typeface.BOLD)
        climbListCatPaint.textSize = (if (isVerticalLayout) 16f else 20f) * fontSizeScale
        
        // Theme colors for TV profile (Green, White, Blue, Yellow, Red, Purple)
        val (primaryColor, gradientBottomColor, dimmedColor) = when (tvColor) {
            "white" -> Triple(Color.parseColor("#FFFFFF"), Color.parseColor("#333333"), Color.parseColor("#77FFFFFF"))
            "blue" -> Triple(Color.parseColor("#00E5FF"), Color.parseColor("#002244"), Color.parseColor("#7700E5FF"))
            "yellow" -> Triple(Color.parseColor("#FFDE00"), Color.parseColor("#423800"), Color.parseColor("#77FFDE00")) // Tour de France (Maillot Jaune)
            "red" -> Triple(Color.parseColor("#E30613"), Color.parseColor("#400206"), Color.parseColor("#77E30613")) // Vuelta a España (La Roja)
            "purple" -> Triple(Color.parseColor("#FF3385"), Color.parseColor("#450022"), Color.parseColor("#77FF3385")) // Giro d'Italia (Maglia Rosa)
            else -> Triple(Color.parseColor("#00E676"), Color.parseColor("#003311"), Color.parseColor("#7700E676"))
        }
        val primaryHex = String.format("#%06X", (0xFFFFFF and primaryColor))

        linePaint.color = primaryColor
        kmDoneTextPaint.color = primaryColor
        FontHelper.applyFontToPaint(kmDoneTextPaint, fontFamilyKey, Typeface.BOLD)
        kmDoneTextPaint.textSize = (if (isVerticalLayout) 18f else 21f) * fontSizeScale
        
        FontHelper.applyFontToPaint(kmRemainTextPaint, fontFamilyKey, Typeface.BOLD)
        kmRemainTextPaint.textSize = (if (isVerticalLayout) 18f else 21f) * fontSizeScale

        elevDoneTextPaint.color = primaryColor
        FontHelper.applyFontToPaint(elevDoneTextPaint, fontFamilyKey, Typeface.BOLD)
        elevDoneTextPaint.textSize = (if (isVerticalLayout) 13f else 15f) * fontSizeScale
        
        FontHelper.applyFontToPaint(elevRemainTextPaint, fontFamilyKey, Typeface.BOLD)
        elevRemainTextPaint.textSize = (if (isVerticalLayout) 13f else 15f) * fontSizeScale
        
        FontHelper.applyFontToPaint(peakTextPaint, fontFamilyKey, Typeface.BOLD)
        peakTextPaint.textSize = (if (isVerticalLayout) 14f else 16f) * fontSizeScale
        
        FontHelper.applyFontToPaint(gridTextPaint, fontFamilyKey, Typeface.NORMAL)
        gridTextPaint.textSize = (if (isVerticalLayout) 12f else 14f) * fontSizeScale

        val minElev = profileElevations.minOrNull() ?: 0f
        val maxElev = profileElevations.maxOrNull() ?: 0f
        // Add padding to elevRange to avoid exaggerating the Y axis
        val elevRange = (maxElev - minElev).coerceAtLeast(10f) * (if (isVerticalLayout) 1.6f else 2.5f)

        val ptsCount = profileElevations.size
        
        pathLine.reset()
        pathFill.reset()
        
        val startX = 0f
        val endX = drawW
        val bottomY = drawH - 15f
        val topY = if (isVerticalLayout) (drawH * 0.44f) else 100f // In vertical, lower the profile so top half is dedicated to minimap & climb list

        // Draw Y-Axis Grid
        val gridStep = when {
            elevRange > 2000 -> 500f
            elevRange > 1000 -> 250f
            elevRange > 500 -> 100f
            else -> 50f
        }
        val startGridElev = (minElev / gridStep).toInt() * gridStep
        var currGridElev = startGridElev
        while (currGridElev <= maxElev + gridStep) {
            if (currGridElev >= minElev) {
                val gy = bottomY - ((currGridElev - minElev) / elevRange) * (bottomY - topY)
                if (gy in topY..bottomY) {
                    canvas.drawLine(0f, gy, drawW, gy, gridPaint)
                    canvas.drawText("${currGridElev.toInt()}m", 5f, gy - 5f, gridTextPaint)
                }
            }
            currGridElev += gridStep
        }

        // Build the full path
        for (i in 0 until ptsCount) {
            val elev = profileElevations[i]
            val x = startX + (i.toFloat() / (ptsCount - 1)) * drawW
            val y = bottomY - ((elev - minElev) / elevRange) * (bottomY - topY)
            
            if (i == 0) {
                pathLine.moveTo(x, y)
                pathFill.moveTo(x, bottomY)
                pathFill.lineTo(x, y)
            } else {
                pathLine.lineTo(x, y)
                pathFill.lineTo(x, y)
            }
            if (i == ptsCount - 1) {
                pathFill.lineTo(x, bottomY)
                pathFill.close()
            }
        }

        val distinctClimbs = activeClimbs.deduplicateOverlappingClimbs()

        // 1. Draw future outline & optional background fill for mountain silhouette
        if (highlightClimbs) {
            val futureFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#30555555") // Faint dark grey base silhouette
                style = Paint.Style.FILL
            }
            canvas.drawPath(pathFill, futureFillPaint)
        }
        canvas.drawPath(pathLine, futureLinePaint)

        // 2. Highlight climbs on route profile if enabled
        if (highlightClimbs && distinctClimbs.isNotEmpty()) {
            val climbFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(0f, topY, 0f, bottomY, Color.parseColor("#666666"), Color.parseColor("#262626"), Shader.TileMode.CLAMP)
            }
            val climbLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CCCCCC") // Light grey leaning towards white
                strokeWidth = 3.5f
                style = Paint.Style.STROKE
            }
            for (climb in distinctClimbs) {
                val startPct = (climb.startDistance / totalDistance).toFloat().coerceIn(0f, 1f)
                val endPct = (climb.endDistance / totalDistance).toFloat().coerceIn(0f, 1f)
                val cStartX = startX + startPct * drawW
                val cEndX = startX + endPct * drawW
                if (cEndX > cStartX) {
                    canvas.save()
                    canvas.clipRect(cStartX, 0f, cEndX, drawH)
                    canvas.drawPath(pathFill, climbFillPaint)
                    canvas.drawPath(pathLine, climbLinePaint)
                    canvas.restore()
                }
            }
        }

        // 3. Calculate progress and draw ridden area in chosen neon color
        val progress = (currentDistance / totalDistance).toFloat().coerceIn(0f, 1f)
        val currentX = startX + progress * drawW

        // Draw filled area with gradient, clipped to current progress
        canvas.save()
        canvas.clipRect(startX, 0f, currentX, drawH)
        
        val gradient = LinearGradient(
            0f, topY, 0f, bottomY,
            primaryColor,
            gradientBottomColor,
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradient
        canvas.drawPath(pathFill, fillPaint)
        
        // Draw the line over the filled area so it's bright
        canvas.drawPath(pathLine, linePaint)
        canvas.restore()

        // Draw peaks for climbs
        for (climb in distinctClimbs) {
            val catUp = climb.category.uppercase()
            val mappedCat = when {
                catUp.contains("1") -> "1ª"
                catUp.contains("2") -> "2ª"
                catUp.contains("3") -> "3ª"
                catUp.contains("4") -> "4ª"
                catUp.contains("ESPECIAL") || catUp.contains("H.C") || catUp.contains("HC") -> "H.C."
                climb.category.isNotBlank() -> climb.category
                climb.isCustom -> "Puerto"
                else -> null
            }
            if (mappedCat != null || !climb.name.isNullOrBlank()) {
                val endPct = (climb.endDistance / totalDistance).toFloat().coerceIn(0f, 1f)
                val peakIdx = (endPct * (ptsCount - 1)).toInt().coerceIn(0, ptsCount - 1)

                // Buscar la cresta real más alta en un entorno de +/- 500m para clavar el banderín en la cima exacta
                val searchWindow = (500.0 / totalDistance * (ptsCount - 1)).toInt().coerceAtLeast(3)
                val winStart = (peakIdx - searchWindow).coerceAtLeast(0)
                val winEnd = (peakIdx + searchWindow).coerceAtMost(ptsCount - 1)
                var bestPeakIdx = peakIdx
                var maxSeenElev = profileElevations[peakIdx]
                for (k in winStart..winEnd) {
                    if (profileElevations[k] > maxSeenElev) {
                        maxSeenElev = profileElevations[k]
                        bestPeakIdx = k
                    }
                }
                val trueEndPct = bestPeakIdx.toFloat() / (ptsCount - 1)
                val px = startX + trueEndPct * drawW
                val py = bottomY - ((profileElevations[bestPeakIdx] - minElev) / elevRange) * (bottomY - topY)

                val showName = showClimbNames && !climb.name.isNullOrBlank()
                val label = if (showName) {
                    if (mappedCat != null) "${climb.name} ($mappedCat)" else climb.name!!
                } else {
                    mappedCat ?: climb.name ?: ""
                }
                if (label.isNotBlank()) {
                    // Dibujar banderín en la cima del puerto
                    drawFlag(canvas, px, py, "#F59E0B", 0.7f)
                    val textW = peakTextPaint.measureText(label)
                    val safePx = px.coerceIn(textW / 2f + 10f, drawW - textW / 2f - 10f)
                    val textY = (py - 24f).coerceAtLeast(topY - 10f)
                    canvas.drawText(label, safePx, textY, peakTextPaint)
                }
            }
        }

        // Draw icon or dot at current position
        val currentElevIndex = (progress * (ptsCount - 1)).toInt().coerceIn(0, ptsCount - 1)
        val currElev = profileElevations[currentElevIndex]
        val currY = bottomY - ((currElev - minElev) / elevRange) * (bottomY - topY)
        
        // Draw Dot
        canvas.drawCircle(currentX, currY, 8f, linePaint)
        val dotPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawCircle(currentX, currY, 5f, dotPaint)

        // Draw Start and End Flags
        val firstY = bottomY - ((profileElevations.firstOrNull() ?: minElev) - minElev) / elevRange * (bottomY - topY)
        val lastY = bottomY - ((profileElevations.lastOrNull() ?: minElev) - minElev) / elevRange * (bottomY - topY)
        drawFlag(canvas, startX + 10f, firstY, primaryHex) // Start Flag (Primary Neon)
        drawFlag(canvas, endX - 10f, lastY, "#FFFFFF") // End Flag (White/Checkered)

        // Top Header Base Y: In vertical layout, avoid cutting into top margin / system bar
        val headerBaseY = if (isVerticalLayout) 36f else 30f

        // Draw Classified Climbs List on the Right
        val catClimbs = distinctClimbs.mapNotNull { climb ->
            val catUp = climb.category.uppercase()
            val mappedCat = when {
                catUp.contains("1") -> "1ª"
                catUp.contains("2") -> "2ª"
                catUp.contains("3") -> "3ª"
                catUp.contains("4") -> "4ª"
                catUp.contains("ESPECIAL") || catUp.contains("H.C") || catUp.contains("HC") -> "H.C."
                climb.category.isNotBlank() -> climb.category
                climb.isCustom -> "Puerto"
                else -> null
            }
            if (mappedCat != null || !climb.name.isNullOrBlank()) {
                Pair(climb, mappedCat ?: "Puerto")
            } else null
        }.sortedBy { it.first.startDistance }

        if (catClimbs.isNotEmpty()) {
            val listStartX = if (isVerticalLayout) (drawW * 0.44f) else (drawW * 0.50f)

            // Title: LISTA DE PUERTOS centered above the climb list (localized)
            val listTitle = try {
                context.getString(R.string.profile_tv_climb_list_title).uppercase()
            } catch (e: Exception) {
                "LISTA DE PUERTOS"
            }
            val ptoPrefix = try {
                context.getString(R.string.profile_tv_climb_item_prefix)
            } catch (e: Exception) {
                "Pto"
            }

            val maxItemWidth = catClimbs.maxOfOrNull { item ->
                val climb = item.first
                val cat = item.second
                val named = showClimbNames && !climb.name.isNullOrBlank()
                val ptoStr = if (named) "${climb.name} " else "$ptoPrefix 99 "
                val line1W = climbListPaint.measureText(ptoStr) + climbListCatPaint.measureText("(H.C.)") +
                    climbListPaint.measureText(String.format(" · km %.1f", climb.startDistance / 1000.0))
                val line2W = climbListPaint.measureText(climbDetailText(climb)) * 0.85f
                maxOf(line1W, line2W)
            } ?: (drawW - listStartX - 10f)
            val listWidth = maxItemWidth.coerceAtMost(drawW - 10f - listStartX)
            val headerCenterX = listStartX + listWidth / 2f

            val climbHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (if (isVerticalLayout) 15f else 17f) * fontSizeScale
                textAlign = Paint.Align.CENTER
            }
            FontHelper.applyFontToPaint(climbHeaderPaint, fontFamilyKey, Typeface.BOLD)
            val headerTitleY = if (isVerticalLayout) (headerBaseY + 34f) else 62f
            canvas.drawText(listTitle, headerCenterX, headerTitleY, climbHeaderPaint)

            var climbY = headerTitleY + (if (isVerticalLayout) 24f else 28f)

            // Con más de 3 puertos la lista avanza sola: los superados salen por arriba
            val firstPending = catClimbs.indexOfFirst { it.first.endDistance > currentDistance }
            val firstShown = if (catClimbs.size > 3) {
                (if (firstPending < 0) catClimbs.size - 3 else firstPending).coerceAtMost(catClimbs.size - 3)
            } else 0

            val completedLabel = try {
                context.getString(R.string.profile_tv_climb_completed)
            } catch (e: Exception) {
                "OK"
            }

            for ((i, item) in catClimbs.withIndex()) {
                if (i < firstShown) continue
                val climb = item.first
                val cat = item.second
                val startKm = climb.startDistance / 1000.0
                
                val isPassed = climb.endDistance <= currentDistance
                if (isPassed) {
                    climbListPaint.color = Color.parseColor("#9E9E9E") // Clearly legible light-mid grey
                    climbListCatPaint.color = Color.parseColor("#81C784") // Soft green for completed
                } else {
                    climbListPaint.color = Color.WHITE // Crisp white for sunlight readability
                    climbListCatPaint.color = Color.parseColor("#FFB300") // Vivid amber for upcoming pass
                }

                // Línea 1: nombre (o Pto N), categoría y km de inicio (o estado Completado)
                val showName = showClimbNames && !climb.name.isNullOrBlank()
                val ptoStr = if (showName) "${climb.name} " else "$ptoPrefix ${i + 1} "
                canvas.drawText(ptoStr, listStartX, climbY, climbListPaint)
                val ptoWidth = climbListPaint.measureText(ptoStr)

                val catStr = "($cat)"
                canvas.drawText(catStr, listStartX + ptoWidth, climbY, climbListCatPaint)
                val catWidth = climbListCatPaint.measureText(catStr)

                val statusOrKm = if (isPassed) " · ✓ $completedLabel" else String.format(" · km %.1f", startKm)
                canvas.drawText(statusOrKm, listStartX + ptoWidth + catWidth, climbY, if (isPassed) climbListCatPaint else climbListPaint)

                // Línea 2: longitud, pendiente media y máxima, alineada a la izquierda con el nombre
                val baseSize = climbListPaint.textSize
                climbListPaint.textSize = baseSize * 0.85f
                climbY += (if (isVerticalLayout) 18f else 22f) * fontSizeScale
                canvas.drawText(climbDetailText(climb), listStartX, climbY, climbListPaint)
                climbListPaint.textSize = baseSize

                climbY += (if (isVerticalLayout) 22f else 25f) * fontSizeScale
                val maxListY = if (isVerticalLayout) (topY - 15f) else (drawH - 20f)
                if (climbY > maxListY) break // Avoid colliding with profile in vertical layout
            }
        }

        // Draw 2D Mini-map
        if (routeCoords.isNotEmpty()) {
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLng = Double.MAX_VALUE
            var maxLng = -Double.MAX_VALUE
            for (pt in routeCoords) {
                if (pt.first < minLat) minLat = pt.first
                if (pt.first > maxLat) maxLat = pt.first
                if (pt.second < minLng) minLng = pt.second
                if (pt.second > maxLng) maxLng = pt.second
            }
            
            val mapBoxLeft = 10f
            val mapBoxTop = if (isVerticalLayout) (headerBaseY + 45f) else 75f
            val mapBoxRight = if (isVerticalLayout) (drawW * 0.40f) else (drawW * 0.45f)
            val mapBoxBottom = if (isVerticalLayout) (topY - 15f) else (topY + 180f)
            
            val mapW = (mapBoxRight - mapBoxLeft).coerceAtLeast(10f)
            val mapH = (mapBoxBottom - mapBoxTop).coerceAtLeast(10f)
            
            // Draw faint concentric lines as "contour" background extending all the way to the right below the climb list
            val contourBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#15FFFFFF") // Much fainter white
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.save()
            canvas.clipRect(mapBoxLeft, mapBoxTop, drawW - 10f, mapBoxBottom)
            for (r in 10..400 step 20) {
                canvas.drawCircle(mapBoxLeft + mapW * 0.3f, mapBoxTop + mapH * 0.4f, r.toFloat(), contourBgPaint)
                canvas.drawCircle(mapBoxRight - mapW * 0.2f, mapBoxBottom - mapH * 0.2f, r.toFloat(), contourBgPaint)
                canvas.drawCircle(drawW * 0.7f, mapBoxTop + mapH * 0.5f, r.toFloat(), contourBgPaint)
            }
            canvas.restore()
            
            val midLat = (minLat + maxLat) / 2.0
            val latScale = mapH / Math.max(1e-6, maxLat - minLat)
            val lngSpanRaw = (maxLng - minLng) * Math.cos(Math.toRadians(midLat))
            val lngScale = mapW / Math.max(1e-6, lngSpanRaw)
            
            val scale = Math.min(latScale, lngScale)
            
            val contentW = (maxLng - minLng) * Math.cos(Math.toRadians(midLat)) * scale
            val contentH = (maxLat - minLat) * scale
            val offX = mapBoxLeft + (mapW - contentW) / 2f
            val offY = mapBoxTop + (mapH - contentH) / 2f
            
            val mapPathPast = Path()
            val mapPathFuture = Path()
            
            val currentIdx = (progress * (routeCoords.size - 1)).toInt().coerceIn(0, routeCoords.size - 1)
            var currPx = 0f
            var currPy = 0f
            
            for (i in routeCoords.indices) {
                val pt = routeCoords[i]
                val px = (offX + (pt.second - minLng) * Math.cos(Math.toRadians(midLat)) * scale).toFloat()
                val py = (offY + contentH - (pt.first - minLat) * scale).toFloat()
                
                if (i == 0) {
                    mapPathPast.moveTo(px, py)
                    if (currentIdx == 0) {
                        mapPathFuture.moveTo(px, py)
                        currPx = px
                        currPy = py
                    }
                } else if (i <= currentIdx) {
                    mapPathPast.lineTo(px, py)
                    if (i == currentIdx) {
                        mapPathFuture.moveTo(px, py)
                        currPx = px
                        currPy = py
                    }
                } else {
                    if (i == currentIdx + 1 && currentIdx == 0) {
                        // Just to make sure path starts correctly if index is 0
                    }
                    mapPathFuture.lineTo(px, py)
                }
            }
            
            val miniMapPastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            val miniMapFuturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#666666")
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            
            canvas.drawPath(mapPathFuture, miniMapFuturePaint)

            // If highlightClimbs is enabled, highlight climbs in light grey on the minimap route track
            if (highlightClimbs && distinctClimbs.isNotEmpty()) {
                val miniMapClimbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#CCCCCC") // Light grey leaning towards white
                    strokeWidth = 4.5f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                for (climb in distinctClimbs) {
                    val startPct = (climb.startDistance / totalDistance).toFloat().coerceIn(0f, 1f)
                    val endPct = (climb.endDistance / totalDistance).toFloat().coerceIn(0f, 1f)
                    val sIdx = (startPct * (routeCoords.size - 1)).toInt().coerceIn(0, routeCoords.size - 1)
                    val eIdx = (endPct * (routeCoords.size - 1)).toInt().coerceIn(0, routeCoords.size - 1)
                    if (eIdx > sIdx) {
                        val climbTrackPath = Path()
                        for (ci in sIdx..eIdx) {
                            val cpt = routeCoords[ci]
                            val cpx = (offX + (cpt.second - minLng) * Math.cos(Math.toRadians(midLat)) * scale).toFloat()
                            val cpy = (offY + contentH - (cpt.first - minLat) * scale).toFloat()
                            if (ci == sIdx) climbTrackPath.moveTo(cpx, cpy) else climbTrackPath.lineTo(cpx, cpy)
                        }
                        canvas.drawPath(climbTrackPath, miniMapClimbPaint)
                    }
                }
            }

            canvas.drawPath(mapPathPast, miniMapPastPaint)
            
            val dotP = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawCircle(currPx, currPy, 5f, miniMapPastPaint)
            canvas.drawCircle(currPx, currPy, 3f, dotP)
            
            // Draw climb markers on minimap
            val miniMapPeakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F59E0B") // Amber/Orange color for category
                style = Paint.Style.FILL
            }
            
            for (climb in distinctClimbs) {
                val catUp = climb.category.uppercase()
                val mappedCat = when {
                    catUp.contains("1") -> "1ª"
                    catUp.contains("2") -> "2ª"
                    catUp.contains("3") -> "3ª"
                    catUp.contains("4") -> "4ª"
                    catUp.contains("ESPECIAL") || catUp.contains("H.C") || catUp.contains("HC") -> "H.C."
                    else -> null
                }
                if (mappedCat != null) {
                    val endPct = (climb.endDistance / totalDistance).toFloat().coerceIn(0f, 1f)
                    val peakIdx = (endPct * (routeCoords.size - 1)).toInt().coerceIn(0, routeCoords.size - 1)
                    val pt = routeCoords[peakIdx]
                    val px = (offX + (pt.second - minLng) * Math.cos(Math.toRadians(midLat)) * scale).toFloat()
                    val py = (offY + contentH - (pt.first - minLat) * scale).toFloat()
                    
                    val path = Path()
                    path.moveTo(px, py - 6f) // Triangle pointing up
                    path.lineTo(px + 6f, py + 4f)
                    path.lineTo(px - 6f, py + 4f)
                    path.close()
                    canvas.drawPath(path, miniMapPeakPaint)
                }
            }

            // Draw Start and End Flags on minimap
            val startPt = routeCoords.first()
            val pxStart = (offX + (startPt.second - minLng) * Math.cos(Math.toRadians(midLat)) * scale).toFloat()
            val pyStart = (offY + contentH - (startPt.first - minLat) * scale).toFloat()
            drawFlag(canvas, pxStart, pyStart, primaryHex, 0.6f) // Start Flag (Primary Neon, smaller)
            
            val endPt = routeCoords.last()
            val pxEnd = (offX + (endPt.second - minLng) * Math.cos(Math.toRadians(midLat)) * scale).toFloat()
            val pyEnd = (offY + contentH - (endPt.first - minLat) * scale).toFloat()
            drawFlag(canvas, pxEnd, pyEnd, "#FFFFFF", 0.6f) // End Flag (White, smaller)
        }

        // Draw Top Texts (Title & Distance/Gain) AFTER minimap so it's always clean and crisp on top
        val titleText = routeName.ifEmpty { "RUTA" }.uppercase()
        val titleY = headerBaseY
        canvas.drawText(titleText, 14f, titleY, textPaint)
        
        val distText = String.format("Total %.1f km - %s", totalDistance / 1000.0, altitudeGainText)
        val distY = titleY + (if (isVerticalLayout) 20f else 25f)
        canvas.drawText(distText, 14f, distY, subTextPaint)

        // --- DRAW KM & ELEVATION TEXTS AND CYCLIST LINE LAST SO THEY ARE ON TOP OF EVERYTHING ---
        val kmDoneText = String.format("%.1f km", currentDistance / 1000.0)
        val remainDist = (totalDistance - currentDistance).coerceAtLeast(0.0)
        val kmRemainText = String.format("-%.1f km", remainDist / 1000.0)

        // Calculate elevation done vs elevation remaining
        var elevDone = 0.0
        var elevRemain = 0.0
        if (profileElevations.size >= 2) {
            val currIdx = (progress * (profileElevations.size - 1)).toInt().coerceIn(0, profileElevations.size - 1)
            for (i in 1..currIdx) {
                val d = profileElevations[i] - profileElevations[i - 1]
                if (d > 0) elevDone += d
            }
            for (i in currIdx + 1 until profileElevations.size) {
                val d = profileElevations[i] - profileElevations[i - 1]
                if (d > 0) elevRemain += d
            }
        }
        val elevDoneText = String.format("+%dm", elevDone.toInt())
        val elevRemainText = String.format("-%dm", elevRemain.toInt())

        val doneWidth = maxOf(kmDoneTextPaint.measureText(kmDoneText), elevDoneTextPaint.measureText(elevDoneText))
        val remainWidth = maxOf(kmRemainTextPaint.measureText(kmRemainText), elevRemainTextPaint.measureText(elevRemainText))

        // Check if remaining numbers run out of room on the right: push them to the left side
        val needPushLeft = (currentX + 50f > drawW)
        val needPushRight = (currentX - 50f < 0f)

        // Cyclist vertical line top: in vertical layout extend all the way to topY (the minimap/chart boundary)
        val lineTopY = if (isVerticalLayout) topY else (currY - 50f).coerceAtLeast(topY - 15f)
        // Text center: place labels just below the cyclist dot, constrained within the profile area
        val textCenterY = (currY - (if (isVerticalLayout) 35f else 45f)).coerceIn(topY + 40f, bottomY - 35f)

        canvas.save()
        canvas.translate(currentX, textCenterY)
        canvas.rotate(-90f) // from bottom to top

        val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        val maxHalfWidth = maxOf(doneWidth, remainWidth) / 2f + 12f

        if (needPushLeft) {
            // Both Done and Remaining are drawn on the left side of the vertical line
            canvas.drawRect(-maxHalfWidth, -75f, maxHalfWidth, 0f, bgPaint)
            canvas.drawText(kmDoneText, 0f, -44f, kmDoneTextPaint)
            canvas.drawText(elevDoneText, 0f, -28f, elevDoneTextPaint)
            canvas.drawText(kmRemainText, 0f, -12f, kmRemainTextPaint)
            canvas.drawText(elevRemainText, 0f, 2f, elevRemainTextPaint)
        } else if (needPushRight) {
            // Both drawn on the right side if at the very beginning
            canvas.drawRect(-maxHalfWidth, 0f, maxHalfWidth, 75f, bgPaint)
            canvas.drawText(kmDoneText, 0f, 16f, kmDoneTextPaint)
            canvas.drawText(elevDoneText, 0f, 32f, elevDoneTextPaint)
            canvas.drawText(kmRemainText, 0f, 48f, kmRemainTextPaint)
            canvas.drawText(elevRemainText, 0f, 64f, elevRemainTextPaint)
        } else {
            // Normal: Done on the left (negative Y rotated), Remaining on the right (positive Y rotated)
            canvas.drawRect(-maxHalfWidth, -44f, maxHalfWidth, 44f, bgPaint)
            
            // Left side (Done)
            canvas.drawText(kmDoneText, 0f, -26f, kmDoneTextPaint)
            canvas.drawText(elevDoneText, 0f, -10f, elevDoneTextPaint)
            
            // Right side (Remaining)
            canvas.drawText(kmRemainText, 0f, 22f, kmRemainTextPaint)
            canvas.drawText(elevRemainText, 0f, 38f, elevRemainTextPaint)
        }
        
        canvas.restore()

        // Línea blanca indicadora del ciclista acotada: no invade el minimapa superior
        canvas.drawLine(currentX, bottomY, currentX, lineTopY, dashLinePaint)

        canvas.restore()
    }
}
