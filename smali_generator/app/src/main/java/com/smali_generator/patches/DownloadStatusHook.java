package com.smali_generator.patches;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

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
 * FITUR 3: Download Status
 *
 * Aktifkan kemampuan mengunduh/menyimpan foto dan video status
 * langsung dari penampil status. Hook method yang mencegah save,
 * dan redirect save ke folder /sdcard/WhatsApp/Status Download/.
 */
public class DownloadStatusHook implements Hook {

    private static final String TAG = "WAPatch.StatusDL";

    static Method preventStatusSaveMethod;
    static Method statusSaveMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading DownloadStatusHook...");
        if (!ModSettings.isDownloadStatusEnabled()) {
            Log.i(TAG, "DownloadStatus disabled, skip");
            return;
        }
        try {
            // Hook yang mencegah save
            String preventClass = "{{STATUS_PREVENT_SAVE_CLASS}}";
            String preventMethod = "{{STATUS_PREVENT_SAVE_METHOD}}";

            if (!preventClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(preventClass);
                if (c != null) {
                    preventStatusSaveMethod = ReflectionUtils.findMethod(c, preventMethod);
                }
            }

            if (preventStatusSaveMethod == null) {
                preventStatusSaveMethod = scanPreventSave();
            }

            if (preventStatusSaveMethod != null) {
                Method hook = DownloadStatusHook.class.getDeclaredMethod(
                        "preventSaveHook", Object.class);
                Method backup = DownloadStatusHook.class.getDeclaredMethod(
                        "preventSaveBackup", Object.class);
                HookMain.backupAndHook(preventStatusSaveMethod, hook, backup);
                Log.i(TAG, "Hooked preventStatusSave");
            } else {
                Log.e(TAG, "preventStatusSaveMethod not found");
            }

            // Hook actual save
            String saveClass = "{{STATUS_SAVE_CLASS}}";
            String saveMethod = "{{STATUS_SAVE_METHOD}}";

            if (!saveClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(saveClass);
                if (c != null) {
                    statusSaveMethod = ReflectionUtils.findMethod(c, saveMethod, File.class);
                }
            }

            if (statusSaveMethod == null) {
                statusSaveMethod = scanStatusSave();
            }

            if (statusSaveMethod != null) {
                Method hook = DownloadStatusHook.class.getDeclaredMethod(
                        "statusSaveHook", Object.class, File.class);
                Method backup = DownloadStatusHook.class.getDeclaredMethod(
                        "statusSaveBackup", Object.class, File.class);
                HookMain.backupAndHook(statusSaveMethod, hook, backup);
                Log.i(TAG, "Hooked statusSave method");
            } else {
                Log.e(TAG, "statusSaveMethod not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading DownloadStatusHook: " + e.getMessage());
        }
    }

    /**
     * Selalu return true (boleh disimpan) untuk media status.
     */
    public static boolean preventSaveHook(Object self) {
        if (!ModSettings.isDownloadStatusEnabled()) {
            return preventSaveBackup(self);
        }
        try {
            // Cek apakah ini memang konteks status media
            boolean isStatus = false;
            try {
                Field f = ReflectionUtils.findField(self.getClass(), "isStatus_");
                if (f != null) isStatus = (Boolean) f.get(self);
            } catch (Exception ignored) {
            }
            if (!isStatus) {
                // Fallback: cek dari class name
                isStatus = self.getClass().getName().toLowerCase().contains("status");
            }

            if (isStatus) {
                Log.i(TAG, "ALLOWED save for status media");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "preventSaveHook error: " + e.getMessage());
        }
        return preventSaveBackup(self);
    }

    public static boolean preventSaveBackup(Object self) {
        return false;
    }

    /**
     * Save status media ke folder custom + jangan hapus original.
     */
    public static void statusSaveHook(Object self, File mediaFile) {
        if (!ModSettings.isDownloadStatusEnabled()) {
            statusSaveBackup(self, mediaFile);
            return;
        }
        try {
            // Panggil original save dulu (ke cache internal)
            statusSaveBackup(self, mediaFile);

            if (mediaFile != null && mediaFile.exists()) {
                File statusDir = new File(Environment.getExternalStorageDirectory(),
                        "WhatsApp/Status Download");
                if (!statusDir.exists()) statusDir.mkdirs();

                String ext = "";
                String name = mediaFile.getName();
                int dot = name.lastIndexOf('.');
                if (dot > 0) ext = name.substring(dot);

                File dest = new File(statusDir, "STATUS_" + System.currentTimeMillis() + ext);
                copyFile(mediaFile, dest);
                Log.i(TAG, "Saved status media to: " + dest.getAbsolutePath());

                try {
                    Context ctx = ReflectionUtils.getAppContext();
                    if (ctx != null) {
                        Toast.makeText(ctx, "Status disimpan ke: Status Download", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "statusSaveHook error: " + e.getMessage());
            statusSaveBackup(self, mediaFile);
        }
    }

    public static void statusSaveBackup(Object self, File mediaFile) {
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

    private static Method scanPreventSave() { return null; }
    private static Method scanStatusSave() { return null; }

    @Override
    public void unload() {
        Log.i(TAG, "DownloadStatusHook unloaded");
    }
}
