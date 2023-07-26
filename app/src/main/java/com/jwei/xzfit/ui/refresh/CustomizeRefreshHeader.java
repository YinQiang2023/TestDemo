package com.jwei.xzfit.ui.refresh;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshKernel;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;
import com.scwang.smart.refresh.layout.constant.SpinnerStyle;
import com.jwei.xzfit.R;

import java.lang.ref.WeakReference;

public class CustomizeRefreshHeader extends RelativeLayout implements RefreshHeader {
    private TextView mTextView;
    private ImageView mArrow;
    private boolean isArrowDown = false;
    private SetOnRefreshState setOnRefreshState;
    private WeakReference<Context> mContext;
    private boolean isShowRefreshing = false;

    public CustomizeRefreshHeader(Context context) {
        super(context);
        this.initView(context, null, 0);
    }

    public CustomizeRefreshHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initView(context, attrs, 0);
    }

    public CustomizeRefreshHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        setCanShowRefreshing(false);
        mContext = new WeakReference<>(context);
        setMinimumHeight(dp2px(context, 80));
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(CENTER_IN_PARENT);
        View headerView = View.inflate(context, R.layout.refresh_header, null);
        mTextView = (TextView) headerView.findViewById(R.id.textview);
        mArrow = (ImageView) headerView.findViewById(R.id.iv_refresh_center);
        addView(headerView, params);
    }

    /**
     * 是否显示刷新中状态
     *
     * @param isShowRefreshing
     */
    public void setCanShowRefreshing(boolean isShowRefreshing) {
        this.isShowRefreshing = isShowRefreshing;
    }

    @Override
    public void onInitialized(RefreshKernel kernel, int height, int extendHeight) { // 尺寸定义完成
    }

    @Override
    public void onMoving(boolean isDragging, float percent, int offset, int height, int maxDragHeight) {
        float startPercent = 0.20f;

        if (percent > startPercent && percent < 1) {
            float tempPercent = (percent - startPercent) * 1.0f / (1 - startPercent);
        }
    }

    @Override
    public void onReleased(@NonNull RefreshLayout refreshLayout, int height, int maxDragHeight) {

    }

    public void onPullingDown(float percent, int offset, int headHeight, int extendHeight) { // 手指拖动下拉（会连续多次调用）
        float startPercent = 0.20f;

        if (percent > startPercent && percent < 1) {
            float tempPercent = (percent - startPercent) * 1.0f / (1 - startPercent);
        }
    }

    public void onReleasing(float percent, int offset, int headHeight, int extendHeight) {  // 手指释放之后的持续动画
    }

    @Override
    public void onStartAnimator(RefreshLayout layout, int headHeight, int extendHeight) {
        RotateAnimation ta = new RotateAnimation(0, 360,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        ta.setDuration(500);
        ta.setRepeatCount(10000);
        ta.setInterpolator(new LinearInterpolator());
        ta.setFillAfter(true);
//        mCircleProgressView.startAnimation(ta);
    }

    @Override
    public int onFinish(RefreshLayout layout, boolean success) {
        return 100; // 动画结束,延迟多少毫秒之后再收回
    }

    @Override
    public void onHorizontalDrag(float percentX, int offsetX, int offsetMax) {

    }

    @Override
    public boolean isSupportHorizontalDrag() {
        return false;
    }

    @Override
    public void setPrimaryColors(int... colors) {
        setBackgroundColor(getResources().getColor(R.color.index_bg_color));
    }

    @NonNull
    public View getView() {
        return this;
    }

    @Override
    public SpinnerStyle getSpinnerStyle() {
        return SpinnerStyle.Translate;
    }

    @Override
    public void onStateChanged(RefreshLayout refreshLayout, RefreshState oldState, RefreshState newState) { // 状态改变事件
        switch (newState) {
            case None: // 无状态
//                if (mCircleProgressView != null) mCircleProgressView.setProgressPersent(0);
                if (isArrowDown) {
                    arrowAnimation();
                    isArrowDown = false;
                }
                break;
            case PullDownToRefresh: // 可以下拉状态
                mTextView.setText(mContext.get().getString(R.string.healthy_sports_refresh_tips));
                break;
            case Refreshing: // 刷新中状态
                if (isShowRefreshing) {
                    mTextView.setText(mContext.get().getString(R.string.refreshing_tips));
                }
                if (setOnRefreshState != null)
                    setOnRefreshState.onRefreshing();
                break;
            case ReleaseToRefresh:  // 释放就开始刷新状态
                mTextView.setText(mContext.get().getString(R.string.healthy_sports_release_refresh_tips));
                if (!isArrowDown) {
                    arrowAnimation();
                    isArrowDown = true;
                }
                break;
        }
    }

    private void arrowAnimation() {
        RotateAnimation ra = new RotateAnimation(0, isArrowDown ? 0 : 180.0f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(200);
        ra.setRepeatCount(0);
        ra.setInterpolator(new LinearInterpolator());
        ra.setFillAfter(true);
        mArrow.startAnimation(ra);
    }

    /**
     * dp转px
     */
    private int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    public void setSetOnRefreshState(SetOnRefreshState setOnRefreshState) {
        this.setOnRefreshState = setOnRefreshState;
    }

    public interface SetOnRefreshState {
        void onRefreshing();
    }
}
