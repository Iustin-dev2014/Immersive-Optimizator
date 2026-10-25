# Immersive Optimizator

Low-overhead performance diagnostics and opt-in optimization foundations for **Minecraft Java Edition 1.20.1**, **Minecraft Forge 47.2.0**, and **Java 17**.

> **Development status: Phase 2 — diagnostics, not an optimization claim.** This version periodically logs client FPS, sampled frame-time average and p95, JVM heap use, and garbage-collection deltas. It observes performance only; it does not change rendering, world simulation, or JVM settings. No FPS improvement is claimed.

## Requirements

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.0
- JDK 17 (for development)
- Internet access on the first Gradle build so Gradle and Forge dependencies can be downloaded

## Build and test

Linux/macOS:
```sh
chmod +x ./gradlew
./gradlew --no-daemon clean test build
```

Windows:
```bat
gradlew.bat --no-daemon clean test build
```

The mod JAR is written to `build/libs/`. Install it in a Forge 1.20.1 instance with the matching Forge version.

The Gradle JVM setting in `gradle.properties` controls **the build process only**; it is not an in-game RAM limit.

## Phase 2: what is measured

When `performanceMonitoring=true`, every 10 seconds the client log receives one `Performance sample (10s)` line with:
- `fps`: Minecraft's current FPS reading at the report point.
- `frameAvgMs`: average of the most recent sampled render-frame intervals.
- `frameP95Ms`: 95th-percentile frame interval; lower is generally smoother.
- `heapMiB`: currently used heap / maximum heap visible to the JVM.
- `gcCollectionsDelta` and `gcTimeDeltaMs`: garbage-collection activity since the prior report.
- `samples`: number of valid frame intervals in the rolling window (maximum 600).

Frame-time samples are bounded to 600 entries. The monitor logs only once per 10 seconds and performs no optimization. Set `performanceMonitoring=false` in the generated Forge config file to disable sampling. These metrics help identify likely bottlenecks; they do not independently prove whether the CPU, GPU, disk, server, or a specific mod is responsible.

## Reproducible baseline protocol

1. Use the same Minecraft version, Forge version, world, render distance, simulation distance, resolution, resource packs, shaders, and power mode for each run.
2. Restart Minecraft between runs. Let the world settle for 2 minutes, then collect at least 6 consecutive 10-second log samples while standing in the same location.
3. Repeat the same route/action for chunk-loading tests. Do not compare a newly generating world against a previously generated one.
4. Record median FPS, median `frameAvgMs`, median `frameP95Ms`, heap range, and GC time. Compare at least three runs per configuration.
5. Compare the same instance with this mod's monitoring enabled and disabled to estimate diagnostic overhead. For later optimization builds, compare control vs. enabled feature with only one setting changed at a time.

### Acceptance gates before any optimization is called successful

- Automated CI: `./gradlew --no-daemon clean test build` passes.
- Measurement overhead: target less than 1% median FPS change and no repeatable p95 frame-time regression above 2% when diagnostics are enabled. If this gate is not met, reduce sampling/reporting overhead.
- Performance claim: require at least three paired runs and report median and spread, not one best result. Treat changes below run-to-run noise as inconclusive.
- Stability: no new crashes, visual defects, gameplay changes, or compatibility regressions in the tested scenario.

These are test targets, not results already achieved.

## Phase plan

1. **Build foundation:** complete.
2. **Diagnostics foundation:** client frame-time/FPS and JVM memory/GC snapshots, plus unit tests for percentile calculations. Current phase.
3. **Profiling expansion:** client tick duration, loading/chunk symptoms, startup and server tick diagnostics where available.
4. **Profiles and GUI:** Low-End, Balanced, High Performance and Custom profiles with clear user controls.
5. **Safe optimizations:** individually switchable changes, compatibility checks and reproducible A/B benchmarks.
6. **Opt-in experiments:** advanced scheduling/rendering/memory/entity experiments only after correctness, stress and rollback testing.
7. **Stabilization:** broader modpack matrix, documentation, regression testing and release criteria.

## Safety and compatibility principles

- Experimental behavior is disabled by default.
- Never promise a universal FPS target; gains depend on hardware, drivers, world, shaders and installed mods.
- Do not mutate Minecraft world or rendering state unsafely from background threads.
- Do not force garbage collection in the game loop or silently change gameplay simulation.
- Client-side diagnostics cannot repair TPS problems on a remote server.
- Each future optimization should be independently configurable, measured, and reversible.

## Continuous integration

GitHub Actions runs `./gradlew --no-daemon clean test build` for pushes and pull requests to `main`, and on manual dispatch. A successful run uploads the compiled JAR as an artifact.

## License

MIT. See [LICENSE](LICENSE).
