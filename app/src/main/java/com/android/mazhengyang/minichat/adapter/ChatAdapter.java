package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.model.IContactCallback;
import com.android.mazhengyang.minichat.util.NetUtils;
import com.android.mazhengyang.minichat.widget.BadgeView;
import com.android.mazhengyang.minichat.widget.SwipeMenuLayout;

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
    private IContactCallback contactListCallback;

    public void setContactListCallback(IContactCallback contactListCallback) {
        Log.d(TAG, "setContactListCallback: setContactListCallback=" + contactListCallback);
        this.contactListCallback = contactListCallback;
    }

    public void updateChattedUserList(List<ContactBean> chattedContactList) {
        Log.d(TAG, "updateChattedUserList: userListCallback=" + contactListCallback);
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
        ContactItemViewHolder vh = new ContactItemViewHolder(v);
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
            ((ContactItemViewHolder) holder).ivContactIcon.setImageResource(R.drawable.user_self);
        } else {
            ((ContactItemViewHolder) holder).ivContactIcon.setImageResource(R.drawable.user_friend);
        }

        ((ContactItemViewHolder) holder).bvUnReadMsgCount.setBadgeCount(contactBean.getUnReadMsgCount());
        ((ContactItemViewHolder) holder).tvContactName.setText(contactBean.getUserName());
        ((ContactItemViewHolder) holder).tvRecentMessage.setText(contactBean.getRecentMsg());
        ((ContactItemViewHolder) holder).tvRecentTime.setText(contactBean.getRecentTime());
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


    public class ContactItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        @BindView(R.id.swipeMenuLayout)
        SwipeMenuLayout swipeMenuLayout;
        @BindView(R.id.msg_layout)
        View msgLayout;
        @BindView(R.id.ivContactIcon)
        ImageView ivContactIcon;
        @BindView(R.id.bvUnReadMsgCount)
        BadgeView bvUnReadMsgCount;
        @BindView(R.id.tvContactName)
        TextView tvContactName;
        @BindView(R.id.tvRecentMessage)
        TextView tvRecentMessage;
        @BindView(R.id.tvRecentTime)
        TextView tvRecentTime;
        @BindView(R.id.btnUnRead)
        Button btnUnRead;
        @BindView(R.id.btnDelete)
        Button btnDelete;

        public ContactItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            msgLayout.setOnClickListener(this);
            btnUnRead.setOnClickListener(this);
            btnDelete.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getPosition();
            Log.d(TAG, "onClick: position=" + position);
            switch (v.getId()) {
                case R.id.btnUnRead:
                    swipeMenuLayout.quickClose();
                    break;
                case R.id.btnDelete:
                    swipeMenuLayout.quickClose();
                    chattedContactList.remove(position);
                    notifyItemRemoved(position);
                    break;
                case R.id.msg_layout:
                    if (contactListCallback != null) {
                        ContactBean contactBean = chattedContactList.get(position);
                        contactListCallback.onContactItemClick(contactBean);
                    }
            }

        }
    }
}
