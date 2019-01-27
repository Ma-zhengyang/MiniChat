package com.android.mazhengyang.minichat.model;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.ContactBean;

import java.util.List;
import java.util.Map;

/**
 * Created by mazhengyang on 19-1-10.
 */

public interface ISocketCallback {

    //UdpThread中调用，MainActivity中实现

    //刷新联系人列表
    void freshContact(List<ContactBean> list);

    //刷新消息列表
    void freshMessage(Map<String, List<MessageBean>> listMap, boolean withVibrate, boolean withSound);

    //刷新聊过天的用户列表
    void freshChattedUserList(List<ContactBean> list);
}
