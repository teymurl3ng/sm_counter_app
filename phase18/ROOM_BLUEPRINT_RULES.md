# Room implementation rules — Phase 1

1. The finalized Core Logic Spec is the source of truth for business behavior.
2. The finalized SQL is the reference for schema shape, constraints, triggers, views, indexes, and seeded reference data.
3. Do not invent business rules. When the spec is silent, stop and flag the decision instead of guessing.
4. Do not introduce Python or a second business-logic implementation.
5. Do not convert the project into a full accounting system, server application, ORM-heavy architecture, or multi-user backend.
6. Do not delete or weaken existing integrity protections merely because Room has a different API. Reproduce their behavior with Room-supported mechanisms, SQLite triggers, or explicit application-layer validation as appropriate.
7. Business transactions that touch multiple tables must be atomic. Use Room 3 write transactions.
8. Current stock and account balances remain derived read models; the app must not maintain duplicate mutable balance fields.
9. Correction semantics are already defined by the spec: the correction event stores corrected truth; attached ledger movement stores the delta.
10. Historical purchase rates are frozen on the event and audit calculations use the frozen snapshots.
11. Delivery surcharge is allowed on any sale; channel remains reporting-only.
12. Every phase must ship with executable tests for the behavior it implements.
13. Do not call a phase complete based only on code inspection. Run the strongest available build/test verification and state exactly what was or was not executable in the environment.
14. Prefer boring, explicit code over clever abstractions.
15. Do not perform unrelated cleanup or refactoring during implementation.
