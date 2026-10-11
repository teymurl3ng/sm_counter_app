"""Verify saved actual PROD phone acceptance and preserve the previous release snapshot."""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    manifest = json.loads((HERE / 'verification-prod-phone-final.json').read_text(encoding='utf-8'))
    for name, digest in manifest['artifact_and_report_sha256'].items():
        assert sha(ROOT / name) == digest, name
    release = json.loads((ROOT / 'audit/2026-10-10/releases/verification-release-final.json').read_text(encoding='utf-8'))
    for name, digest in release['workspace_sha256'].items():
        if name in manifest['subsequently_appended_root_reports']:
            assert sha(ROOT / name) == manifest['artifact_and_report_sha256'][name], name
        else:
            assert sha(ROOT / name) == digest, name
    first = json.loads((HERE / 'prod-read-check.json').read_text(encoding='utf-8'))
    repeat = json.loads((HERE / 'prod-repeat-check.json').read_text(encoding='utf-8'))
    ui = json.loads((HERE / 'phone-sync-verification.json').read_text(encoding='utf-8'))
    assert first['sync']['rows'] == repeat['sync_rows'] == 36
    assert first['sync']['max_seq'] == repeat['max_seq'] == ui['cursor'] == 107
    assert first['counts']['reconciliation'] == repeat['reconciliation_rows'] == 0
    assert first['sync']['non_objects'] == first['sync']['unknown_tables'] == first['sync']['mismatched_shops'] == 0
    assert not first['authenticated_direct_sync_insert_allowed']
    assert sum(row['changed_after_deployment_baseline'] for row in first['by_shop_table']) == 16
    owner = first['owner_identity'][0]
    assert owner['username'] == 'teimur' and owner['role'] == 'owner' and owner['active'] and not owner['terminated']
    assert owner['last_auth_sign_in_at'].startswith('2026-10-10T14:44:59')
    assert ui['environment'] == 'prod' and ui['identity'] == 'verified' and ui['session'] == 'active'
    assert ui['waiting_to_upload'] == 0 and ui['repeat_quiescent']
    assert 'Push: Drained · Pull: applied=0 updated=16 skipped=0 parked=0 conflicts=0' in ui['initial_text']
    assert 'Push: Drained · Pull: applied=0 updated=0 skipped=0 parked=0 conflicts=0' in ui['repeat_text']
    for filename, expected in [('phone-sync-result.xml', ui['initial_text']), ('phone-sync-repeat-first.xml', ui['repeat_text'])]:
        nodes = list(ET.fromstring((HERE / filename).read_bytes()).iter('node'))
        assert any(n.attrib.get('package') == 'com.saaf.milkshop' for n in nodes)
        assert [n.attrib['text'] for n in nodes if n.attrib.get('text')] == expected
    assert manifest['real_prod_phone_auth_and_sync_verified'] and not manifest['authenticated_prod_viewer_verified']
    print('PASS: saved real release PROD identity/session, initial updated=16, zero upload queue, '
          'quiescent repeat, server cursor/count/integrity, closed direct-write boundary and '
          'unchanged prior release artifacts/source. PROD Viewer login remains pending.')


if __name__ == '__main__':
    main()
