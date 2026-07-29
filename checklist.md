# Fuerz4 Assistant — Checklist

Estado de las fases del desarrollo (ver `CLAUDE.md` para el detalle de arquitectura y decisiones).

## Phase 0 — Repo & scaffolding
- [x] `git init`, `.gitignore` (Android + `apikey.properties`)
- [x] Proyecto Compose (`com.fuerz4.assistant`, minSdk 30, targetSdk 34), Hilt/Compose/Retrofit/OkHttp vía version catalog
- [x] `apikey.properties.example`, wiring de `buildConfigField`

## Phase 1 — Theming & branding
- [x] Paleta de colores + `Fuerz4Theme.kt` (#BA4C1B / #1A1F25 / #FCFCFC), esquemas M3 claro/oscuro
- [x] Ícono adaptativo derivado de `FA.svg`/`FA.png`
- [x] `MainActivity`, `NavGraph`, `BottomNavBar` (Inicio/Dispositivos/Perfil)

## Phase 2 — Networking & crypto layer
- [x] Port de `SecureUtil.kt` (cifrado XOR), `WsseUtil.kt` (WSSE), `AuthInterceptor.kt`, `NanoApi.kt`, DTOs
- [x] `SessionManager` (token cifrado), `NetworkMonitor`
- [x] Tests unitarios: round-trip de cifrado, formato de header WSSE

## Phase 3 — Auth screens
- [x] Login (`/login/login`), Registro (`/users/registration`), Recuperar contraseña en 3 pasos
- [x] Manejo de estado offline en cada pantalla

## Phase 4 — Profile screen
- [x] Ver/actualizar nombre (`/users/update`), cerrar sesión

## Phase 5 — Devices + WiFi provisioning
- [x] Listado, alta (con selección de tipo + formulario), baja de dispositivos
- [x] `WifiProvisioningManager`, `UdpProvisioningClient`, `DeviceIdGenerator`
- [x] Flujo completo: conectar a `nUdpWiFi` → enviar config UDP → restaurar red → registrar en backend

## Phase 6 — Chat + voice + wake word
- [x] Chat de texto y voz, botón de limpiar conversación (sólo en memoria)
- [x] `SpeechRecognizerManager`, `TextToSpeechManager`, TTS sólo en respuestas por voz
- [x] Wake word "Che fuerza" con Vosk: descarga del modelo bajo demanda, servicio en primer plano, arbitraje de micrófono
- [x] Integración con `/api/services/chat/converse`

## Phase 7 — Offline handling
- [x] Chequeo de conectividad antes de cada acción de red (login, registro, perfil, dispositivos, chat)
- [x] Chequeo específico de WiFi (no sólo "hay internet") antes de aprovisionar un dispositivo

## Phase 8 — Backend (repo NanoServer, no incluido en este repo)
- [x] `ResponseType.SC_DUPLICATE_DEVICE(807)`
- [x] `devices/list|create|update|remove` en `DevicesWebService.java`, WSSE + LoginToken, verificación de propiedad
- [x] `ChatWebService.java`: integración con Gemini (HttpClient + Jackson) + loop de tool-calling
- [x] `GEMINI_API_KEY`/`GEMINI_MODEL` en `ServerProperties` + `conf/app.properties(.example)`
- [ ] Validación manual con curl/WSSE firmado contra un servidor real corriendo (pendiente — requiere clave de Gemini real y despliegue)

## Phase 9 — i18n
- [x] Auditoría de todos los Composables: sin strings hardcodeados, todo en `values/strings.xml`

## Phase 10 — CLAUDE.md
- [x] Documentación de arquitectura, decisiones, limitaciones conocidas y pasos de testing

## Phase 11 — Publicación
- [ ] Crear repositorio público en GitHub y hacer push (paso final, pendiente de confirmación del usuario)

---

## Pendientes de validación manual (no verificables en este entorno)
- Aprovisionamiento WiFi en dispositivos reales, distintos fabricantes (Samsung/Xiaomi son históricamente más estrictos con `WifiNetworkSpecifier`)
- Detección de wake word en segundo plano/Doze, con distintas configuraciones de optimización de batería
- Precisión/latencia real de STT y TTS en español
- El endpoint `/api/services/chat/converse` y la integración con Gemini no se probaron contra un servidor real (requiere clave de API válida)
