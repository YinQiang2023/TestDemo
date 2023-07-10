package com.jwei.publicone.ui.device.setting.heartrate

import com.jwei.publicone.view.wheelview.contract.TextProvider
import java.io.Serializable

class SettingHeartRateBean(/*var value: Int, */var name: String) : TextProvider, Serializable {
    override fun provideText(): String {
        return name
    }

    override fun toString(): String {
        return "$name"
    }
}