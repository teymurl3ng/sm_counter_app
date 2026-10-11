# Deferred visual findings

## Open day-status button — reported 11 October 2026

User reports that the shared header's **Open** button became round and its text is misaligned after the bar unification. Record for the next visual pass; user explicitly requested no fix now.

Location: `phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierScreenHeader.kt`, shared across role headers. Current code gives the Surface a fixed 48dp height and fully rounded corners, while its text uses padding without explicit vertical centering. Installed-app evidence: `interactive-count-cash.png` and `interactive-cashier-more.png` in this folder.

Next pass: restore an intentional pill proportion and centered text for both Open and Closed, checking default and enlarged text while preserving the shared header geometry and existing day-status action. No UI/code changes made for this note.
