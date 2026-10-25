package dev.iustin.immersiveoptimizator.diagnostics;

import java.util.Arrays;

/** Small deterministic helpers used by diagnostics and their unit tests. */
public final class PerformanceMath {
    private PerformanceMath() {
    }

    /**
     * Returns the nearest-rank percentile of a non-empty sample.
     * The input is copied, so callers' arrays are not modified.
     */
    public static double percentile(double[] values, double percentile) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        if (!Double.isFinite(percentile) || percentile < 0.0 || percentile > 1.0) {
            throw new IllegalArgumentException("percentile must be between 0 and 1");
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }
}
