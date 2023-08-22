package com.smartwear.publicwatch.ui.device.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class PhoneDtoModel implements Parcelable {
    private String name;        //联系人姓名
    private String telPhone;    //电话号码
    private long date;        //未接来电时间
    private String smsContext;  //短信内容

    protected PhoneDtoModel(Parcel in) {
        name = in.readString();
        telPhone = in.readString();
        date = in.readLong();
        smsContext = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(telPhone);
        dest.writeLong(date);
        dest.writeString(smsContext);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PhoneDtoModel> CREATOR = new Creator<PhoneDtoModel>() {
        @Override
        public PhoneDtoModel createFromParcel(Parcel in) {
            return new PhoneDtoModel(in);
        }

        @Override
        public PhoneDtoModel[] newArray(int size) {
            return new PhoneDtoModel[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelPhone() {
        return telPhone;
    }

    public void setTelPhone(String telPhone) {
        this.telPhone = telPhone;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getSmsContext() {
        return smsContext;
    }

    public void setSmsContext(String smsContext) {
        this.smsContext = smsContext;
    }

    public PhoneDtoModel() {
    }

    public PhoneDtoModel(String name, String telPhone) {
        this.name = name;
        this.telPhone = telPhone;
    }

    @Override
    public String toString() {
        return "PhoneDtoModel{" +
                "name='" + name + '\'' +
                ", telPhone='" + telPhone + '\'' +
                ", date='" + date + '\'' +
                ", smsContext='" + smsContext + '\'' +
                '}';
    }
}
