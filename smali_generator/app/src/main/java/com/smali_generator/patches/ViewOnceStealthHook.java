package com.smali_generator.patches;

import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 5c + 5d: View-Once Stealth (Prioritas Tertinggi)
 *
 * Tujuan: Mencegah pengirim tahu bahwa kita sudah membuka foto/video view-once.
 *
 * - Intersep method yang kirim receipt "viewed" SEBELUM network call (5c).
 * - Intersep receipt "viewed" yang masuk ke sisi pengirim (5d).
 * - Sembunyikan flags/viewed-state di local database.
 */
public class ViewOnceStealthHook implements Hook {

    private static final String TAG = "WAPatch.ViewOnceStealth";

    // Sisi penerima: method kirim receipt viewed
    static Method sendReceiptMethod;
    // Sisi pengirim: method proses receipt viewed
    static Method processReceiptMethod;
    // Flag lokal viewed
    static Method setViewedMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading ViewOnceStealthHook...");
        if (!ModSettings.isViewOnceStealthEnabled()) {
            Log.i(TAG, "ViewOnceStealth disabled, skip");
            return;
        }
        try {
            // 1. Hook kirim receipt viewed (penerima)
            String sClass = "{{VIEW_ONCE_SEND_RECEIPT_CLASS}}";
            String sMethod = "{{VIEW_ONCE_SEND_RECEIPT_METHOD}}";

            if (!sClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(sClass);
                if (c != null) {
                    sendReceiptMethod = ReflectionUtils.findMethod(c, sMethod, Object.class, Object.class);
                }
            }
            if (sendReceiptMethod == null) sendReceiptMethod = scanSendReceipt();

            if (sendReceiptMethod != null) {
                Method hook = ViewOnceStealthHook.class.getDeclaredMethod(
                        "sendReceiptHook", Object.class, Object.class, Object.class);
                Method backup = ViewOnceStealthHook.class.getDeclaredMethod(
                        "sendReceiptBackup", Object.class, Object.class, Object.class);
                HookMain.backupAndHook(sendReceiptMethod, hook, backup);
                Log.i(TAG, "Hooked sendReceipt");
            } else {
                Log.e(TAG, "sendReceiptMethod not found");
            }

            // 2. Hook proses receipt viewed (pengirim)
            String pClass = "{{VIEW_ONCE_PROCESS_RECEIPT_CLASS}}";
            String pMethod = "{{VIEW_ONCE_PROCESS_RECEIPT_METHOD}}";

            if (!pClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(pClass);
                if (c != null) {
                    processReceiptMethod = ReflectionUtils.findMethod(c, pMethod, Object.class);
                }
            }
            if (processReceiptMethod == null) processReceiptMethod = scanProcessReceipt();

            if (processReceiptMethod != null) {
                Method hook = ViewOnceStealthHook.class.getDeclaredMethod(
                        "processReceiptHook", Object.class, Object.class);
                Method backup = ViewOnceStealthHook.class.getDeclaredMethod(
                        "processReceiptBackup", Object.class, Object.class);
                HookMain.backupAndHook(processReceiptMethod, hook, backup);
                Log.i(TAG, "Hooked processReceipt");
            } else {
                Log.e(TAG, "processReceiptMethod not found");
            }

            // 3. Hook set flag viewed lokal
            String vClass = "{{VIEW_ONCE_STATUS_CLASS}}";
            String vMethod = "{{VIEW_ONCE_STATUS_METHOD}}";

            if (!vClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(vClass);
                if (c != null) {
                    setViewedMethod = ReflectionUtils.findMethod(c, vMethod, Object.class, boolean.class);
                }
            }
            if (setViewedMethod == null) setViewedMethod = scanSetViewed();

            if (setViewedMethod != null) {
                Method hook = ViewOnceStealthHook.class.getDeclaredMethod(
                        "setViewedHook", Object.class, boolean.class);
                Method backup = ViewOnceStealthHook.class.getDeclaredMethod(
                        "setViewedBackup", Object.class, boolean.class);
                HookMain.backupAndHook(setViewedMethod, hook, backup);
                Log.i(TAG, "Hooked setViewed");
            } else {
                Log.e(TAG, "setViewedMethod not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading ViewOnceStealthHook: " + e.getMessage());
        }
    }

    /**
     * 5c: Sisi penerima — cegah kirim receipt "viewed" untuk view-once.
     * Jika view-once, return tanpa panggil backup.
     * Jika bukan, panggil backup (kirim receipt normal).
     */
    public static void sendReceiptHook(Object self, Object messageInfo, Object userJid) {
        if (!ModSettings.isViewOnceStealthEnabled()) {
            sendReceiptBackup(self, messageInfo, userJid);
            return;
        }
        try {
            boolean isViewOnce = isViewOnceMessage(messageInfo);
            if (isViewOnce) {
                Log.i(TAG, "BLOCKED viewed receipt for view-once");
                return; // Jangan kirim receipt
            }
            sendReceiptBackup(self, messageInfo, userJid);
        } catch (Exception e) {
            Log.e(TAG, "sendReceiptHook error: " + e.getMessage());
            sendReceiptBackup(self, messageInfo, userJid);
        }
    }

    public static void sendReceiptBackup(Object self, Object messageInfo, Object userJid) {
        // YAHFA inject original
    }

    /**
     * 5d: Sisi pengirim — buang receipt "viewed" untuk view-once.
     * Jika tipe receipt adalah viewed untuk view-once, return tanpa panggil backup.
     */
    public static void processReceiptHook(Object self, Object receiptInfo) {
        if (!ModSettings.isViewOnceStealthEnabled()) {
            processReceiptBackup(self, receiptInfo);
            return;
        }
        try {
            boolean isViewOnceReceipt = isViewOnceReceipt(receiptInfo);
            if (isViewOnceReceipt) {
                Log.i(TAG, "BLOCKED incoming viewed receipt (sender-side)");
                return; // Buang receipt
            }
            processReceiptBackup(self, receiptInfo);
        } catch (Exception e) {
            Log.e(TAG, "processReceiptHook error: " + e.getMessage());
            processReceiptBackup(self, receiptInfo);
        }
    }

    public static void processReceiptBackup(Object self, Object receiptInfo) {
        // YAHFA inject original
    }

    /**
     * Sembunyikan flag lokal "viewed".
     * Jika view-once, jangan set flag viewed = true.
     */
    public static void setViewedHook(Object self, boolean viewed) {
        if (!ModSettings.isViewOnceStealthEnabled()) {
            setViewedBackup(self, viewed);
            return;
        }
        try {
            // Selalu set false untuk view-once supaya UI tidak tandai sebagai dilihat
            // dan tidak trigger sync receipt.
            Log.i(TAG, "Forcing local viewed flag to FALSE for view-once");
            setViewedBackup(self, false);
        } catch (Exception e) {
            Log.e(TAG, "setViewedHook error: " + e.getMessage());
            setViewedBackup(self, viewed);
        }
    }

    public static void setViewedBackup(Object self, boolean viewed) {
        // YAHFA inject original
    }

    /* ---------- Helpers ---------- */

    private static boolean isViewOnceMessage(Object messageInfo) {
        if (messageInfo == null) return false;
        try {
            Field f = ReflectionUtils.findField(messageInfo.getClass(), "viewOnce_");
            if (f != null) return (boolean) f.get(messageInfo);
            Field mt = ReflectionUtils.findField(messageInfo.getClass(), "messageType_");
            if (mt != null) return messageInfo.getClass().getName().toLowerCase().contains("viewonce");
        } catch (Exception e) {
            Log.e(TAG, "isViewOnceMessage error: " + e.getMessage());
        }
        return false;
    }

    private static boolean isViewOnceReceipt(Object receiptInfo) {
        if (receiptInfo == null) return false;
        try {
            // Cek tipe receipt — viewed biasanya ada field type_
            Field typeField = ReflectionUtils.findField(receiptInfo.getClass(), "type_");
            if (typeField != null) {
                Object typeObj = typeField.get(receiptInfo);
                if (typeObj instanceof Integer) {
                    int t = (Integer) typeObj;
                    // Tipe receipt viewed perlu dicari via reverse engineering
                    if (t == 5 || t == 13) {
                        // Cek juga apakah pesan yang di-receipt adalah view-once
                        Field keyField = ReflectionUtils.findField(receiptInfo.getClass(), "key_");
                        if (keyField != null) {
                            Object key = keyField.get(receiptInfo);
                            return isViewOnceMessage(key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isViewOnceReceipt error: " + e.getMessage());
        }
        return false;
    }

    private static Method scanSendReceipt() { return null; }
    private static Method scanProcessReceipt() { return null; }
    private static Method scanSetViewed() { return null; }

    @Override
    public void unload() {
        Log.i(TAG, "ViewOnceStealthHook unloaded");
    }
}
