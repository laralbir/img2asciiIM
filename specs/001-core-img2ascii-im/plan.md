# Plan Arquitectónico `001`: `img2asciiIM` (Clean Architecture + TDD)

| Campo | Valor |
| :--- | :--- |
| **Especificación Asociada** | [`specs/001-core-img2ascii-im/spec.md`](spec.md) |
| **Hoja de Ruta TDD** | [`specs/001-core-img2ascii-im/tasks.md`](tasks.md) |
| **Namespace / Paquete Raíz** | `com.laralbir.img2asciiim` |
| **Arquitectura** | Clean Architecture (`domain` ← `data`, `domain` ← `ui`) + UDF (`StateFlow`) |

---

## 1. Diagrama de Flujo de Dependencias y Capas

La regla de dependencia apunta estrictamente hacia `domain`. El núcleo matemático desconoce por completo Android SDK, Jetpack Compose, `Bitmap` o `Uri`.

```mermaid
flowchart TD
    subgraph ExternalIn["Fuentes de Entrada Android (Cero Permisos)"]
        PP["Photo Picker (PickVisualMedia)"]
        SAF["SAF OpenDocument (Local, SD, Google Drive)"]
        CAM["Camara Sistema (TakePicture + FileProvider)"]
        SH_IN["Share Sheet Entrante (ACTION_SEND image/*)"]
    end

    subgraph UI["Capa UI (com.laralbir.img2asciiim.ui)"]
        ACT["MainActivity (Edge-to-Edge + Intents)"]
        SCR["ConverterScreen (Compose Material 3)"]
        BUB["ChatBubblePreview + TuningControlsSheet"]
        VM["ConverterViewModel (StateFlow + Effects)"]
    end

    subgraph DOMAIN["Capa Domain (100% Kotlin Puro - Cero android.*)"]
        UC["ConvertImageToImArtUseCase"]
        PORT_DEC["ImageSourceDecoder (Interface)"]
        PORT_SH["ImMessageDeliveryPort (Interface)"]
        RES["Luminance + ToneAdjuster + AspectRatioResampler"]
        DITH["DitheringEngine (Floyd-Steinberg / Atkinson)"]
        REND["Renderers (Braille U+2800 / ASCII / Blocks)"]
        PRESET["ImPresetPolicy + CharacterBudgetEnforcer + Formatter"]
    end

    subgraph DATA["Capa Data (com.laralbir.img2asciiim.data)"]
        DEC["AndroidImageSourceDecoder (ImageDecoder / BitmapFactory)"]
        EXIF["ExifNormalizer (androidx.exifinterface)"]
        TMP["CameraTempFileProvider (cacheDir)"]
        SHARE["AndroidImMessageDelivery (Package Intent + Clipboard)"]
    end

    subgraph ExternalOut["Entrega Mensajeria"]
        WA["WhatsApp (com.whatsapp)"]
        TG["Telegram (org.telegram.messenger)"]
        IG["Instagram Direct (com.instagram.android)"]
        CLIP["ClipboardManager / Share Sheet Fallback"]
    end

    PP --> ACT
    SAF --> ACT
    CAM --> ACT
    SH_IN --> ACT

    ACT --> SCR
    SCR --> BUB
    SCR --> VM

    VM --> UC
    VM --> PORT_DEC
    VM --> PORT_SH

    UC --> RES
    RES --> DITH
    DITH --> REND
    REND --> PRESET

    DEC -.->|"implementa"| PORT_DEC
    DEC --> EXIF
    ACT --> TMP
    SHARE -.->|"implementa"| PORT_SH

    SHARE --> WA
    SHARE --> TG
    SHARE --> IG
    SHARE --> CLIP
```

---

## 2. Estructura de Paquetes (`app/src/main/java/com/laralbir/img2asciiim/`)

```text
com.laralbir.img2asciiim/
├── domain/                          # 100% Kotlin puro (JVM < 100 ms, cero imports android.*)
│   ├── model/
│   │   ├── ImageSourceRef.kt        # Value class / wrapper opaco de identificador de imagen
│   │   ├── PixelGrid.kt             # Rejilla ARGB/Luminancia pre-escalada e inmutable
│   │   ├── CharsetMode.kt           # BRAILLE_UNICODE, ASCII_CLASSIC, UNICODE_BLOCKS
│   │   ├── DitherMode.kt            # NONE, FLOYD_STEINBERG, ATKINSON
│   │   ├── ImPlatformPreset.kt      # WHATSAPP, TELEGRAM, INSTAGRAM_DM (reglas y límites)
│   │   ├── ConversionParams.kt      # widthColumns, brightness, contrast, invert, dither, mode, preset
│   │   ├── AsciiConversionResult.kt # rawArt, formattedMessage, cols, rows, charCount, maxChars, wasAutoScaled
│   │   └── DomainError.kt           # InvalidOrCorruptedImage, CloudFileOfflineUnavailable, EmptyImage
│   ├── engine/
│   │   ├── LuminanceExtractor.kt    # Composición Alfa + luminancia perceptual BT.709
│   │   ├── ToneAdjuster.kt          # Brillo [-1..1], contraste [0.5..3.0] e inversión claro/oscuro
│   │   ├── AspectRatioResampler.kt  # Box/Area-averaging resampler con compensación tipográfica
│   │   ├── DitheringEngine.kt       # Difusión de error sobre buffer FloatArray reutilizable
│   │   ├── BrailleUnicodeRenderer.kt# Subceldas 2x4 -> U+2800..U+28FF (U+2800 para vacío)
│   │   ├── AsciiClassicRenderer.kt  # Rampa monoespaciada "@%#*+=-:. "
│   │   └── UnicodeBlockRenderer.kt  # Rampa de bloques "█▓▒░ " / "█▓▒░⠀"
│   ├── preset/
│   │   ├── CharacterBudgetEnforcer.kt # Calcula el ancho máximo exacto <= 1.000 / 4.096 / 65.536
│   │   └── ImMessageFormatter.kt    # Envoltorio ```\n...\n``` vs texto directo sin backticks
│   ├── port/
│   │   └── ImageSourceDecoder.kt    # suspend fun decode(source: ImageSourceRef, maxSidePx: Int): Result<PixelGrid>
│   └── usecase/
│       └── ConvertImageToImArtUseCase.kt
│
├── data/                            # Adaptadores Android SDK (ContentResolver, Bitmap, EXIF, Intents)
│   ├── image/
│   │   ├── AndroidImageSourceDecoder.kt # Submuestreo previo + lectura segura de streams SAF/Drive
│   │   ├── SampleSizeCalculator.kt  # Cálculo determinista de inSampleSize / targetSize
│   │   ├── ExifOrientationNormalizer.kt # Transformación matricial de las 8 orientaciones EXIF
│   │   └── CameraCaptureFileStore.kt# Creación y purga de temporales en cacheDir/camera_captures/
│   └── share/
│       ├── ImShareIntentFactory.kt  # ACTION_SEND dirigido + detección de paquete + chooser fallback
│       └── SystemClipboardAdapter.kt# Copia en ClipData("img2asciiIM", formattedMessage)
│
├── ui/                              # Jetpack Compose Material 3 + Edge-to-Edge + Navigation 3
│   ├── navigation/
│   │   └── AppNavigation.kt         # Grafo Navigation 3 preparado para ampliación modular
│   ├── converter/
│   │   ├── ConverterUiState.kt      # Estado inmutable único expuesto por StateFlow
│   │   ├── ConverterUiEvent.kt      # Acciones del usuario (sealed interface)
│   │   ├── ConverterUiEffect.kt     # Efectos efímeros (copiado, lanzar intent, snackbar)
│   │   ├── ConverterViewModel.kt    # Orquestación reactiva (collectLatest en Dispatchers.Default)
│   │   ├── ConverterScreen.kt       # Pantalla principal adaptativa
│   │   └── components/
│   │       ├── SourceIngestionBar.kt    # Botones Galería, Archivos/Drive (SAF), Cámara
│   │       ├── ImPresetSelectorRow.kt   # Chips WhatsApp, Telegram, Instagram Direct
│   │       ├── ChatBubblePreviewCard.kt # Burbuja realista + toggle claro/oscuro + contador N/Max
│   │       ├── TuningControlsPanel.kt   # Sliders ancho, brillo, contraste, dithering, invertir
│   │       └── DeliveryBottomBar.kt     # Copiar en 1 toque + Enviar directo a la app IM
│   └── theme/
│       └── Theme.kt                 # Tema Material 3 dinámico + tipografías Monospace / Sans
│
└── di/
    └── AppContainer.kt              # Inyección de dependencias ligera en Kotlin puro (cero reflexión)
```

---

## 3. Pipeline de Procesamiento y Presupuesto de Memoria / Tiempo

```mermaid
sequenceDiagram
    participant User as "Usuario"
    participant UI as "ConverterScreen / VM"
    participant Data as "AndroidImageSourceDecoder"
    participant Domain as "ConvertImageToImArtUseCase"

    User->>UI: Selecciona imagen (PhotoPicker / SAF Drive / Camara / Share)
    UI->>Data: decode(uri, maxSidePx = 640) en Dispatchers.IO
    Note over Data: Cabecera -> Submuestreo (48MP a ~640px) -> Rotacion EXIF -> PixelGrid (< 1.7 MB)
    Data-->>UI: Result.success(PixelGrid)
    UI->>Domain: invoke(pixelGrid, conversionParams) en Dispatchers.Default
    Note over Domain: BudgetEnforcer -> BoxResampler -> Luminancia/Tono -> Dithering -> Renderer + Wrapper
    Domain-->>UI: AsciiConversionResult (< 50 ms)
    UI-->>User: Renderiza ChatBubblePreview + indicador N / Max chars
```

### Decisiones Técnicas Clave del Pipeline

1. **Tope de Memoria `< 20 MB` garantizado por diseño (`RNF-02`):**
   - El ancho máximo permitido en mensajería es `72` columnas; incluso en modo `BRAILLE_UNICODE` (`2×4` subpíxeles), la rejilla efectiva máxima requiere apenas `144` subpíxeles horizontales.
   - Decodificar la imagen de origen a un lado máximo de **`640 px`** ofrece más de `4×` de sobremuestreo para el *box-averaging resampler* mientras que un `Bitmap` ARGB_8888 de `640 × 640` ocupa solo **`1,64 MB`** (frente a los **`192 MB`** que consumiría decodificar en crudo una foto de `48 MP` `8000 × 6000`).
   - Una vez copiado a `PixelGrid`, el `Bitmap` temporal queda elegible de inmediato para GC sin almacenarse nunca en `ConverterUiState`.

2. **Matemática del Presupuesto de Caracteres (`CharacterBudgetEnforcer`):**
   - Para un número de columnas `C` y filas `R`, el arte crudo ocupa `L_raw(C, R) = R * C + (R - 1)` caracteres (cada fila tiene `C` caracteres y hay `R - 1` saltos `\n`).
   - Si el perfil lleva envoltorio monoespaciado (```` ```\n ```` + ```` \n``` ````), el coste fijo adicional es `W = 8` caracteres (`W = 0` en Instagram Direct).
   - Longitud total exacta: `L_total(C, R) = R * (C + 1) - 1 + W`.
   - Dado que `R = max(1, round(C * (imgHeight / imgWidth) * verticalScaleFactor))` es monótona creciente respecto a `C`, si `L_total(C_solicitado) > MaxBudget` (`1.000` en Instagram DM o `4.096` en Telegram), `CharacterBudgetEnforcer` halla mediante **búsqueda binaria en `O(log C)`** el mayor `C_efectivo` tal que `L_total(C_efectivo) <= MaxBudget`.

3. **Integración Limpia con el Manifiesto Android (`AndroidManifest.xml`):**
   - **Permisos:** Cero `INTERNET`, cero `READ_MEDIA_IMAGES`, cero `READ_EXTERNAL_STORAGE`, cero `CAMERA` (bajo Opción A de `DD-01`).
   - **Visibilidad de Paquetes Android 11+ (`<queries>`):**
     ```xml
     <queries>
         <package android:name="com.whatsapp" />
         <package android:name="com.whatsapp.w4b" />
         <package android:name="org.telegram.messenger" />
         <package android:name="org.thunderdog.challegram" />
         <package android:name="com.instagram.android" />
     </queries>
     ```
   - **`FileProvider`:** Configurado exclusivamente sobre `<cache-path name="camera_captures" path="camera_captures/" />` con `android:exported="false"` y `android:grantUriPermissions="true"`.
   - **Receptor Share Sheet:** `<intent-filter>` con `android.intent.action.SEND`, categoría `DEFAULT` y `mimeType="image/*"`.

---

## 4. Estrategia de Pruebas (Pirámide TDD)

```mermaid
flowchart TD
    E2E["Nivel 3: Instrumentados USB / Wi-Fi ADB (~10%) - Journey E2E, Heap < 20MB, Latencia < 150ms"]
    ROBO["Nivel 2: Robolectric + Compose UI JVM (~25%) - Submuestreo Bitmap/EXIF, SAF, Intents, a11y"]
    UNIT["Nivel 1: Unit Tests JVM Puros (~65%, < 100ms) - Domain completo + ViewModel Turbine"]

    UNIT --> ROBO
    ROBO --> E2E
```

### Nivel 1 — Unit Tests JVM Puros (`./gradlew testDebugUnitTest`)
- **Alcance:** Todo `domain/**` y `ConverterViewModel`.
- **Herramientas:** JUnit, `kotlin.test`, `kotlinx-coroutines-test` (`StandardTestDispatcher`, `runTest`), `app.cash.turbine:turbine`.
- **Garantías:** Verifica bit a bit cada punto Braille `U+2800..U+28FF`, conservación de proporciones, difusión de error Floyd-Steinberg/Atkinson, límites exactos de `1.000`, `4.096` y `65.536` caracteres y transiciones del `StateFlow` ante deslizamientos rápidos de controles. Tiempo objetivo de toda la capa `domain/`: **< 100 ms**.

### Nivel 2 — Integración Android & UI en JVM con Robolectric (`./gradlew testDebugUnitTest`)
- **Alcance:** `data/**` (`AndroidImageSourceDecoder`, `ExifOrientationNormalizer`, `ImShareIntentFactory`, auditoría de `AndroidManifest.xml`) y `ui/**` (`ConverterScreen` con `createComposeRule()`).
- **Herramientas:** Robolectric, `androidx.compose.ui:ui-test-junit4`, `ExifInterface`.
- **Garantías:** Confirma que decodificar imágenes grandes aplica `inSampleSize > 1`, que `IOException` de un archivo de Google Drive sin caché no rompe el proceso, que `ACTION_SEND` apunta al paquete correcto o cae en `Intent.createChooser`, y que todos los nodos interactivos cumplen accesibilidad.

### Nivel 3 — Instrumentados End-to-End en Dispositivo Físico / AVD (`./gradlew connectedDebugAndroidTest`)
- **Alcance:** Flujo integrado sobre runtime ART real conectado por **USB o Wi-Fi ADB**.
- **Herramientas:** AndroidX Test Runner, Compose Android UI Test, `ClipboardManager` real del sistema, perfilado de memoria `Runtime.getRuntime()`.
- **Garantías:** Certifica en silicio real que una imagen generada de `8000 × 6000` (48 MP) se convierte en `< 150 ms` consumiendo `< 20 MB` de RAM adicional, que `U+2800` mantiene alineación visual en el motor tipográfico Skia/Minikin de Android y que todo funciona sin conectividad de red.
