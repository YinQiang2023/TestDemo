package com.smartwear.publicwatch.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.*
import com.gyf.barlibrary.ImmersionBar
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.ui.GlobalEventManager
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.view.Watermark
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.DisplayUtil
import java.util.*

abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel>(
    val inflate: (LayoutInflater) -> VB, private val clazz: (Class<VM>),
) : AppCompatActivity() {
    /**
     * 跳转携带的数据基本类型
     */
    protected val INTENT_BASIC = "intent_basic"
    protected lateinit var binding: VB
    protected lateinit var viewModel: VM
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(clazz) as VM
        initTitleBar(binding.root)

        val titleId = setTitleId()
        if (titleId != 0) {
            ImmersionBar.with(this)
                .keyboardEnable(isKeyBoard)
                .statusBarDarkFont(false)
                .titleBar(titleId)
                .init()
        } else {
            ImmersionBar.with(this)
                .keyboardEnable(isKeyBoard)
                .statusBarDarkFont(false)
                .init()
        }

        //页面计时
        canTiming = true
        initView()
        initData()
        addBetaView()
    }


    override fun onDestroy() {
        super.onDestroy()
        ImmersionBar.with(this).destroy()
        canTiming = false
        canVisible = false
    }

    private var layoutRight: LinearLayout? = null
    var tvTitle: TextView? = null
    var tvCenterTitle: TextView? = null
    var tvTitle2: TextView? = null
    var ivRightIcon: ImageView? = null
    var tvRIght: TextView? = null

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initTitleBar(v: View) {
        tvTitle = v.findViewById(R.id.tvTitle)
        tvCenterTitle = v.findViewById(R.id.tvCenterTitle)
        tvTitle2 = v.findViewById(R.id.tvTitle2)
        ivRightIcon = v.findViewById(R.id.ivRightIcon)
        layoutRight = v.findViewById(R.id.layoutRight)
        tvRIght = v.findViewById(R.id.tvRIght)
        tvTitle?.setOnClickListener {
            if (finishListener != null) {
                finishListener?.onFinish()
            }
            finish()
        }
        //阿拉伯适配
        val language = Locale.getDefault().language;
        if (language.equals("ar")) {
            val drawableLeft: Drawable? = getDrawable(R.mipmap.left_arrow)
            tvTitle?.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null, drawableLeft, null
            )
            tvTitle?.compoundDrawablePadding = 4
        }
    }

    protected open fun setTvTitle(title: Int) {
        if (tvCenterTitle != null) {
            tvCenterTitle!!.text = resources.getString(title)
        }

    }

    protected open fun setTvTitle(title: String) {
        if (tvCenterTitle != null) {
            tvCenterTitle!!.text = title
        }
    }

    protected open fun setOnlyTitle(title: Int) {
        if (tvTitle2 != null) {
            tvCenterTitle?.visibility = View.GONE
            layoutRight?.visibility = View.GONE
            tvTitle2!!.visibility = View.VISIBLE
            tvTitle2!!.text = resources.getString(title)
        }
    }

    protected open fun setOnlyTitle(title: String) {
        if (tvTitle2 != null) {
            tvCenterTitle?.visibility = View.GONE
            layoutRight?.visibility = View.GONE
            tvTitle2!!.visibility = View.VISIBLE
            tvTitle2!!.text = title
        }
    }


    /**
     * 设置标题栏右侧图标或右侧文字
     * */
    protected fun setRightIconOrTitle(@DrawableRes imgId: Int = 0, rightText: String = "", clickListener: View.OnClickListener? = null) {
        layoutRight?.visibility = View.VISIBLE
        if (imgId != 0) {
            ivRightIcon?.visibility = View.VISIBLE
            tvRIght?.visibility = View.GONE
            ivRightIcon?.setImageDrawable(ContextCompat.getDrawable(this, imgId))
            ivRightIcon?.setOnClickListener(clickListener)
        }
        if (!TextUtils.isEmpty(rightText)) {
            tvRIght?.visibility = View.VISIBLE
            ivRightIcon?.visibility = View.GONE
            tvRIght?.text = rightText
            tvRIght?.setOnClickListener(clickListener)
        }
    }

    interface FinishListener {
        fun onFinish()
    }

    private var finishListener: FinishListener? = null

    open fun setFinishListener(finishListener: FinishListener?) {
        this.finishListener = finishListener
    }

    protected open fun initData() {
    }

    protected open fun initView() {
    }

    /**
     * 设置点击事件
     * */
    protected fun setViewsClickListener(listener: View.OnClickListener, vararg v: View) {
        ClickUtils.applySingleDebouncing(v, Global.SINGLE_CLICK_INTERVAL, listener)
    }

    var isDarkFont = true
    var isKeyBoard = true
    protected open fun setTitleId(): Int {
        return 0
    }

    /**
     * 添加Beta版本说明
     * */
    @SuppressLint("SetTextI18n")
    private fun addBetaView() {
        if (AppUtils.isBetaApp()) {
            Watermark.getInstance()
                .setText("${AppUtils.getAppVersionName()}_Beta")
                .setTextSize(ConvertUtils.dp2px(10f).toFloat())
                .setTextColor(Color.parseColor("#80ff0000"))
                .show(this)
        }
    }

    /**
     * 重写 getResource 方法，防止系统字体影响
     */
    override fun getResources(): Resources {
        val resources = super.getResources()
        return DisplayUtil.getResources(this, resources, 1f)
    }

    override fun attachBaseContext(newBase: Context) {
        AppUtils.tryBlock {
            val config: Configuration = newBase.getResources().getConfiguration()
            //保持字体大小不随系统设置变化（用在界面加载之前）
            config.fontScale = 1f
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                val displayMetrics: DisplayMetrics = newBase.resources.displayMetrics
                newBase.resources.updateConfiguration(config, displayMetrics)
                super.attachBaseContext(newBase)
            } else {
                //super.attachBaseContext(newBase.createConfigurationContext(config))
                applyOverrideConfiguration(config)
                super.attachBaseContext(newBase)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LogUtils.e("newConfig --->$newConfig")
    }

    protected open fun onBackPress() {
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                onBackPress()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        GlobalEventManager.onActivityResult(requestCode, resultCode, data)
    }

    fun AppCompatActivity.closeNow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition()
        } else {
            finish()
        }
    }

    //region 页面计时逻辑
    //1.页面可见时间
    var visibleTime = 0
    var canTiming = false
    var canVisible = false

    fun startVisibleTimeTimer(){
        ThreadUtils.executeByIo(VisibleTimeTimer())
    }

    override fun onResume() {
        super.onResume()
        canVisible = true
    }

    override fun onPause() {
        super.onPause()
        canVisible = false
    }

    private inner class VisibleTimeTimer:ThreadUtils.SimpleTask<Int>(){
        override fun doInBackground(): Int {
            while (canTiming){
                Thread.sleep(1000)
                if(canVisible){
                    visibleTime++
                }
            }
            return visibleTime
        }
        override fun onSuccess(result: Int?) {

        }
    }

    //endregion
}