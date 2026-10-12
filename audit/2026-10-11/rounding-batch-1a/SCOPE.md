# Batch 1a — native money boundary

Authorized output: reproduce the Sale conversion mismatch, align its catalog/receipt conversion with the existing decimal HALF_UP financial read boundary, and run affected checks. No drawing, UI, domain/schema, sync or historical-data changes.

The frozen business specification §7 defines quantity × rate and value + surcharge. It does not prescribe half-paisa rounding. Its deferred note retains rounding tolerance for ledger comparisons. The implementation stores raw Double event/ledger facts; the existing read-model converter uses decimal HALF_UP at the integer-paisa boundary. This batch follows that established boundary policy rather than introducing ledger quantization.

Production edit boundary: NativeSaleFacade.kt only, reusing application/readmodels/HybridReadFormatting.kt::money without changing that helper.

Checks: literal positive/negative ties, adjacent values, safe large values, nonfinite/overflow; native product-rate conversion; amount/quantity/surcharge receipt agreement with Activity and cash reads; integer-paisa collection/expense continuity; failed transaction rollback; unchanged correction truth/history; existing ordinary SaleTest and two correction methods.

Stop after focused checks and evidence/status update. Any broader floating-point tolerance or stored-value policy issue is recorded separately. No device installation or release is required to verify this conversion change.

Observed during the first post-fix run: the receipt/Activity conversion matches, but the unchanged account_balances SQL view pre-rounds SUM(amount) to two decimals. On the JVM framework driver, 2.125 becomes 2.12 there. The original cash-agreement assertion failed and its evidence is retained. Cash-view compatibility is now characterized explicitly rather than counted as repaired. Closing this needs a separate bundled-driver reproduction and reviewed view/migration boundary; B5 remains partial. No domain/schema dependency was added to this run.
