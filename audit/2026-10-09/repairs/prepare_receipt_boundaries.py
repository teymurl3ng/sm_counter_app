"""One-time source edit, retained as repair provenance, not a replayable installer."""
from pathlib import Path

root = Path(__file__).resolve().parents[3]
base = root / "phase18/app/src/main/java/com/saaf/milkshop/application"

def wrap_result(text, method, nested_try=False):
    method_start = text.index(f"suspend fun {method}(")
    indent = "            " if nested_try else "        "
    start = text.index(indent + "val result = ", method_start)
    boundary = "\n        } finally" if nested_try else "\n    }"
    end = text.index(boundary, start)
    body = text[start:end]
    assert body.count("return Native") == 1, method
    body = body.replace("return Native", "Native", 1)
    wrapped = indent + "// The mutation, required evidence and receipt reads commit together.\n"
    wrapped += indent + "// A read/authorization failure cannot report failure after saving.\n"
    wrapped += indent + "return database.withWriteTransaction {\n"
    wrapped += "\n".join("    " + line if line else "" for line in body.split("\n"))
    wrapped += "\n" + indent + "}"
    return text[:start] + wrapped + text[end:]

targets = ["sale/NativeSaleFacade.kt", "collection/NativeCollectionFacade.kt",
    "expense/NativeExpenseFacade.kt", "drawing/NativeDrawingFacade.kt",
    "salarypayment/NativeSalaryPaymentFacade.kt", "vendorpayment/NativeVendorPaymentFacade.kt",
    "operations/NativeReceiveMilkFacade.kt", "operations/NativeMakeYogurtFacade.kt",
    "operations/NativeRecordSpoilageFacade.kt"]
for rel in targets:
    path = base / rel
    text = wrap_result(path.read_text(encoding="utf-8"), "record")
    if "import androidx.room3.withWriteTransaction" not in text:
        text = text.replace("\n\nimport ", "\n\nimport androidx.room3.withWriteTransaction\nimport ", 1)
    path.write_text(text, encoding="utf-8")

path = base / "cashclose/NativeCashCloseFacade.kt"
text = path.read_text(encoding="utf-8")
for method in ("recordCount", "closeDay"):
    text = wrap_result(text, method)
text = wrap_result(text, "reopenDay", nested_try=True)
text = text.replace("""            // tell closed-with-count apart from absorbed. Written immediately
            // after the close action (its own audit already exists); a crash in
            // this window loses the seal rows, never the close itself.""",
    """            // tell closed-with-count apart from absorbed. The surrounding
            // transaction includes the close, these seal rows and its receipt;
            // a crash or audit failure cannot separate their commits.""")
text = text.replace("""        deliveries.markDelivered(sessionHandle, deliveryId, date)
        return load(sessionHandle, date)""", """        return database.withWriteTransaction {
            deliveries.markDelivered(sessionHandle, deliveryId, date)
            load(sessionHandle, date)
        }""")
path.write_text(text, encoding="utf-8")
print("Wrapped nine entry facades and cash-close count/close/reopen/resolve boundaries")
