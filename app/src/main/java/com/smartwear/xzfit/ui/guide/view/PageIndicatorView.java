package com.smartwear.xzfit.ui.guide.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.smartwear.xzfit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PageIndicatorView extends LinearLayout {

    private Context mContext = null;
    private int dotSize = 15; // 指示器的大小（dp）
    private int margins = 4; // 指示器间距（dp）
    private List<View> indicatorViews = null; // 存放指示器

    public PageIndicatorView(Context context) {
        this(context, null);
    }

    public PageIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;

        setGravity(Gravity.CENTER);
        setOrientation(HORIZONTAL);
//        setBackgroundColor(ContextCompat.getColor(mContext , R.color.tran));
        dotSize = dip2px(context, dotSize);
        margins = dip2px(context, margins);
    }

    public void initIndicator(int count) {

        if (indicatorViews == null) {
            indicatorViews = new ArrayList<>();
        } else {
            indicatorViews.clear();
            removeAllViews();
        }
        View view;
        LayoutParams params = new LayoutParams(dotSize, dotSize);
        params.setMargins(margins, margins, margins, margins);
        for (int i = 0; i < count; i++) {
            view = new View(mContext);
            Button btn = new Button(mContext);
            view.setBackgroundResource(R.mipmap.indicator_no_select);
            addView(view, params);
            indicatorViews.add(view);
        }
        if (indicatorViews.size() > 0) {
            indicatorViews.get(0).setBackgroundResource(R.mipmap.indicator_select);
        }
    }

    public void setSelectedPage(int selected) {
        for (int i = 0; i < indicatorViews.size(); i++) {
            if (i == (Locale.getDefault().getLanguage().equals("ar") ? indicatorViews.size() - (selected + 1) : selected)) {
                indicatorViews.get(i).setBackgroundResource(R.mipmap.indicator_select);
            } else {
                indicatorViews.get(i).setBackgroundResource(R.mipmap.indicator_no_select);
            }
        }
    }

    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}