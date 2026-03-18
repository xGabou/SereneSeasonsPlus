package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonPlusCommon;
import net.Gabou.gaboulibs.util.CompatUtils;
import net.Gabou.gaboulibs.util.InitGuards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeasonalTreesFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger("SeasonalTreesFeature");
    private static final String INIT_KEY = SereneSeasonPlusCommon.MODID + ":seasonal_trees";

    private SeasonalTreesFeature() {
    }

    public static void bootstrap() {
        if (!InitGuards.tryStart(INIT_KEY)) {
            return;
        }

        CompatUtils.PaSspHost host = CompatUtils.getPaSspHost();
        boolean isHost = CompatUtils.shouldThisModHostPaSspFeature(SereneSeasonPlusCommon.MODID);
        String reason = resolveReason(host);
        LOGGER.info("Seasonal trees init: {} (elected host: {}, reason: {})", isHost ? "host" : "not host", host, reason);
        if (!isHost) {
            return;
        }

        registerMutatingHooks();
    }

    private static String resolveReason(CompatUtils.PaSspHost host) {
        boolean paLoaded = CompatUtils.isProjectAtmosphereLoaded();
        boolean sspLoaded = CompatUtils.isSereneSeasonsPlusLoaded();
        if (paLoaded && sspLoaded) {
            if (host == CompatUtils.PaSspHost.SERENE_SEASONS_PLUS) {
                return "both loaded default SSP";
            }
            if (host == CompatUtils.PaSspHost.PROJECT_ATMOSPHERE) {
                return "override active";
            }
            return "both loaded";
        }
        if (sspLoaded) {
            return "only SSP loaded";
        }
        if (paLoaded) {
            return "only PA loaded";
        }
        return "neither loaded";
    }

    private static void registerMutatingHooks() {
    }
}
