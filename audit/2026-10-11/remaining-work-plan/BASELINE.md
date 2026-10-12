# Remaining-work baseline

Date: 11 October 2026. Planning only; no new build, phone operation, test suite, account change, sync or deployment.

## Starting point

- Checkout: main, HEAD b379f67. The earlier repair/capsule/chrome checkpoint is committed. Later entrance, Quick PIN, settings grouping, night-header and menu-role changes are in the working tree. Preserve them.
- Latest recorded interactive delivery: A06 R8VXA02SNKH, com.saaf.milkshop.qa, APK SHA-256 c91e455f0c444cdd30776b67357cf7af68ea14b8c66fd6f7484f4201ea4e0e73. The delivery left Preview Owner > Shop settings. This is saved install evidence, not a new inspection of current device state.
- Ordinary signed production 0.4.1 / code 5 was installed and accepted on October 10; subsequent preview passes preserved it. Its production identity/sync and Viewer login/read acceptance are already complete at their recorded scope.
- Latest menu repair has 26 unique latest passing affected checks. Latest completed broad checkpoint remains the earlier 606/609 formal FAIL with three focused retries passing. Later entrance checkpoint was cancelled at the user's request. None describes a full run of today's combined source.
- Earlier source-hash manifest has 569 app/test/resource/build source entries. Baseline capture verifies continuity, records current hashes and adds the specifically inspected external files. No databases, credentials, keystore bytes or private backup payloads are copied.
- SOURCE_HASHES.json and EXTERNAL_SOURCE_HASHES.json are the starting contracts; git-status.txt retains tracked/untracked context. ANCHORS.csv records exact source statements, not runtime proofs. TEST_INDEX.csv inventories existing @Test methods in relevant classes; presence is not a pass.

## Confirmed limits, with stronger implementation evidence

1. **Profile sync:** SyncEnvelopeFactory already embeds all five profile families and SyncMigrationSql captures their parent rows. SyncApplyEngine applies product name/unit only, creates a limited new Customer profile only, and does not apply existing Customer or Vendor/Salary/Operating Expense profile updates. Missing work is receiver behavior plus a safe catch-up for already consumed records. It is not a missing upload subsystem.
2. **Planning sync:** existing budget/target entities have UID, archive and supersession fields; the sender emits supersedes_uid. Receiver returns SKIPPED. Local integer supersession IDs must be remapped; writing sender IDs directly would be incorrect.
3. **Drawings:** service posts a negative Cash/Bank movement, requires amount > 0, and corrections exclude drawings. The database OWNER_DRAWING financial-movement trigger rejects nonnegative movements even for a correction. A complete cancel/reduce remedy cannot be a button or an expanded Kotlin set alone.
4. **Rounding:** Sale uses kotlin.math.round(value * 100); Activity/More use BigDecimal HALF_UP. Official API semantics differ at ties. Confirmed policy divergence; no current runtime financial-loss reproduction or mandate to rewrite historical money.
5. **Viewer reset policy:** inspected local reset route checks caller/session and business isolation, but does not fetch the target role before its privileged Auth update. Current intended policy is separate from deployed-state verification. A UI-only restriction is insufficient.
6. **PIN settings:** five More forms enforce four digits; login and core permit 4–12. Wrong-account protection/retry policy is already implemented and must survive alignment.
7. **Cashier Activity:** only own SALE/CREDIT_COLLECTION events are read/listed; own EXPENSE events are not exposed. This is missing visibility, not missing expense posting.
8. **Identity:** cashier-scoped transport enrollment exists; per-person verified server linking/device credential revocation is not implemented, and local link-state enforcement is intentionally false pending guardrails. Local cashier access and actor attribution exist.

## Visual baseline to preserve

The existing installed-app settings capture was inspected again for this baseline: ../menu-typography-repair/after-a06-settings-top.png. It shows the current deep forest header, neutral body cards, stable footer, aligned facts/actions and distinct metric/action text. It is an earlier captured state, not current whole-app acceptance.

- Manrope and current light/night palette remain the authority. No palette/shape/outer-chrome redesign in remaining body passes.
- Section title 16sp/22sp SemiBold; subgroup/supporting 14sp/20sp Medium or SemiBold; value 16sp/24sp SemiBold; money 16sp/24sp Bold with existing metric token. Selected mint surfaces use their contrasting selected foreground.
- Existing Section: 16dp inner inset, 16dp radius, 1dp outline. Existing Record/Navigation variant: 16dp horizontal / 12dp vertical inset, 12dp radius, minimum 72dp when applicable, natural growth. Same-purpose rows share treatment; text does not all need a card.
- Short facts/actions share first baselines. Long facts stack; 150%+ uses established reflow. Preserve full money, units, identities and status. Primary row label, metadata and metric occupy distinct scan slots.
- Portrait phone only. No landscape, new footer/toolbar, global font change, task-tile restyle or nested-card blanket migration.

The previous grouping SCREEN_MAP.csv and night LIST_SITES.csv remain source maps, not lists of independently reproduced visual defects. Each execution batch takes a representative before capture of its own affected family, then stops at its listed files/states.

## Unknowns deliberately left visible

Remote Viewer presentation publication, external server source publication, scheduled-backup history, provider plan/configuration, private historical partial records and current S24 installation were not queried. Old reports cannot settle current remote state. These are bounded preflights in their assigned batches.

Single-device and two-device launch paths are conditional branches. This plan does not assume the owner has abandoned the intended owner-phone/shop-device workflow. Two-device readiness must close its extra gates rather than being inferred from single-phone acceptance.
