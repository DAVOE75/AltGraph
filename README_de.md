<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/icon.png" alt="ALTGRAPH Logo" width="150" />
</p>
# ALTGRAPH (v1.1.0 ELITE)
<p>Lesen auf: <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README.md"> Español</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_en.md"> English</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_fr.md"> Français</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_it.md"> Italiano</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_de.md"> Deutsch</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_pt.md"> Português</a></p>

**ALTGRAPH** ist eine professionelle Höhen- und Leistungserweiterung der nächsten Generation für **Hammerhead Karoo** Fahrradcomputer (Karoo 2 und Karoo 3), entwickelt von **hesiOX** unter Verwendung des offiziellen `karoo-ext` SDKs.
Es revolutioniert die Radsport-Höhenmessung durch eine **Suite von 6 3D-Höhenvisualisierungsmodellen** mit dynamischer Live-Umschaltung, einer kontinuierlichen 15-Stufen-Gradientenskala, einem gleitenden 50-Meter-Fenster, adaptiver Multiskalen-Auflösung, Passanalysen, optionaler topografischer Berechnung, mathematischer Tornanti-Erkennung (Haarnadelkurven), POI-Filterung, Touch-Zoom-Steuerung, Google Sans / Condensed Schriften, 90°-Querformatmodus, Ziel-**VAM**-Geschwindigkeit und wissenschaftlicher Berechnung des **Ermüdungsgrades (GF)**.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="ALTGRAPH Logo" width="350" />
</p>

## 🚀 VERSION 1.1.0 ELITE: BENUTZERDEFINIERTE BERGPÄSSE (POIs)

Das meisterwartete Feature für Radsport und Granfondos: **präzise festlegen, welche Anstiege echte Pässe sind, und verhindern, dass automatische Algorithmen Pässe durch Flachstücke oder Wellen in zwei Teile zerlegen**.

### 🏔️ Wie funktioniert es?
1. **Erkennung über POIs auf der Route (Komoot, Strava, RideWithGPS oder Karoo Dashboard)**:
   - Startpunkt: `START: PassName` oder `INICIO: PassName` (z. B. `START: Grossglockner`).
   - Gipfelpunkt: `SUMMIT: PassName` oder `CIMA: PassName` (z. B. `SUMMIT: Grossglockner`).
   - *Oder nur Gipfel*: Wird nur `SUMMIT: Stilfser Joch` markiert, ermittelt AltGraph automatisch den natürlichen Talbeginn.
2. **Offizielle & Überschriebene Einstufung**:
   - **Automatisch nach Vuelta / APM**: AltGraph ermittelt Distanz, Höhenmeter, Durchschnittssteigung und vergibt die offizielle Kategorie (4., 3., 2., 1. Kategorie oder HC).
   - **Manuelle Zwangskategorie**: Hänge die Kategorie in eckigen Klammern an (z. B. `SUMMIT: Grossglockner [HC]`, `[1C]` oder `[2C]`) und das System übernimmt sie direkt.
3. **Voller Vorrang**:
   - Benutzerdefinierte Pässe **ersetzen und überschreiben automatisch erkannte Steigungen**, schließen Flachstücke ein und zeigen den echten Namen im **Pass-Viewer**, im **TV-Profil 2D** und in der **Routenleiste**.
4. **Routenspeicher & Globaler GPS-Passkatalog (Dashboard-Stil)**:
   - **Auf Routenebene**: Der Pass wird für die aktive Route gespeichert und bei zukünftigen Fahrten sofort wiederhergestellt, selbst wenn die ursprünglichen POIs nicht mehr in der Datei enthalten sind.
   - **Im globalen GPS-Katalog**: Gespeichert mit exakten GPS-Koordinaten von Start und Gipfel. Jede andere Route, die dieselben Koordinaten durchfährt, erkennt und aktiviert den Pass automatisch!
5. **Überarbeitete Fahrer-Icons**:
   - Neue aerodynamische Zeitfahr- / Triathlon-Vektorfahrer, vergrößert für maximale Lesbarkeit in der Warnleiste.

## NEUE INTEGRATION: KGHOST-WIDGET IM HÖHENPROFIL-RADAR (v1.0.85 ELITE)

In den Radar-Elite-Funktionen kann jetzt der **[KGhost](https://github.com/lockevod/KGhost)-Abstand** aktiviert werden, wodurch ein Echtzeit-Duell gegen den virtuellen Ghost-Fahrer direkt in der Alarmleiste des Radars angezeigt wird:
- **Professionelle Doppel-Fahrer-Darstellung**: Echter Fahrer in Bordeaux-Rot und Ghost-Fahrer in metallischem Hellgrau mit dezentem Schatten, verbunden durch einen dynamischen Pfeil, der auf den Führenden zeigt.
- **Taktische Echtzeit-Positionierung**: Wer führt, fährt optisch vorne (rechts im Querformat, oben im Hochformat).
- **Live-Leistungsmetriken**: Präziser Abstand in Zeit und Distanz in Echtzeit (z. B. `+4:46 / +3.7 km`).
- **Intelligente Alternanz**: Sanfter 4-Sekunden-Wechsel zwischen dem Ghost-Widget und den taktischen Steigungswarnungen.
- **Besonderer Dank & Zusammenarbeit**: Dieses Feature entstand durch die enge Zusammenarbeit zwischen **AltGraph** und der [**KGhost**](https://github.com/lockevod/KGhost)-App für Hammerhead Karoo. Die Nutzung erfordert die installierte [**KGhost**](https://github.com/lockevod/KGhost)-App, deren Entwicklerteam wir herzlich für die Kooperation danken.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_settings.png" width="230" alt="KGhost Gap Einstellung in Radar Elite" />
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_radar.png" width="230" alt="KGhost Widget im Höhenprofil-Radar" />
</p>

## VERSION 1.0.78: TV-PROFIL 2D (ELITE)

- **TV-Profil 2D (ELITE-Neuheit)**: Neues visuelles Datenfeld, das die Höhenprofile großer Radsport-Übertragungen (Tour de France, Giro d'Italia, Vuelta a España) nachempfindet. Es zeichnet die Silhouette der Etappe und füllt den gefahrenen Bereich in Echtzeit mit der gewählten Neonfarbe (**Tour-Gelb**, **Giro-Rosa**, **Vuelta-Rot**, Klassiker-Grün, Cyan oder Reinweiß).
- **Passerkennung & zentrierte Anstiegsliste**: Kontrastreiche hellgraue Schattierung von Pässen auf Profil und Routen-Minimap, ergänzt durch eine zentrierte Aufschlüsselung von Kategorien, Distanzen und Durchschnittssteigungen.
- **90°-Querformat-Drehung**: Exklusiver Schalter im Elite-Menü zur 90°-Drehung des Graphen für maximale Bildschirmbreite auf dem Karoo.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/rutapuritoTV.png" width="500" alt="TV-Profil Weiß" />
</p>
<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv.png" width="250" alt="TV-Profil Gelb" />
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv1.png" width="250" alt="TV-Profil Rosa" />
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltvve.png" width="250" alt="TV-Profil Rot" />
</p>

## VERSION 1.0.44: DYNAMISCHES PACING & FLÜSSIGE VERLÄUFE

- **Power Guide / Pacing (ELITE-Neuheit)**: Intelligente Pacing-Führung im Altimetrie-Radar. Basierend auf deiner FTP berechnet Altgraph in Echtzeit die idealen Wattwerte für jeden Abschnitt des Anstiegs (` ZIEL: 240W` oder ` DRÜCKEN: 300W`). Dein sportlicher Leiter am Lenker!
- **Flüssige Verläufe (ELITE-Neuheit)**: Sanfte visuelle Übergänge zwischen Steigungsblöcken im Höhenradar.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_smooth_gradients_v2.png" width="400" alt="Flüssige Verläufe" /></p>

## VERSION 1.0.43: GELÄNDETEXTUREN (GRAVEL/MTB)

- **Oberflächentexturen (ELITE-Neuheit)**: Dynamische Texturmuster für Schotter (Gravel) und Trails direkt auf dem 3D-Band.

## STABILITÄT, AUFLÖSUNG & TRACKING (v1.0.10 - v1.0.40)

- **Einstellbare Radar-Auflösung**: Manuelle Festlegung auf **20m, 50m, 100m, 150m, 300m, 500m oder 1km**.
- **Robuste GPS-Interpolation**: Verhindert Sprünge zu km 0 bei Neuberechnungen.
- **Vollständige Übersetzungen**: 100% nativ in Deutsch, Englisch, Spanisch, Französisch, Italienisch und Portugiesisch.

## VERSION 1.0.9: STEIGUNGSZONEN-HISTOGRAMM ELITE

- **Steigungszonen (Histogramm %)**: Prozentuale Verteilung von Distanz und Fahrzeit über 8 Steigungsklassen.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_gradient_zones.png" width="300" alt="Steigungszonen" /></p>

## VERSION 1.0.5: DYNAMISCHE SKALA & FARBLINIE

| Farbige Route | Steile Rampen im Detail | Einzelne Passansicht |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors.png" width="220" alt="Route 1" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_2.png" width="220" alt="Route 2" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_3.png" width="220" alt="Pass" /> |

## VERSION 1.0.0: ALTIMETRIE-RADAR & PRO-MODUS

### Altimetrie-Radar (3D-Kartenüberlagerung)
Legt das vorausschauende Höhenprofil direkt über deine Karoo-Navigationskarte.
<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_radar_altimetrico.png" width="400" alt="Altimetrie-Radar" />
</p>

## Echte Screenshots auf dem Karoo 3

| Stil-Einstellungen | Datenfeld-Auswahl | Komplettes Dashboard |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="220" alt="Stil" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_data_selection.png" width="220" alt="Auswahl" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_dashboard_completo.png" width="220" alt="Dashboard" /> |

| 3D-Altimetrie (200m) | 3D-Altimetrie (1km) | 3D-Altimetrie (10km) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="220" alt="3D 200m" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_1km.png" width="220" alt="3D 1km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_10km.png" width="220" alt="3D 10km" /> |

| 3D-Altimetrie (20km) | 3D-Altimetrie (200km) | Pass-Viewer (3. Kat.) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_20km.png" width="220" alt="3D 20km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_200km.png" width="220" alt="3D 200km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer.png" width="220" alt="Pass 3. Kat." /> |

| Pass-Viewer (HC) | Pass-Viewer (Isometrisch) | Isometrisches GPS (Global) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_especial.png" width="220" alt="Pass HC" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_iso.png" width="220" alt="Pass Iso" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_gps.png" width="220" alt="GPS Iso" /> |

---

## Suite von 6 Revolutionären Altimetriemodellen
1. ** Klassisch 3D**: Elegantes 3D-Relief, sanfte 0–15%-Monochromtönung und scharfe Schilder.
2. ** Isometrischer Horizont**: Cockpit-Perspektive mit dynamisch geneigtem Lichtstrahl.
3. ** Taktische Oasen & Mauern**: Markierte Erholungsflachstücke (` OASIS`) und Rampenwarnungen (` MAUER`).
4. ** Kraftfeld & Ermüdung**: Gravitationslinien und schwebendes VAM-Führungstempo.
5. ** Obsidian-Monolith**: Dunkler Facettenkristall mit leuchtendem Plasmakern.
6. ** Globale Isometrische Karte**: Aus echten GPS-Koordinaten berechnetes 3D-Drahtgitter.

### Steigungsskala mit Hoher Sichtbarkeit

| Steigung | Farbe | Farbton | Fahrgefühl / Gelände |
| :--- | :---: | :--- | :--- |
| **≤ -10.0%** | | Dunkles Marineblau | Sehr steile Abfahrt |
| **-10.0% bis -5.0%** | | Dunkelblau | Mäßig steile Abfahrt |
| **-5.0% bis -2.0%** | | Mittelblau | Leichte Abfahrt |
| **-2.0% bis < 0.0%** | | Hellblau | Falsche Ebene bergab |
| **0.0% bis 3.0%** | | Waldgrün | Flach / Falsche Ebene bergauf |
| **3.0% bis 5.0%** | | Kräftiges Gelb | Mäßiger Anstieg |
| **5.0% bis 8.0%** | | Intensives Orange | Fordernder Anstieg |
| **8.0% bis 10.0%** | | Ziegelorange | Harte Rampe |
| **10.0% bis 13.0%** | | Intensives Rot | Steile Mauer |
| **13.0% bis 17.0%** | | Dunkles Granatrot | Extrem steile Wand |
| **> 17.0%** | | Pechschwarz | Extreme Steigung (>17%) |

### Zero-Allocation-Architektur für Karoo 3
* Speicherallokationsfreies Rendering: Null Unterbrechungen durch den Garbage Collector (GC).
* Wiederverwendung von Bitmap- und Canvas-Puffern in Echtzeit.

---

## ALTGRAPH vs. Nativer Karoo Climber
ALTGRAPH ergänzt den Climber durch eine **umfassende grafische Perspektive**:
- **Kontinuierliches 50m-Rollen**: Das Profil gleitet im Rhythmus deiner Trittfrequenz ohne ruckartige Balkensprünge.
- **6 Visuelle Modelle**: Maßgeschneidertes Erlebnis für jeden Fahrstil.
- **Sofortiger Touch-Zoom**: Wechsel zwischen 200m, 1km, 10km, 20km und 50km durch einfaches Antippen.
- **Ermüdungsgrad (GF)**: Wissenschaftliche Pass-Schwierigkeit nach APM-Kriterien.

---

## Installation auf Karoo 2 und Karoo 3
Basiert auf dem offiziellen `karoo-ext` SDK, erfordert **kein Root** und ist 100% sicher.

**Per Sideloading (Empfohlen für Karoo 3):**
1. Lade `app-release.apk` aus den [GitHub Releases](https://github.com/DAVOE75/ALTGRAPH/releases) herunter.
2. Übertrage die APK über die offizielle **Hammerhead Companion** App auf dein Karoo.
3. Nach der Installation prüft ALTGRAPH künftige Updates automatisch.

**Per ADB (Erweitert):**
1. Aktiviere Entwickleroptionen und USB-Debugging auf dem Karoo.
2. Verbinde das Gerät mit dem PC und führe aus: `adb install -r app-release.apk`

---

## Projekt unterstützen

Wenn dir ALTGRAPH gefällt und du die Weiterentwicklung unterstützen möchtest:

<div align="center">
 <a href="https://buymeacoffee.com/hesiox" target="_blank">
 <img src="https://img.shields.io/badge/_Spendier_einen_Kaffee-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Spendier einen Kaffee" />
 </a>
</div>
