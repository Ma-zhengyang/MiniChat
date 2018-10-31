package com.android.mazhengyang.library.printer;

/**
 * Created by mazhengyang on 18-10-30.
 */

public interface Printer {

    void d(String tag, String message);

    void w(String tag, String message);

    void i(String tag, String message);

    void e(String tag, String message);

    void v(String tag, String message);

    void start();

    void stop();

    void clean();
}
