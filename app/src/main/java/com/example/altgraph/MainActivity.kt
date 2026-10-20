package com.example.altgraph

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FontHelper.init(this)
        prefs = AppPreferences.getInstance(this)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#09090B"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 20, 20, 28)
        }

        // 1. Header Card (Logo, Título y Versión)
        val headerCard = createCardContainer()
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.altgraph_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 8
            }
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            text = "${getString(R.string.title_settings)} • v$versionName ELITE"
            textSize = 12f
            setTextColor(Color.parseColor("#A1A1AA"))
            gravity = Gravity.CENTER
            setPadding(0, 2, 0, 4)
        }

        headerCard.addView(logo)
        headerCard.addView(title)
        headerCard.addView(subtitle)

        // 2. Bar de Pestañas Categorizadas (4 Pestañas)
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Contenedores de las 4 Pestañas
        val tabEstiloContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tab3dContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tab2dContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tabVamContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tabProContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tabEliteContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val tabContainers = listOf(
            tabEstiloContainer,
            tab3dContainer,
            tab2dContainer,
            tabVamContainer,
            tabProContainer,
            tabEliteContainer
        )

        val tabTitles = listOf(getString(R.string.tab_style), getString(R.string.tab_3d), getString(R.string.tab_2d), getString(R.string.tab_vam), getString(R.string.tab_pro), getString(R.string.tab_elite))
        val tabButtons = mutableListOf<Button>()

        fun selectTab(activeIdx: Int) {
            tabContainers.forEachIndexed { idx, container ->
                container.visibility = if (idx == activeIdx) View.VISIBLE else View.GONE
            }
            tabButtons.forEachIndexed { idx, btn ->
                if (idx == activeIdx) {
                    btn.setTextColor(Color.BLACK)
                    btn.background = GradientDrawable().apply {
                        setColor(Color.parseColor("#38BDF8"))
                        cornerRadius = 18f
                    }
                } else {
                    btn.setTextColor(Color.WHITE)
                    btn.background = GradientDrawable().apply {
                        setColor(Color.parseColor("#27272A"))
                        cornerRadius = 18f
                    }
                }
            }
        }

        tabTitles.forEachIndexed { idx, titleStr ->
            val btn = Button(this).apply {
                text = titleStr
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(4, 12, 4, 12)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    leftMargin = 3
                    rightMargin = 3
                }
                setOnClickListener { selectTab(idx) }
            }
            tabButtons.add(btn)
            tabBar.addView(btn)
        }

        // ==========================================
        // PESTAÑA 1: ESTILO Y APARIENCIA
        // ==========================================

        // Opción: Modelo de Altimetría (Selector de las 5 Vistas Revolucionarias)
        val styleCard = createCardContainer()
        val styleLabel = createSectionLabel(getString(R.string.label_vista_global))
        val initialStyle = prefs.altimetriaStyle
        val styleValueText = createValueText(getString(initialStyle.titleRes))
        val styleDescText = TextView(this).apply {
            text = getString(initialStyle.descRes)
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 12)
        }

        val styleOptions = AltimetriaStyle.entries

        val styleSpinner = Spinner(this).apply {
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, styleOptions.map { getString(it.titleRes) })
            this.adapter = adapter
            setSelection(styleOptions.indexOf(initialStyle))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = styleOptions[position]
                    prefs.altimetriaStyle = selected
                    styleValueText.text = getString(selected.titleRes)
                    styleDescText.text = getString(selected.descRes)
                    Toast.makeText(this@MainActivity, getString(R.string.toast_vista, getString(selected.titleRes)), Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8
                bottomMargin = 8
            }
        }

        styleCard.addView(styleLabel)
        styleCard.addView(styleValueText)
        styleCard.addView(styleDescText)
        styleCard.addView(styleSpinner)

        // Opción: Modelo de Altimetría (Visor de Puertos)
        val climbStyleCard = createCardContainer()
        val climbStyleLabel = createSectionLabel(getString(R.string.label_vista_puertos))
        val climbInitialStyle = prefs.climbAltimetriaStyle
        val climbStyleValueText = createValueText(getString(climbInitialStyle.titleRes))
        val climbStyleDescText = TextView(this).apply {
            text = getString(climbInitialStyle.descRes)
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 12)
        }

        val climbStyleSpinner = Spinner(this).apply {
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, styleOptions.map { getString(it.titleRes) })
            this.adapter = adapter
            setSelection(styleOptions.indexOf(climbInitialStyle))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = styleOptions[position]
                    prefs.climbAltimetriaStyle = selected
                    climbStyleValueText.text = getString(selected.titleRes)
                    climbStyleDescText.text = getString(selected.descRes)
                    Toast.makeText(this@MainActivity, getString(R.string.toast_visor_puertos, getString(selected.titleRes)), Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8
                bottomMargin = 8
            }
        }

        climbStyleCard.addView(climbStyleLabel)
        climbStyleCard.addView(climbStyleValueText)
        climbStyleCard.addView(climbStyleDescText)
        climbStyleCard.addView(climbStyleSpinner)

        // Opción: Tipografía (Dropdown Spinner)
        val fontCard = createCardContainer()
        val fontLabel = createSectionLabel(getString(R.string.setting_font_family))
        val currentFontOpt = FontHelper.options.find { it.key == prefs.fontFamilyKey } ?: FontHelper.options[0]
        val fontValueText = createValueText(currentFontOpt.getLocalizedLabel())

        val fontSpinner = Spinner(this).apply {
            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                FontHelper.options.map { it.getLocalizedLabel() }
            )
            setAdapter(adapter)

            val selectedIndex = FontHelper.options.indexOfFirst { it.key == prefs.fontFamilyKey }
            if (selectedIndex >= 0) setSelection(selectedIndex)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val opt = FontHelper.options[position]
                    prefs.fontFamilyKey = opt.key
                    fontValueText.text = opt.getLocalizedLabel()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
                bottomMargin = 4
            }
        }

        fontCard.addView(fontLabel)
        fontCard.addView(fontValueText)
        fontCard.addView(fontSpinner)

        // Opción: Girar 90° Modo Apaisado
        val rotate90Card = createCardContainer()
        val switchRotate90 = Switch(this).apply {
            text = getString(R.string.setting_rotate_90_cw) + getString(R.string.suffix_global)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.rotate90Clockwise
            setOnCheckedChangeListener { _, isChecked ->
                prefs.rotate90Clockwise = isChecked
            }
        }
        rotate90Card.addView(switchRotate90)

        val climbRotate90Card = createCardContainer()
        val climbSwitchRotate90 = Switch(this).apply {
            text = getString(R.string.setting_rotate_90_cw) + getString(R.string.suffix_puertos)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.climbRotate90Clockwise
            setOnCheckedChangeListener { _, isChecked ->
                prefs.climbRotate90Clockwise = isChecked
            }
        }
        climbRotate90Card.addView(climbSwitchRotate90)

        // Opción: Mostrar Nombre del Puerto en el Visor
        val climbNameCard = createCardContainer()
        val climbSwitchName = Switch(this).apply {
            text = getString(R.string.setting_show_climb_name_title)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.showClimbNameInViewer
            setOnCheckedChangeListener { _, isChecked ->
                prefs.showClimbNameInViewer = isChecked
            }
        }
        val climbNameDesc = TextView(this).apply {
            text = getString(R.string.setting_show_climb_name_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 0)
        }
        climbNameCard.addView(climbSwitchName)
        climbNameCard.addView(climbNameDesc)

        // Opción: Controles de zoom arriba en el Visor de Puertos
        val climbZoomTopCard = createCardContainer()
        val climbSwitchZoomTop = Switch(this).apply {
            text = getString(R.string.setting_climb_zoom_top_title)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.climbViewerZoomAtTop
            setOnCheckedChangeListener { _, isChecked ->
                prefs.climbViewerZoomAtTop = isChecked
            }
        }
        val climbZoomTopDesc = TextView(this).apply {
            text = getString(R.string.setting_climb_zoom_top_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 0)
        }
        climbZoomTopCard.addView(climbSwitchZoomTop)
        climbZoomTopCard.addView(climbZoomTopDesc)

        // Opción: Tamaño de Letra 3D
        val font3dCard = createCardContainer()
        val font3dLabel = createSectionLabel(getString(R.string.setting_font_size_3d))
        val fontScales = listOf(1.0f to getString(R.string.font_normal), 1.25f to getString(R.string.font_large), 1.5f to getString(R.string.font_xl))
        val currentFontLabel = fontScales.find { it.first == prefs.fontSize3dScale }?.second ?: getString(R.string.font_normal)
        val font3dValueText = createValueText(currentFontLabel)
        val font3dRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val fontButtons = mutableListOf<Button>()
        fontScales.forEach { (scale, labelStr) ->
            val btn = createSegmentButton(labelStr, prefs.fontSize3dScale == scale) {
                prefs.fontSize3dScale = scale
                font3dValueText.text = labelStr
                updateSegmentActiveStates(fontButtons, fontScales.indexOfFirst { it.first == scale })
            }
            fontButtons.add(btn)
            font3dRow.addView(btn)
        }

        font3dCard.addView(font3dLabel)
        font3dCard.addView(font3dValueText)
        font3dCard.addView(font3dRow)

        // Opción: Controles de Zoom Táctil
        val showZoomCard = createCardContainer()
        val switchShowZoom = Switch(this).apply {
            text = getString(R.string.setting_show_3d_zoom)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.show3dZoomControls
            setOnCheckedChangeListener { _, isChecked ->
                prefs.show3dZoomControls = isChecked
            }
        }
        showZoomCard.addView(switchShowZoom)

        tabEstiloContainer.addView(styleCard)
        tabEstiloContainer.addView(climbStyleCard)
        tabEstiloContainer.addView(fontCard)
        tabEstiloContainer.addView(rotate90Card)
        tabEstiloContainer.addView(climbRotate90Card)
        tabEstiloContainer.addView(climbNameCard)
        tabEstiloContainer.addView(climbZoomTopCard)
        tabEstiloContainer.addView(font3dCard)
        tabEstiloContainer.addView(showZoomCard)

        // ==========================================
        // PESTAÑA 2: ALTIMETRÍA 3D
        // ==========================================

        fun formatLookaheadText(meters: Int): String {
            return if (meters < 1000) {
                "$meters m"
            } else {
                "${meters / 1000} km"
            }
        }

        // Opción: Anticipación 3D
        val lookaheadCard = createCardContainer()
        val lookaheadLabel = createSectionLabel(getString(R.string.setting_lookahead_meters_3d))
        val lookaheadValueText = createValueText(formatLookaheadText(prefs.lookaheadMeters3d))
        val lookaheadRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val lookaheadMinusBtn = createActionButton(getString(R.string.btn_reduce)) {
            val curr = prefs.lookaheadMeters3d
            val nextVal = when {
                curr > 150000 -> 150000
                curr > 100000 -> 100000
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
            prefs.lookaheadMeters3d = nextVal
            lookaheadValueText.text = formatLookaheadText(nextVal)
        }

        val lookaheadPlusBtn = createActionButton(getString(R.string.btn_increase)) {
            val curr = prefs.lookaheadMeters3d
            val nextVal = when {
                curr < 500 -> curr + 50
                curr == 500 -> 1000
                curr < 2000 -> 2000
                curr < 5000 -> 5000
                curr < 10000 -> 10000
                curr < 20000 -> 20000
                curr < 50000 -> 50000
                curr < 100000 -> 100000
                curr < 150000 -> 150000
                curr < 200000 -> 200000
                else -> 200000
            }
            prefs.lookaheadMeters3d = nextVal
            lookaheadValueText.text = formatLookaheadText(nextVal)
        }

        lookaheadRow.addView(lookaheadMinusBtn)
        lookaheadRow.addView(lookaheadPlusBtn)

        lookaheadCard.addView(lookaheadLabel)
        lookaheadCard.addView(lookaheadValueText)
        lookaheadCard.addView(lookaheadRow)

        // Opción: Mostrar Cotas en 3D
        val showCotasCard = createCardContainer()
        val switchShowCotas = Switch(this).apply {
            text = getString(R.string.setting_show_3d_cotas)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.show3dCotas
            setOnCheckedChangeListener { _, isChecked ->
                prefs.show3dCotas = isChecked
            }
        }
        showCotasCard.addView(switchShowCotas)

        // Opción: Mostrar Rampas Duras
        val showRampsCard = createCardContainer()
        val switchShowRamps = Switch(this).apply {
            text = getString(R.string.setting_show_3d_ramps)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.show3dRamps
            setOnCheckedChangeListener { _, isChecked ->
                prefs.show3dRamps = isChecked
            }
        }
        showRampsCard.addView(switchShowRamps)

        // Opción: Pendiente Mínima de Rampa
        val rampMinCard = createCardContainer()
        val rampMinLabel = createSectionLabel(getString(R.string.setting_ramp_min_slope))
        val rampMinValueText = createValueText("${prefs.rampMinSlopePct.toInt()}%")
        val rampMinRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val rampMinMinusBtn = createActionButton(getString(R.string.btn_reduce_1_pct)) {
            if (prefs.rampMinSlopePct > 5.0) {
                prefs.rampMinSlopePct -= 1.0
                rampMinValueText.text = "${prefs.rampMinSlopePct.toInt()}%"
            }
        }

        val rampMinPlusBtn = createActionButton(getString(R.string.btn_increase_1_pct)) {
            if (prefs.rampMinSlopePct < prefs.rampMaxSlopePct - 1.0) {
                prefs.rampMinSlopePct += 1.0
                rampMinValueText.text = "${prefs.rampMinSlopePct.toInt()}%"
            }
        }

        rampMinRow.addView(rampMinMinusBtn)
        rampMinRow.addView(rampMinPlusBtn)

        rampMinCard.addView(rampMinLabel)
        rampMinCard.addView(rampMinValueText)
        rampMinCard.addView(rampMinRow)

        // Opción: Pendiente Máxima de Rampa
        val rampMaxCard = createCardContainer()
        val rampMaxLabel = createSectionLabel(getString(R.string.setting_ramp_max_slope))
        val rampMaxValueText = createValueText("${prefs.rampMaxSlopePct.toInt()}%")
        val rampMaxRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val rampMaxMinusBtn = createActionButton(getString(R.string.btn_reduce_1_pct)) {
            if (prefs.rampMaxSlopePct > prefs.rampMinSlopePct + 1.0) {
                prefs.rampMaxSlopePct -= 1.0
                rampMaxValueText.text = "${prefs.rampMaxSlopePct.toInt()}%"
            }
        }

        val rampMaxPlusBtn = createActionButton(getString(R.string.btn_increase_1_pct)) {
            if (prefs.rampMaxSlopePct < 30.0) {
                prefs.rampMaxSlopePct += 1.0
                rampMaxValueText.text = "${prefs.rampMaxSlopePct.toInt()}%"
            }
        }

        rampMaxRow.addView(rampMaxMinusBtn)
        rampMaxRow.addView(rampMaxPlusBtn)

        rampMaxCard.addView(rampMaxLabel)
        rampMaxCard.addView(rampMaxValueText)
        rampMaxCard.addView(rampMaxRow)
        
        // Opción: Mostrar Curvas de Herradura
        val showHairpinsCard = createCardContainer()
        val switchShowHairpins = Switch(this).apply {
            text = getString(R.string.setting_show_hairpins)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.showHairpins
            setOnCheckedChangeListener { _, isChecked ->
                prefs.showHairpins = isChecked
            }
        }
        showHairpinsCard.addView(switchShowHairpins)
        
        // Opción: Filtro de Puntos de Interés (Hitos) con 4 conmutadores específicos
        val poiFilterCard = createCardContainer()
        val poiFilterLabel = createSectionLabel(getString(R.string.setting_poi_filter))
        
        val switchTowns = Switch(this).apply {
            text = getString(R.string.setting_poi_towns)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.showPoiTowns
            setOnCheckedChangeListener { _, isChecked -> prefs.showPoiTowns = isChecked }
            setPadding(0, 4, 0, 4)
        }
        val switchWater = Switch(this).apply {
            text = getString(R.string.setting_poi_water)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.showPoiWater
            setOnCheckedChangeListener { _, isChecked -> prefs.showPoiWater = isChecked }
            setPadding(0, 4, 0, 4)
        }
        val switchViewpoints = Switch(this).apply {
            text = getString(R.string.setting_poi_viewpoints)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.showPoiViewpoints
            setOnCheckedChangeListener { _, isChecked -> prefs.showPoiViewpoints = isChecked }
            setPadding(0, 4, 0, 4)
        }
        val switchSummits = Switch(this).apply {
            text = getString(R.string.setting_poi_summits)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.showPoiSummits
            setOnCheckedChangeListener { _, isChecked -> prefs.showPoiSummits = isChecked }
            setPadding(0, 4, 0, 4)
        }

        poiFilterCard.addView(poiFilterLabel)
        poiFilterCard.addView(switchTowns)
        poiFilterCard.addView(switchWater)
        poiFilterCard.addView(switchViewpoints)
        poiFilterCard.addView(switchSummits)

        // Opción: Método de Cálculo Topográfico
        val topographicCard = createCardContainer()
        val switchTopographic = Switch(this).apply {
            text = getString(R.string.setting_use_topographic)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.useTopographicCalculation
            setOnCheckedChangeListener { _, isChecked ->
                prefs.useTopographicCalculation = isChecked
            }
        }
        topographicCard.addView(switchTopographic)

        // Opción: Pendiente Máxima en Encabezado
        val showMaxGradCard = createCardContainer()
        val switchShowMaxGrad = Switch(this).apply {
            text = getString(R.string.setting_show_max_gradient)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.showMaxGradient
            setOnCheckedChangeListener { _, isChecked ->
                prefs.showMaxGradient = isChecked
            }
        }
        showMaxGradCard.addView(switchShowMaxGrad)

        // Opción: Mostrar Encabezado con Datos y Pendientes en 3D
        val showHeaderStatsCard = createCardContainer()
        val switchShowHeaderStats = Switch(this).apply {
            text = getString(R.string.setting_show_3d_header_stats)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.show3dHeaderStats
            setOnCheckedChangeListener { _, isChecked ->
                prefs.show3dHeaderStats = isChecked
            }
        }
        showHeaderStatsCard.addView(switchShowHeaderStats)

        // Opción: Mostrar Brujula en Ruta
        val showCompassCard = createCardContainer()
        val switchShowCompass = Switch(this).apply {
            text = getString(R.string.setting_show_route_compass)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.showRouteCompass
            setOnCheckedChangeListener { _, isChecked ->
                prefs.showRouteCompass = isChecked
            }
        }
        // Opción: Posición de Flechas de Avance (< / >)
        val panPositionCard = createCardContainer()
        val panPositionLabel = createSectionLabel(getString(R.string.setting_pan_buttons_position_title))
        val panPositionDesc = TextView(this).apply {
            text = getString(R.string.setting_pan_buttons_position_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 8)
        }
        val panPositions = listOf(
            "bottom" to getString(R.string.pan_position_bottom),
            "top" to getString(R.string.pan_position_top)
        )
        val currentPanPosLabel = panPositions.find { it.first == prefs.panButtonsPosition }?.second ?: getString(R.string.pan_position_bottom)
        val panPosValueText = createValueText(currentPanPosLabel)
        val panPosRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val panPosButtons = mutableListOf<Button>()
        panPositions.forEach { (posKey, posLabel) ->
            val btn = createSegmentButton(posLabel, prefs.panButtonsPosition == posKey) {
                prefs.panButtonsPosition = posKey
                panPosValueText.text = posLabel
                updateSegmentActiveStates(panPosButtons, panPositions.indexOfFirst { it.first == posKey })
            }
            panPosButtons.add(btn)
            panPosRow.addView(btn)
        }
        panPositionCard.addView(panPositionLabel)
        panPositionCard.addView(panPositionDesc)
        panPositionCard.addView(panPosValueText)
        panPositionCard.addView(panPosRow)

        tab3dContainer.addView(lookaheadCard)
        tab3dContainer.addView(panPositionCard)
        tab3dContainer.addView(showCompassCard)
        tab3dContainer.addView(showCotasCard)
        tab3dContainer.addView(showRampsCard)
        tab3dContainer.addView(rampMinCard)
        tab3dContainer.addView(rampMaxCard)
        tab3dContainer.addView(showHairpinsCard)
        tab3dContainer.addView(poiFilterCard)
        tab3dContainer.addView(topographicCard)
        tab3dContainer.addView(showMaxGradCard)
        tab3dContainer.addView(showHeaderStatsCard)

        // ==========================================
        // PESTAÑA 3: ESTRATEGA 2D
        // ==========================================

        // Opción: Tamaño de Bloque (50m, 100m, 250m, 500m, 1km)
        val blockCard = createCardContainer()
        val blockLabel = createSectionLabel(getString(R.string.setting_block_size))
        val currentBlockText = if (prefs.blockSizeMeters >= 1000.0) "${(prefs.blockSizeMeters / 1000).toInt()} km" else "${prefs.blockSizeMeters.toInt()} m"
        val blockValueText = createValueText(currentBlockText)
        val blockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val blockButtons = mutableListOf<Button>()
        val blockSizes = listOf(50.0, 100.0, 250.0, 500.0, 1000.0)

        blockSizes.forEach { size ->
            val labelStr = if (size >= 1000.0) "${(size / 1000).toInt()}km" else "${size.toInt()}m"
            val btn = createSegmentButton(labelStr, prefs.blockSizeMeters == size) {
                prefs.blockSizeMeters = size
                blockValueText.text = if (size >= 1000.0) "${(size / 1000).toInt()} km" else "${size.toInt()} m"
                updateSegmentActiveStates(blockButtons, blockSizes.indexOf(size))
            }
            blockButtons.add(btn)
            blockRow.addView(btn)
        }

        blockCard.addView(blockLabel)
        blockCard.addView(blockValueText)
        blockCard.addView(blockRow)

        // Opción: Tramos Visibles
        val visibleBlocksCard = createCardContainer()
        val visibleBlocksLabel = createSectionLabel(getString(R.string.setting_visible_blocks_count))
        val visibleBlocksValueText = createValueText("${prefs.visibleBlocksCount} ${getString(R.string.label_tramos)}")
        val visibleBlocksRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val visibleMinusBtn = createActionButton(getString(R.string.btn_reduce_1)) {
            if (prefs.visibleBlocksCount > 1) {
                prefs.visibleBlocksCount -= 1
                visibleBlocksValueText.text = "${prefs.visibleBlocksCount} ${getString(R.string.label_tramos)}"
            }
        }

        val visiblePlusBtn = createActionButton(getString(R.string.btn_increase_1)) {
            if (prefs.visibleBlocksCount < 10) {
                prefs.visibleBlocksCount += 1
                visibleBlocksValueText.text = "${prefs.visibleBlocksCount} ${getString(R.string.label_tramos)}"
            }
        }

        visibleBlocksRow.addView(visibleMinusBtn)
        visibleBlocksRow.addView(visiblePlusBtn)

        visibleBlocksCard.addView(visibleBlocksLabel)
        visibleBlocksCard.addView(visibleBlocksValueText)
        visibleBlocksCard.addView(visibleBlocksRow)

        // Opción: Mostrar % en Bloques
        val showPctCard = createCardContainer()
        val switchShowPct = Switch(this).apply {
            text = getString(R.string.setting_show_block_percentages)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.showBlockPercentages
            setOnCheckedChangeListener { _, isChecked ->
                prefs.showBlockPercentages = isChecked
            }
        }
        showPctCard.addView(switchShowPct)

        // Opción: Alertas de Ataque
        val alertCard = createCardContainer()
        val switchAlerts = Switch(this).apply {
            text = getString(R.string.setting_enable_alerts)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.attackAlertEnabled
            setOnCheckedChangeListener { _, isChecked ->
                prefs.attackAlertEnabled = isChecked
            }
        }
        alertCard.addView(switchAlerts)

        // Opción: Umbral de Ataque
        val attackCard = createCardContainer()
        val attackLabel = createSectionLabel(getString(R.string.setting_attack_threshold))
        val attackValueText = createValueText("${prefs.thresholdAttackPct.toInt()}%")
        val attackRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val attackButtons = mutableListOf<Button>()
        val thresholds = listOf(8.0, 10.0, 12.0, 15.0)

        thresholds.forEach { thresh ->
            val btn = createSegmentButton("${thresh.toInt()}%", prefs.thresholdAttackPct == thresh) {
                prefs.thresholdAttackPct = thresh
                attackValueText.text = "${thresh.toInt()}%"
                updateSegmentActiveStates(attackButtons, thresholds.indexOf(thresh))
            }
            attackButtons.add(btn)
            attackRow.addView(btn)
        }

        attackCard.addView(attackLabel)
        attackCard.addView(attackValueText)
        attackCard.addView(attackRow)

        tab2dContainer.addView(blockCard)
        tab2dContainer.addView(visibleBlocksCard)
        tab2dContainer.addView(showPctCard)
        tab2dContainer.addView(alertCard)
        tab2dContainer.addView(attackCard)

        // ==========================================
        // PESTAÑA 4: RENDIMIENTO Y VAM
        // ==========================================

        // Opción: VAM Objetivo
        val vamCard = createCardContainer()
        val vamLabel = createSectionLabel(getString(R.string.setting_target_vam))
        val vamValueText = createValueText("${prefs.targetVam} m/h")
        val vamRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val vamMinusBtn = createActionButton(getString(R.string.btn_reduce_50)) {
            if (prefs.targetVam > 500) {
                prefs.targetVam -= 50
                vamValueText.text = "${prefs.targetVam} m/h"
            }
        }

        val vamPlusBtn = createActionButton(getString(R.string.btn_increase_50)) {
            if (prefs.targetVam < 2000) {
                prefs.targetVam += 50
                vamValueText.text = "${prefs.targetVam} m/h"
            }
        }

        vamRow.addView(vamMinusBtn)
        vamRow.addView(vamPlusBtn)

        vamCard.addView(vamLabel)
        vamCard.addView(vamValueText)
        vamCard.addView(vamRow)

        // Opción: Calidad de Asfalto (TA)
        val asphaltCard = createCardContainer()
        val asphaltLabel = createSectionLabel(getString(R.string.setting_asphalt_factor))
        val currentAsphaltQuality = FatigueGradeCalculator.AsphaltQuality.entries.find { abs(it.factor - prefs.asphaltFactor) < 0.05 } ?: FatigueGradeCalculator.AsphaltQuality.GOOD
        val asphaltValueText = createValueText("${currentAsphaltQuality.getLocalizedLabel()} (TA = %.1f)".format(Locale.US, currentAsphaltQuality.factor))
        val asphaltRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val asphaltButtons = mutableListOf<Button>()
        val qualities = FatigueGradeCalculator.AsphaltQuality.entries

        qualities.forEach { q ->
            val isActive = abs(prefs.asphaltFactor - q.factor) < 0.05
            val btn = createSegmentButton(q.getLocalizedLabel().take(6), isActive) {
                prefs.asphaltFactor = q.factor
                asphaltValueText.text = "${q.getLocalizedLabel()} (TA = %.1f)".format(Locale.US, q.factor)
                updateSegmentActiveStates(asphaltButtons, qualities.indexOf(q))
            }
            asphaltButtons.add(btn)
            asphaltRow.addView(btn)
        }

        asphaltCard.addView(asphaltLabel)
        asphaltCard.addView(asphaltValueText)
        asphaltCard.addView(asphaltRow)

        tabVamContainer.addView(vamCard)
        tabVamContainer.addView(asphaltCard)

        // Botón de Guardar y Salir Fijado al Final

        // ==========================================
        // PESTAÑA 5: PRO (MÁXIMO NIVEL SDK)
        // ==========================================

        val proLabel = createSectionLabel(getString(R.string.label_pro_features))
        tabProContainer.addView(proLabel)

        // Smart Zoom
        val smartZoomCard = createCardContainer()
        val switchSmartZoom = Switch(this).apply {
            text = getString(R.string.setting_smart_zoom)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.smartZoomEnabled
            setOnCheckedChangeListener { _, isChecked -> prefs.smartZoomEnabled = isChecked }
        }
        smartZoomCard.addView(switchSmartZoom)
        tabProContainer.addView(smartZoomCard)

        // Oasis Tracking
        val oasisCard = createCardContainer()
        val switchOasis = Switch(this).apply {
            text = getString(R.string.setting_oasis_tracking)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.oasisTrackingEnabled
            setOnCheckedChangeListener { _, isChecked -> prefs.oasisTrackingEnabled = isChecked }
        }
        oasisCard.addView(switchOasis)
        tabProContainer.addView(oasisCard)

        // Virtual Pacer
        val pacerCard = createCardContainer()
        val switchPacer = Switch(this).apply {
            text = getString(R.string.setting_virtual_pacer)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.virtualPacerEnabled
            setOnCheckedChangeListener { _, isChecked -> prefs.virtualPacerEnabled = isChecked }
        }
        pacerCard.addView(switchPacer)
        tabProContainer.addView(pacerCard)

        // Switchback Dynamics
        val switchbackCard = createCardContainer()
        val switchSwitchback = Switch(this).apply {
            text = getString(R.string.setting_switchback_rendering)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.switchbackRenderingEnabled
            setOnCheckedChangeListener { _, isChecked -> prefs.switchbackRenderingEnabled = isChecked }
        }
        switchbackCard.addView(switchSwitchback)
        tabProContainer.addView(switchbackCard)

        // Energy Management
        val energyCard = createCardContainer()
        val switchEnergy = Switch(this).apply {
            text = getString(R.string.setting_energy_management)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.energyManagementEnabled
            setOnCheckedChangeListener { _, isChecked -> prefs.energyManagementEnabled = isChecked }
        }
        energyCard.addView(switchEnergy)
        tabProContainer.addView(energyCard)

        // ==========================================
        // PESTAÑA 6: ELITE (PAYWALL)
        // ==========================================
        val eliteLabel = createSectionLabel(getString(R.string.label_elite_header))
        tabEliteContainer.addView(eliteLabel)
        
        val unlockTitle = TextView(this).apply {
            text = if (prefs.isEliteUnlocked) getString(R.string.elite_status_unlocked) else getString(R.string.elite_status_locked)
            textSize = 14f
            setTextColor(if (prefs.isEliteUnlocked) Color.GREEN else Color.RED)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        tabEliteContainer.addView(unlockTitle)
        
        val licenseCard = createCardContainer()
        val licenseInput = android.widget.EditText(this).apply {
            hint = getString(R.string.elite_hint_license)
            setHintTextColor(Color.parseColor("#71717A"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setText(prefs.licenseKey)
            isSingleLine = true
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }
        
        val activateBtn = Button(this).apply {
            text = getString(R.string.elite_btn_activate)
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2563EB")) // Blue
                cornerRadius = 12f
            }
        }
        
        licenseCard.addView(licenseInput)
        licenseCard.addView(activateBtn)
        tabEliteContainer.addView(licenseCard)
        


        // Strava Mock
        val stravaCard = createCardContainer()
        val switchStrava = Switch(this).apply {
            text = getString(R.string.elite_strava_segments)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.stravaSegmentsEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked -> prefs.stravaSegmentsEnabled = isChecked }
        }
        stravaCard.addView(switchStrava)
        tabEliteContainer.addView(stravaCard)
        
        // Weather Mock
        val weatherCard = createCardContainer()
        val switchWeather = Switch(this).apply {
            text = getString(R.string.elite_weather_overlay)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.weatherOverlayEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked -> prefs.weatherOverlayEnabled = isChecked }
        }
        weatherCard.addView(switchWeather)
        tabEliteContainer.addView(weatherCard)
        
        // Radar Altimetrico
        val radarCard = createCardContainer()
        val switchRadar = Switch(this).apply {
            text = getString(R.string.elite_radar_enabled)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteRadarEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked -> prefs.eliteRadarEnabled = isChecked }
        }
        radarCard.addView(switchRadar)
        tabEliteContainer.addView(radarCard)
        
        // Opción: Texturizado por Superficie (Gravel/MTB) (Elite)
        val showSurfaceTexturesCard = createCardContainer()
        val switchShowSurfaceTextures = Switch(this).apply {
            text = getString(R.string.setting_show_surface_textures)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.showSurfaceTextures
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.showSurfaceTextures = isChecked
            }
        }
        showSurfaceTexturesCard.addView(switchShowSurfaceTextures)
        tabEliteContainer.addView(showSurfaceTexturesCard)
        
        // Radar Resolution Spinner
        val radarResCard = createCardContainer()
        val radarResLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 16)
        }
        val radarResTitle = TextView(this).apply {
            text = getString(R.string.label_radar_resolution)
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
            alpha = if (prefs.isEliteUnlocked) 1.0f else 0.5f
        }
        radarResLayout.addView(radarResTitle)
        
        val radarResSpinner = Spinner(this).apply {
            isEnabled = prefs.isEliteUnlocked
            val options = arrayOf(
                getString(R.string.radar_resolution_auto),
                getString(R.string.radar_resolution_20m),
                getString(R.string.radar_resolution_50m),
                getString(R.string.radar_resolution_100m),
                getString(R.string.radar_resolution_150m),
                getString(R.string.radar_resolution_300m),
                getString(R.string.radar_resolution_500m),
                getString(R.string.radar_resolution_1km)
            )
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            
            val currentRes = prefs.eliteRadarResolution
            setSelection(when(currentRes) {
                1000 -> 7
                500 -> 6
                300 -> 5
                150 -> 4
                100 -> 3
                50 -> 2
                20 -> 1
                else -> 0
            })
            
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val newRes = when(position) {
                        7 -> 1000
                        6 -> 500
                        5 -> 300
                        4 -> 150
                        3 -> 100
                        2 -> 50
                        1 -> 20
                        else -> 0
                    }
                    prefs.eliteRadarResolution = newRes
                    (view as? TextView)?.setTextColor(Color.parseColor("#4CAF50"))
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        radarResLayout.addView(radarResSpinner)
        radarResCard.addView(radarResLayout)
        tabEliteContainer.addView(radarResCard)
        
        // Map Overlay Mode Spinner
        val mapOverlayCard = createCardContainer()
        val mapOverlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 16)
        }
        val mapOverlayTitle = TextView(this).apply {
            text = getString(R.string.elite_map_overlay_mode)
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
            alpha = if (prefs.isEliteUnlocked) 1.0f else 0.5f
        }
        mapOverlayLayout.addView(mapOverlayTitle)
        
        val mapOverlaySpinner = Spinner(this).apply {
            isEnabled = prefs.isEliteUnlocked
            val options = arrayOf(
                getString(R.string.elite_map_overlay_off),
                getString(R.string.elite_map_overlay_climbs),
                getString(R.string.elite_map_overlay_all)
            )
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            
            // Set initial selection
            val currentMode = prefs.eliteMapOverlayMode
            setSelection(when(currentMode) {
                "all" -> 2
                "climbs" -> 1
                else -> 0
            })
            
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val newMode = when(position) {
                        2 -> "all"
                        1 -> "climbs"
                        else -> "off"
                    }
                    prefs.eliteMapOverlayMode = newMode
                    (view as? TextView)?.setTextColor(Color.parseColor("#4CAF50"))
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        mapOverlayLayout.addView(mapOverlaySpinner)
        
        val switchSmoothMap = Switch(this).apply {
            text = getString(R.string.setting_smooth_map_overlay)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteSmoothMapOverlayEnabled
            isEnabled = prefs.isEliteUnlocked
            setPadding(0, 16, 0, 0)
            setOnCheckedChangeListener { _, v -> prefs.eliteSmoothMapOverlayEnabled = v }
        }
        mapOverlayLayout.addView(switchSmoothMap)

        mapOverlayCard.addView(mapOverlayLayout)
        tabEliteContainer.addView(mapOverlayCard)

        // ── Radar ELITE sub-features ─────────────────────────────────────────
        val eliteSubLabel = createSectionLabel(getString(R.string.label_elite_features))
        tabEliteContainer.addView(eliteSubLabel)

        // Ghost / Virtual Pacer
        val ghostCard = createCardContainer()
        val ghostLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val switchGhost = Switch(this).apply {
            text = getString(R.string.label_ghost)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteGhostEnabled
            isEnabled = prefs.isEliteUnlocked
        }
        ghostLayout.addView(switchGhost)

        // Contenedor desplegable del estilo de visualización de KGhost
        val ghostStyleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 0, 0)
            visibility = if (prefs.eliteGhostEnabled) View.VISIBLE else View.GONE
        }
        val ghostStyleTitle = TextView(this).apply {
            text = getString(R.string.label_ghost_style)
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 4)
            alpha = if (prefs.isEliteUnlocked) 1.0f else 0.5f
        }
        ghostStyleLayout.addView(ghostStyleTitle)

        val ghostStyleOptions = listOf(
            Pair("classic", getString(R.string.ghost_style_classic)),
            Pair("velon", getString(R.string.ghost_style_velon)),
            Pair("pursuit", getString(R.string.ghost_style_pursuit)),
            Pair("tissot", getString(R.string.ghost_style_tissot))
        )
        val ghostStyleSpinner = Spinner(this).apply {
            isEnabled = prefs.isEliteUnlocked
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, ghostStyleOptions.map { it.second })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            val currentIdx = ghostStyleOptions.indexOfFirst { it.first == prefs.eliteGhostStyle }.coerceAtLeast(0)
            setSelection(currentIdx)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position in ghostStyleOptions.indices) {
                        prefs.eliteGhostStyle = ghostStyleOptions[position].first
                        (view as? TextView)?.setTextColor(Color.parseColor("#38BDF8"))
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        ghostStyleLayout.addView(ghostStyleSpinner)
        ghostLayout.addView(ghostStyleLayout)

        switchGhost.setOnCheckedChangeListener { _, isChecked ->
            prefs.eliteGhostEnabled = isChecked
            ghostStyleLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        ghostCard.addView(ghostLayout)
        tabEliteContainer.addView(ghostCard)

        // Energy Bar
        val energyBarCard = createCardContainer()
        val switchEnergyBar = Switch(this).apply {
            text = getString(R.string.label_energy_bar)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteEnergyBarEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, v -> prefs.eliteEnergyBarEnabled = v }
        }
        energyBarCard.addView(switchEnergyBar)
        tabEliteContainer.addView(energyBarCard)

        // POI Ruler
        val poiRulerCard = createCardContainer()
        val switchPoiRuler = Switch(this).apply {
            text = getString(R.string.label_poi_icons)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.elitePoiRulerEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, v -> prefs.elitePoiRulerEnabled = v }
        }
        poiRulerCard.addView(switchPoiRuler)
        tabEliteContainer.addView(poiRulerCard)

        // Histogram
        val histCard = createCardContainer()
        val switchHist = Switch(this).apply {
            text = getString(R.string.label_histogram)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteHistogramEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, v -> prefs.eliteHistogramEnabled = v }
        }
        histCard.addView(switchHist)
        tabEliteContainer.addView(histCard)

        // Radar 3D
        val radar3dCard = createCardContainer()
        val switchRadar3d = Switch(this).apply {
            text = getString(R.string.label_radar_3d)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteRadar3dEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, v -> prefs.eliteRadar3dEnabled = v }
        }
        radar3dCard.addView(switchRadar3d)
        
        // --- THEME SELECTOR ---
        val themeLabel = TextView(this).apply {
            text = getString(R.string.label_radar_theme)
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 8)
        }
        radar3dCard.addView(themeLabel)
        
        val themes = arrayOf(getString(R.string.theme_standard), getString(R.string.theme_oasis), getString(R.string.theme_crucibles), getString(R.string.theme_mist), getString(R.string.theme_forcefield))
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
        }
        val themeButtons = mutableListOf<android.widget.Button>()
        themes.forEach { theme ->
            val shortName = when (theme) {
                getString(R.string.theme_standard) -> getString(R.string.theme_classic_alt)
                getString(R.string.theme_crucibles) -> getString(R.string.theme_crucible_alt)
                getString(R.string.theme_forcefield) -> getString(R.string.theme_forcefield_alt)
                else -> theme
            }
            val btn = createSegmentButton(shortName, prefs.eliteRadarTheme == theme) {
                prefs.eliteRadarTheme = theme
                updateSegmentActiveStates(themeButtons, themes.indexOf(theme))
            }
            themeButtons.add(btn)
            themeRow.addView(btn)
        }
        radar3dCard.addView(themeRow)

        tabEliteContainer.addView(radar3dCard)

        // Radar Alerts (Avisos Dinámicos)
        val radarAlertsCard = createCardContainer()
        val switchRadarAlerts = Switch(this).apply {
            text = getString(R.string.label_alerts_radar)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteRadarAlertsEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, v -> prefs.eliteRadarAlertsEnabled = v }
        }
        radarAlertsCard.addView(switchRadarAlerts)
        // Power Guide (Pacing Dinámico)
        val powerGuideCard = createCardContainer()
        val switchPowerGuide = Switch(this).apply {
            text = getString(R.string.setting_power_guide)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.elitePowerGuideEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.elitePowerGuideEnabled = isChecked
            }
        }
        powerGuideCard.addView(switchPowerGuide)

        // Smooth Gradients
        val smoothGradientsCard = createCardContainer()
        val switchSmoothGradients = Switch(this).apply {
            text = getString(R.string.setting_smooth_gradients)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteSmoothGradientsEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.eliteSmoothGradientsEnabled = isChecked
            }
        }
        smoothGradientsCard.addView(switchSmoothGradients)
        tabEliteContainer.addView(smoothGradientsCard)
        tabEliteContainer.addView(powerGuideCard)

        // Profile TV 2D Rotate
        val profileTvRotateCard = createCardContainer()
        val switchProfileTvRotate = Switch(this).apply {
            text = getString(R.string.pref_elite_profile_tv_2d_rotate_title)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteProfileTv2dRotate
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.eliteProfileTv2dRotate = isChecked
            }
        }
        val textProfileTvRotateDesc = TextView(this).apply {
            text = getString(R.string.pref_elite_profile_tv_2d_rotate_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 0)
        }
        profileTvRotateCard.addView(switchProfileTvRotate)
        profileTvRotateCard.addView(textProfileTvRotateDesc)
        tabEliteContainer.addView(profileTvRotateCard)

        // Profile TV Neon Color Selector
        val profileTvColorCard = createCardContainer()
        val profileTvColorLabel = createSectionLabel(getString(R.string.pref_elite_profile_tv_color_title))
        profileTvColorCard.addView(profileTvColorLabel)

        val neonColors = listOf(
            "green" to getString(R.string.color_neon_green),
            "white" to getString(R.string.color_neon_white),
            "blue" to getString(R.string.color_neon_blue),
            "yellow" to getString(R.string.color_neon_yellow),
            "red" to getString(R.string.color_neon_red),
            "purple" to getString(R.string.color_neon_purple)
        )

        val colorButtons = mutableListOf<Button>()
        val colorRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 6 }
        }
        val colorRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        neonColors.forEachIndexed { idx, pair ->
            val colorKey = pair.first
            val colorName = pair.second
            val isActive = prefs.eliteProfileTvColor == colorKey
            val btn = createSegmentButton(colorName, isActive) {
                prefs.eliteProfileTvColor = colorKey
                updateSegmentActiveStates(colorButtons, idx)
            }.apply {
                isEnabled = prefs.isEliteUnlocked
            }
            colorButtons.add(btn)
            if (idx < 3) colorRow1.addView(btn) else colorRow2.addView(btn)
        }

        profileTvColorCard.addView(colorRow1)
        profileTvColorCard.addView(colorRow2)
        tabEliteContainer.addView(profileTvColorCard)

        // Highlight Climbs on Route Profile
        val highlightClimbsCard = createCardContainer()
        val switchHighlightClimbs = Switch(this).apply {
            text = getString(R.string.pref_elite_highlight_climbs_title)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteHighlightClimbs
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.eliteHighlightClimbs = isChecked
            }
        }
        val textHighlightClimbsDesc = TextView(this).apply {
            text = getString(R.string.pref_elite_highlight_climbs_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 0)
        }
        highlightClimbsCard.addView(switchHighlightClimbs)
        highlightClimbsCard.addView(textHighlightClimbsDesc)
        tabEliteContainer.addView(highlightClimbsCard)

        // Show Climb Names in TV Profile
        val profileTvClimbNamesCard = createCardContainer()
        val switchProfileTvClimbNames = Switch(this).apply {
            text = getString(R.string.pref_elite_profile_tv_show_climb_names_title)
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteProfileTvShowClimbNames
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.eliteProfileTvShowClimbNames = isChecked
            }
        }
        val textProfileTvClimbNamesDesc = TextView(this).apply {
            text = getString(R.string.pref_elite_profile_tv_show_climb_names_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 0)
        }
        profileTvClimbNamesCard.addView(switchProfileTvClimbNames)
        profileTvClimbNamesCard.addView(textProfileTvClimbNamesDesc)
        tabEliteContainer.addView(profileTvClimbNamesCard)

        // Custom POI Climbs (Puertos Definidos por Usuario)
        val customClimbsCard = createCardContainer()
        customClimbsCard.orientation = LinearLayout.VERTICAL
        val switchCustomClimbs = Switch(this).apply {
            text = getString(R.string.pref_elite_custom_climbs_title)
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.eliteCustomClimbsEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, isChecked ->
                prefs.eliteCustomClimbsEnabled = isChecked
            }
        }
        val textCustomClimbsDesc = TextView(this).apply {
            text = getString(R.string.pref_elite_custom_climbs_desc)
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 0)
        }
        customClimbsCard.addView(switchCustomClimbs)
        customClimbsCard.addView(textCustomClimbsDesc)
        tabEliteContainer.addView(customClimbsCard)

        // Power Zone Bar
        val powerBarCard = createCardContainer()
        powerBarCard.orientation = LinearLayout.VERTICAL

        val switchPowerBar = Switch(this).apply {
            text = "Barra de Zona de Entrenamiento"
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.elitePowerBarEnabled
            isEnabled = prefs.isEliteUnlocked
            setOnCheckedChangeListener { _, v -> prefs.elitePowerBarEnabled = v }
        }
        powerBarCard.addView(switchPowerBar)

        val ftpLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 0)
        }
        
        val ftpLabel = TextView(this).apply {
            text = "Tu FTP (W): "
            setTextColor(Color.LTGRAY)
            textSize = 14f
        }
        ftpLayout.addView(ftpLabel)

        val ftpInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.DKGRAY)
            setText(prefs.userFtp.toString())
            isEnabled = prefs.isEliteUnlocked
            layoutParams = LinearLayout.LayoutParams(150, LinearLayout.LayoutParams.WRAP_CONTENT)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    s?.toString()?.toIntOrNull()?.let {
                        if (it > 0) prefs.userFtp = it
                    }
                }
            })
        }
        ftpLayout.addView(ftpInput)
        
        powerBarCard.addView(ftpLayout)
        tabEliteContainer.addView(powerBarCard)

        licenseInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                activateBtn.performClick()
                true
            } else false
        }

        activateBtn.setOnClickListener {
            // Cerrar el teclado para dejar el resto de opciones accesibles
            licenseInput.clearFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(licenseInput.windowToken, 0)

            val key = licenseInput.text.toString().trim()
            prefs.licenseKey = key
            val unlocked = prefs.isEliteUnlocked
            unlockTitle.text = if (unlocked) getString(R.string.elite_status_unlocked) else getString(R.string.elite_status_locked)
            unlockTitle.setTextColor(if (unlocked) Color.GREEN else Color.RED)
            
            // Dynamically enable/disable switches
            switchStrava.isEnabled = unlocked
            switchWeather.isEnabled = unlocked
            switchRadar.isEnabled = unlocked
            switchGhost.isEnabled = unlocked
            ghostStyleSpinner.isEnabled = unlocked
            ghostStyleTitle.alpha = if (unlocked) 1.0f else 0.5f
            ghostStyleLayout.visibility = if (switchGhost.isChecked) View.VISIBLE else View.GONE
            switchEnergyBar.isEnabled = unlocked
            switchPoiRuler.isEnabled = unlocked
            switchHist.isEnabled = unlocked
            switchRadar3d.isEnabled = unlocked
            switchPowerBar.isEnabled = unlocked
            switchRadarAlerts.isEnabled = unlocked
            switchPowerGuide.isEnabled = unlocked
            switchSmoothGradients.isEnabled = unlocked
            switchShowSurfaceTextures.isEnabled = unlocked
            switchProfileTvRotate.isEnabled = unlocked
            switchHighlightClimbs.isEnabled = unlocked
            switchProfileTvClimbNames.isEnabled = unlocked
            switchCustomClimbs.isEnabled = unlocked
            colorButtons.forEach { it.isEnabled = unlocked }
            ftpInput.isEnabled = unlocked
            radarResSpinner.isEnabled = unlocked
            mapOverlaySpinner.isEnabled = unlocked
            switchSmoothMap.isEnabled = unlocked
            radarResTitle.alpha = if (unlocked) 1.0f else 0.5f
            mapOverlayTitle.alpha = if (unlocked) 1.0f else 0.5f

            // Los interruptores se crearon bloqueados (apagados): reflejar el estado real al desbloquear
            if (unlocked) {
                switchStrava.isChecked = prefs.stravaSegmentsEnabled
                switchWeather.isChecked = prefs.weatherOverlayEnabled
                switchRadar.isChecked = prefs.eliteRadarEnabled
                switchGhost.isChecked = prefs.eliteGhostEnabled
                switchEnergyBar.isChecked = prefs.eliteEnergyBarEnabled
                switchPoiRuler.isChecked = prefs.elitePoiRulerEnabled
                switchHist.isChecked = prefs.eliteHistogramEnabled
                switchRadar3d.isChecked = prefs.eliteRadar3dEnabled
                switchPowerBar.isChecked = prefs.elitePowerBarEnabled
                switchRadarAlerts.isChecked = prefs.eliteRadarAlertsEnabled
                switchPowerGuide.isChecked = prefs.elitePowerGuideEnabled
                switchSmoothGradients.isChecked = prefs.eliteSmoothGradientsEnabled
                switchShowSurfaceTextures.isChecked = prefs.showSurfaceTextures
                switchProfileTvRotate.isChecked = prefs.eliteProfileTv2dRotate
                switchHighlightClimbs.isChecked = prefs.eliteHighlightClimbs
                switchProfileTvClimbNames.isChecked = prefs.eliteProfileTvShowClimbNames
                switchCustomClimbs.isChecked = prefs.eliteCustomClimbsEnabled
                switchSmoothMap.isChecked = prefs.eliteSmoothMapOverlayEnabled
                val currentMode = prefs.eliteMapOverlayMode
                mapOverlaySpinner.setSelection(when(currentMode) {
                    "all" -> 2
                    "climbs" -> 1
                    else -> 0
                })
                val activeColorIdx = neonColors.indexOfFirst { it.first == prefs.eliteProfileTvColor }
                if (activeColorIdx >= 0) updateSegmentActiveStates(colorButtons, activeColorIdx)
            }
            
            if (unlocked) {
                Toast.makeText(this, getString(R.string.elite_toast_unlocked), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.elite_toast_invalid), Toast.LENGTH_SHORT).show()
            }
        }

        // Final Button Layout
        val saveExitButton = Button(this).apply {
            text = getString(R.string.btn_save_and_exit)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22C55E"))
                cornerRadius = 22f
            }
            setPadding(28, 20, 28, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
                bottomMargin = 16
            }
            setOnClickListener {
                Toast.makeText(this@MainActivity, getString(R.string.status_saved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        rootLayout.addView(headerCard)
        rootLayout.addView(tabBar)
        rootLayout.addView(tabEstiloContainer)
        rootLayout.addView(tab3dContainer)
        rootLayout.addView(tab2dContainer)
        rootLayout.addView(tabVamContainer)
        rootLayout.addView(tabProContainer)
        rootLayout.addView(tabEliteContainer)
        rootLayout.addView(saveExitButton)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        // Seleccionar pestaña por defecto (Tab 0: Estilo)
        selectTab(0)
    }

    private fun createCardContainer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                setStroke(2, Color.parseColor("#27272A"))
                cornerRadius = 20f
            }
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }
    }

    private fun createSectionLabel(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 4)
        }
    }

    private fun createValueText(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }
    }

    private fun createSegmentButton(textStr: String, isActive: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = textStr
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(6, 10, 6, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                leftMargin = 3
                rightMargin = 3
            }
            applyButtonStyle(this, isActive)
            setOnClickListener { onClick() }
        }
    }

    private fun createActionButton(textStr: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = textStr
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(12, 12, 12, 12)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                leftMargin = 6
                rightMargin = 6
            }
            applyButtonStyle(this, false)
            setOnClickListener { onClick() }
        }
    }

    private fun updateSegmentActiveStates(buttons: List<Button>, activeIndex: Int) {
        buttons.forEachIndexed { idx, btn ->
            applyButtonStyle(btn, idx == activeIndex)
        }
    }

    private fun applyButtonStyle(btn: Button, isActive: Boolean) {
        if (isActive) {
            btn.setTextColor(Color.BLACK)
            btn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#38BDF8"))
                cornerRadius = 14f
            }
        } else {
            btn.setTextColor(Color.WHITE)
            btn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#27272A"))
                cornerRadius = 14f
            }
        }
    }
}