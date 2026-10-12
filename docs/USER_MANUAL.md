# Saaf Milk User Manual

**From first setup to everyday work, cash closing and recovery**

Edition: 11 October 2026. Based on the current project source, including the newer native Android interface. Amounts are in Pakistani rupees (PKR); 100 paisa = PKR 1. All worked examples are fictional.

This manual explains implemented behavior, gives practical operating procedures, and identifies current limits. It does not certify the installed APK, live server, or private shop records. The latest source contains changes newer than the signed October 10 release, so an installed version may have different screen wording or layout.

**The main answer:** Saaf Milk currently fits a small owner-managed dairy shop selling Milk and Yogurt, receiving milk from suppliers, making yogurt, selling for immediate payment or credit, and reconciling one shared Cash drawer. It does not yet fit every kind of shop. A 24/7 dairy shop can record its work, but the current calendar-day close and lack of separate shift accounting impose important limits. Read Chapter 16 before adopting it for continuous trading.

## Contents

1. What the app does and which shops it fits
2. Understand the records before you start
3. First installation, accounts and access
4. Set up the shop and opening balances
5. Your screens and everyday controls
6. Start a normal working day
7. Receive milk, make yogurt and record spoilage
8. Record sales and collect customer credit
9. Pay suppliers, expenses and salaries; owner withdrawals
10. Organize deliveries and attendance
11. Count cash, close the day and reopen it
12. Correct mistakes and read the history
13. Understand stock, money, costing and profit
14. A complete worked shop day
15. Backup, restore, sync and phone replacement
16. 24/7 shops, overnight trading and shift handovers
17. Weekly/monthly routines, troubleshooting and checklists
18. Current limits and verification notes

## 1. What the app does and which shops it fits

Saaf Milk is an Android shop-record system. Most daily entries save on the phone, so normal work does not need a network connection for every sale. The app combines stock, money accounts, customer and supplier balances, physical operations, staff access, management reports and protected recovery.

The validated business model is:

- Receive raw milk in liters, with supplier cost calculated using Fat%, LR and total solids.
- Convert measured milk input into measured yogurt output in kilograms.
- Sell Milk or Yogurt for Cash, JazzCash, Bank or customer credit.
- Collect old customer debt and pay supplier debt separately from the original sale/purchase.
- Record operating costs, salary payments and spoilage.
- Track deliveries, attendance, physical cash counts, closing, reopening and corrections.

### Suitability guide

| Shop or need | Current fit | Explanation |
|---|---|---|
| Small dairy shop with a daily closing time | Strongest fit | Matches the validated receiving, production, selling and closing model. |
| Dairy shop selling retail and wholesale | Supported within the model | Channels label the sale; the entered rate determines its price. |
| Dairy shop offering home delivery and credit | Supported, with limits | Delivery records and customer debt exist; advanced reminder/billing automation is future work. |
| 24/7 dairy shop using one drawer | Conditional | Calendar-day records work, but overnight cutoff and shift closing are incomplete. See Chapter 16. |
| General grocery, clothing, pharmacy or hardware shop | Not a complete fit today | Catalog entry does not provide general purchasing, pieces/packs, barcodes, tax, returns or product-specific costing. |
| Restaurant or complex manufacturer | Not a complete fit today | The production model is milk-to-yogurt, not arbitrary recipes or restaurant orders. |
| Several branches or independent cashier tills | Not established | Current cash close is one Cash account; unrestricted multi-device equivalence is not yet established. |
| Full accounts, payroll, tax filing or payment processing | Outside current scope | The app provides operational and management records, not those complete systems. |

The product can expand later. That future direction is not proof that today's implementation correctly handles every product or shop.

## 2. Understand the records before you start

### Stock and units

Milk is tracked in **liters**. Yogurt is tracked in **kg**. Measure what physically arrived, was used, was produced, was sold or was lost. TS-adjusted liters are used for supplier pricing; they are not additional physical stock.

The current product form supports Liters and Kg. A quantity of 1 cannot automatically mean one carton, one bottle or one piece. Adding a product also does not create a general receiving or manufacturing workflow for it.

### Money and debt

The normal payment accounts are **Cash**, **JazzCash** and **Bank**. They are separate balances. Choosing JazzCash records money in that account; it does not contact JazzCash or verify a transfer. Confirm payment independently before recording it as received.

| Figure | Meaning |
|---|---|
| Cash balance | What the ledger expects in the shop Cash drawer. |
| JazzCash/Bank balance | Recorded balance for that payment account, not a live provider statement. |
| Customer balance | Money the customer owes the shop. A sale on credit increases it. |
| Supplier balance | Money the shop owes the supplier. A milk receipt increases it. |
| Sales | Value of product sold, including credit sales; read surcharge labels separately. |
| Collections | Money received against existing customer debt. |
| Profit estimate | A cost-based management result, when enough costing information exists. |

A credit sale can increase sales while adding nothing to Cash. A collection can increase Cash without creating a new sale. A supplier payment reduces Cash and supplier debt without receiving more milk.

### Three different meanings of opening

1. **Initial opening balances:** the stock, money and debts already present when you first adopt the app. Enter these once before the first business event.
2. **Starting a normal day:** check carried-forward balances and begin entering work on the current open date. Do not enter the same opening balances again.
3. **Reopening a closed date:** an authorized action with a reason and full passphrase, used to permit further records or corrections for a locked date.

Closing does not reset the drawer to zero, settle debts, remove stock or automatically correct a shortage.

## 3. First installation, accounts and access

### Start with the right installation

Use the approved production installation supplied for the shop. QA/preview installations are used for testing and may contain sample accounts or records. Do not start authoritative shop records in a sample-data installation. Updating the approved app is different from uninstalling it or clearing its data; clearing data can remove the local database and device-bound unlock material.

Before adoption, choose the shared shop phone, arrange protected backup storage, and choose a starting date when stock and money can be counted accurately. Keep the existing record system during the initial checked trial.

### Install and update the phone app

The current Android project requires Android 8.0 or later (minimum API 26). That minimum is a compatibility setting, not proof that every phone has been tested.

1. Obtain the approved signed production APK from the project maintainer and save it on the chosen phone.
2. Open the APK. If Android requests installation permission for the app opening that file, follow the phone's installation prompt and then install.
3. Open the installed shop app and confirm that you are using the intended production installation, rather than a separately labelled QA/preview app.
4. Allow notification permission when offered if you want backup/sync action alerts. Connect to the internet when setting up server sync or Google Drive.
5. Follow the Owner setup below. For an existing shop database, safeguard it before updating and install the approved update over the existing app. Do not clear data or uninstall as a routine update step.

There is no app-store distribution or installed-version check certified by this manual. Ask the maintainer which APK is the approved shop release when the supplied files include several variants.

### Create the first Owner

1. Open the app and tap **Continue**.
2. On a fresh installation, use **Create Owner**.
3. Enter your display name and login name. The local login name is 3-32 lowercase letters, numbers, dots, underscores or hyphens.
4. Enter and confirm a private full passphrase. The current local policy requires 15-128 characters and rejects some predictable choices.
5. Save the displayed Owner recovery code somewhere private, away from the phone. It is shown once.
6. Acknowledge that you saved it and proceed to shop setup.

The Owner account passphrase, Owner recovery code, portable-backup passphrase and server credential are different things. Do not assume one substitutes for another. The current native sign-in screens do not expose a complete forgotten-Owner-passphrase recovery journey; if needed, use the supported recovery procedure for the installed build with the maintainer. Do not erase the database to fix a forgotten login.

### Choose the appropriate staff role

| Role | Normal work | Key boundary |
|---|---|---|
| Owner | Shop oversight, daily work, setup, reports, access, recovery and closing | Full local control. |
| Store manager | Daily selling/receiving/payments, stock work, ordinary master data, corrections and closing | No local reopen, ownership administration or full backup restore authority. |
| Cashier | Sales, customer collections, expenses, delivery tasks and cash count/close | Cannot reopen; does not get the full management financial view. |
| Operations staff | Receive milk, make yogurt, spoilage, delivery tasks and own linked attendance | No supplier-payment, salary-payment or close permission. |
| Administrator | Setup, ordinary staff administration, records and allowed local administration | Local permissions are broad; it is not merely a harmless data-entry role. Full restore and privileged-user management remain restricted. |

These describe current local permission bundles and normal screen journeys. A role may have service permission without a dedicated button for every action in its own shell. Do not infer authority merely because another person's screen has a button.

Create staff identities in **Users & roles**, assign the job role, and use private enrollment so the staff member chooses their own full passphrase. An **Employee** profile is a salary/attendance record, not a login. Link the user to the employee if self-attendance is required.

### Quick unlock and shared phones

After signing in, open **More > Personal security**. Use the full passphrase to enroll Quick PIN and/or Android phone security as offered by the installed build. Current native setup forms request a four-digit PIN. PIN unlocking is tied to that user on that installation.

Select your own account before unlocking. Switch account at staff handover; sign out when the phone is unattended. Android biometrics/phone credentials belong to the device, so grant that method carefully on a shared phone. A staff member must not use another person's Owner session.

Sensitive actions such as reopening and final restore can require full passphrase authentication even when Quick PIN works for routine login. Quick unlock does not decrypt a portable backup on a replacement phone.

## 4. Set up the shop and opening balances

Complete this chapter **before the first sale, receipt or other business event**.

### Shop identity, people and catalog

1. Open **More > Shop settings** and save the shop name, phone and address. The current currency is PKR.
2. Check **Products** and default sale rates. Retain the built-in Milk and Yogurt identities for the dairy workflows; parts of receiving, production and costing still depend on them.
3. Create each supplier whose outstanding debt or milk deliveries you will record.
4. Create customers who have old debt or will buy on credit. Immediate walk-in sales do not need a debtor account.
5. Create employees for salary records, then create/link login accounts separately where needed.
6. Review active expense categories and archive unused profiles only when appropriate. Archiving is not deletion of history.

Changing a product's unit does not convert existing stock. Do not relabel old liters as kg or rename the dairy products to imitate unrelated inventory.

### Rates and assumptions

Check supplier milk rate, supplier delivery fee per raw liter, TS standard, Milk/Yogurt retail rates and default sale delivery surcharge. The current settings service requires these values to be greater than zero. A sale can leave its surcharge switched off.

The supplier **delivery fee per liter** and a sale's **delivery surcharge** are distinct. One contributes to milk acquisition cost; the other is an amount charged to the customer.

Use the agreed commercial values. The historical specification's 13% TS standard is an app/business assumption, not a legal assertion in this manual. Check the value applicable to your supplier arrangement.

Saved purchases retain the rates used when recorded. Sales retain their entered rate. Editing settings does not reprice old transactions. Milk/Yogurt retail settings and catalog defaults are both exposed; check the prefilled sale rate after any price change and override it deliberately where needed.

### Enter the initial opening snapshot

In **Shop settings > Opening balances**, enter the starting date and:

- Physical stock by product.
- Existing Cash, JazzCash and Bank balances.
- Each customer's old outstanding debt.
- Each supplier's old unpaid balance.

Enter debts as positive amounts in the appropriately labelled customer/supplier rows. Negative opening amounts are not supported. Do not enter an opening customer debt as a new sale, or an opening supplier debt as a new milk receipt.

Save and independently compare every balance. Opening state requires a saved shop identity, can be recorded only once, and cannot first be added after business events already exist. If every opening balance is zero, skip it. An existing opening snapshot can be corrected through its specific correction flow, with fresh authentication; supply the full true snapshot, not just one difference.

### Opening stock cost is separate

Stock quantity says how much you have. **Opening cost basis** says what that stock cost. For dependable Milk/Yogurt profit reporting, save opening cost evidence in **Costing & plans > Opening cost basis**. The cost-basis quantity must match the effective opening quantity. Identify an estimate honestly and explain its source.

Without an opening cost basis, sales and cash records can still exist, but the app may report **Starting cost needed** rather than a dependable profit. If opening quantities are corrected later, review the corresponding cost basis again.

### Finish setup

Set up encrypted export/Drive recovery, store the recovery secrets, and test recovery on an isolated destination. If sync will be used, connect this phone to the correct server shop using provisioned credentials. A server connection does not replace opening-state entry or recovery planning.

## 5. Your screens and everyday controls

Screen labels can vary slightly by build and role. Follow the named task when the exact menu position differs.

| User | Main areas and purpose |
|---|---|
| Owner | **Overview** for current position; **Work** for transactions; **Records** for history/balances/closing/corrections; **More** for people, catalog, settings, costing, security, backup and sync. |
| Cashier | **New sale**, **Collect/Record payment**, **Activity**, and **More**, including expense and cash-count/close entry points. |
| Operations staff | Daily work with **Receive milk**, **Make yogurt**, **Record spoilage**, **Deliveries** and **My attendance**. |
| Store manager | Supervision/daily work, records and ordinary people/catalog management. |
| Administrator | Work desk/setup health, people/access, records and administration menus. |

The **Open/Closed** indicator concerns posting for the displayed business date. It is not a door-opening switch or staff clock-in indicator.

Before saving, check the date, person/product, unit, amount and payment account. Wait for success and read the saved result/record number. If saving fails, read the message and correct the field. If an app interruption leaves you unsure whether it saved, check history before entering it again.

Use Back to leave forms and respond to any draft-discard prompt. An unsaved draft is not a ledger record. The current product scope is portrait phone use; do not assume tablet, landscape or desktop layouts have equivalent acceptance.

## 6. Start a normal working day

### Opening routine

1. Charge/unlock the phone and sign in as the correct person.
2. Confirm the shop identity and displayed business date. Keep phone time automatic and the shop timezone correct.
3. Check that the date is open. A new calendar date normally permits posting without repeating initial setup.
4. Count the physical float and compare it with carried-forward Cash. Investigate any difference from the prior closing evidence.
5. Check physical Milk/Yogurt stock against the displayed quantities.
6. Review unresolved deliveries, missing receipts, credit collections due and backup/sync notices.
7. Operations staff mark attendance if their login is linked to an employee.
8. Begin recording work as it happens.

There is no normal daily opening-balance reset. Yesterday's remaining stock, money and debt carry forward. Counting cash records evidence; it does not add the float again.

If an unexpected amount has been deposited into or taken out of the drawer, identify the real movement. The current interface is not a complete cash-transfer/capital-injection system. Do not disguise a bank transfer, added float or unexplained shortage as a sale or expense just to make totals match.

### During the day

Record a milk receipt before selling the newly received stock. Record yogurt production before selling its output. Keep credit sales attached to the correct customer, and keep each money movement in its actual account. Coordinate shared-phone use so entries belong to the person doing the work.

The app prevents supported stock-removing operations that would make saved stock negative. If it says stock is insufficient, verify missing receipts/production and the selected product/unit rather than inventing stock.

## 7. Receive milk, make yogurt and record spoilage

### Receive milk

Open **Receive milk**, choose the supplier, and enter raw liters measured at the shop, Fat%, LR and any explanatory note. Review the calculation and save.

The implemented calculation is:

```text
SNF% = 0.25 x LR + 0.22 x Fat% + 0.72
TS% = Fat% + SNF%
Adjusted liters = Raw liters x (TS% / TS standard)
Amount owed = Adjusted liters x supplier milk rate
            + Raw liters x supplier delivery fee
```

Use 4 for Fat of 4%, and 13 for a 13% TS standard. Do not enter 0.04 or 0.13.

**Example:** 100 raw liters, Fat 4%, LR 30, TS standard 13%, milk rate PKR 170 and delivery fee PKR 11/liter:

```text
SNF = 7.50 + 0.88 + 0.72 = 9.10%
TS = 4.00 + 9.10 = 13.10%
Adjusted liters = 100 x 13.10 / 13 = 100.769230...
Amount owed = 100.769230... x 170 + 100 x 11
            = PKR 18,230.77 displayed
```

Milk stock rises by **100 liters**, supplier debt rises by approximately **PKR 18,230.77**, and Cash does not move. If you pay the supplier immediately, also record **Pay supplier**; receiving alone does not record payment.

### Make yogurt

Open **Make yogurt**. Enter the actual milk liters used and actual yogurt kg produced. Review and save. A batch using 20 liters and producing 17 kg removes 20 liters of Milk and adds 17 kg of Yogurt.

The app does not assume a fixed production yield. A typical ratio is only a reference. Measure the batch; do not enter a convenient calculated output instead of the actual output. Yogurt conversion-cost policies affect costing, not the physical quantities recorded by production.

If the owner uses yield alerts, review an unusual result and record an ordinary/abnormal loss assessment when warranted. A yield alert is a prompt to investigate, not proof of abnormal loss.

### Record spoilage

Open **Record spoilage**, choose the product, enter the lost quantity in its unit, select/enter the reason and save. Spoilage reduces stock. It does not create a sale, customer debt or cash payment.

Record a measured spill, spoiled milk or unusable yogurt once. Do not record the same loss both as a separate spoilage entry and as a duplicate production-loss quantity. Explain ordinary batch yield versus separately identified abnormal loss to avoid double counting.

## 8. Record sales and collect customer credit

### A sale paid now

1. Open **New sale/Record sale**.
2. Choose the active product and verify its unit.
3. Check the prefilled rate; change it if this transaction uses a different agreed price.
4. Enter either quantity or product value using the selected entry mode.
5. Choose the payment account where the money actually arrived.
6. Add a delivery surcharge if applicable, review the total and save once.

```text
Quantity entry: product value = quantity x rate
Value entry: quantity = product value / rate
Sale total = product value + enabled surcharge
```

Selling 2 liters at PKR 220 gives product value PKR 440. With a PKR 50 surcharge, total is PKR 490. A paid Cash sale removes 2 liters and adds PKR 490 to Cash.

In value entry, the entered value is the **product amount**, before surcharge. PKR 200 at PKR 220/liter gives about 0.909091 liters. If you also enable PKR 50 delivery, total is PKR 250.

Retail/wholesale and credit-retail/credit-wholesale are reporting labels. A wholesale label does not automatically discount the price. The surcharge can apply to any channel; switching channel does not determine whether delivery is charged.

### A sale on credit

Choose **On credit/Customer account** and the correct customer instead of a money account. Saving removes stock and increases that customer's debt by the sale total. It does not add money to Cash/JazzCash/Bank.

Example: 3 kg of Yogurt at PKR 280 plus PKR 50 delivery creates debt of PKR 890. Collecting it later is a separate collection entry.

The normal sale mechanism selects one immediate-payment account or one customer-credit destination. Do not assume split tender, part-paid credit, combined shopping baskets or payment-terminal integration. Do not split one mixed payment into duplicate full sales.

### Collect old debt

Open **Collect/Record payment**, select the customer, enter the amount actually received and its actual payment account, then save. A PKR 1,000 Cash collection reduces customer debt by PKR 1,000 and increases Cash by PKR 1,000. It does not remove stock or create another sale.

For partial repayment, enter only the amount received. Check the remaining balance afterward. Do not knowingly over-collect without an agreed supported treatment of customer advances; an amount field alone does not establish an advance/deposit feature.

Customer limits/contact fields are not proof of automatic credit refusal, debt aging, bad-debt accounting, reminder messages or bill sharing. Those advanced credit workflows are not currently complete.

## 9. Pay suppliers, expenses and salaries; owner withdrawals

### Pay supplier

Choose **Pay supplier**, select the supplier, enter the actual payment, choose Cash/JazzCash/Bank and save. The selected money account decreases; that supplier's debt decreases by the same amount. No stock is received.

Agree supplier statements against the ledger. Do not use this form as a new milk receipt or treat a payment as the milk's cost of sales. Supplier prepayments need a separately agreed treatment; this manual does not assume a complete advance-payment workflow.

### Record expense

Choose **Record expense**, the correct category, actual amount and payment account. Save only when the expense/payment happened. Categories include rent, electricity, internet, water, fuel, poly bags, LPG and other; setup can manage categories, while planning remains narrower.

The money account decreases and the expense is available to relevant reports. A Cashier may enter expenses but currently cannot inspect them in their own Activity list. Have an authorized supervisor verify a doubtful entry in Records before entering it again.

### Pay salary

Choose **Pay salary**, the employee, amount and actual payment account. Salary posting decreases the payment account and records the employee payment. It is not a complete wage calculator: attendance does not automatically compute salary, overtime, deductions or weekly settlement. Enter the agreed amount actually paid.

### Owner drawing

**Owner drawing** records personal cash taken from the shop drawer. It decreases Cash and is excluded from operating expenses. The current native screen uses the Cash drawer. It is not a bank-deposit, shop-cost or salary form.

**Current limitation:** an incorrect owner drawing has no supported ordinary correction/reversal in the inspected source. The service accepts only positive withdrawals, and ordinary corrections exclude this event type. Keep drawings outside an authoritative trial until a supported remedy is delivered, or accept that limitation explicitly. Do not follow the old code comment suggesting a reverse drawing; the necessary reversal entry is not implemented.

## 10. Organize deliveries and attendance

### Deliveries

Use **Deliveries** to create a logistics record with customer, product, quantity, route/notes and date supported by the screen. Delivery records can be linked to an already-recorded sale; some sale flows offer creation of a linked delivery directly after saving.

Mark a pending delivery **Delivered** when it actually arrived or **Cancelled** when cancelled. Archive finished records when appropriate. The close screen can list unresolved deliveries from the date and earlier open dates.

Delivery creation, delivery status and archiving do **not** themselves sell product, collect money or reverse an existing sale. Record the financial sale once, and use a supported correction/remedy if a linked sale was wrong. Cancelling delivery is not a customer refund. Do not mark an undelivered order as delivered merely to get past close.

There is no complete delivery-worker assignment, route optimization or driver cash-settlement system.

### Attendance

Operations staff open **My attendance** and mark themselves present. Their app user must be linked to an employee profile. If the screen says unlinked, ask the Owner/Administrator to make that link.

Attendance is a dated record. It is not a shift timer, clock-out system or automatic payroll calculation. Record it before closing that date.

## 11. Count cash, close the day and reopen it

### What count and close each do

**Count cash** saves physical evidence: the drawer amount, count time and optional notes. You can count without closing. **Close day** records a close action and locks posting for that business date, with coverage rules for earlier open dates.

The expected cash figure is the Cash ledger balance, including its carried-forward opening amount and all recorded Cash movements. It is not simply today's sales. JazzCash and Bank are excluded from the physical drawer count.

```text
Cash variance = physically counted Cash - app expected Cash
```

Positive means excess; negative means shortage. Zero means the count and recorded expectation agree.

### Daily closing procedure

1. Finish the day's entries and stop other people from changing the drawer during the count/close.
2. Verify milk receipts, production, sales, collections, expenses, supplier/salary payments and any missing work.
3. Resolve pending deliveries honestly. Finish/cancel them as appropriate, or use the authorized archival treatment and retain the explanation.
4. If enrolled, run **Sync now** and inspect waiting uploads/refusals/review items. Server contact alone is not proof that all phones agree.
5. Open **Cash count & close/Count cash** and check the displayed business date and any earlier-day coverage notice.
6. Count every note and coin actually in the shop drawer. Include its remaining float; exclude personal money, wallet/bank balances and promised payments.
7. Save the physical count.
8. Compare counted, expected and variance. Investigate differences. Enter a reason for any non-zero variance before closing.
9. If Cash changed after counting, record a fresh count. Use the latest count for that date.
10. Confirm **Close day** and verify its success evidence and Closed indicator.
11. Check protected backup status, sign out/secure the device, and keep cash according to the shop's physical custody procedure.

The current local bundles let Owner, Manager and Cashier count/close. Operations staff cannot close. Admin's broad local service permissions do not imply that its administration shell presents the same closing journey.

### A variance is evidence, not an automatic adjustment

If expected Cash is PKR 6,920 and counted Cash is PKR 6,900, variance is **PKR -20**. Saving the count or close does not subtract PKR 20 from the ledger. Find the real cause where possible: wrong account, omitted payment, duplicate sale, change error or physical loss. Use a supported correction for an incorrect record. Do not invent an expense merely to force a zero variance.

### Closing can cover earlier unclosed dates

A close on date D normally locks earlier dates as well where no effective explicit reopen exempts them. The current close screen lists earlier open dates with events that the close will absorb. Those dates do not acquire their own historical physical counts: they were covered by the later close.

Read the coverage note before accepting. Leaving days open and later closing once creates a broader reconciliation period, not a separate trustworthy cash count for each missing day.

### After close

Supported records for the locked date reject posting. Money, debt and stock continue to carry forward. On the next calendar date, new work normally posts to the new open date; you do not recreate initial balances.

Closing early while you still need to sell on the same date prevents those entries until authorized reopening. Reopening is not a normal staff handover tool.

### Reopen and reclose

In the current native cash-close journey, an authorized Owner enters a reason and the full passphrase, then confirms reopen. Local Admin also has reopen service permission, but a dedicated Admin journey must be checked in the installed build. Manager/Cashier cannot reopen.

After reopening, enter/correct the necessary work and let refused sync work drain where applicable. **Count again after reopening**, explain any variance and close again. The original close/reopen/reclose history remains; reopening does not erase the old count or actions.

The normal native cash-close ViewModel acts on today's business date. The domain can represent historical dates, but this is not evidence of an unrestricted historical-date picker in the current native UI. If you need to reopen an old or absorbed date and no supported control exposes it, obtain the supported procedure from the maintainer. Do not change phone time or manipulate storage to reach it.

## 12. Correct mistakes and read the history

Open **Records**, find the record, inspect its product/person, original values and movements, then use **Correct** if the supported action is offered.

Corrections preserve the original and add a replacement record. Enter the **complete corrected truth**, not the difference. The app calculates the stock/money difference necessary to reconcile it.

Example: a Cash sale was entered as 2 liters at PKR 220, but was actually 1.5 liters. Enter 1.5 liters as the corrected quantity. The corrected sale value is PKR 330; the ledger restores 0.5 liters and reduces Cash by PKR 110. It does not add another full PKR 330 sale.

Supported correction types cover milk receipts, yogurt production, sales, collections, supplier payments, expenses, salary payments and spoilage. Opening state has its separate correction flow. Owner drawing has the limitation described in Chapter 9. Delivery status and attendance use their own record mechanisms.

Correct the latest effective record in a chain. An already replaced record cannot be corrected again directly. A stock correction can fail if it would make stock negative.

The current native correction form retains the source record's business date and identities. It does not offer a general change of product, customer, supplier, employee or payment account. If the mistake is the wrong account/person/product, or you need a full cancellation/refund, do not assume amount correction solves it; use an explicitly supported remedy for that build. If the source date is locked, the date must be properly reopened before that dated correction can save.

For supplier receipt corrections, the recalculation uses the original frozen supplier rates/TS standard. Today's price change does not silently change yesterday's commercial terms.

**Records > Balances** and customer/supplier details provide debt context. Account history shows increases, decreases and running balances. **Closing** shows close/reopen evidence; **Security history** records access and sensitive-action evidence. A business date and a record's creation timestamp answer different questions: when the work belongs, and when it was entered.

## 13. Understand stock, money, costing and profit

### Stock equations

```text
Milk remaining = opening Milk + received raw liters
               - Milk sold - Milk used for yogurt - Milk spoiled
               + net supported corrections

Yogurt remaining = opening Yogurt + kg produced
                 - Yogurt sold - Yogurt spoiled
                 + net supported corrections
```

Deliveries do not remove stock a second time. Recorded stock is only as complete as the entries and measurements behind it.

### Money equations

```text
Cash expected = initial/carry-forward Cash
              + Cash-paid sales + Cash credit collections
              - Cash supplier payments - Cash expenses
              - Cash salary payments - Cash owner drawings
              + net supported corrections

Customer debt = opening debt + credit sale totals
              - collections + net supported corrections

Supplier debt = opening debt + milk-receipt amounts owed
              - supplier payments + net supported corrections
```

JazzCash and Bank follow their own recorded movements, separately. A movement recorded to the wrong account can make total recorded money look plausible while the physical Cash comparison is wrong.

### How inventory costs work

Milk costing uses perpetual **moving weighted-average acquisition cost**. A receipt adds its purchase amount, including the supplier delivery fee, to the cost of the physical raw liters received.

```text
New average cost/unit = (old stock cost + receipt cost)
                      / (old stock quantity + receipt quantity)
```

Example: 10 liters costing PKR 1,800 plus 100 liters costing PKR 18,230.77 gives 110 liters costing PKR 20,030.77, or approximately PKR 182.10/liter. A later purchase at another cost changes the pool average.

For Yogurt, ordinary milk input cost is transferred to the batch, approved conversion cost is added, and total cost is divided across the actual output. Lower output can therefore mean a higher cost/kg. Identified abnormal milk loss is accounted for separately rather than hidden in ordinary batch cost.

In **Costing & plans**, the Owner reviews effective monthly conversion cost components: culture, packaging, variable energy, direct labor and fixed production overhead. Each component uses cost and a kg basis; fixed overhead uses normal capacity. Save a review reason and evidence. These policies allocate costs; they do not pay suppliers/employees or move Cash.

### Read profit reports correctly

Open Owner **Month reports** and select This month, Last month or a supported custom range. The current custom range accepts YYYY-MM-DD dates, includes the end date shown to the user, and is limited to one year.

```text
Product gross profit = product sales revenue - cost of products sold
Gross margin % = product gross profit / product sales revenue x 100

Estimated recorded result = product sales revenue + delivery surcharges
                          - cost of products sold - spoilage cost
                          - abnormal production loss cost
                          - recorded operating expenses
                          - recorded salary payments
```

Example: product sales PKR 10,000, cost sold PKR 7,000, surcharge PKR 100, spoilage PKR 200, expenses PKR 500 and salaries PKR 800 produce product gross profit PKR 3,000 and estimated recorded result **PKR 1,600** (assuming no abnormal loss and complete supported cost history).

Credit product sales contribute revenue even when unpaid. Customer collections and supplier payments are not new revenue or cost of products sold. Owner drawings are not operating expenses. Unsold stock cost remains in stock rather than all being expensed immediately.

Respect **Starting cost needed**, **Yogurt cost policy needs review**, **Some stock cost is not supported yet** and other incomplete-coverage labels. Missing cost is not zero cost. Even **Cost history complete** only means the app has the required supported cost history; it does not prove that every business expense, tax, liability or physical loss was entered.

The result is explicitly a **cost-based management estimate**. It is not statutory net profit, audited accounts or a full accrual calculation. Current reporting has no formal lower-of-cost/NRV write-down system. The result also subtracts recorded expenses/salaries alongside inventory conversion allocations; it is not a complete automatic overhead/payroll reconciliation. Review potential overlap in production labor/energy/packaging with the person responsible for costing before relying on the result for a larger accounting purpose.

### Budgets and targets

Milk purchase plans and collection targets run Monday-to-next-Monday weeks. Operating-cost budgets run calendar months. Plans can be revised for periods that have not ended, or copied from a previous period where offered.

The milk plan uses **planned liters x saved supplier milk rate**; its sales target uses **planned liters x saved retail Milk rate**. It does not apply the full Fat/LR/TS receipt calculation, vendor delivery fee, yogurt conversion or spoilage to that budget. It is a simple planning baseline, not an exact purchase invoice forecast. Collection targets do not collect money; budgets do not post expenses. Incoming shared planning sync is currently incomplete.

## 14. A complete worked shop day

This fictional example uses one phone, one Cash drawer, no owner drawings, and the rates from Chapter 7. Retail rates are Milk PKR 220/liter and Yogurt PKR 280/kg.

### Carried-forward position

| Item | Starting amount |
|---|---|
| Milk | 10 liters |
| Yogurt | 5 kg |
| Cash | PKR 5,000 |
| JazzCash | PKR 2,000 |
| Bank | PKR 10,000 |
| Customer A owes shop | PKR 1,500 |
| Shop owes Supplier A | PKR 2,000 |

### Work recorded, in order

| Action | Saved effects |
|---|---|
| Receive 100 L; Fat 4, LR 30 | Milk +100 L; Supplier A debt +PKR 18,230.77 displayed. |
| Make yogurt: 20 L input, 17 kg output | Milk -20 L; Yogurt +17 kg. |
| Sell 30 L Milk for Cash | Milk -30 L; Cash +PKR 6,600. |
| Sell 4 kg Yogurt for Cash | Yogurt -4 kg; Cash +PKR 1,120. |
| Sell 10 L Milk on Customer A credit | Milk -10 L; customer debt +PKR 2,200. |
| Sell 3 kg Yogurt on credit, surcharge 50 | Yogurt -3 kg; customer debt +PKR 890. |
| Collect PKR 1,000 from Customer A in Cash | Debt -PKR 1,000; Cash +PKR 1,000. |
| Pay Supplier A PKR 5,000 from Cash | Supplier debt -PKR 5,000; Cash -PKR 5,000. |
| Pay PKR 300 Cash expense | Cash -PKR 300. |
| Pay PKR 1,500 Cash salary | Cash -PKR 1,500. |
| Record 2 L Milk and 1 kg Yogurt spoilage | Milk -2 L; Yogurt -1 kg. |

### Expected final position

```text
Milk = 10 + 100 - 20 - 30 - 10 - 2 = 48 liters
Yogurt = 5 + 17 - 4 - 3 - 1 = 14 kg
Cash = 5,000 + 6,600 + 1,120 + 1,000 - 5,000 - 300 - 1,500
     = PKR 6,920
Customer A debt = 1,500 + 2,200 + 890 - 1,000 = PKR 3,590
Supplier A debt = 2,000 + 18,230.77 - 5,000 = PKR 15,230.77
JazzCash = PKR 2,000; Bank = PKR 10,000 (no movements today)
```

Product sales were **PKR 10,760**, delivery surcharge **PKR 50**, and total sale charges **PKR 10,810**. Immediate Cash sales were PKR 7,720; the old-debt collection added another PKR 1,000 of Cash receipts. Those figures are different from Cash remaining and from profit.

Count the drawer. If it contains PKR 6,900, record that actual amount and the **PKR -20** variance. Investigate/explain it and close. Cash remains PKR 6,920 in the ledger until a supported real record/correction changes it. The next day carries the records forward; do not enter PKR 6,900 as a new initial opening balance.

This example does not infer profit from Cash. Profit requires opening stock cost and the appropriate Yogurt costing policy, plus complete cost/expense evidence. Purchase totals shown here are rounded for readability; small display differences at fractional-paisa boundaries require the money-policy review in Chapter 18.

## 15. Backup, restore, sync and phone replacement

### Four separate protections

| Mechanism | Purpose | Important limit |
|---|---|---|
| Local database | Saves normal shop work on this phone | Losing/clearing the phone can lose unsafeguarded local work. |
| Automatic app-private snapshot | Device-local recovery protection | Device-bound key; not a portable replacement-phone backup. |
| Encrypted export / Google Drive backup | Portable recovery point | Requires the appropriate full backup/recovery passphrase. |
| Business server sync | Exchanges supported shop records | Not equivalent to a complete backup of device/security/server identity. |

### Export an encrypted backup

1. Sign in as Owner and open **More > Backup & restore**.
2. Choose encrypted export, enter the backup passphrase and select the file destination with Android's document picker.
3. Wait for success and keep a copy off the shop phone.
4. Store the passphrase separately and test inspection/restore on an isolated destination.

Administrator's supported backup menu is local export, not full Owner cloud/recovery authority. A file left only on the same phone does not protect against losing that phone.

### Connect Google Drive recovery

As Owner, connect the chosen Google account, authorize Drive access, enter the recovery passphrase and choose the metered-network policy. Run **Back up now** and confirm a successful recovery point.

The periodic target is approximately every 24 hours, subject to Android scheduling, network, battery and storage conditions. It is not a guaranteed midnight job. Periodic Wi-Fi/unmetered requirements depend on the selected policy. The current stale threshold is 48 hours; check the **last successful backup**, not merely the last attempt.

Drive points use the app's private Drive area, so they are not ordinary visible documents in My Drive. Use the app's recovery-point list. If backup is delayed, reconnect authorization, restore connectivity or free quota according to the message. Existing recovery points are retained on relevant failure paths; do not assume a pending upload protects today's newest records.

### Restore safely

Restore replaces the destination installation's selected data; it is not a merge or an undo of just one sale. First safeguard any newer destination work.

1. Use Owner access on the destination and open **Backup & restore**.
2. For a new blank native installation, establish the temporary Owner access needed to reach this screen if the installed build does not provide a pre-login restore route. Do not enter real work into that temporary vault.
3. Select the encrypted file, or connect the correct Drive account and choose a recovery point.
4. Enter the correct portable recovery passphrase. Review creation time, compatibility and included data before proceeding.
5. Complete required full-passphrase authentication and explicit replacement confirmation. Re-inspect if the short-lived inspection expires.
6. After successful restore, sign in using the restored accounts. Sessions are invalidated; temporary destination account state can be replaced.
7. Check stock, money, debtor/vendor balances, sample history and close actions against independent expectations.
8. Re-enroll device-local Quick unlock and review/reconnect cloud backup and server enrollment before resuming work.

A replacement phone does not inherit the old phone's Quick PIN/Keystore material. Keep the backup secret even if everyday phone security usually unlocks everything. Losing all usable backups or their portable secrets cannot be repaired by guessing a PIN.

### Connect and monitor business sync

Open Owner **More > Sync**. Enter the provisioned **Server username** and **Server credential** (the current form identifies a 15-digit credential), connect the phone, and choose the correct shop if prompted. Server identity is different from the local staff login. Use the deployed production environment for real work; a DEV selector may appear in debug builds.

Use **Sync now** to exchange queued work, then inspect waiting uploads, any halted/refused work and **Needs review** entries. Local success means saved on the phone; it does not mean uploaded, applied elsewhere or backed up.

For restored items needing review, **Keep mine** releases the local version for upload; **Take server** applies the server version when available. Decide from records/evidence, not from the button's convenience. Keep recovery evidence before resolving uncertain business history.

Switching local accounts does not stop installation sync. **Stop syncing this phone** stops upload activity without deleting local records, and it does not necessarily erase retained enrollment/grace information or revoke the server account. Use supported administration for actual access removal.

### Offline and multiple-phone limits

For an active enrolled installation, the current verification grace is 14 days from the last successful server verification. After expiry, operational writes such as sales, collections, receipts, expenses, payroll, production, spoilage, delivery, attendance, corrections and cash close remain allowed in degraded mode. Privileged work such as reopening, master-data/settings changes, user management, drawings and backup actions can be blocked until verification is restored. This is not an unlimited guarantee of every offline action.

Live sync is implemented. However, current incoming profile/catalog metadata updates and shared budgets/collection targets are incomplete. Device-specific central revocation and server-verified individual clerk attribution are not complete guarantees. A zero waiting-upload count is insufficient proof of complete two-phone equivalence.

Use one controlled writable dairy-shop device for the initial trial. Before both phones write, verify full ledger convergence, people/catalog profiles, planning, restore and the closed-day recovery sequence on the chosen release. A lost counter phone and a server-account incident require maintainer-supported response; local account disabling alone is not proof of central device revocation.

If phone B records offline work for a date phone A has closed, upload can halt at the refused item. The agreed remedy is to reopen the affected closed date on the closing device, let the queued work upload/apply, reconcile again, save a fresh count and reclose. Historical UI access may require a supported procedure. Do not assume automatic conversion into corrections or move the work to a convenient date to bypass the refusal.

## 16. 24/7 shops, overnight trading and shift handovers

### Does it suit every 24/7 shop?

**No.** Continuous trading alone does not prevent entry, but the current close model is not a complete 24/7 or multi-shift POS model. Suitability depends on whether the shop can accept calendar dates, one drawer, a brief controlled count/close boundary and the limits below.

The business date is the calendar date in the shop timezone, using trusted time where available and the phone clock as fallback. **There is no inspected configurable 4 a.m./6 a.m. business-day cutoff.** The normal native close screen operates on today's date.

### Daytime shop that closes before midnight

This is the simplest pattern: finish entries, count, close the current date and stop trading until the next date. In the morning, check carried-forward balances and record work on the new date.

### Shop open past midnight but closing at 2 a.m.

The app changes calendar date at midnight. Work after midnight belongs to the new date; it does not automatically remain part of the previous evening's trade.

If you close at 2 a.m. using today's screen, you close the **new** calendar date. Sales later that morning on that same date are then blocked unless it is reopened. Unclosed earlier dates may also be absorbed. Therefore a routine 2 a.m. close is not equivalent to an overnight business day ending at 2 a.m.

Do not solve this by changing phone time or repeatedly reopening as the normal workflow. A shop needing a consistent evening-through-2 a.m. trading day needs configurable cutoff and supported prior-day close design before a clean fit can be claimed.

### Calendar-day 24/7 operation

A possible **limited operating procedure**, subject to a rehearsal on the actual build, is:

1. Coordinate a short posting/cash pause before calendar midnight.
2. Finish that date's work, resolve pending deliveries and sync where enrolled.
3. Count and close the current date while the screen still shows that date.
4. Resume recording only when the screen has refreshed to the next calendar date and shows it open.
5. Keep the carried-forward drawer balance and record custody handover separately.

This requires closing before midnight, accepting a pause, and verifying date refresh. If the count/close crosses midnight, reopen/refresh the screen and check its date before saving; do not assume the old loaded date is still the posting target. The existing workflow is not an atomic midnight handover tool, so this procedure is not a guarantee of seamless uninterrupted selling.

If the business cannot accept that pause or needs reliable after-midnight closing of the previous day, the present UI is not a good complete fit.

### Staff handover is not day closing

At a 3 p.m. shift change, save a cash count if permitted, compare with the ledger, record the handover note, and switch to the incoming user's account. Leave the business date open if trading continues.

There are no separate shift sessions/till balances, independent cashier floats, per-shift close/reopen, paid-in/paid-out till management or automatic salary-from-shift calculation. A count is timestamped evidence against the shared drawer, not a separate shift settlement report.

### Handover note template

```text
Date/time and displayed business date:
Outgoing / incoming user:
Physical Cash / expected Cash / variance:
Milk liters / Yogurt kg:
Pending deliveries and unresolved entries:
Backup/sync notice:
Custody acknowledgement and follow-up:
```

This is a suggested shop procedure, not an automatically generated shift report.

### What a full 24/7 implementation would need

Configurable business-day boundaries; explicit selected-date closing after rollover; date-consistent saves during midnight; supported shift/till sessions and custody transfers; an accepted treatment of multiple devices during count/close; and a verified late/offline-entry recovery journey. Those are product changes, not settings already hidden in this build.

## 17. Weekly/monthly routines, troubleshooting and checklists

### Weekly routine

- Agree supplier receipts/payments against statements and outstanding balances.
- Review customer debt and record actual collections without duplicating sales.
- Check staff payments and attendance; calculate agreed pay outside unsupported payroll features.
- Inspect unusual yield/spoilage and correction history.
- Review weekly Milk plans and collection targets where used.
- Confirm a current off-phone backup and investigate recurring sync/backup notices.

### Monthly routine

- Review Owner reports for the correct period and all cost-coverage notices.
- Review actual expenses, salaries, supplier costs and production conversion allocations.
- Approve/review the month's yogurt conversion-cost policy and supporting evidence.
- Update future/current rates deliberately and verify sale defaults.
- Review user roles, inactive staff, master-data archive status and recovery-secret custody.
- Review monthly budgets and perform an isolated recovery drill after material changes.

### Troubleshooting

| What you see | What to do |
|---|---|
| Day is closed | Confirm the displayed date. For further work on it, obtain authorized reopen, then recount/reclose. |
| Not enough stock | Check missing milk receipt/production, product/unit and mistaken quantities. Correct the real record. |
| Cash count cannot close | Use the latest count, give a non-zero variance reason, resolve pending deliveries, and recount if Cash changed. |
| Cash differs from sales | Reconcile float, credit sales, collections and Cash outflows; check payment accounts. |
| Expected differs from physical Cash | Save the actual count and investigate. Counting alone does not alter the ledger. |
| Expense absent from Cashier Activity | Ask a supervisor to inspect Records; do not duplicate it. |
| Attendance says unlinked | Link the local user to the correct employee profile. |
| Record already corrected | Follow the replacement chain and correct the latest effective record. |
| Correction cannot change wrong account/product | That form does not provide a general reassignment/cancellation. Obtain the supported remedy. |
| Profit unavailable/incomplete | Add valid opening cost evidence, review Yogurt policy and unsupported movements. Missing cost is not zero. |
| Session expired or Quick PIN fails | Select your account, use the full passphrase where available, and re-enroll Quick unlock through Personal security if needed. |
| Privileged action blocked while offline | Check enrolled verification/grace status and reconnect; routine-entry permission does not imply reopen/setup permission. |
| Backup delayed or stale | Check last success, network policy, battery/storage, Drive authorization and quota; run Back up now. |
| Wrong backup passphrase | Use the secret for that file/vault. Login PIN, server credential and Owner recovery code are not substitutes. |
| Upload halted on closed-day work | Preserve evidence; reopen on the closing device through the supported route, drain/reconcile, recount and reclose. |
| Two phones disagree | Check queues/review items and current metadata/planning limitations. Do not repeatedly re-enter or restore blindly. |
| App interrupted during save | Inspect Records before retrying; avoid duplicate entry. |

### Opening checklist

- Correct user, shop and business date; date open.
- Physical float and stock compared with carried-forward records.
- Pending deliveries and planned work reviewed.
- Phone charged; backup/sync concerns checked.
- Staff attendance/linking checked where relevant.

### Closing checklist

- All actual work entered once with correct account/unit.
- Other writers stopped during count/close.
- Pending delivery coverage resolved honestly.
- Sync status reviewed if enrolled; earlier-date coverage understood.
- Fresh physical Cash count saved, variance investigated/explained.
- Close success verified; backup checked; custody and sign-out completed.

## 18. Current limits and verification notes

### Before replacing the existing record system

The repository records implementation of the main dairy workflows and earlier production phone/server acceptance. It also leaves a current-build shopkeeper trial, independently checked shop day, recovery rehearsal, full release validation and some money/access boundaries unfinished. Treat this manual as an operating guide to inspected behavior, not a release-readiness certificate.

The most material boundaries are:

- **General retail:** Liters/Kg catalog support and dairy-specific receiving/production/costing do not establish universal shop support.
- **Continuous trading:** calendar dates; today's native close; no configurable overnight cutoff or complete shift/till accounting.
- **Owner withdrawals:** no supported ordinary reversal/correction yet.
- **Money precision:** display/receipt conversion paths use differing rounding approaches. Fractional-paisa and correction boundaries require the recorded policy review. The examples here do not prove all such cases consistent.
- **Multi-device equivalence:** live sync exists, but incoming people/catalog profile updates and planning are incomplete; full two-phone ledger/closed-day acceptance remains required.
- **Access:** local per-person roles/PINs exist; complete central device revocation and server-verified clerk attribution are not established. Server Admin-to-Owner credential-reset trust requires an explicit operating decision.
- **Native journeys:** underlying recovery/historical-date services do not prove every corresponding user-facing path is present. Use the supported procedure for the installed build.
- **Missing wider workflows:** no complete split-payment/refund/general cash-transfer flow, arbitrary purchasing/recipes, barcode checkout, receipt printing, payment hardware, automated payroll, tax filing or statutory statements.
- **Credit expansion:** debt balances/sales/collections exist; the planned credit-aging/reminder/bill-sharing program is not delivered as a complete feature.

### How this manual was checked

Business behavior was checked against current Kotlin domain services, native facades/ViewModels and selected schema guards. Historical README/blueprint claims were not treated as proof where later source differs. No live business data, user accounts or installed app were changed while preparing the manual. No new runtime/device certification is claimed.

Repository verification map (for maintainers updating the manual):

| Manual topic | Main inspected implementation |
|---|---|
| Scope and latest readiness | PRODUCT_SCOPE.md; PRODUCT.md; REMAINING_WORK_RECONCILED_2026-10-11.md |
| Local roles/access | domain/Security.kt; nativeui/auth/NativeAuthenticationScreens.kt |
| Store identity and initial balances | domain/StoreIdentity.kt; domain/OpeningState.kt; application/owner/NativeOwnerShopSettingsFacade.kt |
| Receipt/sale formulas | domain/VendorPurchase.kt; domain/Sale.kt; application/sale/NativeSaleFacade.kt |
| Stock guards and corrections | data/DatabaseSql.kt; domain/Correction.kt; application/commands/HistoryBridgeCommandService.kt; application/owner/NativeOwnerRecordsFacade.kt |
| Count, close, coverage and reopen | domain/PhysicalCashCount.kt; domain/DailyClose.kt; data/Phase3DailyCloseDao.kt; application/cashclose/NativeCashCloseFacade.kt |
| Calendar-date/current-date UI behavior | sync/BusinessClock.kt; nativeui/cashclose/NativeCashCloseViewModel.kt |
| Deliveries and attendance | domain/Delivery.kt; application/operations/NativeDeliveriesFacade.kt; application/operations/NativeAttendanceFacade.kt |
| Owner withdrawal limitation | domain/OwnerDrawing.kt; domain/Correction.kt; application/drawing/NativeDrawingFacade.kt |
| Costing, reports and plans | domain/Costing.kt; domain/BudgetManagement.kt; application/owner/NativeOwnerReportsFacade.kt |
| Backup and Drive schedule | application/owner/NativeOwnerBackupFacade.kt; cloud/CloudBackupScheduling.kt; cloud/CloudBackupCoordinator.kt |
| Server sync and degraded mode | application/owner/NativeOwnerSyncFacade.kt; nativeui/owner/OwnerSyncScreen.kt; domain/ServerAccount.kt |

Kotlin paths above are relative to `phase18/app/src/main/java/com/saaf/milkshop/`. For an updated build, recheck those sources and rehearse setup, normal work, corrections, count/close, next-day rollover and isolated restore before issuing a new edition.
