package com.android.mazhengyang.minichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;
import com.android.mazhengyang.minichat.fragment.ChatHistoryFragment;
import com.android.mazhengyang.minichat.fragment.ChatRoomFragment;
import com.android.mazhengyang.minichat.fragment.MeFragment;
import com.android.mazhengyang.minichat.fragment.UserListFragment;
import com.android.mazhengyang.minichat.model.ISocketCallback;
import com.android.mazhengyang.minichat.model.IUserListCallback;
import com.android.mazhengyang.minichat.util.NetUtils;
import com.android.mazhengyang.minichat.util.daynightmodeutils.ChangeModeController;
import com.android.mazhengyang.minichat.util.daynightmodeutils.ChangeModeHelper;
import com.android.mazhengyang.minichat.widget.BadgeView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ISocketCallback, IUserListCallback {

    private static final String TAG = "MiniChat." + MainActivity.class.getSimpleName();

    public static final int MESSAGE_FRESH_USERLIST = 1024;
    public static final int MESSAGE_FRESH_MESSAGE = 1025;
    public static final int MESSAGE_FRESH_MESSAGE_INDICATOR = 1026;

    private static final int INDEX_CHAT_HISTORY = 0;
    private static final int INDEX_USERLIST = 1;
    private static final int INDEX_ME = 2;
    private static final int INDEX_CHATROOM = 3;
    private int indexPervious;
    private int indexCurrent;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    private BadgeView badgeView;

    private MainHandler mainHandler = new MainHandler();
    private NetWorkStateReceiver netWorkStateReceiver;
    private UdpThread udpThread;
    private ChatHistoryFragment chatHistoryFragment;
    private UserListFragment userListFragment;
    private MeFragment meFragment;
    private ChatRoomFragment chatRoomFragment;
    private Fragment currentFragment;

    //用户列表
    private List<UserBean> userList;
    //正在和自己聊天的对方用户
    private UserBean currentChatingUser;
    //聊过天的用户列表
    private List<UserBean> chatedUserList = new ArrayList<>();
    //保存消息
    //String   对方ip地址
    //List<MessageBean>  和对方的聊天内容
    private Map<String, List<MessageBean>> messageList = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        ChangeModeController.getInstance().init(this, R.attr.class);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        NetUtils.resetLocalIpAddress();

        initView();

        if (ChangeModeHelper.getChangeMode(this) == ChangeModeHelper.MODE_NIGHT) {
            ChangeModeController.changeNight(this, R.style.NightTheme);
        }

        udpThread = UdpThread.getInstance();
        udpThread.setSocketCallback(this);
        udpThread.startWork();

        netWorkStateReceiver = new NetWorkStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, intentFilter);

        userListFragment = new UserListFragment();
        userListFragment.setUserListCallback(this);
        showFragment(userListFragment);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();

        boolean isWifiConnected = NetUtils.isWifiConnected(this);
        Log.d(TAG, "onResume: isWifiConnected=" + isWifiConnected);
        udpThread.setOnline(isWifiConnected);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();

        udpThread.release();
        mainHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(netWorkStateReceiver);
        ChangeModeController.onDestory();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        if (currentFragment == chatRoomFragment) {
            switch (indexPervious) {
                case INDEX_CHAT_HISTORY:
                    showFragment(chatHistoryFragment);
                    break;
                case INDEX_USERLIST:
                    showFragment(userListFragment);
                    break;
                case INDEX_ME:
                    showFragment(meFragment);
                    break;
            }
            return;
        }
        super.onBackPressed();
    }

    /**
     * 监听网络状态
     */
    private class NetWorkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                boolean isWifiConnected = NetUtils.isWifiConnected(MainActivity.this);
                Log.d(TAG, "onReceive: isWifiConnected=" + isWifiConnected);

                if (!isWifiConnected) {
                    Toast.makeText(MainActivity.this, R.string.wifi_unconnected, Toast.LENGTH_SHORT).show();
                }

                udpThread.setOnline(isWifiConnected);
            }
        }
    }

    /**
     * @param titleRes
     * @param withBadgeView
     * @return
     */
    private View makeTabView(int titleRes, boolean withBadgeView) {
        View tabView;
        if (withBadgeView) {
            tabView = LayoutInflater.from(this).inflate(R.layout.tab_item_with_badgeview, null);
            badgeView = tabView.findViewById(R.id.bvUnReadMsgCount);
        } else {
            tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null);
        }
        TextView title = tabView.findViewById(R.id.tv_title);
        title.setText(titleRes);

        return tabView;
    }

    /**
     * 初始化界面控件
     */
    private void initView() {

        TabLayout.Tab chatHistoryTab = tabLayout.newTab();
        View tabView = makeTabView(R.string.tab_chat_history, true);
        chatHistoryTab.setCustomView(tabView);
        tabLayout.addTab(chatHistoryTab, INDEX_CHAT_HISTORY);

        TabLayout.Tab userListTab = tabLayout.newTab();
        userListTab.setCustomView(makeTabView(R.string.tab_userlist, false));
        tabLayout.addTab(userListTab, INDEX_USERLIST);

        TabLayout.Tab selfTab = tabLayout.newTab();
        selfTab.setCustomView(makeTabView(R.string.tab_self, false));
        tabLayout.addTab(selfTab, INDEX_ME);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                View v = tab.getCustomView();
                TextView title = v.findViewById(R.id.tv_title);
                title.setTextColor(getColor(R.color.black));

                int position = tab.getPosition();
                Log.d(TAG, "onTabSelected: position=" + position);

                switch (position) {
                    case INDEX_CHAT_HISTORY:
                        if (chatHistoryFragment == null) {
                            chatHistoryFragment = new ChatHistoryFragment();
                            chatHistoryFragment.setUserListCallback(MainActivity.this);
                        }
                        chatHistoryFragment.setChatedUserList(chatedUserList);
                        showFragment(chatHistoryFragment);
                        break;
                    case INDEX_USERLIST:
                        showFragment(userListFragment);
                        break;
                    case INDEX_ME:
                        if (meFragment == null) {
                            meFragment = new MeFragment();
                        }
                        showFragment(meFragment);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View v = tab.getCustomView();
                TextView title = v.findViewById(R.id.tv_title);
                title.setTextColor(getColor(R.color.deepGray));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    /**
     * 显示相应界面
     *
     * @param fragment
     */
    private void showFragment(Fragment fragment) {

        if (fragment != null && fragment != currentFragment) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_content, fragment)
                    .show(fragment)
                    .commit();
            currentFragment = fragment;

            if (fragment == chatHistoryFragment) {
                tabLayout.getTabAt(INDEX_CHAT_HISTORY).select();
                indexCurrent = INDEX_CHAT_HISTORY;
            } else if (fragment == userListFragment) {
                tabLayout.getTabAt(INDEX_USERLIST).select();
                indexCurrent = INDEX_USERLIST;
            } else if (fragment == meFragment) {
                tabLayout.getTabAt(INDEX_ME).select();
                indexCurrent = INDEX_ME;
            }

            if (fragment == chatRoomFragment) {
                tabLayout.setVisibility(View.GONE);
                indexPervious = indexCurrent;
                indexCurrent = INDEX_CHATROOM;
            } else {
                tabLayout.setVisibility(View.VISIBLE);
            }

            if (currentFragment != chatRoomFragment) {
                currentChatingUser = null;
            }

        }
    }

    /**
     * 主线程更新UdpThread中的资源回调
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_FRESH_USERLIST:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_USERLIST");

                    if (userListFragment != null) {
                        userListFragment.freshUserList(userList);
                    }
                    break;
                case MESSAGE_FRESH_MESSAGE:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_MESSAGE");

                    List<MessageBean> list = messageList.get(currentChatingUser.getUserIp());
                    chatRoomFragment.freshMessageList(list);
                    break;
                case MESSAGE_FRESH_MESSAGE_INDICATOR:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_MESSAGE_INDICATOR");

                    updateUnReadIndicator();
                    if (chatHistoryFragment != null) {
                        chatHistoryFragment.updateChatedUserList();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 刷新用户列表
     *
     * @param userList
     */
    @Override
    public void freshUserList(List<UserBean> userList) {
        this.userList = userList;
        mainHandler.sendEmptyMessage(MESSAGE_FRESH_USERLIST);
    }

    /**
     * 刷新消息
     *
     * @param messageBean
     */
    @Override
    public void freshMessage(MessageBean messageBean) {

        String senderIp = messageBean.getSenderIp();
        String receiverIp = messageBean.getReceiverIp();
        String selfIp = NetUtils.getLocalIpAddress();

        UserBean whoSend = null;//这条消息是谁发来的
        UserBean whoReceiver = null;//这条消息是谁接收的的
        for (UserBean userBean : userList) {
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
        String key;
        if (senderIp.equals(selfIp)) {//我发给对方的,key是receiverIp
            key = receiverIp;
        } else {//对方发给我的,key是senderIp
            key = senderIp;
        }
        if (messageList.containsKey(key)) {
            List<MessageBean> list = messageList.get(key);
            list.add(messageBean);
        } else {
            List<MessageBean> list = new ArrayList<>();
            list.add(messageBean);
            messageList.put(key, list);
        }

        if (currentFragment == chatRoomFragment) {//正在聊天界面

            Log.d(TAG, "freshMessage: current is in chatRoomFragment");

            if (currentChatingUser.getUserIp().equals(selfIp)) { //当前聊天界面用户对象就是自己
                Log.d(TAG, "freshMessage: current chat view is self");
                if (senderIp.equals(receiverIp)) {//自己和自己发消息，无需处理
                    Log.d(TAG, "freshMessage: message send by self");
                    messageBean.setReaded(true);
                } else {//别人消息进来
                    Log.d(TAG, "freshMessage: message send by other");
                    int unReadMsgCount = whoSend.getUnReadMsgCount();
                    whoSend.setUnReadMsgCount(++unReadMsgCount);
                }
                if (!chatedUserList.contains(whoSend)) {
                    chatedUserList.add(whoSend);
                }
                whoSend.setRecentMsg(messageBean.getMsg());
            } else {//当前正在和朋友聊天
                String ip = currentChatingUser.getUserIp();
                //消息是当前正在聊天的朋友发的，不需处理
                if (ip.equals(senderIp)) {//对方发消息时
                    Log.d(TAG, "freshMessage: 11111");
                    messageBean.setReaded(true);

                    if (!chatedUserList.contains(whoSend)) {
                        chatedUserList.add(whoSend);
                    }
                    whoSend.setRecentMsg(messageBean.getMsg());
                } else if (ip.equals(receiverIp)) {//自己发消息时
                    Log.d(TAG, "freshMessage: 22222");
                    messageBean.setReaded(true);

                    if (!chatedUserList.contains(whoReceiver)) {
                        chatedUserList.add(whoReceiver);
                    }
                    whoReceiver.setRecentMsg(messageBean.getMsg());
                } else {
                    //第三方朋友消息进来
                    Log.d(TAG, "freshMessage: 33333");
                    if (whoSend != null) {
                        int unReadMsgCount = whoSend.getUnReadMsgCount();
                        whoSend.setUnReadMsgCount(++unReadMsgCount);
                        if (!chatedUserList.contains(whoSend)) {
                            chatedUserList.add(whoSend);
                        }
                        whoSend.setRecentMsg(messageBean.getMsg());
                    }
                }
            }

        } else {//当前不在聊天界面

            Log.d(TAG, "freshMessage: current is not in chatRoomFragment");

            if (whoSend != null) {
                int unReadMsgCount = whoSend.getUnReadMsgCount();
                whoSend.setUnReadMsgCount(++unReadMsgCount);
                if (!chatedUserList.contains(whoSend)) {
                    chatedUserList.add(whoSend);
                }
                whoSend.setRecentMsg(messageBean.getMsg());
            }
        }

        //在UdpThread回调，必须放到主线程更新UI
        if (currentFragment == chatRoomFragment) {
            mainHandler.sendEmptyMessage(MESSAGE_FRESH_MESSAGE);
        }
        mainHandler.sendEmptyMessage(MESSAGE_FRESH_MESSAGE_INDICATOR);
    }

    /**
     * @param user
     */
    @Override
    public void onUserItemClick(UserBean user) {

        if (chatRoomFragment == null) {
            chatRoomFragment = new ChatRoomFragment();
        }

        chatRoomFragment.setUserBean(user);
        List<MessageBean> list = messageList.get(user.getUserIp());
        chatRoomFragment.setMessageBeanList(list);
        showFragment(chatRoomFragment);
        currentChatingUser = user;

        if (!user.getUserIp().equals(NetUtils.getLocalIpAddress())) {
            user.setUnReadMsgCount(0);
            updateUnReadIndicator();
        }
    }

    private void updateUnReadIndicator() {
        int totleUnReadCount = 0;
        for (UserBean userBean : chatedUserList) {
            totleUnReadCount += userBean.getUnReadMsgCount();
        }
        badgeView.setBadgeCount(totleUnReadCount);
    }

}
