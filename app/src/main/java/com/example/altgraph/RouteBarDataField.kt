package com.example.altgraph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RouteBarDataType(extension: String) : DataTypeImpl(extension, "route_bar") {

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
                                        "remaining_distance" to strategy.remainingDistance
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

        val routeBarView = RouteBarView(context)
        // If it's a very tall and thin view, it's likely on the side. 


        val w = if (config.viewSize.first > 0) config.viewSize.first else 120
        val h = if (config.viewSize.second > 0) config.viewSize.second else 600

        routeBarView.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        routeBarView.layout(0, 0, w, h)

        var cachedBitmap: Bitmap? = null
        var cachedCanvas: Canvas? = null
        val bgPaint = Paint().apply { color = Color.TRANSPARENT }
        var lastFrame: List<Any?>? = null

        val viewJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                    val prefs = AppPreferences.getInstance(context)
                    val isElite = prefs.eliteRadarEnabled
                    safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter) }) {
                        // Suspend (calcula en el hilo del calculador); dentro de safeUpdate para que un fallo no mate el bucle
                        val strategy = if (isElite) AltgraphRepository.strategyForRouteBar(w, snap) else null
                        if (cachedBitmap == null || cachedBitmap!!.width != w || cachedBitmap!!.height != h) {
                            cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            cachedCanvas = Canvas(cachedBitmap!!)
                        }

                        val currentBmp = cachedBitmap!!
                        val currentCanvas = cachedCanvas!!

                        val gap = if (isElite && prefs.eliteGhostEnabled) snap.ghostGap else null

                        // Nada ha cambiado (parado, sin ruta...): no redibujar ni reenviar el bitmap
                        val frame = listOf(strategy, gap, prefs.snapshot)
                        if (frame == lastFrame) return@safeUpdate

                        // Clear the canvas with transparent background so map shows through
                        currentCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                
                        if (isElite) {
                            routeBarView.strategyData = strategy
                            routeBarView.ghostGap = gap
                            routeBarView.ghostStyle = prefs.eliteGhostStyle
                            routeBarView.showEnergyBar = prefs.eliteEnergyBarEnabled
                            routeBarView.showPoiRuler = prefs.elitePoiRulerEnabled
                            routeBarView.showHistogram = prefs.eliteHistogramEnabled
                            routeBarView.showRadar3d = prefs.eliteRadar3dEnabled
                            routeBarView.radarTheme = prefs.eliteRadarTheme
                            routeBarView.showPowerBar = prefs.elitePowerBarEnabled
                            routeBarView.showRadarAlerts = prefs.eliteRadarAlertsEnabled
                            routeBarView.showPowerGuide = prefs.elitePowerGuideEnabled
                            routeBarView.userFtp = prefs.userFtp
                            routeBarView.draw(currentCanvas)
                        } else {
                            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.RED
                                textSize = 14f
                                textAlign = Paint.Align.CENTER
                            }
                            currentCanvas.drawText("ELITE", w/2f, h/2f, p)
                        }

                        val remoteViews = RemoteViews(context.packageName, R.layout.view_remote_graphic)
                        remoteViews.setImageViewBitmap(R.id.img_graphic, currentBmp)

                        emitter.onNext(UpdateGraphicConfig(showHeader = false))
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
        }
    }
}
