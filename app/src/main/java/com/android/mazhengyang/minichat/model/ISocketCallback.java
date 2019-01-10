package com.android.mazhengyang.minichat.model;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;

import java.util.List;

/**
 * Created by mazhengyang on 19-1-10.
 */

public interface ISocketCallback {

    //UdpThread中调用，MainActivity中实现


    //刷新用户列表
    void freshUserList(List<UserBean> userList);

    //刷新消息
    void freshMessage(MessageBean messageBean);

}
