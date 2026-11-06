# Especificación `001`: Motor e Interfaz Base de `img2asciiIM`

| Campo | Valor |
| :--- | :--- |
| **ID de Especificación** | `001-core-img2ascii-im` |
| **Estado** | `IMPLEMENTADO — Verificado en hardware USB (Pixel 10 Pro XL) y suite TDD (2026-11-06)` |
| **Autor(es)** | `sdd-architect` |
| **Fecha de Creación** | `2026-11-06` |
| **Última Actualización** | `2026-11-06` |
| **Paquete Raíz** | `com.laralbir.img2asciiim` |
| **Repositorio** | `https://github.com/laralbir/img2asciiIM` |
| **Dependencias Previas** | Ninguna (Especificación fundacional del MVP) |

> **Gobernanza SDD → TDD (`AGENTS.md`):**
> Esta especificación ha sido revisada y **aprobada por el usuario (`APROBADO`)** con la **Opción A** seleccionada en todas las Decisiones de Diseño (`DD-01`..`DD-05`). Queda habilitada la ejecución de los Vertical Slices TDD en [`tasks.md`](tasks.md) bajo la regla inquebrantable de comenzar siempre cada tarea con un test automatizado fallando en estado **`RED`**.

---

## 1. Contexto y Problema

### 1.1 Situación Actual y Motivación
En numerosos escenarios reales de movilidad —especialmente los **planes gratuitos de Wi-Fi a bordo de aerolíneas comerciales** (*"Free In-Flight Messaging"*), conexiones satelitales básicas, cruceros o zonas de cobertura móvil saturada— el proveedor de red aplica inspección profunda de paquetes o listas blancas que **permiten únicamente el tráfico de texto plano** de aplicaciones de mensajería instantánea (**WhatsApp**, **Telegram** e **Instagram Direct**), bloqueando por completo la subida y descarga de fotografías, vídeos o notas de voz.

Cuando un pasajero necesita compartir visualmente una captura de tarjeta de embarque, el mapa de un punto de encuentro, una foto desde la ventanilla del avión, un documento almacenado en su carpeta sincronizada de **Google Drive** o un selfie rápido, la imagen queda retenida indefinidamente con el icono de "reloj/enviando".

Los conversores de imagen a ASCII existentes fracasan en este contexto por cuatro motivos técnicos:
1. **Dependen de servidores web**, los cuales también están bloqueados por el portal cautivo del avión.
2. **Generan salidas de 80–120 columnas** pensadas para monitores de escritorio que sufren saltos de línea forzados (*line-wrap*) ilegibles en burbujas estrechas de móvil.
3. **Ignoran las restricciones específicas de cada app IM**: WhatsApp y Telegram exigen bloques ```` ``` ```` para activar tipografía monoespaciada y Telegram corta a los `4.096` caracteres; **Instagram Direct no soporta bloques Markdown monoespaciados**, colapsa espacios normales en su fuente proporcional y rechaza mensajes de más de **`1.000` caracteres**.
4. **Piden permisos invasivos de lectura global** o agotan la memoria RAM (`OutOfMemoryError`) al abrir fotos modernas de 48 MP+.

### 1.2 Visión de `img2asciiIM`
**img2asciiIM** transforma **100% offline en el propio dispositivo** cualquier imagen procedente de galería, cámara, almacenamiento local/nube vía SAF (incluyendo **Google Drive**) o el menú *Compartir* de Android en una pieza compacta y nítida de arte de texto (**Braille Unicode `U+2800..U+28FF`**, **ASCII Monoespaciado** o **Bloques Unicode `█▓▒░`**), ajustada matemáticamente a la anchura de burbuja y al límite exacto de caracteres de **WhatsApp**, **Telegram** o **Instagram Direct**, lista para enviarse o copiarse en un solo toque.

### 1.3 Fuera de Alcance (*Non-Goals* del MVP `001`)
- Conversión de vídeo o GIFs animados a secuencias de mensajes de texto.
- Servidores backend propios, cuentas de usuario, analítica o telemetría en la nube.
- Envío automatizado en segundo plano mediante APIs privadas/bots no oficiales de WhatsApp, Telegram o Instagram (toda entrega usa los mecanismos estándar de Android `ACTION_SEND` y `ClipboardManager` iniciados por el usuario).
- Historial persistente en base de datos local de conversiones pasadas (previsto para una futura especificación `002` si el usuario lo solicita).

---

## 2. Historias de Usuario

| ID | Prioridad | Historia de Usuario | Valor Entregado |
| :--- | :---: | :--- | :--- |
| **HU-01** | `P0` | **Como** viajero con permisos de privacidad estrictos, **quiero** abrir imágenes desde mi galería, archivos locales, tarjeta SD o mi unidad de **Google Drive** sin otorgar permisos globales de lectura a la app, **para** elegir cualquier imagen con seguridad total. | Ingesta universal cero-permisos |
| **HU-02** | `P0` | **Como** usuario en pleno vuelo o en movimiento, **quiero** tomar una foto directa con la cámara o pulsar *Compartir → img2asciiIM* desde Google Drive o la Galería, **para** convertir una imagen al vuelo en menos de 3 toques. | Flujo inmediato desde cámara y Share Sheet |
| **HU-03** | `P0` | **Como** emisor del mensaje, **quiero** alternar entre Braille Unicode, ASCII clásico y Bloques, ajustando ancho, brillo, contraste, inversión (tema claro/oscuro) y *dithering* sobre una vista previa realista de burbuja, **para** garantizar que la imagen sea reconocible en el móvil del destinatario. | Legibilidad máxima en pantallas verticales |
| **HU-04** | `P0` | **Como** usuario de WhatsApp, Telegram o Instagram Direct, **quiero** elegir el perfil de mi app de destino, **para** que el formato tipográfico y la longitud del mensaje respeten automáticamente sus reglas (`65.536`, `4.096` o `1.000` caracteres, con o sin ```` ``` ````). | Cero mensajes truncados o deformados |
| **HU-05** | `P0` | **Como** pasajero conectado solo al plan de mensajería del avión, **quiero** enviar el texto directamente a la app elegida o copiarlo en 1 toque sin requerir internet para la conversión, **para** transmitir información visual cuando el envío de fotos está bloqueado. | Comunicación visual sobre canal de solo texto |
| **HU-06** | `P0` | **Como** usuario de Android moderno (incluyendo Pixel Launcher con iconos temáticos y tema claro/oscuro), **quiero** que `img2asciiIM` cuente con un icono adaptativo y monocromo propio, imágenes de lanzamiento (*launch/branding*) y una *Splash Screen* nativa instantánea sin pantallas en blanco ni demoras artificiales, **para** percibir una experiencia pulida desde el primer fotograma. | Identidad visual nativa y arranque instantáneo |

---

## 3. Requisitos Funcionales y Criterios de Aceptación (`Given / When / Then`)

### RF-01: Ingesta Universal de Imágenes (Sin Permisos Invasivos)

La aplicación **debe** ofrecer cuatro vías nativas de entrada de imágenes sin declarar `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` ni `MANAGE_EXTERNAL_STORAGE`:

1. **Selector Moderno de Fotos (`PickVisualMedia`):** Integración con Android Photo Picker (`PickVisualMediaRequest(ImageOnly)`) para seleccionar fotos de la galería local y proveedores de fotos en la nube integrados en el sistema.
2. **Selector Universal de Documentos SAF (`OpenDocument` con filtro `image/*`):** Acceso unificado mediante el Storage Access Framework a almacenamiento interno, carpeta `Descargas`, tarjetas SD, unidades USB OTG y proveedores `DocumentsProvider` en la nube instalados en el dispositivo (**Google Drive**, Microsoft OneDrive, Dropbox, Nextcloud).
3. **Captura Directa desde Cámara:** Disparo fotográfico a través de contrato seguro con `FileProvider` sobre directorio privado de caché `cacheDir/camera_captures/` (o visor `CameraX` según **DD-01**).
4. **Recepción Externa vía Share Sheet (`Intent.ACTION_SEND` con MIME `image/*`):** Registro de `<intent-filter>` en `MainActivity` para recibir `Intent.EXTRA_STREAM` cuando el usuario comparte una imagen desde Google Drive, Galería, gestor de archivos o navegador hacia `img2asciiIM` (soportando tanto arranque en frío como `onNewIntent` con actividad ya abierta).

#### Criterios de Aceptación de `RF-01`

- **CA-01.1 — Selección desde Photo Picker (`PickVisualMedia`)**
  - **Given:** El usuario se encuentra en la pantalla principal de `img2asciiIM` sin ningún permiso de almacenamiento concedido.
  - **When:** Pulsa la acción **"Galería"** y selecciona una fotografía JPEG/PNG/WebP/HEIC en el Photo Picker del sistema.
  - **Then:** La app decodifica la `Uri` recibida de forma submuestreada, normaliza su orientación EXIF y renderiza inmediatamente la vista previa de arte en texto usando el perfil IM activo.

- **CA-01.2 — Selección desde Google Drive / Almacenamiento vía SAF (`OpenDocument`)**
  - **Given:** El usuario tiene archivos de imagen en almacenamiento local, tarjeta SD o proveedores SAF como **Google Drive**.
  - **When:** Pulsa **"Archivos / Drive"** y elige un documento `image/*` en el selector SAF del sistema.
  - **Then:** La app lee el flujo de bytes mediante `ContentResolver.openInputStream(uri)` sin requerir permisos de almacenamiento y genera el arte en texto correspondiente.

- **CA-01.3 — Captura directa con la Cámara**
  - **Given:** El dispositivo dispone de hardware de cámara (`PackageManager.FEATURE_CAMERA_ANY`).
  - **When:** El usuario pulsa **"Cámara"**, captura una fotografía y confirma la toma.
  - **Then:** La foto almacenada en el archivo temporal gestionado por `FileProvider` se decodifica respetando la rotación EXIF del sensor, se muestra convertida en la burbuja de vista previa y los temporales antiguos se limpian sin dejar basura persistente.

- **CA-01.4 — Recepción directa y segura desde Android Share Sheet (`ACTION_SEND`)**
  - **Given:** El usuario visualiza una imagen dentro de **Google Drive** o la app **Galería** del sistema.
  - **When:** Pulsa *Compartir*, elige **`img2asciiIM`** en el Share Sheet de Android (`Intent.ACTION_SEND` con `type = "image/*"` y `EXTRA_STREAM`, tanto en arranque en frío `onCreate` como en caliente `onNewIntent`).
  - **Then:** `img2asciiIM` valida que `uri.scheme == "content"` y que el `ContentProvider` emisor no pertenezca al propio UID de la aplicación (defensa *Confused Deputy*), carga y convierte la imagen directamente en pantalla sin pedir al usuario que vuelva a seleccionarla.

- **CA-01.5 — Cancelación inocua de cualquier selector o cámara**
  - **Given:** Ya existe una imagen convertida en pantalla con ajustes personalizados por el usuario.
  - **When:** El usuario abre Galería, Archivos/Drive o Cámara y cancela la operación volviendo atrás (`resultCode == RESULT_CANCELED` o `uri == null`).
  - **Then:** El arte previamente generado y todos los controles permanecen intactos sin mostrar mensajes de error.

---

### RF-02: Motor de Conversión 100% Offline en Dispositivo

El motor de dominio **debe** ejecutar todo el pipeline de imagen a texto localmente en el dispositivo, sin llamadas de red, ofreciendo tres modos de caracteres y controles fotográficos en tiempo real:

1. **Modos de Caracteres (`CharsetMode`):**
   - **`BRAILLE_UNICODE` (`⠀`–`⣿`, `U+2800`–`U+28FF`):** Cada carácter codifica una submatriz binaria de **2×4 subpíxeles** (duplicando la resolución horizontal y cuadruplicando la vertical respecto a ASCII). Para cualquier celda cuyos 8 subpíxeles estén apagados, el motor **debe** emitir `U+2800` (*Braille Pattern Blank* `'⠀'`) y **jamás** el espacio ASCII `' '` (`U+0020`), evitando el colapso horizontal de espacios en fuentes proporcionales.
   - **`ASCII_CLASSIC`:** Rampa calibrada por densidad óptica sin espacios ASCII `U+0020` ni acentos graves `` ` `` (`U+0060`): `"@%#*+=-:."` (9 niveles, usando `'.'` como extremo claro para impedir que WhatsApp invalide el bloque ```` ``` ```` cuando el primer o último píxel es claro y evitando cierres prematuros de bloque).
   - **`UNICODE_BLOCKS`:** Rampa de bloques sombreados de alto impacto visual sin `U+0020` (`"█▓▒░."` en entornos monoespaciados o `"█▓▒░⠀"` en fuentes proporcionales).
2. **Controles en Tiempo Real:**
   - **Ancho en columnas (`widthColumns`):** Rango configurable `12..72` columnas (acotado dinámicamente si el presupuesto de caracteres del perfil activo lo exige).
   - **Brillo (`brightness`):** Desplazamiento continuo en `[-1.0, +1.0]` (por defecto `0.0`).
   - **Contraste (`contrast`):** Factor multiplicativo alrededor del punto medio `0.5` en `[0.5, 3.0]` (por defecto `1.0`).
   - **Inversión Claro/Oscuro (`invert`):** Invierte el mapeo luminancia → densidad de tinta para adaptar el arte según el receptor use **tema oscuro** (tinta clara sobre burbuja oscura: píxeles brillantes usan más puntos/tinta) o **tema claro** (tinta oscura sobre burbuja clara: píxeles oscuros usan más puntos/tinta).
   - **Dithering (`DitherMode`):** `NONE` (umbral directo), `FLOYD_STEINBERG` (difusión 7/16, 3/16, 5/16, 1/16) y `ATKINSON` (difusión 1/8 a 6 vecinos con alto contraste en siluetas y rostros).
3. **Fidelidad Geométrica:**
   - Normalización automática de las 8 orientaciones **EXIF** antes de muestrear.
   - Compensación de la **relación de aspecto de celda tipográfica** (`cellWidthToHeightRatio`, calibrada a `~0.50` ancho/alto por carácter) para evitar que las imágenes aparezcan alargadas verticalmente al representarse como texto.

#### Criterios de Aceptación de `RF-02`

- **CA-02.1 — Codificación exacta Braille Unicode `U+2800..U+28FF` sin colapso de blancos**
  - **Given:** Una imagen de entrada procesada en modo `BRAILLE_UNICODE`.
  - **When:** Una región de 2×4 subpíxeles tiene 0 puntos activos, otra tiene los 8 puntos activos y otra activa únicamente la diagonal superior-izquierda `(0,0)` e inferior-derecha `(1,3)`.
  - **Then:** El motor emite respectivamente `U+2800` (`'⠀'`), `U+28FF` (`'⣿'`) y `U+2881` (`0x2800 + 0x01 + 0x80 = '⢀'` + punto 1 = `'⢁'`), y la cadena resultante **no contiene ningún espacio ASCII `U+0020`**.

- **CA-02.2 — Inversión simétrica para Tema Oscuro vs. Tema Claro**
  - **Given:** Una imagen mitad izquierda negra pura (`RGB 0,0,0`) y mitad derecha blanca pura (`RGB 255,255,255`).
  - **When:** Se convierte en modo `UNICODE_BLOCKS` con `invert = false` (pensado para burbuja clara / tinta oscura) y luego con `invert = true` (pensado para burbuja oscura / tinta clara).
  - **Then:** Con `invert = false` la mitad negra genera `'█'` y la mitad blanca el carácter vacío; al activar `invert = true`, las densidades se intercambian exactamente.

- **CA-02.3 — Preservación de proporciones geométricas y rotación EXIF**
  - **Given:** Una fotografía cuadrada (`1000 × 1000 px`) o una imagen rectangular con etiqueta EXIF `ORIENTATION_ROTATE_90`.
  - **When:** Se convierte a `widthColumns = 32` con factor de aspecto de celda `0.5`.
  - **Then:** La imagen con EXIF `90°` aparece enderezada y la imagen cuadrada genera `16` filas de caracteres (`32 cols × 16 filas`, proporción visual 1:1 en pantalla), evitando deformación vertical.

- **CA-02.4 — Difusión de error en gradientes grises (`FLOYD_STEINBERG` / `ATKINSON`)**
  - **Given:** Una imagen con gradiente uniforme al 50% de luminancia (`Y = 0.5`).
  - **When:** Se convierte a `BRAILLE_UNICODE` con `DitherMode.FLOYD_STEINBERG` o `DitherMode.ATKINSON`.
  - **Then:** El resultado contiene una trama intermedia distribuida (densidad media de puntos encendidos entre `35%` y `65%`) en lugar de colapsar toda la superficie a `100%` blanco (`⠀`) o `100%` lleno (`⣿`).

- **CA-02.5 — Funcionamiento íntegro en Modo Avión**
  - **Given:** El dispositivo tiene todas las interfaces de red desactivadas (Modo Avión) o conectadas a un Wi-Fi de avión sin salida a internet.
  - **When:** El usuario carga una imagen disponible localmente o en caché y modifica modo, ancho, brillo, contraste o *dithering*.
  - **Then:** Todas las conversiones se completan localmente con éxito y cero errores de red.

---

### RF-03: Perfiles Optimizados por App de Mensajería (*IM Presets*)

El sistema **debe** proporcionar tres perfiles preconfigurados que aplican las reglas reales de renderizado y límites de longitud de cada plataforma:

| Perfil (`ImPlatformPreset`) | Paquete Canónico Android | Envoltorio Monoespaciado | Modo Recomendado Inicial | Ancho Inicial (Rango Recomendado) | Límite Estricto |
| :--- | :--- | :---: | :--- | :---: | :---: |
| **WhatsApp** | `com.whatsapp` | Sí: ```` ```<arte>``` ```` *(sin `U+0020` inicial)* | `ASCII_CLASSIC` *(admite todos)* | `32` cols (`28–36` cols) | **`65.536`** chars *(práctico `≤ 4.000`)* |
| **Telegram** | `org.telegram.messenger` | Sin ```` ``` ```` en Braille / ```` ```\n<arte>\n``` ```` en ASCII | **`BRAILLE_UNICODE`** *(o `ASCII_CLASSIC`)* | `30` cols (`28–38` cols) | **`4.096`** chars UTF-16 |
| **Instagram Direct** | `com.instagram.android` | **No** *(texto plano directo)* | **`BRAILLE_UNICODE`** *(o `UNICODE_BLOCKS`)* | `22` cols (`16–24` cols) | **`1.000`** chars *(y cota `≤ 1.000` bytes UTF-8)* |

> **Referencias técnicas auditadas:**
> - Restricciones de red en avión y clientes IM: [`docs/IM_CONSTRAINTS.md`](../../docs/IM_CONSTRAINTS.md)
> - Seguridad de Intents, SAF, Photo Picker y AGP 9: [`docs/ANDROID_ARCHITECTURE_AND_SECURITY.md`](../../docs/ANDROID_ARCHITECTURE_AND_SECURITY.md)

#### Criterios de Aceptación de `RF-03`

- **CA-03.1 — Formato monoespaciado y ancho óptimo en WhatsApp**
  - **Given:** El usuario selecciona el perfil **WhatsApp**.
  - **When:** Se genera el mensaje final para cualquier imagen.
  - **Then:** El texto de salida comienza exactamente por ```` ```\n ```` y termina por ```` \n``` ````, utiliza por defecto un ancho de `32` columnas para evitar *line-wrap* en pantallas verticales y nunca excede `65.536` caracteres totales.

- **CA-03.2 — Cumplimiento estricto del presupuesto de `4.096` caracteres en Telegram**
  - **Given:** El usuario selecciona el perfil **Telegram** con una imagen muy vertical (ej. captura de pantalla `9:21`) y solicita `widthColumns = 70` (lo cual produciría > `4.096` caracteres).
  - **When:** El motor calcula la salida para Telegram.
  - **Then:** El texto final incluye el envoltorio ```` ```\n...\n``` ```` y su longitud total (`output.length`, incluyendo los 8 caracteres del envoltorio y todos los `\n`) es **menor o igual a `4.096` caracteres** (aplicando la política de auto-ajuste o partición definida en **DD-02**), informando en la UI del ajuste aplicado.

- **CA-03.3 — Regla crítica de Instagram Direct (Sin ```` ``` ````, Braille Unicode y `≤ 1.000` caracteres)**
  - **Given:** El usuario cambia el perfil activo a **Instagram Direct**.
  - **When:** Se aplica el preset y se renderiza la imagen.
  - **Then:**
    1. El modo de caracteres cambia automáticamente por defecto a **`BRAILLE_UNICODE`** (`U+2800..U+28FF`).
    2. El texto generado **NO** incluye envoltorios ```` ``` ```` (dado que Instagram DM los mostraría literalmente sin cambiar la fuente).
    3. La longitud total del mensaje (`output.length`, incluyendo saltos de línea `\n`) es garantizadamente **`≤ 1.000` caracteres**.
    4. Si el usuario intenta seleccionar manualmente `ASCII_CLASSIC` bajo el perfil Instagram Direct, la UI muestra un aviso explícito de que Instagram usa fuente proporcional y puede desalinear columnas ASCII.

---

### RF-04: Vista Previa de Burbuja, Envío Directo y Portapapeles en 1 Toque

La pantalla principal **debe** mostrar una vista previa fiel del mensaje e integrar entrega inmediata:

1. **Vista Previa de Burbuja de Chat:** Simula visualmente la burbuja de la app seleccionada (fuente monoespaciada cuando el perfil usa ```` ``` ````, fuente proporcional del sistema cuando el perfil es Instagram Direct), permitiendo conmutar la vista previa entre fondo claro y oscuro, y mostrando en vivo `caracteres_actuales / límite_máximo` y dimensiones `columnas × filas`.
2. **Copia en 1 Toque:** Botón primario **"Copiar"** que escribe el mensaje formateado final en `ClipboardManager` (`ClipData.newPlainText`), disparando respuesta háptica suave y confirmación visual (respetando la superposición nativa de portapapeles en Android 13+).
3. **Envío Directo Dirigido + Patrón *Clipboard-First* + Fallback Seguro:** Botón **"Enviar a `<WhatsApp | Telegram | Instagram>`"** que lanza un `Intent(Intent.ACTION_SEND)` con `type = "text/plain"`, `Intent.EXTRA_TEXT = formattedMessage` y `setPackage(targetPackage)` (con bloque `<queries>` en `AndroidManifest.xml` para API 30+). Para **Instagram Direct** (cuyo receptor `ACTION_SEND` `text/plain` puede ignorar texto sin URL en algunas versiones), copia siempre primero al portapapeles (*Clipboard-First*) antes de abrir Instagram o el selector. Si el paquete destino no está instalado o lanza `ActivityNotFoundException` / `SecurityException`, **debe** abrir automáticamente `Intent.createChooser(...)` sin fallar jamás.

#### Criterios de Aceptación de `RF-04`

- **CA-04.1 — Copia exacta en 1 toque al portapapeles**
  - **Given:** Existe un arte generado para el perfil activo (ej. WhatsApp con bloque ```` ``` ```` o Instagram DM en Braille sin ```` ``` ````).
  - **When:** El usuario pulsa el botón **"Copiar"**.
  - **Then:** El portapapeles del sistema contiene exactamente la cadena formateada lista para pegar en el chat y el usuario recibe confirmación inmediata.

- **CA-04.2 — Envío directo cuando la app de mensajería está instalada**
  - **Given:** El perfil activo es **Telegram** (`org.telegram.messenger`) y dicha aplicación está instalada en el dispositivo.
  - **When:** El usuario pulsa **"Enviar a Telegram"**.
  - **Then:** Se construye y lanza un `Intent.ACTION_SEND` (`text/plain`) con `package = "org.telegram.messenger"` y `EXTRA_TEXT` idéntico al mensaje validado (`≤ 4.096` chars).

- **CA-04.3 — Fallback seguro al Share Sheet del sistema si la app no está instalada**
  - **Given:** El perfil activo es **Instagram Direct** (`com.instagram.android`), pero Instagram **no** está instalado en el dispositivo.
  - **When:** El usuario pulsa **"Enviar a Instagram"**.
  - **Then:** La aplicación no lanza `ActivityNotFoundException`; en su lugar abre el Share Sheet nativo de Android (`Intent.createChooser`) con el texto generado e informa discretamente al usuario.

---

### RF-05: Identidad Visual Nativa — Icono Adaptativo, Imágenes de Launch y Splash Screen

La aplicación **debe** contar con una identidad visual propia, coherente con [`docs/assets/banner.png`](../../docs/assets/banner.png) y 100% alineada con las especificaciones modernas de Android (API 26–37), reemplazando por completo cualquier recurso genérico del template de proyecto:

1. **Icono Adaptativo y Temático (`@mipmap/ic_launcher` y `@mipmap/ic_launcher_round` en `mipmap-anydpi-v26`):**
   - **Geometría y Zona Segura:** Lienzo de `108dp × 108dp` respetando estrictamente la **zona segura central de `66dp × 66dp`** (margen perimetral de `21dp` por lado para evitar recortes bajo máscaras circulares, *squircle*, lágrima o redondeadas de distintos fabricantes OEM).
   - **Tres Capas Vectoriales/Gráficas Dedicadas:**
     - `<background>` (`@drawable/ic_launcher_background`): Superficie oscura técnica corporativa con acento sutil de rejilla.
     - `<foreground>` (`@drawable/ic_launcher_foreground`): Glifo principal simbolizando la transformación de imagen/vuelo en una matriz nítida de puntos ASCII/Braille dentro de una burbuja de mensajería instantánea.
     - **`<monochrome>`** (`@drawable/ic_launcher_monochrome`): Silueta vectorial monocroma de un único canal con transparencia limpia para soporte nativo de **Themed Icons / Material You en Android 13+ (API 33+)**.
   - **Fallbacks Rasterizados `.webp`:** Iconos `ic_launcher.webp` e `ic_launcher_round.webp` en `mipmap-mdpi` (`48×48 px`), `mipmap-hdpi` (`72×72 px`), `mipmap-xhdpi` (`96×96 px`), `mipmap-xxhdpi` (`144×144 px`) y `mipmap-xxxhdpi` (`192×192 px`) regenerados a partir del nuevo arte maestro (eliminando el androide verde genérico del template).

2. **Splash Screen Nativa Instantánea (`androidx.core:core-splashscreen`, API 26–37):**
   - **Integración Oficial Cero-Demora:** Uso exclusivo de la API `SplashScreen` de Jetpack invocando `installSplashScreen()` en `MainActivity.onCreate()` **antes** de `super.onCreate(savedInstanceState)`, seguido inmediatamente de `enableEdgeToEdge()`. **Queda prohibido** crear actividades intermedias (`SplashActivity`) o introducir esperas artificiales (`delay()`, `Thread.sleep()`, bloqueos innecesarios en `setKeepOnScreenCondition`).
   - **Tema de Arranque `Theme.Img2asciiIM.Starting` (`parent="Theme.SplashScreen"`):** Definido en `res/values/themes.xml`, `res/values-night/themes.xml` y `res/values-v31/themes.xml`, configurando:
     - `windowSplashScreenBackground`: Color de fondo adaptado a modo claro y modo oscuro corporativo (evitando destellos blancos al abrir la app de noche o en cabina de avión a oscuras).
     - `windowSplashScreenAnimatedIcon`: `@drawable/ic_splash_logo` (`VectorDrawable` nítido o `AnimatedVectorDrawable` `≤ 300 ms` según **DD-05**, inscrito estrictamente dentro de la **zona segura central de `160dp × 160dp`** sobre un lienzo de `240dp × 240dp`).
     - `windowSplashScreenBrandingImage` (Android 12+ / API 31+): `@drawable/ic_splash_branding` (`200dp × 80dp`, marca tipográfica monoespaciada limpia `img2asciiIM`).
     - `postSplashScreenTheme`: `@style/Theme.Img2asciiIM`, transfiriendo atómicamente al tema principal `Edge-to-Edge` de la aplicación tras el primer fotograma.

3. **Imágenes Maestras de Launch y Distribución (`docs/assets/` y tienda):**
   - **Icono Maestro de Alta Resolución (`512 × 512 px`):** `docs/assets/icon-512.png` (PNG RGBA de alta fidelidad listo para ficha de tienda y distribución).
   - **Gráfico Promocional de Lanzamiento / *Feature Graphic* (`1024 × 500 px`):** `docs/assets/launch-feature-1024x500.png` (composición visual coherente con `docs/assets/banner.png` y la estética de la aplicación, sin texto crítico en los márgenes de recorte).

#### Criterios de Aceptación de `RF-05`

- **CA-05.1 — Icono adaptativo completo con capa `<monochrome>` y densidades `mipmap-*` sin activos del template**
  - **Given:** Los recursos empaquetados de `img2asciiIM` instalados en cualquier versión soportada de Android (API 26–37, incluyendo launchers con *Themed Icons* activados en Android 13+).
  - **When:** El sistema resuelve `@mipmap/ic_launcher` y `@mipmap/ic_launcher_round`.
  - **Then:** Ambos descriptores XML en `mipmap-anydpi-v26` declaran `<background>`, `<foreground>` y `<monochrome>` con lienzo `108dp × 108dp` y contenido principal en la zona segura `66dp × 66dp`, y todas las variantes `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` contienen archivos `.webp` válidos con dimensiones exactas (`48`, `72`, `96`, `144`, `192` px) cuyos hashes difieren de los recursos genéricos del template.

- **CA-05.2 — Arranque en frío con `SplashScreen` nativa y transición inmediata a `Theme.Img2asciiIM` + `Edge-to-Edge`**
  - **Given:** La aplicación se inicia en frío (*cold start*) tanto desde el launcher como mediante un `Intent.ACTION_SEND` con tema del sistema en modo claro u oscuro.
  - **When:** Se ejecuta `MainActivity.onCreate()`.
  - **Then:** El sistema muestra inmediatamente `Theme.Img2asciiIM.Starting` con `windowSplashScreenBackground` e `ic_splash_logo` (más `ic_splash_branding` en Android 12+), invoca `installSplashScreen()` antes de `super.onCreate()` sin `SplashActivity` intermedia ni `delay()` artificial, y conmuta a `@style/Theme.Img2asciiIM` con `enableEdgeToEdge()` tan pronto como el primer fotograma de Compose está listo.

- **CA-05.3 — Activos maestros de launch (`512×512` y `1024×500`) validados en dimensiones y formato**
  - **Given:** El directorio documental y de publicación `docs/assets/` del proyecto.
  - **When:** Se ejecuta la validación automatizada de activos de marca.
  - **Then:** Existen `docs/assets/icon-512.png` (PNG válido de exactamente `512 × 512 px`) y `docs/assets/launch-feature-1024x500.png` (PNG válido de exactamente `1024 × 500 px`), alineados estéticamente con `docs/assets/banner.png` y los vectores del launcher.

### RF-06: Respeto de Márgenes de Pantalla Móvil e Insets del Sistema (`Edge-to-Edge`)

La aplicación **debe** garantizar un diseño ergonómico adaptable tanto al sistema operativo como a las limitaciones físicas de visualización en clientes de mensajería móvil:

- **Criterio 06.1 — Margen de Navegación del Sistema en Barra de Acciones (`RNF-07`):**
  - **Given:** La app ejecutándose en cualquier dispositivo Android con navegación gestual o barra clásica de 3 botones.
  - **When:** Se renderiza la barra inferior de acciones fijas (`BottomActionControls`).
  - **Then:** El contenedor de botones aplica `navigationBarsPadding()`, asegurando una separación táctil mínima de los botones respecto al borde inferior del sistema operativo sin solapamiento de la barra de navegación.

- **Criterio 06.2 — Alerta de Desbordamiento de Ancho Móvil (> 32 Columnas):**
  - **Given:** Una conversión configurada con ancho `widthColumns > 32`.
  - **When:** Se observa la vista previa del mensaje o se ajusta el slider de columnas.
  - **Then:** La interfaz muestra una advertencia visual destacada indicando que superar ~32 columnas provocará saltos de línea automáticos y deformación en clientes móviles de mensajería instantánea como WhatsApp y Telegram, sugiriendo usar 28–32 columnas o el modo Braille HD.

### RF-07: Generación y Decodificación de Enlaces Autónomos de Alta Definición (`Self-Contained Deep Links #data=...`)

La aplicación **debe** permitir adjuntar opcionalmente un enlace web estándar (`https://laralbir.github.io/img2asciiIM/#data=...`) que contiene la imagen fotográfica comprimida y codificada en Base64 URL-safe dentro del hash fragment de la URL, funcionando como puente híbrido entre el arte de texto instantáneo y la fotografía original:

- **Criterio 07.1 — Compresión y Codificación Autónoma de Enlace en Cliente:**
  - **Given:** Una imagen cargada en la aplicación y el preset WhatsApp o Telegram activo.
  - **When:** El usuario activa la opción «Adjuntar enlace de alta resolución».
  - **Then:** El sistema comprime una versión miniatura optimizada de la imagen (JPEG/WebP compacto de ~4–10 KB), la codifica en Base64 URL-safe y genera la URL `https://laralbir.github.io/img2asciiIM/#data=<payload>`, asegurando que el mensaje total (arte de texto + salto + enlace) no supere el límite de caracteres de la plataforma (`65.536` en WhatsApp). En Instagram DM, la opción se deshabilita automáticamente dado su límite estricto de 1.000 caracteres.

- **Criterio 07.2 — Ingesta y Reconstrucción por Deep Link (`Intent.ACTION_VIEW`):**
  - **Given:** Un usuario en un dispositivo con `img2asciiIM` instalada que pulsa un enlace `https://laralbir.github.io/img2asciiIM/#data=...` o `?data=...` recibido en un chat.
  - **When:** Android resuelve el intent `VIEW` hacia `MainActivity`.
  - **Then:** La aplicación extrae el payload de datos del intent URI, reconstruye los píxeles originales en memoria de forma 100% offline (sin petición de red) y carga la imagen en la vista previa a todo color y alta resolución.

- **Criterio 07.3 — Visor Web Estático de Respaldo (`docs/index.html`):**
  - **Given:** Un destinatario que pulsa el enlace en un navegador web o en un dispositivo sin la app instalada.
  - **When:** El navegador abre `https://laralbir.github.io/img2asciiIM/#data=...`.
  - **Then:** La página estática HTML/JS (alojable en GitHub Pages sin servidor ni backend) lee `window.location.hash`, extrae el payload Base64 y dibuja la fotografía completa directamente en un `<canvas>` o `<img>` en el navegador.

### RF-08: Publicación de Releases Firmadas en GitHub y Actualizaciones Automáticas In-App

La aplicación **debe** contar con un sistema automatizado de integración y entrega continua (CI/CD) para publicar APKs firmadas con control de versiones semántico (`vX.Y.Z`) en GitHub Releases, así como un mecanismo nativo in-app para comprobar la existencia de nuevas versiones y guiar al usuario en su actualización sin depender de tiendas comerciales:

- **Criterio 08.1 — Configuración Segura de Firma de Release en Gradle (`app/build.gradle.kts`):**
  - **Given:** Un entorno de compilación de release (local o GitHub Actions runner).
  - **When:** Se ejecuta `./gradlew assembleRelease`.
  - **Then:** Gradle carga las credenciales de firma desde variables de entorno (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `KEYSTORE_PASSWORD`) o `keystore.properties` si existen. En ausencia de credenciales de producción (ej. desarrollo local), recurre limpiamente a la clave de depuración (`debug.keystore`) permitiendo generar APKs instalables sin romper el build.

- **Criterio 08.2 — Pipeline CI/CD Automatizado en GitHub Actions (`.github/workflows/release.yml`):**
  - **Given:** Un push de etiqueta Git con formato de versión semántica `v*.*.*` (ej. `v1.0.0`).
  - **When:** El flujo de trabajo de GitHub Actions se ejecuta en `ubuntu-latest` con JDK 21.
  - **Then:** Se ejecutan todos los tests unitarios (`./gradlew testDebugUnitTest`), se compila el APK firmado `app-release.apk`, se calcula su suma criptográfica SHA-256 (`sha256sum app-release.apk > app-release.apk.sha256`) y se publica automáticamente la Release en GitHub con el APK, el fichero de checksum y las notas de cambios generadas a partir de Conventional Commits.

- **Criterio 08.3 — Comprobador In-App de Nuevas Versiones (`GitHubUpdateChecker`):**
  - **Given:** La app en ejecución y conectividad de red disponible.
  - **When:** El usuario solicita comprobar actualizaciones (o la app realiza una comprobación opcional contra `https://api.github.com/repos/laralbir/img2asciiIM/releases/latest`).
  - **Then:** La aplicación obtiene los metadatos JSON de la última release, extrae el tag (ej. `v1.1.0`), las notas de la versión y la URL directa del APK adjunto, y compara la versión contra `BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`. Si existe una versión superior, expone el estado para notificar al usuario.

- **Criterio 08.4 — Descarga e Instalación Asistida del APK:**
  - **Given:** Una nueva versión detectada por el comprobador.
  - **When:** El usuario pulsa «Actualizar» o «Descargar APK».
  - **Then:** La app facilita la descarga directa del APK de la release o abre el navegador en el asset oficial de GitHub Releases para instalar la actualización.

- **Criterio 08.5 — Resiliencia Total Offline (Sin Fallos en Vuelo):**
  - **Given:** El usuario en Modo Avión o conectado a una red sin salida a internet (Wi-Fi de solo mensajería).
  - **When:** La app comprueba actualizaciones.
  - **Then:** La comprobación detecta la ausencia de conectividad o fallo de red y finaliza de manera silenciosa o con un aviso discreto («Sin conexión para comprobar actualizaciones»), sin provocar bloqueos, excepciones no capturadas ni degradar el conversor de imágenes.

---

## 4. Restricciones de Dominio e Invariantes Técnicos

1. **Estándar de Bits Braille Unicode (ISO/TR 11548-1 / Unicode `U+2800..U+28FF`):**
   Cada carácter Braille representa una celda de 2 columnas (`dx ∈ {0, 1}`) por 4 filas (`dy ∈ {0, 1, 2, 3}`). El punto de código se calcula como `Char(0x2800 + mask)`, donde la máscara de 8 bits sigue estrictamente:
   - `(dx = 0, dy = 0)` → Punto 1 → `0x01`
   - `(dx = 0, dy = 1)` → Punto 2 → `0x02`
   - `(dx = 0, dy = 2)` → Punto 3 → `0x04`
   - `(dx = 1, dy = 0)` → Punto 4 → `0x08`
   - `(dx = 1, dy = 1)` → Punto 5 → `0x10`
   - `(dx = 1, dy = 2)` → Punto 6 → `0x20`
   - `(dx = 0, dy = 3)` → Punto 7 → `0x40`
   - `(dx = 1, dy = 3)` → Punto 8 → `0x80`
2. **Paridad UTF-16 y Conteo de Caracteres:** Todos los caracteres emitidos por los modos monocromos (`U+0020..U+007E`, `U+2588..U+2593` y `U+2800..U+28FF`, además de `\n` `U+000A`) pertenecen al *Basic Multilingual Plane* (BMP), por lo que `1 carácter visual == 1 code point Unicode == 1 unidad UTF-16 (String.length)`. Los saltos de línea son siempre LF (`\n`, 1 carácter), nunca CRLF (`\r\n`).
3. **Fórmula de Luminancia Perceptual (`BT.709`):** Para un píxel sRGB compuesto sobre el fondo base:
   `Y = 0.2126f * R_norm + 0.7152f * G_norm + 0.0722f * B_norm`, acotado en `[0.0f, 1.0f]`.
4. **Sanitización de Envoltorio Markdown:** Ninguna rampa de caracteres podrá contener el carácter acento grave `` ` `` para impedir el cierre prematuro del bloque ```` ``` ```` en WhatsApp o Telegram.

---

## 5. Restricciones No Funcionales (`RNF`)

| ID | Categoría | Requisito Medible | Método de Verificación |
| :--- | :--- | :--- | :--- |
| **RNF-01** | **Latencia de Conversión** | El pipeline completo en memoria (`PixelGrid` → cadena formateada) **debe** completarse en **`< 150 ms`** (objetivo P95 `< 80 ms`) en un dispositivo Android de gama media para anchos IM (`12–60` columnas), sin bloquear el hilo principal (`Main`). | Test cronometrado en JVM y test instrumentado en dispositivo |
| **RNF-02** | **Presupuesto de Memoria (RAM)** | La decodificación de cualquier imagen —incluyendo fotografías de cámara de **48 MP–108 MP** o exportaciones pesadas— **debe** usar submuestreo previo (`inSampleSize` / `ImageDecoder.setTargetSize`) manteniendo el incremento máximo de heap **`< 20 MB`**. | Test con imagen sintética de alta resolución y medición de dimensiones decodificadas |
| **RNF-03** | **Privacidad y Cero Red** | La aplicación **no declarará `android.permission.INTERNET`** ni permisos de lectura de almacenamiento en `AndroidManifest.xml`, garantizando por sistema operativo que ninguna imagen abandona jamás el dispositivo durante su procesado. | Test de auditoría del `AndroidManifest.xml` |
| **RNF-04** | **Operación 100% Offline** | El 100% de las funciones de conversión, previsualización, copiado y lanzamiento de `ACTION_SEND` **deben** funcionar con el dispositivo en Modo Avión o conectado a Wi-Fi de avión restringido. | Prueba E2E en dispositivo con conectividad deshabilitada |
| **RNF-05** | **Accesibilidad (`a11y`)** | Todos los botones e iconos tendrán `contentDescription` descriptivos en español, tamaño táctil mínimo `48dp × 48dp`, y el contenedor de vista previa agrupará su semántica (`clearAndSetSemantics`) para anunciar un resumen legible en **TalkBack** en lugar de leer cientos de símbolos Braille o ASCII uno tras otro. | Compose Semantics UI Test |
| **RNF-06** | **Velocidad de Suite TDD** | Todos los tests unitarios de `domain/` **deben** ejecutarse en JVM pura en **`< 100 ms`** en conjunto. | Ejecución `./gradlew testDebugUnitTest` |
| **RNF-07** | **Respeto de Insets de Navegación (Edge-to-Edge Safe Area)** | La barra inferior de acciones (`Copiar` y `Enviar a...`) **debe** aplicar obligatoriamente `navigationBarsPadding()` para garantizar que la barra de navegación del sistema Android (tanto barra de 3 botones como barra gestual) jamás se superponga ni interfiera con los botones de acción interactivos, extendiendo a su vez el fondo del contenedor hasta el borde inferior de la pantalla. | Compose UI Test y verificación en hardware físico USB |

---

## 6. Casos Borde y Manejo de Errores (*Edge Cases*)

| ID | Caso Borde / Condición de Fallo | Comportamiento Esperado del Sistema | Capa Responsable |
| :--- | :--- | :--- | :--- |
| **CB-01** | **Transparencia Alfa (PNG / WebP con fondo transparente, ej. stickers o logos)** | Componer deterministamente el canal alfa sobre blanco (`#FFFFFF`) cuando `invert == false` y sobre negro (`#000000`) cuando `invert == true`, evitando que zonas transparentes (`RGBA 0,0,0,0`) se interpreten erróneamente siempre como negro sólido. | `domain/` |
| **CB-02** | **Archivo remoto en Google Drive (SAF) no cacheado localmente estando en el avión sin red a Drive** | `ContentResolver.openInputStream` lanza `IOException` / `FileNotFoundException`. `data/` captura la excepción y devuelve un `DecodeError.CloudFileOfflineUnavailable` tipado; `ui/` muestra un mensaje claro indicando que el archivo de la nube no está disponible sin conexión. | `data/` + `ui/` |
| **CB-03** | **Relaciones de aspecto extremas (panorámica `10000 × 200` o captura larga `1080 × 12000`)** | El remuestreador garantiza un mínimo de `1` columna y `1` fila (`coerceAtLeast(1)`) sin división por cero, y el ajustador de presupuesto reduce dimensiones de forma limpia para no superar `1.000` (Instagram) ni `4.096` (Telegram) caracteres. | `domain/` |
| **CB-04** | **Archivo corrupto, de 0 bytes o MIME engañoso (no es imagen válida)** | El decodificador detecta cabeceras inválidas (`outWidth <= 0` / `DecodeException`), no crashea y emite `DecodeError.InvalidOrCorruptedImage`. | `data/` |
| **CB-05** | **Eliminación de espacios finales (*trailing spaces*) por clientes IM** | En modos `ASCII_CLASSIC` y `UNICODE_BLOCKS` sobre WhatsApp/Telegram el bloque ```` ``` ```` preserva el espaciado; además, las líneas derechas vacías pueden recortarse sin afectar la alineación izquierda, y en `BRAILLE_UNICODE` `U+2800` tiene categoría Unicode `So` (*Symbol, Other*), por lo que ningún `trim()` estándar lo elimina. | `domain/` |
| **CB-06** | **Rotación de pantalla o cambio de tema del sistema Android durante la edición** | `ConverterViewModel` preserva el `PixelGrid` ya decodificado y todos los parámetros del usuario en su `StateFlow`, evitando re-decodificar la `Uri` o perder ajustes. | `ui/` |
| **CB-07** | **Compatibilidad de `SplashScreen` y `<monochrome>` en versiones Android anteriores (API 26–32)** | `androidx.core:core-splashscreen` retroporta fondo e icono central en API 26–30 mientras `windowSplashScreenBrandingImage` se aísla limpiamente en `values-v31/themes.xml` (API 31+), y la etiqueta `<monochrome>` en `mipmap-anydpi-v26` es ignorada de forma segura por el sistema en API < 33 sin errores de recursos ni crashes. | `res/` + `ui/` |

---

## 7. Matriz de Trazabilidad TDD

| Requisitos y Casos Borde | Capa | Nivel de Test | Suites Previstas |
| :--- | :---: | :---: | :--- |
| `CA-02.2`, `CA-02.3`, `CB-01`, `CB-03` | `domain` | Unit JVM (`Slice 1`) | `LuminanceExtractorTest`, `ToneAdjusterTest`, `AspectRatioResamplerTest` |
| `CA-02.1`, `CA-02.4`, `CB-05` | `domain` | Unit JVM (`Slice 2`) | `BrailleUnicodeRendererTest`, `AsciiRendererTest`, `DitheringEngineTest` |
| `CA-03.1`, `CA-03.2`, `CA-03.3` | `domain` | Unit JVM (`Slice 3`) | `ImPresetPolicyTest`, `CharacterBudgetEnforcerTest`, `ImMessageFormatterTest` |
| `CA-01.1..01.4`, `RNF-02`, `RNF-03`, `CB-02`, `CB-04` | `data` | Robolectric (`Slice 4`) | `AndroidImageSourceDecoderTest`, `ExifNormalizerTest`, `ManifestSecurityTest` |
| `CA-01.5`, `CA-02.5`, `CB-06` | `ui` | JVM + Turbine (`Slice 5`) | `ConverterViewModelTest` |
| `CA-04.1..04.3`, `RNF-05` | `ui` + `data` | Robolectric Compose UI (`Slice 6`) | `ConverterScreenTest`, `ImShareLauncherTest` |
| `RNF-01`, `RNF-02`, `RNF-04`, `CA-05.2` | `e2e` | Instrumentado USB/Wi-Fi (`Slice 7`) | `EndToEndConversionAndroidTest` (incluyendo chequeo E2E de *cold start*) |
| `CA-05.1`, `CA-05.2`, `CA-05.3`, `CB-07` | `res` + `ui` | Robolectric + Assets (`Slice 8`) | `BrandAssetsAndSplashTest` (`BrandAssetsAndSplashRobolectricTest` + chequeo de arranque) |

---

## 8. Decisiones de Diseño Validadas por el Usuario

> **Estado de Decisiones:** Confirmadas por el usuario (`2026-11-06`), adoptando la **Opción A** en `DD-01`..`DD-05`.

### DD-01: Integración de Cámara en el MVP (`TakePicture` del Sistema vs. `CameraX` Integrado)
- **Opción A (Recomendada para MVP):** Usar el contrato oficial `ActivityResultContracts.TakePicture()` junto con `FileProvider` en caché privada.
  - *Ventajas:* **No requiere declarar el permiso `android.permission.CAMERA`** en el manifiesto, aprovecha el procesado computacional (HDR/modo noche) de la cámara nativa del teléfono y mantiene el APK ligero y fiable.
- **Opción B:** Integrar un visor propio con **`CameraX`** dentro de la app (permitiendo incluso previsualizar el filtro ASCII/Braille en vivo antes de disparar).
  - *Coste:* Requiere solicitar permiso `CAMERA` en tiempo de ejecución y añade dependencias de ciclo de vida de cámara.
- **Estado:** `[x] Aprobada: Opción A`

### DD-02: Política al Superar el Límite de Caracteres en Telegram (`4.096`) o Instagram Direct (`1.000`)
- **Opción A (Recomendada para MVP):** **Auto-escalado preventivo a 1 único mensaje garantizado**. Si las dimensiones solicitadas superan `1.000` (Instagram) o `4.096` (Telegram) caracteres, el motor reduce automáticamente el número efectivo de columnas/filas hasta el máximo exacto que cabe íntegro en **un solo mensaje**, evitando que la imagen se parta en varias burbujas separadas por cabeceras o timestamps del chat.
- **Opción B:** Ofrecer ambos modos: *Ajustar a 1 mensaje* (por defecto) + un interruptor opcional *"Dividir en varios mensajes (Parte 1/N)"* con botones secuenciales de copia/envío para imágenes verticales largas en Telegram/Instagram.
- **Estado:** `[x] Aprobada: Opción A`

### DD-03: Modos de Arte en el MVP (Monocromo Puro vs. Mosaico de Emojis / ANSI)
- **Opción A (Recomendada para MVP):** **Solo modos monocromos de alta precisión** (`BRAILLE_UNICODE`, `ASCII_CLASSIC`, `UNICODE_BLOCKS`). Nota: WhatsApp, Telegram (en burbujas móviles normales) e Instagram Direct no interpretan códigos de color ANSI (`\u001B[31m`), y los emojis de cuadros de colores (`🟥🟦🟩`) ocupan pares sustitutos UTF-16 (2 chars por píxel, agotando Instagram DM en apenas ~20×20 píxeles).
- **Opción B:** Añadir un cuarto modo opcional **Mosaico Emoji de Color (`🟥🟧🟨🟩🟦🟪🟫⬛⬜`)** pensado para iconos pequeños o banderas en WhatsApp/Telegram.
- **Estado:** `[x] Aprobada: Opción A`

### DD-04: Resolución de Variantes Oficiales de Apps de Mensajería (ej. WhatsApp Business)
- **Opción A (Recomendada):** Cadena de resolución inteligente: probar primero el paquete principal (`com.whatsapp`, `org.telegram.messenger`, `com.instagram.android`); si no está instalado, comprobar variantes oficiales conocidas (`com.whatsapp.w4b` para WhatsApp Business; `org.thunderdog.challegram` / `org.telegram.messenger.web` para Telegram); y si ninguna está instalada, abrir el Share Sheet estándar (`Intent.createChooser`).
- **Opción B:** Comprobar únicamente los tres nombres de paquete canónicos y pasar directamente al Share Sheet si no existen.
- **Estado:** `[x] Aprobada: Opción A`

### DD-05: Estilo Visual del Icono y Animación de Entrada en Splash Screen
- **Opción A (Recomendada):** Icono vectorial minimalista oscuro (`#0D1117`, coherente con `docs/assets/banner.png`) con glifo luminoso esmeralda/cian combinando avión/imagen transformándose en matriz de puntos Braille/ASCII dentro de una burbuja IM, acompañado en la Splash Screen por una **animación vectorial sutil de aparición (`AnimatedVectorDrawable` `≤ 300 ms`)** que **jamás bloquea ni retrasa** la carga del contenido + `windowSplashScreenBrandingImage` monoespaciada en Android 12+.
- **Opción B:** Mismo diseño de icono adaptativo/monocromo e imágenes de launch, pero con **Splash Screen 100% estática** (`VectorDrawable` fijo sin animación de entrada ni imagen inferior de *branding*).
- **Estado:** `[x] Aprobada: Opción A`

---

## 9. Registro de Aprobación (*Sign-off*)

- [x] Todos los requisitos (`RF-01`..`RF-05`, `RNF-01`..`RNF-06`) cuentan con criterios **Given / When / Then** deterministas y trazables.
- [x] El plan arquitectónico ([`plan.md`](plan.md)) y la hoja de ruta TDD ([`tasks.md`](tasks.md)) están alineados con esta especificación.
- [x] **Decisiones Abiertas (`DD-01`..`DD-05`) confirmadas por el usuario (`Opción A` en todas).**
- [x] **Estado cambiado a `APROBADO` para habilitar el Ciclo TDD 1 (`2026-11-06`).**
