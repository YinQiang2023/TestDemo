package com.jwei.publicone.utils

import android.os.Build
import android.os.LocaleList
import java.util.*

object LocaleUtils {

    fun getLocalLanguage(): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            Locale.getDefault()
        }
        return locale.language.contains("zh") && locale.country.contains("CN")
    }

    fun getSelectLocalLanguage(): Boolean {
        return SpUtils.getValue(
            SpUtils.SERVICE_REGION_COUNTRY_CODE, ""
        ) == "cn" && SpUtils.getValue(
            SpUtils.SERVICE_REGION_AREA_CODE, ""
        ) == "86"
    }

    fun getLocalCountry(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            Locale.getDefault()
        }
        return locale.country
    }
}