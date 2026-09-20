# OfflineBrain local knowledge + dialogue upgrade

This upgrade fixes the UNKNOWN/Verification routing bug and adds a broad deterministic
offline knowledge seed plus a richer dialogue layer.

Important: no finite source file can literally contain all human knowledge. The design
therefore makes the local corpus extensible by domain packs; this package provides the
initial broad corpus and the routing architecture.

Untouched: Gemini/API handling, web/research engines, UI, XAB-Benchmark and Gradle/workflows.
