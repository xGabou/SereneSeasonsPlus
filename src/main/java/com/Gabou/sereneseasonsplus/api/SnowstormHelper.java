package com.Gabou.sereneseasonsplus.api;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
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
     * Writes snowstorm settings to the Fabric JSON config and saves it.
     */
    private static void updateSnowstormConfig(boolean enabled, int intensity) {
        try {
            SereneExtendedConfig.SNOWSTORM_ENABLED.set(enabled);
            SereneExtendedConfig.SNOWSTORM_INTENSITY.set(intensity);
            SereneExtendedConfig.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to update Serene Seasons Plus config: {}", e.getMessage());
        }
    }
}

