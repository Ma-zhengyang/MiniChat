package com.android.mazhengyang.minichat.util;

/**
 * Created by mazhengyang on 18-12-25.
 */

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 夜间模式辅助类
 */
public class SharedPreferencesHelper {

    private static String KEY_DAYNIGHT = "key_daynight";
    private static String KEY_SOUND = "key_sound";
    private static String KEY_VIBRATE = "key_vibrate";

    public static final int MODE_DAY = 1;
    public static final int MODE_NIGHT = 0;

    public static final int MODE_SOUND_ON = 1;
    public static final int MODE_SOUND_OFF = 0;

    public static final int MODE_VIBRATE_ON = 1;
    public static final int MODE_VIBRATE_OFF = 0;

    public static int getDayNightMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
        return sp.getInt(KEY_DAYNIGHT, MODE_DAY);
    }

    public static void setDayNightMode(Context ctx, int mode) {
        SharedPreferences sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_DAYNIGHT, mode).commit();
    }

    public static int getSoundMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
        return sp.getInt(KEY_SOUND, MODE_SOUND_ON);
    }

    public static void setSoundMode(Context ctx, int mode) {
        SharedPreferences sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_SOUND, mode).commit();
    }

    public static int getVibrateMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
        return sp.getInt(KEY_VIBRATE, MODE_VIBRATE_ON);
    }

    public static void setVibrateMode(Context ctx, int mode) {
        SharedPreferences sp = ctx.getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_VIBRATE, mode).commit();
    }

}