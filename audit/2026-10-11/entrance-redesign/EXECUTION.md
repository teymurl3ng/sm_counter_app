# Entrance and status implementation

Checkpoint before implementation: b379f67ef707dce9deaa5181f01a3974428331f8, pushed to the verified private origin/main. Approved composition and primary research are in ../entrance-redesign-plan/PLAN.md and RESEARCH.md.

## Bounded changes

- Welcome: neutral safe-area canvas, existing compact mark, one configured shop identity, one reassurance line, reachable Continue footer. Identity enters with opacity only.
- Access: one neutral, font-metric-sized toolbar for every state. Page headings and instructions belong to the scroll body. Account rows use existing surfaces with consistent inner padding, 8dp gaps, and no extra dividers. Initial avatars have a 40dp minimum and may grow to fit accessible text.
- Shared day status: separate visible pill and minimum 48dp target; both labels reserve the widest text width for the global text setting. Centered label, unchanged action and colors. Signed-in header/footer height rules remain unchanged.
- Natural-height access buttons and weighted selected-account text prevent the new compact frame from clipping large text. OS icon contrast follows the actual entrance/work surface.

## Verification boundaries

A06 portrait only. Test-owned .qa.tests handles instrumentation. Update interactive .qa with install -r only after checks. No ordinary app install, reset, account/database change, permission change, financial logic change, global list/menu redesign, new logo, or broad animation work.

The existing five protected source files are byte-identical to the approved execution baseline. Test results and visual evidence will be recorded separately. Images remain UNREVIEWED until Owner review.

## User-directed validation scope

The user explicitly requested: "only run the tests that are potentially touched by the new code addition". The full checkpoint was cancelled and will not be rerun for this pass. This instruction overrides the general checkpoint requirement in NATIVE_SCREEN_ACCEPTANCE.md for the current change. It does not make a full-checkpoint PASS claim. Validation is limited to the final 17 geometry cases, 15 selected actual UI journeys, and a separate fresh-owner bootstrap/recovery journey. Selection is retained in scoped-test-selection.txt.
