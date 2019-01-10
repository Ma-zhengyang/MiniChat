package com.android.mazhengyang.minichat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.adapter.ChatHistoryAdapter;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-29.
 */

public class ChatHistoryFragment extends Fragment {

    private static final String TAG = "MiniChat." + ChatHistoryFragment.class.getSimpleName();

    private List<UserBean> chatedUserList;
    private Map<String, List<MessageBean>> messageBeanList;

    private ChatHistoryAdapter chatHistoryAdapter;

    @BindView(R.id.tv_head)
    TextView tvHead;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    public void setChatedUserList(List<UserBean> chatedUserList) {
        this.chatedUserList = chatedUserList;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_chat_history, null);
        ButterKnife.bind(this, view);

        tvHead.setText(R.string.tab_chat_history);

        Context context = getContext();

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        chatHistoryAdapter = new ChatHistoryAdapter(context, chatedUserList);
        recyclerView.setAdapter(chatHistoryAdapter);

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

}
