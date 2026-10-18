<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/icon.png" alt="Logotipo ALTGRAPH" width="150" />
</p>
# ALTGRAPH (v1.0.85 ELITE)
<p>Ler em: <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README.md">🇪🇸 Español</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_en.md">🇬🇧 English</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_fr.md">🇫🇷 Français</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_it.md">🇮🇹 Italiano</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_de.md">🇩🇪 Deutsch</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_pt.md">🇵🇹 Português</a></p>

**ALTGRAPH** é uma extensão profissional de altimetria e desempenho de ciclismo de última geração para os ciclocomputadores **Hammerhead Karoo** (Karoo 2 e Karoo 3), desenvolvida por **hesiOX** utilizando o SDK oficial `karoo-ext`.
Revoluciona o conceito tradicional de altimetria de ciclismo incorporando um **Conjunto de 6 Modelos de Visualização Altimétrica 3D** com seletor dinâmico ao vivo, escala monocromática contínua de 15 níveis, avanço quântico com janela deslizante de 50 metros, resolução multiescala adaptável, análise de passagens de montanha, cálculo topográfico opcional (projeção horizontal pura), detecção matemática de curvas fechadas (tornanti), filtragem de POIs (Cidades, Fontes, Miradouros, Cumes), controlos de zoom por toque, tipos de letra Google Sans / Condensed, modo horizontal rodado a 90°, ritmo **VAM** alvo e cálculo científico do **Grau de Fadiga (GF)**.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="Logotipo ALTGRAPH" width="350" />
</p>

## 👻 NOVA INTEGRAÇÃO: WIDGET KGHOST NO RADAR ALTIMÉTRICO (v1.0.85 ELITE) 👻

Nas Funções Radar Elite, pode agora ativar o **Gap KGhost**, visualizando em tempo real na barra de alertas do Radar Altimétrico a disputa contra o ciclista virtual (Ghost):
- **Visualização Dupla Profissional**: Ciclista real em vermelho bordeaux e ciclista Ghost em cinzento claro metalizado com sombreamento subtil, ligados por uma seta dinâmica que aponta para o líder.
- **Posicionamento Tático em Tempo Real**: Se estiver na frente, o seu ciclista aparece na dianteira (à direita em horizontal, em cima em vertical). Se o Ghost liderar, ele assume a frente visual.
- **Métricas em Direto**: Diferença precisa em tempo e distância em tempo real (ex. `+4:46 / +3.7 km`).
- **Alternância Inteligente**: Ciclo fluido de 4 segundos entre o widget do Ghost e os avisos de montanha/paredes.
- **Agradecimento e Colaboração Especial**: Esta funcionalidade resulta da colaboração próxima entre a **AltGraph** e a aplicação **KGhost** desenvolvida para Hammerhead Karoo. Requer a instalação da app **KGhost**, a cujos desenvolvedores agradecemos sinceramente pela cooperação para elevar o desempenho no ciclismo.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_radar.png" width="360" alt="Widget KGhost no Radar Altimétrico" />
</p>

## 🔥 VERSÃO 1.0.78: PERFIL TV 2D (ELITE) 🔥

- **Perfil TV 2D (Novidade ELITE)**: Novo campo de dados visual que reproduz os perfis altimétricos das transmissões televisivas das Grandes Voltas (Tour de France, Giro d'Italia, La Vuelta). Desenha a silhueta da etapa e preenche dinamicamente em tempo real a área percorrida com o seu tom néon favorito (**Amarelo Tour de France**, **Rosa Giro d'Italia**, **Vermelho La Vuelta**, Verde Clássicas, Ciano ou Branco Puro).
- **Detecção de Subidas e Lista Centrada**: Destaque das montanhas em cinzento claro contrastado no perfil e no minimapa de rota, com cabeçalho centrado e detalhe completo (categorias, distâncias e inclinações médias).
- **Rotação Horizontal a 90°**: Ajuste exclusivo no menu Elite para rodar o gráfico 90º e desfrutar da largura visual máxima no Karoo.

<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/rutapuritoTV.png" width="500" alt="Perfil TV Branco" />
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv.png" width="250" alt="Perfil TV Amarelo" />
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltv1.png" width="250" alt="Perfil TV Rosa" />
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/perfiltvve.png" width="250" alt="Perfil TV Vermelho" />
</p>

## 🚀 VERSÃO 1.0.44: PACING DINÂMICO E TRANSIÇÕES SUAVES 🚀

- **Power Guide / Pacing (Novidade ELITE)**: Sistema inteligente de gestão de ritmo integrado no Radar Altimétrico. Com base no seu FTP, calcula os watts ideais em tempo real para cada secção da subida e mostra alertas estratégicos no ecrã (`⚡ ALVO: 240W` ou `🔴 FORÇA: 300W`). O seu diretor desportivo no guiador!
- **Transições Suaves (Novidade ELITE)**: Opção para suavizar as transições visuais entre blocos de inclinação no Radar Altimétrico.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_smooth_gradients_v2.png" width="400" alt="Transições Suaves" /></p>

## 🚀 VERSÃO 1.0.43: TEXTURAS DE TERRENO (GRAVEL/BTT) 🚀

- **Texturização de Superfície (Novidade ELITE)**: Texturas visuais dinâmicas para Gravilha (Gravel) e Trilhos sobrepostas à inclinação 3D.

## 🚀 ESTABILIDADE, RESOLUÇÃO E MOTOR DE RASTREIO (v1.0.10 - v1.0.40) 🚀

- **Resolução de Radar Personalizável**: Bloqueie a resolução em **20m, 50m, 100m, 150m, 300m, 500m ou 1km**.
- **Motor GPS Blindado**: Algoritmo reescrito para eliminar reinícios a 0 metros durante recálculos de rota.
- **Traduções Nativas**: 100% traduzido em português, inglês, espanhol, francês, italiano e alemão.

## 🚀 VERSÃO 1.0.9: HISTOGRAMA DE ZONAS DE INCLINAÇÃO ELITE 🚀

- **Zonas de Inclinação (Histograma %)**: Analise a percentagem de tempo e distância percorrida em cada uma das 8 faixas de inclinação (desde descidas até inclinações extremas >17%).

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_gradient_zones.png" width="300" alt="Zonas de Inclinação" /></p>

## 🌟 VERSÃO 1.0.5: ESCALA DINÂMICA E MAPA COLORIDO 🌟

| 🗺️ Rota Colorida | 🗺️ Detalhe de Rampas Duras | 🗺️ Vista de Subida Individual |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors.png" width="220" alt="Rota 1" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_2.png" width="220" alt="Rota 2" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_3.png" width="220" alt="Subida" /> |

## 🚀 VERSÃO 1.0.0: RADAR ALTIMÉTRICO E EXPERIÊNCIA PRO 🚀

### 🌟 Radar Altimétrico (Sobreposição de Mapa 3D)
Sobrepõe o relevo do terreno diretamente no seu mapa de navegação Karoo.
<p align="center">
  <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_radar_altimetrico.png" width="400" alt="Radar Altimetrico" />
</p>

## 📸 Capturas de Ecrã Reais no Karoo 3

| 🎨 Painel de Estilo | 🎛️ Menu de Seleção | 📊 Painel Completo |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="220" alt="Estilo" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_data_selection.png" width="220" alt="Seleção" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_dashboard_completo.png" width="220" alt="Painel" /> |

| 🏔️ Altimetria 3D (200m) | 🏔️ Altimetria 3D (1km) | 🏔️ Altimetria 3D (10km) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="220" alt="3D 200m" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_1km.png" width="220" alt="3D 1km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_10km.png" width="220" alt="3D 10km" /> |

| 🏔️ Altimetria 3D (20km) | 🏔️ Altimetria 3D (200km) | 🏔️ Visor de Subidas (3ª Cat) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_20km.png" width="220" alt="3D 20km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_200km.png" width="220" alt="3D 200km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer.png" width="220" alt="Visor de Subidas 3ª Cat" /> |

| 🏔️ Visor de Subidas (HC) | 🏔️ Visor de Subidas (Isométrico) | 🗺️ GPS Isométrico (Global) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_especial.png" width="220" alt="Visor HC" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_iso.png" width="220" alt="Visor Iso" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_gps.png" width="220" alt="GPS Iso" /> |

---

## 🌟 Conjunto de 6 Modelos Altimétricos Revolucionários
1. **🏔️ Clássico 3D**: Relevo chanfrado elegante, sombreado suave de 0 a 15% e etiquetas nítidas.
2. **🌅 Horizonte Isométrico**: Visão cockpit em perspetiva com feixe de luz dinâmico inclinado à rampa.
3. **❄️ Oásis e Muros Táticos**: Destaque para áreas planas de recuperação (`❄️ OASIS`) e avisos de rampas duras (`🔥 MURO`).
4. **⚡ Campo de Força e Fadiga**: Linhas de tensão gravitacional e guia de ritmo VAM.
5. **💎 Monólito de Obsidiana**: Cristal escuro facetado com núcleo de plasma brilhante.
6. **🗺️ Mapa Isométrico Global**: Reconstrução isométrica real da estrada gerada pelas coordenadas GPS.

### 🎨 Escala de Inclinação de Alta Visibilidade

| Inclinação | Cor | Tonalidade | Sensação / Terreno |
| :--- | :---: | :--- | :--- |
| **≤ -10.0%** | 🔵 | Azul marinho escuro | Descida muito acentuada |
| **-10.0% a -5.0%** | 🔵 | Azul escuro | Descida acentuada |
| **-5.0% a -2.0%** | 🔵 | Azul médio | Descida suave |
| **-2.0% a < 0.0%** | 🔵 | Azul celeste | Falso plano a descer |
| **0.0% a 3.0%** | 🟢 | Verde floresta intenso | Plano / Falso plano a subir |
| **3.0% a 5.0%** | 🟡 | Amarelo forte | Subida moderada |
| **5.0% a 8.0%** | 🟠 | Laranja intenso | Subida exigente |
| **8.0% a 10.0%** | 🟠 | Laranja tijolo | Rampa dura |
| **10.0% a 13.0%** | 🔴 | Vermelho intenso | Muro duro |
| **13.0% a 17.0%** | 🟤 | Granate escuro | Muro extremo |
| **> 17.0%** | ⚫ | Preto azeviche | Inclinação extrema (>17%) |

### ⚡ Arquitetura Zero-Allocation para Karoo 3
* Motor de renderização sem alocação de memória: zero pausas do Garbage Collector (GC).
* Reutilização em tempo real de buffers Bitmap e Canvas.

---

## 🆚 ALTGRAPH vs Climber Nativo do Karoo
ALTGRAPH não substitui o Climber nativo, mas complementa-o oferecendo uma **perspetiva gráfica total**:
- **Avanço Contínuo em Janela de 50m**: O perfil desliza suavemente com a cadência da pedalada.
- **6 Modelos Visuais**: Experiência personalizada adaptada ao seu estilo.
- **Zoom Tátil Instantâneo**: Alterne entre 200m, 1km, 10km, 20km e 50km com um simples toque no ecrã.
- **Grau de Fadiga (GF)**: Cálculo científico da dureza da subida baseado nos coeficientes APM.

---

## 📲 Instalação no Karoo 2 e Karoo 3
Baseado no SDK oficial `karoo-ext`, a instalação **não requer root** e é 100% segura.

**Via Sideloading (Recomendado para Karoo 3):**
1. Descarregue o ficheiro `app-release.apk` da versão mais recente nas [Releases do GitHub](https://github.com/DAVOE75/ALTGRAPH/releases).
2. Transfira-o para o Karoo através da aplicação oficial **Hammerhead Companion** no seu smartphone.
3. Uma vez instalada, o ALTGRAPH procura automaticamente futuras atualizações.

**Via ADB (Avançado):**
1. Ative as Opções de Programador e a depuração USB no Karoo.
2. Ligue o dispositivo ao computador e execute: `adb install -r app-release.apk`

---

## ☕ Apoiar o projeto

Se aprecia o ALTGRAPH e deseja apoiar o seu desenvolvimento contínuo:

<div align="center">
  <a href="https://buymeacoffee.com/hesiox" target="_blank">
    <img src="https://img.shields.io/badge/☕_Pague--me_um_café-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Pague-me um café" />
  </a>
</div>
