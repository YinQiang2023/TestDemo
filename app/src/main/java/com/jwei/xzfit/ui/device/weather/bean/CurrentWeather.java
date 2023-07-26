package com.jwei.xzfit.ui.device.weather.bean;

import java.util.ArrayList;

public class CurrentWeather {
    public WeatherCoord coord;
    public ArrayList<WeatherItem> weather;
    public String base;
    public WeatherMain main;
    public String visibility;
    public WeatherWind wind;
    public WeatherRain rain;
    public WeatherClouds clouds;
    public String dt;
    public WeatherSys sys;
    public String timezone;
    public String id;
    public String name;
    public String cod;

    @Override
    public String toString() {
        return "CurrentWeather{" +
                "coord=" + coord +
                ", weather=" + weather +
                ", base='" + base + '\'' +
                ", main=" + main +
                ", visibility='" + visibility + '\'' +
                ", wind=" + wind +
                ", rain=" + rain +
                ", clouds=" + clouds +
                ", dt='" + dt + '\'' +
                ", sys=" + sys +
                ", timezone='" + timezone + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cod='" + cod + '\'' +
                '}';
    }
}
