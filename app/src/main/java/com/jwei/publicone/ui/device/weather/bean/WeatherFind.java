package com.jwei.publicone.ui.device.weather.bean;

import java.util.ArrayList;

public class WeatherFind {
    public String message;
    public String cod;
    public String count;
    public ArrayList<WeatherFindItem> list;

    public class WeatherFindItem {
        public String id;
        public String name;
        public WeatherCoord coord;
        public WeatherMain main;
        public String dt;
        public WeatherWind wind;
        public WeatherSys sys;
        public WeatherRain rain;
        public WeatherClouds clouds;
        public ArrayList<WeatherItem> weather;
    }
}
