package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mazhengyang.minichat.MainActivity;
import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.UserBean;
import com.android.mazhengyang.minichat.util.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class UserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + UserListAdapter.class.getSimpleName();

    private List<UserBean> list;

    private Context context;

    public UserListAdapter(Context context, List<UserBean> list) {
        Log.d(TAG, "UserListAdapter: ");
        this.context = context;
        this.list = list;
    }

    public void freshUserList(List<UserBean> list) {
        Log.d(TAG, "freshUserList: ");
        this.list = list;
        this.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        UserItemViewHolder vh = new UserItemViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (list == null) {
            return;
        }

        UserBean user = list.get(position);
        if (user != null) {
            String name = user.getUserName();
            String ip = user.getUserIp();

            if (user.isOnline()) {
                ((UserItemViewHolder) holder).tvUserName.setTextColor(Color.BLACK);
            } else {
                ((UserItemViewHolder) holder).tvUserName.setTextColor(Color.GRAY);
            }

            if (ip.equals(Utils.getLocalIpAddress())) {
                ((UserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_self);
                ((UserItemViewHolder) holder).tvUserName.setText(name);
            } else {
                ((UserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_friend);
                ((UserItemViewHolder) holder).tvUserName.setText(name);
            }
            ((UserItemViewHolder) holder).tvUserIp.setText(ip);
        }
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        Log.d(TAG, "getItemCount: count=" + list.size());
        return list.size();
    }

    public class UserItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.ivUserIcon)
        ImageView ivUserIcon;
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
            ((MainActivity) context).onUserItemClick(list.get(this.getPosition()));
        }
    }
}
