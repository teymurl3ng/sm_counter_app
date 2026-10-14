# Phase 4, 8a2 — bounded reset integration

Implement only the Viewer reset route, RPC allowlists, its staff-detail reset control, the CLI reset command and their policy/operation documentation. The SQL from 8a1 stays byte-identical. Existing external changes are preserved; proposed files and before-images are retained here for a reviewable deployment diff.

Acceptance: policy denial or failed/malformed reservation/claim makes zero Auth calls; a validated claim makes one Auth call; unknown Auth results retain the guard and expose pending guidance; confirmed success preserves the one-time credential even if completion/audit fails; status lookup does not mutate Auth or leak other-business metadata; CLI follows the same maintenance reservation protocol and retains production confirmation. Verify with synthetic route/CLI gateways and affected TypeScript/parser checks.

No remote migration, publication, real credential reset, Android work, recovery expansion, dependency upgrade or broad regression. Deployment/real disposable-identity acceptance and 8b/8c remain separate. Stop after this unit and update the six-phase plan.
