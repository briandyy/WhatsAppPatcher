package com.smali_generator.patches;

import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 6: Sembunyikan Blue Tick (Read Receipts) dengan "Kirim Saat Balas"
 *
 * Hook method yang mengirim read receipts.
 * Cegah read receipts dikirim secara default.
 * Namun saat user benar-benar membalas pesan, kirim read receipt untuk pesan tersebut.
 *
 * Implementasi: Gunakan thread-local flag "shouldSendReceipt".
 * Saat hook sendReadReceipt(), jika flag == false → block.
 * Jika user mengetik balasan → set flag = true di thread ini.
 */
public class HideBlueTickHook implements Hook {

    private static final String TAG = "WAPatch.HideBlueTick";

    // Flag thread-local: true saat user sedang membalas pesan
    static final ThreadLocal<Boolean> replyMode = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    static Method sendReadReceiptMethod;
    static Method sendMessageMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading HideBlueTickHook...");
        if (!ModSettings.isHideBlueTickEnabled()) {
            Log.i(TAG, "HideBlueTick disabled, skip");
            return;
        }
        try {
            // Hook sendReadReceipt
            String receiptClass = "{{SEND_READ_RECEIPT_CLASS}}";
            String receiptMethod = "{{SEND_READ_RECEIPT_METHOD}}";

            if (!receiptClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(receiptClass);
                if (c != null) {
                    sendReadReceiptMethod = ReflectionUtils.findMethod(c, receiptMethod,
                            Object.class, Object.class);
                }
            }

            if (sendReadReceiptMethod == null) {
                sendReadReceiptMethod = scanReadReceipt();
            }

            if (sendReadReceiptMethod != null) {
                Method hook = HideBlueTickHook.class.getDeclaredMethod(
                        "sendReadReceiptHook", Object.class, Object.class, Object.class);
                Method backup = HideBlueTickHook.class.getDeclaredMethod(
                        "sendReadReceiptBackup", Object.class, Object.class, Object.class);
                HookMain.backupAndHook(sendReadReceiptMethod, hook, backup);
                Log.i(TAG, "Hooked sendReadReceipt method");
            } else {
                Log.e(TAG, "sendReadReceiptMethod not found");
            }

            // Hook send message (saat user kirim balasan → set replyMode = true)
            String sendMsgClass = "{{SEND_MESSAGE_CLASS}}";
            String sendMsgMethod = "{{SEND_MESSAGE_METHOD}}";

            if (!sendMsgClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(sendMsgClass);
                if (c != null) {
                    sendMessageMethod = ReflectionUtils.findMethod(c, sendMsgMethod, Object.class);
                }
            }

            if (sendMessageMethod == null) {
                sendMessageMethod = scanSendMessage();
            }

            if (sendMessageMethod != null) {
                Method hook = HideBlueTickHook.class.getDeclaredMethod(
                        "sendMessageHook", Object.class, Object.class);
                Method backup = HideBlueTickHook.class.getDeclaredMethod(
                        "sendMessageBackup", Object.class, Object.class);
                HookMain.backupAndHook(sendMessageMethod, hook, backup);
                Log.i(TAG, "Hooked sendMessage method");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading HideBlueTickHook: " + e.getMessage());
        }
    }

    /**
     * Hook sendReadReceipt.
     * Jika replyMode == false: block.
     * Jika replyMode == true: allow (panggil original).
     */
    public static void sendReadReceiptHook(Object self, Object chatJid, Object messageId) {
        if (!ModSettings.isHideBlueTickEnabled()) {
            sendReadReceiptBackup(self, chatJid, messageId);
            return;
        }
        try {
            boolean isReplying = replyMode.get();
            if (isReplying) {
                Log.i(TAG, "ALLOWED read receipt (user is replying)");
                sendReadReceiptBackup(self, chatJid, messageId);
            } else {
                Log.i(TAG, "BLOCKED read receipt (not replying)");
                // Jangan panggil backup
            }
        } catch (Exception e) {
            Log.e(TAG, "sendReadReceiptHook error: " + e.getMessage());
            sendReadReceiptBackup(self, chatJid, messageId);
        }
    }

    public static void sendReadReceiptBackup(Object self, Object chatJid, Object messageId) {
        // YAHFA inject original
    }

    /**
     * Hook sendMessage: set replyMode = true di thread ini.
     * Ini menandakan user sedang membalas pesan.
     */
    public static void sendMessageHook(Object self, Object message) {
        if (!ModSettings.isHideBlueTickEnabled()) {
            sendMessageBackup(self, message);
            return;
        }
        try {
            // Set flag bahwa user sedang membalas
            replyMode.set(true);
            Log.d(TAG, "replyMode set to TRUE for this thread");
            sendMessageBackup(self, message);
            // Reset setelah beberapa saat (opsional)
            // Tapi kita biarkan manual di thread lain
        } catch (Exception e) {
            Log.e(TAG, "sendMessageHook error: " + e.getMessage());
            sendMessageBackup(self, message);
        }
    }

    public static void sendMessageBackup(Object self, Object message) {
        // YAHFA inject original
    }

    private static Method scanReadReceipt() {
        return null;
    }

    private static Method scanSendMessage() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "HideBlueTickHook unloaded");
    }
}
