package com.simplelibrary.sp;

import android.text.TextUtils;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 说明：
 * Created by jjs on 2019/4/10
 */
public class HistorySp {
    protected SPUtils mSPUtils;
    private final String KEY_User = "HistorySp";
    private List<String> mHistoryList;
    private int maxSize = 6;

    public HistorySp() {
        mSPUtils = SPUtils.getInstance(KEY_User);
        mHistoryList = new ArrayList<>();
    }

    public List<String> getHistoryList() {
        mHistoryList.clear();
        String data = mSPUtils.getString(KEY_User);
        if (!TextUtils.isEmpty(data)) {
            String[] strings = data.split("=");
            mHistoryList.addAll(Arrays.asList(strings));
        }
        return mHistoryList;
    }

    public void addHistory(String str) {
        if (StringUtils.isEmpty(str)) {
            return;
        }
        mHistoryList.remove(str);
        if (mHistoryList.size() >= maxSize) {
            mHistoryList.remove(mHistoryList.size() - 1);
        }
        mHistoryList.add(0, str);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mHistoryList.size(); i++) {
            builder.append(mHistoryList.get(i)).append("=");
        }
        builder.deleteCharAt(builder.length() - 1);
        mSPUtils.put(KEY_User, builder.toString());
    }

    public void clear() {
        mSPUtils.clear(true);
        mHistoryList.clear();
    }
}
