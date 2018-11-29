package com.android.mazhengyang.minichat.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.User;
import com.android.mazhengyang.minichat.listener.Listener;
import com.android.mazhengyang.minichat.listener.UDPMessageListener;
import com.android.mazhengyang.minichat.model.UDPMessage;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class ChatService extends Service implements UDPMessageListener.Callback {

    private static final String TAG = "MiniChat." + ChatService.class.getSimpleName();

    //保存当前在线用户，键值为用户的ip
    private Map<String, User> userMap = new ConcurrentHashMap<>();
    //保存用户发的消息，每个ip都会开启一个消息队列来缓存消息
    private Map<String, Queue<UDPMessage>> messagesMap = new ConcurrentHashMap<>();

    private UDPMessageListener udpMessageListener;

    public interface ChatServiceListener {
        void showMsg(String msg);

        void updateUserMap(Map<String, User> userMap);
    }

    private ChatServiceListener listener;

    public void setChatServiceListener(ChatServiceListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        udpMessageListener = UDPMessageListener.getInstance(userMap, messagesMap);
        udpMessageListener.setCallback(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        udpMessageListener.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ChatServiceBinder();
    }

    public void start() {
        udpMessageListener.open();
    }

    @Override
    public void handleReceivedMsg(int type) {
        Log.d(TAG, "handleReceivedMsg: type=" + type);
        switch (type) {
            case Listener.LOGIN_SUCC:
            case Listener.ADD_USER:
            case Listener.REMOVE_USER:
                if (listener != null) {
                    listener.updateUserMap(userMap);
                }
                break;
            case Listener.ASK_VIDEO:
            case Listener.REPLAY_VIDEO_ALLOW:
            case Listener.REPLAY_VIDEO_NOT_ALLOW:

            case Listener.REPLAY_SEND_FILE:
            case Listener.RECEIVE_MSG:

            case Listener.ASK_SEND_FILE:
                //    sendBroadcast(new Intent(MessageUpdateBroadcastReceiver.ACTION_NOTIFY_DATA));
                break;
            case Listener.TO_ALL_MESSAGE:
                //     sendBroadcast(new Intent(RoomChatBroadcastReceiver.ACTION_NOTIFY_DATA));
                break;

        }
    }

    @Override
    public void showMsg(String msg) {
        Log.d(TAG, "showMsg: ");
        if (listener != null) {
            listener.showMsg(msg);
        }
    }

    public final class ChatServiceBinder extends Binder {

        public ChatService getService() {
            return ChatService.this;
        }
    }

}
