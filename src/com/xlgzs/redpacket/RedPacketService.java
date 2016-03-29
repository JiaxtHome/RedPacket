package com.xlgzs.redpacket;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class RedPacketService extends NotificationListenerService {

    private static final String TAG = "packet";
    private RedPacketDialog mDialog;
    private PendingIntent mGotoIntent;
    private IDialogCallback mCallback = new IDialogCallback() {

        @Override
        public void onGotoRedPacketClicked() {
            Log.i(TAG, "service onGotoRedPacketClicked...");
            try {
                mGotoIntent.send();
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDeleteClicked() {

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "service bind...");
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "service created...");
        super.onCreate();
        mDialog = new RedPacketDialog(this, mCallback);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "service destroyed...");
        super.onDestroy();
    }

    private Handler mHandler = new Handler();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!isEnable()) {
            return;
        }
        final Notification notification = sbn.getNotification();
        CharSequence tickerText = notification.tickerText;
        String pkg = sbn.getPackageName();
        Log.i(TAG, "package:" + pkg + "-------Text: " + tickerText);
        if (findRedPacket(pkg, tickerText)) {
            mGotoIntent = notification.contentIntent;
            new Thread() {

                @Override
                public void run() {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            mDialog.onRedPacketPosted(notification);
                        }
                    });
                }
            }.start();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "Removed:" + "-----" + sbn.toString());
    }

    private boolean findRedPacket(String pkgName, CharSequence tickerText) {
        for (String pkg : Constants.Observables) {
            if (pkgName.equals(pkg)) {
                if (tickerText == null) {
                    return false;
                }
                String content = tickerText.toString();
                for (String keyWords : Constants.KeyWords) {
                    if (content.contains(keyWords)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isEnable() {
        boolean enable = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(Constants.PREFERENCES_KEY_TOTAL_SWITCHER,
                true);
        return enable;
    }
}
