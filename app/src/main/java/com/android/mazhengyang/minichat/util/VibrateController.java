package com.android.mazhengyang.minichat.util;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import com.android.mazhengyang.minichat.fragment.SettingFragment;

/**
 * Created by mazhengyang on 19-1-17.
 */

public class VibrateController {

    private static final String TAG = "MiniChat." + VibrateController.class.getSimpleName();

    private static final int VIBRATOR_TIME = 300;
    private static boolean isOn = true;
    private static Vibrator vibrator;

    public static void vibrate(Activity ctx) {
        if (isOn) {
            if (vibrator == null) {
                vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null) {
//                Log.d(TAG, "vibrate: ");
                vibrator.vibrate(VIBRATOR_TIME);
            }
        }
    }

    public static void setEnable(boolean on) {
        Log.d(TAG, "setEnable: on=" + on);
        isOn = on;
    }

}
