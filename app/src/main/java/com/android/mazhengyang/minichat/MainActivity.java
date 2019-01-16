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

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ISocketCallback, IUserListCallback {

    private static final String TAG = "MiniChat." + MainActivity.class.getSimpleName();

    public static final int MESSAGE_FRESH_USERLIST = 1024;
    public static final int MESSAGE_FRESH_MESSAGE = 1025;
    public static final int MESSAGE_FRESH_CHAT_HISTORY = 1026;

    private static final int INDEX_CHAT_HISTORY = 0;
    private static final int INDEX_USERLIST = 1;
    private static final int INDEX_ME = 2;
    private static final int INDEX_CHATROOM = 3;
    private int indexPrevious;
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

    //在UdpThread中实现
    private List<UserBean> userList;
    private List<UserBean> chattedUserList;
    private Map<String, List<MessageBean>> messageListMap;

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
            switch (indexPrevious) {
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
                        chatHistoryFragment.setChatedUserList(chattedUserList);
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

            if (fragment == chatRoomFragment) {
                tabLayout.setVisibility(View.GONE);
            } else {
                tabLayout.setVisibility(View.VISIBLE);
            }

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
                indexPrevious = indexCurrent;
                indexCurrent = INDEX_CHATROOM;
            } else {
                udpThread.setChattingUser(null);
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
                    UserBean chattingUser = udpThread.getChattingUser();
                    if (chattingUser != null) {
                        List<MessageBean> list = messageListMap.get(chattingUser.getUserIp());
                        chatRoomFragment.freshMessageList(list);
                    }
                    break;
                case MESSAGE_FRESH_CHAT_HISTORY:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_CHAT_HISTORY");
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
     * @param listMap
     */
    @Override
    public void freshMessage(Map<String, List<MessageBean>> listMap) {
        //在UdpThread回调，必须放到主线程更新UI
        this.messageListMap = listMap;
        if (currentFragment == chatRoomFragment) {
            mainHandler.sendEmptyMessage(MESSAGE_FRESH_MESSAGE);
        }
    }

    /**
     * 刷新聊过天的用户列表
     *
     * @param list
     */
    @Override
    public void freshChattedUserList(List<UserBean> list) {
        this.chattedUserList = list;
        mainHandler.sendEmptyMessage(MESSAGE_FRESH_CHAT_HISTORY);
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
        if (messageListMap != null) {
            chatRoomFragment.setMessageBeanList(messageListMap.get(user.getUserIp()));
        }
        showFragment(chatRoomFragment);
        udpThread.setChattingUser(user);

        if (!user.getUserIp().equals(NetUtils.getLocalIpAddress())) {
            user.setUnReadMsgCount(0);
            updateUnReadIndicator();
        }
    }

    /**
     * 更新未读消息数
     */
    private void updateUnReadIndicator() {
        int totel = 0;
        if (chattedUserList != null) {
            for (UserBean userBean : chattedUserList) {
                totel += userBean.getUnReadMsgCount();
            }
        }
        badgeView.setBadgeCount(totel);
    }

}
