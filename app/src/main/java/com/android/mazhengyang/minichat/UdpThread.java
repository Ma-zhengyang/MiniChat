package com.android.mazhengyang.minichat;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;
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

    public static final int ACTION_ONLINE = 1000;//上线
    public static final int ACTION_OFFLINE = ACTION_ONLINE + 1;//下线
    public static final int ACTION_ONLINED = ACTION_OFFLINE + 1;//登录成功，用来反馈对方

    public static final int MESSAGE_TO_ALL = 2000;//广播,发送消息给全部ip
    public static final int MESSAGE_TO_TARGET = MESSAGE_TO_ALL + 1;//发送消息给指定ip

    private Handler handler;

    private boolean isOnline;
    //用于接收和发送数据的socket，DatagramSocket只能向指定地址发送，MulticastSocket能实现多点广播
    private MulticastSocket multicastSocket;
    private DatagramPacket datagramPacket;
    //保存用户列表
    private List<UserBean> userList = new ArrayList<>();
    //保存用户发的消息，每个ip都会开启一个消息队列来缓存消息
    private Map<String, Queue<MessageBean>> messagesMap = new ConcurrentHashMap<>();

    private int port = Constant.MESSAGE_PORT;
    private final static int DEFAULT_BUFFERSIZE = 1024 * 2;
    private byte[] bufferData;

    private ExecutorService executorService;

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

    public void start(Handler handler) {
        Log.d(TAG, "start: ");
        this.handler = handler;

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
        handler.removeCallbacksAndMessages(null);
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

        //显示出自己发的消息
        String selfIp = NetUtils.getLocalIpAddress();
        if (messagesMap.containsKey(selfIp)) {
            messagesMap.get(selfIp).add(messageBean);//更新现有
        } else {
            Queue<MessageBean> queue = new ConcurrentLinkedQueue<>();
            queue.add(messageBean);
            messagesMap.put(selfIp, queue);//新增
        }
        freshMessage(messagesMap);

        String receiverIp = messageBean.getReceiverIp();

        if (selfIp.equals(receiverIp)) {
            Log.d(TAG, "send: this message is to self.");
            //如果是自己发给自己的，就不发出去，以免在run()接收处理后又显示一遍
        } else {
            //发生给对方
            try {
                send(messageBean.toString(), InetAddress.getByName(receiverIp));
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "send: " + e);
            }
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
            String sourceIp = datagramPacket.getAddress().getHostAddress();//对方ip。自己给自己发的话这个ip就是自己

            Log.d(TAG, "handleReceivedMsg: selfIp=" + selfIp);
            Log.d(TAG, "handleReceivedMsg: sourceIp=" + sourceIp);

            switch (messageBean.getType()) {
                case ACTION_ONLINE: //来自noticeOnline中的群播，每个用户都会收到，包括自己
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINE");

                    boolean isOld = false;
                    //user已经存在，直接更新在线状态
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(sourceIp)) {
                            Log.d(TAG, "handleReceivedMsg: " + sourceIp + " has existed.");
                            if (!user.isOnline()) {
                                user.setOnline(true);
                            }
                            isOld = true;
                            break;
                        }
                    }

                    if (!isOld) {
                        //新user，加入
                        Log.d(TAG, "handleReceivedMsg: " + sourceIp + " not existed, add it.");
                        UserBean newUser = new UserBean();
                        newUser.setUserIp(sourceIp);
                        newUser.setUserName(messageBean.getSenderName());
                        newUser.setDeviceCode(messageBean.getDeviceCode());
                        newUser.setOnline(true);
                        newUser.setSelf(selfIp.equals(sourceIp));
                        userList.add(newUser);
                    }

                    //自己上线后，对方接收到ACTION_ONLINE后执行这里，这样就会把对方加入到自己列表
                    if (!selfIp.equals(sourceIp)) {
                        send(packUdpMessage(Constant.ALL_ADDRESS, "", ACTION_ONLINED).toString(),
                                datagramPacket.getAddress());
                    }
                    freshUserList(userList);

                    break;
                case ACTION_OFFLINE:
                    Log.d(TAG, "handleReceivedMsg: ACTION_OFFLINE");
                    for (UserBean user : userList) {
                        if (user.getUserIp().equals(sourceIp) && user.isOnline()) {
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
                    user.setUserIp(sourceIp);
                    user.setUserName(messageBean.getSenderName());
                    user.setDeviceCode(messageBean.getDeviceCode());
                    user.setOnline(true);
                    userList.add(user);

                    freshUserList(userList);
                    break;
                case MESSAGE_TO_TARGET://接收对方发过来的消息
                    Log.d(TAG, "handleReceivedMsg: MESSAGE_TO_TARGET");
                    if (messagesMap.containsKey(selfIp)) {
                        Log.d(TAG, "handleReceivedMsg: 11111");
                        messagesMap.get(selfIp).add(messageBean);//更新现有
                    } else {
                        Log.d(TAG, "handleReceivedMsg: 22222");
                        Queue<MessageBean> queue = new ConcurrentLinkedQueue<>();
                        queue.add(messageBean);
                        messagesMap.put(selfIp, queue);//新增
                    }
                    freshMessage(messagesMap);
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
        Message message = new Message();
        message.what = Constant.MESSAGE_FRESH_USERLIST;
        message.obj = userList;
        handler.sendMessage(message);
    }

    private void freshMessage(Map<String, Queue<MessageBean>> messageMap) {
        Message message = new Message();
        message.what = Constant.MESSAGE_FRESH_MESSAGE;
        message.obj = messageMap;
        handler.sendMessage(message);
    }

}
