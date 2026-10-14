# Candidate superseded by presentation repair

14 October: release/publication is paused. The saved 0.4.2 APK predates the [layout/keyboard source repair](../layout-keyboard-audit/RESULT.md). Keep it as a provenance artifact; rebuild/freeze a new candidate before installation or publication of that repair. Phone and business acceptance remain deferred, not passed.

# Release candidate handover

Android candidate0.4.2/versionCode6 uses the existing com.saaf.milkshop package and release signing key. Follow RESULT.md for its exact APK/hash/certificate/source commit. This engineering finish does not claim acceptance tests or actual shop setup were performed.

Shop Day QA is separately installed on A06/S24 with disposable accounts and empty stock/Cash10000 starting fixtures. Keep real transactions out of it; its shared test password is not a production credential.

Real shop setup needs your intended shop name/currency/rates, correct opening balances/stock, local owner recovery code and staff access, and the corresponding existing PROD transport account/shop. Local owner/password/PIN is distinct from the server credential. Lock app leaves business sync active; Stop sync retains local records but disconnects transport. Owner/Admin reopening needs authenticated authority and a reason; Cashier can count/close but cannot reopen.

Signing keystore is present and protected from Git. Its independent backup location/custody still needs your confirmation; do not paste passwords into this chat. The existing prior public certificate hash is recorded for package continuity. Keep the keystore/properties securely in independent storage for future updates.

Hosted identity recovery uses the published safe empty-destination workflow. Never run fresh bootstrap/restore against populated DEV/PROD. The isolated hosted rehearsal is deferred; source publication alone is not a recovery proof.

Guarded credential reset preserves a pending reference on unknown Auth outcome. Do not resend Auth or force-release uncertain participants. Known success can complete the original request without changing a credential again. Private keys stay on the server. Use published PROTECTED_RESET_OPERATIONS.md for exact operator guidance.

Rollback: keep the signed prior release as reference, but do not install an older app over schema17 or erase local data. Keep a verified encrypted backup with recovery phrase and use a compatible app/restore path. For Viewer incidents, retain reset guards/pending requests and fail closed; rolling back to the old unguarded reset route is not an approved recovery. Future fixes should be forward updates unless a separately verified rollback is available.
