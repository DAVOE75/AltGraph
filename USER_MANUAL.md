# 📖 Manual de Usuario - ALTGRAPH (v1.1.0 ELITE)

**ALTGRAPH** es una extensión avanzada de altimetría y análisis de rendimiento para ciclocomputadores **Hammerhead Karoo** (Karoo 2 y Karoo 3), desarrollada por **David García Pascual**.

---

### 🌟 Novedad Destacada v1.1.0: Puertos Definidos por Usuario (POIs)
Permite delimitar exactamente el inicio y final de los puertos en tus rutas para que **prevalezcan sobre los algoritmos automáticos**:
1. **Inicio y Cima por POIs**:
   - `INICIO: NombreDelPuerto` (o `START:`) al pie de la subida.
   - `CIMA: NombreDelPuerto` (o `SUMMIT:`) en la coronación.
   - O solo `CIMA: NombreDelPuerto`: AltGraph calcula de forma inteligente el inicio natural del puerto.
2. **Categorización Oficial Vuelta a España / APM**:
   - Cálculo automático de distancia, desnivel, pendiente media y categoría oficial (4ª, 3ª, 2ª, 1ª o Especial/HC).
   - O forzar categoría deseada entre corchetes: `CIMA: NombreDelPuerto [1C]`, `[2C]`, `[ESP]` o `[HC]`.
3. **Prevalencia Absoluta**:
   - Reemplaza puertos partidos por descansillos y muestra el nombre real en **Visor de Puertos**, **Perfil TV 2D** y **Barra de Ruta**.

---

## 📸 Capturas de Pantalla en Karoo 3 Real

| 🏔️ Altimetría 3D | 🎨 Panel Estilo | 🏔️ Panel 3D | 📊 Panel 2D | 🚴 Panel VAM |
| :---: | :---: | :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="160" alt="3D Profile" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="160" alt="Estilo" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_3d.png" width="160" alt="3D" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_2d.png" width="160" alt="2D" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_vam.png" width="160" alt="VAM" /> |

---

## 🇪🇸 ESPAÑOL

### 1. Instalación y Configuración
1. Transfiere la APK `ALTGRAPH-v0.5.0.apk` a tu dispositivo Karoo mediante **ADB** (`adb install -r ALTGRAPH-v0.5.0.apk`).
2. En tu Karoo, entra en **Ajustes > Perfiles de Carrera** y selecciona el perfil donde quieras incluir los campos.
3. Edita una página de datos, pulsa en **Añadir Campo** y selecciona la categoría **ALTGRAPH**.

### 2. Panel de Configuración por Pestañas Categorizadas
Al abrir **ALTGRAPH** desde el menú de aplicaciones de tu Karoo 3, dispones de 4 pestañas organizadas:

* **🎨 Pestaña ESTILO**:
  - **VISTA Y MODELO DE ALTIMETRÍA (Selector de 5 Modelos)**:
    - 🏔️ **Clásica 3D (Por defecto)**: Perfil profesional con bisel 3D sutil, degradado monocromático suave de 15 tramos del 0 al 15% y cotas nítidas.
    - 🌅 **Horizonte Isométrico**: Perspectiva de cabina (*cockpit 3D*) inclinada hacia el horizonte, carril central tipo pista y haz de luz frontal de la baliza orientado según la pendiente inminente.
    - ❄️ **Oasis y Crisoles Tácticos**: Resalta descansillos de recuperación $\le 3\%$ en azul hielo con carteles `❄️ OASIS [dist]m` y rampas críticas $\ge 12\%$ en fuego térmico con avisos `🔥 MURO [dist]m`, con bruma de hipoxia $>1400\text{m}$.
    - ⚡ **Campo de Fuerza y Fatiga**: Líneas de tensión gravitatoria ancladas milimétricamente al relieve, onda cinética senoidal en la base (cian/carmesí según resistencia) y línea guía de ritmo VAM flotante.
    - 💎 **Monolito de Obsidiana y Plasma**: Montaña facetada en cristal de obsidiana ahumada, faceta superior 3D pulida, núcleo interior de plasma radiante y haz láser blanco con resplandor neón.
  - **Tipo de Letra (Google Sans / Condensed)**: `Condensed (Estrecha/Alta)`, `Sans-Serif (Estándar)`, `Sans Medium`, `Sans Black (Extra Gruesa)`, `Monospace Digital`.
  - **Girar Gráfico 90° (Modo Apaisado / Horizontal)**: Activado / Desactivado.
  - **Tamaño de Letra en Altimetría 3D**: `Normal`, `Grande` o `Extra Gr.`.
  - **Mostrar Controles de Zoom Táctil (🔍)**: Activado / Desactivado.

* **🏔️ Pestaña 3D**:
  - **Distancia de Anticipación 3D (Eje X)**: 200m a 500m (pasos de 50m), salto a 1 km y de 1 km en 1 km hasta 100 km.
  - **Mostrar Cotas de Altitud en Gráfico 3D**: Activado / Desactivado.
  - **Mostrar Indicadores de Rampas Duras (≥10%) en 3D**: Activado / Desactivado.
  - **Pendiente Mínima de Rampa (%)**: 5% a 20% (10% por defecto).
  - **Pendiente Máxima de Rampa (%)**: 10% a 30% (15% por defecto).
  - **Mostrar Curvas de Herradura (Líneas Negras)**: Activado / Desactivado.
  - **Filtro de Puntos de Interés (Hitos POIs)**: 🏘️ Pueblos, 💧 Fuentes, 📸 Miradores, 📡 Cimas.
  - **Método de Cálculo Topográfico (Proyección Horizontal)**: Activado / Desactivado.
  - **Mostrar Pendiente Máxima en Encabezado 3D**: Activado / Desactivado.

* **📊 Pestaña 2D**:
  - **Distancia por Bloque del Perfil**: 50 m, 100 m, 250 m, 500 m o 1 km.
  - **Número de Tramos Visibles en Perfil**: Ajuste de 1 a 10 tramos (5 por defecto).
  - **Mostrar % en Bloques del Perfil**: Activado / Desactivado.
  - **Activar Alertas Visuales de Ataque**: Activado / Desactivado.
  - **Umbral de Alerta de Ataque (%)**: 8%, 10%, 12% o 15%.

* **🚴 Pestaña VAM**:
  - **VAM Objetivo (m/h)**: Ajuste desde 500 m/h hasta 2000 m/h.
  - **Tipo / Calidad de Asfalto (TA)**: Muy Bueno (0.1), Bueno (0.5), Regular (1.2), Malo / Gravilla (1.7).

### 3. Campos de Datos Disponibles (v0.5.0)

#### 🏔️ 1. Altimetría 3D (`3D Altimetry`)
- **Descripción**: Perfil 3D avanzado con selector de 5 estilos (Clásico, Horizonte Iso, Oasis Tácticos, Campo Fuerza, Monolito Obsidiana). Incorpora ventana rodante cuántica de 50m, progresión física del ciclista por la cresta, resolución adaptativa multiescala (50m a 10km), escala monocromática suave de 15 tramos del 0 al 15% (blanco a rojo), cotas de altitud nítidas, cápsulas oscuras de alto contraste para %, lectura triple en encabezado (**Pendiente Actual**, **Pendiente Media del tramo** y **Pendiente Máxima**), marcas de herraduras, hitos POI y zoom táctil en pantalla.

#### 📊 2. Estratega de Altimetría (`Altimetry Profile & Strategy`)
- **Descripción**: Perfil de elevación por bloques ajustables (50m a 1km) con tramos visibles personalizables (1 a 10), código de colores y % de pendiente impreso en cada bloque.

#### ⛰️ 3. Índice Grado de Fatiga GF (`Fatigue Grade GF`)
- **Descripción**: Muestra el Grado de Fatiga acumulado de la subida restante basándose en el modelo científico:
  $$\text{GF} = \sum \text{DU}^* + \text{TA} + \left(\frac{\text{PMx}}{5}\right)$$

#### 🚴 4. Ritmo VAM Objetivo (`ClimbPacing VAM Target`) — *Opción C Dual*
- **Descripción**: Asistente de ritmo con VAM instantánea (`850 m/h`), velocidad objetivo recomendada en km/h (`12.5 km/h`) e insignia de estado (`🟢 EN RITMO`, `🔴 SOBREESFUERZO`, `🔵 POR DEBAJO`).

#### 📈 5. Tendencia 3D y Rampa Máxima (`3D Gradient Trend & Max Ramp`)
- **Descripción**: Anticipa los cambios de inclinación analizando la tendencia inmediata (↗️ Endureciendo, ➔ Estable, ↘️ Suavizando) e indica el % de rampa máxima alcanzada.

---

## 🇬🇧 ENGLISH

### 1. Installation & Setup
1. Transfer `ALTGRAPH-v0.5.0.apk` to your Karoo device via **ADB** (`adb install -r ALTGRAPH-v0.5.0.apk`).
2. On your Karoo, go to **Settings > Ride Profiles** and edit your preferred profile.
3. Add a new Data Field and select the **ALTGRAPH** category.

### 2. Tabbed Extension Settings Panel
Open **ALTGRAPH** from your Karoo app launcher to navigate 4 structured tabs:

* **🎨 STYLE Tab**:
  - **ALTIMETRY VIEW & MODEL (5 Models Suite Selector)**:
    - 🏔️ **Classic 3D (Default)**: Subtle 3D bevel, continuous 15-step monochromatic gradient from 0 to 15% (white to red), crisp cotas.
    - 🌅 **Horizon Isometric**: 3D cockpit perspective converging to horizon, runway center striping, and beacon headlight beam oriented by road slope.
    - ❄️ **Tactical Oases & Crucibles**: Highlights recovery shelves $\le 3\%$ in ice cyan with `❄️ OASIS [dist]m` badges and steep walls $\ge 12\%$ with `🔥 MURO [dist]m`, plus hypoxia altitude haze $>1400\text{m}$.
    - ⚡ **Dynamic Force Field**: Gravitational tension lines anchored along the slope, kinetic baseline sine wave, and floating target VAM pacing guideline.
    - 💎 **Monolithic Obsidian & Plasma**: Faceted smoky obsidian crystal mountain, polished top facet, inner radiant plasma core, and neon laser ridge.
  - **Font Family**: Google Sans / Condensed options.
  - **90° Landscape Rotation Mode**: Fullscreen 800x480px view.
  - **3D Slope Font Scale**: Normal, Large, Extra Large.
  - **Touch Zoom Controls toggle**: On/Off.

* **🏔️ 3D Tab**: Lookahead Distance (200m to 100km), Cotas, Ramp Flags, Ramp Min/Max Range, Hairpins, POI Filter, Topographic Method, Max Slope readout.
* **📊 2D Tab**: Profile Block Distance, Visible Blocks, Attack Alert Threshold.
* **🚴 VAM Tab**: Target VAM, Asphalt Quality Factor.

### 3. Available Data Fields (v0.5.0)

#### 🏔️ 1. 3D Altimetry
- **Description**: Next-generation 3D elevation profile with 5 selectable visualization models, 50m quantum rolling window, physical beacon climbing along top edge, adaptive multi-scale resolution, 15-step monochromatic gradient, high-contrast badges, triple header telemetry, GPS hairpins, POIs, and touch zoom.

#### 📊 2. Altimetry Profile & Strategy
- **Description**: Dynamic elevation profile divided into customizable distance blocks with attack warning alerts.

#### ⛰️ 3. Fatigue Grade Index (GF)
- **Description**: Displays accumulated fatigue difficulty based on the scientific formula:
  $$\text{GF} = \sum \text{DU}^* + \text{TA} + \left(\frac{\text{PMx}}{5}\right)$$

#### 🚴 4. ClimbPacing VAM Target — *Option C Dual*
- **Description**: Pacing assistant with instantaneous VAM (`850 m/h`), recommended target speed (`12.5 km/h`), and status badge.

#### 📈 5. 3D Gradient Trend & Max Ramp
- **Description**: Predicts slope changes (↗️ Steepening, ➔ Steady, ↘️ Easing) and tracks peak % ramp.



---
## 🆕 What's new in v0.5.0 / Novedades v0.5.0

### 🇪🇸 ESPAÑOL
- **Precisión Algorítmica Dinámica**: La Pendiente Media y la Rampa Máxima ahora se calculan analizando el relieve metro a metro en alta resolución (ignorando el suavizado de los bloques visuales) y se acotan estrictamente a la distancia que tengas en pantalla (200m, 5km, 100km...).
- **Cálculo de Pendiente Media Real**: La media del puerto ahora descarta automáticamente las bajadas y llaneos, haciendo el promedio matemático de esfuerzo puramente de las secciones de subida.
- **Grado de Fatiga (GF) y VAM Desligados del Zoom**: El motor científico de APM ya no sufre de "dilución de pendiente" al alejar el zoom. Evalúa la dureza real sobre los puntos brutos de la telemetría, ofreciendo resultados perfectos independientemente de tu configuración de escala.
- **Iconos de Cumbre (Map-Pin)**: Las antiguas balizas circulares se han sustituido por un icono profesional de chincheta topográfica blanca con el núcleo rojo para marcar los puertos oficiales.

### 🇬🇧 ENGLISH
- **Dynamic Algorithmic Precision**: Average Slope and Max Ramp are now calculated point-by-point in high resolution (ignoring visual block smoothing) and are strictly bounded to your current viewport distance (200m, 5km, 100km...).
- **True Average Grade Calculation**: The climb's average grade now automatically discards descents and flats, mathematically averaging the effort only on pure ascending sections.
- **Zoom-Independent Fatigue Grade (GF) & VAM**: The scientific APM engine no longer suffers from "slope dilution" when zooming out. It evaluates true hardness on raw telemetry points, providing perfect results regardless of your scale setting.
- **Map-Pin Summit Icons**: The old circular beacons have been replaced by a professional white topographic map-pin icon with a red core to mark official climbs.

### 🇫🇷 FRANÇAIS
- **Précision Algorithmique Dynamique** : La pente moyenne et la rampe maximale sont désormais calculées point par point en haute résolution (ignorant le lissage visuel) et sont strictement limitées à la distance affichée à l'écran (200m, 5km, 100km...).
- **Calcul de la Pente Moyenne Réelle** : La moyenne de l'ascension écarte automatiquement les descentes et les plats, calculant mathématiquement l'effort uniquement sur les sections montantes pures.
- **Indice de Fatigue (GF) et VAM Indépendants du Zoom** : Le moteur scientifique APM ne souffre plus de "dilution de la pente" lors du dézoom. Il évalue la véritable dureté sur les points de télémétrie bruts, offrant des résultats parfaits quelle que soit votre échelle.
- **Icônes de Sommet (Map-Pin)** : Les anciennes balises circulaires ont été remplacées par une icône professionnelle d'épingle topographique blanche avec un cœur rouge pour marquer les cols officiels.

### 🇮🇹 ITALIANO
- **Precisione Algoritmica Dinamica**: La pendenza media e la rampa massima sono ora calcolate punto per punto in alta risoluzione (ignorando l'arrotondamento visivo) e sono strettamente limitate alla distanza dello schermo (200m, 5km, 100km...).
- **Calcolo della Pendenza Media Reale**: La media della salita ora scarta automaticamente le discese e le parti in piano, calcolando matematicamente lo sforzo puramente delle sezioni di salita.
- **Grado di Fatica (GF) e VAM Indipendenti dallo Zoom**: Il motore scientifico APM non soffre più della "diluizione della pendenza" quando si rimpicciolisce. Valuta la vera durezza sui punti grezzi della telemetria, offrendo risultati perfetti indipendentemente dall'impostazione della scala.
- **Icone Vetta (Map-Pin)**: I vecchi fari circolari sono stati sostituiti da un'icona professionale a forma di spillo topografico bianco con nucleo rosso per segnare le salite ufficiali.

### 🇩🇪 DEUTSCH
- **Dynamische algorithmische Präzision**: Durchschnittliche Steigung und maximale Rampe werden nun Punkt für Punkt in hoher Auflösung berechnet (visuelle Glättung wird ignoriert) und sind streng an die aktuelle Bildschirmdistanz gebunden (200m, 5km, 100km...).
- **Echte Durchschnittssteigung**: Die Durchschnittssteigung des Anstiegs verwirft nun automatisch Abfahrten und flache Stücke und berechnet die mathematische Anstrengung nur auf reinen Steigungsabschnitten.
- **Zoom-unabhängiger Ermüdungsgrad (GF) & VAM**: Die wissenschaftliche APM-Engine leidet nicht mehr unter "Steigungsverdünnung" beim Herauszoomen. Sie bewertet die wahre Härte anhand der rohen Telemetriedaten und liefert unabhängig von der Skalierung perfekte Ergebnisse.
- **Karten-Pin Gipfelsymbole**: Die alten kreisförmigen Leuchtfeuer wurden durch ein professionelles weißes topografisches Karten-Pin-Symbol mit rotem Kern ersetzt, um offizielle Anstiege zu markieren.

---
## 🚀 What's new in v0.6.0 (World Tour Pro Features) / Novedades v0.6.0

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

---
## 👑 What's new in v1.1.0 ELITE (User-Defined Passes & Global Climb Library)

### 🇪🇸 ESPAÑOL
- **Puertos de Montaña Definidos por Usuario (POIs)**: Define con exactitud milimétrica el inicio (`INICIO: Nombre`) y la cima (`CIMA: Nombre [Cat]`) de tus puertos favoritos mediante POIs en Komoot, Strava, RideWithGPS o Karoo Dashboard. Prevalecen sobre cualquier puerto detectado automáticamente evitando divisiones erróneas por descansillos.
- **Persistencia por Ruta y Catálogo Global GPS**: Los puertos quedan guardados automáticamente tanto en la ruta activa como en una biblioteca global por coordenadas GPS de inicio y cima (estilo Hammerhead Dashboard / CLIMBER). Al cargar cualquier ruta que pase por ellos, se activan al instante.
- **Iconos Ciclistas Aerodinámicos Rediseñados**: Silueta vectorizada de ciclista contrarreloj / triatlón optimizada y ampliada en la barra de avisos.
- **Nombre de Puerto Configurable**: Opción en Ajustes de Estilo para mostrar el nombre del puerto o solo el icono sobre el gráfico del Visor de Puertos.

### 🇬🇧 ENGLISH
- **User-Defined Mountain Passes (POIs)**: Pinpoint start (`START: Name`) and summit (`SUMMIT: Name [Cat]`) of true climbs via POIs. Prevents algorithms from breaking climbs on false flats and overrides automatic detections.
- **Route & Global GPS Persistence**: Climbs are saved per route and into a global GPS library (start/summit coordinates). Any route traversing those coordinates automatically activates the climb.
- **Aero TT Cyclist Icons**: Enlarged aerodynamic time-trial vector icons for high legibility in the alert bar.
- **Configurable Climb Name**: Toggle between showing the climb name or just the icon above the Climb Viewer chart.

### 🇫🇷 FRANÇAIS
- **Cols Définis par l'Utilisateur (POIs)** : Marquez le début (`DEBUT: Nom`) et le sommet (`SOMMET: Nom [Cat]`) de vos cols réels via POIs. Prévalence totale sur les détections automatiques.
- **Persistance Itinéraire & Catalogue Global GPS** : Enregistrement par parcours et dans une bibliothèque GPS globale. Tout parcours passant par ces coordonnées active le col instantanément.
- **Icônes Cyclistes Aéro Rénovées** : Silhouette aérodynamique de contre-la-montre agrandie dans la barre d'alertes.
- **Nom du Col Configurable** : Option pour afficher le nom du col ou uniquement l'icône dans la Vue Cols.

### 🇮🇹 ITALIANO
- **Salite Definite dall'Utente (POI)** : Definisci inizio (`INIZIO: Nome`) e cima (`CIMA: Nome [Cat]`) con precisione assoluta. Prevalgono sui rilevamenti automatici evitando divisioni inopportune.
- **Persistenza per Percorso e Catalogo Globale GPS** : Salvataggio sul percorso e nella libreria GPS globale per il rilevamento automatico su qualsiasi rotta.
- **Icone Ciclisti Aero Riprogettate** : Sagoma cronometro / triathlon ingrandita nella barra avvisi.
- **Nome Salita Configurabile** : Scelta tra nome della salita o solo icona sul grafico del Visualizzatore Salite.

### 🇩🇪 DEUTSCH
- **Benutzerdefinierte Pässe (POIs)** : Bestimme Start (`START: Name`) und Gipfel (`SUMMIT: Name [Cat]`) exakt per POI. Vorrang vor automatischen Steigungen ohne Unterbrechungen durch Flachstücke.
- **Routenspeicher & Globaler GPS-Katalog** : Speicherung für die Route und im weltweiten GPS-Passkatalog des Geräts.
- **Aerodynamische Fahrer-Icons** : Vergrößerte Zeitfahr-Vektorsilhouette in der Hinweisleiste.
- **Passname konfigurierbar** : Schalter zur Anzeige des Passnamens oder nur des Icons über dem Pass-Viewer.

### 🇵🇹 PORTUGUÊS
- **Subidas Definidas pelo Utilizador (POIs)** : Defina início (`INICIO: Nome`) e cume (`CIMA: Nome [Cat]`) por POIs. Prevalecem sobre subidas automáticas mantendo a subida unificada.
- **Persistência por Rota e Catálogo Global GPS** : Gravação na rota e catálogo GPS global para ativação automática em qualquer traçado.
- **Ícones de Ciclistas Aerodinâmicos** : Silhueta de contrarrelógio ampliada na barra de alertas.
- **Nome da Subida Configurável** : Opção para exibir o nome da subida ou apenas o ícone no Visualizador de Subidas.

