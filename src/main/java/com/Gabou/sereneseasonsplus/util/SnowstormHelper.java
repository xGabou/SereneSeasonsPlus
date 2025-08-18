package com.Gabou.sereneseasonsplus.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Utility to toggle the snowstorm state in the Serene Seasons Plus config.
 *
 * <p>The Project Atmosphere mod can call these helpers to enable or disable a
 * snowstorm, which affects how the snow pilling logic behaves.</p>
 */
public class SnowstormHelper {
    private static final Logger LOGGER = LogManager.getLogger("SnowstormHelper");

    private SnowstormHelper() {}

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
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("sereneseasonsplus-common.toml");
        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath).sync().build()) {
            config.load();
            config.set("snowstorm.enabled", enabled);
            config.set("snowstorm.intensity", intensity);
            config.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to update Serene Seasons Plus config: {}", e.getMessage());
        }

        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FMLPaths.CONFIGDIR.get());
    }
}

