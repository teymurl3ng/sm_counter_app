# Surface redesign review

All 62 screen-source entries have a recorded treatment in SCREEN_TRACKER.csv. Role wrappers inherit the underlying shared content. The implementation reapplies all 36 previous shared row call sites and adds the mapped missing equivalents; it does not put a card around every label.

The shared variants use 16dp horizontal inner padding, flexible height, quiet outlines and theme surfaces. Navigation/entity/record rows start at 72dp, selection rows at 56dp; row radius is 12dp and grouped section radius is 16dp. Separate content padding follows click handling so the whole surface stays tappable. Page gutters are 16dp. Existing radios/checks retain selection meaning.

The Cashier Count cash defect was reproduced before changing code: sibling form controls overlapped inside a transition Box. A spaced Column now groups the amount, optional note and Record button; expected drawer cash remains hidden.

## Evidence

- [Before Count cash](before/cashier-count.png)
- [Representative after contact sheet](inspection/after-contact-sheet.png)
- [After Count cash fixture](checkpoint-evidence/checkpoint-current/native-ux-a06/cashier-blind-count-form.png)
- [Count cash in installed interactive QA](inspection/interactive-count-cash.png)
- [Cashier More in installed interactive QA](inspection/interactive-cashier-more.png)
- [Owner menu](checkpoint-evidence/checkpoint-current/native-ux-a06/owner-more-surfaces.png)
- [Admin menu](checkpoint-evidence/checkpoint-current/native-ux-a06/admin-more-surfaces.png)
- [Manager menu](checkpoint-evidence/checkpoint-current/native-ux-a06/manager-more-surfaces.png)
- [Operations menu](checkpoint-evidence/checkpoint-current/native-ux-a06/operations-more-surfaces.png)
- [Cashier menu, scrolled](checkpoint-evidence/checkpoint-current/native-ux-a06/cashier-more-surfaces.png)
- [Products list](checkpoint-evidence/checkpoint-current/native-ux-a06/products-list.png)
- [Dark, 150% fixture text](checkpoint-evidence/checkpoint-current/native-ux-a06/surface-dark-large.png)
- [Light, 150% fixture text](checkpoint-evidence/checkpoint-current/native-ux-a06/surface-light-large.png)
- [Known Receive milk / deliveries re-entry recording](known-reentry-route.mp4)

The geometry fixture overrides Compose density to exercise 150% text. Its exported fontScale field describes the device setting, not this local override. Its system bars and the standalone Cashier fixture are not representative app chrome. The other role menu captures use the actual application host.

The complete checkpoint exports 24 screenshots and one route recording. Preserve its original bundle under checkpoint-evidence; later focused captures have separate provenance. Every screenshot sidecar remains UNREVIEWED. Automated bounds/workflow checks and my representative image inspection do not constitute Owner visual approval or a felt-haptic verdict.

This is representative visual coverage, not an exhaustive inspection of every mapped screen state. Use TEST_ON_PHONE.md for your review. Remaining acceptance consists of Owner visual review, physical motion/haptic review, and a fully green single checkpoint after the documented test corrections. Further unrelated design ideas stay deferred.
