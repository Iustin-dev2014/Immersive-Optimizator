package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.ImmersiveOptimizator;
import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

/** Low-overhead client diagnostics. Metrics are local; nothing is uploaded. */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientPerformanceMonitor {
    private static final int WINDOW_SIZE = 600;
    private static final long REPORT_INTERVAL_NANOS = 10_000_000_000L;
    private static final double[] FRAME_TIMES_MS = new double[WINDOW_SIZE];
    private static int nextIndex;
    private static int sampleCount;
    private static long previousFrameNanos;
    private static long lastReportNanos;
    private static long lastGcCollections;
    private static long lastGcTimeMs;
    private static volatile Snapshot latest = new Snapshot(0, 0, 0, 0, 0, 0, 0, 0);

    public record Snapshot(int fps, double averageFrameMs, double p95FrameMs, long heapUsedMiB,
                          long heapMaxMiB, long gcCollectionsDelta, long gcTimeDeltaMs, int samples) {}

    private ClientPerformanceMonitor() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!OptimizerConfig.PERFORMANCE_MONITORING.get()) {
            previousFrameNanos = 0L; lastReportNanos = 0L; sampleCount = 0; nextIndex = 0;
            return;
        }
        long now = System.nanoTime();
        if (previousFrameNanos != 0L) {
            double frameMs = (now - previousFrameNanos) / 1_000_000.0;
            if (Double.isFinite(frameMs) && frameMs > 0.0 && frameMs < 10_000.0) {
                FRAME_TIMES_MS[nextIndex] = frameMs;
                nextIndex = (nextIndex + 1) % WINDOW_SIZE;
                sampleCount = Math.min(sampleCount + 1, WINDOW_SIZE);
            }
        }
        previousFrameNanos = now;
        if (lastReportNanos == 0L) {
            lastReportNanos = now;
            long[] gc = readGcTotals();
            lastGcCollections = gc[0]; lastGcTimeMs = gc[1];
            return;
        }
        if (now - lastReportNanos >= REPORT_INTERVAL_NANOS) report(now);
    }

    public static Snapshot latest() { return latest; }

    private static void report(long now) {
        Minecraft minecraft = Minecraft.getInstance();
        Runtime runtime = Runtime.getRuntime();
        long usedMiB = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMiB = runtime.maxMemory() / (1024L * 1024L);
        double averageMs = 0.0, p95Ms = 0.0;
        if (sampleCount > 0) {
            double[] samples = new double[sampleCount];
            System.arraycopy(FRAME_TIMES_MS, 0, samples, 0, sampleCount);
            Arrays.sort(samples);
            double total = 0.0;
            for (double sample : samples) total += sample;
            averageMs = total / samples.length;
            p95Ms = PerformanceMath.percentile(samples, 0.95);
        }
        long[] gc = readGcTotals();
        long gcCollectionsDelta = Math.max(0L, gc[0] - lastGcCollections);
        long gcTimeDelta = Math.max(0L, gc[1] - lastGcTimeMs);
        int fps = minecraft.getFps();
        latest = new Snapshot(fps, round2(averageMs), round2(p95Ms), usedMiB, maxMiB,
                gcCollectionsDelta, gcTimeDelta, sampleCount);
        ImmersiveOptimizator.LOGGER.info(
                "Performance sample (10s): fps={}, frameAvgMs={}, frameP95Ms={}, heapMiB={}/{}, gcCollectionsDelta={}, gcTimeDeltaMs={}, samples={}, logicalProcessors={}, profile={}",
                fps, round2(averageMs), round2(p95Ms), usedMiB, maxMiB,
                gcCollectionsDelta, gcTimeDelta, sampleCount, runtime.availableProcessors(),
                ClientOptimizationController.getActiveProfile());
        ClientOptimizationController.onPerformanceSample(fps, p95Ms);
        lastGcCollections = gc[0]; lastGcTimeMs = gc[1]; lastReportNanos = now;
    }

    public static Path exportReport() throws Exception {
        if (!OptimizerConfig.EXPORT_DIAGNOSTICS.get()) throw new IllegalStateException("Diagnostic export is disabled in config.");
        Minecraft minecraft = Minecraft.getInstance();
        Runtime runtime = Runtime.getRuntime();
        Snapshot s = latest;
        String report = """
                IMMERSIVE OPTIMIZATOR - LOCAL DIAGNOSTIC REPORT
                Generated: %s
                Minecraft: 1.20.1
                Mod version: 0.4.0-alpha.1
                OS: %s (%s)
                Java: %s
                Logical processors: %d
                JVM heap used/max MiB: %d/%d
                Profile: %s
                Automatic profile: %s
                Turbo: %s
                Render distance: %d chunks
                Scene: %s
                FPS: %d
                Average frame time: %.2f ms
                P95 frame time: %.2f ms
                GC collections in last window: %d
                GC time delta in last window: %d ms
                Frame samples in window: %d

                Notes:
                - Heap memory is not total system RAM.
                - FPS and frame time depend on hardware, world, settings and other mods.
                - This report is generated locally and is not uploaded automatically.
                """.formatted(Instant.now(), System.getProperty("os.name"),
                System.getProperty("os.arch"), System.getProperty("java.version"),
                runtime.availableProcessors(), s.heapUsedMiB(), s.heapMaxMiB(),
                ClientOptimizationController.getActiveProfile(), OptimizerConfig.AUTO_PROFILE.get(),
                ClientOptimizationController.isTurboEnabled(), minecraft.options.renderDistance().get(),
                minecraft.hasSingleplayerServer() ? "Single-player / integrated server" : "Multiplayer or menu",
                s.fps(), s.averageFrameMs(), s.p95FrameMs(), s.gcCollectionsDelta(),
                s.gcTimeDeltaMs(), s.samples());
        Path directory = minecraft.gameDirectory.toPath().resolve("immersive-optimizator-reports");
        Files.createDirectories(directory);
        Path output = directory.resolve("report-" + System.currentTimeMillis() + ".txt");
        Files.writeString(output, report, StandardCharsets.UTF_8);
        return output;
    }

    private static long[] readGcTotals() {
        long collections = 0L, timeMs = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() > 0L) collections += bean.getCollectionCount();
            if (bean.getCollectionTime() > 0L) timeMs += bean.getCollectionTime();
        }
        return new long[]{collections, timeMs};
    }
    private static double round2(double value) { return Math.round(value * 100.0) / 100.0; }
}
