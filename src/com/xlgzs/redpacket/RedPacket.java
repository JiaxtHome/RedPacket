package com.xlgzs.redpacket;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class RedPacket extends Activity {

    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private CheckSwitchButton mTotalSwitcher;
    private CheckSwitchButton mRedirectSwitcher;
    private TextView mOnOffSuggestion;
    private TextView mRedirectSuggestion;
    private Button mActivate;
    private Button mSetKeyguard;
    private Button mSetAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_packet);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean activated = isActivated();
        if (activated) {
            mActivate.setText(R.string.activated);
        } else {
            mActivate.setText(R.string.activate);
        }
        mActivate.setEnabled(!activated);

        boolean enable = isTotalSwitchEnable();
        mTotalSwitcher.setChecked(enable);

        boolean redirect = isRedirectEnable();
        mRedirectSwitcher.setChecked(redirect);

        boolean accessibilityEnable = isAccessibilityEnable();
        if (accessibilityEnable) {
            mSetAccessibility.setText(R.string.accessibility_open);
        } else {
            mSetAccessibility.setText(R.string.set_accessibility);
        }
        mSetAccessibility.setEnabled(!accessibilityEnable);
    }

    private void initViews() {
        mTotalSwitcher = (CheckSwitchButton) findViewById(R.id.total_switch);
        mOnOffSuggestion = (TextView) findViewById(R.id.total_switch_suggestion);
        mRedirectSuggestion = (TextView) findViewById(R.id.redirect_switch_suggestion);
        mTotalSwitcher.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mOnOffSuggestion.setText(R.string.open);
                } else {
                    mOnOffSuggestion.setText(R.string.close);
                }
                mRedirectSwitcher.setEnabled(isChecked);
                mRedirectSuggestion.setEnabled(isChecked);
                getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                        .putBoolean(Constants.PREFERENCES_KEY_TOTAL_SWITCHER, isChecked).apply();
            }
        });

        mRedirectSwitcher = (CheckSwitchButton) findViewById(R.id.redirect_switch);
        mRedirectSwitcher.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                        .putBoolean(Constants.PREFERENCES_KEY_REDIRECT_SWITCHER, isChecked).apply();
            }
        });

        mActivate = (Button) findViewById(R.id.activate);
        mActivate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        mSetKeyguard = (Button) findViewById(R.id.set_keyguard);
        mSetKeyguard.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startKeyguardChooser();
            }
        });
        mSetAccessibility = (Button) findViewById(R.id.set_accessibility);
        mSetAccessibility.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startAccessibility();
            }
        });
    }

    private boolean isActivated() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isTotalSwitchEnable() {
        boolean enable = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(Constants.PREFERENCES_KEY_TOTAL_SWITCHER,
                true);
        return enable;
    }

    public boolean isRedirectEnable() {
        boolean enable = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(
                Constants.PREFERENCES_KEY_REDIRECT_SWITCHER, false);
        return enable;
    }

    private void startKeyguardChooser() {
        Intent intent = new Intent();
        ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.ChooseLockGeneric");
        intent.setComponent(cm);
        startActivity(intent);
    }

    private void startAccessibility() {
        try {
            // 打开系统设置中辅助功能
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, R.string.open_accessibility, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAccessibilityEnable() {
        boolean enable = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(
                Constants.PREFERENCES_KEY_ACCESSIBILITY_ENABLE, false);
        return enable;
    }
}
