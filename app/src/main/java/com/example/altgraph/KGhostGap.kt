package com.example.altgraph

import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import java.util.Locale

// Streams de la extensión KGhost: segundos (tiempo) y metros (distancia); positivo = por delante.
const val KGHOST_GAP_TIME = "TYPE_EXT::kghost::kghost-gap-time"
const val KGHOST_GAP_DIST = "TYPE_EXT::kghost::kghost-gap-dist"

data class GapPart(val value: Double, val estimated: Boolean)

data class GhostGap(val timeS: Double?, val distM: Double?, val estimated: Boolean)

fun kghostPart(state: StreamState): GapPart? {
    if (state !is StreamState.Streaming) return null
    val v = state.dataPoint.values[DataType.Field.SINGLE] ?: return null
    return GapPart(v, state.dataPoint.values["estimated"] == 1.0)
}

fun combineGhostGap(time: GapPart?, dist: GapPart?): GhostGap? {
    if (time == null && dist == null) return null
    return GhostGap(time?.value, dist?.value, time?.estimated == true || dist?.estimated == true)
}

private fun sign(v: Double) = if (v > 0) "+" else if (v < 0) "−" else ""

private fun timeText(s: Double): String {
    val r = Math.round(s)
    val a = Math.abs(r)
    val body = if (a < 60) "$a s" else String.format(Locale.US, "%d:%02d", a / 60, a % 60)
    return (if (r > 0) "+" else if (r < 0) "−" else "") + body
}

private fun distText(d: Double): String {
    val r = Math.round(d)
    val a = Math.abs(d)
    return if (Math.abs(r) < 1000) {
        sign(r.toDouble()) + Math.abs(r) + " m"
    } else {
        sign(d) + String.format(Locale.US, "%.1f km", a / 1000.0)
    }
}

fun timeGapText(s: Double): String = timeText(s)

fun distGapText(d: Double): String = distText(d)

fun formatGhostGap(gap: GhostGap, maxWidth: Float, measure: (String) -> Float): String {
    val t = gap.timeS?.let(::timeText)
    val d = gap.distM?.let(::distText)
    val full = listOfNotNull(t, d).joinToString(" · ")
    if (t == null || d == null || measure(full) <= maxWidth) return full
    return t
}

fun ghostGapColor(gap: GhostGap): Int {
    if (gap.estimated) return 0xFFFFC107.toInt()
    val v = gap.timeS ?: gap.distM ?: 0.0
    return when {
        v > 0 -> 0xFF4CAF50.toInt()
        v < 0 -> 0xFFF44336.toInt()
        else -> 0xFFFFFFFF.toInt()
    }
}
