package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.model.IContactCallback;
import com.android.mazhengyang.minichat.util.NetUtils;
import com.android.mazhengyang.minichat.widget.BadgeView;
import com.daimajia.swipe.SwipeLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-29.
 */

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + ChatAdapter.class.getSimpleName();

    private Context context;
    private List<ContactBean> chattedContactList;

    private IContactCallback userListCallback;

    public void setUserListCallback(IContactCallback userListCallback) {
        Log.d(TAG, "setUserListCallback: userListCallback=" + userListCallback);
        this.userListCallback = userListCallback;
    }

    public void updateChatedUserList(List<ContactBean> chattedContactList) {
        Log.d(TAG, "updateChatedUserList: userListCallback=" + userListCallback);
        this.chattedContactList = chattedContactList;
        notifyDataSetChanged();
    }

    public ChatAdapter(Context context, List<ContactBean> chattedContactList) {
        Log.d(TAG, "ChatAdapter: ");
        this.context = context;
        this.chattedContactList = chattedContactList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        ChatedUserItemViewHolder vh = new ChatedUserItemViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (chattedContactList == null) {
            Log.d(TAG, "onBindViewHolder: chattedContactList is null");
            return;
        }

        ContactBean contactBean = chattedContactList.get(position);

        if (contactBean.getDeviceCode()
                .equals(NetUtils.getDeviceCode(context))) {
            ((ChatedUserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_self);
        } else {
            ((ChatedUserItemViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_friend);
        }

        ((ChatedUserItemViewHolder) holder).bvUnReadMsgCount.setBadgeCount(contactBean.getUnReadMsgCount());
        ((ChatedUserItemViewHolder) holder).tvUserName.setText(contactBean.getUserName());
        ((ChatedUserItemViewHolder) holder).tvRecentMessage.setText(contactBean.getRecentMsg());
        ((ChatedUserItemViewHolder) holder).tvRecentTime.setText(contactBean.getRecentTime());

    }

    @Override
    public int getItemCount() {
        if (chattedContactList == null) {
            Log.d(TAG, "getItemCount: chattedContactList is null");
            return 0;
        }
        int count = chattedContactList.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public class ChatedUserItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.swipelayout)
        SwipeLayout swipeLayout;
        @BindView(R.id.ivUserIcon)
        ImageView ivUserIcon;
        @BindView(R.id.bvUnReadMsgCount)
        BadgeView bvUnReadMsgCount;
        @BindView(R.id.tvUserName)
        TextView tvUserName;
        @BindView(R.id.tvRecentMessage)
        TextView tvRecentMessage;
        @BindView(R.id.tvRecentTime)
        TextView tvRecentTime;

        @BindView(R.id.delete)
        ImageView deleteBtn;

        public ChatedUserItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);

            swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
            swipeLayout.addDrag(SwipeLayout.DragEdge.Right, swipeLayout.findViewWithTag("controlView"));
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "delete click", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClick(View v) {
            if (userListCallback != null) {
                userListCallback.onUserItemClick(chattedContactList.get(getPosition()));
            }
        }
    }
}
