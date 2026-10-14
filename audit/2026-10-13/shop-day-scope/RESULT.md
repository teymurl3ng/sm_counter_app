# 9a0 — Shop-day scope and independent baseline complete

This is a planning result. No Android story, human rehearsal, ordinary-device setup, server operation or release build ran in this batch. A1 is in progress, not accepted as a live pilot.

Saved [SCOPE.md](SCOPE.md), [staff task cards](TASK_CARDS.md), [literal expected values](EXPECTED.json) and [source baseline](SOURCE_BASELINE.json). The calculator imports no app code and uses exact rational arithmetic with half-up paisa boundaries from the accepted costing design. It verifies acquisition, every ledger step, correction deltas, count/reclose amounts, stock, cost conservation and the next-date sale. The original frozen logic and schema hashes match. Generation and a subsequent check both pass; 394 main/spec/selected-existing-test files are recorded and unchanged across the check.

Verified final D expectations: Cash **PKR 9,335.00**; customer receivable **180.00**; supplier payable **16,576.92**; Milk **78.5 L**; Yogurt **16 kg**. First count **9,330.00**, variance **-5.00**; fresh recount **9,335.00**, variance zero. Final effective product revenue **565.00**, COGS **485.44**, recorded expense **50.00**, estimated recorded result **29.56** with complete supported-flow cost coverage expected. These are future acceptance targets, not observed app results.

The draft drawer total was corrected before freezing: a credit Yogurt sale adds receivable, not cash. The independent cash identity is 10,000 + 285 + 100 - 50 - 1,000 = 9,335. A synthetic monthly conversion policy is explicitly a cost allocation, not another Cash expense. Production output remains measured; no fixed yield calculation or new yield policy is introduced.

Next: **9a1**, one new instrumentation file, two planned shop-day methods and two existing close-regression methods, exactly as listed in SCOPE.md. Save app observations separately from EXPECTED.json; do not change the oracle to agree with a failing app. Stop for a separately scoped production fix only if a mismatch is reproduced. No full suite or unrelated visual audit.

Separate open acceptance: uncoached human tasks, actual phone behavior, physical two-phone convergence if two devices will write, ordinary shared-device enrollment/handover, hosted identity/reset publication and the final signed installation. This batch changes none of those statuses to complete.
