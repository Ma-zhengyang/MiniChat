package com.android.mazhengyang.minichat;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.mazhengyang.minichat.bean.BaseBean;
import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.model.ISocketCallback;
import com.android.mazhengyang.minichat.saver.MessageSaver;
import com.android.mazhengyang.minichat.util.CharacterParser;
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

    private MessageSaver messageSaver;
    private Context context;

    //联系人列表
    private List<BaseBean> contactList = new ArrayList<>();
    //聊过天的用户列表
    private List<ContactBean> chattedContactList = new ArrayList<>();
    //保存消息  String对方IMEI码, List<MessageBean>聊天内容
    private Map<String, List<MessageBean>> messageListMap = new ConcurrentHashMap<>();
    //正在和自己聊天的对方用户
    private ContactBean chattingUser;

    private CharacterParser characterParser;

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

    public void setChattingUser(ContactBean contactBean) {
        this.chattingUser = contactBean;
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
            characterParser = CharacterParser.getInstance();
            messageSaver = new MessageSaver();
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
                            NetUtils.getDeviceCode(context),
                            Constant.ALL_ADDRESS,
                            "",
                            ACTION_ONLINE).toString(),
                            InetAddress.getByName(Constant.ALL_ADDRESS));
                } else {

                    //如果是wifi信号等原因中途断网的，是无法send的，只能把自己设置下线
                    for (BaseBean bean : contactList) {
                        ContactBean contactBean = (ContactBean) bean;
                        if (contactBean.getDeviceCode().equals(NetUtils.getDeviceCode(context))
                                && contactBean.isOnline()) {
                            contactBean.setOnline(false);
                            freshContact(contactList);
                            break;
                        }
                    }

                    //正常release方式退出的，能send
                    send(packUdpMessage(NetUtils.getDeviceCode(context),
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
        messageSaver.finish();
        instance = null;
    }

    private void send(String messageBean, InetAddress destIp) {
        send(messageBean, destIp, Constant.MESSAGE_PORT);
    }

    public void send(MessageBean messageBean) {

        if (!isRunning) {
            return;
        }

        //自己给自己发消息，处理一次就行
        if (messageBean.getReceiverDeviceCode()
                .equals(NetUtils.getDeviceCode(context))) {
            freshMessage(messageBean);
            Log.d(TAG, "send: self to self.");
            return;
        }

        //对方发消息自己收到才会走handleReceivedMsg，自己发的消息也需要保存
        freshMessage(messageBean);

        //发送给对方
        try {
            String messageString = messageBean.toString();
            Log.d(TAG, "send: messageString=" + messageString);
            send(messageString, InetAddress.getByName(messageBean.getReceiverIp()));
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
        messageBean.setSenderDeviceCode(NetUtils.getDeviceCode(context));
        messageBean.setReceiverDeviceCode(receiverDeviceCode);
        messageBean.setMessage(message);
        messageBean.setSendTime(DataUtil.formatTime(System.currentTimeMillis()));
        messageBean.setType(type);
        messageBean.setAlreadyRead(false);
        return messageBean;
    }


    /**
     * 处理接收到的消息
     *
     * @param data
     * @param
     */
    private void handleReceivedMsg(byte[] data, DatagramPacket datagramPacket) {
        Log.d(TAG, "handleReceivedMsg: start.");
        try {

            String s = new String(data, 0, datagramPacket.getLength(), Constant.ENCOD);
            MessageBean messageBean = new MessageBean(new JSONObject(s));

            Log.d(TAG, "handleReceivedMsg: s=" + s);

            String selfDeviceCode = NetUtils.getDeviceCode(context);
            String senderDeviceCode = messageBean.getSenderDeviceCode();
            String selfIp = NetUtils.getLocalIpAddress();
            String senderIp = datagramPacket.getAddress().getHostAddress();//对方ip。自己给自己发的话这个ip就是自己
            int type = messageBean.getType();

            Log.d(TAG, "handleReceivedMsg: selfDeviceCode=" + selfDeviceCode);
            Log.d(TAG, "handleReceivedMsg: senderDeviceCode=" + senderDeviceCode);
            Log.d(TAG, "handleReceivedMsg: selfIp=" + selfIp);
            Log.d(TAG, "handleReceivedMsg: senderIp=" + senderIp);
            Log.d(TAG, "handleReceivedMsg: type=" + type);
            Log.d(TAG, "handleReceivedMsg: ========" + messageBean.getSendTime());

            switch (type) {
                case ACTION_ONLINE: //来自noticeOnline中的群播，每个用户都会收到，包括自己
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINE");
                    boolean isExist = false;
                    //user已经存在，直接更新在线状态
                    for (BaseBean bean : contactList) {
                        ContactBean contactBean = (ContactBean) bean;
                        if (contactBean.getUserIp().equals(senderIp)) {
                            Log.d(TAG, "handleReceivedMsg: " + senderIp + " has existed.");
                            if (!contactBean.isOnline()) {
                                contactBean.setOnline(true);
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
                        String name = messageBean.getSenderName();
                        String pinyin = characterParser.getSelling(name);
                        String sortLetter = pinyin.substring(0, 1).toUpperCase();
                        newUser.setUserName(name);
                        newUser.setNamePinyin(pinyin);
                        if (sortLetter.matches("[A-Z]")) {
                            newUser.setSortLetter(sortLetter);
                        } else {
                            newUser.setSortLetter("#");
                        }
                        newUser.setDeviceCode(messageBean.getSenderDeviceCode());
                        newUser.setOnline(true);
                        contactList.add(newUser);
                    }

                    //自己上线后，对方接收到ACTION_ONLINE后执行这里，这样就会把对方加入到自己列表
                    if (!selfDeviceCode.equals(senderDeviceCode)) {
                        send(packUdpMessage(NetUtils.getDeviceCode(context),
                                Constant.ALL_ADDRESS,
                                "",
                                ACTION_ONLINED).toString(),
                                datagramPacket.getAddress());
                    }

                    freshContact(contactList);
                    break;
                case ACTION_OFFLINE:
                    Log.d(TAG, "handleReceivedMsg: ACTION_OFFLINE");
                    for (BaseBean bean : contactList) {
                        ContactBean contactBean = (ContactBean) bean;
                        if (contactBean.getUserIp().equals(senderIp) && contactBean.isOnline()) {
                            contactBean.setOnline(false);
                            freshContact(contactList);
                            break;
                        }
                    }
                    break;
                //在对方登陆成功后返回的验证消息，把对方加入自己列表
                case ACTION_ONLINED:
                    Log.d(TAG, "handleReceivedMsg: ACTION_ONLINED");
                    ContactBean contactBean = new ContactBean();
                    contactBean.setUserIp(senderIp);
                    String name = messageBean.getSenderName();
                    String pinyin = characterParser.getSelling(name);
                    String sortLetter = pinyin.substring(0, 1).toUpperCase();
                    contactBean.setUserName(name);
                    contactBean.setNamePinyin(pinyin);
                    // 正则表达式，判断首字母是否是英文字母
                    if (sortLetter.matches("[A-Z]")) {
                        contactBean.setSortLetter(sortLetter);
                    } else {
                        contactBean.setSortLetter("#");
                    }
                    contactBean.setUserName(messageBean.getSenderName());
                    contactBean.setDeviceCode(messageBean.getSenderDeviceCode());
                    contactBean.setOnline(true);
                    contactList.add(contactBean);

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
    private void freshContact(List<BaseBean> contactList) {
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

//        String senderIp = messageBean.getSenderIp();
//        String receiverIp = messageBean.getReceiverIp();

        String senderDeviceCode = messageBean.getSenderDeviceCode();
        String receiverDeviceCode = messageBean.getReceiverDeviceCode();
        String selfDeviceCode = NetUtils.getDeviceCode(context);

        ContactBean whoSend = null;//这条消息是谁发来的
        ContactBean whoReceiver = null;//这条消息是谁接收的
        for (BaseBean bean : contactList) {
            ContactBean contactBean = (ContactBean) bean;
            if (whoSend == null) {
                if (contactBean.getDeviceCode().equals(senderDeviceCode)) {
                    whoSend = contactBean;
                    Log.d(TAG, "freshMessage: whoSend=" + whoSend.getUserName());
                }
            }
            if (whoReceiver == null) {
                if (contactBean.getDeviceCode().equals(receiverDeviceCode)) {
                    whoReceiver = contactBean;
                    Log.d(TAG, "freshMessage: whoReceiver=" + whoReceiver.getUserName());
                }
            }
            if (whoSend != null && whoReceiver != null) {
                break;
            }
        }

        //把消息存入列表
        String key;
        if (senderDeviceCode.equals(selfDeviceCode)) {//我发给对方的,key是receiverDeviceCode
            key = receiverDeviceCode;
        } else {//对方发给我的,senderDeviceCode
            key = senderDeviceCode;
        }
        if (messageListMap.containsKey(key)) {
            List<MessageBean> list = messageListMap.get(key);
            list.add(messageBean);
        } else {
            List<MessageBean> list = new ArrayList<>();
            list.add(messageBean);
            messageListMap.put(key, list);
        }

        messageBean.setKey(key);
        //保存消息到文件
        messageSaver.addMessage(messageBean);

        if (chattingUser != null) {//正在聊天界面
            Log.d(TAG, "freshMessage: current is in chatRoomFragment");

            if (chattingUser.getDeviceCode().equals(selfDeviceCode)) { //当前聊天界面用户对象就是自己
                Log.d(TAG, "freshMessage: current chat view is self");
                if (senderDeviceCode.equals(receiverDeviceCode)) {//自己和自己发消息，无需设置未读
                    Log.d(TAG, "freshMessage: message send by self");
                    messageBean.setAlreadyRead(true);
                    if (listener != null) {
                        listener.freshMessage(messageListMap, false, false);
                    }
                } else {//别人消息进来
                    Log.d(TAG, "freshMessage: message send by other");
                    int unReadMsgCount = whoSend.getUnReadMsgCount();
                    whoSend.setUnReadMsgCount(++unReadMsgCount);
                    if (listener != null) {
                        listener.freshMessage(messageListMap, true, false);
                    }
                }
                if (!chattedContactList.contains(whoSend)) {
                    chattedContactList.add(whoSend);
                }
                whoSend.setRecentMsg(messageBean.getMessage());
                whoSend.setRecentTime(messageBean.getSendTime());
            } else {//当前正在和朋友聊天
                String deviceCode = chattingUser.getDeviceCode();//对方deviceCode
                //消息是当前正在聊天的朋友发的，不需处理
                if (deviceCode.equals(senderDeviceCode)) {//对方发消息时
                    messageBean.setAlreadyRead(true);
                    if (!chattedContactList.contains(whoSend)) {
                        chattedContactList.add(whoSend);
                    }
                    if (listener != null) {
                        listener.freshMessage(messageListMap, true, false);
                    }
                    whoSend.setRecentMsg(messageBean.getMessage());
                    whoSend.setRecentTime(messageBean.getSendTime());
                } else if (deviceCode.equals(receiverDeviceCode)) {//自己发消息时
                    messageBean.setAlreadyRead(true);
                    if (!chattedContactList.contains(whoReceiver)) {
                        chattedContactList.add(whoReceiver);
                    }
                    if (listener != null) {
                        listener.freshMessage(messageListMap, false, false);
                    }
                    whoReceiver.setRecentMsg(messageBean.getMessage());
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
                            listener.freshMessage(messageListMap, true, false);
                        }
                        whoSend.setRecentMsg(messageBean.getMessage());
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
                    listener.freshMessage(messageListMap, true, true);
                }
                whoSend.setRecentMsg(messageBean.getMessage());
                whoSend.setRecentTime(messageBean.getSendTime());
            }
        }
        if (listener != null) {
            listener.freshChattedUserList(chattedContactList);
        }

        Log.d(TAG, "freshMessage: end");
    }

}
