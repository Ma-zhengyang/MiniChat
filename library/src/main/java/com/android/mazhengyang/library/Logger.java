package com.android.mazhengyang.library;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.android.mazhengyang.library.printer.DiskPrinter;
import com.android.mazhengyang.library.printer.LogcatPrinter;
import com.android.mazhengyang.library.printer.Printer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mazhengyang on 18-10-30.
 */

public class Logger {

    private static final String TAG = Logger.class.getSimpleName();

    private List<Printer> printers = new ArrayList<>();

    private Builder builder;

    public Logger(Builder bulider) {

        this.builder = bulider;

        if (ActivityCompat.checkSelfPermission(bulider.context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(bulider.context, "缺少读写权限，请先在设置中打开", Toast.LENGTH_LONG).show();
        } else {
            printers.add(new DiskPrinter(bulider.context));
        }

        if (bulider.print) {
            printers.add(new LogcatPrinter());
        }
    }

    public void v(String tag, String message) {
        if ((builder.type & LogType.VERBOSE) == LogType.VERBOSE) {
            for (Printer printer : printers) {
                printer.v(tag, message);
            }
        }
    }

    public void d(String tag, String message) {
        if ((builder.type & LogType.DEBUG) == LogType.DEBUG) {
            for (Printer printer : printers) {
                printer.d(tag, message);
            }
        }
    }

    public void i(String tag, String message) {
        if ((builder.type & LogType.INFO) == LogType.INFO) {
            for (Printer printer : printers) {
                printer.i(tag, message);
            }
        }
    }

    public void w(String tag, String message) {
        if ((builder.type & LogType.WARN) == LogType.WARN) {
            for (Printer printer : printers) {
                printer.w(tag, message);
            }
        }
    }

    public void e(String tag, String message) {
        if ((builder.type & LogType.ERROR) == LogType.ERROR) {
            for (Printer printer : printers) {
                printer.e(tag, message);
            }
        }
    }

    public void start() {
        for (Printer printer : printers) {
            printer.start();
        }
    }

    public void stop() {
        for (Printer printer : printers) {
            printer.stop();
        }
        builder = null;
    }

    public void cleanPreviewLog() {
        for (Printer printer : printers) {
            printer.clean();
        }
    }

    public static Builder newBuilder(Context context) {
        return new Builder(context);
    }

    public static class Builder {
        public Logger logger;
        public Context context;
        public int type;
        public boolean print = true;

        public Builder(Context context) {
            this.context = context;
        }

        /**
         * @param type 日志输出类型
         * @return
         */
        public Builder setLogType(int type) {
            this.type = type;
            return this;
        }

        /**
         * @param print //是否输出到控制台，默认打印
         * @return
         */
        public Builder withPrint(boolean print) {
            this.print = print;
            return this;
        }

        public Logger bulid() {
            if (logger == null) {
                logger = new Logger(this);
            }
            return logger;
        }
    }

}
