# Guía de Depuración Android por USB y Wi-Fi (`img2asciiIM`)

Este proyecto incluye un entorno automatizado en macOS para conectar dispositivos físicos Android —tanto por **cable USB** como por **Wi-Fi**—, compilar e instalar `com.laralbir.img2asciiim`, inspeccionar `logcat` filtrado dinámicamente por PID y ejecutar la suite de tests TDD.

Todos los comandos están disponibles mediante `make` (consulta `make help`) o directamente con `./scripts/adb-debug.sh`.

---

## 1. Requisitos previos en el móvil Android

Antes de conectar el teléfono por primera vez, configura lo siguiente en el dispositivo:

1. **Activar las Opciones para desarrolladores**:
   - Abre **Ajustes → Información del teléfono** (en Samsung: *Acerca del teléfono → Información de software*).
   - Pulsa **7 veces seguidas** sobre **Número de compilación** (*Build number*) hasta ver el mensaje *«¡Ya eres desarrollador!»* (introduce tu PIN si lo solicita).
2. **Para depurar por cable (Flujos 1 y 3)**:
   - Entra en **Ajustes → Sistema → Opciones para desarrolladores**.
   - Activa **Depuración por USB**.
   - Al conectar el cable al Mac, desbloquea el móvil, marca **«Permitir siempre desde este ordenador»** en el aviso de huella RSA y pulsa **Permitir**.
3. **Para depurar por Wi-Fi sin cables en Android 11+ (Flujo 2)**:
   - Conecta el móvil a la **misma red Wi-Fi** que tu Mac (evita redes «Invitados / Guest» con aislamiento de clientes).
   - En **Opciones para desarrolladores**, activa **Depuración inalámbrica** y confirma *Permitir siempre en esta red*.

> [!IMPORTANT]
> **Configuración adicional obligatoria en Xiaomi, Redmi y POCO (MIUI / HyperOS)**
> 1. En *Ajustes → Sobre el teléfono*, pulsa 7 veces sobre **Versión de HyperOS** (o **Versión de MIUI**).
> 2. Dentro de *Ajustes adicionales → Opciones de desarrollador*, además de *Depuración USB*, activa estas dos opciones (requieren cuenta Mi activa y tarjeta SIM o Wi-Fi):
>    - **Instalar vía USB**: imprescindible para que `make deploy` y `make test-device` no fallen con `INSTALL_FAILED_USER_RESTRICTED`.
>    - **Depuración USB (Ajustes de seguridad)**: permite inyectar eventos táctiles desde Android Device Hub y pruebas instrumentadas.

---

## 2. Diagnóstico rápido del entorno (`make doctor`)

En cualquier momento puedes verificar el estado completo de herramientas, red y dispositivos ejecutando:

```bash
make doctor
```

Este comando comprueba:
- Rutas y versiones de `adb` y `android` CLI, además de `ANDROID_HOME`.
- La dirección IP local activa de tu Mac (`en0`/`en1`) y el estado del demonio mDNS (`adb mdns check`).
- Dispositivos conectados por **USB**, dispositivos conectados por **Wi-Fi**, anuncios **mDNS** detectados en la LAN y emuladores AVD disponibles.
- Alertas si algún móvil aparece como `unauthorized` u `offline`, mostrando los pasos exactos para aceptar la clave RSA.

---

## 3. Flujo 1: Depuración por Cable USB

Es la vía más directa y de menor latencia para desplegar APKs grandes o pasar suites largas de tests instrumentados.

```bash
# 1. Conecta el cable USB de datos y verifica autorización, modelo y versión Android
make usb

# 2. Compila en modo Debug, instala (con delta rápido) y abre MainActivity
make deploy

# 3. En la misma terminal o en otra pestaña, sigue los logs filtrados por PID
make logs
```

> [!TIP]
> `make logs` detecta automáticamente el **PID** de `com.laralbir.img2asciiim`. Si lanzas `make logs` antes de abrir la app —o si vuelves a ejecutar `make deploy` y el proceso cambia de PID—, el script alterna y reconecta el filtro `--pid` automáticamente sin perder trazas ni `AndroidRuntime` crashes.

---

## 4. Flujo 2: Depuración por Wi-Fi sin cables (Android 11+)

No necesitas ningún cable USB. Requiere Android 11 (API 30) o superior y que Mac y móvil compartan red local.

### Opción A: Escaneo de código QR directo en la terminal (`make wifi-qr`)

1. Ejecuta en tu Mac:
   ```bash
   make wifi-qr
   ```
2. Verás un **código QR nítido en la terminal**.
3. En tu móvil, ve a **Ajustes → Opciones para desarrolladores → Depuración inalámbrica → Vincular dispositivo con código QR** y apunta la cámara a la terminal.
4. El script detectará el anuncio `_adb-tls-pairing._tcp` por mDNS, ejecutará `adb pair` automáticamente y acto seguido se conectará al puerto seguro `_adb-tls-connect._tcp`.
5. Despliega y observa los logs igual que por cable:
   ```bash
   make deploy
   make logs
   ```

### Opción B: Código numérico de 6 dígitos (`make wifi-pair`)

Útil si prefieres no usar la cámara o si deseas introducir `IP:PUERTO` manualmente:

1. En el móvil, entra en **Depuración inalámbrica → Vincular dispositivo con código de vinculación** y **deja esa ventana emergente abierta** (el puerto cambia si se cierra).
2. Ejecuta en tu Mac:
   ```bash
   make wifi-pair
   ```
   - Si el router permite mDNS, el script detectará la `IP:PUERTO` del móvil sola y únicamente te pedirá escribir el **código de 6 dígitos**.
   - También puedes pasar ambos valores en una sola línea:
     ```bash
     make wifi-pair ADDR=192.168.1.50:39123 CODE=482910
     ```
3. Tras vincular, el script localizará y conectará automáticamente el puerto TLS de depuración (o te pedirá el puerto principal de *Depuración inalámbrica* si el router filtra mDNS).

---

## 5. Flujo 3: Puente clásico USB → Wi-Fi TCP/IP (`make wifi-from-usb`)

Ideal cuando **el router bloquea paquetes multicast mDNS**, hay subredes internas sin descubrimiento Bonjour o trabajas con un dispositivo Android 8/9/10.

1. Conecta el móvil un instante por **cable USB** (asegurándote de que tenga el Wi-Fi conectado a tu red local).
2. Ejecuta:
   ```bash
   make wifi-from-usb
   ```
   *(Opcionalmente cambia el puerto por defecto 5555 con `make wifi-from-usb PORT=5556`).*
3. El script:
   - Obtiene la IP Wi-Fi real del teléfono (`wlan0`),
   - Reinicia `adbd` en modo TCP/IP (`adb tcpip 5555`),
   - Ejecuta `adb connect <ip_movil>:5555` y verifica la sesión.
4. Cuando veas el mensaje de confirmación verde, **desconecta el cable USB** y trabaja por Wi-Fi con `make deploy` y `make logs`.
5. Al terminar tu sesión, puedes desconectar los túneles Wi-Fi con:
   ```bash
   make disconnect
   ```

---

## 6. Ejecución de Tests TDD: Local vs Dispositivo Físico

El flujo de desarrollo guiado por pruebas (TDD) del proyecto combina ciclos ultrarrápidos en la JVM del Mac con validación real en dispositivo:

| Comando | Tarea de Gradle subyacente | Cuándo usarlo |
| :--- | :--- | :--- |
| `make test` | `./gradlew testDebugUnitTest` | **Ciclo TDD continuo (Rojo → Verde → Refactor)**: ejecuta tests unitarios, lógica de dominio/conversión ASCII, ViewModels con Coroutines/Turbine y tests de Compose UI sobre Robolectric en segundos y sin necesitar móvil conectado. |
| `make test-device` | `./gradlew connectedDebugAndroidTest` | **Validación en hardware real / instrumentación**: instala el APK de pruebas y ejecuta `androidTest` sobre el móvil conectado (por USB o Wi-Fi) o sobre el emulador activo para validar permisos, intents reales de compartir, rendimiento en GPU/CPU ARM y comportamiento del sistema Android. |

Si no tienes un móvil físico a mano, puedes levantar el emulador configurado del entorno con:

```bash
make emu-start   # Arranca Pixel_9_Pro_XL y espera a que termine el boot
make deploy
make test-device
make emu-stop    # Detiene el emulador al finalizar
```

---

## 7. Espejo de pantalla en vivo con Android Device Hub (Mirroring)

Tanto los dispositivos conectados por **USB** como los conectados por **Wi-Fi** (`make wifi-qr`, `make wifi-pair` o `make wifi-from-usb`) aparecen automáticamente en el panel lateral **Android Device Hub**:

1. Asegúrate de que tu móvil figura como `autorizado` en `make doctor`.
2. Abre el panel **Android Device Hub** en el entorno de trabajo.
3. Verás tu teléfono físico (junto a cualquier emulador AVD) con estado **Online**.
4. Haz clic en **Mirror** sobre el dispositivo:
   - Verás el **vídeo en vivo** de la pantalla del teléfono directamente en tu Mac.
   - Podrás interactuar con la app usando **clics y arrastres del ratón** (toques y gestos), el **teclado del Mac** y la **rueda de desplazamiento**.
5. Mientras el *mirroring* está abierto, también es posible inspeccionar la jerarquía de Compose o tomar capturas anotadas desde terminal sin cortar la transmisión:
   ```bash
   # Árbol de elementos visibles de la UI en JSON
   android layout --device <serial> --pretty

   # Captura de pantalla con recuadros numerados de elementos interactivos
   android screen capture --device <serial> --annotate --output /tmp/screen.png
   ```

---

## 8. Selección de dispositivo y resolución de problemas

### Varios dispositivos conectados a la vez (ej. USB + Wi-Fi o móvil + emulador)
- Si ejecutas `make deploy` o `make logs` en una terminal interactiva habiendo más de un dispositivo online, aparecerá un **selector numerado** para elegir destino.
- También puedes fijar el número de serie sin preguntas pasando `SERIAL`:
  ```bash
  make deploy SERIAL=192.168.1.213:5555
  make logs SERIAL=emulator-5554
  ```

### Problemas frecuentes

| Síntoma | Causa habitual | Solución rápida |
| :--- | :--- | :--- |
| `unauthorized` en `make doctor` | No se aceptó el diálogo RSA en el móvil | Desbloquea el teléfono, marca *«Permitir siempre»* y pulsa **Permitir**. Si no sale el aviso, en *Opciones para desarrolladores* pulsa *Revocar autorizaciones de depuración USB* y reconecta el cable. |
| `make wifi-qr` agota los 90 s sin detectar el móvil | Router con *AP Isolation* (red de invitados), VPN activa en Mac/móvil o bloqueo multicast mDNS | Desactiva VPNs locales; si el router filtra mDNS usa `make wifi-pair ADDR=<ip:puerto> CODE=<6dig>` o `make wifi-from-usb`. |
| `adb pair` falla indicando puerto rechazado | El diálogo del código en el móvil se cerró o rotó la pantalla | Android asigna un **puerto aleatorio nuevo** cada vez que abres el popup de código. Ábrelo de nuevo y usa el nuevo puerto. |
| `INSTALL_FAILED_USER_RESTRICTED` al ejecutar `make deploy` | Bloqueo de seguridad MIUI / HyperOS (Xiaomi/POCO/Redmi) | Activa **Instalar vía USB** y **Depuración USB (Ajustes de seguridad)** en *Opciones de desarrollador*. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Ya existe una versión con distinta firma en el móvil | Ejecuta `adb uninstall com.laralbir.img2asciiim` y vuelve a lanzar `make deploy`. |
