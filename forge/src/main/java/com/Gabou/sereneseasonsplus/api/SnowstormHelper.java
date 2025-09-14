package com.Gabou.sereneseasonsplus.api;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility to toggle the snowstorm state in the Serene Seasons Plus config.
 *
 * <p>The Project Atmosphere mod can call these helpers to enable or disable a
 * snowstorm, which affects how the snow pilling logic behaves.</p>
 */
public class SnowstormHelper {
    private static final Logger LOGGER = LogManager.getLogger("SnowstormHelper");

    /**
     * Utility holder; not instantiable.
     */
    private SnowstormHelper() {
    }

    /**
     * Enables snowstorm mode and sets the desired storm intensity.
     * Intended to be called from Project Atmosphere when a snowstorm starts.
     *
     * @param intensity 0..100 intensity value written to config
     */
    public static void startSnowstorm(int intensity) {
        updateSnowstormConfig(true, intensity);
    }

    /**
     * Disable snowstorm mode.
     */
    public static void stopSnowstorm() {
        updateSnowstormConfig(false, 0);
    }

    /**
     * Writes snowstorm settings to sereneseasonsplus-common.toml and attempts
     * to refresh Forge's config so the change applies immediately.
     */
    private static void updateSnowstormConfig(boolean enabled, int intensity) {
        try {
            SereneExtendedConfig.SNOWSTORM_ENABLED.set(enabled);
            SereneExtendedConfig.SNOWSTORM_INTENSITY.set(intensity);
            var set = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON);
            if (set == null) return;
            for (ModConfig cfg : set) {
                if (cfg.getModId().equals(SereneSeasonsPlusForge.MODID)) {
                    cfg.save(); 
                    return;
                }
            }

            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FMLPaths.CONFIGDIR.get());
        } catch (Exception e) {
            LOGGER.warn("Failed to update Serene Seasons Plus config: {}", e.getMessage());
        }
    }
}

