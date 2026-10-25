package dev.iustin.immersiveoptimizator;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for Immersive Optimizator.
 *
 * <p>Client-side diagnostics and reversible adaptive graphics profiles are initialized here.</p>
 */
@Mod(ImmersiveOptimizator.MOD_ID)
public final class ImmersiveOptimizator {
    public static final String MOD_ID = "immersiveoptimizator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ImmersiveOptimizator() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OptimizerConfig.SPEC);
        LOGGER.info("Immersive Optimizator initialized. Dashboard key: O. Diagnostics remain local.");
    }
}
