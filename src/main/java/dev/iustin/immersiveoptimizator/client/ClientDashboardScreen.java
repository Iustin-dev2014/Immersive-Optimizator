package dev.iustin.immersiveoptimizator.client;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import dev.iustin.immersiveoptimizator.config.OptimizerConfig.PerformanceProfile;
import dev.iustin.immersiveoptimizator.diagnostics.ClientOptimizationController;
import dev.iustin.immersiveoptimizator.diagnostics.ClientPerformanceMonitor;
import dev.iustin.immersiveoptimizator.diagnostics.CompatibilityDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-page optimization dashboard. The UI is touch-friendly and uses vanilla
 * widgets so it can also be operated in Android Java launchers that support Forge.
 */
public final class ClientDashboardScreen extends Screen {
    private static final String[] TABS = {
            "gui.immersiveoptimizator.tab.home",
            "gui.immersiveoptimizator.tab.performance",
            "gui.immersiveoptimizator.tab.graphics",
            "gui.immersiveoptimizator.tab.diagnostics",
            "gui.immersiveoptimizator.tab.mobile",
            "gui.immersiveoptimizator.tab.compatibility"
    };

    private final Screen parent;
    private final int page;
    private String statusMessage;
    private int contentTop;
    private int rowGap;
    private int buttonLeft;
    private int buttonTotalWidth;
    private int columnWidth;
    private List<String> compatibilityLines = List.of();

    public ClientDashboardScreen(Screen parent) {
        this(parent, 0, "");
    }

    public ClientDashboardScreen(Screen parent, int page, String statusMessage) {
        super(Component.translatable("gui.immersiveoptimizator.title"));
        this.parent = parent;
        this.page = Math.max(0, Math.min(TABS.length - 1, page));
        this.statusMessage = statusMessage == null ? "" : statusMessage;
    }

    @Override
    protected void init() {
        int gap = 2;
        int usableWidth = Math.max(1, this.width - 16);
        int tabWidth = Math.max(1, (usableWidth - gap * (TABS.length - 1)) / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            final int targetPage = i;
            int x = 8 + i * (tabWidth + gap);
            addRenderableWidget(Button.builder(Component.translatable(TABS[i]),
                            button -> openPage(targetPage))
                    .bounds(x, 31, tabWidth, 20).build());
        }

        buttonTotalWidth = Math.min(300, Math.max(1, this.width - 16));
        buttonLeft = (this.width - buttonTotalWidth) / 2;
        columnWidth = Math.max(1, (buttonTotalWidth - 6) / 2);
        rowGap = this.height > 220 ? 25 : 22;
        contentTop = page == 5
                ? Math.max(104, Math.min(128, this.height - 52))
                : (this.height > 220 ? 64 : 54);

        switch (page) {
            case 0 -> buildHomePage();
            case 1 -> buildPerformancePage();
            case 2 -> buildGraphicsPage();
            case 3 -> buildDiagnosticsPage();
            case 4 -> buildMobilePage();
            case 5 -> buildCompatibilityPage();
            default -> buildHomePage();
        }

        int doneWidth = Math.min(140, Math.max(1, this.width - 16));
        addRenderableWidget(Button.builder(Component.translatable("gui.immersiveoptimizator.done"),
                        button -> onClose())
                .bounds((this.width - doneWidth) / 2, Math.max(0, this.height - 25), doneWidth, 20).build());
    }

    private void buildHomePage() {
        addAction(Component.translatable("gui.immersiveoptimizator.profile", profileLabel(ClientOptimizationController.getActiveProfile())),
                0, 0, ClientOptimizationController::cycleProfile);
        addAction(Component.translatable("gui.immersiveoptimizator.automatic", onOff(OptimizerConfig.AUTO_PROFILE.get())),
                1, 0, ClientOptimizationController::toggleAutomatic);
        addAction(Component.translatable("gui.immersiveoptimizator.turbo", onOff(ClientOptimizationController.isTurboEnabled())),
                0, 1, ClientOptimizationController::toggleTurbo);
        addAction(Component.translatable("gui.immersiveoptimizator.low_end", onOff(ClientOptimizationController.isLowEndMode())),
                1, 1, ClientOptimizationController::toggleLowEndMode);
        addAction(Component.translatable("gui.immersiveoptimizator.context_profiles", onOff(OptimizerConfig.USE_CONTEXT_PROFILES.get())),
                0, 2, ClientOptimizationController::toggleContextProfiles);
        addAction(Component.translatable("gui.immersiveoptimizator.open_diagnostics"),
                1, 2, () -> openPage(3));
    }

    private void buildPerformancePage() {
        addAction(Component.translatable("gui.immersiveoptimizator.automatic", onOff(OptimizerConfig.AUTO_PROFILE.get())),
                0, 0, ClientOptimizationController::toggleAutomatic);
        addAction(Component.translatable("gui.immersiveoptimizator.context_profiles", onOff(OptimizerConfig.USE_CONTEXT_PROFILES.get())),
                1, 0, ClientOptimizationController::toggleContextProfiles);
        addAction(Component.translatable("gui.immersiveoptimizator.singleplayer_profile",
                        profileLabel(OptimizerConfig.SINGLEPLAYER_PROFILE.get())),
                0, 1, ClientOptimizationController::cycleSingleplayerProfile);
        addAction(Component.translatable("gui.immersiveoptimizator.multiplayer_profile",
                        profileLabel(OptimizerConfig.MULTIPLAYER_PROFILE.get())),
                1, 1, ClientOptimizationController::cycleMultiplayerProfile);
        addAction(Component.translatable("gui.immersiveoptimizator.low_fps_threshold",
                        OptimizerConfig.LOW_FPS_THRESHOLD.get()),
                0, 2, ClientOptimizationController::cycleLowFpsThreshold);
        addAction(Component.translatable("gui.immersiveoptimizator.frame_threshold",
                        OptimizerConfig.HIGH_FRAME_TIME_THRESHOLD_MS.get()),
                1, 2, ClientOptimizationController::cycleHighFrameTimeThreshold);
    }

    private void buildGraphicsPage() {
        addAction(Component.translatable("gui.immersiveoptimizator.adaptive_distance",
                        onOff(OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get())),
                0, 0, ClientOptimizationController::toggleAdaptiveDistance);
        addAction(Component.translatable("gui.immersiveoptimizator.eco_distance",
                        OptimizerConfig.ECO_RENDER_DISTANCE_CAP.get()),
                1, 0, ClientOptimizationController::cycleEcoRenderDistanceCap);
        addAction(Component.translatable("gui.immersiveoptimizator.ultra_distance",
                        OptimizerConfig.ULTRA_RENDER_DISTANCE_CAP.get()),
                0, 1, ClientOptimizationController::cycleUltraRenderDistanceCap);
        addAction(Component.translatable("gui.immersiveoptimizator.apply_profile", "Eco"),
                1, 1, () -> ClientOptimizationController.applyManualProfile(PerformanceProfile.ECO));
        addAction(Component.translatable("gui.immersiveoptimizator.apply_profile", "Performance"),
                0, 2, () -> ClientOptimizationController.applyManualProfile(PerformanceProfile.PERFORMANCE));
        addAction(Component.translatable("gui.immersiveoptimizator.apply_profile", "Ultra"),
                1, 2, () -> ClientOptimizationController.applyManualProfile(PerformanceProfile.ULTRA_PERFORMANCE));
    }

    private void buildDiagnosticsPage() {
        addAction(Component.translatable("gui.immersiveoptimizator.monitoring",
                        onOff(OptimizerConfig.PERFORMANCE_MONITORING.get())),
                0, 0, ClientOptimizationController::toggleMonitoring);
        addAction(Component.translatable("gui.immersiveoptimizator.export_enabled",
                        onOff(OptimizerConfig.EXPORT_DIAGNOSTICS.get())),
                1, 0, ClientOptimizationController::toggleExportDiagnostics);
        addAction(Component.translatable("gui.immersiveoptimizator.export_report"),
                0, 1, this::exportReport);
        addAction(Component.translatable("gui.immersiveoptimizator.compatibility_warnings",
                        onOff(OptimizerConfig.COMPATIBILITY_WARNINGS.get())),
                1, 1, ClientOptimizationController::toggleCompatibilityWarnings);
        addAction(Component.translatable("gui.immersiveoptimizator.show_warnings"),
                0, 2, () -> setStatus(CompatibilityDiagnostics.summary()));
        addAction(Component.translatable("gui.immersiveoptimizator.return_auto"),
                1, 2, () -> {
                    ClientOptimizationController.setAutomaticEnabled(true);
                });
    }

    private void buildMobilePage() {
        addAction(Component.translatable("gui.immersiveoptimizator.low_end", onOff(ClientOptimizationController.isLowEndMode())),
                0, 0, ClientOptimizationController::toggleLowEndMode);
        addAction(Component.translatable("gui.immersiveoptimizator.turbo", onOff(ClientOptimizationController.isTurboEnabled())),
                1, 0, ClientOptimizationController::toggleTurbo);
        addAction(Component.translatable("gui.immersiveoptimizator.adaptive_distance",
                        onOff(OptimizerConfig.ADAPTIVE_RENDER_DISTANCE.get())),
                0, 1, ClientOptimizationController::toggleAdaptiveDistance);
        addAction(Component.translatable("gui.immersiveoptimizator.eco_distance",
                        OptimizerConfig.ECO_RENDER_DISTANCE_CAP.get()),
                1, 1, ClientOptimizationController::cycleEcoRenderDistanceCap);
        addAction(Component.translatable("gui.immersiveoptimizator.apply_profile", "Eco"),
                0, 2, () -> ClientOptimizationController.applyManualProfile(PerformanceProfile.ECO));
        addAction(Component.translatable("gui.immersiveoptimizator.return_auto"),
                1, 2, () -> {
                    ClientOptimizationController.setAutomaticEnabled(true);
                });
    }

    private void buildCompatibilityPage() {
        compatibilityLines = new ArrayList<>();
        compatibilityLines.add("Embeddium (renderer): " + present("embeddium"));
        compatibilityLines.add("ModernFix (load/memory): " + present("modernfix"));
        compatibilityLines.add("FerriteCore (memory): " + present("ferritecore"));
        compatibilityLines.add("ImmediatelyFast (rendering): " + present("immediatelyfast"));
        List<String> warnings = CompatibilityDiagnostics.warnings();
        if (!OptimizerConfig.COMPATIBILITY_WARNINGS.get()) {
            compatibilityLines.add("Built-in compatibility warnings are disabled.");
        } else if (warnings.isEmpty()) {
            compatibilityLines.add("No known conflict in the built-in checks.");
        } else {
            compatibilityLines.add("WARNING: " + warnings.get(0));
            if (warnings.size() > 1) compatibilityLines.add(warnings.get(1));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.immersiveoptimizator.compatibility_warnings",
                        onOff(OptimizerConfig.COMPATIBILITY_WARNINGS.get())),
                        button -> {
                            ClientOptimizationController.toggleCompatibilityWarnings();
                            refresh();
                        })
                .bounds(buttonLeft, contentTop, columnWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.immersiveoptimizator.export_report"),
                        button -> { exportReport(); refresh(); })
                .bounds(buttonLeft + columnWidth + 6, contentTop, columnWidth, 20).build());
    }

    private void addAction(Component label, int column, int row, Runnable action) {
        int x = buttonLeft + column * (columnWidth + 6);
        int y = contentTop + row * rowGap;
        addRenderableWidget(Button.builder(label, button -> {
                    action.run();
                    if (Minecraft.getInstance().screen == this) refresh();
                })
                .bounds(x, y, columnWidth, 20).build());
    }

    private void exportReport() {
        try {
            statusMessage = Component.translatable("gui.immersiveoptimizator.report_saved",
                    ClientPerformanceMonitor.exportReport().getFileName().toString()).getString();
        } catch (Exception exception) {
            statusMessage = Component.translatable("gui.immersiveoptimizator.report_failed",
                    exception.getClass().getSimpleName()).getString();
        }
    }

    private void setStatus(String message) {
        statusMessage = message == null ? "" : message;
    }

    private void openPage(int targetPage) {
        Minecraft.getInstance().setScreen(new ClientDashboardScreen(parent, targetPage, statusMessage));
    }

    private void refresh() {
        Minecraft.getInstance().setScreen(new ClientDashboardScreen(parent, page, statusMessage));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int center = width / 2;
        ClientPerformanceMonitor.Snapshot snapshot = ClientPerformanceMonitor.latest();
        graphics.drawCenteredString(font, title, center, 7, 0xFFFFFF);
        String metrics = "FPS " + snapshot.fps() + " | p95 " + snapshot.p95FrameMs() + " ms | "
                + snapshot.heapUsedMiB() + "/" + snapshot.heapMaxMiB() + " MiB";
        graphics.drawCenteredString(font, fit(metrics, width - 16), center, 19, 0xD5D5D5);

        if (page == 5) {
            drawCompatibilityDetails(graphics);
        } else if (!statusMessage.isBlank()) {
            graphics.drawCenteredString(font, fit(statusMessage, width - 18), center,
                    Math.max(0, height - 38), 0xFFFF80);
        }
    }

    private void drawCompatibilityDetails(GuiGraphics graphics) {
        int y = 54;
        int x = Math.max(8, width / 2 - 145);
        int maxLines = height <= 165 ? 4 : 6;
        for (int i = 0; i < Math.min(maxLines, compatibilityLines.size()); i++) {
            graphics.drawString(font, fit(compatibilityLines.get(i), width - 16), x, y + i * 12, 0xE0E0E0, false);
        }
        if (!statusMessage.isBlank() && height > 165) {
            graphics.drawCenteredString(font, fit(statusMessage, width - 18), width / 2,
                    Math.max(0, height - 39), 0xFFFF80);
        }
    }

    private static String present(String id) {
        try {
            return ModList.get().isLoaded(id) ? "detected" : "not detected";
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }

    private String fit(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        return font.plainSubstrByWidth(text, maxWidth);
    }

    private static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private static String profileLabel(PerformanceProfile profile) {
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
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
