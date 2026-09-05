#!/usr/bin/env python3
"""Fail builds when removed prototype or misleading UI behavior returns."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI_ROOT = ROOT / "ui"
SOURCE_ROOT = UI_ROOT / "src"
PACKAGE_ROOT = ROOT / "phase18" / "app" / "src" / "main" / "assets" / "hybrid"

REMOVED_SOURCE_PATHS = (
    "ui/src/viewmodels/useMilkShopViewModel.ts",
    "ui/src/types/milkShop.ts",
    "ui/src/types.ts",
    "ui/src/components/accounts/CustomerLedgerView.tsx",
    "ui/src/components/accounts/LedgerHubView.tsx",
    "ui/src/components/expenses/ExpensesView.tsx",
    "ui/src/components/inventory/InventoryView.tsx",
    "ui/src/components/layout/Navbar.tsx",
    "ui/src/components/management/EmployeesView.tsx",
    "ui/src/components/management/ManagementHubView.tsx",
    "ui/src/components/operations/DailyOperationsView.tsx",
    "ui/src/components/reports/DailyCloseSummaryModal.tsx",
    "ui/src/components/reports/ReportsView.tsx",
    "ui/src/components/settings/SettingsView.tsx",
    "ui/src/components/suppliers/SupplierLedgerView.tsx",
    "ui/src/components/wholesale/WholesaleView.tsx",
    "ui/src/components/common/Badge.tsx",
    "ui/src/components/common/ConfirmModal.tsx",
    "ui/src/components/common/MetricCard.tsx",
    "ui/src/components/common/Modal.tsx",
    "ui/src/components/common/NoProductErrorModal.tsx",
    "ui/src/components/common/PaymentTransactionView.tsx",
    "ui/src/components/hybrid/Phase7HybridShell.tsx",
)

REMOVED_DEPENDENCIES = (
    "clsx",
    "html2canvas",
    "jspdf",
    "motion",
    "recharts",
    "tailwind-merge",
)

SOURCE_RULES = {
    "browser-local business storage": re.compile(r"\b(?:localStorage|sessionStorage|indexedDB)\b"),
    "synthetic report curve": re.compile(r"\bMath\.sin\s*\("),
    "obsolete browser business store": re.compile(r"\buseMilkShopViewModel\b"),
    "obsolete hybrid shell": re.compile(r"\bPhase7HybridShell\b"),
    "browser-owned file import": re.compile(r"\bFileReader\b|\bcreateObjectURL\s*\(|type\s*=\s*[\"']file[\"']"),
    "prototype import/export or clear-all action": re.compile(
        r"\b(?:exportData|importData|clearAllData|handleImport|handleExport|resetAllData)\b"
    ),
    "removed presentation package import": re.compile(
        r"\b(?:html2canvas|jsPDF|recharts|tailwind-merge)\b"
    ),
    "misleading opening-balance label": re.compile(r"Recorded money total", re.IGNORECASE),
    "client-combined opening account balances": re.compile(r"accountBalances\s*\.\s*reduce\s*\("),
}

PACKAGE_RULES = {
    "browser-local business storage": SOURCE_RULES["browser-local business storage"],
    "synthetic report curve": SOURCE_RULES["synthetic report curve"],
    "obsolete browser business store": SOURCE_RULES["obsolete browser business store"],
    "obsolete hybrid shell": SOURCE_RULES["obsolete hybrid shell"],
    "browser-owned file import": SOURCE_RULES["browser-owned file import"],
    "prototype import/export or clear-all action": SOURCE_RULES["prototype import/export or clear-all action"],
    "removed presentation package import": SOURCE_RULES["removed presentation package import"],
    "misleading opening-balance label": SOURCE_RULES["misleading opening-balance label"],
    "client-combined opening account balances": SOURCE_RULES["client-combined opening account balances"],
}


class CleanupError(RuntimeError):
    pass


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def text_files(root: Path, suffixes: set[str]) -> list[Path]:
    return sorted(path for path in root.rglob("*") if path.is_file() and path.suffix.lower() in suffixes)


def forbidden_matches(files: list[Path], rules: dict[str, re.Pattern[str]]) -> list[str]:
    failures: list[str] = []
    for path in files:
        text = path.read_text(encoding="utf-8", errors="replace")
        for label, pattern in rules.items():
            match = pattern.search(text)
            if match:
                line = text.count("\n", 0, match.start()) + 1
                failures.append(f"{relative(path)}:{line}: {label}")
    return failures


def verify_removed_paths() -> None:
    present = [path for path in REMOVED_SOURCE_PATHS if (ROOT / path).exists()]
    if present:
        raise CleanupError("Removed prototype paths returned:\n  " + "\n  ".join(present))


def verify_dependencies() -> None:
    package = json.loads((UI_ROOT / "package.json").read_text(encoding="utf-8"))
    declared = set(package.get("dependencies", {})) | set(package.get("devDependencies", {}))
    returned = sorted(declared.intersection(REMOVED_DEPENDENCIES))

    lock = json.loads((UI_ROOT / "package-lock.json").read_text(encoding="utf-8"))
    locked_paths = set(lock.get("packages", {}))
    returned.extend(
        dependency
        for dependency in REMOVED_DEPENDENCIES
        if f"node_modules/{dependency}" in locked_paths and dependency not in returned
    )
    if returned:
        raise CleanupError("Removed dependencies returned: " + ", ".join(sorted(returned)))


def verify_json_boundary(source_files: list[Path]) -> None:
    parse_sites: list[str] = []
    for path in source_files:
        text = path.read_text(encoding="utf-8")
        if "JSON.parse" in text:
            parse_sites.append(relative(path))
    allowed = ["ui/src/bridge/nativeBridge.ts"]
    if parse_sites != allowed:
        raise CleanupError(
            "JSON.parse must remain confined to the native bridge response boundary; found: "
            + (", ".join(parse_sites) if parse_sites else "none")
        )


def verify_active_entry() -> None:
    main = (SOURCE_ROOT / "main.tsx").read_text(encoding="utf-8")
    app = (SOURCE_ROOT / "App.tsx").read_text(encoding="utf-8")
    if "./App" not in main or "<App" not in main:
        raise CleanupError("ui/src/main.tsx no longer enters through App")
    if "HybridAppShell" not in app or "<HybridAppShell" not in app:
        raise CleanupError("ui/src/App.tsx no longer enters through HybridAppShell")


def verify_opening_explanation() -> None:
    panel = SOURCE_ROOT / "components" / "hybrid" / "OpeningStatePanel.tsx"
    text = panel.read_text(encoding="utf-8")
    if "opening-account-balance-explanation" not in text or "not combined into one total" not in text:
        raise CleanupError("OpeningStatePanel must explain why unlike account balances are not totaled")


def verify(*, source_only: bool = False) -> tuple[int, int]:
    if not SOURCE_ROOT.is_dir():
        raise CleanupError(f"Missing UI source root: {SOURCE_ROOT}")
    if not source_only and not (PACKAGE_ROOT / "index.html").is_file():
        raise CleanupError("Missing packaged hybrid index.html")

    source_files = text_files(SOURCE_ROOT, {".ts", ".tsx"})
    package_files = [] if source_only else text_files(PACKAGE_ROOT, {".html", ".js", ".css"})
    if not source_files:
        raise CleanupError("UI source inventory is empty")
    if not source_only and not any(path.suffix == ".js" for path in package_files):
        raise CleanupError("Packaged JavaScript inventory is empty")

    verify_removed_paths()
    verify_dependencies()
    verify_json_boundary(source_files)
    verify_active_entry()
    verify_opening_explanation()

    failures = forbidden_matches(source_files, SOURCE_RULES)
    if not source_only:
        failures.extend(forbidden_matches(package_files, PACKAGE_RULES))
    if failures:
        raise CleanupError("Forbidden cleanup markers found:\n  " + "\n  ".join(failures))
    return len(source_files), len(package_files)


def self_test() -> None:
    cases = {
        "browser-local business storage": "localStorage.setItem('products', '[]')",
        "synthetic report curve": "const trend = Math.sin(day)",
        "obsolete browser business store": "useMilkShopViewModel()",
        "obsolete hybrid shell": "<Phase7HybridShell />",
        "browser-owned file import": "const reader = new FileReader()",
        "prototype import/export or clear-all action": "clearAllData()",
        "removed presentation package import": "import html2canvas from 'html2canvas'",
        "misleading opening-balance label": "Recorded money total",
        "client-combined opening account balances": "accountBalances.reduce((sum, item) => sum + item.balance, 0)",
    }
    for label, sample in cases.items():
        if not SOURCE_RULES[label].search(sample):
            raise CleanupError(f"Cleanup self-test did not catch {label}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true", help="Prove representative forbidden patterns are rejected")
    parser.add_argument("--source-only", action="store_true", help="Check source before rebuilding packaged assets")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    source_count, package_count = verify(source_only=args.source_only)
    prefix = "UI cleanup gate self-test and verification OK" if args.self_test else "UI cleanup gate OK"
    package_summary = (
        "packaged files deferred until post-package verification"
        if args.source_only
        else f"{package_count} packaged files scanned"
    )
    print(
        f"{prefix}: {source_count} active TypeScript/TSX files; "
        f"{len(REMOVED_SOURCE_PATHS)} prototype paths and {len(REMOVED_DEPENDENCIES)} dependencies absent; "
        f"{package_summary}."
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (CleanupError, OSError, ValueError, json.JSONDecodeError) as failure:
        print(f"UI cleanup gate FAILED: {failure}")
        raise SystemExit(1)
