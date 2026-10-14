# 8c2 complete — lock versus business sync

All five role menus now use **Lock app** and **End your session. Business sync is unchanged.** Shared copy is in `NativeAccessCopy.kt`; existing action callbacks, tags, modifiers, haptics and role rules are preserved. The title no longer implies switching a user and signing out of the business connection are the same action. Switch account remains its separate existing action.

Owner Sync now identifies **Business sync connection**, **Sync account role** and **Sync session**, with a short explanation that the connection is separate from the local person. Stop-sync confirmation explains that uploads/downloads stop until reconnecting while local accounts, saved records and unsent entries remain. Suspended/revoked server status explains its current local-access limitation when link-state enforcement is disabled. The offline countdown explains that daily work continues and reconnection is required for privileged actions; no 14-day policy or enforcement behavior changes.

## Selected A06 verification

**13 exact selected methods passed on the first instrumented run**, in 26.79 seconds, after the isolated APK build succeeded. All five actual role menus are checked, including Admin/Manager menus through their real screen entry points and one action call. Existing stop-sync dialog/confirmation actions pass; server-status and zero-grace copy renders as intended. Default light and dark 200% Sync fixtures pass existing complete-text/geometry, review-action and busy-state checks. No broad suite ran.

Two meaningful domain checks independently compare saved transaction headers/inventory/financial children, local users, Room binding/link state and complete unsent outbox rows before/after:

- Local lock calls the real native authentication facade: its local session becomes unusable, while server config, token/session, file/Room binding and queued records remain, and runtime still reports enrolled.
- Stop-sync runs through the real enrollment service with an unavailable synthetic logout response: exactly one `scope=local` request is sent; files/tokens clear and runtime cannot drain. Local session, records, users, Room identity history and unsent rows remain unchanged. This is fake HTTP, not a live Supabase credential or revoke experiment.

Ordinary/preview/QA package version/install provenance matches before/after. Current domain policy, enrollment/runtime and relevant authentication/Sync-facade source hashes match preflight. Inverse-copy comparison verifies that only label/detail changed in the five menus, allowing original LF/CRLF normalization. Source/APK hashes and exact results are retained in `verification.json`, `source-and-apk-hashes.json` and `a06-test-summary.json`.

The test-fixture generator initially refused an optional production parameter; it was corrected to omit explicit defaults and generate only the required callbacks. No app API visibility or default behavior changed. The inverse-copy verifier initially read source with the Windows default encoding; using explicit UTF-8 and normalized committed source fixes that check while comparing every non-copy character. The same default-encoding issue had changed a bullet in an unrelated, unselected Operations fixture; both strings were restored to their original Unicode text. No selected test method or production behavior changed in that cleanup. All 13 selected app tests passed on their first run.

Two actual screenshots were reviewed: `sync-light.png` shows the new connection heading/explanation and readable facts; `sync-dark-200.png` shows long account/shop facts wrapping in the scrolled 200% fixture. The large-text capture is scrolled and does not show the introduction simultaneously. Its existing bottom-navigation truncation belongs to the named phase-5 appearance acceptance, not this copy batch; no new header/navigation redesign was added. These representative captures are not a complete visual audit.

## Remaining boundaries

Changes are local and verified in the instrumentation app; the ordinary signed app has not been updated. No real phone enrollment, account/password reset, server/Drive change, paid/provider action, central person/device revocation feature or policy-flag change ran. B9 wording and these preservation checks are complete at bounded QA scope. The current offline central-revocation limitation remains explicit; no claim of remotely revoked per-person offline access.

Actual shop handover remains tied to intended release/device/shop and privately held approved credentials. Hosted identity recovery/publication and protected-reset browser/production gates remain open. **Next independent existing unit is 9a: prepare the literal synthetic shop-day fixture and expected money/stock/role outcomes before its focused rehearsal.** Do not create a new operations subsystem, expand central identity B7 or unattended conflict B8 automatically. Physical phone/haptic and two-writable-phone acceptance, and phase-6 release/signing, remain their named gates.
