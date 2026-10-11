import { PGlite } from '@electric-sql/pglite';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import assert from 'node:assert/strict';
import pgDriver from 'pg';

const root = 'C:/Users/Teymur/Desktop/saaf-anchor-tmp';
const nativePort = process.env.SAAF_SQL_TEST_PORT ? Number(process.env.SAAF_SQL_TEST_PORT) : null;
const nativeOptions = {host:'127.0.0.1',port:nativePort,user:'saaf_offline',database:'postgres'};
const connection = nativePort ? new pgDriver.Client(nativeOptions) : null;
if (connection) await connection.connect();
const pg = connection ? {
  query:(...args)=>connection.query(...args), exec:sql=>connection.query(sql), close:()=>connection.end(),
} : new PGlite();
let checks = 0;
async function check(name, fn) { await fn(); checks++; console.log(`PASS ${name}`); }
async function rejected(sql, args, code) {
  await assert.rejects(pg.query(sql, args), error => error.code === code);
}
try {
  // Minimal local stand-ins for Supabase-supplied Auth objects. No credentials,
  // live project configuration or network access are used by this script.
  await pg.exec(`CREATE ROLE anon; CREATE ROLE authenticated; CREATE ROLE service_role BYPASSRLS;
    CREATE SCHEMA auth; CREATE TABLE auth.users(id uuid PRIMARY KEY);
    CREATE FUNCTION auth.uid() RETURNS uuid LANGUAGE sql STABLE AS
      $$SELECT nullif(current_setting('request.jwt.claim.sub', true), '')::uuid$$;
    CREATE FUNCTION auth.role() RETURNS text LANGUAGE sql STABLE AS
      $$SELECT current_setting('request.jwt.claim.role', true)$$;
    GRANT USAGE ON SCHEMA auth TO anon, authenticated, service_role;
    GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO anon, authenticated, service_role;`);
  await check(`fresh bootstrap executes in ${nativePort ? 'native PostgreSQL' : 'PostgreSQL WASM'} with local Auth stand-ins`, async () => {
    const bootstrap = readFileSync(join(root, 'bootstrap.sql'), 'utf8').split(/\r?\n/);
    const files = bootstrap.filter(line => line.startsWith('\\ir ')).map(line => line.slice(4));
    for (const file of files) {
      try { await pg.exec(readFileSync(join(root, file), 'utf8')); }
      catch (error) { console.error(`MIGRATION FAILED ${file}: ${error.code}: ${error.message}`); throw error; }
    }
    await pg.exec(bootstrap.filter(line => !line.startsWith('\\')).join('\n'));
  });
  const business = '11111111-1111-4111-8111-111111111111';
  const otherBusiness = '11111111-1111-4111-8111-111111111112';
  const owner1 = '22222222-2222-4222-8222-222222222221';
  const owner2 = '22222222-2222-4222-8222-222222222222';
  const owner3 = '22222222-2222-4222-8222-222222222223';
  const shop = '33333333-3333-4333-8333-333333333333';
  await pg.query("INSERT INTO businesses(id,name) VALUES ($1,'A'),($2,'B')", [business, otherBusiness]);
  for (const [id, biz, username] of [[owner1,business,'owner.one'],[owner2,business,'owner.two'],[owner3,otherBusiness,'owner.three']]) {
    await pg.query('INSERT INTO auth.users(id) VALUES ($1)', [id]);
    await pg.query("INSERT INTO staff(id,auth_user_id,business_id,username,display_name,role) VALUES($1,$1,$2,$3,$3,'owner')", [id,biz,username]);
  }
  await pg.query("INSERT INTO shops(id,business_id,name) VALUES ($1,$2,'Shop A')",[shop,business]);
  // Supabase supplies base-table service-role privileges. Mirror only the
  // staff permissions needed by this disposable CLI fixture; these are not
  // product migration statements and do not touch any deployed database.
  await pg.exec('GRANT SELECT,UPDATE,DELETE ON public.staff TO service_role');
  await check('direct staff writes cannot remove the final active owner, regardless of other business owners', async () => {
    await pg.query('UPDATE staff SET active=false WHERE id=$1',[owner2]);
    await rejected('UPDATE staff SET active=false WHERE id=$1',[owner1],'P0001');
    await rejected("UPDATE staff SET role='cashier' WHERE id=$1",[owner1],'P0001');
    await rejected('DELETE FROM staff WHERE id=$1',[owner1],'P0001');
    assert.equal((await pg.query('SELECT active FROM staff WHERE id=$1',[owner1])).rows[0].active,true);
  });
  await pg.query("SELECT set_config('request.jwt.claim.role','service_role',false)");
  await check('ambiguous create reconciliation retains unlinked Auth identity and resolves linked identity', async () => {
    const unknown = '44444444-4444-4444-8444-444444444444';
    await pg.query('INSERT INTO auth.users(id) VALUES ($1)',[unknown]);
    async function record(authId, username) {
      return (await pg.query("SELECT admin_record_auth_reconciliation($1,'create_verify',$2,$3,$4,'owner.one','UNREACHABLE',$5) AS id",
        [business,authId,username,owner1,'55555555-5555-4555-8555-555555555555'])).rows[0].id;
    }
    const pending = await record(unknown,'unknown.user');
    assert.equal((await pg.query('SELECT admin_verify_created_identity($1) AS resolved',[pending])).rows[0].resolved,false);
    assert.equal((await pg.query('SELECT count(*)::int AS count FROM auth.users WHERE id=$1',[unknown])).rows[0].count,1);
    const linked = await record(owner2,'owner.two');
    assert.equal((await pg.query('SELECT admin_verify_created_identity($1) AS resolved',[linked])).rows[0].resolved,true);
    await pg.query("SELECT set_config('request.jwt.claim.role','authenticated',false)");
    await rejected('SELECT admin_verify_created_identity($1)',[pending],'42501');
  });
  await pg.query("SELECT set_config('request.jwt.claim.sub',$1,false)",[owner1]);
  await check('fresh sync RPCs assign sequences and return shop-scoped records', async () => {
    const envelope = [{table:'products',uid:'local-product',payload:{name:'Milk',unit:'Liters'}}];
    const pushed = (await pg.query('SELECT sync_push($1::jsonb,$2,$3::uuid,0) AS result',[JSON.stringify(envelope),'local-device',shop])).rows[0].result;
    assert.equal(pushed.ok,true);
    const first = (await pg.query('SELECT server_seq::int AS seq FROM sync_records')).rows[0].seq;
    await pg.query('SELECT sync_push($1::jsonb,$2,$3::uuid,0)',[JSON.stringify(envelope),'local-device',shop]);
    assert.equal((await pg.query('SELECT server_seq::int AS seq FROM sync_records')).rows[0].seq,first);
    envelope[0].payload.name='Updated milk';
    await pg.query('SELECT sync_push($1::jsonb,$2,$3::uuid,0)',[JSON.stringify(envelope),'local-device',shop]);
    assert.ok((await pg.query('SELECT server_seq::int AS seq FROM sync_records')).rows[0].seq>first);
    const pulled=(await pg.query('SELECT sync_pull(0,200,$1::uuid) AS result',[shop])).rows[0].result;
    assert.equal(pulled.ok,true); assert.equal(pulled.records.length,1);
    assert.equal(pulled.records[0].payload.name,'Updated milk');
  });
  await check('authenticated RLS cannot read another business or call internal identity verification', async () => {
    await pg.exec('RESET ROLE; SET ROLE authenticated');
    try {
      await pg.query("SELECT set_config('request.jwt.claim.sub',$1,false)",[owner3]);
      assert.equal((await pg.query('SELECT count(*)::int AS n FROM sync_records')).rows[0].n,0);
      await rejected('SELECT admin_verify_created_identity($1)', ['55555555-5555-4555-8555-555555555555'],'42501');
      await pg.query("SELECT set_config('request.jwt.claim.sub',$1,false)",[owner1]);
      assert.equal((await pg.query('SELECT count(*)::int AS n FROM sync_records')).rows[0].n,1);
    } finally { await pg.exec('RESET ROLE'); }
  });
  if (nativePort) {
    for (const isolation of ['READ COMMITTED','REPEATABLE READ']) {
      for (const operation of ['disable','demote','delete']) {
        await check(`competing ${operation} operations at ${isolation} retain an active owner`, async () => {
          await pg.query("UPDATE staff SET active=true,role='owner' WHERE id=$1 OR id=$2",[owner1,owner2]);
          const first = new pgDriver.Client(nativeOptions), second = new pgDriver.Client(nativeOptions);
          await first.connect(); await second.connect();
          try {
            await first.query('SET ROLE service_role'); await second.query('SET ROLE service_role');
            await first.query(`BEGIN ISOLATION LEVEL ${isolation}`);
            await second.query(`BEGIN ISOLATION LEVEL ${isolation}`);
            // Establish the second connection's snapshot before first commits.
            assert.equal((await second.query('SELECT count(*)::int AS n FROM staff WHERE active AND business_id=$1',[business])).rows[0].n,2);
            const mutation = operation==='disable' ? 'UPDATE staff SET active=false WHERE id=$1'
              : operation==='demote' ? "UPDATE staff SET role='cashier' WHERE id=$1" : 'DELETE FROM staff WHERE id=$1';
            // Reuse the disable mutation for first so every case starts with
            // two fixture rows and leaves one surviving row after rollback.
            await first.query('UPDATE staff SET active=false WHERE id=$1',[owner1]);
            const pid = (await second.query('SELECT pg_backend_pid() AS pid')).rows[0].pid;
            const blocked = second.query(mutation,[owner2]).then(()=>({success:true}),error=>({error}));
            let waiting=false;
            for (let i=0;i<100;i++) {
              waiting=(await pg.query("SELECT wait_event_type='Lock' AS waiting FROM pg_stat_activity WHERE pid=$1",[pid])).rows[0]?.waiting;
              if (waiting) break;
              await new Promise(resolve=>setTimeout(resolve,20));
            }
            assert.equal(waiting,true,'Competing mutation must wait on the shared business boundary');
            await first.query('COMMIT');
            const result=await blocked;
            assert.equal(result.error?.code,isolation==='READ COMMITTED' ? 'P0001' : '40001');
            await second.query('ROLLBACK');
            assert.equal((await pg.query("SELECT count(*)::int AS n FROM staff WHERE business_id=$1 AND active AND role='owner'",[business])).rows[0].n,1);
          } finally {
            await first.query('ROLLBACK').catch(()=>{}); await second.query('ROLLBACK').catch(()=>{});
            await first.end(); await second.end();
          }
        });
      }
    }
  }
  if (process.env.SAAF_SQL_CATALOG_DUMP === '1') {
    assert.ok(connection, 'Catalog comparison uses the native disposable cluster');
    const evidence = new URL('../evidence/', import.meta.url);
    const result = await connection.query(readFileSync(new URL('supabase-catalog-readonly.sql', evidence), 'utf8'));
    const catalog = result.flatMap(part => part.rows ?? []).find(row => row.audit_catalog)?.audit_catalog;
    assert.ok(catalog);
    writeFileSync(new URL('supabase-local-bootstrap-catalog.json', evidence), JSON.stringify(catalog, null, 2) + '\n');
    console.log('Local bootstrap catalog saved for comparison with read-only live snapshots.');
  }
  console.log(`${checks} SQL checks passed. ${nativePort ? 'Concurrent native connections verified.' : 'Single-session runtime; concurrency not verified.'} Deployed Supabase parity and real Auth behavior are not validated.`);
} finally { await pg.close(); }
