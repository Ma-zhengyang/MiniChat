package com.android.mazhengyang.minichat;

import android.os.Build;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.User;
import com.android.mazhengyang.minichat.model.UDPMessage;
import com.android.mazhengyang.minichat.util.Constant;
import com.android.mazhengyang.minichat.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mazhengyang on 18-12-6.
 */

public class UdpThread extends Thread {

    private static final String TAG = "MiniChat." + UdpThread.class.getSimpleName();

    public static final int USER_ONLINE = 1;
    public static final int USER_OFFLINE = 2;

    private boolean isRunning;
    private boolean isOnline;
    //用于接收和发送数据的socket ，DatagramSocket只能向指定地址发送，MulticastSocket能实现多点广播
    private MulticastSocket socket;
    private DatagramPacket packet;

    private List<User> userList = new ArrayList<>();

    private int port = Constant.MESSAGE_PORT;
    private final static int DEFAULT_BUFFERSIZE = 1024 * 2;
    private byte[] bufferData;

    private ExecutorService executorService;

    private Callback callback;

    public interface Callback {
        void updateUserMap(List<User> userList);
    }

    private static UdpThread instance;

    public UdpThread(Callback callback) {

        this.callback = callback;

        try {
            executorService = Executors.newFixedThreadPool(10);
            socket = new MulticastSocket(port);
            bufferData = new byte[DEFAULT_BUFFERSIZE];
            packet = new DatagramPacket(bufferData, bufferData.length);
            setPriority(MAX_PRIORITY);
            start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "UdpThread: " + e);
        }
    }

    public static UdpThread getInstance(Callback callback) {
        if (instance == null) {
            instance = new UdpThread(callback);
        }
        return instance;
    }

    @Override
    public void run() {
        while (true) {
            if (!isOnline) {
                continue;
            }
            try {
                if (socket != null) {
                    socket.receive(packet);
                    if (packet.getLength() == 0) {
                        continue;
                    }
                    Log.d(TAG, "run: packet.getLength()=" + packet.getLength());
                    handleReceivedMsg(bufferData, packet);
                    packet.setLength(DEFAULT_BUFFERSIZE);
                } else {
                    Log.d(TAG, "run: socket is null");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "run: " + e);
            }
        }
    }

    /**
     * 启动
     */
    public void startRun() {
        Log.d(TAG, "startRun: isRunning=" + isRunning);
        if (!isRunning) {
            isOnline = true;
            noticeOnline();
            isRunning = true;
        }
    }

    /**
     * 停止
     */
    public void stopRun() {
        Log.d(TAG, "stopRun: isRunning=" + isRunning);
        if (isRunning) {
            isOnline = false;
            isRunning = false;
            for (User user : userList) {
                if (user.isSelf() && user.isOnline()) {
                    user.setOnline(false);
                    if (callback != null) {
                        callback.updateUserMap(userList);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "release: ");
        noticeOffline();
        stopRun();
        interrupt();
        executorService.shutdown();
        executorService = null;
        instance = null;
    }

    /**
     * 通知上线
     */
    private void noticeOnline() {
        Log.d(TAG, "noticeOnline: ");
        try {
            send(packUdpMessage("", USER_ONLINE).toString(),
                    InetAddress.getByName(Constant.ALL_ADDRESS));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "noticeOnline: " + e);
        }
    }

    /**
     * 通知下线
     */
    private void noticeOffline() {
        Log.d(TAG, "noticeOffline: ");
        try {
            send(packUdpMessage("", USER_OFFLINE).toString(),
                    InetAddress.getByName(Constant.ALL_ADDRESS));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "noticeOffline: " + e);
        }
    }

    /**
     * 处理接收到的消息
     *
     * @param data
     * @param packet
     */
    private void handleReceivedMsg(byte[] data, DatagramPacket packet) {
        try {

            String temp = new String(data, 0, packet.getLength(), Constant.ENCOD);
            UDPMessage udpMessage = new UDPMessage(new JSONObject(temp));
            String targetIp = packet.getAddress().getHostAddress();

            Log.d(TAG, "handleReceivedMsg: targetIp=" + targetIp);

            switch (udpMessage.getType()) {
                case USER_ONLINE:
                    Log.d(TAG, "handleReceivedMsg: USER_ONLINE");

                    //user已经存在，直接更新在线状态
                    for (User user : userList) {
                        if (user.getUserIp().equals(targetIp) && !user.isOnline()) {
                            user.setOnline(true);
                            Log.d(TAG, "handleReceivedMsg: set user online");
                            if (callback != null) {
                                callback.updateUserMap(userList);
                            }
                            return;
                        }
                    }

                    //新user，加入
                    User newUser = new User();
                    newUser.setUserIp(targetIp);
                    newUser.setUserName(udpMessage.getSenderName());
                    newUser.setDeviceCode(udpMessage.getDeviceCode());
                    newUser.setOnline(true);
                    newUser.setSelf(Utils.getLocalIpAddress().equals(targetIp));
                    userList.add(newUser);
                    Log.d(TAG, "handleReceivedMsg: add new user");
                    if (callback != null) {
                        callback.updateUserMap(userList);
                    }
                    break;
                case USER_OFFLINE:
                    Log.d(TAG, "handleReceivedMsg: USER_OFFLINE");
                    for (User user : userList) {
                        if (user.getUserIp().equals(targetIp) && user.isOnline()) {
                            user.setOnline(false);
                            if (callback != null) {
                                callback.updateUserMap(userList);
                            }
                            break;
                        }
                    }
                    break;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void send(String msg, InetAddress destIp) {
        send(msg, destIp, Constant.MESSAGE_PORT);
    }

    /**
     * 发送UDP数据包
     *
     * @param msg      消息
     * @param destIp   目标地址
     * @param destPort 目标端口
     */
    private void send(final String msg, final InetAddress destIp, final int destPort) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: send");
                    DatagramPacket packet = new DatagramPacket(msg.getBytes(Constant.ENCOD),
                            msg.length(), destIp, destPort);
                    socket.send(packet);
                    if (!isOnline) {
                        Log.d(TAG, "run: close socket");
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: " + e);
                }
            }
        });
    }

    /**
     * 封装消息
     *
     * @param msg
     * @param type
     * @return
     */
    private UDPMessage packUdpMessage(String msg, int type) {
        UDPMessage message = new UDPMessage();
        message.setSenderName(Build.DEVICE);
        message.setDeviceCode(Build.DEVICE);
        message.setMsg(msg);
        message.setType(type);
        message.setOwn(true);
        return message;
    }

}
