# Hoja de Ruta TDD `001`: Vertical Slices (`Red → Green → Refactor`)

| Campo | Valor |
| :--- | :--- |
| **Especificación Asociada** | [`specs/001-core-img2ascii-im/spec.md`](spec.md) |
| **Plan Arquitectónico** | [`specs/001-core-img2ascii-im/plan.md`](plan.md) |
| **Estado de Ejecución** | `BLOQUEADO HASTA SIGN-OFF DEL USUARIO EN spec.md` |
| **Disciplina Obligatoria** | Cada tarea **debe** completar secuencialmente `[RED]` → `[GREEN]` → `[REFACTOR]` |

---

## Gate 0 — Aprobación de Especificación (*Pre-TDD Sign-off*)

- [ ] **G0.1:** El usuario revisa [`spec.md`](spec.md) y resuelve las Decisiones de Diseño Abiertas (`DD-01`, `DD-02`, `DD-03`, `DD-04`).
- [ ] **G0.2:** `sdd-architect` actualiza `spec.md` con las decisiones confirmadas y cambia el estado de `BORRADOR` a `APROBADO`.
- [ ] **G0.3:** El esqueleto vacío de compilación (`./gradlew testDebugUnitTest` sin lógica de negocio) compila en limpio.

---

## Slice 1 — Extracción de Luminancia, Ajuste Tonal y Remuestreador con Relación de Aspecto (`domain/` 100% Kotlin Puro)

> **Cobertura:** `CA-02.2`, `CA-02.3`, `CB-01`, `CB-03`, `RNF-06` (`< 100 ms` JVM, cero imports `android.*`).

- [ ] **Tarea 1.1 — `PixelGrid` y `LuminanceExtractor` (BT.709 + Composición Alfa)**
  - **`[RED]`:** Crear `app/src/test/java/com/laralbir/img2asciiim/domain/engine/LuminanceExtractorTest.kt` verificando: (1) negro puro `0.0f`, blanco puro `1.0f` y ponderación `BT.709` exacta en primarios R/G/B; (2) píxeles transparentes `RGBA(0,0,0,0)` compuestos sobre blanco cuando `invert = false` y sobre negro cuando `invert = true` (`CB-01`). Ejecutar `./gradlew testDebugUnitTest` y confirmar fallo.
  - **`[GREEN]`:** Implementar `PixelGrid` y `LuminanceExtractor` en `domain/model/` y `domain/engine/` sin imports de Android. Confirmar tests en verde.
  - **`[REFACTOR]`:** Reemplazar estructuras intermedias por `FloatArray` contiguo para evitar *boxing* y minimizar presión de GC.

- [ ] **Tarea 1.2 — `ToneAdjuster` (Brillo, Contraste e Inversión Claro/Oscuro)**
  - **`[RED]`:** Crear `ToneAdjusterTest.kt` comprobando saturación segura en `[0.0f, 1.0f]`, punto pivote de contraste en `0.5f` y simetría exacta al alternar `invert = true/false` (`CA-02.2`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `ToneAdjuster.kt` superando todos los casos.
  - **`[REFACTOR]`:** Fusionar pasada de brillo/contraste/inversión *in-place* sobre el buffer tonal cuando sea seguro, manteniendo inmutabilidad externa.

- [ ] **Tarea 1.3 — `AspectRatioResampler` (Muestreo por Promedio de Área y Geometría de Celda)**
  - **`[RED]`:** Crear `AspectRatioResamplerTest.kt` verificando que: (1) una imagen cuadrada `1000 × 1000` convertida a `32` columnas con `cellAspectRatio = 0.5f` produce `16` filas en ASCII (`32 × 16`) y `64 × 64` subpíxeles (`32 × 16` caracteres de `2×4`) en modo Braille (`CA-02.3`); (2) imágenes extremas `10000 × 200` garantizan al menos `1` fila/columna sin división por cero (`CB-03`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `AspectRatioResampler.kt` mediante *box/area averaging* determinista.
  - **`[REFACTOR]`:** Precalcular límites de ventana de muestreo y verificar que la suite de `Slice 1` corre en `< 25 ms`.

---

## Slice 2 — Renderizadores ASCII, Bloques y Braille Unicode (`U+2800..U+28FF`) + Dithering (`domain/`)

> **Cobertura:** `CA-02.1`, `CA-02.4`, `CB-05`, Invariantes `U+2800..U+28FF` y UTF-16 BMP.

- [ ] **Tarea 2.1 — Renderizadores `AsciiClassicRenderer` y `UnicodeBlockRenderer`**
  - **`[RED]`:** Crear `AsciiAndBlockRendererTest.kt` verificando el mapeo monótono de densidad sobre `"@%#*+=-:. "` y `"█▓▒░ "` (o `"█▓▒░⠀"` en perfil proporcional), ausencia total del carácter `` ` `` y saltos de línea estrictamente `\n`. Ejecutar `./gradlew testDebugUnitTest` y confirmar fallo.
  - **`[GREEN]`:** Implementar `AsciiClassicRenderer.kt` y `UnicodeBlockRenderer.kt`.
  - **`[REFACTOR]`:** Extraer tabla de cuantización compartida usando `StringBuilder` con capacidad exacta preasignada `rows * (cols + 1)`.

- [ ] **Tarea 2.2 — `BrailleUnicodeRenderer` (`U+2800`..`U+28FF` ISO/TR 11548-1)**
  - **`[RED]`:** Crear `BrailleUnicodeRendererTest.kt` validando: (1) cada uno de los 8 puntos individuales `(dx, dy)` contra su máscara exacta (`0x01`, `0x02`, `0x04`, `0x08`, `0x10`, `0x20`, `0x40`, `0x80`); (2) celda vacía produce `U+2800` (`'⠀'`) y **nunca** espacio ASCII `' '` (`U+0020`); (3) celda llena produce `U+28FF` (`'⣿'`) y diagonal `(0,0)+(1,3)` produce `U+2881` (`CA-02.1`); (4) `String.trim()` no recorta `U+2800` al principio ni al final (`CB-05`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `BrailleUnicodeRenderer.kt` superando todos los vectores de prueba.
  - **`[REFACTOR]`:** Optimizar el empaquetado de bits de subceldas `2×4` sin asignaciones de colecciones por celda.

- [ ] **Tarea 2.3 — `DitheringEngine` (Floyd-Steinberg y Atkinson)**
  - **`[RED]`:** Crear `DitheringEngineTest.kt` probando que una rejilla gris uniforme al `50%` (`0.5f`) procesada con `FLOYD_STEINBERG` y `ATKINSON` produce una densidad media de puntos encendidos entre `35%` y `65%` (frente al `0%` o `100%` de un umbral rígido desplazado) y que no desborda índices en los bordes izquierdo, derecho ni inferior (`CA-02.4`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar difusión de error `Floyd-Steinberg` (ventana 2 filas) y `Atkinson` (ventana 3 filas) en `DitheringEngine.kt`.
  - **`[REFACTOR]`:** Usar buffers circulares de filas (`FloatArray`) reutilizables y verificar tiempo total de `domain/` `< 50 ms`.

---

## Slice 3 — Perfiles de Plataforma IM y Ajustador de Presupuesto de Caracteres (`domain/`)

> **Cobertura:** `RF-03`, `CA-03.1` (WhatsApp `65.536`), `CA-03.2` (Telegram `4.096`), `CA-03.3` (Instagram DM `1.000` sin ```` ``` ````), Caso de uso integrado `ConvertImageToImArtUseCase`.

- [ ] **Tarea 3.1 — `ImPlatformPreset` e `ImMessageFormatter`**
  - **`[RED]`:** Crear `ImPresetAndFormatterTest.kt` verificando que: (1) **WhatsApp** y **Telegram** envuelven la salida con ```` ```\n<arte>\n``` ```` (exactamente `+8` caracteres); (2) **Instagram Direct** omite todo backtick ```` ``` ```` y establece `BRAILLE_UNICODE` como modo predeterminado (`CA-03.1`, `CA-03.3`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `ImPlatformPreset.kt` e `ImMessageFormatter.kt`.
  - **`[REFACTOR]`:** Centralizar metadatos de paquete Android objetivo, anchos recomendados y banderas de soporte monoespaciado en el modelo de dominio.

- [ ] **Tarea 3.2 — `CharacterBudgetEnforcer` y `ConvertImageToImArtUseCase`**
  - **`[RED]`:** Crear `CharacterBudgetEnforcerTest.kt` y `ConvertImageToImArtUseCaseTest.kt` comprobando con imágenes panorámicas, cuadradas y capturas ultra-verticales (`9:21`) que: (1) bajo **Instagram Direct** `formattedMessage.length <= 1_000` siempre (`CA-03.3`); (2) bajo **Telegram** `formattedMessage.length <= 4_096` incluyendo los 8 caracteres del bloque ```` ``` ```` (`CA-03.2`); (3) bajo **WhatsApp** `formattedMessage.length <= 65_536` (`CA-03.1`); (4) cuando hay reducción automática se activa `wasAutoScaled = true`. Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar búsqueda binaria de columnas máximas en `CharacterBudgetEnforcer.kt` y orquestar el pipeline completo en `ConvertImageToImArtUseCase.kt`.
  - **`[REFACTOR]`:** Auditar con un test de arquitectura estática que `domain/` contiene **cero** referencias a `android.*` o `androidx.*` y que toda la suite `Slice 1..3` corre en `< 100 ms` (`RNF-06`).

---

## Slice 4 — Decodificador Seguro Submuestreado, Normalización EXIF y Adaptadores URI/SAF/Cámara (`data/`)

> **Cobertura:** `RF-01` (`CA-01.1..01.4`), `RNF-02` (`< 20 MB` RAM), `RNF-03` (Cero permisos `INTERNET`/almacenamiento), `CB-02` (Google Drive offline), `CB-04` (imagen corrupta).

- [ ] **Tarea 4.1 — `SampleSizeCalculator` y Decodificación Submuestreada Segura**
  - **`[RED]`:** Crear `SampleSizeCalculatorTest.kt` y `AndroidImageSourceDecoderTest.kt` (Robolectric) verificando que: (1) para una cabecera de `8000 × 6000` (`48 MP`) con objetivo `maxSidePx = 640`, el factor de submuestreo reduce el `Bitmap` alojado a `<= 1280 px` sin cargar la imagen completa (`RNF-02`); (2) un stream de 0 bytes o basura devuelve `Result.failure(DomainError.InvalidOrCorruptedImage)` (`CB-04`); (3) un `InputStream` de proveedor SAF/Google Drive que lanza `IOException` devuelve `Result.failure(DomainError.CloudFileOfflineUnavailable)` (`CB-02`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `SampleSizeCalculator.kt` y `AndroidImageSourceDecoder.kt`.
  - **`[REFACTOR]`:** Garantizar cierre determinista de todos los `InputStream` con `.use {}` y liberación inmediata de `Bitmap` intermedios.

- [ ] **Tarea 4.2 — `ExifOrientationNormalizer` y `CameraCaptureFileStore` (`FileProvider`)**
  - **`[RED]`:** Crear `ExifOrientationNormalizerTest.kt` y `CameraCaptureFileStoreTest.kt` (Robolectric) verificando: (1) una imagen asimétrica con etiquetas EXIF `ROTATE_90`, `ROTATE_180`, `ROTATE_270` y `FLIP_HORIZONTAL` queda orientada correctamente en el `PixelGrid` resultante (`CA-02.3`); (2) `CameraCaptureFileStore` genera URIs válidas de `FileProvider` bajo `cacheDir/camera_captures/` y elimina temporales obsoletos (`CA-01.3`); (3) `ManifestSecurityTest` comprueba que `AndroidManifest.xml` **no** contiene `INTERNET`, `READ_MEDIA_IMAGES` ni `READ_EXTERNAL_STORAGE` (`RNF-03`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `ExifOrientationNormalizer.kt`, `CameraCaptureFileStore.kt` y la configuración XML de `file_paths.xml` + `AndroidManifest.xml`.
  - **`[REFACTOR]`:** Unificar transformaciones matriciales EXIF para no duplicar copias de `Bitmap` cuando la orientación ya es `ORIENTATION_NORMAL`.

---

## Slice 5 — `ConverterViewModel` y Estado Unidireccional Inmutable (`StateFlow` + `Turbine`)

> **Cobertura:** `CA-01.5` (cancelación inocua), `CA-02.5` (reactividad offline), `CA-03.3` (aviso ASCII en Instagram), `CB-06` (persistencia en rotación).

- [ ] **Tarea 5.1 — Orquestación de Carga, Presets IM y Controles en Tiempo Real**
  - **`[RED]`:** Crear `ConverterViewModelTest.kt` usando `StandardTestDispatcher` y **Turbine** (`uiState.test { ... }`) verificando: (1) carga inicial y recepción de URI desde `ACTION_SEND` (`CA-01.4`); (2) cancelación de selector (`uri = null`) preserva intacto el arte previo (`CA-01.5`); (3) cambiar a preset `INSTAGRAM_DM` selecciona `BRAILLE_UNICODE`, elimina backticks y garantiza `<= 1.000` chars (`CA-03.3`); (4) ráfagas de cambios en sliders de ancho/contraste cancelan cálculos obsoletos (`collectLatest`) emitiendo el último resultado consistente; (5) errores de archivo cloud offline actualizan el estado de aviso sin borrar el trabajo anterior (`CB-02`). Ejecutar `./gradlew testDebugUnitTest` y confirmar fallo.
  - **`[GREEN]`:** Implementar `ConverterUiState.kt`, `ConverterUiEvent.kt`, `ConverterUiEffect.kt` y `ConverterViewModel.kt`.
  - **`[REFACTOR]`:** Separar el `PixelGrid` cacheado en memoria privada del `ViewModel` respecto al `ConverterUiState` renderizado para evitar comparaciones pesadas de arrays en recomposiciones de Compose.

---

## Slice 6 — Interfaz Jetpack Compose Material 3, Vista Previa de Burbuja e Intents de Entrega (`ui/` + `data/share/`)

> **Cobertura:** `RF-04` (`CA-04.1` Portapapeles, `CA-04.2` Envío directo, `CA-04.3` Fallback Share Sheet), `RNF-05` (Accesibilidad `contentDescription` y semántica agrupada).

- [ ] **Tarea 6.1 — `ImShareIntentFactory` y `SystemClipboardAdapter`**
  - **`[RED]`:** Crear `ImShareIntentFactoryTest.kt` (Robolectric `ShadowPackageManager`) verificando: (1) cuando `org.telegram.messenger` está instalado, el `Intent` devuelto es `ACTION_SEND` (`text/plain`) con `package = "org.telegram.messenger"` (`CA-04.2`); (2) cuando `com.instagram.android` **no** está instalado, devuelve un `Intent` `ACTION_CHOOSER` seguro sin lanzar excepción (`CA-04.3`); (3) `<queries>` en `AndroidManifest.xml` declara los paquetes objetivo para Android 11+; (4) `SystemClipboardAdapter` deposita exactamente `formattedMessage` en `ClipboardManager` (`CA-04.1`). Ejecutar tests y confirmar fallo.
  - **`[GREEN]`:** Implementar `ImShareIntentFactory.kt` y `SystemClipboardAdapter.kt`.
  - **`[REFACTOR]`:** Extraer resolución de paquetes alternativos (ej. WhatsApp Business según `DD-04`) en una tabla declarativa limpia.

- [ ] **Tarea 6.2 — `ConverterScreen`, `ChatBubblePreviewCard` y Controles Compose Material 3**
  - **`[RED]`:** Crear `ConverterScreenTest.kt` (Robolectric + `createComposeRule()`) validando: (1) presencia y disparo de botones **"Galería"**, **"Archivos / Drive"** y **"Cámara"**; (2) renderizado de la burbuja de vista previa con contador `N / Máx` caracteres y conmutador de fondo claro/oscuro; (3) aviso visible al forzar `ASCII_CLASSIC` en perfil **Instagram Direct** (`CA-03.3`); (4) botones **"Copiar"** y **"Enviar a ..."** operativos; (5) todos los controles interactivos poseen `contentDescription` no vacío y la burbuja expone semántica resumida accesible para TalkBack (`RNF-05`). Ejecutar `./gradlew testDebugUnitTest` y confirmar fallo.
  - **`[GREEN]`:** Implementar `ConverterScreen.kt`, los subcomponentes en `ui/converter/components/` y la conexión Edge-to-Edge + recepción `ACTION_SEND` en `MainActivity.kt`.
  - **`[REFACTOR]`:** Añadir anotaciones `@Preview` (claro/oscuro, teléfono compacto, WhatsApp vs Instagram DM), estabilizar lambdas de Compose y ejecutar `./gradlew lintDebug` con **0 errores**.

---

## Slice 7 — Verificación End-to-End en Dispositivo Físico / AVD (USB y Wi-Fi ADB)

> **Cobertura:** `RNF-01` (`< 150 ms`), `RNF-02` (`< 20 MB` RAM con foto 48 MP), `RNF-04` (Modo Avión / Offline total), Renderizado real de fuentes Android.

- [ ] **Tarea 7.1 — Suite Instrumentada `EndToEndConversionAndroidTest`**
  - **`[RED]`:** Crear `app/src/androidTest/java/com/laralbir/img2asciiim/EndToEndConversionAndroidTest.kt` automatizando en dispositivo real/AVD: (1) generación en caché de prueba de una imagen sintética de `8000 × 6000` (`48 MP`) con rotación EXIF `90°`; (2) inyección de la imagen en la app, verificando que el incremento de heap durante la decodificación y conversión es `< 20 MB` (`RNF-02`) y que el tiempo del motor es `< 150 ms` (`RNF-01`); (3) cambio entre perfiles WhatsApp, Telegram e Instagram Direct comprobando longitud exacta y copia real al `ClipboardManager` del dispositivo; (4) verificación visual/métrica de que una fila de `U+2800` tiene ancho mayor que cero en el componente `Text` con fuente proporcional del sistema.
  - **`[GREEN]`:** Ejecutar `./gradlew connectedDebugAndroidTest` sobre dispositivo enlazado por **USB o Wi-Fi ADB** y ajustar cualquier detalle de integración con ART/Skia hasta pasar al 100% en verde.
  - **`[REFACTOR]`:** Ejecutar validación en **Modo Avión** (`RNF-04`), documentar métricas reales obtenidas y actualizar el estado de [`spec.md`](spec.md) a `IMPLEMENTADO`.
