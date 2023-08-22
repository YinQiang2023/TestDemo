package com.smartwear.publicwatch.ui.view

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.blankj.utilcode.util.ClickUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.databinding.LayoutNoInternetBinding
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.user.bean.UserBean
import com.smartwear.publicwatch.utils.GlideApp

class ViewForLayoutNoInternet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {
    private lateinit var binding: LayoutNoInternetBinding
    private var listener: OnRetryListener? = null
    companion object{
        const val TYPE_NO_NETWORK = 0
        const val TYPE_REF = 1
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSave.id -> {
                listener?.onOnRetry()
            }
        }
    }


    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        binding = LayoutNoInternetBinding.inflate(LayoutInflater.from(context), this, true)
        ClickUtils.applySingleDebouncing(binding.btnSave, Global.SINGLE_CLICK_INTERVAL, this)
        refreshHead()


    }

    fun setType(type:Int){
        when(type){
            TYPE_NO_NETWORK->{
                binding.tvState.visibility = View.VISIBLE
                binding.btnSave.visibility = View.VISIBLE
                binding.ivRef.visibility = View.GONE
                (binding.ivRef.background as AnimationDrawable?)?.stop()
            }
            TYPE_REF->{
                binding.tvState.visibility = View.GONE
                binding.btnSave.visibility = View.GONE
                binding.ivRef.visibility = View.VISIBLE
                //开启帧动画
                (binding.ivRef.background as AnimationDrawable?)?.start()
            }
        }
    }

    /**
     * 刷新头部信息
     */
    fun refreshHead() {
        val mUserBean = UserBean().getData()
        if (!TextUtils.isEmpty(mUserBean.head)) {
            GlideApp.with(this).load(mUserBean.head)
                .error(R.mipmap.ic_mine_avatar)
                .placeholder(R.mipmap.ic_mine_avatar)
                .into(binding.ivHead)
        } else {
            GlideApp.with(this).load(R.mipmap.ic_mine_avatar)
                .error(R.mipmap.ic_mine_avatar)
                .placeholder(R.mipmap.ic_mine_avatar)
                .into(binding.ivHead)
        }
    }

    fun hideHead() {
       binding.ivHead.visibility= GONE
       binding.textView17.visibility= GONE
    }


    /**
     * 设置重试监听
     * */
    fun setRetryListener(listener: OnRetryListener) {
        this.listener = listener
    }

    interface OnRetryListener {
        fun onOnRetry()
    }
}