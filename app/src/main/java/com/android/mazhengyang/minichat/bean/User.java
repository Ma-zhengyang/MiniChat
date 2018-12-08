package com.android.mazhengyang.minichat.bean;

import java.io.Serializable;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class User implements Serializable {

    private String userName;    // 用户名
    private String userIp;      //ip地址
    private String deviceCode;//手机设备码
    private boolean isOnline;
    private boolean isSelf;

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
}
