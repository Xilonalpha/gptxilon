#!/usr/bin/env python3
import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
cases = json.loads((ROOT / "cases.json").read_text(encoding="utf-8"))

ap = argparse.ArgumentParser()
ap.add_argument("--outputs", required=True)
ap.add_argument("--out", default="xab-report.json")
a = ap.parse_args()

case_ids = [c.get("id") for c in cases]
if any(not cid for cid in case_ids) or len(case_ids) != len(set(case_ids)):
    raise SystemExit("Invalid/duplicate case id in cases.json")

groups = {}
for c in cases:
    group = c.get("session_group")
    if group:
        groups.setdefault(group, []).append(c["id"])

for group, ids in groups.items():
    if group == "memory-orbit" and ids != ["MEM-001", "MEM-002", "MEM-003"]:
        raise SystemExit("Invalid memory-orbit sequence order")

out = {}
for n, line in enumerate(Path(a.outputs).read_text(encoding="utf-8").splitlines(), 1):
    if line.strip():
        x = json.loads(line)
        cid = x.get("id")
        if not cid or cid in out:
            raise SystemExit(f"Invalid/duplicate id on line {n}")
        out[cid] = x

unknown = sorted(set(out) - set(case_ids))
if unknown:
    raise SystemExit("Output contains unknown case id(s): " + ", ".join(unknown))

results = []
for c in cases:
    x = out.get(c["id"])
    raw_score = x.get("score") if x else None
    score = raw_score if isinstance(raw_score, int) and 0 <= raw_score <= 2 else None
    status = "scored" if score is not None else (
        "missing" if x is None else "manual_review_required"
    )
    results.append({
        "id": c["id"],
        "category": c["category"],
        "session_group": c.get("session_group"),
        "score": score,
        "max": 2,
        "status": status,
        "evidence": (x or {}).get("evidence", "")
    })

report = {
    "benchmark": "XAB-1",
    "version": "1.1.0",
    "generated_at": datetime.now(timezone.utc).isoformat(),
    "cases_sha256": hashlib.sha256((ROOT / "cases.json").read_bytes()).hexdigest(),
    "outputs_sha256": hashlib.sha256(Path(a.outputs).read_bytes()).hexdigest(),
    "results": results,
    "categories": {}
}

for cat in sorted({c["category"] for c in cases}):
    xs = [r for r in results if r["category"] == cat]
    ss = [r["score"] for r in xs if r["score"] is not None]
    complete = len(ss) == len(xs)
    partial = round(sum(ss) / (2 * len(ss)) * 100, 2) if ss else None
    report["categories"][cat] = {
        "cases": len(xs),
        "scored_cases": len(ss),
        "complete": complete,
        "score": partial if complete else None,
        "partial_score": partial if ss else None
    }

ss = [r["score"] for r in results if r["score"] is not None]
complete = len(ss) == len(results)
partial = round(sum(ss) / (2 * len(ss)) * 100, 2) if ss else None
report["overall"] = {
    "cases": len(results),
    "scored_cases": len(ss),
    "complete": complete,
    "score": partial if complete else None,
    "partial_score": partial if ss else None,
    "missing_case_ids": [r["id"] for r in results if r["status"] == "missing"],
    "manual_review_case_ids": [
        r["id"] for r in results if r["status"] == "manual_review_required"
    ]
}

Path(a.out).write_text(
    json.dumps(report, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8"
)
print(json.dumps(report["overall"], indent=2))
