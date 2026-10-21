package com.example.altgraph

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.ArrayDeque
import java.util.Locale

/**
 * Motor de locución por voz (Voice Copilot) para AltGraph.
 *
 * Se ejecuta en segundo plano como singleton. Gestiona la inicialización de TextToSpeech (TTS),
 * la petición de AUDIOFOCUS para atenuar suavemente la música mientras habla,
 * y una cola con prioridades y control inteligente de anti-spam para no saturar al ciclista.
 */
class VoiceDirector private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceDirector"
        private const val UTTERANCE_PREFIX = "altgraph_voice_"

        @Volatile
        private var INSTANCE: VoiceDirector? = null

        fun getInstance(context: Context): VoiceDirector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceDirector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Control de audio focus según versión de Android
    private var audioFocusRequest: AudioFocusRequest? = null

    // Cola de locuciones pendientes
    private val speechQueue = ArrayDeque<String>()
    private var isSpeaking = false

    // Control anti-spam: marcas de tiempo por tipo de aviso para no repetir
    private var lastSpokenTimeMs = 0L
    private val minSilenceBetweenCuesMs = 12000L // Mínimo 12 segundos entre frases
    private var lastAnnouncedClimbIndex = -1
    private var lastSummit500AnnouncedClimbIndex = -1
    private var lastSummit100AnnouncedClimbIndex = -1
    private var lastAnnouncedSteepDistance = -10000.0
    private var lastAnnouncedWallDistance = -10000.0
    private var lastAnnouncedHairpinDistance = -10000.0
    private var lastAnnouncedKGhostDistance = 0.0
    private var lastKGhostWasLeading: Boolean? = null
    private val announcedPoiNames = mutableSetOf<String>()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val locale = Locale.getDefault()
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback a inglés si el idioma local no estuviera descargado
                engine.setLanguage(Locale.US)
            }
            engine.setPitch(1.0f)
            engine.setSpeechRate(1.05f) // Levemente ágil para ciclismo

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        isSpeaking = false
                        abandonAudioFocus()
                        processNextInQueue()
                    }
                }

                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        isSpeaking = false
                        abandonAudioFocus()
                        processNextInQueue()
                    }
                }
            })

            isTtsReady = true
            Log.i(TAG, "VoiceDirector TTS initialized successfully for locale: ${locale.language}")
        } else {
            Log.w(TAG, "VoiceDirector TTS initialization failed with status: $status")
            isTtsReady = false
        }
    }

    /**
     * Encola una frase para ser leída si las preferencias lo permiten y se supera el filtro de silencio.
     */
    fun speak(phrase: String, priority: Boolean = false) {
        val prefs = AppPreferences.getInstance(context)
        if (!prefs.isEliteUnlocked || !prefs.voiceDirectorEnabled) return
        if (phrase.isBlank()) return

        val now = System.currentTimeMillis()
        if (!priority && (now - lastSpokenTimeMs) < minSilenceBetweenCuesMs) {
            return
        }

        synchronized(speechQueue) {
            if (priority) {
                speechQueue.addFirst(phrase)
            } else {
                speechQueue.addLast(phrase)
            }
        }

        mainHandler.post {
            processNextInQueue()
        }
    }

    private fun processNextInQueue() {
        if (isSpeaking) return
        val next = synchronized(speechQueue) {
            if (speechQueue.isEmpty()) null else speechQueue.removeFirst()
        } ?: return

        if (!isTtsReady || tts == null) return

        requestAudioFocus()
        lastSpokenTimeMs = System.currentTimeMillis()
        isSpeaking = true

        val utteranceId = "$UTTERANCE_PREFIX${System.currentTimeMillis()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(next, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(next, TextToSpeech.QUEUE_FLUSH, hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId))
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestAudioFocus error", e)
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "abandonAudioFocus error", e)
        }
    }

    /**
     * Evalúa las condiciones de la ruta y genera los avisos pertinentes según los ajustes.
     */
    fun evaluate(snapshot: Snapshot, prefs: AppPreferences) {
        if (!prefs.isEliteUnlocked || !prefs.voiceDirectorEnabled) return
        if (!snapshot.isNavigatingRoute) return

        val strategy = snapshot.strategy
        val currentDist = strategy.windowStartMeters
        val climbs = snapshot.routeClimbs

        // 1. AVISO DE PUERTO (Inicio y Cima)
        if (prefs.voiceClimbsEnabled && climbs.isNotEmpty()) {
            for ((idx, climb) in climbs.withIndex()) {
                val distToClimb = climb.startDistance - currentDist
                if (distToClimb in 50.0..250.0 && lastAnnouncedClimbIndex != idx) {
                    lastAnnouncedClimbIndex = idx
                    val climbName = climb.name ?: (context.getString(R.string.profile_tv_climb_item_prefix) + " " + (idx + 1))
                    val km = String.format(Locale.getDefault(), "%.1f", climb.length / 1000.0)
                    val avgGrade = String.format(Locale.getDefault(), "%.1f", climb.avgGrade)
                    val catStr = if (climb.category.isNotBlank()) climb.category else ""
                    val phrase = context.getString(R.string.voice_climb_start, climbName, km, avgGrade, catStr).trim()
                    speak(phrase, priority = true)
                    break
                }

                // Cima próxima (a 500 metros)
                val distToSummit = climb.endDistance - currentDist
                if (distToSummit in 450.0..550.0 && currentDist >= climb.startDistance && lastSummit500AnnouncedClimbIndex != idx) {
                    lastSummit500AnnouncedClimbIndex = idx
                    val phrase = context.getString(R.string.voice_climb_summit_near, "500")
                    speak(phrase, priority = true)
                    break
                }

                // Cima inminente (a 100 metros)
                if (distToSummit in 70.0..130.0 && currentDist >= climb.startDistance && lastSummit100AnnouncedClimbIndex != idx) {
                    lastSummit100AnnouncedClimbIndex = idx
                    val phrase = context.getString(R.string.voice_climb_summit_near, "100")
                    speak(phrase, priority = true)
                    break
                }
            }
        }

        // 2. ENDURECIMIENTO DE PENDIENTE (Próximo tramo supera el umbral configurado)
        val subBlocksList: List<Float> = strategy.subBlocks
        if (prefs.voiceSteepEnabled && subBlocksList.isNotEmpty()) {
            val threshold = prefs.voiceSteepThreshold
            val subBlockSize = strategy.subBlockSizeMeters.coerceAtLeast(20.0)
            
            // Miramos los próximos 300m a 600m
            val lookAheadCount = (500.0 / subBlockSize).toInt().coerceIn(2, subBlocksList.size)
            val upcomingGrades: List<Float> = subBlocksList.take(lookAheadCount)
            if (upcomingGrades.isNotEmpty()) {
                val sum = upcomingGrades.fold(0.0) { acc, g -> acc + g.toDouble() }
                val avgUpcoming = sum / upcomingGrades.size
                if (avgUpcoming >= threshold && (currentDist - lastAnnouncedSteepDistance) > 1500.0) {
                    lastAnnouncedSteepDistance = currentDist
                    val meters = (lookAheadCount * subBlockSize).toInt()
                    val gradeFormatted = String.format(Locale.getDefault(), "%.1f", avgUpcoming)
                    val phrase = context.getString(R.string.voice_steep_ahead, meters.toString(), gradeFormatted)
                    speak(phrase, priority = false)
                }
            }
        }

        // 3. MURO DE DESARROLLO (Rampas >= 15% o muro extremo)
        val subBlocksMaxList: List<Float> = strategy.subBlocksMax
        if (prefs.voiceGearWallEnabled && subBlocksMaxList.isNotEmpty()) {
            val maxAhead = subBlocksMaxList.take(4).maxByOrNull { it } ?: 0f
            if (maxAhead >= 15.0f && (currentDist - lastAnnouncedWallDistance) > 1200.0) {
                lastAnnouncedWallDistance = currentDist
                val gradeFormatted = String.format(Locale.getDefault(), "%.0f", maxAhead.toDouble())
                val phrase = context.getString(R.string.voice_wall_ahead, gradeFormatted, "150")
                speak(phrase, priority = true)
            }
        }

        // 4. CURVAS DE HERRADURA (Apex)
        val hairpinsList: List<Double> = strategy.hairpins
        if (prefs.voiceHairpinsEnabled && hairpinsList.isNotEmpty()) {
            val nextHairpin = hairpinsList.firstOrNull { h -> h > currentDist && (h - currentDist) in 40.0..120.0 }
            if (nextHairpin != null && (nextHairpin - lastAnnouncedHairpinDistance) > 300.0) {
                lastAnnouncedHairpinDistance = nextHairpin
                val distAhead = (nextHairpin - currentDist).toInt()
                val gradeAtHairpin = String.format(Locale.getDefault(), "%.0f", strategy.visibleAvgGrade.coerceAtLeast(8.0))
                val phrase = context.getString(R.string.voice_hairpin_ahead, distAhead.toString(), gradeAtHairpin)
                speak(phrase, priority = false)
            }
        }

        // 5. GAP CON RIVAL KGHOST
        if (prefs.voiceKGhostEnabled) {
            val ghost = snapshot.ghostGap
            val distM = ghost?.distM
            val timeS = ghost?.timeS
            if (ghost != null && distM != null && timeS != null) {
                val meters = Math.abs(distM).toInt()
                val isLeading = distM < 0.0 // En KGhost distancia negativa = vas por delante

                // Detectar Sorpasso / Cambio de líder
                val previousState = lastKGhostWasLeading
                if (previousState != null && previousState != isLeading && meters > 10) {
                    lastKGhostWasLeading = isLeading
                    if (isLeading) {
                        speak(context.getString(R.string.voice_kghost_overtake_lead), priority = true)
                    } else {
                        speak(context.getString(R.string.voice_kghost_overtaken), priority = true)
                    }
                } else if (previousState == null) {
                    lastKGhostWasLeading = isLeading
                }

                // Avisos periódicos por intervalo de distancia (ej. cada 1000m)
                val interval = prefs.voiceKGhostIntervalMeters.coerceAtLeast(500.0)
                if ((currentDist - lastAnnouncedKGhostDistance) >= interval) {
                    lastAnnouncedKGhostDistance = currentDist
                    val absSeconds = Math.abs(Math.round(timeS))
                    val timeStr = if (absSeconds < 60) "$absSeconds s" else String.format(Locale.getDefault(), "%d:%02d", absSeconds / 60, absSeconds % 60)
                    val phrase = if (isLeading) {
                        context.getString(R.string.voice_kghost_lead, meters.toString(), timeStr)
                    } else {
                        context.getString(R.string.voice_kghost_chase, meters.toString(), timeStr)
                    }
                    speak(phrase, priority = false)
                }
            }
        }

        // 6. PUNTOS DE AGUA (POIs)
        val poisList: List<Poi> = strategy.pois
        if (prefs.voicePoisEnabled && poisList.isNotEmpty()) {
            for (poi in poisList) {
                if (poi.type == PoiType.WATER) {
                    val distToPoi = poi.relativeDistance
                    if (distToPoi in 50.0..150.0 && !announcedPoiNames.contains(poi.name)) {
                        announcedPoiNames.add(poi.name)
                        val phrase = context.getString(R.string.voice_poi_water, distToPoi.toInt().toString())
                        speak(phrase, priority = false)
                        break
                    }
                }
            }
        }
    }

    fun shutdown() {
        abandonAudioFocus()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
        tts = null
        isTtsReady = false
        speechQueue.clear()
        announcedPoiNames.clear()
        INSTANCE = null
    }
}
