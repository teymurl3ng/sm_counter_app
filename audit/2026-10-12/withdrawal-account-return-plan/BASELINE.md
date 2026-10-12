# Verified baseline

Planning only, 2026-10-12. No app code, tests, migrations, installs, server state or shop data changed in this step.

The previous withdrawal remedy's delivered hashes still match. Its 16 distinct passing cases were checked against their XML, rather than accepted from the previous summary. This is verification of prior evidence, not a new test run. Current Room version is 14. SOURCE_HASHES.json captures the 22 relevant current files.

Observed implementation:
- OwnerDrawingService can book any existing Cash/Bank account; the native entry facade always chooses Cash.
- CorrectionInput.OwnerDrawing supports amount correction/cancellation. CorrectionService and the SQL guard force the root's original account and one exact cash delta. Owner/Admin require both CORRECTION and OWNER_DRAWING permissions. Manager's ordinary correction permission is insufficient.
- Records has explicit cancellation, mandatory fresh reason and Cancelled state. There is no editable payment account or actual returned-money operation.
- Events store amount and correction parent, but no explicit corrected cash-account truth or separate voluntary-owner-money-in type.
- Sync envelopes group ledger/actor/satellite children into the parent event payload and remap account/product/correction references by UID. The receiver currently considers an identical existing event present when the known ledger/spoilage work is empty; new owner-funds metadata must be included in that completeness decision.
- Backup restore inserts event headers before financial rows. New details must restore between them, in dependency order. Snapshot schema version is 10, independently of Room version 14.
- Analytics sums explicit SALE/EXPENSE/SALARY and other families. New owner-money-in must remain outside revenue/operating expense/profit and be named explicitly in the touched native history readers.
- Generic account ledger UI supports Customer/Vendor, not Cash/Bank. This plan adds a withdrawal-linked history section in Records, not a new cash-ledger product.
- No native date-picker usage was found in the bounded native-ui search. A small Material date dialog is planned for the new receipt date field.

Remote payload validation and deployment were not queried. New event-type acceptance must be checked in the first source-only preflight that needs it; compatibility with a live server is not asserted here.
