package com.jwei.publicone.ui.refresh;


import android.content.Context;
import android.util.AttributeSet;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;

public class CustomizeRefreshLayout extends SmartRefreshLayout {
    private CustomizeRefreshHeader refreshheader;

    public CustomizeRefreshLayout(Context context) {
        super(context);
        initView(context);
    }

    public CustomizeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        setReboundDuration(300); // 设置回弹动画时长

        setEnableAutoLoadMore(false);// 设置是否监听列表在滚动到底部时触发加载事件

        if (getRefreshHeader() instanceof CustomizeRefreshHeader) {
            refreshheader = (CustomizeRefreshHeader) getRefreshHeader();
            if (refreshheader != null) {
                refreshheader.setSetOnRefreshState(new CustomizeRefreshHeader.SetOnRefreshState() {
                    @Override
                    public void onRefreshing() {
                        finishRefresh(false);
                    }
                });
            }
        }
    }

    // 下拉/上拉完成
    public void complete() {
        if (mState == RefreshState.Loading) {
            finishLoadMore();
        } else {
            finishRefresh();
        }
    }
}
