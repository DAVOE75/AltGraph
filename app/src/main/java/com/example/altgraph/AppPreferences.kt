package com.example.altgraph

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Copia de todos los ajustes, para detectar si algo cambió entre frames. */
    val snapshot: Map<String, *> get() = prefs.all

    var fontFamilyKey: String
        get() = prefs.getString(KEY_FONT_FAMILY, "sans-serif-condensed") ?: "sans-serif-condensed"
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value).apply()

    var blockSizeMeters: Double
        get() = prefs.getFloat(KEY_BLOCK_SIZE, 100.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_BLOCK_SIZE, value.toFloat()).apply()

    var thresholdAttackPct: Double
        get() = prefs.getFloat(KEY_THRESHOLD_ATTACK, 10.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_THRESHOLD_ATTACK, value.toFloat()).apply()

    var targetVam: Int
        get() = prefs.getInt(KEY_TARGET_VAM, 900)
        set(value) = prefs.edit().putInt(KEY_TARGET_VAM, value).apply()

    var attackAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_ATTACK_ALERT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ATTACK_ALERT_ENABLED, value).apply()

    var asphaltFactor: Double
        get() = prefs.getFloat(KEY_ASPHALT_FACTOR, 0.5f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_ASPHALT_FACTOR, value.toFloat()).apply()

    var fontSize3dScale: Float
        get() = prefs.getFloat(KEY_FONT_SIZE_3D_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE_3D_SCALE, value).apply()

    var showBlockPercentages: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BLOCK_PERCENTAGES, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BLOCK_PERCENTAGES, value).apply()

    var lookaheadMeters3d: Int
        get() = prefs.getInt(KEY_LOOKAHEAD_METERS_3D, 350)
        set(value) = prefs.edit().putInt(KEY_LOOKAHEAD_METERS_3D, value).apply()

    var showGearBadge: Boolean
        get() = prefs.getBoolean(KEY_SHOW_GEAR_BADGE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_GEAR_BADGE, value).apply()

    var show3dCotas: Boolean
        get() = prefs.getBoolean(KEY_SHOW_3D_COTAS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_3D_COTAS, value).apply()

    var show3dRamps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_3D_RAMPS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_3D_RAMPS, value).apply()

    var showMaxGradient: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MAX_GRADIENT, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MAX_GRADIENT, value).apply()

    var show3dHeaderStats: Boolean
        get() = prefs.getBoolean(KEY_SHOW_3D_HEADER_STATS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_3D_HEADER_STATS, value).apply()

    var showRouteCompass: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ROUTE_COMPASS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ROUTE_COMPASS, value).apply()

    var rotate90Clockwise: Boolean
        get() = prefs.getBoolean(KEY_ROTATE_90_CW, false)
        set(value) = prefs.edit().putBoolean(KEY_ROTATE_90_CW, value).apply()

    var visibleBlocksCount: Int
        get() = prefs.getInt(KEY_VISIBLE_BLOCKS_COUNT, 5)
        set(value) = prefs.edit().putInt(KEY_VISIBLE_BLOCKS_COUNT, value.coerceIn(1, 10)).apply()

    var rampMinSlopePct: Double
        get() = prefs.getFloat(KEY_RAMP_MIN_SLOPE, 10.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_RAMP_MIN_SLOPE, value.toFloat()).apply()

    var rampMaxSlopePct: Double
        get() = prefs.getFloat(KEY_RAMP_MAX_SLOPE, 15.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_RAMP_MAX_SLOPE, value.toFloat()).apply()

    var useTopographicCalculation: Boolean
        get() = prefs.getBoolean(KEY_USE_TOPOGRAPHIC_CALCULATION, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_TOPOGRAPHIC_CALCULATION, value).apply()

    var showHairpins: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HAIRPINS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_HAIRPINS, value).apply()

    var gearChainrings: String
        get() = prefs.getString(KEY_GEAR_CHAINRINGS, GearAdvisor.DEFAULT_CHAINRINGS) ?: GearAdvisor.DEFAULT_CHAINRINGS
        set(value) = prefs.edit().putString(KEY_GEAR_CHAINRINGS, value).apply()

    var gearCassette: String
        get() = prefs.getString(KEY_GEAR_CASSETTE, GearAdvisor.DEFAULT_CASSETTE) ?: GearAdvisor.DEFAULT_CASSETTE
        set(value) = prefs.edit().putString(KEY_GEAR_CASSETTE, value).apply()

    var beaconColor: Int
        get() = prefs.getInt(KEY_BEACON_COLOR, 0xFF38BDF8.toInt())
        set(value) = prefs.edit().putInt(KEY_BEACON_COLOR, value).apply()

    var beaconSize: String
        get() = prefs.getString(KEY_BEACON_SIZE, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_BEACON_SIZE, value).apply()

    var beaconShape: String
        get() = prefs.getString(KEY_BEACON_SHAPE, "circle") ?: "circle"
        set(value) = prefs.edit().putString(KEY_BEACON_SHAPE, value).apply()

    var showPoiTowns: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POI_TOWNS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POI_TOWNS, value).apply()

    var showPoiWater: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POI_WATER, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POI_WATER, value).apply()

    var showPoiViewpoints: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POI_VIEWPOINTS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POI_VIEWPOINTS, value).apply()

    var showPoiSummits: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POI_SUMMITS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POI_SUMMITS, value).apply()

    var show3dZoomControls: Boolean
        get() = prefs.getBoolean(KEY_SHOW_3D_ZOOM, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_3D_ZOOM, value).apply()

    var panButtonsPosition: String
        get() = prefs.getString(KEY_PAN_BUTTONS_POSITION, "bottom") ?: "bottom"
        set(value) = prefs.edit().putString(KEY_PAN_BUTTONS_POSITION, value).apply()

    var customMapProvider: String
        get() = prefs.getString(KEY_CUSTOM_MAP_PROVIDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_MAP_PROVIDER, value).apply()

    var altimetriaStyle: AltimetriaStyle
        get() {
            val key = prefs.getString(KEY_ALTIMETRIA_STYLE, AltimetriaStyle.CLASSIC.key) ?: AltimetriaStyle.CLASSIC.key
            return AltimetriaStyle.fromKey(key)
        }
        set(value) = prefs.edit().putString(KEY_ALTIMETRIA_STYLE, value.key).apply()

    var climbAltimetriaStyle: AltimetriaStyle
        get() {
            val key = prefs.getString(KEY_CLIMB_ALTIMETRIA_STYLE, AltimetriaStyle.CLASSIC.key) ?: AltimetriaStyle.CLASSIC.key
            return AltimetriaStyle.fromKey(key)
        }
        set(value) = prefs.edit().putString(KEY_CLIMB_ALTIMETRIA_STYLE, value.key).apply()

    // --- PRO FEATURES (NIVEL SDK MÁXIMO) ---
    var smartZoomEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_ZOOM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_ZOOM_ENABLED, value).apply()

    var oasisTrackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_OASIS_TRACKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OASIS_TRACKING_ENABLED, value).apply()

    var virtualPacerEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIRTUAL_PACER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIRTUAL_PACER_ENABLED, value).apply()

    var switchbackRenderingEnabled: Boolean
        get() = prefs.getBoolean(KEY_SWITCHBACK_RENDERING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SWITCHBACK_RENDERING_ENABLED, value).apply()

    var energyManagementEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENERGY_MANAGEMENT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENERGY_MANAGEMENT_ENABLED, value).apply()

    // --- ELITE FEATURES (PAYWALL) ---
    var licenseKey: String
        get() = prefs.getString(KEY_LICENSE_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LICENSE_KEY, value).apply()

    val isEliteUnlocked: Boolean
        get() = licenseKey == "ALTGRAPH-ELITE-2026"
        
    var weatherOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_OVERLAY_ENABLED, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_WEATHER_OVERLAY_ENABLED, value).apply()

    var stravaSegmentsEnabled: Boolean
        get() = prefs.getBoolean(KEY_STRAVA_SEGMENTS_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_STRAVA_SEGMENTS_ENABLED, value).apply()

    var eliteRadarEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_RADAR_ENABLED, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_RADAR_ENABLED, value).apply()

    // --- ELITE RADAR FEATURES ---
    var eliteGhostEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_GHOST_ENABLED, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_GHOST_ENABLED, value).apply()

    var eliteGhostStyle: String
        get() = prefs.getString(KEY_ELITE_GHOST_STYLE, "classic") ?: "classic"
        set(value) = prefs.edit().putString(KEY_ELITE_GHOST_STYLE, value).apply()

    var eliteEnergyBarEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_ENERGY_BAR_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_ENERGY_BAR_ENABLED, value).apply()

    var elitePoiRulerEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_POI_RULER_ENABLED, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_POI_RULER_ENABLED, value).apply()

    var eliteHistogramEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_HISTOGRAM_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_HISTOGRAM_ENABLED, value).apply()

    var eliteRadar3dEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_RADAR_3D_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_RADAR_3D_ENABLED, value).apply()

    var eliteRadarTheme: String
        get() = prefs.getString(KEY_ELITE_RADAR_THEME, "Estándar") ?: "Estándar"
        set(value) = prefs.edit().putString(KEY_ELITE_RADAR_THEME, value).apply()

    var elitePowerBarEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_POWER_BAR_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_POWER_BAR_ENABLED, value).apply()

    var eliteRadarAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_RADAR_ALERTS_ENABLED, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_RADAR_ALERTS_ENABLED, value).apply()

    var elitePowerGuideEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_POWER_GUIDE_ENABLED, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_POWER_GUIDE_ENABLED, value).apply()

    var eliteSmoothGradientsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_SMOOTH_GRADIENTS_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_SMOOTH_GRADIENTS_ENABLED, value).apply()

    var eliteProfileTv2dRotate: Boolean
        get() = prefs.getBoolean(KEY_ELITE_PROFILE_TV_2D_ROTATE, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_PROFILE_TV_2D_ROTATE, value).apply()

    var eliteProfileTvColor: String
        get() = if (isEliteUnlocked) prefs.getString(KEY_ELITE_PROFILE_TV_COLOR, "green") ?: "green" else "green"
        set(value) = prefs.edit().putString(KEY_ELITE_PROFILE_TV_COLOR, value).apply()

    var eliteHighlightClimbs: Boolean
        get() = prefs.getBoolean(KEY_ELITE_HIGHLIGHT_CLIMBS, true) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_HIGHLIGHT_CLIMBS, value).apply()

    var eliteProfileTvShowClimbNames: Boolean
        get() = prefs.getBoolean(KEY_ELITE_PROFILE_TV_SHOW_CLIMB_NAMES, true)
        set(value) = prefs.edit().putBoolean(KEY_ELITE_PROFILE_TV_SHOW_CLIMB_NAMES, value).apply()
        
    var eliteMapOverlayMode: String
        get() = if (isEliteUnlocked) prefs.getString(KEY_ELITE_MAP_OVERLAY_MODE, "climbs") ?: "climbs" else "off"
        set(value) = prefs.edit().putString(KEY_ELITE_MAP_OVERLAY_MODE, value).apply()

    var eliteSmoothMapOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_SMOOTH_MAP_OVERLAY, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_ELITE_SMOOTH_MAP_OVERLAY, value).apply()

    var eliteCustomClimbsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ELITE_CUSTOM_CLIMBS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ELITE_CUSTOM_CLIMBS_ENABLED, value).apply()

    var userFtp: Int
        get() = prefs.getInt(KEY_USER_FTP, 250)
        set(value) = prefs.edit().putInt(KEY_USER_FTP, value).apply()

    // Marcha según esfuerzo (FTP, peso y FC máxima); apagado vuelve a la tabla por pendiente
    var gearEffortEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEAR_EFFORT, true)
        set(value) = prefs.edit().putBoolean(KEY_GEAR_EFFORT, value).apply()

    var riderWeightKg: Int
        get() = prefs.getInt(KEY_RIDER_WEIGHT, 75)
        set(value) = prefs.edit().putInt(KEY_RIDER_WEIGHT, value.coerceIn(30, 200)).apply()

    var maxHeartRate: Int
        get() = prefs.getInt(KEY_MAX_HR, 185)
        set(value) = prefs.edit().putInt(KEY_MAX_HR, value.coerceIn(100, 230)).apply()

    var userWPrime: Int
        get() = prefs.getInt(KEY_USER_WPRIME, 20000)
        set(value) = prefs.edit().putInt(KEY_USER_WPRIME, value).apply()

    var wbalOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_WBAL_OVERLAY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WBAL_OVERLAY_ENABLED, value).apply()

    var gearWallEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEAR_WALL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GEAR_WALL_ENABLED, value).apply()

    // --- ELITE VOICE COPILOT ALERTS ---
    var voiceDirectorEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_DIRECTOR_ENABLED, false) && isEliteUnlocked
        set(value) = prefs.edit().putBoolean(KEY_VOICE_DIRECTOR_ENABLED, value).apply()

    var voiceClimbsEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_CLIMBS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_CLIMBS_ENABLED, value).apply()

    var voiceSteepEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_STEEP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_STEEP_ENABLED, value).apply()

    var voiceSteepThreshold: Double
        get() = prefs.getFloat(KEY_VOICE_STEEP_THRESHOLD, 8.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_VOICE_STEEP_THRESHOLD, value.toFloat()).apply()

    var voiceGearWallEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_GEAR_WALL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_GEAR_WALL_ENABLED, value).apply()

    var voiceHairpinsEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_HAIRPINS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_HAIRPINS_ENABLED, value).apply()

    var voiceKGhostEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_KGHOST_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_KGHOST_ENABLED, value).apply()

    var voiceKGhostIntervalMeters: Double
        get() = prefs.getFloat(KEY_VOICE_KGHOST_INTERVAL, 1000.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_VOICE_KGHOST_INTERVAL, value.toFloat()).apply()

    var voicePoisEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_POIS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_POIS_ENABLED, value).apply()

    var voiceSurfacesEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_SURFACES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_SURFACES_ENABLED, value).apply()

    var voiceElevationMilestonesEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ELEVATION_MILESTONES, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ELEVATION_MILESTONES, value).apply()

    var voicePeriodicReportEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_PERIODIC_REPORT, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_PERIODIC_REPORT, value).apply()

    var voicePeriodicIntervalKm: Double
        get() = prefs.getFloat(KEY_VOICE_PERIODIC_INTERVAL_KM, 5.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_VOICE_PERIODIC_INTERVAL_KM, value.toFloat()).apply()

    var voiceCadenceAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_CADENCE_ALERT, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_CADENCE_ALERT, value).apply()

    var voicePreChimeEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_PRE_CHIME, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_PRE_CHIME, value).apply()

    var voiceSpeechRate: Float
        get() = prefs.getFloat(KEY_VOICE_SPEECH_RATE, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_SPEECH_RATE, value).apply()

    var eliteRadarResolution: Int
        get() = prefs.getInt(KEY_ELITE_RADAR_RESOLUTION, 0)
        set(value) = prefs.edit().putInt(KEY_ELITE_RADAR_RESOLUTION, value).apply()

    var climbRotate90Clockwise: Boolean
        get() = prefs.getBoolean(KEY_CLIMB_ROTATE_90_CW, false)
        set(value) = prefs.edit().putBoolean(KEY_CLIMB_ROTATE_90_CW, value).apply()

    var showClimbNameInViewer: Boolean
        get() = prefs.getBoolean(KEY_SHOW_CLIMB_NAME_IN_VIEWER, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_CLIMB_NAME_IN_VIEWER, value).apply()

    var climbViewerZoomAtTop: Boolean
        get() = prefs.getBoolean(KEY_CLIMB_VIEWER_ZOOM_AT_TOP, true)
        set(value) = prefs.edit().putBoolean(KEY_CLIMB_VIEWER_ZOOM_AT_TOP, value).apply()
        
    var routeMapHeadingUp: Boolean
        get() = prefs.getBoolean(KEY_ROUTE_MAP_HEADING_UP, false)
        set(value) = prefs.edit().putBoolean(KEY_ROUTE_MAP_HEADING_UP, value).apply()

    var showSurfaceTextures: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SURFACE_TEXTURES, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SURFACE_TEXTURES, value).apply()

    // "anchor": baliza fija al 20% y la gráfica se desplaza. "paged": la baliza se mueve y la gráfica salta de página.
    var beaconMode3d: String
        get() = prefs.getString(KEY_BEACON_MODE_3D, BEACON_MODE_ANCHOR) ?: BEACON_MODE_ANCHOR
        set(value) = prefs.edit().putString(KEY_BEACON_MODE_3D, value).apply()

    var showRouteDistances3d: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ROUTE_DISTANCES_3D, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ROUTE_DISTANCES_3D, value).apply()

    // "auto": fusiona solo cuando los bloques son estrechos; "always"; "off". El ajuste booleano anterior (true) equivale a "always".
    var mergeBlocksMode3d: String
        get() = prefs.getString(KEY_MERGE_MODE_3D, null)
            ?: if (prefs.getBoolean(KEY_MERGE_SIMILAR_BLOCKS_3D, false)) MERGE_ALWAYS else MERGE_AUTO
        set(value) = prefs.edit().putString(KEY_MERGE_MODE_3D, value).apply()

    var hideGradeRangeEnabled3d: Boolean
        get() = prefs.getBoolean(KEY_HIDE_GRADE_RANGE_ENABLED_3D, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_GRADE_RANGE_ENABLED_3D, value).apply()

    var hideGradeMin3d: Float
        get() = prefs.getFloat(KEY_HIDE_GRADE_MIN_3D, -2f)
        set(value) = prefs.edit().putFloat(KEY_HIDE_GRADE_MIN_3D, value).apply()

    var hideGradeMax3d: Float
        get() = prefs.getFloat(KEY_HIDE_GRADE_MAX_3D, 2f)
        set(value) = prefs.edit().putFloat(KEY_HIDE_GRADE_MAX_3D, value).apply()

    // "pill": recuadro negro (actual); "clean": número alargado sin recuadro con el % pequeño como un símbolo de grados
    var pctLabelStyle3d: String
        get() = prefs.getString(KEY_PCT_LABEL_STYLE_3D, PCT_STYLE_PILL) ?: PCT_STYLE_PILL
        set(value) = prefs.edit().putString(KEY_PCT_LABEL_STYLE_3D, value).apply()

    var mergeMarginPct3d: Float
        get() = prefs.getFloat(KEY_MERGE_MARGIN_3D, 0f)
        set(value) = prefs.edit().putFloat(KEY_MERGE_MARGIN_3D, value.coerceIn(0f, 10f)).apply()

    var routeInfoSide3d: String
        get() = prefs.getString(KEY_ROUTE_INFO_SIDE_3D, ROUTE_INFO_RIGHT) ?: ROUTE_INFO_RIGHT
        set(value) = prefs.edit().putString(KEY_ROUTE_INFO_SIDE_3D, value).apply()

    companion object {
        const val ROUTE_INFO_RIGHT = "right"
        const val ROUTE_INFO_LEFT = "left"
        private const val KEY_ROUTE_INFO_SIDE_3D = "route_info_side_3d"
        private const val KEY_MERGE_MARGIN_3D = "merge_margin_pct_3d"
        const val PCT_STYLE_PILL = "pill"
        const val PCT_STYLE_CLEAN = "clean"
        private const val KEY_PCT_LABEL_STYLE_3D = "pct_label_style_3d"
        const val BEACON_MODE_ANCHOR = "anchor"
        const val BEACON_MODE_PAGED = "paged"
        private const val KEY_BEACON_MODE_3D = "beacon_mode_3d"
        private const val KEY_SHOW_ROUTE_DISTANCES_3D = "show_route_distances_3d"
        private const val KEY_MERGE_SIMILAR_BLOCKS_3D = "merge_similar_blocks_3d"
        private const val KEY_MERGE_MODE_3D = "merge_blocks_mode_3d"
        const val MERGE_OFF = "off"
        const val MERGE_AUTO = "auto"
        const val MERGE_ALWAYS = "always"
        private const val KEY_HIDE_GRADE_RANGE_ENABLED_3D = "hide_grade_range_enabled_3d"
        private const val KEY_HIDE_GRADE_MIN_3D = "hide_grade_min_3d"
        private const val KEY_HIDE_GRADE_MAX_3D = "hide_grade_max_3d"
        private const val KEY_GEAR_EFFORT = "gear_effort_enabled"
        private const val KEY_RIDER_WEIGHT = "rider_weight_kg"
        private const val KEY_MAX_HR = "max_heart_rate"
        private const val PREFS_NAME = "altgraph_settings"
        private const val KEY_FONT_FAMILY = "font_family_key"
        private const val KEY_BLOCK_SIZE = "block_size_meters"
        private const val KEY_THRESHOLD_ATTACK = "threshold_attack_pct"
        private const val KEY_TARGET_VAM = "target_vam"
        private const val KEY_ATTACK_ALERT_ENABLED = "attack_alert_enabled"
        private const val KEY_ASPHALT_FACTOR = "asphalt_factor"
        private const val KEY_FONT_SIZE_3D_SCALE = "font_size_3d_scale"
        private const val KEY_SHOW_BLOCK_PERCENTAGES = "show_block_percentages"
        private const val KEY_LOOKAHEAD_METERS_3D = "lookahead_meters_3d"
        private const val KEY_SHOW_GEAR_BADGE = "show_gear_badge"
        private const val KEY_SHOW_3D_COTAS = "show_3d_cotas"
        private const val KEY_SHOW_3D_RAMPS = "show_3d_ramps"
        private const val KEY_SHOW_MAX_GRADIENT = "show_max_gradient"
        private const val KEY_SHOW_3D_HEADER_STATS = "show_3d_header_stats"
        private const val KEY_SHOW_ROUTE_COMPASS = "show_route_compass"
        private const val KEY_ROTATE_90_CW = "rotate_90_clockwise"
        private const val KEY_CLIMB_ROTATE_90_CW = "climb_rotate_90_clockwise"
        private const val KEY_SHOW_CLIMB_NAME_IN_VIEWER = "show_climb_name_in_viewer"
        private const val KEY_CLIMB_VIEWER_ZOOM_AT_TOP = "climb_viewer_zoom_at_top"
        private const val KEY_ROUTE_MAP_HEADING_UP = "route_map_heading_up"
        private const val KEY_VISIBLE_BLOCKS_COUNT = "visible_blocks_count"
        private const val KEY_RAMP_MIN_SLOPE = "ramp_min_slope_pct"
        private const val KEY_RAMP_MAX_SLOPE = "ramp_max_slope_pct"
        private const val KEY_USE_TOPOGRAPHIC_CALCULATION = "use_topographic_calculation"
        private const val KEY_SHOW_HAIRPINS = "show_hairpins"
        private const val KEY_SHOW_POI_TOWNS = "show_poi_towns"
        private const val KEY_SHOW_POI_WATER = "show_poi_water"
        private const val KEY_SHOW_POI_VIEWPOINTS = "show_poi_viewpoints"
        private const val KEY_SHOW_POI_SUMMITS = "show_poi_summits"
        private const val KEY_SHOW_3D_ZOOM = "show_3d_zoom_controls"
        private const val KEY_GEAR_CHAINRINGS = "gear_chainrings"
        private const val KEY_GEAR_CASSETTE = "gear_cassette"
        private const val KEY_BEACON_COLOR = "beacon_color"
        private const val KEY_BEACON_SIZE = "beacon_size"
        private const val KEY_BEACON_SHAPE = "beacon_shape"
        private const val KEY_PAN_BUTTONS_POSITION = "pan_buttons_position"
        private const val KEY_SHOW_SURFACE_TEXTURES = "show_surface_textures"
        private const val KEY_CUSTOM_MAP_PROVIDER = "custom_map_provider_key"
        private const val KEY_ALTIMETRIA_STYLE = "altimetria_style_key"
        private const val KEY_CLIMB_ALTIMETRIA_STYLE = "climb_altimetria_style_key"

        private const val KEY_SMART_ZOOM_ENABLED = "smart_zoom_enabled"
        private const val KEY_OASIS_TRACKING_ENABLED = "oasis_tracking_enabled"
        private const val KEY_VIRTUAL_PACER_ENABLED = "virtual_pacer_enabled"
        private const val KEY_SWITCHBACK_RENDERING_ENABLED = "switchback_rendering_enabled"
        private const val KEY_ENERGY_MANAGEMENT_ENABLED = "energy_management_enabled"

        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_WEATHER_OVERLAY_ENABLED = "weather_overlay_enabled"
        private const val KEY_STRAVA_SEGMENTS_ENABLED = "strava_segments_enabled"
        private const val KEY_ELITE_RADAR_ENABLED = "elite_radar_enabled"
        private const val KEY_ELITE_GHOST_ENABLED = "elite_ghost_enabled"
        private const val KEY_ELITE_GHOST_STYLE = "elite_ghost_style"
        private const val KEY_ELITE_ENERGY_BAR_ENABLED = "elite_energy_bar_enabled"
        private const val KEY_ELITE_POI_RULER_ENABLED = "elite_poi_ruler_enabled"
        private const val KEY_ELITE_HISTOGRAM_ENABLED = "elite_histogram_enabled"
        private const val KEY_ELITE_RADAR_3D_ENABLED = "elite_radar_3d_enabled"
        private const val KEY_ELITE_RADAR_THEME = "elite_radar_theme"
        private const val KEY_ELITE_POWER_BAR_ENABLED = "elite_power_bar_enabled"
        private const val KEY_ELITE_RADAR_ALERTS_ENABLED = "elite_radar_alerts_enabled"
        private const val KEY_ELITE_POWER_GUIDE_ENABLED = "elite_power_guide_enabled"
        private const val KEY_ELITE_SMOOTH_GRADIENTS_ENABLED = "elite_smooth_gradients_enabled"
        private const val KEY_ELITE_PROFILE_TV_2D_ROTATE = "elite_profile_tv_2d_rotate"
        private const val KEY_ELITE_PROFILE_TV_COLOR = "elite_profile_tv_color"
        private const val KEY_ELITE_HIGHLIGHT_CLIMBS = "elite_highlight_climbs"
        private const val KEY_ELITE_PROFILE_TV_SHOW_CLIMB_NAMES = "elite_profile_tv_show_climb_names"
        private const val KEY_ELITE_MAP_OVERLAY_MODE = "elite_map_overlay_mode"
        private const val KEY_ELITE_SMOOTH_MAP_OVERLAY = "elite_smooth_map_overlay"
        private const val KEY_ELITE_CUSTOM_CLIMBS_ENABLED = "elite_custom_climbs_enabled"
        private const val KEY_USER_FTP = "user_ftp"
        private const val KEY_USER_WPRIME = "user_wprime"
        private const val KEY_WBAL_OVERLAY_ENABLED = "wbal_overlay_enabled"
        private const val KEY_GEAR_WALL_ENABLED = "gear_wall_enabled"
        private const val KEY_VOICE_DIRECTOR_ENABLED = "voice_director_enabled"
        private const val KEY_VOICE_CLIMBS_ENABLED = "voice_climbs_enabled"
        private const val KEY_VOICE_STEEP_ENABLED = "voice_steep_enabled"
        private const val KEY_VOICE_STEEP_THRESHOLD = "voice_steep_threshold"
        private const val KEY_VOICE_GEAR_WALL_ENABLED = "voice_gear_wall_enabled"
        private const val KEY_VOICE_HAIRPINS_ENABLED = "voice_hairpins_enabled"
        private const val KEY_VOICE_KGHOST_ENABLED = "voice_kghost_enabled"
        private const val KEY_VOICE_KGHOST_INTERVAL = "voice_kghost_interval"
        private const val KEY_VOICE_POIS_ENABLED = "voice_pois_enabled"
        private const val KEY_VOICE_SURFACES_ENABLED = "voice_surfaces_enabled"
        private const val KEY_VOICE_ELEVATION_MILESTONES = "voice_elevation_milestones"
        private const val KEY_VOICE_PERIODIC_REPORT = "voice_periodic_report"
        private const val KEY_VOICE_PERIODIC_INTERVAL_KM = "voice_periodic_interval_km"
        private const val KEY_VOICE_CADENCE_ALERT = "voice_cadence_alert"
        private const val KEY_VOICE_PRE_CHIME = "voice_pre_chime"
        private const val KEY_VOICE_SPEECH_RATE = "voice_speech_rate"
        private const val KEY_ELITE_RADAR_RESOLUTION = "elite_radar_resolution"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

enum class AltimetriaStyle(val key: String, val icon: String, val titleRes: Int, val descRes: Int) {
    CLASSIC(
        key = "classic",
        icon = "",
        titleRes = R.string.style_classic_title,
        descRes = R.string.style_classic_desc
    ),
    HORIZON_ISOMETRIC(
        key = "horizon_iso",
        icon = "",
        titleRes = R.string.style_horizon_title,
        descRes = R.string.style_horizon_desc
    ),
    TACTICAL_OASES(
        key = "tactical_oases",
        icon = "",
        titleRes = R.string.style_oasis_title,
        descRes = R.string.style_oasis_desc
    ),
    DYNAMIC_FORCE_FIELD(
        key = "force_field",
        icon = "",
        titleRes = R.string.style_force_title,
        descRes = R.string.style_force_desc
    ),
    MONOLITHIC_OBSIDIAN(
        key = "monolithic",
        icon = "",
        titleRes = R.string.style_monolith_title,
        descRes = R.string.style_monolith_desc
    ),
    GLOBAL_ISOMETRIC(
        key = "global_iso",
        icon = "",
        titleRes = R.string.style_global_title,
        descRes = R.string.style_global_desc
    ),
    HILLSHADE_APEX(
        key = "hillshade_apex",
        icon = "",
        titleRes = R.string.style_hillshade_title,
        descRes = R.string.style_hillshade_desc
    );

    companion object {
        fun fromKey(key: String): AltimetriaStyle {
            return entries.find { it.key == key } ?: CLASSIC
        }
    }
}
