package com.android.mazhengyang.minichat.saver;

import android.os.Environment;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.MessageBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by mazhengyang on 19-1-25.
 */

public class Storage {

    private static final String TAG = Storage.class.getSimpleName();

    private static ArrayList<Writer> writerList;

    private static final int INITIAL_BUFFER_SIZE = 256;
    private static StringBuffer buffer = new StringBuffer(INITIAL_BUFFER_SIZE);

    public static void addMessage(MessageBean messageBean) {
        // Save the message.
        Log.d(TAG, "addMessage: start.");
        
        String title = messageBean.getSenderDeviceCode();
        
        PrintWriter printWriter = null;

        for (Writer writer : writerList) {
            if (title.equals(writer.getTitle())) {
                printWriter = writer.getPrintWriter();
                break;
            }
        }

        if (printWriter == null) {
            printWriter = createPrintWriter(title);
            writerList.add(new Writer(title, printWriter));
        }

        if (buffer.length() > 0) {
            buffer.delete(0, buffer.length());
        }

        buffer.append(messageBean.toString());

        printWriter.println(buffer.toString());
        printWriter.flush();

        Log.d(TAG, "addMessage: end.");
        
    }

    private static PrintWriter createPrintWriter(String title) {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            Log.d(TAG, "createPrintWriter: not MEDIA_MOUNTED");
            return null;
        }

        String direcotry = Environment.getDataDirectory().getAbsolutePath();
        Log.d(TAG, "createPrintWriter: direcotry" + direcotry);

        File dir = new File(direcotry);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File messageFile = new File(direcotry, title);
        if (!messageFile.exists()) {
            try {
                if (!messageFile.createNewFile()) {
                    Log.e(TAG, "Unable to create " + title);
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "createPrintWriter: ", e);
            }
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(messageFile, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "createPrintWriter: ", e);
        }

        if (fileOutputStream != null) {
            PrintWriter printWriter = new PrintWriter(fileOutputStream);
            return printWriter;
        } else {
            Log.e(TAG, "Failed to create the log file (no stream)");
        }
        return null;
    }

    public static void stop() {
        Log.d(TAG, "stop: ");
        for (Writer writer : writerList) {
            PrintWriter printWriter = writer.getPrintWriter();
            if (printWriter != null) {
                printWriter.close();
                printWriter = null;
            }
        }

    }

    private static class Writer {
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


}
