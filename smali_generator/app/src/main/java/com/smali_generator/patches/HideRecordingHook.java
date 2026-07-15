package com.smali_generator.patches;

import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 8: Sembunyikan Recording Indicator
 *
 * Hook method yang mengirim status "recording audio..." ke server.
 * Cegah recording indicator ditampilkan ke pihak lain.
 */
public class HideRecordingHook implements Hook {

    private static final String TAG = "WAPatch.HideRecording";

    static Method sendRecordingMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading HideRecordingHook...");
        if (!ModSettings.isHideRecordingEnabled()) {
            Log.i(TAG, "HideRecording disabled, skip");
            return;
        }
        try {
            String recClass = "{{SEND_RECORDING_CLASS}}";
            String recMethod = "{{SEND_RECORDING_METHOD}}";

            if (!recClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(recClass);
                if (c != null) {
                    sendRecordingMethod = ReflectionUtils.findMethod(c, recMethod,
                            Object.class);
                }
            }

            if (sendRecordingMethod == null) {
                sendRecordingMethod = scanRecordingMethod();
            }

            if (sendRecordingMethod != null) {
                Method hook = HideRecordingHook.class.getDeclaredMethod(
                        "sendRecordingHook", Object.class, Object.class);
                Method backup = HideRecordingHook.class.getDeclaredMethod(
                        "sendRecordingBackup", Object.class, Object.class);
                HookMain.backupAndHook(sendRecordingMethod, hook, backup);
                Log.i(TAG, "Hooked sendRecording method");
            } else {
                Log.e(TAG, "sendRecordingMethod not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading HideRecordingHook: " + e.getMessage());
        }
    }

    public static void sendRecordingHook(Object self, Object chatJid) {
        if (!ModSettings.isHideRecordingEnabled()) {
            sendRecordingBackup(self, chatJid);
            return;
        }
        try {
            Log.i(TAG, "BLOCKED recording indicator");
            // Jangan panggil backup → jangan kirim recording status
        } catch (Exception e) {
            Log.e(TAG, "sendRecordingHook error: " + e.getMessage());
            sendRecordingBackup(self, chatJid);
        }
    }

    public static void sendRecordingBackup(Object self, Object chatJid) {
        // YAHFA inject original
    }

    private static Method scanRecordingMethod() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "HideRecordingHook unloaded");
    }
}
