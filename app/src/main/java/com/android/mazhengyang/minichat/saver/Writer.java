package com.android.mazhengyang.minichat.saver;

import java.io.PrintWriter;

/**
 * Created by mazhengyang on 19-1-28.
 */

public class Writer {

    String title;
    PrintWriter printWriter;

    public Writer(String title, PrintWriter printWriter) {
        this.title = title;
        this.printWriter = printWriter;
    }

    public String getTitle() {
        return title;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

}
