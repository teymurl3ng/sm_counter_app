# Milk Shop — Phase 1

## Scope

Phase 1 is the **Android/Room data foundation plus the first complete business operation**:

1. Android project shell.
2. Room 3.0.1 database using the finalized business model as the blueprint.
3. Products, accounts, events, inventory log, financial log, settings.
4. Canonical read views and calculation-audit views.
5. SQLite integrity triggers reproducing the frozen SQL guarantees that Room annotations alone cannot express.
6. Per-connection foreign-key enforcement.
7. Seed data exactly matching the frozen SQL.
8. Vendor Purchase transaction implemented as a Room/Kotlin business operation.
9. Instrumentation tests for the purchase formula, purchase effects, views, and early input rejection.

## Explicitly out of scope

- No UI beyond a launch placeholder.
- No sales implementation yet.
- No production implementation yet.
- No vendor payment implementation yet.
- No credit collection implementation yet.
- No expense implementation yet.
- No salary implementation yet.
- No correction workflow implementation yet.
- No network/server/backend.
- No Python.
- No accounting system beyond the finalized spec.

## Blueprint rule

The finalized Core Logic Spec is the authority for business meaning.
The finalized SQL is the structural/integrity blueprint.
This phase reimplements those behaviors in Kotlin + Room rather than mechanically translating SQL.

11D transaction visual migration checkpoint: applied the approved Purchase visual pattern to the remaining transaction screens without changing business behavior or data semantics.
