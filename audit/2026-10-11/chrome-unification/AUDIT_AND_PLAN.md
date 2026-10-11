# Stable top and bottom bars

Scope: native app chrome, wording and placement across all account types and access steps. Source map: 69 work-header call sites, 63 screen-source files, five work hosts and five role navigation bars. HEADER_CALLS.csv lists each call site; SCREEN_MAP.csv records treatment. A source map is not an exhaustive screenshot audit of every embedded state.

## Verified implementation findings

|Priority|Affected family|Evidence|Treatment|
|---|---|---|---|
|P0|Every work-header caller, including inherited role wrappers|Shared CashierScreenHeader used unconstrained title and three identity lines; long titles/names can wrap and change its measured height.|One fixed title slot and one fixed context slot; single-line text with full semantics, quiet smaller metadata and a reserved day-action slot.|
|P0|Account picker, setup, passphrase, method picker, PIN, recovery and startup states|AccessFrame sized itself from title, subtitle and conditional Back row; account names were interpolated into titles.|Same band geometry as work; concise titles, reserved leading Back slot, full identity cards/instructions in scrolling body.|
|P0|Owner, Admin, Manager, Cashier and Operations bottom bars|Five repeated NavigationBar definitions; Cashier labels allowed two lines while others used unconstrained Text.|One common safe-area owner, height rule and two-line label slot for all roles. Selected tabs retain their indicators and haptics.|
|P1|Welcome|Weighted spacers distributed the entire page around a variable shop-name heading.|Same pinned top band; scroll the branding/assurances; pin Continue outside that scroll area.|
|P1|Cashier Activity record detail|An if(refreshing) LinearProgressIndicator was inserted into the Column flow.|Use the existing drawing overlay, which cannot change layout height.|
|Verified existing safeguard|Five authenticated work hosts|Headers/navigation already sit outside NativeSectionTransition; nested pane headers are suppressed. Crossfades already disable size interpolation.|Keep this structure. No broad animation rewrite. Loading, error, receipt and submenu content change inside the reserved viewport.|

## Design contract

The title slot starts at 56dp, with a 24sp/28sp single-line title. The context slot starts at 80dp, with 16sp/20sp/16sp line-height identity/metadata lines. Horizontal gutter is 16dp; the trailing action slot is 96dp wide and actions retain at least 48dp height. The minimum 136dp content band is shared by work, access and Welcome, plus the OS status-bar inset. Title length, actor name, shop name, open/closed text and presence of Back do not resize it at the same global text setting.

At unusually large system text, slots grow from measured fixed font samples (including padding and rounding) using one global rule; every screen still gets identical geometry. We preserve body text scaling rather than clip enlarged text or shrink individual page titles unpredictably. Horizontal overflow in chrome ellipsizes; full names/instructions remain accessible in identity cards/body and full text semantics.

Bottom bars share a minimum 80dp slot plus the OS navigation inset. The label slot reserves two lines even for short words. Larger text uses the same global height rule across all roles. Auth has no signed-in role navigation; it shares the safe bottom inset. Welcome has a pinned Continue footer. These are intentional task differences, not arbitrary geometry.

Access copy: Choose account, Create Owner, Sign in, Sign-in method, Quick PIN, Recovery code and Accounts unavailable. Account names move out of variable page headings and remain in the identity content. Work count/close titles become Count & close. Other descriptive work titles retain their wording within the shared single-line slot.

The keyboard necessarily changes the usable viewport; existing IME handling hides work navigation and keeps forms scrollable. Do not preserve a dead bar slot above the keyboard or disable accessible text scaling. Routine destination/refresh changes must not move top/bottom/body boundaries.

References: Android's [pinned app bars and Scaffold](https://developer.android.com/develop/ui/compose/components/app-bars) establish stable bars separate from scroll content; [Android text scaling guidance](https://support.google.com/accessibility/android/answer/12159181?hl=en) warns that fixed text boxes can clip enlarged content. Our exact dimensions are product choices, not claimed external mandates.

## Bounded verification

1. qaFast and affected chrome tests: compare actual work headers across short/long titles and identity/status changes; compare all five role footers/all tabs; compare access-step title anchors. Exercise default, 150% and 200% fixture text, plus dark mode and representative real-host screenshots.
2. Inspect current screenshots, then run one full checkpoint on frozen sources. Resolve only confirmed failures caused by these changes. Preserve failed-run provenance.
3. Install interactive QA with install -r on A06 only; retain QA data and leave ordinary app/protected money code untouched. Owner review remains required.

Out of scope: new financial/auth permissions, data changes, capsule redesign iteration, generic content animation polish, tablet redesign or landscape support. Phone portrait lock remains. New unrelated observations are recorded for a later pass.

Implementation verification caught a clipped metadata line at 200% despite stable outer bounds. The final helper reserves actual measured font metrics from fixed samples, independent of destination content; nine strengthened geometry/text-layout tests pass. The initial 35-test run preceded these final refinements; the complete checkpoint runs the final source.
