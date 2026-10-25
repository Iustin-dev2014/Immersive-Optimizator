package dev.iustin.immersiveoptimizator.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuration for diagnostics and opt-in adaptive client optimization. */
public final class OptimizerConfig {
    public enum PerformanceProfile {
        ULTRA_PERFORMANCE,
        BALANCED,
        QUALITY,
        CUSTOM
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<PerformanceProfile> PROFILE;
    public static final ForgeConfigSpec.BooleanValue PERFORMANCE_MONITORING;
    public static final ForgeConfigSpec.BooleanValue AUTO_PROFILE;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_RENDER_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue EXPERIMENTAL_OPTIMIZATIONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        PROFILE = builder
                .comment("Manual profile used when autoProfile is disabled.")
                .defineEnum("profile", PerformanceProfile.BALANCED);
        PERFORMANCE_MONITORING = builder
                .comment("Enable low-overhead performance metrics.")
                .define("performanceMonitoring", true);
        AUTO_PROFILE = builder
                .comment("Adapt between Ultra Performance, Balanced and Quality using measured frame times.")
                .define("autoProfile", true);
        ADAPTIVE_RENDER_DISTANCE = builder
                .comment("Allow Ultra Performance to reduce render distance to at most 8 chunks. Disabled by default to preserve the user's distance.")
                .define("adaptiveRenderDistance", false);
        EXPERIMENTAL_OPTIMIZATIONS = builder
                .comment("Reserved for future experimental performance-changing features. Disabled by default.")
                .define("experimentalOptimizations", false);
        builder.pop();

        SPEC = builder.build();
    }

    private OptimizerConfig() {
    }
}
