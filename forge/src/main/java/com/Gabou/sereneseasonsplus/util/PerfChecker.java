package com.Gabou.sereneseasonsplus.util;

import net.minecraftforge.fml.ModList;

public class PerfChecker {
    public static boolean hasPerfMod() {
        return ModList.get().getModFileById("embeddium") != null
                || ModList.get().getModFileById("xenon") != null
                || ModList.get().getModFileById("chloride") != null;
    }
}
