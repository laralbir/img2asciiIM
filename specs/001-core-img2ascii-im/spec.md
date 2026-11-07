# Especificación `001`: Motor e Interfaz Base de `img2asciiIM`

| Campo | Valor |
| :--- | :--- |
| **ID de Especificación** | `001-core-img2ascii-im` |
| **Estado** | `IMPLEMENTADO — Actualizado con Visualización de Foto Real (HD), Guardado en Galería (MediaStore) y Cota Segura 3.800 Chars (2026-11-07)` |
| **Autor(es)** | `sdd-architect` |
| **Fecha de Creación** | `2026-11-06` |
| **Última Actualización** | `2026-11-07` |
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

### 1.3 El Desafío del Destinatario sin Conexión y los Enlaces Web en Android
En los escenarios de vuelo comercial, cruceros o Modo Avión, cuando el receptor recibe un mensaje con un enlace autónomo HD (`#d=...`), surgen dos dificultades técnicas habituales:
1. **Delegación no deseada al navegador web:** Por defecto, muchas versiones y capas de personalización de Android abren enlaces HTTPS estándar (`https://laralbir.github.io/img2asciiIM/#d=...`) en Google Chrome u otros navegadores web en lugar de transferirlos directamente a la aplicación nativa instalada, si los App Links no han sido explícitamente activados por el usuario en los ajustes de «Abrir de forma predeterminada».
2. **Fallo de navegación en redes restringidas:** Al abrir Chrome en el Wi-Fi gratuito de mensajería del avión (que filtra y bloquea el tráfico web general), el navegador se detiene inmediatamente mostrando el error *«Sin conexión a internet»*, dejando al receptor sin poder visualizar la fotografía.

Para ofrecer una experiencia 100 % fiable en cualquier circunstancia, `img2asciiIM` incorpora una solución de doble vía:
- **Importación Inteligente desde Portapapeles (100 % Offline):** El receptor simplemente copia el mensaje o el enlace recibido en WhatsApp o Telegram y abre `img2asciiIM`. La aplicación detecta automáticamente la presencia de la imagen en el portapapeles al iniciar o reanudar la app (o el usuario pulsa el chip interactivo **«Portapapeles»**), extrayendo y decodificando el mapa de bits WebP en memoria **100 % sin internet**, sin tocar el navegador y con cero consumo de datos.
- **Configuración Asistida de Enlaces Nativos de Android:** La aplicación incorpora una acción directa que abre la sección de ajustes del sistema Android («Abrir de forma predeterminada» / *Open by default*), permitiendo al usuario autorizar que el dominio `laralbir.github.io` se abra siempre de forma nativa e instantánea en `img2asciiIM` sin pasar por Chrome.

### 1.4 Fuera de Alcance (*Non-Goals* del MVP `001`)
- Conversión de vídeo o GIFs animados a secuencias de mensajes de texto.
- Servidores backend propios, cuentas de usuario, analítica o telemetría en la nube.
- Envío automatizado en segundo plano mediante APIs privadas/bots no oficiales de WhatsApp, Telegram o Instagram (toda entrega usa los mecanismos estándar de Android `ACTION_SEND` y `ClipboardManager` iniciados por el usuario).
- Historial persistente en base de datos local de conversiones pasadas (previsto para una futura especificación `002` si el usuario lo solicita).

---

## 2. Historias de Usuario

| ID | Prioridad | Historia de Usuario | Valor Entregado |
| :--- | :---: | :--- | :--- |
| **HU-01** | `P0` | **Como** viajero con permisos de privacidad estrictos, **quiero** iniciar la aplicación con un lienzo limpio (*Empty State*) e importar imágenes desde mi galería, archivos locales, tarjeta SD o mi unidad de **Google Drive** sin otorgar permisos globales de lectura ni depender de imágenes demo no deseadas, **para** elegir cualquier imagen con seguridad y control total. | Ingesta universal cero-permisos y arranque limpio |
| **HU-02** | `P0` | **Como** usuario en pleno vuelo o en movimiento, **quiero** tomar una foto directa con la cámara o pulsar *Compartir → img2asciiIM* desde Google Drive o la Galería, **para** convertir una imagen al vuelo en menos de 3 toques. | Flujo inmediato desde cámara y Share Sheet |
| **HU-03** | `P0` | **Como** emisor del mensaje, **quiero** alternar entre Braille Unicode, ASCII clásico y Bloques, ajustando ancho, brillo, contraste, inversión (tema claro/oscuro) y *dithering* sobre una vista previa realista de burbuja, **para** garantizar que la imagen sea reconocible en el móvil del destinatario. | Legibilidad máxima en pantallas verticales |
| **HU-04** | `P0` | **Como** usuario de WhatsApp, Telegram o Instagram Direct, **quiero** elegir el perfil de mi app de destino, **para** que el formato tipográfico y la longitud del mensaje respeten automáticamente sus reglas (`65.536`, `4.096` o `1.000` caracteres, con o sin ```` ``` ````). | Cero mensajes truncados o deformados |
| **HU-05** | `P0` | **Como** pasajero conectado solo al plan de mensajería del avión, **quiero** enviar el texto directamente a la app elegida o copiarlo en 1 toque sin requerir internet para la conversión, **para** transmitir información visual cuando el envío de fotos está bloqueado. | Comunicación visual sobre canal de solo texto |
| **HU-06** | `P0` | **Como** usuario de Android moderno (incluyendo Pixel Launcher con iconos temáticos y tema claro/oscuro), **quiero** que `img2asciiIM` cuente con un icono adaptativo y monocromo propio, imágenes de lanzamiento (*launch/branding*) y una *Splash Screen* nativa instantánea sin pantallas en blanco ni demoras artificiales, **para** percibir una experiencia pulida desde el primer fotograma. | Identidad visual nativa y arranque instantáneo |
| **HU-07** | `P0` | **Como** usuario que desea compartir fotografías de alta nitidez sobre canales de mensajería, **quiero** conmutar modularmente entre arte de texto y un enlace autónomo HD con códec WebP Lossy (`#d=...`), con redimensionamiento adaptativo estricto bajo la cota universal segura `MAX_SAFE_URL_LENGTH = 3.800` caracteres (evitando el truncamiento destructivo a 4.096 caracteres de WhatsApp y la división de burbujas en Telegram, y en modo híbrido reservando `3.800 - art.length - 1` caracteres) y apertura directa nativa (`img2asciiim://view?d=...`), **para** maximizar la resolución y asegurar que el mensaje nunca se divida, se recorte ni muestre una imagen en negro en el chat. | Envío híbrido WebP, cota segura 3.800 y App Links |
| **HU-08** | `P0` | **Como** usuario fuera de tiendas comerciales, **quiero** comprobar in-app la disponibilidad de nuevas versiones oficiales desde GitHub Releases y actualizar mi APK firmado de forma asistida y segura, **para** mantenerme actualizado con total resiliencia sin fallos en vuelo. | Actualizaciones in-app y CI/CD firmado |
| **HU-09** | `P0` | **Como** receptor de un mensaje sin conexión a internet (en pleno vuelo o en Modo Avión), **quiero** copiar el mensaje o enlace recibido en WhatsApp o Telegram y abrir `img2asciiIM` para que detecte automáticamente la imagen desde el portapapeles (o pulsar el chip «Portapapeles»), **para** ver la fotografía de forma inmediata y 100 % offline sin necesidad de navegador web. | Importación instantánea offline desde portapapeles |
| **HU-10** | `P1` | **Como** usuario de Android, **quiero** acceder directamente desde la aplicación a los Ajustes de «Abrir de forma predeterminada» de Android, **para** configurar que los enlaces compatibles se abran siempre en `img2asciiIM` de manera nativa sin pasar por Google Chrome. | Enlace directo a configuración de App Links nativos |
| **HU-11** | `P0` | **Como** destinatario de un enlace compartido por WhatsApp/Telegram, **quiero** ver la fotografía original en alta definición y a todo color dentro de la app, y poder guardarla en mi galería, además de ver el arte de texto si lo deseo. | Visualización de fotografía real HD y persistencia en galería |

---

## 3. Requisitos Funcionales y Criterios de Aceptación (`Given / When / Then`)

### RF-01: Ingesta Universal de Imágenes (Sin Permisos Invasivos)

La aplicación **debe** ofrecer cinco vías nativas de entrada de imágenes sin declarar `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` ni `MANAGE_EXTERNAL_STORAGE`:

1. **Selector Moderno de Fotos (`PickVisualMedia`):** Integración con Android Photo Picker (`PickVisualMediaRequest(ImageOnly)`) para seleccionar fotos de la galería local y proveedores de fotos en la nube integrados en el sistema.
2. **Selector Universal de Documentos SAF (`OpenDocument` con filtro `image/*`):** Acceso unificado mediante el Storage Access Framework a almacenamiento interno, carpeta `Descargas`, tarjetas SD, unidades USB OTG y proveedores `DocumentsProvider` en la nube instalados en el dispositivo (**Google Drive**, Microsoft OneDrive, Dropbox, Nextcloud).
3. **Captura Directa desde Cámara:** Disparo fotográfico a través de contrato seguro con `FileProvider` sobre directorio privado de caché `cacheDir/camera_captures/` (o visor `CameraX` según **DD-01**).
4. **Recepción Externa vía Share Sheet (`Intent.ACTION_SEND` con MIME `image/*`):** Registro de `<intent-filter>` en `MainActivity` para recibir `Intent.EXTRA_STREAM` cuando el usuario comparte una imagen desde Google Drive, Galería, gestor de archivos o navegador hacia `img2asciiIM` (soportando tanto arranque en frío como `onNewIntent` con actividad ya abierta).
5. **Importación desde Portapapeles (`ClipboardManager`):** Ingesta asistida de mensajes o enlaces con payload fotográfico WebP recibidos en chats (WhatsApp, Telegram, etc.), disponible tanto por detección reactiva al reanudar la app como mediante el chip interactivo **«Portapapeles»** en la barra de origen de imágenes y en el Empty State.

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

- **CA-01.6 — Estado Inicial Limpio al Iniciar (`Empty State`) sin Imagen Demo:**
  - **Given:** La aplicación se inicia por primera vez o en cualquier arranque en frío/caliente desde el lanzador del sistema sin un `Intent.ACTION_SEND` ni Deep Link entrante.
  - **When:** Se compone la pantalla principal de la aplicación.
  - **Then:** La aplicación se presenta en un estado limpio (*Empty State*) interactivo con ilustración técnica y tarjetas/botones directos invitando a seleccionar foto desde **Galería** (`PickVisualMedia`), **Archivos / Drive** (SAF `OpenDocument`), **Cámara** (`TakePicture`) o importar desde **Portapapeles**, sin precargar ninguna imagen de muestra/demo artificial. El área de vista previa no muestra conversiones previas hasta que el usuario elija activamente una fotografía, optimizando el arranque y garantizando una experiencia sin elementos intrusivos.

- **CA-01.7 — Selección e Importación desde Portapapeles (Chip Manual):**
  - **Given:** El portapapeles del sistema contiene un mensaje o enlace con un payload válido de `img2asciiIM` (`#d=...`, `#data=...`, etc.).
  - **When:** El usuario pulsa el chip interactivo **«Portapapeles»** en la barra de origen o en el Empty State.
  - **Then:** La aplicación extrae el payload fotográfico, decodifica el `PixelGrid` 100 % offline en memoria, establece `ImageSourceOrigin.CLIPBOARD` y renderiza inmediatamente la vista previa sin emitir tráfico de red. Si el portapapeles no contiene un payload válido, muestra un aviso claro y no altera el estado.

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
| **WhatsApp** | `com.whatsapp` | Sí: ```` ```<arte>``` ```` *(sin `U+0020` inicial)* | `ASCII_CLASSIC` *(admite todos)* | `32` cols (`28–36` cols) | **`65.536`** chars multilínea *(URLs continuas `≤ 3.800` por corte a 4.096 chars)* |
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

- **CA-04.4 — Entrega Clipboard-First en WhatsApp para Cargas Extensas (hasta 65.536 chars de texto):**
  - **Given:** Un mensaje generado bajo el perfil WhatsApp con arte de texto extenso (hasta `65.536` caracteres) y/o enlace HD activo (`attachDeepLink == true`, donde la URL generada nunca excede `MAX_SAFE_URL_LENGTH = 3.800` caracteres).
  - **When:** El usuario pulsa **"Enviar a WhatsApp"**.
  - **Then:** El sistema aplica el patrón *Clipboard-First*: (1) copia preventivamente la totalidad del mensaje formateado en `ClipboardManager` mediante `ClipData.newPlainText`; (2) lanza el `Intent.ACTION_SEND` hacia `com.whatsapp`; (3) muestra un aviso en pantalla (Snackbar persistente o Toast amigable) informando: *"Mensaje copiado al portapapeles. Si WhatsApp recorta el contenido, mantén presionado el campo de chat y selecciona Pegar"*, salvaguardando la integridad del mensaje ante las cotas de IPC/Binder de Android o posibles recortes en `EXTRA_TEXT` del cliente destino.

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

### RF-07: Composición de Envío Híbrido, Enlaces Autónomos WebP de Alta Definición (`#d=...`) y Visor Web / App Links

La aplicación **debe** permitir elegir de forma modular los componentes del mensaje a enviar mediante dos conmutadores independientes, adjuntando opcionalmente un enlace web estándar (`https://laralbir.github.io/img2asciiIM/#d=...`) o invocable mediante el esquema nativo de la app (`img2asciiim://view?d=...`) que contiene la imagen fotográfica comprimida con el códec **WebP Lossy** y codificada en Base64 URL-safe dentro del hash fragment de la URL, funcionando como puente híbrido entre el arte de texto instantáneo y la fotografía original:

- **Criterio 07.1 — Conmutadores Modulares y Supresión de Texto Descriptivo Superfluo:**
  - **Given:** Una imagen cargada en la aplicación y el preset WhatsApp o Telegram activo.
  - **When:** El usuario interactúa con los controles de composición de mensaje:
    - `[x] Incluir arte de texto (ASCII / Braille)` (`includeTextArt`, por defecto `true`).
    - `[x] Adjuntar enlace a foto HD (#d=...)` (`attachDeepLink`, por defecto `false`).
  - **Then:** El sistema actualiza el estado de la UI y la vista previa del mensaje en tiempo real:
    1. Si `includeTextArt == true` y `attachDeepLink == true`: El mensaje generado se compone exclusivamente del bloque de arte de texto seguido de un salto de línea y la URL:
       ```text
       <arte_ascii_o_braille>
       https://laralbir.github.io/img2asciiIM/#d=<payload>
       ```
    2. Si `includeTextArt == false` y `attachDeepLink == true`: El mensaje generado contiene **única y exclusivamente la URL**:
       ```text
       https://laralbir.github.io/img2asciiIM/#d=<payload>
       ```
    3. **Eliminación Total de Texto Superfluo:** Quedan completamente suprimidas cadenas decorativas como `"📸 Ver foto en alta definición:"` o `"🔗 Ver foto..."`, maximizando la resolución y los bytes disponibles del payload fotográfico al no desperdiciar decenas de caracteres en cadenas redundantes.
    4. **Invariante de Mensaje No Vacío (`CB-08`):** Si el usuario intenta desmarcar ambas casillas simultáneamente, el sistema bloquea la desactivación de la última opción activa (o reactiva automáticamente la otra), garantizando la invariante de que el mensaje final nunca esté vacío ni carezca de contenido.
    5. **Inhabilitación en Instagram Direct:** En el perfil Instagram Direct, la opción de enlace HD se deshabilita automáticamente con indicación visual de no disponibilidad debido al límite estricto de 1.000 caracteres de dicha plataforma.

- **Criterio 07.2 — Migración a Códec WebP Lossy y Prefijo Corto Canónico `#d=`:**
  - **Given:** Una imagen fotográfica a codificar para el enlace autónomo en cualquier perfil IM soportado.
  - **When:** `DeepLinkImageCodec` genera el payload de la imagen.
  - **Then:**
    1. El codificador comprime el mapa de píxeles utilizando `Bitmap.CompressFormat.WEBP_LOSSY` en API 30+ (o `Bitmap.CompressFormat.WEBP` en API 26–29), logrando una reducción comprobada de entre el **35 % y el 50 % en el tamaño en bytes** respecto a la compresión JPEG clásica para una fidelidad perceptual equivalente (PSNR / SSIM).
    2. La cadena Base64 resultante se codifica en formato URL-safe sin saltos de línea ni relleno de signo igual (`Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING`).
    3. La URL canónica utiliza el prefijo corto de hash **`#d=<base64>`**, ahorrando 3 caracteres vitales respecto al anterior `#data=`.
    4. **Compatibilidad y Tolerancia de Entrada hacia Atrás:** El decodificador nativo (`DeepLinkImageCodec.decodeFromDeepLink`) y el visor web soportan de forma tolerante y priorizada:
       - `#d=<payload>` (canónico nuevo)
       - `#data=<payload>` (retrocompatible previo)
       - `?d=<payload>` (parámetro query alternativo)
       - `?data=<payload>` (parámetro query previo)

- **Criterio 07.3 — Redimensionamiento Adaptativo Estricto, Cota Universal Segura (`MAX_SAFE_URL_LENGTH = 3.800`) y Límite Duro de WhatsApp / Telegram:**
  - **Given:** Una imagen fotográfica cargada y la generación de mensaje en curso para cualquier preset soportado (WhatsApp o Telegram).
  - **When:** Se calcula el enlace HD según la plataforma y la combinación de conmutadores activa.
  - **Then:**
    1. **Límite Duro de WhatsApp en Cadenas Continuas (`4.096` caracteres):**
       - Aunque WhatsApp admite hasta `65.536` caracteres en mensajes con texto multilínea ordinario, la aplicación WhatsApp y la pila IPC/componentes de renderizado de texto de Android aplican un truncamiento destructivo forzado en tokens y URLs continuas (cadenas sin espacios en blanco) a los `4.096` caracteres exactos. Cualquier enlace continuo que sobrepase los 4.096 caracteres es cortado de manera irreversible al enviarse o pegarse en el chat, destruyendo el final del payload Base64 y provocando fallos de decodificación o imágenes en negro en los receptores.
    2. **Restricción Universal de Enlace Seguro (`MAX_SAFE_URL_LENGTH = 3.800` caracteres):**
       - Ningún enlace Deep Link generado por `DeepLinkImageCodec` podrá superar bajo ninguna circunstancia los **`3.800` caracteres** de longitud total (`url.length <= MAX_SAFE_URL_LENGTH`), con independencia de la plataforma seleccionada (WhatsApp, Telegram) o del modo activo (Estándar o Expandido HD).
       - Este margen de seguridad de $4.096 - 3.800 = 296$ caracteres salvaguarda contra truncados destructivos por WhatsApp, partición en múltiples burbujas en Telegram (cuyo límite de mensaje es `4.096`) y variaciones de serialización o encoding en capas OEM de Android.
    3. **Reglas de Presupuesto Adaptativo por Plataforma y Modo:**
       - **Telegram:**
         - **Modo Solo Enlace HD (`includeTextArt == false`):** Se aplica un presupuesto máximo de **`3.800` caracteres** (`MAX_SAFE_URL_LENGTH`), garantizando que la URL completa no supere dicha cota.
         - **Modo Híbrido Dual (`includeTextArt == true`):** El presupuesto del enlace se calcula dinámicamente como:
           $$\text{budget}_{\text{deeplink}} = \min(3.800, 3.900 - \text{art.length} - 1)$$
           garantizando simultáneamente que el mensaje total no supere `3.900` caracteres (inmunidad anti-división en Telegram) y que la URL individual no supere `3.800` caracteres.
       - **WhatsApp:**
         - **Modo Solo Enlace HD (`includeTextArt == false`):** A pesar de que los mensajes multilínea de WhatsApp toleran hasta `65.536` caracteres, el enlace continuo está sujeto al límite duro de `4.096` caracteres. Por ello, el generador establece un presupuesto estricto de **`3.800` caracteres** (`MAX_SAFE_URL_LENGTH`), escalando la imagen en WebP Lossy para maximizar la nitidez sin riesgo de truncamiento.
         - **Modo Híbrido Dual (`includeTextArt == true`):** El enlace HD se mantiene acotado estrictamente a $\le 3.800$ caracteres (`MAX_SAFE_URL_LENGTH`), permitiendo que el arte de texto acompañe al enlace de forma armónica.
       - En envíos extensos de texto en WhatsApp que puedan verse afectados por limitaciones de IPC/Binder de Android en `EXTRA_TEXT`, se preserva complementariamente la entrega Clipboard-First (`CA-04.4`).
    4. **Algoritmo de Reducción Adaptativa Estricto (Convergencia Determinista):**
       Si la URL inicial excede el presupuesto asignado ($\text{url.length} > \text{budget}$), `DeepLinkImageCodec` ejecuta un algoritmo adaptativo de 4 fases que converge de manera determinista:
       - **Fase 1 (Reducción de Calidad WebP):** Decrece progresivamente la calidad WebP en pasos de 5 unidades ($Q_{n+1} = Q_n - 5$) mientras $\text{url.length} > \text{budget}$ y $Q > 20$.
       - **Fase 2 (Reducción Dimensional Progresiva):** Si aún sobrepasa el presupuesto, reduce la dimensión mayor un 15% en cada iteración ($D_{n+1} = \lfloor 0.85 \times D_n \rfloor$, forzando decremento de al menos 1 píxel si el redondeo estanca) mientras $\text{url.length} > \text{budget}$ y $D > 48\text{ px}$.
       - **Fase 3 (Afinado de Calidad Mínima):** Si persiste el exceso, reduce la calidad en decrementos unitarios ($Q_{n+1} = Q_n - 1$) mientras $\text{url.length} > \text{budget}$ y $Q > 15$.
       - **Fase 4 (Garantía Estricta Residual):** Si excepcionalmente se requiere mayor reducción (ej. presupuestos híbridos muy estrechos), reduce dimensiones al 80% ($D_{n+1} = \lfloor 0.80 \times D_n \rfloor$) hasta 16 px, calidad hasta 5 ($Q_{n+1} = Q_n - 2$), y dimensión píxel a píxel hasta 1 px si fuera necesario, garantizando matemáticamente $\text{url.length} \le \text{budget}$.

- **Criterio 07.4 — Esquema de App Nativa (`img2asciiim://view?d=...`), Ingesta y Guardia de Integridad RIFF WebP:**
  - **Given:** Un enlace en formato universal `https://laralbir.github.io/img2asciiIM/#d=...` o mediante el esquema personalizado `img2asciiim://view?d=...`.
  - **When:** El usuario pulsa el enlace en cualquier aplicación externa o navegador web en Android, o la app lo recibe vía Intent o Clipboard.
  - **Then:**
    1. `AndroidManifest.xml` resuelve el intent `VIEW` hacia `MainActivity` a través de los dos filtros declarados:
       - Filtro App Links HTTPS con `android:autoVerify="true"` para `host="laralbir.github.io"` y `pathPrefix="/img2asciiIM"`.
       - Filtro de esquema personalizado con `android:scheme="img2asciiim"`, `android:host="view"`.
    2. `MainActivity.kt` intercepta `Intent.ACTION_VIEW`, extrae el payload Base64 sin importar si proviene de `#d=`, `#data=`, `?d=` o `?data=`, y delega en `DeepLinkImageCodec.decodeFromDeepLink()`.
    3. **Guardia de Integridad de Cabecera RIFF WebP en Recepción (`CB-15`, `CB-17`):**
       Antes de delegar en `BitmapFactory.decodeByteArray`, el decodificador inspecciona los bytes mágicos iniciales del flujo decodificado:
       - Si los primeros 4 bytes corresponden a `"RIFF"` (`0x52, 0x49, 0x46, 0x46`) y los bytes 8..11 a `"WEBP"` (`0x57, 0x45, 0x42, 0x50`), extrae el tamaño esperado del payload declarado en la cabecera RIFF (bytes 4..7 en formato little-endian de 32 bits, sumando 8 bytes de cabecera: $\text{expectedSize} = \text{readLittleEndian32}(\text{bytes}[4..7]) + 8$).
       - Comprueba la condición de integridad: $\text{bytes.size} \ge \text{expectedSize}$.
       - Si $\text{bytes.size} < \text{expectedSize}$, determina de forma inmediata que el payload ha sido truncado (por ejemplo, cortado a 4.096 caracteres por WhatsApp o por un copiado parcial del mensaje) y devuelve deterministamente `DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)`.
       - Esta comprobación preventiva impide que Android o la biblioteca de decodificación intenten procesar un flujo incompleto, evitando que se muestre una imagen en negro o deformada en pantalla y permitiendo presentar un mensaje de error claro al usuario (`CB-17`).
    4. El decodificador reconstruye el `PixelGrid` original a color en memoria **100 % offline (cero peticiones de red)** y actualiza la vista con `ImageSourceOrigin.DEEP_LINK`. Si el payload no es una imagen válida o está dañado, emite `ImageDecodeError.InvalidOrCorruptedImage` sin provocar excepciones no controladas.

- **Criterio 07.5 — Visor Web Estático de Respaldo (`docs/index.html`) con Banner App Directo:**
  - **Given:** Un destinatario que abre el enlace en un navegador web (Chrome, Safari, Firefox, Edge en Android, iOS o escritorio).
  - **When:** El navegador procesa `https://laralbir.github.io/img2asciiIM/#d=...` (o `#data=...`, `?d=...`, `?data=...`).
  - **Then:**
    1. El visor web estático sin servidor alojado en GitHub Pages lee el fragmento de URL con JavaScript en el cliente, normaliza el Base64 URL-safe, crea un objeto `Blob` con MIME `image/webp` (o `image/jpeg` por inspección de cabecera de bytes) y proyecta la imagen en un elemento `<img>` / `<canvas>`.
    2. Si el navegador se ejecuta en un dispositivo Android, la interfaz muestra un banner interactivo prominente con el botón **«⚡ Abrir en App img2asciiIM»** apuntando al esquema nativo `img2asciiim://view?d=<base64>`, permitiendo conmutar a la aplicación instalada con un solo toque y sin intermediarios.
    3. Si la app no está instalada, ofrece un botón secundario para descargar el APK firmado desde GitHub Releases.

### RF-08: Publicación de Releases Firmadas en GitHub y Actualizaciones Automáticas In-App

La aplicación **debe** contar con un sistema automatizado de integración y entrega continua (CI/CD) para compilar y publicar APKs firmadas con control de versiones semántico (`vX.Y.Z`) en GitHub Releases, así como un mecanismo nativo in-app para comprobar la existencia de nuevas versiones y guiar al usuario en su actualización sin depender de tiendas comerciales privativas:

- **Criterio 08.1 — Configuración Segura de Firma de Release en Gradle (`app/build.gradle.kts`):**
  - **Given:** Un entorno de compilación de release (local o runner de GitHub Actions).
  - **When:** Se ejecuta `./gradlew assembleRelease`.
  - **Then:** Gradle lee las credenciales de firma desde variables de entorno seguras (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `KEYSTORE_PASSWORD`) o `keystore.properties` si existen. En ausencia de credenciales de producción (ej. entorno de desarrollo local o bifurcaciones comunitarias), el script recurre de forma transparente a la clave de depuración (`debug.keystore`), permitiendo generar APKs instalables de release sin romper la compilación ni exigir secretos obligatorios.

- **Criterio 08.2 — Pipeline CI/CD Automatizado en GitHub Actions (`.github/workflows/release.yml`):**
  - **Given:** Un push de etiqueta Git con formato de versión semántica `v*.*.*` (ej. `v1.0.0`).
  - **When:** El flujo de trabajo de GitHub Actions se ejecuta en `ubuntu-latest` con JDK 21.
  - **Then:** Se ejecutan de manera obligatoria todos los tests unitarios y de integración JVM (`./gradlew testDebugUnitTest`), se compila el APK firmado `app-release.apk`, se calcula su suma criptográfica SHA-256 (`sha256sum img2asciiIM-vX.Y.Z.apk > img2asciiIM-vX.Y.Z.apk.sha256`) y se publica automáticamente la Release en GitHub con el archivo APK, el archivo de checksum y las notas de cambios generadas a partir de Conventional Commits.

- **Criterio 08.3 — Comprobador In-App de Nuevas Versiones (`GitHubUpdateChecker`):**
  - **Given:** La app en ejecución y conectividad de red disponible.
  - **When:** El usuario pulsa la opción «Buscar actualizaciones» en la barra superior o en el menú de información (o la app realiza una comprobación opcional).
  - **Then:** `GitHubUpdateChecker` consulta la API REST de GitHub Releases (`https://api.github.com/repos/laralbir/img2asciiIM/releases/latest`), parsea el JSON recibido extrayendo el tag (`tag_name`), las notas de versión (`body`), la URL HTML (`html_url`) y la URL directa de descarga del activo `.apk` (`browser_download_url`). Compara la versión remota contra `BuildConfig.VERSION_NAME` usando comparación semántica semver determinista (`isNewer(remote, local)`). Si la versión remota es mayor, emite `UpdateCheckResult.UpdateAvailable`; si es igual o menor, emite `UpdateCheckResult.UpToDate`.

- **Criterio 08.4 — Descarga e Instalación Asistida del APK:**
  - **Given:** Una nueva versión detectada por el comprobador (`UpdateCheckResult.UpdateAvailable`).
  - **When:** El usuario observa el diálogo informativo de nueva versión y pulsa «Descargar APK» o «Ver en GitHub».
  - **Then:** La app abre el enlace directo de descarga del APK (`apkDownloadUrl`) o la página oficial de la release en el navegador mediante un `Intent(Intent.ACTION_VIEW)`, permitiendo al usuario descargar e instalar el paquete oficial verificado.

- **Criterio 08.5 — Resiliencia Total Offline y Modo Avión (Cero Fallos en Vuelo):**
  - **Given:** El usuario con el móvil en Modo Avión o conectado a una red sin salida a internet (Wi-Fi satelital de avión o portal cautivo bloqueado).
  - **When:** Se ejecuta la comprobación de actualizaciones.
  - **Then:** Las conexiones HTTP aplican un tiempo límite estricto de conexión y lectura (`connectTimeoutMs = 4000`, `readTimeoutMs = 4000`). Ante cualquier `IOException`, error de resolución DNS, código HTTP de error o timeout, el comprobador captura la situación limpiamente y devuelve `UpdateCheckResult.OfflineOrError("Sin conexión a internet o servicio no disponible")`, sin lanzar excepciones no controladas ni afectar al funcionamiento del conversor fotográfico.

### RF-09: Ingesta Offline desde Portapapeles y Configuración de Enlaces Nativos (App Links)

La aplicación **debe** ofrecer una solución completa y de máxima resiliencia cuando el receptor se encuentra sin conexión web general (Modo Avión o Wi-Fi satelital restringido a mensajería) o cuando el sistema Android redirige por defecto los enlaces web hacia Google Chrome:

- **Criterio 09.1 — Detección Reactiva Automática al Iniciar o Reanudar (`Lifecycle.Event.ON_RESUME`):**
  - **Given:** El usuario copia en WhatsApp, Telegram o cualquier aplicación un mensaje o enlace que contiene un payload fotográfico (`#d=`, `#data=`, `?d=`, `?data=` o `img2asciiim://view?d=`) y abre o reanuda `img2asciiIM`.
  - **When:** La actividad pasa al estado de ciclo de vida `RESUMED` (`onResume`).
  - **Then:** La aplicación inspecciona de manera asíncrona y no bloqueante el contenido de texto en `ClipboardManager`. Si detecta un payload válido de `img2asciiIM` y la aplicación se encuentra en el estado inicial (*Empty State*), importa y decodifica la fotografía automáticamente estableciendo `ImageSourceOrigin.CLIPBOARD`; si ya existe una fotografía activa modificada por el usuario, muestra un banner o notificación interactiva en pantalla (*«Foto detectada en el portapapeles. ¿Deseas abrirla?»* con acción directa «Abrir»), garantizando que el usuario no pierda su trabajo previo.

- **Criterio 09.2 — Botón y Chip Manual «Portapapeles» en la Barra de Origen y Empty State:**
  - **Given:** La pantalla principal de `img2asciiIM` en cualquier estado (Empty State o workspace activo).
  - **When:** El usuario pulsa el chip interactivo **«Portapapeles»** ubicado en la fila superior de fuentes de imagen (junto a «Galería», «Archivos / Drive» y «Cámara») o en la tarjeta correspondiente del Empty State.
  - **Then:**
    1. Si el portapapeles contiene un mensaje o enlace con payload fotográfico compatible, la app extrae el Base64, decodifica el `PixelGrid`, asigna `ImageSourceOrigin.CLIPBOARD` y renderiza el resultado fotográfico al instante.
    2. Si el portapapeles está vacío o no contiene ningún enlace ni payload de `img2asciiIM`, la interfaz muestra un aviso claro y amigable (*«El portapapeles no contiene un enlace ni una foto de img2asciiIM»*), sin alterar la imagen o el estado actual ni provocar excepciones.

- **Criterio 09.3 — Extracción Tolerante y Sanitización en Mensajes Mixtos o Híbridos:**
  - **Given:** Un texto copiado en el portapapeles que corresponde a un mensaje híbrido completo (bloque de arte ASCII/Braille, texto conversacional del remitente y la URL `#d=...`).
  - **When:** El extractor procesa la cadena copiada.
  - **Then:** El analizador determinista aísla y extrae con precisión el payload Base64 ignorando todos los caracteres de dibujo, saltos de línea y emojis adyacentes, procediendo a su decodificación sin requerir que el usuario aísle manualmente la URL en el chat.

- **Criterio 09.4 — Operación 100% Offline Garantizada (Cero Dependencia de Red y Cero Navegador):**
  - **Given:** El dispositivo receptor en Modo Avión estricto o conectado a una red Wi-Fi de avión que filtra y bloquea el tráfico web ordinario.
  - **When:** Se ejecuta la importación desde el portapapeles (tanto por detección automática como por pulsación del chip manual).
  - **Then:** Todo el ciclo de extracción, validación de cabecera (`RIFF...WEBP` / `0xFF 0xD8`), decodificación `Bitmap` y composición de `PixelGrid` se realiza en memoria local en menos de `150 ms`, mostrando la fotografía nítida sin invocar a Google Chrome ni realizar llamadas de red.

- **Criterio 09.5 — Lanzamiento Asistido de la Pantalla «Abrir de forma predeterminada» de Android:**
  - **Given:** El usuario desea que Android abra automáticamente los enlaces de `img2asciiIM` en la app instalada en lugar de delegarlos en Google Chrome.
  - **When:** El usuario pulsa la opción «Configurar enlaces predeterminados» en la interfaz (menú superior o diálogo de ayuda de enlaces).
  - **Then:** La aplicación construye y despacha un `Intent` explícito hacia la configuración del sistema Android:
    ```kotlin
    Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
      data = Uri.parse("package:" + context.packageName)
    }
    ```
    Si el sistema o la capa del fabricante (OEM) no soporta dicha acción (`ActivityNotFoundException`), aplica un fallback seguro hacia `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, guiando al usuario a la sección «Abrir de forma predeterminada» para marcar «Abrir enlaces compatibles» y añadir el dominio `laralbir.github.io`.

### RF-10: Visualización de Fotografía Real Decodificada y Guardado en Galería

La aplicación **debe** proporcionar la visualización inmediata de la fotografía real decodificada en alta definición y a todo color junto con la capacidad de persistirla en el almacenamiento multimedia público del dispositivo sin requerir permisos invasivos en Android moderno:

- **Criterio 10.1 — Generación de Bitmap de Vista Previa y Activación Predeterminada de «Foto Real (HD)»:**
  - **Given:** Una imagen decodificada o cargada desde cualquier origen, con especial énfasis en flujos de recepción e importación de mensajes (`ImageSourceOrigin.CLIPBOARD`, `ImageSourceOrigin.DEEP_LINK` o `ImageSourceOrigin.INCOMING_SHARE`).
  - **When:** El conversor/ViewModel procesa la imagen e inicializa el estado de visualización en `ConverterViewModel`.
  - **Then:**
    1. La aplicación genera un `Bitmap` de vista previa a todo color a partir del mapa de píxeles decodificado (`PixelGrid` / `previewBitmap`).
    2. La interfaz conmuta y activa de forma automática y por defecto la pestaña de visualización **«Foto Real (HD)»**, permitiendo al receptor o usuario contemplar inmediatamente la fotografía original a todo color sin requerir toques adicionales.

- **Criterio 10.2 — Selector Interactivo de Vista Previa por Pestañas («Foto Real (HD)» vs. «Arte de Texto»):**
  - **Given:** Una imagen cargada en la pantalla principal de la aplicación.
  - **When:** El usuario interactúa con el selector de pestañas ubicado sobre el área central de previsualización.
  - **Then:**
    1. La interfaz presenta dos pestañas claramente rotuladas y accesibles: **«Foto Real (HD)»** y **«Arte de Texto»**.
    2. Al seleccionar la pestaña **«Foto Real (HD)»**, la app renderiza el mapa de bits original a todo color con escalado proporcional adaptativo (`ContentScale.Fit`), fondo de alto contraste y soporte de inspección visual nítida.
    3. Al seleccionar la pestaña **«Arte de Texto»**, la app conmuta a la burbuja de chat con la matriz de caracteres de texto (Braille Unicode, ASCII o Bloques), permitiendo alternar el tema claro/oscuro y ajustar los controles de edición y motor en tiempo real.

- **Criterio 10.3 — Acción «Guardar Foto» para Persistencia en Galería sin Permisos en Android 10+ (`MediaStore`):**
  - **Given:** Una fotografía decodificada visible en la aplicación.
  - **When:** El usuario pulsa la acción o botón **«Guardar Foto»** (ubicado en el visor de foto real y/o en la barra de acciones de la interfaz).
  - **Then:**
    1. La aplicación persiste el `Bitmap` decodificado en la galería pública del dispositivo mediante la API oficial `MediaStore.Images.Media` bajo el directorio relativo `Pictures/img2asciiIM/` (`RELATIVE_PATH = "Pictures/img2asciiIM"`, `MIME_TYPE = "image/png"` o `"image/jpeg"`).
    2. La operación de guardado se ejecuta **100 % offline**, en un despachador de segundo plano (`Dispatchers.IO`), sin solicitar ni requerir ningún permiso de almacenamiento en tiempo de ejecución en Android 10+ (API 29+) gracias al modelo Scoped Storage del sistema.
    3. Tras la persistencia exitosa, la imagen queda inmediatamente indexada y visible en Google Fotos y en la app Galería del sistema Android, y la interfaz proporciona retroalimentación inmediata al usuario (notificación háptica suave y mensaje Snackbar/Toast confirmando: *«Foto guardada en la galería (Pictures/img2asciiIM)»*).

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
5. **Invariante de Cota Máxima de Enlace Seguro (`MAX_SAFE_URL_LENGTH = 3.800` caracteres) y Límites de WhatsApp y Telegram:**
   Para cualquier enlace Deep Link autónomo generado (`#d=...`):
   - $\text{length}(\text{url}) \le \text{MAX\_SAFE\_URL\_LENGTH} = 3.800$ caracteres en **todas** las plataformas soportadas y combinaciones de modos (estándar o expandido HD en WhatsApp y Telegram).
   - *Justificación técnica:* WhatsApp y la pila IPC/componentes de renderizado de texto de Android aplican un truncamiento forzado en tokens y URLs continuas (cadenas sin espacios) a los 4.096 caracteres exactos. Igualmente, Telegram corta mensajes a los 4.096 caracteres partiéndolos en dos burbujas. El margen de seguridad de $4.096 - 3.800 = 296$ caracteres garantiza total inmunidad frente a recortes destructivos de URL y particiones de burbuja.
   - En Telegram bajo modo híbrido dual (`includeTextArt == true` y `attachDeepLink == true`), se cumple complementariamente $\text{length}(\text{arte}) + 1 + \text{length}(\text{url}) \le 3.900$ con $\text{length}(\text{url}) \le 3.800$.
6. **Invariante de Eficiencia de Códec WebP Lossy:** El generador de enlaces autónomos produce exclusivamente flujos WebP Lossy (`WEBP_LOSSY` / `WEBP`), garantizando un ahorro del 35% al 50% de caracteres frente a JPEG para idéntica fidelidad perceptual.
7. **Invariante de Extracción Determinista de Payloads en Portapapeles:**
   La extracción de payloads desde el portapapeles se realiza mediante un analizador determinista tolerante (patrón regex estricto sin retroceso catastrófico):
   `(?:https?://[^#?\s]+/img2asciiIM/(?:index\.html)?)?(?:#|\?)(?:d|data)=([A-Za-z0-9_-]+)|img2asciiim://view\?(?:d|data)=([A-Za-z0-9_-]+)`
   garantizando un procesamiento lineal $O(N)$ sobre la longitud del texto copiado ($N \le 70.000$ caracteres), aislando con precisión el payload WebP aunque el usuario copie el mensaje completo con arte de texto y comentarios.
8. **Invariante de Privacidad Efímera en Portapapeles (Cero Persistencia de Datos Personales):**
   El portapapeles del sistema se inspecciona única y exclusivamente para identificar si contiene un payload fotográfico compatible con `img2asciiIM`. Ningún texto de chat, contraseña o dato personal no relacionado se almacena en memoria secundaria, disco, base de datos ni registros de log (`Logcat`), respetando la privacidad absoluta del usuario.
9. **Invariante de Guardia de Integridad de Cabecera RIFF WebP en Recepción:**
   Todo flujo binario decodificado con cabecera `"RIFF"` (`0x52 0x49 0x46 0x46`) y `"WEBP"` (`0x57 0x45 0x42 0x50`) debe satisfacer estrictamente $\text{bytes.size} \ge \text{expectedPayloadSize}$, donde $\text{expectedPayloadSize} = \text{readLittleEndian32}(\text{bytes}[4..7]) + 8$. Si $\text{bytes.size} < \text{expectedPayloadSize}$, el payload se clasifica como truncado (por clientes IM como WhatsApp o copiado incompleto) y se rechaza deterministamente como `DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)`, impidiendo el renderizado de imágenes en negro o flujos corruptos.

---

## 5. Restricciones No Funcionales (`RNF`)

| ID | Categoría | Requisito Medible | Método de Verificación |
| :--- | :--- | :--- | :--- |
| **RNF-01** | **Latencia de Conversión** | El pipeline completo en memoria (`PixelGrid` → cadena formateada) **debe** completarse en **`< 150 ms`** (objetivo P95 `< 80 ms`) en un dispositivo Android de gama media para anchos IM (`12–60` columnas), sin bloquear el hilo principal (`Main`). | Test cronometrado en JVM y test instrumentado en dispositivo |
| **RNF-02** | **Presupuesto de Memoria (RAM)** | La decodificación de cualquier imagen —incluyendo fotografías de cámara de **48 MP–108 MP** o exportaciones pesadas— **debe** usar submuestreo previo (`inSampleSize` / `ImageDecoder.setTargetSize`) manteniendo el incremento máximo de heap **`< 20 MB`**. | Test con imagen sintética de alta resolución y medición de dimensiones decodificadas |
| **RNF-03** | **Privacidad y Cero Exfiltración** | El 100 % del motor de conversión fotográfica y decodificación de deep links opera estrictamente en memoria local sin emitir tráfico de red. El permiso `android.permission.INTERNET` se utiliza exclusivamente para la comprobación manual/opcional de releases oficiales contra la API pública de GitHub (`api.github.com`), sin telemetría, analítica ni exfiltración de imágenes. | Test de auditoría del `AndroidManifest.xml` y trazabilidad de red |
| **RNF-04** | **Operación 100% Offline** | El 100% de las funciones de conversión, previsualización, copiado y lanzamiento de `ACTION_SEND` **deben** funcionar con el dispositivo en Modo Avión o conectado a Wi-Fi de avión restringido. | Prueba E2E en dispositivo con conectividad deshabilitada |
| **RNF-05** | **Accesibilidad (`a11y`)** | Todos los botones e iconos tendrán `contentDescription` descriptivos en español, tamaño táctil mínimo `48dp × 48dp`, y el contenedor de vista previa agrupará su semántica (`clearAndSetSemantics`) para anunciar un resumen legible en **TalkBack** en lugar de leer cientos de símbolos Braille o ASCII uno tras otro. | Compose Semantics UI Test |
| **RNF-06** | **Velocidad de Suite TDD** | Todos los tests unitarios de `domain/` **deben** ejecutarse en JVM pura en **`< 100 ms`** en conjunto. | Ejecución `./gradlew testDebugUnitTest` |
| **RNF-07** | **Respeto de Insets de Navegación (Edge-to-Edge Safe Area)** | La barra inferior de acciones (`Copiar` y `Enviar a...`) **debe** aplicar obligatoriamente `navigationBarsPadding()` para garantizar que la barra de navegación del sistema Android (tanto barra de 3 botones como barra gestual) jamás se superponga ni interfiera con los botones de acción interactivos, extendiendo a su vez el fondo del contenedor hasta el borde inferior de la pantalla. | Compose UI Test y verificación en hardware físico USB |
| **RNF-08** | **Actualizaciones Seguras y Resiliencia de Red** | La comprobación de nuevas versiones en GitHub Releases debe aplicar un tiempo límite estricto `≤ 4.000 ms`, degradar de forma silenciosa e inocua en Modo Avión o redes cautivas sin lanzar excepciones no controladas, y verificar sumas criptográficas SHA-256 en las releases publicadas por CI/CD. | Test unitario con simulación de timeouts/red y workflow GitHub Actions |
| **RNF-09** | **Latencia y Privacidad en Portapapeles** | La comprobación del portapapeles en `onResume` o al pulsar el chip manual **debe** completarse en **`< 20 ms`** en un hilo no bloqueante (`Dispatchers.Default`), sin provocar tirones (*jank*) en el renderizado de Compose ni registrar datos privados del portapapeles en logs. | Test cronometrado JVM y auditoría de privacidad |

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
| **CB-08** | **Intento de desactivar simultáneamente arte de texto y enlace HD** | La interfaz y el ViewModel bloquean la desactivación de la última opción activa (o reactivan automáticamente la otra), garantizando la invariante de que el mensaje final nunca esté vacío ni carezca de contenido compartible. | `ui/` + `domain/` |
| **CB-09** | **Deep Link entrante malformado, truncado, en formato heredado (`#data=`) o con payload corrupto** | `DeepLinkImageCodec.decodeFromDeepLink()` soporta tolerante e indistintamente `#d=`, `#data=`, `?d=` y `?data=`, así como flujos WebP y JPEG, y captura de forma segura `IllegalArgumentException` o fallos de decodificación devolviendo `DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)`, notificando mediante snackbar sin crashear. | `data/` + `ui/` |
| **CB-10** | **Fallo de red, timeout o portal cautivo en comprobador in-app de GitHub Releases** | `GitHubUpdateChecker.check()` aplica timeouts de 4.000 ms y captura `IOException` y códigos HTTP anómalos devolviendo `UpdateCheckResult.OfflineOrError`, informando discretamente al usuario sin interrumpir la experiencia de conversión en vuelo. | `data/` + `ui/` |
| **CB-11** | **Riesgo de truncado en Binder/IPC de Android en `EXTRA_TEXT` para mensajes extensos (> 4 KB) en WhatsApp** | `ImShareIntentFactory` y `SystemClipboardAdapter` aplican entrega Clipboard-First obligatoria: copian preventivamente la totalidad del texto (hasta 65.536 chars) al portapapeles y notifican en pantalla para que el usuario pueda pegar manualmente si WhatsApp o el SO recortan el texto. | `data/` + `ui/` |
| **CB-12** | **Arranque de la aplicación sin imagen precargada (Empty State)** | La aplicación inicia en un estado limpio interactivo sin instanciar bitmaps demo ni disparar el pipeline de conversión, manteniendo el consumo de memoria en el mínimo basal e invitando explícitamente a cargar fotos desde Galería, Archivos/Drive o Cámara. | `ui/` |
| **CB-13** | **Portapapeles con texto arbitrario o sin payload fotográfico (ej. texto de chat normal)** | El extractor determina que la cadena no contiene un payload válido de `img2asciiIM`. En `onResume` no realiza ninguna acción intrusiva; si el usuario pulsó el chip «Portapapeles», muestra una notificación informativa (*«El portapapeles no contiene un enlace ni una foto de img2asciiIM»*) sin alterar la imagen en pantalla. | `data/` + `ui/` |
| **CB-14** | **Portapapeles vacío o con tipos de datos no textuales (audio, clip binario)** | `ClipboardManager.primaryClip` es `null` o no contiene elementos con texto plano; el adaptador maneja la ausencia de forma segura sin excepciones `NullPointerException`. | `data/` + `ui/` |
| **CB-15** | **Portapapeles con payload WebP truncado o corrupto por copiado manual incompleto en el chat** | `DeepLinkImageCodec` aplica la guardia de integridad de cabecera RIFF WebP y captura de forma segura `IllegalArgumentException` o fallos de decodificación devolviendo `DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)`, informando al usuario en la UI para que copie el enlace completo. | `data/` + `ui/` |
| **CB-16** | **Dispositivo OEM o versión Android sin soporte para `ACTION_APP_OPEN_BY_DEFAULT_SETTINGS`** | La app captura `ActivityNotFoundException` y redirige inmediatamente a `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, garantizando que el usuario acceda siempre a la configuración de la app en Ajustes del sistema. | `ui/` |
| **CB-17** | **Payload truncado por cliente de mensajería (límite duro de 4.096 caracteres en WhatsApp o enlaces de versiones anteriores sin cota de 3.800 chars)** | Cuando un remitente envía un enlace de una versión antigua sin acotación o el cliente de mensajería (WhatsApp/Android) trunca la URL continua al alcanzar los 4.096 caracteres, faltan los bytes finales del WebP. En recepción (vía App Link, visor web o portapapeles), `DeepLinkImageCodec` comprueba la guardia de cabecera RIFF WebP ($\text{bytes.size} < \text{expectedSize}$), detecta el truncamiento preventivamente antes de decodificar (evitando que Android intente decodificar bytes incompletos y pinte una imagen en negro o corrupta) y devuelve `DomainResult.Failure(ImageDecodeError.InvalidOrCorruptedImage)`. La UI notifica que el enlace fue cortado por el cliente de mensajería y sugiere solicitar el reenvío desde una versión actualizada. | `data/` + `ui/` |
| **CB-18** | **Fallo al persistir en almacenamiento multimedia (`MediaStore`) al pulsar «Guardar Foto»** | Capturar de forma segura `IOException` o `IllegalStateException` durante la escritura en `MediaStore`, liberando el descriptor abierto (`Uri`) de forma atómica y notificando en la interfaz con un mensaje amigable (*«No se pudo guardar la foto en la galería»*) sin provocar fallos de aplicación. | `data/` + `ui/` |

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
| `CA-06.1`, `CA-06.2`, `RNF-07` | `ui` | Robolectric + USB (`Slice 9`) | `ConverterScreenRobolectricTest`, `BottomActionControlsTest` |
| `CA-07.1..07.5`, `CB-08`, `CB-09`, `CB-17` | `data` + `ui` | Unit JVM + Robolectric (`Slice 10`) | `DeepLinkImageCodecTest`, `ConverterViewModelTest`, `ConverterScreenRobolectricTest` |
| `CA-08.1..08.5`, `RNF-08`, `CB-10` | `data` + `ci` | Unit JVM + CI/CD (`Slice 11`) | `GitHubUpdateCheckerTest`, `.github/workflows/release.yml` |
| `CA-01.7`, `CA-09.1..09.5`, `RNF-09`, `CB-13..17` | `data` + `ui` | Unit JVM + Robolectric (`Slice 12`) | `ClipboardImageExtractorTest`, `ConverterViewModelClipboardTest`, `NativeLinksSettingsTest` |
| `CA-10.1`, `CA-10.2`, `CA-10.3`, `CB-18` | `ui` + `data` | Unit JVM + Robolectric (`Slice 13`) | `RealPhotoViewerTest`, `SaveImageToGalleryTest` |

---

## 8. Decisiones de Diseño Validadas por el Usuario

> **Estado de Decisiones:** Confirmadas por el usuario (`2026-11-06`), adoptando la **Opción A** en `DD-01`..`DD-07`.

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

### DD-06: Conmutadores Modulares, Códec WebP Lossy, Redimensionamiento Adaptativo Estricto, Cota Universal `MAX_SAFE_URL_LENGTH = 3.800` y Límite Duro de WhatsApp (4.096 chars)
- **Opción A (Recomendada y Aprobada):** Permitir conmutar independientemente `[x] Incluir arte de texto (ASCII / Braille)` y `[x] Adjuntar enlace a foto HD (#d=...)`. Emplear el códec **WebP Lossy** (ahorro 35–50% frente a JPEG), prefijo corto `#d=<base64>` y restricción universal de enlace seguro **`MAX_SAFE_URL_LENGTH = 3.800` caracteres** tanto en WhatsApp como en Telegram (evitando el límite duro de WhatsApp y la pila Android que corta cadenas continuas a los 4.096 chars, así como la división en dos burbujas en Telegram). Incorporar algoritmo de reducción adaptativa estricto de 4 fases para convergencia determinista a $\le \text{budget}$, entrega **Clipboard-First** en WhatsApp para textos extensos, y **guardia de integridad de cabecera RIFF WebP** en recepción para detectar proactivamente enlaces truncados (`CB-15`, `CB-17`) evitando renderizar imágenes en negro o corruptas.
- **Opción B:** Forzar siempre la presencia del arte de texto con códec JPEG tradicional, prefijos de texto explicativos, dimensiones estáticas sin recálculo adaptativo ni comprobación de cabecera RIFF.
- **Estado:** `[x] Aprobada: Opción A`

### DD-07: Distribución Autónoma de Releases Firmadas y Actualizaciones In-App
- **Opción A (Recomendada):** Implementar pipeline automatizado en GitHub Actions (`.github/workflows/release.yml`) que ante tags Git `v*.*.*` compila con JDK 21, firma el APK oficial (con fallback transparente a `debug` en builds locales), genera sumas criptográficas SHA-256 y publica la GitHub Release oficial. Dotar a la aplicación de un comprobador in-app (`GitHubUpdateChecker`) contra la API pública de GitHub con comparación semver determinista y resiliencia offline total ante fallos de red en vuelo.
- **Opción B:** Depender exclusivamente de descargas manuales desde la interfaz web de GitHub sin comprobador integrado en la aplicación ni cálculo de checksums en el workflow.
- **Estado:** `[x] Aprobada: Opción A`

### DD-08: Arquitectura de App Links y Esquema Nativo (`img2asciiim://` + Universal Links + `.well-known/assetlinks.json`)
- **Opción A (Recomendada y Aprobada):** Arquitectura dual de apertura:
  1. Esquema URI personalizado `img2asciiim://view?d=<base64>` para invocación directa determinista desde el visor web móvil o enlaces directos sin depender de diálogos de ambigüedad.
  2. Android App Links verificados sobre `https://laralbir.github.io/img2asciiIM/` (`android:autoVerify="true"`), respaldados por la publicación de `docs/.well-known/assetlinks.json` y el archivo `docs/.nojekyll` en GitHub Pages para evitar que Jekyll ignore el directorio con punto.
- **Opción B:** Usar únicamente enlaces web HTTPS estándar sin esquema nativo ni integración de App Links, dependiendo siempre del navegador web.
- **Estado:** `[x] Aprobada: Opción A`

### DD-09: Estado Inicial de la Aplicación (Empty State Interactivo vs. Precarga de Imagen Demo)
- **Opción A (Recomendada y Aprobada):** Iniciar la aplicación con un **Empty State** limpio, elegante y funcional. La pantalla inicial no carga automáticamente ninguna imagen de muestra, evitando confusión y uso innecesario de memoria, y presenta accesos directos destacados para seleccionar foto desde **Galería**, **Archivos / Drive**, **Cámara** o importar desde **Portapapeles**.
- **Opción B:** Cargar de manera predeterminada y obligatoria una imagen de muestra demo sintética en cada arranque en frío.
- **Estado:** `[x] Aprobada: Opción A`

### DD-10: Flujo de Ingesta de Portapapeles (Detección Automática Asistida + Chip Manual) y Configuración de Enlaces Nativos
- **Opción A (Recomendada y Aprobada):** Arquitectura híbrida de ingesta de portapapeles y enlace nativo:
  1. Detección automática no intrusiva al reanudar la app (`onResume`), auto-cargando la foto si la app está en Empty State o mostrando un aviso amigable de acción rápida si ya hay una conversión en curso.
  2. Chip interactivo **«Portapapeles»** permanentemente accesible en la barra superior de origen de imagen y en el Empty State.
  3. Botón directo de configuración en la interfaz para guiar al usuario a los Ajustes de Android (`Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS`), permitiéndole habilitar la apertura directa de `laralbir.github.io` en la aplicación nativa sin pasar por Google Chrome.
- **Opción B:** Exclusivamente chip manual en la barra de origen, sin detección automática en `onResume` ni acceso asistido a los Ajustes de enlaces de Android.
- **Estado:** `[x] Aprobada: Opción A`

### DD-11: Visualización de Fotografía Real Decodificada (Pestañas Duales HD/Texto) y Persistencia en Galería vía MediaStore
- **Opción A (Recomendada y Aprobada):** Arquitectura de visualización dual integrada directamente en el contenedor central de previsualización mediante un selector de pestañas (**«Foto Real (HD)»** y **«Arte de Texto»**). Al decodificar o importar imágenes desde portapapeles, deep link o share entrante, la app activa por defecto la pestaña «Foto Real (HD)», garantizando que el receptor vea la imagen original al instante. Persistencia directa en galería mediante `MediaStore.Images.Media` en `Pictures/img2asciiIM/` (100% offline, sin permisos en Android 10+).
- **Opción B:** Ventana modal flotante o actividad separada para ver la foto real y exportación dependiendo exclusivamente del Share Sheet del sistema sin guardado directo en galería.
- **Estado:** `[x] Aprobada: Opción A`

---

## 9. Registro de Aprobación (*Sign-off*)

- [x] Todos los requisitos (`RF-01`..`RF-10`, `RNF-01`..`RNF-09`) cuentan con criterios **Given / When / Then** deterministas y trazables.
- [x] El plan arquitectónico ([`plan.md`](plan.md)) y la hoja de ruta TDD ([`tasks.md`](tasks.md)) están alineados con esta especificación.
- [x] **Decisiones Abiertas (`DD-01`..`DD-11`) confirmadas por el usuario (`Opción A` en todas).**
- [x] **Estado cambiado a `APROBADO` para habilitar el Ciclo TDD 1 (`2026-11-06`).**
