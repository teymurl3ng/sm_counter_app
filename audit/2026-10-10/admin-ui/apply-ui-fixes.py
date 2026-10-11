"""Apply only reviewed UI files, refusing changed or unexpected destinations."""
from pathlib import Path
import hashlib
import json

BASE = Path(__file__).resolve().parent
TARGET = Path("C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer").resolve()
manifest = json.loads((BASE / "manifest.json").read_text(encoding="utf-8"))
pending = []
for entry in manifest:
    relative = Path(entry["path"])
    if relative.is_absolute() or ".." in relative.parts or relative.parts[:2] != ("src", "app"):
        raise SystemExit("Refusing non-UI path")
    destination = (TARGET / relative).resolve()
    if not destination.is_relative_to(TARGET):
        raise SystemExit("Refusing destination outside the viewer")
    fixed = (BASE / "fixed" / relative).read_bytes()
    current_hash = hashlib.sha256(destination.read_bytes()).hexdigest()
    if hashlib.sha256(fixed).hexdigest() != entry["after"]:
        raise SystemExit(f"Fixed source changed: {relative}")
    if current_hash == entry["after"]:
        continue
    if current_hash != entry["before"]:
        raise SystemExit(f"Viewer file changed since inspection: {relative}")
    pending.append((destination, fixed))

# Validate every destination before writing any file.
for destination, fixed in pending:
    destination.write_bytes(fixed)
print(f"Applied {len(pending)} reviewed UI files; no API, authentication or database files changed.")
