# A06 Quick unlock method-choice acceptance

Date: 2026-09-12

Device: Samsung SM-A065F / Android 16

Package: `com.saaf.milkshop.qa`

The A06 had restored the S24 7:05 recovery point. The restored Saaf account correctly had no transferred Quick unlock enrollment. Quick unlock became available after the user added an Android screen PIN; no fingerprint was enrolled on the phone.

`01-existing-both-methods-copy-corrected.png` records the updated `Phone security` wording for an older enrollment that contained both methods. It no longer claims a fingerprint exists.

The user removed that enrollment and confirmed that the replacement setup started with both method choices off. Quick PIN-only setup produced the single Quick PIN method and Quick PIN login worked. This result is recorded by direct user report; no PIN or passphrase was captured.

The user then replaced the enrollment with Phone security only. `02-phone-security-only.png` records `Enabled method: Phone security.` After sign-out, only the matching Phone security route appeared, the Android phone-lock prompt succeeded, and the restored account opened. The login result is recorded by direct user report.

The focused regression verifies that both choices begin unchecked, the submit action remains unavailable until a method is selected, the explanatory copy is present, and the enrollment form contains no fingerprint claim. The complete ordinary S24 regression passes 273/273 with zero skips or failures.
