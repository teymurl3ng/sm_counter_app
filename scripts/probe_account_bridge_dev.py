#!/usr/bin/env python3
"""Account Bridge DEV contract probes (non-destructive).

Runs probes P0-P8 from ACCOUNT_BRIDGE_DEV_PROBE_PLAN.md against the DEV
Supabase project and prints redacted PASS/FAIL evidence only. Never prints
tokens, credentials, or keys. Reads secrets at runtime from the local
'SM Admin Server' working copy; nothing is written to disk.

Usage:
  python probe_account_bridge_dev.py <server_dir> [credentials_file]

  <server_dir> must contain .env.dev. The credentials file defaults to
  '<server_dir>/saaf server admin credentials.txt'; any file with the same
  two-line 'Username:'/'Server credential:' format works (e.g. a dedicated
  DEV probe fixture credential).
"""

import json
import re
import sys
import time
import urllib.error
import urllib.request


def load_env(path):
    env = {}
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, _, value = line.partition("=")
                env[key.strip()] = value.strip()
    return env


def load_credential(path):
    username = None
    credential = None
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            if line.lower().startswith("username:"):
                username = line.partition(":")[2].strip()
            elif "credential" in line.lower() and ":" in line:
                credential = re.sub(r"\D", "", line.partition(":")[2])
    return username, credential


def request(method, url, headers, body=None, timeout=20):
    data = json.dumps(body).encode() if body is not None else b"{}"
    req = urllib.request.Request(url, data=data, method=method)
    for key, value in headers.items():
        req.add_header(key, value)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as err:
        return err.code, err.read().decode("utf-8", "replace")
    except Exception as err:  # network layer — classification input, not a crash
        return None, type(err).__name__


def redact_token(token):
    if not token:
        return "<absent>"
    parts = token.split(".")
    return f"<jwt:{len(parts)}-parts,{len(token)}-chars>" if len(parts) == 3 else f"<opaque:{len(token)}-chars>"


def main():
    if len(sys.argv) not in (2, 3):
        print(__doc__)
        sys.exit(2)
    server_dir = sys.argv[1].rstrip("/\\")
    env = load_env(f"{server_dir}/.env.dev")
    base = env["SUPABASE_URL"].rstrip("/")
    publishable = env["SUPABASE_PUBLISHABLE_KEY"]
    credentials_file = (sys.argv[2] if len(sys.argv) == 3
                        else f"{server_dir}/saaf server admin credentials.txt")
    username, credential = load_credential(credentials_file)
    if not username or not credential or len(credential) != 15:
        print("SETUP FAIL: username/15-digit credential not found or malformed")
        sys.exit(1)
    key_shape = "legacy-jwt" if re.fullmatch(r"[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+", publishable) else "sb_publishable"

    results = []

    def record(pid, expectation, ok, evidence):
        results.append((pid, "PASS" if ok else "FAIL", expectation, evidence))
        print(f"{pid}: {'PASS' if ok else 'FAIL'} — {evidence}")

    def hdrs(bearer=None, apikey=True):
        h = {}
        if apikey:
            h["apikey"] = publishable
        if bearer:
            h["Authorization"] = f"Bearer {bearer}"
        return h

    t0 = time.time()
    # P0 health
    status, body = request("POST", f"{base}/rest/v1/rpc/ping", hdrs(publishable))
    p0_ok = status == 200 and "T" in body
    record("P0", "200 timestamptz via anon publishable bearer", p0_ok,
           f"status={status} body_shape={'timestamptz' if 'T' in body else body[:40]!r}")

    # P1 valid sign-in
    status, body = request("POST", f"{base}/auth/v1/token?grant_type=password",
                           hdrs(publishable), {"email": f"{username}@saafmilk.local", "password": credential})
    access = refresh = None
    if status == 200:
        try:
            parsed = json.loads(body)
            access = parsed.get("access_token")
            refresh = parsed.get("refresh_token")
        except json.JSONDecodeError:
            pass
    p1_ok = status == 200 and access and refresh
    record("P1", "200 with access_token+refresh_token", p1_ok,
           f"status={status} access={redact_token(access)} refresh={'<present>' if refresh else '<absent>'}")

    # P2 profile with user token
    status, body = request("POST", f"{base}/rest/v1/rpc/my_profile", hdrs(access))
    expected_fields = {"staff_id", "username", "display_name", "role", "active", "terminated_at", "business_id", "shops"}
    p2_fields = set()
    p2_evidence = f"status={status}"
    if status == 200 and body.strip():
        try:
            profile = json.loads(body)
            if isinstance(profile, dict):
                p2_fields = expected_fields & set(profile)
                shops = profile.get("shops")
                p2_evidence += (f" fields={len(p2_fields)}/{len(expected_fields)}"
                                f" role={profile.get('role')!r} active={profile.get('active')!r}"
                                f" shops={'array:' + str(len(shops)) if isinstance(shops, list) else type(shops).__name__}")
        except json.JSONDecodeError:
            p2_evidence += " body=non-json"
    else:
        p2_evidence += f" body_shape={'null' if body.strip() in ('null','') else body[:40]!r}"
    record("P2", "200 with frozen my_profile field set", status == 200 and p2_fields == expected_fields, p2_evidence)

    # P3 invalid credentials
    status, body = request("POST", f"{base}/auth/v1/token?grant_type=password",
                           hdrs(publishable), {"email": f"{username}@saafmilk.local", "password": "000000000000001"})
    code = ""
    try:
        code = (json.loads(body).get("error_code") or json.loads(body).get("msg") or "")
        if isinstance(code, str) and len(code) > 40:
            code = code[:40]
    except json.JSONDecodeError:
        code = "<non-json>"
    record("P3", "400 with error_code invalid_credentials", status == 400 and code == "invalid_credentials",
           f"status={status} error_code={code!r}")

    # P4 publishable key as bearer on authenticated RPC
    status, body = request("POST", f"{base}/rest/v1/rpc/my_profile", hdrs(publishable))
    record("P4", "4xx rejection (publishable is not a user JWT)", status is not None and 400 <= status < 500,
           f"status={status}")

    # P5 missing apikey (valid user bearer)
    status, body = request("POST", f"{base}/rest/v1/rpc/my_profile", hdrs(access, apikey=False))
    record("P5", "401 without apikey header", status == 401, f"status={status}")

    # P6 token refresh
    status, body = request("POST", f"{base}/auth/v1/token?grant_type=refresh_token",
                           hdrs(publishable), {"refresh_token": refresh})
    new_access = None
    if status == 200:
        try:
            new_access = json.loads(body).get("access_token")
        except json.JSONDecodeError:
            pass
    record("P6", "200 with a distinct new access token", status == 200 and new_access and new_access != access,
           f"status={status} new_access={redact_token(new_access)} differs={new_access != access if new_access else 'n/a'}")

    # P7 logout, then prove the token is rejected
    status, _ = request("POST", f"{base}/auth/v1/logout", hdrs(new_access or access))
    time.sleep(1)
    status_after, _ = request("POST", f"{base}/rest/v1/rpc/my_profile", hdrs(new_access or access))
    record("P7", "204 logout and subsequent 401 on token reuse",
           status == 204 and status_after == 401, f"logout_status={status} reuse_status={status_after}")

    # P8 malformed sign-in body
    status, body = request("POST", f"{base}/auth/v1/token?grant_type=password", hdrs(publishable), {})
    code = ""
    try:
        parsed = json.loads(body)
        code = parsed.get("error_code") or parsed.get("msg") or ""
        if isinstance(code, str) and len(code) > 40:
            code = code[:40]
    except json.JSONDecodeError:
        code = "<non-json>"
    record("P8", "400 validation error that is NOT invalid_credentials",
           status == 400 and code != "invalid_credentials", f"status={status} error_code={code!r}")

    print(f"\nenvironment=DEV key_type={key_shape} elapsed={time.time() - t0:.1f}s")
    print(f"summary: {sum(1 for r in results if r[1] == 'PASS')}/{len(results)} PASS")
    print("P9/P10 require manual fixtures (see plan §2) — not runnable here.")


if __name__ == "__main__":
    main()
