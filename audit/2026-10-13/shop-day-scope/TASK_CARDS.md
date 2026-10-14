# Staff rehearsal cards — not executed

Use only a disposable acceptance installation prepared for **Shop Day QA**. The evaluator prepares the store, settings, three local users, supplier/customer and opening cash described in SCOPE.md. Keep the result table with the evaluator; do not give navigation instructions or show staff the expected balances before recording their observations. No real cash, people, shop records or cloud enrollment is required for the synthetic rehearsal.

## Operator

1. Record receipt of 100 litres of supplier milk, measured at the shop. Fat is 4%, LR is 28.
2. Record a measured yogurt batch using 20 litres of milk and producing 17 kilograms of yogurt.
3. Tell the evaluator whether the saved receipt and stock changes make sense. Record any assistance required; don't coach around an unclear control.

## Cashier

1. Enter a milk cash sale of 2 litres at PKR 190 per litre. This is an intentional mistaken quantity; the actual sale was 1.5 litres and cash received was PKR 285. The Owner will correct it before closing.
2. Sell 1 kg of yogurt to the synthetic customer on credit, at PKR 280 per kg.
3. Collect PKR 100 from that customer into Cash.
4. Record PKR 50 Fuel expense paid from Cash.
5. Arrange delivery linked to the yogurt sale. Leave it pending for the first closing attempt.

## Owner

1. Pay the synthetic supplier PKR 1,000 from Cash.
2. Correct the mistaken milk sale from 2 litres to 1.5 litres, keeping the same rate. Confirm that the original entry is still visible and the corrected receipt is understandable.

## Cashier closing, then Owner review

1. Enter the evaluator's first synthetic drawer count: PKR 9,330. Describe the staged discrepancy honestly as `QA staged count: PKR 5 missed during first count`.
2. Try closing while the delivery remains pending. Observe the explanation and finish that delivery. The evaluator separately checks that fulfilment did not charge the sale twice.
3. Try closing without a variance explanation, then provide the staged-count explanation and close. Report the displayed expected cash, counted cash and difference to the evaluator.
4. Try reopening as Cashier; report the response.
5. Owner reopens for `QA review the recorded drawer count`, using full passphrase authentication. This is a count review; it does not authorize a balancing cash entry.
6. Cashier makes a fresh count: PKR 9,335, with note `QA recount includes the PKR 5 missed in the first count`. Close again and report the saved result. Keep both counts and close/reopen history.

## Next date

On the next permitted business date, Cashier confirms the day is open and records one cash milk sale of 0.5 litres at PKR 190 per litre. Report the carried and resulting balances to the evaluator. The automated test supplies explicit dates; the human rehearsal records how the actual supported date flow behaves and does not silently change the phone clock.

## Evaluator record

Record each task's outcome, actor, assistance, unclear wording or missing feedback, and independently compare stored facts with EXPECTED.json. Never classify a coached task as uncoached success. The intentionally staged quantity/count mistakes are fixture steps, not newly discovered app defects. Keep unrelated suggestions in the existing later queue; don't extend this rehearsal.
