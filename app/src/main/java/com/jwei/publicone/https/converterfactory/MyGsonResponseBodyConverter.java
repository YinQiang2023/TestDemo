package com.jwei.publicone.https.converterfactory;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.jwei.publicone.BuildConfig;
import com.jwei.publicone.https.HttpCommonAttributes;
import com.jwei.publicone.utils.AESUtils;
import com.jwei.publicone.utils.JsonUtils;
import com.jwei.publicone.utils.SpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Created by https://blog.csdn.net/weixin_42273922/article/details/105947197
 * on 2021/7/14
 */
class MyGsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final TypeAdapter<T> adapter;

    MyGsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) {
        try {
            JSONObject json = new JSONObject();
            try {
                json = new JSONObject(value.string());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String resultString = AESUtils.decrypt(json.optString("result"), JsonUtils.serviceKey);

//            Log.i("MyGsonResponseConverter"," resultString "+resultString);
            JSONObject resultJson = new JSONObject(resultString);
            String code = resultJson.optString("code");
            if (HttpCommonAttributes.LOGIN_OUT.equals(code) || HttpCommonAttributes.AUTHORIZATION_EXPIRED.equals(code)) {
                JSONObject data = resultJson.optJSONObject("data");
                if (data != null) {
                    long loginTime = data.optLong("loginTime");
                    SpUtils.putValue(SpUtils.LAST_DEVICE_LOGIN_TIME, String.valueOf(loginTime));
                }
            }
            if(BuildConfig.DEBUG) LogUtils.d("HTTP",resultString);
            T result = adapter.fromJson(resultString);
            value.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) "value";
    }
}