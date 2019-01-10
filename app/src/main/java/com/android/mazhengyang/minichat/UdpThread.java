package com.android.mazhengyang.minichat;

import android.os.Build;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;
import com.android.mazhengyang.minichat.model.ISocketCallback;
import com.android.mazhengyang.minichat.util.Constant;
import com.android.mazhengyang.minichat.util.NetUtils;

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

    public static final int ACTION_ONLINE = 1000;//上线
    public static final int ACTION_OFFLINE = ACTION_ONLINE + 1;//下线
    public static final int ACTION_ONLINED = ACTION_OFFLINE + 1;//登录成功，用来反馈对方

    public static final int MESSAGE_TO_ALL = 2000;//广播,发送消息给全部ip
    public static final int MESSAGE_TO_TARGET = MESSAGE_TO_ALL + 1;//发送消息给指定ip

    private boolean isOnline;
    //用于接收和发送数据的socket，DatagramSocket只能向指定地址发送，MulticastSocket能实现多点广播
    private MulticastSocket multicastSocket;
    private DatagramPacket datagramPacket;
    //保存用户列表
    private List<UserBean> userList = new ArrayList<>();

    private int port = Constant.MESSAGE_PORT;
    private final static int DEFAULT_BUFFERSIZE = 1024 * 2;
    private byte[] bufferData;

    private ExecutorService executorService;

    private ISocketCallback socketCallback;

    private static UdpThread instance;

    public static UdpThread getInstance() {
        if (instance == null) {
            instance = new UdpThread();
        }
        return instance;
    }

    public void setSocketCallback(ISocketCallback socketCallback) {
        this.socketCallback = socketCallback;
    }

    @Override
    public void run() {
        while (true) {
            if (!isOnline) {
                continue;
            }
            try {
                if (multicastSocket != null) {
                    multicastSocket.receive(datagramPacket);
                    if (datagramPacket.getLength() == 0) {
                        continue;
                    }
                    handleReceivedMsg(bufferData, datagramPacket);
                    datagramPacket.setLength(DEFAULT_BUFFERSIZE);
                } else {
                    Log.d(TAG, "run: multicastSocket is null");
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

    public void startWork() {
        Log.d(TAG, "start: ");
        try {
            executorService = Executors.newFixedThreadPool(10);
            multicastSocket = new MulticastSocket(port);
            bufferData = new byte[DEFAULT_BUFFERSIZE];
            datagramPacket = new DatagramPacket(bufferData, bufferData.length);
            setPriority(MAX_PRIORITY);
            start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "UdpThread: " + e);
        }
    }

    /**
     * 通知上下线
     *
     * @param isOnline
     */
    public void setOnline(boolean isOnline) {
        Log.d(TAG, "setOnline: ");

        if (this.isOnline != isOnline) {
            this.isOnline = isOnline;

            try {
                if (isOnline) {
                    send(packUdpMessage(Constant.ALL_ADDRESS, "", ACTION_ONLINE).toString(),
                            InetAddress.getByName(Constant.ALL_ADDRESS));
                } else {

                    //如果是wifi信号等原因中途断网的，是无法send的，只能把自己设置下线
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(NetUtils.getLocalIpAddress()) && user.isOnline()) {
                            user.setOnline(false);
                            freshUserList(userList);
                            break;
                        }
                    }

                    //正常relese方式退出的，能send
                    send(packUdpMessage(Constant.ALL_ADDRESS, "", ACTION_OFFLINE).toString(),
                            InetAddress.getByName(Constant.ALL_ADDRESS));
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "startRun: " + e);
            }
        }

    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "release: ");

        setOnline(false);
        interrupt();
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        instance = null;
    }

    private void send(String messageBean, InetAddress destIp) {
        send(messageBean, destIp, Constant.MESSAGE_PORT);
    }

    public void send(MessageBean messageBean) {

        //自己给自己发消息
        String selfIp = NetUtils.getLocalIpAddress();
        if (selfIp.equals(messageBean.getReceiverIp())) {
            freshMessage(messageBean);
            return;
        }

        //发送给对方
        try {
            String receiverIp = messageBean.getReceiverIp();
            send(messageBean.toString(), InetAddress.getByName(receiverIp));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "send: " + e);
        }
    }

    /**
     * 发送UDP数据包
     *
     * @param msg      消息
     * @param destIp   目标地址
     * @param destPort 目标端口
     */
    private void send(final String msg, final InetAddress destIp, final int destPort) {
        // Log.d(TAG, "send: " + executorService);
        Log.d(TAG, "send: destIp=" + destIp);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: send");
                    DatagramPacket datagramPacket = new DatagramPacket(msg.getBytes(Constant.ENCOD),
                            msg.length(), destIp, destPort);
                    multicastSocket.send(datagramPacket);
                    if (!isOnline) {
                        Log.d(TAG, "run: close multicastSocket");
                        multicastSocket.close();
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
     * @param receiverIp
     * @param message
     * @param type
     * @return
     */
    public MessageBean packUdpMessage(String receiverIp, String message, int type) {
        MessageBean messageBean = new MessageBean();
        messageBean.setSenderName(Build.DEVICE);
        messageBean.setSenderIp(NetUtils.getLocalIpAddress());
        messageBean.setReceiverIp(receiverIp);
        messageBean.setDeviceCode(Build.DEVICE);
        messageBean.setMsg(message);
        messageBean.setType(type);
        return messageBean;
    }


    /**
     * 处理接收到的消息
     *
     * @param data
     * @param datagramPacket
     */
    private void handleReceivedMsg(byte[] data, DatagramPacket datagramPacket) {
        Log.d(TAG, "handleReceivedMsg: start.");
        try {

            String s = new String(data, 0, datagramPacket.getLength(), Constant.ENCOD);
            MessageBean messageBean = new MessageBean(new JSONObject(s));

            String selfIp = NetUtils.getLocalIpAddress();
            String senderIp = datagramPacket.getAddress().getHostAddress();//对方ip。自己给自己发的话这个ip就是自己

            Log.d(TAG, "handleReceivedMsg: selfIp=" + selfIp);
            Log.d(TAG, "handleReceivedMsg: senderIp=" + senderIp);

            switch (messageBean.getType()) {
                case ACTION_ONLINE: //来自noticeOnline中的群播，每个用户都会收到，包括自己
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINE");

                    boolean isOld = false;
                    //user已经存在，直接更新在线状态
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(senderIp)) {
                            Log.d(TAG, "handleReceivedMsg: " + senderIp + " has existed.");
                            if (!user.isOnline()) {
                                user.setOnline(true);
                            }
                            isOld = true;
                            break;
                        }
                    }

                    if (!isOld) {
                        //新user，加入
                        Log.d(TAG, "handleReceivedMsg: " + senderIp + " not existed, add it.");
                        UserBean newUser = new UserBean();
                        newUser.setUserIp(senderIp);
                        newUser.setUserName(messageBean.getSenderName());
                        newUser.setDeviceCode(messageBean.getDeviceCode());
                        newUser.setOnline(true);
                        newUser.setSelf(selfIp.equals(senderIp));
                        userList.add(newUser);
                    }

                    //自己上线后，对方接收到ACTION_ONLINE后执行这里，这样就会把对方加入到自己列表
                    if (!selfIp.equals(senderIp)) {
                        send(packUdpMessage(Constant.ALL_ADDRESS, "", ACTION_ONLINED).toString(),
                                datagramPacket.getAddress());
                    }
                    freshUserList(userList);

                    break;
                case ACTION_OFFLINE:
                    Log.d(TAG, "handleReceivedMsg: ACTION_OFFLINE");
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(senderIp) && user.isOnline()) {
                            user.setOnline(false);
                            freshUserList(userList);
                            break;
                        }
                    }
                    break;
                //在对方登陆成功后返回的验证消息，把对方加入自己列表
                case ACTION_ONLINED:
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINED");
                    UserBean user = new UserBean();
                    user.setUserIp(senderIp);
                    user.setUserName(messageBean.getSenderName());
                    user.setDeviceCode(messageBean.getDeviceCode());
                    user.setOnline(true);
                    userList.add(user);

                    freshUserList(userList);
                    break;
                case MESSAGE_TO_TARGET://接收对方发过来的消息
                    Log.d(TAG, "handleReceivedMsg: MESSAGE_TO_TARGET");
                    freshMessage(messageBean);
                    break;
                case MESSAGE_TO_ALL:
                    Log.d(TAG, "handleReceivedMsg: MESSAGE_TO_ALL");
                    break;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "handleReceivedMsg: end.");
    }


    private void freshUserList(List<UserBean> userList) {
        if (socketCallback != null) {
            Log.d(TAG, "freshUserList: ");
            socketCallback.freshUserList(userList);
        } else {
            Log.e(TAG, "freshUserList: socketCallback=null");
        }
    }

    private void freshMessage(MessageBean messageBean) {
        if (socketCallback != null) {
            Log.d(TAG, "freshMessage: ");
            socketCallback.freshMessage(messageBean);
        } else {
            Log.e(TAG, "freshUserList: socketCallback=null");
        }
    }

}
