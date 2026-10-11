"""Verify the saved release snapshot; no network, device writes or app mutation."""
from pathlib import Path
import hashlib
import json
import zipfile


ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    manifest = json.loads((HERE / "verification-release-final.json").read_text(encoding="utf-8"))
    for relative, expected in manifest["workspace_sha256"].items():
        assert sha(ROOT / relative) == expected, f"Workspace evidence changed: {relative}"
    for relative, expected in manifest["viewer_source_sha256"].items():
        assert sha(Path(manifest["viewer_checkout"]) / relative) == expected, f"Viewer changed: {relative}"
    with zipfile.ZipFile(HERE / "viewer-release-source.zip") as archive:
        assert set(archive.namelist()) == set(manifest["viewer_source_sha256"])
        for relative, expected in manifest["viewer_source_sha256"].items():
            assert hashlib.sha256(archive.read(relative)).hexdigest() == expected, relative
    apk = json.loads((HERE / "android-release-artifact.json").read_text(encoding="utf-8"))
    device = json.loads((HERE / "android-production-device.json").read_text(encoding="utf-8"))
    ready = json.loads((HERE / "android-production-ready.json").read_text(encoding="utf-8"))
    assert sha(HERE / apk["apk"]) == apk["sha256"] == device["installed_apk_sha256"]
    assert device["matches_release"] and apk["signer_matches_previous_release"]
    assert apk["package"] == "com.saaf.milkshop" and apk["version_code"] == 5
    assert not apk["debuggable"] and not apk["native_preview_fixtures_enabled"]
    assert ready["production_ready_screen"] == "Create Owner" and ready["production_pid"]
    assert not ready["production_account_created"] and not ready["qa_data_reset"]
    assert "package:com.saaf.milkshop.qa" in ready["user_0_disabled_packages"]
    assert "BUILD SUCCESSFUL" in (HERE / "android-production-build.txt").read_text(encoding="utf-8")
    test_log = (HERE / "viewer-release-tests.txt").read_text(encoding="utf-8")
    assert "tests 114" in test_log and "pass 114" in test_log and "fail 0" in test_log
    final = json.loads((HERE / "viewer-final-release-identity.json").read_text(encoding="utf-8"))
    assert final["verified_source_commit"] == manifest["viewer_commit"]
    assert final["readyState"] == "READY" and final["target"] == "production"
    listing = json.loads((HERE / "viewer-production-deployments.json").read_text(encoding="utf-8"))
    deployed = next(d for d in listing["deployments"] if d["url"] == final["url"])
    assert deployed["meta"]["githubCommitSha"] == manifest["viewer_commit"]
    inspect = json.loads((HERE / "viewer-production-inspect.json").read_text(encoding="utf-8"))
    assert inspect["id"] == final["id"] and inspect["url"] == final["url"]
    for filename in ["viewer-stage-smoke.json", "viewer-production-smoke.json"]:
        checks = json.loads((HERE / filename).read_text(encoding="utf-8"))["checks"]
        by_path = {item["path"]: item for item in checks}
        assert by_path["/login"]["status"] == 200 and by_path["/login"]["has_prod_label"]
        assert by_path["/api/overview"]["status"] == 401
        assert by_path["/api/overview"]["body"]["env"] == "prod"
        assert by_path["/api/health"]["status"] == 401
        assert "/login" in by_path["/staff"]["final_url"]
    assert not manifest["genuine_prod_login_verified"]
    assert not manifest["authenticated_prod_sync_verified"]
    print(f"PASS: {len(manifest['workspace_sha256'])} workspace/evidence hashes, "
          f"{len(manifest['viewer_source_sha256'])} Viewer source/ZIP hashes, signed APK/device identity, "
          "114-test result and saved production publication/guard evidence. "
          "Actual PROD authentication/sync remains pending.")


if __name__ == "__main__":
    main()
