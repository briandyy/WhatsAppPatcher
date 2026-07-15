package com.smali_generator.patches;

import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 5a: View-Once Permanent
 *
 * Tujuan: Membuat media view-once tetap terlihat di chat, seolah-olah pesan biasa.
 *
 * Hook method yang menghapus media view-once setelah dilihat.
 * Media harus tetap ada di chat (incoming & outgoing).
 */
public class ViewOncePermanentHook implements Hook {

    private static final String TAG = "WAPatch.ViewOncePerm";

    // Method yang menghapus media view-once dari local storage
    static Method deleteViewOnceMethod;

    // Method yang set viewOnce_ flag = true di pesan
    static Method setViewOnceFlagMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading ViewOncePermanentHook...");
        if (!ModSettings.isViewOncePermanentEnabled()) {
            Log.i(TAG, "ViewOncePermanent disabled, skip");
            return;
        }
        try {
            // --- Hook 1: Cegah penghapusan media view-once ---
            String delClass = "{{VIEW_ONCE_DELETE_CLASS}}";
            String delMethod = "{{VIEW_ONCE_DELETE_METHOD}}";
            String delSig = "{{VIEW_ONCE_DELETE_SIG}}";

            Class<?> delClassRef = null;
            if (!delClass.startsWith("{{")) {
                delClassRef = ReflectionUtils.findClass(delClass);
                if (delClassRef != null) {
                    deleteViewOnceMethod = ReflectionUtils.findMethod(
                            delClassRef, delMethod, Object.class);
                }
            }

            // Fallback: scan berdasarkan field reference ke viewOnce
            if (deleteViewOnceMethod == null) {
                deleteViewOnceMethod = scanDeleteMethod();
            }

            if (deleteViewOnceMethod != null) {
                Method hook = ViewOncePermanentHook.class.getDeclaredMethod(
                        "deleteViewOnceHook", Object.class, Object.class);
                Method backup = ViewOncePermanentHook.class.getDeclaredMethod(
                        "deleteViewOnceBackup", Object.class, Object.class);
                HookMain.backupAndHook(deleteViewOnceMethod, hook, backup);
                Log.i(TAG, "Hooked deleteViewOnce method");
            } else {
                Log.e(TAG, "deleteViewOnceMethod not found");
            }

            // --- Hook 2: Set viewOnce_ = false di pesan yang masuk ---
            // Ini bekerja sama dengan DecryptProtobuf — setelah decrypt,
            // ubah field viewOnce_ dari true ke false supaya app anggap ini pesan biasa.
            String flagClass = "{{VIEW_ONCE_FLAG_CLASS}}";
            String flagMethod = "{{VIEW_ONCE_FLAG_METHOD}}";

            Class<?> flagClassRef = null;
            if (!flagClass.startsWith("{{")) {
                flagClassRef = ReflectionUtils.findClass(flagClass);
                if (flagClassRef != null) {
                    setViewOnceFlagMethod = ReflectionUtils.findMethod(
                            flagClassRef, flagMethod, Object.class, boolean.class);
                }
            }

            if (setViewOnceFlagMethod == null) {
                setViewOnceFlagMethod = scanSetFlagMethod();
            }

            if (setViewOnceFlagMethod != null) {
                Method hook = ViewOncePermanentHook.class.getDeclaredMethod(
                        "setViewOnceFlagHook", Object.class, boolean.class);
                Method backup = ViewOncePermanentHook.class.getDeclaredMethod(
                        "setViewOnceFlagBackup", Object.class, boolean.class);
                HookMain.backupAndHook(setViewOnceFlagMethod, hook, backup);
                Log.i(TAG, "Hooked setViewOnceFlag method");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading ViewOncePermanentHook: " + e.getMessage());
        }
    }

    /**
     * Hook penghapusan view-once media.
     * Jika fitur aktif, jangan panggil backup (jangan hapus).
     */
    public static void deleteViewOnceHook(Object self, Object messageInfo) {
        if (!ModSettings.isViewOncePermanentEnabled()) {
            deleteViewOnceBackup(self, messageInfo);
            return;
        }
        try {
            boolean isViewOnce = isViewOnceMessage(messageInfo);
            if (isViewOnce) {
                Log.i(TAG, "BLOCKED deletion of view-once media");
                return; // Jangan hapus
            }
            // Bukan view-once, hapus normal
            deleteViewOnceBackup(self, messageInfo);
        } catch (Exception e) {
            Log.e(TAG, "deleteViewOnceHook error: " + e.getMessage());
            deleteViewOnceBackup(self, messageInfo);
        }
    }

    public static void deleteViewOnceBackup(Object self, Object messageInfo) {
        // YAHFA inject original method body di sini
    }

    /**
     * Hook set viewOnce_ flag.
     * Saat fitur aktif, selalu set false supaya UI anggap pesan biasa.
     */
    public static void setViewOnceFlagHook(Object self, boolean value) {
        if (!ModSettings.isViewOncePermanentEnabled()) {
            setViewOnceFlagBackup(self, value);
            return;
        }
        try {
            Log.i(TAG, "Forcing viewOnce flag to FALSE (permanent view)");
            setViewOnceFlagBackup(self, false);
        } catch (Exception e) {
            Log.e(TAG, "setViewOnceFlagHook error: " + e.getMessage());
            setViewOnceFlagBackup(self, value);
        }
    }

    public static void setViewOnceFlagBackup(Object self, boolean value) {
        // YAHFA inject original method body di sini
    }

    private static boolean isViewOnceMessage(Object messageInfo) {
        if (messageInfo == null) return false;
        try {
            Field f = ReflectionUtils.findField(messageInfo.getClass(), "viewOnce_");
            if (f != null) {
                return (boolean) f.get(messageInfo);
            }
            // Fallback: cek field "mediaType_"
            Field mt = ReflectionUtils.findField(messageInfo.getClass(), "mediaType_");
            if (mt != null) {
                int type = (int) mt.get(messageInfo);
                return type == 42; // placeholder view-once media type
            }
        } catch (Exception e) {
            Log.e(TAG, "isViewOnceMessage error: " + e.getMessage());
        }
        return false;
    }

    private static Method scanDeleteMethod() {
        return null; // Artifactory generator akan isi
    }

    private static Method scanSetFlagMethod() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "ViewOncePermanentHook unloaded");
    }
}
