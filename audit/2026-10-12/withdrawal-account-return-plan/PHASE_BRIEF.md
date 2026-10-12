# Remaining withdrawal work — simple plan

Already finished: correcting an amount on the same account, cancelling a withdrawal that never happened, and cash rounding.

1. **Fix the wrong account.** Let Owner/Admin say where the money actually came from. Restore the amount to the wrongly recorded account and deduct the corrected amount from the right one. Keep the original record. Make sure later corrections, cancellation, backup and sync still work.
2. **Record money actually received back.** Add a separate dated receipt linked to the withdrawal. Partial/full returns change the receiving account's balance and do not erase the withdrawal or count as sales. Include a way to fix or cancel a mistaken receipt. Highlight unusually large/excess amounts for explicit review; do not invent a debt or silently discard actual money received.
3. **Finish the two screens.** Add an account selector to the withdrawal editor and a Record money returned action in withdrawal details. The return form has amount, receiving account, actual date and note, followed by a clear review and saved receipt. Keep the current portrait header/footer and form style.

Owner/Admin can write; Manager/Cashier cannot. Recording mistakes keep their original dates and follow the existing reopening rules. Actual money received uses the day it arrived; an old closed withdrawal does not prevent a new receipt on an open day.

One bounded run per numbered step, with its affected checks and a saved result. We stop after that step; unrelated findings go into the later queue. No full-suite run or ordinary-app install during implementation.

Execution update: step 1 is complete at its [scoped result](../withdrawal-account-correction/RESULT.md). Step 2 is the next separate run; step 3 starts after its money rules pass. B3 and Phase 1 remain partial until all three are complete.
