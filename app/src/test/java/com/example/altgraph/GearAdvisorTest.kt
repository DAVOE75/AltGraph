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
}
