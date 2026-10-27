package com.example.altgraph

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import kotlinx.coroutines.cancel

class AltimetriaExtensionService : KarooExtension("altgraph", "2.9.7") {

    override val types: List<DataTypeImpl> by lazy {
        listOf(
            ClimbViewerDataType("altgraph"),
            RouteBarDataType("altgraph"),
            AltimetriaGraphDataType("altgraph"),
            Altimetria3DGraphDataType("altgraph"),
            FatigueGradeDataField("altgraph"),
            ClimbPacingDataField("altgraph"),
            GradientTrendDataField("altgraph"),
            GradeZonesDataField("altgraph"),
            RouteProfile2DDataField("altgraph")
        )
    }

    private var mapOverlayManager: MapOverlayManager? = null
    private var karooSystem: KarooSystemService? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        FontHelper.init(applicationContext)
        AltgraphRepository.start(applicationContext)
        karooSystem = KarooSystemService(applicationContext)
        karooSystem?.connect { }
    }

    override fun startMap(emitter: io.hammerhead.karooext.internal.Emitter<io.hammerhead.karooext.models.MapEffect>) {
        val sys = karooSystem ?: return
        // Cada startMap creaba un manager y un consumer nuevos sin quitar los anteriores
        mapOverlayManager?.stop()
        val manager = MapOverlayManager(applicationContext, scope, sys)
        mapOverlayManager = manager
        manager.start(emitter)

        emitter.setCancellable {
            manager.stop()
            if (mapOverlayManager === manager) mapOverlayManager = null
        }
    }

    override fun onDestroy() {
        mapOverlayManager?.stop()
        karooSystem?.disconnect()
        scope.cancel()
        AltgraphRepository.stop()
        super.onDestroy()
    }
}