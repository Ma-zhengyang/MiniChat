package com.android.mazhengyang.logger;

/**
 * Created by mazhengyang on 18-10-31.
 */

public class LogUtil {

    public static void v(String tag, String msg) {

        MyApplication.getLogger().v(tag, msg);
    }

    public static void d(String tag, String msg) {

        MyApplication.getLogger().d(tag, msg);
    }

    public static void i(String tag, String msg) {

        MyApplication.getLogger().i(tag, msg);
    }

    public static void w(String tag, String msg) {

        MyApplication.getLogger().w(tag, msg);
    }

    public static void e(String tag, String msg) {

        MyApplication.getLogger().e(tag, msg);
    }
}
