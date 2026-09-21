import json
import os
import re
import unicodedata

BASE = "AIAssistantApp/app/src/main/assets"

TARGETS = {
    "geography": 400,
    "chemistry": 350,
    "physics": 350,
    "biology": 350,
    "astronomy": 300,
    "computing": 400,
    "ai": 350,
    "history": 500,
    "mathematics": 400,
    "medicine": 500,
    "engineering": 400,
    "earth_science": 300,
    "economics": 250,
    "language": 250,
    "science": 250,
    "civics": 200
}

def norm(text):
    text = str(text).strip().lower()
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = text.replace("ș", "s").replace("ş", "s")
    text = text.replace("ț", "t").replace("ţ", "t")
    text = re.sub(r"[^a-z0-9 ]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()

def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

index_path = os.path.join(BASE, "knowledge_index.json")

if not os.path.exists(index_path):
    raise SystemExit(f"ERROR: {index_path} not found")

index = load_json(index_path)

totals = {}
all_topics = {}
all_aliases = {}

total_entries = 0
total_aliases = 0
json_errors = 0

print("=" * 72)
print("WORLD AI KNOWLEDGE BASE AUDIT")
print("=" * 72)
print()

for dataset in index.get("datasets", []):
    domain = dataset["domain"]
    filename = dataset["file"]
    path = os.path.join(BASE, filename)

    if not os.path.exists(path):
        print(f"[MISSING] {filename}")
        json_errors += 1
        continue

    try:
        data = load_json(path)
    except Exception as e:
        print(f"[JSON ERROR] {filename}: {e}")
        json_errors += 1
        continue

    entries = data if isinstance(data, list) else data.get("entries", [])

    entry_count = len(entries)
    alias_count = 0

    for entry in entries:
        topic = norm(entry.get("topic", ""))
        aliases = entry.get("aliases", [])

        alias_count += len(aliases)

        if topic:
            all_topics.setdefault(topic, []).append(
                (domain, entry.get("topic", ""))
            )

        for alias in aliases:
            a = norm(alias)
            if a:
                all_aliases.setdefault(a, []).append(
                    (domain, entry.get("topic", ""), alias)
                )

    totals[domain] = entry_count
    total_entries += entry_count
    total_aliases += alias_count

print(f"{'DOMAIN':15} {'CURRENT':>7} {'TARGET':>7} {'NEED':>7}")
print("-" * 45)

for domain, target in TARGETS.items():
    current = totals.get(domain, 0)
    need = max(0, target - current)
    print(f"{domain:15} {current:7} {target:7} {need:7}")

print("-" * 45)
target_total = sum(TARGETS.values())
print(f"{'TOTAL':15} {total_entries:7} {target_total:7} {max(0, target_total-total_entries):7}")

duplicate_topics = {
    k: v for k, v in all_topics.items()
    if len(v) > 1
}

duplicate_aliases = {
    k: v for k, v in all_aliases.items()
    if len(v) > 1
}

print()
print("QUALITY CHECK")
print("-" * 45)
print(f"JSON errors:             {json_errors}")
print(f"Unique topics:           {len(all_topics)}")
print(f"Duplicate topics:        {len(duplicate_topics)}")
print(f"Unique aliases:          {len(all_aliases)}")
print(f"Duplicate aliases:       {len(duplicate_aliases)}")
print(f"Total entries:           {total_entries}")
print(f"Total aliases:           {total_aliases}")

if duplicate_topics:
    print("\nDUPLICATE TOPICS:")
    for key, values in sorted(duplicate_topics.items()):
        print(f"  {key}: {values}")

if duplicate_aliases:
    print("\nDUPLICATE ALIASES:")
    for key, values in sorted(duplicate_aliases.items()):
        print(f"  {key}: {values}")

print()
print("AUDIT COMPLETE")
