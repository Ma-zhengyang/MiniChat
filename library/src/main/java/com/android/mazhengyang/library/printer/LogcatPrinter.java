package com.android.mazhengyang.library.printer;

import android.util.Log;

/**
 * Created by mazhengyang on 18-10-30.
 */

public class LogcatPrinter implements Printer {

    private static final String TAG = LogcatPrinter.class.getSimpleName();

    public LogcatPrinter() {

    }

    @Override
    public void start() {
        Log.d(TAG, "start: ");
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop: ");
    }

    @Override
    public void clean() {
        Log.d(TAG, "clean: ");
    }

    @Override
    public void v(String tag, String message) {
        Log.v(tag, message);
    }

    @Override
    public void d(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void i(String tag, String message) {
        Log.i(tag, message);
    }

    @Override
    public void w(String tag, String message) {
        Log.w(tag, message);
    }

    @Override
    public void e(String tag, String message) {
        Log.e(tag, message);
    }

}
