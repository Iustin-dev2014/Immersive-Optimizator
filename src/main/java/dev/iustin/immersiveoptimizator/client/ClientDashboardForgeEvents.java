package dev.iustin.immersiveoptimizator.client;

import dev.iustin.immersiveoptimizator.ImmersiveOptimizator;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opens the dashboard when its key is pressed, without interrupting another screen. */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDashboardForgeEvents {
    private ClientDashboardForgeEvents() {}
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientDashboardEvents.OPEN_DASHBOARD.consumeClick()) {
            if (minecraft.screen == null) minecraft.setScreen(new ClientDashboardScreen(null));
        }
    }
}
