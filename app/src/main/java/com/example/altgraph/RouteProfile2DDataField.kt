package com.example.altgraph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.example.altgraph.R
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

class RouteProfile2DDataField(extension: String) : DataTypeImpl(extension, "profile_tv_2d") {

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
                                        DataType.Field.SINGLE to strategy.riderProgress.toDouble()
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
        AltgraphRepository.hold(emitter)

        val w = if (config.viewSize.first > 0) config.viewSize.first else 200
        val h = if (config.viewSize.second > 0) config.viewSize.second else 200

        val routeProfileView = RouteProfile2DView(context)
        // Set fixed layout params so it draws correctly to the bitmap
        routeProfileView.layoutParams = android.view.ViewGroup.LayoutParams(w, h)
        routeProfileView.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        routeProfileView.layout(0, 0, w, h)

        var cachedBitmap: Bitmap? = null
        var cachedCanvas: Canvas? = null

        var lastFrame: List<Any?>? = null
        var lastRouteName: String? = null
        var lastRouteLength: Double = -1.0

        val viewJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                    val prefs = AppPreferences.getInstance(context)
                    val isElite = prefs.isEliteUnlocked
                    safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter) }) {
                        if (cachedBitmap == null || cachedBitmap!!.width != w || cachedBitmap!!.height != h) {
                            cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            cachedCanvas = Canvas(cachedBitmap!!)
                        }

                        val currentBmp = cachedBitmap!!
                        val currentCanvas = cachedCanvas!!
                        val strategy = snap.strategy

                        val isRotated = prefs.eliteProfileTv2dRotate
                        val frame = listOf(
                            snap.routeTotalLength,
                            strategy.remainingDistance,
                            isRotated,
                            isElite,
                            snap.routeClimbs.size,
                            snap.routeClimbs.map { it.name to it.startDistance },
                            prefs.eliteProfileTvShowClimbNames,
                            prefs.eliteProfileTvColor,
                            prefs.eliteHighlightClimbs
                        )
                        if (frame == lastFrame) return@safeUpdate

                        // Fetch new full profile if route changed
                        if (strategy.routeName != lastRouteName || snap.routeTotalLength != lastRouteLength) {
                            val fullProfile = AltgraphRepository.getFullProfileElevations()
                            routeProfileView.profileElevations = fullProfile
                            
                            val fullCoords = AltgraphRepository.getFullRouteCoords()
                            routeProfileView.routeCoords = fullCoords
                            
                            var gain = 0.0
                            for (i in 1 until fullProfile.size) {
                                val diff = fullProfile[i] - fullProfile[i - 1]
                                if (diff > 0) gain += diff
                            }
                            routeProfileView.altitudeGain = gain
                            routeProfileView.routeName = strategy.routeName ?: ""
                            lastRouteName = strategy.routeName
                            lastRouteLength = snap.routeTotalLength
                        }

                        currentCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                
                        if (isElite) {
                            routeProfileView.totalDistance = snap.routeTotalLength
                            // current distance is overall distance travelled
                            routeProfileView.currentDistance = snap.currentRouteDistance
                            routeProfileView.isRotated = isRotated
                            routeProfileView.fontFamilyKey = prefs.fontFamilyKey
                            routeProfileView.fontSizeScale = prefs.fontSize3dScale
                            routeProfileView.altitudeGainText = context.getString(R.string.profile_tv_2d_gain, routeProfileView.altitudeGain)
                            routeProfileView.activeClimbs = snap.routeClimbs
                            routeProfileView.tvColor = prefs.eliteProfileTvColor
                            routeProfileView.highlightClimbs = prefs.eliteHighlightClimbs
                            routeProfileView.showClimbNames = prefs.eliteProfileTvShowClimbNames

                            routeProfileView.layoutParams = android.view.ViewGroup.LayoutParams(w, h)
                            routeProfileView.measure(
                                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
                            )
                            routeProfileView.layout(0, 0, w, h)

                            // Draw view
                            routeProfileView.draw(currentCanvas)
                        } else {
                            // Draw Elite Locked message
                            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.GRAY
                                textSize = 16f
                                textAlign = Paint.Align.CENTER
                            }
                            currentCanvas.drawText("ELITE REQUIRED", w / 2f, h / 2f, textPaint)
                        }

                        lastFrame = frame

                        val remoteViews = RemoteViews(context.packageName, R.layout.view_remote_graphic).apply {
                            setImageViewBitmap(R.id.img_graphic, currentBmp)
                        }

                        emitter.onNext(UpdateGraphicConfig(showHeader = false))
                        emitter.updateView(remoteViews)
                    }
                }
                delay(1000)
            }
        }

        latch.attach(viewJob) {
            AltgraphRepository.release(emitter)
            cachedBitmap?.recycle()
        }
    }
}
