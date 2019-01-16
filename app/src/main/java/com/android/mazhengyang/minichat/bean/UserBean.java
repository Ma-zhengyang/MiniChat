package com.android.mazhengyang.minichat.bean;

import java.io.Serializable;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class UserBean implements Serializable {

    private String userName; //用户名
    private String userIp; //ip地址
    private String deviceCode; //手机设备码
    private boolean isOnline; //是否在线
    private boolean isSelf; //是否是自己
    private int unReadMsgCount = 0; //未读消息数
    private String recentMsg; //最近一条消息
    private String recentTime; //最近一条消息时间

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean self) {
        isSelf = self;
    }

    public int getUnReadMsgCount() {
        return unReadMsgCount;
    }

    public void setUnReadMsgCount(int unreadMsgCount) {
        this.unReadMsgCount = unreadMsgCount;
    }

    public String getRecentMsg() {
        return recentMsg;
    }

    public void setRecentMsg(String recentMsg) {
        this.recentMsg = recentMsg;
    }

    public String getRecentTime() {
        return recentTime;
    }

    public void setRecentTime(String recentTime) {
        this.recentTime = recentTime;
    }
}
