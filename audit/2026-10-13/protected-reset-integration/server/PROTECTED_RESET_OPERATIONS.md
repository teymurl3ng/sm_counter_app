# Protected credential reset — deployment and Owner review

Prepared changes are not a deployed fix. First verify the destination, current C1.6 schema and migration history. Apply `20261012194632_protected_credential_reset_guard.sql` before enabling the matching Viewer and CLI. Verify RPC grants/RLS and a disposable identity's denial/success/pending cases; existing staff credentials are not acceptance fixtures. Keep privileged keys outside browser bundles. Do not roll back to the old direct-Auth reset client while claiming the policy remains protected.

Owner: same-business linked non-terminated targets in the five supported roles, including disabled targets. Admin: active Manager/Cashier/Operator only. Reset never enables an account. CLI is separate Owner-controlled service-key maintenance, with existing project confirmation retained.

Each reset has a UUID reference. The Viewer “Check reset status” control and CLI `reset-server-credential --username USER --status` read pending reference/state without sending an Auth update. A reference may belong to a reservation in which the selected staff member is the caller. No password is stored in the reservation or audit.

- Denied reservation: this invocation sent no Auth update.
- Lost begin/claim acknowledgement: this invocation sent no Auth update; a reservation may remain. Check status before doing anything further.
- Confirmed Auth success: save the displayed credential immediately. HTTP 202/CLI exit 2 can mean completion/audit acknowledgement is pending. Retry **completion only**, using `admin_finish_credential_reset` with the reference and `completed` once the trusted gateway's success is established. It is idempotent and sends no Auth update.
- Auth error or lost response: outcome is unknown. Neither a transport error nor an HTTP error is treated as proof of no mutation. Keep the guard; do not resend Auth, use a timeout unlock, or assert `not_applied` from a guess. The ordinary UI offers no force-release button.

Only the trusted server/maintenance operator may resolve requests. For a definitely undispatched reserved request, verify state and absence of dispatch under the same database request-row lock before cancelling with `not_applied`; a separate unlocked status read cannot authorize cancellation because another worker could claim it. A dispatching request may use `not_applied` only with trusted proof no Auth mutation occurred and no request can still complete. An uncertain request cannot be cancelled as not-applied. Known success can complete it. If outcome evidence is unavailable, it remains held: record that operational blocker and resolve it through the separately scoped recovery process. Do not erase rows or alter the guard to bypass it.

Pending locks cover caller/target authorization and identity fields; display-name edits remain possible. Do not drop the schema, trigger or migration while any participant remains pending. Status cannot retrieve a lost one-time credential. Production deployment acceptance must include this recovery limitation and Owner custody of one-time credentials.

No real credential or remote database was changed in the synthetic integration checks.
