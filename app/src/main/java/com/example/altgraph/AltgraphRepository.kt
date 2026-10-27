package com.example.altgraph

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.OnLocationChanged
import io.hammerhead.karooext.models.OnMapZoomLevel
import io.hammerhead.karooext.models.OnNavigationState
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Consumers de una instancia de KarooSystemService: se dan de alta una sola vez (el SDK los
 * vuelve a registrar él mismo en cada reconexión) y se quitan todos en stop().
 */
internal class ConsumerRegistry(
    private val add: (String) -> String,
    private val remove: (String) -> Unit
) {
    // kind -> id del consumer vigente
    private val ids = LinkedHashMap<String, String>()

    fun register(kinds: List<String>) {
        if (ids.isNotEmpty()) return
        kinds.forEach { ids[it] = add(it) }
    }

    // Sustituye el consumer de un kind que el host terminó
    fun reAdd(kind: String) {
        // Quitar el previo (no-op si el SDK ya lo quitó) evita dejar un consumer vivo huérfano
        ids[kind]?.let(remove)
        ids[kind] = add(kind)
    }

    fun removeAll() {
        // Un remove que falla (p. ej. DeadObjectException del binder) no debe abortar stop(): se sigue con el resto
        ids.values.forEach { id -> try { remove(id) } catch (e: Exception) { /* sin Log aquí; se ignora por elemento */ } }
        ids.clear()
    }
}

/** Espera antes de re-suscribir un stream terminado: 2 s, se duplica y se limita a 60 s. */
internal fun nextBackoffMs(previousMs: Long?): Long =
    if (previousMs == null) 2000L else minOf(previousMs * 2, 60000L)

/** Tras un terminal el backoff vuelve a 2 s solo si el stream estuvo vivo >= 60 s desde el re-alta. */
internal fun backoffAfterTerminal(previousMs: Long?, upSinceReAddMs: Long?): Long =
    if (upSinceReAddMs != null && upSinceReAddMs >= 60_000L) 2000L else nextBackoffMs(previousMs)

/** Solo se re-suscribe si el repositorio sigue en la misma vida (start/stop no ocurrió entre medias). */
internal fun shouldResubscribe(lostAtGeneration: Int, currentGeneration: Int, running: Boolean): Boolean =
    running && lostAtGeneration == currentGeneration

/**
 * Única conexión con Karoo y único hilo del calculador compartido.
 * Todo acceso a `core` ocurre en el hilo "altgraph-repo".
 */
object AltgraphRepository {

    private const val TAG = "AltgraphRepository"
    private const val KIND_NAV = "nav"
    private const val KIND_LOC = "loc"
    private const val KIND_ZOOM = "zoom"

    private val core = AltgraphCore()
    private val gate = TickGate()
    private val navListeners = CopyOnWriteArrayList<(OnNavigationState) -> Unit>()
    private val _snapshot = MutableStateFlow<Snapshot?>(null)
    val snapshot: StateFlow<Snapshot?> = _snapshot

    // Un solo hilo para toda la vida del proceso y nunca se cierra: tras stop()+start() el trabajo
    // que siguiera en curso (tick o evento no suspend) termina antes de que corra nada nuevo sobre core
    private val repoDispatcher: ExecutorCoroutineDispatcher by lazy {
        Executors.newSingleThreadExecutor { Thread(it, "altgraph-repo") }.asCoroutineDispatcher()
    }

    // Se crean en start() y se anulan en stop(); protegidos por el monitor del objeto
    @Volatile private var scope: CoroutineScope? = null
    private var karooSystem: KarooSystemService? = null
    private var consumers: ConsumerRegistry? = null
    private var tickJob: Job? = null
    @Volatile private var appContext: Context? = null
    // Último estado de navegación (solo se escribe en el hilo del repositorio)
    @Volatile private var lastNav: OnNavigationState? = null
    // Vida del repositorio: sube en start() y stop() para descartar re-suscripciones de una vida anterior
    @Volatile private var generation = 0
    // Espera vigente por stream terminado (solo se toca en el hilo del repositorio)
    private val backoff = mutableMapOf<String, Long>()
    // Kinds ya anunciados como recuperados desde su último terminal (solo hilo del repositorio)
    private val recovered = mutableSetOf<String>()
    // Instante del último re-alta correcto por kind: lo escribe resubscribe (monitor) y lo lee el hilo del repositorio
    private val lastReAddAt = ConcurrentHashMap<String, Long>()

    @Synchronized
    fun start(context: Context) {
        if (scope != null) return
        Log.i(TAG, "AltgraphRepository start")
        val ctx = context.applicationContext ?: context
        appContext = ctx
        val s = CoroutineScope(SupervisorJob() + repoDispatcher + CoroutineExceptionHandler { _, e -> Log.e(TAG, "repo", e) })
        scope = s
        // Primer trabajo del hilo: el backoff y el hueco KGhost de una vida anterior no se arrastran tras stop()/start()
        // (core vive todo el proceso; si KGhost ya no está, el hueco viejo quedaría congelado)
        // También se anula el snapshot: corre tras cualquier tick en curso de la vida anterior, que podría haberlo republicado tras stop()
        s.launch { backoff.clear(); recovered.clear(); lastReAddAt.clear(); core.kghostTime = null; core.kghostDist = null; _snapshot.value = null }
        val gen = ++generation

        val system = KarooSystemService(ctx)
        karooSystem = system
        val registry = ConsumerRegistry(
            add = { kind -> addConsumer(system, s, kind, gen) },
            remove = { id -> system.removeConsumer(id) }
        )
        val kinds = listOf(KIND_NAV, KIND_LOC, KIND_ZOOM) + NavigationSync.STREAM_TYPES + KGHOST_GAP_TIME + KGHOST_GAP_DIST
        // Alta única antes de conectar: el SDK registra en cada (re)conexión todos los consumers
        // que tiene. No se tocan desde el callback de connect, que corre dentro de ese recorrido.
        registry.register(kinds)
        consumers = registry
        Log.i(TAG, "consumers registrados: ${kinds.size}")
        // Sin conexión no hay dato KGhost; al reconectar el SDK re-registra y KGhost reenvía su estado.
        // El callback corre dentro del recorrido de listeners del SDK: solo encola, no toca consumers.
        system.connect { connected ->
            if (!connected) s.launch {
                setKGhostPart(KGHOST_GAP_TIME, null)
                setKGhostPart(KGHOST_GAP_DIST, null)
            }
        }

        // Holders que siguieran activos de antes de un stop()
        if (gate.isActive) tickJob = launchTick(s)
    }

    // Nota: el ejecutor no se cierra; cancelar el scope basta y un start() rápido reutiliza el mismo hilo
    @Synchronized
    fun stop() {
        val s = scope ?: return
        consumers?.removeAll()
        consumers = null
        // disconnect() puede lanzar DeadObjectException si el binder del host ya murió; el resto del stop() debe ejecutarse siempre
        try {
            karooSystem?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "disconnect failed (host binder dead?)", e)
        }
        karooSystem = null
        tickJob = null
        s.cancel()
        scope = null
        generation++
        // Tras un stop() el core queda obsoleto y Karoo reenvía la navegación al reconectar
        lastNav = null
        // Un snapshot de la vida anterior no debe seguir pintándose
        _snapshot.value = null
    }

    // hold/release llegan desde hilos Binder: el monitor ordena launch/cancel igual que el gate
    @Synchronized
    fun hold(token: Any) {
        if (gate.add(token)) tickJob = scope?.let { launchTick(it) }
    }

    @Synchronized
    fun release(token: Any) {
        if (gate.remove(token)) {
            tickJob?.cancel()
            tickJob = null
        }
    }

    suspend fun strategyForRouteBar(viewWidth: Int, base: Snapshot): StrategyData =
        withContext(dispatcher()) { core.strategyForRouteBar(appContext, viewWidth, base) }

    suspend fun strategyFor3D(base: Snapshot): StrategyData =
        withContext(dispatcher()) { core.strategyFor3D(appContext, base) }

    suspend fun climbStrategy(climb: RouteClimb, start: Double?, length: Double?): StrategyData =
        withContext(dispatcher()) { core.climbStrategy(climb, start, length) }
        
    suspend fun getFullProfileElevations(): FloatArray =
        withContext(dispatcher()) { core.getFullProfileElevations() }

    suspend fun getFullRouteCoords(): List<Pair<Double, Double>> =
        withContext(dispatcher()) { core.getFullRouteCoords() }

    fun pan3d(deltaMeters: Double) {
        scope?.launch { core.pan3dMeters += deltaMeters }
    }

    // Alta y reenvío en el mismo bloque del hilo del repositorio: un evento ya encolado no llega dos veces
    fun addNavListener(l: (OnNavigationState) -> Unit) {
        val s = scope ?: run { navListeners.add(l); return }
        s.launch {
            navListeners.add(l)
            // Karoo entregó el estado de navegación al conectar, antes de que existiera esta vista: se reenvía el último
            lastNav?.let { deliverNav(l, it) }
        }
    }

    // Baja inmediata (la entrega comprueba pertenencia) y otra en el hilo por si el alta seguía encolada
    fun removeNavListener(l: (OnNavigationState) -> Unit) {
        navListeners.remove(l)
        scope?.launch { navListeners.remove(l) }
    }

    private fun deliverNav(l: (OnNavigationState) -> Unit, event: OnNavigationState) {
        if (l !in navListeners) return
        try {
            l(event)
        } catch (e: Exception) {
            Log.e(TAG, "navListener", e)
        }
    }

    private fun dispatcher(): ExecutorCoroutineDispatcher =
        if (scope != null) repoDispatcher else throw CancellationException("AltgraphRepository detenido")

    private val climbAlerts = ClimbAlertTracker()

    private fun showClimbAlert(ctx: Context, alert: ClimbAlertTracker.Alert) {
        val system = karooSystem ?: return
        if (!system.connected) return
        val c = alert.climb
        val name = c.name?.takeIf { it.isNotBlank() }
            ?: "${ctx.getString(R.string.profile_tv_climb_item_prefix)} ${alert.index + 1}"
        val parts = mutableListOf(
            String.format(java.util.Locale.getDefault(), "%.1f km", c.length / 1000.0),
            String.format(java.util.Locale.getDefault(), "%.1f%%", c.avgGrade)
        )
        if (c.category.isNotBlank()) parts.add(c.category.trim())
        if (c.totalElevation > 0.0) parts.add("+${Math.round(c.totalElevation)} m")
        val sent = system.dispatch(
            InRideAlert(
                id = "altgraph-climb-${Math.round(c.startDistance)}",
                icon = R.drawable.ic_climb_alert,
                title = name,
                detail = parts.joinToString(" · "),
                autoDismissMs = 8000L,
                backgroundColor = R.color.climb_alert_bg,
                textColor = R.color.white
            )
        )
        Log.i(TAG, "Climb alert '$name' dispatched=$sent")
    }

    private fun launchTick(s: CoroutineScope): Job = s.launch {
        while (isActive) {
            // Un tick que falla se registra y el bucle sigue (tick no es suspend: no traga cancelaciones)
            try {
                val ctx = appContext
                val prefs = if (ctx != null) AppPreferences.getInstance(ctx) else null
                val ftp = prefs?.userFtp ?: 250
                val wPrime = prefs?.userWPrime ?: 20000
                val snap = core.tick(ctx, ftp, wPrime)
                _snapshot.value = snap

                // Aviso de puerto en el ride (no es Elite: sustituye al Climber de Hammerhead si el usuario lo desactiva)
                if (ctx != null && prefs != null) {
                    if (prefs.climbAlertEnabled && snap.isNavigatingRoute) {
                        climbAlerts.next(snap.currentRouteDistance, snap.routeClimbs)?.let { showClimbAlert(ctx, it) }
                    } else if (!snap.isNavigatingRoute) {
                        climbAlerts.reset()
                    }
                }

                // Voice Copilot (Elite)
                if (ctx != null && prefs != null && prefs.voiceDirectorEnabled && prefs.isEliteUnlocked) {
                    VoiceDirector.getInstance(ctx).evaluate(snap, prefs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "tick", e)
            }
            delay(1000)
        }
    }

    // El host terminó el stream (el SDK ya quitó el consumer): se espera con backoff y se vuelve a dar de alta
    private suspend fun onStreamTerminal(kind: String, reason: String, gen: Int) {
        // Sin stream no hay dato: la parte de KGhost se limpia hasta que vuelva a llegar
        if (isKGhost(kind)) setKGhostPart(kind, null)
        // remove consume el instante: si el re-alta falla y vuelve aquí, no cuenta como stream vivo y el backoff sigue creciendo
        val upMs = lastReAddAt.remove(kind)?.let { SystemClock.elapsedRealtime() - it }
        val d = backoffAfterTerminal(backoff[kind], upMs)
        backoff[kind] = d
        recovered.remove(kind)
        Log.w(TAG, "stream $kind terminado ($reason), re-suscribiendo en $d ms")
        delay(d)
        resubscribe(kind, gen)
    }

    private fun isKGhost(kind: String) = kind == KGHOST_GAP_TIME || kind == KGHOST_GAP_DIST

    // Solo en el hilo del repositorio; registra únicamente las transiciones null <-> dato
    private fun setKGhostPart(kind: String, part: GapPart?) {
        val old = if (kind == KGHOST_GAP_TIME) core.kghostTime else core.kghostDist
        if (kind == KGHOST_GAP_TIME) core.kghostTime = part else core.kghostDist = part
        if (old == null && part != null) Log.i(TAG, "KGhost gap received: $kind")
        else if (old != null && part == null) Log.i(TAG, "KGhost gap cleared: $kind")
    }

    // Mismo monitor que start/stop: un re-alta no se cruza con un stop()
    @Synchronized
    private fun resubscribe(kind: String, gen: Int) {
        if (!shouldResubscribe(gen, generation, scope != null)) return
        Log.i(TAG, "stream $kind re-suscrito")
        try {
            consumers?.reAdd(kind)
            lastReAddAt[kind] = SystemClock.elapsedRealtime()
        } catch (e: Exception) {
            Log.w(TAG, "stream $kind: re-alta fallida", e)
            // El SDK mete el listener en su mapa antes del AIDL addEventConsumer, que puede lanzar si el host acaba de morir.
            // Sin conexión no se reintenta: al reconectar el SDK re-registra todo su mapa (incluido el huérfano) y
            // stop() -> disconnect() quita todos los listeners del SDK, así que nada sobrevive a la vida.
            if (karooSystem?.connected == true) scope?.launch { onStreamTerminal(kind, "re-add failed: ${e.message}", gen) }
            else Log.w(TAG, "re-add of $kind failed while disconnected; the SDK will register it on reconnect")
        }
    }

    // Los callbacks corren en hilos Binder: solo encolan en el hilo del repositorio y vuelven
    private fun addConsumer(system: KarooSystemService, s: CoroutineScope, kind: String, gen: Int): String = when (kind) {
        KIND_NAV -> system.addConsumer<OnNavigationState> { event ->
            s.launch {
                NavigationSync.applyNavigation(core.calculator, event.state)
                lastNav = event
                navListeners.forEach { l -> deliverNav(l, event) }
            }
        }
        KIND_LOC -> system.addConsumer<OnLocationChanged> { event ->
            s.launch { core.calculator.updateCurrentLocation(event.lat, event.lng) }
        }
        KIND_ZOOM -> system.addConsumer<OnMapZoomLevel> { event ->
            s.launch { core.mapZoomLevel = event.zoomLevel }
        }
        else -> system.addConsumer<OnStreamState>(
            OnStreamState.StartStreaming(kind),
            onError = { r -> s.launch { onStreamTerminal(kind, "error: $r", gen) } },
            onComplete = { s.launch { onStreamTerminal(kind, "complete", gen) } }
        ) { event ->
            val st = event.state
            if (isKGhost(kind)) {
                // Cualquier estado cuenta (Searching, Idle...: sin dato la parte se limpia); no pasa por NavigationSync
                val part = kghostPart(st)
                s.launch {
                    if (st is StreamState.Streaming && kind in backoff && recovered.add(kind)) Log.i(TAG, "stream $kind recovered")
                    setKGhostPart(kind, part)
                }
            } else if (st is StreamState.Streaming) {
                val values = st.dataPoint.values
                s.launch {
                    if (kind in backoff && recovered.add(kind)) Log.i(TAG, "stream $kind recovered")
                    NavigationSync.applyStream(core.calculator, kind, values)
                    // Mismo valor y fallback que leían ClimbPacing y GradientTrend con su propio consumer
                    if (kind == DataType.Type.ELEVATION_GRADE)
                        core.rawGrade = values[DataType.Field.ELEVATION_GRADE] ?: values[DataType.Field.SINGLE] ?: 0.0
                }
            }
        }
    }
}
