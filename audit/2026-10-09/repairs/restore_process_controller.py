"""Opt-in A06 process-death probes; never touches the ordinary application."""
from pathlib import Path
import json, subprocess, time

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "audit/2026-10-09/repairs/evidence"
ADB = Path("C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe")
SERIAL = "R8VXA02SNKH"
TARGET = "com.saaf.milkshop.qa.tests"
RUNNER = TARGET + ".test/com.saaf.milkshop.SaafMilkTestRunner"
CASE = "com.saaf.milkshop.RestoreProcessDeathProbe"
assert TARGET.endswith(".qa.tests")

def adb(*args, **kwargs):
    return subprocess.run([str(ADB), "-s", SERIAL, *args], capture_output=True, text=True,
                          creationflags=subprocess.CREATE_NO_WINDOW, **kwargs)

results = []
for stage in ["before_commit", "after_commit"]:
    logpath = OUT / f"a06-restore-{stage}-deliberately-killed.txt"
    with logpath.open("w", encoding="utf-8") as log:
        preparation = subprocess.Popen([str(ADB), "-s", SERIAL, "shell", "am", "instrument", "-w",
            "-e", "saafSkipBootstrap", "true",
            "-e", "audit_crash_stage", stage, "-e", "class", CASE + "#prepareCrashWindow", RUNNER],
            stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        marker = None
        deadline = time.monotonic() + 50
        while time.monotonic() < deadline:
            read = adb("shell", "run-as", TARGET, "cat", "files/audit_restore_case/ready.json", timeout=5)
            if read.returncode == 0:
                try:
                    candidate = json.loads(read.stdout)
                    if candidate.get("stage") == stage: marker = candidate; break
                except json.JSONDecodeError: pass
            if preparation.poll() is not None:
                raise RuntimeError(f"Preparation exited before the {stage} marker; inspect {logpath}")
            time.sleep(.5)
        if marker is None:
            adb("shell", "am", "force-stop", TARGET, timeout=10)
            preparation.wait(timeout=15)
            raise RuntimeError(f"No {stage} readiness marker within 50 seconds")
        stopped = adb("shell", "am", "force-stop", TARGET, timeout=10)
        assert stopped.returncode == 0, stopped.stderr
        prep_exit = preparation.wait(timeout=15)
    # Deliberate preparation termination is recorded as an interruption, never PASS.
    verification = adb("shell", "am", "instrument", "-w", "-e", "audit_crash_stage", stage,
        "-e", "saafSkipBootstrap", "true",
        "-e", "class", CASE + "#verifyRecoveryAfterProcessDeath", RUNNER, timeout=60)
    verify_text = verification.stdout + verification.stderr
    (OUT / f"a06-restore-{stage}-fresh-process-verification.txt").write_text(verify_text, encoding="utf-8")
    passed = verification.returncode == 0 and "OK (1 test)" in verify_text and "FAILURES" not in verify_text
    result = {"stage":stage,"readiness_marker_observed":True,"preparation_intentionally_force_stopped":True,
        "preparation_adb_exit":prep_exit,"verification_adb_exit":verification.returncode,"fresh_process_pass":passed}
    results.append(result)
    (OUT / "a06-restore-process-results.json").write_text(json.dumps(results, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(result), flush=True)
    assert passed, f"Fresh-process verification failed for {stage}; inspect saved output"
print("Both process-death recovery checks passed; no power-loss/filesystem durability claim.", flush=True)
