# Phase 7 — Hybrid Shell and Build-Proof Vertical Slice

**Status: COMPLETE — 2026-08-28**

## 1. Purpose and boundary

Phase 7 proves the complete packaged path before broad screen wiring:

> React → explicit web build → packaged Android assets → origin-restricted WebView → canonical bridge message → Kotlin authentication/authorization → Room → response → React refresh.

The proof slice deliberately implements only eight contract actions: bridge information, owner-bootstrap status, first-owner creation, login, logout, current session, master-data read, and product creation. The other Phase 6 actions remain specified but are not falsely advertised as implemented runtime operations.

## 2. Implemented shell

- `HybridActivity` is the launcher and loads `https://appassets.androidplatform.net/assets/hybrid/index.html` through AndroidX `WebViewAssetLoader`.
- The inherited `MainActivity` remains compiled and directly launchable by the established native regression tests, but it is no longer the product launcher.
- The WebView accepts the `saafMilkNative` asynchronous message listener only from the packaged app-assets origin and main frame.
- File/content access, DOM storage, mixed content, and arbitrary external navigation are disabled. The app requests no Internet permission.
- A hybrid-only no-title theme gives React the application viewport while Android retains system-bar ownership.
- Renderer termination produces a safe restart message and does not claim that data was lost.

AndroidX WebKit `1.17.0` is used for the current stable asset-loader/message-listener APIs ([official AndroidX WebKit release notes](https://developer.android.com/jetpack/androidx/releases/webkit)). The app still supports its existing Android API 26 minimum.

## 3. Authenticated bridge slice

`HybridBridgeDispatcher` strictly validates the Phase 6 envelope, contract version, UUID request ID, exact payload fields, authentication mode, scalar formats, and 1 MiB request limit. It rejects unsupported actions and duplicate in-flight request IDs without exposing exceptions, SQL, paths, or secrets.

The implemented actions are:

| Action | Native behavior |
|---|---|
| `system.bridgeInfo` | Returns contract version/hash and the eight actually supported Phase 7 actions. |
| `query.bootstrapStatus` | Reads whether first-owner setup is required. |
| `command.auth.bootstrapOwner` | Creates the deliberate first owner, native credential/recovery state, audit entry, and memory-only session. |
| `command.auth.login` | Uses the existing Argon2id security service and returns an opaque memory-only session handle. |
| `command.auth.logout` | Invalidates the native session and clears the in-memory UI session. |
| `query.currentSession` | Revalidates the session and returns native identity, role, and effective permissions. |
| `query.masterData` | Uses the frozen Phase 5 permission-aware read model. |
| `command.createProduct` | Requires native `MANAGE_MASTER_DATA`, reuses `ProductCatalogService`, writes Room state, and records a durable actor snapshot. |

React never supplies an actor ID or authoritative role. The session handle is kept only in React/native application memory and is never written to localStorage, sessionStorage, cookies, URLs, logs, DOM text, or backups.

## 4. React proof screen

The active packaged React entry now provides:

- contract version/hash/action negotiation before any account or business request;
- deliberate first-owner setup with native password-policy errors;
- one-time recovery-code display and explicit acknowledgement;
- login and logout;
- resume-time session revalidation;
- role-aware usability while retaining native authorization;
- native product listing;
- an awaited product-create command followed by a fresh native master-data read;
- plain success/error messages with no fake save state.

The supplied AI Studio screens and mock view model remain in the source as Phase 8 migration input, but the packaged Phase 7 entry does not import or execute them. It does not read or write mock/localStorage business data.

## 5. Explicit web-build stage

`scripts/build-web-ui.ps1` performs the separate web stage:

1. verify the frozen bridge contract;
2. restore locked npm dependencies when needed;
3. run the TypeScript/Vite production build;
4. replace only `phase18/app/src/main/assets/hybrid/` with the generated output.

Gradle does not run npm. Android `preBuild` only verifies that the separately generated packaged index exists and contains its ES-module entry, then runs the existing bridge drift gate.

## 6. Verification

Physical device: `SM-A065F` (`RFCX116CYYX`).

- bridge contract version 1 / 78-operation manifest drift check — PASS;
- TypeScript check and production Vite build — PASS;
- relative ES-module/CSS asset packaging — PASS;
- Kotlin and Android-test Kotlin compilation — PASS;
- debug APK and test APK assembly — PASS;
- debug APK installation and hybrid launcher — PASS;
- focused Phase 7 device suite — 3/3 PASS;
- complete connected-device regression — 127/127 PASS, 0 skipped, 0 failed, 0 errors;
- host-side debug unit tests — 7/7 PASS;
- clean-install first-owner viewport/system-bar screenshot inspection — PASS after removing the inherited title bar and correcting status-icon contrast;
- frozen Phase 18 logic specification and SQL blueprint hashes — unchanged.

The device tests prove owner setup messages against an isolated Room database, secret non-echo, lossless values, caller-supplied-role rejection, cashier direct-bridge denial, durable actor audit, login, packaged asset origin/settings, product save/readback, foreground resume, and logout.

## 7. Deliberately later work

Phase 7 does not claim broad product completion:

- only eight of the 78 contract operations are runtime-dispatched;
- the product form is proof of the shared bridge pattern, not the completed management screen;
- remaining account recovery, staff administration, transactions, ledgers, reports, settings, costing, close, and backup screens belong to Phase 8 screen-by-screen migration;
- process recreation, keyboard/back-stack depth, renderer recovery acceptance, and remaining runtime edge cases stay in Phase 9;
- the old native activity remains as a tested safety/reference implementation until later cleanup gates explicitly remove it;
- no Room schema or accounting formula changed in this phase.

## 8. Next gate

Begin Phase 8 with a screen-by-screen capability ledger. For each migrated screen, implement only already-contracted actions, await every native mutation, render stable plain-language errors, refresh the declared native queries, add a hybrid integration test, and then remove that screen's active mock/local business behavior. Do not expose all remaining bridge actions in one unreviewed batch.

## Plain English

Saaf Milk now opens as a real hybrid Android app. A new installation asks you to create the owner account; an existing installation asks you to sign in. The first connected feature can read the protected product list and add a product only after Android confirms the signed-in user has permission. The screen cannot invent a role, pretend a save worked, or keep shop records in browser storage. Every older native test still passes, so this new entrance did not damage the proven accounting and security foundation.
