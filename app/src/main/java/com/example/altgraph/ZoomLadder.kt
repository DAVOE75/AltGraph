package com.example.altgraph

// Niveles de zoom 3D (metros de ventana): dos o tres por cada tamaño de bloque principal, para que cada paso cambie de verdad
// la escala de bloques en vez de ir comprimiendo poco a poco el perfil y sus porcentajes
object ZoomLadder {
    val LEVELS = intArrayOf(
        50, 100, 200, 350, 500, 1000, 2000, 3000, 5000, 7000, 10000,
        14000, 20000, 30000, 50000, 75000, 100000, 150000, 200000
    )

    fun zoomIn(curr: Int): Int = LEVELS.lastOrNull { it < curr } ?: LEVELS.first()

    // routeMaxMeters: longitud de la ruta; el último paso hacia fuera la muestra entera
    // Horizonte Táctico: kilómetros enteros de 1 a 10 y luego 15 y 20 km
    private val HORIZON_LEVELS = intArrayOf(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 15000, 20000)

    /** Niveles de vista del horizonte hasta la distancia predictiva (que es siempre el nivel más lejano). */
    fun horizonLevels(pred: Int): List<Int> = (HORIZON_LEVELS.filter { it < pred } + pred).distinct().sorted()

    /** Valor de la lista de distancias más cercano a [m] (para redondear ajustes antiguos como 500 m o 1,5 km). */
    fun nearestHorizonLevel(m: Int): Int = HORIZON_LEVELS.minByOrNull { Math.abs(it - m) } ?: 2000

    /** Toque en la gráfica: baja un nivel y, tras el más cercano, vuelve a la distancia predictiva. Devuelve el valor a guardar (0 = predictiva). */
    fun horizonCycle(view: Int, pred: Int): Int {
        val levels = horizonLevels(pred)
        val cur = if (view > 0) view.coerceAtMost(pred) else pred
        val next = levels.lastOrNull { it < cur } ?: return 0
        return if (next >= pred) 0 else next
    }

    /** Botón +: un nivel más cerca (sin dar la vuelta). */
    fun horizonIn(view: Int, pred: Int): Int {
        val levels = horizonLevels(pred)
        val cur = if (view > 0) view.coerceAtMost(pred) else pred
        val next = levels.lastOrNull { it < cur } ?: cur
        return if (next >= pred) 0 else next
    }

    /** Botón −: un nivel más lejos, hasta la distancia predictiva. */
    fun horizonOut(view: Int, pred: Int): Int {
        val levels = horizonLevels(pred)
        val cur = if (view > 0) view.coerceAtMost(pred) else pred
        val next = levels.firstOrNull { it > cur } ?: cur
        return if (next >= pred) 0 else next
    }

    // routeMaxMeters: longitud de la ruta; el último paso hacia fuera la muestra entera
    fun zoomOut(curr: Int, routeMaxMeters: Int?): Int {
        val maxAllowed = routeMaxMeters ?: LEVELS.last()
        if (curr >= maxAllowed) return curr
        val next = LEVELS.firstOrNull { it > curr } ?: LEVELS.last()
        return if (next >= maxAllowed) maxAllowed else next
    }
}
