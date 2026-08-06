# Fuerz4 Assistant

Android app (Kotlin + Jetpack Compose) — a Grok/Gemini-style chat assistant (text + voice, offline
wake word) for managing IoT devices ("Energía"/"Ambiente" sensors) that report into the existing
**NanoServer** backend. See `checklist.md` for phase-by-phase status.

## Stack

- Kotlin, Jetpack Compose (Material 3), MVVM + light Clean Architecture (`data` / `domain` / `presentation`)
- Hilt (DI), Retrofit + OkHttp + kotlinx.serialization, StateFlow/SharedFlow, coroutines
- minSdk 30, targetSdk 34, compileSdk 35 (bumped from 34 solely to satisfy Vico's transitive
  `androidx.core` AAR metadata requirement — see "Device detail & history" below; targetSdk/minSdk
  are unchanged, so this has no runtime behavior impact, just a compile-time API surface bump)
- Gradle 8.7, AGP 8.5.2 (older than the AGP versions Google lists as tested against compileSdk 35,
  so a `WARNING: We recommend using a newer Android Gradle plugin` is expected/benign on every
  build until AGP is upgraded), Kotlin 2.0.21 (Compose compiler plugin) — **requires JDK 17** to
  run Gradle itself (this machine's default JDKs are 8/11; a local JDK 17 was used to build/
  validate — point `org.gradle.java.home` or `JAVA_HOME` at a JDK 17 install before building)

## Backend dependency: NanoServer

This app is a client for **NanoServer** (`../NanoServer`, separate repo, Java/Spring), base URL
`https://nano.fuerz4.com/api/services`. NanoServer needed additions to support this app — see
"NanoServer companion changes" below. Those changes live in the NanoServer repo, not here.

### Auth: WSSE + XOR "encryption"

Every `webServices/*` call carries two independent auth layers, both required and both mirrored
byte-for-byte from a sibling app (`tsm_android`) that talks to the same backend family:

1. **Gateway auth (all calls)** — `X-WSSE` header (`UsernameToken Username="KEY", PasswordDigest="…", Nonce="…", Created="…"`)
   + `Authorization: WSSE profile="UsernameToken"`, built by `AuthInterceptor.kt` /
   `WsseUtil.kt` from `BuildConfig.SECURITY_KEY`/`SECURITY_SECRET` (SHA-1 digest of
   `nonce+created+secret`, nonce replay-checked server-side). This is a shared *machine*
   credential, the same one already used by NanoServer's other mobile-facing endpoints — **not**
   real encryption, just a signed-freshness scheme.
2. **User identity (Login/Users/Devices/Chat mutating calls)** — a `LoginToken` header, obtained
   from `/login/login`'s response header and persisted via `SessionManager` (`EncryptedSharedPreferences`).

Login/Users request/response bodies are further wrapped in NanoServer's own **XOR cipher**
(`SecureUtil.kt`, ported from `SecureUtils.java`) — a reversible, date-rotated-key scheme, not
real cryptography. It only needs to match the server's algorithm exactly; do not "improve" it
independently on one side. Devices/Chat endpoints (added for this app, see below) intentionally
skip this wrapper and just send/receive plain JSON, matching `McpWebService`'s existing precedent.

### Error handling

NanoServer sends its response-type codes (e.g. `800` invalid credentials, `806` invalid params)
as the **literal HTTP status**, so `NanoApi` methods return `retrofit2.Response<T>` (not a bare
body) and `safeApiCall`/`safeApiCallBody` (`data/remote/NanoApiResult.kt`) turn non-2xx responses
into `Result.failure(NanoApiError.ServerCode(code))` by parsing the shared `{success,data,message}`
envelope from the error body. `presentation/common/ApiErrorMapper.kt` maps each code to localized
copy. Extend that `when` if the backend adds new codes.

### Endpoints used

| Endpoint | Auth | Body |
|---|---|---|
| `/login/login`, `/login/tokenLogin`, `/login/requestCode`, `/login/validateCode`, `/login/resetPassword` | WSSE | `encryptedData` (XOR) |
| `/users/registration` | WSSE | `encryptedData` + `images` |
| `/users/update` | WSSE + LoginToken | `encryptedData` + `images` |
| `/devices/list`, `/devices/create`, `/devices/update`, `/devices/remove` | WSSE + LoginToken | plain JSON |
| `/devices/latestValue`, `/devices/history` | WSSE + LoginToken | plain JSON |
| `/chat/converse` | WSSE + LoginToken | plain JSON |

The last three rows (**Devices CRUD**, **device latest/history**, **Chat**) did not exist before
this project — NanoServer only had a cookie-authenticated admin controller for device CRUD/logs and
a WSSE **read-only** `/mcp/devices` listing. See "NanoServer companion changes" below.

## Device WiFi provisioning

Flow: `DeviceTypePickerScreen` → `DeviceFormScreen` (name + home WiFi SSID/password + optional
volts/amps or temp/hum) → `DeviceFormViewModel.startProvisioning()`:

1. Gate on **WiFi transport specifically** (`NetworkMonitor.isWifiConnected()`), not just "any
   internet" — per spec, and because the phone needs WiFi hardware active to join the device's AP.
2. `WifiProvisioningManager.connectToDeviceAp("nUdpWiFi", "$n=f16e7r81")` — `WifiNetworkSpecifier`
   + `ConnectivityManager.requestNetwork`/`NetworkCallback`, then `bindProcessToNetwork(network)`.
3. `UdpProvisioningClient.provision(...)` — direct port of `CuatroBoomClt/UdpSender.java`'s wire
   protocol: JSON to `192.168.4.1:4642` (socket also bound to local port 4642), 10s timeout,
   255-byte receive buffer, success = device echoes the sent bytes back verbatim. Payload shape
   depends on type:
   - Energía (`WFEM` prefix): `{"ssid":"%s","pass":"%s","id":"%s","volts":%.2f,"amps":%.2f}`
   - Ambiente (`WFTM` prefix): `{"ssid":"%s","pass":"%s","id":"%s","temp":%.2f,"hum":%.2f}`
4. `WifiProvisioningManager.releaseAndRestore()` — **always** in a `finally`, so the phone is never
   stuck bound to the device AP even on failure.
5. `DeviceRepository.createDevice(...)` against the new `/devices/create` endpoint. The generated
   `id`/`uuid` (`DeviceIdGenerator`) doubles as the UDP payload's `id` and the server's row key —
   it must keep the `WFEM`/`WFTM` prefix since NanoServer's `Device.getType()` derives type from it.

**Platform constraint (important):** Android has never let apps read the saved password of *any*
WiFi network, including the one the phone is currently on — so the home-network SSID/password the
device gets provisioned with must always be typed by the user; there is no way to auto-fill it.
Also, since API 29+, releasing a `WifiNetworkSpecifier` request only deprioritizes this process's
routing — the phone's regular WiFi connection was never actually torn down at the OS level, so
there's no API to force or guarantee reconnection. `releaseAndRestore()` is a release-and-verify
step (polls default-network state for up to 15s), not a forced reconnect. **Not verified on real
hardware in this environment — flagged as the highest-risk area for on-device testing** (Samsung/
Xiaomi have a history of being stricter about `WifiNetworkSpecifier` than stock/Pixel).

## Devices list: edit vs. detail, and delete

On `DevicesListScreen`, each row's pencil icon (`Icons.Filled.Edit`, replacing the trash icon that
used to live there) opens `DeviceFormScreen` in edit mode; tapping the row body itself instead opens
the new `DeviceReadingsScreen` (below). Delete moved off the list entirely: `DeviceFormScreen` now
shows a bordered "Eliminar dispositivo" `OutlinedButton` (error-color border/container, only when
`isEditMode`) below Save, behind an `AlertDialog` confirmation
(`DeviceFormViewModel.onDeleteClick`/`confirmDelete`) — reuses the same
`devices_delete_confirm_title`/`_message` strings the old list-screen dialog used, and reuses the
existing `ProvisioningStep.Success` → `onProvisioned()` navigation-back-to-list path on success.

## Device detail & history

Tapping a device row opens `DeviceReadingsScreen`/`DeviceReadingsViewModel`, showing that device's
latest reading, a line chart, and three selectors: which value to plot (Volts/Amps for Energía,
Temp/Hum for Ambiente), a time range (Día/Mes/Año/Todo), and — for the first three ranges — a date
(Material3 `DatePickerDialog`; hidden for "Todo"). Range → server granularity mapping lives in
`domain/model/DeviceReading.kt`'s `DeviceHistoryRange` enum (Día=`byHour`, Mes=`byDay`, Año=`byMonth`,
Todo=`byYear`); the `(from, until)` window for a given range + selected date is computed by the pure,
clock-injectable `DeviceHistoryRangeCalculator.bounds(...)` (unit-tested in
`DeviceHistoryRangeCalculatorTest`, mirroring `SecureUtilTest`'s clock-injection pattern).

This needed two new NanoServer endpoints that didn't exist before this feature — the underlying
`energyLogService`/`environmentLogService` history/latest-value queries were previously only
reachable from `ChatWebService`'s internal Gemini/Grok tool-calls and from a cookie-authenticated
admin controller (`EnergyController`/`EnvironmentController`), neither usable by this app's
WSSE+LoginToken auth. `DeviceHistoryWebService.java` (new, mirrors `DevicesWebService`'s auth +
ownership-check pattern exactly) adds `POST /api/services/devices/latestValue` and
`POST /api/services/devices/history`, returning the existing `EnergyLogDto`/`EnvironmentLogDto`
(latest) and `EnergyLogsDto`/`EnvironmentLogsDto` (history) — no new response DTOs needed
server-side. **Not validated against live device history data in this environment** — same caveat
as the chat/Gemini integration.

Android-side, `DeviceReadingDto` (`data/remote/dto/DeviceHistoryDtos.kt`) is one unified
nullable-field shape covering both server DTOs, relying on `NetworkModule`'s shared
`Json { ignoreUnknownKeys = true }` to discard whichever fields don't apply — the same trick
`DeviceSettingsDto` already used.

**Chart library:** [Vico](https://github.com/patrykandpatrick/vico) (`compose-m3` artifact,
2.1.2) — Compose-native, and its `rememberM3VicoTheme()` derives chart colors straight from
`MaterialTheme.colorScheme` (so the line color matches `NaranjaOscuro` automatically without any
hardcoded color in the chart code). **All Vico 2.x versions transitively require `androidx.core`
≥1.15.0**, which fails Android's AAR-metadata check unless `compileSdk` is at least 35 — this is
why `compileSdk` was bumped from 34 to 35 (see "Stack" above) specifically to unblock this one
dependency; `minSdk`/`targetSdk` are untouched.

## Chat, voice, and the wake word

- **Text vs voice reply behavior** (`ChatViewModel.send(text, inputMode)`): every message is
  tagged `InputMode.TEXT` or `InputMode.VOICE` at the point it's sent. The assistant's reply is
  always appended as a text bubble; `TextToSpeechManager.speak(reply)` is called **only** when
  `inputMode == VOICE`. This is the single mechanism behind "typed → text only, spoken → text + TTS".
- **STT**: `SpeechRecognizerManager` wraps Android's native `android.speech.SpeechRecognizer`
  (`es-AR` locale, falls back per system rules). **TTS**: `TextToSpeechManager` wraps native
  `android.speech.tts.TextToSpeech`, same locale.
- **Wake word ("Che fuerza")**: **Vosk** (`com.alphacephei:vosk-android:0.3.75`, resolves from
  Maven Central — verified against the real POM, this is not a placeholder dependency), fully
  offline, using Vosk's limited-grammar constructor with
  `WakeWordGrammar.GRAMMAR_JSON = ["che fuerza", "[unk]"]` for accuracy and low CPU/battery use
  vs. a general-vocabulary model.
  - **Model distribution**: the Spanish model is **downloaded on first use** by
    `VoskModelProvisioner` from `BuildConfig.VOSK_MODEL_URL` into app-private storage
    (`filesDir/vosk-model`), then unzipped — it is **not** bundled in the APK/AAB and **not**
    committed to this (public) repo, to avoid permanently bloating the git history with a
    multi-ten-MB binary. Set `VOSK_MODEL_URL` in `apikey.properties` to a hosted zip of e.g.
    `vosk-model-small-es-0.42` (see alphacephei.com/vosk/models). Requires connectivity the first
    time the user enables the wake-word toggle.
  - **Lifecycle**: `VoskWakeWordService` is a foreground service
    (`android:foregroundServiceType="microphone"`, `FOREGROUND_SERVICE_MICROPHONE` permission),
    started/stopped only by the user's toggle in `ChatScreen`'s top bar (`ChatViewModel.setWakeWordEnabled`).
    It requires the model to already be present (downloads it first if needed) and `RECORD_AUDIO`
    + (API 33+) `POST_NOTIFICATIONS` permissions, requested inline from `ChatScreen`. On detecting
    the phrase (`org.vosk.android.RecognitionListener.onResult`/`onFinalResult`, checked via the
    hypothesis JSON's `text` field), it notifies `WakeWordEventBus`, which `ChatViewModel` observes
    to kick off `startVoiceInput()` — the same path as tapping the mic button.
  - **Mic arbitration**: `MicArbiter` is a single-owner semaphore (`NONE`/`WAKE_WORD`/
    `SPEECH_RECOGNIZER`/`TTS`) — `VoskWakeWordService` pauses its Vosk recognizer
    (`SpeechService.setPause(true)`) whenever it doesn't hold the mic, and resumes when it's
    released. This prevents the wake-word listener and an active voice-input capture from fighting
    over the microphone.
  - **Not verified on real hardware** — wake-word detection in background/Doze across OEM battery
    optimizers needs manual testing; flagged in `checklist.md`.
- Conversation history is **in-memory only** (`ChatViewModel._uiState.messages`) — never persisted
  to disk, cleared by the trash-can button or on process death. The last 20 turns are sent per
  request (`MAX_HISTORY_TURNS`) to bound request size/cost; the server is stateless per request.

## Offline handling

`NetworkMonitor` (`ConnectivityManager.NetworkCallback` → `StateFlow<Boolean>`) backs every
network-gated action. Two distinct checks exist and are used for different things:
- `isOnline` (generic internet, any transport) — gates login/register/profile/devices-list/chat.
- `isWifiConnected()` (WiFi transport specifically) — gates starting device provisioning, since the
  spec calls out WiFi specifically (the phone needs WiFi hardware active to join the device's AP,
  not just "some internet" which could be cellular).

`presentation/common/OfflineBanner.kt` is the shared banner component (used in `ChatScreen`); other
screens surface the offline case through the same `error: UiText?` field they already use for
other failures, via `UiText.Resource(R.string.common_offline_message)`.

## Secrets

Nothing that must stay out of the (public) repo is hardcoded. `apikey.properties` (gitignored) is
loaded in `app/build.gradle.kts` into `BuildConfig` fields: `SECURITY_KEY`/`SECURITY_SECRET` (WSSE
credential) and `VOSK_MODEL_URL`. Copy `apikey.properties.example` → `apikey.properties` and fill
in real values before building. The LLM API key (Gemini) lives **server-side only**, in
NanoServer's `conf/app.properties` (also gitignored there) — this app never sees it, by design
(see "Chat/LLM proxy" below).

## i18n

All user-facing strings live in `values/strings.xml` (Spanish only, per spec) — no hardcoded
strings in any Composable. The default `values/` bucket doubles as the fallback, so adding e.g.
`values-en/strings.xml` later needs no code changes.

## Testing

- Unit tests exist for the crypto layer (`SecureUtilTest`, `WsseUtilTest` —
  `app/src/test/java/.../data/crypto/`): encrypt/decrypt round-trips, WSSE header shape, and a
  digest verified against a manually-computed SHA-1 vector. `SecureUtil.pickKeyIndex`/`encrypt`
  take an optional `Calendar`/clock parameter specifically so these are deterministic, unlike the
  original `Calendar.getInstance()`-calling reference implementations.
- Run: `./gradlew testDebugUnitTest` (needs `JAVA_HOME` pointed at a JDK 17).
- Not covered by automated tests here (needs real devices/backend, see `checklist.md`): WiFi
  provisioning end-to-end, wake-word detection, STT/TTS quality, and the live Gemini tool-calling
  loop on the backend.

## NanoServer companion changes

Made in the **separate** NanoServer repo (not this one), left as uncommitted working-tree changes
there pending review — do not assume they're pushed anywhere:

- `ResponseType.SC_DUPLICATE_DEVICE(807)` — new code for device-create UUID conflicts.
- `DevicesWebService.java` — added `/devices/list|create|update|remove`, WSSE + `LoginToken`
  authenticated (resolves the owning user server-side via the token, never trusts a client-supplied
  email — stricter than `McpWebService`'s existing looser pattern), with an ownership check on
  update/remove (loaded device's `userEmail` must match the resolved user).
- `ChatWebService.java` (new) — `POST /api/services/chat/converse`. Calls the Gemini REST
  `generateContent` API via the already-present `httpclient`/`jackson-databind` (no new Maven
  dependency, matching this codebase's conservative Spring 4.1.6/Java 8 dependency footprint).
  Declares 5 function tools mirroring the separate `mcp-server/server.py` MCP bridge's tools
  (`list_devices`, `get_home_temperature_humidity`, `get_temperature_humidity_history`,
  `get_home_energy_consumption`, `get_energy_consumption_history`) and executes them **in-process**
  against the same `deviceService`/`energyLogService`/`environmentLogService` beans
  `McpWebService` already uses — the Python bridge is not called and stays as-is, unused by this
  app, avoiding the need to expose it publicly. Tool-call loop capped at 5 iterations.
  **Not validated against a live Gemini API key in this environment** — the JSON request/response
  shape (`contents`/`parts`/`functionCall`/`functionResponse`) matches Gemini's documented REST
  API as of this writing, but should be exercised with a real key + WSSE-signed test call before
  relying on it in production (see `checklist.md`).
- `ServerProperties.GEMINI_API_KEY`/`GEMINI_MODEL`, sourced from `conf/app.properties` exactly like
  `securityKey`/`securitySecret`; placeholders added to `conf/app.properties.example`.
- New DTOs: `DeviceListRequestDto`, `DeviceRemoveRequestDto`, `ChatRequestDto`, `ChatTurnDto`,
  `ChatResponseDto`.
- **Known duplication, not refactored**: `ChatWebService`'s view-type history dispatch (the
  `switch` over `EnvironmentViewType`/`EnergyViewType`) duplicates logic already in
  `McpWebService`. A shared `DeviceQueryUtils` was considered (and is a reasonable follow-up) but
  skipped here to avoid modifying `McpWebService`'s existing, working code without a test harness
  to verify the refactor didn't regress it.
- `DeviceHistoryWebService.java` (new) — `POST /api/services/devices/latestValue` and
  `POST /api/services/devices/history`, added for the Android app's new device detail/history
  screen (see "Device detail & history" above). WSSE + `LoginToken` authenticated with the same
  ownership check as `DevicesWebService.update/remove`. Reuses the existing `EnergyLogDto`/
  `EnvironmentLogDto`/`EnergyLogsDto`/`EnvironmentLogsDto` — no new response DTOs. New request
  DTOs: `DeviceLatestRequestDto`, `DeviceHistoryRequestDto`. Its date-parsing/view-type-dispatch
  code is intentionally a near-duplicate of `ChatWebService.toolEnergyHistory`/
  `toolEnvironmentHistory`, same "known duplication, not refactored" rationale as above.
- Verified: `mvn -o compile` succeeds for the whole project with these changes.
