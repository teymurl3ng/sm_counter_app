# Independently specified ledger slice

Synthetic PKR scenario for saved-domain verification and uncoached task testing. Do not run it against a live shop or the interactive preview's existing records. These are planned expectations, not executed results.

Fixture: Cash drawer 10,000.00; customer receivable 0; Milk stock 20 L; Yogurt stock 10 kg; selling rates Milk 190.00/L and Yogurt 200.00/kg. No surcharge/tax/fee/spoilage in this slice; no supplier payable or Bank movement. Use current valid opening/cost setup so no unsupported state is invented.

| Step | Operation | Cash | Receivable | Milk L | Yogurt kg |
|---|---|---:|---:|---:|---:|
| 0 | Opening | 10000.00 | 0.00 | 20 | 10 |
| 1 | Sell 2 L Milk, cash | 10380.00 | 0.00 | 18 | 10 |
| 2 | Sell 1 kg Yogurt, on credit | 10380.00 | 200.00 | 18 | 9 |
| 3 | Collect 100.00 from that customer into Cash | 10480.00 | 100.00 | 18 | 9 |
| 4 | Expense 50.00 paid from Cash | 10430.00 | 100.00 | 18 | 9 |
| 5 | Correct Milk sale to 1.5 L at the same rate | 10335.00 | 100.00 | 18.5 | 9 |

Correction writes immutable delta -95.00 Cash and +0.5 L Milk; the effective cash sale is 285.00. Effective sales 485.00 (285 cash + 200 credit); recorded operating expense 50.00. Collection is not additional sales revenue. Stock cost/profit cannot be asserted from selling prices; costing coverage needs separate literal acquisition/conversion inputs.

Optional **only after batch 1b**: record erroneous Owner drawing 300.00, then correct its truth to 100.00. Net cash decreases 100.00; final cash 10235.00. Original -300.00 plus linked +200.00 must not alter operating expense, revenue, debtor balance or stock. If this planned remedy is not implemented, omit both drawing steps and the expected closing cash is 10335.00. Do not exercise an unsupported positive drawing as a workaround.

Mark delivery of the recorded credit sale once; linking/fulfilment must not duplicate the already booked sale/stock decrement. Count expected cash and close; repeat close must be refused/idempotent under the current contract. A separate fixture counts 5.00 short and requires a truthful variance reason. Wrong role cannot reopen; Owner reopen records reason/actor, then the fresh recount/reclose path succeeds.

Append supplier-purchase and yogurt-production cases only after literal expected input/output/cost/payable numbers are independently calculated from the frozen specification. That bounded prep is part of batch 9, not evidence already obtained by this simple slice. Date rollover and offline multi-device close are separate scenarios.

Stop condition: staff can complete the tasks without navigation coaching; stored movements, effective summaries and full UID-linked records match the prepared table; duplicate/failed operations do not silently change totals. Record unclear wording or extra steps without investigating unrelated screens.
