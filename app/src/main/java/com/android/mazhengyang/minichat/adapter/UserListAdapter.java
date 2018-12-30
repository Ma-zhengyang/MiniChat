package com.android.mazhengyang.minichat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.bean.UserBean;
import com.android.mazhengyang.minichat.util.NetUtils;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by mazhengyang on 18-11-27.
 */

public class UserListAdapter extends BaseAdapter implements
        StickyListHeadersAdapter, SectionIndexer {

    private static final String TAG = "MiniChat." + UserListAdapter.class.getSimpleName();

    private List<UserBean> list;

    private String[] mCountries;
    private int[] mSectionIndices;
    private Character[] mSectionLetters;
    private LayoutInflater mInflater;

    private Context context;

    public UserListAdapter(Context context, List<UserBean> list) {
        Log.d(TAG, "UserListAdapter: list=" + list);
        this.context = context;
        mInflater = LayoutInflater.from(context);
        this.list = list;
        mCountries = new String[1];

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                mCountries[i] = list.get(i).getUserName();
            }

            mSectionIndices = getSectionIndices();
            mSectionLetters = getSectionLetters();
        }
    }

    public void freshUserList(List<UserBean> list) {
        Log.d(TAG, "freshUserList: list=" + list);
        this.list = list;

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                mCountries[i] = list.get(i).getUserName();
            }

            mSectionIndices = getSectionIndices();
            mSectionLetters = getSectionLetters();
        }

        this.notifyDataSetChanged();
    }

    private int[] getSectionIndices() {
        Log.d(TAG, "getSectionIndices: ");
        ArrayList<Integer> sectionIndices = new ArrayList<>();
        char lastFirstChar = mCountries[0].charAt(0);
        sectionIndices.add(0);
        for (int i = 1; i < mCountries.length; i++) {
            if (mCountries[i].charAt(0) != lastFirstChar) {
                lastFirstChar = mCountries[i].charAt(0);
                sectionIndices.add(i);
            }
        }
        int[] sections = new int[sectionIndices.size()];
        for (int i = 0; i < sectionIndices.size(); i++) {
            sections[i] = sectionIndices.get(i);
        }
        return sections;
    }

    private Character[] getSectionLetters() {
        Log.d(TAG, "getSectionLetters: ");
        Character[] letters = new Character[mSectionIndices.length];
        for (int i = 0; i < mSectionIndices.length; i++) {
            letters[i] = mCountries[mSectionIndices[i]].charAt(0);
        }
        return letters;
    }

    @Override
    public int getCount() {
        if (list == null) {
            Log.d(TAG, "getCount: list is null");
            return 0;
        }
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        if (list == null) {
            Log.d(TAG, "getItem: list is null");
            return null;
        }
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (list == null) {
            Log.d(TAG, "getView: list is null");
            return null;
        }

        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_user, parent, false);
            holder.ivUserIcon = convertView.findViewById(R.id.ivUserIcon);
            holder.tvUserName = convertView.findViewById(R.id.tvUserName);
            holder.tvUserIp = convertView.findViewById(R.id.tvUserIp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        UserBean user = list.get(position);
        Log.d(TAG, "getView: user=" + user);

        if (user != null) {
            String name = user.getUserName();
            String ip = user.getUserIp();

            Log.d(TAG, "getView: name=" + name);
            Log.d(TAG, "getView: ip=" + ip);

            if (user.isOnline()) {
                holder.tvUserName.setTextColor(Color.BLACK);
            } else {
                holder.tvUserName.setTextColor(Color.GRAY);
            }

            if (ip.equals(NetUtils.getLocalIpAddress())) {
                holder.ivUserIcon.setImageResource(R.drawable.user_self);
                holder.tvUserName.setText(name);
            } else {
                holder.ivUserIcon.setImageResource(R.drawable.user_friend);
                holder.tvUserName.setText(name);
            }
            holder.tvUserIp.setText(ip);
        }

        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {

        if (list == null) {
            Log.d(TAG, "onBindViewHolder: list is null");
            return null;
        }

        HeaderViewHolder holder;

        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = mInflater.inflate(R.layout.header, parent, false);
            holder.headerText = convertView.findViewById(R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        // set header text as first char in name
        CharSequence headerChar = mCountries[position].subSequence(0, 1);
        holder.headerText.setText(headerChar);

        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        return mCountries[position].subSequence(0, 1).charAt(0);
    }

    @Override
    public Object[] getSections() {
        return mSectionLetters;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (mSectionIndices.length == 0) {
            return 0;
        }

        if (sectionIndex >= mSectionIndices.length) {
            sectionIndex = mSectionIndices.length - 1;
        } else if (sectionIndex < 0) {
            sectionIndex = 0;
        }
        return mSectionIndices[sectionIndex];
    }

    @Override
    public int getSectionForPosition(int position) {
        for (int i = 0; i < mSectionIndices.length; i++) {
            if (position < mSectionIndices[i]) {
                return i - 1;
            }
        }
        return mSectionIndices.length - 1;
    }

    class HeaderViewHolder {
        TextView headerText;
    }

    class ViewHolder {
        ImageView ivUserIcon;
        TextView tvUserName;
        TextView tvUserIp;
    }
}
