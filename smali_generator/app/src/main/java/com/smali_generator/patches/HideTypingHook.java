package com.smali_generator.patches;

import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 7: Sembunyikan Typing Indicator
 *
 * Hook method yang mengirim status "typing..." ke server.
 * Cegah typing indicator dikirim ke pihak lain.
 */
public class HideTypingHook implements Hook {

    private static final String TAG = "WAPatch.HideTyping";

    static Method sendTypingMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading HideTypingHook...");
        if (!ModSettings.isHideTypingEnabled()) {
            Log.i(TAG, "HideTyping disabled, skip");
            return;
        }
        try {
            String typingClass = "{{SEND_TYPING_CLASS}}";
            String typingMethod = "{{SEND_TYPING_METHOD}}";

            if (!typingClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(typingClass);
                if (c != null) {
                    sendTypingMethod = ReflectionUtils.findMethod(c, typingMethod,
                            Object.class);
                }
            }

            if (sendTypingMethod == null) {
                sendTypingMethod = scanTypingMethod();
            }

            if (sendTypingMethod != null) {
                Method hook = HideTypingHook.class.getDeclaredMethod(
                        "sendTypingHook", Object.class, Object.class);
                Method backup = HideTypingHook.class.getDeclaredMethod(
                        "sendTypingBackup", Object.class, Object.class);
                HookMain.backupAndHook(sendTypingMethod, hook, backup);
                Log.i(TAG, "Hooked sendTyping method");
            } else {
                Log.e(TAG, "sendTypingMethod not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading HideTypingHook: " + e.getMessage());
        }
    }

    public static void sendTypingHook(Object self, Object chatJid) {
        if (!ModSettings.isHideTypingEnabled()) {
            sendTypingBackup(self, chatJid);
            return;
        }
        try {
            Log.i(TAG, "BLOCKED typing indicator");
            // Jangan panggil backup → jangan kirim typing
        } catch (Exception e) {
            Log.e(TAG, "sendTypingHook error: " + e.getMessage());
            sendTypingBackup(self, chatJid);
        }
    }

    public static void sendTypingBackup(Object self, Object chatJid) {
        // YAHFA inject original
    }

    private static Method scanTypingMethod() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "HideTypingHook unloaded");
    }
}
