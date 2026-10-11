"""Offline pagination, scoping and bootstrap checks. Never loads credential files."""
import importlib.util
import tempfile
import unittest
import subprocess
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

ROOT=Path('C:/Users/Teymur/Desktop/saaf-anchor-tmp')
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec)
    import sys
    sys.modules[name]=mod
    spec.loader.exec_module(mod)
    return mod
snapshot=module('identity_snapshot',ROOT/'scripts/identity_snapshot.py')
admin=module('repair_admin',ROOT/'admin.py')

class ServerOfflineTest(unittest.TestCase):
    def test_rest_respects_server_cap_and_collects_every_row(self):
        source=[{'id':str(i)} for i in range(1007)]
        offsets=[]
        def fetch(path):
            query=parse_qs(urlsplit(path).query)
            start=int(query['offset'][0]); offsets.append(start)
            self.assertEqual(['id.asc'],query['order'])
            return source[start:start+73]
        self.assertEqual(source,snapshot.collect_rest(fetch,'staff'))
        self.assertEqual(1007,offsets[-1])

    def test_auth_collects_more_than_first_hundred(self):
        source=[{'id':str(i)} for i in range(237)]
        def fetch(path):
            page=int(parse_qs(urlsplit(path).query)['page'][0])
            return {'users':source[(page-1)*100:page*100]}
        self.assertEqual(source,snapshot.collect_auth(fetch)['users'])

    def test_duplicate_or_invalid_page_is_rejected(self):
        with self.assertRaises(ValueError): snapshot.collect_rest(lambda _: [{'id':'same'}],'shops')
        with self.assertRaises(ValueError): snapshot.collect_auth(lambda _: {'error':'denied'})

    def test_cli_count_is_paginated_and_business_scoped(self):
        client=object.__new__(admin.AdminClient)
        seen=[]
        def select(table,query):
            seen.append(query)
            return [{'id':'owner'}] if query['offset']=='0' else []
        client.rest_select=select
        self.assertEqual(1,client.count_active_owners('business-A'))
        self.assertTrue(all(q['business_id']=='eq.business-A' for q in seen))

    def test_bootstrap_order_and_migration_copies(self):
        sql=(ROOT/'bootstrap.sql').read_text(encoding='utf-8')
        files=[line.removeprefix('\\ir ') for line in sql.splitlines() if line.startswith('\\ir ')]
        self.assertEqual('migration.sql',files[0]); self.assertEqual('migration_sync_fresh.sql',files[-1])
        self.assertTrue(all((ROOT/file).is_file() for file in files))
        self.assertEqual((ROOT/'migration_c1_6_integrity.sql').read_bytes(),
            (ROOT/'supabase/migrations/20261010120000_c1_6_integrity.sql').read_bytes())
        sync=(ROOT/'migration_sync_fresh.sql').read_text()
        self.assertNotIn('AS $function$;',sync)
        self.assertEqual(8,sync.count('$function$'))
        self.assertIn('ENABLE ROW LEVEL SECURITY',sync)

    def test_workflow_switches_branch_before_snapshot_and_stages_only_snapshot(self):
        workflow=(ROOT/'.github/workflows/identity-dr.yml').read_text()
        self.assertLess(workflow.index('git checkout --detach'),workflow.index('run: python3'))
        self.assertNotIn('.snapshot_dir',workflow)
        self.assertIn('git add -- "$SNAPSHOT_DIR"',workflow)
        self.assertIn('cancel-in-progress: false',workflow)

    def test_repeated_backup_branch_runs_preserve_old_snapshots(self):
        # Exercise the actual Git branch/push sequence with a local bare remote.
        # No Auth/REST access and no project repository mutations.
        with tempfile.TemporaryDirectory(prefix='saaf-dr-git-') as tmp:
            root=Path(tmp); remote=root/'remote.git'; work=root/'work'
            def git(*args,cwd=None):
                return subprocess.run(['git',*args],cwd=cwd,check=True,capture_output=True,text=True).stdout
            git('init','--bare',str(remote)); git('init',str(work))
            git('config','user.name','Offline test',cwd=work)
            git('config','user.email','offline@example.invalid',cwd=work)
            git('remote','add','origin',str(remote),cwd=work)
            (work/'source.txt').write_text('source')
            git('add','source.txt',cwd=work); git('commit','-m','Source',cwd=work)
            source=git('rev-parse','HEAD',cwd=work).strip()
            git('checkout','--orphan','identity-backups',cwd=work); git('rm','-rf','.',cwd=work)
            for index in range(3):
                if index:
                    git('checkout','--detach',source,cwd=work)
                    git('fetch','origin','identity-backups',cwd=work)
                    git('checkout','--detach','FETCH_HEAD',cwd=work)
                stamp=f'snapshot-{index}'
                folder=work/stamp; folder.mkdir(); (folder/'manifest.json').write_text('{}')
                git('add','--',stamp,cwd=work); git('commit','-m',stamp,cwd=work)
                git('push','origin','HEAD:refs/heads/identity-backups',cwd=work)
            tracked=git('ls-tree','-r','--name-only','HEAD',cwd=work).splitlines()
            self.assertEqual([f'snapshot-{i}/manifest.json' for i in range(3)],tracked)
            self.assertNotIn('.snapshot_dir',tracked)

if __name__=='__main__': unittest.main(verbosity=2)
