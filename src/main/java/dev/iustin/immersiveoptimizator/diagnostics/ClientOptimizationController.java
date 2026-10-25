package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.ImmersiveOptimizator;
import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import dev.iustin.immersiveoptimizator.config.OptimizerConfig.PerformanceProfile;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies reversible client graphics presets; it never changes world simulation or save data. */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientOptimizationController {
    private static PerformanceProfile activeProfile;
    private static int lowPerformanceWindows;
    private static int stableWindows;
    private static int baselineRenderDistance = -1;
    private static int lastManagedRenderDistance = -1;
    private static boolean turboEnabled;
    private static PerformanceProfile profileBeforeLowEnd;
    private static boolean autoBeforeLowEnd;
    private static boolean adaptiveDistanceBeforeLowEnd;

    private ClientOptimizationController() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) return;

        if (OptimizerConfig.MOBILE_LOW_END_MODE.get()) {
            if (activeProfile != PerformanceProfile.ECO) {
                activeProfile = PerformanceProfile.ECO;
                applyProfile(minecraft, activeProfile);
            }
            return;
        }

        if (activeProfile == null) {
            activeProfile = OptimizerConfig.AUTO_PROFILE.get()
                    ? ProfileDecision.hardwareBaseline(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory())
                    : configuredManualProfile(minecraft);
            applyProfile(minecraft, activeProfile);
        } else if (!OptimizerConfig.AUTO_PROFILE.get() && !turboEnabled) {
            PerformanceProfile configured = configuredManualProfile(minecraft);
            if (configured != activeProfile) {
                activeProfile = configured;
                lowPerformanceWindows = 0;
                stableWindows = 0;
                applyProfile(minecraft, activeProfile);
            }
        }
    }

    /** Called by the monitor once per report window, never once per frame. */
    public static void onPerformanceSample(int fps, double frameP95Ms) {
        if (!OptimizerConfig.AUTO_PROFILE.get() || turboEnabled || OptimizerConfig.MOBILE_LOW_END_MODE.get()) return;
        if (activeProfile == null) {
            activeProfile = ProfileDecision.hardwareBaseline(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
        }
        if (activeProfile == PerformanceProfile.CUSTOM || activeProfile == PerformanceProfile.ECO) return;

        if (fps < OptimizerConfig.LOW_FPS_THRESHOLD.get()
                || frameP95Ms > OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.get()) {
            lowPerformanceWindows++;
            stableWindows = 0;
        } else if (fps >= OptimizerConfig.LOW_FPS_THRESHOLD.get() + 25
                && frameP95Ms > 0.0D
                && frameP95Ms < OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.get() * 0.5D) {
            stableWindows++;
            lowPerformanceWindows = 0;
        } else {
            lowPerformanceWindows = 0;
            stableWindows = 0;
        }

        PerformanceProfile next = ProfileDecision.choose(
                activeProfile, fps, frameP95Ms, lowPerformanceWindows, stableWindows,
                OptimizerConfig.LOW_FPS_THRESHOLD.get(),
                OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.get());
        if (next != activeProfile) {
            activeProfile = next;
            applyProfile(Minecraft.getInstance(), activeProfile);
            ImmersiveOptimizator.LOGGER.info(
                    "Adaptive profile changed to {} (fps={}, p95={}ms)", activeProfile, fps, frameP95Ms);
            lowPerformanceWindows = 0;
            stableWindows = 0;
        }
    }

    public static PerformanceProfile getActiveProfile() {
        if (OptimizerConfig.MOBILE_LOW_END_MODE.get()) return PerformanceProfile.ECO;
        return activeProfile == null ? configuredManualProfile(Minecraft.getInstance()) : activeProfile;
    }

    public static PerformanceProfile getConfiguredManualProfile() {
        return configuredManualProfile(Minecraft.getInstance());
    }

    private static PerformanceProfile configuredManualProfile(Minecraft minecraft) {
        if (OptimizerConfig.USE_CONTEXT_PROFILES.get() && minecraft != null) {
            if (minecraft.hasSingleplayerServer()) return normalize(OptimizerConfig.SINGLEPLAYER_PROFILE.get());
            if (minecraft.getConnection() != null) return normalize(OptimizerConfig.MULTIPLAYER_PROFILE.get());
        }
        return normalize(OptimizerConfig.PROFILE.get());
    }

    private static void storeManualProfile(PerformanceProfile profile, Minecraft minecraft) {
        if (OptimizerConfig.USE_CONTEXT_PROFILES.get() && minecraft != null) {
            if (minecraft.hasSingleplayerServer()) {
                OptimizerConfig.SINGLEPLAYER_PROFILE.set(profile);
                return;
            }
            if (minecraft.getConnection() != null) {
                OptimizerConfig.MULTIPLAYER_PROFILE.set(profile);
                return;
            }
        }
        OptimizerConfig.PROFILE.set(profile);
    }

    public static boolean isTurboEnabled() { return turboEnabled; }
    public static boolean isLowEndMode() { return OptimizerConfig.MOBILE_LOW_END_MODE.get(); }

    public static void cycleProfile() {
        Minecraft minecraft = Minecraft.getInstance();
        PerformanceProfile[] profiles = {
                PerformanceProfile.ECO, PerformanceProfile.BALANCED,
                PerformanceProfile.PERFORMANCE, PerformanceProfile.ULTRA_PERFORMANCE
        };
        PerformanceProfile current = getActiveProfile();
        int currentIndex = -1;
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i] == current) currentIndex = i;
        }
        PerformanceProfile next = profiles[(currentIndex + 1) % profiles.length];
        OptimizerConfig.AUTO_PROFILE.set(false);
        OptimizerConfig.MOBILE_LOW_END_MODE.set(false);
        turboEnabled = false;
        activeProfile = next;
        storeManualProfile(next, minecraft);
        applyProfile(minecraft, next);
        OptimizerConfig.save();
    }

    public static void applyManualProfile(PerformanceProfile profile) {
        Minecraft minecraft = Minecraft.getInstance();
        OptimizerConfig.AUTO_PROFILE.set(false);
        OptimizerConfig.MOBILE_LOW_END_MODE.set(false);
        turboEnabled = false;
        activeProfile = normalize(profile);
        storeManualProfile(activeProfile, minecraft);
        applyProfile(minecraft, activeProfile);
        OptimizerConfig.save();
    }

    public static void toggleAutomatic() {
        setAutomaticEnabled(!OptimizerConfig.AUTO_PROFILE.get());
    }

    public static void setAutomaticEnabled(boolean enable) {
        OptimizerConfig.AUTO_PROFILE.set(enable);
        if (enable) {
            OptimizerConfig.MOBILE_LOW_END_MODE.set(false);
            turboEnabled = false;
            activeProfile = ProfileDecision.hardwareBaseline(
                    Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
            lowPerformanceWindows = 0;
            stableWindows = 0;
        } else {
            activeProfile = configuredManualProfile(Minecraft.getInstance());
        }
        applyProfile(Minecraft.getInstance(), activeProfile);
        OptimizerConfig.save();
    }

    public static void toggleContextProfiles() {
        OptimizerConfig.USE_CONTEXT_PROFILES.set(!OptimizerConfig.USE_CONTEXT_PROFILES.get());
        if (OptimizerConfig.USE_CONTEXT_PROFILES.get()) {
            OptimizerConfig.AUTO_PROFILE.set(false);
            OptimizerConfig.MOBILE_LOW_END_MODE.set(false);
        }
        activeProfile = configuredManualProfile(Minecraft.getInstance());
        applyProfile(Minecraft.getInstance(), activeProfile);
        OptimizerConfig.save();
    }

    public static void cycleSingleplayerProfile() {
        OptimizerConfig.SINGLEPLAYER_PROFILE.set(nextManualProfile(OptimizerConfig.SINGLEPLAYER_PROFILE.get()));
        if (OptimizerConfig.USE_CONTEXT_PROFILES.get() && Minecraft.getInstance().hasSingleplayerServer()) {
            OptimizerConfig.AUTO_PROFILE.set(false);
            activeProfile = OptimizerConfig.SINGLEPLAYER_PROFILE.get();
            applyProfile(Minecraft.getInstance(), activeProfile);
        }
        OptimizerConfig.save();
    }

    public static void cycleMultiplayerProfile() {
        OptimizerConfig.MULTIPLAYER_PROFILE.set(nextManualProfile(OptimizerConfig.MULTIPLAYER_PROFILE.get()));
        if (OptimizerConfig.USE_CONTEXT_PROFILES.get() && Minecraft.getInstance().getConnection() != null
                && !Minecraft.getInstance().hasSingleplayerServer()) {
            OptimizerConfig.AUTO_PROFILE.set(false);
            activeProfile = OptimizerConfig.MULTIPLAYER_PROFILE.get();
            applyProfile(Minecraft.getInstance(), activeProfile);
        }
        OptimizerConfig.save();
    }

    private static PerformanceProfile nextManualProfile(PerformanceProfile current) {
        return switch (current) {
            case ECO -> PerformanceProfile.BALANCED;
            case BALANCED -> PerformanceProfile.PERFORMANCE;
            case PERFORMANCE -> PerformanceProfile.ULTRA_PERFORMANCE;
            case ULTRA_PERFORMANCE, QUALITY, CUSTOM -> PerformanceProfile.ECO;
        };
    }

    public static void toggleTurbo() {
        turboEnabled = !turboEnabled;
        OptimizerConfig.MOBILE_LOW_END_MODE.set(false);
        if (turboEnabled) {
            activeProfile = PerformanceProfile.ULTRA_PERFORMANCE;
            applyProfile(Minecraft.getInstance(), activeProfile);
        } else if (OptimizerConfig.AUTO_PROFILE.get()) {
            activeProfile = ProfileDecision.hardwareBaseline(
                    Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
            applyProfile(Minecraft.getInstance(), activeProfile);
        } else {
            activeProfile = configuredManualProfile(Minecraft.getInstance());
            applyProfile(Minecraft.getInstance(), activeProfile);
        }
        OptimizerConfig.save();
    }

    public static void toggleLowEndMode() {
        boolean enable = !OptimizerConfig.MOBILE_LOW_END_MODE.get();
        Minecraft minecraft = Minecraft.getInstance();
        if (enable) {
            profileBeforeLowEnd = getActiveProfile();
            autoBeforeLowEnd = OptimizerConfig.AUTO_PROFILE.get();
            adaptiveDistanceBeforeLowEnd = OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get();
            OptimizerConfig.MOBILE_LOW_END_MODE.set(true);
            OptimizerConfig.AUTO_PROFILE.set(false);
            OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.set(true);
            turboEnabled = false;
            activeProfile = PerformanceProfile.ECO;
            storeManualProfile(PerformanceProfile.ECO, minecraft);
        } else {
            OptimizerConfig.MOBILE_LOW_END_MODE.set(false);
            OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.set(adaptiveDistanceBeforeLowEnd);
            if (autoBeforeLowEnd) {
                setAutomaticEnabled(true);
                profileBeforeLowEnd = null;
                return;
            }
            activeProfile = profileBeforeLowEnd == null
                    ? configuredManualProfile(minecraft) : profileBeforeLowEnd;
            storeManualProfile(activeProfile, minecraft);
            profileBeforeLowEnd = null;
        }
        applyProfile(minecraft, activeProfile);
        OptimizerConfig.save();
    }

    public static void toggleAdaptiveDistance() {
        OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.set(!OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get());
        applyProfile(Minecraft.getInstance(), getActiveProfile());
        OptimizerConfig.save();
    }

    public static void cycleEcoRenderDistanceCap() {
        int[] values = {4, 5, 6, 8, 10, 12};
        OptimizerConfig.ECO_RENDER_DISTANCE_CAP.set(nextValue(OptimizerConfig.ECO_RENDER_DISTANCE_CAP.get(), values));
        applyProfile(Minecraft.getInstance(), getActiveProfile());
        OptimizerConfig.save();
    }

    public static void cycleUltraRenderDistanceCap() {
        int[] values = {4, 6, 8, 10, 12, 16};
        OptimizerConfig.ULTRA_RENDER_DISTANCE_CAP.set(nextValue(OptimizerConfig.ULTRA_RENDER_DISTANCE_CAP.get(), values));
        applyProfile(Minecraft.getInstance(), getActiveProfile());
        OptimizerConfig.save();
    }

    private static int nextValue(int current, int[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }

    public static void cycleLowFpsThreshold() {
        int[] values = {20, 25, 30, 35, 40, 45};
        OptimizerConfig.LOW_FPS_THRESHOLD.set(nextValue(OptimizerConfig.LOW_FPS_THRESHOLD.get(), values));
        OptimizerConfig.save();
    }

    public static void cycleHighFrameTimeThreshold() {
        double[] values = {30.0D, 35.0D, 40.0D, 50.0D, 60.0D, 75.0D};
        double current = OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.get();
        double next = values[0];
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - current) < 0.01D) {
                next = values[(i + 1) % values.length];
                break;
            }
        }
        OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.set(next);
        OptimizerConfig.save();
    }

    public static void toggleMonitoring() {
        OptimizerConfig.PERFORMANCE_MONITORING.set(!OptimizerConfig.PERFORMANCE_MONITORING.get());
        OptimizerConfig.save();
    }

    public static void toggleExportDiagnostics() {
        OptimizerConfig.EXPORT_DIAGNOSTICS.set(!OptimizerConfig.EXPORT_DIAGNOSTICS.get());
        OptimizerConfig.save();
    }

    public static void toggleCompatibilityWarnings() {
        OptimizerConfig.COMPATIBILITY_WARNINGS.set(!OptimizerConfig.COMPATIBILITY_WARNINGS.get());
        OptimizerConfig.save();
    }

    private static PerformanceProfile normalize(PerformanceProfile profile) {
        return profile == null ? PerformanceProfile.BALANCED : profile;
    }

    private static void applyProfile(Minecraft minecraft, PerformanceProfile profile) {
        if (minecraft == null || minecraft.options == null || profile == null
                || profile == PerformanceProfile.CUSTOM) return;
        var options = minecraft.options;
        boolean changed = false;

        if (baselineRenderDistance < 0) baselineRenderDistance = options.renderDistance().get();
        if (lastManagedRenderDistance >= 0 && options.renderDistance().get() == lastManagedRenderDistance) {
            if (options.renderDistance().get() != baselineRenderDistance) {
                options.renderDistance().set(baselineRenderDistance);
                changed = true;
            }
        } else {
            baselineRenderDistance = options.renderDistance().get();
        }
        lastManagedRenderDistance = -1;

        GraphicsStatus graphics = GraphicsStatus.FAST;
        boolean ao = false;
        CloudStatus clouds = CloudStatus.OFF;
        ParticleStatus particles = ParticleStatus.MINIMAL;
        double entityDistance = 0.5D;
        int renderTarget = baselineRenderDistance;

        switch (profile) {
            case ECO -> {
                graphics = GraphicsStatus.FAST;
                ao = false;
                clouds = CloudStatus.OFF;
                particles = ParticleStatus.MINIMAL;
                entityDistance = 0.5D;
                if (OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get()) {
                    renderTarget = Math.min(baselineRenderDistance, OptimizerConfig.ECO_RENDER_DISTANCE_CAP.get());
                }
            }
            case BALANCED -> {
                graphics = GraphicsStatus.FAST;
                ao = false;
                clouds = CloudStatus.FAST;
                particles = ParticleStatus.DECREASED;
                entityDistance = 0.75D;
            }
            case PERFORMANCE -> {
                graphics = GraphicsStatus.FAST;
                ao = false;
                clouds = CloudStatus.OFF;
                particles = ParticleStatus.DECREASED;
                entityDistance = 0.65D;
            }
            case ULTRA_PERFORMANCE -> {
                graphics = GraphicsStatus.FAST;
                ao = false;
                clouds = CloudStatus.OFF;
                particles = ParticleStatus.MINIMAL;
                entityDistance = 0.5D;
                if (OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get()) {
                    renderTarget = Math.min(baselineRenderDistance, OptimizerConfig.ULTRA_RENDER_DISTANCE_CAP.get());
                }
            }
            case QUALITY -> {
                graphics = GraphicsStatus.FANCY;
                ao = true;
                clouds = CloudStatus.FANCY;
                particles = ParticleStatus.ALL;
                entityDistance = 1.0D;
            }
            default -> { return; }
        }

        if (options.graphicsMode().get() != graphics) { options.graphicsMode().set(graphics); changed = true; }
        if (options.ambientOcclusion().get() != ao) { options.ambientOcclusion().set(ao); changed = true; }
        if (options.cloudStatus().get() != clouds) { options.cloudStatus().set(clouds); changed = true; }
        if (options.particles().get() != particles) { options.particles().set(particles); changed = true; }
        if (Math.abs(options.entityDistanceScaling().get() - entityDistance) > 0.001D) {
            options.entityDistanceScaling().set(entityDistance);
            changed = true;
        }

        boolean shouldManageDistance = OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get()
                && (profile == PerformanceProfile.ECO || profile == PerformanceProfile.ULTRA_PERFORMANCE);
        if (shouldManageDistance && options.renderDistance().get() != renderTarget) {
            options.renderDistance().set(renderTarget);
            lastManagedRenderDistance = renderTarget;
            changed = true;
        }
        if (changed) options.save();
    }
}
