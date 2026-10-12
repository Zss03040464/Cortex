# Cortex: Native Embodied Mobile Agent System for Android

> **Cortex is a native embodied agent system built specifically for Android mobile devices. By fusing an advanced reasoning Agent brain with an internal native Debian container, ColorOS Mirage headless virtual displays, and full-duplex NDJSON event streaming, Cortex gives your mobile device real autonomous cognition, perception, and closed-loop task execution capabilities.**

---

## 🌟 Core Architecture & Innovations

1. **Single Brain Embodied Architecture**:
   - Replaces convoluted multi-layer orchestration with a single, authoritative Agent decision center.
   - The Cortex decision core drives reasoning, planning, tool execution, and live verification directly.
   - The Android host provides high-precision system APIs, accessibility execution, and hardware awareness.

2. **Full-Duplex Streaming & Step Decomposition**:
   - **Streaming NDJSON Pipeline**: Real-time parsing of LLM token streams and thinking blocks.
   - **Structured Layout**: Visually partitions upfront intentions, collapsible tool invocation cards, and factual conclusions.
   - **Persistent Record Auditing**: Authoritative post-run record sync ensures complete traceability of thought chains.

3. **ColorOS Mirage Headless Virtual Display**:
   - **Underlying PC Interconnect Injection**: Built upon ColorOS native system service `OplusMirageWindowManager`.
   - **Zero Disturbance on Display 0**: All GUI automations run cleanly on an isolated virtual display (Display ID >= 10000). No focus stealing, no soft keyboard popups, zero interference with daily user interactions.
   - **Anti-Collusion Safety Guardrail**: Automatically yields whenever the user actively uses the target app on Display 0.

4. **Headless Linux Container & Interactive PTY Terminal**:
   - **Kernel Namespace Isolation**: `run_chroot.sh` with private unshare mount spaces guarantees 0 mount leakages.
   - **Complete Local Runtime**: Pre-installed Node.js, Python, and CLI toolchains enable completely offline agent tasks without PC cables.
   - **Full-featured PTY Terminal Dock**: Supports TUI apps, custom mobile shortcut bars, ANSI colors, and background daemon monitoring.

5. **Cross-Boundary File Gateway**:
   - Transparent mapping and bi-directional resolution between Android host storage and internal container chroot rootfs (`/root/...`).
   - Frictionless attachment of images, SAF document imports, and directory references.
