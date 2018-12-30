package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.UdpThread;
import com.android.mazhengyang.minichat.bean.MessageBean;
import com.android.mazhengyang.minichat.util.Constant;
import com.android.mazhengyang.minichat.util.NetUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-17.
 */

public class ChatRoomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + ChatRoomAdapter.class.getSimpleName();

    private List<MessageBean> messageList = new ArrayList<>();//保存聊天信息

    private Context context;

    public ChatRoomAdapter(Context context, Map<String, Queue<MessageBean>> messagesMap) {
        Log.d(TAG, "ChatRoomAdapter: ");
        this.context = context;
//          this.messagesMap = messagesMap;
    }

    public void freshMessageMap(Map<String, Queue<MessageBean>> messagesMap) {

        Log.d(TAG, "freshMessageMap: ");

        String selfIp = NetUtils.getLocalIpAddress();

        Queue<MessageBean> queue = messagesMap.get(selfIp);
        if (queue != null) {
            Iterator<MessageBean> iterator = queue.iterator();
            while (iterator.hasNext()) {
                MessageBean message = iterator.next();
                switch (message.getType()) {
                    case UdpThread.MESSAGE_TO_TARGET:
                        messageList.add(message);
                        break;
                }
            }
            queue.clear();
            this.notifyDataSetChanged();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        ChatRoomAdapter.UserItemViewHolder vh = new ChatRoomAdapter.UserItemViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (messageList == null) {
            Log.d(TAG, "onBindViewHolder: messageList is null");
            return;
        }
        MessageBean messageBean = messageList.get(position);
        ((UserItemViewHolder) holder).tvMessage.setText(messageBean.getMsg());
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

    public class UserItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.tvMessage)
        TextView tvMessage;

        public UserItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void onClick(View v) {

        }
    }

}
