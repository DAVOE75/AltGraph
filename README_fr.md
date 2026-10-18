<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/icon.png" alt="Logo ALTGRAPH" width="150" />
</p>
# ALTGRAPH (v1.0.85 ELITE)
<p>Lire en : <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README.md">🇪🇸 Español</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_en.md">🇬🇧 English</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_fr.md">🇫🇷 Français</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_it.md">🇮🇹 Italiano</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_de.md">🇩🇪 Deutsch</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_pt.md">🇵🇹 Português</a></p>

**ALTGRAPH** est une extension professionnelle d'altimétrie et de performance cycliste de nouvelle génération pour les compteurs GPS **Hammerhead Karoo** (Karoo 2 et Karoo 3), développée par **hesiOX** à l'aide du SDK officiel `karoo-ext`.
Elle révolutionne le concept traditionnel de l'altimétrie cycliste en intégrant une **Suite de 6 Modèles de Visualisation Altimétrique 3D** avec sélecteur dynamique en direct, échelle monochromatique continue de 15 niveaux, avancée quantique en fenêtre glissante de 50 mètres, résolution adaptative multi-échelle, analyse des cols de montagne, calcul topographique optionnel (projection horizontale pure), détection mathématique des virages en épingle (tornanti), filtrage par catégories de POI (Villes, Fontaines, Belvédères, Sommets), commandes de zoom tactile, polices Google Sans / Condensed, mode paysage pivoté à 90°, allure **VAM** cible et calcul scientifique du **Degré de Fatigue (GF)**.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="Logo ALTGRAPH" width="350" />
</p>

## 👻 NOUVELLE INTÉGRATION : WIDGET KGHOST DANS LE RADAR ALTIMÉTRIQUE (v1.0.85 ELITE) 👻

Dans les Fonctions Radar Elite, vous pouvez désormais activer l'**Écart KGhost**, affichant en temps réel sur la barre d'alertes du Radar Altimétrique le duel face au coureur virtuel (Ghost) :
- **Rendu Visuel Double Professionnel** : Cycliste réel en rouge bordeaux et cycliste Ghost en gris clair métallisé avec ombrage subtil, reliés par une flèche dynamique pointant vers le leader.
- **Positionnement Tactique en Temps Réel** : Si vous menez, votre cycliste s'affiche en tête (à droite en horizontal ou en haut en vertical). Si le Ghost est devant, il prend la tête visuelle.
- **Métriques en Direct** : Écart précis en temps et distance en temps réel (ex. `+4:46 / +3.7 km`).
- **Alternance Intelligente** : Cycle fluide de 4 secondes entre le widget du Ghost et les alertes de cols/murs.
- **Remerciements et Collaboration Spéciale** : Cette fonctionnalité est le fruit de la collaboration étroite entre **AltGraph** et l'application **KGhost** pour Hammerhead Karoo. Nécessite l'application **KGhost** installée, dont nous remercions chaleureusement l'équipe pour leur coopération.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_radar.png" width="360" alt="Widget KGhost sur Radar Altimétrique" />
</p>

## 🔥 VERSION 1.0.78: PROFIL TV 2D (ELITE) 🔥

- **Profil TV 2D (Nouveauté ELITE)** : Nouveau champ de données visuel qui reproduit les profils altimétriques des retransmissions télévisées des Grands Tours (Tour de France, Giro, Vuelta). Il dessine la silhouette de l'étape et remplit dynamiquement en temps réel la zone parcourue avec votre couleur néon favorite (**Jaune Tour de France**, **Rose Giro d'Italia**, **Rouge La Vuelta**, Vert Classiques, Cyan ou Blanc Pur).
- **Détection des Cols et Liste Centrée** : Marquage des cols en gris clair contrasté sur le profil et sur la mini-carte de parcours, avec titre centré et détails complets (catégories, distances et pourcentages moyens).
- **Rotation Paysage 90°** : Réglage exclusif dans le menu Elite pour pivoter le graphique à 90° et profiter d'une largeur visuelle maximale sur Karoo.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/rutapuritoTV.png" width="500" alt="Profil TV Blanc" />
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv.png" width="250" alt="Profil TV Jaune" />
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv1.png" width="250" alt="Profil TV Rose" />
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltvve.png" width="250" alt="Profil TV Rouge" />
</p>

## 🚀 VERSION 1.0.44: PACING DYNAMIQUE & DÉGRADÉS FLUIDES 🚀

- **Power Guide / Pacing (Nouveauté ELITE)** : Système de gestion d'effort intelligent intégré au Radar Altimétrique. Basé sur votre FTP, Altgraph calcule en temps réel les watts idéaux pour chaque portion du col et affiche des alertes stratégiques à l'écran (`⚡ CIBLE : 240W` ou `🔴 POUSSEZ : 300W`). Votre propre directeur sportif sur le guidon !
- **Dégradés Fluides (Nouveauté ELITE)** : Option pour adoucir les transitions visuelles entre blocs de pourcentages différents sur le Radar Altimétrique.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_smooth_gradients_v2.png" width="400" alt="Dégradés Fluides" /></p>

## 🚀 VERSION 1.0.43: TEXTURES DE TERRAIN (GRAVEL/VTT) 🚀

- **Texturation de Surface (Nouveauté ELITE)** : Option pour appliquer des textures de terrain sur le ruban 3D (motifs de gravier et sentiers superposés au dénivelé).

## 🚀 STABILITÉ, RÉSOLUTION ET MOTEUR DE SUIVI (v1.0.10 - v1.0.40) 🚀

- **Résolution Personnalisable** : Choix forcé de la résolution du Radar (**20m, 50m, 100m, 150m, 300m, 500m ou 1km**).
- **Moteur de Suivi GPS Blindé** : Réécriture complète de l'algorithme d'interpolation sur polyline, éliminant tout saut au km 0.
- **Traductions Natives** : 100% traduit en français, anglais, espagnol, italien, allemand et portugais.

## 🚀 VERSION 1.0.9: HISTOGRAMME DES ZONES DE PENTE ELITE 🚀

- **Zones de Pente (Histogramme %)** : Visualisez le pourcentage de temps et de distance passé dans chaque zone de difficulté (de la descente aux pentes extrêmes >17%).

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_gradient_zones.png" width="300" alt="Zones de Pente" /></p>

## 🌟 VERSION 1.0.5: ÉCHELLE DYNAMIQUE ET TRACÉ COUPLÉ 🌟

| 🗺️ Parcours Coloré | 🗺️ Détail des Rampes | 🗺️ Vue d'un Col |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors.png" width="220" alt="Parcours 1" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_2.png" width="220" alt="Parcours 2" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_3.png" width="220" alt="Col" /> |

## 🚀 VERSION 1.0.0: RADAR ALTIMÉTRIQUE & EXPÉRIENCE PRO 🚀

### 🌟 Radar Altimétrique (Superposition Carte 3D)
Superpose le relief du terrain directement sur votre carte de navigation Karoo.
<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_radar_altimetrico.png" width="400" alt="Radar Altimetrique" />
</p>

## 📸 Captures d'Écran sur Karoo 3

| 🎨 Panneau Style | 🎛️ Menu Sélection | 📊 Dashboard Complet |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="220" alt="Style" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_data_selection.png" width="220" alt="Sélection" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_dashboard_completo.png" width="220" alt="Dashboard" /> |

| 🏔️ Altimétrie 3D (200m) | 🏔️ Altimétrie 3D (1km) | 🏔️ Altimétrie 3D (10km) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="220" alt="3D 200m" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_1km.png" width="220" alt="3D 1km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_10km.png" width="220" alt="3D 10km" /> |

| 🏔️ Altimétrie 3D (20km) | 🏔️ Altimétrie 3D (200km) | 🏔️ Visionneuse de Cols (3e Cat) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_20km.png" width="220" alt="3D 20km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_200km.png" width="220" alt="3D 200km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer.png" width="220" alt="Visionneuse 3e Cat" /> |

| 🏔️ Visionneuse de Cols (HC) | 🏔️ Visionneuse (Isométrique) | 🗺️ GPS Isométrique (Global) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_especial.png" width="220" alt="Visionneuse HC" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_iso.png" width="220" alt="Visionneuse Iso" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_gps.png" width="220" alt="GPS Iso" /> |

---

## 🌟 Suite de 6 Modèles Altimétriques Révolutionnaires
1. **🏔️ Classique 3D** : Biseau 3D élégant, ombrage doux 0-15% et étiquettes nettes.
2. **🌅 Horizon Isométrique** : Vue cockpit en perspective avec faisceau lumineux dynamique.
3. **❄️ Oasis et Creusets Tactiques** : Repérage des replats de récupération (`❄️ OASIS`) et alertes de murs raides (`🔥 MUR`).
4. **⚡ Champ de Force et Fatigue** : Lignes de tension gravitationnelle et ligne de rythme VAM.
5. **💎 Monolithe d'Obsidienne** : Cristal d'obsidienne sombre avec cœur de plasma éclatant.
6. **🗺️ Carte Isométrique Globale** : Vue aérienne 3D réelle construite depuis les coordonnées GPS.

### 🎨 Échelle de Pente à Haute Visibilité

| Pente | Couleur | Teinte | Sensation / Terrain |
| :--- | :---: | :--- | :--- |
| **≤ -10.0%** | 🔵 | Bleu marine foncé | Descente très raide |
| **-10.0% à -5.0%** | 🔵 | Bleu foncé | Descente soutenue |
| **-5.0% à -2.0%** | 🔵 | Bleu moyen | Descente modérée |
| **-2.0% à < 0.0%** | 🔵 | Bleu céleste | Faux-plat descendant |
| **0.0% à 3.0%** | 🟢 | Vert forêt intense | Plat / Faux-plat montant |
| **3.0% à 5.0%** | 🟡 | Jaune vif | Montée modérée |
| **5.0% à 8.0%** | 🟠 | Orange intense | Montée soutenue |
| **8.0% à 10.0%** | 🟠 | Orange brique | Rampe difficile |
| **10.0% à 13.0%** | 🔴 | Rouge intense | Mur exigeant |
| **13.0% à 17.0%** | 🟤 | Grenat foncé | Mur extrême |
| **> 17.0%** | ⚫ | Noir de jais | Pente extrême (>17%) |

### ⚡ Architecture Zero-Allocation pour Karoo 3
* Moteur de rendu sans allocation mémoire : zéro pause du Garbage Collector (GC).
* Réutilisation fluide des tampons Bitmap et Canvas.

---

## 🆚 ALTGRAPH vs Climber Natif du Karoo
ALTGRAPH ne remplace pas le Climber natif, mais offre une **perspective graphique enrichie** :
- **Avance Continue en Fenêtre de 50m** : Le relief défile avec fluidité au rythme de votre pédalage.
- **6 Modèles Graphiques** : Multiples représentations visuelles adaptées à chaque pratique.
- **Zoom Tactile Instantané** : Basculez entre 200m, 1km, 10km, 20km et 50km d'une simple pression.
- **Degré de Fatigue (GF)** : Indice scientifique de difficulté calculé selon les coefficients APM.

---

## 📲 Installation sur Karoo 2 et Karoo 3
Développé sur le SDK officiel `karoo-ext`, l'installation ne requiert **aucun root** et est 100% sécurisée.

**Via Sideloading (Recommandé sur Karoo 3) :**
1. Téléchargez `app-release.apk` depuis les [Releases GitHub](https://github.com/DAVOE75/ALTGRAPH/releases).
2. Transférez le fichier via l'application officielle **Hammerhead Companion** sur votre smartphone.
3. Une fois installée, ALTGRAPH vérifie automatiquement les futures mises à jour.

**Via ADB (Avancé) :**
1. Activez les options de développement et le débogage USB sur votre Karoo.
2. Connectez l'appareil au PC et lancez : `adb install -r app-release.apk`

---

## ☕ Soutenir le projet

Si vous appréciez ALTGRAPH et souhaitez encourager son développement continu :

<div align="center">
  <a href="https://buymeacoffee.com/hesiox" target="_blank">
    <img src="https://img.shields.io/badge/☕_Offrez--moi_un_café-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Offrez-moi un café" />
  </a>
</div>
