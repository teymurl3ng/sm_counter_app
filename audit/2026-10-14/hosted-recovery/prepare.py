from pathlib import Path
import sys, subprocess, hashlib, json, re
folder=Path(__file__).resolve().parent
root=folder.parents[2]
server=root/'audit/2026-10-14/finish-engineering/server-checkout'
sha=subprocess.check_output(['git','rev-parse','HEAD'],cwd=server,text=True).strip()
assert sha=='c21b9837584725d95e8d5de63c00974fa92e30df'
assert not subprocess.check_output(['git','status','--porcelain','--untracked-files=no'],cwd=server,text=True).strip()
sys.path.insert(0,str(server))
import recover_identity
fixture=json.loads((root/'audit/2026-10-13/identity-recovery-proposal/synthetic-fixture.json').read_text())
recover_identity.validate_export(fixture)
files={}
def expand(path):
    assert path.resolve().is_relative_to(server.resolve())
    raw=path.read_bytes()
    files[str(path.relative_to(server))]=hashlib.sha256(raw).hexdigest()
    result=[]
    for line in raw.decode('utf-8').replace('\r\n','\n').splitlines(keepends=True):
        directive=re.match(r'\s*\\ir\s+(.+?)\s*$',line)
        if directive:
            result.append(expand(path.parent/directive.group(1)))
        elif line.strip().startswith('\\set ON_ERROR_STOP'):
            continue
        else:
            assert not line.lstrip().startswith('\\'), 'Unsupported psql directive'
            result.append(line)
    return ''.join(result)+'\n'
sql=expand(server/'bootstrap_recovery.sql')
(folder/'bootstrap-expanded.sql').write_text(sql,encoding='utf-8',newline='\n')
record={'sourceCommit':sha,'sourceFiles':files,'expandedSqlSha256':hashlib.sha256(sql.encode()).hexdigest(),'sqlChars':len(sql),
 'fixtureSha256':hashlib.sha256(json.dumps(fixture,sort_keys=True).encode()).hexdigest(),
 'fixtureCounts':{k:len(fixture[k]) for k in ['businesses','shops','staff','staff_shops','audit_log']}}
record['expectedFunctionBodies']={name:hashlib.md5(body.encode()).hexdigest() for filename in
 ['20261012194632_protected_credential_reset_guard.sql','20261013075706_safe_empty_identity_restore.sql']
 for name,body in re.findall(r'CREATE FUNCTION\s+([\w.]+).*?\bAS \$\$(.*?)\$\$;',
 (server/'supabase/migrations'/filename).read_text(encoding='utf-8'),re.S|re.I)}
(folder/'prepared.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps({'sourceCommit':sha,'sqlBytes':len(sql.encode()),'includes':len(files),'fixtureCounts':record['fixtureCounts']}))
