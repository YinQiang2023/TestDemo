package com.jwei.xzfit.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.view.View
import com.jwei.xzfit.R
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.AppCompatImageView
import android.content.ContextWrapper
import android.text.TextUtils
import android.util.TypedValue
import android.widget.LinearLayout
import com.blankj.utilcode.util.*
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.dialog.customdialog.MyDialog


object DialogUtils {
    //var dialogWeakReferences: ArrayList<WeakReference<Dialog>?> = arrayListOf()

    fun dialogShowLoad(context: Context?): Dialog {
        /*val dialog = showBaseDialog(
            context, null, null, false, true, null, null, null
        )
        //等待dialog可点击关闭
        dialog.setCanceledOnTouchOutside(true)
        return dialog*/
        return showLoad(context, true)
    }

    fun dialogShowContent(context: Context?, content: String, rightBtnString: String, dialogClickListener: DialogClickListener?): Dialog {
        return showBaseDialog(context, null, content, true, false, null, rightBtnString, dialogClickListener)
    }

    fun dialogShowContentAndTwoBtn(
        context: Context?,
        content: String,
        leftBtnString: String?,
        rightBtnString: String,
        dialogClickListener: DialogClickListener?
    ): Dialog {
        return showBaseDialog(context, null, content, true, false, leftBtnString, rightBtnString, dialogClickListener)
    }

    @JvmStatic
    fun showLoad(context: Context?, defaultShow: Boolean = false): Dialog {
        return showBaseDialog(context, defaultShow)
    }

    private fun showBaseDialog(context: Context?, defaultShow: Boolean = true): Dialog {
        if (context == null) throw NullPointerException("context cannot be NULL")
        val baseDialog = MyDialog(context, R.style.dialog)
        baseDialog.setContentView(R.layout.dialog_progress)
        baseDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        val ivDialogCenter: AppCompatImageView = baseDialog.findViewById(R.id.ivDialogCenter)
        val animationDrawable = ivDialogCenter.background as AnimationDrawable
        animationDrawable.start()

        if (defaultShow) {
            getDialogActivity(context)?.let { act ->
                if (!act.isFinishing && !act.isDestroyed) {
                    baseDialog.show()
                }
            }
        }
        //dialogWeakReferences.add(WeakReference(baseDialog))
        setActivityLifecycleCallbacks(baseDialog)
        return baseDialog
    }

    fun showDialogContentAndTwoBtn(
        context: Context?,
        content: String,
        leftBtnString: String?,
        rightBtnString: String,
        dialogClickListener: DialogClickListener?
    ): Dialog {
        return showBaseDialog(
            context, null, content, true, false, leftBtnString, rightBtnString, dialogClickListener, false
        )
    }

    fun showDialogTwoBtn(
        context: Context?,
        title: String?,
        content: String,
        leftBtnString: String?,
        rightBtnString: String,
        dialogClickListener: DialogClickListener?
    ): Dialog {
        return showBaseDialog(
            context, title, content, true, false, leftBtnString, rightBtnString, dialogClickListener, false
        )
    }

    fun showDialogTitle(
        context: Context?,
        title: String?,
        content: String,
        leftBtnString: String?,
        rightBtnString: String,
        dialogClickListener: DialogClickListener?
    ): Dialog {
        return showBaseDialog(
            context, title, content, true, false, leftBtnString, rightBtnString, dialogClickListener, false
        )
    }

    fun showDialogTitleAndOneButton(
        context: Context?,
        title: String?,
        content: String,
        rightBtnString: String,
        dialogClickListener: DialogClickListener?
    ): Dialog {
        return showBaseDialog(context, title, content, true, false, null, rightBtnString, dialogClickListener)
    }

    fun showBaseDialog(
        context: Context?,
        title: String?,
        content: String?,
        isShowBottomBtn: Boolean,
        isShowCenterImg: Boolean,
        leftBtnString: String?,
        rightBtnString: String?,
        dialogClickListener: DialogClickListener?,
        defaultShow: Boolean = true
    ): Dialog {
        if (context == null) throw NullPointerException("context cannot be NULL")
        val baseDialog = MyDialog(context, R.style.dialog)
        baseDialog.setContentView(R.layout.dialog_base)
        baseDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        val tvDialogTitle: AppCompatTextView = baseDialog.findViewById(R.id.tvDialogTitle)
        val ivDialogCenter: AppCompatImageView = baseDialog.findViewById(R.id.ivDialogCenter)
        val tvDialogCenter: AppCompatTextView = baseDialog.findViewById(R.id.tvDialogCenter)
        val btnTvLeft: AppCompatTextView = baseDialog.findViewById(R.id.btnTvLeft)
        val btnTvRight: AppCompatTextView = baseDialog.findViewById(R.id.btnTvRight)
        val lyTwoBtn: LinearLayout = baseDialog.findViewById(R.id.lyTwoBtn)
        val view5 = baseDialog.findViewById<View>(R.id.view5)
        if (!isShowBottomBtn) {
            lyTwoBtn.visibility = View.INVISIBLE
        }
        if (leftBtnString != null) {
            btnTvLeft.text = leftBtnString
        } else {
            btnTvLeft.visibility = View.GONE
            view5.visibility = View.GONE
        }
        if (rightBtnString != null) {
            btnTvRight.text = rightBtnString
        }

        if (!TextUtils.isEmpty(title)) {
            tvDialogTitle.visibility = View.VISIBLE
            tvDialogTitle.text = title
        } else {
            tvDialogTitle.visibility = View.GONE
        }

        if (content != null) {
            tvDialogCenter.text = content
        }
        if (!isShowCenterImg) {
            ivDialogCenter.visibility = View.GONE
        } else {
            val animationDrawable = ivDialogCenter.background as AnimationDrawable
            animationDrawable.start()
        }
        ClickUtils.applySingleDebouncing(baseDialog.findViewById<View>(R.id.btnTvLeft)){
            baseDialog.dismiss()
            dialogClickListener?.OnCancel()
        }
        ClickUtils.applySingleDebouncing(btnTvRight){
            baseDialog.dismiss()
            dialogClickListener?.OnOK()
        }
        if (defaultShow) {
            getDialogActivity(context)?.let { act ->
                if (!act.isFinishing && !act.isDestroyed) {
                    baseDialog.show()
                }
            }
        }
        //dialogWeakReferences.add(WeakReference(baseDialog))
        setActivityLifecycleCallbacks(baseDialog)
        return baseDialog
    }

    //region dismissDialog

    /**
     * 监听dialog依附的页面生命周期
     */
    private fun setActivityLifecycleCallbacks(dialog: Dialog?) {
        getDialogActivity(dialog?.context)?.let { act ->
            ActivityUtils.removeActivityLifecycleCallbacks(act)
            ActivityUtils.addActivityLifecycleCallbacks(act, object : Utils.ActivityLifecycleCallbacks() {
                //使用onActivityPaused 存在弹窗互顶问题，导致流程丢失
                override fun onActivityDestroyed(activity: Activity) {
                    //LogUtils.d("activity Paused")
                    if (dialog != null && dialog.isShowing) {
                        LogUtils.d("onDestroyed() ---> dialog关闭")
                        dialog.dismiss()
                    }
                    if (disDialogTask != null) {
                        ThreadUtils.cancel(disDialogTask)
                        disDialogTask = null
                    }
                    if (disDialog != null && disDialog!!.isShowing) {
                        LogUtils.d("onDestroyed() ---> dialog关闭")
                        disDialog!!.dismiss()
                    }
                    disDialog = null
                    ActivityUtils.removeActivityLifecycleCallbacks(act)
                    super.onActivityDestroyed(activity)
                }
            })
        }
    }

//    /**
//     * 清除所有未关闭的dialog
//     * */
//    fun clearAllDialog() {
//        if (!dialogWeakReferences.isNullOrEmpty()) {
//            dialogWeakReferences.forEach { dialogWk ->
//                val dialog = dialogWk?.get()
//                if (dialog != null && dialog.isShowing) {
//                    getDialogActivity(dialog.context)?.let { act ->
//                        if (!act.isFinishing && !act.isDestroyed) {
//                            LogUtils.d("清除未关闭的dialog$dialogWk")
//                            dialog.dismiss()
//                        }
//                    }
//                }
//            }
//            dialogWeakReferences.clear()
//        }
//    }

    var disDialogTask: ThreadUtils.Task<Long>? = null
    var disDialog: Dialog? = null

    /**
     * 关闭dialog
     * */
    @JvmStatic
    fun dismissDialog(dialog: Dialog?, time: Long = 500L) {
        if (disDialogTask != null && disDialog == dialog) {
            ThreadUtils.cancel(disDialogTask)
        }
        disDialogTask = object : ThreadUtils.Task<Long>() {
            override fun doInBackground(): Long {
                Thread.sleep(time)
                return 0
            }

            override fun onSuccess(result: Long?) {
                if (dialog != null && dialog.isShowing) {
                    getDialogActivity(dialog.context)?.let { act ->
                        if (!act.isFinishing && !act.isDestroyed) {
                            LogUtils.d("计时完成 ---> dialog关闭")
                            dialog.dismiss()
                        }
                    }
                }
                disDialogTask = null
                disDialog = null
            }

            override fun onCancel() {
                LogUtils.d("dialog关闭被取消")
            }

            override fun onFail(t: Throwable?) {
                t?.printStackTrace()
            }
        }
        ThreadUtils.executeByCached(disDialogTask)
        disDialog = dialog
    }

    /**
     * 获取dialog依附的Activity
     * */
    fun getDialogActivity(cont: Context?): Activity? {
        return when (cont) {
            null -> {
                null
            }
            is Activity -> {
                cont
            }
            is ContextWrapper -> {
                getDialogActivity(cont.baseContext)
            }
            else -> null
        }
    }
    //endregion

    interface DialogClickListener {
        fun OnOK()
        fun OnCancel()
    }

    fun dp2px(spValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spValue, BaseApplication.mContext.resources.displayMetrics
        ).toInt()
    }
}