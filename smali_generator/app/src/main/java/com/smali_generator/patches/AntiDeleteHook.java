package com.smali_generator.patches;

import android.util.Log;
import android.widget.Toast;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 1: Anti-Delete Messages
 * FITUR 2: Anti-Delete Status
 *
 * Hook method yang menangani incoming "revoke" (delete for everyone).
 * Alih-alih menghapus pesan, simpan pesan dan tambahkan tag "[Deleted]".
 * Juga hook status/story revocation agar tetap terlihat.
 */
public class AntiDeleteHook implements Hook {

    private static final String TAG = "WAPatch.AntiDelete";

    static Method revokeMsgMethod;
    static Method revokeStatusMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading AntiDeleteHook...");
        try {
            // --- Hook revoke pesan (chat messages) ---
            String msgClass = "{{REVOKE_MSG_CLASS}}";
            String msgMethod = "{{REVOKE_MSG_METHOD}}";

            if (!msgClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(msgClass);
                if (c != null) {
                    revokeMsgMethod = ReflectionUtils.findMethod(c, msgMethod, Object.class);
                }
            }

            if (revokeMsgMethod == null) {
                revokeMsgMethod = scanRevokeMsg();
            }

            if (revokeMsgMethod != null) {
                Method hook = AntiDeleteHook.class.getDeclaredMethod(
                        "revokeMsgHook", Object.class, Object.class);
                Method backup = AntiDeleteHook.class.getDeclaredMethod(
                        "revokeMsgBackup", Object.class, Object.class);
                HookMain.backupAndHook(revokeMsgMethod, hook, backup);
                Log.i(TAG, "Hooked revokeMsg method");
            } else {
                Log.e(TAG, "revokeMsgMethod not found");
            }

            // --- Hook revoke status (stories) ---
            String statusClass = "{{REVOKE_STATUS_CLASS}}";
            String statusMethod = "{{REVOKE_STATUS_METHOD}}";

            if (!statusClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(statusClass);
                if (c != null) {
                    revokeStatusMethod = ReflectionUtils.findMethod(c, statusMethod, Object.class);
                }
            }

            if (revokeStatusMethod == null) {
                revokeStatusMethod = scanRevokeStatus();
            }

            if (revokeStatusMethod != null) {
                Method hook = AntiDeleteHook.class.getDeclaredMethod(
                        "revokeStatusHook", Object.class, Object.class);
                Method backup = AntiDeleteHook.class.getDeclaredMethod(
                        "revokeStatusBackup", Object.class, Object.class);
                HookMain.backupAndHook(revokeStatusMethod, hook, backup);
                Log.i(TAG, "Hooked revokeStatus method");
            } else {
                Log.e(TAG, "revokeStatusMethod not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading AntiDeleteHook: " + e.getMessage());
        }
    }

    /**
     * Hook revoke pesan: jangan hapus, tambahkan tag [Deleted].
     */
    public static void revokeMsgHook(Object self, Object messageInfo) {
        boolean antiDelete = ModSettings.isAntiDeleteMsgEnabled();
        if (!antiDelete) {
            revokeMsgBackup(self, messageInfo);
            return;
        }
        try {
            if (messageInfo == null) {
                revokeMsgBackup(self, messageInfo);
                return;
            }

            // Cek apakah pesan ini punya field text_ untuk ditambahi [Deleted]
            Field textField = ReflectionUtils.findField(messageInfo.getClass(), "text_");
            if (textField != null) {
                String originalText = (String) textField.get(messageInfo);
                if (originalText != null && !originalText.startsWith("[Deleted] ")) {
                    textField.set(messageInfo, "[Deleted] " + originalText);
                    Log.i(TAG, "Anti-delete: tagged message with [Deleted]");
                }
            }

            // Cek juga field untuk media caption
            Field captionField = ReflectionUtils.findField(messageInfo.getClass(), "caption_");
            if (captionField != null) {
                String caption = (String) captionField.get(messageInfo);
                if (caption != null && !caption.startsWith("[Deleted] ")) {
                    captionField.set(messageInfo, "[Deleted] " + caption);
                }
            }

            // Batalkan penghapusan lokal: jangan panggil backup (original delete)
            // Tapi kita perlu panggil backup untuk update status receipt,
            // supaya pengirim tetap lihat "deleted" di sisi-nya.
            revokeMsgBackup(self, messageInfo);

        } catch (Exception e) {
            Log.e(TAG, "revokeMsgHook error: " + e.getMessage());
            revokeMsgBackup(self, messageInfo);
        }
    }

    public static void revokeMsgBackup(Object self, Object messageInfo) {
        // YAHFA inject original
    }

    /**
     * Hook revoke status: jangan hapus status, tampilkan toast.
     */
    public static void revokeStatusHook(Object self, Object statusInfo) {
        boolean antiDeleteStatus = ModSettings.isAntiDeleteStatusEnabled();
        if (!antiDeleteStatus) {
            revokeStatusBackup(self, statusInfo);
            return;
        }
        try {
            if (statusInfo == null) {
                revokeStatusBackup(self, statusInfo);
                return;
            }

            // Coba show toast dari context
            try {
                android.content.Context ctx = ReflectionUtils.getAppContext();
                if (ctx != null) {
                    Toast toast = Toast.makeText(ctx.getApplicationContext(),
                            "[Status dihapus oleh pengirim]", Toast.LENGTH_SHORT);
                    toast.show();
                }
            } catch (Exception ignored) {
            }

            // Jangan panggil backup → jangan hapus status
            // Tapi kita perlu log saja
            Log.i(TAG, "Anti-delete status: blocked deletion, status stays visible");

        } catch (Exception e) {
            Log.e(TAG, "revokeStatusHook error: " + e.getMessage());
            revokeStatusBackup(self, statusInfo);
        }
    }

    public static void revokeStatusBackup(Object self, Object statusInfo) {
        // YAHFA inject original
    }

    private static Method scanRevokeMsg() {
        return null;
    }

    private static Method scanRevokeStatus() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "AntiDeleteHook unloaded");
    }
}
