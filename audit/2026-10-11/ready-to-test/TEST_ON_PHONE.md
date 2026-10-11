# Test the isolated QA app on the A06

The handoff updates only com.saaf.milkshop.qa and preserves its existing data. Its launcher label is **Saaf Milk (preview)**. Ordinary com.saaf.milkshop is separate. Existing Preview accounts use the debug passphrase **1234567890qwert**; use your existing credentials for any custom QA accounts.

Suggested bounded check (about 5–10 minutes):

1. Account selection: select an account, go Back, and select it again three times. Watch the header and body transitions for flashes or full-screen jumps.
2. Owner Work: open/close Receive milk and Active deliveries ten times. Check the second entry as carefully as the first.
3. Haptics: try bottom navigation, a More submenu, and a save action. Note any missing, excessive or inconsistent pulse; Android touch vibration must be enabled for this check.
4. Save feedback: record one small sale with available stock. Confirm the receipt remains until Start new sale. Try an invalid entry and check that its reason is visible.
5. Catalog and name: add an extra product and configure its price, then confirm it appears in Sell. Change the shop name in Settings and check role headers after switching accounts.
6. Cashier close: inspect Count cash/Close day and its blockers. A cashier may close after a current count and resolved deliveries; only Owner/Admin may reopen. Use QA data only.
7. Review the fresh account/recovery screenshots and the re-entry video. Recovery images use demo values; they are not real account secrets.

Capsule/menu appearance is intentionally deferred. For this pass, report a finding as: **account role / screen / action / expected result / actual result**. Physical haptic and visual approval require your review; automated tests cannot establish how a vibration feels.
