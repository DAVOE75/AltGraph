package com.example.altgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutAndBackDirectionTest {

    private val step = 0.00009 // ~10 m de latitud
    private val lon = -0.2

    // Misma carretera: 2 km de ida y 2 km de vuelta por los mismos puntos
    private fun outAndBack(): AltimetriaStrategyCalculator {
        val c = AltimetriaStrategyCalculator()
        val pts = ArrayList<RoutePoint>()
        for (i in 0..200) pts.add(RoutePoint(38.8 + i * step, lon, 100.0 + i * 0.5, i * 10.0))
        for (i in 1..200) pts.add(RoutePoint(38.8 + (200 - i) * step, lon, 200.0 - i * 0.5, 2000.0 + i * 10.0))
        c.routePoints = pts
        c.currentSpeed = 5.0
        return c
    }

    private fun ride(c: AltimetriaStrategyCalculator, fromM: Int, toM: Int, jitter: Boolean = false) {
        val dir = if (toM >= fromM) 5 else -5
        var m = fromM
        var n = 0
        while (if (dir > 0) m <= toM else m >= toM) {
            val noise = if (jitter) (if (n % 2 == 0) 0.00002 else -0.00002) else 0.0
            c.updateCurrentLocation(38.8 + m / 10.0 * step + noise, lon)
            m += dir
            n++
        }
    }

    @Test
    fun outboundNeverJumpsToReturnLeg() {
        val c = outAndBack()
        var last = 0.0
        for (m in 0..1500 step 5) {
            c.updateCurrentLocation(38.8 + m / 10.0 * step + (if ((m / 5) % 2 == 0) 0.00002 else -0.00002), lon)
            assertTrue("salto a la vuelta en $m m: ${c.currentRouteDistance}", c.currentRouteDistance < 2000.0)
            assertTrue("retroceso en $m m", c.currentRouteDistance >= last - 5.0)
            last = c.currentRouteDistance
        }
        assertEquals(1500.0, c.currentRouteDistance, 15.0)
    }

    @Test
    fun returnLegIsRespectedWhenRidingBack() {
        val c = outAndBack()
        ride(c, 0, 2000)
        // continúa por el tramo de vuelta de la propia ruta
        ride(c, 1995, 1500)
        assertEquals(2500.0, c.currentRouteDistance, 15.0)
    }

    @Test
    fun turningAroundMidClimbJumpsToReturnLeg() {
        val c = outAndBack()
        ride(c, 0, 1000, jitter = true)
        assertEquals(1000.0, c.currentRouteDistance, 15.0)
        ride(c, 995, 500, jitter = true)
        // en el punto de 600 m de la carretera, la vuelta está en 4000 - 600
        assertEquals(3500.0, c.currentRouteDistance, 20.0)
    }

    @Test
    fun smallStopAndSlowCreepDoesNotJump() {
        val c = outAndBack()
        ride(c, 0, 800)
        repeat(60) { c.updateCurrentLocation(38.8 + 80 * step + (if (it % 2 == 0) 0.00001 else -0.00001), lon) }
        assertTrue(c.currentRouteDistance < 2000.0)
    }
}
