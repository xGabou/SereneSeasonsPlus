package net.Gabou.sereneseasonsplus.client;

import net.fabricmc.loader.api.FabricLoader;

public class PerfChecker {
    public static boolean hasPerfMod() {
        return FabricLoader.getInstance().isModLoaded("embeddium")
                || FabricLoader.getInstance().isModLoaded("sodium")   // common on Fabric
                || FabricLoader.getInstance().isModLoaded("iris")     // shaders helper, often paired with sodium
                || FabricLoader.getInstance().isModLoaded("lithium")  // server-side perf
                || FabricLoader.getInstance().isModLoaded("starlight"); // lighting perf
    }
}
