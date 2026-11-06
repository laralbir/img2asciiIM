# Guía de Arquitectura Moderna y Seguridad Android (API 26–37 / AGP 9)

Este documento recoge las directrices técnicas y de seguridad auditadas sobre las guías oficiales de Android (AGP 9, Jetpack Compose, Navigation 3, Edge-to-Edge, Adaptive UI, Intent Security y Google Play Policy) aplicables a **img2asciiIM**.

---

## 1. Política de Cero Permisos Invasivos (Play Policy Compliance)

- **Prohibido declarar** `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` o `MANAGE_EXTERNAL_STORAGE` en `AndroidManifest.xml`.
- **Prohibido declarar** `INTERNET` (garantía criptográfica y de sistema de que la conversión es **100 % offline**, compatible con Wi-Fi de avión restringido a IM).
- **Ingesta de imágenes sin permisos de almacenamiento**:
  1. **Photo Picker (`ActivityResultContracts.PickVisualMedia`)**: acceso unificado a galería local y biblioteca en la nube integrada (Google Photos, etc.).
  2. **Storage Access Framework (`ActivityResultContracts.OpenDocument` con `arrayOf("image/*")`)**: acceso universal a **Google Drive**, OneDrive, Dropbox, Nextcloud, Descargas, tarjeta SD y almacenamiento USB OTG.
  3. **Cámara**:
     - Si se delega en la cámara del sistema (`ActivityResultContracts.TakePicture` + `FileProvider`), **no** se debe declarar `<uses-permission android:name="android.permission.CAMERA" />` en el `AndroidManifest.xml` (declararlo sin concederlo en *runtime* provoca `SecurityException` en `ACTION_IMAGE_CAPTURE`). Revocar siempre el permiso temporal del `Uri` en el callback de retorno (`revokeUriPermission`).
     - Si se opta por visor integrado **CameraX** (`androidx.camera.compose.CameraXViewfinder`), declarar `CAMERA` junto con `<uses-feature android:name="android.hardware.camera.any" android:required="false" />`.

---

## 2. Blindaje de Seguridad en Recepción `ACTION_SEND` (`image/*`)

Cuando el usuario comparte una imagen desde Google Drive o la Galería directamente hacia **img2asciiIM**:

1. **Paridad `onCreate` / `onNewIntent`**: procesar el `Intent` entrante con el mismo validador tanto en arranque en frío como con la actividad ya abierta (`super.onNewIntent(intent)` + `setIntent(intent)`).
2. **Extracción tipada segura**: usar `IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)` capturando `BadParcelableException` / `RuntimeException` y anulando `intent.selector = null`.
3. **Defensa *Confused Deputy* y bloqueo de `file://`**:
   - Exigir estrictamente `uri.scheme == ContentResolver.SCHEME_CONTENT` (rechazar `file://`, `android.resource://`, `http(s)://`).
   - Verificar mediante `packageManager.resolveContentProvider(authority, 0)` que `providerInfo.applicationInfo.uid != Process.myUid()`, impidiendo que un `Intent` externo fuerce a la app a leer su propio `FileProvider` privado.
4. **Patrón de Copia en Cuarentena (Prevención de Path Traversal y OOM)**:
   - Nunca construir rutas locales usando `OpenableColumns.DISPLAY_NAME` ni `uri.lastPathSegment` (`../../`).
   - Copiar en `Dispatchers.IO` con límite estricto de tamaño (ej. máx. 30 MB) a un archivo temporal con nombre `UUID.randomUUID()` dentro de `cacheDir/incoming_images/`, o decodificar directamente con submuestreo (`ImageDecoder` / `BitmapFactory.Options.inJustDecodeBounds` + `inSampleSize`).
5. **`FileProvider` de mínimo privilegio**: nunca usar rutas raíz (`path="."` o `<root-path>`); acotar exclusivamente a `<cache-path name="camera_captures" path="camera_captures/" />`.

---

## 3. Navigation 3, Edge-to-Edge y Adaptive UI

- **Navigation 3 (`androidx.navigation3`)**:
  - Rutas tipadas `@Serializable` que implementan `NavKey` y pila persistente con `rememberNavBackStack(...)`.
  - Incluir decoradores `rememberSaveableStateHolderNavEntryDecorator()` y `rememberViewModelStoreNavEntryDecorator()` en `NavDisplay` para que los `ViewModel` tengan ciclo de vida acotado a cada pantalla.
  - Proteger acciones de navegación frente a dobles toques con `dropUnlessResumed { ... }`.
- **Edge-to-Edge y Adaptabilidad**:
  - Mantener `enableEdgeToEdge()` antes de `setContent` y `android:windowSoftInputMode="adjustResize"`.
  - **Nunca** bloquear `android:screenOrientation` ni `android:configChanges` en `AndroidManifest.xml` (soporte nativo para móviles en horizontal, tablets y plegables en modo *tabletop*).
  - Preservar estado de UI con `rememberSaveable` / `rememberSerializable` y verificar recreación en tests con `StateRestorationTester`.

---

## 4. Estrategia Oficial de Testing para TDD en AGP 9

- **Fakes sobre Mocks**: definir interfaces limpias en `domain/` y usar implementaciones `Fake*` con estado en `src/test`, inyectándolas directamente por constructor sin necesidad de mocks frágiles.
- **Pirámide TDD rápida**:
  1. **Unit Tests puros JVM (`src/test`)**: motor ASCII/Braille, dithering, cálculo de presupuestos de caracteres IM y `ViewModel`s con `kotlinx-coroutines-test` + `Turbine` (< 1 s).
  2. **Tests de UI Compose locales con Robolectric (`src/test`)**: `@RunWith(RobolectricTestRunner::class)` + `ComposeTestRule`, priorizando selectores semánticos (`onNodeWithText`, `onNodeWithContentDescription`, roles de accesibilidad) y `StateRestorationTester` (~2–4 s sin emulador).
  3. **Tests Instrumentados E2E (`src/androidTest`)**: integración real en dispositivo físico por **USB** o **Wi-Fi** (`./gradlew connectedDebugAndroidTest`).
