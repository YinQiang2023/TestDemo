package com.jwei.publicone.ui.region;

/**
 * author : ym
 * package_name : com.transsion.data.model.bean
 * class_name : RegionBean
 * description : 国家/区域
 * time : 2021-11-04 15:56
 */
public class RegionBean extends SortModel {

    /**
     * 区域码
     */
    public String name;

    /**
     * 国家码
     */
    public String countryIsoCode;

    /**
     * 区域码
     */
    public String areaCode;

    public RegionBean() {
    }

    public RegionBean(String name, String code) {
        this.name = name;
        this.areaCode = code;
    }

    public RegionBean(String name, String countryIsoCode, String areaCode) {
        this.name = name;
        this.countryIsoCode = countryIsoCode;
        this.areaCode = areaCode;
    }

    @Override
    public String toString() {
        return "RegionBean{" +
                "name='" + name + '\'' +
                ", areaCode=" + areaCode +
                ", countryCode='" + countryIsoCode + '\'' +
                '}';
    }
}
