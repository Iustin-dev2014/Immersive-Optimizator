package dev.iustin.immersiveoptimizator.client;

import dev.iustin.immersiveoptimizator.ImmersiveOptimizator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the mod's entry button to vanilla Video Settings without replacing that screen. */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VideoSettingsButtonEvents {
    private VideoSettingsButtonEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof VideoSettingsScreen screen)) {
            return;
        }

        int width = 122;
        int x = Math.max(4, screen.width - width - 4);
        Button button = Button.builder(
                        Component.translatable("gui.immersiveoptimizator.optimization_settings"),
                        ignored -> Minecraft.getInstance().setScreen(new ClientDashboardScreen(screen)))
                .bounds(x, 4, width, 20)
                .build();
        event.addListener(button);
    }
}
