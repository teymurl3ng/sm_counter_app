# Shop settings grouping — batch 1 handoff

Implemented the first batch only. Shop settings now has coherent shop-details, supplier/retail rates and opening-balances surfaces, followed by the existing independent expense-category rows. Category detail has a compact information surface. Shared section/heading/fact components use the current palette/shape tokens, own one 16dp inset, and stack facts/actions for long content or 150%+ text. Routine edit buttons retain their existing callbacks/tags. No new intermediate screen, collapsing section or animation was added.

Existing forms, validation, eligibility branches, archive/restore confirmations and role behavior are retained. Eight unchanged editor/helper functions were compared to the pre-change source (unchanged-forms.json). Existing NativeListItem, theme, chrome, ViewModels, domain/data code and previous entrance/Quick PIN changes were not rewritten. This batch changes exactly two production sources and two test sources. No shop settings were saved during interactive inspection.

Validation:

- Isolated instrumentation and preview builds: PASS. Initial build command split an unquoted dotted Gradle property in PowerShell; the corrected quoted command compiled successfully. Both logs are retained. No application-source fix was needed for that command failure.
- A06: 26 unique selected tests PASS, zero failures. Four grouped-component checks, eighteen settings state/action/layout checks and four real Owner/Admin settings-navigation checks. selection.txt and a06-test-summary.json specify the exact selection/results. Unchanged form-write tests and unrelated workflows were excluded.
- S24: three representative component/settings/category checks PASS. These overlap the A06 selection; 29 test executions, 26 unique tests overall.
- Inventory: PASS, 1,005 rows, untested=0, orphans=0. No interactive tag was introduced or removed.
- Test coverage includes default/light/dark, 150% and 200% fixture text, long identities/address/category names and large money, read-only semantics, heading semantics, minimum action target, wrapping, missing identity, recorded/unrecorded/blocked opening states, empty categories, archive/restore confirmation and existing navigation. Source hashes remained frozen through both test runs and the final preview build.
- A06 interactive preview: two untouched identity-edit/back cycles PASS for stable top/bottom position snapshots. No settings were written. This does not certify frame-by-frame motion smoothness or felt haptics.

Evidence: seventeen A06 fixture PNGs, six S24 fixture PNGs, one installed-app before capture and two installed-app after captures, each with an UNREVIEWED sidecar. Representative captures were inspected by the agent for group insets, readable complete values, wrapping, action reflow and category-detail composition. User visual approval remains pending; no sidecars were self-approved. Fixtures use sample data and overridden Compose font scale, not modified phone preferences. The A06 light fixture's system-status icon treatment differs from the interactive app; the installed-app capture is the appearance reference for that bar. Existing large-text chrome ellipsis is outside this body-only batch.

Updated com.saaf.milkshop.qa with install -r on A06 R8VXA02SNKH and wireless S24 SM-S921W. Preview first-install times were retained; ordinary com.saaf.milkshop version/install metadata matches before/after on both. No account transfer, reset, credential enrollment or shop-data migration was performed. The installed preview APK is Saaf-Milk-QA.apk, SHA-256 743f4960afb0cb064a967449aafc6ddba41a00f82120ccd5a1389d681dc742ac.

The A06 is left at Preview Owner → Shop settings. The S24 is left in the existing teymur Owner session → Shop settings, as confirmed by its final installed-app capture. The S24 account picker has no public Preview Owner; that lookup stopped. The navigation script's account argument does not identify an already-authenticated session, so its printed Preview Admin label was superseded by the visible Owner identity. No private Owner credential was entered or changed.

No full qaFast/qaCheckpoint or unrelated suite was run, following the user's scoped-test instruction. No commit/push was requested for this batch. Implementation is ready for user appearance review. Stop here; batches 2–4 remain separate work.
