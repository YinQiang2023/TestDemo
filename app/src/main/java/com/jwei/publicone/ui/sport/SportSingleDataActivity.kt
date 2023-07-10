package com.jwei.publicone.ui.sport

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ThreadUtils
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivitySportSingleDataBinding
import com.jwei.publicone.databinding.ItemSportSingleDataBinding
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.ui.sport.bean.SportSingleDataBean
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.TimeUtils
import com.jwei.publicone.viewmodel.SportModel
import com.zhapp.ble.utils.UnitConversionUtils
import kotlin.math.roundToInt

/**
 * Created by Android on 2021/10/9.
 * 运动单个数据查看
 */
class SportSingleDataActivity : BaseActivity<ActivitySportSingleDataBinding, SportModel>(
    ActivitySportSingleDataBinding::inflate, SportModel::class.java
) {

    //private var mSportType = Global.SPORT_SINGLE_DATA_TYPE_TIME
    private var mItemData: SportSingleDataBean? = null

    private var mDatas = mutableListOf<SportSingleDataBean>()

    //数据观察者
    private lateinit var mTimeObserver: Observer<Long>
    private lateinit var mDistanceObserver: Observer<Float>
    private lateinit var mSpeedObserver: Observer<Float>
    private lateinit var mMinkmObserver: Observer<String>
    private lateinit var mCaloriesObserver: Observer<Float>
    //TODO 心率
    //TODO 心跳
    //TODO 步频
    //TODO 步数


    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    //region initView
    override fun initView() {
        super.initView()
        //mSportType = intent.getIntExtra("type",Global.SPORT_SINGLE_DATA_TYPE_TIME)
        mItemData = intent.getParcelableExtra<SportSingleDataBean>("data")
        if (mItemData == null) {
            finish()
            return
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@SportSingleDataActivity, 3)
            setHasFixedSize(true)
            adapter = initAdapter()
            //上边距
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = ConvertUtils.dp2px(54F)
                }
            })
        }
    }
    //endregion

    //region data
    override fun initData() {
        super.initData()

        /*if(Global.IS_DEVICE_CONNECTED){
            //TODO 有设备时的情况
        }else{

        }*/

        mDatas.apply {
            add(
                SportSingleDataBean(
                    1,
                    Global.SPORT_SINGLE_DATA_TYPE_TIME,
                    getString(R.string.sport_time),
                    getString(R.string.sport_data_type_time),
                    "",
                    getString(R.string.sport_data_type_time),
                    R.drawable.sport_time_sl
                )
            )
            add(
                SportSingleDataBean(
                    -1, Global.SPORT_SINGLE_DATA_TYPE_DISTANCE,
                    getString(R.string.healthy_sports_list_distance),
                    getString(R.string.healthy_sports_list_distance),
                    "",
                    if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                        R.string.unit_distance_1
                    ),
                    R.drawable.sport_distance_sl
                )
            )
            add(
                SportSingleDataBean(
                    -1, Global.SPORT_SINGLE_DATA_TYPE_SPEED,
                    StringBuilder().append(getString(R.string.sport_data_type_speed))
                        .append("(")
                        .append(
                            StringBuilder().append(
                                if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                                    R.string.unit_distance_1
                                )
                            )
                                .append("/")
                                .append(getString(R.string.h))
                                .toString()
                        )
                        .append(")")
                        .toString(),
                    getString(R.string.sport_data_type_speed),
                    "",
                    StringBuilder().append(
                        if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                            R.string.unit_distance_1
                        )
                    )
                        .append("/")
                        .append(getString(R.string.h))
                        .toString(),
                    R.drawable.sport_speed_sl
                )
            )
            add(
                SportSingleDataBean(
                    -1, Global.SPORT_SINGLE_DATA_TYPE_CALORIES,
                    StringBuilder().append(getString(R.string.healthy_sports_list_calories))
                        .append("(")
                        .append(getString(R.string.unit_calories))
                        .append(")")
                        .toString(),
                    getString(R.string.healthy_sports_list_calories),
                    "",
                    getString(R.string.unit_calories),
                    R.drawable.sport_calories_sl
                )
            )
            add(
                SportSingleDataBean(
                    -1, Global.SPORT_SINGLE_DATA_TYPE_MINKM,
                    getString(R.string.sport_minkm),
                    getString(R.string.sport_minkm),
                    "",
                    StringBuilder().append(getString(R.string.h))
                        .append("/")
                        .append(
                            if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                                R.string.unit_distance_1
                            )
                        )
                        .toString(),
                    R.drawable.sport_minkm_sl
                )
            )
        }


        mDatas.findLast { it.dataType == mItemData?.dataType }?.let {
            it.isChecked = true
            //it.index = mItemData!!.index
            binding.recyclerView.adapter?.notifyItemChanged(mDatas.indexOf(it))
        }

        //选项改变
        viewModel.sportDataType.observe(this, {
            removeAllObserver()
            //region 观察者初始化
            if (!::mTimeObserver.isInitialized) {
                mTimeObserver = object : Observer<Long> {
                    override fun onChanged(t: Long?) {
                        t?.let { binding.tvData.text = TimeUtils.millis2String(t) }
                    }
                }
            }
            if (!::mDistanceObserver.isInitialized) {
                mDistanceObserver = object : Observer<Float> {
                    override fun onChanged(t: Float?) {
                        t?.let {
                            val value = if (AppUtils.getDeviceUnit() == 0) {
                                UnitConversionUtils.bigDecimalFormat(t / 1000f)
                            } else {
                                UnitConversionUtils.bigDecimalFormat(t / 1000f / 1.61f)
                            }
                            binding.tvData.text = value
                        }
                    }
                }
            }
            if (!::mSpeedObserver.isInitialized) {
                mSpeedObserver = object : Observer<Float> {
                    override fun onChanged(t: Float?) {
                        t?.let { binding.tvData.text = UnitConversionUtils.bigDecimalFormat((t)) }
                    }
                }
            }
            if (!::mMinkmObserver.isInitialized) {
                mMinkmObserver = object : Observer<String> {
                    override fun onChanged(t: String?) {
                        t?.let { binding.tvData.text = t }
                    }
                }
            }
            if (!::mCaloriesObserver.isInitialized) {
                mCaloriesObserver = object : Observer<Float> {
                    override fun onChanged(t: Float?) {
                        t?.let { binding.tvData.text = t.roundToInt().toString() }
                    }
                }
            }
            //TODO 心率
            //TODO 心跳
            //TODO 步频
            //TODO 步数
            //endregion
            binding.tvUnit.text = mDatas.find { item -> item.dataType == it }?.unit

            when (it) {
                Global.SPORT_SINGLE_DATA_TYPE_TIME -> {
                    viewModel.sportLiveData.getSportTime().observe(this, mTimeObserver)
                    viewModel.sportLiveData.getSportTime()
                        .postValue(viewModel.sportLiveData.getSportTime().value)
                }
                Global.SPORT_SINGLE_DATA_TYPE_DISTANCE -> {
                    viewModel.sportLiveData.getSportDistance().observe(this, mDistanceObserver)
                    viewModel.sportLiveData.getSportDistance()
                        .postValue(viewModel.sportLiveData.getSportDistance().value)
                }
                Global.SPORT_SINGLE_DATA_TYPE_SPEED -> {
                    viewModel.sportLiveData.getSportSpeed().observe(this, mSpeedObserver)
                    viewModel.sportLiveData.getSportSpeed()
                        .postValue(viewModel.sportLiveData.getSportSpeed().value)
                }
                Global.SPORT_SINGLE_DATA_TYPE_MINKM -> {
                    viewModel.sportLiveData.getSportMinkm().observe(this, mMinkmObserver)
                    viewModel.sportLiveData.getSportMinkm()
                        .postValue(viewModel.sportLiveData.getSportMinkm().value)
                }
                Global.SPORT_SINGLE_DATA_TYPE_CALORIES -> {
                    viewModel.sportLiveData.getCalories().observe(this, mCaloriesObserver)
                    viewModel.sportLiveData.getCalories()
                        .postValue(viewModel.sportLiveData.getCalories().value)
                }
                Global.SPORT_SINGLE_DATA_TYPE_HEART_RATE -> {
                    //binding.tvUnit.text = getString(R.string.unit_heart)
                    //TODO 心率
                }
                Global.SPORT_SINGLE_DATA_TYPE_STEP_RATE -> {
                    //binding.tvUnit.text = getString(R.string.unit_step_reta)
                    //TODO 步频
                }
                Global.SPORT_SINGLE_DATA_TYPE_STEP_NUM -> {
                    //binding.tvUnit.text = getString(R.string.unit_step)
                    //TODO 步数
                }
                else -> {
                    binding.tvUnit.text = getString(R.string.sport_data_type_time)
                    viewModel.sportLiveData.getSportTime().observe(this, mTimeObserver)
                }
            }
        })

        AppUtils.tryBlock {
            viewModel.changeSingleDataType(mItemData!!.dataType)
        }

    }

    /**
     * 移除所有数据观察者
     * */
    private fun removeAllObserver() {
        if (::mTimeObserver.isInitialized) {
            //viewModel.sportTime.removeObserver(mTimeObserver)
            viewModel.sportLiveData.getSportTime().removeObserver(mTimeObserver)
        }
        if (::mDistanceObserver.isInitialized) {
            viewModel.sportLiveData.getSportDistance().removeObserver(mDistanceObserver)
        }
        if (::mSpeedObserver.isInitialized) {
            viewModel.sportLiveData.getSportSpeed().removeObserver(mSpeedObserver)
        }
        if (::mMinkmObserver.isInitialized) {
            viewModel.sportLiveData.getSportMinkm().removeObserver(mMinkmObserver)
        }
        if (::mCaloriesObserver.isInitialized) {
            viewModel.sportLiveData.getCalories().removeObserver(mCaloriesObserver)
        }

    }
    //endregion

    private fun initAdapter(): CommonAdapter<SportSingleDataBean, ItemSportSingleDataBinding> {
        return object : CommonAdapter<SportSingleDataBean, ItemSportSingleDataBinding>(mDatas) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemSportSingleDataBinding {
                return ItemSportSingleDataBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(
                v: ItemSportSingleDataBinding,
                t: SportSingleDataBean,
                position: Int
            ) {
                v.tvName.text = t.title
                v.cbIcon.setLeftDrawable(t.imgId)
                v.cbIcon.isChecked = t.isChecked

                v.cbIcon.setOnClickListener {
                    v.cbIcon.isChecked = !v.cbIcon.isChecked
                    mDatas.filter { it.dataType != t.dataType }.forEach { it.isChecked = false }
                    t.isChecked = true
                    notifyDataSetChanged()
                    viewModel.changeSingleDataType(t.dataType)

                    ThreadUtils.runOnUiThreadDelayed({
                        t.index = mItemData!!.index
                        val intent = intent!!
                        intent.putExtra("data", t)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }, 100)
                }
            }
        }
    }

}