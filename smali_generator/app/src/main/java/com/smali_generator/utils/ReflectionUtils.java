package com.smali_generator.utils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility reflection untuk menemukan class/method obfuscated di runtime.
 * Semua method static, bisa dipakai dari mana saja.
 */
public class ReflectionUtils {

    private static final String TAG = "WAPatch.Reflection";
    private static Context appContext;

    public static Context getAppContext() {
        if (appContext != null) {
            return appContext;
        }
        try {
            Application app = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            appContext = app.getApplicationContext();
            return appContext;
        } catch (Exception e) {
            Log.e(TAG, "getAppContext failed: " + e.getMessage());
            return null;
        }
    }

    public static void setAppContext(Context ctx) {
        appContext = ctx.getApplicationContext();
    }

    /**
     * Cari class berdasarkan nama pakai Class.forName.
     * Jika gagal, return null tanpa throw.
     */
    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Class not found: " + className);
            return null;
        }
    }

    /**
     * Cari field dengan nama spesifik di class, cari juga di superclass.
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) return null;
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        Log.e(TAG, "Field not found: " + fieldName + " in " + clazz.getName());
        return null;
    }

    /**
     * Cari method dengan signature yang cocok.
     * Cocokkan berdasarkan parameter types dan return type.
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null || methodName == null) return null;
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Method not found: " + methodName + " in " + clazz.getName());
            return null;
        }
    }

    /**
     * Cari semua method di class yang cocok dengan pattern return type dan parameter count.
     * Berguna untuk method obfuscated yang namanya random tapi signature-nya unik.
     */
    public static Method findMethodBySignature(Class<?> clazz, Class<?> returnType, int paramCount) {
        if (clazz == null) return null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getReturnType().equals(returnType) && method.getParameterTypes().length == paramCount) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    /**
     * Cari method yang dipanggil setelah membuka ViewOnce media.
     * Pattern: method yang ambil parameter message-related, kemungkinan return void,
     * dan ada field atau method terkait "viewOnce" / "receipt" / "read".
     */
    public static Method findViewOnceReceiptMethod(Class<?> clazz) {
        if (clazz == null) return null;
        for (Method method : clazz.getDeclaredMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length >= 1) {
                // Cari method yang kemungkinan kirim receipt
                // Contoh signature: method(Object messageInfo, Object userJid)
                if (method.getReturnType() == void.class || method.getReturnType() == Void.TYPE) {
                    // Periksa apakah ada parameter bertipe class internal WhatsApp
                    for (Class<?> p : params) {
                        if (p.getName().contains("Jid") || p.getName().contains("Message")) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
            }
        }
        return null;
    }
}
