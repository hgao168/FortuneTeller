# FortuneTeller — Android Acceptance Report

**Verdict: PASS-WITH-ISSUES**

| | |
|---|---|
| Revision under test | `fb0a6afdc30bb7c57a58e4574fe20cbdda32178f` (verified via `git rev-parse HEAD`) |
| Branch | `staging` |
| Subject | `feat(android): FortuneTeller Android app — palm reading, match, history parity with iOS` |
| Tested by | Independent QA (no source files modified) |
| Date | 2026-10-18 (UTC) |
| Toolchain | JDK 17 (`/home/henry/opt/jdk17`), Android SDK `/home/henry/android-sdk`, Gradle wrapper, `--offline`, warm cache |
| Report file | `qa/fortune-android-acceptance.md` (only file created) |

Both gradle tasks were forced to re-execute with `--rerun-tasks`. The first invocation reported
`testDebugUnitTest UP-TO-DATE` — i.e. the developer's cached result — so it was re-run from scratch
to produce independent evidence. All results below come from tasks that actually executed
(`26 actionable tasks: 26 executed` / `39 actionable tasks: 39 executed`).

---

## 1. Automated tests — PASS

```
cd android && ./gradlew :app:testDebugUnitTest --offline --console=plain --rerun-tasks
BUILD SUCCESSFUL in 6s
26 actionable tasks: 26 executed
```

Parsed from `android/app/build/test-results/testDebugUnitTest/*.xml` (regenerated 2026-10-18T12:49Z):

| Suite | tests | failures | errors | skipped | time (s) |
|---|---|---|---|---|---|
| `ai.movenova.fortuneteller.ModelsTest` | 5 | 0 | 0 | 0 | 0.095 |
| `ai.movenova.fortuneteller.MultipartFormDataTest` | 6 | 0 | 0 | 0 | 0.020 |
| **Total** | **11** | **0** | **0** | **0** | |

**Developer claim of "11 tests, 0 failures" is CONFIRMED** — and confirmed by a fresh execution, not
by inherited build state. Individual cases:

- ModelsTest: `decodesReadingResponse`, `decodesMatchResponse`, `toleratesUnknownAndMissingFields`,
  `decodesFencedJsonAfterBackendStripsIt`, `scopeAndMatchTypeMapFromApiValues`
- MultipartFormDataTest: `contentTypeIncludesBoundary`, `defaultBoundaryIsUnique`, `fieldEncoding`,
  `fileEncodingHeadersAndBytes`, `multipleFieldsOrderingPreserved`, `customMimeType`

Coverage caveat (see defect D9): no test asserts the *actual* field names emitted by
`ApiClient.analyzePalm` / `matchPalm` (e.g. `user_id`, `language`, `image_a`). The encoder is tested
generically, so contract-correctness of the client calls rests on static review, not tests.

## 2. Build — PASS

```
cd android && ./gradlew :app:assembleDebug --offline --console=plain --rerun-tasks
BUILD SUCCESSFUL in 8s
39 actionable tasks: 39 executed
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`
- Size: **18,094,862 bytes (18 MB)** — matches the developer's "~18 MB" claim
- mtime: `2026-10-18 14:49:12 +0200`
- `output-metadata.json` present alongside it

Merged manifest confirms `minSdkVersion=26`, `targetSdkVersion=36`, `versionName=1.0.0`.
No compiler warnings escalated to errors; no dependency resolution occurred (fully offline).

## 3. Backend contract parity — PASS

Read `backend/app/main.py` (lines 64–134) against `ApiClient.kt` (lines 48–106) and
`MultipartFormData.kt`.

### `POST /analyze-palm` — field names exact

| Backend (`main.py`) | Required | Android (`ApiClient.kt:54-58`) | Match |
|---|---|---|---|
| `image` (UploadFile) | yes | `addFile("image", "palm.jpg", "image/jpeg", …)` | ✅ |
| `scope` (Form) | yes | `addField("scope", scope.apiValue)` | ✅ |
| `language` (Form) | no | `language?.takeIf { isNotBlank }?.let { addField("language", it) }` | ✅ |
| `user_id` (Form) | no | `userId?.takeIf { isNotBlank }?.let { addField("user_id", it) }` | ✅ |

### `POST /match-palm` — field names exact

| Backend (`main.py`) | Required | Android (`ApiClient.kt:72-78`) | Match |
|---|---|---|---|
| `image_a` (UploadFile) | yes | `addFile("image_a", "palm_a.jpg", "image/jpeg", …)` | ✅ |
| `image_b` (UploadFile) | yes | `addFile("image_b", "palm_b.jpg", "image/jpeg", …)` | ✅ |
| `match_type` (Form) | yes | `addField("match_type", matchType.apiValue)` | ✅ |
| `person_a_birth` (Form) | yes | `addField("person_a_birth", personABirth)` | ✅ |
| `person_b_birth` (Form) | yes | `addField("person_b_birth", personBBirth)` | ✅ |
| `language` (Form) | no | same optional pattern | ✅ |

### Enumerations — exact

- `ALLOWED_SCOPES = {today, month, year, long_term}` (`main.py:14`) ↔
  `ReadingScope` apiValues `today|month|year|long_term` (`Models.kt:9-13`) ✅
- `ALLOWED_MATCH_TYPES = {romantic, friend}` (`main.py:15`) ↔
  `MatchType` apiValues `romantic|friend` (`Models.kt:22-24`) ✅

### Transport details

- `X-API-Key` header: backend expects alias `X-API-Key` (`main.py:51`) and only enforces when the
  `API_KEY` env var is set; Android sends it only when non-blank (`ApiClient.kt:86-88`). ✅
- 8 MB cap: `MAX_IMAGE_BYTES = 8 * 1024 * 1024` (`main.py:16`), enforced per image at
  `main.py:82-83` and `main.py:119-120`. Client maps 413 → `ApiError.TooLarge` (`ApiClient.kt:101`). ✅
  (Client-side *pre-flight* guard is missing — defect D2.)
- Error mapping: 401 → `Unauthorized`, 413 → `TooLarge`, 400 → `BadRequest(detail)`,
  other → `Server(code)` (`ApiClient.kt:98-104`). Backend emits exactly these four codes. ✅
- Multipart encoding is hand-built and byte-exact (`MultipartFormData.kt:41-65`): CRLF line endings,
  `text/plain; charset=UTF-8` for fields, correct `Content-Disposition` with `filename`, terminating
  `--boundary--\r\n`. Content-Type header set from `form.contentType` (`ApiClient.kt:84`). ✅

## 4. Response model parity — PASS

`backend/app/schemas.py` vs `data/Models.kt`:

**`PalmReadingResponse`** — `scope`, `summary`, `sections[].title`, `sections[].text`, `advice`,
`disclaimer`: all present (`Models.kt:32-45`), all with defaults so a sparse payload still decodes. ✅

**`PalmMatchResponse`** — `match_type` → `@SerialName("match_type") val matchType` (`Models.kt:49`),
`score`, `summary`, `strengths`, `tensions`, `advice`, `disclaimer`: all present. ✅ Snake_case handled
correctly and proven by `ModelsTest.decodesMatchResponse` (asserts `response.matchType == "romantic"`
from a `match_type` key).

Decoder is configured `ignoreUnknownKeys = true`, `isLenient = true`, `explicitNulls = false`
(`ApiClient.kt:27-32`) — forward-compatible with new backend fields. ✅

`score` range 0..100 is enforced server-side (`schemas.py:24`, `ge=0, le=100`) but **not** validated
client-side — see defect D8.

## 5. iOS feature parity — PASS-WITH-GAPS

Reference: `ios/FortuneTeller/*.swift` (1859 LOC across 13 files).

| Feature | iOS | Android | Status |
|---|---|---|---|
| Read flow, 4 scopes | `PalmModels.swift:4-7`, `ContentView.swift:135` | `ReadScreen.kt:194-205`, `Models.kt:9-13` | ✅ |
| Capture (camera) in read | `ContentView.swift:153`, `PalmCameraView.swift` | `ReadScreen.kt:77,175` (`TakePicture` + FileProvider) | ✅ |
| Gallery picker in read | `PalmPhotoPicker.swift` | `ReadScreen.kt:97,182` (`PickVisualMedia`) | ✅ |
| Reading result: summary / sections / advice / disclaimer | `ReadingResultView.swift` | `ReadingResultScreen.kt:93-130` | ✅ |
| Match: 2 images | `MatchView.swift:65-79` | `MatchScreen.kt:141-198` | ✅ |
| Match: romantic / friend | `PalmModels.swift:36-37` | `MatchScreen.kt:126-139` | ✅ |
| Match: 2 birth dates | `MatchView.swift:44,49` | `MatchScreen.kt:182-196` (`DatePickerDialog`) | ✅ (format differs, D10) |
| Match: camera capture | `MatchView.swift:17,65,77,147` | none — gallery only | ❌ **D4** |
| Match result: score + strengths / tensions / advice | `MatchResultView.swift:23-50` | `MatchResultScreen.kt:81-135` | ✅ |
| History: list with thumbnails | `HistoryView.swift:96-134` | `HistoryScreen.kt:116-133` | ✅ |
| History: save | `ReadingResultView.swift:47`, `MatchResultView.swift:89` | `ReadingResultScreen.kt:136-149`, `MatchResultScreen.kt:148-161` | ✅ |
| History: delete | `HistoryView.swift:34-35` (`onDelete`) | `HistoryScreen.kt:182-188` (delete icon) | ✅ |
| History: open detail | `HistoryView.swift:69,73` | `MainActivity.kt:164-172` | ✅ functional, **image missing — D3** |
| Language switch en / zh | `LanguageRuntime` | `LanguageMenu` (`CaptureSupport.kt:51`), `AppSettings.kt` | ✅ |
| Two thumbnails stored for match | `PalmStore.swift:45` | `imageBase64` + `imageBBase64` (`Models.kt:68-69`) | ✅ stored; only A is rendered (minor) |

Genuine gaps: **camera is absent from the match flow**, and **history detail drops the saved image**
even though it is persisted and shown in the list.

## 6. i18n coverage — PASS

Programmatic comparison of `res/values/strings.xml` vs `res/values-zh/strings.xml`:

- `values`: **62 keys**; `values-zh`: **62 keys**
- Missing in zh: **none**; extra in zh: **none**
- Empty values: **none** in either locale
- Identical text in both (4): `app_name` ("FortuneTeller" — brand, correct),
  `history_score` ("%1$d%%" — format string, correct), `language_english` ("English" — correct),
  `language_chinese` ("简体中文" — correct, endonym)

Also confirmed: the build compiles, so every `R.string.*` referenced in Kotlin resolves; there are no
dangling resource references.

## 7. Security / hygiene — PASS (with two notes)

- **No hardcoded secrets.** `ApiClient` takes `baseUrl`/`apiKey` from `BuildConfig`
  (`ApiClient.kt:39-40`); `build.gradle.kts:10-12` reads them from Gradle properties with safe
  defaults (`": []`-free: API key defaults to `""`). Verified in the generated
  `BuildConfig.java`: `BACKEND_API_KEY = ""`, `BACKEND_BASE_URL = "http://10.0.2.2:8000"`.
  `git ls-files android | xargs grep` for `sk-`, `API_KEY=`, `Bearer `, `AIza`, `password` → **no
  tracked matches**. `local.properties` contains only `sdk.dir` and is gitignored
  (`android/.gitignore:4`). ✅
- **No credential logging.** Zero `Log.`, `println`, `System.out` calls in `app/src/main/java`. The
  only `apiKey` references are the constructor and the header assignment (`ApiClient.kt:40,86,87`). ✅
- **Manifest permissions** (`AndroidManifest.xml:4-10`): `INTERNET` (required), `CAMERA` (requested
  at runtime and used by the capture flow), `READ_MEDIA_IMAGES` (**declared but never requested or
  used** — gallery goes through the system Photo Picker, which needs no permission). No
  `READ_EXTERNAL_STORAGE`, no location/contacts/phone state, `android.hardware.camera` marked
  `required="false"`. FileProvider is `exported="false"` with a narrow `cache-path` grant
  (`file_paths.xml`). ⚠️ one unnecessary permission — defect D6.
- ⚠️ `android:allowBackup="true"` (`AndroidManifest.xml:13`) — saved readings, including base64 palm
  JPEGs, are eligible for cloud backup. See D6.
- ⚠️ Cleartext default base URL — see **D1**, the most material finding in this report.

## 8. Persistence — PASS (static verification only)

`PalmStore.kt` serialises the full `List<SavedReading>` to `context.filesDir/saved_readings.json`
(`PalmStore.kt:16,44-46`) and reloads it in `init` (`PalmStore.kt:20-22`), so history survives process
death. `SavedReading` is `@Serializable` with every field carrying a default except `id`, `createdAt`,
`kind` (`Models.kt:59-70`), so older records still decode after a schema addition; thumbnails are
base64 JPEG strings inside the same document (no file-path breakage if the app is moved/restored).

**This is static verification.** No emulator or device is available in this environment, so
"survives process death" is asserted from code structure, **not** observed at runtime. The atomicity
weakness is noted as D7.

---

## Defects

### D1 — Debug builds cannot reach the default backend (cleartext blocked) — **HIGH**
- **Where:** `android/app/build.gradle.kts:10-11` (`?:"http://10.0.2.2:8000"`) +
  `android/app/src/main/AndroidManifest.xml` (no `android:usesCleartextTraffic`, no
  `networkSecurityConfig`). Merged manifest confirms `targetSdkVersion=36`.
- **Expected:** a debug build with the default configuration completes a reading against the local
  backend.
- **Actual:** from Android 9 (API 28) cleartext is denied by default; OkHttp consults
  `NetworkSecurityPolicy`, so every request throws `java.net.UnknownServiceException: CLEARTEXT
  communication to 10.0.2.2 not permitted by network security policy`, surfaced as the generic
  network error string.
- **Repro:** `assembleDebug`, install on any API 28+ emulator with defaults, tap Analyze with an image.
- **Note:** production is unaffected provided `BACKEND_BASE_URL` is https; this breaks local
  dev/QA end-to-end only. **Not reproduced on a device** (no emulator available) — platform-policy
  finding from static analysis.
- **Fix direction:** debug-only manifest or `res/xml/network_security_config.xml` permitting cleartext
  for `10.0.2.2`/`localhost` only.

### D2 — Claimed "8 MB client-side guard" does not exist — **MEDIUM**
- **Where:** `Images.kt:27` (`compressForUpload` = downsample to 1600 px @ quality 85, no size check);
  call sites `ReadScreen.kt:224-226`, `MatchScreen.kt:217-221`.
- **Expected:** commit message: "8MB client-side guard via JPEG compression"; a payload over 8 MB
  should be rejected locally with the localized `error_image_too_large`.
- **Actual:** `grep` for `1024|8192|8_000_000` across `app/src` returns **zero** matches. Only the
  server's 413 is mapped to `ApiError.TooLarge`. In practice 1600 px @ 85 lands far below 8 MB, so the
  handler is effectively dead code and the claim in the commit message is overstated.
- **Repro:** static — no code path compares `jpeg.size` to a cap.

### D3 — History detail renders no palm image (iOS parity gap) — **MEDIUM**
- **Where:** `MainActivity.kt:186-201` — `imageABytes = null`, `imageBBytes = null`,
  `imageBytes = null` for the `historyDetail` route.
- **Expected:** iOS `HistoryView.swift:69,73` passes `reading.thumbnailImage` into the detail view.
- **Actual:** detail screen shows text only, although `SavedReading.imageBase64` / `imageBBase64` are
  persisted (`ReadingResultScreen.kt:145`, `MatchResultScreen.kt:156-157`) and already decoded for the
  list (`HistoryScreen.kt:116`). Fix is a one-liner: `Images.bitmapFromBase64(saved.imageBase64)`.
- **Repro:** save a reading → History → tap `›`.

### D4 — Match flow has no camera capture (iOS parity gap) — **MEDIUM**
- **Where:** `MatchScreen.kt:80-85` — only `PickVisualMedia` launchers; `ReadScreen.kt:77` shows the
  camera path exists and is reusable.
- **Expected:** iOS `MatchView.swift:65,77` offers camera for person A and B (`showCamera`).
- **Actual:** users can only pick from the gallery in the match flow.
- **Repro:** open the Match tab — the only image affordance is "pick image".

### D5 — Result routes are not process-death safe — **LOW**
- **Where:** `MainActivity.kt:60-67,80` — `AppNavPayload` held in `remember` only.
- **Expected:** navigating to `readingResult` / `matchResult` always shows a populated result.
- **Actual:** the nav back stack is restored by `NavController`, but the payload is not; after process
  death the screen renders an empty summary, `${response?.score ?: 0}%` → "0%", and an empty sections
  list (`ReadingResultScreen.kt:97`, `MatchResultScreen.kt:97`). No crash, but a misleading screen.
- **Repro:** reach a result screen, "Don't keep activities" / kill the process, return to the app.
- **Note:** `HistoryDetailRoute` (`MainActivity.kt:180-184`) calls `onDone()` during composition when
  `saved == null` — a side effect in composition, same root cause.

### D6 — Over-permissioning and cloud backup of palm images — **LOW**
- **Where:** `AndroidManifest.xml:6` (`READ_MEDIA_IMAGES`, never requested, gallery uses the Photo
  Picker), `AndroidManifest.xml:13` (`allowBackup="true"`).
- **Expected:** least-privilege manifest; palm photos are sensitive biometric-adjacent data.
- **Actual:** an unused runtime permission is declared, and saved readings (base64 JPEG thumbnails)
  are eligible for cloud backup. Consider `android:dataExtractionRules` / `allowBackup="false"`.
- **Repro:** static.

### D7 — Non-atomic history write can lose all readings — **LOW**
- **Where:** `PalmStore.kt:44-46` (`file.writeText`) and `PalmStore.kt:39-42` (decode failure →
  `getOrDefault(emptyList())`).
- **Expected:** a crash or kill mid-write leaves the previous history intact.
- **Actual:** a truncated JSON file makes `load()` silently return an empty list — the whole history
  disappears with no user-visible error. `AtomicFile`/temp-file-then-rename plus an `IOException`
  branch would fix it.
- **Repro:** static.

### D8 — `score` not validated or clamped client-side — **LOW**
- **Where:** `Models.kt:50` (`val score: Int = 0`), rendered at `MatchResultScreen.kt:97` and
  `HistoryScreen.kt:171`.
- **Expected:** `schemas.py:24` guarantees 0..100.
- **Actual:** the client accepts any integer and renders it verbatim (`-5%`, `140%`). Only reachable
  through a non-conforming server/proxy, so low impact.

### D9 — Contract tests do not assert real request field names — **LOW**
- **Where:** `app/src/test/.../MultipartFormDataTest.kt` (generic encoder assertions),
  `ModelsTest.kt` (decode assertions).
- **Expected:** a test pinning `image`/`scope`/`language`/`user_id` and
  `image_a`/`image_b`/`match_type`/`person_a_birth`/`person_b_birth` as emitted by `ApiClient`.
- **Actual:** the field names verified in check 3 are verified *only* by reading `ApiClient.kt`; a
  rename would not fail any test. `ApiClient` is not injectable for a fake transport (no interface),
  which is why this gap exists.

### D10 — Birth-date wire format differs from iOS — **INFO**
- Android sends `"YYYY-MM-DD"` (`MatchScreen.kt:92`); iOS sends ISO-8601 datetime
  (`APIClient.swift:87-88`). The backend interpolates the raw string into the prompt
  (`backend/app/analyzer/match.py:33-36`) and only checks non-empty (`main.py:109-110`), so there is
  **no functional break** — but the contract is looser than iOS and unvalidated server-side.

### D11 — Default scope differs from iOS — **INFO**
- Android `MONTH` (`ReadScreen.kt:71`) vs iOS `TODAY` (`ContentView.swift:26`). Cosmetic.

---

## Out of scope / BLOCKED

- **Native UI runtime verification — BLOCKED.** No Android emulator and no iOS simulator are available
  in this environment (WSL). Nothing in this report is a device or simulator observation. Launch,
  navigation, camera capture, Photo Picker, runtime permission prompts, composition/layout, and
  process-death behaviour are **not** runtime-verified; checks 5–8 are static review plus build
  evidence.
- **Live backend round-trip — NOT RUN.** No server was started and no request was issued; contract
  parity is a source-level comparison of both sides. (Compounded by D1, which would block a cleartext
  round-trip from a debug build anyway.)
- **Release build — NOT RUN.** Only `assembleDebug` was in scope. Note that `release` has
  `isMinifyEnabled = false` and no `signingConfig` (`build.gradle.kts:35-38`), so R8 shrinking and
  release signing are untested.

## Reproduction summary

```bash
export JAVA_HOME=/home/henry/opt/jdk17
export ANDROID_HOME=/home/henry/android-sdk ANDROID_SDK_ROOT=/home/henry/android-sdk
cd /home/henry/workspace/FortuneTeller/android

./gradlew :app:testDebugUnitTest --offline --console=plain --rerun-tasks   # BUILD SUCCESSFUL in 6s
./gradlew :app:assembleDebug      --offline --console=plain --rerun-tasks # BUILD SUCCESSFUL in 8s

# 11 tests / 0 failures
grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
  app/build/test-results/testDebugUnitTest/TEST-*.xml

ls -l app/build/outputs/apk/debug/app-debug.apk   # 18094862 bytes
```

`git status` on `android/` remained clean throughout; the only file written by QA is this report.
