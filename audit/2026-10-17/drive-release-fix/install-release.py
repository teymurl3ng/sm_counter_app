"""Update only the authorized existing ordinary installations with the frozen repair."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
ADB = "C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe"
PACKAGE = "com.saaf.milkshop"
m = json.loads((OUT / "RELEASE_MANIFEST.json").read_text())
assert (m["package"], m["versionName"], m["versionCode"]) == (PACKAGE, "0.6.1", 10)
assert m["googleWebClientIdPresent"] and m["googleReleaseRegistrationVerified"]
assert m["certificateSha256"] == "9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1"
apk = OUT / m["apk"]
assert hashlib.sha256(apk.read_bytes()).hexdigest() == m["apkSha256"]
assert all(hashlib.sha256((ROOT / p).read_bytes()).hexdigest() == h for p, h in m["sourceSha256"].items())

def adb(serial, *args):
    r = subprocess.run([ADB, "-s", serial, *args], capture_output=True, text=True,
                       encoding="utf-8", errors="replace", timeout=180)
    assert r.returncode == 0, "ADB operation failed: " + args[0]
    return r.stdout

def info(serial):
    raw = adb(serial, "shell", "dumpsys", "package", PACKAGE)
    return {k: re.findall(pattern, raw) for k, pattern in {
        "versionName": r"versionName=(.*)", "versionCode": r"versionCode=(\d+)",
        "uid": r"(?:userId|appId)=(\d+)", "firstInstall": r"firstInstallTime=(.*)",
    }.items()}

available = [line.split()[0] for line in subprocess.check_output([ADB, "devices"], text=True).splitlines()[1:]
             if line.strip().endswith("device")]
targets = {
    "A06": next((s for s in available if s == "R8VXA02SNKH"), None),
    "S24": next((s for s in available if s == "RFCX116CYYX" or s.startswith("adb-RFCX116CYYX-")), None),
}
requested = sys.argv[1:]
assert requested and all(name in targets for name in requested), "Name each intended phone explicitly"
p = OUT / "RELEASE_INSTALLS.json"
records = json.loads(p.read_text()) if p.exists() else []
for name in requested:
    serial = targets[name]
    assert serial, name + " is not connected"
    before = info(serial)
    assert before["uid"] and before["firstInstall"] and before["versionCode"] in (["9"], ["10"])
    if before["versionCode"] == ["10"]:
        installed = adb(serial, "shell", "pm", "path", "--user", "0", PACKAGE).splitlines()[0].removeprefix("package:")
        assert adb(serial, "shell", "sha256sum", installed).split()[0] == m["apkSha256"]
        print(name + ": exact repair already installed; no repeat", flush=True)
        continue
    assert "Success" in adb(serial, "install", "-r", "--user", "0", str(apk))
    after = info(serial)
    assert before["uid"] == after["uid"] and before["firstInstall"] == after["firstInstall"]
    assert after["versionCode"] == ["10"] and all(v.strip() == "0.6.1" for v in after["versionName"])
    installed = adb(serial, "shell", "pm", "path", "--user", "0", PACKAGE).splitlines()[0].removeprefix("package:")
    assert adb(serial, "shell", "sha256sum", installed).split()[0] == m["apkSha256"]
    record = {"phone": name, "before": before, "after": after, "sameInstallationAndUid": True,
              "apkHashMatches": True, "verified": True,
              "operation": "install -r --user 0; no uninstall or data clear",
              "realDriveAcceptance": "pending private owner authentication/consent"}
    records = [r for r in records if r["phone"] != name] + [record]
    p.write_text(json.dumps(records, indent=2) + "\n")
    print(name + ": signed 0.6.1/code10 verified; exact APK hash and installation retained", flush=True)
