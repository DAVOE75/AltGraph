<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="Logotipo ALTGRAPH" width="150" />
</p>
# ALTGRAPH (v2.8 REVOLUTION)
<p>Ler em: <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README.md"> Español</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_en.md"> English</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_fr.md"> Français</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_it.md"> Italiano</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_de.md"> Deutsch</a> | <a href="https://github.com/DAVOE75/ALTGRAPH/blob/main/README_pt.md"> Português</a></p>

**ALTGRAPH** é uma extensão profissional de altimetria e desempenho de ciclismo de última geração para os ciclocomputadores **Hammerhead Karoo** (Karoo 2 e Karoo 3), desenvolvida por **hesiOX** utilizando o SDK oficial `karoo-ext`.
Revoluciona o conceito tradicional de altimetria de ciclismo incorporando um **Conjunto de 7 Modelos de Visualização Altimétrica 3D**, telemetria biomecânica, análise metabólica e avisos por voz em tempo real.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/logo-1.png" alt="Logotipo ALTGRAPH" width="350" />
</p>

## VERSÃO 2.8: BALIZA PERSONALIZÁVEL, TRANSMISSÃO E MARCHA RECOMENDADA

* **Baliza do ciclista no 3D global**: posicionamento exato por distância (sem desfasamento) e opções de tamanho, forma (círculo, losango, seta) e cor no painel.
* **Marcha recomendada**: calculada pela inclinação na sua posição exata e pela sua **transmissão** (pratos e carretos configuráveis no separador 3D). O visualizador de subidas mostra «Rec. ⚙ XxY» pela inclinação média.
* **Novo ícone de ciclista KGhost**: branco com contorno preto interior para o rival virtual, preenchimento vermelho com contorno vermelho intenso para o ciclista; seta mais fina e curta.
* **Novos valores predefinidos**: rotação TV 2D e destaque de subidas ativos; barra de potência, barra em perspetiva isométrica, barra de energia e segmentos Strava desativados.
* Etiqueta «Barra de Zona de Treino» traduzida nos 6 idiomas.
* **Comportamento da baliza em 3D**: escolha entre baliza fixa (a 20%, o gráfico desloca-se; predefinição) ou baliza móvel com salto de página. Os blocos ficam ancorados à rota, pelo que as percentagens e cores já não mudam ao avançar.
* **Distância da rota**: total e restante no gráfico 3D. Se ocultar a marcha recomendada, o perfil desce e ganha altura.
* **Perfil mais limpo (opcional)**: une blocos vizinhos da mesma cor (Não / Auto conforme o zoom / Sempre) e oculta percentagens dentro de um intervalo (ex. de -2 a 2).
* **Etiqueta de inclinação «Limpa» (opcional)**: número branco alto sem caixa, com contorno exterior fino e sombra, e um pequeno % como símbolo de graus. Usa o seu tipo de letra e aplica-se ao 3D global e ao visualizador de subidas.
* **Melhorias 3D**: as etiquetas de inclinação ficam sobre o sombreado do troço percorrido, a etiqueta cresce com o zoom, a régua mostra as distâncias dos sub-blocos com zoom alto e as curvas em ferradura têm uma pequena linha vertical no gráfico.
* **Mapa**: a sobreposição de inclinação e a sua suavização são redesenhadas ao mudar a definição.

---

## VERSÃO 2.3: AVISOS POR VOZ DE SUPERFÍCIE (CASCALHO / TRILHA / ASFALTO) & SELETOR DE VOZ

Na versão 2.3, o AltGraph alarga as capacidades do **Copiloto de Voz** com a deteção do tipo de piso e personalização acústica avançada:

* **Avisos de Troços de Superfície (Cascalho / Trilha / Asfalto)**:
  * Deteção da transição de superfície através de waypoints/POIs no traçado (`gravel`, `grava`, `ripio`, `sterrato`, `kies`, `trail`, `sendero`, `mtb`, `singletrack`, `asfalto`, `asphalt`, `road`, `paved`).
  * Anúncio por voz antecipado ao entrar no setor indicando o tipo de superfície e a distância exata até à próxima transição. Exemplo: *"Entrando em trecho de cascalho, durante 2,4 quilómetros"* ou *"Entrando em trecho de trilha, durante 800 metros"*.
  * Interruptor independente nas definições **ELITE > Copiloto de Voz**.
* **Seletor de Voz Feminina / Masculina**:
  * Permite escolher nas definições entre timbre de voz feminino ou masculino para uma experiência à medida.
* **Sincronização e Rigor do Copiloto**:
  * Ajuste de cadência nos alertas de cume (500m e 100m) em tempo real sem latência.
  * Correção dos alertas de posição e ultrapassagem face ao ciclista virtual (Ghost).
  * Localização integral de todas as locuções nos 6 idiomas (Português, Espanhol, Inglês, Alemão, Francês, Italiano).

---

## VERSÃO 2.2: COPILOTO DE VOZ ALTIMÉTRICO PROFISSIONAL (ÁUDIO BLUETOOTH)

> [!NOTE]
> **Requisito de áudio no Karoo 2 e Karoo 3**: O altifalante/buzzer interno integrado nos dispositivos Hammerhead Karoo foi concebido a nível de hardware exclusivamente para sinais sonoros de sistema a volume fixo (beeps de radar e navegação). Para ouvir as locuções de voz humana sintetizada (Text-to-Speech) do Copiloto, é **indispensável emparelhar um dispositivo de áudio Bluetooth** (auriculares, auscultadores de condução óssea tipo Shokz ou uma mini coluna Bluetooth no guiador). Ao emparelhar o dispositivo Bluetooth, o sistema Karoo ativará de imediato o controlo de volume multimédia.

Na versão 2.2, o AltGraph introduz um sistema completo de **avisos por voz audíveis (Voice Copilot)** a correr em segundo plano via Bluetooth e configurável no menu **ELITE**:

* **Avisos de Subidas e Cumes**:
  * Anúncio por voz na aproximação ao início da subida com o nome do passo, quilómetros de ascensão, inclinação média e categoria oficial.
  * Progressão da subida: anúncio de cada quilómetro superado com leitura da inclinação atual.
  * Duplo aviso de cume: alerta prévio a 500 metros e aviso final a 100 metros de coroar.
* **Alerta de Agravamento de Inclinação (Slope Ahead)**:
  * Analisa o traçado seguinte e avisa com antecedência quando o troço ultrapassa o patamar de inclinação média configurado pelo ciclista (ajustável para 6%, 7%, 8%, 9%, 10% ou 12%). Exemplo: *"A subida agrava-se: próximos 500 metros a 10.4 por cento de média."*
* **Deteção de Descansos e Alívio de Inclinação**:
  * Notifica quando uma subida acentuada suaviza para permitir recuperação e ritmo constante.
* **Muros e Rampas Extremas**:
  * Aviso preventivo perante inclinações de 15% ou superiores para preparar a relação mecânica antecipadamente.
* **Curvas em Gancho (Apex Cues)**:
  * Avisos por voz na aproximação a ganchos apertados com forte desnível.
* **Marcos de Desnível Acumulado**:
  * Notificação por voz ao atingir marcos de ganho de elevação acumulado (a cada 250m ou 500m) acompanhada da altitude atual.
* **Resumo Periódico de Atividade**:
  * Locução periódica configurável (a cada 3, 5, 10, 15 ou 20 km) resumindo distância total, velocidade média e ganho de elevação acumulado.
* **Alerta de Cadência Baixa em Subida**:
  * Aviso preventivo quando a cadência cai abaixo de 60 rpm em inclinações superiores a 8%.
* **Alerta de Reserva Anaeróbica W'bal**:
  * Notificação prioritária quando a bateria anaeróbica fica abaixo dos 20% para evitar a exaustão.
* **Rádio-Corrida KGhost (Diferença e Ultrapassagem)**:
  * Leitura periódica da distância métrica e cronométrica face ao ciclista virtual (Ghost), configurável a cada 500m, 1 km, 2 km ou 5 km.
  * Avisos instantâneos quando assumes a liderança ou quando o Ghost te ultrapassa.
* **Pontos de Água (POIs)**:
  * Notificação sonora a menos de 150 metros de fontes e pontos de água registados no percurso.
* **Fidelidade Acústica e Gestão Inteligente de Áudio**:
  * Tom prévio de atenção (Pre-Chime): emite um sinal sonoro antes do início da fala para ativar atempadamente os auriculares Bluetooth e evitar que as primeiras sílabas sejam cortadas.
  * Ritmo de fala calibrado (0.95x) para máxima clareza com vento.
  * Motor TextToSpeech nativo com atenuação de música (audio ducking) e fila prioritária anti-sobreposição.

---

## VERSÃO 1.1.0 ELITE: PASSOS DE MONTANHA DEFINIDOS PELO UTILIZADOR (POIs)

A funcionalidade mais aguardada para cicloturismo e competição de topo: **definir com exatidão métrica quais as subidas que são verdadeiros passos de montanha, impedindo que algoritmos automáticos dividam a subida em duas devido a zonas planas ou falsos planos intermédios**.

### Como funciona?
1. **Deteção por POIs na rota (Komoot, Strava, RideWithGPS ou Karoo Dashboard)**:
 - Ponto de início: `INICIO: NomeSubida` ou `START: NomeSubida` (ex: `INICIO: Alto da Torre`).
 - Cume da subida: `CIMA: NomeSubida` ou `SUMMIT: NomeSubida` (ex: `CIMA: Alto da Torre`).
 - *Ou apenas Cume*: Se marcar apenas `CIMA: Serra da Estrela`, o AltGraph calcula de forma inteligente o início natural da subida no vale.
2. **Categorização Oficial e Forçada**:
 - **Automática segundo Vuelta / APM**: O AltGraph calcula distância, ganho de elevação, inclinação média e atribui a categoria oficial (4ª, 3ª, 2ª, 1ª ou Especial/HC).
 - **Categoria Forçada Manual**: Adicionando a categoria entre parênteses retos (ex: `CIMA: Torre [HC]`, `[1C]` ou `[2C]`), o sistema assume-a de imediato.
3. **Prevalência Total**:
 - As suas subidas personalizadas **substituem e anulam qualquer subida detetada automaticamente**, unindo os descansos e exibindo o nome real no **Visualizador de Subidas**, no **Perfil TV 2D** e na **Barra de Rota**.
4. **Persistência por Rota e Catálogo Global GPS (Estilo Dashboard)**:
 - **Ao nível da Rota**: A subida fica gravada para o itinerário ativo, sendo restaurada imediatamente em sessões futuras mesmo sem os POIs originais.
 - **Ao nível Global GPS**: Guardada com coordenadas GPS exatas de início e cume; qualquer outro percurso que passe pelas mesmas coordenadas reconhece e ativa a subida automaticamente!
5. **Ícones de Ciclistas Redesenhados**:
 - Novos ícones vetoriais aerodinâmicos de contrarrelógio / triatlo, aumentados para máxima legibilidade na barra de alertas.

## NOVA INTEGRAÇÃO: WIDGET KGHOST NO RADAR ALTIMÉTRICO (v1.0.85 ELITE)

Nas Funções Radar Elite, pode agora ativar o **Gap [KGhost](https://github.com/lockevod/KGhost)**, visualizando em tempo real na barra de alertas do Radar Altimétrico a disputa contra o ciclista virtual (Ghost):
- **Visualização Dupla Profissional**: Ciclista real em vermelho bordeaux e ciclista Ghost em cinzento claro metalizado com sombreamento subtil, ligados por uma seta dinâmica que aponta para o líder.
- **Posicionamento Tático em Tempo Real**: Se estiver na frente, o seu ciclista aparece na dianteira (à direita em horizontal, em cima em vertical). Se o Ghost liderar, ele assume a frente visual.
- **Métricas em Direto**: Diferença precisa em tempo e distância em tempo real (ex. `+4:46 / +3.7 km`).
- **Alternância Inteligente**: Ciclo fluido de 4 segundos entre o widget do Ghost e os avisos de montanha/paredes.
- **Agradecimento e Colaboração Especial**: Esta funcionalidade resulta da colaboração próxima entre a **AltGraph** e a aplicação [**KGhost**](https://github.com/lockevod/KGhost) desenvolvida para Hammerhead Karoo. Requer a instalação da app [**KGhost**](https://github.com/lockevod/KGhost), a cujos desenvolvedores agradecemos sinceramente pela cooperação para elevar o desempenho no ciclismo.

<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_settings.png" width="230" alt="Definição Gap KGhost no Radar Elite" />
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_kghost_radar.png" width="230" alt="Widget KGhost no Radar Altimétrico" />
</p>

## VERSÃO 1.0.78: PERFIL TV 2D (ELITE)

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

## VERSÃO 1.0.44: PACING DINÂMICO E TRANSIÇÕES SUAVES

- **Power Guide / Pacing (Novidade ELITE)**: Sistema inteligente de gestão de ritmo integrado no Radar Altimétrico. Com base no seu FTP, calcula os watts ideais em tempo real para cada secção da subida e mostra alertas estratégicos no ecrã (` ALVO: 240W` ou ` FORÇA: 300W`). O seu diretor desportivo no guiador!
- **Transições Suaves (Novidade ELITE)**: Opção para suavizar as transições visuais entre blocos de inclinação no Radar Altimétrico.

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_smooth_gradients_v2.png" width="400" alt="Transições Suaves" /></p>

## VERSÃO 1.0.43: TEXTURAS DE TERRENO (GRAVEL/BTT)

- **Texturização de Superfície (Novidade ELITE)**: Texturas visuais dinâmicas para Gravilha (Gravel) e Trilhos sobrepostas à inclinação 3D.

## ESTABILIDADE, RESOLUÇÃO E MOTOR DE RASTREIO (v1.0.10 - v1.0.40)

- **Resolução de Radar Personalizável**: Bloqueie a resolução em **20m, 50m, 100m, 150m, 300m, 500m ou 1km**.
- **Motor GPS Blindado**: Algoritmo reescrito para eliminar reinícios a 0 metros durante recálculos de rota.
- **Traduções Nativas**: 100% traduzido em português, inglês, espanhol, francês, italiano e alemão.

## VERSÃO 1.0.9: HISTOGRAMA DE ZONAS DE INCLINAÇÃO ELITE

- **Zonas de Inclinação (Histograma %)**: Analise a percentagem de tempo e distância percorrida em cada uma das 8 faixas de inclinação (desde descidas até inclinações extremas >17%).

<p align="center"><img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_gradient_zones.png" width="300" alt="Zonas de Inclinação" /></p>

## VERSÃO 1.0.5: ESCALA DINÂMICA E MAPA COLORIDO

| Rota Colorida | Detalhe de Rampas Duras | Vista de Subida Individual |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors.png" width="220" alt="Rota 1" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_2.png" width="220" alt="Rota 2" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_map_route_colors_3.png" width="220" alt="Subida" /> |

## VERSÃO 1.0.0: RADAR ALTIMÉTRICO E EXPERIÊNCIA PRO

### Radar Altimétrico (Sobreposição de Mapa 3D)
Sobrepõe o relevo do terreno diretamente no seu mapa de navegação Karoo.
<p align="center">
 <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_radar_altimetrico.png" width="400" alt="Radar Altimetrico" />
</p>

## Capturas de Ecrã Reais no Karoo 3

| Painel de Estilo | Menu de Seleção | Painel Completo |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_settings_estilo.png" width="220" alt="Estilo" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_data_selection.png" width="220" alt="Seleção" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_dashboard_completo.png" width="220" alt="Painel" /> |

| Altimetria 3D (200m) | Altimetria 3D (1km) | Altimetria 3D (10km) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile.png" width="220" alt="3D 200m" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_1km.png" width="220" alt="3D 1km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_10km.png" width="220" alt="3D 10km" /> |

| Altimetria 3D (20km) | Altimetria 3D (200km) | Visor de Subidas (3ª Cat) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_20km.png" width="220" alt="3D 20km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_200km.png" width="220" alt="3D 200km" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer.png" width="220" alt="Visor de Subidas 3ª Cat" /> |

| Visor de Subidas (HC) | Visor de Subidas (Isométrico) | GPS Isométrico (Global) |
| :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_especial.png" width="220" alt="Visor HC" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_climb_viewer_iso.png" width="220" alt="Visor Iso" /> | <img src="https://raw.githubusercontent.com/DAVOE75/ALTGRAPH/main/art/screenshot_3d_profile_gps.png" width="220" alt="GPS Iso" /> |

---

## Conjunto de 6 Modelos Altimétricos Revolucionários
1. ** Clássico 3D**: Relevo chanfrado elegante, sombreado suave de 0 a 15% e etiquetas nítidas.
2. ** Horizonte Isométrico**: Visão cockpit em perspetiva com feixe de luz dinâmico inclinado à rampa.
3. ** Oásis e Muros Táticos**: Destaque para áreas planas de recuperação (` OASIS`) e avisos de rampas duras (` MURO`).
4. ** Campo de Força e Fadiga**: Linhas de tensão gravitacional e guia de ritmo VAM.
5. ** Monólito de Obsidiana**: Cristal escuro facetado com núcleo de plasma brilhante.
6. ** Mapa Isométrico Global**: Reconstrução isométrica real da estrada gerada pelas coordenadas GPS.

### Escala de Inclinação de Alta Visibilidade

| Inclinação | Cor | Tonalidade | Sensação / Terreno |
| :--- | :---: | :--- | :--- |
| **≤ -10.0%** | | Azul marinho escuro | Descida muito acentuada |
| **-10.0% a -5.0%** | | Azul escuro | Descida acentuada |
| **-5.0% a -2.0%** | | Azul médio | Descida suave |
| **-2.0% a < 0.0%** | | Azul celeste | Falso plano a descer |
| **0.0% a 3.0%** | | Verde floresta intenso | Plano / Falso plano a subir |
| **3.0% a 5.0%** | | Amarelo forte | Subida moderada |
| **5.0% a 8.0%** | | Laranja intenso | Subida exigente |
| **8.0% a 10.0%** | | Laranja tijolo | Rampa dura |
| **10.0% a 13.0%** | | Vermelho intenso | Muro duro |
| **13.0% a 17.0%** | | Granate escuro | Muro extremo |
| **> 17.0%** | | Preto azeviche | Inclinação extrema (>17%) |

### Arquitetura Zero-Allocation para Karoo 3
* Motor de renderização sem alocação de memória: zero pausas do Garbage Collector (GC).
* Reutilização em tempo real de buffers Bitmap e Canvas.

---

## ALTGRAPH vs Climber Nativo do Karoo
ALTGRAPH não substitui o Climber nativo, mas complementa-o oferecendo uma **perspetiva gráfica total**:
- **Avanço Contínuo em Janela de 50m**: O perfil desliza suavemente com a cadência da pedalada.
- **6 Modelos Visuais**: Experiência personalizada adaptada ao seu estilo.
- **Zoom Tátil Instantâneo**: Alterne entre 200m, 1km, 10km, 20km e 50km com um simples toque no ecrã.
- **Grau de Fadiga (GF)**: Cálculo científico da dureza da subida baseado nos coeficientes APM.

---

## Instalação no Karoo 2 e Karoo 3
Baseado no SDK oficial `karoo-ext`, a instalação **não requer root** e é 100% segura.

**Via Sideloading (Recomendado para Karoo 3):**
1. Descarregue o ficheiro `app-release.apk` da versão mais recente nas [Releases do GitHub](https://github.com/DAVOE75/ALTGRAPH/releases).
2. Transfira-o para o Karoo através da aplicação oficial **Hammerhead Companion** no seu smartphone.
3. Uma vez instalada, o ALTGRAPH procura automaticamente futuras atualizações.

**Via ADB (Avançado):**
1. Ative as Opções de Programador e a depuração USB no Karoo.
2. Ligue o dispositivo ao computador e execute: `adb install -r app-release.apk`

---

#### Licença

Copyright © 2026 David García Pascual. **Todos os direitos reservados.** O código e os recursos do ALTGRAPH são obra original do autor e são publicados apenas para consulta. Não é permitido copiá-los, modificá-los, redistribuí-los nem reutilizá-los noutros projetos sem autorização prévia por escrito. Veja [LICENSE](LICENSE). As funções ELITE são atualmente gratuitas e passarão a ser pagas por subscrição.

---

## Apoiar o projeto

Se aprecia o ALTGRAPH e deseja apoiar o seu desenvolvimento contínuo:

<div align="center">
 <a href="https://buymeacoffee.com/hesiox" target="_blank">
 <img src="https://img.shields.io/badge/_Pague--me_um_café-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black" alt="Pague-me um café" />
 </a>
</div>
