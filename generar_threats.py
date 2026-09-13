#!/usr/bin/env python3
import hashlib
import json
import urllib.request
from datetime import datetime, timezone

FEEDS = [
    ("https://urlhaus.abuse.ch/downloads/hostfile/", "MALWARE", "CRITICAL", "abuse.ch URLhaus"),
    ("https://openphish.com/feed.txt", "PHISHING", "HIGH", "OpenPhish Community"),
]
MAX_INDICATORS = 2000
TODAY = datetime.now(timezone.utc).strftime("%Y-%m-%d")


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": "AuraDefense-FeedBuilder/1.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8", errors="ignore").splitlines()


def host_from_line(line):
    line = line.strip()
    if not line or line.startswith("#"):
        return None
    if "://" in line:
        line = line.split("://", 1)[1]
    host = line.split("/")[0].split(":")[0].strip().lower()
    if "." not in host or host.endswith(".test") or host == "localhost":
        return None
    return host


indicators = []
seen = set()
for url, category, severity, source in FEEDS:
    try:
        for line in fetch(url):
            host = host_from_line(line)
            if not host or host in seen:
                continue
            seen.add(host)
            indicators.append({
                "id": f"{category.lower()}-{hashlib.md5(host.encode()).hexdigest()[:10]}",
                "indicator": host,
                "indicatorType": "DOMAIN",
                "category": category,
                "severity": severity,
                "descriptionEs": f"Dominio reportado activo como {category.lower()} por {source}.",
                "source": source,
                "updatedAt": TODAY,
            })
    except Exception as error:
        print(f"[AVISO] Fallo descargando {url}: {error}")

indicators = indicators[:MAX_INDICATORS]
payload = {
    "version": f"aura-real-{TODAY}",
    "updatedAt": TODAY,
    "source": "URLhaus (abuse.ch) + OpenPhish Community — dominios activos reportados",
    "indicators": indicators,
}
with open("threats.json", "w", encoding="utf-8") as output:
    json.dump(payload, output, ensure_ascii=False, indent=1)
print(f"OK: {len(indicators)} indicadores REALES escritos ({TODAY})")
