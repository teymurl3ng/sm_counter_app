# Phase 3 — Authenticated User Accounts and Authorization Proposal

**Status: APPROVED 2026-08-27 — schema-version-7 native security and authenticated shell implemented and device-verified**

**Prepared: 2026-08-27**

**Forward supersession — 2026-09-06:** The biometrics/quick-PIN deferral below remains accurate Phase 3 history, but it is no longer the forward decision. Phase 14 live-use evidence led the owner to approve native device-bound Quick unlock as a mandatory pre-release capability. See `QUICK_UNLOCK_REQUIREMENT.md` and Phase 14.6.

## 1. Decision requested

The user approved this model as the contract for the next additive native expansion, Room schema version 7.

Implementation checkpoint (2026-08-27): the schema-7 native security foundation, Android backup exclusions, encrypted portable format 2, Android-Keystore local snapshots, first-owner/login/logout/recovery flow, private staff enrollment/reset, user administration, app-owned sessions, role-aware navigation, and protected native command checks are implemented and device-verified. React/bridge integration remains later work.

The original approval authorized implementation and tests. It did not authorize cloud accounts, remote login, biometrics, custom roles, or a numeric quick-PIN shortcut; those were outside this Phase 3 slice. Cloud backup was approved later, and `QUICK_UNLOCK_REQUIREMENT.md` now separately approves the device-bound fast path for Phase 14.6 while preserving this historical boundary.

## 2. Application and threat model

Saaf Milk is currently an offline-first Android application used on a shop device, potentially shared by an owner and staff. There is no remote identity provider or server that can verify identity or recover an account.

The first security release should protect against:

- use of an unattended or casually shared device under another person's identity;
- staff performing operations outside their assigned responsibility;
- privilege escalation through the future React/WebView bridge;
- weak or exposed password storage;
- exported-backup theft or modification;
- loss of accountability when users are disabled, renamed, or data is restored.

It cannot honestly promise resistance to a fully rooted, malware-compromised device or provide remote identity proofing. Those limits must remain explicit.

NIST SP 800-63B is written for digital identity systems and is not automatically a legal requirement for this local app. PCI DSS is also not automatically applicable unless the product handles payment-card data. Their controls are used here as conservative industry benchmarks, together with OWASP mobile/application guidance.

## 3. Recommended identity model

### 3.1 Users and employees are different concepts

- An **application user** is a login identity and security principal.
- An **employee profile** is operational/master data associated with a salary account.
- A user may optionally link to one employee profile.
- Creating an employee never creates login access.
- Disabling or leaving an employee does not silently delete historical user identity; an authorized administrator explicitly disables the user.
- A stable `user_id` is authoritative. Login name, display name, employee link, and role may change without changing historical identity.

### 3.2 Proposed native tables

#### `app_users`

- stable `user_id` primary key;
- unique normalized login name;
- display name;
- fixed role: `owner`, `admin`, `manager`, `cashier`, or `operator`;
- optional unique employee-account link;
- status: `active` or `disabled`;
- credential-setup-required flag;
- created/updated/disabled timestamps and responsible user IDs;
- no hard deletion after use.

Login names should be restricted to a simple lowercase ASCII form, while display names remain Unicode-capable. This avoids ambiguous case/Unicode login matching without restricting how a person's name is displayed.

#### `user_credentials`

- one active passphrase verifier per user;
- algorithm and parameter version;
- random salt;
- password hash/verifier;
- credential creation/change time;
- failed-attempt count and throttling/lock state;
- forced-change flag for administrator-assisted reset.

There is never a plaintext or reversibly encrypted password.

#### `saved_recovery_codes`

- owner user ID;
- hashed single-use recovery code;
- created, used, and replaced timestamps.

The code itself is shown only once and is intended to be printed or stored offline by the owner.

#### `security_audit_entries`

Append-only records for:

- bootstrap, successful/failed login, logout, timeout, and lockout;
- user creation, role/status change, credential reset/change, and recovery;
- permission denial;
- backup creation/export/restore;
- daily close/reopen and other designated high-risk actions.

Each record stores timestamp, action, result/reason code, stable actor user ID when known, target identity when applicable, and a safe display snapshot. Passwords, recovery codes, session secrets, and password hashes are never logged.

#### Actor attribution extensions

- Add nullable verified `actor_user_id` attribution for new canonical events and audited operational actions without rewriting frozen historical records.
- Add nullable `actor_user_id` to daily-close history while retaining the existing actor-name/role snapshot for readable history and legacy backups.
- Schema-6 rows remain legacy assertions with a null verified user ID. They are not retroactively assigned to an invented user.

### 3.3 Migration and bootstrap

- Migration 6→7 creates empty user/security tables and nullable attribution extensions only.
- It creates no user, password, role assignment, session, or audit event.
- After migration, the application enters a native one-time owner-setup gate because no pre-authentication identity exists that could safely be promoted automatically.
- The human deliberately creates the first owner; only then may normal business commands resume.
- On a fresh installation, first-owner setup occurs before store identity/opening-state setup so the initial configuration is attributable and protected.
- The bootstrap operation is allowed only while no user exists. It creates exactly one owner and a corresponding bootstrap audit entry in one transaction.

## 4. Credential policy

### 4.1 Passphrases

Recommended policy for all roles in the first security release:

- minimum 15 characters;
- maximum at least 128 characters;
- spaces, printing characters, and Unicode accepted and normalized consistently with NFC;
- no mandatory uppercase/lowercase/digit/symbol mixture;
- reject common, breached, context-specific, and easily guessed values using an offline blocklist;
- allow paste and password-manager/autofill use;
- offer a temporary reveal control on the native credential screen;
- no routine periodic expiration;
- require a change after reset or evidence of compromise.

This follows the current NIST preference for length, blocklists, and rate limiting instead of composition rules.

### 4.2 Password storage

Use a vetted Argon2id implementation with, at minimum, the current OWASP baseline:

- memory: 19 MiB;
- iterations: 2;
- parallelism: 1;
- random salt of at least 16 bytes;
- verifier output of at least 32 bytes;
- constant-time verifier comparison.

Parameters are stored with each credential so they can be raised later and the verifier can be upgraded after a successful login. Before finalizing values, benchmark the selected maintained library on the connected low-end `SM-A065F`; never weaken below the OWASP minimum merely to make a test fast.

No custom cryptographic primitive may be invented. If a suitable maintained Argon2id dependency cannot be approved, the fallback requires a new explicit decision rather than silently substituting a fast general-purpose hash.

### 4.3 Enrollment and reset

- There are no default credentials.
- An owner/admin creates an eligible staff identity in `credential setup required` state.
- The staff member chooses their passphrase in a native secure screen; the administrator must not learn or assign a reusable default password.
- Owners may manage every role subject to last-owner safeguards; admins may manage manager/cashier/operator accounts.
- Admins cannot create, reset, disable, promote, or demote an owner.
- No user can delete or disable the final active owner.
- A credential or role change invalidates that user's active session immediately.

### 4.4 Failed-attempt policy

- Failed attempts are counted against the user account, not merely the current screen or process.
- Attempts 1–4 receive the same generic failure response without revealing whether the login exists.
- Starting with attempt 5, impose an increasing delay: 30 seconds, 1 minute, 2 minutes, 5 minutes, then 15 minutes.
- At 10 consecutive failures, lock password login for 30 minutes. Further failed cycles remain throttled rather than creating an unrecoverable permanent lock.
- A successful full login resets the counter. An authorized reset may unlock staff; an owner recovery code remains usable under its own throttling policy.
- Persisted wall-clock and monotonic elapsed-time checks should be combined so simply changing the device clock or restarting the process cannot trivially remove an active delay.

### 4.5 Recovery

- Staff credentials are reset by an authorized owner/admin and require a change at next login.
- Owner recovery uses a saved random recovery code containing at least 128 bits of entropy, stronger than NIST's 64-bit minimum.
- The stored recovery verifier is hashed and rate-limited.
- A used code is invalidated and a replacement is issued.
- Security questions, default master passwords, hardcoded support passwords, and email/SMS recovery are not used in this offline release.

## 5. Sessions and credential UI

- Credential entry, first-owner setup, login, reauthentication, recovery, and credential change are native Android screens/dialogs, not React/WebView forms.
- React never receives a password, password hash, recovery code, session token, or authoritative role.
- The native application owns the active session and attaches the verified actor to bridge commands internally.
- Sessions are memory-only in the first release. Process death or a full application restart requires login again.
- Explicit logout terminates the session and clears protected screens/back-stack state.
- Inactivity timeout: 15 minutes, chosen for a shared financial terminal and consistent with the conservative PCI unattended-console benchmark.
- Overall shift timeout: 12 hours, after which full login is required even with activity.
- When the app is backgrounded, protected content is obscured immediately; remaining in the background for 5 minutes requires reauthentication on return.
- Role change, disablement, credential change/reset, and restore invalidate affected sessions.

Recent full-passphrase reauthentication (within 5 minutes) is required for:

- reopening a closed day;
- creating/disabling an admin or owner, or changing privileged roles;
- credential recovery/reset and recovery-code replacement;
- backup export and restore;
- any future destructive reset;
- changes to security policy.

At Phase 3, an optional short numeric quick PIN was deliberately deferred. A standalone PIN would be a weak single-factor password, and Android device biometrics cannot identify which shop employee is acting on a shared device. This paragraph is historical: Phase 14 evidence has now triggered the separately approved, device-bound, rate-limited design in `QUICK_UNLOCK_REQUIREMENT.md`.

## 6. Authorization model

Authorization is deny-by-default and permission-based in Kotlin. Roles are fixed bundles of native permission constants; the UI uses effective permissions only to improve presentation.

Every native command checks permission at its service boundary. Hiding or disabling a React control is never authorization. Direct bridge calls receive the same check and a structured unauthenticated, expired-session, or permission-denied error.

### 6.1 Recommended initial role matrix

| Capability | Owner | Admin | Manager | Cashier | Operator |
|---|---:|---:|---:|---:|---:|
| View operational dashboard/history | Yes | Yes | Yes | Yes | Yes |
| View full financial reports | Yes | Yes | Yes | Limited | No |
| Sales and customer collections | Yes | Yes | Yes | Yes | No |
| Purchases and supplier payments | Yes | Yes | Yes | No | Purchase intake only |
| Expenses | Yes | Yes | Yes | Yes | No |
| Salary payments | Yes | Yes | Yes | No | No |
| Production, spoilage, delivery | Yes | Yes | Yes | Delivery status only | Yes |
| Customer/vendor/product/employee master data | Yes | Yes | Yes | No | No |
| Attendance | Yes | Yes | Yes | No | Yes |
| Corrections | Yes | Yes | Yes | No | No |
| Physical cash count and ordinary day close | Yes | Yes | Yes | Yes | No |
| Reopen a closed day | Yes | Yes | No | No | No |
| Business/rate/security settings | Yes | Yes, except owner policy | No | No | No |
| Create/export backup | Yes | Yes | Local snapshot only | No | No |
| Restore backup | Yes | No | No | No | No |
| Manage manager/cashier/operator users | Yes | Yes | No | No | No |
| Manage owners/admins and recovery policy | Yes | No | No | No | No |
| View security audit | Yes | Yes | No | No | No |

“Limited” financial access means only the balances and transaction context required to complete the cashier's current work, not profit, payroll, full expense analysis, or owner-level reports.

The permission constants, not screen names, are authoritative. If later shop workflow proves this bundle too coarse, custom permissions require a separate reviewed extension rather than editable arbitrary ACLs in the first release.

## 7. Backup and Android storage policy

### 7.1 Current gap

The current `.milkbackup` is readable JSON with a plain SHA-256 checksum. The checksum detects accidental corruption, but anyone who edits the file can recompute it. It provides neither confidentiality nor authenticated tamper resistance.

Credential verifiers, recovery-code verifiers, or security audit details must not be added to that format unchanged.

### 7.2 Required backup format update

Before user credentials enter backups:

- introduce an encrypted/authenticated backup format version 2;
- use a vetted AEAD construction such as AES-256-GCM with a fresh random nonce;
- derive the portable export key from an owner-supplied backup passphrase using Argon2id and a fresh salt;
- apply the same 15-character minimum, blocklist, and no-composition-rule policy to the backup passphrase;
- require the backup passphrase for restore and never store it;
- encrypt automatic local snapshots under a random key protected by Android Keystore;
- include user identities, roles/status, credential verifiers, recovery-code verifiers, attribution, and security audit only inside the authenticated encrypted payload;
- never back up active sessions or failed-login timing state;
- invalidate all sessions after restore and require a normal login;
- retain import support for legacy format-1/schema-6 backups, which contain no users; after such a restore, require deliberate first-owner setup.

This preserves user accounts across a legitimate device migration without exposing reusable credential material in exported plaintext.

### 7.3 Android Auto Backup

The manifest currently has `android:allowBackup="true"` with no explicit extraction rules, so Android may include the Room database, preferences, and internal files by default. Because Saaf Milk already owns a verified business-backup format, add explicit Android backup/data-extraction exclusions for the database, local snapshots, credentials, recovery state, and sessions. Do not rely on manufacturer-specific interpretation of `allowBackup` alone.

Android Keystore is appropriate for non-exportable device-local encryption keys. Portable exports use the owner-supplied backup passphrase instead, because a Keystore-only key cannot move to another device.

The portable password verifier is deliberately not tied to a non-exportable device-only pepper in this first release, because doing so would make legitimate cross-device restore impossible without forced credential replacement. The compensating controls are memory-hard Argon2id verifiers, application sandbox/device encryption, authenticated encrypted exports, and explicit Android-backup exclusions. A future server-backed or device-bound credential design may revisit peppering as a separately tested migration.

## 8. Audit and historical behavior

- Users are disabled, not deleted.
- Audit entries are append-only through normal application operation.
- Each audited action retains stable user ID plus name/role snapshot at the time of action.
- Role/name changes never rewrite past entries.
- Failed authorization is auditable without logging secrets or overly sensitive input.
- New business events receive verified actor attribution.
- Existing schema-6 and older rows remain explicitly `legacy/unverified`; migration does not fabricate attribution.
- Restore itself is an owner-only, recently reauthenticated, audited action.

## 9. Required Phase 4 tests

At minimum:

- 6→7 migration creates no user or credential and preserves all business rows;
- first-owner bootstrap is possible exactly once and is transactional;
- final active owner cannot be disabled/demoted;
- username uniqueness and normalization;
- passphrase length/blocklist/Unicode behavior;
- Argon2id parameter persistence and verifier upgrade;
- generic login errors and constant-time comparison path;
- throttling, lockout, successful-login reset, and denial-of-service-safe owner recovery;
- session inactivity, overall timeout, background timeout, logout, restart, and invalidation;
- every permission in the role matrix, including direct service and future bridge bypass attempts;
- recent-reauthentication gates;
- user creation, disablement, role change, reset, and recovery audit entries;
- verified daily-close/reopen attribution and legacy null attribution;
- encrypted backup confidentiality, tamper rejection, wrong-passphrase failure, version-2 round trip, session invalidation, and legacy version-1 restore;
- Android backup rules exclude all intended sensitive/custom-backup files.

## 10. Standards basis

- [NIST SP 800-63B-4](https://pages.nist.gov/800-63-4/sp800-63b.html): password length, blocklists, no composition/periodic-change rules, salted password hashing, rate limiting, recovery codes, and session reauthentication.
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html): Argon2id recommendation and minimum work parameters.
- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html): least privilege, deny by default, and permission checks on every request.
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html): generic errors, throttling/lockout considerations, recovery, and reauthentication for sensitive operations.
- [Android Keystore guidance](https://developer.android.com/privacy-and-security/keystore): non-exportable device-local cryptographic keys and constrained key use.
- [Android Auto Backup guidance](https://developer.android.com/identity/data/autobackup): default backup scope and explicit inclusion/exclusion rules for sensitive files.
- [PCI SSC FAQ for Requirement 8.2.8](https://www.pcisecuritystandards.org/faqs/1147/): 15-minute unattended-console reauthentication benchmark. This is used as risk guidance, not a claim that Saaf Milk is PCI DSS compliant.

## 11. Recommendation

Approve this model as one coherent security slice. Implementing only a login screen or only a `users` table would create misleading security: the native authorization checks, verified actor attribution, session lifecycle, recovery, audit trail, migration behavior, and encrypted backup boundary are all required parts of the same capability.
