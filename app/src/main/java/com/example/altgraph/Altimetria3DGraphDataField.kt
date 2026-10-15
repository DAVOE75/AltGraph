package com.example.altgraph

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Altimetria3DGraphDataType(extension: String) : DataTypeImpl(extension, "altimetria_3d") {

    // Un solo receiver por campo aunque haya varias vistas abiertas: con uno por vista
    // cada toque hacía zoom/pan varias veces
    private val zoomReceiverRef = RefCounted<Pair<Context, BroadcastReceiver>> { (ctx, r) ->
        try { ctx.unregisterReceiver(r) } catch (e: Exception) {}
    }

    companion object {
        const val ACTION_CYCLE_3D_ZOOM = "com.example.altgraph.ACTION_CYCLE_3D_ZOOM"
        const val ACTION_ZOOM_IN = "com.example.altgraph.ACTION_ZOOM_IN"
        const val ACTION_ZOOM_OUT = "com.example.altgraph.ACTION_ZOOM_OUT"
        const val ACTION_TOGGLE_COMPASS = "com.example.altgraph.ACTION_TOGGLE_COMPASS"
        const val ACTION_PAN_LEFT = "com.example.altgraph.ACTION_PAN_LEFT"
        const val ACTION_PAN_RIGHT = "com.example.altgraph.ACTION_PAN_RIGHT"
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        val latch = CancelLatch()
        emitter.setCancellable { latch.cancel() }
        AltgraphRepository.hold(emitter)
        val job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                    val strategy = snap.strategy
                    safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter) }) {
                        emitter.onNext(
                            StreamState.Streaming(
                                DataPoint(
                                    dataTypeId = dataTypeId,
                                    values = mapOf(
                                        DataType.Field.SINGLE to strategy.avgGrade,
                                        "remaining_distance" to strategy.remainingDistance,
                                        "time_to_summit" to strategy.timeToSummit.toDouble(),
                                        "total_fatigue_grade" to strategy.totalFatigueGrade.toDouble()
                                    )
                                )
                            )
                        )
                    }
                }
                delay(1000)
            }
        }
        latch.attach(job) {
            AltgraphRepository.release(emitter)
        }
    }


    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val latch = CancelLatch()
        emitter.setCancellable { latch.cancel() }
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        AltgraphRepository.hold(emitter)

        zoomReceiverRef.acquire(emitter) {
            val zoomReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val routeMaxMeters = AltgraphRepository.snapshot.value?.routeTotalLength?.takeIf { it > 500.0 }?.toInt()
                    if (intent?.action == ACTION_ZOOM_IN && ctx != null) {
                        val prefs = AppPreferences.getInstance(ctx)
                        val curr = prefs.lookaheadMeters3d
                        val nextVal = when {
                            curr <= 50 -> 50
                            curr <= 500 -> curr - 50
                            curr <= 1000 -> curr - 100
                            curr <= 5000 -> curr - 500
                            curr <= 20000 -> curr - 1000
                            curr <= 50000 -> curr - 5000
                            curr <= 100000 -> curr - 10000
                            curr <= 200000 -> curr - 50000
                            else -> 200000
                        }
                        prefs.lookaheadMeters3d = nextVal
                        prefs.smartZoomEnabled = false
                    } else if (intent?.action == ACTION_ZOOM_OUT && ctx != null) {
                        val prefs = AppPreferences.getInstance(ctx)
                        val curr = prefs.lookaheadMeters3d
                        val maxAllowed = routeMaxMeters ?: 200000
                        if (curr >= maxAllowed) {
                            // Ya estamos al zoom máximo permitido (ruta completa o 200km)
                            return
                        }
                        var nextVal = when {
                            curr >= 200000 -> 200000
                            curr >= 100000 -> curr + 50000
                            curr >= 50000 -> curr + 10000
                            curr >= 20000 -> curr + 5000
                            curr >= 5000 -> curr + 1000
                            curr >= 1000 -> curr + 500
                            curr >= 500 -> curr + 100
                            curr >= 50 -> curr + 50
                            else -> 50
                        }
                        if (routeMaxMeters != null && (nextVal > routeMaxMeters || (curr < routeMaxMeters && nextVal >= routeMaxMeters))) {
                            nextVal = routeMaxMeters
                        }
                        prefs.lookaheadMeters3d = nextVal.coerceAtMost(maxAllowed)
                        prefs.smartZoomEnabled = false
                    } else if (intent?.action == ACTION_TOGGLE_COMPASS && ctx != null) {
                        val prefs = AppPreferences.getInstance(ctx)
                        prefs.routeMapHeadingUp = !prefs.routeMapHeadingUp
                    } else if (intent?.action == ACTION_PAN_LEFT && ctx != null) {
                        val prefs = AppPreferences.getInstance(ctx)
                        val step = prefs.lookaheadMeters3d / 3.0
                        AltgraphRepository.pan3d(-step)
                    } else if (intent?.action == ACTION_PAN_RIGHT && ctx != null) {
                        val prefs = AppPreferences.getInstance(ctx)
                        val step = prefs.lookaheadMeters3d / 3.0
                        AltgraphRepository.pan3d(step)
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_ZOOM_IN)
                addAction(ACTION_ZOOM_OUT)
                addAction(ACTION_TOGGLE_COMPASS)
                addAction(ACTION_PAN_LEFT)
                addAction(ACTION_PAN_RIGHT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(zoomReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.applicationContext.registerReceiver(zoomReceiver, filter)
            }
            context.applicationContext to zoomReceiver
        }

        val altimetria3DView = Altimetria3DView(context)
        val w = if (config.viewSize.first > 0) config.viewSize.first else 480
        val h = if (config.viewSize.second > 0) config.viewSize.second else 240

        altimetria3DView.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        altimetria3DView.layout(0, 0, w, h)

        var cachedBitmap: Bitmap? = null
        var cachedCanvas: Canvas? = null
        var lastFrame: List<Any?>? = null

        val viewJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                    safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter) }) {
                        // Suspend (con paneo calcula en el hilo del calculador); dentro de safeUpdate para que un fallo no mate el bucle
                        val strategy = AltgraphRepository.strategyFor3D(snap)
                        val prefs = AppPreferences.getInstance(context)
                        val startElev = if (strategy.windowStartElevation > 0.0) strategy.windowStartElevation else snap.currentElevation

                        // Nada ha cambiado (parado, sin ruta...): no redibujar ni reenviar el bitmap.
                        // Es el campo más caro: render 3D por software y bitmap grande por Binder.
                        val frame = listOf(strategy, startElev, snap.instantBarometricGrade, snap.currentHeading, prefs.snapshot)
                        if (frame == lastFrame) return@safeUpdate
                        altimetria3DView.oasisDistanceToNextCrucible = strategy.oasisDistanceToNextCrucible
                        altimetria3DView.virtualPacerRelativeDistance = strategy.virtualPacerRelativeDistance
                        altimetria3DView.energyBatteryLevel = strategy.energyBatteryLevel
                
                        // ELITE
                        altimetria3DView.stravaSegmentDistance = strategy.stravaSegmentDistance
                        altimetria3DView.stravaPrGhostDistance = strategy.stravaPrGhostDistance
                        altimetria3DView.windEffectIntensity = strategy.windEffectIntensity
                        val isHeadingUp = prefs.routeMapHeadingUp
                        val mapRotAngle = if (isHeadingUp) {
                            // Si el heading es 90, la cámara apunta al este, así que rotamos -90 para que apunte arriba
                            -snap.currentHeading 
                        } else {
                            0.0 
                        }
                
                        altimetria3DView.update3DData(
                            blocks = strategy.nextBlocks,
                            elevation = startElev,
                            maxElev = 727.0,
                            grade = snap.instantBarometricGrade,
                            remainingDist = strategy.remainingDistance,
                            blockSizeMeters = strategy.blockSizeMeters,
                            fontScale = prefs.fontSize3dScale,
                            lookaheadMeters = prefs.lookaheadMeters3d,
                            showCotas = prefs.show3dCotas,
                            showRamps = prefs.show3dRamps,
                            showMaxGrade = prefs.showMaxGradient,
                            fontFamilyKey = prefs.fontFamilyKey,
                            rotate90 = prefs.rotate90Clockwise,
                            rampMinSlope = prefs.rampMinSlopePct,
                            rampMaxSlope = prefs.rampMaxSlopePct,
                            showHairpins = prefs.showHairpins,
                            showPois = (prefs.showPoiTowns || prefs.showPoiWater || prefs.showPoiViewpoints || prefs.showPoiSummits),
                            hairpins = strategy.hairpins,
                            pois = strategy.pois,
                            showZoomControls = prefs.show3dZoomControls,
                            curvatureOffsets = strategy.curvatureOffsets,
                            riderProgress = strategy.riderProgress,
                            windowStartMeters = strategy.windowStartMeters,
                            subBlocks = strategy.subBlocks,
                            subBlockSizeMeters = strategy.subBlockSizeMeters,
                            majorBlockSizeMeters = strategy.majorBlockSizeMeters,
                            profileElevations = strategy.profileElevations,
                            showBlockPercentages = prefs.showBlockPercentages,
                            altimetriaStyle = prefs.altimetriaStyle,
                            targetVam = prefs.targetVam,
                            activeClimbs = snap.routeClimbs,
                            visibleAvgGrade = strategy.visibleAvgGrade,
                            visibleMaxGrade = strategy.visibleMaxGrade,
                            routeName = strategy.routeName,
                            routeCoords = strategy.routeCoords,
                            mapRotationAngle = mapRotAngle,
                            isHeadingUp = isHeadingUp,
                            showSurfaceTextures = prefs.showSurfaceTextures,
                            routeTotalLength = snap.routeTotalLength
                        )

                        if (cachedBitmap == null || cachedBitmap?.width != w || cachedBitmap?.height != h) {
                            // cachedBitmap?.recycle()
                            val newBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            cachedBitmap = newBmp
                            cachedCanvas = Canvas(newBmp)
                        }

                        val currentBmp = cachedBitmap!!
                        val currentCanvas = cachedCanvas!!
                        altimetria3DView.draw(currentCanvas)

                          // --- DRAW COMPASS ---
                          if (prefs.showRouteCompass && strategy.routeCoords.isNotEmpty() && prefs.altimetriaStyle == AltimetriaStyle.GLOBAL_ISOMETRIC) {
                              val rotate90 = prefs.rotate90Clockwise
                              val vw = if (rotate90) h.toFloat() else w.toFloat()
                              val vh = if (rotate90) w.toFloat() else h.toFloat()

                              val pillW = (vw * 0.24f).coerceIn(86f, 135f)
                              val pillH = (vh * 0.15f).coerceIn(26f, 40f)
                              val marginT = 4f
                              val marginR = 16f
                              val bottom = marginT + pillH
                      
                              val vCompassX = vw - marginR - (pillW / 2f)
                              val vCompassY = bottom + 35f
                      
                              currentCanvas.save()
                              if (rotate90) {
                                  currentCanvas.translate(w.toFloat(), 0f)
                                  currentCanvas.rotate(90f)
                              }
                      
                              val cx = vCompassX
                              val cy = vCompassY
                    
                              val compassPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                  color = android.graphics.Color.WHITE
                                  style = android.graphics.Paint.Style.STROKE
                                  strokeWidth = 3f
                              }
                              currentCanvas.drawCircle(cx, cy, 20f, compassPaint)
                      
                              val arrowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                  color = android.graphics.Color.RED
                                  style = android.graphics.Paint.Style.FILL
                              }
                              val bgArrowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                  color = android.graphics.Color.GRAY
                                  style = android.graphics.Paint.Style.FILL
                              }
                      
                              val angleOffset = if (isHeadingUp) -snap.currentHeading else 0.0
                      
                              currentCanvas.save()
                              currentCanvas.rotate(angleOffset.toFloat(), cx, cy)
                      
                              // Draw north arrow (red)
                              val pathNorth = android.graphics.Path()
                              pathNorth.moveTo(cx, cy - 20f)
                              pathNorth.lineTo(cx + 8f, cy)
                              pathNorth.lineTo(cx - 8f, cy)
                              pathNorth.close()
                              currentCanvas.drawPath(pathNorth, arrowPaint)
                      
                              // Draw south arrow (gray)
                              val pathSouth = android.graphics.Path()
                              pathSouth.moveTo(cx, cy + 20f)
                              pathSouth.lineTo(cx + 8f, cy)
                              pathSouth.lineTo(cx - 8f, cy)
                              pathSouth.close()
                              currentCanvas.drawPath(pathSouth, bgArrowPaint)
                      
                              currentCanvas.restore()
                      
                              // Draw text N
                              val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                  color = android.graphics.Color.WHITE
                                  textSize = 14f
                                  textAlign = android.graphics.Paint.Align.CENTER
                                  typeface = android.graphics.Typeface.DEFAULT_BOLD
                              }
                      
                              if (!isHeadingUp) {
                                  currentCanvas.drawText("N", cx, cy - 25f, textPaint)
                              } else {
                                  // Letra "R" de Rumbo o "H" de Heading
                                  currentCanvas.drawText("H", cx, cy - 25f, textPaint)
                              }
                      
                              currentCanvas.restore()
                          }

                        val layoutId = if (prefs.rotate90Clockwise) R.layout.view_remote_3d_land else R.layout.view_remote_3d
                        val remoteViews = RemoteViews(context.packageName, layoutId)
                        remoteViews.setImageViewBitmap(R.id.img_graphic, currentBmp)

                        if (prefs.show3dZoomControls) {
                            val intentZoomIn = Intent(ACTION_ZOOM_IN).apply { setPackage(context.packageName) }
                            val pendingZoomIn = PendingIntent.getBroadcast(context, 4, intentZoomIn, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            remoteViews.setOnClickPendingIntent(R.id.btn_zoom_in, pendingZoomIn)

                            val intentZoomOut = Intent(ACTION_ZOOM_OUT).apply { setPackage(context.packageName) }
                            val pendingZoomOut = PendingIntent.getBroadcast(context, 5, intentZoomOut, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            remoteViews.setOnClickPendingIntent(R.id.btn_zoom_out, pendingZoomOut)
                    
                            remoteViews.setViewVisibility(R.id.btn_zoom_in, android.view.View.VISIBLE)
                            remoteViews.setViewVisibility(R.id.btn_zoom_out, android.view.View.VISIBLE)
                        } else {
                            remoteViews.setViewVisibility(R.id.btn_zoom_in, android.view.View.GONE)
                            remoteViews.setViewVisibility(R.id.btn_zoom_out, android.view.View.GONE)
                        }
                                        if (prefs.showRouteCompass && strategy.routeCoords.isNotEmpty() && prefs.altimetriaStyle == AltimetriaStyle.GLOBAL_ISOMETRIC) {
                              val intentCompass = Intent(ACTION_TOGGLE_COMPASS).apply {
                                  setPackage(context.packageName)
                              }
                              val pendingIntentCompass = PendingIntent.getBroadcast(
                                  context,
                                  1,
                                  intentCompass,
                                  PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                              )
                              remoteViews.setViewVisibility(R.id.btn_toggle_compass, android.view.View.VISIBLE)
                              remoteViews.setViewVisibility(R.id.btn_toggle_compass_land, android.view.View.VISIBLE)
                              remoteViews.setOnClickPendingIntent(R.id.btn_toggle_compass, pendingIntentCompass)
                              remoteViews.setOnClickPendingIntent(R.id.btn_toggle_compass_land, pendingIntentCompass)
                          } else {
                              remoteViews.setViewVisibility(R.id.btn_toggle_compass, android.view.View.GONE)
                              remoteViews.setViewVisibility(R.id.btn_toggle_compass_land, android.view.View.GONE)
                          }

                          val intentPanLeft = Intent(ACTION_PAN_LEFT).apply { setPackage(context.packageName) }
                          val pendingPanLeft = PendingIntent.getBroadcast(context, 2, intentPanLeft, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                          remoteViews.setOnClickPendingIntent(R.id.btn_pan_left, pendingPanLeft)

                          val intentPanRight = Intent(ACTION_PAN_RIGHT).apply { setPackage(context.packageName) }
                          val pendingPanRight = PendingIntent.getBroadcast(context, 3, intentPanRight, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                          remoteViews.setOnClickPendingIntent(R.id.btn_pan_right, pendingPanRight)

                        emitter.updateView(remoteViews)

                        // Solo tras enviar con éxito: si el frame falla, el siguiente tick lo reintenta
                        lastFrame = frame
                    }
                }
                delay(1000)
            }
        }
        // Siempre libera el token al terminar el bucle (excepción, parada del repositorio...); es idempotente
        viewJob.invokeOnCompletion {
            AltgraphRepository.release(emitter)
        }

        latch.attach(viewJob) {
            AltgraphRepository.release(emitter)
            // cachedBitmap?.recycle()
            cachedBitmap = null
            cachedCanvas = null
            zoomReceiverRef.release(emitter)
        }
    }
}