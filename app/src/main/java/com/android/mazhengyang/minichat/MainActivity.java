package com.android.mazhengyang.minichat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.android.mazhengyang.minichat.adapter.UserAdapter;
import com.android.mazhengyang.minichat.bean.User;
import com.android.mazhengyang.minichat.service.ChatService;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.SpinnerStyle;
import com.scwang.smartrefresh.layout.footer.BallPulseFooter;
import com.scwang.smartrefresh.layout.header.ClassicsHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ChatService.ChatServiceListener {

    private static final String TAG = "MiniChat." + MainActivity.class.getSimpleName();

    private static final int SHOW_MSG = 1024;
    private static final int UPDATA_USERMAP = 1025;

    private List<User> list = new ArrayList<>();

    private ChatService chatService;
    private Connection connection;

    private UserAdapter userAdapter;
    private BaseHandler baseHandler;

    @BindView(R.id.refreshLayout)
    RefreshLayout refreshLayout;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        baseHandler = new BaseHandler();

        initView();
        initService();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        if (chatService != null) {
            unbindService(connection);
        }
    }

    private void initView() {
        Log.d(TAG, "initView: ");

        refreshLayout.setRefreshHeader(new ClassicsHeader(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        userAdapter = new UserAdapter(this);
        recyclerView.setAdapter(userAdapter);
    }

    private void initService() {
        Log.d(TAG, "initService: ");
        Intent intent = new Intent(this, ChatService.class);
        connection = new Connection();
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void updateUserMap(Map<String, User> userMap) {
        if (chatService != null) {

            list.clear();

            Set<Map.Entry<String, User>> set = userMap.entrySet();

            for (Map.Entry<String, User> entry : set) {
                User user = entry.getValue();
                Log.d(TAG, "onReceive: add " + user.getUserName() + ", " + user.getIp());
                list.add(user);
            }

            Message message = new Message();
            message.what = UPDATA_USERMAP;
            baseHandler.sendMessage(message);
        }
    }

    private class BaseHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case SHOW_MSG:
                    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case UPDATA_USERMAP:
                    userAdapter.update(list);
                    break;
            }
        }
    }

    @Override
    public void showMsg(String msg) {
        Log.d(TAG, "showMsg: msg=" + msg);
        Message message = new Message();
        message.what = SHOW_MSG;
        message.obj = msg;
        baseHandler.sendMessage(message);
    }

    public class Connection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "onServiceConnected: ");
            chatService = ((ChatService.ChatServiceBinder) binder).getService();
            chatService.setChatServiceListener(MainActivity.this);
            chatService.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: ");
            chatService = null;
        }
    }

}
