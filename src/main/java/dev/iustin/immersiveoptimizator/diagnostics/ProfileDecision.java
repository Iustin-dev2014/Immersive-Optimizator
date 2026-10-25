package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import dev.iustin.immersiveoptimizator.config.OptimizerConfig.PerformanceProfile;

/** Pure, testable policy for choosing an adaptive graphics profile. */
public final class ProfileDecision {
    private ProfileDecision() {}

    /**
     * Conservative initial heuristic. Heap size and logical processors are signals,
     * not a full benchmark of the physical RAM, GPU, thermals or drivers.
     */
    public static PerformanceProfile hardwareBaseline(int logicalProcessors, long maxHeapBytes) {
        if (logicalProcessors <= 2 || maxHeapBytes <= 2L * 1024L * 1024L * 1024L) {
            return PerformanceProfile.ULTRA_PERFORMANCE;
        }
        if (logicalProcessors <= 4 || maxHeapBytes <= 4L * 1024L * 1024L * 1024L) {
            return PerformanceProfile.PERFORMANCE;
        }
        return PerformanceProfile.BALANCED;
    }

    public static PerformanceProfile choose(
            PerformanceProfile current,
            int fps,
            double frameP95Ms,
            int lowPerformanceWindows,
            int stableWindows) {
        if (current == null || current == PerformanceProfile.CUSTOM || current == PerformanceProfile.ECO) {
            return current;
        }

        int lowFps = OptimizerConfig.LOW_FPS_THRESHOLD.get();
        double criticalFrameMs = OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.get();

        if (fps < lowFps || frameP95Ms > criticalFrameMs) {
            return lowPerformanceWindows >= 2 ? PerformanceProfile.ULTRA_PERFORMANCE : current;
        }

        if (fps < lowFps + 20 || frameP95Ms > criticalFrameMs * 0.625D) {
            return current == PerformanceProfile.ULTRA_PERFORMANCE
                    ? PerformanceProfile.PERFORMANCE : current;
        }

        if (fps >= lowFps + 25 && frameP95Ms > 0.0D
                && frameP95Ms < criticalFrameMs * 0.5D && stableWindows >= 3) {
            return current == PerformanceProfile.ULTRA_PERFORMANCE
                    ? PerformanceProfile.PERFORMANCE : PerformanceProfile.BALANCED;
        }
        return current;
    }
}
