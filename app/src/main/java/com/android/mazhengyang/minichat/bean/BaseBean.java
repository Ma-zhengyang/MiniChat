package com.android.mazhengyang.minichat.bean;

/**
 * Created by mazhengyang on 19-3-4.
 */

public class BaseBean {

    private String userName; //用户名
    private String namePinyin; //名字拼音
    private String sortLetter; //首字母，用于排序

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNamePinyin() {
        return namePinyin;
    }

    public void setNamePinyin(String namePinyin) {
        this.namePinyin = namePinyin;
    }


    public String getSortLetter() {
        return sortLetter;
    }

    public void setSortLetter(String sortLetter) {
        this.sortLetter = sortLetter;
    }

}
