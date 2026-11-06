# Especificación `NNN`: `<Título Conciso de la Funcionalidad>`

| Campo | Valor |
| :--- | :--- |
| **ID de Especificación** | `NNN-slug-descriptivo` |
| **Estado** | `BORRADOR` *(Valores válidos: `BORRADOR` \| `EN REVISIÓN` \| `APROBADO` \| `IMPLEMENTADO`)* |
| **Autor(es)** | `<Rol / Nombre>` |
| **Fecha de Creación** | `YYYY-MM-DD` |
| **Última Actualización** | `YYYY-MM-DD` |
| **Paquete Raíz** | `com.laralbir.img2asciiim` |
| **Dependencias Previas** | `<Ninguna o lista de specs NNN requeridas>` |

> **Regla de Gobernanza SDD → TDD (`AGENTS.md`):**
> Ningún agente o desarrollador puede escribir código de producción para los requisitos descritos en este documento mientras el campo **Estado** no sea `APROBADO` por el usuario y exista primero un test automatizado en estado `RED` que valide el criterio correspondiente.

---

## 1. Contexto y Problema

### 1.1 Situación Actual y Motivación
Describir con claridad el problema real del usuario, el entorno operativo (por ejemplo, restricciones de red, límites de apps de mensajería o ergonomía móvil) y por qué es necesario resolverlo ahora.

### 1.2 Objetivo de la Funcionalidad
Resumen ejecutivo del resultado observable que obtendrá el usuario cuando esta especificación esté implementada.

### 1.3 Fuera de Alcance (*Non-Goals*)
- `<Elemento explícitamente excluido 1 para evitar desviaciones de alcance>`
- `<Elemento explícitamente excluido 2>`

---

## 2. Historias de Usuario

| ID | Prioridad | Historia de Usuario | Valor Entregado |
| :--- | :---: | :--- | :--- |
| **HU-01** | `P0` | **Como** `<perfil de usuario>`, **quiero** `<acción concreta>`, **para** `<beneficio medible>`. | `<Impacto>` |
| **HU-02** | `P1` | **Como** `<perfil de usuario>`, **quiero** `<acción concreta>`, **para** `<beneficio medible>`. | `<Impacto>` |

---

## 3. Requisitos Funcionales y Criterios de Aceptación (`Given / When / Then`)

### RF-01: `<Nombre del Requisito Funcional 1>`
Descripción normativa sin ambigüedades de lo que el sistema **debe** hacer.

#### Criterios de Aceptación de `RF-01`
- **CA-01.1 — `<Escenario principal / Happy Path>`**
  - **Given (Dado que):** `<precondición verificable y estado inicial>`
  - **When (Cuando):** `<acción o evento disparador>`
  - **Then (Entonces):** `<resultado observable exacto e invariantes cumplidos>`

- **CA-01.2 — `<Escenario alternativo o validación de límites>`**
  - **Given (Dado que):** `<precondición>`
  - **When (Cuando):** `<acción>`
  - **Then (Entonces):** `<resultado observable>`

---

### RF-02: `<Nombre del Requisito Funcional 2>`
Descripción normativa sin ambigüedades.

#### Criterios de Aceptación de `RF-02`
- **CA-02.1 — `<Escenario>`**
  - **Given (Dado que):** `<precondición>`
  - **When (Cuando):** `<acción>`
  - **Then (Entonces):** `<resultado observable>`

---

## 4. Restricciones de Dominio e Invariantes Técnicos

Documentar fórmulas matemáticas, rangos numéricos permitidos, codificaciones Unicode, límites de protocolos de terceros y contratos de pureza arquitectónica:
- **Invariante 1 (`domain/`):** `<Ej. Cero imports android.*, determinismo idempotente ante la misma entrada>`
- **Invariante 2 (Formato / Protocolo):** `<Ej. Presupuesto máximo de unidades UTF-16, rango Unicode U+2800..U+28FF>`

---

## 5. Restricciones No Funcionales (`RNF`)

| ID | Categoría | Requisito Medible | Método de Verificación |
| :--- | :--- | :--- | :--- |
| **RNF-01** | **Rendimiento / Latencia** | `<Ej. Procesamiento < 150 ms en gama media>` | Benchmark / Test cronometrado |
| **RNF-02** | **Memoria (RAM)** | `<Ej. Pico de asignación < 20 MB con imágenes de 48 MP>` | Test de submuestreo / `Debug.MemoryInfo` |
| **RNF-03** | **Privacidad y Permisos** | `<Ej. Cero permisos de almacenamiento ni permiso INTERNET>` | Inspección de `AndroidManifest.xml` fusionado |
| **RNF-04** | **Operación Offline** | `<Ej. Funcionalidad 100% operativa en Modo Avión>` | Prueba instrumentada sin red |
| **RNF-05** | **Accesibilidad (a11y)** | `<Ej. contentDescription completos y áreas táctiles >= 48dp>` | Compose Semantics Test + TalkBack |

---

## 6. Casos Borde y Manejo de Errores (*Edge Cases*)

| ID | Caso Borde / Condición de Fallo | Comportamiento Esperado del Sistema | Capa Responsable |
| :--- | :--- | :--- | :--- |
| **CB-01** | `<Ej. Imagen con canal alfa transparente>` | `<Componer sobre fondo determinista según modo claro/oscuro antes de extraer luminancia>` | `domain/` |
| **CB-02** | `<Ej. URI remota de Google Drive no cacheada estando en Modo Avión>` | `<Capturar IOException sin crash y mostrar mensaje claro en UI>` | `data/` + `ui/` |
| **CB-03** | `<Ej. Usuario cancela selector del sistema>` | `<Conservar el estado actual intacto sin emitir error>` | `ui/` |

---

## 7. Matriz de Trazabilidad TDD

| Criterio / Caso Borde | Capa Principal | Tipo de Prueba (`JVM` / `Robolectric` / `AndroidTest`) | Suite de Test Objetivo |
| :--- | :---: | :---: | :--- |
| `CA-01.1`, `CB-01` | `domain` | Unit JVM (`< 100 ms`) | `<NombreSuiteTest.kt>` |
| `CA-01.2`, `CB-02` | `data` | Robolectric | `<NombreDecoderTest.kt>` |
| `CA-02.1`, `CB-03` | `ui` | Turbine + Compose UI Test | `<NombreViewModelTest.kt>` / `<NombreScreenTest.kt>` |

---

## 8. Decisiones Abiertas (*Open Design Decisions*)

> Todas las decisiones de esta sección deben ser confirmadas por el usuario antes de pasar el estado a `APROBADO`.

### DD-01: `<Título de la Decisión Abierta>`
- **Contexto:** `<Por qué existen varias alternativas viables>`
- **Opción A (Recomendada):** `<Descripción, ventajas y costes>`
- **Opción B:** `<Descripción, ventajas y costes>`
- **Resolución del Usuario:** `[ ] Pendiente` — `<Registrar aquí la elección acordada>`

---

## 9. Registro de Aprobación (*Sign-off*)

- [ ] Todos los requisitos tienen criterios **Given / When / Then** verificables mediante tests automatizados.
- [ ] Las **Decisiones Abiertas (`DD-XX`)** han sido respondidas por el usuario.
- [ ] El plan arquitectónico (`plan.md`) y el desglose TDD (`tasks.md`) están sincronizados con esta especificación.
- **Aprobado por:** `<Pendiente>` | **Fecha:** `<Pendiente>`
