#!/usr/bin/env python3
"""Verify the versioned Ed25519 interoperability vector."""

import base64
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VECTOR = ROOT / "test-fixtures/crypto/threat-feed-vector.json"
PUBLIC_KEY = ROOT / "test-fixtures/crypto/threat-feed-public-key.base64"


def canonical(feed: dict) -> bytes:
    indicators = sorted(feed["indicators"], key=lambda item: item["id"])
    payload = {
        "ruleId": feed["ruleId"],
        "source": feed["source"],
        "version": feed["version"],
        "evidence": feed["evidence"],
        "expiresAt": feed["expiresAt"],
        "indicators": indicators,
    }
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def main() -> None:
    feed = json.loads(VECTOR.read_text(encoding="utf-8"))
    payload = canonical(feed)
    assert hashlib.sha256(payload).hexdigest() == feed["checksum"]
    assert len(payload) == feed["size"]

    with tempfile.TemporaryDirectory() as directory:
        directory = Path(directory)
        payload_path = directory / "payload"
        signature_path = directory / "signature"
        public_der_path = directory / "public.der"
        public_pem_path = directory / "public.pem"
        payload_path.write_bytes(payload)
        signature_path.write_bytes(base64.b64decode(feed["signature"], validate=True))
        public_der_path.write_bytes(base64.b64decode(PUBLIC_KEY.read_text(encoding="ascii")))
        subprocess.run(
            ["openssl", "pkey", "-pubin", "-inform", "DER", "-in", str(public_der_path), "-out", str(public_pem_path)],
            check=True,
            stdout=subprocess.DEVNULL,
        )
        subprocess.run(
            ["openssl", "pkeyutl", "-verify", "-pubin", "-inkey", str(public_pem_path), "-in", str(payload_path), "-sigfile", str(signature_path)],
            check=True,
            stdout=subprocess.DEVNULL,
        )


if __name__ == "__main__":
    main()
