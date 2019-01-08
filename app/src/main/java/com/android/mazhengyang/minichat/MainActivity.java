package com.android.mazhengyang.minichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;
import com.android.mazhengyang.minichat.fragment.ChatHistoryFragment;
import com.android.mazhengyang.minichat.fragment.ChatRoomFragment;
import com.android.mazhengyang.minichat.fragment.MeFragment;
import com.android.mazhengyang.minichat.fragment.UserListFragment;
import com.android.mazhengyang.minichat.util.Constant;
import com.android.mazhengyang.minichat.util.NetUtils;
import com.android.mazhengyang.minichat.util.daynightmodeutils.ChangeModeController;
import com.android.mazhengyang.minichat.util.daynightmodeutils.ChangeModeHelper;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MiniChat." + MainActivity.class.getSimpleName();

    @BindView(R.id.bottom_nav)
    BottomNavigationView bottomNavigationView;

    private NetWorkStateReceiver netWorkStateReceiver;
    private UdpThread udpThread;
    private UserListFragment userListFragment;
    private ChatHistoryFragment chatHistoryFragment;
    private ChatRoomFragment chatRoomFragment;
    private MeFragment meFragment;
    private Fragment currentFragment;

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
        udpThread.start(new MainHandler());

        netWorkStateReceiver = new NetWorkStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, intentFilter);

        userListFragment = new UserListFragment();
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

        unregisterReceiver(netWorkStateReceiver);
        ChangeModeController.onDestory();
    }

    @Override
    public void onBackPressed() {
        if (currentFragment != userListFragment) {
            showFragment(userListFragment);
            return;
        }
        super.onBackPressed();
    }

    private void initView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int id = item.getItemId();
                        switch (id) {
                            case R.id.nav_id_chat_history:
                                if (chatHistoryFragment == null) {
                                    chatHistoryFragment = new ChatHistoryFragment();
                                }
                                showFragment(chatHistoryFragment);
                                break;
                            case R.id.nav_id_userlist:
                                if (userListFragment == null) {
                                    userListFragment = new UserListFragment();
                                }
                                showFragment(userListFragment);
                                break;
                            case R.id.nav_id_self:
                                if (meFragment == null) {
                                    meFragment = new MeFragment();
                                }
                                showFragment(meFragment);
                                break;
                        }
                        return true;
                    }
                });
    }

    private void showFragment(Fragment fragment) {

        if (fragment != null && fragment != currentFragment) {

            if (fragment == chatRoomFragment) {
                bottomNavigationView.setVisibility(View.GONE);
            } else {
                bottomNavigationView.setVisibility(View.VISIBLE);
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_content, fragment)
                    .show(fragment)
                    .commit();
            currentFragment = fragment;
        }
    }

    public void onUserItemClick(UserBean user) {

        if (chatRoomFragment == null) {
            chatRoomFragment = new ChatRoomFragment();
        }
        chatRoomFragment.setUserBean(user);
        showFragment(chatRoomFragment);
    }

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handleMessage: msg.what=" + msg.what);
            switch (msg.what) {
                case Constant.MESSAGE_FRESH_USERLIST:
                    if (userListFragment != null) {
                        userListFragment.fresh((List<UserBean>) msg.obj);
                    }
                    break;
                case Constant.MESSAGE_FRESH_MESSAGE:
                    if (currentFragment == userListFragment) {

                    } else if (currentFragment == chatHistoryFragment) {

                    } else {
                        chatRoomFragment.fresh((Map<String, Queue<MessageBean>>) msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    }

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
                } /*else {
                    Toast.makeText(MainActivity.this, R.string.wifi_connected, Toast.LENGTH_SHORT).show();
                }*/

                udpThread.setOnline(isWifiConnected);
            }
        }
    }
}
