package dev.iustin.immersiveoptimizator.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PerformanceMathTest {
    @Test
    void calculatesNearestRankPercentileWithoutMutatingInput() {
        double[] values = {40.0, 10.0, 30.0, 20.0};

        assertEquals(40.0, PerformanceMath.percentile(values, 0.95));
        assertEquals(40.0, values[0], "input array must not be sorted in place");
    }

    @Test
    void supportsMedianAndMinimum() {
        assertEquals(2.0, PerformanceMath.percentile(new double[]{4.0, 1.0, 3.0, 2.0}, 0.5));
        assertEquals(1.0, PerformanceMath.percentile(new double[]{4.0, 1.0, 3.0, 2.0}, 0.0));
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> PerformanceMath.percentile(new double[0], 0.5));
        assertThrows(IllegalArgumentException.class, () -> PerformanceMath.percentile(new double[]{1.0}, 1.1));
    }
}
