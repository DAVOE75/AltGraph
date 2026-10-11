<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/icon.png" alt="ALTGRAPH Logo" width="160" />
</p>
# ALTGRAPH (v1.0.9 ELITE)
<p>Read in: <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README.md">🇪🇸 Español</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_en.md">🇬🇧 English</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_fr.md">🇫🇷 Français</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_it.md">🇮🇹 Italiano</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_de.md">🇩🇪 Deutsch</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_pt.md">🇵🇹 Português</a></p>
**ALTGRAPH** is a next-generation professional altimetry and performance extension for **Hammerhead Karoo** cycling computers (Karoo 2 and Karoo 3), developed by **David García Pascual** using the official `karoo-ext` SDK.
It revolutionizes the traditional concept of cycling altimetry by incorporating a **Suite of 6 Altimetry Visualization Models** with a live dynamic selector, a 15-stage continuous monochromatic scale, a 50-meter rolling window quantum advance, adaptive multiscale resolution, mountain pass analysis, optional topographic calculation (pure horizontal projection), mathematical detection of hairpin turns (tornanti), filtering by POI categories (Towns, Fountains, Viewpoints, Summits), a tap-to-zoom scale switcher, Google Sans / Condensed typography, a 90° rotated full-screen landscape mode, dual **VAM** pace with recommended speed, and scientific calculation of the **Fatigue Grade (GF)**.




## 🔥 VERSION 1.0.77: TV PROFILE 2D (ELITE) 🔥

- **TV Profile 2D (ELITE Novelty)**: A new visual data field that replicates cycling broadcast elevation profiles (Giro/Tour). Draws the entire stage silhouette and dynamically fills the ridden area with your chosen neon gradient (Green, White, Blue, Yellow, Red, or Purple), showing real-time rider position.
- **Climb Detection & Climb List**: Highlights mountain passes on the elevation profile and minimap in distinct light grey, with a centered header and detailed breakdown of categories, distances, and average grades.
- **Landscape Rotation**: Includes an exclusive Elite toggle to rotate the chart 90º for maximum visual width on Karoo.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_tv_profile.png" width="380" alt="TV Profile Green" />
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_tv_profile_purple.png" width="380" alt="TV Profile Purple" />
</p>

## 🚀 VERSION 1.0.44: DYNAMIC PACING (POWER GUIDE) 🚀

- **Power Guide / Pacing (ELITE Novelty)**: A new intelligent system has been added to the Altimetry Radar. Based on your FTP, Altgraph will calculate the ideal watts for each section of the climb in real-time and show you strategy alerts on screen (e.g. '⚡ TARGET: 240W' or '🔴 PUSH: 300W'). Your own sports director on your handlebars!

## 🚀 VERSION 1.0.43: TERRAIN TEXTURES (GRAVEL/MTB) 🚀

**Surface Texturing (ELITE Novelty)**: A new toggle has been added to the Elite Control Panel to activate textures on the 3D ribbon. Now, if you enable this option, the rendering engine will dynamically interleave visual patterns for Gravel (dark dots) and Trail (crossed path lines) over the usual gradient colors.

*⚠️ Note: It currently works in a preview mode (Mock) to showcase the new graphics engine. The texture will perfectly match the real terrain as soon as Hammerhead releases the "Surface" data in their developer SDK.*

## 🚀 VERSION 1.0.9: ELITE GRADIENT ZONES HISTOGRAM 🚀

- **NEW ELITE FEATURE (v1.0.9) - Gradient Zones (Histogram %)**: Brand new visual data field! Analyze your ride in real-time by seeing exactly what percentage of distance and absolute time you have spent in each of the 8 gradient zones (from Descent to Extreme >17%). Fully integrated colors matching the Altimetry Radar, dynamic text sizing, and is unlocked automatically with your Elite Subscription key.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_gradient_zones.png" width="300" alt="Gradient Zones" /></p>

## 🚀 VERSION 1.0.6: INSTALLATION FIX & 3D IMPROVEMENTS 🚀

- **3D Performance & Smoothness**: Resolved a severe memory exhaustion and freezing issue (*Garbage Collection thrashing*). The 3D altimetry models and topographic peaks have been heavily optimized, resulting in extremely smooth graphical performance and lower battery consumption.
- **APK Signature & Auto-update fix**: Fixed the installation error (`INSTALL_PARSE_FAILED_NO_CERTIFICATES`) caused by missing certificates in the APK. Manual downloads and direct auto-updates from the Karoo will now install flawlessly.


## 🌟 VERSION 1.0.5: DYNAMIC SCALE & OPTIONAL ALERTS 🌟

- **NEW ELITE FEATURE - Optional Dynamic Alerts**: You can now toggle the popup alerts ("Steep Wall", "Upcoming Climb", "Meters to Summit") on/off from the ELITE tab in the app's settings. If you disable them, the 3D altimetry radar will dynamically expand to fill the entire data field, providing a much larger map area!
- **Dynamic Scaling in Multi-Field Layouts**: We've rewritten the rendering logic for the bottom distance ruler. The text marks (0m, 50m, 1km...) now resize dynamically based on the exact available screen estate. This solves a visual bug where numbers were cut in half when inserting the radar into tighter data page layouts.
- **Genuine GPS Calibration**: The progress of the Altimetry radar is now mathematically locked to the real GPS polling from your Karoo device (and not solely bounded to raw route polyline calculations). This prevents elevation progression mismatch after riding long distances.
- **Rendering logic**: Fixed a graphical overlapping bug that occurred when hiding the dynamic alerts bar on horizontal views.

## 🐛 VERSION 1.0.4: MILLIMETRIC GPS ACCURACY 🐛

- **Critical GPS Synchronization Fix (Corner Cutting Fix)**: We have completely rewritten the distance calculation mathematical engine to include a *Dynamic Scalar Calibrator*. From now on, simplified projected distances (polylines) are stretched and adapted to match the real physical distances tracked by your Karoo unit on the road. The radar distance tracking now perfectly matches the telemetry, showing true slope percentages exactly under your front wheel, from kilometer zero to the end of your route.
- **Continuous Pointer Offset Fix (X-Axis)**: Resolved all X-Axis compression issues that caused the display to warn you of steep ramps or descents at the wrong time due to delayed pointer tracking.
- **Rendering Engine Update**: Optimized real-time dynamic Hairpin curve generation.

## 🐛 VERSION 1.0.3: DISTANCE & ROUTE CORRECTION 🐛

- **Altimetry Radar Fix:** Resolved a critical bug that caused the radar and the rider progress marker to erroneously jump back to kilometer 0 (km 0) at the start of a climb or after riding several kilometers.
- **Continuous route progress:** The application now properly compensates for the internal route (polyline) simplifications made by the Karoo GPS. Ridden distance, the bottom bar, and notifications will maintain their continuous global progress throughout your ride.

## 🐛 VERSION 1.0.2: CRITICAL FIXES 🐛

- **Circular/Out-and-Back Routes Bug Fix**: Resolved an issue where the GPS incorrectly snapped to the end of the route right at the start, causing the graph to track backwards ("snap-to-end bug").
- **UI Improvement (Radar)**: The maximum grade indicator now displays a colored arrow (Red for ascent, Dark Blue for descent) above the grade percentage, significantly improving visibility and style.

## 🚀 WHATS NEW IN VERSION 1.0.0: ALTIMETRY RADAR & PRO EXPERIENCE 🚀

We have reached version **1.0.0**! This update brings a complete redesign of the experience, making control and visualization more precise and professional than ever.

### 🌟 Altimetry Radar (New 3D View Mode)
The new **Altimetry Radar** mode overlays your Karoo map, allowing you to see both pure navigation and the upcoming terrain relief, all exquisitely integrated into the same screen.
<p align="center">
  <img src="art/screenshot_radar_altimetrico.png" width="400" />
</p>

### 🔧 New Features and Improvements:
- **Training Zones Bar**: Full integration of the bottom colored bar (7 zones) based on your power meter or heart rate data in the Altimetry Radar mode.
- **Fine Zoom Control (+ / -)**: Replaced the old magnifying glass with dedicated **[+]** and **[-]** buttons. You can now zoom down to **50-meter blocks**, overriding the "Smart Zoom" algorithm if you want absolute manual control.
- **Horizontal Panning (< / >)**: New interactive buttons in the profile view that allow you to freely drag (pan) the graph left and right without altering the scale. Anticipate the climb!
- **Native Landscape Mode**: Full support for Karoo mounted horizontally. Now touch buttons and the profile perfectly re-orient according to your visual field, placing touch controls ergonomically at the edges of the screen.
- **Real-Time Cyclist Icon**: The ball (current cyclist position) now moves fluidly across the graphical profile and elevation colors, giving you immediate feedback on where you are on the route.
- **Dynamic Compass**: Smart hiding of the compass in views other than the Global Isometric Map, cleaning up the interface to show only what is relevant.

## 📸 Capturas en Pantalla Real de Karoo 3

| 🎨 Style Tab | 🎛️ Selection Menu | 📊 Full Dashboard |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="220" alt="🎨 Style Tab" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_data_selection.png" width="220" alt="🎛️ Selection Menu" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_dashboard_completo.png" width="220" alt="📊 Full Dashboard" /> |

| 🏔️ 3D Altimetry (200m) | 🏔️ 3D Altimetry (1km) | 🏔️ 3D Altimetry (10km) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="220" alt="🏔️ 3D Altimetry (200m)" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_1km.png" width="220" alt="🏔️ 3D Altimetry (1km)" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_10km.png" width="220" alt="🏔️ 3D Altimetry (10km)" /> |

- **Independent Settings (v0.7.0)**: You can now separately configure the 3D style (Classic, Isometric, etc.) for the Global Graph and the Climb Viewer from the settings.
- **Screen Rotation (v0.7.0)**: Adjust landscape mode (Rotate 90°) separately for the global graph and climb viewer.

| 🏔️ 3D Altimetry (20km) | 🏔️ 3D Altimetry (200km) | 🏔️ Climb Viewer - 3rd Cat |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_20km.png" width="220" alt="🏔️ 3D Altimetry (20km)" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_200km.png" width="220" alt="🏔️ 3D Altimetry (200km)" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer.png" width="220" alt="🏔️ Climb Viewer - 3rd Cat" /> |

| 🏔️ Climb Viewer - HC (Especial) | 🏔️ Climb Viewer (Isometric) <div align="center">

| 🗺️ GPS Isometric (Global) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_especial.png" width="220" alt="🏔️ Climb Viewer - HC (Especial)" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_iso.png" width="220" alt="🏔️ Climb Viewer (Isometric)" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_gps.png" width="220" alt="🗺️ GPS Isometric (Global)" /> |

</div>


---
## 🌐 Official Web Portal & Multi-language Support
Visit the **[ALTGRAPH Official Web Portal (davoe75.github.io/ALTGRAPH)](https://davoe75.github.io/ALTGRAPH/)** for interactive documentation in Spanish, English, French, Italian, German, and Portuguese.

## 👑 ALTGRAPH ELITE 👑 (v0.7.0)
- **Strava Live Segments 3D (Beta):** Live simulation (KOM Ghost) on extremely hard climbs.
- **Wind & Weather Overlay:** Native integration (Open-Meteo) to render live wind direction using 3D vectors (Headwind/Tailwind).
## 🚀 Features & Highlights (v0.4.1 Stable)
### 🌟 Suite of 6 Revolutionary Altimetry Models (Switchable on the fly)
Allows the cyclist to choose between 6 visual ways to interpret the mountain from the top card of the **🎨 Style** tab:
1. **🏔️ Classic 3D (Default)**: Professional profile with a subtle 3D bevel, smooth monochromatic gradient from 0 to 15%, rotated altitude markers, and high-contrast dark capsules with crisp typography for percentages.
2. **🌅 Isometric Horizon (`Horizonte Iso`)**: Deep cockpit perspective projecting towards the horizon, a dashed central runway-style lane, and a **dynamic front light beam** that tilts according to the physical slope of the imminent ramp.
3. **❄️ Oasis and Tactical Crucibles (`Oasis Tácticos`)**: Smart detection of recovery flats (<=3% and >=30m) glowing in ice blue with dynamic badges, fire crucibles for critical ramps (>=12%) with tactical badges, and an atmospheric hypoxia haze at altitudes >=1400m.
4. **⚡ Force Field & Fatigue (`Campo Fuerza`)**: Relief with gravitational tension lines anchored with pixel precision to the slope, a kinetic sine wave at the base (cyan when advancing with inertia, rapid crimson when the slope slows your cadence), and an ascending pace guideline (VAM) floating smoothly over the profile.
5. **💎 Obsidian & Plasma Monolith (`Monolito`)**: Mountain sculpted in dark faceted obsidian crystal, polished 3D upper facet, a radiant inner plasma core, and a top crest glowing with a white laser beam and a light blue neon aura.
### 🎨 High Visibility Intense Gradient Scale

| Gradient Range | Color | Hue | Riding Feel / Terrain |
| :--- | :---: | :--- | :--- |
| **≤ -10.0%** | 🔵 | Dark navy blue | Very steep descent |
| **-10.0% to -5.0%** | 🔵 | Dark blue | Medium descent |
| **-5.0% to -2.0%** | 🔵 | Medium blue | Mild descent |
| **-2.0% to < 0.0%** | 🔵 | Light sky blue | False flat downhill |
| **0.0% to 3.0%** | 🟢 | Intense forest green | Flat / Gentle incline |
| **3.0% to 5.0%** | 🟡 | Strong yellow | Moderate climb |
| **5.0% to 8.0%** | 🟠 | Intense orange | Demanding climb |
| **8.0% to 10.0%** | 🟠 | Dark terracotta orange | Steep ramp |
| **10.0% to 13.0%** | 🔴 | Intense red | Severe wall |
| **13.0% to 17.0%** | 🟤 | Dark garnet red | Extreme wall |
| **> 17.0%** | ⚫ | Jet black | Extreme grade (>17%) |
### 🔄 50m Quantum Rolling Window and Physical Advance
* The cyclist's beacon physically ascends the upper contour of the slope from 0 to 50m.
* Upon completing each 50-meter multiple, the window cleanly shifts 50m to the left without abrupt scale jumps.
### 📏 Adaptive Multiscale Resolution
* `<= 500m`: **50m** sub-blocks within 100m blocks.
* `500m` to `5km`: **100m** blocks (filtering out blocks > 500m).
* `5km` to `20km`: **500m** blocks (filtering out blocks > 2km).
* `20km` to `50km`: **1km** blocks (filtering out blocks > 5km).
* `> 100km`: **10km** stretches (filtering out blocks > 20km).
### ⚡ Zero-Allocation Architecture for Karoo 3
* Memory allocation-free rendering canvas: zero background Garbage Collector (GC) pauses.
* Real-time `Bitmap` and `Canvas` buffer reuse.
### 📊 Triple Header and Telemetry
* Live readout of **CURRENT GRADE**, **AVERAGE GRADE** of the visible stretch, and **MAX GRADE**.
* Vector detection of hairpin turns (tornanti), POI filtering (Towns, Fountains, Viewpoints, Summits), exact topographic method, target VAM pace, and Fatigue Grade (GF) index.

## 🆚 ALTGRAPH vs Native Karoo Climber
ALTGRAPH doesn't replace the native Climber; it complements it by offering a **comprehensive graphical view** that can be customized as a `Data Field` or full-screen. Its main differences are:
- **Adaptive Resolution & Continuous Rendering**: The profile physically advances in 50m rolling windows. The bars don't "jump" statically; they flow towards you matching your pedaling cadence.
- **Visualization Models**: The native Climber has a fixed view; ALTGRAPH offers 5 revolutionary models.
- **Dynamic Touch Scales**: You can instantly change the route zoom (200m, 1km, 10km, 20km, 50km, etc.) simply by tapping the magnifying glass without leaving your workout.
- **Fatigue Grade (GF)**: The only tool on Karoo that calculates the scientific hardness of a climb based on the APM coefficient.

## ⚙️ Features & Functions in Detail
### 📈 Exclusive Data Fields (Tactical Dashboard)
Besides the graphics engine, ALTGRAPH exposes advanced metrics as individual fields so you can design your perfect screen:
- **🧠 Altimetry Strategist**: Displays a smart visual block summarizing the upcoming kilometers. It divides the horizon into colored segments so you can see at a glance if a hard climb (red), a flat rest (green), or rolling terrain is approaching.
- **🏔️ 3D Mountain Pass Viewer (Visor de Puertos)**: Independently explore every mountain pass detected on your route. Rendered in full screen with touch navigation, it shows length, elevation gain, average grade, and official **La Vuelta a España** categorization (3rd, 2nd, 1st Cat, or Special/HC) mathematically calculated via our APM engine.
- **⚡ Fatigue Grade (GF)**: A scientific engine based on the APM coefficient that evaluates the real hardness of the climb live, displaying a numerical wear coefficient.
- **🚀 Target VAM Pace**: Set your ideal VAM (Average Ascent Speed) in the settings. This field calculates the exact slope under your wheels and dictates exactly what **speed (km/h)** you must ride at that moment to reach your target at the summit.
- **📐 3D Trend & Max Ramp**: Warns you if the slope is getting steeper or mellowing out before your legs even feel it.
### 🗺️ Advanced Navigation and Topography
- **Official Topographic Icons**: Identification of Summits, Mountain Passes (Map-Pin), Towns, and Fountains.
- **Mathematical Curve Detection**: The algorithm detects tight turns (tornanti/hairpins) and gives you a graphical warning.
- **Pure Topographic Calculation**: Optionally, calculate distances based on horizontal projection for absolute rigor in extreme high-mountain passes.
- **Dark and High Contrast Themes**: Interface designed for instant readability under direct sunlight.

## 📲 Installation on Karoo 2 and Karoo 3
Built on the official `karoo-ext` SDK, the installation **requires no root** or dangerous OS modifications. It is 100% safe.
**Via Sideloading (Recommended for Karoo 3)**
1. Download the latest `.apk` file from the [Releases](https://github.com/DAVOE75/ALTGRAPH/releases) section.
2. Send it to your Karoo using the official **Hammerhead Companion** app on your smartphone.
3. Once installed, the ALTGRAPH extension will automatically look for future updates by reading the `manifest.json`.
**Via ADB (Advanced)**
1. Enable Developer Options and USB debugging on your Karoo device.
2. Connect the device to your PC and run: `adb install -r altgraph.apk`

## 🔧 Initial Configuration
To bring ALTGRAPH to life on your training screen:
1. On your Karoo, go to **Profiles** and edit your usual profile (e.g., Road or MTB).
2. Edit one of the data pages and select your preferred layout (supports everything from a central graphical block to 100% full screen).
3. Tap the cell to choose the field, scroll down to the extensions section, and choose **ALTGRAPH**.
4. Optional: Open the ALTGRAPH companion app from the App Launcher to adjust your preferences, like the default 3D model or your target VAM.

## 🧠 Architecture and How It Works
- **Total Native Integration**: Extracts the route, track, and live progress from the `OnNavigationState` of the official `karoo-ext` API.
- **Telemetry Processing**: The cyclist's progress is triangulated by crossing the `DISTANCE_TO_DESTINATION` stream with the original elevation polyline, filtering out GPS errors.
- **Zero-Allocation Rendering**: The entire 3D graphics engine has been programmed on a pure Android Canvas. No "garbage" objects are generated in the loop (Zero GC pauses), guaranteeing 0 visual lags and minimal battery impact.

## ⚠️ Known Limitations
- If you go off the loaded route (Off-Route), the graph will stop advancing until the navigation system recalculates or you return to the official path.
- If Karoo doesn't provide the elevation polyline for a third-party imported route, ALTGRAPH will rely on averages to draw the profile.

#### 🔮 Roadmap and Upcoming Features (Free-Ride Mode)

The next big milestone in ALTGRAPH's development is **Real-Time Altimetric Generation (Free-Ride Mode)**. 
Currently, the extension requires loading a route (GPX) to obtain the profiles. As soon as Hammerhead releases and enables access to imminent elevation data in its SDK for external developers, **ALTGRAPH will generate the 3D graph and all its tactical metrics on the fly**, without needing a loaded track. You'll be able to explore freely, and the mountain will draw itself in front of you in real-time.

## 📦 Compilation for Developers
```bash
./gradlew assembleDebug # Compile test APK
./gradlew testDebugUnitTest # Run math tests
```

## 🤝 Credits and Acknowledgements
- Built on the official Hammerhead **[karoo-ext](https://github.com/hammerheadnav/karoo-ext)** SDK (Apache 2.0 License).
- Inspired by the open-source modding community for Karoo (such as the legendary *Ki2* extension or *Climber+*).
- Developed by **David García Pascual**.

#### 📄 License and Disclaimer

This open-source project is distributed under the **MIT** license - Copyright 2026 David García Pascual.
*Disclaimer: This extension is not affiliated with, endorsed, sponsored, or supported by Hammerhead or SRAM.*

## 🚀 What's new in v0.7.0 (World Tour Pro Features) / Novedades v0.7.0
### 🇪🇸 ESPAÑOL
- **Smart Zoom Táctico (Auto-Escala)**: El zoom 3D se ajusta automáticamente según la pendiente instantánea, cerrándose en muros y abriéndose en llanos panorámicos.
- **Oasis Tracking**: El sistema calcula y proyecta un holograma en el cielo indicando la distancia exacta hasta el próximo "Crisol de Fuego" (rampa > 8%), permitiendo gestionar el esfuerzo y recuperación.
- **Virtual Pacer**: Añadido el cálculo físico por VAM que proyecta una orbe fantasma `Cyan` que avanza por el perfil de la altimetría.
- **Dinámica de Herraduras 3D (Switchbacks)**: La geometría de la carretera hace un zig-zag lateral tridimensional al llegar a una curva de herradura real.
- **Human Battery (Energy Management)**: HUD táctico que evalúa tu quema por VAM, baja tus reservas al subir fuerte y las recarga en llano.
### 🇬🇧 ENGLISH
- **Tactical Smart Zoom (Auto-Scale)**: The 3D zoom automatically adjusts based on the instant slope, zooming in on walls and opening up on panoramic flats.
- **Oasis Tracking**: The system calculates and projects a hologram in the sky indicating the exact distance to the next "Crucible" (ramp > 8%), allowing you to manage effort and recovery.
- **Virtual Pacer**: Physical VAM calculation added that projects a `Cyan` ghost orb advancing along the altimetry profile.
- **3D Switchback Dynamics**: The road geometry makes a 3D lateral zigzag when approaching a real hairpin curve.
- **Human Battery (Energy Management)**: Tactical HUD that evaluates your VAM burn, lowers your reserves when pushing hard, and recharges them on flats.
### 🇫🇷 FRANÇAIS
- **Smart Zoom Tactique (Auto-Scale)**: Le zoom 3D s'ajuste automatiquement en fonction de la pente instantanée.
- **Oasis Tracking**: Le système calcule et projette un hologramme dans le ciel indiquant la distance exacte jusqu'au prochain "Creuset" (rampe > 8%).
- **Virtual Pacer**: Ajout du calcul physique par VAM qui projette un orbe fantôme `Cyan` avançant sur le profil.
- **Dynamique de Lacets 3D (Switchbacks)**: La géométrie de la route effectue un zigzag latéral 3D à l'approche d'un véritable lacet.
- **Human Battery (Energy Management)**: HUD tactique qui évalue votre dépense VAM, réduit vos réserves lors d'efforts intenses et les recharge sur le plat.
### 🇮🇹 ITALIANO
- **Smart Zoom Tattico (Auto-Scale)**: Lo zoom 3D si adatta automaticamente in base alla pendenza istantanea.
- **Oasis Tracking**: Il sistema calcola e proietta un ologramma nel cielo indicando la distanza esatta fino al prossimo "Crogiolo" (rampa > 8%).
- **Virtual Pacer**: Aggiunto il calcolo fisico tramite VAM che proietta una sfera fantasma `Cyan` che avanza lungo il profilo.
- **Dinamiche Tornanti 3D (Switchbacks)**: La geometria della strada compie uno zig-zag laterale 3D quando ci si avvicina a un vero tornante.
- **Human Battery (Energy Management)**: HUD tattico che valuta il tuo dispendio VAM, riduce le tue riserve durante gli sforzi intensi e le ricarica in pianura.
### 🇩🇪 DEUTSCH
- **Taktischer Smart Zoom (Auto-Scale)**: Der 3D-Zoom passt sich automatisch der aktuellen Steigung an.
- **Oasis Tracking**: Das System berechnet und projiziert ein Hologramm in den Himmel, das die genaue Entfernung zur nächsten "Bewährungsprobe" (Rampe > 8%) anzeigt.
- **Virtual Pacer**: Physische VAM-Berechnung hinzugefügt, die eine `Cyan` Geisterkugel projiziert, die entlang des Höhenprofils voranschreitet.
- **3D-Kehren-Dynamik (Switchbacks)**: Die Straßengeometrie macht einen lateralen 3D-Zickzack, wenn man sich einer echten Haarnadelkurve nähert.
- **Human Battery (Energy Management)**: Taktisches HUD, das deinen VAM-Verbrauch bewertet, deine Reserven bei harter Anstrengung senkt und sie in der Ebene wieder auflädt.
### 🏅 Climb Categorization (Grand Tour Style)
ALTGRAPH automatically detects and classifies mountain passes on your route using technical criteria from Grand Tours (La Vuelta a España, Tour de France), based on the **Fatigue Grade (APM)**. Detection criteria require a minimum of 3 km at 3% (or short 10% walls) or a minimum accumulated hardness of 20.0 APM.
The official categories assigned are:
- **Special (HC)**: > 240 APM
- **1st Category**: 150 - 240 APM
- **2nd Category**: 85 - 150 APM
- **3rd Category**: 40 - 85 APM
- **4th Category**: 20 - 40 APM
