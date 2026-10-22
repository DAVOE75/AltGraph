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
import android.media.ToneGenerator
import java.util.ArrayDeque
import java.util.Locale

/**
 * Motor de locución por voz (Voice Copilot) para AltGraph.
 *
 * Se ejecuta en segundo plano como singleton. Gestiona la inicialización de TextToSpeech (TTS),
 * tono de atención pre-chime para despertar auriculares Bluetooth,
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
    private var toneGenerator: ToneGenerator? = null

    // Control de audio focus según versión de Android
    private var audioFocusRequest: AudioFocusRequest? = null

    // Cola de locuciones pendientes
    private val speechQueue = ArrayDeque<String>()
    private var isSpeaking = false

    // Control anti-spam: marcas de tiempo y distancias para no saturar
    private var lastSpokenTimeMs = 0L
    private val minSilenceBetweenCuesMs = 4500L // 4.5 segundos entre frases para no perder avisos clave
    private var lastAnnouncedClimbIndex = -1
    private var lastSummit500AnnouncedClimbIndex = -1
    private var lastSummit100AnnouncedClimbIndex = -1
    private var lastAnnouncedKmInClimb = -1
    private var lastAnnouncedSteepDistance = -10000.0
    private var lastAnnouncedReliefDistance = -10000.0
    private var lastAnnouncedWallDistance = -10000.0
    private var lastAnnouncedHairpinDistance = -10000.0
    private var lastAnnouncedKGhostDistance = 0.0
    private var lastKGhostWasLeading: Boolean? = null
    private var lastAnnouncedElevationMilestone = 0
    private var lastPeriodicDistanceKm = 0.0
    private var lastCadenceWarningTimeMs = 0L
    private var lastWbalWarningPercent = 100
    private val announcedPoiNames = mutableSetOf<String>()

    // Estimador de desnivel positivo acumulado cuando no hay sensor dedicado
    private var lastRecordedElevation = 0.0
    private var estimatedElevationGain = 0.0

    init {
        initTts()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed", e)
        }
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
            val prefs = AppPreferences.getInstance(context)
            engine.setSpeechRate(prefs.voiceSpeechRate) // Cadencia de voz configurada (0.95x para máxima claridad)

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

        val prefs = AppPreferences.getInstance(context)
        tts?.setSpeechRate(prefs.voiceSpeechRate)

        requestAudioFocus()
        lastSpokenTimeMs = System.currentTimeMillis()
        isSpeaking = true

        // Si pre-chime está activado, emitimos un bip sutil para despertar el auricular Bluetooth
        if (prefs.voicePreChimeEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                Log.w(TAG, "Error playing pre-chime", e)
            }
        }

        val utteranceId = "$UTTERANCE_PREFIX${System.currentTimeMillis()}"
        val delayBeforeSpeak = if (prefs.voicePreChimeEnabled) 200L else 0L

        mainHandler.postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(next, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(next, TextToSpeech.QUEUE_FLUSH, hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId))
            }
        }, delayBeforeSpeak)
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
        val elev = snapshot.currentElevation

        // Acumular ganancia de elevación estimada
        if (lastRecordedElevation > 0.0 && elev > lastRecordedElevation && snapshot.currentSpeed > 0.5) {
            val delta = elev - lastRecordedElevation
            if (delta in 0.2..15.0) { // Descartar saltos o ruido
                estimatedElevationGain += delta
            }
        }
        if (elev > 0.0) lastRecordedElevation = elev

        // 1. AVISO DE PUERTO (Inicio, Kilómetros y Cima)
        if (prefs.voiceClimbsEnabled && climbs.isNotEmpty()) {
            for ((idx, climb) in climbs.withIndex()) {
                val distToClimb = climb.startDistance - currentDist
                if (distToClimb in 50.0..250.0 && lastAnnouncedClimbIndex != idx) {
                    lastAnnouncedClimbIndex = idx
                    lastAnnouncedKmInClimb = 0
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

                // Progresión kilómetro a kilómetro dentro del puerto (ej: Km 1, Km 2...)
                if (currentDist >= climb.startDistance && currentDist <= climb.endDistance) {
                    val distInsideClimb = currentDist - climb.startDistance
                    val kmInside = (distInsideClimb / 1000.0).toInt()
                    if (kmInside >= 1 && kmInside != lastAnnouncedKmInClimb && (distInsideClimb % 1000.0) < 180.0) {
                        lastAnnouncedKmInClimb = kmInside
                        val currentGradeStr = String.format(Locale.getDefault(), "%.0f", strategy.visibleAvgGrade.coerceAtLeast(0.0))
                        val phrase = context.getString(R.string.voice_climb_km_progress, kmInside.toString(), currentGradeStr)
                        speak(phrase, priority = false)
                    }
                }
            }
        }

        // 2. ENDURECIMIENTO DE PENDIENTE (Próximo tramo supera el umbral configurado)
        val subBlocksList: List<Float> = strategy.subBlocks
        if (prefs.voiceSteepEnabled && subBlocksList.isNotEmpty()) {
            val threshold = prefs.voiceSteepThreshold
            val subBlockSize = strategy.subBlockSizeMeters.coerceAtLeast(20.0)
            
            // Miramos los próximos 300m a 500m
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
                } else if (strategy.visibleAvgGrade >= 8.0 && avgUpcoming < 3.5 && (currentDist - lastAnnouncedReliefDistance) > 2000.0) {
                    // Descansillo / Alivio de pendiente en subida
                    lastAnnouncedReliefDistance = currentDist
                    val meters = (lookAheadCount * subBlockSize).toInt()
                    val phrase = context.getString(R.string.voice_slope_relief, meters.toString())
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

        // 5. HITOS DE DESNIVEL ACUMULADO (Cada 250m o 500m)
        if (prefs.voiceElevationMilestonesEnabled) {
            val milestoneStep = 500
            val currentMilestone = (estimatedElevationGain / milestoneStep).toInt() * milestoneStep
            if (currentMilestone >= milestoneStep && currentMilestone > lastAnnouncedElevationMilestone) {
                lastAnnouncedElevationMilestone = currentMilestone
                val currentAltStr = elev.toInt().toString()
                val phrase = context.getString(R.string.voice_elevation_milestone, currentMilestone.toString(), currentAltStr)
                speak(phrase, priority = false)
            }
        }

        // 6. RESUMEN PERIÓDICO DE RUTA (Cada X km)
        if (prefs.voicePeriodicReportEnabled) {
            val intervalKm = prefs.voicePeriodicIntervalKm.coerceAtLeast(2.0)
            val currentKm = currentDist / 1000.0
            if (currentKm >= intervalKm && (currentKm - lastPeriodicDistanceKm) >= intervalKm) {
                lastPeriodicDistanceKm = currentKm
                val distStr = String.format(Locale.getDefault(), "%.0f", currentKm)
                val avgSpeedKmh = (snapshot.currentSpeed * 3.6).coerceAtLeast(0.0)
                val speedStr = String.format(Locale.getDefault(), "%.1f", avgSpeedKmh)
                val elevGainStr = estimatedElevationGain.toInt().toString()
                val phrase = context.getString(R.string.voice_periodic_summary, distStr, speedStr, elevGainStr)
                speak(phrase, priority = false)
            }
        }

        // 7. ALERTA DE CADENCIA BAJA EN SUBIDAS (< 60 rpm en rampas > 8%)
        if (prefs.voiceCadenceAlertEnabled) {
            val now = System.currentTimeMillis()
            val cad = strategy.currentCadence
            val grade = strategy.visibleAvgGrade
            if (cad in 20.0..58.0 && grade >= 8.0 && snapshot.currentSpeed > 1.5) {
                if ((now - lastCadenceWarningTimeMs) > 90000L) { // Máximo una vez cada 90s
                    lastCadenceWarningTimeMs = now
                    val phrase = context.getString(R.string.voice_cadence_low, cad.toInt().toString())
                    speak(phrase, priority = false)
                }
            }
        }

        // 8. BATERÍA ANAERÓBICA W'BAL (Alerta crítica cuando cae por debajo de 20%)
        val wbal = snapshot.wbal
        if (wbal != null) {
            val userWPrime = prefs.userWPrime.toDouble().coerceAtLeast(5000.0)
            val pct = ((wbal / userWPrime) * 100.0).toInt().coerceIn(0, 100)
            if (pct <= 20 && lastWbalWarningPercent > 20) {
                lastWbalWarningPercent = pct
                val phrase = context.getString(R.string.voice_wbal_warning, pct.toString())
                speak(phrase, priority = true)
            } else if (pct > 50) {
                lastWbalWarningPercent = pct
            }
        }

        // 9. GAP CON RIVAL KGHOST
        if (prefs.voiceKGhostEnabled) {
            val ghost = snapshot.ghostGap
            val distM = ghost?.distM
            val timeS = ghost?.timeS
            if (ghost != null && distM != null && timeS != null) {
                val meters = Math.abs(distM).toInt()
                val isLeading = distM > 0.0 // En KGhost distancia positiva = ciclista real por delante

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

        // 10. PUNTOS DE AGUA (POIs)
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
            toneGenerator?.release()
            toneGenerator = null
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
