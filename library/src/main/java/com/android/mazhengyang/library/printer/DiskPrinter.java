package com.android.mazhengyang.library.printer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.android.mazhengyang.library.LogType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Created by mazhengyang on 18-10-30.
 */

public class DiskPrinter implements Printer {

    private static final String TAG = DiskPrinter.class.getSimpleName();

    private String direcotry;
    private String currentFileName;
    private static PrintWriter printWriter;

    private static final String INITIAL_FILE_PREFIX = "log";
    private static final String filePrefix = INITIAL_FILE_PREFIX;

    private static final int INITIAL_BUFFER_SIZE = 256;
    private static StringBuffer buffer = new StringBuffer(INITIAL_BUFFER_SIZE);

    public DiskPrinter(Context context) {
        direcotry = context.getExternalFilesDir(null).getAbsolutePath();
    }

    @Override
    public void start() {
        Log.d(TAG, "start: ");
        getLogFile();
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop: ");
        if (printWriter != null) {
            printWriter.close();
            printWriter = null;
        }
    }

    @Override
    public void clean() {
        if (direcotry == null) {
            return;
        }
        File dir = new File(direcotry);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (File f : files) {
            String title = f.getName();
            if (title.startsWith(filePrefix)
                    && !title.equals(currentFileName)) {
                Log.d(TAG, "clean: delete " + title);
                f.delete();
            }
        }
    }

    @Override
    public void v(String tag, String message) {
        write(tag, message, LogType.VERBOSE);
    }

    @Override
    public void d(String tag, String message) {
        write(tag, message, LogType.DEBUG);
    }

    @Override
    public void i(String tag, String message) {
        write(tag, message, LogType.INFO);
    }

    @Override
    public void w(String tag, String message) {
        write(tag, message, LogType.WARN);
    }

    @Override
    public void e(String tag, String message) {
        write(tag, message, LogType.ERROR);
    }

    private static String getCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int MM = cal.get(Calendar.MONTH) + 1;
        int DD = cal.get(Calendar.DATE);
        int HH = cal.get(Calendar.HOUR_OF_DAY);
        int mm = cal.get(Calendar.MINUTE);
        int SS = cal.get(Calendar.SECOND);
        int MI = cal.get(Calendar.MILLISECOND);
        return String.format("%d-%d %d:%d:%d.%d", MM, DD, HH, mm, SS, MI);
    }

    private static void write(String tag, String message, int level) {
        if (printWriter != null) {

            if (buffer.length() > 0) {
                buffer.delete(0, buffer.length());
            }

            buffer.append(getCurrentTime());
            buffer.append("  ");

            switch (level) {
                case LogType.ERROR:
                    buffer.append('E');
                    break;
                case LogType.WARN:
                    buffer.append('W');
                    break;
                case LogType.INFO:
                    buffer.append('I');
                    break;
                case LogType.DEBUG:
                    buffer.append('D');
                    break;
                case LogType.VERBOSE:
                default:
                    buffer.append('V');
                    break;
            }

            if (tag != null) {
                buffer.append("  ");
                buffer.append(tag);
                buffer.append(':');
            }

            if (message != null) {
                buffer.append("  ");
                buffer.append(message);
            }

            printWriter.println(buffer.toString());
            printWriter.flush();
        }
    }

    private String getExternalStorageDirectory() {

        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        return Environment.getExternalStorageDirectory()
                .getAbsolutePath();
    }

    private synchronized void getLogFile() {

//        String direcotry = getExternalStorageDirectory();

        Log.d(TAG, "getLogFile: direcotry=" + direcotry);

        if (direcotry == null) {
            return;
        }

        try {
            File dir = new File(direcotry);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = String.format("%s_%s.txt", filePrefix, getCurrentTime());
            Log.d(TAG, "getLogFile: fileName=" + fileName);

            File logFile = new File(direcotry, fileName);
            if (!logFile.exists()) {
                if (!logFile.createNewFile()) {
                    Log.e(TAG, "Unable to create new log file");
                    return;
                }
            }

            FileOutputStream fileOutputStream = new FileOutputStream(logFile, false);

            if (fileOutputStream != null) {
                printWriter = new PrintWriter(fileOutputStream);
                currentFileName = fileName;
            } else {
                Log.e(TAG, "Failed to create the log file (no stream)");
            }

        } catch (IOException e) {
            Log.e(TAG, "getLogFile: ", e);
        }
    }
}
