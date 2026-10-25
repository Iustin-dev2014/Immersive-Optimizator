package dev.iustin.immersiveoptimizator.config;

import net.minecraftforge.common.ForgeConfigSpec;
import dev.iustin.immersiveoptimizator.ImmersiveOptimizator;

/** User-configurable, client-side options for adaptive graphics and diagnostics. */
public final class OptimizerConfig {
    public enum PerformanceProfile {
        ECO, BALANCED, PERFORMANCE, ULTRA_PERFORMANCE, QUALITY, CUSTOM
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<PerformanceProfile> PROFILE;
    public static final ForgeConfigSpec.EnumValue<PerformanceProfile> SINGLEPLAYER_PROFILE;
    public static final ForgeConfigSpec.EnumValue<PerformanceProfile> MULTIPLAYER_PROFILE;
    public static final ForgeConfigSpec.BooleanValue PERFORMANCE_MONITORING;
    public static final ForgeConfigSpec.BooleanValue AUTO_PROFILE;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_RENDER_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue USE_CONTEXT_PROFILES;
    public static final ForgeConfigSpec.BooleanValue MOBILE_LOW_END_MODE;
    public static final ForgeConfigSpec.BooleanValue EXPERIMENTAL_OPTIMIZATIONS;
    public static final ForgeConfigSpec.BooleanValue EXPORT_DIAGNOSTICS;
    public static final ForgeConfigSpec.BooleanValue COMPATIBILITY_WARNINGS;
    public static final ForgeConfigSpec.IntValue ECO_RENDER_DISTANCE_CAP;
    public static final ForgeConfigSpec.IntValue ULTRA_RENDER_DISTANCE_CAP;
    public static final ForgeConfigSpec.IntValue LOW_FPS_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue HIGH_FRAME_TIME_THRESHOLD_MS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        PROFILE = builder.comment("Default/manual profile when context-specific profiles are disabled.")
                .defineEnum("profile", PerformanceProfile.BALANCED);
        AUTO_PROFILE = builder.comment("Adapt the selected profile based on measured FPS and frame times.")
                .define("autoProfile", true);
        USE_CONTEXT_PROFILES = builder.comment("Use separate manual profiles for single-player and multiplayer.")
                .define("useContextProfiles", false);
        SINGLEPLAYER_PROFILE = builder.comment("Manual profile used in single-player when context profiles are enabled.")
                .defineEnum("singleplayerProfile", PerformanceProfile.PERFORMANCE);
        MULTIPLAYER_PROFILE = builder.comment("Manual profile used in multiplayer when context profiles are enabled.")
                .defineEnum("multiplayerProfile", PerformanceProfile.PERFORMANCE);
        MOBILE_LOW_END_MODE = builder.comment("Sticky low-end/mobile preset; forces Eco graphics until disabled.")
                .define("mobileLowEndMode", false);
        PERFORMANCE_MONITORING = builder.comment("Collect client-only frame-time and JVM heap/GC metrics.")
                .define("performanceMonitoring", true);
        ADAPTIVE_RENDER_DISTANCE = builder.comment("Allow Eco and Ultra profiles to reduce render distance.")
                .define("adaptiveRenderDistance", true);
        ECO_RENDER_DISTANCE_CAP = builder.comment("Maximum render distance used by Eco when adaptive distance is enabled.")
                .defineInRange("ecoRenderDistanceCap", 6, 4, 12);
        ULTRA_RENDER_DISTANCE_CAP = builder.comment("Maximum render distance used by Ultra when adaptive distance is enabled.")
                .defineInRange("ultraRenderDistanceCap", 8, 4, 16);
        LOW_FPS_THRESHOLD = builder.comment("FPS below which the automatic policy treats performance as critical.")
                .defineInRange("lowFpsThreshold", 30, 10, 60);
        HIGH_FRAME_TIME_THRESHOLD_MS = builder.comment("P95 frame time in milliseconds considered critical by the adaptive policy.")
                .defineInRange("highFrameTimeThresholdMs", 40.0D, 20.0D, 100.0D);
        EXPORT_DIAGNOSTICS = builder.comment("Permit exporting a diagnostic report to a local text file.")
                .define("exportDiagnostics", true);
        COMPATIBILITY_WARNINGS = builder.comment("Show warnings for a few known combinations of performance mods.")
                .define("compatibilityWarnings", true);
        EXPERIMENTAL_OPTIMIZATIONS = builder.comment("Reserved for future engine-level experiments; no risky world changes are performed.")
                .define("experimentalOptimizations", false);

        builder.pop();
        SPEC = builder.build();
    }

    /** Persist changes made by the in-game settings panel to the common TOML config. */
    public static void save() {
        try {
            SPEC.save();
        } catch (RuntimeException exception) {
            ImmersiveOptimizator.LOGGER.warn("Could not persist Immersive Optimizator settings; they may reset on restart.", exception);
        }
    }

    private OptimizerConfig() {}
}
