# Admin website UI review — 2026-10-10

Applied locally to `C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer`.
The viewer checkout was clean before this work. Thirteen presentation files
changed; API routes, authentication, database code, dependencies, and the Android
app were not changed. No deployment or account administration was performed.

## Confirmed findings and repairs

- **Invisible Create Staff link:** the unlayered global `a { color: inherit }`
  rule outranked Tailwind v4's layered text utilities. Reintroducing the original
  stylesheet in an isolated browser preview reproduced identical foreground and
  background colors (`rgb(12, 25, 41)`). Moving element defaults into `@layer base`
  restores the intended link colors and lets form typography utilities apply.
  The fixed light button uses `rgb(240, 247, 255)` text on `rgb(12, 25, 41)`;
  the fixed dark button uses `rgb(11, 17, 32)` text on `rgb(226, 232, 240)`.
- **Dark theme inconsistencies:** credential success panels used `dark:` classes
  tied to system preference while the actual theme toggle uses `data-theme`.
  Success surfaces now use the same theme variables. Accent and destructive
  buttons use explicit contrasting foreground tokens. Native controls advertise
  the active color scheme. Light success/warning text colors were darkened.
- **Tablet search field:** at the default 870-pixel browser width, fixed-width
  filters squeezed Search to roughly 50 pixels. A responsive grid gives Search
  about 260 pixels there, with one column on phones and four on large screens.
- **Overflow:** the main grid column can now shrink; tables scroll inside their
  containers. Credential displays use smaller phone typography and padding.
- **Staff creation:** shop-loading failures previously appeared as an empty shop
  list without recovery; only the first 100 shops were fetched. The form now
  reports failures, retries explicitly, fetches subsequent pages, and cancels
  requests on unmount. It freezes editable fields during submission, honors HTTP
  failures, and connects labels to their controls. Existing server role/shop
  rules remain authoritative.
- **Staff profile:** the role selector now reflects refreshed server state;
  assigning/unassigning a shop invalidates the staff list as well as the profile
  and shops cache so displayed shop memberships update on navigation.
- **Accessibility and feedback:** navigation exposes `aria-current`; a skip link,
  keyboard focus indicators, reduced-motion behavior, and expired-session
  feedback were added. Credential reveal uses an explicit associated label.
  Theme switching survives unavailable browser storage.

## Verification

On the real viewer checkout after applying the changes:

- Normal `next build` (Turbopack), including TypeScript: passed.
- ESLint for `src/app`: passed.
- Existing regression suite: **114 passed, 0 failed**.
- `git diff --check`: passed.
- All thirteen applied files match the hashes in `manifest.json`.

Browser checks used an isolated, loopback-only Next.js preview with synthetic
staff, shops, and credential responses. No environment files or real credentials
were copied. Preview-only session and API fixtures are excluded from the patch
and were not applied to the viewer. Checks covered light/dark action-link colors,
shop failure/retry, a shop from a second page, associated form labels, disabled
inputs during submission, success rendering, expired-session feedback, credential
reveal, navigation indication, and the 320-pixel credential screen. The latter
had a 320-pixel document width with no horizontal page overflow.

Screenshots:

- `staff-original-css.png`: original stylesheet reproduction with the other UI
  fixes retained; this is a CSS comparison, not a complete original-site capture.
- `staff-after-light.png`: repaired staff list and responsive filters.
- `credential-dark-320.png`: synthetic success state in dark mode at 320 pixels.

`admin-ui.patch` contains the reviewable changes. `apply-ui-fixes.py` checks every
original/fixed file hash before copying any UI file; `original/` and `fixed/`
retain local snapshots and are ignored along with the temporary preview checkout.

The production build still reports the existing Next.js middleware-to-proxy
deprecation. Migrating that authentication boundary was outside this UI repair.
Browser verification used fixtures; live account writes and deployed behavior
were not tested.
