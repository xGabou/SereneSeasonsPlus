package com.Gabou.sereneseasonsplus.util;

public final class DebugMode {
    private static volatile boolean enabled = false;

    private DebugMode() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }
}

