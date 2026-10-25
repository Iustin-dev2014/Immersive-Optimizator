package dev.iustin.immersiveoptimizator.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuration for client-side diagnostics and reversible adaptive graphics profiles. */
public final class OptimizerConfig {
    public enum PerformanceProfile { ECO, BALANCED, PERFORMANCE, ULTRA_PERFORMANCE, QUALITY, CUSTOM }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<PerformanceProfile> PROFILE;
    public static final ForgeConfigSpec.BooleanValue PERFORMANCE_MONITORING;
    public static final ForgeConfigSpec.BooleanValue AUTO_PROFILE;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_RENDER_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue EXPERIMENTAL_OPTIMIZATIONS;
    public static final ForgeConfigSpec.BooleanValue EXPORT_DIAGNOSTICS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        PROFILE = builder.comment("Manual profile: ECO, BALANCED, PERFORMANCE, ULTRA_PERFORMANCE, QUALITY or CUSTOM.")
                .defineEnum("profile", PerformanceProfile.BALANCED);
        PERFORMANCE_MONITORING = builder.comment("Collect low-overhead client performance metrics.")
                .define("performanceMonitoring", true);
        AUTO_PROFILE = builder.comment("Automatically adapt graphics profile using frame-time measurements.")
                .define("autoProfile", true);
        ADAPTIVE_RENDER_DISTANCE = builder.comment("Allow Eco/Ultra profiles to lower render distance.")
                .define("adaptiveRenderDistance", true);
        EXPERIMENTAL_OPTIMIZATIONS = builder.comment("Reserved for future engine-level optimizations; no unsafe world changes are performed.")
                .define("experimentalOptimizations", false);
        EXPORT_DIAGNOSTICS = builder.comment("Allow exporting a local diagnostic report from the in-game dashboard.")
                .define("exportDiagnostics", true);
        builder.pop();
        SPEC = builder.build();
    }
    private OptimizerConfig() {}
}
