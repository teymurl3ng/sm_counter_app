"""Bounded acceptance against the running local Viewer and isolated DEV shop.

Credentials remain in the owner's private temporary file; no tokens are logged.
Creates one disposable admin, then disables it. Does not touch PROD or old users.
"""
import json
import pathlib
import urllib.request
import urllib.error
import http.cookiejar
from datetime import datetime, timezone

HERE = pathlib.Path(__file__).resolve().parent
PRIVATE = pathlib.Path('C:/Users/Teymur/AppData/Local/Temp/saaf-accept-owner-20261010.json')
STAFF_PRIVATE = pathlib.Path('C:/Users/Teymur/AppData/Local/Temp/saaf-accept-staff-20261010.json')
VIEWER = 'http://127.0.0.1:3100'
DEV = 'https://tghzmofkkdsaasvnixps.supabase.co'
SHOP = 'b0d97cf0-8304-4ef2-b8fc-3c3367230d94'
report = {'started_at': datetime.now(timezone.utc).isoformat(), 'environment': 'dev', 'checks': []}

def call(client, url, body=None, headers=None):
    h = {'Content-Type': 'application/json', 'Origin': VIEWER}
    h.update(headers or {})
    req = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(), headers=h)
    try:
        with client.open(req, timeout=30) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read())

def viewer_client():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

def check(name, condition, **evidence):
    report['checks'].append({'name': name, 'passed': bool(condition), **evidence})
    if not condition:
        raise AssertionError(name)

def login(client, username, credential):
    status, data = call(client, VIEWER + '/api/auth/login', {'username': username, 'credential': credential})
    return status == 200 and data.get('ok') is True

try:
    owner = json.loads(PRIVATE.read_text())
    env = {}
    for line in pathlib.Path('C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer/.env.local').read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            k, v = line.split('=', 1)
            env[k.strip()] = v.strip().strip('"').strip("'")
    assert env['ACCOUNT_SERVER_URL'].rstrip('/') == DEV and env['ACCOUNT_SERVER_ENV'] == 'dev'
    auth_headers = {'apikey': env['ACCOUNT_SERVER_PUBLISHABLE_KEY']}
    direct = urllib.request.build_opener()
    status, other_session = call(direct, DEV + '/auth/v1/token?grant_type=password',
                                {'email': owner['username'] + '@saafmilk.local', 'password': owner['server_credential']}, auth_headers)
    check('independent_real_auth_session', status == 200 and bool(other_session.get('refresh_token')), http_status=status)

    a, b = viewer_client(), viewer_client()
    check('owner_viewer_login_a', login(a, owner['username'], owner['server_credential']))
    check('owner_viewer_login_b', login(b, owner['username'], owner['server_credential']))
    status, data = call(a, VIEWER + '/api/auth/logout', {})
    check('viewer_logout_a', status == 200 and data.get('ok') is True, http_status=status)
    status, data = call(a, VIEWER + '/api/health')
    check('logged_out_viewer_denied', status == 401, http_status=status)
    status, data = call(b, VIEWER + '/api/health')
    check('other_viewer_session_survives', status == 200 and data.get('ok') is True, http_status=status)
    status, refreshed = call(direct, DEV + '/auth/v1/token?grant_type=refresh_token',
                             {'refresh_token': other_session['refresh_token']}, auth_headers)
    check('independent_refresh_survives_viewer_logout', status == 200 and bool(refreshed.get('access_token')), http_status=status)

    if STAFF_PRIVATE.exists():
        staff = json.loads(STAFF_PRIVATE.read_text())
        assert staff['username'] == 'accept.staff.1010' and staff['environment'] == 'dev'
        assert staff['staff_id'] == 'fc6eee75-2c1a-4f66-8958-8a5d00cddab4'
        report['fixture_reused'] = True
        status, data = call(b, VIEWER + '/api/admin/rpc', {'rpc': 'admin_enable_staff', 'payload': {'p_staff_id': staff['staff_id']}})
        check('owner_enables_existing_disposable_fixture', status == 200 and data.get('ok') is True, http_status=status)
    else:
        status, staff = call(b, VIEWER + '/api/admin/create-staff',
                             {'username': 'accept.staff.1010', 'display_name': 'Acceptance Disposable Admin', 'role': 'admin', 'shop_ids': [SHOP]})
        if status == 200 and staff.get('staff_id') and staff.get('credential'):
            STAFF_PRIVATE.write_text(json.dumps({'username': 'accept.staff.1010', 'credential': staff['credential'], 'staff_id': staff['staff_id'], 'environment': 'dev'}))
        check('create_disposable_staff_real_route', status == 200 and staff.get('ok') is True and bool(staff.get('staff_id')), http_status=status, staff_id=staff.get('staff_id'), error_code=(staff.get('error') or {}).get('code'))
    s = viewer_client()
    check('disposable_staff_real_login', login(s, 'accept.staff.1010', staff['credential']))
    s_mut = viewer_client()
    check('independent_staff_session_for_mutation_check', login(s_mut, 'accept.staff.1010', staff['credential']))
    status, data = call(s, VIEWER + '/api/overview')
    check('active_staff_protected_read', status == 200 and data.get('ok') is True, http_status=status)
    status, data = call(b, VIEWER + '/api/admin/rpc', {'rpc': 'admin_disable_staff', 'payload': {'p_staff_id': staff['staff_id']}})
    check('owner_disables_disposable_staff', status == 200 and data.get('ok') is True, http_status=status, error_code=(data.get('error') or {}).get('code'))
    status, data = call(s, VIEWER + '/api/overview')
    check('disabled_existing_session_protected_read_denied', status in (401, 403) and data.get('ok') is False, http_status=status, error_code=(data.get('error') or {}).get('code'))
    status, data = call(s_mut, VIEWER + '/api/admin/rpc', {'rpc': 'admin_disable_staff', 'payload': {'p_staff_id': staff['staff_id']}})
    check('disabled_session_mutation_denied', status in (401, 403) and data.get('ok') is False, http_status=status, error_code=(data.get('error') or {}).get('code'))
    status, data = call(viewer_client(), VIEWER + '/api/auth/login', {'username': 'accept.staff.1010', 'credential': staff['credential']})
    check('disabled_staff_new_login_denied', status in (401, 403) and data.get('ok') is False, http_status=status, error_code=(data.get('error') or {}).get('code'))
    status, data = call(b, VIEWER + '/api/admin/rpc', {'rpc': 'admin_disable_staff', 'payload': {'p_staff_id': '847cab72-60ae-46b1-b251-1445da669a4a'}})
    check('last_active_owner_disable_refused', status == 409 and (data.get('error') or {}).get('code') == 'LAST_OWNER', http_status=status, error_code=(data.get('error') or {}).get('code'))
    status, data = call(b, VIEWER + '/api/admin/reconcile-auth', {})
    check('real_reconciliation_endpoint_quiescent', status == 200 and data.get('ok') is True and data.get('processed') == 0 and data.get('resolved') == 0, http_status=status, processed=data.get('processed'), resolved=data.get('resolved'))
    report['outcome'] = 'pass'
except Exception as exc:
    report['outcome'] = 'incomplete_or_failed'
    report['failure_type'] = type(exc).__name__
    # No provider bodies, keys, passwords or tokens in failure logs.
    raise
finally:
    report['finished_at'] = datetime.now(timezone.utc).isoformat()
    (HERE / 'real-dev-workflows.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
