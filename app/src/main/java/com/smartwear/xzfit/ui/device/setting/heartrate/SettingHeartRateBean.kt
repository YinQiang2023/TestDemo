package com.smartwear.xzfit.ui.device.setting.heartrate

import com.smartwear.xzfit.view.wheelview.contract.TextProvider
import java.io.Serializable

class SettingHeartRateBean(/*var value: Int, */var name: String) : TextProvider, Serializable {
    override fun provideText(): String {
        return name
    }

    override fun toString(): String {
        return "$name"
    }
}