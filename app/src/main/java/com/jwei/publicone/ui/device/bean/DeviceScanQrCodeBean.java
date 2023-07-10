package com.jwei.publicone.ui.device.bean;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.blankj.utilcode.util.LogUtils;
import com.zhapp.ble.utils.BleUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DeviceScanQrCodeBean implements Parcelable {

    private String appDownUrl = ""; //APP下载地址
    private String radio = "";      //广播
    private String random = "";     //随机验证码
    private String name = "-";       //设备名称
    private DeviceRadioBroadcastBean mDeviceRadioBroadcastBean;

    protected DeviceScanQrCodeBean(Parcel in) {
        appDownUrl = in.readString();
        radio = in.readString();
        random = in.readString();
        name = in.readString();
        mDeviceRadioBroadcastBean = in.readParcelable(DeviceRadioBroadcastBean.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appDownUrl);
        dest.writeString(radio);
        dest.writeString(random);
        dest.writeString(name);
        dest.writeParcelable(mDeviceRadioBroadcastBean, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DeviceScanQrCodeBean> CREATOR = new Creator<DeviceScanQrCodeBean>() {
        @Override
        public DeviceScanQrCodeBean createFromParcel(Parcel in) {
            return new DeviceScanQrCodeBean(in);
        }

        @Override
        public DeviceScanQrCodeBean[] newArray(int size) {
            return new DeviceScanQrCodeBean[size];
        }
    };

    public String getAppDownUrl() {
        return appDownUrl;
    }

    public void setAppDownUrl(String appDownUrl) {
        this.appDownUrl = appDownUrl;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceScanQrCodeBean() {
    }

    public DeviceRadioBroadcastBean getmDeviceRadioBroadcastBean() {
        return mDeviceRadioBroadcastBean;
    }

    public void setmDeviceRadioBroadcastBean(DeviceRadioBroadcastBean mDeviceRadioBroadcastBean) {
        this.mDeviceRadioBroadcastBean = mDeviceRadioBroadcastBean;
    }

    public static class DeviceRadioBroadcastBean implements Parcelable {
        private byte[] data;        //原始数据
        //private String reservedID = ""; //特征ID
        private String deviceMac = ""; //mac地址
        private int deviceType = 0; //设备类型
        private int deviceVersion = 0; //设备版本号

        private boolean isBind = false; //是否被绑定
        private boolean isUserMode = false; //是否用户模式
        private boolean isDirectConnection = false; //是否支持直连绑定
        private boolean isSupportHeadset = false; //是否支持BT通话蓝牙
        private boolean isHeadsetBond = false; //BT蓝牙是否已配对
        private boolean isHeadsetBroadcast = false; //BT蓝牙是否已广播
        private String headsetMac = "";   //BT蓝牙MAC

        protected DeviceRadioBroadcastBean(Parcel in) {
            data = in.createByteArray();
            deviceMac = in.readString();
            deviceType = in.readInt();
            deviceVersion = in.readInt();
            isBind = in.readByte() != 0;
            isUserMode = in.readByte() != 0;
            isDirectConnection = in.readByte() != 0;
            isSupportHeadset = in.readByte() != 0;
            isHeadsetBond = in.readByte() != 0;
            isHeadsetBroadcast = in.readByte() != 0;
            headsetMac = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByteArray(data);
            dest.writeString(deviceMac);
            dest.writeInt(deviceType);
            dest.writeInt(deviceVersion);
            dest.writeByte((byte) (isBind ? 1 : 0));
            dest.writeByte((byte) (isUserMode ? 1 : 0));
            dest.writeByte((byte) (isDirectConnection ? 1 : 0));
            dest.writeByte((byte) (isSupportHeadset ? 1 : 0));
            dest.writeByte((byte) (isHeadsetBond ? 1 : 0));
            dest.writeByte((byte) (isHeadsetBroadcast ? 1 : 0));
            dest.writeString(headsetMac);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<DeviceRadioBroadcastBean> CREATOR = new Creator<DeviceRadioBroadcastBean>() {
            @Override
            public DeviceRadioBroadcastBean createFromParcel(Parcel in) {
                return new DeviceRadioBroadcastBean(in);
            }

            @Override
            public DeviceRadioBroadcastBean[] newArray(int size) {
                return new DeviceRadioBroadcastBean[size];
            }
        };

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

//        public String getReservedID() {
//            return reservedID;
//        }
//
//        public void setReservedID(String reservedID) {
//            this.reservedID = reservedID;
//        }

        public String getDeviceMac() {
            return deviceMac;
        }

        public void setDeviceMac(String deviceMac) {
            this.deviceMac = deviceMac;
        }

        public int getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(int deviceType) {
            this.deviceType = deviceType;
        }

        public int getDeviceVersion() {
            return deviceVersion;
        }

        public void setDeviceVersion(int deviceVersion) {
            this.deviceVersion = deviceVersion;
        }

        public boolean isBind() {
            return isBind;
        }

        public void setBind(boolean bind) {
            isBind = bind;
        }

        public boolean isUserMode() {
            return isUserMode;
        }

        public void setUserMode(boolean userMode) {
            isUserMode = userMode;
        }

        public boolean isDirectConnection() {
            return isDirectConnection;
        }

        public void setDirectConnection(boolean directConnection) {
            isDirectConnection = directConnection;
        }

        public boolean isSupportHeadset() {
            return isSupportHeadset;
        }

        public void setSupportHeadset(boolean supportHeadset) {
            isSupportHeadset = supportHeadset;
        }

        public boolean isHeadsetBond() {
            return isHeadsetBond;
        }

        public void setHeadsetBond(boolean headsetBond) {
            isHeadsetBond = headsetBond;
        }

        public boolean isHeadsetBroadcast() {
            return isHeadsetBroadcast;
        }

        public void setHeadsetBroadcast(boolean headsetBroadcast) {
            isHeadsetBroadcast = headsetBroadcast;
        }

        public String getHeadsetMac() {
            return headsetMac;
        }

        public void setHeadsetMac(String headsetMac) {
            this.headsetMac = headsetMac;
        }

        public DeviceRadioBroadcastBean() {
        }

        public DeviceRadioBroadcastBean(String input_data) {

            LogUtils.d("DeviceRadioBroadcastBean = input_data = " + input_data);

            if (input_data != null && !input_data.equals("") && !input_data.equals("null")) {
                byte[] my_data = new byte[0];
                try {
                    my_data = hexString2bytes(input_data);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                LogUtils.d("DeviceRadioBroadcastBean = my_data = " + bytes2HexString(my_data));
                LogUtils.d("DeviceRadioBroadcastBean = my_data len = " + my_data.length);
                setData(my_data);
                //旧协议
                if (my_data.length >= 11) {
                    StringBuilder mac_address = new StringBuilder("");
                    int mac_data_len = 6;
                    for (int i = 0; i < mac_data_len; i++) {
                        mac_address.append(String.format(Locale.ENGLISH, "%02X", data[i]));
                        if (i != mac_data_len - 1) {
                            mac_address.append(":");
                        }
                    }

                    int deviceType = ((data[7] & 0xff) << 8) | data[6] & 0xff;
                    int deviceVersion = ((data[8] & 0xff) << 16) | ((data[9] & 0xff) << 8) | data[10] & 0xff;

                    setDeviceMac(mac_address.toString().toUpperCase());
//                    setDeviceType(10026);
                    setDeviceType(deviceType);
                    setDeviceVersion(deviceVersion);

                    int[] deviceParams = BleUtils.BinstrToIntArray(data[11]);
                    // 未绑定
                    setBind(deviceParams[0] == 1);
                    setUserMode(deviceParams[1] == 0);
                    setDirectConnection(deviceParams[2] == 1);
                    // 是否支持BT
                    setSupportHeadset(deviceParams[3] == 1);
                    // BT是否配对
                    setHeadsetBond(deviceParams[4] == 1);
                    // BT是否已广播
                    setHeadsetBroadcast(deviceParams[5] == 1);
                    //deviceParams[6];//保留
                    //deviceParams[7];//保留
                }
                //新协议
                if (data.length >= 20) {
                    StringBuilder hmac = new StringBuilder("");
                    for (int i = 14; i < 20; i++) {
                        hmac.append(String.format(Locale.ENGLISH, "%02X", data[i]));
                        if (i != 19) {
                            hmac.append(":");
                        }
                    }
                    setHeadsetMac(hmac.toString().toUpperCase(Locale.ROOT));
                }
            }
        }


        @Override
        public String toString() {
            return "DeviceRadioBroadcastBean{" +
//                    "data=" + bytes2HexString(data) +
                    ", deviceMac='" + deviceMac +
                    ", deviceType=" + deviceType +
                    ", deviceVersion=" + deviceVersion +
                    ", isBind=" + isBind +
                    ", isUserMode=" + isUserMode +
                    ", isDirectConnection=" + isDirectConnection +
                    ", isSupportHeadset=" + isSupportHeadset +
                    ", isHeadsetBond=" + isHeadsetBond +
                    ", isHeadsetBroadcast=" + isHeadsetBroadcast +
                    ", headsetMac='" + headsetMac +
                    '}';
        }
    }

    public DeviceScanQrCodeBean(String data) {
        //radio=d855eb6b43c8384e010209000000&random=016260&name=E15_43C8";
        //radio=e72415133d083075010206340000d82415133d08&random=804545;
        if (data == null) return;
        Uri mUri = Uri.parse(data);
        // 协议
        String scheme = mUri.getScheme();
        // 域名+端口号+路径+参数
        String scheme_specific_part = mUri.getSchemeSpecificPart();
        // 域名+端口号
        String authority = mUri.getAuthority();
        // fragment
        String fragment = mUri.getFragment();
        // 域名
        String host = mUri.getHost();
        // 端口号
        int port = mUri.getPort();
        // 路径
        String path = mUri.getPath();
        // 参数
        String query = mUri.getQuery();

        LogUtils.d("======协议===scheme ==" + scheme);
        LogUtils.d("======域名+端口号+路径+参数==scheme_specific_part ===" + scheme_specific_part);
        LogUtils.d("======域名+端口号===authority ==" + authority);
        LogUtils.d("======fragment===fragment ==" + fragment);
        LogUtils.d("======域名===host ==" + host);
        LogUtils.d("======端口号===port ==" + port);
        LogUtils.d("======路径===path ==" + path);
        LogUtils.d("======参数===query ==" + query);


        // 依次提取出Path的各个部分的字符串，以字符串数组的形式输出
        List<String> pathSegments = mUri.getPathSegments();
        for (String str : pathSegments) {
            LogUtils.d("======路径拆分====path ==" + str);
        }
        // 获得所有参数 key
        Set<String> params = mUri.getQueryParameterNames();
        for (String param : params) {
            LogUtils.d("=====params=====" + param);
        }

        // 根据参数的 key，取出相应的值
        if (isUriExistParam(mUri, "radio")) {
            String radio = mUri.getQueryParameter("radio").trim();
            LogUtils.d("======参数===radio ==" + radio);
            setRadio(radio);
        }
        if (isUriExistParam(mUri, "random")) {
            String random = mUri.getQueryParameter("random").trim();
            LogUtils.d("======参数===random ==" + random);
            setRandom(random);
        }
        if (isUriExistParam(mUri, "name")) {
            String name = mUri.getQueryParameter("name").trim();
            LogUtils.d("======参数===name ==" + name);
            setName(name);
        }
        setAppDownUrl(data.trim());
        setmDeviceRadioBroadcastBean(new DeviceRadioBroadcastBean(radio));
    }

    @Override
    public String toString() {
        return "DeviceScanQrCodeBean{" +
                "appDownUrl='" + appDownUrl + '\'' +
                ", radio='" + radio + '\'' +
                ", random='" + random + '\'' +
                ", name='" + name + '\'' +
                ", mDeviceRadioBroadcastBean=" + mDeviceRadioBroadcastBean +
                '}';
    }

    /**
     * 是否存在参数
     *
     * @param uri
     * @param param
     * @return
     */
    private boolean isUriExistParam(Uri uri, String param) {
        return uri.getQueryParameter(param) != null;
    }

    /**
     * 将十六进制的字符串转换成字节 = 不带空格
     *
     * @param input_data d855eb6b43c8384e010209000000
     * @return
     * @throws NumberFormatException
     */
    private static byte[] hexString2bytes(String input_data) throws NumberFormatException {
        int dataLen = input_data.length() / 2;
        byte[] byteData = new byte[dataLen];
        for (int i = 0; i < dataLen; i++) {
            String str = input_data.substring(i * 2, i * 2 + 2);
            byteData[i] = (byte) Integer.parseInt(str, 16);
        }
        return byteData;
    }

    /**
     * byte[] 转 十六进制字符串-为了打印原始数据
     *
     * @param data
     * @return
     */
    private static String bytes2HexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length);
        if (data != null && data.length > 0) {
            for (byte byteChar : data)
                sb.append(String.format(Locale.ENGLISH, "%02X ", byteChar));
        }
        return sb.toString();
    }
}
