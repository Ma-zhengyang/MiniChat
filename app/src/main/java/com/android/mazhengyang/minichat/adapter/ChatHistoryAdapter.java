package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.bean.UserBean;
import com.android.mazhengyang.minichat.util.NetUtils;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-29.
 */

public class ChatHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + ChatHistoryAdapter.class.getSimpleName();

    private Context context;
    private List<UserBean> chatedUserList;

    public ChatHistoryAdapter(Context context, List<UserBean> chatedUserList) {
        Log.d(TAG, "ChatHistoryAdapter: ");
        this.context = context;
        this.chatedUserList = chatedUserList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_chat_history, parent, false);
        ChatedUserItemViewHolder vh = new ChatedUserItemViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (chatedUserList == null) {
            Log.d(TAG, "onBindViewHolder: chatedUserList is null");
            return;
        }

        UserBean userBean = chatedUserList.get(position);
        ((ChatedUserItemViewHolder) holder).tvUserName.setText(userBean.getUserName());
//        ((ChatedUserItemViewHolder) holder).tvMessage.setText(messageBean.getMsg());

        String senderIp = userBean.getUserIp();
        Log.d(TAG, "onBindViewHolder: senderIp=" + senderIp);

        if (senderIp.equals(NetUtils.getLocalIpAddress())) {
            ((ChatedUserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_self);
        } else {
            ((ChatedUserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_friend);
        }

    }

    @Override
    public int getItemCount() {
        if (chatedUserList == null) {
            Log.d(TAG, "getItemCount: chatedUserList is null");
            return 0;
        }
        int count = chatedUserList.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public class ChatedUserItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.ivUserIcon)
        ImageView ivUserIcon;
        @BindView(R.id.tvUserName)
        TextView tvUserName;
        @BindView(R.id.tvMessage)
        TextView tvMessage;

        public ChatedUserItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
