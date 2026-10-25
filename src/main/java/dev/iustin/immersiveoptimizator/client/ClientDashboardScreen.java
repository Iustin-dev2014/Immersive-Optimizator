package dev.iustin.immersiveoptimizator.client;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import dev.iustin.immersiveoptimizator.diagnostics.ClientOptimizationController;
import dev.iustin.immersiveoptimizator.diagnostics.ClientPerformanceMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** In-game control panel for profiles and local diagnostics. */
public final class ClientDashboardScreen extends Screen {
    private final Screen parent;
    private String statusMessage = "Metrics stay on this computer.";

    public ClientDashboardScreen(Screen parent) {
        super(Component.literal("Immersive Optimizator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        int top = Math.max(64, this.height / 2 - 92);
        addRenderableWidget(Button.builder(Component.literal("Cycle profile: " + label(ClientOptimizationController.getActiveProfile())),
                button -> { ClientOptimizationController.cycleProfile(); clearWidgets(); init(); })
                .bounds(left, top, 300, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Automatic: " + (OptimizerConfig.AUTO_PROFILE.get() ? "ON" : "OFF")),
                button -> { ClientOptimizationController.toggleAutomatic(); clearWidgets(); init(); })
                .bounds(left, top + 25, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Adaptive distance: " + (OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get() ? "ON" : "OFF")),
                button -> { ClientOptimizationController.toggleAdaptiveDistance(); clearWidgets(); init(); })
                .bounds(left + 155, top + 25, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Turbo: " + (ClientOptimizationController.isTurboEnabled() ? "ON" : "OFF")),
                button -> { ClientOptimizationController.toggleTurbo(); clearWidgets(); init(); })
                .bounds(left, top + 50, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Export report"),
                button -> {
                    try {
                        statusMessage = "Saved: " + ClientPerformanceMonitor.exportReport().getFileName();
                    } catch (Exception exception) {
                        statusMessage = "Export failed: " + exception.getClass().getSimpleName();
                    }
                }).bounds(left + 155, top + 50, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left, top + 80, 300, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int center = width / 2;
        int top = Math.max(28, height / 2 - 126);
        ClientPerformanceMonitor.Snapshot s = ClientPerformanceMonitor.latest();
        graphics.drawCenteredString(font, title, center, top, 0xFFFFFF);
        graphics.drawCenteredString(font, "FPS: " + s.fps() + "   Frame p95: " + s.p95FrameMs() + " ms", center, top + 18, 0xE0E0E0);
        graphics.drawCenteredString(font, "Heap: " + s.heapUsedMiB() + "/" + s.heapMaxMiB() + " MiB   GC: " + s.gcTimeDeltaMs() + " ms / window", center, top + 31, 0xC0C0C0);
        graphics.drawCenteredString(font, "CPU threads: " + Runtime.getRuntime().availableProcessors()
                + "   Render distance: " + Minecraft.getInstance().options.renderDistance().get(), center, top + 44, 0xC0C0C0);
        graphics.drawCenteredString(font, "Turbo is a graphics preset, not a CPU overclock.", center, height - 28, 0xAAAAAA);
        graphics.drawCenteredString(font, statusMessage, center, height - 16, 0xFFFF80);
    }

    private static String label(OptimizerConfig.PerformanceProfile profile) {
        return switch (profile) {
            case ECO -> "Eco";
            case BALANCED -> "Balanced";
            case PERFORMANCE -> "Performance";
            case ULTRA_PERFORMANCE -> "Ultra";
            case QUALITY -> "Quality";
            case CUSTOM -> "Custom";
        };
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
