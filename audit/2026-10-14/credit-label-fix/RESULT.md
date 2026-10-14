# Sale Credit wording correction — 14 October 2026

Verified the shared native sale resource still displayed Customer account. The payment option and unselected review fallback now say Credit; the picker says Choose customer; the supporting prompt says Required for a credit sale. This applies to cashier, owner and manager through their shared sale form.

Source commit: `305a91dac8ba70e839e92f80d52de8e4920089a7`. One production resource file changed, three strings only. Customer selection, ledger account types, permissions, sale calculations and storage remain unchanged. No tests were added or run; broader functional/visual acceptance remains deferred. This is a terminology correction within finish batch 7.

Signed `:app:assembleRelease` succeeded. The regular 0.4.2/code 6 APK passed certificate, package, preview-disabled and embedded-source-commit checks. S24 received a normal-profile update with no uninstall/data clearing; its installed APK hash matches the built artifact and the launcher opened. A06 was disconnected and was not updated. PACKAGE_MANIFEST.json and INSTALLED.json retain the exact provenance. Source and records are local; no Git push or new visual acceptance ran.
