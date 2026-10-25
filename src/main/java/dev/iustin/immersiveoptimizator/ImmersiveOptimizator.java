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
 * <p>Phase 1 establishes the Forge mod, configuration foundation and build
 * pipeline. Performance-changing systems will be introduced in later phases
 * behind explicit configuration and compatibility checks.</p>
 */
@Mod(ImmersiveOptimizator.MOD_ID)
public final class ImmersiveOptimizator {
    public static final String MOD_ID = "immersiveoptimizator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ImmersiveOptimizator() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OptimizerConfig.SPEC);
        LOGGER.info("Immersive Optimizator initialized. Experimental optimizations remain opt-in.");
    }
}
