# Offline server SQL verification

`npm ci --ignore-scripts` and `npm test` execute the actual ordered bootstrap in PostgreSQL WASM. The five checks cover bootstrap execution, sequential final-owner protection, ambiguous-create reconciliation, sync sequences and authenticated RLS.

`python native-run.py` uses the official portable PostgreSQL archive cached in `../runtime/postgresql.zip`, extracts only runtime files, creates a temporary cluster bound to localhost, runs the same checks and six concurrent owner-removal cases, then shuts down and removes the cluster. The native run has eleven checks. It verifies READ COMMITTED and REPEATABLE READ with independent client connections.

The fixtures supply minimal local Auth tables/functions/roles. Limited SELECT/UPDATE/DELETE permissions on the fixture staff table model existing service-role CLI access. They are test setup only. No blanket privilege grants were added to product bootstrap or migrations.

This is database execution evidence; it does not verify deployed Supabase schema parity, real Auth endpoints, live sessions or production recovery. The record of the downloaded runtime and its local SHA-256 is in `../evidence/sql-runtime-provenance.txt`. Dependencies are pinned in package-lock.json; large runtime/dependency files are ignored.

Subsequent live schema metadata was captured read-only through MCP and compared separately; see ../SUPABASE_VERIFICATION_2026-10-10.md. With SAAF_SQL_CATALOG_DUMP=1, check.mjs exports the resulting disposable bootstrap catalog. With SAAF_SQL_TEST_SCRIPT=live-boundary.mjs, native-run.py executes eleven additional local replay/repair checks using captured live functions/grants/policies. These reproduce S01 and test the proposed repair, including full rollback on incompatible history. They do not issue real Auth sessions or mutate live projects. Both final eleven-check native logs are preserved separately.
