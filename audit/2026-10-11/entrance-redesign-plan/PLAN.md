# Entrance and day-status redesign plan

Stage: planning complete; implementation has not started. Evidence: [baseline](BASELINE.md), [research](RESEARCH.md), `baseline-manifest.json` and retained images under `baseline/`.

## Fixed goals

1. Welcome has no green top section and presents the configured shop once.
2. Choose/Switch account has no green top section; account choice is the clear primary task.
3. Recommend applying the same neutral frame to every access step so selecting an account does not restore the removed band.
4. Open and Closed use a horizontal status pill with centered text across all account types, without moving work-header content.

Two entrance layouts (Welcome and shared access) plus one status component. Existing signed-in header/footer geometry, business permissions, account data, draft guards, callbacks and haptics remain the baseline. This turn ends at the requested planning stage.

## Design specification

|Element|Deliberate treatment|
|---|---|
|Palette/font|Existing ivory canvas, warm surface, deep-green text/accent and yellow main action; dark equivalents. Existing Manrope weights. No new font or global token redesign.|
|Welcome identity|Existing mark at 80dp; centered configured shop name at 30sp/38sp semibold; tagline “Your shop, ready for the day.” at 16sp/24sp. One quiet reassurance line “Daily work offline · Private account access”. Related items form one composition, not stacked competing cards. Long names wrap in the scroll body.|
|Welcome placement|24dp side gutters; identity group begins below the safe top inset with 32dp breathing space. Use a bounded flexible gap to the footer on roomy screens; content scrolls on small/large-text screens. No repeated Welcome toolbar or empty Back slot.|
|Welcome footer|Keep existing Continue action/tag and yellow treatment. Minimum 56dp height, 14dp corner radius, label 16sp/24sp semibold; expand for accessible text. Hint: “Choose your account next.” Footer stays reachable above the safe bottom inset.|
|Access toolbar|Neutral page background, Back in a reserved 48dp leading slot and shop name at 14sp/20sp semibold; one-line ellipsis only for this compact shop context. Minimum toolbar height 56dp plus status inset, using one global font-metric rule across every access state. Keep the slot when Back is disallowed. No three-line metadata section.|
|Access heading/body|24sp/32sp semibold page heading, 14sp/20sp supporting copy; 16dp side gutters, 24dp after toolbar, 8dp title-to-instruction, 20dp before accounts/form. Title/instructions are in the scrollable body. Full shop/name details remain available in the body when compact context ellipsizes.|
|Picker copy|“Choose account” / “Tap your name to sign in.” Switch mode: “Switch account” / “Choose another account. Back keeps you signed in.” Remove redundant all-caps list heading. Keep one short footer: “Staff accounts are created by an Owner or Administrator.” Existing preview-only credential help remains a quiet footer in preview builds.|
|Account rows|Retain established rounded Entity surfaces, 72dp minimum with natural growth, 16dp/12dp inner padding, 40dp initial avatar, 12dp avatar-to-text gap and reserved 24dp chevron. Name 16sp/24sp semibold, role/enrolled-method text 14sp/20sp. 8dp exterior gaps; no divider behind/between separately outlined rows. Long names/methods wrap; the entire row is one selectable target.|
|Other access content|Keep current concise titles, selected-account card, credential fields, enrolled methods, recovery warning/code, acknowledgement, errors and retry. Only frame, hierarchy and related spacing change. No fake methods or general Sign up/Forgot password additions.|
|Open/Closed|Visible horizontal pill: 32dp minimum height, 12dp horizontal inset, centered 14sp/20sp semibold label; intrinsic height grows with font metrics. Minimum touch area 48dp high; rounded ends follow actual pill height. One slot sized for the widest of Open/Closed at the current global font setting; equal placement and no state-dependent shift. No text ellipsis. Keep existing status colors and role-authorized action; accessibility names it as day status, not an instruction to open the day.|
|Motion/system bars|Keep the existing short opacity transition; no slide, bounce or size interpolation of the access frame. Keep bar/body bounds fixed for a given global text setting and handle the keyboard once. Match OS icon contrast to the neutral entrance in both themes; preserve green signed-in behavior. Opening/closing keyboard may necessarily change usable viewport.|

Small-screen/large-text behavior outranks decorative spacing. Exact metrics are starting constraints; verify actual native font layout rather than forcing a clipped fixed rectangle. Work-header height must retain its existing shared rule. The status action slot may grow uniformly with global font scale to accommodate both labels; no per-screen sizing exceptions.

## Screen/state map

|Family|Explicit coverage|
|---|---|
|Welcome|Default, long shop name, small portrait viewport, large text, light/dark; Continue and Back from access.|
|Picker|Ordinary choose and signed-in switch/cancel modes; empty/one/current six/many accounts; long names and method summaries; inline error.|
|Shared access|Loading, startup failure/retry, first Owner setup, passphrase, method picker, Quick PIN and recovery/acknowledgement. Together with picker: eight visible state types; nine cases when both picker modes are counted.|
|Signed-in day status|Owner, Administrator, Manager, Cashier and Operations; Open/Closed, actionable/read-only, default/150%/200%, theme parity; actual role permission unchanged.|

## Implementation sequence and boundaries

1. Produce one reviewable composition for Welcome, picker/sign-in and Open/Closed against the frozen baseline; no open-ended variant search. Lock spacing/copy before applying.
2. Update Welcome and one shared neutral access frame, including both picker modes and account-row framing. Keep all existing navigation/security handlers and stable tags.
3. Correct the shared status pill and system-bar appearance. Do not resize signed-in shells or rework other menus/capsules.
4. Update focused geometry, text-layout and workflow checks. Old tests asserting identical work/access header geometry must be replaced by separate work and entrance contracts; preserve no-jump assertions within each family. Exercise the real setup/recovery journey separately on test-owned data.
5. Run qaFast and affected access/chrome groups, inspect representative A06 portrait evidence in both themes and large text, then one full checkpoint on frozen implementation. Record failures honestly; correct only confirmed in-scope regressions. Require zero untested inventory controls. Owner review remains the visual acceptance gate.
6. Install with `install -r` into interactive `.qa` only after validation; retain all six accounts and shop data. Preserve ordinary app/protected source hashes. Deliver before/after evidence and a short test checklist.

Expected production touchpoints: WelcomeScreen, NativeAuthenticationScreens/shared access helper, CashierScreenHeader/its slot interface, NativePreviewActivity system-bar appearance and localized copy resources. Related test fixtures/evidence/build filters only as needed. This is a limited entrance/status pass, not an app-wide rewrite.

## Stop conditions

Done when the named entrance surfaces have no green band; identity/actions remain clear and reachable; access state transitions do not resize chrome; account surfaces align without double framing; Open/Closed have centered unclipped labels and stable placement; relevant functional/geometry checks are recorded; and review evidence is delivered.

No new logo, dairy artwork, auth recovery/enrollment features, account/database changes, permission changes, cashier day-close policy, financial logic, global menus, landscape/tablet design or broad motion audit. Any newly discovered unrelated issue goes to NEXT_PASS.md without investigation. The previous full-checkpoint timing failures are baseline context, not an extra workstream here.
