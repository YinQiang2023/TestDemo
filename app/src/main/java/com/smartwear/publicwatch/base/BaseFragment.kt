package com.smartwear.publicwatch.base

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ClickUtils
import com.gyf.barlibrary.ImmersionBar
import com.gyf.barlibrary.ImmersionOwner
import com.gyf.barlibrary.ImmersionProxy
import com.smartwear.publicwatch.ui.data.Global
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel>(
    var inflater: (LayoutInflater) -> VB, private val clazz: (Class<VM>)
) : Fragment(), ImmersionOwner, CoroutineScope by MainScope() {

    protected lateinit var binding: VB
    protected lateinit var viewmodel: VM

    /**
     * ImmersionBar代理类
     */
    private val mImmersionProxy = ImmersionProxy(this)

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        mImmersionProxy.isUserVisibleHint = isVisibleToUser
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mImmersionProxy.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = inflater(layoutInflater)
        viewmodel = ViewModelProvider(this)[clazz]
        initView()
        initData()
        return binding.root
    }

    open fun initView() {}
    open fun initData() {}

    override fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mImmersionProxy.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        mImmersionProxy.onResume()
    }

    override fun onPause() {
        super.onPause()
        mImmersionProxy.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mImmersionProxy.onDestroy()
        cancel()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        mImmersionProxy.onHiddenChanged(hidden)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mImmersionProxy.onConfigurationChanged(newConfig)
    }

    /**
     * 懒加载，在view初始化完成之前执行
     * On lazy after view.
     */
    override fun onLazyBeforeView() {}

    /**
     * 懒加载，在view初始化完成之后执行
     * On lazy before view.
     */
    override fun onLazyAfterView() {}

    /**
     * Fragment用户可见时候调用
     * On visible.
     */
    override fun onVisible() {}

    override fun initImmersionBar() {
        val id = setTitleId()
        if (id != 0) {
            ImmersionBar.with(this).keyboardEnable(isKeyBoard).statusBarDarkFont(false).titleBar(id).init()
        } else {
            ImmersionBar.with(this).keyboardEnable(isKeyBoard).statusBarDarkFont(false).init()
        }
    }

    var isKeyBoard = true
    open fun setTitleId(): Int {
        return 0
    }

    /**
     * Fragment用户不可见时候调用
     * On invisible.
     */
    override fun onInvisible() {}

    /**
     * 是否可以实现沉浸式，当为true的时候才可以执行initImmersionBar方法
     * Immersion bar enabled boolean.
     *
     * @return the boolean
     */
    override fun immersionBarEnabled(): Boolean {
        return true
    }

    /**
     * 设置点击事件
     * */
    protected fun setViewsClickListener(listener: View.OnClickListener, vararg v: View) {
        ClickUtils.applySingleDebouncing(v, Global.SINGLE_CLICK_INTERVAL, listener)
    }

}