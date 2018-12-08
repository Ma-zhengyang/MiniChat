package com.android.mazhengyang.minichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.mazhengyang.minichat.bean.User;
import com.android.mazhengyang.minichat.fragment.UserListFragment;
import com.android.mazhengyang.minichat.util.Utils;

import java.util.List;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements UdpThread.Callback {

    private static final String TAG = "MiniChat." + MainActivity.class.getSimpleName();

    public static final int MESSAGE_UPDATE_USER_LIST = 1024;

    private NetWorkStateReceiver netWorkStateReceiver;

    private Handler handler = new MainHandler();
    private UdpThread udpThread;

    private UserListFragment userListFragment;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        udpThread = UdpThread.getInstance(this);

        netWorkStateReceiver = new NetWorkStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netWorkStateReceiver, intentFilter);

        userListFragment = new UserListFragment();
        showFragment(userListFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean isWifiConnected = Utils.isWifiConnected(this);
        Log.d(TAG, "onResume: isWifiConnected=" + isWifiConnected);
        if (isWifiConnected) {
            udpThread.startRun();
        } else {
            Toast.makeText(this, R.string.wifi_unconnected, Toast.LENGTH_SHORT).show();
        }
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
        handler.removeCallbacksAndMessages(null);
    }

    private void showFragment(Fragment fragment) {

        if (fragment != null && fragment != currentFragment) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_main, fragment)
                    .show(fragment)
                    .commit();
            currentFragment = fragment;
        }
    }

    @Override
    public void updateUserMap(List<User> userList) {
        Message message = new Message();
        message.what = MESSAGE_UPDATE_USER_LIST;
        message.obj = userList;
        handler.sendMessage(message);
    }

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handleMessage: msg.what=" + msg.what);
            switch (msg.what) {
                case MESSAGE_UPDATE_USER_LIST:
                    if (currentFragment == userListFragment) {
                        userListFragment.updateUserMap((List<User>) msg.obj);
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
                boolean isWifiConnected = Utils.isWifiConnected(MainActivity.this);
                Log.d(TAG, "onReceive: isWifiConnected=" + isWifiConnected);
                if (isWifiConnected) {
                    udpThread.startRun();
                } else {
                    udpThread.stopRun();
                }
            }
        }
    }

}
