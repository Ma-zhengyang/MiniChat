package com.android.mazhengyang.minichat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.adapter.ContactAdapter;
import com.android.mazhengyang.minichat.bean.BaseBean;
import com.android.mazhengyang.minichat.bean.ContactBean;
import com.android.mazhengyang.minichat.bean.HeaderBean;
import com.android.mazhengyang.minichat.model.IContactCallback;
import com.android.mazhengyang.minichat.util.CharacterParser;
import com.android.mazhengyang.minichat.util.PinyinComparator;
import com.android.mazhengyang.minichat.widget.SideBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mazhengyang on 18-12-6.
 */

public class ContactFragment extends Fragment implements
        ContactAdapter.OnContactItemClickListener {

    private static final String TAG = "MiniChat." + ContactFragment.class.getSimpleName();

    private IContactCallback contactCallback;
    private List<BaseBean> contactList;
    private ContactAdapter contactAdapter;
    private PinyinComparator pinyinComparator;

    @BindView(R.id.tv_head)
    TextView tvHead;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.sidebar)
    SideBar sideBar;
    @BindView(R.id.letterToastView)
    TextView letterToastView;

    public void setContactCallback(IContactCallback contactCallback) {
        this.contactCallback = contactCallback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_contact, null);
        ButterKnife.bind(this, view);
        Context context = getContext();

        tvHead.setText(R.string.tab_contactlist);

        //测试用
//        List<BaseBean> list = createTestList();
//        pinyinComparator = new PinyinComparator();
//        // 根据a-z进行排序源数据
//        if (list != null) {
//            Collections.sort(list, pinyinComparator);
//        }
//        //加入头部
//        contactList = addContactHeader(list);

        pinyinComparator = new PinyinComparator();
        // 根据a-z进行排序源数据
        if (contactList != null) {
            Collections.sort(contactList, pinyinComparator);
            //加入头部
            contactList = addContactHeader(contactList);
        }

//        for (int i = 0; i < contactList.size(); i++) {
//            Log.d(TAG, "onCreateView: " + contactList.get(i) + ", " + contactList.get(i).getSortLetter());
//        }

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        contactAdapter = new ContactAdapter(context, contactList);
        contactAdapter.setOnContactItemClickListener(this);
        recyclerView.setAdapter(contactAdapter);

        sideBar.setLetterToastView(letterToastView);
        sideBar.setSortLetters(getHeader(contactList));
        sideBar.setOnTouchingLetterChangedListener(onTouchingLetterChangedListener);

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: ");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    public void freshContact(List<BaseBean> list) {
        Log.d(TAG, "freshContact: ");
        if (list != null) {
            Collections.sort(list, pinyinComparator);
            list = addContactHeader(list);
            sideBar.setSortLetters(getHeader(list));
            contactAdapter.freshContact(list);
        }
        this.contactList = list;
    }

    @Override
    public void OnContactItemClick(BaseBean bean) {
        if (contactCallback != null) {
            contactCallback.onContactItemClick(bean);
        }
    }

    private SideBar.OnTouchingLetterChangedListener onTouchingLetterChangedListener
            = new SideBar.OnTouchingLetterChangedListener() {
        @Override
        public void onTouchingLetterChanged(String s) {
            for (int i = 0; i < contactList.size(); i++) {
                BaseBean bean = contactList.get(i);
                if (bean instanceof HeaderBean) {
                    String sortLetter = bean.getSortLetter();
                    if (s.equals(sortLetter)) {
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager)
                                recyclerView.getLayoutManager();
                        int position = i;
                        int firstItem = linearLayoutManager.findFirstVisibleItemPosition();
                        int lastItem = linearLayoutManager.findLastVisibleItemPosition();

                        Log.d(TAG, "onTouchingLetterChanged: firstItem=" + firstItem);
                        Log.d(TAG, "onTouchingLetterChanged: lastItem=" + lastItem);
                        Log.d(TAG, "onTouchingLetterChanged: position =" + position);

                        if (position <= firstItem) {
                            recyclerView.scrollToPosition(position);
                        } else if (position <= lastItem) {
                            int top = recyclerView.getChildAt(position - firstItem).getTop();
                            recyclerView.scrollBy(0, top);
                        } else {
                            recyclerView.scrollToPosition(position);
                        }

                        break;

                    }
                }
            }
        }
    };

    private ArrayList<BaseBean> addContactHeader(List<BaseBean> list) {
        //移除HeaderBean
        ArrayList<BaseBean> noHeaderList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                BaseBean bean = list.get(i);
                if (bean instanceof ContactBean) {
                    noHeaderList.add(bean);
                }
            }
        }
        //添加HeaderBean
        ArrayList<BaseBean> hasHeaderList = new ArrayList<>();
        if (noHeaderList != null && noHeaderList.size() > 0) {
            String sortLetter = noHeaderList.get(0).getSortLetter();
            HeaderBean headerBean = new HeaderBean();
            headerBean.setSortLetter(sortLetter);
            hasHeaderList.add(headerBean);
            hasHeaderList.add(noHeaderList.get(0));
            Log.d(TAG, "addContactHead: add 0 " + sortLetter);
            for (int i = 1; i < noHeaderList.size(); i++) {
                if (!noHeaderList.get(i).getSortLetter().equals(sortLetter)) {
                    sortLetter = noHeaderList.get(i).getSortLetter();
                    HeaderBean headerBean1 = new HeaderBean();
                    headerBean1.setSortLetter(sortLetter);
                    hasHeaderList.add(headerBean1);
                    Log.d(TAG, "addContactHead: add " + i + " " + sortLetter);
                }
                hasHeaderList.add(noHeaderList.get(i));
            }
        }
        return hasHeaderList;
    }

    private String[] getHeader(List<BaseBean> list) {
        if (list != null && list.size() > 0) {
            ArrayList<String> headerList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                BaseBean bean = list.get(i);
                if (bean instanceof HeaderBean) {
                    headerList.add(bean.getSortLetter());
                }
            }
            return headerList.toArray(new String[headerList.size()]);
        }

        return null;
    }

    private List<BaseBean> createTestList() {
        String[] contacts = getResources().getStringArray(R.array.contacts);
        List<BaseBean> list = new ArrayList<>();

        for (int i = 0; i < contacts.length; i++) {
            ContactBean contactBean = new ContactBean();
            contactBean.setUserIp("");
            String name = contacts[i];
            String pinyin = CharacterParser.getInstance().getSelling(name);
            String sortLetter = pinyin.substring(0, 1).toUpperCase();
            contactBean.setUserName(name);
            contactBean.setNamePinyin(pinyin);
            if (sortLetter.matches("[A-Z]")) {
                contactBean.setSortLetter(sortLetter);
            } else {
                contactBean.setSortLetter("#");
            }
            contactBean.setDeviceCode("");
            contactBean.setOnline(true);
            list.add(contactBean);
        }
        return list;
    }

}
