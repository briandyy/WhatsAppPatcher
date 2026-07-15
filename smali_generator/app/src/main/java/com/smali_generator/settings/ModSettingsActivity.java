package com.smali_generator.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity pengaturan mod yang bisa diakses dari WhatsApp Settings → "Mod Settings".
 * UI native Android dengan Switch toggles untuk setiap fitur.
 */
@SuppressWarnings("deprecation")
public class ModSettingsActivity extends Activity {

    private static final String TAG = "WAPatch.SettingsUI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "ModSettingsActivity opened");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        container.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("WhatsApp Mod Settings");
        title.setTextSize(22);
        title.setPadding(0, 0, 0, dpToPx(12));
        container.addView(title);

        addToggle(container, "View Once Stealth (Prioritas Tertinggi)",
                ModSettings.isViewOnceStealthEnabled(),
                ModSettings.KEY_VIEW_ONCE_STEALTH);

        addToggle(container, "View Once Permanent",
                ModSettings.isViewOncePermanentEnabled(),
                ModSettings.KEY_VIEW_ONCE_PERMANENT);

        addToggle(container, "View Once Download",
                ModSettings.isViewOnceDownloadEnabled(),
                ModSettings.KEY_VIEW_ONCE_DOWNLOAD);

        addToggle(container, "Group Status for Non-Admin",
                ModSettings.isGroupStatusEnabled(),
                ModSettings.KEY_GROUP_STATUS);

        addToggle(container, "Anti-Delete Messages",
                ModSettings.isAntiDeleteMsgEnabled(),
                ModSettings.KEY_ANTI_DELETE_MSG);

        addToggle(container, "Anti-Delete Status",
                ModSettings.isAntiDeleteStatusEnabled(),
                ModSettings.KEY_ANTI_DELETE_STATUS);

        addToggle(container, "Download Status Media",
                ModSettings.isDownloadStatusEnabled(),
                ModSettings.KEY_DOWNLOAD_STATUS);

        addToggle(container, "Hide Blue Tick (Read Receipts)",
                ModSettings.isHideBlueTickEnabled(),
                ModSettings.KEY_HIDE_BLUE_TICK);

        addToggle(container, "Hide Typing Indicator",
                ModSettings.isHideTypingEnabled(),
                ModSettings.KEY_HIDE_TYPING);

        addToggle(container, "Hide Recording Indicator",
                ModSettings.isHideRecordingEnabled(),
                ModSettings.KEY_HIDE_RECORDING);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);
        setContentView(scrollView);
    }

    private void addToggle(LinearLayout container, String label, boolean initialValue, final String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(16);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(initialValue);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ModSettings.setEnabled(key, isChecked);
            Toast.makeText(this, label + ": " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        row.addView(tv);
        row.addView(sw);
        container.addView(row);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
