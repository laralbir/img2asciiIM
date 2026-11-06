# Guía de Usuario de `img2asciiIM`

![img2asciiIM Banner](assets/banner.png)

> **Comparte cualquier foto, captura o mapa como un mensaje de texto nítido por WhatsApp, Telegram o Instagram Direct cuando tu conexión bloquea las imágenes —100 % sin internet desde tu propio móvil.**

---

## Índice Rápido

1. [¿Qué es `img2asciiIM` y cuándo te salva el viaje?](#1-qué-es-img2asciiim-y-cuándo-te-salva-el-viaje)
2. [Consejo de Oro antes de Despegar (Archivos en la Nube y Google Drive)](#2-consejo-de-oro-antes-de-despegar-archivos-en-la-nube-y-google-drive)
3. [Paso 1: Cómo Elegir o Capturar tu Imagen (4 Formas)](#3-paso-1-cómo-elegir-o-capturar-tu-imagen-4-formas)
4. [Paso 2: Elegir la App de Destino (WhatsApp, Telegram o Instagram)](#4-paso-2-elegir-la-app-de-destino-whatsapp-telegram-o-instagram)
5. [Paso 3: Ajustar el Estilo y la Nitidez (Recetas Rápidas)](#5-paso-3-ajustar-el-estilo-y-la-nitidez-recetas-rápidas)
6. [Paso 4: Elegir Qué Enviar (Arte de Texto y/o Enlace a Foto HD)](#6-paso-4-elegir-qué-enviar-arte-de-texto-yo-enlace-a-foto-hd)
7. [Paso 5: Enviar en 1 Toque o Copiar al Portapapeles](#7-paso-5-enviar-en-1-toque-o-copiar-al-portapapeles)
8. [Cómo Funciona el Enlace HD Autónomo y el Visor Web (`#data=...`)](#8-cómo-funciona-el-enlace-hd-autónomo-y-el-visor-web-data)
9. [Comprobación y Actualizaciones In-App desde GitHub Releases](#9-comprobación-y-actualizaciones-in-app-desde-github-releases)
10. [Preguntas Frecuentes (FAQ) y Solución de Problemas Rápidos](#10-preguntas-frecuentes-faq-y-solución-de-problemas-rápidos)

---

## 1. ¿Qué es `img2asciiIM` y cuándo te salva el viaje?

¿Alguna vez te has conectado al **Wi-Fi gratuito de mensajería de un avión** (*Free Messaging*), al bono básico de un **crucero** o a una red móvil saturada en una **zona rural, estadio o montaña** y has intentado mandar una foto?

Seguramente viste cómo tus mensajes escritos llegaban al segundo con doble *check*, mientras que cualquier fotografía, captura de pantalla, audio o *sticker* se quedaba congelado indefinidamente con el **icono del reloj (*«Enviando…»*)**.

### ¿Por qué el texto llega al instante pero las fotos se quedan bloqueadas?

Las aplicaciones como **WhatsApp**, **Telegram** e **Instagram Direct** utilizan dos caminos distintos por dentro:
- **Un canal ultraligero de señalización** por el que viajan los mensajes de **texto puro** (ocupa apenas unos pocos kilobytes).
- **Servidores multimedia separados** para subir y descargar fotos, vídeos, notas de voz y *stickers*.

En vuelos comerciales (**Iberia**, **Vueling**, **Lufthansa**, **Delta Air Lines**, **United Airlines**, **Air Europa**, entre otras) y barcos de crucero, el router satelital autoriza el canal ligero de texto para que puedas chatear gratis, pero **bloquea los servidores multimedia y el resto de páginas web** para evitar que el enlace del avión se colapse:

```mermaid
flowchart LR
    Foto["Tu foto o captura en el móvil"] -->|"Envío normal de imagen"| Bloqueo["Firewall del Wi-Fi a bordo (Bloqueado: icono del reloj)"]
    Foto -->|"Conversión local en img2asciiIM"| Arte["Mosaico compacto de texto (Braille / ASCII / Bloques)"]
    Arte -->|"Viaja como mensaje de texto puro"| Destino["Llega al instante a WhatsApp, Telegram o Instagram"]
```

**`img2asciiIM` resuelve este problema en segundos:** convierte cualquier fotografía, mapa del punto de encuentro, cartel de puerta de embarque, plaza de aparcamiento o vista desde la ventanilla en un **dibujo compacto hecho con caracteres de texto** (puntos Braille de alta precisión, arte ASCII clásico o bloques sombreados). Como para la red es un simple mensaje escrito, **atraviesa el Wi-Fi gratuito del avión al instante** y tu contacto reconoce la imagen directamente dentro de su burbuja de chat.

| Situación real | ¿Qué permite la red? | Cómo te ayuda `img2asciiIM` |
| :--- | :--- | :--- |
| **Wi-Fi gratuito en vuelos** (*Free Messaging* en Iberia, Vueling, Lufthansa, Delta, United, Air Europa…) | Solo mensajes de texto en apps de chat; fotos, audios y webs bloqueados. | Convierte tu foto sin internet y la envía como texto plano que llega en el acto. |
| **Cruceros y ferris** (bono básico de chat marítimo) | Texto básico por satélite; subida multimedia desactivada o carísima. | Comparte un selfie, horario de excursión o plano del barco sin pagar el pase multimedia completo. |
| **Montaña, festivales o cobertura rural mínima (`E` / `2G` / `3G` saturado)** | Los megabytes de una foto caducan por falta de señal, pero un texto corto sí sale. | Un mensaje de `img2asciiIM` ocupa **menos de 2 a 4 KB** (miles de veces menos que una foto de cámara). |

### Privacidad total: 100 % en tu móvil y cero permisos invasivos

> [!IMPORTANT]
> **Tus fotos jamás salen de tu teléfono para convertirse.**
> - **Sin conexión a internet (`100 % Offline`):** `img2asciiIM` ni siquiera solicita permiso de acceso a Internet en Android. Toda la conversión ocurre en milisegundos dentro del procesador de tu móvil, incluso con el **Modo Avión** activado.
> - **Sin acceso global a tu galería:** La aplicación nunca pide permiso para leer todas tus fotos o archivos privados. Utiliza los selectores seguros del sistema Android, por lo que únicamente recibe la imagen puntual que tú elijas en cada momento.

---

## 2. Consejo de Oro antes de Despegar (Archivos en la Nube y Google Drive)

`img2asciiIM` se lleva de maravilla con **Google Drive**, Microsoft OneDrive, Dropbox y las fototecas en la nube, pero hay un detalle fundamental que conviene conocer **antes de subir al avión**:

Cuando abres Google Drive o tu galería en la nube sin conexión, muchas veces **ves la miniatura pequeña** de tus archivos porque el móvil guarda esa vista previa, pero **el archivo real en alta calidad sigue guardado únicamente en internet**. Si ya estás a `10.000` metros de altura con el Wi-Fi gratuito del avión, el router bloqueará la descarga desde Google Drive y el archivo no podrá abrirse.

### Qué hacer en el aeropuerto antes de embarcar (30 segundos)

Si llevas en **Google Drive** (o en OneDrive / Dropbox) un plano del hotel, una captura de reserva, un mapa o cualquier imagen que quieras poder enviar durante el vuelo:

1. Abre la app **Google Drive** mientras aún tengas datos móviles o Wi-Fi en tierra.
2. Busca la imagen que te interese y pulsa el icono de tres puntos **`⋮`** a su derecha.
3. Activa la opción **«Disponible sin conexión»** (*Available offline*) —o pulsa **Descargar** para guardarla en tu carpeta local `Descargas`—.

| Origen de la imagen | En tierra (con datos o Wi-Fi normal) | En pleno vuelo (Modo Avión o Wi-Fi *Free Messaging*) |
| :--- | :---: | :--- |
| **Cámara del móvil** | Funciona siempre | **Funciona siempre al 100 %** |
| **Galería local y carpeta `Descargas`** | Funciona siempre | **Funciona siempre al 100 %** |
| **Tarjeta SD o pendrive USB-C** | Funciona siempre | **Funciona siempre al 100 %** |
| **Google Drive / OneDrive / Dropbox** *(marcado «Disponible sin conexión»)* | Funciona siempre | **Funciona siempre** *(se lee de la memoria local)* |
| **Google Drive / Fotos solo en la nube** *(sin descargar antes)* | Funciona (se descarga al momento) | **No disponible** *(el Wi-Fi del avión bloquea la nube)* |

> [!TIP]
> **¿Prefieres preparar tus mensajes con total calma?** Puedes hacer una foto por la ventanilla en **Modo Avión estricto**, ajustar el resultado en `img2asciiIM`, pulsar **Copiar** y conectarte al Wi-Fi del avión solo el instante necesario para pegar y enviar el texto.

---

## 3. Paso 1: Cómo Elegir o Capturar tu Imagen (4 Formas)

El flujo completo en `img2asciiIM` está pensado para que pases de una foto a un mensaje listo en pocos segundos:

```mermaid
flowchart TD
    Paso1["Paso 1: Elige o haz tu foto (Galería, Archivos/Drive, Cámara o Compartir)"] --> Paso2["Paso 2: Elige la app de destino (WhatsApp, Telegram o Instagram)"]
    Paso2 --> Paso3["Paso 3: Ajusta estilo, tema Claro/Oscuro y nitidez en vivo"]
    Paso3 --> Paso4["Paso 4: Elige qué enviar (Arte ASCII/Braille y/o Enlace a foto HD)"]
    Paso4 --> Paso5["Paso 5: Pulsa Enviar o Copiar en 1 toque"]
```

Tienes **cuatro maneras** igual de cómodas de cargar una imagen (y si abres cualquiera de ellas por error y pulsas *Atrás*, la imagen y los ajustes que ya tuvieras en pantalla se conservan intactos):

### 1. Botón «Galería»
Abre el selector moderno de fotos de Android para elegir cualquier imagen (`JPEG`, `PNG`, `WebP` o `HEIC`) de tu teléfono o de tu fototeca integrada. No importa si tu cámara hace fotos gigantescas de **48 MP o 108 MP** ni si hiciste la foto con el móvil girado: `img2asciiIM` reduce el tamaño de forma inteligente sin agotar la batería ni la memoria y **endereza automáticamente la orientación**.

### 2. Botón «Archivos / Drive»
Abre el explorador universal de documentos de Android. Es la opción ideal cuando la imagen no está en tu carrete de fotos habitual, sino en:
- Tu unidad de **Google Drive**, OneDrive, Dropbox o Nextcloud.
- La carpeta **`Descargas`** (por ejemplo, un plano descargado del correo o de la web de la aerolínea).
- Una **tarjeta microSD** o un **pendrive / lector de tarjetas USB-C** conectado al teléfono.

### 3. Botón «Cámara»
Dispara una foto al momento sin salir de `img2asciiIM`. Es perfecta durante el viaje para capturar:
- Las vistas de las alas, las nubes o el amanecer desde la **ventanilla del avión**.
- El panel luminoso con tu **puerta de embarque**, cinta de equipaje o vía de tren.
- El número de columna y planta donde dejaste el coche en el **parking del aeropuerto**.
- Un **mapa impreso** o cartel informativo.

*(Las capturas temporales de cámara se guardan únicamente en la caché privada de la app y se limpian automáticamente para no llenar el espacio de tu teléfono).*

### 4. Desde otra app con el menú «Compartir» (la vía más rápida)
Si ya tienes una imagen abierta en tu app de **Galería**, en **Google Drive** o en tu gestor de archivos:
1. Toca el botón estándar **Compartir** de Android.
2. Elige **`img2asciiIM`** en la lista de aplicaciones.
3. La aplicación se abrirá directamente con esa foto **ya convertida** en la vista previa, ahorrándote buscarla de nuevo.

---

## 4. Paso 2: Elegir la App de Destino (WhatsApp, Telegram o Instagram)

Cada aplicación de mensajería tiene sus propias reglas sobre **el tipo de letra** y **el tamaño máximo permitido por mensaje**. Si envías el mismo bloque de texto sin adaptarlo, en unas apps se verá torcido y en otras se cortará por la mitad.

Al tocar arriba **WhatsApp**, **Telegram** o **Instagram Direct**, `img2asciiIM` configura automáticamente todas las medidas internas por ti:

| App de Destino | Qué hace `img2asciiIM` automáticamente | Modo Inicial | Límite Seguro |
| :--- | :--- | :--- | :--- |
| **WhatsApp** *(y WhatsApp Business)* | Envuelve el dibujo en triple tilde ```` ```...``` ```` para activar la **letra monoespaciada** (ancho fijo), elimina símbolos que romperían el formato y ajusta el ancho a **`~32` columnas** para pantallas verticales. | **ASCII** *(admite Braille y Bloques)* | Hasta `65.536` caracteres *(ajuste ágil `≤ 4.000`)* |
| **Telegram** | Ajusta automáticamente el tamaño para no superar jamás los **`4.096` caracteres** (evitando que Telegram parta tu imagen en dos mensajes distintos) y usa **Braille directo** (`~30` columnas) limpio y de alta resolución. | **Braille** *(admite ASCII y Bloques)* | **`4.096` caracteres** exactos |
| **Instagram Direct** | Desactiva los símbolos ```` ``` ```` (Instagram no los soporta), activa **Braille Unicode** con espacios especiales indestructibles (`⠀`) que mantienen la forma perfecta en la fuente normal de Instagram, y compacta el tamaño (`~22–24` columnas) por debajo de **`1.000` caracteres**. | **Braille** *(o Bloques)* | **`1.000` caracteres** estrictos |

### Detalles clave de cada plataforma

- **En WhatsApp:** WhatsApp exige el bloque ```` ``` ```` para alinear las columnas, pero tiene una trampa oculta: si el dibujo contiene una tilde invertida `` ` `` o empieza con espacios normales en blanco, WhatsApp desactiva el formato monoespaciado y desarma toda la imagen. `img2asciiIM` filtra y sustituye automáticamente esos caracteres para que el bloque **nunca se rompa**.
- **En Telegram:** Si intentas mandar una captura de pantalla muy alargada que supere `4.096` caracteres, Telegram normalmente corta el texto en dos burbujas separadas. `img2asciiIM` **recalcula las proporciones de forma automática** para que tu imagen entre siempre entera en **un único mensaje**. Además, si cambias manualmente a modo ASCII clásico, añade un salto de línea protector tras ```` ``` ```` para que Telegram no borre la primera fila confundiéndola con el nombre de un lenguaje de programación.
- **En Instagram Direct:** Instagram Direct no tiene letra monoespaciada (una `W` es mucho más ancha que un punto `.`) y además encoge los espacios normales. Por eso `img2asciiIM` utiliza el modo especial **Braille Unicode (`⠀..⣿`)**: Android dibuja todas las celdas Braille con el mismo ancho —incluida la celda vacía especial `⠀`— logrando que la imagen mantenga su geometría exacta dentro del estricto tope de `1.000` caracteres.

> [!WARNING]
> **Evita usar «ASCII clásico» en Instagram Direct.** Si seleccionas manualmente el modo ASCII teniendo elegido el perfil de Instagram, la app te mostrará un aviso porque la letra proporcional de Instagram desalineará las columnas. Para Instagram Direct, usa siempre **Braille** (máximo detalle) o **Bloques** (iconos sencillos de alto contraste).

---

## 5. Paso 3: Ajustar el Estilo y la Nitidez (Recetas Rápidas)

Justo encima de los controles verás la **Vista Previa de Burbuja**, que te muestra en tiempo real cómo quedará el mensaje junto con el contador de **`caracteres actuales / límite máximo`** y el tamaño en **`columnas × filas`**.

### A. Elige entre los 3 Estilos de Dibujo

- **Braille (`⠀..⣿`) — Máxima definición fotográfica:**
  Cada carácter es una rejilla de **8 micropuntos (`2 × 4`)**, lo que multiplica por ocho los detalles que caben en cada letra. Es la mejor opción para **rostros, selfies, paisajes desde la ventanilla, planos con calles y capturas detalladas**, y funciona de maravilla en las tres aplicaciones.
- **ASCII (`@#%...`) — El estilo retro clásico:**
  Construye la imagen combinando letras y signos clásicos (`@%#*+=-:.`). Es ideal en **WhatsApp** (o al copiar y pegar código en **Telegram**) cuando quieres ese aspecto informático auténtico de máquina de escribir.
- **Bloques (`█▓▒░`) — Trazos sólidos y alto impacto:**
  Rellena las zonas con bloques de distinta intensidad. Funciona genial para **logotipos, iconos grandes, señales de dirección, dibujos sencillos o siluetas muy contrastadas**.

### B. El truco maestro: Tema Claro vs. Tema Oscuro (Botón «Invertir»)

A diferencia de una foto normal, un mensaje de texto se pinta con el color de letra del teléfono de quien lo lee:
- En un móvil en **Modo Claro**, la burbuja de chat es clara (blanca o verde pálido) y las letras son **oscuras** (cuantos más puntos pones, más **oscura** se ve esa zona).
- En un móvil en **Modo Oscuro**, la burbuja es casi negra y las letras son **blancas** (cuantos más puntos pones, más **brillante** se ve esa zona).

Por eso, si envías un retrato pensado para fondo claro a alguien que usa Modo Oscuro, lo verá como el negativo de una foto antigua (pelo blanco y cara oscura).

1. Usa el **conmutador de vista previa (Burbuja Clara / Burbuja Oscura)** para simular la pantalla de tu amigo.
2. Activa o desactiva el botón **Invertir** según cómo use el móvil tu destinatario:
   - **Tu contacto usa Modo Claro:** deja **Invertir desactivado**.
   - **Tu contacto usa Modo Oscuro:** activa **Invertir**.

*(Si abres un logotipo o sticker `PNG` / `WebP` con fondo transparente, `img2asciiIM` adapta automáticamente el fondo transparente al modo elegido para que no aparezca un recuadro negro indeseado).*

### C. Brillo, Contraste, Ancho y Dithering (Tramado)

- **Ancho (columnas):** Controla cuántas letras ocupa cada fila a lo ancho (`12` a `72` columnas). El valor inicial ya viene calibrado para cada app (`32` en WhatsApp, `30` en Telegram, `22–24` en Instagram). Si tu contacto tiene un móvil pequeño o usa un tamaño de letra grande en Android, bájalo un poco para evitar que las líneas salten de renglón.
- **Brillo y Contraste:** Las fotos tomadas a través del cristal doble de la ventanilla del avión suelen salir algo blanquecinas o con neblina. Subir ligeramente el **Contraste** hace que las nubes, el ala o el horizonte destaquen al instante.
- **Dithering (Tramado de sombras):** Decide cómo se traducen los tonos grises suaves a puntos:
  - **Floyd-Steinberg:** Reparte los matices de forma suave y orgánica. Es el mejor para **caras, retratos, cielos y fotografías naturales**.
  - **Atkinson:** Marca mucho más los bordes y deja los fondos limpios sin ruido. Es espectacular para **mapas, carteles de aeropuerto, horarios, siluetas y retratos pequeños**.
  - **Sin tramado (`Ninguno`):** Aplica un corte directo blanco/negro. Úsalo para **logotipos limpios, dibujos en blanco y negro o iconos planos**.

### Tabla de Recetas Rápidas

| Qué quieres enviar | Estilo recomendado | Dithering (Tramado) | Consejo extra |
| :--- | :--- | :--- | :--- |
| **Selfie o foto de personas** | **Braille (`⠀..⣿`)** | **Floyd-Steinberg** | Acerca o encuadra bien las caras y ajusta **Invertir** según el tema (claro/oscuro) de tu contacto. |
| **Vistas desde la ventanilla (alas, nubes, puesta de sol)** | **Braille (`⠀..⣿`)** | **Floyd-Steinberg** | Sube un poco el **Contraste** para compensar el reflejo del cristal del avión. |
| **Mapa de punto de encuentro, cartel o puerta de embarque** | **Braille (`⠀..⣿`)** | **Atkinson** | El tramado *Atkinson* perfila letras grandes, flechas y calles con mucha mayor claridad. |
| **Estilo retro en un grupo de WhatsApp** | **ASCII (`@#%...`)** | **Floyd-Steinberg** o **Atkinson** | Mantén el ancho entre `28` y `32` columnas para que todos en el grupo lo vean sin cortes. |
| **Logo, bandera, señal o silueta fuerte** | **Bloques (`█▓▒░`)** | **Sin tramado** *(o Atkinson)* | Contraste alto para conseguir bloques limpios y reconocibles al primer golpe de vista. |

---

## 6. Paso 4: Elegir Qué Enviar (Arte de Texto y/o Enlace a Foto HD)

Justo debajo de los controles de nitidez encontrarás la sección **Componentes del mensaje**, que te permite decidir exactamente qué quieres incluir en el chat mediante dos casillas independientes:

- **`[x] Incluir arte de texto (ASCII / Braille)`**
- **`[x] Adjuntar enlace a foto HD (#data=...)`**

```mermaid
flowchart TD
    Eleccion{"¿Qué componentes activas en Paso 4?"}
    Eleccion -->|"Solo Arte de Texto"| Modo1["Modo Clásico (Ultraligero)\nMensaje de texto puro (2-4 KB)\nMáxima privacidad y compatibilidad"]
    Eleccion -->|"Arte + Enlace HD"| Modo2["Modo Híbrido Dual\nDibujo en la burbuja + Enlace debajo\nMiniatura compacta (~320 px)"]
    Eleccion -->|"Solo Enlace HD"| Modo3["Modo Enlace HD Puro (Máxima Nitidez)\nTodo el presupuesto para la foto\nWhatsApp: 640-800 px (calidad 75)\nTelegram: 200-240 px (calidad 55)"]
```

### Los 3 Modos de Envío según tu Necesidad

#### 1. Modo Clásico: Solo Arte de Texto *(Opción por Defecto)*
- **Ajuste:** `[x] Incluir arte de texto` activado · `[ ] Adjuntar enlace a foto HD` desactivado.
- **Resultado:** Tu mensaje se compone únicamente del dibujo en puntos Braille, caracteres ASCII o bloques.
- **Cuándo usarlo:** Cuando quieras enviar un mensaje ultraligero (apenas `2` a `4 KB`), sin enlaces web de ningún tipo, perfecto para el canal de texto más básico del avión o cuando tu contacto solo necesita ver el dibujo en su pantalla al instante.

#### 2. Modo Híbrido: Arte de Texto + Enlace a Foto HD
- **Ajuste:** `[x] Incluir arte de texto` activado · `[x] Adjuntar enlace a foto HD` activado.
- **Resultado:** El mensaje muestra el dibujo en texto en la burbuja del chat y, tras un doble salto de renglón, añade el enlace oficial:
  ```text
  <dibujo en braille o ascii>

  🔗 Ver foto en alta definición:
  https://laralbir.github.io/img2asciiIM/#data=...
  ```
- **Cuándo usarlo:** Quieres el impacto visual inmediato del dibujo en el chat, pero además ofreces a tu contacto la posibilidad de abrir la foto original a color si pulsa el enlace.
- **Detalle técnico:** En este modo, la imagen del enlace se comprime en tamaño miniatura (**hasta 320 px a calidad 60**) para que la suma del arte de texto y del enlace Base64 URL-safe quepa con total holgura dentro del límite de la plataforma.

#### 3. Modo Enlace HD Puro: Máxima Nitidez Fotográfica *(Sin Arte de Texto)*
- **Ajuste:** `[ ] Incluir arte de texto` desactivado · `[x] Adjuntar enlace a foto HD` activado.
- **El secreto:** Al apagar el dibujo de texto, **todo el presupuesto de caracteres de la app de mensajería se dedica íntegramente a la fotografía incrustada en el enlace**.
- **Resoluciones fotográficas alcanzadas:**
  - **En WhatsApp:** WhatsApp admite mensajes de hasta **`65.536` caracteres**. Al no restar espacio con caracteres de arte, `img2asciiIM` escala y comprime la foto a resoluciones de **`640` a `800` píxeles a calidad 75** (generando un payload Base64 de entre `35` y `50 KB`, que viaja holgadamente dentro del límite permitido).
  - **En Telegram:** Telegram limita cada mensaje a **`4.096` caracteres**. Sin arte de texto, la app escala la foto a **`200` a `240` píxeles a calidad 50–60** (~`2.800–3.800` caracteres Base64), asegurando que el mensaje completo entre en una **única burbuja sin cortarse**.
- **El resultado en el chat:** Tu mensaje queda completamente limpio, profesional y directo:
  ```text
  📸 Ver foto en alta definición:
  https://laralbir.github.io/img2asciiIM/#data=...
  ```

> [!TIP]
> **Salvaguarda de mensaje no vacío:** La aplicación nunca te dejará desmarcar las dos casillas a la vez. Si intentas apagar la última que queda encendida, el sistema mantendrá activa una de ellas para garantizar que jamás mandes un mensaje en blanco por error.

> [!NOTE]
> **¿Por qué en Instagram Direct el enlace HD aparece desactivado?**
> Instagram Direct impone un límite estricto de **`1.000` caracteres / 1.000 bytes** por mensaje de texto. En un espacio tan reducido es matemáticamente imposible incrustar una fotografía en Base64 sin recortarla. Por eso, en el perfil de Instagram la app desactiva automáticamente el conmutador de enlace HD y prioriza el arte en Braille de alta precisión, que luce espectacular y respeta los `1.000` caracteres al milímetro.

---

## 7. Paso 5: Enviar en 1 Toque o Copiar al Portapapeles

Cuando la vista previa de burbuja y los componentes seleccionados se vean a tu gusto, tienes dos acciones directas en la parte inferior:

### Opción A: Botón «Enviar a WhatsApp / Telegram / Instagram»
- Lanza directamente la aplicación de mensajería seleccionada con tu mensaje preparado para que solo tengas que **tocar el chat de tu contacto y pulsar Enviar**.
- Si utilizas una variante oficial (como **WhatsApp Business** o **Telegram X**) o en ese teléfono no tienes instalada esa aplicación concreta, `img2asciiIM` abrirá automáticamente el menú estándar **Compartir** de Android para que puedas enviarlo por cualquier otra vía sin errores.

> [!NOTE]
> **¿Cómo funciona el envío rápido a Instagram Direct (*Copiado Automático*)?**
> Por limitaciones propias de la app de Instagram en Android, algunas versiones abren la lista de chats pero ignoran los textos externos cuando no contienen un enlace web tradicional.
> Para que nunca pierdas un segundo, al pulsar **«Enviar a Instagram»**, `img2asciiIM` **guarda siempre primero tu arte en el portapapeles de forma automática** antes de abrir Instagram:
> 1. Si Instagram rellena el mensaje solo, simplemente elige a tu contacto y envíalo.
> 2. Si Instagram abre tus mensajes directos en blanco, entra en el chat de la persona, **mantén pulsado el cuadro *«Enviar mensaje…»*** y toca **Pegar**. ¡Tu imagen en Braille aparecerá entera y lista para enviar!

### Opción B: Botón «Copiar»
- Copia el resultado exacto al portapapeles de Android en **1 solo toque** (notarás una suave vibración de confirmación y el aviso en pantalla).
- Es comodísimo cuando:
  - Ya tienes la conversación abierta en segundo plano y prefieres cambiar de app y pegar manualmente.
  - Quieres pegar el dibujo o el enlace en **otra app compatible con el Wi-Fi del avión** (como Facebook Messenger, Google Messages, Signal, Slack o notas personales).
  - En **Telegram** has elegido expresamente el modo **ASCII clásico** con bloque ```` ``` ```` y quieres pegarlo directamente en la caja de escritura del chat para que Telegram aplique el formato de código al instante.

---

## 8. Cómo Funciona el Enlace HD Autónomo y el Visor Web (`#data=...`)

Tal vez te preguntes: *¿Cómo es posible enviar una foto dentro de un enlace web si el Wi-Fi del avión tiene bloqueada la subida de fotos y no me deja navegar por páginas web?*

### El Secreto: La Imagen Viaja DENTRO de las Letras del Enlace

En un sistema convencional de internet, para mandar una foto tienes que subirla a un servidor (Google Drive, Imgur, servidores de WhatsApp) y mandar un enlace a ese servidor (`https://servidor.com/mi-foto.jpg`). En el avión, ese servidor está bloqueado, por lo que la foto nunca llega.

En cambio, `img2asciiIM` genera un **enlace 100 % autónomo y autosuficiente**:

```text
https://laralbir.github.io/img2asciiIM/#data=UklGRvAAAABXRUJQVlA4...
```

1. Toda la imagen fotográfica (comprimida en JPEG/WebP) se traduce a una secuencia de letras y números llamada **Base64 URL-safe**.
2. Esa secuencia se coloca detrás del símbolo de almohadilla **`#data=`** (*hash fragment*).
3. Para el router del avión y para la aplicación de chat, ese enlace es simplemente **un mensaje de texto plano ordinario**, por lo que **atraviesa el cortafuegos satelital sin ningún bloqueo**.
4. La parte detrás del `#` jamás se transmite a ningún servidor de internet ni a terceros: se procesa íntegramente en el dispositivo de quien abre el enlace.

### Qué Ocurre Cuando tu Amigo Abre el Enlace

El enlace funciona para cualquier persona y en cualquier dispositivo:

```mermaid
flowchart TD
    Enlace["Tu amigo pulsa el enlace https://laralbir.github.io/img2asciiIM/#data=..."]
    Enlace --> Dispositivo{"¿Tiene instalada img2asciiIM en su Android?"}
    Dispositivo -- "SÍ" --> Nativo["Apertura directa en img2asciiIM (Deep Link)\nReconstrucción en pantalla en milisegundos\n100% OFFLINE (Cero gasto de datos)"]
    Dispositivo -- "NO (o usa iPhone / PC)" --> Web["Abre en navegador web (Chrome, Safari, Firefox)\nVisor estático HTML5 / Canvas en docs/index.html\nDecodifica el hash en pantalla sin servidor externo"]
```

- **Si tu contacto tiene `img2asciiIM` en Android:**
  Android detecta el enlace automáticamente (`Intent.ACTION_VIEW`), abre la app de inmediato, extrae los datos y **dibuja la fotografía original en alta definición a todo color**, sin necesidad de internet y sin gastar ni un solo megabyte de datos.
- **Si tu contacto no tiene la app instalada (o usa iPhone, iPad o PC):**
  El enlace se abre en su navegador web preferido (Safari, Chrome, Edge, Firefox). Una página web estática ultraligera alojada en GitHub Pages lee el fragmento `#data=...` con JavaScript directamente en el navegador del usuario y **proyecta la fotografía en pantalla** en menos de una décima de segundo, ofreciendo además un enlace para descargar la app en Android si lo desea.

---

## 9. Comprobación y Actualizaciones In-App desde GitHub Releases

`img2asciiIM` es un proyecto de software libre y privado que no depende de los servicios privativos de **Google Play Store**. Para que siempre disfrutes de las últimas mejoras y correcciones sin complicaciones, la aplicación incluye su propio **sistema nativo de comprobación y actualización in-app**:

### Cómo Comprobar si Hay una Versión Nueva

1. Toca el menú de tres puntos o el botón **«Comprobar actualizaciones»** en la barra superior de `img2asciiIM`.
2. La app consultará de forma segura el repositorio oficial en GitHub (`https://api.github.com/repos/laralbir/img2asciiIM/releases/latest`).
3. Si estás usando la última versión, verás un mensaje confirmando que tu versión (`v1.0.0`) está al día.
4. Si hay una versión más moderna disponible (por ejemplo, `v1.1.0`), aparecerá una tarjeta con:
   - El número de versión nueva.
   - Las **notas de la versión** explicando qué mejoras incluye.
   - Un botón **«Descargar APK»** que descarga directamente el instalador oficial firmado.

### Resiliencia Total en Modo Avión y Vuelos

> [!IMPORTANT]
> **Cero fallos ni congelaciones en pleno vuelo:**
> Si pulsas comprobar actualizaciones mientras estás en **Modo Avión**, en el desierto o conectado al Wi-Fi del avión (que suele bloquear el acceso a las APIs de GitHub), la aplicación aplica un tiempo límite ultracorto (**4 segundos**) y cancela la petición silenciosamente con un aviso amigable (*«Sin conexión para comprobar actualizaciones»*). La app jamás se bloquea, no muestra errores técnicos ni interrumpe la conversión de tus fotos.

### Seguridad y Verificación Criptográfica (SHA-256)

Cada actualización oficial de `img2asciiIM` se compila y firma automáticamente mediante un flujo verificado de **Integración Continua (CI/CD)** en GitHub Actions:
- Cada archivo APK publicado viene acompañado de su archivo de suma criptográfica **`SHA-256`** (`img2asciiIM-vX.Y.Z.apk.sha256`).
- Esto garantiza que el instalador que descargas es exactamente el mismo que se compiló a partir del código fuente público, sin modificaciones de terceros ni malware.

---

## 10. Preguntas Frecuentes (FAQ) y Solución de Problemas Rápidos

### «¿El enlace de foto HD gasta datos de mi bono satelital o del Wi-Fi del avión?»
**Cero datos extra.** Como la foto va codificada en texto dentro del propio enlace, viaja por el canal de texto plano autorizado por la aerolínea. Al enviarlo no se conecta a servidores multimedia, y cuando el destinatario tiene la app instalada, la imagen se reconstruye en su memoria sin descargar nada de internet.

---

### «¿Por qué se ve mejor la foto cuando apago el conmutador de arte de texto?»
Al desactivar el arte de texto (ASCII / Braille), el mensaje no tiene que reservar caracteres para dibujar filas de puntos. Todo el cupo disponible (hasta **`65.536` caracteres en WhatsApp**) pasa a estar disponible para la foto del enlace, permitiendo a la app comprimir la imagen con una resolución mucho mayor (**`640` a `800` píxeles**) y una calidad fotográfica visiblemente superior.

---

### «Mi amigo me dice que ve las líneas partidas en dos renglones o la imagen "descuadrada"»
**Causa:** Cada fila de tu dibujo es un poquito más ancha que la burbuja de chat del móvil de tu amigo (ocurre cuando su teléfono tiene la pantalla estrecha o lleva activada la opción de **Tamaño de fuente grande** en los ajustes de accesibilidad de Android/iPhone).  
**Solución rápida:** Baja el deslizador de **Ancho** unas `4` o `6` columnas (por ejemplo, ponlo en **`24–26` columnas** para WhatsApp/Telegram o **`18–20` columnas** para Instagram Direct) y vuelve a enviarlo. Se verá perfectamente alineado en cualquier pantalla.

---

### «La imagen se reconoce, pero se ve rara, como en negativo (las caras oscuras y el fondo brillante)»
**Causa:** Estás enviando la imagen con el ajuste pensado para Modo Claro a alguien que tiene el móvil en **Modo Oscuro** (o viceversa).  
**Solución rápida:** Toca el botón **Invertir** en el Paso 3 y comprueba el resultado cambiando el interruptor **Claro / Oscuro** de la vista previa antes de reenviarlo.

---

### «Intento abrir un archivo de Google Drive en mitad del vuelo y dice que no está disponible sin conexión»
**Causa:** Aunque Google Drive te muestre la miniatura del archivo en la lista, la imagen completa nunca llegó a descargarse en la memoria interna de tu móvil antes del despegue, y el Wi-Fi gratuito del avión bloquea la conexión con los servidores de Google Drive.  
**Solución:**
- **Para el próximo vuelo:** Marca tus documentos importantes con **«Disponible sin conexión»** dentro de Google Drive antes de embarcar (ver [Consejo de Oro antes de Despegar](#2-consejo-de-oro-antes-de-despegar-archivos-en-la-nube-y-google-drive)).
- **Si ya estás volando:** Comprueba si habías descargado antes esa imagen en tu carpeta local **`Descargas`** o en tu **Galería**, o haz una foto directa con el botón **Cámara** si tienes el documento en papel o en la pantalla de otro dispositivo (como tu portátil o tablet).

---

### «En WhatsApp el mensaje sale con un botón "Leer más" abajo»
**Causa:** WhatsApp pliega visualmente cualquier mensaje de más de unas `15–20` líneas para no ocupar toda la pantalla del chat de golpe.  
**Solución:** No está cortado: en cuanto tu contacto toca **«Leer más»**, la burbuja se despliega entera manteniendo la alineación perfecta. Si prefieres que tu imagen se vea completa desde el primer vistazo sin tener que pulsar *«Leer más»*, reduce ligeramente el **Ancho** (al bajar el ancho también disminuye el número de filas de alto manteniendo la proporción) o usa el modo **Solo Enlace HD** que envía un texto de apenas 2 líneas.

---

### «¿Cómo sé si tengo la versión más reciente de `img2asciiIM` si no uso Google Play?»
Solo tienes que tocar el icono de menú o el botón **«Comprobar actualizaciones»** en la parte superior de la app. Si tienes conexión a internet en tierra o Wi-Fi normal, la app te avisará al instante de si existe una versión más nueva en GitHub Releases y te permitirá descargarla en 1 toque.

---

### «¿Puedo enviar el código QR de mi tarjeta de embarque con `img2asciiIM` para pasar por el torno del aeropuerto?»
**No para lectores láser u ópticos.** `img2asciiIM` está diseñado para que **una persona vea y entienda visualmente** una foto, el número de puerta, el asiento asignado, un mapa o una cara cuando no puedes mandar imágenes. Los escáneres ópticos de seguridad y embarque del aeropuerto necesitan el código QR original de alta precisión, no una recreación artística en caracteres de texto.

---

### «¿Consume batería o datos móviles convertir varias fotos seguidas?»
**Consumo mínimo y cero datos.** La conversión tarda menos de una décima de segundo en tu procesador, no utiliza internet en ningún momento y procesa las fotos de forma optimizada en memoria para cuidar tanto tu privacidad como la batería durante todo el vuelo.
