package com.smali_generator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.smali_generator.patches.AntiDeleteHook;
import com.smali_generator.patches.DownloadStatusHook;
import com.smali_generator.patches.GroupStatusHook;
import com.smali_generator.patches.HideBlueTickHook;
import com.smali_generator.patches.HideRecordingHook;
import com.smali_generator.patches.HideTypingHook;
import com.smali_generator.patches.ViewOnceDownloadHook;
import com.smali_generator.patches.ViewOncePermanentHook;
import com.smali_generator.patches.ViewOnceStealthHook;
import com.smali_generator.settings.ModSettings;
import com.smali_generator.utils.ReflectionUtils;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class TheAmazingPatch {

    static final String TAG = "WhatsAppPatcherMod";

    static AtomicBoolean is_loaded = new AtomicBoolean(false);

    public static void on_load() {
        if (is_loaded.getAndSet(true)) {
            Log.w(TAG, "on_load called again, skip");
            return;
        }

        Log.e(TAG, "=== WhatsApp Patch Loaded ===");

        Context context = ReflectionUtils.getAppContext();
        Log.e(TAG, "Context = " + context);

        if (context != null) {
            ModSettings.init(context);
            Log.e(TAG, "ModSettings initialized");

            // Register dynamic broadcast receiver for toggling features via adb:
            // adb shell am broadcast -a com.smali_generator.TOGGLE --ez antiDelete false
            try {
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        if (intent == null) return;
                        String action = intent.getAction();
                        if ("com.smali_generator.TOGGLE".equals(action)) {
                            boolean antiDelete = intent.getBooleanExtra("antiDelete", ModSettings.isAntiDeleteOn());
                            boolean antiDeleteStatus = intent.getBooleanExtra("antiDeleteStatus", ModSettings.isAntiDeleteStatusOn());
                            boolean viewOnceStealth = intent.getBooleanExtra("viewOnceStealth", ModSettings.isViewOnceStealthOn());
                            boolean viewOncePermanent = intent.getBooleanExtra("viewOncePermanent", ModSettings.isViewOncePermanentOn());
                            boolean hideBlueTick = intent.getBooleanExtra("hideBlueTick", ModSettings.isHideBlueTickOn());
                            boolean hideTyping = intent.getBooleanExtra("hideTyping", ModSettings.isHideTypingOn());
                            boolean hideRecording = intent.getBooleanExtra("hideRecording", ModSettings.isHideRecordingOn());
                            boolean groupStatus = intent.getBooleanExtra("groupStatus", ModSettings.isGroupStatusOn());
                            boolean downloadStatus = intent.getBooleanExtra("downloadStatus", ModSettings.isDownloadStatusOn());
                            ModSettings.setAntiDeleteOn(antiDelete);
                            ModSettings.setAntiDeleteStatusOn(antiDeleteStatus);
                            ModSettings.setViewOnceStealthOn(viewOnceStealth);
                            ModSettings.setViewOncePermanentOn(viewOncePermanent);
                            ModSettings.setHideBlueTickOn(hideBlueTick);
                            ModSettings.setHideTypingOn(hideTyping);
                            ModSettings.setHideRecordingOn(hideRecording);
                            ModSettings.setGroupStatusOn(groupStatus);
                            ModSettings.setDownloadStatusOn(downloadStatus);
                            Log.e(TAG, "Toggles updated via broadcast");
                        }
                    }
                }, new IntentFilter("com.smali_generator.TOGGLE"), Context.RECEIVER_NOT_EXPORTED);
                Log.e(TAG, "BroadcastReceiver registered (adb: am broadcast -a com.smali_generator.TOGGLE)");
            } catch (Exception e) {
                Log.e(TAG, "BroadcastReceiver registration failed: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Context null! Hooks may fail.");
        }

        Hook[] hooks = {
                new ViewOnceStealthHook(),
                new ViewOncePermanentHook(),
                new ViewOnceDownloadHook(),
                new GroupStatusHook(),
                new AntiDeleteHook(),
                new DownloadStatusHook(),
                new HideBlueTickHook(),
                new HideTypingHook(),
                new HideRecordingHook(),
        };

        for (Hook hook : hooks) {
            try {
                hook.load();
                Log.i(TAG, hook.getClass().getSimpleName() + " loaded");
            } catch (Exception e) {
                Log.e(TAG, hook.getClass().getSimpleName() + " FAILED: " + e.getMessage());
                e.printStackTrace();
            }
        }

        Log.e(TAG, "=== All hooks attempted ===");
    }
}
