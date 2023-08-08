package com.smartwear.xzfit.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import com.blankj.utilcode.util.ConvertUtils;
import com.smartwear.xzfit.R;


/**
 * Created by dai on 20/5/4.
 */
public class CustomProgressTextView extends androidx.appcompat.widget.AppCompatTextView {

    //背景画笔
    private Paint mBackgroundPaint;
    //背景颜色
    private int mBackgroundColor;

    private int mStrokeColor;

    //进度条背景颜色
    private int mProgressColor;

    private float mProgress = -1;
    //    private int mMaxProgress;
//    private int mMinProgress;
    private float mProgressPercent;

    private float mRadius;

    private RectF mBackgroundBounds;
    private LinearGradient mProgressBgGradient;

    //普通状态
    public static final int NORMAL = 0;
    //有进度状态
    public static final int DOWNLOADING = 1;

    private int mState;
    private boolean progressbtnBgStroke;
    private RectF rectF;
    private Path path;
    private Path path1;


    public CustomProgressTextView(Context context) {
        this(context, null);

    }

    public CustomProgressTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            initAttrs(context, attrs);
            init();
        }

    }

    private void initAttrs(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomProgressTextView);
        mBackgroundColor = a.getColor(R.styleable.CustomProgressTextView_background_color, Color.parseColor("#6699ff"));
        mStrokeColor = a.getColor(R.styleable.CustomProgressTextView_stroke_color, Color.parseColor("#6699ff"));
        mProgressColor = a.getColor(R.styleable.CustomProgressTextView_progress_color, Color.LTGRAY);
        mRadius = a.getFloat(R.styleable.CustomProgressTextView_radius, getMeasuredHeight() / 2f);
        mRadius = ConvertUtils.dp2px(mRadius);
//        mTextColor = a.getColor(R.styleable.AnimDownloadProgressButton_progressbtn_text_color, mBackgroundColor);
//        mTextCoverColor = a.getColor(R.styleable.AnimDownloadProgressButton_progressbtn_text_covercolor, Color.WHITE);
        progressbtnBgStroke = a.getBoolean(R.styleable.CustomProgressTextView_progress_bg_stroke, false);
//        installingNeedProgress = a.getBoolean(R.styleable.AnimDownloadProgressButton_installing_need_progress, false);
        a.recycle();
    }

    private void init() {

//        mMaxProgress = 100;
//        mMinProgress = 0;
        mProgress = 0;

        //设置背景画笔
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        //初始化状态设为NORMAL
        mState = NORMAL;
        invalidate();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInEditMode()) {
            drawing(canvas);
        }
    }

    private void drawing(Canvas canvas) {
        drawBackground(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (mBackgroundBounds == null) {
            mBackgroundBounds = new RectF();
            mBackgroundBounds.left = 0;
            mBackgroundBounds.top = 0;
            mBackgroundBounds.right = getMeasuredWidth();
            mBackgroundBounds.bottom = getMeasuredHeight();
        }
        if (mRadius == 0) {
            mRadius = getMeasuredHeight() / 2f;
        }

        //color
        switch (mState) {
            case NORMAL:
                if (mBackgroundPaint.getShader() != null) {
                    mBackgroundPaint.setShader(null);
                }
                if (progressbtnBgStroke) {
                    mBackgroundPaint.setStyle(Paint.Style.STROKE);
                    mBackgroundPaint.setStrokeWidth(ConvertUtils.dp2px(1));
                    mBackgroundPaint.setColor(mStrokeColor);
                } else {
                    mBackgroundPaint.setStyle(Paint.Style.FILL);
                    mBackgroundPaint.setColor(mBackgroundColor);
                }
                canvas.drawRoundRect(mBackgroundBounds, mRadius, mRadius, mBackgroundPaint);
                break;
            case DOWNLOADING:
                if (progressbtnBgStroke) {
                    mBackgroundPaint.setStyle(Paint.Style.STROKE);
                    mBackgroundPaint.setStrokeWidth(ConvertUtils.dp2px(1));
                    mBackgroundPaint.setColor(mStrokeColor);
                } else {
                    mBackgroundPaint.setStyle(Paint.Style.FILL);
                    mBackgroundPaint.setColor(mBackgroundColor);
                }
                canvas.drawRoundRect(mBackgroundBounds, mRadius, mRadius, mBackgroundPaint);
                drawProgress(canvas);
                break;
        }
    }

    private void drawProgress(Canvas canvas) {
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mProgressPercent = mProgress / 100f;
        if (mProgressPercent >= 0.99) {
            mProgressPercent = 0.999f;
        }

        mBackgroundPaint.setColor(mProgressColor);
        if (rectF == null) {
            rectF = new RectF();
            rectF.left = mBackgroundBounds.left;
            rectF.top = mBackgroundBounds.top;
            rectF.bottom = mBackgroundBounds.bottom;
        }
        rectF.right = mBackgroundBounds.right * mProgressPercent/*0.05f*/;

        if (rectF.right - rectF.left <= 2 * mRadius) {
            float cx = 0;
            float cy = 0;
            if (path == null) {
                path = new Path();
            }
            cx = mBackgroundBounds.left + mRadius;
            cy = mBackgroundBounds.top + mRadius;
            path.addCircle(cx, cy, mRadius, Path.Direction.CCW);
            canvas.clipPath(path);
//            canvas.drawColor(Color.RED);
            float cx1 = rectF.right - mRadius;
            float cy1 = 0;
            if (path1 == null) {
                path1 = new Path();
            }
            cy1 = mBackgroundBounds.top + mRadius;
            path1.addCircle(cx1, cy1, mRadius, Path.Direction.CCW);
            canvas.clipPath(path1, Region.Op.INTERSECT);
            canvas.drawColor(mProgressColor);

        } else {
            canvas.drawRoundRect(rectF, mRadius, mRadius, mBackgroundPaint);
        }

    }


    public int getState() {
        return mState;
    }

    public void setState(int state, float progress) {
        this.mState = state;
        if (state == NORMAL) {
            mProgress = 0;
            invalidate();
        } else if (state == DOWNLOADING) {
            setProgress(progress);
        }
    }

    public void setProgress(float progress) {
        if (progress == mProgress) return;
        if (progress >= 0 && progress <= 100) {
            mProgress = progress;
        } else if (progress < 0) {
            mProgress = 0;
        } else if (progress > 100) {
            mProgress = 100;
        }

        if (mProgress <= 0) {
            mState = NORMAL;
        } else {
            mState = DOWNLOADING;
        }

        invalidate();
    }

    public float getProgress() {
        return mProgress;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mState = ss.state;
        mProgress = ss.progress;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, (int) mProgress, mState);
    }

    public static class SavedState extends BaseSavedState {

        private int progress;
        private int state;

        public SavedState(Parcelable parcel, int progress, int state) {
            super(parcel);
            this.progress = progress;
            this.state = state;
        }

        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            state = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }


}
