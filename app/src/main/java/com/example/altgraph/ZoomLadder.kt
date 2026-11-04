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
    fun zoomOut(curr: Int, routeMaxMeters: Int?): Int {
        val maxAllowed = routeMaxMeters ?: LEVELS.last()
        if (curr >= maxAllowed) return curr
        val next = LEVELS.firstOrNull { it > curr } ?: LEVELS.last()
        return if (next >= maxAllowed) maxAllowed else next
    }
}
