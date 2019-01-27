package com.android.mazhengyang.minichat;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.model.ISocketCallback;
import com.android.mazhengyang.minichat.util.Constant;
import com.android.mazhengyang.minichat.util.DataUtil;
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
import java.util.concurrent.ConcurrentHashMap;
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

    public static final int MESSAGE_TO_ALL = 2000;//发送消息给全部ip地址段
    public static final int MESSAGE_TO_TARGET = MESSAGE_TO_ALL + 1;//发送消息给指定ip

    //用于接收和发送数据的socket，DatagramSocket只能向指定地址发送，MulticastSocket能实现多点广播
    private MulticastSocket multicastSocket;
    private DatagramPacket datagramPacket;
    private ExecutorService executorService;
    private boolean isRunning;
    private boolean isOnline;

    private int port = Constant.MESSAGE_PORT;
    private final static int DEFAULT_BUFFERSIZE = 1024 * 2;
    private byte[] bufferData;

    private Context context;

    //联系人列表
    private List<ContactBean> contactList = new ArrayList<>();
    //聊过天的用户列表
    private List<ContactBean> chattedContactList = new ArrayList<>();
    //保存消息  String对方IMEI码, List<MessageBean>聊天内容
    private Map<String, List<MessageBean>> messageList = new ConcurrentHashMap<>();

    //正在和自己聊天的对方用户
    private ContactBean chattingUser;

    private ISocketCallback listener;

    private static UdpThread instance;

    public static UdpThread getInstance() {
        if (instance == null) {
            instance = new UdpThread();
        }
        return instance;
    }

    public void setSocketCallback(ISocketCallback socketCallback) {
        this.listener = socketCallback;
    }

    public void setChattingUser(ContactBean userBean) {
        this.chattingUser = userBean;
    }

    public ContactBean getChattingUser() {
        return chattingUser;
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

    public void startWork(Context context) {
        Log.d(TAG, "startWork: ");
        try {
            this.context = context;
            executorService = Executors.newFixedThreadPool(10);
            multicastSocket = new MulticastSocket(port);
            bufferData = new byte[DEFAULT_BUFFERSIZE];
            datagramPacket = new DatagramPacket(bufferData, bufferData.length);
            setPriority(MAX_PRIORITY);
            isRunning = true;
            start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "startWork: " + e);
        }
    }

    /**
     * 通知上下线
     *
     * @param isOnline
     */
    public void setOnline(boolean isOnline) {
        if (!isRunning) {
            return;
        }

        Log.d(TAG, "setOnline: ");

        if (this.isOnline != isOnline) {
            this.isOnline = isOnline;

            try {
                if (isOnline) {
                    send(packUdpMessage(
                            NetUtils.getDeviceId(context),
                            Constant.ALL_ADDRESS,
                            "",
                            ACTION_ONLINE).toString(),
                            InetAddress.getByName(Constant.ALL_ADDRESS));
                } else {

                    //如果是wifi信号等原因中途断网的，是无法send的，只能把自己设置下线
                    for (ContactBean user : contactList) {
                        if (user.getUserIp().equals(NetUtils.getLocalIpAddress()) && user.isOnline()) {
                            user.setOnline(false);
                            freshContact(contactList);
                            break;
                        }
                    }

                    //正常release方式退出的，能send
                    send(packUdpMessage(NetUtils.getDeviceId(context),
                            Constant.ALL_ADDRESS,
                            "",
                            ACTION_OFFLINE).toString(),
                            InetAddress.getByName(Constant.ALL_ADDRESS));
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "setOnline: " + e);
            }
        }

    }

    /**
     * 释放资源
     */
    public void release() {

        if (!isRunning) {
            return;
        }

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

        if (!isRunning) {
            return;
        }

        String messageString = messageBean.toString();
        Log.d(TAG, "send: messageString=" + messageString);

        String selfIp = NetUtils.getLocalIpAddress();
        String receiverIp = messageBean.getReceiverIp();

        //自己给自己发消息，处理一次就行
        if (selfIp.equals(receiverIp)) {
            freshMessage(messageBean);
            return;
        }

        //对方发消息自己收到才会走handleReceivedMsg，自己发的消息也需要保存
        freshMessage(messageBean);

        //发送给对方
        try {
            send(messageString, InetAddress.getByName(receiverIp));
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
        Log.d(TAG, "send: msg=" + msg);
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
    public MessageBean packUdpMessage(String receiverDeviceCode, String receiverIp, String message, int type) {
        Log.d(TAG, "packUdpMessage: ");
        MessageBean messageBean = new MessageBean();
        messageBean.setSenderName(Build.DEVICE);
        messageBean.setSenderIp(NetUtils.getLocalIpAddress());
        messageBean.setReceiverIp(receiverIp);
        messageBean.setSenderDeviceCode(NetUtils.getDeviceId(context));
        messageBean.setReceiverDeviceCode(receiverDeviceCode);
        messageBean.setMsg(message);
        messageBean.setSendTime(DataUtil.formatTime(System.currentTimeMillis()));
        messageBean.setType(type);
        messageBean.setReaded(false);
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

            Log.d(TAG, "handleReceivedMsg: s=" + s);

            String selfIp = NetUtils.getLocalIpAddress();
            String senderIp = datagramPacket.getAddress().getHostAddress();//对方ip。自己给自己发的话这个ip就是自己

            Log.d(TAG, "handleReceivedMsg: selfIp=" + selfIp);
            Log.d(TAG, "handleReceivedMsg: senderIp=" + senderIp);

            int type = messageBean.getType();
            Log.d(TAG, "handleReceivedMsg: type=" + type);

            switch (type) {
                case ACTION_ONLINE: //来自noticeOnline中的群播，每个用户都会收到，包括自己
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINE");
                    boolean isExist = false;
                    //user已经存在，直接更新在线状态
                    for (ContactBean user : contactList) {
                        if (user.getUserIp().equals(senderIp)) {
                            Log.d(TAG, "handleReceivedMsg: " + senderIp + " has existed.");
                            if (!user.isOnline()) {
                                user.setOnline(true);
                            }
                            isExist = true;
                            break;
                        }
                    }

                    if (!isExist) {
                        //新user，加入
                        Log.d(TAG, "handleReceivedMsg: " + senderIp + " not existed, add it.");
                        ContactBean newUser = new ContactBean();
                        newUser.setUserIp(senderIp);
                        newUser.setUserName(messageBean.getSenderName());
                        newUser.setDeviceCode(messageBean.getSenderDeviceCode());
                        newUser.setOnline(true);
                        newUser.setSelf(selfIp.equals(senderIp));
                        contactList.add(newUser);
                    }

                    //自己上线后，对方接收到ACTION_ONLINE后执行这里，这样就会把对方加入到自己列表
                    if (!selfIp.equals(senderIp)) {
                        send(packUdpMessage(NetUtils.getDeviceId(context),
                                Constant.ALL_ADDRESS,
                                "",
                                ACTION_ONLINED).toString(),
                                datagramPacket.getAddress());
                    }

                    freshContact(contactList);
                    break;
                case ACTION_OFFLINE:
                    Log.d(TAG, "handleReceivedMsg: ACTION_OFFLINE");
                    for (ContactBean user : contactList) {
                        if (user.getUserIp().equals(senderIp) && user.isOnline()) {
                            user.setOnline(false);
                            freshContact(contactList);
                            break;
                        }
                    }
                    break;
                //在对方登陆成功后返回的验证消息，把对方加入自己列表
                case ACTION_ONLINED:
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINED");
                    ContactBean user = new ContactBean();
                    user.setUserIp(senderIp);
                    user.setUserName(messageBean.getSenderName());
                    user.setDeviceCode(messageBean.getSenderDeviceCode());
                    user.setOnline(true);
                    contactList.add(user);

                    freshContact(contactList);
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


    /**
     * 刷新用户列表
     *
     * @param contactList
     */
    private void freshContact(List<ContactBean> contactList) {
        Log.d(TAG, "freshContact: start");
        if (listener != null) {
            listener.freshContact(contactList);
        }
        Log.d(TAG, "freshContact: end");
    }

    /**
     * 刷新消息列表
     *
     * @param messageBean
     */
    private void freshMessage(MessageBean messageBean) {
        Log.d(TAG, "freshMessage: start");

        String senderIp = messageBean.getSenderIp();
        String receiverIp = messageBean.getReceiverIp();
        String selfIp = NetUtils.getLocalIpAddress();

        String senderDeviceCode = messageBean.getSenderDeviceCode();
        String receiverDeviceCode = messageBean.getReceiverDeviceCode();
        String deviceCodeSelf = NetUtils.getDeviceId(context);

        ContactBean whoSend = null;//这条消息是谁发来的
        ContactBean whoReceiver = null;//这条消息是谁接收的
        for (ContactBean userBean : contactList) {
            if (whoSend == null) {
                if (userBean.getUserIp().equals(senderIp)) {
                    whoSend = userBean;
                    Log.d(TAG, "freshMessage: whoSend=" + whoSend.getUserName());
                }
            }
            if (whoReceiver == null) {
                if (userBean.getUserIp().equals(receiverIp)) {
                    whoReceiver = userBean;
                    Log.d(TAG, "freshMessage: whoReceiver=" + whoReceiver.getUserName());
                }
            }
            if (whoSend != null && whoReceiver != null) {
                break;
            }
        }

        //把消息存入列表
//        String key;
//        if (senderIp.equals(selfIp)) {//我发给对方的,key是receiverIp
//            key = receiverIp;
//        } else {//对方发给我的,key是senderIp
//            key = senderIp;
//        }
//        if (messageList.containsKey(key)) {
//            List<MessageBean> list = messageList.get(key);
//            list.add(messageBean);
//        } else {
//            List<MessageBean> list = new ArrayList<>();
//            list.add(messageBean);
//            messageList.put(key, list);
//        }

        String key;
        if (senderIp.equals(selfIp)) {//我发给对方的,receiverDeviceCode
            key = receiverDeviceCode;
        } else {//对方发给我的,key是senderIp
            key = senderDeviceCode;
        }
        if (messageList.containsKey(key)) {
            List<MessageBean> list = messageList.get(key);
            list.add(messageBean);
        } else {
            List<MessageBean> list = new ArrayList<>();
            list.add(messageBean);
            messageList.put(key, list);
        }

        if (chattingUser != null) {//正在聊天界面
            Log.d(TAG, "freshMessage: current is in chatRoomFragment");

            if (chattingUser.getUserIp().equals(selfIp)) { //当前聊天界面用户对象就是自己
                Log.d(TAG, "freshMessage: current chat view is self");
                if (senderIp.equals(receiverIp)) {//自己和自己发消息，无需设置未读
                    Log.d(TAG, "freshMessage: message send by self");
                    messageBean.setReaded(true);
                    if (listener != null) {
                        listener.freshMessage(messageList, false, false);
                    }
                } else {//别人消息进来
                    Log.d(TAG, "freshMessage: message send by other");
                    int unReadMsgCount = whoSend.getUnReadMsgCount();
                    whoSend.setUnReadMsgCount(++unReadMsgCount);
                    if (listener != null) {
                        listener.freshMessage(messageList, true, false);
                    }
                }
                if (!chattedContactList.contains(whoSend)) {
                    chattedContactList.add(whoSend);
                }
                whoSend.setRecentMsg(messageBean.getMsg());
                whoSend.setRecentTime(messageBean.getSendTime());
            } else {//当前正在和朋友聊天
                String ip = chattingUser.getUserIp();
                //消息是当前正在聊天的朋友发的，不需处理
                if (ip.equals(senderIp)) {//对方发消息时
                    messageBean.setReaded(true);
                    if (!chattedContactList.contains(whoSend)) {
                        chattedContactList.add(whoSend);
                    }
                    if (listener != null) {
                        listener.freshMessage(messageList, true, false);
                    }
                    whoSend.setRecentMsg(messageBean.getMsg());
                    whoSend.setRecentTime(messageBean.getSendTime());
                } else if (ip.equals(receiverIp)) {//自己发消息时
                    messageBean.setReaded(true);
                    if (!chattedContactList.contains(whoReceiver)) {
                        chattedContactList.add(whoReceiver);
                    }
                    if (listener != null) {
                        listener.freshMessage(messageList, false, false);
                    }
                    whoReceiver.setRecentMsg(messageBean.getMsg());
                    whoReceiver.setRecentTime(messageBean.getSendTime());
                } else {
                    //第三方朋友消息进来
                    if (whoSend != null) {
                        int unReadMsgCount = whoSend.getUnReadMsgCount();
                        whoSend.setUnReadMsgCount(++unReadMsgCount);
                        if (!chattedContactList.contains(whoSend)) {
                            chattedContactList.add(whoSend);
                        }
                        if (listener != null) {
                            listener.freshMessage(messageList, true, false);
                        }
                        whoSend.setRecentMsg(messageBean.getMsg());
                        whoSend.setRecentTime(messageBean.getSendTime());
                    }
                }
            }

        } else {//当前不在聊天界面
            Log.d(TAG, "freshMessage: current is not in chatRoomFragment");

            if (whoSend != null) {
                int unReadMsgCount = whoSend.getUnReadMsgCount();
                whoSend.setUnReadMsgCount(++unReadMsgCount);
                if (!chattedContactList.contains(whoSend)) {
                    chattedContactList.add(whoSend);
                }
                if (listener != null) {
                    listener.freshMessage(messageList, true, true);
                }
                whoSend.setRecentMsg(messageBean.getMsg());
                whoSend.setRecentTime(messageBean.getSendTime());
            }
        }
        if (listener != null) {
            listener.freshChattedUserList(chattedContactList);
        }

        Log.d(TAG, "freshMessage: end");
    }

}
