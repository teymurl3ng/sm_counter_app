BEGIN READ ONLY; SET LOCAL statement_timeout='10s';
SELECT set_config('request.jwt.claims',jsonb_build_object('role','authenticated','sub',(SELECT auth_user_id::text FROM public.staff WHERE role='owner' AND active AND terminated_at IS NULL AND auth_user_id IS NOT NULL ORDER BY created_at LIMIT 1))::text,true);
SET LOCAL ROLE authenticated;
SELECT jsonb_build_object('role',current_user,'auth_uid_is_set',auth.uid() IS NOT NULL,'eligible_shops',(SELECT count(*) FROM public.sync_staff_shop_ids()),'profile_nonnull',public.my_profile() IS NOT NULL,'profile_role',public.my_profile()->>'role','pull_ok',public.sync_pull(0,1,NULL)->'ok','pull_records',jsonb_array_length(public.sync_pull(0,1,NULL)->'records'),'overview_nonnull',public.admin_overview() IS NOT NULL) AS result;
ROLLBACK;
