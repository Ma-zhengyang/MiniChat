package com.android.mazhengyang.minichat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.UdpThread;
import com.android.mazhengyang.minichat.adapter.ChatRoomAdapter;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-17.
 */

public class ChatRoomFragment extends Fragment {

    private static final String TAG = "MiniChat." + ChatRoomFragment.class.getSimpleName();

    private UserBean userBean;
    private Map<String, Queue<MessageBean>> messagesMap;
    private ChatRoomAdapter chatRoomAdapter;

    @BindView(R.id.tv_head)
    TextView tvHead;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.chatroom_edittext)
    EditText editText;
    @BindView(R.id.chatroom_sendbtn)
    Button sendBtn;

    public void setUserBean(UserBean userBean) {
        Log.d(TAG, "setUserBean: ");
        this.userBean = userBean;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_chatroom, null);
        ButterKnife.bind(this, view);

        tvHead.setText(userBean.getUserName());

        Context context = getContext();

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        chatRoomAdapter = new ChatRoomAdapter(context, messagesMap);
        recyclerView.setAdapter(chatRoomAdapter);

        sendBtn.setOnClickListener(send);
        sendBtn.setEnabled(false);
        editText.addTextChangedListener(textWatcher);

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: ");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString() != null && s.toString().length() > 0) {
                sendBtn.setEnabled(true);
            } else {
                sendBtn.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    View.OnClickListener send = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String receiverIp = userBean.getUserIp();
            String message = editText.getText().toString().trim();

            Log.d(TAG, "onClick: receiverIp=" + receiverIp);
            Log.d(TAG, "onClick: message=" + message);

            if (!"".equals(message)) {
                UdpThread udpThread = UdpThread.getInstance();
                MessageBean messageBean = udpThread.packUdpMessage(receiverIp, message, UdpThread.MESSAGE_TO_TARGET);
                udpThread.send(messageBean);
                editText.setText(null);
            }
        }
    };

    public void fresh(Map<String, Queue<MessageBean>> messagesMap) {
        this.messagesMap = messagesMap;
        chatRoomAdapter.freshMessageMap(messagesMap);
    }

}
