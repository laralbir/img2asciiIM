#!/usr/bin/env bash
# ==============================================================================
# scripts/adb-debug.sh — Entorno de Depuración Android (USB + Wi-Fi) en macOS
# Proyecto: img2asciiIM (com.laralbir.img2asciiim)
# ==============================================================================
set -euo pipefail

APP_ID="com.laralbir.img2asciiim"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ------------------------------------------------------------------------------
# Colores y formato de salida en terminal
# ------------------------------------------------------------------------------
if [[ -z "${NO_COLOR:-}" && -t 1 ]]; then
  BOLD=$'\033[1m'
  DIM=$'\033[2m'
  RED=$'\033[0;31m'
  GREEN=$'\033[0;32m'
  YELLOW=$'\033[1;33m'
  BLUE=$'\033[0;34m'
  MAGENTA=$'\033[0;35m'
  CYAN=$'\033[0;36m'
  RESET=$'\033[0m'
else
  BOLD=""
  DIM=""
  RED=""
  GREEN=""
  YELLOW=""
  BLUE=""
  MAGENTA=""
  CYAN=""
  RESET=""
fi

header() { printf "\n${BOLD}${CYAN}━━━ %s ━━━${RESET}\n" "$1"; }
info()   { printf "${BLUE}ℹ${RESET}  %s\n" "$1"; }
step()   { printf "${MAGENTA}➜${RESET}  %s\n" "$1"; }
ok()     { printf "${GREEN}✔${RESET}  %s\n" "$1"; }
warn()   { printf "${YELLOW}⚠${RESET}  %s\n" "$1"; }
err()    { printf "${RED}✖${RESET}  %s\n" "$1" >&2; }

# ------------------------------------------------------------------------------
# Resolución de ANDROID_HOME y PATH (adb + android CLI)
# ------------------------------------------------------------------------------
ORIG_ANDROID_HOME="${ANDROID_HOME:-}"

resolve_sdk_env() {
  local sdk_candidate="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

  if [[ -z "$sdk_candidate" && -f "${PROJECT_ROOT}/local.properties" ]]; then
    sdk_candidate="$(awk -F'=' '/^sdk\.dir=/{print $2}' "${PROJECT_ROOT}/local.properties" | tr -d '\r' | tail -n 1 || true)"
  fi
  if [[ -z "$sdk_candidate" && -d "${HOME}/Library/Android/sdk" ]]; then
    sdk_candidate="${HOME}/Library/Android/sdk"
  fi

  if [[ -n "$sdk_candidate" && -d "$sdk_candidate" ]]; then
    export ANDROID_HOME="$sdk_candidate"
    case ":${PATH}:" in
      *":${ANDROID_HOME}/platform-tools:"*) ;;
      *) export PATH="${ANDROID_HOME}/platform-tools:${PATH}" ;;
    esac
  fi

  if [[ -d "/opt/homebrew/bin" ]]; then
    case ":${PATH}:" in
      *":/opt/homebrew/bin:"*) ;;
      *) export PATH="/opt/homebrew/bin:${PATH}" ;;
    esac
  fi
}

require_adb() {
  if ! command -v adb >/dev/null 2>&1; then
    err "No se encontró 'adb' en el PATH ni en ANDROID_HOME/platform-tools."
    err "Verifica la instalación del Android SDK (por ejemplo en ~/Library/Android/sdk)."
    exit 1
  fi
  adb start-server >/dev/null 2>&1 || true
}

# ------------------------------------------------------------------------------
# Utilidades de red y clasificación de dispositivos
# ------------------------------------------------------------------------------
get_mac_lan_info() {
  local default_if=""
  default_if="$(route -n get default 2>/dev/null | awk '/interface:/{print $2}' | head -n 1 || true)"

  local candidates=()
  [[ -n "$default_if" ]] && candidates+=("$default_if")
  candidates+=("en0" "en1" "en2")

  local seen=":"
  local iface ip
  for iface in "${candidates[@]}"; do
    [[ "$seen" == *":${iface}:"* ]] && continue
    seen="${seen}${iface}:"
    ip="$(ipconfig getifaddr "$iface" 2>/dev/null || true)"
    if [[ -n "$ip" ]]; then
      printf "%s (%s)" "$ip" "$iface"
      return 0
    fi
  done
  return 1
}

classify_serial() {
  local serial="$1"
  if [[ "$serial" == emulator-* ]]; then
    printf "emulator"
  elif [[ "$serial" == *"_adb-tls-"* || "$serial" == *:* ]]; then
    printf "wifi"
  else
    printf "usb"
  fi
}

get_device_prop() {
  local serial="$1"
  local prop="$2"
  adb -s "$serial" shell -n getprop "$prop" 2>/dev/null | tr -d '\r' | head -n 1 || true
}

print_device_card() {
  local serial="$1"
  local label="${2:-Dispositivo conectado}"
  local brand model release sdk
  brand="$(get_device_prop "$serial" ro.product.manufacturer)"
  model="$(get_device_prop "$serial" ro.product.model)"
  release="$(get_device_prop "$serial" ro.build.version.release)"
  sdk="$(get_device_prop "$serial" ro.build.version.sdk)"

  ok "${BOLD}${label}${RESET}"
  printf "   • Serie (serial) : ${CYAN}%s${RESET}\n" "$serial"
  printf "   • Modelo         : %s %s\n" "${brand:-Android}" "${model:-Desconocido}"
  printf "   • Android / API  : Android %s (API %s)\n" "${release:-?}" "${sdk:-?}"
}

print_rsa_instructions() {
  local bad_serials="$1"
  printf "\n${YELLOW}${BOLD}┌──────────────────────────────────────────────────────────────────────────┐${RESET}\n"
  printf "${YELLOW}${BOLD}│ ATENCIÓN: Dispositivo bloqueado / sin autorizar (%s)${RESET}\n" "$bad_serials"
  printf "${YELLOW}${BOLD}└──────────────────────────────────────────────────────────────────────────┘${RESET}\n"
  warn "Sigue estos pasos en tu móvil Android:"
  printf "   1. Desbloquea la pantalla del teléfono.\n"
  printf "   2. En el aviso ${BOLD}«¿Permitir depuración por USB?»${RESET} (clave RSA), marca\n"
  printf "      ${BOLD}«Permitir siempre desde este ordenador»${RESET} y pulsa ${GREEN}${BOLD}Permitir${RESET}.\n"
  printf "   3. Si no aparece la ventana emergente:\n"
  printf "      • Cambia el modo USB a «Transferencia de archivos / Android Auto».\n"
  printf "      • En ${DIM}Opciones para desarrolladores${RESET}, pulsa ${BOLD}«Revocar autorizaciones de depuración USB»${RESET},\n"
  printf "        desconecta y vuelve a conectar el cable, o ejecuta: ${CYAN}adb reconnect offline${RESET}\n"
}

# Devuelve líneas "instance<TAB>type<TAB>endpoint" de adb mdns services
list_mdns_records() {
  adb mdns services 2>/dev/null | awk 'NR > 1 && NF >= 3 { print $1 "\t" $2 "\t" $3 }' || true
}

# Busca un endpoint IPv4 (o cualquier endpoint de respaldo) en adb mdns services
# Uso: find_mdns_endpoint <tipo_parcial> [nombre_instancia_exacto] [ip_preferida]
find_mdns_endpoint() {
  local wanted_type="$1"
  local wanted_name="${2:-}"
  local wanted_ip="${3:-}"
  local records
  records="$(list_mdns_records)"
  [[ -z "$records" ]] && return 1

  local best_v4="" best_any=""
  local inst stype ep
  while IFS=$'\t' read -r inst stype ep; do
    [[ -z "$inst" ]] && continue
    [[ "$stype" != *"${wanted_type}"* ]] && continue
    if [[ -n "$wanted_name" && "$inst" != "$wanted_name" ]]; then
      continue
    fi
    if [[ -n "$wanted_ip" && "$ep" != "${wanted_ip}:"* ]]; then
      continue
    fi
    if [[ "$ep" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+$ ]]; then
      best_v4="$ep"
      break
    elif [[ -z "$best_any" ]]; then
      best_any="$ep"
    fi
  done <<< "$records"

  if [[ -n "$best_v4" ]]; then
    printf "%s" "$best_v4"
    return 0
  elif [[ -n "$best_any" ]]; then
    printf "%s" "$best_any"
    return 0
  fi
  return 1
}

# Tras un 'adb pair' exitoso, descubre y conecta el puerto de _adb-tls-connect._tcp
auto_connect_after_pair() {
  local phone_ip="$1"
  step "Buscando puerto de conexión inalámbrica (_adb-tls-connect._tcp) para ${phone_ip}..."

  local attempt connected_serial="" connect_ep=""
  for attempt in 1 2 3 4 5 6 7 8; do
    # 1. ¿adb ya auto-conectó mediante ADB_MDNS_AUTO_CONNECT?
    connected_serial="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}' | grep -E "_adb-tls-connect|^${phone_ip}:" | head -n 1 || true)"
    if [[ -n "$connected_serial" ]]; then
      ok "¡Conexión TLS establecida automáticamente!"
      print_device_card "$connected_serial" "Dispositivo Wi-Fi listo para depurar"
      return 0
    fi

    # 2. ¿Aparece _adb-tls-connect._tcp en adb mdns services?
    connect_ep="$(find_mdns_endpoint "_adb-tls-connect._tcp" "" "$phone_ip" || find_mdns_endpoint "_adb-tls-connect._tcp" "" "" || true)"
    if [[ -n "$connect_ep" ]]; then
      step "Servicio detectado en ${BOLD}${connect_ep}${RESET}. Ejecutando 'adb connect'..."
      local out
      out="$(adb connect "$connect_ep" 2>&1 || true)"
      printf "   %s\n" "$out"
      sleep 1
      connected_serial="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}' | grep -E "^${connect_ep}$|_adb-tls-connect|^${phone_ip}:" | head -n 1 || true)"
      if [[ -n "$connected_serial" ]]; then
        print_device_card "$connected_serial" "Dispositivo Wi-Fi listo para depurar"
        return 0
      fi
    fi
    sleep 1
  done

  warn "El router no propagó el anuncio mDNS de conexión (_adb-tls-connect._tcp)."
  if [[ -t 0 ]]; then
    printf "\nEn la pantalla principal de ${BOLD}Depuración inalámbrica${RESET} de tu móvil verás\n"
    printf "el apartado ${BOLD}«Dirección IP y puerto»${RESET} (distinto al puerto de vinculación).\n"
    local manual_target=""
    read -r -p "Introduce PUERTO (ej. 41523) o IP:PUERTO (ej. ${phone_ip}:41523): " manual_target
    manual_target="${manual_target//[[:space:]]/}"
    if [[ "$manual_target" =~ ^[0-9]+$ && -n "$phone_ip" ]]; then
      manual_target="${phone_ip}:${manual_target}"
    fi
    if [[ -n "$manual_target" ]]; then
      step "Conectando a ${manual_target}..."
      adb connect "$manual_target"
      sleep 1
      if adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -Fxq "$manual_target"; then
        print_device_card "$manual_target" "Dispositivo Wi-Fi listo para depurar"
        return 0
      fi
    fi
  fi

  err "No se pudo completar la conexión final. Ejecuta manualmente: adb connect ${phone_ip}:<PUERTO>"
  return 1
}

# Selección inteligente de dispositivo objetivo para deploy / logs
# Acepta: --usb | --wifi | --emulator | <serial exacto> | <subcadena del modelo, ej. "pixel 10" o "pixel10">
select_target_device() {
  local requested="${1:-${ANDROID_SERIAL:-}}"

  local online_lines=()
  local bad_list=()
  local line serial state
  while IFS= read -r line; do
    [[ -z "$line" || "$line" == "List of devices"* || "$line" == \** ]] && continue
    serial="$(awk '{print $1}' <<< "$line")"
    state="$(awk '{print $2}' <<< "$line")"
    [[ -z "$serial" ]] && continue
    if [[ "$state" == "device" ]]; then
      online_lines+=("$serial")
    elif [[ "$state" == "unauthorized" || "$state" == "offline" ]]; then
      bad_list+=("${serial}(${state})")
    fi
  done < <(adb devices)

  if [[ -n "$requested" ]]; then
    case "$requested" in
      --usb|usb)
        local s
        for s in "${online_lines[@]+"${online_lines[@]}"}"; do
          [[ "$(classify_serial "$s")" == "usb" ]] && { printf "%s" "$s"; return 0; }
        done
        err "No hay ningún dispositivo Android conectado y autorizado por cable USB ahora mismo."
        [[ ${#bad_list[@]} -gt 0 ]] && print_rsa_instructions "${bad_list[*]}" >&2
        return 1
        ;;
      --wifi|wifi)
        local s
        for s in "${online_lines[@]+"${online_lines[@]}"}"; do
          [[ "$(classify_serial "$s")" == "wifi" ]] && { printf "%s" "$s"; return 0; }
        done
        err "No hay ningún dispositivo Android conectado por Wi-Fi ahora mismo."
        return 1
        ;;
      --emulator|emulator)
        local s
        for s in "${online_lines[@]+"${online_lines[@]}"}"; do
          [[ "$(classify_serial "$s")" == "emulator" ]] && { printf "%s" "$s"; return 0; }
        done
        err "No hay ningún emulador Android en ejecución ahora mismo."
        return 1
        ;;
    esac

    local st
    st="$(adb devices | awk -v s="$requested" 'NR>1 && $1==s {print $2}' | head -n 1 || true)"
    if [[ "$st" == "device" ]]; then
      printf "%s" "$requested"
      return 0
    elif [[ "$st" == "unauthorized" || "$st" == "offline" ]]; then
      print_rsa_instructions "$requested ($st)" >&2
      return 1
    fi

    # Búsqueda flexible por nombre de modelo (ej. "pixel 10", "pixel10", "pixel_10")
    local norm_req norm_model s model
    norm_req="$(printf "%s" "$requested" | tr '[:upper:]' '[:lower:]' | tr -d ' _-')"
    for s in "${online_lines[@]+"${online_lines[@]}"}"; do
      model="$(get_device_prop "$s" ro.product.model)"
      norm_model="$(printf "%s" "$model" | tr '[:upper:]' '[:lower:]' | tr -d ' _-')"
      if [[ -n "$norm_req" && "$norm_model" == *"$norm_req"* ]]; then
        printf "%s" "$s"
        return 0
      fi
    done

    err "El dispositivo '${requested}' no coincide con ningún serial ni modelo conectado."
    err "Ejecuta './scripts/adb-debug.sh doctor' para ver los dispositivos disponibles."
    return 1
  fi

  if [[ ${#online_lines[@]} -eq 0 ]]; then
    err "No hay ningún dispositivo Android activo y autorizado en este momento."
    if [[ ${#bad_list[@]} -gt 0 ]]; then
      print_rsa_instructions "${bad_list[*]}" >&2
    else
      printf "   Conecta un móvil o inicia el emulador con uno de estos comandos:\n" >&2
      printf "   • Cable USB          : ${CYAN}make usb${RESET}\n" >&2
      printf "   • Wi-Fi QR (Andr 11+): ${CYAN}make wifi-qr${RESET}\n" >&2
      printf "   • Wi-Fi Código 6 díg : ${CYAN}make wifi-pair${RESET}\n" >&2
      printf "   • Emulador local     : ${CYAN}make emu-start${RESET}\n" >&2
    fi
    return 1
  fi

  if [[ ${#online_lines[@]} -eq 1 ]]; then
    printf "%s" "${online_lines[0]}"
    return 0
  fi

  # Varios dispositivos activos: menú interactivo en TTY o prioridad física (USB > Wi-Fi > Emulador)
  if [[ -t 0 && -t 2 ]]; then
    printf "\n${BOLD}Hay varios dispositivos conectados:${RESET}\n" >&2
    local i=1 s kind model
    for s in "${online_lines[@]}"; do
      kind="$(classify_serial "$s")"
      model="$(get_device_prop "$s" ro.product.model)"
      printf "  [${CYAN}%d${RESET}] %-36s (%s — %s)\n" "$i" "$s" "$kind" "${model:-Android}" >&2
      i=$((i + 1))
    done
    local choice=""
    read -r -p "Selecciona dispositivo [1-${#online_lines[@]}, Enter=1]: " choice </dev/tty >&2 || true
    choice="${choice:-1}"
    if [[ "$choice" =~ ^[0-9]+$ ]] && (( choice >= 1 && choice <= ${#online_lines[@]} )); then
      printf "%s" "${online_lines[$((choice - 1))]}"
      return 0
    fi
    err "Selección inválida: ${choice}"
    return 1
  fi

  # Modo no interactivo: priorizar USB > Wi-Fi > Emulador
  local s
  for s in "${online_lines[@]}"; do
    [[ "$(classify_serial "$s")" == "usb" ]] && { printf "%s" "$s"; return 0; }
  done
  for s in "${online_lines[@]}"; do
    [[ "$(classify_serial "$s")" == "wifi" ]] && { printf "%s" "$s"; return 0; }
  done
  printf "%s" "${online_lines[0]}"
}

# ------------------------------------------------------------------------------
# Generador QR ISO/IEC 18004 autocontenido en Python 3 (cero dependencias pip)
# ------------------------------------------------------------------------------
render_qr_terminal() {
  local payload="$1"
  if ! command -v python3 >/dev/null 2>&1; then
    err "Se requiere 'python3' para renderizar el código QR en la terminal."
    err "Alternativa inmediata: usa './scripts/adb-debug.sh wifi-pair' (código de 6 dígitos)."
    return 1
  fi

  python3 - "$payload" <<'PY'
import os
import sys

VERSIONS_L = {
    1: (21, 26, 7, 1, []),
    2: (25, 44, 10, 1, [6, 18]),
    3: (29, 70, 15, 1, [6, 22]),
    4: (33, 100, 20, 1, [6, 26]),
    5: (37, 134, 26, 1, [6, 30]),
    6: (41, 172, 18, 2, [6, 34]),
}

EXP = [0] * 512
LOG = [0] * 256
x = 1
for i in range(255):
    EXP[i] = x
    LOG[x] = i
    x <<= 1
    if x & 0x100:
        x ^= 0x11D
for i in range(255, 512):
    EXP[i] = EXP[i - 255]

def gf_mul(a, b):
    return 0 if (a == 0 or b == 0) else EXP[LOG[a] + LOG[b]]

def rs_encode(data, nsym):
    gen = [1]
    for i in range(nsym):
        nxt = [0] * (len(gen) + 1)
        for j, c in enumerate(gen):
            nxt[j] ^= c
            nxt[j + 1] ^= gf_mul(c, EXP[i])
        gen = nxt
    res = list(data) + [0] * nsym
    for i in range(len(data)):
        coef = res[i]
        if coef != 0:
            for j in range(1, len(gen)):
                res[i + j] ^= gf_mul(gen[j], coef)
    return res[len(data):]

def encode_qr(payload):
    ver = next((v for v, (_, tot, ec, nb, _) in VERSIONS_L.items() if len(payload) + 2 <= tot - ec * nb), None)
    if ver is None:
        raise ValueError("Payload QR demasiado largo")
    size, total_cw, ec_cw, num_blocks, aligns = VERSIONS_L[ver]
    data_cw_total = total_cw - ec_cw * num_blocks

    bits = [0, 1, 0, 0]
    for i in range(7, -1, -1):
        bits.append((len(payload) >> i) & 1)
    for b in payload:
        for i in range(7, -1, -1):
            bits.append((b >> i) & 1)
    bits.extend([0] * min(4, data_cw_total * 8 - len(bits)))
    while len(bits) % 8:
        bits.append(0)

    codewords = []
    for i in range(0, len(bits), 8):
        v = 0
        for bit in bits[i:i + 8]:
            v = (v << 1) | bit
        codewords.append(v)
    pads = (0xEC, 0x11)
    while len(codewords) < data_cw_total:
        codewords.append(pads[len(codewords) & 1])  # parity irrelevant: alternate 0xEC, 0x11 from start of pad
    # Ensure strict ISO pad alternation starting with 0xEC
    raw_len = len(bits) // 8
    for idx in range(raw_len, data_cw_total):
        codewords[idx] = pads[(idx - raw_len) % 2]

    bsz = data_cw_total // num_blocks
    dblocks, eblocks = [], []
    for b in range(num_blocks):
        blk = codewords[b * bsz:(b + 1) * bsz]
        dblocks.append(blk)
        eblocks.append(rs_encode(blk, ec_cw))

    final_cw = [blk[i] for i in range(bsz) for blk in dblocks] + [blk[i] for i in range(ec_cw) for blk in eblocks]
    final_bits = [(cw >> i) & 1 for cw in final_cw for i in range(7, -1, -1)]
    if ver >= 2:
        final_bits.extend([0] * 7)

    grid = [[0] * size for _ in range(size)]
    is_func = [[False] * size for _ in range(size)]

    def set_f(r, c, val):
        if 0 <= r < size and 0 <= c < size:
            grid[r][c] = val
            is_func[r][c] = True

    def finder(r0, c0):
        for dr in range(-1, 8):
            for dc in range(-1, 8):
                val = 1 if (0 <= dr <= 6 and 0 <= dc <= 6 and (dr in (0, 6) or dc in (0, 6) or (2 <= dr <= 4 and 2 <= dc <= 4))) else 0
                set_f(r0 + dr, c0 + dc, val)

    finder(0, 0)
    finder(0, size - 7)
    finder(size - 7, 0)

    for ar in aligns:
        for ac in aligns:
            if (ar == 6 and ac == 6) or (ar == 6 and ac == size - 7) or (ar == size - 7 and ac == 6):
                continue
            for dr in range(-2, 3):
                for dc in range(-2, 3):
                    set_f(ar + dr, ac + dc, 1 if (abs(dr) == 2 or abs(dc) == 2 or (dr == 0 and dc == 0)) else 0)

    for i in range(8, size - 8):
        set_f(6, i, 1 if i % 2 == 0 else 0)
        set_f(i, 6, 1 if i % 2 == 0 else 0)

    set_f(size - 8, 8, 1)
    for i in range(9):
        if not is_func[8][i]: set_f(8, i, 0)
        if not is_func[i][8]: set_f(i, 8, 0)
    for i in range(8):
        if not is_func[8][size - 1 - i]: set_f(8, size - 1 - i, 0)
        if not is_func[size - 1 - i][8]: set_f(size - 1 - i, 8, 0)

    bi, upward, col = 0, True, size - 1
    while col > 0:
        if col == 6:
            col -= 1
        for r in (range(size - 1, -1, -1) if upward else range(size)):
            for dc in (0, -1):
                c = col + dc
                if not is_func[r][c]:
                    grid[r][c] = final_bits[bi] if bi < len(final_bits) else 0
                    bi += 1
        upward = not upward
        col -= 2

    def write_fmt(g, mask):
        d5 = (0b01 << 3) | mask
        rem = d5 << 10
        for i in range(14, 9, -1):
            if (rem >> i) & 1:
                rem ^= 0x537 << (i - 10)
        fmt = ((d5 << 10) | rem) ^ 0x5412
        b15 = [(fmt >> i) & 1 for i in range(15)]
        coords = [(8,0),(8,1),(8,2),(8,3),(8,4),(8,5),(8,7),(8,8),(7,8),(5,8),(4,8),(3,8),(2,8),(1,8),(0,8)]
        for i, (r, c) in enumerate(coords):
            g[r][c] = b15[14 - i]
        for i in range(7):
            g[size - 1 - i][8] = b15[14 - i]
        for i in range(8):
            g[8][size - 8 + i] = b15[7 - i]

    masks = [
        lambda r, c: (r + c) % 2 == 0,
        lambda r, c: r % 2 == 0,
        lambda r, c: c % 3 == 0,
        lambda r, c: (r + c) % 3 == 0,
        lambda r, c: (r // 2 + c // 3) % 2 == 0,
        lambda r, c: (r * c) % 2 + (r * c) % 3 == 0,
        lambda r, c: ((r * c) % 2 + (r * c) % 3) % 2 == 0,
        lambda r, c: ((r + c) % 2 + (r * c) % 3) % 2 == 0,
    ]

    def penalty(g):
        sc = 0
        for r in range(size):
            rv, rl = g[r][0], 1
            for c in range(1, size):
                if g[r][c] == rv: rl += 1
                else:
                    if rl >= 5: sc += 3 + (rl - 5)
                    rv, rl = g[r][c], 1
            if rl >= 5: sc += 3 + (rl - 5)
        for c in range(size):
            rv, rl = g[0][c], 1
            for r in range(1, size):
                if g[r][c] == rv: rl += 1
                else:
                    if rl >= 5: sc += 3 + (rl - 5)
                    rv, rl = g[r][c], 1
            if rl >= 5: sc += 3 + (rl - 5)
        for r in range(size - 1):
            for c in range(size - 1):
                v = g[r][c]
                if g[r][c+1] == v and g[r+1][c] == v and g[r+1][c+1] == v:
                    sc += 3
        return sc

    best_g, best_sc = None, 10**9
    for m, fn in enumerate(masks):
        cand = [row[:] for row in grid]
        for r in range(size):
            for c in range(size):
                if not is_func[r][c] and fn(r, c):
                    cand[r][c] ^= 1
        write_fmt(cand, m)
        sc = penalty(cand)
        if sc < best_sc:
            best_g, best_sc = cand, sc
    return best_g

mat = encode_qr(sys.argv[1].encode("utf-8"))
border = 4
n = len(mat)
full = n + border * 2
pad = [[0] * full for _ in range(full)]
for r in range(n):
    for c in range(n):
        pad[r + border][c + border] = mat[r][c]

use_ansi = sys.stdout.isatty() and not os.environ.get("NO_COLOR")
prefix = "   \033[38;5;231;48;5;16m" if use_ansi else "   "
suffix = "\033[0m" if use_ansi else ""
for r in range(0, full, 2):
    top = pad[r]
    bot = pad[r + 1] if r + 1 < full else [0] * full
    # Bloque lleno ('█') = zona blanca (#ffffff), espacio (' ') = módulo negro (#000000):
    # funciona tanto en terminales claras como oscuras (con ANSI) y también si se filtran códigos ANSI.
    chars = []
    for c in range(full):
        t, b = 1 - top[c], 1 - bot[c]
        chars.append("█" if (t and b) else "▀" if (t and not b) else "▄" if (not t and b) else " ")
    print(prefix + "".join(chars) + suffix)
PY
}

# ==============================================================================
# SUBCOMANDOS
# ==============================================================================

cmd_help() {
  cat <<EOF
${BOLD}${CYAN} Herramienta de Depuración Android (USB + Wi-Fi) — img2asciiIM ${RESET}

${BOLD}USO:${RESET}
  ./scripts/adb-debug.sh <subcomando> [argumentos]

${BOLD}SUBCOMANDOS DISPONIBLES:${RESET}
  ${GREEN}doctor${RESET} | ${GREEN}status${RESET}
      Verifica adb, android CLI, ANDROID_HOME, IP Wi-Fi del Mac, soporte mDNS,
      dispositivos USB/Wi-Fi conectados, servicios mDNS y emuladores AVD.

  ${GREEN}usb${RESET}
      Espera un móvil por cable USB, comprueba la autorización RSA y muestra
      modelo, versión de Android, nivel de API y número de serie.

  ${GREEN}wifi-qr${RESET}
      [Android 11+] Genera un código QR en la terminal para «Depuración inalámbrica»,
      detecta el escaneo por mDNS, vincula y conecta automáticamente sin cables.

  ${GREEN}wifi-pair${RESET} [ip:puerto] [codigo]
      [Android 11+] Vincula por código de 6 dígitos (autodescubre IP:puerto vía mDNS
      si se omite) y conecta automáticamente al puerto TLS de depuración.

  ${GREEN}wifi-from-usb${RESET} [puerto]
      Puente clásico USB → Wi-Fi TCP/IP (puerto por defecto: 5555). Ideal si el router
      bloquea mDNS o en versiones antiguas de Android. Permite soltar el cable al terminar.

  ${GREEN}disconnect${RESET} [ip:puerto]
      Cierra las sesiones ADB por Wi-Fi activas ('adb disconnect').

  ${GREEN}deploy${RESET} [serial]
      Compila, instala y lanza ${APP_ID} en modo Debug sobre el dispositivo
      USB, Wi-Fi o emulador seleccionado.

  ${GREEN}logs${RESET} [serial]
      Muestra 'adb logcat' en vivo filtrado dinámicamente por el PID de
      ${APP_ID} (con respaldo por paquete si la app aún no arrancó).

${BOLD}EJEMPLOS RÁPIDOS:${RESET}
  ./scripts/adb-debug.sh doctor
  ./scripts/adb-debug.sh wifi-qr
  ./scripts/adb-debug.sh wifi-pair 192.168.1.50:39123 482910
  ./scripts/adb-debug.sh wifi-from-usb
  ./scripts/adb-debug.sh deploy
  ./scripts/adb-debug.sh logs
EOF
}

cmd_doctor() {
  header "Diagnóstico del Entorno Android (USB + Wi-Fi)"

  # 1. Herramientas en el Mac
  printf "\n${BOLD}1. Herramientas y Red del Host (macOS):${RESET}\n"
  if command -v adb >/dev/null 2>&1; then
    local adb_path adb_ver
    adb_path="$(command -v adb)"
    adb_ver="$(adb --version | head -n 2 | tr '\n' ' ' | sed 's/  */ /g')"
    ok "adb          : ${adb_path} ${DIM}(${adb_ver% })${RESET}"
    adb start-server >/dev/null 2>&1 || true
  else
    err "adb          : NO ENCONTRADO (instala platform-tools en Android SDK)"
  fi

  if command -v android >/dev/null 2>&1; then
    local android_path android_ver
    android_path="$(command -v android)"
    android_ver="$(android --version 2>/dev/null | head -n 1 || echo "instalado")"
    ok "android CLI  : ${android_path} ${DIM}(${android_ver})${RESET}"
  else
    warn "android CLI  : No encontrado en PATH (se usará './gradlew' como respaldo)"
  fi

  if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}" ]]; then
    if [[ -n "$ORIG_ANDROID_HOME" ]]; then
      ok "ANDROID_HOME : ${ANDROID_HOME} ${DIM}(exportado en entorno)${RESET}"
    else
      ok "ANDROID_HOME : ${ANDROID_HOME} ${DIM}(autodetectado vía local.properties/SDK)${RESET}"
    fi
  else
    warn "ANDROID_HOME : No configurado o directorio inexistente"
  fi

  local lan_info=""
  if lan_info="$(get_mac_lan_info)"; then
    ok "IP Wi-Fi Mac : ${lan_info} ${DIM}(el móvil debe estar en esta misma red local)${RESET}"
  else
    warn "IP Wi-Fi Mac : Sin IP local activa en en0/en1 (necesaria para depuración por Wi-Fi)"
  fi

  if command -v adb >/dev/null 2>&1; then
    local mdns_out=""
    if mdns_out="$(adb mdns check 2>&1)"; then
      ok "ADB mDNS     : ${mdns_out}"
    else
      warn "ADB mDNS     : No disponible (${mdns_out}) — usa 'wifi-from-usb' o IP manual"
    fi
  fi

  # 2. Clasificación de dispositivos conectados en adb devices -l
  local usb_items=() wifi_items=() emu_running=() unauthorized_items=()
  if command -v adb >/dev/null 2>&1; then
    local line serial state rest kind model extra
    while IFS= read -r line; do
      [[ -z "$line" || "$line" == "List of devices"* || "$line" == \** ]] && continue
      serial="$(awk '{print $1}' <<< "$line")"
      state="$(awk '{print $2}' <<< "$line")"
      rest="$(cut -d' ' -f3- <<< "$line" | sed 's/^ *//')"
      [[ -z "$serial" ]] && continue

      kind="$(classify_serial "$serial")"
      if [[ "$state" == "device" ]]; then
        model="$(get_device_prop "$serial" ro.product.model)"
        local rel sdk
        rel="$(get_device_prop "$serial" ro.build.version.release)"
        sdk="$(get_device_prop "$serial" ro.build.version.sdk)"
        extra="${GREEN}autorizado${RESET} — ${BOLD}${model:-Android}${RESET} (Android ${rel:-?}, API ${sdk:-?})"
      elif [[ "$state" == "unauthorized" ]]; then
        extra="${RED}${BOLD}NO AUTORIZADO (acepta el diálogo RSA en el móvil)${RESET}"
        unauthorized_items+=("${serial} [unauthorized]")
      elif [[ "$state" == "offline" ]]; then
        extra="${YELLOW}${BOLD}OFFLINE (reconecta el cable o ejecuta 'adb reconnect offline')${RESET}"
        unauthorized_items+=("${serial} [offline]")
      else
        extra="${YELLOW}${state}${RESET} ${DIM}${rest}${RESET}"
      fi

      case "$kind" in
        usb)      usb_items+=("${CYAN}${serial}${RESET} → ${extra}") ;;
        wifi)     wifi_items+=("${CYAN}${serial}${RESET} → ${extra}") ;;
        emulator) emu_running+=("${CYAN}${serial}${RESET} → ${extra}") ;;
      esac
    done < <(adb devices -l 2>/dev/null || true)
  fi

  printf "\n${BOLD}2. Dispositivos Físicos por Cable USB:${RESET}\n"
  if [[ ${#usb_items[@]} -gt 0 ]]; then
    local item
    for item in "${usb_items[@]}"; do printf "   • %s\n" "$item"; done
  else
    printf "   ${DIM}(Ningún dispositivo USB conectado)${RESET}\n"
  fi

  printf "\n${BOLD}3. Dispositivos Físicos por Wi-Fi (TLS / TCP 5555):${RESET}\n"
  if [[ ${#wifi_items[@]} -gt 0 ]]; then
    local item
    for item in "${wifi_items[@]}"; do printf "   • %s\n" "$item"; done
  else
    printf "   ${DIM}(Ningún dispositivo conectado actualmente por Wi-Fi)${RESET}\n"
  fi

  printf "\n${BOLD}4. Servicios mDNS Descubiertos en la Red ('adb mdns services'):${RESET}\n"
  local mdns_records=""
  mdns_records="$(list_mdns_records)"
  if [[ -n "$mdns_records" ]]; then
    local inst stype ep hint
    while IFS=$'\t' read -r inst stype ep; do
      [[ -z "$inst" ]] && continue
      hint=""
      case "$stype" in
        *"_adb-tls-pairing._tcp"*) hint="${MAGENTA}[Listo para vincular: make wifi-pair]${RESET}" ;;
        *"_adb-tls-connect._tcp"*) hint="${GREEN}[Depuración inalámbrica activa: adb connect ${ep}]${RESET}" ;;
        *"_adb._tcp"*)             hint="${CYAN}[TCP/IP clásico activo: adb connect ${ep}]${RESET}" ;;
      esac
      printf "   • %-30s %-24s ${BOLD}%s${RESET} %s\n" "$inst" "$stype" "$ep" "$hint"
    done <<< "$mdns_records"
  else
    printf "   ${DIM}(Ningún servicio ADB anunciado por mDNS ahora mismo)${RESET}\n"
  fi

  printf "\n${BOLD}5. Emuladores Android (AVDs):${RESET}\n"
  if command -v android >/dev/null 2>&1; then
    local avds=""
    avds="$(android emulator list 2>/dev/null | sed '/^[[:space:]]*$/d' || true)"
    if [[ -n "$avds" ]]; then
      local avd
      while IFS= read -r avd; do
        printf "   • AVD disponible : ${BOLD}%s${RESET} ${DIM}(iniciar con: make emu-start)${RESET}\n" "$avd"
      done <<< "$avds"
    else
      printf "   ${DIM}(No hay AVDs creados)${RESET}\n"
    fi
  fi
  if [[ ${#emu_running[@]} -gt 0 ]]; then
    local er
    for er in "${emu_running[@]}"; do
      printf "   • En ejecución   : %s\n" "$er"
    done
  fi

  if [[ ${#unauthorized_items[@]} -gt 0 ]]; then
    print_rsa_instructions "${unauthorized_items[*]}"
  fi
  printf "\n"
}

cmd_usb() {
  require_adb
  header "Conexión por Cable USB"

  # Comprobar si ya hay un dispositivo USB sin autorizar antes de bloquear en wait-for-usb-device
  local unauth_usb=""
  unauth_usb="$(adb devices | awk 'NR>1 && ($2=="unauthorized" || $2=="offline") && $1 !~ /:/ && $1 !~ /^emulator-/ && $1 !~ /_adb-tls-/ {print $1 " (" $2 ")"}' | paste -sd ', ' - || true)"
  if [[ -n "$unauth_usb" ]]; then
    print_rsa_instructions "$unauth_usb"
  fi

  step "Esperando dispositivo Android conectado y autorizado por USB (Ctrl+C para cancelar)..."
  adb wait-for-usb-device

  local usb_serial=""
  usb_serial="$(adb devices | awk 'NR>1 && $2=="device" && $1 !~ /:/ && $1 !~ /^emulator-/ && $1 !~ /_adb-tls-/ {print $1; exit}' || true)"

  if [[ -z "$usb_serial" ]]; then
    err "Se detectó actividad USB pero no se pudo obtener un número de serie autorizado."
    cmd_doctor
    exit 1
  fi

  print_device_card "$usb_serial" "Dispositivo USB verificado y autorizado"
  info "Siguiente paso: ejecuta ${BOLD}make deploy${RESET} para instalar la app o ${BOLD}make wifi-from-usb${RESET} para pasar a Wi-Fi."
}

cmd_wifi_pair() {
  require_adb
  header "Vinculación Inalámbrica Android 11+ (Código de 6 dígitos)"

  local target_addr="${1:-}"
  local pair_code="${2:-}"

  if [[ -z "$target_addr" ]]; then
    step "Buscando diálogo de vinculación abierto en la red vía mDNS (_adb-tls-pairing._tcp)..."
    target_addr="$(find_mdns_endpoint "_adb-tls-pairing._tcp" "" "" || true)"
    if [[ -n "$target_addr" ]]; then
      ok "Detectado automáticamente móvil en modo vinculación: ${BOLD}${target_addr}${RESET}"
    else
      info "En tu móvil Android 11+:"
      printf "   1. Ve a ${BOLD}Ajustes → Opciones para desarrolladores → Depuración inalámbrica${RESET}.\n"
      printf "   2. Pulsa ${BOLD}«Vincular dispositivo con código de vinculación»${RESET}.\n"
      printf "   3. Mantén esa ventana abierta en el teléfono.\n\n"

      if [[ ! -t 0 ]]; then
        err "No se detectó servicio mDNS y la sesión no es interactiva. Pasa <ip:puerto> y <codigo>."
        exit 1
      fi

      read -r -p "Introduce IP:PUERTO de vinculación (o pulsa Enter para rebuscar por mDNS): " target_addr
      target_addr="${target_addr//[[:space:]]/}"
      if [[ -z "$target_addr" ]]; then
        target_addr="$(find_mdns_endpoint "_adb-tls-pairing._tcp" "" "" || true)"
        if [[ -n "$target_addr" ]]; then
          ok "Detectado por mDNS: ${BOLD}${target_addr}${RESET}"
        else
          read -r -p "No apareció por mDNS. Escribe la IP:PUERTO mostrada en el popup del móvil: " target_addr
          target_addr="${target_addr//[[:space:]]/}"
        fi
      fi
    fi
  fi

  if [[ -z "$target_addr" || "$target_addr" != *:* ]]; then
    err "Formato inválido '${target_addr}'. Debe ser IP:PUERTO (por ejemplo, 192.168.1.45:38192)."
    exit 1
  fi

  if [[ -z "$pair_code" ]]; then
    if [[ ! -t 0 ]]; then
      err "Falta el código de vinculación de 6 dígitos."
      exit 1
    fi
    read -r -p "Introduce el código de vinculación de 6 dígitos: " pair_code
    pair_code="${pair_code//[[:space:]]/}"
  fi

  if [[ -z "$pair_code" ]]; then
    err "El código de vinculación no puede estar vacío."
    exit 1
  fi

  step "Vinculando con ${BOLD}${target_addr}${RESET}..."
  local pair_out=""
  if ! pair_out="$(adb pair "$target_addr" "$pair_code" 2>&1)"; then
    err "Falló 'adb pair': ${pair_out}"
    warn "Comprueba que:"
    printf "   • El popup con el código sigue abierto en el móvil (cambia de puerto cada vez que se cierra).\n"
    printf "   • Mac y móvil están en la misma Wi-Fi (sin red de invitados ni aislamiento AP).\n"
    exit 1
  fi
  printf "   %s\n" "$pair_out"
  if [[ "$pair_out" != *"Successfully paired"* && "$pair_out" != *"paired to"* ]]; then
    warn "Respuesta inesperada de adb pair; verificando conexión..."
  else
    ok "¡Dispositivo vinculado correctamente!"
  fi

  local phone_ip="${target_addr%:*}"
  auto_connect_after_pair "$phone_ip"
}

cmd_wifi_qr() {
  local mode="${1:-}"
  local rand_id rand_pass
  rand_id="studio-$(LC_ALL=C tr -dc 'a-z0-9' </dev/urandom | head -c 8 || true)"
  rand_pass="$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 10 || true)"
  local qr_payload="WIFI:T:ADB;S:${rand_id};P:${rand_pass};;"

  if [[ "$mode" == "--qr-only" ]]; then
    render_qr_terminal "$qr_payload"
    return 0
  fi

  require_adb
  header "Vinculación Inalámbrica por Código QR (Android 11+)"

  local lan_info=""
  if lan_info="$(get_mac_lan_info)"; then
    info "Red Wi-Fi del Mac: ${BOLD}${lan_info}${RESET}"
  else
    warn "No se detectó IP local en el Mac; asegúrate de estar conectado a la misma Wi-Fi que el móvil."
  fi

  if ! adb mdns check >/dev/null 2>&1; then
    warn "mDNS no reporta soporte activo en adb; si el escaneo se queda esperando, usa 'make wifi-pair'."
  fi

  printf "\n${BOLD}Pasos en tu móvil Android (11 o superior):${RESET}\n"
  printf "  1. Abre ${BOLD}Ajustes → Opciones para desarrolladores → Depuración inalámbrica${RESET}.\n"
  printf "  2. Pulsa ${BOLD}«Vincular dispositivo con código QR»${RESET}.\n"
  printf "  3. Apunta la cámara al siguiente código QR:\n\n"

  render_qr_terminal "$qr_payload"

  printf "\n   ${DIM}Sesión mDNS: ${rand_id} | Esperando escaneo (máx. 90 s, Ctrl+C para cancelar)...${RESET}\n"

  local timeout_sec=90
  local elapsed=0
  local pair_ep=""

  while (( elapsed < timeout_sec )); do
    # Prioridad 1: coincidencia exacta con nuestro nombre de servicio studio-<rand>
    pair_ep="$(find_mdns_endpoint "_adb-tls-pairing._tcp" "$rand_id" "" || true)"
    # Prioridad 2: cualquier servicio _adb-tls-pairing._tcp recién anunciado
    if [[ -z "$pair_ep" ]]; then
      pair_ep="$(find_mdns_endpoint "_adb-tls-pairing._tcp" "" "" || true)"
    fi

    if [[ -n "$pair_ep" ]]; then
      printf "\n"
      ok "¡Código QR escaneado! Servicio detectado en ${BOLD}${pair_ep}${RESET}"
      step "Ejecutando vinculación segura TLS..."
      local pair_out=""
      if pair_out="$(adb pair "$pair_ep" "$rand_pass" 2>&1)"; then
        printf "   %s\n" "$pair_out"
        if [[ "$pair_out" == *"Successfully paired"* || "$pair_out" == *"paired to"* ]]; then
          ok "¡Vinculación por QR completada con éxito!"
          local phone_ip="${pair_ep%:*}"
          auto_connect_after_pair "$phone_ip"
          return 0
        fi
      fi
      warn "Intento con ${pair_ep} no coincidió (${pair_out}); reintentando escucha mDNS..."
      pair_ep=""
    fi

    printf "\r   ⏳ Escuchando anuncio mDNS del teléfono... (%ds/%ds) " "$elapsed" "$timeout_sec"
    sleep 1
    elapsed=$((elapsed + 1))
  done

  printf "\n"
  err "Tiempo de espera agotado (${timeout_sec}s) sin recibir anuncio mDNS del móvil."
  warn "Posibles causas: el router tiene 'AP Isolation' / bloquea multicast mDNS, o el móvil está en otra subred."
  info "Alternativas infalibles: ${BOLD}make wifi-pair${RESET} (código manual) o ${BOLD}make wifi-from-usb${RESET} (puente por cable)."
  exit 1
}

cmd_wifi_from_usb() {
  require_adb
  local port="${1:-5555}"
  if [[ ! "$port" =~ ^[0-9]+$ ]] || (( port < 1024 || port > 65535 )); then
    err "Puerto inválido '${port}'. Usa un número entre 1024 y 65535 (por defecto: 5555)."
    exit 1
  fi

  header "Puente Clásico USB → Wi-Fi TCP/IP (Puerto ${port})"

  local unauth_usb=""
  unauth_usb="$(adb devices | awk 'NR>1 && ($2=="unauthorized" || $2=="offline") && $1 !~ /:/ && $1 !~ /^emulator-/ && $1 !~ /_adb-tls-/ {print $1 " (" $2 ")"}' | paste -sd ', ' - || true)"
  if [[ -n "$unauth_usb" ]]; then
    print_rsa_instructions "$unauth_usb"
    exit 1
  fi

  local usb_serial=""
  usb_serial="$(adb devices | awk 'NR>1 && $2=="device" && $1 !~ /:/ && $1 !~ /^emulator-/ && $1 !~ /_adb-tls-/ {print $1; exit}' || true)"

  if [[ -z "$usb_serial" ]]; then
    err "No se detectó ningún móvil conectado por cable USB."
    info "Conecta el teléfono por USB primero y verifica con ${BOLD}make usb${RESET} (o usa ${BOLD}make wifi-qr${RESET} sin cables)."
    exit 1
  fi

  local model
  model="$(get_device_prop "$usb_serial" ro.product.model)"
  ok "Dispositivo USB encontrado: ${BOLD}${model:-Android}${RESET} (${usb_serial})"

  step "Consultando dirección IP Wi-Fi del teléfono (wlan0)..."
  local phone_ip=""
  phone_ip="$(adb -s "$usb_serial" shell ip -f inet addr show wlan0 2>/dev/null \
    | tr -d '\r' \
    | awk '/inet / {sub(/\/.*/, "", $2); print $2; exit}' || true)"

  if [[ -z "$phone_ip" ]]; then
    phone_ip="$(adb -s "$usb_serial" shell ip route 2>/dev/null \
      | tr -d '\r' \
      | awk '/wlan/ && /src / {for (i=1; i<=NF; i++) if ($i=="src") {print $(i+1); exit}}' || true)"
  fi

  if [[ -z "$phone_ip" ]]; then
    phone_ip="$(adb -s "$usb_serial" shell ip -f inet addr show 2>/dev/null \
      | tr -d '\r' \
      | awk '/^[0-9]+: (wlan|swlan)/{w=1; next} /^[0-9]+:/{w=0} w && /inet /{sub(/\/.*/, "", $2); print $2; exit}' || true)"
  fi

  if [[ -z "$phone_ip" ]]; then
    err "No se pudo obtener una IP Wi-Fi activa del teléfono (${usb_serial})."
    warn "Asegúrate de activar el Wi-Fi en el móvil y conectarlo a la misma red que tu Mac."
    exit 1
  fi

  ok "IP Wi-Fi del móvil detectada: ${BOLD}${phone_ip}${RESET}"

  step "Activando modo TCP/IP en el puerto ${port} ('adb -s ${usb_serial} tcpip ${port}')..."
  adb -s "$usb_serial" tcpip "$port"

  step "Esperando 2 segundos a que reinicie el demonio adbd en el móvil..."
  sleep 2

  local endpoint="${phone_ip}:${port}"
  step "Conectando por Wi-Fi a ${BOLD}${endpoint}${RESET}..."
  local conn_out=""
  conn_out="$(adb connect "$endpoint" 2>&1 || true)"
  printf "   %s\n" "$conn_out"

  local verified=0
  local i
  for i in 1 2 3 4 5; do
    if adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -Fxq "$endpoint"; then
      verified=1
      break
    fi
    sleep 1
  done

  if (( verified == 1 )); then
    print_device_card "$endpoint" "Puente USB → Wi-Fi activo"
    printf "\n${GREEN}${BOLD}🎉 ¡Listo! Ya puedes desconectar el cable USB del teléfono con total seguridad.${RESET}\n\n"
  else
    err "No se pudo verificar la conexión a ${endpoint}."
    warn "Verifica que el router no tenga aislamiento entre clientes Wi-Fi (AP Isolation) o firewall activo."
    exit 1
  fi
}

cmd_disconnect() {
  require_adb
  header "Desconexión de Sesiones ADB por Wi-Fi"

  local target="${1:-}"
  local out=""
  if [[ -n "$target" ]]; then
    step "Desconectando ${target}..."
    out="$(adb disconnect "$target" 2>&1 || true)"
  else
    step "Cerrando todas las conexiones TCP/IP de ADB..."
    out="$(adb disconnect 2>&1 || true)"
  fi
  printf "   %s\n" "${out:-Sesiones desconectadas.}"
  ok "Sesiones TCP/IP desconectadas."
  info "Nota: si usas Android 11+ con auto-conexión mDNS TLS y deseas cortar por completo, desactiva «Depuración inalámbrica» en el móvil."
}

cmd_deploy() {
  require_adb
  header "Compilación y Despliegue Debug (${APP_ID})"

  local serial=""
  serial="$(select_target_device "${1:-}")"
  local kind model
  kind="$(classify_serial "$serial")"
  model="$(get_device_prop "$serial" ro.product.model)"

  ok "Destino seleccionado: ${BOLD}${model:-Android}${RESET} [${CYAN}${serial}${RESET}, vía ${kind}]"

  cd "$PROJECT_ROOT"

  local apk_path="app/build/outputs/apk/debug/app-debug.apk"
  step "Compilando APK Debug con './gradlew assembleDebug'..."
  ./gradlew assembleDebug

  # Intentar despliegue rápido incremental (delta install) con 'android run' si está disponible
  if command -v android >/dev/null 2>&1 && [[ -f "$apk_path" ]]; then
    step "Desplegando con 'android run --apks=${apk_path} --device=${serial}' (instalación delta rápida)..."
    if android run --apks="$apk_path" --device="$serial"; then
      ok "¡Aplicación compilada, instalada (delta) y lanzada en ${serial}!"
      info "Ejecuta ${BOLD}make logs${RESET} para ver el logcat filtrado por PID en tiempo real."
      return 0
    fi
    warn "'android run' devolvió aviso/error; aplicando instalación estándar con Gradle + adb..."
  fi

  step "Instalando con './gradlew installDebug' en ${serial}..."
  local install_log
  install_log="$(mktemp -t img2ascii_install.XXXXXX)"
  if ! ANDROID_SERIAL="$serial" ./gradlew installDebug 2>&1 | tee "$install_log"; then
    if grep -q "INSTALL_FAILED_USER_RESTRICTED" "$install_log"; then
      err "El móvil rechazó la instalación por USB/ADB (INSTALL_FAILED_USER_RESTRICTED)."
      warn "En Xiaomi / Redmi / POCO (MIUI / HyperOS):"
      printf "   • Activa ${BOLD}«Instalar vía USB»${RESET} y ${BOLD}«Depuración USB (Ajustes de seguridad)»${RESET} en Opciones para desarrolladores.\n"
    elif grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE" "$install_log"; then
      err "Conflicto de firma con una versión previa instalada (INSTALL_FAILED_UPDATE_INCOMPATIBLE)."
      warn "Desinstala la copia anterior con: ${CYAN}adb -s ${serial} uninstall ${APP_ID}${RESET}"
    fi
    rm -f "$install_log"
    exit 1
  fi
  rm -f "$install_log"

  step "Lanzando actividad principal (${MAIN_ACTIVITY})..."
  if ! adb -s "$serial" shell am start -n "$MAIN_ACTIVITY" >/dev/null 2>&1; then
    adb -s "$serial" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  fi

  ok "¡Aplicación desplegada y en ejecución sobre ${serial}!"
  info "Ejecuta ${BOLD}make logs${RESET} para ver los logs de ${APP_ID} en vivo."
}

cmd_logs() {
  require_adb
  header "Logs en Tiempo Real (${APP_ID})"

  local serial=""
  serial="$(select_target_device "${1:-}")"
  local model
  model="$(get_device_prop "$serial" ro.product.model)"
  info "Dispositivo: ${BOLD}${model:-Android}${RESET} (${CYAN}${serial}${RESET}) — Pulsa ${BOLD}Ctrl+C${RESET} para salir."

  local log_sub_pid=""
  cleanup_logs() {
    if [[ -n "${log_sub_pid:-}" ]]; then
      pkill -P "$log_sub_pid" 2>/dev/null || true
      kill "$log_sub_pid" 2>/dev/null || true
      wait "$log_sub_pid" 2>/dev/null || true
      log_sub_pid=""
    fi
  }
  trap 'cleanup_logs; printf "\n"; info "Transmisión de logs finalizada."; exit 0' INT TERM

  local current_mode=""
  local current_pid=""

  while true; do
    local detected_pid=""
    detected_pid="$(adb -s "$serial" shell pidof -s "$APP_ID" 2>/dev/null | tr -d '\r[:space:]' || true)"

    if [[ -n "$detected_pid" ]]; then
      if [[ "$current_mode" != "pid" || "$current_pid" != "$detected_pid" ]]; then
        cleanup_logs
        current_mode="pid"
        current_pid="$detected_pid"
        ok "Filtrando logcat por PID exacto de ${APP_ID}: ${BOLD}${current_pid}${RESET}"
        ( adb -s "$serial" logcat -T 30 -v color --pid="$current_pid" ) &
        log_sub_pid=$!
      fi
    else
      if [[ "$current_mode" != "fallback" ]]; then
        cleanup_logs
        current_mode="fallback"
        current_pid=""
        warn "La app ${APP_ID} no está abierta todavía; filtrando por paquete/etiqueta hasta que arranque..."
        ( adb -s "$serial" logcat -T 30 -v color | grep --line-buffered -iE "${APP_ID}|img2ascii|AndroidRuntime" ) &
        log_sub_pid=$!
      fi
    fi

    # Verificar que el dispositivo sigue conectado
    if ! adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -Fxq "$serial"; then
      cleanup_logs
      err "Se perdió la conexión con ${serial}."
      exit 1
    fi

    sleep 2
  done
}

# ==============================================================================
# Punto de entrada principal
# ==============================================================================
resolve_sdk_env

SUBCOMMAND="${1:-help}"
shift || true

case "$SUBCOMMAND" in
  doctor|status)
    cmd_doctor "$@"
    ;;
  usb)
    cmd_usb "$@"
    ;;
  wifi-pair)
    cmd_wifi_pair "$@"
    ;;
  wifi-qr)
    cmd_wifi_qr "$@"
    ;;
  wifi-from-usb)
    cmd_wifi_from_usb "$@"
    ;;
  disconnect)
    cmd_disconnect "$@"
    ;;
  deploy)
    cmd_deploy "$@"
    ;;
  logs)
    cmd_logs "$@"
    ;;
  serial)
    require_adb
    select_target_device "${1:---usb}"
    printf "\n"
    ;;
  -h|--help|help)
    cmd_help
    ;;
  *)
    err "Subcomando desconocido: '${SUBCOMMAND}'"
    printf "\n" >&2
    cmd_help >&2
    exit 1
    ;;
esac
