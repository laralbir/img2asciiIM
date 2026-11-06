# img2asciiIM

![img2asciiIM Banner](docs/assets/banner.png)

> **Transforma imágenes en arte ASCII / Braille Unicode compacto 100 % offline para enviarlas como texto plano por WhatsApp, Telegram e Instagram Direct cuando el internet solo permite mensajería instantánea (ej. planes *Free Messaging* en el Wi-Fi de vuelos comerciales).**

---

## Propósito

En muchos vuelos comerciales (Iberia, Vueling, Lufthansa, Delta, United, Air Europa…), cruceros o redes satelitales restringidas, el plan básico de conectividad permite **únicamente paquetes de texto** en apps de mensajería instantánea, bloqueando por DPI/SNI la subida a los servidores multimedia (`mmg.whatsapp.net`, `scontent.cdninstagram.com`, CDNs de Telegram).

**img2asciiIM** convierte cualquier imagen en el propio dispositivo (**sin permiso `INTERNET` y sin permisos invasivos de almacenamiento**) desde:
- **Galería y fotos en la nube** (`Android Photo Picker` — `PickVisualMedia`)
- **Cualquier almacenamiento local o en la nube** (`Storage Access Framework` — **Google Drive**, OneDrive, Dropbox, Nextcloud, Descargas, SD, USB OTG)
- **Cámara del dispositivo** (`FileProvider` / captura directa)
- **Menú Compartir de Android** (`Intent.ACTION_SEND` `image/*` directamente desde Google Drive o Galería hacia `img2asciiIM`)

Y adapta automáticamente la salida a las reglas tipográficas y límites exactos de cada app:

| Plataforma | Modo Óptimo | Formato | Límite Estricto |
| :--- | :--- | :--- | :--- |
| **WhatsApp** | `ASCII_CLASSIC` / `BRAILLE_UNICODE` | Bloque monoespaciado ```` ```...``` ```` (sin `U+0060` ni `U+0020` inicial) | `65.536` chars (`~32` cols recomendadas) |
| **Telegram** | `BRAILLE_UNICODE` / `ASCII_CLASSIC` | Braille directo (`U+2800..U+28FF`) o ```` ```\n...\n``` ```` | **`4.096`** chars UTF-16 |
| **Instagram Direct** | `BRAILLE_UNICODE` (`⠀`–`⣿`) | Sin ```` ``` ```` (`U+2800` anti-colapso en fuente proporcional) + *Clipboard-First* | **`1.000`** chars / `1.000` bytes UTF-8 |

---

## Metodología: SDD + TDD + Clean Architecture en Kotlin

Este proyecto sigue una disciplina estricta **Spec-Driven Development (SDD)** combinada con **Test-Driven Development (TDD)**:

1. **Especificación primero (`specs/`)**: Ninguna funcionalidad de negocio se implementa sin una especificación revisada y en estado `APROBADO` con criterios deterministas **Given / When / Then**.
2. **Ciclo TDD estricto (`Red → Green → Refactor`)**:
   - **Unit Tests JVM (`< 100 ms`)**: Motor puro Kotlin en `domain/` (cero dependencias `android.*`), `ViewModel`s con `kotlinx-coroutines-test` + `Turbine`.
   - **Compose UI Tests en JVM (`~2–5 s`)**: `Robolectric 4.17` + `ComposeTestRule` sin necesidad de emulador.
   - **Tests Instrumentados en Dispositivo Real (`USB` / `Wi-Fi`)**: `./gradlew connectedDebugAndroidTest`.

### Stack Moderno (2026)
- **Lenguaje & Build**: Kotlin `2.3.20` · Android Gradle Plugin `9.0.1` (Built-in Kotlin + Configuration Cache) · Gradle `9.1.0`
- **SDK**: `minSdk = 26` · `targetSdk = 37` · `compileSdk = 37.0`
- **UI & Navegación**: Jetpack Compose (`BOM 2026.03.01`) · Material 3 · Edge-to-Edge · Jetpack Navigation 3 (`1.0.1`)
- **Testing**: JUnit 4 · `kotlin-test` · `kotlinx-coroutines-test` · Cash App `Turbine 1.2.1` · `Robolectric 4.17` · Compose UI Test

---

## Comandos Rápidos (TDD + Debug por USB y Wi-Fi)

El repositorio incluye [`scripts/adb-debug.sh`](scripts/adb-debug.sh) y un [`Makefile`](Makefile) listo para depurar tanto por **cable USB** como por **Wi-Fi** (incluyendo emparejamiento Android 11+ mediante **código QR renderizado en la propia terminal**, código de 6 dígitos con autodescubrimiento mDNS o puente clásico `USB → TCP/IP 5555`):

```bash
# Diagnóstico completo del entorno (SDK, ADB, mDNS, dispositivos USB y Wi-Fi)
make doctor

# Ejecutar suite TDD rápida en JVM (Unit + Turbine + Robolectric Compose UI)
make test

# --- DEBUG POR USB ---
make usb             # Espera dispositivo USB, verifica autorización RSA y muestra info

# --- DEBUG POR WI-FI (3 métodos) ---
make wifi-qr         # Android 11+: Muestra QR en terminal -> escanear desde el móvil y conecta solo
make wifi-pair       # Android 11+: Vinculación por código de 6 dígitos con autodescubrimiento mDNS
make wifi-from-usb   # Puente clásico: pasa un móvil conectado por USB a Wi-Fi (puerto 5555)

# --- DESPLIEGUE Y LOGS EN VIVO ---
make deploy-usb      # Instala y abre directamente en el móvil por USB (ej. Pixel 10 Pro XL)
make deploy-wifi     # Instala y abre directamente en el móvil por Wi-Fi
make deploy SERIAL=pixel10  # También acepta filtro por nombre de modelo o serial exacto
make logs-usb        # Logcat en color filtrado por el PID de img2asciiIM en el móvil USB
make test-device     # Ejecuta tests instrumentados (androidTest) en el móvil USB/Wi-Fi
```

---

## Documentación del Proyecto

- [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md) — **Guía de Usuario**: uso en vuelos comerciales, preparación de Google Drive sin conexión, recetas por tipo de imagen y envío en WhatsApp, Telegram e Instagram Direct.
- [`docs/TECHNICAL_DEVELOPMENT.md`](docs/TECHNICAL_DEVELOPMENT.md) — **Documentación Técnica del Desarrollo**: arquitectura limpia, motor matemático Braille/ASCII/Dithering, seguridad SAF/`ACTION_SEND`, icono adaptativo/SplashScreen y flujo TDD.
- [`docs/DEBUGGING.md`](docs/DEBUGGING.md) — Guía paso a paso para depuración por USB, Wi-Fi (QR / código / puente USB) y *mirroring* en vivo.
- [`docs/IM_CONSTRAINTS.md`](docs/IM_CONSTRAINTS.md) — Estudio técnico de Wi-Fi en vuelos comerciales y límites/peculiaridades de WhatsApp, Telegram e Instagram DM.
- [`docs/ANDROID_ARCHITECTURE_AND_SECURITY.md`](docs/ANDROID_ARCHITECTURE_AND_SECURITY.md) — Directrices auditadas de seguridad de `Intent`s, SAF, Photo Picker y AGP 9.
- [`AGENTS.md`](AGENTS.md) — Constitución de trabajo SDD + TDD, arquitectura por capas y roles multi-agente.
- [`specs/001-core-img2ascii-im/spec.md`](specs/001-core-img2ascii-im/spec.md) — Especificación funcional/técnica (`APROBADO`).
- [`specs/001-core-img2ascii-im/plan.md`](specs/001-core-img2ascii-im/plan.md) — Plan arquitectónico Clean Architecture y estrategia de testing.
- [`specs/001-core-img2ascii-im/tasks.md`](specs/001-core-img2ascii-im/tasks.md) — Hoja de ruta en 8 *Vertical Slices* TDD (`[RED] → [GREEN] → [REFACTOR]`).
