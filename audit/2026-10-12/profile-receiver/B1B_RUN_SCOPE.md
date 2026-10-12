# B1b — controlled catch-up of previously consumed profiles

Status: [complete at bounded QA scope](B1B_RESULT.md). Planning receiver/catch-up remains B2/B10.

The existing Supabase `sync_records` key is unique for (shop, table, UID), and `sync_pull` reads current rows ordered by server sequence. A receiver that advanced its cursor before B1a skipped profile satellites can therefore replay current profile-bearing Product/Account rows from sequence zero without resetting its ordinary cursor or enrolling again.

After an ordinary pull finishes, run this catch-up once per installation/shop/server identity. A fresh complete pull from zero has already seen the same rows, but the existing aggregate pull report cannot distinguish profile refusal from unrelated deferred planning rows, so this one-time profile-only pass also verifies fresh enrollments without silently marking malformed profiles complete. Replay only Product/Account records that carry a `profile` key, in bounded pages. Preserve the existing pending-edit guard and parks, no-push-echo behavior and capture-trigger transaction boundary. A failed fetch or malformed profile must leave the completion marker unset for retry; an already queued local edit may remain parked for ordinary conflict handling. Keep the completion marker outside backed-up Room and bind it to the current installation identity, environment and shop so restore/re-enrollment does not silently inherit it.

Test a consumed cursor, paging, retry after failure, different local IDs, pending edit, repeated call, and fresh cursor-zero path with an isolated fake transport. Do not reset cursors, change server RPCs, mutate live records, expose a new general repair menu or begin planning sync. Run only affected QA tests.
