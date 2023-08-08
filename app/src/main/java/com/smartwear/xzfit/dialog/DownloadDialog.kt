package com.smartwear.xzfit.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.Utils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.GlideApp

class DownloadDialog {


    var progressView: ProgressBar? = null
    var tvSize: TextView? = null
    var tvProgress: TextView? = null
    var context: Activity? = null
    private var baseDialog: Dialog? = null

    constructor(context: Activity?, title: String, contentUrl: String) {
        if (context == null) return
        this.context = context
        showDialog(title, contentUrl, null, null)
    }

    constructor(context: Activity?, title: String, contentUrl: String, bgBitmap: Bitmap?, textBitmap: Bitmap?) {
        if (context == null) return
        this.context = context
        showDialog("", contentUrl, bgBitmap, textBitmap)
    }


    fun showDialog(title: String, contentUrl: String, bgBitmap: Bitmap?, textBitmap: Bitmap?) {
        baseDialog = Dialog(context!!, R.style.dialog)
        baseDialog?.setContentView(R.layout.dialog_download)
        baseDialog?.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        val lp = baseDialog?.window!!.attributes
        lp.width = (ScreenUtils.getScreenWidth() * 0.8).toInt()
        baseDialog?.window!!.attributes = lp
        baseDialog?.setCancelable(false)
        val tvTitle = baseDialog?.findViewById<AppCompatTextView>(R.id.tvDialogTitle)
        val ivCenter = baseDialog?.findViewById<ImageView>(R.id.ivCenter)
        val ivBg = baseDialog?.findViewById<View>(R.id.iv_bg)
        val ivCenterText = baseDialog?.findViewById<ImageView>(R.id.ivCenterText)
        tvProgress = baseDialog?.findViewById<TextView>(R.id.tvProgress)
        progressView = baseDialog?.findViewById<ProgressBar>(R.id.progressView)
        tvSize = baseDialog?.findViewById<TextView>(R.id.tvSize)

        tvTitle?.text = title
        if (contentUrl.isNullOrEmpty()) {
            ivBg?.visibility = View.GONE
            ivCenter?.visibility = View.GONE
        } else {
            if (bgBitmap == null && ivCenter != null && context != null) {
                GlideApp.with(context!!)
                    .load(contentUrl)
                    .placeholder(R.mipmap.no_renderings)
                    .error(R.mipmap.no_renderings)
                    .into(ivCenter)
            }
        }

        if (bgBitmap != null) {
            ivBg?.visibility = View.VISIBLE
            ivCenter?.visibility = View.VISIBLE
            ivCenter?.setImageBitmap(bgBitmap)
        }

        if (textBitmap != null) {
            ivCenterText?.visibility = View.VISIBLE
            ivCenterText?.setImageBitmap(textBitmap)
        }

        ActivityUtils.addActivityLifecycleCallbacks(context, object : Utils.ActivityLifecycleCallbacks() {
            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                cancel()
            }
        })
    }

    fun isShowing(): Boolean {
        if (baseDialog != null) {
            return baseDialog!!.isShowing
        }
        return false
    }

    fun showDialog() {
        AppUtils.tryBlock {
            context?.apply {
                if (!isDestroyed && !isFinishing) {
                    baseDialog?.show()
                }
            }
        }
    }

    fun cancel() {
        AppUtils.tryBlock {
            if (baseDialog?.isShowing == true) {
                baseDialog?.dismiss()
            }
        }
    }

}