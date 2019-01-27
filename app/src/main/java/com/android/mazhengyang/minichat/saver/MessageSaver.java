package com.android.mazhengyang.minichat.saver;

import android.util.Log;

import com.android.mazhengyang.minichat.bean.MessageBean;

import java.util.ArrayList;

/**
 * Created by mazhengyang on 19-1-25.
 */

public class MessageSaver extends Thread {

    private static final String TAG = MessageSaver.class.getSimpleName();

    private static final int SAVE_QUEUE_LIMIT = 3;

    private ArrayList<MessageBean> mQueue;
    private boolean mStop;

    public MessageSaver() {
        mQueue = new ArrayList<MessageBean>();
        start();
    }

    // Runs in main thread
    public synchronized boolean queueFull() {
        return (mQueue.size() >= SAVE_QUEUE_LIMIT);
    }

    // Runs in main thread
    public void addImage(MessageBean messageBean) {

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
            Storage.addMessage(messageBean);
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

            Storage.stop();
        }
    }

}
