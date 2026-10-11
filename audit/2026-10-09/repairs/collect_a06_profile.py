"""Copy only the isolated QA profile artifacts; preserve PNG bytes unchanged."""
from pathlib import Path
import json
import subprocess

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "audit/2026-10-09/repairs/evidence"
ADB = "C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe"
for name in ["native-data-profile.json", "native-motion-profile.json",
             "motion-before.png", "motion-mid.png", "motion-after.png"]:
    result = subprocess.run([ADB, "-s", "R8VXA02SNKH", "exec-out", "run-as",
        "com.saaf.milkshop.qa.tests", "cat", "files/audit_device_profile/" + name],
        capture_output=True, timeout=20, creationflags=subprocess.CREATE_NO_WINDOW)
    assert result.returncode == 0, result.stderr.decode(errors="replace")
    if name.endswith(".json"):
        profile = json.loads(result.stdout)
        summary = {key: ({k: v for k, v in value.items() if k != "values_ms"}
                         if isinstance(value, dict) else value)
                   for key, value in profile.items() if key != "deadline_ms"}
        print(name, json.dumps(summary), flush=True)
    else:
        assert result.stdout.startswith(b"\x89PNG\r\n\x1a\n"), "Expected PNG artifact"
    (OUT / ("a06-" + name)).write_bytes(result.stdout)
