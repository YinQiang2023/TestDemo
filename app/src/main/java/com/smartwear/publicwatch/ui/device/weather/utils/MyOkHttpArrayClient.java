package com.smartwear.publicwatch.ui.device.weather.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by android
 * on 2021/5/31
 */
public class MyOkHttpArrayClient {
    private static final String TAG = MyOkHttpArrayClient.class.getSimpleName();
    private static final int TIME_OUT = 100;// 超时时间
    private volatile static MyOkHttpArrayClient mClient = null;
    private static okhttp3.OkHttpClient mOkHttpClient;


    public static MyOkHttpArrayClient getInstance() {
        if (null == mClient) {
            synchronized (MyOkHttpArrayClient.class) {
                if (null == mClient) {
                    mClient = new MyOkHttpArrayClient();
                }
            }
        }
        return mClient;
    }

    private MyOkHttpArrayClient() {
        okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();
        mOkHttpClient = builder.build();
    }

    private static CommonJsonCallback jsonCallback;

    void asynGetCall(DisposeDataHandle handle, String url) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = mOkHttpClient.newCall(request);
        jsonCallback = new CommonJsonCallback(handle);
        call.enqueue(jsonCallback);
    }

    private static class CommonJsonCallback implements Callback {

        protected final String RESULT_CODE = "code";
        protected final String ERROR_MSG = "ERROR_MSG";
        protected final String EMPTY_MSG = "EMPTY_MSG";
        protected final String COOKIE_STORE = "Set-Cookie";

        /**
         * the java layer exception, do not same to the logic error
         */
        protected final int NETWORK_ERROR = -1;// the network relative error
        protected final int JSON_ERROR = -2;// the JSON relative error
        protected final int OTHER_ERROR = -3;// the unknow error

        /**
         * 将其它线程的数据转发到UI线程
         */
        private Handler mDeliveryHandler;
        private DisposeDataListener mListener;
        private Class<?> mClass;
        private boolean retry = true;

        public CommonJsonCallback(DisposeDataHandle handle) {
            this.mListener = handle.mListener;
            this.mClass = handle.mClass;
            this.mDeliveryHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onFailure(final Call call, final IOException ioexception) {
            // 此时还在非UI线程，因此要转发
            mDeliveryHandler.post(() -> mListener.onFailure(ioexception));
        }

        @Override
        public void onResponse(final Call call, final Response response) throws IOException {
            final String result = response.body().string();
            mDeliveryHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mListener.onSuccess(new JSONArray(result));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }
}
