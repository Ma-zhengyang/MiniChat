package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.User;
import com.android.mazhengyang.minichat.util.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + UserAdapter.class.getSimpleName();

    private List<User> list = new ArrayList<>();

    private Context context;

    public UserAdapter(Context context) {
        super();
        this.context = context;
    }

    public void updateUserMap(List<User> list) {
        this.list = list;
        this.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child, parent, false);
        UserItemViewHolder vh = new UserItemViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        User user = list.get(position);
        if (user != null) {
            String name = user.getUserName();
            String ip = user.getUserIp();

            if (user.isOnline()) {
                ((UserItemViewHolder) holder).tvUserName.setTextColor(Color.BLACK);
            } else {
                ((UserItemViewHolder) holder).tvUserName.setTextColor(Color.GRAY);
            }

            if (ip.equals(Utils.getLocalIpAddress())) {
                ((UserItemViewHolder) holder).tvUserName.setText(name + "(自己)");
            } else {
                ((UserItemViewHolder) holder).tvUserName.setText(name);
            }
            ((UserItemViewHolder) holder).tvUserIp.setText(ip);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class UserItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.tvUserName)
        TextView tvUserName;
        @BindView(R.id.tvUserIp)
        TextView tvUserIp;

        public UserItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

        }
    }
}
