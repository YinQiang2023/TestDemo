package com.smartwear.xzfit.ui.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.blankj.utilcode.util.ClickUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.smartwear.xzfit.databinding.ItemFeedbackScreenshotsAddBinding;
import com.smartwear.xzfit.databinding.ItemFeedbackScreenshotsBinding;
import com.smartwear.xzfit.utils.GlideApp;
import com.smartwear.xzfit.utils.ViewUtils;

import java.io.File;
import java.util.List;

/**
 *
 */
public class FeedbackScreenshotsAdapter extends MultiItemCommonAdapter<FeedbackImgItem, ViewBinding> {


    private int screenWidth;
    private Context mContext;
    private FeedbackClickListener listener;

    public void setFeedbackClickListener(FeedbackClickListener listener) {
        this.listener = listener;
    }

    /**
     * Same as QuickAdapter#QuickAdapter(Context,int) but with
     * some initialization data.
     *
     * @param data A new list is created out of this one to avoid mutable list
     */
    public FeedbackScreenshotsAdapter(Context context, List<FeedbackImgItem> data) {
        super(data);

        mContext = context;
        int[] androiodScreenProperty = ViewUtils.getAndroidScreenProperty(mContext);
        screenWidth = androiodScreenProperty[0] - ConvertUtils.dp2px(16);
    }

    @NonNull
    @Override
    protected ViewBinding createBinding(@Nullable ViewGroup parent, int viewType) {
        switch (viewType) {
            case FeedbackImgItem.IMG:
                return ItemFeedbackScreenshotsBinding.inflate(LayoutInflater.from(mContext), parent, false);
            default:
            case FeedbackImgItem.IMG_ADD:
                return ItemFeedbackScreenshotsAddBinding.inflate(LayoutInflater.from(mContext), parent, false);

        }
    }

    @Override
    public void convert(@NonNull ViewBinding viewBinding, FeedbackImgItem feedbackImgItem, int position) {
        switch (getItemViewType(position)) {
            case FeedbackImgItem.IMG_ADD:
                ItemFeedbackScreenshotsAddBinding addBinding = (ItemFeedbackScreenshotsAddBinding) viewBinding;
                ViewGroup.LayoutParams addLayoutParams = addBinding.ivFeedbackScreenshotsAdd.getLayoutParams();
                if (screenWidth != 0) {
                    addLayoutParams.height = (int) (((screenWidth - 5 * ConvertUtils.dp2px(20)) / 3.0f));
                    addLayoutParams.width = (int) (((screenWidth - 5 * ConvertUtils.dp2px(20)) / 3.0f));
                }
                addBinding.ivFeedbackScreenshotsAdd.setLayoutParams(addLayoutParams);
                ClickUtils.applySingleDebouncing(addBinding.ivFeedbackScreenshotsAdd, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onAdd(feedbackImgItem, position);
                        }
                    }
                });
                break;
            case FeedbackImgItem.IMG:
                ItemFeedbackScreenshotsBinding imgBinding = (ItemFeedbackScreenshotsBinding) viewBinding;
                GlideApp.with(mContext).load((File) feedbackImgItem.extra).into(imgBinding.ivFeedbackScreenshotsValue);
                imgBinding.ivFeedbackScreenshotsValue.setCornerRadius(ConvertUtils.dp2px(14));
                ViewGroup.LayoutParams layoutParams = imgBinding.ivFeedbackScreenshotsValue.getLayoutParams();
                if (screenWidth != 0) {
                    layoutParams.height = (int) (((screenWidth - 5 * ConvertUtils.dp2px(20)) / 3.0f));
                    layoutParams.width = (int) (((screenWidth - 5 * ConvertUtils.dp2px(20)) / 3.0f));
                }
                imgBinding.ivFeedbackScreenshotsValue.setLayoutParams(layoutParams);
                ClickUtils.applySingleDebouncing(imgBinding.ivFeedbackScreenshotsDel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onDel(feedbackImgItem, position);
                        }
                    }
                });
                break;
        }
    }

    @Override
    protected int getItemType(FeedbackImgItem feedbackImgItem) {
        return feedbackImgItem.getItemType();
    }

    public interface FeedbackClickListener {
        void onAdd(FeedbackImgItem feedbackImgItem, int position);

        void onDel(FeedbackImgItem feedbackImgItem, int position);
    }
}
