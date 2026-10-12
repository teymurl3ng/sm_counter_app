# Proposed design and bounded rollout

Give loose information a clear home using the existing warm surface design. Preserve the current top/bottom chrome, navigation routes, permissions and business behavior. No new intermediate settings landing screen or accordion is needed for this pass.

Shop settings becomes four deliberate sections, in this order:

1. **Shop details:** shop name in titleLarge (22sp) within the section; phone, address and currency as readable facts. Existing Edit identity action becomes the section's clearly labelled Edit action and opens the same screen. No second oversized shop-name headline floating above the facts.
2. **Rates:** supplier milk rate, delivery fee and TS standard under Supplier; retail milk/yogurt and default surcharge under Retail & delivery. Keep all current values visible. One Edit rates action retains the current combined edit form. Long labels stack; essential units/meaning stay intact.
3. **Opening balances:** opening date, recording state/counts and notes in one section with the existing eligible Record/Correct action or unavailable explanation. Preserve every gating condition and history notice. Do not make an unavailable operation look clickable.
4. **Expense categories:** named section heading and Add category action; retain existing independent category rows and active/archive labels. Remove extra separators outside bordered rows. Do not wrap each existing row in another capsule.

Category detail uses one compact facts section and the existing archive/restore action. Identity fields remain outlined native fields. Rates/opening editors may gain section headings/spacing where needed to match the overview, but this pass does not rewrite their field controls or validation.

|Pattern|Design specification|Applies to|
|---|---|---|
|Related section|Current `surface` fill, `outlineVariant` 1dp, 16dp radius, 16dp inner inset, no shadow; width fills available body; content-driven height|Shop details, rates, status, record summary and coherent facts|
|Fact row|Label bodyMedium 14sp/20sp onSurfaceVariant; value bodyLarge 16sp/24sp onSurface; money/emphasis semibold. 8-12dp vertical spacing; read-only facts have no forced touch-target height.|Contact, rates, statuses, totals and metadata|
|Fact reflow|Inline label/value only when both fit comfortably; explicit stacked variant for address/notes/long values. At 150%+ text default to stacked facts. No ellipsis for money, dates, rates or essential identities.|All grouped facts, especially Sync status|
|Section heading/action|titleMedium 16sp/22sp, semibold. Existing text action with >=48dp target; wrap/move action below heading when required; do not reduce the title to fit.|Edit identity/rates, Add category, plan actions|
|Page rhythm|16dp body gutters; 16dp between related section surfaces, 24dp where task purpose changes; headings stay with their content. Container owns inset once.|Targeted body content only|
|Repeated independent item|Reuse current NativeListStyle.Record: 12dp radius, minimum 72dp with growth, 16dp horizontal/12dp vertical inset; 8dp outer gap, no exterior divider.|Budget, collection target, yield, loss-review and recovery/review rows|
|State/action|Text and native controls expose Enabled/Disabled/Unavailable and semantic purpose; press feedback only on actual actions. Keep destructive actions distinct from routine editing.|Personal security, user details, restore/archive flows|

No one fixed height fits all sections. Consistency comes from insets, typography, colour and purpose. Preserve outlined fields; avoid card-inside-card duplication. Reuse current Section/Record tokens through small common grouped-fact/status components; do not globally restyle NativeListItem or SaafTheme and accidentally affect existing menus. Read-only section must not merge all facts into one inaccessible announcement. Keep labels associated with values and headings discoverable by TalkBack. Measure text contrast in both themes (normal text >=4.5:1), rather than assuming a surface border solves readability.

Finish in four separate batches, each closed before the next:

|Batch|Exact goal|Stop condition|
|---|---|---|
|1|Shared section/fact primitives + Shop settings overview/category detail|Four clear groups, safe wrapping, existing actions/conditions retained; focused settings/component checks and portrait screenshots reviewed|
|2|Personal-security status/setup layout across five roles|PIN/phone states and credential setup visibly distinct; all-role parity; only affected security-screen checks|
|3|Five entity-detail families + record/activity details|Grouped facts and stable money/identity presentation; Admin/Manager delegation checked without duplicate layouts; only affected detail checks|
|4|Mapped backup/sync, costing rows/details, report totals and audit detail|Every targeted CSV state migrated or explicitly retained with reason; only affected management checks|

Pre-implementation visual baseline: capture one affected Shop settings page before batch 1, plus one representative of each later family before its own batch. There is no need for an unbounded whole-app phone crawl. Confirm P2 proposals against those screenshots before changing that batch; source-backed opportunities are not accepted visual defects yet.

Per changed component: default / 150% / 200% text, long names/address and large money, light/dark, enabled/disabled/not-set and relevant empty/loading/error states. Use A06 and S24 portrait for representative body checks; top/bottom chrome positions must match before/after and remain mounted through edits. Check field reachability with the keyboard only where editor layout changes. Check re-entry for an affected route to ensure no flash or whole-screen size animation is introduced.

Test only existing contracts affected by each batch plus meaningful shared-component layout checks where needed. Run compilation for touched native UI; no full checkpoint or unrelated workflow suite. A passing test does not replace screenshot inspection. Record exact changes, affected test selection and before/after evidence. Discoveries beyond the CSV go to LATER.md and do not enlarge the run.
