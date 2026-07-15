package com.smali_generator;

import android.content.Context;
import android.util.Log;

import com.smali_generator.patches.AntiDeleteHook;
import com.smali_generator.patches.DownloadStatusHook;
import com.smali_generator.patches.GroupStatusHook;
import com.smali_generator.patches.HideBlueTickHook;
import com.smali_generator.patches.HideRecordingHook;
import com.smali_generator.patches.HideTypingHook;
import com.smali_generator.patches.ViewOnceDownloadHook;
import com.smali_generator.patches.ViewOncePermanentHook;
import com.smali_generator.patches.ViewOnceStealthHook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class TheAmazingPatch {

    static final String TAG = "WAPatch";

    static AtomicBoolean is_loaded = new AtomicBoolean(false);

    public static void on_load() {
        if (is_loaded.getAndSet(true)) {
            return;
        }

        Log.e(TAG, "=== WhatsApp Patch Loaded ===");

        Context context = ReflectionUtils.getAppContext();
        if (context != null) {
            ModSettings.init(context);
        }

        // Daftarkan semua hook secara berurutan.
        // Jika ada hook yang gagal load karena method tidak ditemukan,
        // log error dan lanjutkan — jangan crash.
        Hook[] hooks = {
                new ViewOnceStealthHook(),    // Prioritas Tertinggi
                new ViewOncePermanentHook(),
                new ViewOnceDownloadHook(),
                new GroupStatusHook(),        // Prioritas Tinggi
                new AntiDeleteHook(),           // Prioritas Sedang (pesan + status)
                new DownloadStatusHook(),
                new HideBlueTickHook(),
                new HideTypingHook(),
                new HideRecordingHook(),
        };

        for (Hook hook : hooks) {
            try {
                hook.load();
                Log.i(TAG, hook.getClass().getSimpleName() + " loaded successfully");
            } catch (Exception e) {
                Log.e(TAG, hook.getClass().getSimpleName() + " failed: " + e.getMessage());
            }
        }
    }
}
