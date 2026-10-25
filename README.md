# Immersive Optimizator

A client-side performance diagnostics and adaptive graphics mod for Minecraft Forge 1.20.1 (Forge 47.2.0, Java 17).

## Version 0.4.0-alpha.1

- Hardware-aware startup profile using logical processor count and maximum JVM heap.
- Profiles: **Eco**, **Balanced**, **Performance**, and **Ultra** (plus Custom/Quality compatibility values).
- Automatic profile decisions based on FPS and p95 frame time, with hysteresis to avoid rapid switching.
- In-game dashboard on **O** with profile cycling, automatic mode, adaptive render distance, Turbo preset, and report export.
- Local diagnostic reports include OS/architecture, Java version, logical processors, heap figures, active profile, render distance, FPS/frame time and GC deltas.
- Adaptive render distance is enabled by default for Eco/Ultra; Eco targets 4–6 chunks and Ultra caps at 8 chunks.
- Profile application avoids saving options unless values changed.
- No forced garbage collection, no world-save edits, no changes to world/server simulation, and no automatic network telemetry.

## Controls

Press **O** in-game to open the dashboard. Use the Minecraft Controls menu to change the key binding.

The **Export report** button writes a text file under the immersive-optimizator-reports directory in the Minecraft game directory. Reports stay local and are not uploaded automatically.

## Configuration

The Forge common config is normally at .minecraft/config/immersiveoptimizator-common.toml.

- performanceMonitoring=true: collect diagnostic data and enable automatic decisions.
- autoProfile=true: adapt profiles based on frame-time metrics.
- profile="BALANCED": manual profile when auto mode is disabled. Values include ECO, BALANCED, PERFORMANCE, ULTRA_PERFORMANCE, QUALITY, CUSTOM.
- adaptiveRenderDistance=true: allow Eco/Ultra profiles to reduce render distance.
- exportDiagnostics=true: enable local report export.
- experimentalOptimizations=false: reserved for future engine-level changes; no unsafe world changes are performed.

## Important limits

- The mod currently applies vanilla graphics settings; it does not replace Minecraft's renderer or rewrite chunk-generation internals.
- Hardware detection is a heuristic based on logical processor count and maximum JVM heap, not a full GPU benchmark or a reading of total physical RAM.
- JVM heap is not the same as total system memory.
- FPS depends on hardware, resolution, world, mods, resource packs, drivers, and server conditions. Improvements must be measured on comparable runs.
- Turbo is a graphics preset, not CPU/GPU overclocking. No network telemetry is uploaded.

## Reproducible testing

1. Use the same world, location, resolution, resource packs, render distance and other mods.
2. Let the game warm up for two minutes.
3. Record at least five 60-second runs with the same movement/scene.
4. Compare median FPS, p95/p99 frame time, heap, GC deltas and external system CPU usage.
5. Report both improvements and regressions. CI validates compilation and unit tests, not real-world FPS gains.
