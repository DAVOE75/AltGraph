package com.example.altgraph

/** Ultimo trazado del Road Horizon por delante del ciclista, para dibujarlo en el mapa cuando no hay ruta cargada. */
object HorizonOverlayBus {
    class Snapshot(
        val coords: List<Pair<Double, Double>>,
        val grades: List<Float>,
        val stepMeters: Double,
        val riderIndex: Double,
        val timeMs: Long
    )

    @Volatile var latest: Snapshot? = null

    fun publish(coords: List<Pair<Double, Double>>, grades: List<Float>, stepMeters: Double, riderIndex: Double) {
        latest = Snapshot(coords, grades, stepMeters, riderIndex, System.currentTimeMillis())
    }

    fun clear() { latest = null }
}
