package com.example.altgraph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class Altimetria3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var reusableWallPaint: Paint? = null
    private var reusableTopPaint: Paint? = null
    private var reusableKmTextPaint: Paint? = null
    private var reusablePolyPath: Path? = null
    private var reusableArrowPath: Path? = null
    private var reusableLegendPaint: Paint? = null
    private var reusableLegendTextPaint: Paint? = null
    private var reusableLegendTitlePaint: Paint? = null
    private var reusablePinPaint: Paint? = null
    private var reusablePeakTextPaint: Paint? = null
    private var reusableTriPath: Path? = null

    private val bgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    private val cotaLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        strokeWidth = 1.6f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }

    private val cotaTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    // --- PAINTS DE LA CINTA 3D ESTILO LA FLAMME ROUGE ---
    private val ribbonSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ribbonFrontLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2.8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val ribbonBackLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        strokeWidth = 1.4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val sliceSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15FFFFFF")
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
    }

    private val majorSliceSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#25FFFFFF")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val pctBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6000000") // Very dark translucent
        style = Paint.Style.FILL
    }

    private val routeInfoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        textAlign = Paint.Align.RIGHT
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    private val routeInfoValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    private val pctBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        strokeWidth = 0f
        style = Paint.Style.STROKE
    }

    // --- PAINTS Y RUTAS PARA LOS 5 MODELOS DE ALTIMETRÍA REVOLUCIONARIOS ---
    // 1. Horizonte Isométrico (Cockpit 3D)
    private val headlightPath = Path()
    private val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val isoRidgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F2FE")
        strokeWidth = 3.0f
        style = Paint.Style.STROKE
    }

    // 2. Oasis y Crisoles Tácticos
    private val oasisGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val oasisBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E00284C7")
        style = Paint.Style.FILL
    }
    private val oasisBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F2FE")
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
    }
    private val crucibleBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0991B1B")
        style = Paint.Style.FILL
    }
    private val crucibleBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FECACA")
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
    }
    private val shelfRidgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val crucibleRidgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val hypoxiaHazePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 3. Campo de Fuerza y Fatiga
    private var currentMicroSubDivisions = 1

    private val inertiaWavePath = Path()
    private val inertiaWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2.2f
        style = Paint.Style.STROKE
    }
    private val tensionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#35FFFFFF")
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
    }
    private val pacingLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAB308")
        strokeWidth = 1.8f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    // 4. Monolito Obsidiana y Plasma
    private val obsidianFacetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0CFFFFFF")
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
    }
    private val plasmaCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val neonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8038BDF8")
        strokeWidth = 8.0f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = android.graphics.BlurMaskFilter(16f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    private val neonCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2.2f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val holoBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0090D16")
        style = Paint.Style.FILL
    }
    private val holoBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8038BDF8")
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
    }

    private val gravelLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000") // 60% opacity black
        strokeWidth = 6f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 12f), 0f) // Dotted effect
    }

    private val trailLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B33E2723") // 70% opacity dark brown
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val subBlockPctTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 0f, 3f, Color.BLACK)
    }

    // Pared frontal y lateral 3D
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val leftEndcapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }

    private val leftEndcapBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    // Curvas de herradura (líneas transversales sobre el asfalto 3D)
    private val hairpinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val hairpinLineBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    // Barra de distancia inferior 3D
    private val baseBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#050811")
        style = Paint.Style.FILL
    }

    private val baseBarBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2937")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val baseDistTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Encabezados y telemetría
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private val subTitleLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    private val oasisHoloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 0f, 0f, Color.parseColor("#800284C7"))
    }
    private val virtualPacerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8022D3EE") // Semi-transparent Cyan
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#22D3EE"))
    }


    private val liveGradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        typeface = Typeface.DEFAULT_BOLD
    }

    private val maxGradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        typeface = Typeface.DEFAULT_BOLD
    }

    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val percentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    // Baliza del ciclista
    private val beaconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    private val beaconHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4538BDF8")
        style = Paint.Style.FILL
    }

    private val beaconCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var beaconScale = 1f
    private var beaconShape = "circle"

    private fun applyBeaconStyle(color: Int, size: String, shape: String) {
        beaconPaint.color = color
        beaconHaloPaint.color = Color.argb(0x45, Color.red(color), Color.green(color), Color.blue(color))
        beaconScale = when (size) { "small" -> 0.7f; "large" -> 1.5f; else -> 1f }
        beaconShape = shape
    }

    private fun drawBeaconShape(canvas: Canvas, cx: Float, cy: Float, haloR: Float, mainR: Float, coreR: Float) {
        val s = beaconScale
        when (beaconShape) {
            "diamond" -> {
                drawBeaconPoly(canvas, cx, cy, haloR * s, beaconHaloPaint, diamond = true)
                drawBeaconPoly(canvas, cx, cy, mainR * s, beaconPaint, diamond = true)
                drawBeaconPoly(canvas, cx, cy, coreR * s, beaconCorePaint, diamond = true)
            }
            "arrow" -> {
                drawBeaconPoly(canvas, cx, cy, haloR * s, beaconHaloPaint, diamond = false)
                drawBeaconPoly(canvas, cx, cy, mainR * s, beaconPaint, diamond = false)
                drawBeaconPoly(canvas, cx, cy, coreR * s * 0.6f, beaconCorePaint, diamond = false)
            }
            else -> {
                canvas.drawCircle(cx, cy, haloR * s, beaconHaloPaint)
                canvas.drawCircle(cx, cy, mainR * s, beaconPaint)
                canvas.drawCircle(cx, cy, coreR * s, beaconCorePaint)
            }
        }
    }

    // Rombo, o triángulo apuntando hacia abajo (marca el punto exacto)
    private fun drawBeaconPoly(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint, diamond: Boolean) {
        val p = Path()
        if (diamond) {
            p.moveTo(cx, cy - r); p.lineTo(cx + r, cy); p.lineTo(cx, cy + r); p.lineTo(cx - r, cy)
        } else {
            p.moveTo(cx - r, cy - r); p.lineTo(cx + r, cy - r); p.lineTo(cx, cy + r)
        }
        p.close()
        canvas.drawPath(p, paint)
    }

    // POIs estilo insignias circulares La Flamme Rouge
    private val poiLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 1.8f
        style = Paint.Style.STROKE
    }

    private val poiBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2626")
        style = Paint.Style.FILL
    }

    private val poiBadgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val poiBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val poiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Rampas duras
    private val rampArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    private val rampArrowHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.FILL
    }

    private val rampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Zoom Overlay
    private val zoomBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E018181B")
        style = Paint.Style.FILL
    }

    private val zoomBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val zoomBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private var nextBlocks: List<Float> = emptyList()
    private var currentElevation: Double = 350.0
    private var maxElevation: Double = 727.0
    private var currentGrade: Double = 7.2
    private var remainingDistance: Double = 8500.0
    private var blockSizeMeters: Double = 100.0
    private var lookaheadMeters: Int = 350
    private var fontScale: Float = 1.0f
    private var showCotas: Boolean = true
    private var showRamps: Boolean = true
    private var showMaxGrade: Boolean = true
    private var rotate90: Boolean = false
    private var rampMinSlope: Double = 10.0
    private var rampMaxSlope: Double = 15.0
    private var showHairpins: Boolean = true
    private var showPois: Boolean = true
    private var hairpins: List<Double> = emptyList()
    private var pois: List<Poi> = emptyList()
    private var showZoomControls: Boolean = true
    private var showBlockPercentages: Boolean = true
    private var curvatureOffsets: List<Float> = emptyList()
    private var mapRotationAngle: Double = 0.0
    private var isHeadingUp: Boolean = false
    private var riderProgress: Float = 0.0f
    private val riddenDimPath = Path()
    private val riddenDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(120, 0, 0, 0)
    }
    private var windowStartMeters: Double = 0.0
    private var subBlocks: List<Float> = emptyList()
    private var subBlockSizeMeters: Double = 50.0
    private var majorBlockSizeMeters: Double = 100.0
    private var profileElevations: List<Float> = emptyList()
    private var activeClimbs: List<RouteClimb> = emptyList()
    private var altimetriaStyle: AltimetriaStyle = AltimetriaStyle.CLASSIC
    private var targetVam: Int = 900
    private var visibleAvgGrade: Double = 0.0
    private var visibleMaxGrade: Double = 0.0
    private var panButtonsPosition: String = "bottom"

    // Interactive Zoom & Pan
    private var zoomScale: Float = 1.0f
    private var panOffsetX: Float = 0.0f
    private var isDragging: Boolean = false
    private var lastTouchX: Float = 0.0f
    private var routeName: String? = null
    private var routeCoords: List<Pair<Double, Double>> = emptyList()
    var routeTotalLength: Double = 0.0
    var oasisDistanceToNextCrucible: Double? = null
    var virtualPacerRelativeDistance: Double? = null
    var energyBatteryLevel: Double = 100.0
    
    // ELITE Feature States
    var stravaSegmentDistance: Double? = null
    var stravaPrGhostDistance: Double? = null
    var windEffectIntensity: Double? = null // -1 to 1
    // W'bal Feature
    var wbal: Double? = null
    var wbalFailureMeters: Double? = null
    var wPrime: Int = 20000  // anaerobic capacity in joules
    // Target Cadence & Gear Wall Feature
    var currentCadence: Double = 0.0
    var currentSpeedMs: Double = 0.0
    var gearWallEnabled: Boolean = true
    var showGearBadge: Boolean = true
    var mergeSimilarBlocks: Boolean = false
    var hideGradeRangeEnabled: Boolean = false
    var hideGradeMin: Float = -2f
    var hideGradeMax: Float = 2f
    var showRouteDistances: Boolean = false
    // Metros entre el inicio de la ventana y el primer bloque (rejilla anclada a la ruta)
    private var gridOffsetMeters: Double = 0.0
    // Uniones entre columnas fusionadas (índice de muestra): no se dibuja su separador
    private var mergedJoint: BooleanArray = BooleanArray(0)
    // Pendiente media del puerto: si no es null, se recomienda una marcha fija para toda la subida
    var climbGearGrade: Double? = null
    var gearChainrings: String = GearAdvisor.DEFAULT_CHAINRINGS
    var gearCassette: String = GearAdvisor.DEFAULT_CASSETTE

    private val stravaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FC4C02") // Strava Orange
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#FC4C02"))
    }
    
    private val windPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    var isClimbMode: Boolean = false

    private val ribbonQuadPath = Path()
    private val wallPath = Path()
    private val leftEndcapPath = Path()
    private val arrowPath = Path()
    private val baseBarRect = RectF()
    private val leftBaseEndcapPath = Path()
    private val oasisShelfPath = Path()
    private val cruciblePath = Path()
    private val pacingPath = Path()
    private val plasmaCorePath = Path()
    private var showHeaderStats: Boolean = true
    private var showSurfaceTextures: Boolean = false

    fun update3DData(
        blocks: List<Float>,
        elevation: Double,
        maxElev: Double,
        grade: Double,
        remainingDist: Double,
        blockSizeMeters: Double = 100.0,
        fontScale: Float = 1.0f,
        lookaheadMeters: Int = 350,
        showCotas: Boolean = true,
        showRamps: Boolean = true,
        showMaxGrade: Boolean = true,
        fontFamilyKey: String = "sans-serif-condensed",
        rotate90: Boolean = false,
        rotateMinus90: Boolean = false,
        rampMinSlope: Double = 10.0,
        rampMaxSlope: Double = 15.0,
        showHairpins: Boolean = true,
        showPois: Boolean = true,
        hairpins: List<Double> = emptyList(),
        pois: List<Poi> = emptyList(),
        showZoomControls: Boolean = true,
        curvatureOffsets: List<Float> = emptyList(),
        riderProgress: Float = 0.0f,
        windowStartMeters: Double = 0.0,
        subBlocks: List<Float> = emptyList(),
        subBlockSizeMeters: Double = 50.0,
        majorBlockSizeMeters: Double = 100.0,
        profileElevations: List<Float> = emptyList(),
        showBlockPercentages: Boolean = true,
        altimetriaStyle: AltimetriaStyle = AltimetriaStyle.CLASSIC,
        targetVam: Int = 900,
        activeClimbs: List<RouteClimb> = emptyList(),
        visibleAvgGrade: Double = 0.0,
        visibleMaxGrade: Double = 0.0,
        routeName: String? = null,
        customTitle: String? = null,
        showHeaderStats: Boolean = true,
        routeCoords: List<Pair<Double, Double>> = emptyList(),
        mapRotationAngle: Double = 0.0,
        isHeadingUp: Boolean = false,
        showSurfaceTextures: Boolean = false,
        routeTotalLength: Double = 0.0,
        panButtonsPosition: String = "bottom",
        beaconColor: Int = 0xFF38BDF8.toInt(),
        beaconSize: String = "medium",
        beaconShape: String = "circle",
        gridOffsetMeters: Double = 0.0
    ) {
        this.gridOffsetMeters = gridOffsetMeters
        this.panButtonsPosition = panButtonsPosition
        applyBeaconStyle(beaconColor, beaconSize, beaconShape)
        if (routeTotalLength > 0.0) {
            this.routeTotalLength = routeTotalLength
        }
        if (blocks.isNotEmpty()) {
            this.nextBlocks = blocks
        }
        this.routeCoords = routeCoords
        if (elevation > 0) {
            this.currentElevation = elevation
        }
        this.maxElevation = if (maxElev > 0) maxElev else 727.0
        this.currentGrade = grade
        this.remainingDistance = remainingDist
        this.blockSizeMeters = blockSizeMeters
        this.fontScale = fontScale
        this.lookaheadMeters = lookaheadMeters
        this.showCotas = showCotas
        this.showRamps = showRamps
        this.showMaxGrade = showMaxGrade
        this.rotate90 = rotate90
        this.rampMinSlope = rampMinSlope
        this.rampMaxSlope = rampMaxSlope
        this.showHairpins = showHairpins
        this.showPois = showPois
        this.hairpins = hairpins
        this.pois = pois
        this.showZoomControls = showZoomControls
        this.showBlockPercentages = showBlockPercentages
        this.curvatureOffsets = curvatureOffsets
        this.riderProgress = riderProgress
        this.windowStartMeters = windowStartMeters
        this.altimetriaStyle = altimetriaStyle
        this.targetVam = targetVam
        this.activeClimbs = activeClimbs
        this.visibleAvgGrade = visibleAvgGrade
        this.visibleMaxGrade = visibleMaxGrade
        this.routeName = routeName
        this.showHeaderStats = showHeaderStats
        this.mapRotationAngle = mapRotationAngle
        this.isHeadingUp = isHeadingUp
        this.showSurfaceTextures = showSurfaceTextures
        if (subBlocks.isNotEmpty()) {
            this.subBlocks = subBlocks
        }
        this.subBlockSizeMeters = subBlockSizeMeters
        this.majorBlockSizeMeters = majorBlockSizeMeters
        if (profileElevations.isNotEmpty()) {
            this.profileElevations = profileElevations
        }
        this.routeName = routeName

        FontHelper.applyFontToPaint(titlePaint, fontFamilyKey)
        FontHelper.applyFontToPaint(subTitleLabelPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(liveGradePaint, fontFamilyKey)
        FontHelper.applyFontToPaint(maxGradePaint, fontFamilyKey)
        FontHelper.applyFontToPaint(percentTextPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(subBlockPctTextPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(rampTextPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(cotaTextPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(axisTextPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(poiLabelPaint, fontFamilyKey)
        FontHelper.applyFontToPaint(baseDistTextPaint, fontFamilyKey)

        postInvalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showZoomControls) return super.onTouchEvent(event)

        val touchX = event.x
        val touchY = event.y
        val w = width.toFloat()
        val h = height.toFloat()

        val pillW = (w * 0.22f).coerceIn(80f, 130f)
        val pillH = (h * 0.15f).coerceIn(28f, 44f)
        val right = w - 16f
        val left = right - pillW
        val top = 16f
        val bottom = top + pillH
        val centerX = left + (pillW / 2f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (touchY in top..bottom) {
                    if (touchX >= left && touchX < centerX) {
                        performClick()
                        zoomOut()
                        return true
                    }
                    if (touchX >= centerX && touchX <= right) {
                        performClick()
                        zoomIn()
                        return true
                    }
                }
                isDragging = true
                lastTouchX = touchX
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = touchX - lastTouchX
                    lastTouchX = touchX

                    val totalWidth = w * zoomScale
                    val maxPanX = (totalWidth - w).coerceAtLeast(0f)

                    panOffsetX = (panOffsetX - dx).coerceIn(0f, maxPanX)
                    postInvalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun zoomIn() {
        val curr = lookaheadMeters
        val nextVal = when {
            curr > 50000 -> 50000
            curr > 20000 -> 20000
            curr > 10000 -> 10000
            curr > 5000 -> 5000
            curr > 2000 -> 2000
            curr > 1000 -> 1000
            curr == 1000 -> 500
            curr > 200 -> curr - 50
            else -> 200
        }
        lookaheadMeters = nextVal
        AppPreferences.getInstance(context).lookaheadMeters3d = nextVal
        postInvalidate()
    }

    private fun zoomOut() {
        val curr = lookaheadMeters
        val maxAllowed = if (routeTotalLength > 500.0) routeTotalLength.toInt() else 200000
        if (curr >= maxAllowed) return

        var nextVal = when {
            curr >= 200000 -> 200000
            curr >= 100000 -> curr + 50000
            curr >= 50000 -> curr + 10000
            curr >= 20000 -> curr + 5000
            curr >= 5000 -> curr + 1000
            curr >= 1000 -> curr + 500
            curr >= 500 -> curr + 100
            curr >= 50 -> curr + 50
            else -> 50
        }
        if (routeTotalLength > 500.0 && (nextVal > maxAllowed || (curr < maxAllowed && nextVal >= maxAllowed))) {
            nextVal = maxAllowed
        }
        val finalVal = nextVal.coerceAtMost(maxAllowed)
        lookaheadMeters = finalVal
        AppPreferences.getInstance(context).lookaheadMeters3d = finalVal
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0 || h <= 0) return

        if (rotate90) {
            canvas.save()
            canvas.translate(w, 0f)
            canvas.rotate(90f)
            drawRenderContent(canvas, h, w)
            canvas.restore()
        } else {
            drawRenderContent(canvas, w, h)
        }
    }

    private fun drawRenderContent(canvas: Canvas, w: Float, h: Float) {
        // 1. Fondo Oscuro
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Encabezado Título ("Altimetría 3D")
        if (showHeaderStats) {
        val baseTitle = context.getString(R.string.data_type_altimetria_3d_title)
        val titleText = if (!routeName.isNullOrEmpty()) "$baseTitle - $routeName" else baseTitle
        titlePaint.textSize = ((h * 0.080f) * fontScale).coerceIn(16f, 28f)
        canvas.drawText(titleText, 18f, h * 0.07f, titlePaint)

        val numCols = if (showMaxGrade) 3 else 2
        val colW = (w - 32f) / numCols
        val col1X = 16f
        val col2X = 16f + colW
        val col3X = 16f + colW * 2f

        subTitleLabelPaint.textSize = ((h * 0.050f) * fontScale).coerceIn(10f, 15f)

        // COLUMNA 1: PENDIENTE ACTUAL
        val labelCurrentGrade = context.getString(R.string.label_current_gradient)
        canvas.drawText(labelCurrentGrade, col1X, h * 0.14f, subTitleLabelPaint)

        val liveGradeText = "%.1f%%".format(currentGrade)
        liveGradePaint.textSize = ((h * 0.22f) * fontScale).coerceIn(36f, 62f)
        liveGradePaint.color = GradeColorScale.getTelemetryColor(currentGrade)
        canvas.drawText(liveGradeText, col1X, h * 0.27f, liveGradePaint)

        // COLUMNA 2: PENDIENTE MEDIA DEL TRAMO VISIBLE
        val tramoAvgGrade = if (visibleAvgGrade > 0.0) visibleAvgGrade else (if (subBlocks.isNotEmpty()) subBlocks.average() else (if (nextBlocks.isNotEmpty()) nextBlocks.average() else currentGrade))
        val labelAvgGrade = context.getString(R.string.label_avg_gradient_tramo)
        canvas.drawText(labelAvgGrade, col2X, h * 0.14f, subTitleLabelPaint)

        val avgGradeText = "%.1f%%".format(tramoAvgGrade)
        liveGradePaint.textSize = ((h * 0.18f) * fontScale).coerceIn(28f, 50f)
        liveGradePaint.color = GradeColorScale.getTelemetryColor(tramoAvgGrade)
        canvas.drawText(avgGradeText, col2X, h * 0.26f, liveGradePaint)

        // COLUMNA 3: PENDIENTE MÁXIMA DEL TRAMO VISIBLE
        if (showMaxGrade) {
            val tramoMaxGrade = if (visibleMaxGrade > 0.0) visibleMaxGrade else (if (subBlocks.isNotEmpty()) subBlocks.maxOrNull()?.toDouble() ?: 12.8 else (if (nextBlocks.isNotEmpty()) nextBlocks.maxOrNull()?.toDouble() ?: 12.8 else 12.8))
            val labelMaxGrade = context.getString(R.string.label_max_gradient_tramo)
            canvas.drawText(labelMaxGrade, col3X, h * 0.14f, subTitleLabelPaint)

            val maxGradeText = "%.1f%%".format(tramoMaxGrade)
            maxGradePaint.textSize = ((h * 0.18f) * fontScale).coerceIn(28f, 50f)
            maxGradePaint.color = GradeColorScale.getTelemetryColor(tramoMaxGrade)
            canvas.drawText(maxGradeText, col3X, h * 0.26f, maxGradePaint)
        }
        } // Fin de if (showHeaderStats)

        // 3. Botones Minimalistas de Zoom [ - | + ]
        // Desactivado en canvas porque ahora se usan botones XML

        // 4. Perfil Altimétrico
        if (altimetriaStyle == AltimetriaStyle.GLOBAL_ISOMETRIC) {
            drawGlobalIsometricRibbon(canvas, w, h)
        } else {
            draw3DStraightRibbon(canvas, w, h)
        }

        if (showRouteDistances) drawRouteDistances(canvas, w, h)
    }

    // Distancia total de la ruta y distancia que queda, arriba a la derecha de la gráfica
    private fun drawRouteDistances(canvas: Canvas, w: Float, h: Float) {
        if (routeTotalLength <= 0.0) return
        val left = if (remainingDistance > 0.0) remainingDistance.coerceAtMost(routeTotalLength) else 0.0
        val textSize = ((h * 0.050f) * fontScale).coerceIn(10f, 15f)
        routeInfoLabelPaint.textSize = textSize
        routeInfoValuePaint.textSize = textSize * 1.15f
        val lines = listOf(
            context.getString(R.string.label_route_total) to formatRouteKm(routeTotalLength),
            context.getString(R.string.label_route_left) to formatRouteKm(left)
        )
        val right = w - 16f
        val lineH = textSize * 1.45f
        // Con cabecera la franja superior está ocupada: se coloca justo encima del perfil
        val firstBaseline = if (showHeaderStats) h * 0.33f else (h * 0.15f).coerceIn(28f, 44f) + 22f
        for ((idx, line) in lines.withIndex()) {
            val y = firstBaseline + idx * lineH
            canvas.drawText(line.second, right, y, routeInfoValuePaint)
            val valueW = routeInfoValuePaint.measureText(line.second)
            canvas.drawText(line.first, right - valueW - 6f, y, routeInfoLabelPaint)
        }
    }

    private fun formatRouteKm(meters: Double): String = when {
        meters < 1000.0 -> "${meters.toInt()}m"
        meters < 100000.0 -> String.format("%.1fkm", meters / 1000.0)
        else -> String.format("%.0fkm", meters / 1000.0)
    }

    private fun drawZoomOverlay(canvas: Canvas, w: Float, h: Float) {
        val pillW = (w * 0.24f).coerceIn(86f, 135f)
        val pillH = (h * 0.15f).coerceIn(26f, 40f)

        val marginR = 16f
        val marginT = 4f // Subido un poco más hacia arriba (antes 16f)

        val right = w - marginR
        val left = right - pillW
        val top = marginT
        val bottom = top + pillH

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, zoomBgPaint)
        canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, zoomBorderPaint)

        val textSize = (pillH * 0.58f).coerceIn(11f, 16f)
        zoomBtnPaint.textSize = textSize

        val centerY = top + (pillH / 2f) + (textSize * 0.35f)
        val centerX = left + (pillW / 2f)

        val zoomLabel = when {
            lookaheadMeters < 1000 -> "${lookaheadMeters}m"
            lookaheadMeters % 1000 == 0 -> "${lookaheadMeters / 1000}km"
            else -> "${lookaheadMeters / 1000}.${(lookaheadMeters % 1000) / 100}km"
        }
        canvas.drawText(zoomLabel, centerX, centerY, zoomBtnPaint)
    }

    private fun drawTopographicPeaks(canvas: Canvas, xFront: FloatArray, yFront: FloatArray, yBase: FloatArray, totalMicroSamples: Int, microElevations: FloatArray) {
        if (totalMicroSamples < 3 || microElevations.isEmpty()) return

        val peaks = mutableListOf<Pair<Int, Float>>()
        for (i in 1 until totalMicroSamples - 1) {
            val curr = microElevations[i]
            val prev = microElevations[i - 1]
            val next = microElevations[i + 1]
            if (curr > prev && curr > next) {
                peaks.add(Pair(i, curr))
            }
        }
        
        if (microElevations.size > 1) {
            if (microElevations[0] > microElevations[1]) peaks.add(Pair(0, microElevations[0]))
            if (microElevations[totalMicroSamples - 1] > microElevations[totalMicroSamples - 2]) {
                peaks.add(Pair(totalMicroSamples - 1, microElevations[totalMicroSamples - 1]))
            }
        }

        peaks.sortByDescending { it.second }

        val minDistanceSeparationIdx = (totalMicroSamples * 0.05).toInt().coerceAtLeast(3)
        val selectedPeaks = mutableListOf<Pair<Int, Float>>()

        for (peak in peaks) {
            if (selectedPeaks.size >= 3) break
            val tooClose = selectedPeaks.any { Math.abs(it.first - peak.first) < minDistanceSeparationIdx }
            if (!tooClose) {
                selectedPeaks.add(peak)
            }
        }

        if (reusablePinPaint == null) reusablePinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#80000000"))
        }
        
        if (reusablePeakTextPaint == null) reusablePeakTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f // Reduced size
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) // Light instead of bold
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, Color.parseColor("#99000000"))
        }
        val pinPaint = reusablePinPaint!!
        val textPaint = reusablePeakTextPaint!!

        for (peak in selectedPeaks) {
            val idx = peak.first
            val px = xFront[idx]
            val pyTop = yFront[idx]
            val elev = peak.second

            val triSize = 12f
            val triPath = reusableTriPath ?: Path().also { reusableTriPath = it }
            triPath.reset()
            triPath.moveTo(px, pyTop - 6f)
            triPath.lineTo(px - triSize / 2f, pyTop - 6f - triSize)
            triPath.lineTo(px + triSize / 2f, pyTop - 6f - triSize)
            triPath.close()
            
            canvas.drawPath(triPath, pinPaint)
            
            canvas.drawText("${elev.toInt()}m", px, pyTop - 10f - triSize, textPaint)
        }
    }

    private fun draw3DStraightRibbon(canvas: Canvas, w: Float, h: Float) {
        val totalMetersAhead = lookaheadMeters.toDouble().coerceIn(100.0, 200000.0)

        val totalMicroSamples: Int
        val microElevations: FloatArray
        val microGrades: FloatArray
        val microSubDivisions: Int
        val majorBlocksCount: Int

        if (profileElevations.size >= 2 && subBlocks.isNotEmpty()) {
            totalMicroSamples = profileElevations.size
            microElevations = profileElevations.toFloatArray()
            microGrades = subBlocks.toFloatArray()
            val ratio = (majorBlockSizeMeters / subBlockSizeMeters.coerceAtLeast(1.0)).roundToInt().coerceAtLeast(1)
            microSubDivisions = ratio
            currentMicroSubDivisions = microSubDivisions
            majorBlocksCount = ((totalMicroSamples - 1) / microSubDivisions).coerceAtLeast(1)
        } else {
            majorBlocksCount = if (totalMetersAhead > 1000.0) {
                (totalMetersAhead / 1000.0).toInt().coerceIn(2, 10)
            } else {
                (totalMetersAhead / blockSizeMeters.coerceAtLeast(10.0)).toInt().coerceIn(2, 10)
            }
            microSubDivisions = if (totalMetersAhead >= 1000.0) 20 else 5
            currentMicroSubDivisions = microSubDivisions
            totalMicroSamples = (majorBlocksCount * microSubDivisions) + 1

            val microDistMeters = totalMetersAhead / (totalMicroSamples - 1)
            microElevations = FloatArray(totalMicroSamples)
            microGrades = FloatArray(totalMicroSamples - 1)
            var accumulatedElev = currentElevation
            microElevations[0] = accumulatedElev.toFloat()

            for (i in 0 until totalMicroSamples - 1) {
                val majorBlockIdx = (i / microSubDivisions).coerceAtMost(nextBlocks.size - 1)
                val baseGrade = if (majorBlockIdx < nextBlocks.size) nextBlocks[majorBlockIdx].toDouble() else 4.0
                val microGrade = baseGrade.coerceIn(-15.0, 30.0).toFloat()
                microGrades[i] = microGrade
                accumulatedElev += (microDistMeters * (microGrade / 100.0))
                microElevations[i + 1] = accumulatedElev.toFloat()
            }
        }

        val majorSamples = majorBlocksCount + 1

        val profileMode = profileElevations.size >= 2 && subBlocks.isNotEmpty() && subBlockSizeMeters > 0.0
        val gearBadgeShown = gearWallEnabled && showGearBadge

        val startX = (w * 0.10f)
        val baseEndX = (w * 0.98f)
        val defaultGroundY = when {
            showHeaderStats -> h * 0.82f
            isClimbMode && !rotate90 -> h * 0.81f
            else -> h * 0.77f
        }
        val defaultPeakHeight = when {
            showHeaderStats -> h * 0.48f
            isClimbMode && !rotate90 -> h * 0.48f
            else -> h * 0.60f
        }
        // Sin la marcha recomendada debajo del perfil, la gráfica baja y el hueco se usa para un perfil más alto
        val freedBottom = if (!gearBadgeShown && !rotate90) {
            val legendSize = ((h * 0.075f) * fontScale).coerceIn(16f, 23f)
            val legendTop = h - 23f - legendSize * 0.8f
            val barH0 = (h * 0.09f).coerceIn(20f, 32f)
            val gearRoom = ((h * 0.075f) * fontScale).coerceIn(18f, 28f) * 0.75f + 24f
            minOf(gearRoom, legendTop - 4f - barH0 - defaultGroundY).coerceAtLeast(0f)
        } else 0f
        val baseGroundY = defaultGroundY + freedBottom
        val maxPeakHeight = defaultPeakHeight + freedBottom

        // Desplazamiento oblicuo 3D sutil y elegante (profundidad reducida a petición del usuario)
        val depth3dX = (w * 0.012f).coerceIn(4f, 8f)
        val depth3dY = (h * 0.015f).coerceIn(5f, 9f)

        val totalSpanX = (baseEndX - startX) * zoomScale
        // La ventana real mide lookaheadMeters; los bloques extra de margen quedan fuera de pantalla a la derecha.
        // Así la baliza (riderProgress=1) y el último bloque coinciden con el borde derecho.
        val windowSamples = if (profileMode) {
            (lookaheadMeters / subBlockSizeMeters).toFloat().coerceIn(1f, (totalMicroSamples - 1).toFloat())
        } else {
            (totalMicroSamples - 1).coerceAtLeast(1).toFloat()
        }
        val microStepX = totalSpanX / windowSamples
        // La rejilla de bloques queda anclada a la ruta: la muestra 0 está gridOffsetMeters antes del inicio de la ventana
        val gridShiftSamples = if (profileMode) (gridOffsetMeters / subBlockSizeMeters).toFloat() else 0f
        val gridShiftPx = gridShiftSamples * microStepX

        val xFront = FloatArray(totalMicroSamples)
        val yFront = FloatArray(totalMicroSamples)
        val xBack = FloatArray(totalMicroSamples)
        val yBack = FloatArray(totalMicroSamples)
        val yBase = FloatArray(totalMicroSamples)

        // Escala vertical solo con las muestras de la ventana (más un margen a la derecha): los bloques de la rejilla
        // que quedan a la izquierda no deben alterar la escala
        val scaleFrom = if (gridShiftSamples > 0f) gridShiftSamples.toInt().coerceIn(0, totalMicroSamples - 1) else 0
        val scaleTo = if (gridShiftSamples > 0f) (Math.ceil((gridShiftSamples + windowSamples).toDouble()).toInt() + 2).coerceIn(scaleFrom, totalMicroSamples - 1) else totalMicroSamples - 1
        var rawMinElev = Float.MAX_VALUE
        var rawMaxElev = -Float.MAX_VALUE
        for (si in scaleFrom..scaleTo) {
            if (microElevations[si] < rawMinElev) rawMinElev = microElevations[si]
            if (microElevations[si] > rawMaxElev) rawMaxElev = microElevations[si]
        }
        if (rawMinElev == Float.MAX_VALUE) {
            rawMinElev = currentElevation.toFloat()
            rawMaxElev = rawMinElev + 50f
        }
        val actualRange = (rawMaxElev - rawMinElev).coerceAtLeast(0f)

        // Escala visual proporcional: un 15% de pendiente debe ocupar todo el alto.
        // Limitamos este rango mínimo a 250m para que rutas largas no queden planas.
        val minProportionalRange = (totalMetersAhead * 0.15f).toFloat().coerceAtMost(250f)
        val effectiveRange = actualRange.coerceAtLeast(minProportionalRange).coerceAtLeast(15f)

        val padding = (effectiveRange - actualRange) / 2f
        val displayMinElev = rawMinElev - padding
        val finalElevRange = effectiveRange

        // GRÁFICA RECTA: xFront avanza de forma estrictamente lineal, sin zigzagueo de carretera
        for (i in 0 until totalMicroSamples) {
            val px = startX + (i * microStepX) - panOffsetX - gridShiftPx
            val normalizedHeight = ((microElevations[i] - displayMinElev) / finalElevRange).coerceIn(0f, 1f)
            val pYFront = baseGroundY - (normalizedHeight * maxPeakHeight)

            var curveOffset = 0f
            if (showHairpins && hairpins.isNotEmpty()) {
                val dist = windowStartMeters + (i.toDouble() / windowSamples) * lookaheadMeters
                for (hp in hairpins) {
                    val diff = dist - hp
                    if (Math.abs(diff) < 25.0) {
                        val phase = (diff + 25.0) / 50.0
                        curveOffset = (Math.sin(phase * Math.PI * 2.0) * 20.0).toFloat()
                        break
                    }
                }
            }

            xFront[i] = px + curveOffset
            yFront[i] = pYFront

            // Cara trasera extruida en 3D sutil hacia arriba y atrás
            xBack[i] = (px + curveOffset) - depth3dX
            yBack[i] = pYFront - depth3dY

            yBase[i] = baseGroundY
        }

        val majorX = FloatArray(majorSamples)
        val majorYBase = FloatArray(majorSamples)
        val majorElevations = FloatArray(majorSamples)

        for (k in 0 until majorSamples) {
            val microIdx = (k * microSubDivisions).coerceAtMost(totalMicroSamples - 1)
            majorX[k] = xFront[microIdx]
            majorYBase[k] = yBase[microIdx]
            majorElevations[k] = microElevations[microIdx]
        }

        // 1. Isolíneas de altitud de fondo
        val steps = 4
        axisTextPaint.textSize = (h * 0.038f).coerceIn(9f, 13f)
        axisTextPaint.textAlign = Paint.Align.LEFT
        for (k in 0..steps) {
            val ratio = k.toFloat() / steps
            val elevMark = (displayMinElev + (ratio * finalElevRange)).toInt()
            val yIso = baseGroundY - (ratio * maxPeakHeight)

            canvas.drawLine(startX - depth3dX, yIso, baseEndX, yIso, gridPaint)
            canvas.drawText("${elevMark}m", 6f, yIso + 4f, axisTextPaint)
        }

        // Fusión opcional de bloques vecinos del mismo color: una sola columna con la pendiente media del tramo
        val displayGrades: FloatArray
        val mergedRuns = ArrayList<IntRange>()
        if (mergeSimilarBlocks && profileMode && microGrades.size > 1) {
            displayGrades = microGrades.copyOf()
            mergedJoint = BooleanArray(totalMicroSamples)
            var s = 0
            while (s < microGrades.size) {
                val key = getGradeColor(microGrades[s].toDouble())
                var e = s + 1
                while (e < microGrades.size && getGradeColor(microGrades[e].toDouble()) == key) e++
                if (e - s > 1) {
                    var sum = 0.0
                    for (k in s until e) sum += microGrades[k]
                    val avg = (sum / (e - s)).toFloat()
                    for (k in s until e) displayGrades[k] = avg
                    for (j in s + 1 until e) mergedJoint[j] = true
                }
                mergedRuns.add(s until e)
                s = e
            }
        } else {
            displayGrades = microGrades
            mergedJoint = BooleanArray(0)
        }

        // Los bloques de la rejilla situados a la izquierda de la ventana no deben invadir el eje de altitudes
        val clipLeftBlocks = gridShiftPx > 0f
        if (clipLeftBlocks) {
            canvas.save()
            canvas.clipRect(startX - depth3dX - 1f, 0f, w, h)
        }

        // 2. RENDERIZADO DEL PERFIL SEGÚN EL MODELO SELECCIONADO
        when (altimetriaStyle) {
            AltimetriaStyle.CLASSIC -> {
                drawClassicRibbon(canvas, totalMicroSamples, displayGrades, xFront, yFront, xBack, yBack, yBase, depth3dY)
            }
            AltimetriaStyle.HORIZON_ISOMETRIC -> {
                drawHorizonIsometricRibbon(canvas, w, h, totalMicroSamples, displayGrades, xFront, yFront, xBack, yBack, yBase)
            }
            AltimetriaStyle.TACTICAL_OASES -> {
                drawTacticalOasesRibbon(canvas, w, h, totalMicroSamples, displayGrades, xFront, yFront, xBack, yBack, yBase, depth3dY)
            }
            AltimetriaStyle.DYNAMIC_FORCE_FIELD -> {
                drawDynamicForceFieldRibbon(canvas, w, h, totalMicroSamples, displayGrades, xFront, yFront, xBack, yBack, yBase, depth3dY, startX, baseEndX, baseGroundY)
            }
            AltimetriaStyle.MONOLITHIC_OBSIDIAN -> {
                drawMonolithicObsidianRibbon(canvas, w, h, totalMicroSamples, displayGrades, xFront, yFront, xBack, yBack, yBase, startX, baseEndX, baseGroundY, maxPeakHeight)
            }
            AltimetriaStyle.HILLSHADE_APEX -> {
                drawHillshadeApexRibbon(canvas, w, h, totalMicroSamples, displayGrades, xFront, yFront, xBack, yBack, yBase, depth3dY)
            }
            else -> {}
        }

        // 5. DIVISORES DE BLOQUE MAYOR VERTICALES CON COTAS DE ALTITUD
        cotaTextPaint.textSize = ((h * 0.055f) * fontScale).coerceIn(12f, 18f)
        cotaTextPaint.color = Color.parseColor("#E2E8F0") // Más claro y visible
        
        val maxMajorIdx = if (majorElevations.isNotEmpty()) majorElevations.indices.maxByOrNull { majorElevations[it] } ?: -1 else -1
        val minMajorIdx = if (majorElevations.isNotEmpty()) majorElevations.indices.minByOrNull { majorElevations[it] } ?: -1 else -1

        for (k in 0 until majorSamples) {
            val microIdx = (k * microSubDivisions).coerceAtMost(totalMicroSamples - 1)
            val px = xFront[microIdx]
            val pyTop = yFront[microIdx]
            val pyBase = yBase[microIdx]

            // A partir de 50km, mostrar solo los picos más altos y bajos
            val isLargeScale = lookaheadMeters >= 50000
            val shouldDrawCota = showCotas && (!isLargeScale || k == maxMajorIdx || k == minMajorIdx)

            if (shouldDrawCota) {
                // Línea vertical que sube un poco por encima del perfil
                canvas.drawLine(px, pyTop - 12f, px, pyBase, cotaLinePaint)
                
                val cotaText = "${majorElevations[k].toInt()}"
                canvas.save()
                val textOffsetX = px + 6f
                val textOffsetY = pyTop - 18f // Encima de la superficie del perfil
                canvas.translate(textOffsetX, textOffsetY)
                canvas.rotate(-90f)
                canvas.drawText(cotaText, 0f, 0f, cotaTextPaint)
                canvas.restore()
            }
        }

        // 6. PORCENTAJES DE PENDIENTE DENTRO DE LOS BLOQUES (GRANDES, HORIZONTALES Y DE ALTO CONTRASTE)
        // Ocultar los porcentajes en escalas muy amplias (>= 100km) para que el perfil de ruta sea claro
        if (showBlockPercentages && totalMicroSamples > 1 && lookaheadMeters < 100000) {
            val isObsidian = altimetriaStyle == AltimetriaStyle.MONOLITHIC_OBSIDIAN
            val badgeBg = if (isObsidian) holoBadgeBgPaint else pctBadgeBgPaint
            val badgeBorder = if (isObsidian) holoBadgeBorderPaint else pctBadgeBorderPaint

            val blockW = abs(xFront[1] - xFront[0])
            val useRuns = mergedRuns.isNotEmpty()
            val canDrawSubBlocks = blockW >= 28f || useRuns

            if (canDrawSubBlocks) {
                // Dibujar porcentaje grande y horizontal dentro de cada columna (o de cada tramo fusionado)
                val groups: List<IntRange> = if (useRuns) mergedRuns else (0 until totalMicroSamples - 1).map { it..it }
                for (grp in groups) {
                    val a = grp.first
                    val b = (grp.last + 1).coerceAtMost(totalMicroSamples - 1)
                    val g = if (a < displayGrades.size) displayGrades[a] else 4.0f
                    if (isGradeLabelHidden(g)) continue

                    // Un tramo fusionado centra su etiqueta en la parte visible
                    val leftX = if (useRuns) maxOf(xFront[a], startX) else xFront[a]
                    val rightX = if (useRuns) minOf(xFront[b], baseEndX) else xFront[b]
                    val groupW = abs(rightX - leftX)
                    if (useRuns && groupW < 28f) continue
                    val colMidX = (leftX + rightX) / 2f
                    var sumY = 0f
                    for (k in a..b) sumY += yFront[k]
                    val colTopY = sumY / (b - a + 1)
                    val colH = baseGroundY - colTopY

                    val pctStr = if (groupW >= 52f) {
                        if (abs(g - g.roundToInt()) < 0.15f) "%.0f%%".format(g) else "%.1f%%".format(g)
                    } else {
                        "%.0f%%".format(g)
                    }

                    val calcFontSize = (groupW * 0.40f * fontScale).coerceIn(14f, 24f)
                    percentTextPaint.textSize = calcFontSize

                    val isInsideBlock = colH >= (calcFontSize * 2.2f)
                    val centerY = if (isInsideBlock) {
                        colTopY + (colH * 0.50f)
                    } else {
                        colTopY - (calcFontSize * 0.85f)
                    }

                    val textW = percentTextPaint.measureText(pctStr)
                    val padX = (calcFontSize * 0.26f).coerceIn(4f, 8f)
                    val padY = (calcFontSize * 0.18f).coerceIn(3f, 6f)
                    val pillRect = RectF(
                        colMidX - (textW / 2f) - padX,
                        centerY - (calcFontSize * 0.50f) - padY,
                        colMidX + (textW / 2f) + padX,
                        centerY + (calcFontSize * 0.50f) + padY
                    )

                    canvas.drawRoundRect(pillRect, 6f, 6f, badgeBg)
                    canvas.drawRoundRect(pillRect, 6f, 6f, badgeBorder)
                    canvas.drawText(pctStr, colMidX, centerY + (calcFontSize * 0.35f), percentTextPaint)
                }
            } else {
                // Si la escala es muy amplia (>5km con muchas micro-columnas), dibujar porcentaje en cada bloque mayor
                val fontBaseSize = ((h * 0.085f) * fontScale).coerceIn(16f, 26f)
                for (k in 0 until majorBlocksCount) {
                    val microIdx1 = k * microSubDivisions
                    val microIdx2 = ((k + 1) * microSubDivisions).coerceAtMost(totalMicroSamples - 1)

                    var sumG = 0.0
                    var countG = 0
                    for (si in microIdx1 until microIdx2) {
                        if (si < microGrades.size) {
                            sumG += microGrades[si]
                            countG++
                        }
                    }
                    val avgGrade = if (countG > 0) (sumG / countG).toFloat() else (if (k < nextBlocks.size) nextBlocks[k] else 4.0f)
                    if (isGradeLabelHidden(avgGrade)) continue
                    val pctStr = if (abs(avgGrade - avgGrade.roundToInt()) < 0.15f) "%.0f%%".format(avgGrade) else "%.1f%%".format(avgGrade)

                    val midX = (xFront[microIdx1] + xFront[microIdx2]) / 2f
                    val midTopY = (yFront[microIdx1] + yFront[microIdx2]) / 2f
                    val bW = abs(xFront[microIdx2] - xFront[microIdx1])
                    val blockH = baseGroundY - midTopY

                    val calcFontSize = (bW * 0.32f * fontScale).coerceIn(16f, fontBaseSize)
                    percentTextPaint.textSize = calcFontSize

                    val isInsideBlock = blockH >= (calcFontSize * 2.2f)
                    val centerY = if (isInsideBlock) {
                        midTopY + (blockH * 0.50f)
                    } else {
                        midTopY - (calcFontSize * 0.85f)
                    }

                    val textW = percentTextPaint.measureText(pctStr)
                    val padX = (calcFontSize * 0.28f).coerceIn(5f, 9f)
                    val padY = (calcFontSize * 0.18f).coerceIn(3f, 6f)
                    val pillRect = RectF(
                        midX - (textW / 2f) - padX,
                        centerY - (calcFontSize * 0.50f) - padY,
                        midX + (textW / 2f) + padX,
                        centerY + (calcFontSize * 0.50f) + padY
                    )

                    canvas.drawRoundRect(pillRect, 6f, 6f, badgeBg)
                    canvas.drawRoundRect(pillRect, 6f, 6f, badgeBorder)
                    canvas.drawText(pctStr, midX, centerY + (calcFontSize * 0.35f), percentTextPaint)
                }
            }
        }

        // 6.b. MOUNTAIN GATES & SUMMIT BEACONS (Puertos Oficiales SDK)
        drawTopographicPeaks(canvas, xFront, yFront, yBase, totalMicroSamples, microElevations)

        // 7. Flechas con porcentaje para rampas duras (>= 10%)
        if (showRamps) {
            for (k in 0 until majorBlocksCount) {
                val startIdx = k * microSubDivisions
                val endIdx = ((k + 1) * microSubDivisions).coerceAtMost(totalMicroSamples - 1)

                var maxIdx = -1
                var maxG = 0f
                for (i in startIdx until endIdx) {
                    if (i < microGrades.size) {
                        val g = microGrades[i]
                        if (g >= rampMinSlope.toFloat() && g > maxG) {
                            maxG = g
                            maxIdx = i
                        }
                    }
                }

                if (maxIdx >= 0 && !isGradeLabelHidden(maxG)) {
                    val mx = xFront[maxIdx]
                    val my = yBack[maxIdx]

                    val arrowTopY = my - (h * 0.11f)
                    val arrowBottomY = my - 4f

                    canvas.drawLine(mx, arrowTopY, mx, arrowBottomY, rampArrowPaint)

                    arrowPath.reset()
                    arrowPath.moveTo(mx, arrowBottomY)
                    arrowPath.lineTo(mx - 4.5f, arrowBottomY - 7f)
                    arrowPath.lineTo(mx + 4.5f, arrowBottomY - 7f)
                    arrowPath.close()
                    canvas.drawPath(arrowPath, rampArrowHeadPaint)

                    rampTextPaint.textSize = ((h * 0.052f) * fontScale).coerceIn(10f, 17f)
                    val rampLabel = "%.0f%%".format(maxG)
                    canvas.drawText(rampLabel, mx, arrowTopY - 4f, rampTextPaint)
                }
            }
        }

        // 7.b. Curvas de herradura (Líneas transversales negras con borde blanco sobre la calzada 3D)
        if (showHairpins && hairpins.isNotEmpty()) {
            for (hp in hairpins) {
                if (hp !in 0.0..totalMetersAhead) continue

                val exactIndexF = distToSampleIdx(hp, totalMetersAhead, totalMicroSamples)
                val idx = exactIndexF.toInt().coerceIn(0, totalMicroSamples - 2)
                val rem = exactIndexF - idx

                val xF = xFront[idx] + rem * (xFront[idx + 1] - xFront[idx])
                val yF = yFront[idx] + rem * (yFront[idx + 1] - yFront[idx])
                val xB = xBack[idx] + rem * (xBack[idx + 1] - xBack[idx])
                val yB = yBack[idx] + rem * (yBack[idx + 1] - yBack[idx])

                // Dibujar línea transversal sobre la calzada (asfalto 3D)
                canvas.drawLine(xF, yF, xB, yB, hairpinLineBorderPaint)
                canvas.drawLine(xF, yF, xB, yB, hairpinLinePaint)
            }
        }

        // 8. Puntos de Interés (Insignias circulares)
        if (showPois) {
            for (poi in pois) {
                val relDist = poi.relativeDistance
                if (relDist !in 0.0..totalMetersAhead) continue

                val exactIndexF = distToSampleIdx(relDist, totalMetersAhead, totalMicroSamples)
                val idx = exactIndexF.toInt().coerceIn(0, totalMicroSamples - 2)
                val rem = exactIndexF - idx

                val px = xFront[idx] + rem * (xFront[idx + 1] - xFront[idx])
                val pyTop = yBack[idx] + rem * (yBack[idx + 1] - yBack[idx])

                val badgeRadius = 14f
                val badgeCenterY = pyTop - (h * 0.14f)

                canvas.drawLine(px, pyTop, px, badgeCenterY + badgeRadius, poiLinePaint)
                canvas.drawCircle(px, badgeCenterY, badgeRadius, poiBadgePaint)
                canvas.drawCircle(px, badgeCenterY, badgeRadius, poiBadgeBorderPaint)

                poiBadgeTextPaint.textSize = 12f
                val badgeText = when (poi.type) {
                    PoiType.SUMMIT -> "1"
                    PoiType.VIEWPOINT -> "2"
                    else -> poi.icon
                }
                canvas.drawText(badgeText, px, badgeCenterY + 4f, poiBadgeTextPaint)

                poiLabelPaint.textSize = ((h * 0.038f) * fontScale).coerceIn(9f, 13f)
                canvas.drawText(poi.name, px, badgeCenterY - badgeRadius - 5f, poiLabelPaint)
            }
        }

        if (clipLeftBlocks) canvas.restore()

        // 9. Barra inferior de distancia
        val barH = (h * 0.09f).coerceIn(20f, 32f)
        baseBarRect.set(startX, baseGroundY, baseEndX, baseGroundY + barH)
        canvas.drawRect(baseBarRect, baseBarPaint)
        canvas.drawRect(baseBarRect, baseBarBorderPaint)

        // Corte 3D izquierdo de la barra base
        leftBaseEndcapPath.reset()
        leftBaseEndcapPath.moveTo(startX, baseGroundY)
        leftBaseEndcapPath.lineTo(startX - depth3dX, baseGroundY - depth3dY)
        leftBaseEndcapPath.lineTo(startX - depth3dX, baseGroundY + barH - depth3dY)
        leftBaseEndcapPath.lineTo(startX, baseGroundY + barH)
        leftBaseEndcapPath.close()
        canvas.drawPath(leftBaseEndcapPath, baseBarPaint)
        canvas.drawPath(leftBaseEndcapPath, baseBarBorderPaint)

        // Rótulos de distancia en la barra inferior (Ventana rodante de 50 metros)
        baseDistTextPaint.textSize = (barH * 0.68f).coerceIn(12f, 18f)
        val majorDistMeters = if (profileElevations.isNotEmpty()) majorBlockSizeMeters else (totalMetersAhead / majorBlocksCount)

        for (k in 0 until majorSamples) {
            val m = (windowStartMeters - gridOffsetMeters + (k * majorDistMeters)).toLong()
            val distLabel = if (m < 1000) {
                "${m}m"
            } else {
                if (m % 1000L == 0L) "${m / 1000}km" else "${m / 1000}.${(m % 1000L) / 100}km"
            }
            val px = majorX[k]
            if (px < startX - depth3dX) continue
            canvas.drawText(distLabel, px, baseGroundY + (barH * 0.75f), baseDistTextPaint)
        }

        // Leyenda de escala debajo de la gráfica
        val scaleLegendText = if (majorDistMeters < 1000.0) {
            context.getString(R.string.scale_legend_major_block_m, majorDistMeters.toInt())
        } else {
            val km = majorDistMeters / 1000.0
            val kmStr = if (km % 1.0 == 0.0) km.toInt().toString() else String.format("%.1f", km)
            context.getString(R.string.scale_legend_major_block_km, kmStr)
        }
        // Leyenda de escala debajo de la gráfica (más legible y adaptada a la escala de fuente)
        val scaleLegendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1") // Gris muy claro para máximo contraste
            textSize = ((h * 0.075f) * fontScale).coerceIn(16f, 23f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val subBlockMeters = subBlockSizeMeters.toInt()
        val subScaleLegendText = context.getString(R.string.scale_legend_sub_block, subBlockMeters)
        val subScaleLegendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8") // Gris intermedio nítido
            textSize = ((h * 0.065f) * fontScale).coerceIn(13f, 19f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }

        canvas.drawText(scaleLegendText, w / 2f, h - 23f, scaleLegendPaint)
        canvas.drawText(subScaleLegendText, w / 2f, h - 4f, subScaleLegendPaint)
        
        // Dibujar círculos redondeados para pan left/right
        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44FFFFFF")
            style = Paint.Style.FILL
        }
        val btnStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#88FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f * fontScale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        if (panButtonsPosition != "none") {
            val radius = 28f // Flechas más grandes y fáciles de pulsar (antes 22f)
            val cy = if (panButtonsPosition == "top") 45f else (h - 38f)
            val cxLeft = 50f
            val cxRight = w - 50f
            
            canvas.drawCircle(cxLeft, cy, radius, btnPaint)
            canvas.drawCircle(cxLeft, cy, radius, btnStrokePaint)
            canvas.drawText("<", cxLeft, cy + (arrowPaint.textSize * 0.36f), arrowPaint)
            
            canvas.drawCircle(cxRight, cy, radius, btnPaint)
            canvas.drawCircle(cxRight, cy, radius, btnStrokePaint)
            canvas.drawText(">", cxRight, cy + (arrowPaint.textSize * 0.36f), arrowPaint)
        }

        // 10. Baliza del ciclista en 3D: avanza limpiamente por la cresta frontal de la pendiente
        if (riderProgress >= 0f && riderProgress <= 1f) {
            val progressClamped = riderProgress
            // Con perfil real, la muestra i está a i*subBlockSize metros del inicio de la ventana (incluye bloques extra de margen):
            // se posiciona por distancia, no por fracción de la ventana, para no adelantar la baliza.
            val exactIndexF = if (profileElevations.size >= 2 && subBlocks.isNotEmpty() && subBlockSizeMeters > 0.0) {
                (((progressClamped * lookaheadMeters) + gridOffsetMeters) / subBlockSizeMeters).toFloat().coerceIn(0f, (totalMicroSamples - 1).toFloat())
            } else {
                progressClamped * (totalMicroSamples - 1)
            }
            val idx = exactIndexF.toInt().coerceIn(0, totalMicroSamples - 2)
            val rem = exactIndexF - idx
            val rx = xFront[idx] + rem * (xFront[idx + 1] - xFront[idx])
            val ry = yFront[idx] + rem * (yFront[idx + 1] - yFront[idx])

            // Atenúa suavemente el tramo ya recorrido (a la izquierda de la baliza)
            if (idx > 0) {
                val ryBase = yBase[idx] + rem * (yBase[idx + 1] - yBase[idx])
                riddenDimPath.reset()
                riddenDimPath.moveTo(xFront[0], yFront[0])
                for (i in 1..idx) riddenDimPath.lineTo(xFront[i], yFront[i])
                riddenDimPath.lineTo(rx, ry)
                riddenDimPath.lineTo(rx, ryBase)
                for (i in idx downTo 0) riddenDimPath.lineTo(xFront[i], yBase[i])
                riddenDimPath.close()
                canvas.drawPath(riddenDimPath, riddenDimPaint)
            }
            
            // Haz de luz frontal en Horizonte Isométrico (orientado según la pendiente que sube el ciclista)
        if (altimetriaStyle == AltimetriaStyle.HORIZON_ISOMETRIC) {
            val coneLen = (w * 0.26f)
            val slopeDy = if (idx + 1 < totalMicroSamples) yFront[idx + 1] - yFront[idx] else 0f
            val slopeDx = if (idx + 1 < totalMicroSamples) (xFront[idx + 1] - xFront[idx]).coerceAtLeast(1f) else 1f
            val slopeRatio = (slopeDy / slopeDx).coerceIn(-0.8f, 0.8f)
            val targetMidY = ry + (coneLen * slopeRatio)

            val coneEndTopY = targetMidY - (h * 0.08f)
            val coneEndBottomY = targetMidY + (h * 0.08f)
            headlightPath.reset()
            headlightPath.moveTo(rx, ry)
            headlightPath.lineTo(rx + coneLen, coneEndTopY)
            headlightPath.lineTo(rx + coneLen, coneEndBottomY)
            headlightPath.close()

            val headlightShader = LinearGradient(
                rx, ry, rx + coneLen, targetMidY,
                Color.parseColor("#6038BDF8"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            headlightPaint.shader = headlightShader
            canvas.drawPath(headlightPath, headlightPaint)
        }

        val pinY = ry - 26f
        // Mástil vertical luminoso que conecta la carretera con la baliza
        beaconPaint.strokeWidth = 2.5f
        beaconPaint.style = Paint.Style.STROKE
        canvas.drawLine(rx, ry, rx, pinY, beaconPaint)
        beaconPaint.style = Paint.Style.FILL

        // Halo exterior translúcido de alta visibilidad
        drawBeaconShape(canvas, rx, pinY, 16f, 11f, 6.5f)
        }

        // ELITE FEATURE: Wind Vectors
        windEffectIntensity?.let { wind ->
            val wColor = if (wind < 0) Color.RED else Color.GREEN
            windPaint.color = wColor
            windPaint.alpha = (Math.abs(wind) * 255).toInt().coerceIn(50, 255)
            // Draw wind arrows on the ribbon
            val stepSize = Math.max(1, totalMicroSamples / 10)
            for (i in 0 until totalMicroSamples step stepSize) {
                val px = xFront[i]
                val py = yFront[i] - 30f
                val dx = if (wind < 0) -20f else 20f
                canvas.drawLine(px, py, px + dx, py, windPaint)
                canvas.drawLine(px + dx, py, px + dx - Math.signum(dx)*5f, py - 5f, windPaint)
                canvas.drawLine(px + dx, py, px + dx - Math.signum(dx)*5f, py + 5f, windPaint)
            }
        }

        // ELITE FEATURE: Strava Live Segment & PR Ghost
        if (stravaSegmentDistance != null) {
            val dist = stravaSegmentDistance!!
            // Highlight road
            val segmentStartIdx = ((dist - windowStartMeters) / lookaheadMeters * totalMicroSamples).toInt().coerceIn(0, totalMicroSamples - 1)
            val segmentEndIdx = totalMicroSamples - 1
            if (segmentStartIdx < totalMicroSamples) {
                for (i in segmentStartIdx until segmentEndIdx) {
                    canvas.drawCircle(xFront[i], yFront[i], 3f, stravaPaint)
                }
            }
            
            // Draw PR Ghost
            stravaPrGhostDistance?.let { relDist ->
                if (Math.abs(relDist) < lookaheadMeters) {
                    val gX = startX + ((relDist + windowStartMeters - windowStartMeters) / lookaheadMeters * totalMicroSamples * (totalMicroSamples/w)).toFloat() - panOffsetX
                    val gIdx = ((relDist + windowStartMeters - windowStartMeters) / lookaheadMeters * totalMicroSamples).toInt().coerceIn(0, totalMicroSamples - 1)
                    val gY = yFront[gIdx] - 15f
                    val prTextPaint = Paint(stravaPaint).apply { textSize = 12f; textAlign = Paint.Align.CENTER; color = Color.WHITE }
                    canvas.drawText("KOM", gX, gY - 45f, prTextPaint)
                }
            }
        }

        // W'BAL BURN RATE OVERLAY: línea de fuego y punto de quiebre
        val wbalNow = wbal
        val failMeters = wbalFailureMeters
        if (wbalNow != null && wPrime > 0) {
            val wbalFrac = (wbalNow / wPrime).toFloat().coerceIn(0f, 1f)
            // Color de la línea: verde (lleno) -> naranja -> rojo (agotado)
            val fireColor = when {
                wbalFrac > 0.6f -> Color.parseColor("#22C55E")  // verde
                wbalFrac > 0.3f -> Color.parseColor("#F59E0B")  // ámbar
                else            -> Color.parseColor("#EF4444")  // rojo
            }
            // Barra horizontal bajo el perfil que representa el W'bal restante
            val barY = h - 6f
            val barStartX = startX
            val barEndX = baseEndX
            val barW = barEndX - barStartX
            val wbalBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#33FFFFFF")
                style = Paint.Style.FILL
            }
            canvas.drawRect(barStartX, barY - 4f, barEndX, barY, wbalBarPaint)
            val wbalFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fireColor
                style = Paint.Style.FILL
                alpha = 220
            }
            canvas.drawRect(barStartX, barY - 4f, barStartX + barW * wbalFrac, barY, wbalFillPaint)

            // Etiqueta W'bal
            val wbalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fireColor
                textSize = (h * 0.07f).coerceIn(9f, 14f)
                textAlign = Paint.Align.LEFT
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 0f, 1f, Color.BLACK)
            }
            val wbalPct = (wbalFrac * 100).toInt()
            canvas.drawText("W' ${wbalPct}%", barStartX + 4f, barY - 6f, wbalLabelPaint)

            // Marcador de punto de quiebre si hay déficit activo
            if (failMeters != null && failMeters < lookaheadMeters) {
                val failFrac = (failMeters / lookaheadMeters).toFloat().coerceIn(0f, 1f)
                val failIdx = (failFrac * (totalMicroSamples - 1)).toInt().coerceIn(0, totalMicroSamples - 1)
                val failX = xFront[failIdx]
                val failY = yFront[failIdx]

                val failLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#EF4444")
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
                }
                canvas.drawLine(failX, failY - 30f, failX, yBase[failIdx], failLinePaint)

                val failTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#EF4444")
                    textSize = (h * 0.075f).coerceIn(9f, 13f)
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setShadowLayer(4f, 0f, 2f, Color.BLACK)
                }
                val failKm = String.format("%.1f", failMeters / 1000.0)
                canvas.drawText("⚡ ${failKm}km", failX, failY - 34f, failTextPaint)
            }
        }

        // --- TARGET CADENCE & VIRTUAL GEAR WALL ---
        if (gearWallEnabled && totalMicroSamples > 1) {
            // 1. Detección y renderizado del "Muro de Desarrollo" (Gear Wall)
            // Se considera "muro" donde la pendiente supere el 16% o donde para la velocidad actual
            // la cadencia caiga por debajo de 60 rpm con desarrollo mínimo de escalador (34x34 = ratio 1.0).
            val gearWallHatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFFF3B30")
                strokeWidth = 2.5f
                style = Paint.Style.STROKE
            }
            val gearWallBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#44FF3B30")
                style = Paint.Style.FILL
            }

            var inWall = false
            var wallStartIdx = 0

            for (i in 0 until microGrades.size) {
                val grade = microGrades[i]
                val isWall = grade >= 16.0f

                if (isWall && !inWall) {
                    inWall = true
                    wallStartIdx = i
                } else if (!isWall && inWall) {
                    inWall = false
                    val wStart = wallStartIdx
                    val wEnd = i - 1
                    if (wEnd > wStart) {
                        val wallPoly = Path()
                        wallPoly.moveTo(xFront[wStart], yFront[wStart])
                        for (k in wStart..wEnd) wallPoly.lineTo(xFront[k], yFront[k])
                        for (k in wEnd downTo wStart) wallPoly.lineTo(xFront[k], yBase[k])
                        wallPoly.close()
                        canvas.drawPath(wallPoly, gearWallBgPaint)

                        val stepPx = 10f
                        var hx = xFront[wStart]
                        val limitX = xFront[wEnd]
                        while (hx <= limitX) {
                            val sIdx = ((hx - startX + panOffsetX + gridShiftPx) / microStepX).toInt().coerceIn(0, totalMicroSamples - 1)
                            val topY = yFront[sIdx]
                            val botY = yBase[sIdx]
                            canvas.drawLine(hx, topY, hx + 12f, botY, gearWallHatchPaint)
                            hx += stepPx
                        }

                        val midIdx = (wStart + wEnd) / 2
                        val midX = xFront[midIdx]
                        val midY = yFront[midIdx]
                        val wallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#FFFEE2E2")
                            textSize = (h * 0.075f).coerceIn(8f, 12f)
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            setShadowLayer(5f, 0f, 1f, Color.parseColor("#99000000"))
                        }
                        val wallBadge = try { context.getString(R.string.gear_wall_badge) } catch (_: Exception) { "GEAR WALL" }
                        canvas.drawText("⚠️ $wallBadge", midX, (midY - 12f).coerceAtLeast(18f), wallTextPaint)
                    }
                }
            }
            if (inWall && totalMicroSamples - 1 > wallStartIdx) {
                val wStart = wallStartIdx
                val wEnd = totalMicroSamples - 1
                val wallPoly = Path()
                wallPoly.moveTo(xFront[wStart], yFront[wStart])
                for (k in wStart..wEnd) wallPoly.lineTo(xFront[k], yFront[k])
                for (k in wEnd downTo wStart) wallPoly.lineTo(xFront[k], yBase[k])
                wallPoly.close()
                canvas.drawPath(wallPoly, gearWallBgPaint)

                var hx = xFront[wStart]
                val limitX = xFront[wEnd]
                while (hx <= limitX) {
                    val sIdx = ((hx - startX + panOffsetX + gridShiftPx) / microStepX).toInt().coerceIn(0, totalMicroSamples - 1)
                    val topY = yFront[sIdx]
                    val botY = yBase[sIdx]
                    canvas.drawLine(hx, topY, hx + 12f, botY, gearWallHatchPaint)
                    hx += 10f
                }
            }

            // 2. Telemetría de Marcha Recomendada y Proyección de Desarrollo en el eje inferior
            val targetCadence = if (currentCadence > 40.0) currentCadence else 75.0
            val speedKmh = (currentSpeedMs * 3.6).coerceAtLeast(3.0)
            val gearIdxF = if (riderProgress in 0f..1f) {
                if (profileElevations.size >= 2 && subBlocks.isNotEmpty() && subBlockSizeMeters > 0.0)
                    (((riderProgress * lookaheadMeters) + gridOffsetMeters) / subBlockSizeMeters).toFloat()
                else riderProgress * (totalMicroSamples - 1)
            } else 0f
            val gearIdx = gearIdxF.toInt().coerceIn(0, (microGrades.size - 1).coerceAtLeast(0))
            val gearSpan = if (subBlockSizeMeters > 0.0) (50.0 / subBlockSizeMeters).toInt().coerceAtLeast(1) else 1
            val gearEnd = (gearIdx + gearSpan).coerceAtMost(microGrades.size)
            val avgNextGrade = if (microGrades.isEmpty()) 0f else (gearIdx until gearEnd).map { microGrades[it] }.average().toFloat()
            if (showGearBadge) {
            val climbGrade = climbGearGrade
            val recSprocket = GearAdvisor.recommend(climbGrade ?: avgNextGrade.toDouble(), gearChainrings, gearCassette)

            val gearBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CC111827")
                style = Paint.Style.FILL
            }
            val gearBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4438BDF8")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val gearTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                textSize = ((h * 0.075f) * fontScale).coerceIn(18f, 28f)
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 0f, 1f, Color.BLACK)
            }

            // Hueco entre la leyenda de bloques (centro) y la flecha de avance (derecha)
            val legendHalf = maxOf(scaleLegendPaint.measureText(scaleLegendText), subScaleLegendPaint.measureText(subScaleLegendText)) / 2f
            val gapL = w / 2f + legendHalf + 8f
            val arrowsBottom = panButtonsPosition == "bottom"
            val gapR = if (arrowsBottom) w - 50f - 28f - 8f else w - 8f
            val gearLabel = if (climbGrade != null) "Rec. ⚙ $recSprocket" else "⚙ $recSprocket"
            val gearMaxW = if (rotate90) (gapR - gapL) else (w - 40f)
            while (gearTextPaint.measureText(gearLabel) + 28f > gearMaxW && gearTextPaint.textSize > 20f) {
                gearTextPaint.textSize -= 1f
            }
            if (rotate90) gearTextPaint.textSize = gearTextPaint.textSize * 0.62f + 1f
            val tb = android.graphics.Rect()
            gearTextPaint.getTextBounds(gearLabel, 0, gearLabel.length, tb)
            val textW = gearTextPaint.measureText(gearLabel)
            val padX = 14f
            val padY = 10f
            val badgeW = textW + padX * 2f
            val badgeH = tb.height() + padY * 2f
            // Vertical (no apaisado): el engranaje va encima de la leyenda de bloques
            val badgeCx = if (rotate90) (gapL + gapR) / 2f else w / 2f
            val badgeCy = if (!rotate90) h - 23f - scaleLegendPaint.textSize - badgeH / 2f - 2f
                else if (arrowsBottom) h - 24f else h - 12f
            val badgeL = badgeCx - badgeW / 2f
            val badgeT = badgeCy - badgeH / 2f
            // El texto se centra por sus límites reales para que el rectángulo lo envuelva con margen uniforme
            val baseline = badgeCy - (tb.top + tb.bottom) / 2f
            gearBorderPaint.strokeWidth = 2f
            canvas.drawRoundRect(badgeL, badgeT, badgeL + badgeW, badgeT + badgeH, 10f, 10f, gearBadgePaint)
            canvas.drawRoundRect(badgeL, badgeT, badgeL + badgeW, badgeT + badgeH, 10f, 10f, gearBorderPaint)
            canvas.drawText(gearLabel, badgeCx, baseline, gearTextPaint)
            }
        }

        // Meta / Final de ruta: Bandera de cuadros / Línea de meta en el último bloque
        if (routeTotalLength > 0.0) {
            val finishDistInWindow = routeTotalLength - windowStartMeters
            if (finishDistInWindow in 0.0..(totalMetersAhead + 1.0)) {
                val exactIndexF = distToSampleIdx(finishDistInWindow, totalMetersAhead, totalMicroSamples)
                val fIdx = exactIndexF.toInt().coerceIn(0, totalMicroSamples - 2)
                val rem = exactIndexF - fIdx
                val fxFront = xFront[fIdx] + rem * (xFront[fIdx + 1] - xFront[fIdx])
                val fyFront = yFront[fIdx] + rem * (yFront[fIdx + 1] - yFront[fIdx])
                val fxBack = xBack[fIdx] + rem * (xBack[fIdx + 1] - xBack[fIdx])
                val fyBack = yBack[fIdx] + rem * (yBack[fIdx + 1] - yBack[fIdx])
                val fyBase = yBase[fIdx] + rem * (yBase[fIdx + 1] - yBase[fIdx])

                // Línea vertical brillante en la pared
                val finishWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(fxFront, fyFront, fxFront, fyBase, finishWallPaint)

                // Cinta de meta ajedrezada (checkered pattern) sobre el asfalto 3D
                val steps = 6
                for (s in 0 until steps) {
                    val t1 = s / steps.toFloat()
                    val t2 = (s + 1) / steps.toFloat()
                    val p1x = fxFront + t1 * (fxBack - fxFront)
                    val p1y = fyFront + t1 * (fyBack - fyFront)
                    val p2x = fxFront + t2 * (fxBack - fxFront)
                    val p2y = fyFront + t2 * (fyBack - fyFront)

                    val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (s % 2 == 0) Color.WHITE else Color.BLACK
                        strokeWidth = 4f
                        style = Paint.Style.STROKE
                    }
                    canvas.drawLine(p1x, p1y, p2x, p2y, checkPaint)
                }

                // Pequeño banderín / icono de meta o texto FIN sobre la cinta
                val flagPolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    strokeWidth = 2f
                }
                val flagPoleTopY = fyBack - 46f
                canvas.drawLine(fxBack, fyBack, fxBack, flagPoleTopY, flagPolePaint)

                // Bandera de cuadros 4x3
                val cell = 7f
                val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                for (r in 0 until 3) {
                    for (c in 0 until 4) {
                        cellPaint.color = if ((r + c) % 2 == 0) Color.WHITE else Color.BLACK
                        val cl = fxBack + 1f + c * cell
                        val ct = flagPoleTopY + r * cell
                        canvas.drawRect(cl, ct, cl + cell, ct + cell, cellPaint)
                    }
                }
                val flagBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(fxBack + 1f, flagPoleTopY, fxBack + 1f + 4 * cell, flagPoleTopY + 3 * cell, flagBorderPaint)
            }
        }
    }

    // --- MÉTODOS DE RENDERIZADO PARA LOS 5 MODELOS DE ALTIMETRÍA REVOLUCIONARIOS ---

    private fun drawClassicRibbon(
        canvas: Canvas,
        totalMicroSamples: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        xBack: FloatArray,
        yBack: FloatArray,
        yBase: FloatArray,
        depth3dY: Float
    ) {
        for (i in 0 until totalMicroSamples - 1) {
            val grade = if (i < microGrades.size) microGrades[i] else 4.0f
            val baseColor = Color.parseColor(getGradeColor(grade.toDouble()))
            val darkColor = getGradeColorDarkShaded(grade.toDouble())

            wallPath.reset()
            wallPath.moveTo(xFront[i], yFront[i])
            wallPath.lineTo(xFront[i + 1], yFront[i + 1])
            wallPath.lineTo(xFront[i + 1], yBase[i + 1])
            wallPath.lineTo(xFront[i], yBase[i])
            wallPath.close()

            val wallShader = LinearGradient(
                xFront[i], yFront[i],
                xFront[i], yBase[i],
                baseColor, darkColor,
                Shader.TileMode.CLAMP
            )
            wallPaint.shader = wallShader
            canvas.drawPath(wallPath, wallPaint)
            val paint = if (i % currentMicroSubDivisions == 0) majorSliceSeparatorPaint else sliceSeparatorPaint
            if (!isMergedJoint(i)) canvas.drawLine(xFront[i], yFront[i], xFront[i], yBase[i], paint)
        }
        val lastIdx = totalMicroSamples - 1
        val lastPaint = if (lastIdx % currentMicroSubDivisions == 0) majorSliceSeparatorPaint else sliceSeparatorPaint
        canvas.drawLine(xFront[lastIdx], yFront[lastIdx], xFront[lastIdx], yBase[lastIdx], lastPaint)

        val startGrade = if (microGrades.isNotEmpty()) microGrades[0].toDouble() else 4.0
        leftEndcapPath.reset()
        leftEndcapPath.moveTo(xBack[0], yBack[0])
        leftEndcapPath.lineTo(xFront[0], yFront[0])
        leftEndcapPath.lineTo(xFront[0], yBase[0])
        leftEndcapPath.lineTo(xBack[0], yBase[0] - depth3dY)
        leftEndcapPath.close()
        leftEndcapPaint.color = getGradeColorDarkShaded(startGrade)
        canvas.drawPath(leftEndcapPath, leftEndcapPaint)
        canvas.drawPath(leftEndcapPath, leftEndcapBorderPaint)

        for (i in 0 until totalMicroSamples - 1) {
            val grade = if (i < microGrades.size) microGrades[i] else 4.0f
            val topColor = getGradeColorTopFacet(grade.toDouble())

            ribbonQuadPath.reset()
            ribbonQuadPath.moveTo(xFront[i], yFront[i])
            ribbonQuadPath.lineTo(xBack[i], yBack[i])
            ribbonQuadPath.lineTo(xBack[i + 1], yBack[i + 1])
            ribbonQuadPath.lineTo(xFront[i + 1], yFront[i + 1])
            ribbonQuadPath.close()

            ribbonSurfacePaint.color = topColor
            canvas.drawPath(ribbonQuadPath, ribbonSurfacePaint)
            if (showSurfaceTextures) {
                val surfaceType = (i / 10) % 3
                if (surfaceType == 1) {
                    canvas.drawLine(xFront[i], yFront[i], xBack[i], yBack[i], gravelLinePaint)
                } else if (surfaceType == 2) {
                    canvas.drawLine(xFront[i], yFront[i], xBack[i + 1], yBack[i + 1], trailLinePaint)
                    canvas.drawLine(xFront[i + 1], yFront[i + 1], xBack[i], yBack[i], trailLinePaint)
                }
            }
            canvas.drawLine(xBack[i], yBack[i], xBack[i + 1], yBack[i + 1], ribbonBackLinePaint)
            canvas.drawLine(xFront[i], yFront[i], xFront[i + 1], yFront[i + 1], ribbonFrontLinePaint)
        }
    }

    private fun drawHorizonIsometricRibbon(
        canvas: Canvas,
        w: Float,
        h: Float,
        totalMicroSamples: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        xBack: FloatArray,
        yBack: FloatArray,
        yBase: FloatArray
    ) {
        // Pared frontal con perspectiva isométrica que converge al horizonte
        for (i in 0 until totalMicroSamples - 1) {
            val grade = if (i < microGrades.size) microGrades[i] else 4.0f
            val baseColor = Color.parseColor(getGradeColor(grade.toDouble()))
            val darkColor = getGradeColorDarkShaded(grade.toDouble())

            wallPath.reset()
            wallPath.moveTo(xFront[i], yFront[i])
            wallPath.lineTo(xFront[i + 1], yFront[i + 1])
            wallPath.lineTo(xFront[i + 1], yBase[i + 1])
            wallPath.lineTo(xFront[i], yBase[i])
            wallPath.close()

            val wallShader = LinearGradient(
                xFront[i], yFront[i],
                xFront[i], yBase[i],
                baseColor, darkColor,
                Shader.TileMode.CLAMP
            )
            wallPaint.shader = wallShader
            canvas.drawPath(wallPath, wallPaint)
            val paint = if (i % currentMicroSubDivisions == 0) majorSliceSeparatorPaint else sliceSeparatorPaint
            if (!isMergedJoint(i)) canvas.drawLine(xFront[i], yFront[i], xFront[i], yBase[i], paint)
        }
        val lastIdx = totalMicroSamples - 1
        val lastPaint = if (lastIdx % currentMicroSubDivisions == 0) majorSliceSeparatorPaint else sliceSeparatorPaint
        canvas.drawLine(xFront[lastIdx], yFront[lastIdx], xFront[lastIdx], yBase[lastIdx], lastPaint)

        // Corte 3D izquierdo isométrico
        val pFactor0 = 1.0f
        val isoX0 = xFront[0] - (w * 0.035f * pFactor0)
        val isoY0 = yFront[0] - (h * 0.030f * pFactor0)
        val startGrade = if (microGrades.isNotEmpty()) microGrades[0].toDouble() else 4.0

        leftEndcapPath.reset()
        leftEndcapPath.moveTo(isoX0, isoY0)
        leftEndcapPath.lineTo(xFront[0], yFront[0])
        leftEndcapPath.lineTo(xFront[0], yBase[0])
        leftEndcapPath.lineTo(isoX0, yBase[0] - (h * 0.030f * pFactor0))
        leftEndcapPath.close()
        leftEndcapPaint.color = getGradeColorDarkShaded(startGrade)
        canvas.drawPath(leftEndcapPath, leftEndcapPaint)
        canvas.drawPath(leftEndcapPath, leftEndcapBorderPaint)

        // Calzada superior isométrica estilizada tipo pista de despegue hacia el cielo
        for (i in 0 until totalMicroSamples - 1) {
            val grade = if (i < microGrades.size) microGrades[i] else 4.0f
            val topColor = getGradeColorTopFacet(grade.toDouble())

            val pFactor = 1.0f - ((i.toFloat() / (totalMicroSamples - 1).coerceAtLeast(1)) * 0.45f)
            val nextPFactor = 1.0f - (((i + 1).toFloat() / (totalMicroSamples - 1).coerceAtLeast(1)) * 0.45f)
            val isoX1 = xFront[i] - (w * 0.035f * pFactor)
            val isoY1 = yFront[i] - (h * 0.030f * pFactor)
            val isoX2 = xFront[i + 1] - (w * 0.035f * nextPFactor)
            val isoY2 = yFront[i + 1] - (h * 0.030f * nextPFactor)

            ribbonQuadPath.reset()
            ribbonQuadPath.moveTo(xFront[i], yFront[i])
            ribbonQuadPath.lineTo(isoX1, isoY1)
            ribbonQuadPath.lineTo(isoX2, isoY2)
            ribbonQuadPath.lineTo(xFront[i + 1], yFront[i + 1])
            ribbonQuadPath.close()

            ribbonSurfacePaint.color = topColor
            canvas.drawPath(ribbonQuadPath, ribbonSurfacePaint)
            if (showSurfaceTextures) {
                val surfaceType = (i / 10) % 3
                if (surfaceType == 1) {
                    canvas.drawLine(xFront[i], yFront[i], xBack[i], yBack[i], gravelLinePaint)
                } else if (surfaceType == 2) {
                    canvas.drawLine(xFront[i], yFront[i], xBack[i + 1], yBack[i + 1], trailLinePaint)
                    canvas.drawLine(xFront[i + 1], yFront[i + 1], xBack[i], yBack[i], trailLinePaint)
                }
            }
            canvas.drawLine(isoX1, isoY1, isoX2, isoY2, ribbonBackLinePaint)

            // Cresta de horizonte azul hielo nítida
            canvas.drawLine(xFront[i], yFront[i], xFront[i + 1], yFront[i + 1], isoRidgePaint)

            // Línea de carril central tipo pista
            val midFrontX = (xFront[i] + xFront[i + 1]) / 2f
            val midFrontY = (yFront[i] + yFront[i + 1]) / 2f
            val midIsoX = (isoX1 + isoX2) / 2f
            val midIsoY = (isoY1 + isoY2) / 2f
            val laneMidX = (midFrontX + midIsoX) / 2f
            val laneMidY = (midFrontY + midIsoY) / 2f
            if (i % 2 == 0) {
                canvas.drawCircle(laneMidX, laneMidY, 2.0f, beaconCorePaint)
            }
        }
    }

    private fun drawTacticalOasesRibbon(
        canvas: Canvas,
        w: Float,
        h: Float,
        totalMicroSamples: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        xBack: FloatArray,
        yBack: FloatArray,
        yBase: FloatArray,
        depth3dY: Float
    ) {
        // Dibuja el perfil base clásico
        drawClassicRibbon(canvas, totalMicroSamples, microGrades, xFront, yFront, xBack, yBack, yBase, depth3dY)

        // 1. Resaltado de Oasis de Oxígeno (tramos <= 3% para recuperación)
        var oasisStart = -1
        for (i in 0 until totalMicroSamples - 1) {
            val g = if (i < microGrades.size) microGrades[i] else 4.0f
            if (g <= 3.0f) {
                if (oasisStart == -1) oasisStart = i
            } else {
                if (oasisStart != -1) {
                    drawOasisShelf(canvas, oasisStart, i, xFront, yFront, yBase, subBlockSizeMeters)
                    oasisStart = -1
                }
            }
        }
        if (oasisStart != -1) {
            drawOasisShelf(canvas, oasisStart, totalMicroSamples - 1, xFront, yFront, yBase, subBlockSizeMeters)
        }

        // 2. Resaltado de Crisoles de Fuego agrupados (rampas duras >= 12%)
        var crucibleStart = -1
        for (i in 0 until totalMicroSamples - 1) {
            val g = if (i < microGrades.size) microGrades[i] else 4.0f
            if (g >= 12.0f) {
                if (crucibleStart == -1) crucibleStart = i
            } else {
                if (crucibleStart != -1) {
                    drawCrucibleFire(canvas, crucibleStart, i, microGrades, xFront, yFront, yBase, subBlockSizeMeters)
                    crucibleStart = -1
                }
            }
        }
        if (crucibleStart != -1) {
            drawCrucibleFire(canvas, crucibleStart, totalMicroSamples - 1, microGrades, xFront, yFront, yBase, subBlockSizeMeters)
        }

        // 3. Bruma de hipoxia si altitud >= 1400m
        if (currentElevation >= 1400.0) {
            val hazeShader = LinearGradient(
                0f, 0f, 0f, h * 0.30f,
                Color.parseColor("#4594A3B8"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            hypoxiaHazePaint.shader = hazeShader
            canvas.drawRect(0f, 0f, w, h * 0.30f, hypoxiaHazePaint)
        }
    }

    private fun drawOasisShelf(
        canvas: Canvas,
        startIdx: Int,
        endIdx: Int,
        xFront: FloatArray,
        yFront: FloatArray,
        yBase: FloatArray,
        subBlockSize: Double
    ) {
        val x1 = xFront[startIdx]
        val x2 = xFront[endIdx]
        val midX = (x1 + x2) / 2f
        val topY = (yFront[startIdx] + yFront[endIdx]) / 2f

        oasisShelfPath.reset()
        oasisShelfPath.moveTo(xFront[startIdx], yFront[startIdx])
        for (k in startIdx + 1..endIdx) {
            oasisShelfPath.lineTo(xFront[k], yFront[k])
        }
        oasisShelfPath.lineTo(xFront[endIdx], yBase[endIdx])
        oasisShelfPath.lineTo(xFront[startIdx], yBase[startIdx])
        oasisShelfPath.close()

        val glowShader = LinearGradient(
            midX, topY,
            midX, yBase[startIdx],
            Color.parseColor("#750284C7"),
            Color.parseColor("#100284C7"),
            Shader.TileMode.CLAMP
        )
        oasisGlowPaint.shader = glowShader
        canvas.drawPath(oasisShelfPath, oasisGlowPaint)

        // Cresta iluminada en cian hielo siguiendo todos los vértices reales
        for (k in startIdx until endIdx) {
            canvas.drawLine(xFront[k], yFront[k], xFront[k + 1], yFront[k + 1], shelfRidgePaint)
        }

        // Rótulo táctico del descansillo si hay anchura
        val shelfW = abs(x2 - x1)
        if (shelfW >= 36f) {
            val distMeters = ((endIdx - startIdx) * subBlockSize).toInt()
            val label = if (shelfW >= 60f) "OASIS ${distMeters}m" else "OASIS"
            percentTextPaint.textSize = (shelfW * 0.22f * fontScale).coerceIn(11f, 15f)
            val textW = percentTextPaint.measureText(label)
            val badgeRect = RectF(
                midX - (textW / 2f) - 6f,
                topY - 20f,
                midX + (textW / 2f) + 6f,
                topY - 2f
            )
            canvas.drawRoundRect(badgeRect, 6f, 6f, oasisBadgeBgPaint)
            canvas.drawRoundRect(badgeRect, 6f, 6f, oasisBadgeBorderPaint)
            canvas.drawText(label, midX, topY - 7f, percentTextPaint)
        }
    }

    private fun drawCrucibleFire(
        canvas: Canvas,
        startIdx: Int,
        endIdx: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        yBase: FloatArray,
        subBlockSize: Double
    ) {
        val x1 = xFront[startIdx]
        val x2 = xFront[endIdx]
        val midX = (x1 + x2) / 2f
        val topY = (yFront[startIdx] + yFront[endIdx]) / 2f

        cruciblePath.reset()
        cruciblePath.moveTo(xFront[startIdx], yFront[startIdx])
        for (k in startIdx + 1..endIdx) {
            cruciblePath.lineTo(xFront[k], yFront[k])
        }
        cruciblePath.lineTo(xFront[endIdx], yBase[endIdx])
        cruciblePath.lineTo(xFront[startIdx], yBase[startIdx])
        cruciblePath.close()

        val fireShader = LinearGradient(
            midX, topY,
            midX, yBase[startIdx],
            Color.parseColor("#90DC2626"),
            Color.parseColor("#15DC2626"),
            Shader.TileMode.CLAMP
        )
        oasisGlowPaint.shader = fireShader
        canvas.drawPath(cruciblePath, oasisGlowPaint)

        // Cresta de fuego carmesí siguiendo el contorno exacto
        for (k in startIdx until endIdx) {
            canvas.drawLine(xFront[k], yFront[k], xFront[k + 1], yFront[k + 1], crucibleRidgePaint)
        }

        // Insignia táctica de Muro
        val crucibleW = abs(x2 - x1)
        if (crucibleW >= 36f) {
            val distMeters = ((endIdx - startIdx) * subBlockSize).toInt()
            var maxG = 0f
            for (k in startIdx until endIdx) {
                if (k < microGrades.size && microGrades[k] > maxG) maxG = microGrades[k]
            }
            val label = if (crucibleW >= 60f) "MURO ${distMeters}m" else "${maxG.roundToInt()}%"
            percentTextPaint.textSize = (crucibleW * 0.22f * fontScale).coerceIn(11f, 15f)
            val textW = percentTextPaint.measureText(label)
            val badgeRect = RectF(
                midX - (textW / 2f) - 6f,
                topY - 20f,
                midX + (textW / 2f) + 6f,
                topY - 2f
            )
            canvas.drawRoundRect(badgeRect, 6f, 6f, crucibleBadgeBgPaint)
            canvas.drawRoundRect(badgeRect, 6f, 6f, crucibleBadgeBorderPaint)
            canvas.drawText(label, midX, topY - 7f, percentTextPaint)
        }
    }

    private fun drawDynamicForceFieldRibbon(
        canvas: Canvas,
        w: Float,
        h: Float,
        totalMicroSamples: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        xBack: FloatArray,
        yBack: FloatArray,
        yBase: FloatArray,
        depth3dY: Float,
        startX: Float,
        baseEndX: Float,
        baseGroundY: Float
    ) {
        // Dibuja el perfil base clásico
        drawClassicRibbon(canvas, totalMicroSamples, microGrades, xFront, yFront, xBack, yBack, yBase, depth3dY)

        // Líneas de tensión gravitatoria ancladas exactamente a la superficie inclinada
        for (i in 0 until totalMicroSamples - 1) {
            val g = if (i < microGrades.size) microGrades[i] else 4.0f
            if (g >= 8.0f) {
                val stepCount = 3
                val colW = xFront[i + 1] - xFront[i]
                for (s in 1..stepCount) {
                    val ratio = s.toFloat() / (stepCount + 1)
                    val tx = xFront[i] + (ratio * colW)
                    val ty = yFront[i] + (ratio * (yFront[i + 1] - yFront[i])) + 2f
                    canvas.drawLine(tx, ty, tx, yBase[i] - 2f, tensionPaint)
                }
            }
        }

        // Onda cinética de inercia a lo largo de la base (baseGroundY)
        inertiaWavePath.reset()
        val waveAmp = (h * 0.022f)
        val isHighResistance = currentGrade >= 9.0
        inertiaWavePaint.color = if (isHighResistance) Color.parseColor("#EF4444") else Color.parseColor("#38BDF8")

        val waveFrequency = if (isHighResistance) 22f else 14f
        for (step in 0..50) {
            val ratio = step / 50f
            val px = startX + (ratio * (baseEndX - startX))
            val py = baseGroundY + (sin((ratio * waveFrequency) + (riderProgress * 14f)) * waveAmp)
            if (step == 0) inertiaWavePath.moveTo(px, py) else inertiaWavePath.lineTo(px, py)
        }
        canvas.drawPath(inertiaWavePath, inertiaWavePaint)

        // Línea guía de ritmo ascensional (VAM) flotando suavemente sobre el perfil
        pacingPath.reset()
        for (i in 0 until totalMicroSamples) {
            val px = xFront[i]
            val py = yFront[i] - 12f
            if (i == 0) pacingPath.moveTo(px, py) else pacingPath.lineTo(px, py)
        }
        canvas.drawPath(pacingPath, pacingLinePaint)
    }

    private fun drawMonolithicObsidianRibbon(
        canvas: Canvas,
        w: Float,
        h: Float,
        totalMicroSamples: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        xBack: FloatArray,
        yBack: FloatArray,
        yBase: FloatArray,
        startX: Float,
        baseEndX: Float,
        baseGroundY: Float,
        maxPeakHeight: Float
    ) {
        // 1. Cuerpo de montaña en cristal de obsidiana ahumada profunda
        val obsidianShader = LinearGradient(
            startX, baseGroundY - maxPeakHeight,
            startX, baseGroundY,
            Color.parseColor("#151D2C"),
            Color.parseColor("#04070D"),
            Shader.TileMode.CLAMP
        )
        wallPaint.shader = obsidianShader

        for (i in 0 until totalMicroSamples - 1) {
            wallPath.reset()
            wallPath.moveTo(xFront[i], yFront[i])
            wallPath.lineTo(xFront[i + 1], yFront[i + 1])
            wallPath.lineTo(xFront[i + 1], yBase[i + 1])
            wallPath.lineTo(xFront[i], yBase[i])
            wallPath.close()
            canvas.drawPath(wallPath, wallPaint)

            // Facetas de corte de diamante obsidian
            if (i % 2 == 0) {
                canvas.drawLine(xFront[i], yFront[i], xFront[i] + (w * 0.04f), yBase[i], obsidianFacetPaint)
            }
            val paint = if (i % currentMicroSubDivisions == 0) majorSliceSeparatorPaint else sliceSeparatorPaint
            if (!isMergedJoint(i)) canvas.drawLine(xFront[i], yFront[i], xFront[i], yBase[i], paint)
        }
        val lastIdx = totalMicroSamples - 1
        val lastPaint = if (lastIdx % currentMicroSubDivisions == 0) majorSliceSeparatorPaint else sliceSeparatorPaint
        canvas.drawLine(xFront[lastIdx], yFront[lastIdx], xFront[lastIdx], yBase[lastIdx], lastPaint)

        // 2. Faceta superior 3D en cristal pulido oscuro con resplandor
        for (i in 0 until totalMicroSamples - 1) {
            ribbonQuadPath.reset()
            ribbonQuadPath.moveTo(xFront[i], yFront[i])
            ribbonQuadPath.lineTo(xBack[i], yBack[i])
            ribbonQuadPath.lineTo(xBack[i + 1], yBack[i + 1])
            ribbonQuadPath.lineTo(xFront[i + 1], yFront[i + 1])
            ribbonQuadPath.close()

            ribbonSurfacePaint.color = Color.parseColor("#0A1120")
            canvas.drawPath(ribbonQuadPath, ribbonSurfacePaint)
            if (showSurfaceTextures) {
                val surfaceType = (i / 10) % 3
                if (surfaceType == 1) {
                    canvas.drawLine(xFront[i], yFront[i], xBack[i], yBack[i], gravelLinePaint)
                } else if (surfaceType == 2) {
                    canvas.drawLine(xFront[i], yFront[i], xBack[i + 1], yBack[i + 1], trailLinePaint)
                    canvas.drawLine(xFront[i + 1], yFront[i + 1], xBack[i], yBack[i], trailLinePaint)
                }
            }
            canvas.drawLine(xBack[i], yBack[i], xBack[i + 1], yBack[i + 1], obsidianFacetPaint)
        }

        // 3. Núcleo interior de plasma radiante con el degradado de color oficial (estrictamente acotado)
        for (i in 0 until totalMicroSamples - 1) {
            val g = if (i < microGrades.size) microGrades[i] else 4.0f
            val baseColor = Color.parseColor(getGradeColor(g.toDouble()))
            val plasmaColor = Color.argb(165, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

            val coreTopY1 = (yFront[i] + 8f).coerceAtMost(yBase[i] - 4f)
            val coreBottomY1 = (yFront[i] + 24f).coerceAtMost(yBase[i] - 1f)
            val coreTopY2 = (yFront[i + 1] + 8f).coerceAtMost(yBase[i + 1] - 4f)
            val coreBottomY2 = (yFront[i + 1] + 24f).coerceAtMost(yBase[i + 1] - 1f)

            if (coreBottomY1 > coreTopY1 && coreBottomY2 > coreTopY2) {
                plasmaCorePath.reset()
                plasmaCorePath.moveTo(xFront[i], coreTopY1)
                plasmaCorePath.lineTo(xFront[i + 1], coreTopY2)
                plasmaCorePath.lineTo(xFront[i + 1], coreBottomY2)
                plasmaCorePath.lineTo(xFront[i], coreBottomY1)
                plasmaCorePath.close()

                plasmaCorePaint.color = plasmaColor
                canvas.drawPath(plasmaCorePath, plasmaCorePaint)
            }
        }

        // 4. Cresta superior en haz láser blanco nítido con resplandor neón
        for (i in 0 until totalMicroSamples - 1) {
            canvas.drawLine(xFront[i], yFront[i], xFront[i + 1], yFront[i + 1], neonGlowPaint)
            canvas.drawLine(xFront[i], yFront[i], xFront[i + 1], yFront[i + 1], neonCorePaint)
        }
    }

    private fun isMergedJoint(i: Int): Boolean = i < mergedJoint.size && mergedJoint[i]

    // Rango de pendientes sin etiqueta de porcentaje (opción del usuario); los colores no cambian
    private fun isGradeLabelHidden(grade: Float): Boolean =
        hideGradeRangeEnabled && grade >= minOf(hideGradeMin, hideGradeMax) && grade <= maxOf(hideGradeMin, hideGradeMax)

    // Índice (fraccionario) de muestra del perfil para una distancia relativa al inicio de la ventana
    private fun distToSampleIdx(relDist: Double, totalMetersAhead: Double, totalMicroSamples: Int): Float {
        val maxIdx = (totalMicroSamples - 1).toFloat()
        return if (profileElevations.size >= 2 && subBlocks.isNotEmpty() && subBlockSizeMeters > 0.0) {
            ((relDist + gridOffsetMeters) / subBlockSizeMeters).toFloat().coerceIn(0f, maxIdx)
        } else {
            ((relDist / totalMetersAhead).toFloat() * maxIdx).coerceIn(0f, maxIdx)
        }
    }

    private fun getGradeColor(grade: Double): String {
        return GradeColorScale.getColorHex(grade)
    }

    private fun getGradeColorDarkShaded(grade: Double): Int {
        if (grade > 20.0) return Color.parseColor("#05070D")
        val baseColor = Color.parseColor(getGradeColor(grade))
        val r = (Color.red(baseColor) * 0.65f).toInt()
        val g = (Color.green(baseColor) * 0.65f).toInt()
        val b = (Color.blue(baseColor) * 0.65f).toInt()
        return Color.rgb(r, g, b)
    }

    private fun getGradeColorTopFacet(grade: Double): Int {
        if (grade > 20.0) return Color.parseColor("#1E293B")
        val baseColor = Color.parseColor(getGradeColor(grade))
        val r = (Color.red(baseColor) * 0.82f).toInt()
        val g = (Color.green(baseColor) * 0.82f).toInt()
        val b = (Color.blue(baseColor) * 0.82f).toInt()
        return Color.rgb(r, g, b)
    }

    private data class IsoSegment(
        val idx: Int,
        val x1: Float, val y1: Float, val z1: Float,
        val x2: Float, val y2: Float, val z2: Float,
        val grade: Float,
        val km: Float
    )

    private fun projectIso(x: Float, y: Float, z: Float): Pair<Float, Float> {
        val isoX = (x - y) * 0.866025f // cos(30)
        val isoY = (x + y) * 0.5f - z // sin(30)
        return Pair(isoX, isoY)
    }

    private fun drawGlobalIsometricRibbon(canvas: Canvas, w: Float, h: Float) {
        val n = subBlocks.size
        if (n < 2) return

        // Cuando el mapa NO está anclado al norte (heading-up), el ciclista mira siempre
        // "hacia arriba" en pantalla, así que bajamos la ruta hacia el centro inferior
        // para dejar el máximo espacio libre arriba y que la vista sea más clara.
        val gridW = w * 0.6f
        val gridH = gridW
        val centerX = w / 2f
        val centerY = when {
            isHeadingUp && rotate90 -> h * 0.38f
            isHeadingUp -> h * 0.68f
            rotate90 -> h * 0.3f
            !showHeaderStats -> h * 0.50f
            else -> h * 0.55f // Move down significantly in portrait to avoid ClimbViewer header
        }

        // Draw Base Grid
        val gridPaint = Paint().apply {
            color = Color.parseColor("#33FFFFFF")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val gridLines = 10
        for (i in 0..gridLines) {
            val t = i.toFloat() / gridLines
            // Line along X
            val (p1x, p1y) = projectIso(0f, t * gridH, 0f)
            val (p2x, p2y) = projectIso(gridW, t * gridH, 0f)
            canvas.drawLine(p1x + centerX, p1y + centerY, p2x + centerX, p2y + centerY, gridPaint)
            // Line along Y
            val (p3x, p3y) = projectIso(t * gridW, 0f, 0f)
            val (p4x, p4y) = projectIso(t * gridW, gridH, 0f)
            canvas.drawLine(p3x + centerX, p3y + centerY, p4x + centerX, p4y + centerY, gridPaint)
        }

        // Calculate Elevations
        val elevations = FloatArray(n)
        var currentElev = 0f
        for (i in 0 until n) {
            elevations[i] = currentElev
            currentElev += (subBlocks[i] / 100f) * subBlockSizeMeters.toFloat()
        }
        val maxElev = elevations.maxOrNull() ?: 1f
        val minElev = elevations.minOrNull() ?: 0f
        val zScale = (h * 0.25f) / (maxElev - minElev).coerceAtLeast(1f)
        for (i in 0 until n) {
            elevations[i] = (elevations[i] - minElev) * zScale
        }

        // Generate GPS Path
        val path = Array(n) { Pair(0f, 0f) }
        
        if (routeCoords.isNotEmpty()) {
            val minLat = routeCoords.minOf { it.first }
            val maxLat = routeCoords.maxOf { it.first }
            val minLng = routeCoords.minOf { it.second }
            val maxLng = routeCoords.maxOf { it.second }
            
            val avgLatRad = Math.toRadians((minLat + maxLat) / 2.0)
            
            // Convert to relative metric-proportional grid
            val rawPointsMetric = routeCoords.map { 
                val mx = (it.second - minLng) * Math.cos(avgLatRad)
                val my = (maxLat - it.first) // Invert so North is Top
                Pair(mx, my)
            }
            
            val maxMx = rawPointsMetric.maxOf { it.first }
            val maxMy = rawPointsMetric.maxOf { it.second }
            val cx = maxMx / 2.0
            val cy = maxMy / 2.0
            
            val rad = Math.toRadians(mapRotationAngle)
            val cosA = Math.cos(rad)
            val sinA = Math.sin(rad)
            
            val pointsMetric = rawPointsMetric.map {
                val dx = it.first - cx
                val dy = it.second - cy
                val rx = dx * cosA - dy * sinA
                val ry = dx * sinA + dy * cosA
                Pair(rx, ry)
            }
            
            val minRotX = pointsMetric.minOf { it.first }
            val minRotY = pointsMetric.minOf { it.second }
            
            val shiftedPoints = pointsMetric.map {
                Pair(it.first - minRotX, it.second - minRotY)
            }
            
            val maxSpan = shiftedPoints.maxOf { it.first }.coerceAtLeast(shiftedPoints.maxOf { it.second })
            
            // Normalize to [0, gridW]
            val normalizedPoints = shiftedPoints.map {
                val nx = if (maxSpan > 0) (it.first / maxSpan) * gridW else 0.0
                val ny = if (maxSpan > 0) (it.second / maxSpan) * gridH else 0.0
                Pair(nx.toFloat(), ny.toFloat())
            }
            
            // Compute cumulative distances of the path
            val dists = FloatArray(normalizedPoints.size)
            dists[0] = 0f
            for (i in 1 until normalizedPoints.size) {
                val dx = normalizedPoints[i].first - normalizedPoints[i-1].first
                val dy = normalizedPoints[i].second - normalizedPoints[i-1].second
                dists[i] = dists[i-1] + Math.sqrt((dx*dx + dy*dy).toDouble()).toFloat()
            }
            
            val totalPathDist = dists.lastOrNull() ?: 1f
            
            // Interpolate subBlocks evenly along the path
            for (i in 0 until n) {
                val t = i.toFloat() / (n - 1).coerceAtLeast(1)
                val targetD = t * totalPathDist
                
                // Find segment
                var segIdx = dists.binarySearch(targetD)
                if (segIdx < 0) segIdx = -segIdx - 2
                segIdx = segIdx.coerceIn(0, dists.size - 2)
                
                val d0 = dists[segIdx]
                val d1 = dists[segIdx + 1]
                val p0 = normalizedPoints[segIdx]
                val p1 = normalizedPoints[segIdx + 1]
                
                val ratio = if (d1 > d0) (targetD - d0) / (d1 - d0) else 0f
                val interpX = p0.first + ratio * (p1.first - p0.first)
                val interpY = p0.second + ratio * (p1.second - p0.second)
                
                path[i] = Pair(interpX, interpY)
            }
        } else {
            // Fallback to straight line if no GPS
            for (i in 0 until n) {
                val t = i.toFloat() / (n - 1).coerceAtLeast(1)
                path[i] = Pair(t * gridW, gridH / 2f)
            }
        }

        // Create segments and sort by depth (y in screen space)
        val segments = mutableListOf<IsoSegment>()
        for (i in 0 until n - 1) {
            segments.add(IsoSegment(
                idx = i,
                x1 = path[i].first, y1 = path[i].second, z1 = elevations[i],
                x2 = path[i+1].first, y2 = path[i+1].second, z2 = elevations[i+1],
                grade = subBlocks[i],
                km = (i * subBlockSizeMeters / 1000.0).toFloat()
            ))
        }
        segments.sortBy { (it.x1 + it.y1 + it.x2 + it.y2) }

        // Posición del ciclista sobre la ruta global: mismo índice fraccional (riderProgress)
        // que el resto de estilos, proyectado sobre la polilínea GPS real de este renderizador.
        var riderIdxGlobal = -1
        var riderRemGlobal = 0f
        var beaconX = 0f
        var beaconY = 0f
        if (riderProgress in 0f..1f) {
            val riderIndexF = if (subBlockSizeMeters > 0.0) {
                ((riderProgress * lookaheadMeters) / subBlockSizeMeters).toFloat().coerceIn(0f, (n - 1).toFloat())
            } else {
                riderProgress * (n - 1)
            }
            riderIdxGlobal = riderIndexF.toInt().coerceIn(0, n - 2)
            riderRemGlobal = riderIndexF - riderIdxGlobal
            val rxRaw = path[riderIdxGlobal].first + riderRemGlobal * (path[riderIdxGlobal + 1].first - path[riderIdxGlobal].first)
            val ryRaw = path[riderIdxGlobal].second + riderRemGlobal * (path[riderIdxGlobal + 1].second - path[riderIdxGlobal].second)
            val rzRaw = elevations[riderIdxGlobal] + riderRemGlobal * (elevations[riderIdxGlobal + 1] - elevations[riderIdxGlobal])
            val (bx, by) = projectIso(rxRaw, ryRaw, rzRaw)
            beaconX = bx + centerX
            beaconY = by + centerY
        }

        // Reused Paint objects to avoid GC thrashing
        if (reusableWallPaint == null) reusableWallPaint = Paint().apply { style = Paint.Style.FILL }
        if (reusableTopPaint == null) reusableTopPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        if (reusableKmTextPaint == null) reusableKmTextPaint = Paint().apply {
            color = Color.LTGRAY
            isAntiAlias = true
        }
        reusableKmTextPaint?.textSize = 14f * fontScale
        val wallPaint = reusableWallPaint!!
        val topPaint = reusableTopPaint!!
        val kmTextPaint = reusableKmTextPaint!!
        
        if (reusablePolyPath == null) reusablePolyPath = Path()
        if (reusableArrowPath == null) reusableArrowPath = Path()

        val ramps = mutableListOf<String>()

        for (seg in segments) {
            val (p1x, p1y) = projectIso(seg.x1, seg.y1, seg.z1)
            val (p2x, p2y) = projectIso(seg.x2, seg.y2, seg.z2)
            val (b1x, b1y) = projectIso(seg.x1, seg.y1, 0f)
            val (b2x, b2y) = projectIso(seg.x2, seg.y2, 0f)
            
            val baseColor = android.graphics.Color.parseColor(com.example.altgraph.GradeColorScale.getColorHex(seg.grade.toDouble()))
            val isTraveled = riderIdxGlobal >= 0 && seg.idx < riderIdxGlobal
            wallPaint.color = if (isTraveled) darkenColor(baseColor, 0.32f) else baseColor
            topPaint.color = if (isTraveled) Color.parseColor("#4B5563") else Color.WHITE
            
            val poly = reusablePolyPath!!
            poly.reset()
            poly.moveTo(p1x + centerX, p1y + centerY)
            poly.lineTo(p2x + centerX, p2y + centerY)
            poly.lineTo(b2x + centerX, b2y + centerY)
            poly.lineTo(b1x + centerX, b1y + centerY)
            poly.close()
            canvas.drawPath(poly, wallPaint)
            
            canvas.drawLine(p1x + centerX, p1y + centerY, p2x + centerX, p2y + centerY, topPaint)
            
            // Draw km marker every 50 blocks (e.g. 50 * 50m = 2.5km)
            if (seg.idx % 50 == 0 && seg.idx > 0) {
                canvas.drawText("${"%.1f".format(seg.km)}km", b1x + centerX + 5f, b1y + centerY + 15f, kmTextPaint)
            }

            // Iso Ramps: Draw vertical pills for local maxima
            if (showRamps && seg.grade >= rampMinSlope) {
                val startIdx = maxOf(0, seg.idx - 3)
                val endIdx = minOf(subBlocks.size - 1, seg.idx + 3)
                var isMax = true
                for (k in startIdx..endIdx) {
                    val g = subBlocks[k].toFloat()
                    if (g > seg.grade) {
                        isMax = false
                        break
                    }
                }
                
                // Only draw if we haven't drawn a ramp too close recently to prevent overlap
                val isFarEnough = ramps.isEmpty() || abs(seg.km - ramps.last().toFloat()) > 0.3f
                
                if (isMax && isFarEnough) {
                    ramps.add(seg.km.toString())
                    
                    val mx = p1x + centerX
                    val my = p1y + centerY
                    val arrowTopY = my - 45f
                    val arrowBottomY = my - 15f
                    
                    canvas.drawLine(mx, arrowTopY, mx, arrowBottomY, rampArrowPaint)
                    
                    val arrowPath = reusableArrowPath!!
                    arrowPath.reset()
                    arrowPath.moveTo(mx, arrowBottomY)
                    arrowPath.lineTo(mx - 6f, arrowBottomY - 10f)
                    arrowPath.lineTo(mx + 6f, arrowBottomY - 10f)
                    arrowPath.close()
                    canvas.drawPath(arrowPath, rampArrowHeadPaint)
                    
                    rampTextPaint.textSize = ((h * 0.052f) * fontScale).coerceIn(11f, 17f)
                    val rampLabel = "%.0f%%".format(seg.grade)
                    canvas.drawText(rampLabel, mx, arrowTopY - 4f, rampTextPaint)
                }
            }
        }

        // Baliza del ciclista avanzando por la ruta global (oculta si -1f, fuera de tramo)
        if (riderIdxGlobal >= 0) {
            val pinY = beaconY - 10f
            drawBeaconShape(canvas, beaconX, pinY, 9f, 7f, 4f)
        }
        
        // Draw Legend at bottom right
        drawIsoLegend(canvas, w, h)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= factor.coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    private fun drawIsoLegend(canvas: Canvas, w: Float, h: Float) {
        if (reusableLegendPaint == null) reusableLegendPaint = Paint().apply { style = Paint.Style.FILL }
        if (reusableLegendTextPaint == null) reusableLegendTextPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        if (reusableLegendTitlePaint == null) reusableLegendTitlePaint = Paint().apply { color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        
        val paint = reusableLegendPaint!!
        val textPaint = reusableLegendTextPaint!!
        textPaint.textSize = 11f * fontScale
        val titlePaint = reusableLegendTitlePaint!!
        titlePaint.textSize = 13f * fontScale
        
        val legendX = w * 0.05f
        val legendY = h * 0.82f
        val grades = listOf(-1.0, 1.0, 4.0, 6.0, 9.0, 11.0, 14.0, 18.0)
        val labels = listOf("<0%", "0-3%", "3-5%", "5-8%", "8-10%", "10-13%", "13-17%", "17%+")
        
        canvas.drawText(context.getString(R.string.label_avg_slope), legendX, legendY - 10f, titlePaint)
        
        var currentX = legendX
        grades.forEachIndexed { idx, grade ->
            paint.color = android.graphics.Color.parseColor(com.example.altgraph.GradeColorScale.getColorHex(grade))
            val boxWidth = 24f
            val labelW = textPaint.measureText(labels[idx])
            val spacing = maxOf(labelW, boxWidth) + 10f
            
            canvas.drawRect(currentX, legendY, currentX + boxWidth, legendY + 10f, paint)
            // Center text under box
            val textX = currentX + (boxWidth / 2f) - (labelW / 2f)
            canvas.drawText(labels[idx], textX, legendY + 24f, textPaint)
            currentX += spacing
        }
    }

    /**
     * HILLSHADE & APEX — Estilo de visualizaci\u00f3n con sombreado de relieve y marcadores de apex.
     *
     * Fuente de luz virtual oblicua desde arriba-izquierda: luz = (-0.7, -1.0) normalizado.
     * Cada segmento del perfil tiene un vector normal (perpendicular a la pendiente).
     * El dot-product con la luz produce un factor de brillo que modula el color base del gradiente.
     * Resultado: rampas duras (normales casi horizontales) quedan en sombra; llanos reciben luz directa.
     */
    private fun drawHillshadeApexRibbon(
        canvas: Canvas,
        w: Float,
        h: Float,
        totalMicroSamples: Int,
        microGrades: FloatArray,
        xFront: FloatArray,
        yFront: FloatArray,
        xBack: FloatArray,
        yBack: FloatArray,
        yBase: FloatArray,
        depth3dY: Float
    ) {
        // Vector de luz oblicua (normalizado), desde arriba-izquierda
        val lightX = -0.6f
        val lightY = -0.8f
        val lightLen = kotlin.math.sqrt(lightX * lightX + lightY * lightY)
        val lx = lightX / lightLen
        val ly = lightY / lightLen

        val hillshadePath = Path()

        for (i in 0 until totalMicroSamples - 1) {
            val grade = if (i < microGrades.size) microGrades[i] else 0f
            val prevGrade = if (i > 0 && i - 1 < microGrades.size) microGrades[i - 1] else grade
            val nextGrade = if (i + 1 < microGrades.size) microGrades[i + 1] else grade

            // Pendiente normalizada (-1 a 1 para pendientes de 0% a 20%)
            val slopeNorm = (grade / 15f).coerceIn(0f, 1.5f)

            // Cambio relativo de pendiente (escalón de relieve): si la rampa se empina respecto al anterior
            val gradeStep = (grade - prevGrade)

            // Iluminación cenital oblicua de relieve (Hillshading topográfico):
            // 1. Efecto pendiente intrínseca: las rampas más empinadas reciben más sombra cenital en la pared vertical
            // 2. Efecto escalón (aristas de relieve): un incremento brusco de pendiente genera contraste de luz y sombra
            val slopeShade = (1.0f - (slopeNorm * 0.45f)).coerceIn(0.40f, 1.15f)
            val stepShade = (gradeStep * 0.08f).coerceIn(-0.35f, 0.35f)
            val lightFactor = (slopeShade + stepShade).coerceIn(0.35f, 1.25f)

            // Color base del gradiente de pendiente (verde, amarillo, naranja, rojo, negro)
            val baseColor = Color.parseColor(getGradeColor(grade.toDouble()))
            val baseR = Color.red(baseColor)
            val baseG = Color.green(baseColor)
            val baseB = Color.blue(baseColor)

            val rTop = (baseR * lightFactor).toInt().coerceIn(0, 255)
            val gTop = (baseG * lightFactor).toInt().coerceIn(0, 255)
            val bTop = (baseB * lightFactor).toInt().coerceIn(0, 255)

            // La base del muro proyecta sombra de oclusión ambiental profunda hacia el suelo
            val ambientOcclusion = (0.35f * (1f - (slopeNorm * 0.2f))).coerceIn(0.20f, 0.50f)
            val rBot = (baseR * ambientOcclusion).toInt().coerceIn(0, 255)
            val gBot = (baseG * ambientOcclusion).toInt().coerceIn(0, 255)
            val bBot = (baseB * ambientOcclusion).toInt().coerceIn(0, 255)

            val colorTop = Color.rgb(rTop, gTop, bTop)
            val colorBot = Color.rgb(rBot, gBot, bBot)

            // Pared frontal esculpida con degradado vertical de volumen
            hillshadePath.reset()
            hillshadePath.moveTo(xFront[i], yFront[i])
            hillshadePath.lineTo(xFront[i + 1], yFront[i + 1])
            hillshadePath.lineTo(xFront[i + 1], yBase[i + 1])
            hillshadePath.lineTo(xFront[i], yBase[i])
            hillshadePath.close()

            val hillShader = LinearGradient(
                xFront[i], yFront[i],
                xFront[i], yBase[i],
                colorTop, colorBot,
                Shader.TileMode.CLAMP
            )
            val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = hillShader
                style = Paint.Style.FILL
            }
            canvas.drawPath(hillshadePath, hillPaint)

            // Relieve biselado en aristas de cambio de pendiente (Línea de sombra/luz esculpida)
            if (kotlin.math.abs(gradeStep) > 1.2f) {
                val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (gradeStep > 0) Color.parseColor("#55FFFFFF") else Color.parseColor("#66000000")
                    strokeWidth = 1.8f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(xFront[i], yFront[i], xFront[i], yBase[i], ridgePaint)
            }

            // Separador de bloque fino
            val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(if (i % currentMicroSubDivisions == 0) "#66FFFFFF" else "#22FFFFFF")
                strokeWidth = if (i % currentMicroSubDivisions == 0) 1.2f else 0.6f
                style = Paint.Style.STROKE
            }
            if (!isMergedJoint(i)) canvas.drawLine(xFront[i], yFront[i], xFront[i], yBase[i], sepPaint)
        }

        // Cap izquierdo
        if (totalMicroSamples > 1) {
            val startGrade = if (microGrades.isNotEmpty()) microGrades[0].toDouble() else 4.0
            leftEndcapPath.reset()
            leftEndcapPath.moveTo(xBack[0], yBack[0])
            leftEndcapPath.lineTo(xFront[0], yFront[0])
            leftEndcapPath.lineTo(xFront[0], yBase[0])
            leftEndcapPath.lineTo(xBack[0], yBase[0] - depth3dY)
            leftEndcapPath.close()
            leftEndcapPaint.color = getGradeColorDarkShaded(startGrade)
            canvas.drawPath(leftEndcapPath, leftEndcapPaint)
        }

        // Cresta superior (ribbon top) con efecto de luz de cresta
        for (i in 0 until totalMicroSamples - 1) {
            val grade = if (i < microGrades.size) microGrades[i] else 0f
            val topColor = getGradeColorTopFacet(grade.toDouble())

            ribbonQuadPath.reset()
            ribbonQuadPath.moveTo(xFront[i], yFront[i])
            ribbonQuadPath.lineTo(xBack[i], yBack[i])
            ribbonQuadPath.lineTo(xBack[i + 1], yBack[i + 1])
            ribbonQuadPath.lineTo(xFront[i + 1], yFront[i + 1])
            ribbonQuadPath.close()

            ribbonSurfacePaint.color = topColor
            canvas.drawPath(ribbonQuadPath, ribbonSurfacePaint)
            canvas.drawLine(xBack[i], yBack[i], xBack[i + 1], yBack[i + 1], ribbonBackLinePaint)
            canvas.drawLine(xFront[i], yFront[i], xFront[i + 1], yFront[i + 1], ribbonFrontLinePaint)
        }

        // APEX: marcadores en herraduras (hairpins) como chinchetas flotantes 3D
        if (showHairpins && hairpins.isNotEmpty()) {
            for (hp in hairpins) {
                val relDist = hp - windowStartMeters
                if (relDist < 0 || relDist > lookaheadMeters) continue
                val frac = (relDist / lookaheadMeters).toFloat().coerceIn(0f, 1f)
                val microIdx = (frac * (totalMicroSamples - 1)).toInt().coerceIn(0, totalMicroSamples - 1)
                val apexX = xFront[microIdx]
                val apexY = yFront[microIdx]

                // Pendiente local en el apex
                val localGrade = if (microIdx < microGrades.size) microGrades[microIdx] else 0f

                // Palo de la chincheta
                val pinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF6B6B")
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(apexX, apexY - 6f, apexX, apexY - 32f, pinLinePaint)

                // Diamante flotante (apex marker)
                val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF6B6B")
                    style = Paint.Style.FILL
                }
                val diamondBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    strokeWidth = 1.5f
                    style = Paint.Style.STROKE
                }
                val apexPath = Path().apply {
                    val cx = apexX
                    val cy = apexY - 40f
                    val r = 8f
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                canvas.drawPath(apexPath, diamondPaint)
                canvas.drawPath(apexPath, diamondBorderPaint)

                // Etiqueta de pendiente local encima del diamante
                val apexTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = (h * 0.07f).coerceIn(8f, 12f)
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setShadowLayer(3f, 0f, 1f, Color.BLACK)
                }
                canvas.drawText("${localGrade.toInt()}%", apexX, apexY - 52f, apexTextPaint)
            }
        }
    }

}