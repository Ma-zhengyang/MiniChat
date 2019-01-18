package com.android.mazhengyang.minichat.util;

/**
 * Created by mazhengyang on 19-1-17.
 */

public class SoundController {

    private static final String TAG = "MiniChat." + SoundController.class.getSimpleName();

    private static boolean isOn = true;

    public static void play() {

    }

    public static void stop() {

    }

    public static void setEnable(boolean on) {
        isOn = on;
    }
}
