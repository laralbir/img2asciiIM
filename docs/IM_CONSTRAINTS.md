# Restricciones Técnicas de Redes Aéreas y Plataformas de Mensajería Instantánea (`img2asciiIM`)

Este documento especifica las restricciones físicas de red, protocolos de transporte, límites de caracteres, motores tipográficos y contratos de `Intent` en Android verificados para el diseño y desarrollo guiado por pruebas (**TDD**) de **`img2asciiIM`**.

---

## 1. Wi-Fi en Vuelos Comerciales (*Free Messaging Tiers*)

### 1.1 Panorama real en aerolíneas comerciales

La mayoría de las aerolíneas comerciales operan enlaces satelitales compartidos (**Viasat**, **Panasonic Avionics**, **Intelsat/Gogo**, **Inmarsat / European Aviation Network**) con ancho de banda severamente acotado y alta latencia (~500–800 ms en órbita geoestacionaria GEO). Para evitar el colapso del enlace satelital, ofrecen planes gratuitos o básicos restringidos estrictamente a **mensajería de texto**:

| Aerolínea | Plan / Modalidad a bordo | Proveedor habitual | Política efectiva sobre tráfico de mensajería |
| :--- | :--- | :--- | :--- |
| **Iberia** | *Mensajería gratuita* (socios Iberia Club y cabina Business) | Panasonic / Inmarsat EAN / Viasat *(transición parcial futura a Starlink en grupo IAG)* | Solo texto en apps de chat (WhatsApp, iMessage, Messenger). Bloqueo explícito de fotos, vídeos, notas de voz y adjuntos. |
| **Vueling** | Pases promocionales / tarifa básica de mensajería a bordo | Viasat / Inmarsat EAN | Tráfico ligero de texto; descargas y subidas a CDNs multimedia bloqueadas salvo compra de pase completo. |
| **Lufthansa** | *FlyNet® Free Messaging* (con Lufthansa Travel ID) | Deutsche Telekom / Inmarsat / Panasonic | Mensajería gratuita con *throttling* severo y filtrado de protocolos que impide la transferencia práctica de imágenes sin pase superior. |
| **Delta Air Lines** | *Delta Sync / Free Messaging* | Viasat / Intelsat | Portal cautivo especifica *"Text only — photos, videos, and SMS/MMS are not supported"* en el nivel básico/mensajería. |
| **United Airlines** | *Free Messaging* | Panasonic / Viasat / Thales *(despliegue gradual de Starlink)* | Permite texto en WhatsApp, iMessage y Messenger; bloquea transferencia de imágenes, stickers pesados y audio. |
| **Air Europa** | Red local *AIR EUROPA ONTHEAIR* (ocio sin Internet) + bono básico *Chatea* | Panasonic Avionics | La red gratuita solo sirve el portal multimedia interno; el bono básico *Chatea* habilita chat de texto puro pero restringe navegación general y carga multimedia pesada. |

---

### 1.2 Arquitectura de filtrado a bordo: ¿Por qué fallan imágenes, audios y stickers mientras el texto sí llega?

Aunque WhatsApp, Instagram y Telegram cifran sus cargas útiles extremo a extremo o en tránsito (**TLS 1.2/1.3**, **Noise Protocol**, **MTProto**), las pasarelas satelitales a bordo emplean firewalls de capa 7 con **Inspección Profunda de Paquetes (DPI)**, listas blancas de **DNS** y filtrado del campo no cifrado **SNI (*Server Name Indication*)** durante el `ClientHello` de TLS, además de control por rangos IP/ASN y *traffic shaping*:

1. **Separación arquitectónica entre sockets de señalización y CDNs multimedia:**
   Las plataformas de mensajería desacoplan el canal de tiempo real (texto/metadatos) del almacenamiento de binarios pesados:
   * **WhatsApp:**
     * **Canal de texto permitido (Whitelist):** El cliente mantiene una conexión TCP persistente (puertos `443` / `5222`, protocolo *Noise* / XMPP binario) contra los nodos de señalización **`g.whatsapp.net`**, **`g-fallback.whatsapp.net`** y **`e1.whatsapp.net`–`e16.whatsapp.net`**. Los mensajes de texto viajan integrados en tramas diminutas (pocos kilobytes) dentro de este socket ya autorizado.
     * **Canal multimedia bloqueado (Drop / Timeout):** Para enviar o descargar una foto, audio o sticker, la app debe abrir una sesión HTTPS independiente contra el *Media Meta Gateway* (**`mmg.whatsapp.net`**) y las CDNs **`media-*.cdn.whatsapp.net`**. El firewall a bordo detecta estos dominios por DNS/SNI y descarta los paquetes (`DROP` / `TCP RST`), dejando la imagen en un bucle infinito de *"Enviando…"* o reloj pendiente.
   * **Instagram Direct:**
     * **Canal de texto:** Utiliza endpoints ligeros de mensajería/MQTT de Meta (`edge-chat.instagram.com`, `mqtt-mini.facebook.com`, `i.instagram.com` cuando el ecosistema Meta está incluido en el pase de chat).
     * **Canal multimedia bloqueado:** La subida resumible de binarios atraviesa **`rupload.facebook.com`** y la descarga de imágenes/stickers apunta a **`scontent.cdninstagram.com`**, **`*.cdninstagram.com`** y **`*.fbcdn.net`**, todos ellos bloqueados en planes de solo texto.
   * **Telegram:**
     * **Arquitectura dividida:** Los mensajes de texto y metadatos viajan a los **Data Centers principales (DC 1–5)** mediante tramas cortas **MTProto**, mientras que las fotos, vídeos y archivos pesados emplean flujos TCP de alto volumen (`upload.saveFilePart` / `upload.getFile`) o nodos **CDN de medios dedicados** (rangos como `149.154.160.0/20` y `91.108.4.0/22`).
     * **Efecto en vuelo:** Cuando la aerolínea permite tráfico básico hacia Telegram o aplica *traffic shaping* agresivo, las ráfagas binarias sostenidas y los servidores CDN de medios sufren caída por timeout, mientras que un payload compacto de texto UTF-8 (<= 4.096 caracteres, ~4–12 KB) logra transmitirse en pocos paquetes TCP. *(Nota: algunas aerolíneas norteamericanas bloquean MTProto por completo en su tarifa gratuita si no coincide con SNI de WhatsApp/Meta/Apple; si el pasajero contrata el bono básico de chat compatible o tiene salida de texto, el mensaje de texto pasa pero la foto falla).*

---

### 1.3 Por qué la conversión 100% *offline* en el dispositivo (*on-device*) es obligatoria

* **Bloqueo total de APIs HTTPS arbitrarias (*Walled Garden*):** El router del avión intercepta o descarta cualquier petición DNS/TLS hacia dominios no incluidos explícitamente en la lista blanca de mensajería (por ejemplo, `api.openai.com`, `*.run.app`, `*.amazonaws.com` o microservicios propios de conversión de imágenes), devolviendo redirecciones al portal cautivo, `NXDOMAIN` o *connection timeout*.
* **Operación en Modo Avión sin Wi-Fi:** Muchos usuarios capturan una foto desde la ventanilla en **Modo Avión estricto**, generan el arte ASCII/Braille sin conexión y solo activan el Wi-Fi gratuito un instante para pegar/enviar el texto.
* **Requisito arquitectónico para `img2asciiIM`:**
  * **Cero dependencia de red:** Toda la tubería (decodificación `Bitmap`, redimensionado con corrección de relación de aspecto, luminancia/contraste, *dithering* opcional y mapeo a rampa ASCII/Braille) debe ejecutarse **100% localmente en CPU/memoria del dispositivo**.
  * **`AndroidManifest.xml` sin permiso `INTERNET`:** No se debe declarar `<uses-permission android:name="android.permission.INTERNET" />`, garantizando por diseño privacidad absoluta y funcionamiento determinista en vuelo.

---

### 1.4 Selección de imágenes en Android: Almacenamiento Local y Cámara vs. Google Drive / Cloud (SAF & Photo Picker)

| Origen de la imagen | API en Android | ¿Funciona en pleno vuelo? | Explicación técnica a nivel de sistema |
| :--- | :--- | :--- | :--- |
| **Cámara del dispositivo** | `ActivityResultContracts.TakePicture` + URI temporal de `FileProvider` propio | **100% Sí** | Los bytes JPEG/WEBP se escriben directamente en `context.cacheDir` en almacenamiento flash local (UFS/eMMC). |
| **Galería / Almacenamiento interno** | `PickVisualMedia` (ítems locales) / `MediaStore` local | **100% Sí** | El `ContentResolver` abre un descriptor de fichero local (`ParcelFileDescriptor`) sin tocar la pila de red. |
| **Google Drive / Cloud (cacheado *offline* antes del vuelo)** | SAF (`ACTION_OPEN_DOCUMENT`) / `DocumentsProvider` (`com.google.android.apps.docs.storage`) | **Sí (condicional)** | Si el usuario marcó *"Disponible sin conexión"* (*Available offline*) o abrió el archivo antes de despegar, el `DocumentsProvider` sirve la réplica de su caché SQLite/disco local. |
| **Google Drive / Google Photos (*cloud-only*, no cacheado)** | SAF (`ACTION_OPEN_DOCUMENT`) o `CloudMediaProvider` en Photo Picker | **NO (Falla con error I/O)** | La galería muestra la miniatura cacheada, pero al invocar `contentResolver.openInputStream(uri)` el proveedor intenta descargar el binario real desde `*.googleapis.com` / `*.googleusercontent.com` (bloqueados por el firewall del avión), colgando el hilo hasta lanzar `IOException` / `FileNotFoundException`. |

#### Reglas de diseño e invariantes TDD para carga de imágenes:
1. **Lectura asíncrona con *timeout* defensivo:** Toda llamada a `contentResolver.openInputStream(uri)` y `BitmapFactory.decodeStream(...)` debe ejecutarse en `Dispatchers.IO` envuelta en un límite de tiempo razonable (p. ej. `withTimeout(5_000L)`) y capturar `IOException`, `FileNotFoundException` y `SecurityException`, informando al usuario en español (*"No se pudo leer la imagen. Si proviene de Google Drive o la nube, asegúrate de que esté descargada en el dispositivo"*).
2. **Sugerencia de archivos locales en SAF:** Si en algún flujo se lanza `Intent(Intent.ACTION_OPEN_DOCUMENT)`, incluir siempre `putExtra(Intent.EXTRA_LOCAL_ONLY, true)`. *(Nota: en Android `EXTRA_LOCAL_ONLY` es una directiva orientativa —*hint*—; Google Drive puede seguir apareciendo para mostrar archivos cacheados, por lo que el manejo de `IOException` sigue siendo obligatorio).*

---

## 2. Restricciones Técnicas Exactas por Plataforma de Mensajería Instantánea

### 2.1 WhatsApp (`com.whatsapp` y WhatsApp Business `com.whatsapp.w4b`)

* **Límite máximo de mensaje:**
  * **65.536 caracteres** ($2^{16}$ unidades de código UTF-16 `Char` en el cliente móvil estándar). Superar este umbral deshabilita el envío o trunca el texto en el compositor.
  * **Plegado visual (*"Leer más"* / *"Read more"*):** WhatsApp colapsa visualmente las burbujas largas tras las primeras líneas visibles (~15–20 líneas en chat personal, ~4–5 líneas si hay vista previa o plantilla). Al tocar *"Leer más"*, la burbuja se expande íntegra en la misma vista sin romper el bloque monoespaciado. Aun así, mantener el presupuesto práctico por debajo de **3.500–4.000 caracteres** evita jank de renderizado en `RecyclerView` y mantiene márgenes holgados frente a transacciones Binder IPC.
* **Sintaxis Monoespaciada y reglas estrictas del parser de WhatsApp:**
  * **Bloque multilínea:** Triple tilde invertida (*triple backticks*): ```` ```<fila_1>\n<fila_2>\n...<fila_N>``` ````.
  * **Código en línea (*inline code*):** Tilde invertida simple `` `texto` `` (soportado desde 2024).
  * **Aislamiento de formato interno:** Dentro de un bloque ```` ```...``` ```` válido, WhatsApp **ignora** `*negrita*`, `_cursiva_` y `~tachado~`, renderizando todos los caracteres con fuente fija (`Roboto Mono`).
  * **Sin etiqueta de lenguaje:** A diferencia de Markdown en GitHub o Telegram, WhatsApp **no** interpreta texto tras la apertura como nombre de lenguaje (` ```python ` mostraría literalmente la palabra `python`).
* **🚨 CASOS BORDE CRÍTICOS PARA TDD EN WHATSAPP:**
  1. **Colisión mortal con Backticks (`` ` ``, `U+0060`) en la rampa ASCII:**
     * La célebre rampa estándar de 70 niveles de Paul Bourke (`` $@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\|()1{}[]?-_+~<>i!lI;:,"^`'.  ``) **incluye el carácter `` ` `` en la posición 66** (zonas muy claras).
     * WhatsApp **no admite secuencias de escape (`\`)** dentro de ```` ``` ````. Si tres píxeles claros contiguos producen ```` ``` ```` en mitad de la imagen, **el bloque monoespaciado se cierra prematuramente a mitad del dibujo**, destruyendo la alineación de todo el resto del mensaje y reactivando accidentalmente `*`, `_`, `~` y `` ` `` como formato enriquecido. Asimismo, un solo `` ` `` en el primer o último píxel genera ```` ```` ```` pegado al delimitador y rompe el parser.
     * **Invariante TDD:** El carácter **`U+0060` (`` ` ``) está terminantemente prohibido** en cualquier rampa de caracteres (y cualquier sanitizador previo a envolver en ```` ``` ```` debe reemplazar `` ` `` por `'` `U+0027` o `.` `U+002E`).
  2. **Prohibición de espacio ASCII (`U+0020`) o línea en blanco adyacente a ```` ``` ````:**
     * El parser de WhatsApp exige que los ```` ``` ```` de apertura y cierre **toquen directamente** un carácter imprimible sin espacios intermedios: secuencias como ```` ```   hola``` ```` (donde los primeros píxeles del cielo/fondo blanco son espacios `U+0020`) o ```` ```\n\nhola``` ```` provocan que WhatsApp **anule el formato monoespaciado** (mostrando los backticks crudos en fuente proporcional) o desplace la primera fila respecto a las demás.
     * **Invariante TDD:** En el preset ASCII de WhatsApp, el nivel más luminoso de la rampa **NO debe ser `U+0020` (`' '`) sino `.` (`U+002E`) o `·` (`U+00B7`)** —o bien, en modo Braille, `⠀` (`U+2800`, categoría Unicode `So` *Symbol*, no espacio)—, y la envoltura exacta debe ser ```` ```<fila_1>\n...<fila_N>``` ```` (6 caracteres de *overhead*, sin `\n` ni `U+0020` tocando los delimitadores).
* **Ancho práctico en columnas (burbuja vertical móvil):**
  * En teléfonos Android estándar en orientación vertical (`360dp`–`412dp`, `fontScale = 1.0`), descontando márgenes laterales de la conversación y el padding interno de la burbuja de WhatsApp, caben entre **28 y 36 caracteres `Roboto Mono`** por línea antes de que el *soft line-wrapping* parta cada fila en dos líneas (arruinando por completo la geometría 2D).
  * **Valor óptimo por defecto:** **32 columnas** en ASCII monoespaciado (y **28–30 columnas** si se usa Braille `U+2800`–`U+28FF`).
  * **Relación de aspecto de celda:** En `Roboto Mono`, cada glifo tiene proporción `ancho : alto ≈ 1 : 2` (`0.5`), por lo que el escalado previo al muestreo ASCII debe multiplicar el alto de píxeles por **`~0.5`** para evitar que la imagen aparezca estirada verticalmente al doble.
* **Compartición en Android (`Intent.ACTION_SEND`):**
  * Soporta nativamente `Intent(Intent.ACTION_SEND)` con `type = "text/plain"`, `putExtra(Intent.EXTRA_TEXT, payload)` y `setPackage("com.whatsapp")` (con fallback a `"com.whatsapp.w4b"` o `Intent.createChooser(...)` ante `ActivityNotFoundException`).

---

### 2.2 Telegram (`org.telegram.messenger` / `org.telegram.messenger.web`)

* **Límite máximo de mensaje:**
  * **4.096 caracteres estrictos** por mensaje de texto (`maxMessageLength = 4096`, contabilizados como unidades UTF-16 `String.length` en el cliente Android; cada carácter ASCII, bloque `U+2580`–`U+259F` o Braille `U+2800`–`U+28FF` pertenece al *Basic Multilingual Plane* y computa como **1 carácter**, aunque ocupe 3 bytes en UTF-8).
  * **Consecuencia de superar 4.096:** El receptor de compartición de Telegram trunca el borrador o trocea automáticamente el envío en **dos burbujas independientes**, partiendo la imagen en dos mitades (y, si llevaba ```` ``` ````, rompiendo el emparejamiento de delimitadores en ambas burbujas).
  * **Invariante TDD:** `payload.length <= 4096` siempre, contabilizando todos los saltos de línea `\n` y cualquier delimitador:
    $$\text{Caracteres totales} = H \times W + (H - 1) + \text{overhead\_delimitadores} \le 4096$$
* **Comportamiento real de `Intent.ACTION_SEND` (`text/plain`) vs. Markdown en Telegram Android:**
  * Cuando una app externa invoca `Intent(Intent.ACTION_SEND)` con `type = "text/plain"` hacia `org.telegram.messenger`, `LaunchActivity` redirige al selector de chats y entrega `Intent.EXTRA_TEXT` como **texto plano crudo (`text/plain`)**, **sin transformar automáticamente sintaxis Markdown (`**`, ```` ``` ````) en entidades MTProto (`TL_messageEntityPre`)** en el momento de recibir el Intent.
  * Si el usuario envía directamente desde el selector rápido multi-chat (e incluso en varias versiones del compositor si no edita manualmente el texto), los delimitadores ```` ``` ```` **se envían literalmente visibles en pantalla** y el cuerpo se renderiza en la **fuente proporcional `Roboto`** de Telegram, deformando cualquier arte basado en caracteres ASCII estrechos/anchos (`@`, `.`, `i`, `W`, espacio `U+0020`).
  * **Trampa adicional si el usuario copia/pega o si el parser Markdown sí actúa:** En el parser Markdown de Telegram (`MarkdownV2` / compositor), cualquier palabra en la **primera línea inmediatamente posterior a ```` ``` ```` hasta el primer `\n`** se interpreta como **identificador del lenguaje de programación** (p. ej. ```` ```kotlin\n ````) y **desaparece de la primera fila de la imagen**. Por ello, si alguna vez se usa bloque de código en Telegram, es obligatorio escribir ```` ```\n<fila_1>\n...<fila_N>\n``` ```` (8 caracteres de *overhead*).
* **Estrategia de máxima fidelidad para Telegram:**
  1. **Preset primario recomendado — Unicode Braille (`U+2800`–`U+28FF`) en texto plano (sin ```` ``` ````):**
     * Al usar exclusivamente el bloque Braille (`U+2800`–`U+28FF`, empleando **`⠀` `U+2800` *Braille Pattern Blank* en vez de `U+0020`**), el mensaje compartido por `ACTION_SEND` `text/plain` se ve limpio (sin ```` ``` ```` residuales), aprovecha subpíxeles 2×4 de ancho uniforme desde la fuente de símbolos del sistema Android (`NotoSansSymbols`) y ofrece alta resolución dentro de 4.096 caracteres (p. ej., **30 columnas × 40 filas** = **60 × 160 puntos** en solo `1.239` caracteres).
  2. **Modo secundario opcional (ASCII Monospace para Copiar/Pegar o formateo manual):**
     * Si se exporta ASCII clásico con ```` ```\n...\n``` ````, prohibir `` ` `` en la rampa, incluir siempre el `\n` tras el ```` ``` ```` inicial para proteger la fila 1 del parser de lenguaje de Telegram, y ofrecer botón directo de *Copiar al portapapeles*.

---

### 2.3 Instagram Direct / DM (`com.instagram.android`)

* **Límite máximo de mensaje:**
  * **1.000 caracteres estrictos** por mensaje directo (incluyendo saltos de línea `\n`, espacios y símbolos Unicode). Si el texto supera 1.000 caracteres en la app móvil, el envío falla o queda bloqueado.
  * **Nota crítica sobre longitud en caracteres vs. bytes UTF-8:** Mientras la interfaz móvil del cliente Android valida `<= 1.000` caracteres, la capa backend de mensajería de Meta documenta también límites de **1.000 bytes UTF-8** en determinados endpoints. Dado que cada glifo Braille (`U+2800`–`U+28FF`) o de Bloque (`U+2588`–`U+2593`) ocupa **1 `Char` UTF-16 pero 3 bytes en UTF-8**:
    * Todo preset de Instagram DM **DEBE garantizar por contrato `payload.length <= 1000`**.
    * Se recomienda dimensionar el preset por defecto de Instagram DM de forma compacta (~**500–850 caracteres** para el cliente móvil estándar, p. ej. **24 cols × 26 filas = 649 chars**, e incluir un perfil *Instagram DM Compacto / Safe-Bytes* de **<= 320 caracteres** ≈ **<= 960 bytes UTF-8**, p. ej. **20 cols × 14 filas = 293 chars**, que jamás supera ni 1.000 caracteres ni 1.000 bytes UTF-8).
* **Fuente proporcional y colapso de espacios en blanco:**
  * **Ausencia total de bloques monoespaciados:** Instagram Direct **NO interpreta ```` ``` ```` ni `` ` ``**. Cualquier backtick se imprime como texto basura y la burbuja se renderiza invariablemente con fuente proporcional (`Instagram Sans` / `Roboto`).
  * **Fallo catastrófico del ASCII tradicional y del espacio `U+0020`:** En fuente proporcional, `W` o `@` triplican el ancho de `.` o `:`, y el espacio ASCII estándar (`U+0020`) es sumamente estrecho (además de sufrir recorte de márgenes o líneas vacías). El arte ASCII clásico queda completamente irreconocible.
  * **Por qué Unicode Braille (`U+2800`–`U+28FF`) es indispensable en Instagram DM:**
    1. **Métrica uniforme en fallback de símbolos:** Android resuelve todo el rango `U+2800`–`U+28FF` contra la misma fuente del sistema (`NotoSansSymbols`), manteniendo el avance horizontal uniforme entre celdas incluso dentro de un `TextView` proporcional.
    2. **`U+2800` (`⠀`, *Braille Pattern Blank*) en lugar de `U+0020`:** `U+2800` tiene categoría general Unicode `So` (*Other Symbol*), **no** *Whitespace*. Por tanto, Instagram **no lo recorta** al inicio/final de fila, **no lo colapsa** en secuencias repetidas y le asigna el ancho de una celda Braille en lugar del estrecho `U+0020`. *(Caso borde tipográfico opcional: en ROMs con fuentes personalizadas donde `U+2800` difiera levemente de `U+2801`–`U+28FF`, sustituir el blanco puro por `U+2801` `⠁` —un único micropunto— fuerza el 100% del mismo motor de glifos con puntos).*
    3. **Máxima eficiencia por carácter:** Con solo 1.000 caracteres máximos, una matriz de **24 × 26 caracteres Braille (`649` chars)** codifica realmente una imagen binaria/tramada (*dithered*) de **48 × 104 subpíxeles (4.992 puntos)**.
  * **Alternativa de alto contraste — Elementos de Bloque (`█▓▒░`: `U+2588`, `U+2593`, `U+2592`, `U+2591`):**
    * Ofrecen gran solidez visual tanto en modo claro como oscuro, siempre que **jamás se combine con espacio `U+0020`** (el nivel más claro debe ser `░` `U+2591` o `⠀` `U+2800`). No obstante, en `Roboto` proporcional sin monoespaciado pueden aparecer ligeras variaciones o micro-huecos entre bloques respecto a una terminal fija, por lo que **Braille (`U+2800`–`U+28FF`)** es el preset principal recomendado para Instagram DM y **Bloques (`█▓▒░`)** el secundario.
* **Comportamiento de `Intent.ACTION_SEND` (`text/plain`) en `com.instagram.android` y Fallback obligatorio a Portapapeles:**
  * La app Android de Instagram prioriza recibir flujos binarios (`image/*`, `video/*` mediante `Intent.EXTRA_STREAM`) para Stories, Reels y Feed.
  * **Limitaciones severas con `text/plain`:**
    * Instagram **no documenta un contrato público oficial** para pre-poblar texto arbitrario multicarácter en un DM vía `ACTION_SEND`.
    * Según la versión instalada de `com.instagram.android`, lanzar `Intent(Intent.ACTION_SEND)` con `type = "text/plain"` y `setPackage("com.instagram.android")` puede lanzar `ActivityNotFoundException` (si no registra filtro genérico de texto plano), ignorar `EXTRA_TEXT` cuando no contiene una URL `http(s)://`, o lanzar `SecurityException` si se intenta invocar explícitamente actividades internas privadas/no exportadas como `com.instagram.direct.share.handler.DirectShareHandlerActivity` (`exported="false"`).
  * **Patrón obligatorio *Clipboard-First + Safe Intent Fallback* en `img2asciiIM`:**
    1. **Copiar siempre primero al `ClipboardManager`:** Antes de lanzar cualquier Intent hacia Instagram, copiar el arte generado mediante `ClipData.newPlainText("img2asciiIM", payload)`.
    2. **Intent implícito seguro con `try / catch`:** Intentar `Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, payload); setPackage("com.instagram.android") }` capturando tanto `ActivityNotFoundException` como `SecurityException` (**nunca** acoplarse sin protección a nombres de clase internos de Instagram).
    3. **Fallback limpio:** Si el `ACTION_SEND` falla o el usuario elige abrir Instagram directamente, invocar `packageManager.getLaunchIntentForPackage("com.instagram.android")` (o el Android Sharesheet `Intent.createChooser`) acompañado de un mensaje claro en la UI: *"Copiado al portapapeles. Abre el chat en Instagram Direct y pega el mensaje"*.
* **Visibilidad de paquetes en Android 11+ (API 30+):**
  * Para que `getLaunchIntentForPackage(...)` o `queryIntentActivities(...)` detecten las apps instaladas, `AndroidManifest.xml` debe incluir el bloque `<queries>` explícito:
    ```xml
    <queries>
        <package android:name="com.whatsapp" />
        <package android:name="com.whatsapp.w4b" />
        <package android:name="org.telegram.messenger" />
        <package android:name="org.telegram.messenger.web" />
        <package android:name="com.instagram.android" />
    </queries>
    ```

---

## 3. Referencia Técnica del Mapeo Unicode Braille (`U+2800`–`U+28FF`) para TDD

Cada celda Unicode Braille representa una submatriz de **2 columnas × 4 filas (8 puntos)**. Por razones históricas del estándar Unicode (los puntos 7 y 8 se añadieron después del Braille de 6 puntos), **el orden de bits NO es secuencial de arriba abajo en una sola pasada lineal**, lo cual constituye un fallo clásico de implementación si no se cubre con tests unitarios:

| Fila (y) | Columna Izquierda (`dx = 0`) | Máscara Hex / Bit | Columna Derecha (`dx = 1`) | Máscara Hex / Bit |
| :---: | :---: | :---: | :---: | :---: |
| **`dy = 0`** (arriba) | Punto 1 | `0x01` (`1 shl 0`) | Punto 4 | `0x08` (`1 shl 3`) |
| **`dy = 1`** | Punto 2 | `0x02` (`1 shl 1`) | Punto 5 | `0x10` (`1 shl 4`) |
| **`dy = 2`** | Punto 3 | `0x04` (`1 shl 2`) | Punto 6 | `0x20` (`1 shl 5`) |
| **`dy = 3`** (abajo) | **Punto 7** | **`0x40` (`1 shl 6`)** | **Punto 8** | **`0x80` (`1 shl 7`)** |

$$\text{codePoint} = \text{0x2800} + \sum \text{bitMask}(dx, dy)$$

* Celda vacía (0 puntos activos): `U+2800` (`⠀`) — *Braille Pattern Blank*.
* Celda llena (8 puntos activos): `U+28FF` (`⣿`) — `0x01 | 0x02 | 0x04 | 0x40 | 0x08 | 0x10 | 0x20 | 0x80 = 0xFF`.
* Relación de aspecto por subpunto: dado que la celda completa mide ~`1:2` y aloja `2×4` puntos, cada subpíxel Braille tiene relación **`~1:1` (cuadrada)**.

---

## 4. Tabla Comparativa de Presets Recomendados para TDD

Sea $W$ el ancho en columnas (caracteres por fila) y $H$ el alto en filas. El tamaño total del mensaje es:
$$\text{Longitud total (Char)} = H \times W + (H - 1) + C_{\text{envoltura}}$$

| Preset (`PresetId`) | Plataforma Destino (`packageName`) | Ancho rec. ($W$ cols) | Alto máx. ($H$ filas) | Presupuesto máx. (`Char`) | Envoltura (*Wrapper*) y overhead $C_{\text{envoltura}}$ | Rampa / Codificación recomendada (oscuro $\to$ claro o subpíxel) | Caracteres PROHIBIDOS en el cuerpo generado | Método primario de compartición en Android |
| :--- | :--- | :---: | :---: | :---: | :--- | :--- | :--- | :--- |
| **`WHATSAPP_ASCII`** | `com.whatsapp` / `com.whatsapp.w4b` | **32** *(rango seguro: 28–36)* | **56** *(~1.853 chars; tope duro app: 65.536)* | **<= 4.000** *(límite protocolo: 65.536)* | ```` ```<arte>``` ```` sin `\n` ni espacio inicial/final (**6 chars**) | ASCII seguro sin backtick ni espacio final:<br>`@%#*+=-:.` *(9 niveles)* o extendida sin `` ` `` ni `' '` en extremos | **`U+0060` (`` ` ``)** *(rompe bloque)*;<br>**`U+0020` (`' '`)** adyacente a ```` ``` ```` o al final de línea; `\r` | `ACTION_SEND` (`text/plain`) + `setPackage("com.whatsapp")` (fallback `w4b` / Sharesheet) |
| **`WHATSAPP_BRAILLE`** | `com.whatsapp` / `com.whatsapp.w4b` | **28** *(56 px horiz.)* | **40** *(160 px vert.; ~1.165 chars)* | **<= 4.000** *(límite protocolo: 65.536)* | ```` ```<arte>``` ```` (**6 chars**) *(o texto plano Braille)* | Matriz Braille 2×4 `U+2800`–`U+28FF` (blanco = `⠀` `U+2800`, lleno = `⣿` `U+28FF`) | **`U+0060` (`` ` ``)**; **`U+0020` (`' '`)**; `\r` | `ACTION_SEND` (`text/plain`) + `setPackage("com.whatsapp")` |
| **`TELEGRAM_BRAILLE`** *(Recomendado TG)* | `org.telegram.messenger` / `.web` | **30** *(60 px horiz.)* | **45** *(180 px vert.; ~1.394 chars)* | **<= 4.096** *(estricto)* | **Sin envoltura** (**0 chars**) *(evita ```` ``` ```` literales en `ACTION_SEND`)* | Matriz Braille 2×4 `U+2800`–`U+28FF` usando `⠀` (`U+2800`) como espacio vacío | **`U+0020` (`' '`)** *(se estrecha en fuente proporcional)*; `\r` | `ACTION_SEND` (`text/plain`) + `setPackage("org.telegram.messenger")` + botón Copiar |
| **`TELEGRAM_ASCII_CODE`** *(Para pegar/código)* | `org.telegram.messenger` | **32** *(rango: 28–34)* | **50** *(~1.657 chars)* | **<= 4.096** *(estricto)* | ```` ```\n<arte>\n``` ```` (**8 chars**; `\n` inicial obligatorio para no perder fila 1 como tag de idioma) | ASCII seguro sin backtick:<br>`@%#*+=-:.` | **`U+0060` (`` ` ``)**; ausencia de `\n` tras ```` ``` ```` inicial; `\r` | **Copiar al Portapapeles (`ClipboardManager`)** prioritario *(para que el compositor parsee Markdown)* + `ACTION_SEND` secundario |
| **`INSTAGRAM_BRAILLE`** *(Estándar DM)* | `com.instagram.android` | **24** *(48 px horiz.)* | **28** *(112 px vert.; ~699 chars)* | **<= 1.000** *(estricto)* | **Sin envoltura** (**0 chars**) *(IG no soporta ```` ``` ````)* | Matriz Braille 2×4 `U+2800`–`U+28FF` (blanco = `⠀` `U+2800` o `⠁` `U+2801` anti-colapso) | **`U+0060` (`` ` ``)** *(se ve crudo)*;<br>**`U+0020` (`' '`)** *(colapsa y deforma)*; `\r` | **Clipboard-First (`ClipboardManager`)** + `try/catch` de `ACTION_SEND` / apertura de `com.instagram.android` |
| **`INSTAGRAM_BLOCKS`** *(Alto contraste / Safe-Bytes)* | `com.instagram.android` | **20** | **14** *(293 chars = 866 bytes UTF-8)* | **<= 320 chars** *(<= 1.000 bytes UTF-8 y <= 1.000 chars)* | **Sin envoltura** (**0 chars**) | Sombreo de bloques densos:<br>`█▓▒░` (`U+2588`, `U+2593`, `U+2592`, `U+2591`) o `█▓▒⠀` | **`U+0020` (`' '`)**; **`U+0060` (`` ` ``)**; `\r` | **Clipboard-First (`ClipboardManager`)** + lanzamiento seguro de Instagram |

---

## 5. Checklist de Invariantes Obligatorios para la Suite de Tests (TDD)

1. **Invariante Anti-Backtick (`NoBacktickCollisionTest`):**
   * Ninguna rampa predefinida ni personalizada puede emitir `U+0060` (`` ` ``) dentro del contenido de la imagen. Si una rampa externa contiene `` ` ``, el motor debe rechazarla o sustituirlo por `'` (`U+0027`) antes de envolver el bloque.
   * En modo `WHATSAPP_ASCII` / `WHATSAPP_BRAILLE`, el texto final debe empezar exactamente por ` ``` ` seguido de un carácter distinto de espacio/salto de línea, terminar por ` ``` ` precedido de un carácter distinto de espacio/salto de línea, y contener exactamente **2** apariciones del token ` ``` `.
2. **Invariante de Presupuesto Exacto por Plataforma (`PlatformCharacterBudgetTest`):**
   * Para cualquier imagen de entrada (horizontal extrema `20:1`, vertical extrema `1:20`, cuadrada `1:1` o de `1×1` píxel):
     * `WHATSAPP_*`: `result.length <= 65_536` (y `<= preset.maxBudget`, p. ej. `4_000`).
     * `TELEGRAM_*`: `result.length <= 4_096`.
     * `INSTAGRAM_BRAILLE`: `result.length <= 1_000`.
     * `INSTAGRAM_BLOCKS` (*Safe-Bytes*): `result.length <= 1_000` y `result.encodeToByteArray().size <= 1_000`.
   * Cuando la relación de aspecto de una imagen muy vertical haría que $H \times (W + 1) - 1 + C_{\text{envoltura}}$ supere el presupuesto máximo de la plataforma, el motor **debe reducir automáticamente $H$ y $W$ proporcionalmente** en vez de truncar líneas del final del string.
3. **Invariante de Ausencia de Espacios Colapsables en Fuentes Proporcionales (`ProportionalFontSafetyTest`):**
   * En los presets `INSTAGRAM_*` y `TELEGRAM_BRAILLE`, `result.contains(' ')` (`U+0020`) debe ser **`false`**, garantizando que los fondos claros emplean `U+2800` (`⠀`), `U+2801` (`⠁`) o `U+2591` (`░`).
4. **Invariante de Codificación Braille 2×4 (`BrailleBitmaskMappingTest`):**
   * Verificar individualmente los 8 subpíxeles de una celda `2×4`, comprobando específicamente que `(dx=0, dy=3)` activa el bit **`0x40`** (`U+2840` `⡀`) y `(dx=1, dy=0)` activa el bit **`0x08`** (`U+2808` `⠈`), evitando el error clásico de intercambiar el punto 4 con el punto 7.
5. **Invariante de Resiliencia *Offline* y Compartición Segura (`OfflineAndShareSafetyTest`):**
   * Verificar que `AndroidManifest.xml` **no solicita `android.permission.INTERNET`** e incluye `<queries>` para los paquetes de WhatsApp, Telegram e Instagram.
   * Verificar que el flujo de lectura de URIs gestiona `IOException` / timeouts sin bloquear el hilo principal (simulando un archivo de Google Drive *cloud-only* en mitad de un vuelo) y que el flujo de compartición hacia Instagram copia siempre al portapapeles y captura `ActivityNotFoundException` y `SecurityException`.
