package com.smali_generator.patches;

import android.os.Environment;
import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 5b: Download View-Once Media
 *
 * Tujuan: Aktifkan kemampuan menyimpan foto/video view-once ke galeri
 * baik dari penampil media maupun dari bubble chat (jika permanent on).
 *
 * Hook method yang mencegah penyimpanan/unduhan view-once.
 * Juga intercept alur save agar bisa menyimpan resolusi penuh.
 */
public class ViewOnceDownloadHook implements Hook {

    private static final String TAG = "WAPatch.ViewOnceDL";

    static Method preventSaveMethod;
    static Method mediaSaveMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading ViewOnceDownloadHook...");
        if (!ModSettings.isViewOnceDownloadEnabled()) {
            Log.i(TAG, "ViewOnceDownload disabled, skip");
            return;
        }
        try {
            // Hook method yang mencegah save (return false → blok save)
            String preventClass = "{{VIEW_ONCE_PREVENT_SAVE_CLASS}}";
            String preventMethod = "{{VIEW_ONCE_PREVENT_SAVE_METHOD}}";

            if (!preventClass.startsWith("{{")) {
                Class<?> pc = ReflectionUtils.findClass(preventClass);
                if (pc != null) {
                    preventSaveMethod = ReflectionUtils.findMethod(pc, preventMethod);
                }
            }

            if (preventSaveMethod == null) {
                preventSaveMethod = scanPreventSave();
            }

            if (preventSaveMethod != null) {
                Method hook = ViewOnceDownloadHook.class.getDeclaredMethod(
                        "preventSaveHook", Object.class);
                Method backup = ViewOnceDownloadHook.class.getDeclaredMethod(
                        "preventSaveBackup", Object.class);
                HookMain.backupAndHook(preventSaveMethod, hook, backup);
                Log.i(TAG, "Hooked preventSave method");
            }

            // Hook method save media — inject path custom supaya saves ke folder mod
            String saveClass = "{{MEDIA_SAVE_CLASS}}";
            String saveMethod = "{{MEDIA_SAVE_METHOD}}";

            if (!saveClass.startsWith("{{")) {
                Class<?> sc = ReflectionUtils.findClass(saveClass);
                if (sc != null) {
                    mediaSaveMethod = ReflectionUtils.findMethod(sc, saveMethod, File.class);
                }
            }

            if (mediaSaveMethod == null) {
                mediaSaveMethod = scanMediaSave();
            }

            if (mediaSaveMethod != null) {
                Method hook = ViewOnceDownloadHook.class.getDeclaredMethod(
                        "mediaSaveHook", Object.class, File.class);
                Method backup = ViewOnceDownloadHook.class.getDeclaredMethod(
                        "mediaSaveBackup", Object.class, File.class);
                HookMain.backupAndHook(mediaSaveMethod, hook, backup);
                Log.i(TAG, "Hooked mediaSave method");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading ViewOnceDownloadHook: " + e.getMessage());
        }
    }

    /**
     * Hook method yang return boolean "apakah boleh disimpan".
     * Jika view-once, selalu return true (boleh disimpan).
     */
    public static boolean preventSaveHook(Object self) {
        if (!ModSettings.isViewOnceDownloadEnabled()) {
            return preventSaveBackup(self);
        }
        try {
            // Cek apakah media ini view-once
            Field mediaField = ReflectionUtils.findField(self.getClass(), "viewOnce_");
            boolean isViewOnce = false;
            if (mediaField != null) {
                Object mediaObj = mediaField.get(self);
                if (mediaObj instanceof Boolean) {
                    isViewOnce = (Boolean) mediaObj;
                }
            }
            if (!isViewOnce) {
                // Cek juga dari class context
                isViewOnce = self.getClass().getName().toLowerCase().contains("viewonce");
            }

            if (isViewOnce) {
                Log.i(TAG, "ALLOWED save for view-once media");
                return true; // Boleh disimpan
            }
        } catch (Exception e) {
            Log.e(TAG, "preventSaveHook error: " + e.getMessage());
        }
        return preventSaveBackup(self);
    }

    public static boolean preventSaveBackup(Object self) {
        // YAHFA inject original
        return false;
    }

    /**
     * Hook save media — redirect ke folder custom supaya gampang ditemukan.
     * Di sini juga bisa copy file ke /sdcard/WhatsApp/Status Download/.
     */
    public static void mediaSaveHook(Object self, File originalFile) {
        if (!ModSettings.isViewOnceDownloadEnabled()) {
            mediaSaveBackup(self, originalFile);
            return;
        }
        try {
            // Panggil original save dulu
            mediaSaveBackup(self, originalFile);

            // Lalu copy ke folder mod jika ini view-once
            boolean isViewOnce = false;
            try {
                Field f = ReflectionUtils.findField(self.getClass(), "viewOnce_");
                if (f != null) isViewOnce = (Boolean) f.get(self);
            } catch (Exception ignored) {
            }

            if (isViewOnce && originalFile != null && originalFile.exists()) {
                File modDir = new File(Environment.getExternalStorageDirectory(),
                        "WhatsApp/ViewOnce Download");
                if (!modDir.exists()) modDir.mkdirs();

                File dest = new File(modDir, originalFile.getName());
                copyFile(originalFile, dest);
                Log.i(TAG, "Copied view-once media to: " + dest.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "mediaSaveHook error: " + e.getMessage());
            mediaSaveBackup(self, originalFile);
        }
    }

    public static void mediaSaveBackup(Object self, File originalFile) {
        // YAHFA inject original
    }

    private static void copyFile(File src, File dst) throws Exception {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private static Method scanPreventSave() {
        return null;
    }

    private static Method scanMediaSave() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "ViewOnceDownloadHook unloaded");
    }
}
