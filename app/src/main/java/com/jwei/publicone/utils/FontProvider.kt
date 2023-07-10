package com.jwei.publicone.utils

import android.content.Context
import android.graphics.Typeface

/**
 * Created by Android on 2022/10/19.
 */
object FontProvider {

    //region Roboto-BoldCondensedItalic.ttf
    private var robotoBoldCondensedItalic: Typeface? = null

    fun getRobotoBoldCondensedItalic(context :Context):Typeface {
        if(robotoBoldCondensedItalic == null){
            robotoBoldCondensedItalic = Typeface.createFromAsset(context.assets, "font/Roboto-BoldCondensedItalic.ttf")
        }
        return robotoBoldCondensedItalic!!
    }
    //endregion

}