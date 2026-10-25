package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig.PerformanceProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileDecisionTest {
    @Test
    void veryWeakHardwareStartsInUltra() {
        assertEquals(PerformanceProfile.ULTRA_PERFORMANCE, ProfileDecision.hardwareBaseline(2, 8L * 1024 * 1024 * 1024));
        assertEquals(PerformanceProfile.ULTRA_PERFORMANCE, ProfileDecision.hardwareBaseline(8, 2L * 1024 * 1024 * 1024));
    }

    @Test
    void modestHardwareStartsInPerformance() {
        assertEquals(PerformanceProfile.PERFORMANCE, ProfileDecision.hardwareBaseline(4, 8L * 1024 * 1024 * 1024));
        assertEquals(PerformanceProfile.PERFORMANCE, ProfileDecision.hardwareBaseline(8, 4L * 1024 * 1024 * 1024));
    }

    @Test
    void modernHardwareStartsBalanced() {
        assertEquals(PerformanceProfile.BALANCED, ProfileDecision.hardwareBaseline(8, 8L * 1024 * 1024 * 1024));
    }

    @Test
    void poorPerformanceRequiresTwoWindowsBeforeUltra() {
        assertEquals(PerformanceProfile.BALANCED, ProfileDecision.choose(PerformanceProfile.BALANCED, 25, 45.0, 1, 0));
        assertEquals(PerformanceProfile.ULTRA_PERFORMANCE, ProfileDecision.choose(PerformanceProfile.BALANCED, 25, 45.0, 2, 0));
    }

    @Test
    void stablePerformanceStepsBackFromPerformance() {
        assertEquals(PerformanceProfile.BALANCED, ProfileDecision.choose(PerformanceProfile.PERFORMANCE, 60, 16.0, 0, 3));
    }

    @Test
    void customAndEcoAreNotOverridden() {
        assertEquals(PerformanceProfile.CUSTOM, ProfileDecision.choose(PerformanceProfile.CUSTOM, 10, 100, 10, 0));
        assertEquals(PerformanceProfile.ECO, ProfileDecision.choose(PerformanceProfile.ECO, 10, 100, 10, 0));
    }
}
