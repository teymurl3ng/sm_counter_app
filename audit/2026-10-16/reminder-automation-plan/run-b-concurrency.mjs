// Run in functions.exec with tools/text supplied. DEV-only synthetic SQL; no secrets or customer sending.
const project_id='tghzmofkkdsaasvnixps';
const uuid=()=> 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,c=>{const r=Math.floor(Math.random()*16);return (c==='x'?r:(r&3)|8).toString(16)});
const quote=x=>"'"+String(x).replaceAll("'","''")+"'";
async function sql(query) {
  const r=await tools.mcp__codex_apps__supabase_execute_sql({project_id,query});
  if(r.isError) throw Error(JSON.stringify(r.content).slice(0,1200));
  const body=JSON.parse(r.content.find(c=>c.type==='text').text);
  if(body.error) throw Error(JSON.stringify(body.error).slice(0,1200));
  const match=body.result.match(/\n(\[[\s\S]*\])\n<\/untrusted-data-/);
  if(!match)throw Error('Unexpected SQL response shape');
  return JSON.parse(match[1]);
}
const tables=[['public','businesses','id'],['public','shops','id'],['public','staff','id'],['public','sync_records','server_seq'],['credit_private','records','server_seq'],['credit_private','contracts','shop_id']];
const fingerprintQuery='SELECT jsonb_build_object('+tables.map(([schema,table,key])=>quote(table)+`,(SELECT jsonb_build_object('count',count(*),'hash',md5(coalesce(string_agg(to_jsonb(t)::text,'' ORDER BY ${key}),''))) FROM ${schema}.${table} t)`).concat(["'auth_user_ids',(SELECT jsonb_build_object('count',count(*),'hash',md5(coalesce(string_agg(id::text,'' ORDER BY id),''))) FROM auth.users)"]).join(',')+') AS fingerprint';
async function fingerprint(){return (await sql(fingerprintQuery))[0].fingerprint;}
const before=await fingerprint();
// Reuse the established DEV fixture pattern: append disposable Owner identities to a DEV business
// with other active owners. Removing the fixture never needs to suspend the last-owner guard.
const business=(await sql("select business_id from public.staff where role='owner' and active and terminated_at is null group by business_id having count(*)>=2 order by business_id limit 1"))[0]?.business_id;
if(!business)throw Error('No suitable DEV fixture business');
const shop=uuid(),authA=uuid(),authB=uuid(),name='qa_followup_'+uuid().replaceAll('-','').slice(0,12);
const checks=[];let failure=null;let after=null;
const prefix=actor=>`BEGIN; SET LOCAL ROLE authenticated; SELECT set_config('request.jwt.claim.sub',${quote(actor)},true);`;
const snapshot=async()=> (await sql(prefix(authA)+`SELECT public.followup_snapshot_v1(${quote(shop)}) AS snapshot; COMMIT;`))[0].snapshot;
async function op(kind,extra={}) {
  const s=await snapshot();
  const value={kind,...extra,expected_heads:s.heads};
  if(kind!=='DIGEST'){value.customer_uid='qa';value.expected_customer_revision=s.customers.find(x=>x.customer_uid==='qa')?.revision??null;}
  return value;
}
async function command(actor,device,operation,id=uuid(),hold=false) {
  const call=`public.followup_command_v1(${quote(shop)},${quote(device)},${quote(id)},${quote(JSON.stringify(operation))}::jsonb)`;
  const query=hold?`WITH result AS MATERIALIZED (SELECT ${call} AS response), hold AS MATERIALIZED (SELECT pg_sleep(2) FROM result) SELECT result.response,pg_backend_pid() AS pid,statement_timestamp() AS began,clock_timestamp() AS ended FROM result CROSS JOIN hold;`:`SELECT ${call} AS response;`;
  return (await sql(prefix(actor)+query+' COMMIT;'))[0];
}
async function parallel(label,requests,grant) {
  const results=await Promise.all(requests.map(args=>command(...args,true)));
  const overlapping=results[0].pid!==results[1].pid && Math.max(...results.map(x=>Date.parse(x.began)))<Math.min(...results.map(x=>Date.parse(x.ended)));
  const count=results.filter(x=>x.response[grant]===true).length;
  if(!overlapping||count!==1)throw Error('Concurrency assertion failed: '+label+' '+JSON.stringify(results));
  checks.push({label,overlappingDistinctBackends:true,grantCount:count,results});
  return results;
}
try {
  let setup=`BEGIN; INSERT INTO public.shops(id,business_id,name,shop_timezone) VALUES(${quote(shop)},${quote(business)},${quote(name)},'Asia/Karachi'); INSERT INTO auth.users(id) VALUES(${quote(authA)}),(${quote(authB)}); INSERT INTO public.staff(auth_user_id,business_id,username,display_name,role) VALUES`;
  setup+=[authA,authB].map(a=>`(${quote(a)},${quote(business)},${quote('qa_f_'+a.slice(0,20))},'Follow-up QA','owner')`).join(',')+';';
  setup+=`INSERT INTO public.sync_records(business_id,shop_id,table_name,row_uid,payload,origin_device) VALUES(${quote(business)},${quote(shop)},'accounts','qa','{"category":"Customer"}'::jsonb,'fixture'); SET LOCAL ROLE authenticated; SELECT set_config('request.jwt.claim.sub',${quote(authA)},true); SELECT public.credit_contract_enable_v1(${quote(shop)},true);`;
  setup+=`WITH q AS (SELECT date_trunc('minute',(clock_timestamp() AT TIME ZONE 'Asia/Karachi')+interval '2 hours')::time AS t), w AS (SELECT CASE WHEN t='00:00'::time THEN '01:00'::time ELSE t END AS t FROM q) SELECT public.followup_command_v1(${quote(shop)},'one',gen_random_uuid(),jsonb_build_object('kind','POLICY','expected_revision',NULL,'make_primary',true,'phones_updated',true,'value',jsonb_build_object('enabled',true,'weekday',7,'repeat_days',7,'preferred_time','00:00','quiet_start',to_char(t,'HH24:MI'),'quiet_end',to_char(t+interval '1 minute','HH24:MI')))) AS configured FROM w; COMMIT;`;
  const configured=(await sql(setup))[0].configured;
  if(configured.ok!==true)throw Error('Fixture policy refused: '+JSON.stringify(configured));
  const attemptA=uuid(),attemptB=uuid(),acquire=await op('ACQUIRE',{attempt_uid:attemptA,cycle:'qa-cycle'});
  const acquired=await parallel('two installations acquire only one customer',[[authA,'one',acquire,uuid()],[authB,'two',{...acquire,attempt_uid:attemptB},uuid()]],'ok');
  const winner=acquired[0].response.ok===true?[authA,'one',attemptA]:[authB,'two',attemptB];
  const start=await op('START',{attempt_uid:winner[2],channel:'SMS'}),startId=uuid();
  const started=await parallel('concurrent retry authorizes only one external launch',[[winner[0],winner[1],start,startId],[winner[0],winner[1],start,startId]],'launch_authorized');
  if(!started.some(x=>x.response.replayed===true))throw Error('Duplicate START did not recover as replay');
  const resolved=(await command(authA,'one',await op('RESOLVE',{attempt_uid:winner[2],outcome:'RETRY_UNCERTAIN',acknowledge_uncertainty:true}))).response;
  if(resolved.ok!==true)throw Error('Synthetic uncertain attempt did not resolve');
  const digest=await op('DIGEST',{has_work:true}),digestId=uuid();
  const digested=await parallel('concurrent retry authorizes only one notification',[[authA,'one',digest,digestId],[authA,'one',digest,digestId]],'notify_authorized');
  if(!digested.some(x=>x.response.replayed===true))throw Error('Duplicate digest did not recover as replay');
  if((await command(authA,'one',await op('DIGEST',{has_work:true}))).response.notify_authorized!==false)throw Error('Same-date notification repeated');
} catch(error) {failure=String(error);}
finally {
  try {
    const owned=`EXISTS(SELECT 1 FROM public.shops WHERE id=${quote(shop)} AND business_id=${quote(business)} AND name=${quote(name)})`;
    const cleanup=`BEGIN; CREATE TEMP TABLE f_delete_auth AS SELECT auth_user_id FROM public.staff WHERE business_id=${quote(business)} AND auth_user_id IN(${quote(authA)},${quote(authB)}) AND username LIKE 'qa_f_%'; DELETE FROM public.sync_records WHERE shop_id=${quote(shop)} AND ${owned}; DELETE FROM public.staff_shops WHERE shop_id=${quote(shop)} AND ${owned}; DELETE FROM public.shops WHERE id=${quote(shop)} AND business_id=${quote(business)} AND name=${quote(name)}; DELETE FROM public.staff WHERE business_id=${quote(business)} AND auth_user_id IN(SELECT auth_user_id FROM f_delete_auth); DELETE FROM auth.users WHERE id IN(SELECT auth_user_id FROM f_delete_auth); SELECT true AS removed; COMMIT;`;
    await sql(cleanup);after=await fingerprint();
    if(JSON.stringify(after)!==JSON.stringify(before))failure=(failure??'')+' Existing-data fingerprint mismatch';
  }catch(error){failure=(failure??'')+' Cleanup/check failed: '+String(error);}
  const report={project:project_id,business,shop,checks,failure,before,after,existingDataIdentical:JSON.stringify(before)===JSON.stringify(after),actualSending:false};
  store('bConcurrencyMcp',report);
  text({checks:checks.length,failure,existingDataIdentical:report.existingDataIdentical});
}
