#!/usr/bin/env python3

import json
import os
import shutil
import sys
from datetime import datetime
from unicodedata import normalize as u_normalize

ROOT = os.path.dirname(os.path.abspath(__file__))
INDEX_PATH = os.path.join(
    ROOT,
    "AIAssistantApp",
    "app",
    "src",
    "main",
    "assets",
    "knowledge_index.json"
)

def norm(text):
    text = u_normalize("NFKD", str(text))
    text = "".join(c for c in text if not (0x300 <= ord(c) <= 0x36F))
    text = text.lower()
    return "".join(c if c.isalnum() or c.isspace() else " " for c in text).strip()

def atomic_write(path, data):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    os.replace(tmp, path)

def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def main():
    if len(sys.argv) != 2:
        print("Usage: python import_kb_batch.py <batch.json>")
        sys.exit(1)

    batch_path = os.path.abspath(sys.argv[1])

    if not os.path.isfile(batch_path):
        print(f"ERROR: batch file not found: {batch_path}")
        sys.exit(1)

    index = load_json(INDEX_PATH)
    raw_batch = load_json(batch_path)

    if isinstance(raw_batch, dict) and "entries" in raw_batch:
        batch = raw_batch["entries"]
    elif isinstance(raw_batch, list):
        batch = raw_batch
    else:
        print("ERROR: batch must be a JSON list or object with 'entries'.")
        sys.exit(1)

    if not batch:
        print("ERROR: batch is empty.")
        sys.exit(1)

    domains = {str(x.get("domain", "")).strip() for x in batch}
    if len(domains) != 1:
        print(f"ERROR: batch must contain exactly one domain. Found: {sorted(domains)}")
        sys.exit(1)

    domain = next(iter(domains))
    if not domain:
        print("ERROR: batch entries have no domain.")
        sys.exit(1)

    dataset = None
    for item in index.get("datasets", []):
        if item.get("domain") == domain:
            dataset = item
            break

    if dataset is None:
        print(f"ERROR: no dataset registered for domain: {domain}")
        sys.exit(1)

    dataset_path = os.path.join(
        ROOT,
        "AIAssistantApp",
        "app",
        "src",
        "main",
        "assets",
        dataset["file"]
    )

    if not os.path.isfile(dataset_path):
        print(f"ERROR: dataset file not found: {dataset_path}")
        sys.exit(1)

    dataset_data = load_json(dataset_path)

    if isinstance(dataset_data, dict) and isinstance(dataset_data.get("entries"), list):
        current = dataset_data["entries"]
        dataset_is_wrapped = True
    elif isinstance(dataset_data, list):
        current = dataset_data
        dataset_is_wrapped = False
    else:
        print(f"ERROR: unsupported dataset JSON structure: {dataset_path}")
        sys.exit(1)

    existing_topics = {}
    existing_aliases = {}

    for entry in current:
        topic_key = norm(entry.get("topic", ""))
        if topic_key:
            existing_topics.setdefault(topic_key, []).append(entry.get("topic", ""))

        for alias in entry.get("aliases", []):
            alias_key = norm(alias)
            if alias_key:
                existing_aliases.setdefault(alias_key, []).append(alias)

    duplicate_aliases = {
        key: values
        for key, values in existing_aliases.items()
        if len(values) > 1
    }

    print("=" * 72)
    print("KNOWLEDGE BATCH IMPORTER")
    print("=" * 72)
    print(f"Batch entries: {len(batch)}")
    print(f"Domain: {domain}")
    print(f"Dataset: {dataset_path}")
    print(f"Existing entries: {len(current)}")
    print(f"Pre-existing normalized alias duplicates: {len(duplicate_aliases)}")

    for key in sorted(duplicate_aliases):
        print(f"PRE-EXISTING DUPLICATE: '{key}'")

    batch_topics = set()
    batch_aliases = set()

    for i, entry in enumerate(batch, 1):
        required = ["topic", "aliases", "answer", "domain"]

        for field in required:
            if field not in entry:
                print(f"ERROR: entry {i} missing field: {field}")
                sys.exit(1)

        if entry["domain"] != domain:
            print(f"ERROR: entry {i} has inconsistent domain.")
            sys.exit(1)

        topic_key = norm(entry["topic"])

        if topic_key in existing_topics:
            print(f"ERROR: new topic already exists: {entry['topic']}")
            sys.exit(1)

        if topic_key in batch_topics:
            print(f"ERROR: duplicate topic inside batch: {entry['topic']}")
            sys.exit(1)

        batch_topics.add(topic_key)

        local_aliases = set()

        for alias in entry["aliases"]:
            alias_key = norm(alias)

            if not alias_key:
                print(f"ERROR: empty alias in entry: {entry['topic']}")
                sys.exit(1)

            if alias_key in existing_aliases:
                print(
                    f"ERROR: new alias already exists: "
                    f"'{alias}' in topic '{entry['topic']}'"
                )
                sys.exit(1)

            if alias_key in batch_aliases:
                print(
                    f"ERROR: duplicate alias inside batch: "
                    f"'{alias}'"
                )
                sys.exit(1)

            if alias_key in local_aliases:
                print(
                    f"ERROR: duplicate alias inside entry: "
                    f"'{alias}'"
                )
                sys.exit(1)

            local_aliases.add(alias_key)
            batch_aliases.add(alias_key)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    dataset_backup = dataset_path + f".backup_{timestamp}"
    index_backup = INDEX_PATH + f".backup_{timestamp}"

    shutil.copy2(dataset_path, dataset_backup)
    shutil.copy2(INDEX_PATH, index_backup)

    before_count = len(current)

    try:
        current.extend(batch)

        if dataset_is_wrapped:
            dataset_data["entries"] = current
            atomic_write(dataset_path, dataset_data)
        else:
            atomic_write(dataset_path, current)

        dataset["entries"] = len(current)
        atomic_write(INDEX_PATH, index)

        verify_dataset_raw = load_json(dataset_path)
        verify_index = load_json(INDEX_PATH)

        if isinstance(verify_dataset_raw, dict) and isinstance(verify_dataset_raw.get("entries"), list):
            verify_dataset = verify_dataset_raw["entries"]
        elif isinstance(verify_dataset_raw, list):
            verify_dataset = verify_dataset_raw
        else:
            raise RuntimeError("Post-import dataset structure is invalid.")

        topics_after = [
            norm(x.get("topic", ""))
            for x in verify_dataset
        ]

        aliases_after = {}
        for entry in verify_dataset:
            for alias in entry.get("aliases", []):
                key = norm(alias)
                aliases_after.setdefault(key, 0)
                aliases_after[key] += 1

        for entry in batch:
            topic_key = norm(entry["topic"])

            if topics_after.count(topic_key) != 1:
                raise RuntimeError(
                    f"New topic verification failed: {entry['topic']}"
                )

            for alias in entry["aliases"]:
                alias_key = norm(alias)

                if aliases_after.get(alias_key, 0) != 1:
                    raise RuntimeError(
                        f"New alias verification failed: {alias}"
                    )

        index_dataset = next(
            x for x in verify_index["datasets"]
            if x.get("domain") == domain
        )

        if index_dataset.get("entries") != len(verify_dataset):
            raise RuntimeError("Index entry count verification failed.")

    except Exception as exc:
        print(f"ERROR DURING IMPORT: {exc}")
        print("Rolling back dataset and index...")

        shutil.copy2(dataset_backup, dataset_path)
        shutil.copy2(index_backup, INDEX_PATH)

        print("ROLLBACK COMPLETE")
        sys.exit(1)

    print()
    print("IMPORT SUCCESSFUL")
    print(f"Domain: {domain}")
    print(f"Imported entries: {len(batch)}")
    print(f"Entries before:   {before_count}")
    print(f"Entries after:    {len(current)}")
    print(f"New aliases:      {len(batch_aliases)}")
    print("Pre-existing duplicates preserved.")
    print("New topic verification: PASSED")
    print("New alias verification: PASSED")
    print("Index verification: PASSED")
    print("Backups preserved.")

if __name__ == "__main__":
    main()
