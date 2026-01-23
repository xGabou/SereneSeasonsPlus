package com.Gabou.sereneseasonsplus.mixin;


import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SereneSeasonsPlusMixinPlugin implements IMixinConfigPlugin {
    private static final String SERVER_LEVEL_MIXIN = "com.Gabou.sereneseasonsplus.mixin.ServerLevelMixin";
    private static final String SNOW_REAL_MAGIC_MIXIN = "com.Gabou.sereneseasonsplus.mixin.SnowRealMagicCompatMixin";

    private boolean snowRealMagicLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        snowRealMagicLoaded = detectSnowRealMagic();
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

    /**
     * @param s
     * @param classNode
     * @param s1
     * @param iMixinInfo
     */
    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    /**
     * @param s
     * @param classNode
     * @param s1
     * @param iMixinInfo
     */
    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }



    private boolean detectSnowRealMagic() {
        return isModLoaded("net.minecraftforge.fml.ModList")
                || isModLoaded("net.neoforged.fml.ModList")
                || isFabricModLoaded();
    }

    private boolean isModLoaded(String modListClassName) {
        try {
            Class<?> modListClass = Class.forName(modListClassName);
            Object modList = modListClass.getMethod("get").invoke(null);
            Object result = modListClass.getMethod("isLoaded", String.class).invoke(modList, "snowrealmagic");
            return Boolean.TRUE.equals(result);
        } catch (ClassNotFoundException e) {
            return false; // different loader
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isFabricModLoaded() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object result = loaderClass.getMethod("isModLoaded", String.class).invoke(loader, "snowrealmagic");
            return Boolean.TRUE.equals(result);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
