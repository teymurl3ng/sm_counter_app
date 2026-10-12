# B5 bounded production-driver follow-up

Check the half-paisa cash-view behavior using the production BundledSQLiteDriver on A06. Run only NativeBundledMoneyBoundaryTest: literal signed SQL ties; native sale receipt/Activity/cash/count/close agreement for amount, quantity and surcharge; immutable correction and refusal of unexplained one-paisa shortage.

All transaction fixtures are isolated in-memory databases. Connected instrumentation uses com.saaf.milkshop.qa.tests, with custom runner bootstrap disabled. Ordinary com.saaf.milkshop and user-facing preview identities are preserved.

No view/schema/domain change before reproduction. If bundled checks pass, close the host-driver discrepancy with evidence rather than change production to satisfy a different SQLite implementation. If they fail, retain evidence and review the exact account-view/count/trigger compatibility boundary before any edit. No drawing, sync, UI redesign, migration sweep or publication in this run.
