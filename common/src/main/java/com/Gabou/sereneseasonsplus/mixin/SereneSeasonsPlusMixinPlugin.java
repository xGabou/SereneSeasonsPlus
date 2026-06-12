package com.Gabou.sereneseasonsplus.mixin;


import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SereneSeasonsPlusMixinPlugin implements IMixinConfigPlugin {
    private static final String SERVER_LEVEL_MIXIN = "com.Gabou.sereneseasonsplus.mixin.ServerLevelMixin";
    private static final String SNOW_REAL_MAGIC_MIXIN = "com.Gabou.sereneseasonsplus.mixin.SnowRealMagicCompatMixin";
    private static final String BIOME_FREEZING_MIXIN = "com.Gabou.sereneseasonsplus.mixin.BiomeFreezingMixin";
    private static final String SEASON_HOOKS_MIXIN = "com.Gabou.sereneseasonsplus.mixin.SeasonHooksMixin";
    private static final String SERVER_LEVEL_WEATHER_CYCLE_MIXIN = "com.Gabou.sereneseasonsplus.mixin.ServerLevelWeatherCycleMixin";
    private static final String WEATHER_STATE_MIXIN = "com.Gabou.sereneseasonsplus.mixin.WeatherStateMixin";
    private static final String CLIENT_LEVEL_WEATHER_MIXIN = "com.Gabou.sereneseasonsplus.mixin.client.ClientLevelWeatherMixin";

    private boolean snowRealMagicLoaded;
    private boolean projectAtmosphereLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        snowRealMagicLoaded = detectSnowRealMagic();
        projectAtmosphereLoaded = detectProjectAtmosphere();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (SERVER_LEVEL_MIXIN.equals(mixinClassName)) {
            return !snowRealMagicLoaded;
        }
        if (SNOW_REAL_MAGIC_MIXIN.equals(mixinClassName)) {
            return snowRealMagicLoaded;
        }
        if (projectAtmosphereLoaded) {
            if (BIOME_FREEZING_MIXIN.equals(mixinClassName)
                    || SEASON_HOOKS_MIXIN.equals(mixinClassName)
                    || SERVER_LEVEL_WEATHER_CYCLE_MIXIN.equals(mixinClassName)
                    || WEATHER_STATE_MIXIN.equals(mixinClassName)
                    || CLIENT_LEVEL_WEATHER_MIXIN.equals(mixinClassName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // no-op
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private boolean detectSnowRealMagic() {
        return isModLoaded("net.minecraftforge.fml.ModList")
                || isModLoaded("net.neoforged.fml.ModList")
                || isFabricModLoaded();
    }

    private boolean detectProjectAtmosphere() {
        return isModLoaded("net.minecraftforge.fml.ModList", "projectatmosphere")
                || isModLoaded("net.neoforged.fml.ModList", "projectatmosphere")
                || isFabricModLoaded("projectatmosphere");
    }

    private boolean isModLoaded(String modListClassName) {
        return isModLoaded(modListClassName, "snowrealmagic");
    }

    private boolean isModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName);
            Object modList = modListClass.getMethod("get").invoke(null);
            Object result = modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
            return Boolean.TRUE.equals(result);
        } catch (ClassNotFoundException e) {
            return false; // different loader
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isFabricModLoaded() {
        return isFabricModLoaded("snowrealmagic");
    }

    private boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object result = loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
            return Boolean.TRUE.equals(result);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
