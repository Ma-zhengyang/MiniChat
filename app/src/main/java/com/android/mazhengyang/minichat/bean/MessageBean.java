package com.android.mazhengyang.minichat.bean;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class MessageBean {

    private String senderName; //消息发送者的名字
    private String senderIp; //发送者ip
    private String receiverIp; //接收者ip
    private String msg; //信息内容
    private String sendTime; //发送时间
    private String deviceCode;//手机唯一识别号
    private int type;//当前消息的类型
    private boolean readed = false;//消息是否已读

    public MessageBean() {
        sendTime = System.currentTimeMillis() + "";//TODO, 这里显示的是本机时间，并不是对方的时间，可能错乱的
    }

    public MessageBean(JSONObject object) throws JSONException {
        senderName = new String(Base64.decode(object.getString("senderName").getBytes(), Base64.DEFAULT));
        senderIp = object.getString("senderIp");
        receiverIp = object.getString("receiverIp");
        deviceCode = object.getString("deviceCode");
        msg = new String(Base64.decode(object.getString("msg").getBytes(), Base64.DEFAULT));
        type = object.getInt("type");
        sendTime = object.getString("sendTime");
        readed = object.getBoolean("readed");
    }

    /**
     * 采用JSONObject数据结构
     */
    public String toString() {
        JSONObject object = new JSONObject();
        try {
            object.put("senderName", Base64.encodeToString(senderName.getBytes(), Base64.DEFAULT));
            object.put("senderIp", senderIp);
            object.put("receiverIp", receiverIp);
            object.put("deviceCode", deviceCode);
            object.put("msg", Base64.encodeToString(msg.getBytes(), Base64.DEFAULT));
            object.put("type", type);
            object.put("sendTime", System.currentTimeMillis() + "");
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
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
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
