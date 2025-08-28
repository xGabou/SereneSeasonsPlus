package com.Gabou.sereneseasonsplus.api;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility to toggle the snowstorm state in the Serene Seasons Plus config.
 *
 * <p>External mods can call these helpers to enable or disable a
 * snowstorm, which affects how the snow pilling logic behaves.</p>
 */
public class SnowstormHelper {
    private static final Logger LOGGER = LogManager.getLogger("SnowstormHelper");

    private SnowstormHelper() {
    }

    /**
     * Enable snowstorm mode and set the desired intensity.
     *
     * @param intensity intensity value (0-100)
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
     * Writes snowstorm settings to sereneseasonsplus-common.toml and reloads
     * the config so the changes take effect immediately.
     */
    private static void updateSnowstormConfig(boolean enabled, int intensity) {
        try {
            // Update in-memory config values
            SereneExtendedConfig.SNOWSTORM_ENABLED.set(enabled);
            SereneExtendedConfig.SNOWSTORM_INTENSITY.set(intensity);

            // Persist to config file
            var path = FMLPaths.CONFIGDIR.get().resolve(SereneSeasonsPlus.MODID + "-common.toml");
            com.electronwill.nightconfig.core.file.CommentedFileConfig cfg = com.electronwill.nightconfig.core.file.CommentedFileConfig.builder(path).sync().autosave().build();
            cfg.load();
            cfg.set("snowstorm.enabled", enabled);
            cfg.set("snowstorm.intensity", intensity);
            cfg.save();
            cfg.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to update Serene Seasons Plus config: {}", e.getMessage());
        }
    }
}

