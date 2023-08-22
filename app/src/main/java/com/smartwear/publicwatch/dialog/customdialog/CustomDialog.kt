package com.smartwear.publicwatch.dialog.customdialog

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment

/**
 * <pre>
 * dialog构造提供类
 * </pre>
 */
object CustomDialog {

    @JvmStatic
    fun builder(context: Context?) = getBuilder(context)

    @JvmStatic
    fun builder(activity: Activity?) = getBuilder(activity)

    @JvmStatic
    fun builder(fragment: Fragment) = getBuilder(fragment.context)

    @JvmStatic
    fun builder(fragment: android.app.Fragment) = getBuilder(fragment.activity)

    @JvmStatic
    fun builder(view: View) = getBuilder(view.context)


    private fun getBuilder(context: Context?): AbsDialogBuilder {
        if (context == null) {
            throw NullPointerException("context cannot be NULL")
        }
        return DialogBuilder(context).builder()
    }

}