<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="Logo ALTGRAPH" width="150" />
</p>
# ALTGRAPH (v2.8 REVOLUTION)
<p>Leggi in: <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README.md"> Español</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_en.md"> English</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_fr.md"> Français</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_it.md"> Italiano</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_de.md"> Deutsch</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_pt.md"> Português</a></p>

**ALTGRAPH** è un'estensione professionale per l'altimetria e le prestazioni ciclistiche di ultima generazione per i ciclocomputer **Hammerhead Karoo** (Karoo 2 e Karoo 3), sviluppata da **hesiOX** utilizando l'SDK ufficiale `karoo-ext`.
Rivoluziona il concetto tradizionale di altimetria ciclistica integrando una **Suite di 7 Modelli di Visualizzazione Altimetrica 3D**, telemetria biomeccanica, analisi metabolica e avvisi vocali in tempo reale.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="Logo ALTGRAPH" width="350" />
</p>

## VERSIONE 2.8: BALISE PERSONALIZZABILE, TRASMISSIONE E RAPPORTO CONSIGLIATO

* **Baliza del ciclista nel 3D globale**: posizionamento esatto per distanza (senza sfasamento) e opzioni di dimensione, forma (cerchio, rombo, freccia) e colore nel pannello.
* **Rapporto consigliato**: calcolato in base alla pendenza nella tua posizione esatta e alla tua **trasmissione** (corone e pignoni configurabili nella scheda 3D). Il visualizzatore dei passi mostra «Rec. ⚙ XxY» in base alla pendenza media.
* **Nuova icona ciclista KGhost**: bianca con bordo nero interno per il rivale virtuale, riempimento rosso con bordo rosso intenso per il ciclista; freccia più sottile e corta.
* **Nuovi valori predefiniti**: rotazione TV 2D ed evidenziazione delle salite attive; barra potenza, barra in prospettiva isometrica, barra energia e segmenti Strava disattivati.
* Etichetta «Barra zona di allenamento» tradotta in tutte e 6 le lingue.

---

## VERSIONE 2.3: AVVISI VOCALI TIPO DI SUPERFICIE (STERRATO / TRAIL / ASFALTO) & SELETTORE VOCE

Nella versione 2.3, AltGraph amplia le funzionalità del **Voice Copilot** con il rilevamento del tipo di superficie e la personalizzazione acustica:

* **Avvisi per Settori di Superficie (Sterrato / Sentiero / Asfalto)**:
  * Rilevamento automatico del fondo stradale tramite POI/waypoint nel percorso (`gravel`, `grava`, `ripio`, `sterrato`, `kies`, `trail`, `sendero`, `mtb`, `singletrack`, `asfalto`, `asphalt`, `road`, `paved`).
  * Avviso vocale anticipato all'ingresso del settore con tipo di superficie e lunghezza esatta fino alla variazione successiva. Esempio: *"Ingresso nel settore sterrato, per 2,4 chilometri"* o *"Ingresso nel settore sentiero, per 800 metri"*.
  * Interruttore dedicato nelle impostazioni **ELITE > Copilota Vocale**.
* **Selettore Voce Femminile / Maschile**:
  * Consente di scegliere nelle impostazioni tra timbro vocale femminile o maschile.
* **Precisione & Sincronizzazione Ottimizzate**:
  * Avvisi per la cima (500m e 100m) perfettamente calibrati in tempo reale senza ritardi.
  * Correzione degli annunci di posizione rispetto al rivale virtuale (Ghost) nei cambi al comando.
  * Localizzazione completa in tutte le 6 lingue (Italiano, Spagnolo, Inglese, Tedesco, Francese, Portoghese).

---

## VERSIONE 2.2: COPILOTA VOCALE ALTIMETRICO PROFESSIONALE (AUDIO BLUETOOTH)

> [!NOTE]
> **Requisito audio su Karoo 2 e Karoo 3**: L'altoparlante/cicalino interno integrato nei dispositivi Hammerhead Karoo è progettato a livello hardware esclusivamente per i bip di sistema a volume fisso (radar e svolte di navigazione). Per ascoltare la voce umana sintetizzata (Text-to-Speech) del Copilota, è **indispensabile collegare un dispositivo audio Bluetooth** (auricolari, cuffie a conduzione ossea tipo Shokz o un mini altoparlante Bluetooth sul manubrio). Una volta connesso il dispositivo Bluetooth, il sistema Karoo abiliterà automaticamente il cursore del volume multimediale.

Nella versione 2.2, AltGraph introduce un sistema completo di **avvisi vocali audibili (Voice Copilot)** in background tramite Bluetooth e configurabile nella sezione **ELITE**:

* **Annunci di Salite e Cime**:
  * Annuncio vocale in avvicinamento all'inizio della salita con nome, chilometri di ascesa, pendenza media e categoria ufficiale.
  * Progressione della salita: annuncio vocale a ogni chilometro superato con indicazione della pendenza istantanea.
  * Doppio avviso per la vetta: avvertimento a 500 metri e promemoria finale a 100 metri dallo scollinamento.
* **Avviso di Aumento di Pendenza (Slope Ahead)**:
  * Analizza il percorso e avverte in anticipo quando i metri successivi superano la soglia di pendenza media impostata dal ciclista (regolabile a 6%, 7%, 8%, 9%, 10% o 12%). Esempio: *"La salita si indurisce: prossimi 500 metri al 10.4 per cento di media."*
* **Rilevamento Falsipiani e Tratti di Recupero**:
  * Segnala quando un tratto ripido spiana, consentendo di rifiatare e rilanciare l'andatura.
* **Muri e Rampe Estreme**:
  * Avviso preventivo per rampe del 15% o superiori per impostare in anticipo il rapporto ideale.
* **Tornanti (Apex Cues)**:
  * Annunci vocali prima di tornanti stretti con marcata variazione di dislivello.
* **Traguardi di Dislivello Accumulato**:
  * Annuncio vocale al raggiungimento di traguardi di dislivello positivo (ogni 250m o 500m) con lettura dell'altitudine corrente.
* **Riepilogo Periodico della Corsa**:
  * Annuncio periodico configurabile (ogni 3, 5, 10, 15 o 20 km) con distanza percorsa, velocità media e dislivello accumulato.
* **Avviso di Cadenza Bassa in Salita**:
  * Segnale preventivo se la cadenza scende sotto le 60 pedalate al minuto su pendenze superiori all'8%.
* **Allerta Batteria Anaerobica W'bal**:
  * Avviso urgente quando la riserva anaerobica scende sotto il 20% per gestire le energie.
* **Radio-Corsa KGhost (Distacco e Sorpasso)**:
  * Lettura periodica di distacco metrico e cronometrico rispetto al ciclista virtuale (Ghost), personalizzabile ogni 500m, 1 km, 2 km o 5 km.
  * Avvisi istantanei di cambio comando quando superi il Ghost o vieni superato.
* **Punti d'Acqua (POI)**:
  * Notifica vocale a meno di 150 metri da fontane o rifornimenti d'acqua sul tracciato.
* **Fedeltà Acustica e Gestione Audio Intelligente**:
  * Tono preliminare di attenzione (Pre-Chime): emette un segnale acustico prima del parlato per riattivare le cuffie Bluetooth ed evitare frasi mozzate.
  * Cadenza vocale bilanciata (0.95x) per una chiarezza impeccabile controvento.
  * Motore TextToSpeech nativo con attenuazione musicale (audio ducking) e coda anti-sovrapposizione a priorità dinamica.

---

## VERSIONE 1.1.0 ELITE: PASSI DI MONTAGNA DEFINITI DALL'UTENTE (POI)

La novità più attesa per il cicloturismo e le granfondo: **definire con precisione millimetrica le vere salite di montagna ed evitare che gli algoritmi automatici spezzino in due un colle a causa di tratti pianeggianti o falsipiani intermedi**.

### Come funziona?
1. **Rilevamento tramite POI nel percorso (Komoot, Strava, RideWithGPS o Karoo Dashboard)**:
 - Punto iniziale: `INIZIO: NomeSalita` o `INICIO: NomeSalita` (es: `INIZIO: Passo dello Stelvio`).
 - Cima del passo: `CIMA: NomeSalita` o `PASSO: NomeSalita` (es: `CIMA: Passo dello Stelvio`).
 - *Oppure solo Cima*: Indicando solo `CIMA: Mortirolo`, AltGraph risale a ritroso lungo il rilievo fino a localizzare il fondovalle naturale.
2. **Categorizzazione Ufficiale e Forzata**:
 - **Automatica secondo Vuelta a España / APM**: AltGraph calcola distanza, dislivello accumulato, pendenza media e assegna la categoria ufficiale (4ª, 3ª, 2ª, 1ª o Fuori Categoria / HC).
 - **Categoria Forzata Manuale**: Aggiungendo la categoria tra parentesi quadre (es: `CIMA: Stelvio [HC]`, `[1C]`, o `[2C]`), il sistema la rispetta all'istante.
3. **Prevalenza Assoluta**:
 - Le salite personalizzate **sostituiscono e annullano le salite automatiche**, unendo i falsipiani e mostrando il nome reale nel **Visualizzatore Salite**, nel **Profilo TV 2D** e nella **Barra Percorso**.
4. **Persistenza per Percorso e Catalogo Globale GPS (Stile Dashboard)**:
 - **A livello di Percorso**: La salita resta memorizzata per l'itinerario attivo e viene ricaricata istantaneamente nelle uscite successive anche senza POI originari.
 - **A livello Globale GPS**: Salvata con coordinate GPS di inizio e cima esatte; qualunque altro percorso che transiti per gli stessi punti riconosce e attiva automaticamente la salita!
5. **Icone Ciclisti Ridisegnate**:
 - Nuove icone vettoriali aerodinamiche da cronometro / triathlon, ingrandite per una leggibilità perfetta nella barra degli avvisi.

## NUOVA INTEGRAZIONE: WIDGET KGHOST NEL RADAR ALTIMETRICO (v1.0.85 ELITE)

Nelle Funzioni Radar Elite è ora possibile attivare il **Gap [KGhost](https://github.com/lockevod/KGhost)**, visualizzando in tempo reale sulla barra degli avvisi del Radar Altimetrico la sfida contro il ciclista virtuale (Ghost):
- **Visualizzazione Doppia Professionale**: Ciclista reale in rosso bordeaux e ciclista Ghost in grigio chiaro metallizzato con sfumatura sottile, collegati da una freccia dinamica che punta verso il leader.
- **Posizionamento Tattico in Tempo Reale**: Se sei in vantaggio, il tuo ciclista compare in testa (a destra in orizzontale, in alto in verticale). Se il Ghost è avanti, prende la testa visiva.
- **Metriche Live**: Distacco preciso in tempo e distanza in tempo reale (es. `+4:46 / +3.7 km`).
- **Alternanza Intelligente**: Ciclo fluido di 4 secondi tra il widget del Ghost e gli avvisi di salite/muri.
- **Ringraziamenti e Collaborazione Speciale**: Questa funzionalità nasce dalla stretta collaborazione tra **AltGraph** e l'applicazione [**KGhost**](https://github.com/lockevod/KGhost) creata per Hammerhead Karoo. È necessario avere installata l'app [**KGhost**](https://github.com/lockevod/KGhost), e ringraziamo di cuore i suoi sviluppatori per la disponibilità e la cooperazione.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_settings.png" width="230" alt="Impostazione Gap KGhost in Radar Elite" />
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_radar.png" width="230" alt="Widget KGhost su Radar Altimetrico" />
</p>

## VERSIONE 1.0.78: PROFILO TV 2D (ELITE)

- **Profilo TV 2D (Novità ELITE)**: Nuovo campo dati visivo che replica i profili altimetrici delle trasmissioni televisive dei Grandi Giri (Giro d'Italia, Tour de France, La Vuelta). Disegna la sagoma dell'intera tappa e colora dinamicamente in tempo reale l'area percorsa con la tonalità neon preferita (**Rosa Giro d'Italia**, **Giallo Tour de France**, **Rosso La Vuelta**, Verde Classiche, Ciano o Bianco Puro).
- **Rilevamento Salite e Elenco Centrato**: Evidenziazione dei colli in grigio chiaro ad alto contrasto sul profilo e sulla minimappa del percorso, con intestazione centrata e dettagli completi (categorie, distanze e pendenze medie).
- **Rotazione Orizzontale a 90°**: Impostazione esclusiva nel menu Elite per ruotare il grafico di 90° e sfruttare la massima larghezza visiva sul Karoo.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/rutapuritoTV.png" width="500" alt="Profilo TV Bianco" />
</p>
<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv.png" width="250" alt="Profilo TV Giallo" />
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv1.png" width="250" alt="Profilo TV Rosa" />
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltvve.png" width="250" alt="Profilo TV Rosso" />
</p>

## VERSIONE 1.0.44: PACING DINAMICO E SFUMATURE FLUIDE

- **Power Guide / Pacing (Novità ELITE)**: Sistema intelligente di gestione del ritmo integrato nel Radar Altimetrico. In base alla tua FTP, Altgraph calcola in tempo reale i watt ideali per ogni tratto della salita e mostra avvisi strategici sullo schermo (` OBIETTIVO: 240W` o ` SPINGI: 300W`). Il tuo direttore sportivo personale sul manubrio!
- **Sfumature Fluide (Novità ELITE)**: Opzione per ammorbidire le transizioni visive tra blocchi di pendenza differente nel Radar Altimetrico.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_smooth_gradients_v2.png" width="400" alt="Sfumature Fluide" /></p>

## VERSIONE 1.0.43: TEXTURE DEL TERRENO (GRAVEL/MTB)

- **Texture di Superficie (Novità ELITE)**: Attiva texture dinamiche del terreno sul nastro 3D (pattern per ghiaia e sterrato sovrapposti al dislivello).

## STABILITÀ, RISOLUZIONE E MOTORE DI TRACCIAMENTO (v1.0.10 - v1.0.40)

- **Risoluzione Radar Personalizzabile**: Imposta la risoluzione su **20m, 50m, 100m, 150m, 300m, 500m o 1km**.
- **Motore di Tracciamento GPS Blindato**: Algoritmo riscritto per eliminare salti al km 0 durante i ricalcoli del percorso.
- **Traduzioni Native**: Completamente tradotto in italiano, inglese, spagnolo, francese, tedesco e portoghese.

## VERSIONE 1.0.9: ISTOGRAMMA DELLE ZONE DI PENDENZA ELITE

- **Zone di Pendenza (Istogramma %)**: Visualizza il tempo e la percentuale di distanza trascorsa in ciascuna delle 8 fasce di pendenza (dalla discesa alle pendenze estreme >17%).

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_gradient_zones.png" width="300" alt="Zone di Pendenza" /></p>

## VERSIONE 1.0.5: SCALA DINAMICA E TRACCIATO A COLORI

| Percorso a Colori | Dettaglio Tratti Duri | Vista Singola Salita |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors.png" width="220" alt="Percorso 1" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_2.png" width="220" alt="Percorso 2" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_3.png" width="220" alt="Salita" /> |

## VERSIONE 1.0.0: RADAR ALTIMETRICO ED ESPERIENZA PRO

### Radar Altimetrico (Sovrapposizione Mappa 3D)
Sovrappone l'altimetria prospettica direttamente sulla mappa di navigazione del Karoo.
<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_radar_altimetrico.png" width="400" alt="Radar Altimetrico" />
</p>

## Schermate Reali su Karoo 3

| Pannello Stile | Menu Selezione | Dashboard Completa |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="220" alt="Stile" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_data_selection.png" width="220" alt="Selezione" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_dashboard_completo.png" width="220" alt="Dashboard" /> |

| Altimetria 3D (200m) | Altimetria 3D (1km) | Altimetria 3D (10km) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="220" alt="3D 200m" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_1km.png" width="220" alt="3D 1km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_10km.png" width="220" alt="3D 10km" /> |

| Altimetria 3D (20km) | Altimetria 3D (200km) | Visore Salite (3ª Cat) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_20km.png" width="220" alt="3D 20km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_200km.png" width="220" alt="3D 200km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer.png" width="220" alt="Visore Salite 3ª Cat" /> |

| Visore Salite (HC) | Visore Salite (Isometrico) | GPS Isometrico (Globale) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_especial.png" width="220" alt="Visore HC" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_iso.png" width="220" alt="Visore Iso" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_gps.png" width="220" alt="GPS Iso" /> |

---

## Suite di 6 Modelli di Altimetria Rivoluzionari
1. ** Classica 3D**: Profilo con bisello 3D elegante, sfumatura continua 0–15% ed etichette nitide.
2. ** Orizzonte Isometrico**: Prospettiva cockpit 3D con fascio di luce dinamico inclinato alla pendenza.
3. ** Oasi e Crogioli Tattici**: Evidenziazione di tratti di recupero (` OASIS`) e allarmi per rampe ripide (` MURO`).
4. ** Campo di Forza e Fatica**: Linee di tensione gravitazionale e guida di ritmo VAM.
5. ** Monolito di Ossidiana**: Cristallo di ossidiana sfaccettato scuro con nucleo di plasma radiante.
6. ** Mappa Isometrica Globale**: Ricostruzione isometrica reale della strada dai dati GPS.

### Scala delle Pendenze ad Alta Visibilità

| Pendenza | Colore | Tonalità | Sensazione / Terreno |
| :--- | :---: | :--- | :--- |
| **≤ -10.0%** | | Blu navy scuro | Discesa molto ripida |
| **-10.0% a -5.0%** | | Blu scuro | Discesa sostenuta |
| **-5.0% a -2.0%** | | Blu medio | Discesa lieve |
| **-2.0% a < 0.0%** | | Celeste chiaro | Falsopiano in discesa |
| **0.0% a 3.0%** | | Verde foresta intenso | Pianura / Falsopiano in salita |
| **3.0% a 5.0%** | | Giallo intenso | Salita moderata |
| **5.0% a 8.0%** | | Arancione intenso | Salita impegnativa |
| **8.0% a 10.0%** | | Arancione mattone | Rampa dura |
| **10.0% a 13.0%** | | Rosso intenso | Muro duro |
| **13.0% a 17.0%** | | Granata scuro | Muro estremo |
| **> 17.0%** | | Nero pece | Pendenza estrema (>17%) |

### Architettura Zero-Allocation per Karoo 3
* Motore di rendering privo di allocazione di memoria: zero pause del Garbage Collector (GC).
* Riutilizzo in tempo reale dei buffer Bitmap e Canvas.

---

## ALTGRAPH vs Climber Nativo di Karoo
ALTGRAPH non sostituisce il Climber nativo, ma lo completa offrendo una **prospettiva grafica totale**:
- **Avanzamento Continuo in Finestra di 50m**: Il profilo scorre senza scatti rigidi, in perfetta sintonia con la pedalata.
- **6 Modelli Visivi**: Esperienza personalizzata rispetto a un'unica vista fissa.
- **Zoom Touch Istantaneo**: Passa tra 200m, 1km, 10km, 20km e 50km con un semplice tocco sullo schermo.
- **Grado di Fatica (GF)**: Calcolo scientifico della difficoltà della salita basato sugli algoritmi APM.

---

## Installazione su Karoo 2 e Karoo 3
Basato sull'SDK ufficiale `karoo-ext`, l'installazione **non richiede permessi di root** ed è sicura al 100%.

**Tramite Sideloading (Consigliato per Karoo 3):**
1. Scarica il file `app-release.apk` dall'ultima versione su [GitHub Releases](https://github.com/DAVOE75/ALTGRAPH/releases).
2. Trasferiscilo al tuo Karoo usando l'app ufficiale **Hammerhead Companion** sul tuo smartphone.
3. Una volta installata, ALTGRAPH cercherà automaticamente i futuri aggiornamenti.

**Tramite ADB (Avanzato):**
1. Abilita le Opzioni Sviluppatore e il debug USB sul Karoo.
2. Collega il dispositivo al PC ed esegui: `adb install -r app-release.apk`

---

#### Licenza

Copyright © 2026 David García Pascual. **Tutti i diritti riservati.** Il codice e le risorse di ALTGRAPH sono opera originale dell'autore e sono pubblicati solo a scopo di consultazione. Non è consentito copiarli, modificarli, ridistribuirli o riutilizzarli in altri progetti senza previa autorizzazione scritta. Vedi [LICENSE](LICENSE). Le funzioni ELITE sono attualmente gratuite e diventeranno a pagamento con abbonamento.

---

## Supporta il progetto

Se trovi utile ALTGRAPH e desideri supportarne lo sviluppo continuo:

<div align="center">
 <a href="https://buymeacoffee.com/hesiox" target="_blank">
 <img src="https://img.shields.io/badge/_Offrimi_un_caffè-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Offrimi un caffè" />
 </a>
</div>
