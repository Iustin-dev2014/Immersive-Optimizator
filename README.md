# Immersive Optimizator

Client-side optimization controls and diagnostics for Minecraft Java Edition 1.20.1, Forge 47.2.0, Java 17.

## Version 0.5.0-alpha.1

- Adds an **Optimization Settings** button directly to the vanilla **Video Settings** screen using Forge's `ScreenEvent.Init.Post` event; the vanilla screen is not replaced.
- Provides a multi-page control panel: Home, Performance, Video, Diagnostics, Mobile, and Mods.
- Profiles: Eco, Balanced, Performance, Ultra, plus Quality/Custom compatibility values.
- Hardware startup heuristic based on logical processor count and maximum JVM heap; adaptive choices use FPS and p95 frame time.
- Configurable critical FPS/frame-time thresholds to tune how aggressively the automatic policy reacts.
- Separate manual profiles for single-player and multiplayer can be enabled.
- Configurable adaptive render-distance caps for Eco and Ultra; disabling adaptive distance stops the mod from reducing render distance.
- Low-end/mobile preset for devices that benefit from a strict graphics profile; this is a manual preset, not Android hardware detection.
- Local export report includes Java/OS info, logical processors, FPS/frame-time metrics, heap/GC counters, active settings, and a short known-conflict summary.
- Presence checks for Embeddium, ModernFix, FerriteCore and ImmediatelyFast, plus warnings for a few known overlapping/unsupported combinations.
- No forced garbage collection, no edits to world saves, no changes to world/server simulation, and no automatic network telemetry.

## Open the panel

Open **Options → Video Settings → Optimization Settings**. The key binding **O** also opens the panel in clients where a keyboard input is available. Standard Minecraft buttons are used so the interface can be operated by touch in Android Java launchers that successfully run this Forge version.

## Settings

The Forge common configuration is normally located at `.minecraft/config/immersiveoptimizator-common.toml`.

- `autoProfile`: automatic adaptive profile selection.
- `profile`: default/manual profile.
- `useContextProfiles`, `singleplayerProfile`, `multiplayerProfile`: separate manual profiles by play context (used when automatic mode is off).
- `mobileLowEndMode`: sticky Eco preset for low-end devices.
- `adaptiveRenderDistance`, `ecoRenderDistanceCap`, `ultraRenderDistanceCap`: render-distance management.
- `lowFpsThreshold`, `highFrameTimeThresholdMs`: automatic policy thresholds.
- `performanceMonitoring`: collects client-side FPS/frame-time and JVM metrics.
- `exportDiagnostics`: controls report export.
- `compatibilityWarnings`: enables the built-in set of compatibility warnings.

## Real-world optimization notes

This mod applies supported vanilla graphics settings and provides diagnostics; it does not yet replace Minecraft's terrain renderer or rewrite chunk generation. It reports JVM heap, not total device RAM, and it cannot read every GPU, driver, thermal or memory-pressure signal on every platform.

For Forge 1.20.1, users may separately evaluate compatible versions of:
- **Embeddium** for a rewritten terrain/rendering pipeline.
- **ModernFix** for load-time, memory and general performance fixes.
- **FerriteCore** for memory-use reduction.
- **ImmediatelyFast** for additional rendering-pipeline optimizations (choose a Forge-compatible build).

These are optional, separately distributed mods. This project does not download or bundle them. Check each project's current version/loader compatibility before installation. Avoid combinations that overlap, such as Embeddium/Rubidium or Embeddium/OptiFine, and test one change at a time. The built-in scan detects only a few known patterns; it is not a complete mod-conflict detector.

Android compatibility depends on the Java launcher, Forge installation, device GPU/driver, renderer backend, available memory and thermal limits. A mod can expose settings and reduce graphics workload, but cannot guarantee a particular FPS on every phone. The Low-end/Mobile page is available for manual tuning when the launcher supports Forge 1.20.1 mods.

## Benchmark method

1. Back up worlds before testing a new modpack or changing loaders.
2. Use the same world, location, resolution, shaders/resource packs and view direction.
3. Warm up for two minutes and run at least five 60-second tests.
4. Compare median FPS and p95/p99 frame time, not a single peak FPS.
5. Compare before/after with the same other mods. Report any regressions as well as gains.

GitHub Actions runs unit tests and compiles the Forge mod. CI success confirms compilation/tests, not an FPS improvement on every device.
