package com.android.mazhengyang.minichat;

import android.os.Build;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;
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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mazhengyang on 18-12-6.
 */

public class UdpThread extends Thread {

    private static final String TAG = "MiniChat." + UdpThread.class.getSimpleName();

    public static final int USER_ONLINE = 1000;//上线
    public static final int USER_OFFLINE = 1001;//下线
    public static final int LOGIN_SUCC = 1002;//增加用户成功
    public static final int MESSAGE_TO_ALL = 1003;

    private boolean isOnline;
    //用于接收和发送数据的socket，DatagramSocket只能向指定地址发送，MulticastSocket能实现多点广播
    private MulticastSocket socket;
    private DatagramPacket packet;
    //保存用户列表
    private List<UserBean> userList = new ArrayList<>();
    //保存用户发的消息，每个ip都会开启一个消息队列来缓存消息
    private Map<String, Queue<MessageBean>> messages = new ConcurrentHashMap<>();

    private int port = Constant.MESSAGE_PORT;
    private final static int DEFAULT_BUFFERSIZE = 1024 * 2;
    private byte[] bufferData;

    private ExecutorService executorService;

    private Callback callback;

    //回调，在MainActivity中实现
    public interface Callback {
        //刷新用户列表
        void freshUserList(List<UserBean> userList);

        //刷新消息
        void freshMessage(Map<String, Queue<MessageBean>> messageMap);
    }

    private static UdpThread instance;

    public static UdpThread getInstance() {
        if (instance == null) {
            instance = new UdpThread();
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
     *
     * @param callback
     */

    public void startRun(Callback callback) {
        if (!isOnline) {
            Log.d(TAG, "startRun: ");
            isOnline = true;
            this.callback = callback;
            try {
                executorService = Executors.newFixedThreadPool(10);
                socket = new MulticastSocket(port);
                bufferData = new byte[DEFAULT_BUFFERSIZE];
                packet = new DatagramPacket(bufferData, bufferData.length);
                setPriority(MAX_PRIORITY);
                start();
                noticeOnline();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "UdpThread: " + e);
            }
        }
    }

    /**
     * 通知上线
     */
    public void noticeOnline() {
        Log.d(TAG, "noticeOnline: isOnline=" + isOnline);
//        if (!isOnline) {
            isOnline = true;
            try {
                send(packUdpMessage("", USER_ONLINE).toString(),
                        InetAddress.getByName(Constant.ALL_ADDRESS));
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "startRun: " + e);
            }
//        }

    }

    /**
     * 停止
     */
    public void noticeOffline() {
        Log.d(TAG, "noticeOffline: isOnline=" + isOnline);
        if (isOnline) {
            isOnline = false;
            try {
                send(packUdpMessage("", USER_OFFLINE).toString(),
                        InetAddress.getByName(Constant.ALL_ADDRESS));
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "noticeOffline: " + e);
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "release: ");

        noticeOffline();
        interrupt();
        executorService.shutdown();
        executorService = null;
        instance = null;
        isOnline = false;
    }

    /**
     * 处理接收到的消息
     *
     * @param data
     * @param packet
     */
    private void handleReceivedMsg(byte[] data, DatagramPacket packet) {
        try {

            String s = new String(data, 0, packet.getLength(), Constant.ENCOD);
            MessageBean udpMessage = new MessageBean(new JSONObject(s));

            String selfIp = Utils.getLocalIpAddress();
            String targetIp = packet.getAddress().getHostAddress();

            Log.d(TAG, "handleReceivedMsg: selfIp=" + selfIp);
            Log.d(TAG, "handleReceivedMsg: targetIp=" + targetIp);

            switch (udpMessage.getType()) {
                case USER_ONLINE:
                    Log.d(TAG, "handleReceivedMsg: USER_ONLINE");

                    boolean isOld = false;
                    //user已经存在，直接更新在线状态
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(targetIp)) {
                            Log.d(TAG, "handleReceivedMsg: " + targetIp + " is old user");
                            if (!user.isOnline()) {
                                user.setOnline(true);
                            }
                            isOld = true;
                            break;
                        }
                    }

                    if (!isOld) {
                        //新user，加入
                        UserBean newUser = new UserBean();
                        newUser.setUserIp(targetIp);
                        newUser.setUserName(udpMessage.getSenderName());
                        newUser.setDeviceCode(udpMessage.getDeviceCode());
                        newUser.setOnline(true);
                        newUser.setSelf(selfIp.equals(targetIp));
                        userList.add(newUser);
                        Log.d(TAG, "handleReceivedMsg: " + targetIp + " is new user");
                    }

                    //如果是对方发过来的USER_ONLINE信息，回馈对方，把自己加入对方用户列表
                    if (!selfIp.equals(targetIp)) {
                        send(packUdpMessage("", LOGIN_SUCC).toString(), packet.getAddress());
                    }

                    if (callback != null) {
                        callback.freshUserList(userList);
                    }
                    break;
                case USER_OFFLINE:
                    Log.d(TAG, "handleReceivedMsg: USER_OFFLINE");
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(targetIp) && user.isOnline()) {
                            user.setOnline(false);
                            if (callback != null) {
                                callback.freshUserList(userList);
                            }
                            break;
                        }
                    }
                    break;
                //在对方登陆成功后返回的验证消息
                case LOGIN_SUCC:
                    Log.d(TAG, "handleReceivedMsg: LOGIN_SUCC");
                    UserBean user = new UserBean();
                    user.setUserIp(targetIp);
                    user.setUserName(udpMessage.getSenderName());
                    user.setDeviceCode(udpMessage.getDeviceCode());
                    user.setOnline(true);
                    userList.add(user);
                    if (callback != null) {
                        callback.freshUserList(userList);
                    }
                    break;
                case MESSAGE_TO_ALL:
                    Log.d(TAG, "handleReceivedMsg: MESSAGE_TO_ALL");
                    if (messages.containsKey(Constant.ALL_ADDRESS)) {
                        messages.get(Constant.ALL_ADDRESS).add(udpMessage);//更新现有
                    } else {
                        Queue<MessageBean> queue = new ConcurrentLinkedQueue<>();
                        queue.add(udpMessage);
                        messages.put(Constant.ALL_ADDRESS, queue);//新增
                    }

                    if (callback != null) {
                        callback.freshMessage(messages);
                    }
                    break;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void send(String messageBean, InetAddress destIp) {
        send(messageBean, destIp, Constant.MESSAGE_PORT);
    }

    public void send(MessageBean messageBean, InetAddress destIp) {
        send(messageBean.toString(), destIp);
    }

    /**
     * 发送UDP数据包
     *
     * @param msg      消息
     * @param destIp   目标地址
     * @param destPort 目标端口
     */
    private void send(final String msg, final InetAddress destIp, final int destPort) {
        Log.d(TAG, "send: " + executorService);
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
    public MessageBean packUdpMessage(String msg, int type) {
        MessageBean message = new MessageBean();
        message.setSenderName(Build.DEVICE);
        message.setDeviceCode(Build.DEVICE);
        message.setMsg(msg);
        message.setType(type);
        message.setOwn(true);
        return message;
    }

}
