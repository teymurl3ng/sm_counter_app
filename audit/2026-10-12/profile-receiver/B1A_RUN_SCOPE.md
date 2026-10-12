# B1a — apply five profile satellites on receipt

Status: [complete at bounded QA scope](B1A_RESULT.md). Controlled historical catch-up remains B1b.

This run fixes only the receiver's handling of Product, Customer, Vendor, Salary/Employee and Operating Expense/category profiles. Existing sender payloads, parent capture, transport and UI are unchanged. An absent `profile` key preserves an existing satellite for old parent-only payloads; explicit JSON null or a malformed/partial object is refused atomically and audited, rather than silently erasing or defaulting fields. Incoming parent IDs are ignored; the local parent UID determines the child key. A pending local parent edit remains parked by the existing guard.

Implement insert/update of the complete satellite in the same per-record savepoint as its parent, with existing product/account validation and finite numeric values. Verify a fresh receiver with different local IDs, updates/optional-field clearing/archive/rate/active flags, replay, malformed rollback, parent-only compatibility and pending-edit protection using affected sync tests only.

Earlier server rows already consumed by old receivers need a separate controlled catch-up path, B1b. Do not reset the ordinary pull cursor, reinstall the app, enroll again, alter server RPCs, or begin planning sync in this run.
