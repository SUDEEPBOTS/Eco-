package org.telegram.messenger;

import android.content.SharedPreferences;

public class GhostMode {

    public static boolean isEnabled(int account) {
        return getPrefs().getBoolean("ghost_mode_" + account, false);
    }

    public static void setEnabled(int account, boolean enabled) {
        getPrefs().edit().putBoolean("ghost_mode_" + account, enabled).apply();
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext
            .getSharedPreferences("ghost_mode", 0);
    }
}
