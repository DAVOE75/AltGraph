package com.example.altgraph

import io.hammerhead.karooext.models.OnNavigationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AltgraphCoreTest {

    private fun accumulated(c: AltimetriaStrategyCalculator): Double {
        val f = AltimetriaStrategyCalculator::class.java.getDeclaredField("liveDistanceAccumulated")
        f.isAccessible = true
        return (f.get(c) as Number).toDouble()
    }

    @Test
    fun tickAdvancesOncePerTickRegardlessOfReaders() {
        val core = AltgraphCore()
        core.calculator.currentSpeed = 10.0
        val snap = core.tick(null)
        val dist = accumulated(core.calculator)

        core.mapZoomLevel = 14.0
        core.pan3dMeters = 500.0
        repeat(3) {
            Thread.sleep(50)
            core.strategyForRouteBar(null, 120, snap)
            core.strategyFor3D(null, snap)
        }
        assertEquals(dist, accumulated(core.calculator), 0.0)
    }

    @Test
    fun snapshotCarriesRawGradeUnblended() {
        val core = AltgraphCore()
        core.rawGrade = 6.5
        core.calculator.updateLiveGrade(2.0)
        val snap = core.tick(null)
        assertEquals(6.5, snap.rawGrade, 0.0)
    }

    private fun withRoute(c: AltimetriaStrategyCalculator) {
        val poly = PolylineCodec.encode(listOf(40.0 to -3.0, 40.0225 to -3.0, 40.045 to -3.0), 1e5)
        NavigationSync.applyNavigation(
            c,
            OnNavigationState.NavigationState.NavigatingRoute(
                routePolyline = poly, routeDistance = 5000.0, routeElevationPolyline = null,
                rejoinPolyline = null, rejoinDistance = null, name = "T", reversed = false,
                breadcrumb = false, pois = emptyList(), climbs = emptyList()
            )
        )
    }

    @Test
    fun routeBarZoomDoesNotLeakIntoTick() {
        val core = AltgraphCore()
        val twin = AltimetriaStrategyCalculator()
        withRoute(core.calculator)
        withRoute(twin)
        val snap = core.tick(null)
        twin.calculateStrategy(null) // mismo número de ticks que el core
        core.mapZoomLevel = 14.0
        val zoomed = core.strategyForRouteBar(null, 120, snap)
        assertNull(core.calculator.mapZoomLevel)
        // El zoom cambia de verdad la estrategia (la igualdad de abajo no es trivial)
        assertNotEquals(snap.strategy, zoomed)
        // Un zoom que se quedara en el calculador no debe colarse en el tick
        core.calculator.mapZoomLevel = 14.0
        assertEquals(twin.calculateStrategy(null), core.tick(null).strategy)
        assertNull(core.calculator.mapZoomLevel)
    }

    @Test
    fun panLeftOnCalculatorDoesNotLeakIntoTick() {
        val core = AltgraphCore()
        val twin = AltimetriaStrategyCalculator()
        withRoute(core.calculator)
        withRoute(twin)
        core.tick(null)
        twin.calculateStrategy(null)
        core.calculator.manualPanOffsetMeters = 700.0
        assertEquals(twin.calculateStrategy(null), core.tick(null).strategy)
        assertEquals(0.0, core.calculator.manualPanOffsetMeters, 0.0)
    }

    @Test
    fun panOnlyAffects3D() {
        val core = AltgraphCore()
        val poly = PolylineCodec.encode(listOf(40.0 to -3.0, 40.0225 to -3.0, 40.045 to -3.0), 1e5)
        NavigationSync.applyNavigation(
            core.calculator,
            OnNavigationState.NavigationState.NavigatingRoute(
                routePolyline = poly, routeDistance = 5000.0, routeElevationPolyline = null,
                rejoinPolyline = null, rejoinDistance = null, name = "T", reversed = false,
                breadcrumb = false, pois = emptyList(), climbs = emptyList()
            )
        )
        val snap = core.tick(null)
        core.calculator.currentRouteDistance = 1500.0
        val base = core.strategyFor3D(null, snap).windowStartMeters
        core.pan3dMeters = -100.0
        val s3d = core.strategyFor3D(null, snap)
        assertNotEquals(base, s3d.windowStartMeters, 0.0)
        assertEquals(0.0, core.calculator.manualPanOffsetMeters, 0.0)
    }

    @Test
    fun anchoredBeaconStaysAtFixedFractionOfWindow() {
        val core = AltgraphCore()
        withRoute(core.calculator)
        val snap = core.tick(null)
        var checked = 0
        for (d in 0..5000 step 25) {
            core.calculator.currentRouteDistance = d.toDouble()
            val s = core.calculator.calculateStrategy(null)
            assertEquals("d=$d", true, s.riderProgress in 0f..0.96f)
            if (s.windowStartMeters > 0.0 && s.windowStartMeters + s.windowLengthMeters < 4900.0) {
                // Con la ventana en movimiento la baliza queda fija en pantalla (20% por defecto)
                assertEquals("d=$d", 0.2, s.riderProgress.toDouble(), 1e-3)
                checked++
            }
        }
        assertEquals(snap.strategy.riderProgress >= 0f, true)
        assertEquals(true, checked >= 1)
    }

    @Test
    fun gearGradeComesFromRouteAheadOfRider() {
        val core = AltgraphCore()
        withRoute(core.calculator)
        core.tick(null)
        core.calculator.currentRouteDistance = 1500.0
        val s = core.calculator.calculateStrategy(null, advance = false)
        assertEquals(true, s.gearGrade != null)
    }

    @Test
    fun alignedGridDoesNotDependOnRiderPosition() {
        val core = AltgraphCore()
        withRoute(core.calculator)
        core.tick(null)
        for (d in 1000..3000 step 37) {
            core.calculator.currentRouteDistance = d.toDouble()
            val plain = core.calculator.calculateStrategy(null, advance = false)
            val s = core.calculator.calculateStrategy(null, advance = false, alignGrid = true)
            assertEquals("d=$d", 0.0, plain.gridOffsetMeters, 0.0)
            assertEquals("d=$d", plain.windowStartMeters, s.windowStartMeters, 1e-6)
            val gridStart = s.windowStartMeters - s.gridOffsetMeters
            val steps = gridStart / s.subBlockSizeMeters
            assertEquals("d=$d", 0.0, steps - Math.rint(steps), 1e-6)
            assertEquals("d=$d", true, s.gridOffsetMeters >= 0.0 && s.gridOffsetMeters < s.majorBlockSizeMeters + 1e-6)
            // La rejilla alineada incluye bloques extra a la izquierda: la baliza sigue en su sitio relativo a la ventana
            assertEquals("d=$d", plain.riderProgress.toDouble(), s.riderProgress.toDouble(), 1e-6)
        }
    }

    @Test
    fun ghostGapNullWithoutKGhost() {
        assertNull(AltgraphCore().tick(null).ghostGap)
    }

    @Test
    fun ghostGapFromParts() {
        val core = AltgraphCore()
        core.kghostTime = GapPart(-8.0, false)
        core.kghostDist = GapPart(-30.0, false)
        assertEquals(GhostGap(-8.0, -30.0, false), core.tick(null).ghostGap)
    }

    @Test
    fun clearedPartReachesSnapshot() {
        val core = AltgraphCore()
        core.kghostTime = GapPart(-8.0, false)
        core.kghostDist = GapPart(-30.0, false)
        assertEquals(GhostGap(-8.0, -30.0, false), core.tick(null).ghostGap)
        core.kghostTime = null
        core.kghostDist = null
        assertNull(core.tick(null).ghostGap)
    }
}
