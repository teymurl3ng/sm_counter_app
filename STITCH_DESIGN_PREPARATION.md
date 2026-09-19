# Stitch design preparation

Date: 2026-09-19

Status: **STUDY COMPLETE — refined Counter Ledger accepted; Compose foundation started**

## Why we are here

The Counter Ledger blueprints were accepted as a functional starting point. The owner requested Stitch-assisted design refinement before production Compose work and approved the six-screen brief on 2026-09-19. Stitch generated two treatments of returning-user Welcome, Cashier New sale, and Owner Overview. After review, the owner decided that neither treatment was dramatically better than the accepted designs. The study's readability improvements will be applied to a remake of the original Counter Ledger previews.

The accepted role architecture, security behavior, native destination, and original Counter Ledger layout logic remain the foundation. Stitch output is retained as visual evidence, not implementation authority. [The refinement contract](PHASE14_8_2_COUNTER_LEDGER_REFINEMENT.md) records what to carry forward and what to reject.

## Comparison and evidence

- [Local comparison gallery](design/stitch/comparison.html): the six representative screens, grouped for A/B review.
- [Review and limitations](design/stitch/REVIEW.md): findings, correction evidence, and remaining decisions. A review recommendation is not an owner selection or a passed implementation gate.
- [Private Stitch canvas](https://stitch.withgoogle.com/projects/17568665013202933334): project `17568665013202933334`.
- [Approved brief](design/stitch/DESIGN_BRIEF_DRAFT.md) and [generation manifest](design/stitch/generation/manifest.json): the approved context, exact submitted requests, returned identifiers, and retained results under `design/stitch/generation/`.

These are design proposals. Neither A nor B was selected wholesale. Generated dimensions and static screenshots do not prove exact viewport fit, complete journeys, Android behavior, accessibility, or production readiness. Bounded corrections and review findings remain part of the evidence, not an acceptance claim.

## Connection setup

- Official remote endpoint: `https://stitch.googleapis.com/mcp`.
- Codex MCP server name: `stitch`.
- Transport: built-in Streamable HTTP; no npm wrapper, Google Cloud SDK, or extra application dependency is required for the selected API-key route.
- Authentication: `X-Goog-Api-Key` header reads `STITCH_API_KEY` from the environment.
- User configuration: `%USERPROFILE%\.codex\config.toml`; the previous file was backed up beside it before adding Stitch.
- Timeouts: 45 seconds for startup; 300 seconds for tool calls.
- Credential storage: Windows user environment, outside the repository. This is a persistent local credential, not encrypted vault storage. Do not paste it into chat, design prompts, screenshots, or source files.
- This is a design workstation connection. It is not part of the Android app or its Google Drive integration.

The official Stitch [setup guide](https://stitch.withgoogle.com/docs/mcp/setup/) documents API-key authentication. Codex's [MCP documentation](https://developers.openai.com/codex/mcp) documents remote servers and environment-backed headers.

## Authentication and re-setup reference

Setup is complete. After the owner restarted Codex, the current task discovered all 15 Stitch tools and successfully called `list_projects` through the native MCP connection. The instructions below are retained for future re-setup; they do not need repeating now.

1. Open [Stitch settings](https://stitch.withgoogle.com/settings), sign in, and create an API key.
2. In a PowerShell terminal at this repository, run:

   ```powershell
   powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\set-stitch-api-key.ps1
   ```

3. Paste the key into the masked prompt and press Enter. The script stores it in the Windows user environment and runs a read-only connection check. `RemoteSigned` applies only to this process so Windows can run the locally created script; it does not change the permanent execution policy.
4. Confirm both tool discovery and authenticated project access pass. The check does not print project contents or generate designs.
5. Fully quit and reopen Codex so the MCP client can inherit the saved environment variable. Confirm Stitch tools are callable in the resumed task. A configured server alone does not prove the current task has loaded its tools.

For a later read-only recheck:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\test-stitch-connection.ps1
```

If the check succeeds but Codex still cannot authenticate after reopening, check whether its launching process inherited the updated Windows environment. Do not paste the key into the repository as a workaround.

To revoke access, delete the key in Stitch settings. To remove the local connection, use `codex mcp remove stitch` and remove the `STITCH_API_KEY` Windows user variable. Restoring the entire old Codex configuration is unnecessary and could overwrite later changes.

## Source material used for the approved brief

| Source | What the brief preserves |
|---|---|
| [PRODUCT.md](PRODUCT.md) and [PRODUCT_SCOPE.md](PRODUCT_SCOPE.md) | Small-shop context, current capabilities, plain language, offline-first use, honest scope |
| [Role architecture](PHASE14_8_2_ROLE_ARCHITECTURE_RESEARCH.md) | Five accepted roles, access rules, data visibility, supervisor boundaries |
| [Worker workflows](PHASE14_8_2_COUNTER_LEDGER_WORKFLOWS.md) | Cashier and Operations navigation, field order, repeated entry, feedback and recovery |
| [Leadership layouts](PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md) | Owner overview, manager daily supervision, administrator setup and access priorities |
| [Welcome and access](PHASE14_8_2_COUNTER_LEDGER_ACCESS.md) | Dedicated branded entry, shared-phone identity selection, first Owner and private staff setup |
| [Migration plan](PHASE14_8.md) and [inventory](PHASE14_8_1_NATIVE_MIGRATION_INVENTORY.md) | Jetpack Compose destination, retained native business/security services, incremental parity gates |
| [Saved reference blueprints](design/stitch/reference-blueprints/README.md) | Portable copies of the three accepted visual previews, for comparison rather than automatic implementation |

Previews are illustrative. Scenario selectors, mock authentication, invented sample records, and preview-only controls are not product requirements. Resolve any conflict in favor of the documented native behavior and approved role policy. Generation used the approved brief, screen requests, and fictional examples; no full repository or original preview upload was part of the comparison.

## What the brief must preserve

- Cashier opens to rapid selling; Operations opens to physical work; Owner opens to business status and decisions; Store manager opens to today's supervision; Administrator opens to setup/access health.
- Role-specific navigation is stable and never scrolls horizontally. Restricted destinations and data follow native authorization.
- Success is visible where the action happened. Saving, validation, errors, and missing setup preserve understandable next steps and safe entered values.
- Welcome is a branded introductory experience with its own composition. It does not expose authenticated menus or dashboards.
- Sign-in only offers methods actually enrolled for the selected account on that phone. Phone security does not falsely claim a fingerprint is enrolled.
- Full account passphrase, device-local Quick unlock, and portable backup passphrase remain distinct. Owner-only sensitive actions retain strong authentication.
- Daily work remains available offline. Backup is recovery, not synchronization between writable phones.
- Milk/Yogurt workflows remain the validated scope. Do not invent carts, barcode scanning, printing, payment hardware, delivery assignment, or a public staff sign-up flow.
- Android behavior includes 48 dp touch targets, readable large text, keyboard/inset handling, system Back, accessible semantics, clear contrast, and reduced motion.
- Counter Ledger remains the accepted visual starting point: ivory, deep green, restrained yellow actions, clear numerals, and calm separators. Logo/illustration and visual refinement still require review.

## Next checkpoints

1. **Connection — complete:** local authentication and read-only access pass; Stitch tools are loaded and callable in Codex.
2. **Prompt review — complete:** the owner approved the brief, fictional examples, and two treatments of three representative screens, for six initial screens.
3. **Pilot generation — complete:** A/B Welcome, Cashier New sale, and Owner Overview are available in the local gallery and private Stitch project. Exact prompts and results are retained.
4. **Owner review — complete:** neither A nor B is selected wholesale. The original design remains the base; Stitch contributes typography, contrast, touch, hierarchy, and action-visibility lessons.
5. **Refined preview — accepted:** the remade Welcome, Cashier New sale, and Owner Overview were accepted after the compact-payment, quantity-unit, and `Amount` label corrections.
6. **Compose foundation — started:** the accepted theme, Welcome, and Cashier normal state now run in the isolated QA package; service-bound authentication and Sale behavior are the next gate.
7. **Implementation handoff:** save the accepted tokens and interaction decisions; then resume Phase 14.8.3 Compose foundation. Generated HTML remains a reference artifact, not a replacement WebView product.

## Connection and generation evidence

- Codex CLI `0.155.0-alpha.2.6` recognizes the `stitch` configuration and environment-header mapping.
- Both PowerShell scripts pass syntax parsing; the verifier reports missing credentials without sending a request when no key is present.
- Authenticated MCP initialization and discovery passed: 15 tools returned by the official server.
- Authenticated `list_projects` passed in the local terminal; project contents were not printed. The owner also confirmed the passing result.
- Current-task tool availability: verified after the owner's restart on 2026-09-19. All 15 tools were discovered, and a native MCP `list_projects` call succeeded with four accessible projects. Unrelated project details are not retained in this record.
- Private project `17568665013202933334` contains the generated six-screen comparison. Approved context, exact generation/correction requests, returned results, and review evidence are preserved under `design/stitch/generation/`.
- The API connection was used without reading or exposing the credential value. No key was included in a prompt, artifact, or source file.
- No Android or React production source was changed. No app tests were run for this design-artifact checkpoint; the existing test baseline is not evidence that the proposals pass native validation.
- Historical `DESIGN.md` remains intact. No replacement design system or `.impeccable/design.json` was created from the unselected proposals.
