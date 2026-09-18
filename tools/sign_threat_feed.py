#!/usr/bin/env python3
"""Generate a canonical signed threat manifest without exposing the private key.

Environment:
  AURA_THREAT_SIGNING_KEY=base64(Ed25519 private key) or PEM-encoded private key

Usage:
  AURA_THREAT_SIGNING_KEY=... python3 tools/sign_threat_feed.py --input /path/to/feed.json
"""

from __future__ import annotations

import base64
import hashlib
import json
import os
import sys
from pathlib import Path
from typing import Any, Iterable


def _canonical_indicator(indicator: dict[str, Any]) -> str:
    fields = [
        str(indicator.get("id", "")),
        str(indicator.get("indicator", "")),
        str(indicator.get("indicatorType", "DOMAIN")),
        str(indicator.get("category", "MALWARE")),
        str(indicator.get("severity", "HIGH")),
        str(indicator.get("source", "aurafeed")),
        str(indicator.get("updatedAt", "1970-01-01T00:00:00Z")),
    ]
    return "|".join(fields)


def _canonical_payload(feed: dict[str, Any]) -> str:
    base = [
        str(feed.get("ruleId", "")),
        str(feed.get("source", "")),
        str(feed.get("version", "")),
        str(feed.get("evidence", "")),
        str(feed.get("expiresAt", "0")),
    ]
    indicators = feed.get("indicators", [])
    if isinstance(indicators, dict):
        items = indicators.get("items", [])
    else:
        items = indicators
    if isinstance(items, list) and items:
        blob = ";;".join(_canonical_indicator(item) for item in items)
        return "|".join(base + [blob])
    return "|".join(base)


def _sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _load_private_key() -> Any:
    raw_key = os.environ.get("AURA_THREAT_SIGNING_KEY", "").strip()
    if not raw_key:
        raise RuntimeError("AURA_THREAT_SIGNING_KEY no está definido.")

    try:
        from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
        from cryptography.hazmat.primitives.serialization import load_pem_private_key

        if raw_key.startswith("-----BEGIN"):
            return load_pem_private_key(raw_key.encode("utf-8"), password=None)

        try:
            raw_bytes = base64.b64decode(raw_key, validate=True)
            if len(raw_bytes) == 32:
                return Ed25519PrivateKey.from_private_bytes(raw_bytes)
        except Exception:
            pass

        try:
            return load_pem_private_key(base64.b64decode(raw_key), password=None)
        except Exception:
            pass

        raise RuntimeError("AURA_THREAT_SIGNING_KEY debe ser un PEM o una clave Ed25519 en Base64 sin imprimirla en logs.")
    except ImportError as exc:
        raise RuntimeError("Falta la dependencia 'cryptography'. Instálala con: pip install cryptography") from exc


def _sign(feed: dict[str, Any]) -> dict[str, Any]:
    key = _load_private_key()
    payload = _canonical_payload(feed).encode("utf-8")
    checksum = _sha256_hex(payload)
    signature = key.sign(payload)
    feed = dict(feed)
    feed["checksum"] = checksum
    feed["size"] = len(payload)
    feed["signature"] = base64.b64encode(signature).decode("ascii")
    feed["publicKeyId"] = feed.get("publicKeyId", "aura-ed25519-feed-v1")
    return feed


def _read_feed(path: str | None) -> dict[str, Any]:
    if path is None:
        raw = sys.stdin.read()
        if not raw.strip():
            raise RuntimeError("No se recibió JSON de entrada.")
        return json.loads(raw)
    content = Path(path).read_text(encoding="utf-8")
    return json.loads(content)


def main(argv: Iterable[str]) -> int:
    import argparse

    parser = argparse.ArgumentParser(description="Firma un feed JSON de amenazas usando Ed25519.")
    parser.add_argument("--input", help="Ruta al JSON del feed a firmar. Si se omite, se lee desde stdin.")
    parser.add_argument("--pretty", action="store_true", help="Formato legible en la salida JSON.")
    args = parser.parse_args(list(argv))

    try:
        feed = _read_feed(args.input)
        signed = _sign(feed)
        json_dump = json.dumps(signed, ensure_ascii=False, indent=2 if args.pretty else None)
        sys.stdout.write(json_dump)
        if args.pretty:
            sys.stdout.write("\n")
        return 0
    except Exception as exc:  # pragma: no cover - CLI guard
        sys.stderr.write(f"ERROR: {exc}\n")
        return 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
