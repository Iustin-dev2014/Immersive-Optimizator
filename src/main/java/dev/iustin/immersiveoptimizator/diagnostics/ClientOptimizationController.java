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

/** Applies reversible client graphics presets only; never mutates world simulation or saves. */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientOptimizationController {
    private static PerformanceProfile activeProfile;
    private static int lowPerformanceWindows;
    private static int stableWindows;
    private static int baselineRenderDistance = -1;
    private static int lastManagedRenderDistance = -1;
    private static boolean turboEnabled;

    private ClientOptimizationController() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) return;
        if (activeProfile == null) {
            activeProfile = OptimizerConfig.AUTO_PROFILE.get()
                    ? ProfileDecision.hardwareBaseline(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory())
                    : normalize(OptimizerConfig.PROFILE.get());
            applyProfile(minecraft, activeProfile);
        } else if (!OptimizerConfig.AUTO_PROFILE.get()) {
            PerformanceProfile configured = normalize(OptimizerConfig.PROFILE.get());
            if (configured != activeProfile && !turboEnabled) {
                activeProfile = configured;
                lowPerformanceWindows = 0;
                stableWindows = 0;
                applyProfile(minecraft, activeProfile);
            }
        }
    }

    /** Called once per diagnostic report, not per frame. */
    public static void onPerformanceSample(int fps, double frameP95Ms) {
        if (!OptimizerConfig.AUTO_PROFILE.get() || turboEnabled) return;
        if (activeProfile == null)
            activeProfile = ProfileDecision.hardwareBaseline(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
        if (activeProfile == PerformanceProfile.CUSTOM || activeProfile == PerformanceProfile.ECO) return;

        if (fps < 30 || frameP95Ms > 40.0) {
            lowPerformanceWindows++;
            stableWindows = 0;
        } else if (fps >= 55 && frameP95Ms > 0.0 && frameP95Ms < 20.0) {
            stableWindows++;
            lowPerformanceWindows = 0;
        } else {
            lowPerformanceWindows = 0;
            stableWindows = 0;
        }
        PerformanceProfile next = ProfileDecision.choose(activeProfile, fps, frameP95Ms, lowPerformanceWindows, stableWindows);
        if (next != activeProfile) {
            activeProfile = next;
            applyProfile(Minecraft.getInstance(), activeProfile);
            ImmersiveOptimizator.LOGGER.info("Adaptive profile changed to {} (fps={}, p95={}ms)", activeProfile, fps, frameP95Ms);
            lowPerformanceWindows = 0;
            stableWindows = 0;
        }
    }

    public static PerformanceProfile getActiveProfile() {
        return activeProfile == null ? normalize(OptimizerConfig.PROFILE.get()) : activeProfile;
    }
    public static boolean isTurboEnabled() { return turboEnabled; }

    public static void cycleProfile() {
        PerformanceProfile[] profiles = {PerformanceProfile.ECO, PerformanceProfile.BALANCED,
                PerformanceProfile.PERFORMANCE, PerformanceProfile.ULTRA_PERFORMANCE};
        PerformanceProfile current = getActiveProfile();
        int next = 0;
        for (int i = 0; i < profiles.length; i++) if (profiles[i] == current) next = (i + 1) % profiles.length;
        turboEnabled = false;
        activeProfile = profiles[next];
        OptimizerConfig.AUTO_PROFILE.set(false);
        OptimizerConfig.PROFILE.set(activeProfile);
        applyProfile(Minecraft.getInstance(), activeProfile);
    }

    public static void toggleAutomatic() {
        OptimizerConfig.AUTO_PROFILE.set(!OptimizerConfig.AUTO_PROFILE.get());
        if (OptimizerConfig.AUTO_PROFILE.get()) {
            activeProfile = ProfileDecision.hardwareBaseline(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
            lowPerformanceWindows = 0;
            stableWindows = 0;
            applyProfile(Minecraft.getInstance(), activeProfile);
        }
    }

    public static void toggleTurbo() {
        turboEnabled = !turboEnabled;
        if (turboEnabled) {
            activeProfile = PerformanceProfile.ULTRA_PERFORMANCE;
            applyProfile(Minecraft.getInstance(), activeProfile);
        } else if (OptimizerConfig.AUTO_PROFILE.get()) {
            activeProfile = ProfileDecision.hardwareBaseline(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
            applyProfile(Minecraft.getInstance(), activeProfile);
        } else {
            activeProfile = normalize(OptimizerConfig.PROFILE.get());
            applyProfile(Minecraft.getInstance(), activeProfile);
        }
    }

    public static void toggleAdaptiveDistance() {
        OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.set(!OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get());
        applyProfile(Minecraft.getInstance(), getActiveProfile());
    }

    private static PerformanceProfile normalize(PerformanceProfile profile) {
        return profile == null ? PerformanceProfile.BALANCED : profile;
    }

    private static void applyProfile(Minecraft minecraft, PerformanceProfile profile) {
        if (minecraft == null || minecraft.options == null || profile == PerformanceProfile.CUSTOM) return;
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
                graphics = GraphicsStatus.FAST; ao = false; clouds = CloudStatus.OFF;
                particles = ParticleStatus.MINIMAL; entityDistance = 0.5D;
                if (OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get()) renderTarget = Math.max(4, Math.min(baselineRenderDistance, 6));
            }
            case BALANCED -> {
                graphics = GraphicsStatus.FAST; ao = false; clouds = CloudStatus.FAST;
                particles = ParticleStatus.DECREASED; entityDistance = 0.75D;
            }
            case PERFORMANCE -> {
                graphics = GraphicsStatus.FAST; ao = false; clouds = CloudStatus.OFF;
                particles = ParticleStatus.DECREASED; entityDistance = 0.65D;
            }
            case ULTRA_PERFORMANCE -> {
                graphics = GraphicsStatus.FAST; ao = false; clouds = CloudStatus.OFF;
                particles = ParticleStatus.MINIMAL; entityDistance = 0.5D;
                if (OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get())
                    renderTarget = Math.max(4, Math.min(baselineRenderDistance, 8));
            }
            case QUALITY -> {
                graphics = GraphicsStatus.FANCY; ao = true; clouds = CloudStatus.FANCY;
                particles = ParticleStatus.ALL; entityDistance = 1.0D;
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
