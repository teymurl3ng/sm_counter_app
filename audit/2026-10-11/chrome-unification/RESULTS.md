# Stable top and bottom bars — 11 October 2026

Implemented one shared header/footer geometry across the native app. Audited 69 work-header calls and 63 screen-source files; all five role hosts now use the shared navigation sizing. Access screens and Welcome use the same top band. Variable account names/instructions moved out of access titles, Back occupies a reserved leading slot, and long chrome text ellipsizes within its slot. Activity detail refresh now draws an overlay rather than inserting height into the content flow. Existing persistent role-host chrome, draft guards, callbacks and portrait lock are retained.

At a fixed global font setting, title/account/shop/day-status changes do not alter bar height. Enlarged system text uses the same shared sizing rule, measured from fixed font samples including padding. The keyboard still reduces the usable viewport and hides compact work navigation while typing.

|Validation|Result|
|---|---|
|qaFast, final test compilation|PASS; inventory 1,001, untested=0, orphans=0|
|Initial affected matrix|35/35 PASS, before final leading-Back/font-metric refinement|
|Strengthened final geometry/text-layout matrix|9/9 PASS; default/150%/200%, dark, five role footers and access transitions|
|Full frozen-source checkpoint|606/609 PASS; 3 failures; formal checkpoint remains FAIL|
|Exact three failed cases rerun unchanged|3/3 PASS; collection/supplier/salary dirty navigation with keyboard dismissal|
|Separate fresh-owner setup|1/1 PASS after disambiguating duplicate heading/button text in the test selector|
|Current checkpoint evidence|33 PNGs/33 UNREVIEWED; one known-route MP4; Owner visual acceptance BLOCKED|

The three full-run failures concern keyboard/navigation timing. Unchanged focused reruns passed; this does not relabel the full checkpoint or prove that timing is fully resolved. Preserve `checkpoint-summary.json`, XML and both console logs. The first-owner test is outside the full seeded checkpoint; its initial duplicate-text selector failure and corrected rerun are retained separately. Production source stayed frozen throughout the full checkpoint and subsequent reruns. Only the first-owner test selector changed afterwards.

Image inspection caught a vertically clipped metadata line at 200% that outer-bound comparisons alone missed. The final tests additionally assert actual text-layout height; the corrected font-sample sizing passes. Horizontal ellipses remain deliberate. Visual evidence is representative, not every state of every mapped screen; no screenshot was self-marked PASS.

No money/auth/domain policy changes were added. All five approved pre-existing protected files match the 10 October baseline. Ordinary app is untouched; the interactive QA update uses install -r and retains existing accounts/data. Installation details are in HANDOFF.json.

[Phone test checklist](TEST_ON_PHONE.md) · [Review bundle](REVIEW.md) · [Audit/design contract](AUDIT_AND_PLAN.md) · [Screen map](SCREEN_MAP.csv)
