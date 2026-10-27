package com.example.altgraph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClimbAlertTrackerTest {
    private fun climb(start: Double) = RouteClimb(
        startDistance = start, endDistance = start + 2000.0, length = 2000.0,
        totalElevation = 120.0, avgGrade = 6.0
    )

    @Test
    fun alertsOnceWhenApproachingTheStart() {
        val t = ClimbAlertTracker(400.0)
        val climbs = listOf(climb(1000.0))
        assertNull(t.next(500.0, climbs))
        assertNotNull(t.next(650.0, climbs))
        assertNull(t.next(700.0, climbs))
        assertNull(t.next(900.0, climbs))
    }

    @Test
    fun noAlertForClimbAlreadyInside() {
        val t = ClimbAlertTracker(400.0)
        assertNull(t.next(1500.0, listOf(climb(1000.0))))
    }

    @Test
    fun rearmsWhenRidingBackThroughTheRoute() {
        val t = ClimbAlertTracker(400.0)
        val climbs = listOf(climb(3000.0))
        assertNotNull(t.next(2800.0, climbs))
        assertNull(t.next(3100.0, climbs))
        assertNull(t.next(100.0, climbs))
        assertNotNull(t.next(2700.0, climbs))
    }

    @Test
    fun consecutiveClimbsAlertOneAtATime() {
        val t = ClimbAlertTracker(400.0)
        val climbs = listOf(climb(1000.0), climb(1300.0))
        assertEquals(0, t.next(900.0, climbs)!!.index)
        assertEquals(1, t.next(905.0, climbs)!!.index)
        assertNull(t.next(910.0, climbs))
    }
}
