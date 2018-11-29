package com.android.mazhengyang.minichat.listener;

import android.os.Build;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.User;
import com.android.mazhengyang.minichat.model.UDPMessage;
import com.android.mazhengyang.minichat.util.Constant;
import com.android.mazhengyang.minichat.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by mazhengyang on 18-11-29.
 */

public class UDPMessageListener extends UDPListener {

    private static final String TAG = "MiniChat." + UDPMessageListener.class.getSimpleName();

    //文本消息监听端口
    private final int port = Constant.MESSAGE_PORT;
    private final int BUFFER_SIZE = 1024 * 3;//3k的数据缓冲区
    //保存当前在线用户，键值为用户的ip
    final Map<String, User> userMap;
    //保存用户发的消息，每个ip都会开启一个消息队列来缓存消息
    final Map<String, Queue<UDPMessage>> messagesMap;

    private static UDPMessageListener instance;

    private UDPMessageListener(Map<String, User> userMap, Map<String, Queue<UDPMessage>> messagesMap) {
        this.userMap = userMap;
        this.messagesMap = messagesMap;
    }

    public static UDPMessageListener getInstance(Map<String, User> userMap, Map<String, Queue<UDPMessage>> messagesMap) {
        if (instance == null) {
            instance = new UDPMessageListener(userMap, messagesMap);
        }
        return instance;
    }

    private Callback callback;

    public interface Callback {
        void showMsg(String msg);

        void handleReceivedMsg(int type);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void init() {
        setPort(port);
        setBufferSize(BUFFER_SIZE);
    }

    @Override
    public void noticeOnline() {
        Log.d(TAG, "noticeOnline: ");
        try {
            send(makeUdpMessage("", ADD_USER).toString(),
                    InetAddress.getByName(Constant.ALL_ADDRESS));
        } catch (UnknownHostException e) {
            Log.e(TAG, "noticeOnline: " + e);
        }
    }

    @Override
    public void noticeOffline() {
        Log.d(TAG, "noticeOffline: ");
        try {
            send(makeUdpMessage("", REMOVE_USER).toString(),
                    InetAddress.getByName(Constant.ALL_ADDRESS));
        } catch (UnknownHostException e) {
            Log.e(TAG, "noticeOffline: " + e);
        }
    }

    /**
     * 发送UDP数据包
     *
     * @param msg    消息
     * @param destIp 目标地址
     */
    public void send(String msg, InetAddress destIp) {
        send(msg, destIp, Constant.MESSAGE_PORT);
    }

    @Override
    public void close() {
        super.close();
        instance = null;
        if (userMap != null) {
            userMap.clear();
        }
        if (messagesMap != null) {
            userMap.clear();
        }
    }

    @Override
    public void sendMsg(String msg) {
        if (callback != null) {
            callback.showMsg(msg);
        }
    }

    private UDPMessage makeUdpMessage(String msg, int type) {
        UDPMessage message = new UDPMessage();
        message.setType(type);
        message.setSenderName(Build.DEVICE);
        message.setMsg(msg);
        message.setDeviceCode(Build.DEVICE);
        message.setOwn(true);
        return message;
    }

    @Override
    public void handleReceivedMsg(byte[] data, DatagramPacket packet) {
        try {
            String temp = new String(data, 0, packet.getLength(), Constant.ENCOD);//得到接收的消息
            UDPMessage msg = new UDPMessage(new JSONObject(temp));
            String sourceIp = packet.getAddress().getHostAddress();//对方ip
            int type = msg.getType();

            switch (type) {
                case ADD_USER://增加一个用户
                    Log.d(TAG, "handleReceivedMsg: ADD_USER");
                    User user = new User();
                    user.setIp(sourceIp);
                    user.setUserName(msg.getSenderName());
                    user.setDeviceCode(msg.getDeviceCode());
                    //构造回送报文内容
                    if (!Utils.getLocalIpAddress().equals(user.getIp())) {
                        userMap.put(sourceIp, user);
                        send(makeUdpMessage("", LOGIN_SUCC).toString(), packet.getAddress());
                    }
                    break;

                case LOGIN_SUCC://在对方登陆成功后返回的验证消息
                    Log.d(TAG, "handleReceivedMsg: LOGIN_SUCC");
                    user = new User();
                    user.setIp(sourceIp);
                    user.setUserName(msg.getSenderName());
                    user.setDeviceCode(msg.getDeviceCode());
                    userMap.put(sourceIp, user);
                    break;

                case REMOVE_USER://删除用户
                    Log.d(TAG, "handleReceivedMsg: REMOVE_USER");
                    userMap.remove(sourceIp);
                    break;

                case ASK_VIDEO:
                case REPLAY_VIDEO_ALLOW:
                case REPLAY_VIDEO_NOT_ALLOW:
                case REPLAY_SEND_FILE://回复文件传输邀请
                case ASK_SEND_FILE://收到文件传输邀请
                case RECEIVE_MSG://接收到消息
                    Log.d(TAG, "handleReceivedMsg: ADD_USER");
                    if (messagesMap.containsKey(sourceIp)) {
                        messagesMap.get(sourceIp).add(msg);//更新现有
                    } else {
                        Queue<UDPMessage> queue = new ConcurrentLinkedQueue<UDPMessage>();
                        queue.add(msg);
                        messagesMap.put(sourceIp, queue);//新增
                    }
                    break;

                case TO_ALL_MESSAGE://message to all
                    Log.d(TAG, "handleReceivedMsg: TO_ALL_MESSAGE");
                    if (messagesMap.containsKey(Constant.ALL_ADDRESS)) {
                        messagesMap.get(Constant.ALL_ADDRESS).add(msg);//更新现有
                    } else {
                        Queue<UDPMessage> queue = new ConcurrentLinkedQueue<UDPMessage>();
                        queue.add(msg);
                        messagesMap.put(Constant.ALL_ADDRESS, queue);//新增
                    }
                    break;

                case HEART_BEAT://心跳包检测
                    Log.d(TAG, "handleReceivedMsg: HEART_BEAT");
                    send(makeUdpMessage("", HEART_BEAT_REPLY).toString(), packet.getAddress());//回复心跳包
                    user = userMap.get(sourceIp);
                    if (user != null) {
                        user.setHeartTime(System.currentTimeMillis() + "");
                        Log.e("UDPMessageListener", "接收心跳包：" + user.getHeartTime());
                    }
                    break;

                case HEART_BEAT_REPLY://接收到心跳包
                    Log.d(TAG, "handleReceivedMsg: HEART_BEAT_REPLY");
                    user = userMap.get(sourceIp);
                    if (user != null)
                        user.setHeartTime(System.currentTimeMillis() + "");//更新心跳包的最后时间
                    break;

                case REQUIRE_ICON://请求头像
                    Log.d(TAG, "handleReceivedMsg: REQUIRE_ICON");
                    break;
            }
            if (callback != null) {
                callback.handleReceivedMsg(type);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
