package com.example.altgraph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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

class GradeZonesDataField(extension: String) : DataTypeImpl(extension, "grade_zones") {

    private val tracker = GradeZonesTracker()
    private var byTime = false // false = by distance, true = by time

    override fun startStream(emitter: Emitter<StreamState>) {
        val latch = CancelLatch()
        emitter.setCancellable { latch.cancel() }
        AltgraphRepository.hold(emitter)

        val streamJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                    safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter) }) {
                        val grade = snap.rawGrade
                        val speed = snap.currentSpeed
                        
                        tracker.addSample(grade, speed)
                        
                        emitter.onNext(
                            StreamState.Streaming(
                                DataPoint(
                                    dataTypeId = dataTypeId,
                                    values = mapOf(
                                        DataType.Field.SINGLE to grade
                                    )
                                )
                            )
                        )
                    }
                }
                delay(1000)
            }
        }
        latch.attach(streamJob) { AltgraphRepository.release(emitter) }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val latch = CancelLatch()
        emitter.setCancellable { latch.cancel() }
        emitter.onNext(UpdateGraphicConfig(showHeader = false))
        AltgraphRepository.hold(emitter)

        val w = if (config.viewSize.first > 0) config.viewSize.first else 480
        val h = if (config.viewSize.second > 0) config.viewSize.second else 240

        val zonesView = GradeZonesView(context)
        zonesView.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        zonesView.layout(0, 0, w, h)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var lastFrame: List<Double>? = null

        val viewJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val snap = AltgraphRepository.snapshot.value
                if (snap != null) {
                    safeUpdate(onDead = { cancel(); AltgraphRepository.release(emitter) }) {
                        val prefs = AppPreferences.getInstance(context)
                        if (!prefs.isEliteUnlocked) {
                            // Render ELITE locked view
                            bitmap.eraseColor(android.graphics.Color.parseColor("#151515"))
                            val p = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#EAB308") // Gold/Yellow
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                val fontPrefs = AppPreferences.getInstance(context)
                                FontHelper.applyFontToPaint(this, fontPrefs.fontFamilyKey, android.graphics.Typeface.BOLD)
                            }
                            canvas.drawText("ELITE", w / 2f, h / 2f, p)
                            
                            val remoteViews = RemoteViews(context.packageName, R.layout.view_remote_graphic)
                            remoteViews.setImageViewBitmap(R.id.img_graphic, bitmap)
                            emitter.updateView(remoteViews)
                            
                            // Don't update lastFrame so when it unlocks, it refreshes
                        } else {
                            val pctDist = tracker.getPercentages(byTime = false)
                            val pctTime = tracker.getPercentages(byTime = true)
                            
                            // Avoid redrawing if nothing changed
                            val frame = pctDist.toList() + pctTime.toList()
                            if (frame != lastFrame) {
                                zonesView.updateData(pctDist, pctTime, tracker.timePerZone)
                                bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
                                zonesView.draw(canvas)

                                val remoteViews = RemoteViews(context.packageName, R.layout.view_remote_graphic)
                                remoteViews.setImageViewBitmap(R.id.img_graphic, bitmap)

                                emitter.updateView(remoteViews)
                                lastFrame = frame
                            }
                        }
                    }
                }
                delay(1000)
            }
        }

        latch.attach(viewJob) { AltgraphRepository.release(emitter) }
    }
}
