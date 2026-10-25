package dev.iustin.immersiveoptimizator.diagnostics;

import dev.iustin.immersiveoptimizator.config.OptimizerConfig;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/** Presence checks and a small set of known conflict warnings; not a universal conflict detector. */
public final class CompatibilityDiagnostics {
    private CompatibilityDiagnostics() {}

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        if (!OptimizerConfig.COMPATIBILITY_WARNINGS.get()) return warnings;

        boolean embeddium = isLoaded("embeddium");
        boolean optifine = isLoaded("optifine");
        boolean modernFix = isLoaded("modernfix");
        boolean rubidium = isLoaded("rubidium");
        boolean sodium = isLoaded("sodium");

        if (embeddium && optifine) {
            warnings.add("Embeddium and OptiFine are a potentially conflicting renderer combination.");
        }
        if (modernFix && optifine) {
            warnings.add("ModernFix documents that OptiFine is unsupported alongside it.");
        }
        if (embeddium && rubidium) {
            warnings.add("Embeddium and Rubidium provide overlapping renderers; remove one and retest.");
        }
        if (embeddium && sodium) {
            warnings.add("Embeddium and Sodium-family renderers may overlap; verify loader compatibility.");
        }
        return warnings;
    }

    public static String summary() {
        List<String> warnings = warnings();
        if (warnings.isEmpty()) {
            return "No known conflict from the small built-in rule set. This is not proof that every mod is compatible.";
        }
        return String.join(" ", warnings);
    }
}
