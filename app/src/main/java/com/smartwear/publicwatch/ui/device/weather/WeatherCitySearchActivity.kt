package com.smartwear.publicwatch.ui.device.weather

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityWeatherCitySearchBinding
import com.smartwear.publicwatch.databinding.ItemWeatherSearchCityBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.expansion.disposeOnDestroy
import com.smartwear.publicwatch.ui.adapter.MultiItemCommonAdapter
import com.smartwear.publicwatch.ui.device.weather.bean.SearchCityEntity
import com.smartwear.publicwatch.ui.device.weather.utils.MapManagerUtils
import com.smartwear.publicwatch.ui.device.weather.utils.WeatherManagerUtils
import com.smartwear.publicwatch.ui.user.QAActivity
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.viewmodel.DeviceModel
import io.reactivex.rxkotlin.subscribeBy
import java.util.*


class WeatherCitySearchActivity : BaseActivity<ActivityWeatherCitySearchBinding, DeviceModel>(
    ActivityWeatherCitySearchBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private val TAG: String = WeatherCitySearchActivity::class.java.simpleName

    private var list = mutableListOf<SearchCityEntity>()
    private var itemAdapter: MultiItemCommonAdapter<
            SearchCityEntity,
            ItemWeatherSearchCityBinding
            >? = null

    private var locationCityName = ""
    private var locationCoordLon = ""
    private var locationCoordLat = ""

    //等待loading
    private var loadDialog: Dialog? = null

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.weather_city)
        loadDialog = DialogUtils.showLoad(this)
        itemAdapter = initAdapter()
        binding.rvWeather.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(this@WeatherCitySearchActivity)
        }

        setViewsClickListener(
            this,
            binding.ivSearch,
            binding.tvReLocate,
            binding.llCurrentPositioning,
            binding.tvHelp
        )
        positioning()

        binding.etSearch.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(
                v: TextView?,
                actionId: Int,
                event: KeyEvent?
            ): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearch()
                    return true
                }
                return false
            }
        })
        binding.tvHelp.paint.flags = Paint.UNDERLINE_TEXT_FLAG
        binding.etSearch.filters = ProhibitEmojiUtils.inputFilterProhibitChinese(50)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivSearch.id -> {
                onSearch()
            }
            binding.tvReLocate.id -> {
                positioning()
            }
            binding.llCurrentPositioning.id -> {
                if (locationCityName != "" && locationCoordLon != "" && locationCoordLat != "") {
                    SpUtils.getSPUtilsInstance().put(
                        SpUtils.WEATHER_LONGITUDE_LATITUDE,
                        "$locationCoordLon,$locationCoordLat"
                    )
                    SpUtils.getSPUtilsInstance().put(SpUtils.WEATHER_CITY_NAME, locationCityName)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            binding.tvHelp.id -> {
                startActivity(Intent(this, QAActivity::class.java))
            }
        }
    }

    private fun initAdapter(): MultiItemCommonAdapter<SearchCityEntity, ItemWeatherSearchCityBinding> {
        return object :
            MultiItemCommonAdapter<SearchCityEntity, ItemWeatherSearchCityBinding>(list) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemWeatherSearchCityBinding {
                return ItemWeatherSearchCityBinding.inflate(layoutInflater, parent, false)
            }

            @SuppressLint("SetTextI18n")
            override fun convert(bind: ItemWeatherSearchCityBinding, entity: SearchCityEntity, position: Int) {

/*                v.tvCityName.text = t.cityName
//                v.tvCountryName.text = t.countryName
//                v.tvTemperature.text = t.temperature
//                v.tvType.text = t.type
//                v.tvTemperatureRange.text = t.temperatureRange
*/
                bind.tvCityName.text = entity.name
                bind.tvType.text = "${entity.country},${entity.state}"
//                val map = ReflexUtils.objectToMap(entity.local_names)
//                val lang = Locale.getDefault().language.lowercase()
//                if (map.containsKey(lang)) {
//                    val value = map[Locale.getDefault().language.lowercase()].toString()
//                    if (value != "") {
//                        bind.tvCityName.text = value
//                    }
//                }

                bind.layoutItem.setOnClickListener {
                    SpUtils.getSPUtilsInstance().put(
                        SpUtils.WEATHER_LONGITUDE_LATITUDE,
                        "${entity.lon},${entity.lat}"
                    )

                    SpUtils.getSPUtilsInstance().put(
                        SpUtils.WEATHER_CITY_NAME,
                        entity.name
                    )
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                if ((position == (list.size - 1))) {
                    bind.viewLayout01.visibility = View.GONE
                } else {
                    bind.viewLayout01.visibility = View.VISIBLE
                }
            }

            override fun getItemType(t: SearchCityEntity): Int = 0
        }
    }

    //请求城市信息集合
    private fun requestCityList(city: String) {
        LogUtils.i(TAG, "requestCityList city = $city")
        WeatherManagerUtils.searchCity(city)
            .subscribeBy(
                onNext = {
                    LogUtils.i(TAG, "search city by name entity is $it")
                    list.clear()
                    if (it.isEmpty()) {
                        ToastUtils.showToast(getString(R.string.no_data))
                    } else {
                        list.addAll(it)
                    }
                    itemAdapter?.notifyDataSetChanged()
                },
                onComplete = {
                    LogUtils.w(TAG, "search city by name is complete")
                    dismissDialog()
                },
                onError = {
                    dismissDialog()
                    ToastUtils.showToast(resources.getString(R.string.err_network_tips))
                    LogUtils.w(TAG, "search city by name error and message is ${it.message}", true)
                }
            ).disposeOnDestroy(this)
    }

    //执行搜索
    private fun onSearch() {
        list.clear()
        itemAdapter?.notifyDataSetChanged()
        loadDialog?.show()
        requestCityList(binding.etSearch.text.toString().trim())
    }

    //请求定位
    private fun positioning() {
        Log.i(TAG, "positioning")
        PermissionUtils.checkRequestPermissions(
            this.lifecycle,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getString(R.string.permission_location_12) else getString(R.string.permission_location),
            PermissionUtils.PERMISSION_GROUP_LOCATION
        ) {
            binding.tvLocal.text = getString(R.string.weather_search_current_positioning)
            locationCityName = ""
            MapManagerUtils.getLatLon(object : MapManagerUtils.LocationListener {
                override fun onLocationChanged(gpsInfo: MapManagerUtils.GpsInfo?) {
                    MapManagerUtils.stopGps()
                    val longitude = gpsInfo?.longitude
                    val latitude = gpsInfo?.latitude

                    locationCoordLon = longitude.toString()
                    locationCoordLat = latitude.toString()

                    latitude?.let {
                        longitude?.let { it1 ->
                            WeatherManagerUtils.getCurrentWeather(
                                needUpdataCityInfo = true,
                                needReturnCityInfo = true,
                                oldLat = it,
                                oldLon = it1,
                                getOpenWeatherListener = object :
                                    WeatherManagerUtils.GetOpenWeatherListener {
                                    override fun onSuccess(cityName: String) {
                                        runOnUiThread {
                                            if (!TextStringUtils.isNull(cityName)) {
                                                //requestCityList(cityName)
                                                binding.tvLocal.text = cityName
                                                locationCityName = cityName
                                            }
                                        }
                                    }

                                    override fun onFail(msg: String) {
                                        runOnUiThread {
                                            binding.tvLocal.text = getString(R.string.locate_failure_tips)
                                            locationCityName = ""
                                        }
                                    }

                                })
                        }
                    }
                }

                override fun onFailure(msg: String) {
                    MapManagerUtils.stopGps()
                    binding.tvLocal.text = getString(R.string.locate_failure_tips)
                    locationCityName = ""
                    if (msg == "close") {
                        val dialog = DialogUtils.showDialogContentAndTwoBtn(
                            this@WeatherCitySearchActivity,
                            getString(R.string.locate_tips1),
                            getString(R.string.dialog_cancel_btn),
                            getString(R.string.dialog_confirm_btn),
                            object : DialogUtils.DialogClickListener {
                                override fun OnOK() {
                                    startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    )
                                }

                                override fun OnCancel() {}
                            })
                        dialog.show()
                    }
                }
            }, true)
        }
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(loadDialog)
    }
}