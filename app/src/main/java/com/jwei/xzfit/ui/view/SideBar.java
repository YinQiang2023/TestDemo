package com.jwei.xzfit.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jwei.xzfit.R;

public class SideBar extends View {
    private String[] sideData;
    // 触摸事件
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    public static String[] defSideData = {"A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z", "#"};
    private int choose = -1;
    private Paint paint = new Paint();

    private TextView mTextDialog;
    private Context mContext;

    public SideBar(Context context) {
        this(context, null);
    }

    public SideBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SideBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        sideData = defSideData;
    }

    public void setSideData(String[] sideData) {
        this.sideData = sideData;
        postInvalidate();
    }

    /**
     * 为SideBar设置显示字母的TextView
     *
     * @param textDialog
     */
    public void setTextView(TextView textDialog) {
        this.mTextDialog = textDialog;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int width = getWidth();
        int singleHeight = height / sideData.length;// 获取每一个字母的高度

        for (int i = 0; i < sideData.length; i++) {
            paint.setColor(ContextCompat.getColor(mContext, R.color.color_171717));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setAntiAlias(true);
            paint.setTextSize(30);
            if (i == choose) {// 选中的状态
                paint.setColor(ContextCompat.getColor(mContext, R.color.app_index_color));
                paint.setFakeBoldText(true);
            }
            // x坐标等于中间-字符串宽度的一半
            float xPos = width / 2f - paint.measureText(sideData[i]) / 2;
            // x坐标靠右显示
//            float xPos = width;
            float yPos = singleHeight * i + singleHeight/2;
            canvas.drawText(sideData[i], xPos, yPos, paint);
            paint.reset();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float y = event.getY();
        final int oldChoose = choose;
        final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;
        final int c = (int) (y / getHeight() * sideData.length);// 点击y坐标所占总高度的比例*b数组的长度就等于点击b中的个数
        if (action == MotionEvent.ACTION_UP) {
            choose = -1;//
            invalidate();
            if (mTextDialog != null) {
                mTextDialog.setVisibility(View.INVISIBLE);
            }
        } else {
            if (oldChoose != c) {
                if (c >= 0 && c < sideData.length) {
                    if (listener != null) {
                        listener.onTouchingLetterChanged(sideData[c]);
                    }
                    if (mTextDialog != null) {
                        mTextDialog.setText(sideData[c]);
                        mTextDialog.setVisibility(View.VISIBLE);
                    }
                    choose = c;
                    invalidate();
                }
            }
        }
        return true;
    }

    /**
     * 触摸事件
     *
     * @param onTouchingLetterChangedListener
     */
    public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    /**
     * @author coder
     */
    public interface OnTouchingLetterChangedListener {
        void onTouchingLetterChanged(String s);
    }
}
