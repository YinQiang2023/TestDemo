package com.jwei.xzfit.ui.sport

import android.animation.Animator
import android.animation.AnimatorSet
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivitySportCountDownBinding
import com.jwei.xzfit.viewmodel.SportModel
import android.animation.ObjectAnimator
import android.content.Intent
import android.view.KeyEvent
import android.view.animation.LinearInterpolator
import com.blankj.utilcode.util.LogUtils


/**
 * Created by Android on 2021/9/28.
 * 开始运动倒计时
 */
class SportCountDownActivity : BaseActivity<ActivitySportCountDownBinding, SportModel>(
    ActivitySportCountDownBinding::inflate,
    SportModel::class.java
) {

    private lateinit var mAnimationSet: AnimatorSet
    private lateinit var mAnListener: Animator.AnimatorListener

    override fun initView() {
        super.initView()

        //region 动画
        val alphaAnim = ObjectAnimator.ofFloat(binding.tvCountDown, "alpha", 0.1f, 1.0f)
        val scaleXAnim = ObjectAnimator.ofFloat(binding.tvCountDown, "scaleX", 3.0f, 1.0f)
        val scaleYAnim = ObjectAnimator.ofFloat(binding.tvCountDown, "scaleY", 3.0f, 1.0f)
        mAnimationSet = AnimatorSet()
        mAnimationSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        mAnimationSet.duration = 1000L
        mAnimationSet.interpolator = LinearInterpolator()

        mAnListener = object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                viewModel.sportLiveData.resetTempData()
                finish()
                startActivity(Intent(this@SportCountDownActivity, MapSportActivity::class.java))
            }

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationRepeat(animation: Animator?) {}

        }
        //endregion

    }

    override fun initData() {
        super.initData()

        viewModel.startCountDown()

        viewModel.countDown.observe(this, {
            LogUtils.d("countDown == $it")
            binding.tvCountDown.text = "$it"

            if (::mAnimationSet.isInitialized) {
                if (mAnimationSet.isRunning) {
                    mAnimationSet.cancel()
                }
                mAnimationSet.start()
            }

            if (it == 1) {
                mAnimationSet.addListener(mAnListener)
            }
        })
    }

    //region 屏蔽返回键
    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()

        if (::mAnimationSet.isInitialized) {
            mAnimationSet.removeAllListeners()
            mAnimationSet.cancel()
        }
    }

}