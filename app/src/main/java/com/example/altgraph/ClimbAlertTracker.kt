package com.example.altgraph

/**
 * Decide cuándo avisar de un puerto: una vez por puerto, al quedar triggerMeters o menos para su inicio.
 * No avisa de puertos en los que el ciclista ya está dentro. Al volver atrás en la ruta (re-ride) se rearma.
 */
class ClimbAlertTracker(private val triggerMeters: Double = 400.0) {
    data class Alert(val index: Int, val climb: RouteClimb)

    private val alerted = HashSet<Long>()
    private var lastDist = -1.0

    fun next(currentDist: Double, climbs: List<RouteClimb>): Alert? {
        if (lastDist >= 0.0 && currentDist < lastDist - 500.0) alerted.clear()
        lastDist = currentDist
        for ((idx, climb) in climbs.withIndex()) {
            val toStart = climb.startDistance - currentDist
            if (toStart < 0.0 || toStart > triggerMeters) continue
            if (alerted.add(Math.round(climb.startDistance))) return Alert(idx, climb)
        }
        return null
    }

    fun reset() {
        alerted.clear()
        lastDist = -1.0
    }
}
