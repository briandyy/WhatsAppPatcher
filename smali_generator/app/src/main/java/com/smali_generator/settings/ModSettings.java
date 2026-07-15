package com.smali_generator.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Manajemen setting mod menggunakan SharedPreferences.
 * Semua fitur bisa di-toggle on/off saat runtime.
 */
public class ModSettings {

    private static final String PREFS_NAME = "whatsapp_mod_settings";
    private static final String TAG = "WAPatch.Settings";

    // Keys SharedPreferences
    public static final String KEY_VIEW_ONCE_STEALTH    = "view_once_stealth";
    public static final String KEY_VIEW_ONCE_PERMANENT   = "view_once_permanent";
    public static final String KEY_VIEW_ONCE_DOWNLOAD    = "view_once_download";
    public static final String KEY_GROUP_STATUS          = "group_status";
    public static final String KEY_ANTI_DELETE_MSG       = "anti_delete_msg";
    public static final String KEY_ANTI_DELETE_STATUS    = "anti_delete_status";
    public static final String KEY_DOWNLOAD_STATUS       = "download_status";
    public static final String KEY_HIDE_BLUE_TICK       = "hide_blue_tick";
    public static final String KEY_HIDE_TYPING          = "hide_typing";
    public static final String KEY_HIDE_RECORDING        = "hide_recording";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Log.i(TAG, "ModSettings initialized");
        }
    }

    public static boolean isEnabled(String key, boolean defaultValue) {
        if (prefs == null) {
            return defaultValue;
        }
        return prefs.getBoolean(key, defaultValue);
    }

    public static void setEnabled(String key, boolean value) {
        if (prefs != null) {
            prefs.edit().putBoolean(key, value).apply();
        }
    }

    public static boolean isViewOnceStealthEnabled() {
        return isEnabled(KEY_VIEW_ONCE_STEALTH, true);
    }

    public static boolean isViewOncePermanentEnabled() {
        return isEnabled(KEY_VIEW_ONCE_PERMANENT, true);
    }

    public static boolean isViewOnceDownloadEnabled() {
        return isEnabled(KEY_VIEW_ONCE_DOWNLOAD, true);
    }

    public static boolean isGroupStatusEnabled() {
        return isEnabled(KEY_GROUP_STATUS, true);
    }

    public static boolean isAntiDeleteMsgEnabled() {
        return isEnabled(KEY_ANTI_DELETE_MSG, true);
    }

    public static boolean isAntiDeleteStatusEnabled() {
        return isEnabled(KEY_ANTI_DELETE_STATUS, true);
    }

    public static boolean isDownloadStatusEnabled() {
        return isEnabled(KEY_DOWNLOAD_STATUS, true);
    }

    public static boolean isHideBlueTickEnabled() {
        return isEnabled(KEY_HIDE_BLUE_TICK, true);
    }

    public static boolean isHideTypingEnabled() {
        return isEnabled(KEY_HIDE_TYPING, true);
    }

    public static boolean isHideRecordingEnabled() {
        return isEnabled(KEY_HIDE_RECORDING, true);
    }
}
