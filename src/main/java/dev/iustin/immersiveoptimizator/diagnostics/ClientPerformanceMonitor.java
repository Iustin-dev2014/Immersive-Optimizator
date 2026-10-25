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
import java.util.Arrays;

/** Low-overhead client-only diagnostics; it does not mutate JVM or world state. */
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

    private ClientPerformanceMonitor() {
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!OptimizerConfig.PERFORMANCE_MONITORING.get()) {
            previousFrameNanos = 0L;
            lastReportNanos = 0L;
            sampleCount = 0;
            nextIndex = 0;
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
            lastGcCollections = gc[0];
            lastGcTimeMs = gc[1];
            return;
        }

        if (now - lastReportNanos >= REPORT_INTERVAL_NANOS) {
            report(now);
        }
    }

    private static void report(long now) {
        Minecraft minecraft = Minecraft.getInstance();
        Runtime runtime = Runtime.getRuntime();
        long usedMiB = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMiB = runtime.maxMemory() / (1024L * 1024L);
        double averageMs = 0.0;
        double p95Ms = 0.0;

        if (sampleCount > 0) {
            double[] samples = new double[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                samples[i] = FRAME_TIMES_MS[i];
            }
            Arrays.sort(samples);
            double total = 0.0;
            for (double sample : samples) {
                total += sample;
            }
            averageMs = total / samples.length;
            p95Ms = PerformanceMath.percentile(samples, 0.95);
        }

        long[] gc = readGcTotals();
        long gcCollectionsDelta = Math.max(0L, gc[0] - lastGcCollections);
        long gcTimeDelta = Math.max(0L, gc[1] - lastGcTimeMs);
        int fps = minecraft.getFps();

        ImmersiveOptimizator.LOGGER.info(
                "Performance sample (10s): fps={}, frameAvgMs={}, frameP95Ms={}, heapMiB={}/{}, gcCollectionsDelta={}, gcTimeDeltaMs={}, samples={}",
                fps, round2(averageMs), round2(p95Ms), usedMiB, maxMiB,
                gcCollectionsDelta, gcTimeDelta, sampleCount);
        ClientOptimizationController.onPerformanceSample(fps, p95Ms);

        lastGcCollections = gc[0];
        lastGcTimeMs = gc[1];
        lastReportNanos = now;
    }

    private static long[] readGcTotals() {
        long collections = 0L;
        long timeMs = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() > 0L) {
                collections += bean.getCollectionCount();
            }
            if (bean.getCollectionTime() > 0L) {
                timeMs += bean.getCollectionTime();
            }
        }
        return new long[]{collections, timeMs};
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
