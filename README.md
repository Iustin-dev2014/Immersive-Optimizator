# Immersive Optimizator

A Minecraft Forge 1.20.1 client-side performance profiling and adaptive graphics mod, targeting Forge 47.2.0 and Java 17.

## Current features (0.3.0-alpha.1)

- Client-only frame-time sampling with a bounded 600-frame window.
- A 10-second diagnostic report with FPS, average frame time, p95 frame time, Java heap used/max, and GC collection/time deltas.
- Adaptive graphics profiles: **Ultra Performance**, **Balanced**, and **Quality**.
- Manual profile selection via the config file when `autoProfile=false`.
- Automatic profile changes use hysteresis: poor performance must persist for two report windows before Ultra Performance is selected; Quality requires three stable windows.
- The Ultra Performance profile can optionally lower render distance to at most 8 chunks. This is disabled by default to preserve the user's 10–12 chunk preference.
- No forced garbage collection and no changes to world/server simulation.
- Unit tests for percentile calculations and profile decision thresholds.

## Configuration

The Forge common config is normally found at `.minecraft/config/immersiveoptimizator-common.toml`.

- `performanceMonitoring=true`: collect diagnostic data and enable automatic decisions.
- `autoProfile=true`: automatically adapt between profiles using measured FPS and p95 frame time.
- `profile="BALANCED"`: initial/manual profile when auto profile is disabled. Values: `ULTRA_PERFORMANCE`, `BALANCED`, `QUALITY`, `CUSTOM`.
- `adaptiveRenderDistance=false`: allow Ultra Performance to reduce render distance to at most 8 chunks. Leave disabled to preserve 10–12 chunks.
- `experimentalOptimizations=false`: reserved for future experimental features.

The profile system changes vanilla graphics options and saves them. Review your current options before enabling aggressive settings. Selecting `CUSTOM` prevents automatic profile changes and does not apply a preset.

## Interpreting measurements

- FPS and frame time vary by hardware, world, resolution, mods, and server.
- Java heap figures are not the same as total process RAM or total system RAM.
- GC metrics are JVM-wide counters, not proof that GC caused a particular stutter.
- Chunk loading/generation is not yet directly instrumented in this release; this is a follow-up task.
- CPU usage is not currently sampled by the mod.
- No fixed FPS, RAM, or CPU percentage is guaranteed.

## Reproducible performance testing

1. Use the same world, location, resolution, resource packs, render distance, and other mods.
2. Let the game warm up for at least two minutes.
3. Record at least five 60-second runs with the same movement/scene.
4. Compare median FPS, p95/p99 frame time, heap used/max, GC deltas, and system CPU usage from an external monitor.
5. Report both improvements and regressions; do not compare a newly generated area with a static scene.

The CI workflow runs unit tests and compiles the Forge mod using Java 17.
