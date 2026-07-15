package com.smali_generator.patches;

import android.util.Log;

import com.smali_generator.Hook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.lang.reflect.Method;

import lab.galaxy.yahfa.HookMain;

/**
 * FITUR 4: Group Status untuk Non-Admin (Prioritas Tinggi)
 *
 * Tujuan: Bypass permission check spesifik untuk posting status ke grup
 * tanpa membuat user terlihat sebagai admin untuk fitur lain.
 *
 * Hati-hati: JANGAN hook isAdmin global.
 * Hook HANYA method yang mungkin ada di alur group status.
 *
 * Strategi:
 * 1. Hook AB test flag untuk group status → return true.
 * 2. Hook permission check spesifik untuk group status posting → return true.
 * 3. Hook UI gate visibility untuk group status → return true / set visible.
 */
public class GroupStatusHook implements Hook {

    private static final String TAG = "WAPatch.GroupStatus";

    static Method abTestFlagMethod;
    static Method groupStatusPermissionMethod;
    static Method uiVisibilityMethod;

    @Override
    public void load() {
        Log.i(TAG, "Loading GroupStatusHook...");
        if (!ModSettings.isGroupStatusEnabled()) {
            Log.i(TAG, "GroupStatus disabled, skip");
            return;
        }
        try {
            // --- 1. AB Test Flag Hook ---
            String abClass = "{{GROUP_STATUS_AB_CLASS}}";
            String abMethod = "{{GROUP_STATUS_AB_METHOD}}";

            if (!abClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(abClass);
                if (c != null) {
                    abTestFlagMethod = ReflectionUtils.findMethod(c, abMethod);
                }
            }

            if (abTestFlagMethod == null) {
                abTestFlagMethod = scanAbTestMethod();
            }

            if (abTestFlagMethod != null) {
                Method hook = GroupStatusHook.class.getDeclaredMethod(
                        "abTestHook", Object.class);
                Method backup = GroupStatusHook.class.getDeclaredMethod(
                        "abTestBackup", Object.class);
                HookMain.backupAndHook(abTestFlagMethod, hook, backup);
                Log.i(TAG, "Hooked AB test flag: " + abTestFlagMethod.getName());
            } else {
                Log.e(TAG, "abTestFlagMethod not found");
            }

            // --- 2. Permission Check Spesifik Group Status ---
            String permClass = "{{GROUP_STATUS_PERM_CLASS}}";
            String permMethod = "{{GROUP_STATUS_PERM_METHOD}}";

            if (!permClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(permClass);
                if (c != null) {
                    groupStatusPermissionMethod = ReflectionUtils.findMethod(c, permMethod,
                            Object.class);
                }
            }

            if (groupStatusPermissionMethod == null) {
                groupStatusPermissionMethod = scanPermissionMethod();
            }

            if (groupStatusPermissionMethod != null) {
                Method hook = GroupStatusHook.class.getDeclaredMethod(
                        "permissionHook", Object.class, Object.class);
                Method backup = GroupStatusHook.class.getDeclaredMethod(
                        "permissionBackup", Object.class, Object.class);
                HookMain.backupAndHook(groupStatusPermissionMethod, hook, backup);
                Log.i(TAG, "Hooked group status permission: " + groupStatusPermissionMethod.getName());
            } else {
                Log.e(TAG, "groupStatusPermissionMethod not found");
            }

            // --- 3. UI Visibility Gate ---
            String uiClass = "{{GROUP_STATUS_UI_CLASS}}";
            String uiMethod = "{{GROUP_STATUS_UI_METHOD}}";

            if (!uiClass.startsWith("{{")) {
                Class<?> c = ReflectionUtils.findClass(uiClass);
                if (c != null) {
                    uiVisibilityMethod = ReflectionUtils.findMethod(c, uiMethod);
                }
            }

            if (uiVisibilityMethod == null) {
                uiVisibilityMethod = scanUiMethod();
            }

            if (uiVisibilityMethod != null) {
                Method hook = GroupStatusHook.class.getDeclaredMethod(
                        "uiVisibilityHook", Object.class);
                Method backup = GroupStatusHook.class.getDeclaredMethod(
                        "uiVisibilityBackup", Object.class);
                HookMain.backupAndHook(uiVisibilityMethod, hook, backup);
                Log.i(TAG, "Hooked UI visibility: " + uiVisibilityMethod.getName());
            } else {
                Log.e(TAG, "uiVisibilityMethod not found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading GroupStatusHook: " + e.getMessage());
        }
    }

    /**
     * AB Test: selalu return true untuk group status.
     */
    public static boolean abTestHook(Object self) {
        if (!ModSettings.isGroupStatusEnabled()) {
            return abTestBackup(self);
        }
        try {
            Log.i(TAG, "AB test group status FORCED to true");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "abTestHook error: " + e.getMessage());
            return abTestBackup(self);
        }
    }

    public static boolean abTestBackup(Object self) {
        // YAHFA inject original
        return false;
    }

    /**
     * Permission check spesifik group status: selalu return true.
     * Parameter groupInfo berisi info grup, mungkin ada field admin list.
     */
    public static boolean permissionHook(Object self, Object groupInfo) {
        if (!ModSettings.isGroupStatusEnabled()) {
            return permissionBackup(self, groupInfo);
        }
        try {
            // Validasi: pastikan method ini memang untuk group status, bukan admin umum.
            // Kita check apakah ada parameter yang mengandung "group" di nama class-nya.
            boolean isGroupStatusFlow = false;
            if (groupInfo != null) {
                String cn = groupInfo.getClass().getName().toLowerCase();
                if (cn.contains("group") || cn.contains("status")) {
                    isGroupStatusFlow = true;
                }
            }

            if (isGroupStatusFlow) {
                Log.i(TAG, "Group status permission FORCED to true");
                return true;
            }
            return permissionBackup(self, groupInfo);
        } catch (Exception e) {
            Log.e(TAG, "permissionHook error: " + e.getMessage());
            return permissionBackup(self, groupInfo);
        }
    }

    public static boolean permissionBackup(Object self, Object groupInfo) {
        // YAHFA inject original
        return false;
    }

    /**
     * UI visibility: tampilkan menu/tombol group status meskipun non-admin.
     * Return true supaya elemen muncul.
     */
    public static boolean uiVisibilityHook(Object self) {
        if (!ModSettings.isGroupStatusEnabled()) {
            return uiVisibilityBackup(self);
        }
        try {
            Log.i(TAG, "UI visibility group status FORCED to true");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "uiVisibilityHook error: " + e.getMessage());
            return uiVisibilityBackup(self);
        }
    }

    public static boolean uiVisibilityBackup(Object self) {
        // YAHFA inject original
        return false;
    }

    /* ---------- Reflection scanners ---------- */

    private static Method scanAbTestMethod() {
        return null;
    }

    private static Method scanPermissionMethod() {
        return null;
    }

    private static Method scanUiMethod() {
        return null;
    }

    @Override
    public void unload() {
        Log.i(TAG, "GroupStatusHook unloaded");
    }
}
