"""Build only the OAuth-configured 0.6.1 repair; keep the 0.6.0 evidence intact."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
CERT = "9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1"
registration = json.loads((OUT / "GOOGLE_REGISTRATION.json").read_text())
assert registration["registrationVerified"], "Verify the actual Google release registration first"
properties = (ROOT / "phase18/oauth.properties").read_text(encoding="utf-8")
client = re.search(r"(?m)^googleWebClientId\s*=\s*(\S+)\s*$", properties).group(1)
assert re.fullmatch(r"[A-Za-z0-9_-]+\.apps\.googleusercontent\.com", client)
client_hash = hashlib.sha256(client.encode()).hexdigest()
assert registration["webClientIdSha256"] == client_hash
assert not subprocess.check_output(
    ["git", "status", "--porcelain", "--", "phase18"], cwd=ROOT, text=True
).strip(), "Commit production sources before packaging"
commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
tracked = subprocess.check_output([
    "git", "ls-files", "phase18/app/src/main", "phase18/app/schemas",
    "phase18/app/build.gradle.kts", "phase18/build.gradle.kts",
    "phase18/settings.gradle.kts", "phase18/gradle.properties",
], cwd=ROOT, text=True).splitlines()
hashes = {p: hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in tracked}
env = dict(os.environ, JAVA_HOME="C:/Program Files/Android/Android Studio/jbr")
env["PATH"] = env["JAVA_HOME"] + "/bin;" + env["PATH"]
with (OUT / "release-build-console.txt").open("w", encoding="utf-8") as log:
    result = subprocess.run([
        str(ROOT / "phase18/gradlew.bat"), ":app:assembleRelease", "--console=plain",
        "--no-daemon", "-Pkotlin.compiler.execution.strategy=in-process",
    ], cwd=ROOT / "phase18", env=env, stdout=log, stderr=subprocess.STDOUT)
assert result.returncode == 0, "Release build failed; inspect local console"
config = (ROOT / "phase18/app/build/generated/source/buildConfig/release/com/saaf/milkshop/BuildConfig.java").read_text()
assert re.search(r'GOOGLE_WEB_CLIENT_ID\s*=\s*"' + re.escape(client) + r'"', config)
assert re.search(r"ENABLE_NATIVE_PREVIEW_FIXTURES\s*=\s*false", config)
assert re.search(r'NATIVE_PREVIEW_SHARED_PASSPHRASE\s*=\s*""', config)
assert re.search(r'GIT_SHA\s*=\s*"' + re.escape(commit[:7]) + r'"', config)
apk = OUT / "Saaf-Milk-0.6.1.apk"
shutil.copy2(ROOT / "phase18/app/build/outputs/apk/release/app-release.apk", apk)
sdk = Path("C:/Users/Teymur/AppData/Local/Android/Sdk/build-tools/36.0.0")
signature = subprocess.check_output([
    str(sdk / "apksigner.bat"), "verify", "--print-certs", str(apk),
], env=env, text=True, stderr=subprocess.DEVNULL)
assert re.search(r"Signer #1 certificate SHA-256 digest: (\w+)", signature).group(1) == CERT
sha1 = re.search(r"Signer #1 certificate SHA-1 digest: (\w+)", signature).group(1)
assert sha1.lower() == registration["releaseSigningSha1"].replace(":", "").lower()
badging = subprocess.check_output([str(sdk / "aapt.exe"), "dump", "badging", str(apk)], env=env, text=True)
assert "package: name='com.saaf.milkshop' versionCode='10' versionName='0.6.1'" in badging
assert "application-debuggable" not in badging
with zipfile.ZipFile(apk) as archive:
    assert any(client.encode() in archive.read(n) for n in archive.namelist() if re.fullmatch(r"classes\d*\.dex", n))
assert all(hashlib.sha256((ROOT / p).read_bytes()).hexdigest() == h for p, h in hashes.items())
manifest = {
    "package": "com.saaf.milkshop", "versionName": "0.6.1", "versionCode": 10,
    "sourceCommit": commit, "apk": apk.name,
    "apkSha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "certificateSha256": CERT, "debuggable": False, "previewFixtures": False,
    "testPassphraseBlank": True, "databaseSchema": 19,
    "googleWebClientIdPresent": True, "webClientIdSha256": client_hash,
    "googleReleaseRegistrationVerified": True,
    "realDriveConsentUploadInspection": "pending ordinary-device acceptance",
    "sourceSha256": hashes,
}
(OUT / "RELEASE_MANIFEST.json").write_text(json.dumps(manifest, indent=2) + "\n")
print(json.dumps({k: v for k, v in manifest.items() if k != "sourceSha256"}, indent=2))
