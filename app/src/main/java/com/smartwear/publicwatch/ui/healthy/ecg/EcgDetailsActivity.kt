package com.smartwear.publicwatch.ui.healthy.ecg

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.view.View
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityEcgDetailsBinding
import com.smartwear.publicwatch.db.model.Ecg
import com.smartwear.publicwatch.utils.LogUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.viewmodel.EcgModel
import com.zjw.healthdata.bean.EcgInfo

class EcgDetailsActivity : BaseActivity<ActivityEcgDetailsBinding, EcgModel>(ActivityEcgDetailsBinding::inflate, EcgModel::class.java), View.OnClickListener {
    private val TAG: String = EcgDetailsActivity::class.java.simpleName
    override fun setTitleId(): Int = binding.title.id
    private var ecgDataList: Array<String>? = null
    var mEcg: Ecg? = null

    override fun initView() {
        super.initView()
        binding.tvTitle.text = getString(R.string.ecg_details_title)
        setViewsClickListener(this, binding.tvPlayBack, binding.ivViewDetails)
        initHandler()
        EcgUtils.initEcgView(binding.ecgDetailsEcgview)
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.tvPlayBack.id -> {
                if (mEcg!!.ecgData != "") {
                    if (binding.tvPlayBack.text.toString().trim { it <= ' ' } == getString(R.string.ecg_details_play_back_tx)) {
                        timerStart()
                    } else {
                        timerStop()
                    }
                } else {
                    ToastUtils.showToast(R.string.no_data)
                }
            }

            binding.ivViewDetails.id -> {
                startActivity(Intent(this@EcgDetailsActivity, EcgReportActivity::class.java))

            }
        }
    }

    override fun initData() {
        super.initData()
        mEcg = EcgUtils.cacheEcg

        if (mEcg != null) {
            LogUtils.i(TAG, "initData mEcg = " + mEcg.toString())

            ecgDataList = mEcg!!.ecgData.split(",".toRegex()).toTypedArray()
            LogUtils.i(TAG, "initData ecgDataList = " + ecgDataList!!.size)

            if (mEcg!!.hrvResult == "0") {
                binding.tvHealthRating.text = getText(R.string.rating_excellent)
            } else if (mEcg!!.hrvResult == "1") {
                binding.tvHealthRating.text = getText(R.string.rating_good)
            } else if (mEcg!!.hrvResult == "2") {
                binding.tvHealthRating.text = getText(R.string.rating_commonly)
            }

            binding.tvHealthScore.text = mEcg!!.healthIndex
            binding.roundProgressView.setProgress(mEcg!!.healthIndex.toInt() / 100f)

            binding.tvFatigueIndexValue.text = mEcg!!.fatigueIndex
            binding.tvHeartFunctionValue.text = mEcg!!.cardiacFunction
            binding.tvQualityValue.text = mEcg!!.bodyQuality
            binding.tvPhysicalAndMentalLoadValue.text = mEcg!!.bodyLoad

            binding.linearLayout1.visibility = View.VISIBLE
            binding.linearLayoutHr.visibility = View.VISIBLE
            binding.tvHeartValue.text = mEcg!!.heart

            binding.linearLayoutBp.visibility = View.GONE

            binding.constraintLayoutEcgView.visibility = View.VISIBLE
            binding.constraintLayoutPpgView.visibility = View.GONE

        } else {
            LogUtils.i(TAG, "initData mEcg = null")
        }
    }


    private fun timerStart() {
        LogUtils.i(TAG, "timerStart")
        viewModel.mEcgDataProcessing!!.init()
        binding.tvPlayBack.text = getString(R.string.ecg_details_suspend_tx)
        timerHandler.sendEmptyMessage(1)
        timerIsStop = false
    }

    private fun timerStop() {
        LogUtils.i(TAG, "timerStop")
        binding.tvPlayBack.text = getString(R.string.ecg_details_play_back_tx)
        timerHandler.sendEmptyMessage(0)
        timerIsStop = true
        countDown = 0
    }

    private var timerIsStop = false
    private var countDown = 0

    // 计时器
    private val timerHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            // TODO Auto-generated method stub
            super.handleMessage(msg)
            when (msg.what) {
                // 添加更新ui的代码
                1 ->
                    if (!timerIsStop) {
                        sendEmptyMessageDelayed(1, 4)
                        if (countDown >= ecgDataList!!.size - 30) {
                            timerStop()
                        } else {
                            var i = 0
                            while (i < 5) {
                                countDown += 1
                                var mEcgInfo: EcgInfo
                                mEcgInfo = viewModel.getEcgToInfo((ecgDataList?.get(countDown))?.toInt()!!)
                                val ecgValue = mEcgInfo.ecgData
                                sendEcgDate(EcgUtils.getEcgDrawValue(ecgValue))
                                i++
                            }
                        }
                    }
                0 -> {}
            }
        }
    }


    private var mHandler: Handler? = null
    private fun initHandler() {
        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                // TODO Auto-generated method stub
                when (msg.what) {
                    MSG_DATA_ECG -> {
                        val ecgValue = msg.arg2
                        if (ecgValue > 0.0000001f) {
                            binding.ecgDetailsEcgview.setLinePoint(ecgValue.toFloat())
                        }
                    }
                    else -> {}
                }
                super.handleMessage(msg)
            }
        }
    }

    var MSG_DATA_ECG = 0x11
    private fun sendEcgDate(inputValue: Double) {
        val ecgValue: Double = inputValue
        val message = Message()
        message.what = MSG_DATA_ECG
        message.arg2 = ecgValue.toInt()
        mHandler!!.sendMessage(message)
    }

}