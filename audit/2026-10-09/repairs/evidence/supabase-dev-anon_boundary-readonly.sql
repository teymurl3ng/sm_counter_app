BEGIN READ ONLY; SET LOCAL statement_timeout='10s'; SELECT set_config('request.jwt.claims','{"role":"anon"}',true); SET LOCAL ROLE anon; SELECT count(*) FROM public.sync_records; ROLLBACK;
