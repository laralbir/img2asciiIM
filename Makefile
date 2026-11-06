# ==============================================================================
# Makefile — TDD, Compilación y Depuración Android (USB + Wi-Fi)
# Proyecto: img2asciiIM (com.laralbir.img2asciiim)
# ==============================================================================

.DEFAULT_GOAL := help

ADB_DEBUG := ./scripts/adb-debug.sh
AVD       ?= Pixel_9_Pro_XL
SERIAL    ?=
PORT      ?=
ADDR      ?=
CODE      ?=

.PHONY: help test test-device test-usb test-wifi build doctor status usb wifi-qr wifi-pair wifi-from-usb disconnect deploy deploy-usb deploy-wifi logs logs-usb logs-wifi emu-start emu-stop

help:
	@printf "\n\033[1;36m━━━ Comandos de Desarrollo y Depuración — img2asciiIM ━━━\033[0m\n\n"
	@printf "\033[1mTDD y Compilación (Gradle):\033[0m\n"
	@printf "  \033[0;32mmake test\033[0m            Ejecuta tests unitarios locales (JVM + Robolectric)\n"
	@printf "  \033[0;32mmake test-device\033[0m     Ejecuta tests instrumentados en móvil USB/Wi-Fi o emulador\n"
	@printf "  \033[0;32mmake build\033[0m           Compila el APK Debug (./gradlew assembleDebug)\n\n"
	@printf "\033[1mConexión de Dispositivos (USB y Wi-Fi):\033[0m\n"
	@printf "  \033[0;32mmake doctor\033[0m          Diagnóstico completo de adb, android CLI, red, mDNS y dispositivos\n"
	@printf "  \033[0;32mmake usb\033[0m             Espera un móvil por cable USB y verifica autorización RSA\n"
	@printf "  \033[0;32mmake wifi-qr\033[0m         [Android 11+] Vincula y conecta por Wi-Fi escaneando QR en terminal\n"
	@printf "  \033[0;32mmake wifi-pair\033[0m       [Android 11+] Vincula y conecta por Wi-Fi con código de 6 dígitos\n"
	@printf "  \033[0;32mmake wifi-from-usb\033[0m   Puente clásico USB → Wi-Fi TCP/IP (puerto 5555) y soltar cable\n"
	@printf "  \033[0;32mmake disconnect\033[0m      Desconecta sesiones ADB activas por Wi-Fi\n\n"
	@printf "\033[1mDespliegue, Logs y Emulador:\033[0m\n"
	@printf "  \033[0;32mmake deploy\033[0m          Compila, instala y abre la app [SERIAL=serial|pixel10|--usb|--wifi]\n"
	@printf "  \033[0;32mmake deploy-usb\033[0m      Compila, instala y abre la app directamente en el móvil por USB\n"
	@printf "  \033[0;32mmake deploy-wifi\033[0m     Compila, instala y abre la app directamente en el móvil por Wi-Fi\n"
	@printf "  \033[0;32mmake logs\033[0m            Muestra logcat en vivo filtrado por PID de la app [SERIAL=...]\n"
	@printf "  \033[0;32mmake logs-usb\033[0m        Muestra logcat en vivo del móvil conectado por USB\n"
	@printf "  \033[0;32mmake logs-wifi\033[0m       Muestra logcat en vivo del móvil conectado por Wi-Fi\n"
	@printf "  \033[0;32mmake emu-start\033[0m       Arranca el emulador Android ($(AVD))\n"
	@printf "  \033[0;32mmake emu-stop\033[0m        Detiene el emulador Android ($(AVD))\n\n"
	@printf "\033[2mGuía detallada paso a paso: docs/DEBUGGING.md\033[0m\n\n"

test:
	./gradlew testDebugUnitTest

test-device:
	./gradlew connectedDebugAndroidTest

test-usb:
	ANDROID_SERIAL="$$($(ADB_DEBUG) serial --usb)" ./gradlew connectedDebugAndroidTest

test-wifi:
	ANDROID_SERIAL="$$($(ADB_DEBUG) serial --wifi)" ./gradlew connectedDebugAndroidTest

build:
	./gradlew assembleDebug

doctor:
	$(ADB_DEBUG) doctor

status:
	$(ADB_DEBUG) status

usb:
	$(ADB_DEBUG) usb

wifi-qr:
	$(ADB_DEBUG) wifi-qr

wifi-pair:
	$(ADB_DEBUG) wifi-pair $(ADDR) $(CODE)

wifi-from-usb:
	$(ADB_DEBUG) wifi-from-usb $(PORT)

disconnect:
	$(ADB_DEBUG) disconnect $(ADDR)

deploy:
	$(ADB_DEBUG) deploy $(SERIAL)

deploy-usb:
	$(ADB_DEBUG) deploy --usb

deploy-wifi:
	$(ADB_DEBUG) deploy --wifi

logs:
	$(ADB_DEBUG) logs $(SERIAL)

logs-usb:
	$(ADB_DEBUG) logs --usb

logs-wifi:
	$(ADB_DEBUG) logs --wifi

emu-start:
	android emulator start $(AVD)

emu-stop:
	android emulator stop $(AVD)
