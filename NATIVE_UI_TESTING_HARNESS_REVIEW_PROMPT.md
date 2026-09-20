# Prompt for external AI review — Saaf Milk native UI testing harness

Copy everything below the line into another AI. Ask it to critique, stress-test, and improve the idea before we implement.

---

## Your role

You are a senior Android / Compose / QA architect. Review the proposed **non-negotiable testing plan** for a native Jetpack Compose migration. Be blunt. Find holes, overkill, missing failure modes, and cheaper equivalents. Do **not** rewrite product business logic. Do **not** invent accounting formulas. Focus on the **testing process and harness**.

Deliver:

1. Verdict: adopt / adopt with changes / reject (and why).
2. Concrete failure modes this plan would still miss.
3. What is over-engineered vs under-specified.
4. A revised minimal checklist (bullet form) if you change the plan.
5. Exact Android/Compose APIs or patterns you recommend for IME occlusion, viewport bounds, and screenshot dumps on a physical device.
6. How to keep the gate enforceable by agents (not tribal knowledge).

---

## Product context (read carefully)

**Saaf Milk** is a local-first Android milk-shop ledger. Authoritative accounting, permissions, corrections, and security live in native Kotlin (Room / Phase 18). A hybrid React WebView still exists as a behavioral oracle / packaging path, but Phase **14.8** is migrating role UIs to **Compose**.

Current active lane:

- Auth / Welcome (native) — largely verified
- Cashier **Sale** (native) — largely verified under an interaction contract
- Cashier **Collect** (native) — implemented with partial tests; visual/layout gate incomplete
- Activity / More — not built; temporary preview notices only

Visual language for worker roles: **Counter Ledger** (warm ivory `#F4F1E8`, deep green `#173F32`, yellow commit `#F2C85B`, Manrope, ≥48dp targets). Auth stays light-branded even if system is dark; authenticated work area historically follows system theme (tension exists with recent light-forced Cashier experiments — call this out if your test plan depends on theme).

Device of record: Samsung A06, serial `R8VXA02SNKH` (`SM-A065F`).

Packages (hard rule):

- Ordinary app `com.saaf.milkshop` — **do not install/overwrite** during agent QA
- User QA: `com.saaf.milkshop.qa` via Gradle `-Psaaf.isolatedQa=true`
- Instrumentation: `com.saaf.milkshop.qa.tests` via `-Psaaf.instrumentationSandbox=true`

Launcher remains `HybridActivity`. `NativePreviewActivity` is the debug/native review host.

---

## Existing authority docs (conceptual)

These already exist in the repo and constrain any harness:

1. **`NATIVE_INTERACTION_CONTRACT.md`** — Required gate from Phase 14.8.3 onward. Every visible interactive control needs:
   - written workflow (start state, action, state change, visible result)
   - stable Compose test tag
   - device test that performs the real action
   - assertions for resulting state **and** user-visible evidence
   - cancel / dismiss / disabled / error / retry when those states exist
   - For writes: prove saved domain result + refreshed UI; failures prove no partial write
   - Screen acceptance: inventory every control → match workflow row → run device tests → report **passed / failed / untested**; keep checkpoint open if any control is silent or untested

2. **`PHASE14_8_4_CASHIER_COLLECT_BLUEPRINT.md`** — Collect control table + material states (loading, advance confirmation, closed day, missing setup, session expired, etc.). Preview companion HTML is referenced as `design/refined-counter-ledger/collect.html` but **`design/` is missing from this workspace tree** — visual HTML authority may be unavailable here.

3. **`PHASE14_8_2_COUNTER_LEDGER_WORKFLOWS.md`** — Shared worker rules: dock repeats consequence; saving disables commit; success replaces form in-viewport (no scroll-to-discover); errors keep draft; setup gaps name Owner/Admin.

4. **Sale hardening already present** (patterns to reuse, not reinvent):
   - `NativeFoundationUiTest` — IME Done clears focus; landscape usability; end-to-end Room writes
   - `NativeSaleHardeningContractTest` — 150% text, dark theme, TalkBack **semantics** (not full TalkBack service), long lists, closed day, missing refs
   - Auth uses `captureToImage()` for contrast pixel checks (in-memory, not PNG files)
   - Hybrid Phase 12 has PNG dump infrastructure (`Phase12VisualBaselineCaptureTest`) — native Cashier evidence PNGs were historically **manual** A06 captures, not automated by Sale tests

5. **Collect coverage today (honest):**
   - Facade tests: scoped load, Room write, operator denied
   - Screen contract: search/select, shortcuts, note, record + duplicate-tap, advance confirm/cancel, success actions, closed day, load retry, save error keeps amount
   - Foundation: nav to Collect, one Room write path, View-record preview notice
   - **Gaps:** customer/payment Cancel; dirty Back keep/discard; invalid amount matrix; amount reset on customer switch; Pay-full disabled when ≤0; destination retention on Collect another; IME Done; no-match search; landscape/150%/TalkBack for Collect; IME occlusion; PNG dumps; `NATIVE_INTERACTION_CONTRACT.md` still says Collect is “not connected” (doc drift)

6. **UI folder `AGENTS.md`** says presentation-only for the React UI tree. Native Phase 14.8 work uses facades/ViewModels as thin adapters — **do not invent a second accounting engine in Compose**.

7. Standing owner preference after UI tweaks: **commit → install `.qa` → relaunch** every time so physical review is possible.

---

## The proposed plan (what we want you to review)

### Thesis

This is **not Collect-specific**. After **ANY** native UI change (new control, layout, theme, nav, interaction-affecting copy), the same non-negotiable gate must run for every **affected** surface.

### Trigger table

| Change type | Gate scope |
|---|---|
| New control on existing screen | Full describe→test for that control + regression of sibling workflows on that screen |
| Layout / spacing / keyboard / theme | Full look/IME/PNG suite for that screen |
| Copy-only (no behavior) | Update inventory visible-result text; keep workflows green; PNG if layout can shift |
| Navigation / shared chrome | All destinations sharing that chrome |
| New screen | Full inventory; no silent/untested control at first checkpoint |
| Temporary preview destination | Written temporary contract + test; replace both when real destination ships |
| Shared helper (theme, tags, preview activity) | All live native destinations that depend on it are in-scope until proven otherwise |

### Four layers (every time)

```text
UI change
  → Layer 0 Describe inventory (workflow + logic + look contract)
  → Layer 1 Stable tags (NativeUiTags)
  → Layer 2 Workflow + Room proofs (Facade / Screen contract / Foundation)
  → Layer 3 Look: IME occlusion, viewport bounds, landscape, large text, semantics, PNG dumps
  → Report: passed / failed / untested=0
  → Commit + install com.saaf.milkshop.qa + relaunch on A06
```

#### Layer 0 — inventory columns (per control)

Control | Start state | User action | Logic change | Visible result | Look contract (≥48dp, tokens, IME/landscape/large-text visibility) | Tag | Device test method | Evidence PNG id

#### Layer 2 — test class split (already used)

1. Facade — permission + Room truth when reads/writes are authoritative  
2. Screen contract — every control’s visible state change  
3. Foundation — real `NativePreviewActivity` path + ≥1 end-to-end write when applicable  

#### Layer 3 — shared harness idea

New shared helpers (e.g. `NativeLayoutEvidence.kt`) used by **all** native screens:

**Automated asserts (fail CI/device suite):**

- Soft keyboard up → focused field `getBoundsInRoot()` bottom stays **above** IME top (WindowInsets / visible display frame)
- Primary commit / critical follow-ups remain in visible window **or** scrollable into it without permanently covering the focused field
- Landscape + 150% text: core controls reachable (`performScrollTo` + `assertIsDisplayed`)
- TalkBack semantics on primary inputs/actions/live regions (same depth as Sale today — semantics only)

**Screenshots:**

- Compose/window capture → `phase14-evidence/native-<screen>-a06/` + manifest TSV (scenario → file → inventory row)
- Fail if capture fails; **human** reviews pixels (no cross-device pixel-diff goldens — judged too brittle)
- Minimum per screen: default ready, keyboard-risk state, success/error or blocker, any dialog/sheet touched by the change

#### Enforceability idea

- Living control tables in `NATIVE_INTERACTION_CONTRACT.md`
- New gate doc `NATIVE_SCREEN_ACCEPTANCE.md`
- Machine checklist / meta-test: fail if any tagged interactive control for a screen prefix lacks an inventory row / claimed test method (orphan detector)

### First compliance pass (after harness exists)

Retrofit **Auth, Sale, Collect** to the universal bar so the process is real. Future screens (Activity, More, Operations, leadership) enter under this process from day one.

### Explicit non-goals

- Pixel-perfect golden baselines across devices  
- Full AccessibilityService TalkBack walks (unless you argue they are necessary)  
- Implementing Activity/More product surfaces in this harness work  
- Touching ordinary `com.saaf.milkshop` installs  
- Redesigning visuals for taste unless Layer 3 asserts fail  
- Replacing hybrid Phase 12 visual suite (native gate is separate)

### Implementation order we sketched

1. Write `NATIVE_SCREEN_ACCEPTANCE.md` + wire into `NATIVE_INTERACTION_CONTRACT.md`  
2. Build shared layout/PNG + inventory-orphan harness  
3. Retrofit Auth / Sale / Collect  
4. Run on A06; publish per-screen passed/failed/untested; commit; reload `.qa`

---

## Pain that motivated this

- Collect shipped with green-ish tests but looked wrong (Material / dark / misaligned amount field with redundant `PKR` prefix)
- Agents optimized for passing assertions over visual fidelity and Counter Ledger authority
- Interaction contract requires untested=0, but inventories were incomplete and docs drifted
- Sale had better hardening than Collect; no shared “after every UI change” ritual beyond ad-hoc judgment
- Owner now requires: describe every control, prove workflow + look (including keyboard covering content / off-screen), then commit and reload every time

---

## Questions for you (answer all)

### A. Process design

1. Is “after ANY UI change, full Layer 0–3 for affected surfaces” the right bar, or will it create agent/developer avoidance and skipped gates? Propose a **risk-tiered** alternative if needed (e.g. copy-only vs layout vs write-path) that still prevents silent controls and keyboard occlusion regressions.
2. Should description (Layer 0) be required **before** code, or is “same PR as code, blocking merge/checkpoint” enough?
3. How should shared-chrome changes (Cashier bottom nav) force multi-screen runs without making every padding tweak a 40-minute device session?

### B. Technical harness

4. On API 35 / Samsung A06 Compose UI tests, what is the most reliable way to assert “focused field not under IME”? Compare: `WindowInsets.ime`, `ViewCompat.getRootWindowInsets`, `getWindowVisibleDisplayFrame`, Espresso `IdlingResource`, `performImeAction` only. Recommend one primary approach + fallback.
5. Should IME occlusion tests run in **isolated Compose host** (`createComposeRule`) or **full Activity** (`createAndroidComposeRule<NativePreviewActivity>`)? Tradeoffs for flakiness vs realism.
6. For PNG dumps: device path + `adb pull` to `phase14-evidence/` — any better pattern for Windows host + multiple devices (`R8VXA02SNKH` vs wireless ADB)?
7. Is an orphan meta-test (tags without inventory rows) worth it, or will it become noisy? Prefer TSV asset, Kotlin sealed inventory, or annotation on tests?
8. Should landscape + 150% text + IME be **combinatorial** (expensive) or separate axes with one “worst case” combo?

### C. Visual authority

9. Given `design/` HTML is missing from this workspace, what should the look contract cite instead (evidence PNGs, Counter Ledger token doc, blueprint only)?
10. Human PNG review: how do you prevent “PNG exists ⇒ pass” theater? Propose a required review artifact (checklist, annotated failures, size/bounds sidecar JSON).

### D. Agent enforceability

11. Write a **10-line agent rule** that would have prevented the Collect visual failure mode (dark Material dump + redundant PKR + no IME/look proof).
12. What should the mandatory end-of-change report look like (exact template)?

### E. Scope control

13. What would you **cut** from our plan on day one and still catch 80% of regressions?
14. What would you **add** that we missed (rotation during IME, multi-window, TalkBack focus order, screenshot under system dark + light, flaky-test quarantine, etc.)?

---

## Constraints you must respect in recommendations

- Do not propose editing frozen business formulas or widening Cashier to full financial overview.
- Do not propose installing over ordinary `com.saaf.milkshop`.
- Milk/Yogurt remain validated product scope; do not invent carts/printers/payment hardware.
- Temporary preview destinations are allowed only with explicit temporary contracts + tests.
- Prefer extending existing Sale/Auth patterns over greenfield frameworks.
- PowerShell host on Windows; Gradle property quoting matters (`-Psaaf.isolatedQa=true`).

---

## Output format (required)

```text
VERDICT: ...
TIERING: ...          # if you recommend risk tiers
CUTS: ...             # what to remove day one
ADDS: ...             # what we missed
IME_APPROACH: ...     # primary + fallback APIs
HOST_RULE: ...        # compose-rule vs activity-rule for look tests
ORPHAN_CHECK: ...     # yes/no + mechanism
PNG_REVIEW: ...       # how humans cannot rubber-stamp
AGENT_RULE_10_LINES:
  ...
REPORT_TEMPLATE:
  ...
REVISED_MINIMAL_CHECKLIST:
  - ...
RISKS_STILL_OPEN:
  - ...
```

Challenge the plan. If the four-layer model is theater, say so and propose the smallest gate that still matches `NATIVE_INTERACTION_CONTRACT.md`.
