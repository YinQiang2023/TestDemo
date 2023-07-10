package com.jwei.publicone.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.jwei.publicone.R;


public class BatteryPowerView extends AppCompatImageView {
    private Paint mPaint;
    private Bitmap mNullBitmap, mFullBitmap, mBitmapBG/*, mAlarmBitmap*/, mChargingBmp;
    private float pre;
    private boolean isCharging = false;

    public BatteryPowerView(Context context) {
        this(context, null);
    }

    public BatteryPowerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryPowerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mNullBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.battery_null);
        mFullBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.battery_full);
        mChargingBmp = BitmapFactory.decodeResource(getResources(), R.mipmap.battery_charging);
        pre = 1.0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmapBG == null) {
            mBitmapBG = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasbg = new Canvas(mBitmapBG);
            canvasbg.drawBitmap(mFullBitmap, null, new Rect(0, 0, getWidth(), getHeight()), mPaint);
        }
        canvas.save();
        if (isCharging) {
            canvas.drawBitmap(mChargingBmp, 0, 0, mPaint);
        } else {
            canvas.drawBitmap(mNullBitmap, 0, 0, mPaint);
            canvas.restore();
            mPaint.setShader(new BitmapShader(mBitmapBG, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
            canvas.save();
            float temp = (mFullBitmap.getWidth() * 0.06f) + (mFullBitmap.getWidth() * 0.8f * pre);
            float heightTemp = mFullBitmap.getHeight() - mFullBitmap.getHeight() * 0.11f;
            canvas.drawRect((mFullBitmap.getWidth() * 0.06f), mFullBitmap.getHeight() * 0.11f, temp, heightTemp, mPaint);
        }
        canvas.restore();
    }

    public void setPre(float pre, boolean isCharging) {
        this.pre = pre;
        this.isCharging = isCharging;
        postInvalidate();
    }

}
