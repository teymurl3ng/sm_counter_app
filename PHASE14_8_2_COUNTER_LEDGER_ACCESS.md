# Phase 14.8.2 — Counter Ledger welcome and access

Status: **ACCEPTED AS AN IMPLEMENTATION BLUEPRINT — branding and copy refinement deferred**

Date: 2026-09-13

## Decision

The welcome and access journey is accepted as a native implementation blueprint. Final logo geometry, illustration balance, wording, and motion may be refined after the first Compose render, but the journey structure and security meaning below are fixed.

## Introductory experience

Saaf opens with a dedicated branded welcome surface rather than an account picker styled like an authenticated application screen.

- The welcome screen uses the Counter Ledger green, ivory, and yellow palette.
- A large Saaf mark, wordmark, short promise, and restrained dairy/landscape motif establish identity.
- Returning installations say the shop is ready and continue to account selection.
- New installations introduce Saaf, then offer first-Owner setup or recovery of an existing encrypted shop.
- Offline operation and private access are stated quietly near the entry action.
- Authenticated app navigation, dashboards, role labels, and work controls do not appear on this screen.

## Account and sign-in sequence

1. Welcome.
2. Choose the person using the shared phone.
3. Show only sign-in methods actually enrolled for that account on this phone.
4. Complete Quick PIN, Android phone security, or full-passphrase authentication.
5. Enter the role-calibrated authenticated shell.

The selected identity remains visible throughout authentication. Changing the account is a clear action and never silently reuses another person's Quick unlock method.

## Account creation language

Saaf does not present a general public `Sign up` action.

- `Create the first Owner` appears only on an installation with no application users.
- An authorized Owner or Administrator creates ordinary staff identities and assigns Store manager, Cashier, or Operations staff.
- `Finish your setup` lets the staff member privately choose and confirm their own full passphrase.
- Trusted Administrator creation remains a separate Owner-only, strongly authenticated action.

## Quick unlock behavior

- `Quick PIN` is identified as four digits belonging to one account on one phone.
- `Phone security` uses Android's available device credential or biometric prompt without claiming that a fingerprint exists.
- An account with only Quick PIN never displays Phone security as available.
- An account with only a full passphrase goes directly to a clearly labeled full-passphrase choice.
- Cross-account PIN use names the selected account and explains that the PIN belongs to someone else.
- Attempt delays retain a visible full-passphrase route.
- Removing Quick unlock replaces the settings content with an immediate confirmation in the current viewport.

The full account passphrase, Google Drive authorization, and portable backup-vault passphrase remain explicitly separate concepts.

## Deferred polish

The following may change during native rendering without reopening this blueprint:

- final logo curves and optical spacing;
- welcome headline and tagline wording;
- illustration scale across Samsung aspect ratios;
- transition timing and reduced-motion behavior;
- precise type metrics after Android font rendering.

The welcome/authentication separation, first-Owner boundary, private staff setup, account-specific method visibility, and credential wording are not polish items.

## Gate result

This acceptance completes the Phase 14.8.2 five-role, welcome, and access visual-proposal gate. Phase 14.8.3 may now establish the Compose foundation while the verified hybrid application remains runnable as the behavioral fallback.
