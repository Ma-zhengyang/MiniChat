package com.android.mazhengyang.minichat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.adapter.UserListAdapter;
import com.android.mazhengyang.minichat.bean.UserBean;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-6.
 */

public class UserListFragment extends Fragment {

    private static final String TAG = "MiniChat." + UserListFragment.class.getSimpleName();

    private List<UserBean> userList;
    private UserListAdapter userListAdapter;

    @BindView(R.id.tv_head)
    TextView tvHead;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_userlist, null);
        ButterKnife.bind(this, view);

        tvHead.setText(R.string.nav_userlist);

        Context context = getContext();

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        userListAdapter = new UserListAdapter(context, userList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(userListAdapter);

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

    @Override
    public void setArguments(Bundle args) {
        Log.d(TAG, "setArguments: ");
        super.setArguments(args);
    }

    public void fresh(List<UserBean> list) {
        this.userList = list;
        userListAdapter.freshUserList(list);
    }

}
