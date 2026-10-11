"""Reconcile actual A06 evidence and source hashes; never infer test passes."""
from pathlib import Path
import hashlib
import json
import math
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "audit/2026-10-09/repairs/evidence"
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

old = json.loads((OUT / "verification-final.json").read_text())
source_paths = set(old["android_source_sha256"])
source_paths.update("phase18/app/src/androidTest/java/com/saaf/milkshop/" + name + ".kt"
                    for name in ["NativeDeviceProfileProbe", "RestoreProcessDeathProbe"])
source_hashes = {path: digest(ROOT / path) for path in sorted(source_paths)}
changed = [path for path, sha in old["android_source_sha256"].items()
           if source_hashes[path] != sha]
assert not any("/src/main/" in path for path in changed), changed

batches = {}
expected = {"a06-ui-first-102.xml": (102, 5, 0),
            "a06-ui-profile-second-104.xml": (104, 3, 0),
            "a06-ui-profile-final-104.xml": (104, 0, 0),
            "a06-data-final.xml": (44, 0, 2)}
for name, counts in expected.items():
    suite = ET.parse(OUT / name).getroot()
    cases = suite.findall("testcase")
    result = {"tests": len(cases), "failures": sum(c.find("failure") is not None for c in cases),
              "errors": sum(c.find("error") is not None for c in cases),
              "skipped": sum(c.find("skipped") is not None for c in cases)}
    assert (result["tests"], result["failures"], result["skipped"]) == counts, (name, result)
    assert result["errors"] == 0
    assert len({(c.get("classname"), c.get("name")) for c in cases}) == len(cases)
    assert all(int(suite.get(k)) == v for k, v in result.items())
    result["failed_cases"] = [c.get("classname") + "." + c.get("name") for c in cases
                              if c.find("failure") is not None]
    result["skipped_cases"] = [c.get("classname") + "." + c.get("name") for c in cases
                               if c.find("skipped") is not None]
    batches[name] = result

recovery = json.loads((OUT / "a06-restore-process-results.json").read_text())
assert {r["stage"] for r in recovery} == {"before_commit", "after_commit"}
for result in recovery:
    assert result["fresh_process_pass"] and result["preparation_intentionally_force_stopped"]
    text = (OUT / f'a06-restore-{result["stage"]}-fresh-process-verification.txt').read_text()
    assert "OK (1 test)" in text and "FAILURES" not in text

for name, expected_pass in [("a06-profile-first-manual-run.txt", "OK (2 tests)"),
                            ("a06-motion-strengthened-run.txt", "OK (1 test)")]:
    text = (OUT / name).read_text()
    assert expected_pass in text and "FAILURES" not in text
assert "QA_FAST: PASS" in (OUT / "a06-final-static-qa.txt").read_text()

def check_distribution(d):
    values = sorted(d["values_ms"])
    assert d["samples"] == len(values) and values
    assert d["median_ms"] == values[math.ceil(len(values) * .5) - 1]
    assert d["p95_ms"] == values[math.ceil(len(values) * .95) - 1]
    assert d["max_ms"] == max(values)
    return {k: v for k, v in d.items() if k != "values_ms"}

data = json.loads((OUT / "a06-native-data-profile.json").read_text())
motion = json.loads((OUT / "a06-native-motion-profile.json").read_text())
data_stats = {k: check_distribution(data[k]) for k in
              ["collection_command_and_receipt", "sale_command_and_receipt", "month_reports"]}
motion_stats = check_distribution(motion["frame_total"])
assert len(motion["deadline_ms"]) == motion_stats["samples"]
assert motion["frames_over_deadline"] == sum(d > 0 and v > d for v, d in
    zip(motion["frame_total"]["values_ms"], motion["deadline_ms"]))
assert motion["transitions"] == 30
screenshots = {name: digest(OUT / f"a06-motion-{name}.png") for name in ["before", "mid", "after"]}
assert screenshots["before"] != screenshots["after"]
for name in screenshots:
    assert (OUT / f"a06-motion-{name}.png").read_bytes().startswith(b"\x89PNG\r\n\x1a\n")

tracker = (ROOT / "REPAIR_PLAN_2026-10-10.md").read_text(encoding="utf-8")
finding_ids = {int(i) for i in re.findall(r"\bF(\d{2})\b", tracker)}
assert set(range(1, 34)).issubset(finding_ids)
artifacts = sorted(path for path in OUT.glob("a06-*") if path.is_file()
                   and path.name != "a06-evidence-verification.txt")
manifest = {"device": "SM-A065F / Android 16 / USB R8VXA02SNKH",
    "target": "com.saaf.milkshop.qa.tests", "numbered_findings_mapped": 33,
    "device_batches": batches, "counts_overlap_prior_s24_batches": True,
    "recovery": recovery, "data_profile": data_stats,
    "motion_profile": {**motion_stats, "transitions": 30,
                       "frames_over_deadline": motion["frames_over_deadline"]},
    "window_screenshot_sha256": screenshots,
    "owner_visual_acceptance": "Pending", "release_performance": "Pending; debug overruns observed",
    "live_supabase": "Blocked: MCP tools not exposed; saafmilk-dev=DEV; saafmilk-prod=PROD",
    "deployment": "Not performed", "production_source_unchanged_since_prior_manifest": True,
    "changed_existing_test_sources_since_prior_manifest": changed,
    "android_source_sha256": source_hashes,
    "report_sha256": {path: digest(ROOT / path) for path in
        ["PROJECT_AUDIT_2026-10-09.md", "REPAIR_PLAN_2026-10-10.md", "FINDINGS_STATUS.md",
         "audit/2026-10-09/repairs/A06_VERIFICATION_2026-10-10.md",
         "audit/2026-10-09/repairs/README.md"]},
    "verification_log_excluded_from_hashes": "a06-evidence-verification.txt is generated during this verification",
    "a06_evidence_sha256": {path.name: digest(path) for path in artifacts}}
(OUT / "verification-a06-final.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"xml_counts": {k: {s: v[s] for s in ["tests", "failures", "errors", "skipped"]}
                                for k, v in batches.items()},
                  "android_source_hashes": len(source_hashes), "evidence_hashes": len(artifacts),
                  "production_source_changed_since_prior_snapshot": False,
                  "verified_finding_ids": len(finding_ids)}, indent=2))
print("A06_EVIDENCE_RECONCILIATION: PASS")
