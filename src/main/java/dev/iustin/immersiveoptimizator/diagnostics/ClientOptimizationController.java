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

/**
 * Applies conservative, reversible vanilla graphics profiles on the client.
 * It does not force garbage collection or alter world/server simulation.
 */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientOptimizationController {
    private static PerformanceProfile activeProfile;
    private static int lowPerformanceWindows;
    private static int stableWindows;
    private static int baselineRenderDistance = -1;
    private static int lastManagedRenderDistance = -1;

    private ClientOptimizationController() {
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            return;
        }

        if (activeProfile == null) {
            activeProfile = normalize(OptimizerConfig.PROFILE.get());
            applyProfile(minecraft, activeProfile);
        } else if (!OptimizerConfig.AUTO_PROFILE.get()) {
            PerformanceProfile configured = normalize(OptimizerConfig.PROFILE.get());
            if (configured != activeProfile) {
                activeProfile = configured;
                lowPerformanceWindows = 0;
                stableWindows = 0;
                applyProfile(minecraft, activeProfile);
            }
        }
    }

    /** Called once per diagnostics report, never once per frame. */
    public static void onPerformanceSample(int fps, double frameP95Ms) {
        if (!OptimizerConfig.AUTO_PROFILE.get()) {
            return;
        }
        if (activeProfile == null) {
            activeProfile = normalize(OptimizerConfig.PROFILE.get());
        }
        if (activeProfile == PerformanceProfile.CUSTOM) {
            return;
        }

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

        PerformanceProfile next = ProfileDecision.choose(
                activeProfile, fps, frameP95Ms, lowPerformanceWindows, stableWindows);
        if (next != activeProfile) {
            activeProfile = next;
            applyProfile(Minecraft.getInstance(), activeProfile);
            ImmersiveOptimizator.LOGGER.info(
                    "Adaptive graphics profile changed to {} (fps={}, frameP95Ms={})",
                    activeProfile, fps, frameP95Ms);
            lowPerformanceWindows = 0;
            stableWindows = 0;
        }
    }

    private static PerformanceProfile normalize(PerformanceProfile profile) {
        return profile == PerformanceProfile.CUSTOM ? PerformanceProfile.CUSTOM : profile;
    }

    private static void applyProfile(Minecraft minecraft, PerformanceProfile profile) {
        if (minecraft == null || minecraft.options == null || profile == PerformanceProfile.CUSTOM) {
            return;
        }

        var options = minecraft.options;
        int currentDistance = options.renderDistance().get();
        if (baselineRenderDistance < 0) {
            baselineRenderDistance = currentDistance;
        }
        if (lastManagedRenderDistance >= 0 && currentDistance == lastManagedRenderDistance) {
            options.renderDistance().set(baselineRenderDistance);
        } else {
            baselineRenderDistance = currentDistance;
        }
        lastManagedRenderDistance = -1;

        switch (profile) {
            case ULTRA_PERFORMANCE -> {
                options.graphicsMode().set(GraphicsStatus.FAST);
                options.ambientOcclusion().set(false);
                options.cloudStatus().set(CloudStatus.OFF);
                options.particles().set(ParticleStatus.MINIMAL);
                options.entityDistanceScaling().set(0.5D);
                if (OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get()) {
                    int target = Math.min(baselineRenderDistance, 8);
                    if (options.renderDistance().get() != target) {
                        options.renderDistance().set(target);
                        lastManagedRenderDistance = target;
                    }
                }
            }
            case BALANCED -> {
                options.graphicsMode().set(GraphicsStatus.FAST);
                options.ambientOcclusion().set(false);
                options.cloudStatus().set(CloudStatus.FAST);
                options.particles().set(ParticleStatus.DECREASED);
                options.entityDistanceScaling().set(0.75D);
            }
            case QUALITY -> {
                options.graphicsMode().set(GraphicsStatus.FANCY);
                options.ambientOcclusion().set(true);
                options.cloudStatus().set(CloudStatus.FANCY);
                options.particles().set(ParticleStatus.ALL);
                options.entityDistanceScaling().set(1.0D);
            }
            default -> {
                return;
            }
        }
        options.save();
    }
}
