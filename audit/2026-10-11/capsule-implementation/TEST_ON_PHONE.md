# Review the redesigned surfaces

Use **Saaf Milk (preview)** / `com.saaf.milkshop.qa` on the A06 after this pass's installation. Existing QA data stays intact. Preview accounts use `1234567890qwert`; custom accounts keep their existing credentials.

Installed and left open at **Preview Cashier → More → Count cash**, with blank fields. APK: Saaf-Milk-QA.apk. Automated surface checks passed 48/48; full checkpoint was 596/600, with all four failures passing after test-interaction corrections. Formal Owner review and a fully green single checkpoint remain pending.

1. Cashier → More → Count cash: amount, note and Record must be separate. Expected cash must stay hidden. Check with the keyboard open, then record a small QA count and check the receipt.
2. Switch through Owner, Admin, Manager, Cashier and Operations. Scroll each Work/More menu: icons/text/chevrons should align, with space inside both borders and page gutters outside. A long menu must scroll; every item need not fit simultaneously.
3. Products, customers, suppliers, employees and records: inspect short and long names. Names/amounts must remain readable; list cards should have consistent insets.
4. Sell/Collect/Expense/Receive milk pickers: check selected state, long choices and scrolling. Look for an obvious selected indicator as well as color.
5. In dark mode and 150% text, repeat a menu, Count cash, an entity list and a picker. Check borders, contrast and wrapping; phones remain portrait-only.
6. Open/close Receive milk and deliveries repeatedly; inspect the new route recording for flashing. Check bottom navigation and save haptics on the actual phone.

The screen tracker accounts for all 62 source entries; after images are representative, not an exhaustive visual matrix for every state. Owner visual approval remains separate from automated geometry/workflow passes. Report findings as role / screen / action / expected / actual.
