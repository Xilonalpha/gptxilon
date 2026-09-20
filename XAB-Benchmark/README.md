# XAB-1 — Xilon AI Assistant Benchmark

External, reproducible benchmark for the AI Assistant.

## Isolation

**XAB-1 does not modify `AIAssistantApp/`.** The Assistant remains the system under test.

## Categories

Factuality, Research, Web, Reasoning, Coding, Memory and Agentic. Version 1.1 contains 35 cases, five per category.

## Protocol

1. Freeze the benchmark commit.
2. Record Assistant version, configuration and model.
3. Use the exact prompts from `cases.json`.
4. Use a fresh Assistant session for each independent case.
5. **Memory sequence exception:** `MEM-001`, `MEM-002` and `MEM-003` are one ordered test sequence and must run in the **same Assistant session**, in that exact order.
6. Record raw answers in JSONL.
7. Score with `runner.py`.
8. Preserve the report with the benchmark commit SHA.

## Scoring

Scores are 0–2 per case.

A missing output is **not** a score of zero. It is reported as `missing`.

An invalid/unscored value is reported as `manual_review_required`.

The overall `score` is populated only when every case has a valid score. For incomplete runs, `partial_score` is shown only as an interim diagnostic and must not be treated as the official benchmark score.

The benchmark is external and is not part of the Assistant runtime.
