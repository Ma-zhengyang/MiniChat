package com.android.mazhengyang.minichat.bean;

import android.util.Base64;
import android.util.Log;

import com.android.mazhengyang.minichat.util.Base64Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class MessageBean {

    private static final String TAG = "MiniChat." + MessageBean.class.getSimpleName();

    private String senderName; //消息发送者的名字
    private String senderIp; //发送者ip
    private String receiverIp; //接收者ip
    private String senderDeviceCode; //发送者imei码
    private String receiverDeviceCode; //接收者imei码
    private String msg; //信息内容
    private String sendTime; //发送时间 TODO, 这里显示的是本机时间，并不是对方的时间，可能错乱的
    private int type;//当前消息的类型
    private boolean readed = false;//消息是否已读

    public MessageBean() {
        Log.d(TAG, "MessageBean: ");
    }

    public MessageBean(JSONObject object) {
        try {
            senderName = object.getString("senderName");
            senderIp = object.getString("senderIp");
            receiverIp = object.getString("receiverIp");
            senderDeviceCode = object.getString("senderDeviceCode");
            receiverDeviceCode = object.getString("receiverDeviceCode");
            msg = Base64Util.encrypt(object.getString("msg"));
            type = object.getInt("type");
            sendTime = object.getString("sendTime");
            readed = object.getBoolean("readed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 采用JSONObject数据结构
     */
    public String toString() {
        try {
            JSONObject object = new JSONObject();
            object.put("senderName", senderName);
            object.put("senderIp", senderIp);
            object.put("receiverIp", receiverIp);
            object.put("senderDeviceCode", senderDeviceCode);
            object.put("receiverDeviceCode", receiverDeviceCode);
            object.put("msg", Base64Util.decrypt(msg));
            object.put("type", type);
            object.put("sendTime", sendTime);
            object.put("readed", readed);
            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }

    public String getReceiverIp() {
        return receiverIp;
    }

    public void setReceiverIp(String receiverIp) {
        this.receiverIp = receiverIp;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
        Log.d(TAG, "setSendTime: sendTime=" + sendTime);
    }

    public String getSenderDeviceCode() {
        return senderDeviceCode;
    }

    public void setSenderDeviceCode(String senderDeviceCode) {
        this.senderDeviceCode = senderDeviceCode;
    }

    public String getReceiverDeviceCode() {
        return receiverDeviceCode;
    }

    public void setReceiverDeviceCode(String receiverDeviceCode) {
        this.receiverDeviceCode = receiverDeviceCode;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isReaded() {
        return readed;
    }

    public void setReaded(boolean readed) {
        this.readed = readed;
    }
}
