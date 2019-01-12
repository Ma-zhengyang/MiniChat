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
import com.android.mazhengyang.minichat.util.NetUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-17.
 */

public class ChatRoomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + ChatRoomAdapter.class.getSimpleName();

    private static final int TYPE_SELF = 0;
    private static final int TYPE_FRIEND = 1;

    private SimpleDateFormat format;
    private List<MessageBean> messageList;//保存聊天信息

    private Context context;

    public ChatRoomAdapter(Context context, List<MessageBean> list) {
        Log.d(TAG, "ChatRoomAdapter: ");
        this.context = context;
        this.messageList = list;
        format = new SimpleDateFormat("HH:mm", Locale.CHINA);
    }

    public void freshMessageList(List<MessageBean> list) {

        messageList = list;
        this.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: viewType=" + viewType);

        if (viewType == TYPE_SELF) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_send, parent, false);
            UserItemViewHolder vh = new UserItemViewHolder(v);
            return vh;
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_receive, parent, false);
            UserItemViewHolder vh = new UserItemViewHolder(v);
            return vh;
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (messageList == null) {
            Log.d(TAG, "onBindViewHolder: messageList is null");
            return;
        }

        MessageBean messageBean = messageList.get(position);
        ((UserItemViewHolder) holder).tvTime.setText(format.format(new Date(Long.valueOf(messageBean.getSendTime()))));
        ((UserItemViewHolder) holder).tvMessage.setText(messageBean.getMsg());

        String senderIp = messageBean.getSenderIp();
        String receiverIp = messageBean.getReceiverIp();

        Log.d(TAG, "onBindViewHolder: senderIp=" + senderIp);
        Log.d(TAG, "onBindViewHolder: receiverIp=" + receiverIp);

        if (senderIp.equals(NetUtils.getLocalIpAddress())) {
            ((UserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_self);
        } else {
            ((UserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_friend);
        }
    }

    @Override
    public int getItemCount() {
        if (messageList == null) {
            return 0;
        }
        int count = messageList.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        Log.d(TAG, "getItemViewType: ");

        MessageBean messageBean = messageList.get(position);

        if (messageBean.getSenderIp().equals(NetUtils.getLocalIpAddress())) {
            return TYPE_SELF;
        } else {
            return TYPE_FRIEND;
        }
    }

    public class UserItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tvTime)
        TextView tvTime;
        @BindView(R.id.tvMessage)
        TextView tvMessage;
        @BindView(R.id.ivUserIcon)
        ImageView ivUserIcon;

        public UserItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
