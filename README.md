# Immersive Optimizator

Adaptive performance diagnostics and opt-in optimization foundations for **Minecraft Java Edition 1.20.1**, **Minecraft Forge 47.2.0**, and **Java 17**.

> **Development status: Phase 1 — build foundation.** This repository does not yet claim FPS improvements. The first phase establishes a real Forge project, conservative configuration defaults, and a repeatable CI build. Optimization systems will be added and benchmarked in later phases.

## Requirements

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.0
- JDK 17 (for development)
- Internet access on the first Gradle build so Gradle and Forge dependencies can be downloaded

## Build

Linux/macOS:
```sh
chmod +x ./gradlew
./gradlew --no-daemon clean build
```

Windows:
```bat
gradlew.bat --no-daemon clean build
```

The mod JAR is written to `build/libs/`. Install it in a Forge 1.20.1 instance with the matching Forge version.

The Gradle JVM setting in `gradle.properties` controls **the build process only**; it is not an in-game RAM limit.

## Phase plan

1. **Build foundation (current):** ForgeGradle, Gradle Wrapper, Java 17, metadata, conservative config, CI artifact.
2. **Profiling and benchmarks:** frame-time/1% low reporting where available, memory snapshots, startup/loading and server tick diagnostics, with explicit overhead checks.
3. **Profiles and configuration:** Low-End, Balanced, High Performance and Custom profiles, validated persistence and reset.
4. **Monitoring and GUI:** bounded-cost sampling, hysteresis to avoid profile oscillation, clear status and user controls.
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

GitHub Actions runs `./gradlew --no-daemon clean build` for pushes and pull requests to `main`, and on manual dispatch. A successful run uploads the compiled JAR as an artifact.

## License

MIT. See [LICENSE](LICENSE).
