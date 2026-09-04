#!/usr/bin/env python3
"""Verify and optionally regenerate the Phase 6 bridge contract projections."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "bridge" / "contract.json"
DOCUMENT = ROOT / "bridge" / "CONTRACT.md"
KOTLIN_DTOS = ROOT / "phase18" / "app" / "src" / "main" / "java" / "com" / "saaf" / "milkshop" / "application" / "readmodels" / "HybridReadModels.kt"
KOTLIN_READS = ROOT / "phase18" / "app" / "src" / "main" / "java" / "com" / "saaf" / "milkshop" / "application" / "readmodels" / "HybridReadModelService.kt"
KOTLIN_PERMISSIONS = ROOT / "phase18" / "app" / "src" / "main" / "java" / "com" / "saaf" / "milkshop" / "domain" / "Security.kt"
KOTLIN_OUTPUT = ROOT / "phase18" / "app" / "src" / "main" / "java" / "com" / "saaf" / "milkshop" / "application" / "bridge" / "BridgeContractSnapshot.kt"
TYPESCRIPT_OUTPUT = ROOT / "ui" / "src" / "bridge" / "contract.generated.ts"


class ContractError(RuntimeError):
    pass


def canonical_bytes(contract: dict) -> bytes:
    return json.dumps(contract, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def contract_hash(contract: dict) -> str:
    return hashlib.sha256(canonical_bytes(contract)).hexdigest().upper()


def pascal(value: str) -> str:
    return "".join(part[:1].upper() + part[1:] for part in re.split(r"[^A-Za-z0-9]+", value) if part)


def parse_kotlin_dtos(source: str) -> dict[str, list[tuple[str, str]]]:
    result: dict[str, list[tuple[str, str]]] = {}
    for match in re.finditer(r"data class\s+(\w+)\s*\((.*?)\)\s*(?:\n|$)", source, re.S):
        fields = re.findall(r"\bval\s+(\w+)\s*:\s*([^,\n]+)", match.group(2))
        if not fields:
            raise ContractError(f"Could not parse fields for Kotlin DTO {match.group(1)}")
        result[match.group(1)] = [(name, value.strip()) for name, value in fields]
    if not result:
        raise ContractError("No Kotlin DTOs were found")
    return result


def parse_permissions(source: str) -> set[str]:
    match = re.search(r"enum class AppPermission\s*\{(.*?)\n\}", source, re.S)
    if not match:
        raise ContractError("Could not parse AppPermission")
    return set(re.findall(r"^\s*([A-Z][A-Z0-9_]+),?\s*$", match.group(1), re.M))


def response_references(value: str) -> set[str]:
    return set(re.findall(r"[A-Z][A-Za-z0-9_]+", value))


def validate_schema_type(value: str, enums: dict, named_types: set[str], context: str) -> None:
    if not isinstance(value, str):
        raise ContractError(f"{context}: schema type must be a string")
    if value.startswith("list:"):
        validate_schema_type(value[5:], enums, named_types, context)
        return
    if value.startswith("enum:"):
        if value[5:] not in enums:
            raise ContractError(f"{context}: unknown enum {value[5:]}")
        return
    scalars = {"id", "paisa", "decimal", "date", "month", "instant", "string", "secret", "session", "ticket", "boolean", "number"}
    if value not in scalars and value not in named_types:
        raise ContractError(f"{context}: unknown schema type {value}")


def validate_fields(fields: dict, enums: dict, named_types: set[str], context: str) -> None:
    if not isinstance(fields, dict):
        raise ContractError(f"{context}: fields must be an object")
    for name, raw_spec in fields.items():
        if not re.fullmatch(r"[a-z][A-Za-z0-9]*", name):
            raise ContractError(f"{context}: invalid field name {name!r}")
        spec = {"type": raw_spec, "required": True} if isinstance(raw_spec, str) else raw_spec
        if not isinstance(spec, dict) or "type" not in spec:
            raise ContractError(f"{context}.{name}: field specification must declare type")
        if set(spec) - {"type", "required", "nullable"}:
            raise ContractError(f"{context}.{name}: unknown field-schema keys")
        if not isinstance(spec.get("required", True), bool) or not isinstance(spec.get("nullable", False), bool):
            raise ContractError(f"{context}.{name}: required/nullable must be booleans")
        validate_schema_type(spec["type"], enums, named_types, f"{context}.{name}")


def validate(contract: dict, dtos: dict[str, list[tuple[str, str]]], read_source: str, permissions: set[str]) -> None:
    if contract.get("contractVersion") != 4:
        raise ContractError("contractVersion must be 4")
    endpoints = contract.get("endpoints")
    if not isinstance(endpoints, list) or not endpoints:
        raise ContractError("endpoints must be a non-empty list")
    actions = [endpoint.get("action") for endpoint in endpoints]
    if len(actions) != len(set(actions)):
        raise ContractError("Endpoint action names must be unique")

    enums = contract.get("enums")
    named_types = contract.get("types")
    error_codes = contract.get("errorCodes")
    if not isinstance(enums, dict) or not all(isinstance(values, list) and values for values in enums.values()):
        raise ContractError("enums must contain non-empty value lists")
    if not isinstance(named_types, dict):
        raise ContractError("types must be an object")
    schema_named_types = set(named_types) | set(dtos)
    for name, fields in named_types.items():
        validate_fields(fields, enums, schema_named_types, f"types.{name}")
    if not isinstance(error_codes, list) or not error_codes or len(error_codes) != len(set(error_codes)):
        raise ContractError("errorCodes must be a unique non-empty list")
    known_error_codes = set(error_codes)
    action_set = set(actions)

    dto_encodings = contract.get("dtoStringFieldEncodings")
    if not isinstance(dto_encodings, dict):
        raise ContractError("dtoStringFieldEncodings must be explicit")
    encoded_fields: set[str] = set()
    for encoding, fields in dto_encodings.items():
        if encoding not in {"decimal", "date", "month", "instant"} or not isinstance(fields, list):
            raise ContractError(f"Invalid DTO string encoding group {encoding!r}")
        for reference in fields:
            if reference in encoded_fields:
                raise ContractError(f"Duplicate DTO string encoding for {reference}")
            encoded_fields.add(reference)
            dto_name, separator, field_name = reference.partition(".")
            dto_fields = dict(dtos.get(dto_name, []))
            if not separator or field_name not in dto_fields:
                raise ContractError(f"Unknown DTO string field {reference}")
            if dto_fields[field_name].removesuffix("?") != "String":
                raise ContractError(f"Encoded DTO field must be a String: {reference}")

    allowed_auth = {"none", "session", "enrollment"}
    known_types = set(dtos) | set(contract.get("types", {})) | {"BridgeUnit"}
    forbidden_actor_fields = {"actorName", "actorRole", "actorUserId", "authoritativeRole"}
    for endpoint in endpoints:
        action = endpoint.get("action", "")
        kind = endpoint.get("kind")
        if kind not in {"query", "command", "system"} or not action.startswith(f"{kind}."):
            raise ContractError(f"{action}: action prefix and kind must agree")
        if endpoint.get("auth") not in allowed_auth:
            raise ContractError(f"{action}: invalid auth mode")
        permission = endpoint.get("permission")
        if permission is not None and permission not in permissions:
            raise ContractError(f"{action}: unknown permission {permission}")
        if endpoint.get("auth") == "session" and permission is None and action not in {
            "command.auth.logout", "command.auth.reauthenticate", "command.auth.changeOwnPassphrase"
        }:
            raise ContractError(f"{action}: protected endpoint must declare its permission")
        request = endpoint.get("request")
        if not isinstance(request, dict):
            raise ContractError(f"{action}: request must be an object")
        validate_fields(request, enums, schema_named_types, f"{action}.request")
        forbidden = forbidden_actor_fields.intersection(request)
        if forbidden:
            raise ContractError(f"{action}: caller-supplied authority fields are forbidden: {sorted(forbidden)}")
        if "sessionHandle" in request:
            raise ContractError(f"{action}: sessionHandle belongs to the envelope, never payload")
        if not endpoint.get("responseType"):
            raise ContractError(f"{action}: responseType is required")
        refs = response_references(endpoint["responseType"])
        unknown = refs - known_types - {"List", "ReadonlyArray"}
        if unknown:
            raise ContractError(f"{action}: unknown response types {sorted(unknown)}")
        if not isinstance(endpoint.get("errors"), list) or not endpoint["errors"]:
            raise ContractError(f"{action}: errors must be explicit")
        unknown_errors = set(endpoint["errors"]) - known_error_codes
        if unknown_errors:
            raise ContractError(f"{action}: unknown errors {sorted(unknown_errors)}")
        if "persistence" not in endpoint or "refresh" not in endpoint:
            raise ContractError(f"{action}: persistence and refresh behavior are required")
        if not isinstance(endpoint["persistence"], str) or not endpoint["persistence"].strip():
            raise ContractError(f"{action}: persistence must be explicit text")
        if not isinstance(endpoint["refresh"], list) or set(endpoint["refresh"]) - action_set:
            raise ContractError(f"{action}: refresh must contain known actions only")
        if any(not refresh.startswith("query.") for refresh in endpoint["refresh"]):
            raise ContractError(f"{action}: only query actions may be refreshed")
        if endpoint.get("kind") == "query":
            method = endpoint.get("nativeMethod")
            if not method or not re.search(rf"suspend fun\s+{re.escape(method)}\s*\(", read_source):
                raise ContractError(f"{action}: native query method {method!r} does not exist")
            if endpoint.get("persistence") != "none":
                raise ContractError(f"{action}: a query cannot persist state")
        if endpoint.get("kind") == "command" and endpoint.get("persistence") == "none":
            raise ContractError(f"{action}: commands must state their write effect")


def kotlin_type_to_ts(kotlin_type: str, field_name: str, string_encoding: str | None = None) -> str:
    nullable = kotlin_type.endswith("?")
    base = kotlin_type[:-1] if nullable else kotlin_type
    if base.startswith("List<") and base.endswith(">"):
        inner = base[5:-1]
        mapped = f"ReadonlyArray<{kotlin_type_to_ts(inner, field_name)}>"
    elif base == "String":
        mapped = {
            "decimal": "DecimalString",
            "date": "IsoDateString",
            "month": "YearMonthString",
            "instant": "IsoInstantString",
        }.get(string_encoding, "string")
    elif base == "Boolean":
        mapped = "boolean"
    elif base in {"Int", "Double"}:
        mapped = "number"
    elif base == "Long":
        mapped = "PaisaString" if "Paisa" in field_name else "Int64String"
    else:
        mapped = base
    return f"{mapped} | null" if nullable else mapped


def schema_type_to_ts(value: str, enums: dict[str, list[str]]) -> str:
    if value.startswith("list:"):
        return f"ReadonlyArray<{schema_type_to_ts(value[5:], enums)}>"
    if value.startswith("enum:"):
        name = value[5:]
        if name not in enums:
            raise ContractError(f"Unknown enum {name}")
        return name
    return {
        "id": "Int64String",
        "paisa": "PaisaString",
        "decimal": "DecimalString",
        "date": "IsoDateString",
        "month": "YearMonthString",
        "instant": "IsoInstantString",
        "string": "string",
        "secret": "string",
        "session": "SessionHandle",
        "ticket": "OpaqueNativeTicket",
        "boolean": "boolean",
        "number": "number",
    }.get(value, value)


def render_fields(fields: dict, enums: dict[str, list[str]], indent: str = "  ") -> list[str]:
    lines: list[str] = []
    for name, spec in fields.items():
        if isinstance(spec, str):
            spec = {"type": spec, "required": True}
        optional = "" if spec.get("required", True) else "?"
        ts_type = schema_type_to_ts(spec["type"], enums)
        if spec.get("nullable", False):
            ts_type += " | null"
        lines.append(f"{indent}readonly {name}{optional}: {ts_type};")
    return lines


def render_typescript(contract: dict, digest: str, dtos: dict[str, list[tuple[str, str]]]) -> str:
    enums = contract.get("enums", {})
    endpoints = contract["endpoints"]
    string_encodings = {
        reference: encoding
        for encoding, references in contract["dtoStringFieldEncodings"].items()
        for reference in references
    }
    lines = [
        "// GENERATED by scripts/verify-bridge-contract.py --write. DO NOT EDIT.",
        f"export const BRIDGE_CONTRACT_VERSION = {contract['contractVersion']} as const;",
        f"export const BRIDGE_CONTRACT_SHA256 = \"{digest}\" as const;",
        "",
        "export type Int64String = string & { readonly __int64: unique symbol };",
        "export type PaisaString = Int64String & { readonly __paisa: unique symbol };",
        "export type DecimalString = string & { readonly __decimal: unique symbol };",
        "export type IsoDateString = string & { readonly __isoDate: unique symbol };",
        "export type YearMonthString = string & { readonly __yearMonth: unique symbol };",
        "export type IsoInstantString = string & { readonly __isoInstant: unique symbol };",
        "export type SessionHandle = string & { readonly __sessionHandle: unique symbol };",
        "export type OpaqueNativeTicket = string & { readonly __nativeTicket: unique symbol };",
        "export type BridgeUnit = Record<string, never>;",
        "",
    ]
    for name, values in enums.items():
        union = " | ".join(json.dumps(value) for value in values)
        lines.append(f"export type {name} = {union};")
    if enums:
        lines.append("")
    error_union = " | ".join(json.dumps(value) for value in contract["errorCodes"])
    lines.extend([
        f"export type BridgeErrorCode = {error_union};",
        "",
        "export interface BridgeError {",
        "  readonly code: BridgeErrorCode;",
        "  readonly message: string;",
        "  readonly field: string | null;",
        "  readonly retryable: boolean;",
        "}",
        "",
    ])

    for name, fields in contract.get("types", {}).items():
        lines.append(f"export interface {name} {{")
        lines.extend(render_fields(fields, enums))
        lines.extend(["}", ""])

    for name, fields in dtos.items():
        lines.append(f"export interface {name} {{")
        for field_name, kotlin_type in fields:
            reference = f"{name}.{field_name}"
            lines.append(
                f"  readonly {field_name}: {kotlin_type_to_ts(kotlin_type, field_name, string_encodings.get(reference))};"
            )
        lines.extend(["}", ""])

    for endpoint in endpoints:
        request_name = pascal(endpoint["action"]) + "Request"
        lines.append(f"export interface {request_name} {{")
        lines.extend(render_fields(endpoint["request"], enums))
        lines.extend(["}", ""])

    actions = " |\n  ".join(json.dumps(endpoint["action"]) for endpoint in endpoints)
    lines.extend([f"export type BridgeAction =\n  {actions};", ""])
    lines.append("export interface BridgeRequestPayloadByAction {")
    for endpoint in endpoints:
        lines.append(f"  readonly {json.dumps(endpoint['action'])}: {pascal(endpoint['action'])}Request;")
    lines.extend(["}", ""])
    lines.append("export interface BridgeResponseByAction {")
    for endpoint in endpoints:
        response = endpoint["responseType"]
        if response.startswith("List<"):
            response = f"ReadonlyArray<{response[5:-1]}>"
        lines.append(f"  readonly {json.dumps(endpoint['action'])}: {response};")
    lines.extend([
        "}",
        "",
        "export interface BridgeRequest<A extends BridgeAction> {",
        "  readonly contractVersion: typeof BRIDGE_CONTRACT_VERSION;",
        "  readonly requestId: string;",
        "  readonly action: A;",
        "  readonly sessionHandle?: SessionHandle;",
        "  readonly payload: BridgeRequestPayloadByAction[A];",
        "}",
        "",
        "export interface BridgeSuccessResponse<A extends BridgeAction> {",
        "  readonly contractVersion: typeof BRIDGE_CONTRACT_VERSION;",
        "  readonly requestId: string;",
        "  readonly action: A;",
        "  readonly ok: true;",
        "  readonly data: BridgeResponseByAction[A];",
        "}",
        "",
        "export interface BridgeFailureResponse<A extends BridgeAction> {",
        "  readonly contractVersion: typeof BRIDGE_CONTRACT_VERSION;",
        "  readonly requestId: string;",
        "  readonly action: A;",
        "  readonly ok: false;",
        "  readonly error: BridgeError;",
        "}",
        "",
        "export type BridgeResponse<A extends BridgeAction> =",
        "  | BridgeSuccessResponse<A>",
        "  | BridgeFailureResponse<A>;",
        "",
    ])
    lines.append("export const BRIDGE_ENDPOINTS = {")
    for endpoint in endpoints:
        permission = "null" if endpoint.get("permission") is None else json.dumps(endpoint["permission"])
        lines.append(
            f"  {json.dumps(endpoint['action'])}: {{ kind: {json.dumps(endpoint['kind'])}, auth: {json.dumps(endpoint['auth'])}, permission: {permission} }},"
        )
    lines.extend(["} as const;", ""])
    return "\n".join(lines)


def render_kotlin(contract: dict, digest: str) -> str:
    endpoints = contract["endpoints"]
    lines = [
        "// GENERATED by scripts/verify-bridge-contract.py --write. DO NOT EDIT.",
        "package com.saaf.milkshop.application.bridge",
        "",
        "data class BridgeEndpointContract(",
        "    val action: String,",
        "    val kind: String,",
        "    val auth: String,",
        "    val permission: String?,",
        "    val responseType: String,",
        ")",
        "",
        "object BridgeContractSnapshot {",
        f"    const val VERSION = {contract['contractVersion']}",
        f"    const val CONTRACT_SHA256 = \"{digest}\"",
        "    val errorCodes = setOf(",
    ]
    for error_code in contract["errorCodes"]:
        lines.append(f'        "{error_code}",')
    lines.extend([
        "    )",
        "    val endpoints = listOf(",
    ])
    for endpoint in endpoints:
        permission = "null" if endpoint.get("permission") is None else f'"{endpoint["permission"]}"'
        lines.append(
            f'        BridgeEndpointContract("{endpoint["action"]}", "{endpoint["kind"]}", "{endpoint["auth"]}", {permission}, "{endpoint["responseType"]}"),'
        )
    lines.extend(["    )", "}", ""])
    return "\n".join(lines)


def update_document_hash(document: str, digest: str) -> str:
    pattern = r"(Manifest SHA-256:\s*`)(?:PENDING|[0-9A-Fa-f]{64})(`)"
    updated, count = re.subn(pattern, rf"\g<1>{digest}\g<2>", document)
    if count != 1:
        raise ContractError("CONTRACT.md must contain exactly one Manifest SHA-256 marker")
    return updated


def check_exact(path: Path, expected: str) -> None:
    if not path.exists():
        raise ContractError(f"Missing generated projection: {path.relative_to(ROOT)}")
    actual = path.read_text(encoding="utf-8").replace("\r\n", "\n")
    if actual != expected:
        raise ContractError(f"Generated projection is stale: {path.relative_to(ROOT)} (run with --write)")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="Regenerate Kotlin/TypeScript projections and the document hash")
    args = parser.parse_args()
    try:
        contract = json.loads(MANIFEST.read_text(encoding="utf-8"))
        dto_source = KOTLIN_DTOS.read_text(encoding="utf-8")
        dtos = parse_kotlin_dtos(dto_source)
        validate(
            contract,
            dtos,
            KOTLIN_READS.read_text(encoding="utf-8"),
            parse_permissions(KOTLIN_PERMISSIONS.read_text(encoding="utf-8")),
        )
        digest = contract_hash(contract)
        kotlin = render_kotlin(contract, digest)
        typescript = render_typescript(contract, digest, dtos)
        document = update_document_hash(DOCUMENT.read_text(encoding="utf-8"), digest)
        if args.write:
            KOTLIN_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
            TYPESCRIPT_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
            KOTLIN_OUTPUT.write_text(kotlin, encoding="utf-8", newline="\n")
            TYPESCRIPT_OUTPUT.write_text(typescript, encoding="utf-8", newline="\n")
            DOCUMENT.write_text(document, encoding="utf-8", newline="\n")
        else:
            check_exact(KOTLIN_OUTPUT, kotlin)
            check_exact(TYPESCRIPT_OUTPUT, typescript)
            actual_document = DOCUMENT.read_text(encoding="utf-8").replace("\r\n", "\n")
            if actual_document != document:
                raise ContractError("CONTRACT.md manifest hash is stale (run with --write)")
        print(f"Bridge contract OK: v{contract['contractVersion']}, {len(contract['endpoints'])} endpoints, SHA-256 {digest}")
        return 0
    except (OSError, json.JSONDecodeError, ContractError) as error:
        print(f"Bridge contract verification failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
