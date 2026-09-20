# XAB-1 — Xilon AI Assistant Benchmark

External, reproducible benchmark for the AI Assistant.

## Isolation

**XAB-1 does not modify `AIAssistantApp/`.** The Assistant remains the system under test.

## Categories

Factuality, Research, Web, Reasoning, Coding, Memory and Agentic. Version 1 contains 35 cases, five per category.

## Protocol

1. Freeze the benchmark commit.
2. Record Assistant version, configuration and model.
3. Use the exact prompts from `cases.json`.
4. Use a fresh Assistant session for each independent case.
5. Record raw answers in JSONL.
6. Score with `runner.py`.
7. Preserve the report with the benchmark commit SHA.

The benchmark is external and is not part of the Assistant runtime.
