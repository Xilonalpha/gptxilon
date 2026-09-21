#!/usr/bin/env python3

import json
import os
import shutil
import sys
from datetime import datetime
import unicodedata

BASE = "AIAssistantApp/app/src/main/assets"
INDEX_PATH = os.path.join(BASE, "knowledge_index.json")
BATCH_PATH = "knowledge_expansion/history_batch_001.json"


def norm(text):
    text = str(text).strip().lower()
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    return " ".join(text.split())


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(path, data):
    tmp = path + ".tmp"

    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

    os.replace(tmp, path)


print("=" * 72)
print("KNOWLEDGE BATCH IMPORTER")
print("=" * 72)

if not os.path.exists(INDEX_PATH):
    print("ERROR: knowledge_index.json not found")
    sys.exit(1)

if not os.path.exists(BATCH_PATH):
    print(f"ERROR: batch not found: {BATCH_PATH}")
    sys.exit(1)


index = load_json(INDEX_PATH)
batch = load_json(BATCH_PATH)

if not isinstance(batch, list):
    batch = batch.get("entries", [])

if not isinstance(batch, list):
    print("ERROR: batch must be a JSON array or contain 'entries'")
    sys.exit(1)

print(f"Batch entries: {len(batch)}")

if len(batch) != 59:
    print(f"ERROR: expected exactly 59 entries, found {len(batch)}")
    sys.exit(1)


# ------------------------------------------------------------------
# Locate history dataset
# ------------------------------------------------------------------

history_dataset = None

for dataset in index.get("datasets", []):
    if dataset.get("domain") == "history":
        history_dataset = dataset
        break

if history_dataset is None:
    print("ERROR: history dataset not found")
    sys.exit(1)

history_file = history_dataset.get("file")

if not history_file:
    print("ERROR: history dataset has no file")
    sys.exit(1)

history_path = os.path.join(BASE, history_file)

if not os.path.exists(history_path):
    print(f"ERROR: history file not found: {history_path}")
    sys.exit(1)

print(f"History file: {history_path}")


# ------------------------------------------------------------------
# Load current history
# ------------------------------------------------------------------

history_data = load_json(history_path)

if isinstance(history_data, list):
    history_entries = history_data
    history_is_list = True
else:
    history_entries = history_data.get("entries", [])
    history_is_list = False

if not isinstance(history_entries, list):
    print("ERROR: history dataset has invalid structure")
    sys.exit(1)

print(f"Existing history entries: {len(history_entries)}")


# ------------------------------------------------------------------
# Build existing indexes
# ------------------------------------------------------------------

existing_topics = {}
existing_aliases = {}

preexisting_alias_duplicates = {}

for entry in history_entries:

    topic = norm(entry.get("topic", ""))

    if topic:
        if topic in existing_topics:
            print(
                f"WARNING: pre-existing duplicate topic: "
                f"{entry.get('topic')}"
            )

        existing_topics.setdefault(
            topic,
            entry.get("topic", "")
        )

    for alias in entry.get("aliases", []):

        a = norm(alias)

        if not a:
            continue

        if a in existing_aliases:
            old_topic, old_alias = existing_aliases[a]

            preexisting_alias_duplicates.setdefault(
                a,
                []
            ).append(
                (old_topic, old_alias)
            )

            preexisting_alias_duplicates[a].append(
                (entry.get("topic", ""), alias)
            )

        else:
            existing_aliases[a] = (
                entry.get("topic", ""),
                alias
            )


if preexisting_alias_duplicates:
    print(
        f"Pre-existing normalized alias duplicates: "
        f"{len(preexisting_alias_duplicates)}"
    )

    for alias, matches in preexisting_alias_duplicates.items():
        print(
            f"  PRE-EXISTING DUPLICATE: {alias!r}"
        )


# ------------------------------------------------------------------
# Validate the NEW batch only
# ------------------------------------------------------------------

batch_topics = set()
batch_aliases = set()

for i, entry in enumerate(batch, start=1):

    prefix = f"ENTRY {i}"

    if not isinstance(entry, dict):
        print(f"ERROR: {prefix} is not an object")
        sys.exit(1)

    for field in ["topic", "aliases", "answer", "domain"]:
        if field not in entry:
            print(
                f"ERROR: {prefix} missing field '{field}'"
            )
            sys.exit(1)

    if entry.get("domain") != "history":
        print(
            f"ERROR: {prefix} '{entry.get('topic')}' "
            f"has domain '{entry.get('domain')}', "
            f"expected 'history'"
        )
        sys.exit(1)

    topic = norm(entry.get("topic", ""))

    if not topic:
        print(f"ERROR: {prefix} has empty topic")
        sys.exit(1)

    if topic in existing_topics:
        print(
            f"ERROR: new topic already exists: "
            f"{entry.get('topic')}"
        )
        sys.exit(1)

    if topic in batch_topics:
        print(
            f"ERROR: duplicate topic inside batch: "
            f"{entry.get('topic')}"
        )
        sys.exit(1)

    batch_topics.add(topic)

    aliases = entry.get("aliases", [])

    if not isinstance(aliases, list) or len(aliases) < 6:
        print(
            f"ERROR: {prefix} '{entry.get('topic')}' "
            f"must have at least 6 aliases"
        )
        sys.exit(1)

    local_aliases = set()

    for alias in aliases:

        a = norm(alias)

        if not a:
            print(
                f"ERROR: empty alias in "
                f"'{entry.get('topic')}'"
            )
            sys.exit(1)

        if a in local_aliases:
            print(
                f"ERROR: duplicate alias inside "
                f"'{entry.get('topic')}': {alias}"
            )
            sys.exit(1)

        local_aliases.add(a)

        # Existing aliases are forbidden for NEW entries.
        if a in existing_aliases:
            old_topic, old_alias = existing_aliases[a]

            print(
                f"ERROR: new alias already exists in KB: "
                f"'{alias}' -> '{old_topic}'"
            )
            sys.exit(1)

        if a in batch_aliases:
            print(
                f"ERROR: alias duplicated inside batch: "
                f"'{alias}'"
            )
            sys.exit(1)

        batch_aliases.add(a)


print("Pre-import validation: PASSED")


# ------------------------------------------------------------------
# Backup
# ------------------------------------------------------------------

timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

history_backup = history_path + f".backup_{timestamp}"
index_backup = INDEX_PATH + f".backup_{timestamp}"

shutil.copy2(history_path, history_backup)
shutil.copy2(INDEX_PATH, index_backup)

print(f"History backup: {history_backup}")
print(f"Index backup:   {index_backup}")


# ------------------------------------------------------------------
# Import
# ------------------------------------------------------------------

new_history_entries = history_entries + batch

if history_is_list:
    new_history_data = new_history_entries
else:
    new_history_data = dict(history_data)
    new_history_data["entries"] = new_history_entries


new_index = dict(index)
new_datasets = []

for dataset in index.get("datasets", []):

    d = dict(dataset)

    if d.get("domain") == "history":
        d["entries"] = len(new_history_entries)

    new_datasets.append(d)

new_index["datasets"] = new_datasets


# Atomic writes
save_json(history_path, new_history_data)
save_json(INDEX_PATH, new_index)


# ------------------------------------------------------------------
# Post-import verification
# ------------------------------------------------------------------

verify_history = load_json(history_path)

if isinstance(verify_history, list):
    verify_entries = verify_history
else:
    verify_entries = verify_history.get("entries", [])


expected_count = len(history_entries) + len(batch)

if len(verify_entries) != expected_count:

    print(
        f"ERROR: post-import count mismatch. "
        f"Expected {expected_count}, "
        f"found {len(verify_entries)}"
    )

    shutil.copy2(history_backup, history_path)
    shutil.copy2(index_backup, INDEX_PATH)

    print("ROLLBACK COMPLETED")
    sys.exit(1)


# Verify every NEW topic exists exactly once.
final_topics = {}

for entry in verify_entries:

    topic = norm(entry.get("topic", ""))

    if topic:
        final_topics.setdefault(topic, 0)
        final_topics[topic] += 1


for topic in batch_topics:

    if final_topics.get(topic, 0) != 1:

        print(
            f"ERROR: imported topic verification failed: "
            f"{topic}"
        )

        shutil.copy2(history_backup, history_path)
        shutil.copy2(index_backup, INDEX_PATH)

        print("ROLLBACK COMPLETED")
        sys.exit(1)


# Verify every NEW alias exists exactly once.
final_aliases = {}

for entry in verify_entries:

    for alias in entry.get("aliases", []):

        a = norm(alias)

        if a:
            final_aliases.setdefault(a, 0)
            final_aliases[a] += 1


for alias in batch_aliases:

    if final_aliases.get(alias, 0) != 1:

        print(
            f"ERROR: imported alias verification failed: "
            f"{alias}"
        )

        shutil.copy2(history_backup, history_path)
        shutil.copy2(INDEX_PATH, INDEX_PATH)

        print("ROLLBACK COMPLETED")
        sys.exit(1)


print()
print("=" * 72)
print("IMPORT SUCCESSFUL")
print("=" * 72)
print(f"Imported entries: {len(batch)}")
print(f"History before:   {len(history_entries)}")
print(f"History after:    {len(verify_entries)}")
print(f"New aliases:      {len(batch_aliases)}")
print()
print("Pre-existing duplicates preserved.")
print("New topic verification: PASSED")
print("New alias verification: PASSED")
print("Backups preserved.")
print("=" * 72)
