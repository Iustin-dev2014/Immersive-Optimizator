package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig.PerformanceProfile;

/** Pure, testable hysteresis policy for choosing an adaptive graphics profile. */
public final class ProfileDecision {
    private ProfileDecision() {
    }

    public static PerformanceProfile choose(
            PerformanceProfile current,
            int fps,
            double frameP95Ms,
            int lowPerformanceWindows,
            int stableWindows) {
        if (current == null || current == PerformanceProfile.CUSTOM) {
            return current;
        }
        if (fps < 30 || frameP95Ms > 40.0) {
            return lowPerformanceWindows >= 2 ? PerformanceProfile.ULTRA_PERFORMANCE : current;
        }
        if (fps < 50 || frameP95Ms > 25.0) {
            return PerformanceProfile.BALANCED;
        }
        if (fps >= 55 && frameP95Ms > 0.0 && frameP95Ms < 20.0 && stableWindows >= 3) {
            return PerformanceProfile.QUALITY;
        }
        return current;
    }
}
