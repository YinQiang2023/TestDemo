package com.smartwear.publicwatch.ui.device.weather.bean;

import java.util.ArrayList;

/**
 * Created by android
 * on 2021/5/31
 */
public class WeatherAQI {
    public WeatherCoord coord;

    public ArrayList<Aqi> list;

    public class Aqi {
        public String dt;
        public Main main;
        public Components components;
    }

    public static class Main {
        public String aqi;

        @Override
        public String toString() {
            return "Main{" +
                    "aqi='" + aqi + '\'' +
                    '}';
        }
    }

    public static class Components {
        public String co;
        public String no;
        public String no2;
        public String o3;
        public String so2;
        public String pm2_5;
        public String pm10;
        public String nh3;

        @Override
        public String toString() {
            return "Components{" +
                    "co='" + co + '\'' +
                    ", no='" + no + '\'' +
                    ", no2='" + no2 + '\'' +
                    ", o3='" + o3 + '\'' +
                    ", so2='" + so2 + '\'' +
                    ", pm2_5='" + pm2_5 + '\'' +
                    ", pm10='" + pm10 + '\'' +
                    ", nh3='" + nh3 + '\'' +
                    '}';
        }

        /*
        *  "co": 507.36,
                "no": 6.48,
                "no2": 21.42,
                "o3": 6.35,
                "so2": 31.47,
                "pm2_5": 16.35,
                "pm10": 27.85,
                "nh3": 0.39
        *
        * */
    }

}
