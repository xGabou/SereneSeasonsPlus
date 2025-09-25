package com.Gabou.sereneseasonsplus;

import net.neoforged.fml.loading.LoadingModList;

public class PerfChecker {
    public static boolean hasPerfMod() {
        return LoadingModList.get().getModFileById("embeddium") != null
                || LoadingModList.get().getModFileById("xenon") != null
                || LoadingModList.get().getModFileById("chloride") != null
                || LoadingModList.get().getModFileById("sodium") != null;
    }
}
