package com.example.altgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GearAdvisorTest {
    @Test
    fun parsesTeethAndIgnoresGarbage() {
        assertEquals(listOf(34, 50), GearAdvisor.parseTeeth("50, 34;abc,34"))
    }

    @Test
    fun onlyUsesConfiguredGears() {
        val gear = GearAdvisor.recommend(14.0, "40", "10,12,14,16")
        assertTrue(gear == "40x16" || gear == "40x14" || gear == "40x12" || gear == "40x10")
        assertEquals("40x16", gear)
    }

    @Test
    fun climbsPickSmallRingAndFlatPicksBigRing() {
        val climb = GearAdvisor.recommend(12.0, GearAdvisor.DEFAULT_CHAINRINGS, GearAdvisor.DEFAULT_CASSETTE)
        val flat = GearAdvisor.recommend(0.0, GearAdvisor.DEFAULT_CHAINRINGS, GearAdvisor.DEFAULT_CASSETTE)
        assertTrue(climb.startsWith("34x"))
        assertTrue(flat.startsWith("50x"))
    }

    @Test
    fun emptyConfigFallsBackToDefaults() {
        assertTrue(GearAdvisor.recommend(5.0, "", "").contains("x"))
    }

    @Test
    fun cadenceTargetsFollowTerrain() {
        assertEquals(77.5, GearAdvisor.targetCadence(8.0), 1e-9)
        assertEquals(90.0, GearAdvisor.targetCadence(0.5), 1e-9)
        assertEquals(95.0, GearAdvisor.targetCadence(-6.0), 1e-9)
    }

    @Test
    fun effortGearUsesBigRingOnFlatAndSmallRingOnRamp() {
        val rings = GearAdvisor.DEFAULT_CHAINRINGS
        val cogs = GearAdvisor.DEFAULT_CASSETTE
        val flat = GearAdvisor.recommendForEffort(0.0, 250, 75.0, null, rings, cogs)
        val ramp = GearAdvisor.recommendForEffort(7.0, 250, 75.0, null, rings, cogs)
        assertTrue(flat, flat.startsWith("50x"))
        assertTrue(ramp, ramp.startsWith("34x"))
    }

    @Test
    fun effortGearEasesWhenHeartRateIsHigh() {
        fun ratio(g: String): Double { val p = g.split("x"); return p[0].toDouble() / p[1].toDouble() }
        val rings = GearAdvisor.DEFAULT_CHAINRINGS
        val cogs = GearAdvisor.DEFAULT_CASSETTE
        val rested = GearAdvisor.recommendForEffort(0.0, 250, 75.0, 0.60, rings, cogs)
        val tired = GearAdvisor.recommendForEffort(0.0, 250, 75.0, 0.95, rings, cogs)
        assertTrue("$rested vs $tired", ratio(tired) <= ratio(rested))
    }

    @Test
    fun expectedSpeedDropsOnSteeperGrades() {
        val flat = GearAdvisor.expectedSpeed(200.0, 0.0, 75.0)
        val ramp = GearAdvisor.expectedSpeed(200.0, 8.0, 75.0)
        assertTrue(flat > ramp)
        assertTrue(ramp > 1.0)
    }
}
