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

public class MessageSaver extends Thread {

    private static final String TAG = "MiniChat." + MessageSaver.class.getSimpleName();

    private static final int SAVE_QUEUE_LIMIT = 3;

    private ArrayList<MessageBean> mQueue;
    private static ArrayList<Writer> writerList;

    private boolean mStop;

    private static final int INITIAL_BUFFER_SIZE = 256;
    private static StringBuffer buffer = new StringBuffer(INITIAL_BUFFER_SIZE);

    public MessageSaver() {
        mQueue = new ArrayList<>();
        writerList = new ArrayList<>();
        start();
    }

    // Runs in main thread
    public synchronized boolean queueFull() {
        return (mQueue.size() >= SAVE_QUEUE_LIMIT);
    }

    // Runs in main thread
    public void addMessage(MessageBean messageBean) {

        synchronized (this) {
            while (mQueue.size() >= SAVE_QUEUE_LIMIT) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                }
            }
            mQueue.add(messageBean);
            notifyAll(); // Tell saver thread there is new work to do.
        }
    }

    // Runs in saver thread
    @Override
    public void run() {
        while (true) {
            MessageBean messageBean;
            synchronized (this) {
                if (mQueue.isEmpty()) {
                    notifyAll(); // notify main thread in waitDone

                    // Note that we can only stop after we saved all images
                    // in the queue.
                    if (mStop)
                        break;

                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                    continue;
                }
                if (mStop)
                    break;
                messageBean = mQueue.remove(0);
                notifyAll(); // the main thread may wait in addImage
            }
            store(messageBean);
        }
        if (!mQueue.isEmpty()) {
            Log.e(TAG, "Media saver thread stopped with " + mQueue.size() + " images unsaved");
            mQueue.clear();
        }
    }

    // Runs in main thread
    public void finish() {
        synchronized (this) {
            mStop = true;
            notifyAll();

            for (Writer writer : writerList) {
                PrintWriter printWriter = writer.getPrintWriter();
                if (printWriter != null) {
                    printWriter.close();
                    printWriter = null;
                }
            }
        }
    }

    public static void store(MessageBean messageBean) {
        // Save the message.
        Log.d(TAG, "addMessage: start.");

        String deviceCode = messageBean.getKey();

        //创建PrintWriter
        PrintWriter printWriter = null;
        for (Writer writer : writerList) {
            if (deviceCode.equals(writer.getTitle())) {
                printWriter = writer.getPrintWriter();
                break;
            }
        }
        if (printWriter == null) {
            printWriter = createPrintWriter(deviceCode);
            writerList.add(new Writer(deviceCode, printWriter));
        }

        //写数据
        if (buffer.length() > 0) {
            buffer.delete(0, buffer.length());
        }
        buffer.append(messageBean.toString());

        printWriter.println(buffer.toString());
        printWriter.flush();

        Log.d(TAG, "addMessage: end.");

    }

    private static PrintWriter createPrintWriter(String deviceCode) {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            Log.d(TAG, "createPrintWriter: not MEDIA_MOUNTED");
            return null;
        }

        String direcotry = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d(TAG, "createPrintWriter: direcotry=" + direcotry);

//        File dir = new File(direcotry);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }

        String title = deviceCode + ".txt";
        File messageFile = new File(direcotry, title);
        if (!messageFile.exists()) {
            try {
                if (!messageFile.createNewFile()) {
                    Log.d(TAG, "createPrintWriter: Unable to create " + title);
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

}
