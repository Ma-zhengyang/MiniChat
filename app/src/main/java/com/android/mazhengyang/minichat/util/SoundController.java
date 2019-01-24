package com.android.mazhengyang.minichat.util;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

/**
 * Created by mazhengyang on 19-1-17.
 */

public class SoundController {

    private static final String TAG = "MiniChat." + SoundController.class.getSimpleName();

    private static boolean isOn = true;
    private static Ringtone ringtone;

    public static void play(Context context) {
        if (isOn) {
            if (ringtone == null) {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ringtone = RingtoneManager.getRingtone(context, notification);
            }
            if (ringtone != null) {
//                Log.d(TAG, "play: ");
                ringtone.play();
            }
        }
    }

    public static void setEnable(boolean on) {
        Log.d(TAG, "setEnable: on=" + on);
        isOn = on;
    }

}
