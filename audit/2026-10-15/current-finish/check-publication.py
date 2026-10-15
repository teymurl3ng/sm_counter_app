from pathlib import Path
import hashlib, json, re
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
files=[p for p in OUT.rglob('*') if p.is_file() and p.suffix in ('.md','.py','.java','.json')]
files += [ROOT/'REMAINING_AND_LATER_2026-10-15.md']
patterns=[re.compile(r'sb_secret_[A-Za-z0-9_-]{12,}'),re.compile(r'eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}')]
for path in files:
    text=path.read_text(encoding='utf-8')
    assert not any(p.search(text) for p in patterns),'Potential credential material; publication stopped: '+path.name
    if path.suffix=='.json':
        data=json.loads(text)
        def inspect(value):
            if isinstance(value,dict):
                for key,item in value.items():
                    assert key not in ('password','credential','access_token','refresh_token','api_key','secret'), 'Secret-bearing evidence key: '+path.name
                    inspect(item)
            elif isinstance(value,list):
                for item in value: inspect(item)
        inspect(data)
freeze=json.loads((OUT/'freeze.json').read_text())
assert len(freeze['affectedLatestPasses'])==17
for name,expected in freeze['testSourceHashes'].items():
    assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==expected
record=json.loads((OUT/'ordinary-app-observation.json').read_text())
record.update({'visualReview':'Ordinary app Welcome opened; Continue showed the existing Owner account and configured Quick PIN/full-passphrase options.',
    'accountPresenceObserved':True,'privateLoginObserved':False,'businessRecordsCompared':False,
    'hierarchyLimitation':'UI hierarchy exposed no useful text; two local ignored screenshots were reviewed instead. No raw hierarchy retained.'})
(OUT/'ordinary-app-observation.json').write_text(json.dumps(record,indent=2)+'\n')
print('Publication check passed: no raw secrets in selected source/metadata; final17-check hashes match.')
