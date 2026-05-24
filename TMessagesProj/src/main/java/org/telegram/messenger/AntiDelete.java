package org.telegram.messenger;

import android.content.SharedPreferences;
import java.util.ArrayList;

public class AntiDelete {

    public static boolean isEnabled(long dialogId) {
        return getPrefs().getBoolean("anti_delete_" + dialogId, false);
    }

    public static void setEnabled(long dialogId, boolean enabled) {
        getPrefs().edit().putBoolean("anti_delete_" + dialogId, enabled).apply();
    }

    public static boolean isAnyEnabled() {
        return getPrefs().getAll().containsValue(true);
    }

    public static void saveMessages(int account, ArrayList<Integer> messageIds) {
        // Message IDs save karo
        SharedPreferences.Editor editor = getPrefs().edit();
        StringBuilder saved = new StringBuilder(getPrefs().getString("saved_ids", ""));
        for (Integer id : messageIds) {
            if (saved.length() > 0) saved.append(",");
            saved.append(id);
        }
        editor.putString("saved_ids", saved.toString()).apply();
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext
            .getSharedPreferences("anti_delete", 0);
    }
}
