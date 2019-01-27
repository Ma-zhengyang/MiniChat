package com.android.mazhengyang.minichat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.fragment.ChatFragment;
import com.android.mazhengyang.minichat.fragment.ChatRoomFragment;
import com.android.mazhengyang.minichat.fragment.ContactFragment;
import com.android.mazhengyang.minichat.fragment.SettingFragment;
import com.android.mazhengyang.minichat.model.IContactCallback;
import com.android.mazhengyang.minichat.model.ISocketCallback;
import com.android.mazhengyang.minichat.saver.MessageSaver;
import com.android.mazhengyang.minichat.util.DayNightController;
import com.android.mazhengyang.minichat.util.NetUtils;
import com.android.mazhengyang.minichat.util.SharedPreferencesHelper;
import com.android.mazhengyang.minichat.util.SoundController;
import com.android.mazhengyang.minichat.util.VibrateController;
import com.android.mazhengyang.minichat.widget.BadgeView;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ISocketCallback, IContactCallback {

    private static final String TAG = "MiniChat." + MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 1024;

    public static final int MESSAGE_FRESH_CONTACT = 1024;
    public static final int MESSAGE_FRESH_MESSAGE = 1025;
    public static final int MESSAGE_FRESH_CHATTED = 1026;

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
    private MessageSaver messageSaver;

    private ChatFragment chatHistoryFragment;
    private ContactFragment userListFragment;
    private SettingFragment settingFragment;
    private ChatRoomFragment chatRoomFragment;
    private Fragment currentFragment;

    //在UdpThread中实现
    private List<ContactBean> contactList;
    private List<ContactBean> chattedContactList;
    private Map<String, List<MessageBean>> messageListMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        DayNightController.getInstance().init(this, R.attr.class);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        NetUtils.resetLocalIpAddress();

        initView();
        initVibrateSound();

        if (SharedPreferencesHelper.getDayNightMode(this)
                == SharedPreferencesHelper.MODE_NIGHT) {
            DayNightController.changeNight(this, R.style.NightTheme);
        }

        udpThread = UdpThread.getInstance();
        udpThread.setSocketCallback(this);
        messageSaver = new MessageSaver();

        userListFragment = new ContactFragment();
        userListFragment.setContactCallback(this);
        showFragment(userListFragment);

        if (!hasPhonePermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSIONS_REQUEST_READ_PHONE_STATE);
        } else {
            udpThread.startWork(this);
            registerReceiver();
        }

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
        if (netWorkStateReceiver != null) {
            unregisterReceiver(netWorkStateReceiver);
        }
        DayNightController.onDestory();
    }

    private void registerReceiver() {
        Log.d(TAG, "registerReceiver: ");
        netWorkStateReceiver = new NetWorkStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, intentFilter);
    }

    private boolean hasPhonePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            Log.d(TAG, "hasPhonePermission: no READ_PHONE_STATE permission");
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: PERMISSION_GRANTED grant success");
                    udpThread.startWork(this);
                    registerReceiver();
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().
                            getString(R.string.no_permission), Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
            default:
                break;
        }

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
                    showFragment(settingFragment);
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
     * @param iconRes
     * @param titleRes
     * @param withBadgeView
     * @return
     */
    private View makeTabView(int iconRes, int titleRes, boolean withBadgeView) {
        View tabView;
        if (withBadgeView) {
            tabView = LayoutInflater.from(this).inflate(R.layout.tab_item_with_badgeview, null);
            badgeView = tabView.findViewById(R.id.bvUnReadMsgCount);
        } else {
            tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null);
        }
        ImageView icon = tabView.findViewById(R.id.iv_icon);
        icon.setImageResource(iconRes);
        TextView title = tabView.findViewById(R.id.tv_title);
        title.setText(titleRes);

        return tabView;
    }

    /**
     * 初始化界面控件
     */
    private void initView() {

        TabLayout.Tab chatHistoryTab = tabLayout.newTab();
        View tabView = makeTabView(R.drawable.tab_chat_bg, R.string.tab_chat_history, true);
        chatHistoryTab.setCustomView(tabView);
        tabLayout.addTab(chatHistoryTab, INDEX_CHAT_HISTORY);

        TabLayout.Tab userListTab = tabLayout.newTab();
        userListTab.setCustomView(makeTabView(R.drawable.tab_contact_list, R.string.tab_userlist, false));
        tabLayout.addTab(userListTab, INDEX_USERLIST);

        TabLayout.Tab selfTab = tabLayout.newTab();
        selfTab.setCustomView(makeTabView(R.drawable.tab_profile, R.string.tab_self, false));
        tabLayout.addTab(selfTab, INDEX_ME);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                View v = tab.getCustomView();
                TextView title = v.findViewById(R.id.tv_title);
                title.setTextColor(getColor(R.color.green));

                int position = tab.getPosition();
                Log.d(TAG, "onTabSelected: position=" + position);

                switch (position) {
                    case INDEX_CHAT_HISTORY:
                        if (chatHistoryFragment == null) {
                            chatHistoryFragment = new ChatFragment();
                            chatHistoryFragment.setUserListCallback(MainActivity.this);
                        }
                        chatHistoryFragment.setChattedUserList(chattedContactList);
                        showFragment(chatHistoryFragment);
                        break;
                    case INDEX_USERLIST:
                        showFragment(userListFragment);
                        break;
                    case INDEX_ME:
                        if (settingFragment == null) {
                            settingFragment = new SettingFragment();
                        }
                        showFragment(settingFragment);
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
            } else if (fragment == settingFragment) {
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

    private void initVibrateSound() {

        if (SharedPreferencesHelper.getVibrateMode(this)
                == SharedPreferencesHelper.MODE_VIBRATE_ON) {
            VibrateController.setEnable(true);
        } else {
            VibrateController.setEnable(false);
        }
        if (SharedPreferencesHelper.getSoundMode(this)
                == SharedPreferencesHelper.MODE_SOUND_ON) {
            SoundController.setEnable(true);
        } else {
            SoundController.setEnable(false);
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
                case MESSAGE_FRESH_CONTACT:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_CONTACT");
                    if (userListFragment != null) {
                        userListFragment.freshContact(contactList);
                    }
                    break;
                case MESSAGE_FRESH_MESSAGE:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_MESSAGE");
                    if (currentFragment == chatRoomFragment) {
                        ContactBean chattingUser = udpThread.getChattingUser();
                        if (chattingUser != null) {
                            List<MessageBean> list = messageListMap.get(chattingUser.getDeviceCode());
                            chatRoomFragment.freshMessageList(list);
                        }
                    }
                    break;
                case MESSAGE_FRESH_CHATTED:
                    Log.d(TAG, "handleMessage: MESSAGE_FRESH_CHATTED");
                    updateUnReadIndicator();
                    if (chatHistoryFragment != null) {
                        chatHistoryFragment.updateChattedUserList(chattedContactList);
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
     * @param contactList
     */
    @Override
    public void freshContact(List<ContactBean> contactList) {
        this.contactList = contactList;
        mainHandler.sendEmptyMessage(MESSAGE_FRESH_CONTACT);
    }

    /**
     * 刷新消息
     *
     * @param listMap
     */
    @Override
    public void freshMessage(Map<String, List<MessageBean>> listMap, boolean withVibrate, boolean withSound) {
        //在UdpThread回调，必须放到主线程更新UI

        if (withVibrate) {
            VibrateController.vibrate(this);
        }
        if (withSound) {
            SoundController.play(this);
        }

        this.messageListMap = listMap;
        mainHandler.sendEmptyMessage(MESSAGE_FRESH_MESSAGE);
    }

    /**
     * 刷新聊过天的用户列表
     *
     * @param list
     */
    @Override
    public void freshChattedUserList(List<ContactBean> list) {
        this.chattedContactList = list;
        mainHandler.sendEmptyMessage(MESSAGE_FRESH_CHATTED);
    }

    /**
     * @param user
     */
    @Override
    public void onUserItemClick(ContactBean user) {

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
        int total = 0;
        if (chattedContactList != null) {
            for (ContactBean userBean : chattedContactList) {
                total += userBean.getUnReadMsgCount();
            }
        }
        badgeView.setBadgeCount(total);
    }

}
