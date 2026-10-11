// Replay the relevant live catalog privileges/policies in a disposable local DB.
// No real users, credentials, live data, APIs or migrations are changed.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import assert from 'node:assert/strict';
import pgDriver from 'pg';

const server = 'C:/Users/Teymur/Desktop/saaf-anchor-tmp';
const port = Number(process.env.SAAF_SQL_TEST_PORT);
assert.ok(port > 0, 'Use native-run.py with SAAF_SQL_TEST_SCRIPT=live-boundary.mjs');
const pg = new pgDriver.Client({ host: '127.0.0.1', port, user: 'saaf_offline', database: 'postgres' });
await pg.connect();
const evidence = new URL('../evidence/', import.meta.url);
const live = JSON.parse(readFileSync(new URL('supabase-dev-catalog-2026-10-10.json', evidence), 'utf8'));
let checks = 0;
async function check(name, fn) { await fn(); checks++; console.log('PASS ' + name); }
async function expectCode(sql, params, code) {
  await assert.rejects(pg.query(sql, params), error => error.code === code);
}
const business = '11111111-1111-4111-8111-111111111111';
const otherBusiness = '11111111-1111-4111-8111-111111111112';
const owner = '22222222-2222-4222-8222-222222222221';
const cashier = '22222222-2222-4222-8222-222222222224';
const shop = '33333333-3333-4333-8333-333333333333';
async function caller(id) {
  await pg.query("SELECT set_config('request.jwt.claim.sub',$1,false)", [id]);
  await pg.query("SELECT set_config('request.jwt.claim.role','authenticated',false)");
  await pg.query('SET ROLE authenticated');
}
try {
  await pg.query(`CREATE ROLE anon; CREATE ROLE authenticated; CREATE ROLE service_role BYPASSRLS;
    CREATE SCHEMA auth; CREATE TABLE auth.users(id uuid PRIMARY KEY);
    CREATE FUNCTION auth.uid() RETURNS uuid LANGUAGE sql STABLE AS
      $$SELECT nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
    CREATE FUNCTION auth.role() RETURNS text LANGUAGE sql STABLE AS
      $$SELECT current_setting('request.jwt.claim.role',true)$$;
    GRANT USAGE ON SCHEMA auth TO anon,authenticated,service_role;
    GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO anon,authenticated,service_role;`);
  const files = readFileSync(join(server, 'bootstrap.sql'), 'utf8').split(/\r?\n/)
    .filter(l => l.startsWith('\\ir ')).map(l => l.slice(4));
  for (const file of files.filter(f => f !== 'migration_c1_6_integrity.sql'))
    await pg.query(readFileSync(join(server, file), 'utf8'));
  // Mirror the actual live sync trigger, insert policy, privileges and absent shape check.
  for (const name of ['sync_staff_shop_ids','sync_normalize_payload','sync_records_assign_seq','sync_push','sync_pull']) {
    const definition = live.functions.find(f => f.name === name)?.definition;
    assert.ok(definition?.startsWith('CREATE OR REPLACE FUNCTION public.' + name));
    await pg.query(definition);
  }
  await pg.query(`ALTER TABLE public.sync_records DROP CONSTRAINT sync_records_payload_check;
    DROP POLICY sync_records_authorized_read ON public.sync_records;`);
  for (const policy of live.policies.filter(p => p.tablename === 'sync_records')) {
    assert.ok(['SELECT','INSERT'].includes(policy.cmd));
    assert.ok(policy.roles.length === 1 && policy.roles[0] === 'public');
    const expression = policy.cmd === 'SELECT' ? 'USING (' + policy.qual + ')' : 'WITH CHECK (' + policy.with_check + ')';
    await pg.query('CREATE POLICY ' + policy.policyname + ' ON public.sync_records FOR ' + policy.cmd + ' TO PUBLIC ' + expression);
  }
  const grants = live.table_privileges.filter(g => g.table === 'sync_records');
  assert.ok(grants.filter(g => ['anon','authenticated'].includes(g.role)).every(g => g.select && g.insert && g.update && g.delete));
  await pg.query(`GRANT SELECT, INSERT, UPDATE, DELETE ON public.sync_records TO anon,authenticated;
    GRANT USAGE, SELECT ON public.sync_records_server_seq_seq TO anon,authenticated;`);
  await pg.query("INSERT INTO businesses(id,name) VALUES ($1,'Fixture A'),($2,'Fixture B')", [business,otherBusiness]);
  for (const [id,username,role] of [[owner,'fixture.owner','owner'],[cashier,'fixture.cashier','cashier']]) {
    await pg.query('INSERT INTO auth.users(id) VALUES ($1)',[id]);
    await pg.query('INSERT INTO staff(id,auth_user_id,business_id,username,display_name,role) VALUES($1,$1,$2,$3,$3,$4)',[id,business,username,role]);
  }
  await pg.query("INSERT INTO shops(id,business_id,name,shop_timezone) VALUES($1,$2,'Fixture Shop','Asia/Karachi')",[shop,business]);
  await pg.query('INSERT INTO staff_shops(staff_id,shop_id) VALUES($1,$2)',[cashier,shop]);
  const correction = [{table:'events',uid:'rpc-correction',payload:{corrects_event_uid:'earlier-event'}}];
  await caller(cashier);
  await check('live-model RPC refuses cashier history correction', async () => {
    const r=(await pg.query('SELECT sync_push($1::jsonb,$2,$3::uuid,0) AS result',[JSON.stringify(correction),'fixture-device',shop])).rows[0].result;
    assert.equal(r.ok,false); assert.equal(r.error.code,'correction_privilege_required');
  });
  await check('live-model direct INSERT bypasses correction and business-header checks', async () => {
    await pg.query('INSERT INTO sync_records(business_id,shop_id,table_name,row_uid,payload) VALUES($1,$2,$3,$4,$5::jsonb)',
      [otherBusiness,shop,'events','direct-correction',JSON.stringify(correction[0].payload)]);
    assert.equal((await pg.query("SELECT business_id FROM sync_records WHERE row_uid='direct-correction'")).rows[0].business_id,otherBusiness);
  });
  await check('live-model direct INSERT accepts unknown table and JSON null', async () => {
    await pg.query("INSERT INTO sync_records(business_id,shop_id,table_name,row_uid,payload) VALUES($1,$2,'unknown_table','direct-null','null'::jsonb)",[business,shop]);
  });
  await pg.query('RESET ROLE');
  const repair=readFileSync(new URL('../SUPABASE_LIVE_REPAIR_2026-10-10.sql',import.meta.url),'utf8');
  await check('incompatible history aborts the entire repair without dropping rows or partially revoking access', async () => {
    await assert.rejects(pg.query(repair), error => error.code === '23514');
    await pg.query('ROLLBACK');
    assert.equal((await pg.query("SELECT has_table_privilege('authenticated','public.sync_records','INSERT') AS permitted")).rows[0].permitted,true);
    assert.equal((await pg.query("SELECT to_regprocedure('public.admin_verify_created_identity(uuid)') AS fn")).rows[0].fn,null);
    assert.equal((await pg.query('SELECT count(*)::int AS n FROM sync_records')).rows[0].n,2);
  });
  // Remove deliberately invalid fixture rows only; live rows are never touched.
  await pg.query("DELETE FROM sync_records WHERE row_uid IN ('direct-correction','direct-null')");
  await check('reviewable repair applies atomically and reapplies without data changes', async () => {
    await pg.query(repair); await pg.query(repair);
    assert.equal((await pg.query('SELECT count(*)::int AS n FROM sync_records')).rows[0].n,0);
  });
  await caller(cashier);
  await check('repaired boundary denies direct authenticated INSERT and sequence use', async () => {
    await expectCode("INSERT INTO sync_records(business_id,shop_id,table_name,row_uid,payload) VALUES($1,$2,'events','denied','{}'::jsonb)",[business,shop],'42501');
    const p=(await pg.query("SELECT has_sequence_privilege('authenticated','public.sync_records_server_seq_seq','USAGE') AS permitted")).rows[0];
    assert.equal(p.permitted,false);
  });
  await check('repaired boundary preserves correction authorization', async () => {
    const r=(await pg.query('SELECT sync_push($1::jsonb,$2,$3::uuid,0) AS result',[JSON.stringify(correction),'fixture-device',shop])).rows[0].result;
    assert.equal(r.error.code,'correction_privilege_required');
  });
  await pg.query('RESET ROLE'); await caller(owner);
  await check('repaired boundary preserves ordinary RPC push, pull and timezone profile', async () => {
    const envelope=[{table:'products',uid:'valid-product',payload:{name:'Fixture Milk',unit:'L'}}];
    assert.equal((await pg.query('SELECT sync_push($1::jsonb,$2,$3::uuid,0) AS result',[JSON.stringify(envelope),'fixture-device',shop])).rows[0].result.ok,true);
    const pull=(await pg.query('SELECT sync_pull(0,200,$1::uuid) AS result',[shop])).rows[0].result;
    assert.equal(pull.ok,true); assert.equal(pull.records.length,1);
    const profile=(await pg.query('SELECT my_profile() AS result')).rows[0].result;
    assert.equal(profile.shops[0].shop_timezone,'Asia/Karachi');
  });
  await pg.query('RESET ROLE');
  await check('repaired payload constraint rejects JSON null even for privileged writes', async () => {
    await expectCode("INSERT INTO sync_records(business_id,shop_id,table_name,row_uid,payload) VALUES($1,$2,'events','shape-denied','null'::jsonb)",[business,shop],'23514');
  });
  await check('repaired internal redactor keeps recursive semantics with fixed search_path', async () => {
    const r=(await pg.query(`SELECT redact_json_secrets('[{"token":"synthetic-secret","nested":{"password":"synthetic-secret"}}]'::jsonb) AS result`)).rows[0].result;
    assert.deepEqual(r,[{token:'[REDACTED]',nested:{password:'[REDACTED]'}}]);
  });
  await check('repaired C1.6 trigger prevents direct removal of the fixture final owner', async () => {
    await expectCode('UPDATE staff SET active=false WHERE id=$1',[owner],'P0001');
  });
  console.log(checks + ' native live-model/repair boundary checks passed. No live writes or real Auth-session verification.');
} finally { await pg.end(); }
