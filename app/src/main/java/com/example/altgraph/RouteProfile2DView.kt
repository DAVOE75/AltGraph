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
    
    private val pathLine = Path()
    private val pathFill = Path()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Bright green
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val futureLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444") // Dark grey
        strokeWidth = 3f
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
    private val dashLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1f
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

        // Apply fonts dynamically
        FontHelper.applyFontToPaint(textPaint, fontFamilyKey, Typeface.BOLD)
        textPaint.textSize = 24f * fontSizeScale
        
        FontHelper.applyFontToPaint(subTextPaint, fontFamilyKey, Typeface.NORMAL)
        subTextPaint.textSize = 18f * fontSizeScale
        
        FontHelper.applyFontToPaint(climbListPaint, fontFamilyKey, Typeface.NORMAL)
        climbListPaint.textSize = 20f * fontSizeScale
        
        FontHelper.applyFontToPaint(climbListCatPaint, fontFamilyKey, Typeface.BOLD)
        climbListCatPaint.textSize = 20f * fontSizeScale
        
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
        kmDoneTextPaint.textSize = 22f * fontSizeScale
        
        FontHelper.applyFontToPaint(kmRemainTextPaint, fontFamilyKey, Typeface.BOLD)
        kmRemainTextPaint.textSize = 22f * fontSizeScale
        
        FontHelper.applyFontToPaint(peakTextPaint, fontFamilyKey, Typeface.BOLD)
        peakTextPaint.textSize = 16f * fontSizeScale
        
        FontHelper.applyFontToPaint(gridTextPaint, fontFamilyKey, Typeface.NORMAL)
        gridTextPaint.textSize = 14f * fontSizeScale

        val minElev = profileElevations.minOrNull() ?: 0f
        val maxElev = profileElevations.maxOrNull() ?: 0f
        // Add padding to elevRange to avoid exaggerating the Y axis
        val elevRange = (maxElev - minElev).coerceAtLeast(10f) * 2.5f

        val ptsCount = profileElevations.size
        
        pathLine.reset()
        pathFill.reset()
        
        val startX = 0f
        val endX = drawW
        val bottomY = drawH - 20f
        val topY = 100f // leave more space for text

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
                    val textY = (py - 26f).coerceAtLeast(35f)
                    canvas.drawText(label, safePx, textY, peakTextPaint)
                }
            }
        }
        
        val lineTopY = 30f // almost touching the top border
        // (Drawn later after km background)
        


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

        // Draw Top Texts
        val titleText = routeName.ifEmpty { "RUTA" }.uppercase()
        canvas.drawText(titleText, 20f, 30f, textPaint)
        
        val distText = String.format("Total %.1f km - %s", totalDistance / 1000.0, altitudeGainText)
        canvas.drawText(distText, 20f, 55f, subTextPaint)

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
            val listStartX = drawW * 0.50f // Moved slightly to the left (50%) to give more room for larger text

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
                val ptoStr = if (!climb.name.isNullOrBlank()) "${climb.name} " else "$ptoPrefix 9 "
                val catStr = "($cat)"
                val startKm = climb.startDistance / 1000.0
                val lenKm = climb.length / 1000.0
                val avg = climb.avgGrade
                val startStr = String.format(" km %.1f | %.1f km al %.1f%%", startKm, lenKm, avg)
                climbListPaint.measureText(ptoStr) + climbListCatPaint.measureText(catStr) + climbListPaint.measureText(startStr)
            } ?: (drawW - listStartX - 10f)
            val listWidth = maxItemWidth.coerceAtMost(drawW - 10f - listStartX)
            val headerCenterX = listStartX + listWidth / 2f

            val climbHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 17f * fontSizeScale
                textAlign = Paint.Align.CENTER
            }
            FontHelper.applyFontToPaint(climbHeaderPaint, fontFamilyKey, Typeface.BOLD)
            canvas.drawText(listTitle, headerCenterX, 62f, climbHeaderPaint)

            var climbY = 90f // Below the header

            for ((i, item) in catClimbs.withIndex()) {
                val climb = item.first
                val cat = item.second
                val startKm = climb.startDistance / 1000.0
                val lenKm = climb.length / 1000.0
                val avg = climb.avgGrade
                
                // Dim primary color if passed, normal if not
                val isPassed = climb.endDistance <= currentDistance
                if (isPassed) {
                    climbListPaint.color = dimmedColor // Atenuado
                    climbListCatPaint.color = dimmedColor
                } else {
                    climbListPaint.color = Color.parseColor("#DDDDDD")
                    climbListCatPaint.color = Color.parseColor("#F59E0B")
                }

                // Line 1: Pto 1 [Nombre] (3ª) km 3.5 | 0.5 km al 6.5%
                val showName = showClimbNames && !climb.name.isNullOrBlank()
                val ptoStr = if (showName) "${climb.name} " else "$ptoPrefix ${i + 1} "
                canvas.drawText(ptoStr, listStartX, climbY, climbListPaint)
                val ptoWidth = climbListPaint.measureText(ptoStr)

                val catStr = "($cat)"
                canvas.drawText(catStr, listStartX + ptoWidth, climbY, climbListCatPaint)
                val catWidth = climbListCatPaint.measureText(catStr)
                
                val startStr = String.format(" km %.1f | %.1f km al %.1f%%", startKm, lenKm, avg)
                canvas.drawText(startStr, listStartX + ptoWidth + catWidth, climbY, climbListPaint)

                climbY += 25f * fontSizeScale
                if (climbY > drawH - 20f) break // Avoid drawing out of bounds
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
            val mapBoxTop = 75f
            val mapBoxRight = drawW * 0.45f
            val mapBoxBottom = topY + 180f
            
            val mapW = mapBoxRight - mapBoxLeft
            val mapH = mapBoxBottom - mapBoxTop
            
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

        // --- DRAW KM TEXTS AND CYCLIST LINE LAST SO THEY ARE ON TOP OF EVERYTHING ---
        val kmDoneText = String.format("%.1f km", currentDistance / 1000.0)
        val doneWidth = kmDoneTextPaint.measureText(kmDoneText)
        
        val remainDist = (totalDistance - currentDistance).coerceAtLeast(0.0)
        val kmRemainText = String.format("-%.1f km", remainDist / 1000.0)
        val remainWidth = kmRemainTextPaint.measureText(kmRemainText)
        
        canvas.save()
        val textCenterY = 120f // CENTRADOS EN EL MISMO EJE Y
        canvas.translate(currentX, textCenterY)
        canvas.rotate(-90f) // from bottom to top for BOTH
        
        val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        
        // Cuadrado/rectángulo negro completo y unificado para bloquear completamente el fondo
        val maxHalfWidth = maxOf(doneWidth, remainWidth) / 2f + 16f
        canvas.drawRect(-maxHalfWidth, -48f, maxHalfWidth, 54f, bgPaint)
        
        // Done KM: Left side (Negative Y)
        canvas.drawText(kmDoneText, 0f, -10f, kmDoneTextPaint)
        
        // Remaining KM: Right side (Positive Y)
        canvas.drawText(kmRemainText, 0f, 40f, kmRemainTextPaint)
        
        canvas.restore()

        // Línea blanca indicadora del ciclista dibujada POR ENCIMA de todo (incluido el fondo de km)
        canvas.drawLine(currentX, bottomY, currentX, lineTopY, dashLinePaint)

        canvas.restore()
    }
}
