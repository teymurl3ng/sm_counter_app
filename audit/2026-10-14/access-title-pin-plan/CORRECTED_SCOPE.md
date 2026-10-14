# Corrected Quick PIN target

14 October user correction: the complaint concerns the **Quick PIN setup text box in Personal security settings**, specifically the brief dark shape/jitter when tapping it to summon the number keyboard. It does not concern the sign-in keypad.

This overrides the PIN entry target in PLAN.md and its earlier clarification record. Recovery-only merchant-label removal and shared downward title positioning remain authorized.

The earlier sign-in trace reproduced outgoing/incoming content overlap in a synthetic access step. That is not evidence about this complaint. The attempted access-local fade-through change was reverted before any regular app installation; generic sign-in transition, keypad/hardware focus and authentication logic remain unchanged.

Corrected boundary: NativePersonalSecurityPanel.kt and its existing role-owned scroll/inset host, plus only any shared keyboard issue demonstrated by a real setup-field trace. Compare tapping the setup PIN field, reopening its keyboard and switching from passphrase to PIN. Keep native number/password keyboard behavior, existing PIN validity/rate limits/enrollment/biometric rules, and all account/data state. No sign-in animation repair belongs to this task.

Use a synthetic panel in the existing isolated QA app; no secrets, enrollment or business operations. Preserve diagnostic captures and distinguish a reproduced layout/input issue from an OEM-only keyboard animation. Only affected header/recovery/security checks belong in this run. Earlier global acceptance tests stay deferred.
