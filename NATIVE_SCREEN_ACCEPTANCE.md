# Native screen acceptance gate

Status: **REQUIRED for native Compose UI work**

Human interaction authority remains [`NATIVE_INTERACTION_CONTRACT.md`](NATIVE_INTERACTION_CONTRACT.md). This file is the operational QA gate only.

## Risk tiers

| Tier | When | Required proof |
|---|---|---|
| COPY / NON-GEOMETRY | Copy-only change | Workflows stay green; screenshot only if layout can shift |
| VISUAL / LAYOUT | Spacing, chrome, theme, landscape/reachability | Portrait + affected viewport; large text; landscape when geometry/reachability changes |
| TEXT INPUT / IME | Focused fields, keyboard | Focused field visible; IME cannot permanently cover required controls; IME action where defined |
| NAVIGATION / SHARED CHROME | Destinations / shared bars | Affected destinations + representative visuals; dependents at checkpoint |
| WRITE / MONEY | Domain writes | Real saved-domain proof + refreshed UI; duplicate-tap / failure no-partial-write where applicable |
| CHECKPOINT | Screen or shared-helper release | Full required matrix for Auth + Sale + Collect |

## Commands

Run from `phase18/` (single Gradle process — do not wrap in another gradlew):

```text
.\gradlew.bat qaFast
.\gradlew.bat qaScreen -Pqa.screen=collect -Pqa.device=R8VXA02SNKH
.\gradlew.bat qaCheckpoint -Pqa.device=R8VXA02SNKH
```

Set `ANDROID_SERIAL=R8VXA02SNKH` in the shell before device gates when multiple adb devices exist.

### When each is mandatory

- **qaFast** — after any native UI/test/build/doc edit before claiming the change is green.
- **qaScreen** — after edits that affect a specific native screen (`auth` / `sale` / `collect`).
- **qaCheckpoint** — before accepting a screen or shared helper as done; requires `untested=0`.

## Inventory rule

Machine inventory: `phase18/qa/native_control_inventory.tsv`  
Stable tags: `phase18/app/src/main/java/com/saaf/milkshop/nativeui/NativeUiTags.kt`

Every interactive control needs a stable tag and inventory row. Checkpoint fails if `untested > 0`, orphans, duplicate tags, or missing claimed workflow tests exist.

## Screenshot / review rule

Evidence lands on-device under `/sdcard/Download/phase14-native-evidence/native-<screen>-a06/`, then pulls to `phase14-evidence/native-<screen>-a06/`.

PNG existence is not visual PASS. Each capture has a `.review.json` sidecar (`PASS` / `FAIL` / `UNREVIEWED`). Checkpoint prints `unreviewed` count; owner review clears `UNREVIEWED`.

## Package / device safety

- Device of record: Samsung A06 `R8VXA02SNKH` (`SM-A065F`)
- QA app: `com.saaf.milkshop.qa` via `-Psaaf.isolatedQa=true`
- Instrumentation: `com.saaf.milkshop.qa.tests` via `-Psaaf.instrumentationSandbox=true`
- Never install/overwrite ordinary `com.saaf.milkshop`
- Never silently select an arbitrary connected device

## Protected-logic exception

`checkUiOnly` protects domain/data/SQL/bridge baselines. Override only with `-Pallow.logic=true`, which prints `LOGIC EXCEPTION ACTIVE` and lists every protected changed file.

## End-of-change reporting

Report `qaFast` / affected `qaScreen` / `qaCheckpoint` one-line results, inventory untested count, evidence + unreviewed counts, protected-logic changes, and whether the ordinary package was touched.
