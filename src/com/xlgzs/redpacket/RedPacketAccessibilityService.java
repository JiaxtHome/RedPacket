package com.xlgzs.redpacket;

import java.util.List;
import java.util.Random;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * <p>
 * Created by Administrator
 * </p>
 * <p/>
 * 抢红包外挂服务
 */
public class RedPacketAccessibilityService extends AccessibilityService {

    static final String TAG = "packet";

    /**
     * 微信的包名
     */
    static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    /**
     * 红包消息的关键字
     */
    static final String ENVELOPE_TEXT_KEY = "[微信红包]";

    private Handler handler = new Handler();
    private Random random = new Random();
    private static final int MAX_TIME = 1000;
    private static final int MIN_TIME = 500;
    private SoundPool mPool;
    private int mRingId;
    private AudioManager mAudioManager;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        if (!isRedirectEnable()) {
            return;
        }
        if (!event.getPackageName().equals(WECHAT_PACKAGENAME)) {
            return;
        }
        Log.d(TAG, "事件---->" + event);
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            openEnvelope(event);
        }
    }

    /*
     * @Override protected boolean onKeyEvent(KeyEvent event) { //return
     * super.onKeyEvent(event); return true; }
     */

    @Override
    public void onInterrupt() {
        Log.w(TAG, "抢红包服务已终止");
        setAccessibilityEnable(false);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        setAccessibilityEnable(true);
        mPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        mRingId = mPool.load(getApplicationContext(), R.raw.congratulation, 0);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.w(TAG, "抢红包服务已开启");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openEnvelope(AccessibilityEvent event) {
        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            // 点中了红包，下一步就是去拆红包
            handler.removeCallbacksAndMessages(null);
            int s = random.nextInt(MAX_TIME) % (MAX_TIME - MIN_TIME + 1) + MIN_TIME;
            handler.postDelayed(new Runnable() { 

                @Override
                public void run() {
                    getMoney();
                }
            }, s);
        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            // 拆完红包后看详细的纪录界面
        } else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            // 在聊天界面,去点中红包
            handler.removeCallbacksAndMessages(null);
            int s = random.nextInt(MAX_TIME) % (MAX_TIME - MIN_TIME + 1) + MIN_TIME;
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    clickRedPacket();
                }
            }, s);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getMoney() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        int size = nodeInfo.getChildCount();
        for (int i = 0; i < size; i++) {
            AccessibilityNodeInfo n = nodeInfo.getChild(i);
            String name = n.getClassName().toString();
            Log.i(TAG, "child name = " + name);
            if (name.endsWith("Button")) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                int current = mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
                int max = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
                float volume = current * 1.0f / max;
                mPool.play(mRingId, volume, volume, 0, 0, 1);
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void clickRedPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "clickRedPacket rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> envelopList = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
        if (envelopList.isEmpty()) {
            Log.i(TAG, "没有新红包");
        } else {
            // 最新的红包领起
            AccessibilityNodeInfo envelopNode = envelopList.get(envelopList.size() - 1);
            AccessibilityNodeInfo envelopParent = envelopNode.getParent();
            Log.i(TAG, "clickRedPacket-->领取红包:");
            if (envelopParent != null) {
                List<AccessibilityNodeInfo> openList = nodeInfo.findAccessibilityNodeInfosByText("你领取了");
                if (openList.isEmpty()) {
                    envelopParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } else {
                    Rect envelopRec = new Rect();
                    Rect openRec = new Rect();
                    envelopNode.getBoundsInScreen(envelopRec);
                    AccessibilityNodeInfo openNode = openList.get(openList.size() - 1);
                    openNode.getBoundsInScreen(openRec);
                    if (envelopRec.top > openRec.top) {
                        envelopParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    } else {
                        Log.i(TAG, "clickRedPacket-->已经领取该红包!");
                    }
                }
            }
        }
    }

    private boolean isRedirectEnable() {
        boolean enable = getApplicationContext().getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(
                Constants.PREFERENCES_KEY_REDIRECT_SWITCHER, false);
        return enable;
    }

    private void setAccessibilityEnable(boolean enable) {
        getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putBoolean(Constants.PREFERENCES_KEY_ACCESSIBILITY_ENABLE, enable)
                .apply();
    }
}
