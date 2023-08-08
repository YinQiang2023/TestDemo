package com.smartwear.xzfit.ui.healthy.ecg

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.view.View
import com.blankj.utilcode.util.ImageUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityEcgReportBinding
import com.smartwear.xzfit.db.model.Ecg
import com.smartwear.xzfit.ui.user.bean.TargetBean
import com.smartwear.xzfit.ui.user.bean.UserBean
import com.smartwear.xzfit.utils.LogUtils
import com.smartwear.xzfit.utils.TimeUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.viewmodel.EcgModel

class EcgReportActivity : BaseActivity<ActivityEcgReportBinding, EcgModel>(ActivityEcgReportBinding::inflate, EcgModel::class.java), View.OnClickListener {
    private val TAG: String = EcgReportActivity::class.java.simpleName
    override fun setTitleId(): Int = binding.title.id
    private lateinit var mUserBean: UserBean
    private lateinit var mTargetBean: TargetBean
    private var ecg_data: Array<String>? = null

    var mEcg: Ecg? = null

    override fun initView() {
        super.initView()
        binding.tvTitle.text = getString(R.string.ecg_report_title)
        setViewsClickListener(this, binding.presentationBack, binding.presentationShare)
        initHandler()
        EcgUtils.initEcgAllView(binding.ecgReportECGAllView)
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.presentationBack.id -> {
                finish()
            }

            binding.presentationShare.id -> {
                viewModel.shareEcgReportImg(this, ImageUtils.view2Bitmap(binding.rlReportView))

            }
        }
    }


    var lenght = 0
    private val myHandler = Handler()

    @SuppressLint("SetTextI18n")
    override fun initData() {
        super.initData()
        mUserBean = UserBean().getData()
        mTargetBean = TargetBean().getData()

        binding.presentationUserNickanme.text = ":" + mUserBean.nickname
        binding.presentationUserAge.text = ":" + TimeUtils.getAge(mUserBean.birthDate)
        //女性
        if (mUserBean.sex == "1") {
            binding.presentationUserSex.text = ":" + getString(R.string.user_info_female)
        }
        //男性
        else {
            binding.presentationUserSex.text = ":" + getString(R.string.user_info_male)
        }
        //英制
        if (mTargetBean.unit.equals("1")) {
            binding.presentationUserHeight.text = ":" + mUserBean.britishHeight + getString(R.string.unit_height_0)
            binding.presentationUserWeight.text = ":" + mUserBean.britishWeight + getString(R.string.unit_weight_0)
        }
        //公制
        else {
            binding.presentationUserHeight.text = mUserBean.height + getString(R.string.unit_height_1)
            binding.presentationUserWeight.text = mUserBean.weight + getString(R.string.unit_weight_1)
        }
        mEcg = EcgUtils.cacheEcg
        if (mEcg != null) {
            LogUtils.i(TAG, "initData mEcg = " + mEcg.toString())
            binding.ecgTime.text = ":" + mEcg!!.healthMeasuringTime
            binding.presentationUserHeart.text = ":" + mEcg!!.heart + getString(R.string.hr_unit_bpm)
            if (mEcg!!.ecgData != "") {
                val maxSize = EcgUtils.MaxWidth * EcgUtils.LineNumber
                ecg_data = viewModel.getNewReportEcgData(mEcg!!.ecgData, maxSize)
                lenght = ecg_data!!.size
                if (ecg_data!!.size > maxSize) {
                    lenght = maxSize
                }
                myHandler.postDelayed({ update() }, 100)
            } else {
                ToastUtils.showToast(R.string.no_data)
            }
        } else {
            LogUtils.i(TAG, "initData mEcg = null")
        }
    }

    private fun update() {
        viewModel.mEcgDataProcessing!!.init()
        lenght -= 1
        for (i in 0 until lenght) {
            val value = ecg_data?.get(i);
            val mEcgInfo = viewModel.getEcgToInfo((value)?.toInt()!!)
            val ecgValue = mEcgInfo.ecgData
            sendEcgDate(EcgUtils.getAllEcgDrawValue(ecgValue))
        }
    }


    var MSG_REPORT_DATA_ECG = 0x14
    private fun sendEcgDate(inputValue: Double) {
        val ecgValue: Double = inputValue
        val message = Message()
        message.what = MSG_REPORT_DATA_ECG
        message.arg2 = ecgValue.toInt()
        mHandler!!.sendMessage(message)
    }


    var pos = 0
    private var mHandler: Handler? = null
    private fun initHandler() {
        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                // TODO Auto-generated method stub
                when (msg.what) {
                    MSG_REPORT_DATA_ECG -> {
                        var value = msg.arg2
                        value = viewModel.getReportValue(value, pos)
                        if (pos <= lenght) {
                            binding.ecgReportECGAllView.setLinePoint(value.toFloat())
                        }
                        pos += 1;
                    }
                    else -> {}
                }
                super.handleMessage(msg)
            }
        }
    }


}