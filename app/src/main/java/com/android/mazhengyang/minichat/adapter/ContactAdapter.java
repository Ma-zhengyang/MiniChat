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

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.BaseBean;
import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.bean.HeaderBean;
import com.android.mazhengyang.minichat.util.NetUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MiniChat." + ContactAdapter.class.getSimpleName();

    private static final int TYPE_HEAD = 0;
    private static final int TYPE_CONTACT = 1;

    private Context context;
    private List<BaseBean> list;

    public ContactAdapter(Context context, List<BaseBean> list) {
        Log.d(TAG, "ContactAdapter: ");
        this.context = context;
        this.list = list;
    }

    private OnContactItemClickListener onContactItemClickListener;

    public interface OnContactItemClickListener {
        void OnContactItemClick(BaseBean bean);
    }

    public void setOnContactItemClickListener(OnContactItemClickListener listener) {
        this.onContactItemClickListener = listener;
    }

    public void freshContact(List<BaseBean> list) {
        if (list != null) {
            Log.d(TAG, "freshContact: list size=" + list.size());
//            for (int i = 0; i < list.size(); i++) {
//                ContactBean contactBean = list.get(i);
//                Log.d(TAG, "freshContact: i=" + i + "," + contactBean.getUserName());
//            }
        } else {
            Log.d(TAG, "freshContact: list is null");
        }
        this.list = list;
        this.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        if (viewType == TYPE_HEAD) {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_contact_head, parent, false);
            HeadViewHolder vh = new HeadViewHolder(v);
            return vh;
        } else {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_contact, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (list == null) {
            Log.d(TAG, "onBindViewHolder: list is null");
            return;
        }

        BaseBean bean = list.get(position);
        // Log.d(TAG, "onBindViewHolder: position=" + position + ", " + bean + ", " + bean.getSortLetter());

        if (bean instanceof HeaderBean) {
            ((HeadViewHolder) holder).tvLetter.setText(bean.getSortLetter());
        } else {
            ContactBean contactBean = (ContactBean) bean;
            String name = contactBean.getUserName();
            String ip = contactBean.getUserIp();
            String deviceCode = contactBean.getDeviceCode();

            if (contactBean.isOnline()) {
                ((ViewHolder) holder).tvUserName.setTextColor(Color.BLACK);
            } else {
                ((ViewHolder) holder).tvUserName.setTextColor(Color.GRAY);
            }
            if (deviceCode.equals(NetUtils.getDeviceCode(context))) {
                ((ViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_self);
                ((ViewHolder) holder).tvUserName.setText(name);
            } else {
                ((ViewHolder) holder).ivUserIcon.setImageResource(R.drawable.user_friend);
                ((ViewHolder) holder).tvUserName.setText(name);
            }
            ((ViewHolder) holder).tvUserIp.setText(ip);
        }

    }

    @Override
    public int getItemViewType(int position) {
        BaseBean bean = list.get(position);
        //  Log.d(TAG, "getItemViewType: position=" + position + ", " + bean);
        if (bean instanceof HeaderBean) {
            //      Log.d(TAG, "getItemViewType: TYPE_HEAD");
            return TYPE_HEAD;
        } else {
            //   Log.d(TAG, "getItemViewType: TYPE_CONTACT");
            return TYPE_CONTACT;
        }
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            Log.d(TAG, "getItemCount: list is null");
            return 0;
        }
        return list.size();
    }

    public class HeadViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tvLetter)
        TextView tvLetter;

        public HeadViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {

        @BindView(R.id.ivUserIcon)
        ImageView ivUserIcon;
        @BindView(R.id.tvUserName)
        TextView tvUserName;
        @BindView(R.id.tvUserIp)
        TextView tvUserIp;


        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onContactItemClickListener != null) {
                int position = this.getPosition();
                onContactItemClickListener.OnContactItemClick(list.get(position));
            }
        }

    }

}
