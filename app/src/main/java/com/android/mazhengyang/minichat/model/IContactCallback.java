package com.android.mazhengyang.minichat.model;

import com.android.mazhengyang.minichat.bean.ContactBean;

/**
 * Created by mazhengyang on 19-1-10.
 */

public interface IContactCallback {

    //ContactFragment, ChatHistoryFragment中調用，MainActivity中实现

    void onUserItemClick(ContactBean user);

}
