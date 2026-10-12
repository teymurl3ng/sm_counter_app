# Entrance redesign ready for testing

The final build is installed as **Saaf Milk (preview)** (`com.saaf.milkshop.qa`) on the Samsung A06. Last update: **11 October 2026, 13:09:36** local time. It is left on **Choose account**. All six existing accounts were observed after the update. Install used `install -r`; no interactive app data was reset. The ordinary app's version and installation timestamps remain identical.

The requested checkpoint was committed and pushed before implementation: **b379f67** on `origin/main`. The new redesign is currently uncommitted in the shared checkout.

## What changed

- Welcome uses a neutral canvas, one centered shop identity, compact existing mark, a quiet reassurance line, and a reachable Continue footer. Short names are centered as well as long names. The mark fades in without scaling or bouncing.
- Every access step shares a neutral toolbar. Headings and instructions sit in the scroll body. Account rows have consistent padding and gaps, with no extra dividers. Large-text controls can grow naturally.
- Open/Closed use a horizontal pill with a centered complete label. Both reserve equal space at the current font setting and retain the existing role-authorized action. Signed-in header/footer height rules are retained.
- Status-bar icon contrast follows the neutral entrance and existing signed-in work surface.

## Scoped validation

**33 unique relevant device checks passed:** 17 geometry cases, 15 selected actual UI journeys, and one fresh-owner setup/recovery journey. After the last Welcome centering adjustment, only its four cases were rerun: **4/4 PASS**, included in the 33 unique cases. The other tested components are byte-identical to their passing builds. Build compilation passed for the final main and test sources.

The previous `qaFast` passed (93 unit cases passed, one existing skip); unchanged unit suites were not rerun after the final layout-only correction. Inventory remains **1001 controls, untested=0, orphans=0**; no controls or tags were added or removed. The full 617-case checkpoint was **cancelled at the user's explicit request** after 12 reported completed cases. It was not resumed and is not presented as a full PASS. Exact selections/results are retained in `scoped-test-selection.txt`, `geometry-final-results.xml`, `scoped-ui-console.txt`, `bootstrap-console.txt` and `welcome-recheck-console.txt`.

No financial, data, authentication-policy or permission logic changed in this pass. The five pre-existing protected edits are byte-identical to the approved execution baseline. Device mutations used only the A06 serial.

## Visual review

`final-evidence/` contains **32 PNGs with 32 UNREVIEWED sidecars**: 24 native geometry fixtures, six access/recovery fixtures, and two actual interactive preview screens. The four Welcome fixture images are from the final centering recheck. Fixture font scales are 100%, 150% and 200%; short/long shop names, empty/one/six/many account lists, both themes, and Open/Closed are covered. Some fixtures deliberately constrain viewport height; space outside that fixture is not app layout. Developer inspection confirmed the complete 200% Closed label and centered real Welcome. Owner visual acceptance remains pending; no sidecar was self-marked PASS.

## Quick phone check

1. From Choose account, use Back to see Welcome; Continue returns to the account list.
2. Choose your account and move through the enrolled sign-in methods. Check Back and Switch account without toolbar jumps.
3. Inspect Open/Closed on the signed-in header; confirm its label is centered and the original permitted action still applies.
4. Optionally try large text and dark mode. Long names and account lists should scroll while actions stay reachable.

Scope ends here: two entrance compositions and the shared day-status component. Broader menus, branding artwork, business policy, unrelated motion work and landscape were not changed.
