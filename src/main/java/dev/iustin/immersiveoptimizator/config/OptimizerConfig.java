package dev.iustin.immersiveoptimizator.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Conservative configuration defaults for the first implementation phase.
 * These options are a foundation; they do not imply that all optimization
 * modules are implemented yet.
 */
public final class OptimizerConfig {
    public enum PerformanceProfile {
        LOW_END,
        BALANCED,
        HIGH_PERFORMANCE,
        CUSTOM
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<PerformanceProfile> PROFILE;
    public static final ForgeConfigSpec.BooleanValue PERFORMANCE_MONITORING;
    public static final ForgeConfigSpec.BooleanValue EXPERIMENTAL_OPTIMIZATIONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        PROFILE = builder
                .comment("Default profile for future adaptive optimization modules.")
                .defineEnum("profile", PerformanceProfile.BALANCED);
        PERFORMANCE_MONITORING = builder
                .comment("Enable performance metrics when the monitoring module is available.")
                .define("performanceMonitoring", true);
        EXPERIMENTAL_OPTIMIZATIONS = builder
                .comment("Experimental performance-changing features are opt-in and disabled by default.")
                .define("experimentalOptimizations", false);
        builder.pop();

        SPEC = builder.build();
    }

    private OptimizerConfig() {
    }
}
