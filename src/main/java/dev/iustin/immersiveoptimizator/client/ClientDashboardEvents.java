package dev.iustin.immersiveoptimizator.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.iustin.immersiveoptimizator.ImmersiveOptimizator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Registers the dashboard hotkey; it can be rebound in Minecraft Controls. */
@Mod.EventBusSubscriber(modid = ImmersiveOptimizator.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientDashboardEvents {
    public static final KeyMapping OPEN_DASHBOARD = new KeyMapping(
            "key.immersiveoptimizator.open_dashboard", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O, "key.categories.immersiveoptimizator");
    private ClientDashboardEvents() {}
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DASHBOARD);
    }
}
