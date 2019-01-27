package com.android.mazhengyang.minichat.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mazhengyang on 19-1-25.
 */

public class DataUtil {

    private static final String TAG = "MiniChat." + DataUtil.class.getSimpleName();

    private static SimpleDateFormat format;

    /**
     * 格式化时间
     *
     * @param time
     * @return
     */
    public static String formatTime(Long time) {

        if (format == null) {
            format = new SimpleDateFormat("HH:mm", Locale.CHINA);
        }
        return format.format(new Date(time));
    }

}
