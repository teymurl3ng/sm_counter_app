"""Runs a disposable localhost-only PostgreSQL cluster; never installs a service."""
from pathlib import Path
import os, socket, subprocess, sys, tempfile, zipfile

base=Path(__file__).resolve().parent
runtime=base.parent/'runtime'
binary_root=runtime/'portable'
print('Preparing portable PostgreSQL runtime',flush=True)
with zipfile.ZipFile(runtime/'postgresql.zip') as archive:
    for entry in archive.infolist():
        if entry.filename.startswith(('pgsql/bin/','pgsql/lib/','pgsql/share/')) and not entry.is_dir():
            target=(binary_root/entry.filename).resolve()
            assert target.is_relative_to(binary_root.resolve())
            if not target.exists(): archive.extract(entry,binary_root)
bin_dir=binary_root/'pgsql/bin'
def run(args,**kwargs):
    capture='stdout' not in kwargs
    if capture: kwargs.update(stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    result=subprocess.run([str(x) for x in args],creationflags=subprocess.CREATE_NO_WINDOW,**kwargs)
    if capture:
        if result.stdout: print(result.stdout,end='',flush=True)
        if result.stderr: print(result.stderr,end='',file=sys.stderr,flush=True)
    result.check_returncode()
    return result
with tempfile.TemporaryDirectory(prefix='saaf-sql-',dir=runtime) as cluster:
    assert Path(cluster).resolve().is_relative_to(runtime.resolve())
    data=Path(cluster)/'data'; log=Path(cluster)/'postgres.log'
    # Select a free local test port. Startup fails safely if another process wins it.
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0)); port=sock.getsockname()[1]
    print('Initializing disposable localhost cluster',flush=True)
    run([bin_dir/'initdb.exe','-D',data,'-U','saaf_offline','-A','trust','--no-locale','-E','UTF8'],stdout=subprocess.DEVNULL)
    try:
        run([bin_dir/'pg_ctl.exe','-D',data,'-l',log,'-o',f'-h 127.0.0.1 -p {port}','-w','start'],stdout=subprocess.DEVNULL)
        run([bin_dir/'postgres.exe','--version'])
        print('Executing bootstrap and concurrent connection fixtures',flush=True)
        env=dict(os.environ); env['SAAF_SQL_TEST_PORT']=str(port)
        script_name = os.environ.get('SAAF_SQL_TEST_SCRIPT', 'check.mjs')
        assert script_name in ('check.mjs', 'live-boundary.mjs')
        run(['node',base/script_name],env=env)
    finally:
        run([bin_dir/'pg_ctl.exe','-D',data,'-m','fast','-w','stop'],stdout=subprocess.DEVNULL)
        print('Disposable cluster stopped',flush=True)
