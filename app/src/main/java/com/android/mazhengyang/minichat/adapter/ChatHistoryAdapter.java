package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.android.mazhengyang.minichat.bean.UserBean;

import java.util.List;

/**
 * Created by mazhengyang on 18-12-29.
 */

public class ChatHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + ChatHistoryAdapter.class.getSimpleName();

    private List<UserBean> userList;

    public ChatHistoryAdapter(Context context, List<UserBean> list) {

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public void fresh(List<UserBean> userList) {
        this.userList = userList;
        this.notifyDataSetChanged();
    }
}
