package com.android.mazhengyang.minichat.util;

import com.android.mazhengyang.minichat.bean.BaseBean;

import java.util.Comparator;

public class PinyinComparator implements Comparator<BaseBean> {

    @Override
    public int compare(BaseBean o1, BaseBean o2) {
        if (o1.getSortLetter().equals("@")
                || o2.getSortLetter().equals("#")) {
            return -1;
        } else if (o1.getSortLetter().equals("#")
                || o2.getSortLetter().equals("@")) {
            return 1;
        } else {
            return o1.getSortLetter().compareTo(o2.getSortLetter());
        }
    }

}
