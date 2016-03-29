package com.xlgzs.redpacket;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class RedPacketDialog {

    private static final String TAG = "packet";
    private Context mContext;
    private IDialogCallback mCallback;
    private Dialog mDialog;
    private View mContent;
    private TextView mMessage;
    private ImageView mIcon;
    private TextView mTitle;
    private AudioManager mAudioManager;
    private SoundPool mPool;
    private int mRingId;
    private int mHeight;
    private KeyguardLock mKeyguardLock = null;
    private PowerManager.WakeLock mWakeLock;

    public RedPacketDialog(Context context, IDialogCallback callback) {
        mContext = context;
        mCallback = callback;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        mRingId = mPool.load(context, R.raw.ring, 0);
        mHeight = mContext.getResources().getDimensionPixelSize(R.dimen.dialog_height);
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);  
        mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");  
        initDialog();
    }

    @SuppressLint("InflateParams")
    private void initDialog() {
        mDialog = new AlertDialog.Builder(mContext).create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        // initialize content views
        mContent = LayoutInflater.from(mContext).inflate(R.layout.dialog_alert, null);
        Button goButton = (Button) mContent.findViewById(R.id.go);
        goButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mCallback == null) {
                    return;
                } else {
                    mCallback.onGotoRedPacketClicked();
                    mDialog.dismiss();
                }
            }
        });
        mMessage = (TextView) mContent.findViewById(R.id.message);
        mIcon = (ImageView) mContent.findViewById(R.id.icon);
        mTitle = (TextView) mContent.findViewById(R.id.title);
    }

    public void onRedPacketPosted(Notification notification) {
        if (mDialog == null) {
            initDialog();
        }
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
        int current = mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
        int max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
        float volume = current * 1.0f / max;
        mPool.play(mRingId, volume, volume, 0, 0, 1);
        disableKeyguard();
        boolean redirect = isRedirectSwitchEnable();
        Log.i(TAG, "isRedirect = " + redirect);
        if (redirect) {
            mWakeLock.acquire();
            mWakeLock.release();
            mCallback.onGotoRedPacketClicked();
        } else {
            mDialog.show();
            Window window = mDialog.getWindow();
            LayoutParams lp = new LayoutParams();
            lp.height = mHeight;
            window.setContentView(mContent, lp);
            String title = getTitle(notification.tickerText.toString());
            mTitle.setText(title);
            String words = getWords(notification.tickerText.toString());
            mMessage.setText(words);
            mIcon.setImageBitmap(notification.largeIcon);
        }
    }

    private String getTitle(String tickerText) {
        int start = tickerText.indexOf(":");
        String ret = tickerText.substring(0, start);
        return ret;
    }

    private String getWords(String tickerText) {
        int start = tickerText.indexOf("]") + 1;
        String ret = tickerText.substring(start);
        return ret;
    }

    private void disableKeyguard() {
        if (mKeyguardLock == null) {
            KeyguardManager keyguardManger = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
            mKeyguardLock = keyguardManger.newKeyguardLock("");
        }
        mKeyguardLock.disableKeyguard();
        registerScreenOffReceiver();
    }

    private void reenableKeyguard() {
        if (mKeyguardLock == null) {
            KeyguardManager keyguardManger = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
            mKeyguardLock = keyguardManger.newKeyguardLock("");
        }
        mKeyguardLock.reenableKeyguard();
        unRegisterScreenOffReceiver();
    }

    private void registerScreenOffReceiver() {
        Log.i(TAG, "register ScreenOffReceiver");
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenOffReceiver, filter);
    }

    private void unRegisterScreenOffReceiver() {
        Log.i(TAG, "unregister ScreenOffReceiver");
        try {
            mContext.unregisterReceiver(mScreenOffReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            reenableKeyguard();
        }

    };

    private boolean isRedirectSwitchEnable() {
        boolean enable = mContext.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(
                Constants.PREFERENCES_KEY_REDIRECT_SWITCHER, false);
        return enable;
    }

}
