import json
import os
import re
import sys
import unicodedata

BASE = "AIAssistantApp/app/src/main/assets"
INDEX = os.path.join(BASE, "knowledge_index.json")

MIN_ALIASES = 6
MIN_RO = 2
MIN_EN = 2

def norm(text):
    text = str(text).strip().lower()
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = re.sub(r"[^a-z0-9 ]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()

def looks_ro(text):
    t = norm(text)
    words = set(t.split())

    ro_markers = {
        "ce", "este", "sunt", "sunt", "care", "cum",
        "de", "din", "pentru", "despre", "explica",
        "explica", "inseamna", "inseamna", "definitie",
        "cum functioneaza", "care este", "ce sunt",
        "ce reprezinta", "de ce"
    }

    return (
        bool(words & ro_markers)
        or any(x in t for x in [
            "ce este", "ce sunt", "cum functioneaza",
            "care este", "ce inseamna", "explica"
        ])
    )

def looks_en(text):
    t = norm(text)
    words = set(t.split())

    en_markers = {
        "what", "what's", "what", "is", "are", "how",
        "why", "which", "define", "definition", "explain",
        "meaning", "means", "function", "works"
    }

    return bool(words & en_markers) or any(x in t for x in [
        "what is", "what are", "how does", "how do",
        "why is", "why are", "what does", "define"
    ])

def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

if len(sys.argv) != 2:
    print("Usage:")
    print("python validate_kb_batch.py knowledge_expansion/history_batch_001.json")
    sys.exit(2)

batch_path = sys.argv[1]

if not os.path.exists(batch_path):
    print(f"ERROR: batch not found: {batch_path}")
    sys.exit(2)

index = load_json(INDEX)

existing_topics = {}
existing_aliases = {}

for dataset in index.get("datasets", []):
    domain = dataset["domain"]
    path = os.path.join(BASE, dataset["file"])

    if not os.path.exists(path):
        continue

    data = load_json(path)
    entries = data if isinstance(data, list) else data.get("entries", [])

    for entry in entries:
        topic = norm(entry.get("topic", ""))

        if topic:
            existing_topics.setdefault(topic, []).append(
                (domain, entry.get("topic", ""))
            )

        for alias in entry.get("aliases", []):
            a = norm(alias)
            if a:
                existing_aliases.setdefault(a, []).append(
                    (domain, entry.get("topic", ""), alias)
                )

batch = load_json(batch_path)

if not isinstance(batch, list):
    batch = batch.get("entries", [])

if not isinstance(batch, list):
    print("ERROR: batch must be a JSON array or contain 'entries'")
    sys.exit(2)

errors = []
warnings = []

batch_topics = {}
batch_aliases = {}

for i, entry in enumerate(batch, 1):

    prefix = f"ENTRY {i}"

    if not isinstance(entry, dict):
        errors.append(f"{prefix}: not an object")
        continue

    for required in ("topic", "aliases", "answer", "domain"):
        if required not in entry:
            errors.append(f"{prefix}: missing '{required}'")

    topic_raw = entry.get("topic", "")
    topic = norm(topic_raw)

    if not topic:
        errors.append(f"{prefix}: empty topic")

    aliases = entry.get("aliases", [])

    if not isinstance(aliases, list):
        errors.append(f"{prefix}: aliases must be a list")
        aliases = []

    if len(aliases) < MIN_ALIASES:
        errors.append(
            f"{prefix} {topic_raw!r}: "
            f"needs at least {MIN_ALIASES} aliases, found {len(aliases)}"
        )

    normalized_aliases = []

    for alias in aliases:
        a = norm(alias)

        if not a:
            errors.append(
                f"{prefix} {topic_raw!r}: empty alias"
            )
            continue

        normalized_aliases.append(a)

        if a in existing_aliases:
            errors.append(
                f"{prefix} {topic_raw!r}: "
                f"ALIAS ALREADY EXISTS -> {alias!r} "
                f"{existing_aliases[a]}"
            )

        if a in batch_aliases:
            errors.append(
                f"{prefix} {topic_raw!r}: "
                f"DUPLICATE ALIAS IN BATCH -> {alias!r}"
            )

        batch_aliases.setdefault(a, []).append(
            (entry.get("domain", ""), topic_raw, alias)
        )

    unique_aliases = set(normalized_aliases)

    if len(unique_aliases) < len(normalized_aliases):
        errors.append(
            f"{prefix} {topic_raw!r}: duplicate aliases inside entry"
        )

    ro_count = sum(looks_ro(a) for a in aliases)
    en_count = sum(looks_en(a) for a in aliases)

    if ro_count < MIN_RO:
        errors.append(
            f"{prefix} {topic_raw!r}: "
            f"needs at least {MIN_RO} recognizable RO aliases, found {ro_count}"
        )

    if en_count < MIN_EN:
        errors.append(
            f"{prefix} {topic_raw!r}: "
            f"needs at least {MIN_EN} recognizable EN aliases, found {en_count}"
        )

    if topic in existing_topics:
        errors.append(
            f"{prefix}: TOPIC ALREADY EXISTS -> "
            f"{topic_raw!r} {existing_topics[topic]}"
        )

    if topic in batch_topics:
        errors.append(
            f"{prefix}: DUPLICATE TOPIC IN BATCH -> {topic_raw!r}"
        )

    batch_topics.setdefault(topic, []).append(
        (entry.get("domain", ""), topic_raw)
    )

print("=" * 72)
print("KNOWLEDGE BATCH VALIDATOR")
print("=" * 72)
print()
print(f"Batch: {batch_path}")
print(f"Entries: {len(batch)}")
print(f"Existing topics checked: {len(existing_topics)}")
print(f"Existing aliases checked: {len(existing_aliases)}")
print()

if errors:
    print(f"VALIDATION FAILED: {len(errors)} error(s)")
    print()

    for error in errors:
        print("[ERROR]", error)

    if warnings:
        print()
        for warning in warnings:
            print("[WARNING]", warning)

    sys.exit(1)

print("VALIDATION PASSED")
print()
print(f"Every entry has >= {MIN_ALIASES} aliases.")
print(f"Every entry has >= {MIN_RO} recognizable RO aliases.")
print(f"Every entry has >= {MIN_EN} recognizable EN aliases.")
print("No topic duplicates against current KB.")
print("No alias duplicates against current KB.")
print("No duplicate aliases inside the batch.")
print()
print("BATCH IS SAFE TO IMPORT")
