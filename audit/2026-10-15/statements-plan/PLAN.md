# Phase 5: customer statements and deliberate manual reminders

15 October 2026. Baseline `a596cd8e663415ae8d56ecf6a9d762cf37a838d9`. Planning/research/judgment complete. Piece1 is now implemented and accepted at its affected local QA scope; pieces2–3 remain pending. This is phase 5/L3 of the existing six-phase plan. No new phase is added.

**UI fit supplement:** [Exact placement, design-resource reuse, navigation, reflow and feedback](UI_FIT.md), with an [interactive layout study](UI_PREVIEW.html). This is the controlling layout brief: customer detail is locally regrouped first; a single Statements & reminders entry opens dedicated Statement/Reminder body panes inside the existing role host. The first implementation piece includes that presentation extraction, not a new global design pass. Native acceptance remains pending.

## Plain-English outcome

Open a customer, preview a statement for a chosen period, then share its PDF or images. Separately, prepare a polite WhatsApp or SMS message showing that customer's current outstanding balance. The operator reviews the recipient and sends from the other app. Sunday bill preparation does not change any sale's agreed payment deadline.

## Verified baseline

- `NativeOwnerCustomersFacade` already loads customer identity, phone, credit facts and the existing ledger in a consistent Room read transaction. Customer detail already has account history and terms/review history. Reuse this destination; shared header and bottom navigation remain fixed.
- `NativeOwnerRecordsFacade.loadLedger` uses permission-aware `HybridReadModelService.accountLedger`, backed by `HybridFinancialReadService` and `AccountLedgerService`. Full financial permission is checked. Entries contain business/recording dates, original/correction relationships, signed movements and running balances; the canonical current balance comes from `account_balances`.
- `FinancialLogDao.findAccountLedger` includes original entries and correction deltas. Hiding superseded originals would lose valid financial movements. Dates order by business date, event ID and log ID; retain the existing local ledger order for the statement.
- The read DTO rounds each movement and each cumulative balance separately. Summing individually rounded movement labels is not guaranteed to equal an aggregate rounded balance. Statement arithmetic must explicitly reconcile raw aggregate amounts and displayed paisa; the existing financial engine is not being rewritten.
- `StoreIdentityService` supplies configured shop name, phone, address and supported currency PKR. Customer profiles already have optional phone/address. No new currency feature is required.
- Searches of native main Java/Kotlin and the main manifest found no `PdfDocument`, `FileProvider`, share/composer intent or `reminder_log` implementation. The existing security audit storage is a candidate for local observable-action records; it is not proof of cross-phone message history.
- The October blueprint's missing picker/aging/status and no-schema-change claims are superseded by phases 3–4. Its PDF/image/manual-reminder commitment remains current. Product visuals must support the actual catalog, not two hardcoded products.

## Research and adaptation

1. [Zoho Books statement explanation](https://www.zoho.com/books/academy/accounting-principles/what-is-a-statement-of-accounts.html) describes customer activity and a summary with opening balance, invoiced amount, payments and balance due. The official indexed source was accessible through search; direct page retrieval failed. Adopt the statement structure, using this app's posted sales/collections/correction movements rather than inventing invoice records. This is a product example, not a legal standard.
2. [Android secure sharing](https://developer.android.com/training/secure-file-sharing) specifies content URIs with temporary access grants through FileProvider. Use a private statement-cache subdirectory and a narrowly scoped, non-exported provider with URI grants. Do not expose the database or entire cache.
3. [Android Sharesheet](https://developer.android.com/develop/ui/compose/sharing/send) supports text and binary sharing. Share PDF as `application/pdf`, PNG as `image/png`, and multiple statement pages as multiple image streams. Choosing a target or opening its composer cannot establish sending/delivery.
4. [Android messaging intents](https://developer.android.com/guide/components/intents-common#Messaging) documents SMS composition through `ACTION_SENDTO` and `smsto:`. Use composition only, with no SEND_SMS permission. Catch missing handlers and show an actionable explanation.
5. [WhatsApp click-to-chat](https://faq.whatsapp.com/5913398998672934/?locale=en_US) documents recipient links with full international numbers. Validate/confirm the destination before opening; an ordinary browser fallback is not evidence that WhatsApp accepted the draft. File sharing and recipient-addressed text composition remain separate actions; do not promise an automatic attachment to the chosen chat.
6. [Android PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument) offers page-based Canvas output. Use one shared layout plan for PDF and bounded-size image pages, with text wrapping and page numbers. Do not render an unbounded tall bitmap.

## Independent judgment

The judgment hook was executed and saved in `judgment-hook.json`; it emits a fixed reminder and does not validate this plan. The strongest simpler alternative is text-only balance sharing. It would be useful but would leave the already agreed PDF/image commitment unfinished. Conversely, a template editor, new invoice subsystem or cloud messaging provider would exceed L3. Choose one deterministic statement layout and manual handoff.

A historical period's closing balance can differ from today's debt. Keep those concepts visibly separate. The exported statement is a frozen local snapshot for its named period, with generation time and a local-data explanation. A reminder refreshes current facts and names the current as-of date, not the historical statement's amount. Private Owner flags/reasons, credit limits, staff credentials and internal notes must never appear in customer-facing output.

## Frozen decisions and boundaries

- Entry: existing Customers detail, with `Statement` and `Prepare reminder` actions. Use the existing full-financial access boundary for Owner/Manager/Admin; no new Cashier access or role expansion. Revalidate permission and selected customer at generation/handoff; stale session/results are discarded.
- Period: Last 7 days (today and previous six business dates) by default; This month; Custom inclusive start/end, ending no later than today. Convert internally to an exclusive next-day end. Show the actual dates clearly. This selects activity, not payment terms or a recurring schedule.
- Header: configured shop name/contact, Customer statement, customer name, period, generated/as-of text. Body: opening balance; dated sales, collections and explicitly named correction/other adjustments; running balance; closing balance. Positive balance means Amount outstanding, negative means Customer advance, zero means Settled. Include neutral age/terms explanation only when supported; no Owner review labels.
- Arithmetic: obtain a single immutable read snapshot. Opening is the authoritative aggregate before the selected period; closing includes movements through its end. Use existing canonical money aggregation/rounding. Reconcile displayed row movements from successive canonical cumulative balances, with an explicit minor-unit rounding explanation where they differ from individually rounded raw movements. Do not silently invent payments or clamp a negative balance. Reconcile today's closing to the canonical account balance; if inconsistent, show an error instead of exporting a misleading statement. Keep all original/delta corrections and opening/advance entries.
- Activity labels: Sale, Payment received, Correction/adjustment, Opening balance/other known movement as appropriate. Negative correction is not called a customer payment. Unknown product attribution gets a plain adjustment label; no guessed quantity or rate. Private free-form notes stay excluded.
- Product treatment: compact catalog-based product markers plus names/quantities in supported units, with a neutral fallback for unknown/archived products. Do not add unlike units into a pie chart, claim that current product names are historical snapshots, or let decorative totals override ledger amounts. Text remains sufficient without artwork.
- Preview/export: the preview, PDF and PNG pages use the same frozen snapshot and page layout. PDF has a complete readable activity table; images are numbered pages of the same content. Long text wraps and page breaks repeat table headings. No content clipping or silent row cap. Finite rendering/resource limits must fail clearly and offer a smaller period; never claim a truncated document is complete. No new top-level navigation or common-chrome redesign.
- Share: explicit format choice, Android chooser, loading/error/retry feedback and duplicate-tap guard. File names use opaque statement IDs, not customer phone/name. Grant only selected files, keep them available while a receiver may read them, and remove files older than 24 hours at the next statement-use/app-start cleanup. Cache can be evicted by Android; explain/regenerate missing files. Do not delete immediately on app return or claim exact background cleanup timing. Do not clear files still in the active preview/handoff. User-created copies in other apps are outside our cache cleanup.
- Reminder: preview neutral wording with shop, customer, current positive outstanding balance and as-of date; separately identify known overdue amounts only when the existing aging result supports them. No reminder urging payment on settled/advance accounts. Recheck current balance and phone before handoff; changed facts require a refreshed preview. Never auto-open an external app on customer selection.
- Recipient: show and confirm saved phone; allow an explicit international-format override for this one draft without changing the customer profile. No silent country-code guessing. Missing/invalid phone leaves statement sharing available and gives Edit customer/enter draft recipient options. WhatsApp text includes no internal flags. SMS is text-only and the external app remains responsible for sending/cost display; mention carrier charges may apply before composition.
- Observable history: reuse the existing local security-audit mechanism if its inspected contract allows it, storing minimal customer UID, statement/draft ID, period/as-of, channel and observed action only. Labels may be Prepared, Share chooser opened, Message composer opened, Handoff failed, or Target selected when actually observed. Never label Sent/Delivered/Read; external cancellation is generally unknown. This is device-local activity, not a synced reminder queue or suppression rule. No new cloud/schema/sync contract in phase 5; if reuse is unsuitable, re-scope only this persistence boundary before adding storage.
- Exclusions: automatic sending/scheduling, bulk selection, payment links, provider API/purchase, tax-invoice claims, real customer contact during QA, and changes to ledger/sale rules. Phase 6 owns notification automation, cross-device reminder coordination and the separate automatic-SMS decision.

## Three bounded implementation pieces

1. **Statement facts and preview.** Add a focused statement read model/facade beside existing owner readers, period/snapshot reconciliation and customer-detail preview. Reuse existing customer/controller/session and information-surface patterns. Acceptance: independently computed opening/activity/closing amounts, accurate correction and advance labels, permission/stale-state checks.
2. **PDF/image export and Android share.** Add a single page layout/renderer, private cache/FileProvider configuration and share adapter. Acceptance: inspect rendered PDF/PNG against the preview, multi-page/long-name coverage, temporary access scope, cleanup/missing-file recovery and no-handler/cancel behavior.
3. **Manual draft and truthful local action history.** Add recipient/message review, WhatsApp/SMS composition, balance refresh and minimal local observable audit. Acceptance: URI/text encoding, missing/ambiguous phone/apps, stale balance, denial and duplicate taps; use synthetic receivers, never send a real message.

Likely touched areas: new statement classes under `application/owner` and a narrow Android export/share package; existing `OwnerCustomersScreen`, controller, More state/ViewModel/host wiring; main manifest and new XML provider paths. Existing ledger/identity/aging services are read dependencies. Pin exact new test method names when each piece is implemented; do not run entire legacy suites.

## Fixed acceptance examples and stop line

- PKR100 opening + PKR200 sale + PKR50 correction - PKR50 collection = PKR300 closing. Original and correction both remain; correction isn't counted as another original sale/payment.
- Empty period carries opening unchanged. A collection overpays into an advance; labels and reminder availability reflect that. An old sale corrected within the chosen period creates an in-period adjustment.
- Aggregate sub-paisa example and same-date multiple movements reconcile displayed rows to canonical opening/closing, including cancellation near zero. Do not merely sum independently rounded DTO movements.
- Period boundary/today cutoff, archived/unknown product, more than two products, long Unicode names and large values remain accurate; no leakage of private notes/review flags.
- One consistent snapshot during concurrent write; preview/export totals match; refresh before reminder catches a changed payment. Expired or changed session/customer cannot export the wrong account.
- Synthetic short and multi-page documents are visually inspected; PDF and PNG pages have equal facts, repeated headers/page numbers and readable wrapped amounts. Light/dark app preview at normal/200% text retains shared chrome.
- Verify MIME/URI grants, selected-file-only provider access, no handler, chooser dismissal, missing file and aged cleanup. Test receiver can read granted data, not sibling/database files. Returned app state never claims delivery or deletes finance.
- One connected phone suffices for focused native/renderer/share checks. A second phone or backend migration is not required for these read-only exports; phase 6 separately plans coordinated reminders. No full suite or whole-app audit.

Stop after these three pieces and affected checks. Record unrelated findings for later rather than adding to phase 5. The owner/staff's actual private handover remains phase 1. This planning pass changed no app, phone, cloud data or customer contact.

## Progress

- [x] ✅ Baseline, primary-source research, independent judgment and scoped acceptance.
- [x] ✅ ~~1. Statement snapshot and preview, including customer-detail fit.~~ [Result](PREVIEW_RESULT.md),13 unique affected A06 methods.
- [ ] 2. PDF/image output and sharing.
- [ ] 3. Manual message preparation and observable local history.
