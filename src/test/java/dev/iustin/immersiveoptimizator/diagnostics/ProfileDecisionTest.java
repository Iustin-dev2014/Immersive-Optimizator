package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig.PerformanceProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileDecisionTest {
    @Test
    void requiresRepeatedBadWindowsBeforeDroppingToUltraPerformance() {
        assertEquals(PerformanceProfile.BALANCED,
                ProfileDecision.choose(PerformanceProfile.BALANCED, 25, 45.0, 1, 0));
        assertEquals(PerformanceProfile.ULTRA_PERFORMANCE,
                ProfileDecision.choose(PerformanceProfile.BALANCED, 25, 45.0, 2, 0));
    }

    @Test
    void selectsBalancedWhenPerformanceIsBelowTarget() {
        assertEquals(PerformanceProfile.BALANCED,
                ProfileDecision.choose(PerformanceProfile.QUALITY, 45, 22.0, 0, 0));
    }

    @Test
    void upgradesOnlyAfterRepeatedStableWindows() {
        assertEquals(PerformanceProfile.BALANCED,
                ProfileDecision.choose(PerformanceProfile.BALANCED, 80, 16.0, 0, 2));
        assertEquals(PerformanceProfile.QUALITY,
                ProfileDecision.choose(PerformanceProfile.BALANCED, 80, 16.0, 0, 3));
    }

    @Test
    void preservesCustomProfile() {
        assertEquals(PerformanceProfile.CUSTOM,
                ProfileDecision.choose(PerformanceProfile.CUSTOM, 20, 60.0, 5, 0));
    }
}
