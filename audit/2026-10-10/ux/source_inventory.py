"""Reproducible read-only UX source inventory. Run from the repository root.

Counts are search signals, not assertions of runtime coverage or correctness.
Only audit artifacts are written; application sources and device data are untouched.
"""
from pathlib import Path
import csv
import hashlib
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
NATIVE = ROOT / "phase18/app/src/main/java/com/saaf/milkshop/nativeui"


def write_tsv(name, fields, rows):
    with (OUT / name).open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)


def lines_of(text, pattern):
    return ",".join(str(n) for n, line in enumerate(text.splitlines(), 1)
                    if re.search(pattern, line) and not line.lstrip().startswith("import "))


all_rows, screens, motion, feedback, brands = [], [], [], [], []
for path in sorted(NATIVE.rglob("*.kt")):
    text = path.read_text(encoding="utf-8-sig")
    relative = path.relative_to(ROOT).as_posix()
    code = re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)
    code = re.sub(r"^import .*", "", code, flags=re.M)
    all_rows.append(dict(source=relative, sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
    if re.search(r"(?:Screens?|PreviewHost|Shell|Activity)\.kt$", path.name):
        screens.append(dict(
            source=relative,
            family=path.parent.name,
            composables=len(re.findall(r"@Composable\b", code)),
            composable_functions=",".join(re.findall(r"@Composable\s+(?:(?:private|internal|public)\s+)?fun\s+(\w+)", code)),
            interaction_signals=len(re.findall(r"\b(?:onClick|onCheckedChange|onValueChange|onDismissRequest)\s*=|\.(?:clickable|selectable|toggleable)\s*\(", code)),
            direct_haptic_calls=len(re.findall(r"\bhaptic(?:Tap|Select|Commit|Heavy|Confirm)\s*\(", code)),
            transitions=lines_of(text, r"(?:NativeSectionTransition(?:<.*>)?|AnimatedContent|AnimatedVisibility|Crossfade)\s*\("),
            list_signals=lines_of(text, r"LazyColumn\(|LazyRow\(|items\(|forEach(?:Indexed)?\s*\{"),
            feedback_signals=lines_of(text, r"(?:receipt|success|Success|error|Error|showSnackbar|liveRegion)"),
            runtime_status="NOT_EXECUTED_FOR_THIS_AUDIT",
        ))
    for n, line in enumerate(text.splitlines(), 1):
        if re.search(r"(?:NativeSectionTransition(?:<.*>)?|AnimatedContent|AnimatedVisibility|Crossfade)\s*\(", line) and not line.lstrip().startswith("fun "):
            nearby = text.splitlines()[n:n + 12]
            motion.append(dict(source=relative, line=n, call=line.strip(),
                               target_and_key=" | ".join(s.strip() for s in nearby
                                                        if any(w in s for w in ["targetState", "contentKey", "tween(", "scaleIn(", "fadeIn(", "fadeOut(", "ExitTransition", "visible ="])),
                               status="SOURCE_SIGNAL_RUNTIME_PENDING"))
    if "ViewModel" in path.name:
        feedback.append(dict(source=relative,
                             receipt_success_fields=lines_of(text, r"val .*?(?:receipt|Receipt|success|Success|last.*Id|message|Message)"),
                             reset_or_timer=lines_of(text, r"autoClear|delay\(|receipt = null|success = null|Success = null"),
                             warm_refresh=lines_of(text, r"refreshing = true|previous\?\.copy")))
    for n, line in enumerate(text.splitlines(), 1):
        if re.search(r'"[^"\n]*(?:Saaf|SAAF|saaf)[^"\n]*"', line):
            brands.append(dict(source=relative, line=n, text=line.strip(), classification="REVIEW_LITERAL_NOT_AUTOMATIC_REPLACE"))

for path in sorted((ROOT / "phase18/app/src/main/res").glob("values*/strings.xml")):
    for n, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if re.search(r">[^<]*(?:Saaf|SAAF|saaf)[^<]*<", line):
            brands.append(dict(source=path.relative_to(ROOT).as_posix(), line=n, text=line.strip(), classification="REVIEW_VISIBLE_RESOURCE"))

write_tsv("source_hashes.tsv", ["source", "sha256"], all_rows)
write_tsv("screen_inventory.tsv", list(screens[0]), screens)
write_tsv("motion_inventory.tsv", list(motion[0]), motion)
write_tsv("feedback_state_inventory.tsv", list(feedback[0]), feedback)
write_tsv("branding_inventory.tsv", list(brands[0]), brands)

control_path = ROOT / "phase18/qa/native_control_inventory.tsv"
with control_path.open(encoding="utf-8-sig") as f:
    controls = list(csv.DictReader((line for line in f if not line.startswith("#")), delimiter="\t"))


def luminance(rgb):
    channels = [int(rgb[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [c / 12.92 if c <= .04045 else ((c + .055) / 1.055) ** 2.4 for c in channels]
    return sum(c * w for c, w in zip(linear, (.2126, .7152, .0722)))


pairs = [
    ("light ink/background", "17201B", "F4F1E8", "text"),
    ("light muted/background", "68726C", "F4F1E8", "text"),
    ("light muted/surface", "68726C", "FFFDF7", "text"),
    ("light muted/surfaceVariant", "68726C", "EBE7DC", "text"),
    ("light surface/background", "FFFDF7", "F4F1E8", "surface separation, not a WCAG text test"),
    ("light outlineVariant/background", "E6E1D7", "F4F1E8", "divider, not automatically a required 3:1 control boundary"),
    ("light outline/background", "D6D0C3", "F4F1E8", "boundary signal"),
    ("dark onSurfaceVariant/background", "C5CEC8", "101713", "text"),
    ("dark surface/background", "18211C", "101713", "surface separation"),
]
contrast = []
for label, foreground, background, purpose in pairs:
    a, b = sorted((luminance(foreground), luminance(background)))
    contrast.append(dict(pair=label, foreground=foreground, background=background,
                         contrast=(b + .05) / (a + .05), purpose=purpose))
write_tsv("token_contrast.tsv", list(contrast[0]), contrast)

summary = dict(
    date="2026-10-10", method="Static source inventory plus targeted implementation review; no animation pass claimed",
    git_head=subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
    native_kotlin_files=len(all_rows), screen_host_shell_files=len(screens),
    transition_calls=len(motion), viewmodels=len(feedback),
    screen_files_with_direct_haptic_calls=sum(s["direct_haptic_calls"] > 0 for s in screens),
    screen_files_without_direct_haptic_calls=sum(s["direct_haptic_calls"] == 0 for s in screens),
    direct_haptic_calls=sum(s["direct_haptic_calls"] for s in screens),
    control_inventory_rows=len(controls),
    control_inventory_status_counts={s: sum(c["status"] == s for c in controls) for s in sorted(set(c["status"] for c in controls))},
    caveat="Controls marked COVERED in the older inventory do not prove animation, haptic, contrast, or feedback acceptance.",
)
(OUT / "inventory_summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
print(json.dumps(summary, indent=2))
print(json.dumps(contrast, indent=2))
