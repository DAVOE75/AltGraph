package com.example.altgraph

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnNavigationState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private const val TAG = "ALTGRAPH_VIEWER"

class ClimbViewerDataType(extension: String) : DataTypeImpl(extension, "climb_3d") {

    private val scope = CoroutineScope(Dispatchers.Main)
    // Un solo receiver por campo aunque haya varias vistas abiertas: con uno por vista
    // cada toque cambiaba de puerto o de zoom varias veces
    private val interactionReceiverRef = RefCounted<Pair<Context, BroadcastReceiver>> { (ctx, r) ->
        try { ctx.unregisterReceiver(r) } catch (e: Exception) {}
    }

    private var currentClimbIndex = 0
    private var climbLookaheadMeters: Double = 0.0 // 0.0 = full climb
    private var climbPanOffsetMeters: Double = 0.0
    private val refreshChannel = Channel<Unit>(Channel.CONFLATED)

    companion object {
        const val ACTION_CLIMB_NEXT = "com.example.altgraph.ACTION_CLIMB_NEXT"
        const val ACTION_CLIMB_PREV = "com.example.altgraph.ACTION_CLIMB_PREV"
        const val ACTION_ZOOM_IN = "com.example.altgraph.ACTION_CLIMB_ZOOM_IN"
        const val ACTION_ZOOM_OUT = "com.example.altgraph.ACTION_CLIMB_ZOOM_OUT"
        const val ACTION_PAN_LEFT = "com.example.altgraph.ACTION_CLIMB_PAN_LEFT"
        const val ACTION_PAN_RIGHT = "com.example.altgraph.ACTION_CLIMB_PAN_RIGHT"
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        val latch = CancelLatch()
        emitter.setCancellable { latch.cancel() }
        val streamJob = scope.launch {
            while (true) {
                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId = dataTypeId,
                            values = mapOf(DataType.Field.SINGLE to 0.0)
                        )
                    )
                )
                delay(1000)
            }
        }
        latch.attach(streamJob) {}
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val latch = CancelLatch()
        emitter.setCancellable { latch.cancel() }
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        interactionReceiverRef.acquire(emitter) {
            val interactionReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val climbs = AltgraphRepository.snapshot.value?.routeClimbs ?: emptyList()
                    val currentClimb = if (climbs.isNotEmpty()) climbs[currentClimbIndex % climbs.size] else null

                    when (intent?.action) {
                        ACTION_CLIMB_NEXT -> {
                            if (climbs.isNotEmpty()) {
                                currentClimbIndex = (currentClimbIndex + 1) % climbs.size
                                climbLookaheadMeters = 0.0
                                climbPanOffsetMeters = 0.0
                            }
                        }
                        ACTION_CLIMB_PREV -> {
                            if (climbs.isNotEmpty()) {
                                currentClimbIndex = if (currentClimbIndex - 1 < 0) climbs.size - 1 else currentClimbIndex - 1
                                climbLookaheadMeters = 0.0
                                climbPanOffsetMeters = 0.0
                            }
                        }
                        ACTION_ZOOM_IN -> {
                            val climbLen = currentClimb?.length ?: 2000.0
                            val effectiveWindow = if (climbLookaheadMeters <= 0.0) climbLen else climbLookaheadMeters
                            val next = when {
                                effectiveWindow > 20000.0 -> 20000.0
                                effectiveWindow > 10000.0 -> 10000.0
                                effectiveWindow > 5000.0 -> 5000.0
                                effectiveWindow > 2000.0 -> 2000.0
                                effectiveWindow > 1000.0 -> 1000.0
                                effectiveWindow > 500.0 -> 500.0
                                effectiveWindow > 200.0 -> 200.0
                                else -> 200.0
                            }
                            climbLookaheadMeters = next.coerceAtMost(climbLen)
                        }
                        ACTION_ZOOM_OUT -> {
                            val climbLen = currentClimb?.length ?: 2000.0
                            val effectiveWindow = if (climbLookaheadMeters <= 0.0) climbLen else climbLookaheadMeters
                            val next = when {
                                effectiveWindow < 200.0 -> 500.0
                                effectiveWindow < 500.0 -> 1000.0
                                effectiveWindow < 1000.0 -> 2000.0
                                effectiveWindow < 2000.0 -> 5000.0
                                effectiveWindow < 5000.0 -> 10000.0
                                effectiveWindow < 10000.0 -> 20000.0
                                else -> climbLen
                            }
                            if (next >= climbLen) {
                                climbLookaheadMeters = 0.0 // volver a ver puerto completo
                                climbPanOffsetMeters = 0.0
                            } else {
                                climbLookaheadMeters = next
                            }
                        }
                        ACTION_PAN_LEFT -> {
                            val climbLen = currentClimb?.length ?: 2000.0
                            val win = if (climbLookaheadMeters > 0.0) climbLookaheadMeters else climbLen
                            val step = (win / 3.0).coerceAtLeast(100.0)
                            climbPanOffsetMeters = (climbPanOffsetMeters - step).coerceAtLeast(0.0)
                        }
                        ACTION_PAN_RIGHT -> {
                            val climbLen = currentClimb?.length ?: 2000.0
                            val win = if (climbLookaheadMeters > 0.0) climbLookaheadMeters else climbLen
                            val maxOffset = (climbLen - win).coerceAtLeast(0.0)
                            val step = (win / 3.0).coerceAtLeast(100.0)
                            climbPanOffsetMeters = (climbPanOffsetMeters + step).coerceAtMost(maxOffset)
                        }
                    }
                    refreshChannel.trySend(Unit)
                }
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_CLIMB_NEXT)
                addAction(ACTION_CLIMB_PREV)
                addAction(ACTION_ZOOM_IN)
                addAction(ACTION_ZOOM_OUT)
                addAction(ACTION_PAN_LEFT)
                addAction(ACTION_PAN_RIGHT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(interactionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.applicationContext.registerReceiver(interactionReceiver, filter)
            }
            context.applicationContext to interactionReceiver
        }

        AltgraphRepository.hold(emitter)
        val navListener: (OnNavigationState) -> Unit = { navEvent ->
            if (navEvent.state.javaClass.simpleName == "Idle") currentClimbIndex = 0
        }
        AltgraphRepository.addNavListener(navListener)

        val altimetria3DView = Altimetria3DView(context).apply {
            isClimbMode = true
        }
        val w = if (config.viewSize.first > 0) config.viewSize.first else 480
        val h = if (config.viewSize.second > 0) config.viewSize.second else 240

        altimetria3DView.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        altimetria3DView.layout(0, 0, w, h)

        var cachedBitmap: Bitmap? = null
        var cachedCanvas: Canvas? = null

        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, Color.BLACK)
        }
        val bgPaint = Paint().apply { color = Color.BLACK }

        val viewJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                val climbs = snap.routeClimbs
                if (currentClimbIndex >= climbs.size) currentClimbIndex = 0
                val climb = climbs.getOrNull(currentClimbIndex)
                val climbLen = climb?.length ?: 0.0
                val customLength = if (climb == null || climbLookaheadMeters <= 0.0 || climbLookaheadMeters >= climbLen) {
                    climbLen
                } else {
                    climbLookaheadMeters
                }
                val maxPan = (climbLen - customLength).coerceAtLeast(0.0)
                val effectivePan = climbPanOffsetMeters.coerceIn(0.0, maxPan)
                val customStartDist = if (climb == null) 0.0 else climb.startDistance + effectivePan
                safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter); AltgraphRepository.removeNavListener(navListener) }) {
                // Suspend (calcula en el hilo del calculador); dentro de safeUpdate para que un fallo no mate el bucle
                val strategy = if (climb != null) AltgraphRepository.climbStrategy(climb, customStartDist, customLength) else null
                if (cachedBitmap == null || cachedBitmap!!.width != w || cachedBitmap!!.height != h) {
                    cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    cachedCanvas = Canvas(cachedBitmap!!)
                }

                val currentBmp = cachedBitmap!!
                val currentCanvas = cachedCanvas!!
                currentCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

                val prefs = AppPreferences.getInstance(context)
                if (climb == null || strategy == null) {
                    val isRotated = prefs.climbRotate90Clockwise
                    var drawW = w.toFloat()
                    var drawH = h.toFloat()
                    if (isRotated) {
                        currentCanvas.save()
                        currentCanvas.translate(w.toFloat(), 0f)
                        currentCanvas.rotate(90f)
                        drawW = h.toFloat()
                        drawH = w.toFloat()
                    }
                    currentCanvas.drawText("Sin puertos detectados / No climbs", drawW / 2f, drawH / 2f, overlayPaint)
                    if (isRotated) {
                        currentCanvas.restore()
                    }
                } else {
                    
                    val lookahead = customLength.toInt().coerceAtLeast(100)
                    
                    // Calculamos progreso del ciclista
                    val riderProgress = if (snap.currentRouteDistance >= customStartDist && snap.currentRouteDistance <= customStartDist + customLength) {
                        ((snap.currentRouteDistance - customStartDist) / customLength).toFloat()
                    } else {
                        -1f // -1f ocultará la baliza en la gráfica
                    }
                    
                    altimetria3DView.oasisDistanceToNextCrucible = strategy.oasisDistanceToNextCrucible
                altimetria3DView.virtualPacerRelativeDistance = strategy.virtualPacerRelativeDistance
                altimetria3DView.energyBatteryLevel = strategy.energyBatteryLevel
                
                // ELITE
                altimetria3DView.stravaSegmentDistance = strategy.stravaSegmentDistance
                altimetria3DView.stravaPrGhostDistance = strategy.stravaPrGhostDistance
                altimetria3DView.windEffectIntensity = strategy.windEffectIntensity
                altimetria3DView.update3DData(
                        blocks = strategy.nextBlocks,
                        elevation = strategy.windowStartElevation,
                        maxElev = 0.0, // Let view calculate it
                        grade = strategy.avgGrade,
                        remainingDist = strategy.remainingDistance,
                        blockSizeMeters = strategy.blockSizeMeters,
                        fontScale = prefs.fontSize3dScale,
                        lookaheadMeters = lookahead,
                        showCotas = prefs.show3dCotas,
                        showRamps = prefs.show3dRamps,
                        showMaxGrade = prefs.showMaxGradient,
                        fontFamilyKey = prefs.fontFamilyKey,
                        rotate90 = prefs.climbRotate90Clockwise,
                        rotateMinus90 = false,
                        rampMinSlope = prefs.rampMinSlopePct,
                        rampMaxSlope = prefs.rampMaxSlopePct,
                        showHairpins = prefs.showHairpins,
                        showPois = false,
                        hairpins = strategy.hairpins,
                        pois = emptyList(),
                        showZoomControls = false, // We use screen tap to cycle climbs
                        curvatureOffsets = emptyList(),
                        riderProgress = riderProgress,
                        windowStartMeters = strategy.windowStartMeters,
                        subBlocks = strategy.subBlocks,
                        subBlockSizeMeters = strategy.subBlockSizeMeters,
                        majorBlockSizeMeters = strategy.majorBlockSizeMeters,
                        profileElevations = strategy.profileElevations,
                        showBlockPercentages = prefs.showBlockPercentages,
                        altimetriaStyle = prefs.climbAltimetriaStyle,
                        targetVam = prefs.targetVam,
                        activeClimbs = emptyList(),
                        visibleAvgGrade = strategy.visibleAvgGrade,
                        visibleMaxGrade = strategy.visibleMaxGrade,
                        routeName = "",
                        customTitle = "",
                        showHeaderStats = false,
                        routeCoords = strategy.routeCoords,
                        showSurfaceTextures = prefs.showSurfaceTextures,
                        panButtonsPosition = "bottom"
                    )
                    
                    altimetria3DView.draw(currentCanvas)
                    

                    // Draw Header Overlay
                    val isRotated = prefs.climbRotate90Clockwise
                    var drawW = w.toFloat()
                    var drawH = h.toFloat()

                    if (isRotated) {
                        currentCanvas.save()
                        currentCanvas.translate(w.toFloat(), 0f)
                        currentCanvas.rotate(90f)
                        drawW = h.toFloat()
                        drawH = w.toFloat()
                    }
                    overlayPaint.textSize = (drawH * 0.12f).coerceIn(26f, 42f)
                    FontHelper.applyFontToPaint(overlayPaint, prefs.fontFamilyKey)
                    
                    subPaint.textSize = (drawH * 0.06f).coerceIn(16f, 24f)
                    subPaint.textAlign = Paint.Align.LEFT
                    FontHelper.applyFontToPaint(subPaint, prefs.fontFamilyKey)
                    
                    val maxGradePaint = Paint(subPaint).apply {
                        color = Color.parseColor("#EF4444") // Red
                    }
                    FontHelper.applyFontToPaint(maxGradePaint, prefs.fontFamilyKey)
                    
                    val showName = !climb.name.isNullOrBlank()
                    val catPart = if (climb.category.isNotBlank()) " (${climb.category})" else ""
                    val climbTitle = if (showName) {
                        "◀   ${climb.name}$catPart   ▶"
                    } else {
                        "◀   Puerto ${currentClimbIndex + 1}/${climbs.size}$catPart   ▶"
                    }
                    currentCanvas.drawText(climbTitle, drawW / 2f, drawH * 0.10f, overlayPaint)
                    
                    // Line 1: Length | Elevation | Status
                    val distStr = if (snap.isNavigatingRoute) {
                        val distanceToStart = climb.startDistance - snap.currentRouteDistance
                        val km = (Math.abs(distanceToStart) / 1000).toInt()
                        val m = (Math.abs(distanceToStart) % 1000).toInt()
                        if (distanceToStart > 0) {
                            if (km > 0) " | Faltan ${km}km ${m}m" else " | Faltan ${m}m"
                        } else if (distanceToStart <= 0 && snap.currentRouteDistance < climb.endDistance) {
                            " | En puerto"
                        } else {
                            " | Superado"
                        }
                    } else ""
                    
                    val line1 = "${String.format("%.1f", climb.length / 1000f)}km | +${climb.totalElevation.toInt()}m$distStr"
                    
                    // Line 2: Pend. Actual | P.Med | P.Max
                    val liveGrade = snap.instantBarometricGrade
                    val maxG = if (strategy.visibleMaxGrade > 0) strategy.visibleMaxGrade else climb.avgGrade
                    val line2Part1 = "Pend. Actual: ${String.format("%.1f", liveGrade)}%  |  P.Med: ${String.format("%.1f", climb.avgGrade)}%  |  P.Max: "
                    val line2Part2 = "${String.format("%.1f", maxG)}%"
                    
                    val wLine2P1 = subPaint.measureText(line2Part1)
                    val wLine2P2 = maxGradePaint.measureText(line2Part2)
                    
                    val statsY1 = drawH * 0.18f
                    val statsY2 = statsY1 + 40f
                    
                    subPaint.textAlign = Paint.Align.CENTER
                    currentCanvas.drawText(line1, drawW / 2f, statsY1, subPaint)
                    
                    subPaint.textAlign = Paint.Align.LEFT
                    val line2TotalW = wLine2P1 + wLine2P2
                    var currentX = (drawW / 2f) - (line2TotalW / 2f)
                    currentCanvas.drawText(line2Part1, currentX, statsY2, subPaint)
                    currentX += wLine2P1
                    currentCanvas.drawText(line2Part2, currentX, statsY2, maxGradePaint)
                    if (isRotated) {
                        currentCanvas.restore()
                    }
                }

                val isRotatedOuter = AppPreferences.getInstance(context).climbRotate90Clockwise
                val layoutRes = if (isRotatedOuter) R.layout.view_remote_climb_land else R.layout.view_remote_climb
                val remoteViews = RemoteViews(context.packageName, layoutRes)
                remoteViews.setImageViewBitmap(R.id.img_graphic, currentBmp)


                // Navigation Intents
                val intentNext = Intent(ACTION_CLIMB_NEXT).apply { setPackage(context.packageName) }
                val pIntentNext = PendingIntent.getBroadcast(context, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                remoteViews.setOnClickPendingIntent(R.id.btn_next, pIntentNext)
                
                val intentPrev = Intent(ACTION_CLIMB_PREV).apply { setPackage(context.packageName) }
                val pIntentPrev = PendingIntent.getBroadcast(context, 3, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                remoteViews.setOnClickPendingIntent(R.id.btn_prev, pIntentPrev)
                
                // Zoom Intents (+ y -)
                val intentZIn = Intent(ACTION_ZOOM_IN).apply { setPackage(context.packageName) }
                val pIntentZIn = PendingIntent.getBroadcast(context, 1, intentZIn, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                remoteViews.setOnClickPendingIntent(R.id.btn_zoom_in, pIntentZIn)
                remoteViews.setTextViewText(R.id.btn_zoom_in, "+")

                val intentZOut = Intent(ACTION_ZOOM_OUT).apply { setPackage(context.packageName) }
                val pIntentZOut = PendingIntent.getBroadcast(context, 2, intentZOut, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                remoteViews.setOnClickPendingIntent(R.id.btn_zoom_out, pIntentZOut)
                remoteViews.setTextViewText(R.id.btn_zoom_out, "-")

                // Pan Intents (< y >)
                val intentPanLeft = Intent(ACTION_PAN_LEFT).apply { setPackage(context.packageName) }
                val pIntentPanLeft = PendingIntent.getBroadcast(context, 4, intentPanLeft, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val intentPanRight = Intent(ACTION_PAN_RIGHT).apply { setPackage(context.packageName) }
                val pIntentPanRight = PendingIntent.getBroadcast(context, 5, intentPanRight, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                // Pan buttons are always at bottom in ClimbViewer
                remoteViews.setViewVisibility(R.id.btn_pan_left, android.view.View.VISIBLE)
                remoteViews.setViewVisibility(R.id.btn_pan_right, android.view.View.VISIBLE)
                remoteViews.setOnClickPendingIntent(R.id.btn_pan_left, pIntentPanLeft)
                remoteViews.setOnClickPendingIntent(R.id.btn_pan_right, pIntentPanRight)

                emitter.onNext(UpdateGraphicConfig(showHeader = false))
                emitter.updateView(remoteViews)
                }
                }
                withTimeoutOrNull(1000) {
                    refreshChannel.receive()
                }
            }
        }
        // Siempre libera el token al terminar el bucle (excepción, parada del repositorio...); es idempotente
        viewJob.invokeOnCompletion {
            AltgraphRepository.release(emitter)
            AltgraphRepository.removeNavListener(navListener)
        }

        latch.attach(viewJob) {
            // cachedBitmap?.recycle()
            cachedBitmap = null
            cachedCanvas = null
            AltgraphRepository.release(emitter)
            AltgraphRepository.removeNavListener(navListener)
            interactionReceiverRef.release(emitter)
        }
    }
}
