# Phase 14.8.55b — native callers for the two ownership gaps

Date: 2026-09-26
Branch: `main` (follows 14.8.55, commit `8d242ed`)

## Outcome

Both gaps from the 14.8.55 ownership matrix now have native callers with device evidence. The endpoint-ownership precondition for 14.8.6 WebView/bridge retirement is satisfied: **91/91 actions have native ownership** (90 native callers + `system.bridgeInfo` by-design).

## Gap 1 — `command.linkDeliverySale` (deliveries link-to-sale)

- `NativeDeliveriesFacade`: `linkable` rows added to the context (active deliveries without a linked sale, mirroring the hybrid list semantics) and a `linkSale(sessionHandle, deliveryId, saleNumber)` method delegating to the existing bridge command service (same permission, conflict, sale-existence, and audit invariants), with the hybrid's positive-whole-number validation.
- `NativeDeliveriesViewModel`: `linkSale` action with `linkingDeliveryId` busy state and the standard retention/error pattern.
- `DeliveriesScreen`: "Link a recorded sale" section (tag `ops_delivery_link_section_*`, input `ops_delivery_link_input_*`, button `ops_delivery_link_*`); digits-only input dropping leading zeros so the canonical form is enforced at entry; success banner reuses `OpsDeliveriesSuccess`.
- Wired through all five hosts (Operations preview, Manager/Owner entry screens, Cashier More deliveries section).

## Gap 2 — `command.auth.resumeUserEnrollment` (Users & roles resume-setup)

- `NativeOwnerUsersRolesFacade.resumeCredentialSetup` mirroring `beginCredentialReset` (access + role guards, recent-auth enforced by the domain, returns the pending enrollment consumed by the existing complete-enroll flow).
- `OwnerUsersRolesController.resumeCredentialSetup` with the same reauth-retry semantics (`PendingUsersRolesAction.ResumeCredentialSetup`).
- Detail pane shows "Resume setup" (tag `owner_users_roles_resume_setup`) only while `row.setupRequired`.
- Threaded through `OwnerMoreScreen` and `OwnerPreviewHost`.

## Verification

- `qaFast`: PASS (`979` inventory rows, `0` untested/orphans), `CHECK_UI_ONLY: PASS`; `lintDebug` + `assembleDebug` PASS
- Device (SM-A065F): `NativeDeliveriesScreenContractTest` **5/5 PASS** including new `linkSaleRecordsPhysicalFulfilmentAgainstRecordedSale` and `linkSaleRejectsInvalidSaleNumber`; `NativeOwnerUsersRolesScreenContractTest` **13/13 PASS** including new `resumeSetupReopensPendingCredentialEnrollment` and `resumeSetupHiddenWhenSetupComplete`
