package com.jwei.publicone.ui.user.bean;

import android.content.Context;

import com.jwei.publicone.R;

import java.io.Serializable;

public class UserLocalData implements Serializable {
    public String userId;
    public int userMapIndex = 0;
    public int wearLeftRightIndex;

    public String getUserMapIndexStr(Context context) {
        String value;
        if (userMapIndex == 0) {
            value = context.getString(R.string.google);
        } else {
            value = context.getString(R.string.gothe);
        }
        return value;
    }

    public String getWearLeftRightIndexStr(Context context) {
        String value;
        if (wearLeftRightIndex == 0) {
            value = context.getString(R.string.left_hand);
        } else {
            value = context.getString(R.string.right_hand);
        }
        return value;
    }
}
