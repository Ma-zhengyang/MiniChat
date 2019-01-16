package com.android.mazhengyang.minichat.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mazhengyang on 19-1-16.
 */

public class Utils {

    private static final String TAG = "MiniChat." + Utils.class.getSimpleName();

    private static SimpleDateFormat format;

    public static String formatTime(Long time) {

        if (format == null) {
            format = new SimpleDateFormat("HH:mm", Locale.CHINA);
        }
        return format.format(new Date(time));
    }


}
