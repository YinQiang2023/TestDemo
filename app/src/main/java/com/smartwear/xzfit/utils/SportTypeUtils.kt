package com.smartwear.xzfit.utils

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.zhapp.ble.parsing.SportParsing
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import java.lang.Exception

/**
 * Created by Android on 2021/10/18.
 * 运动类型工具类
 */
object SportTypeUtils {
    /**
     * 根据数据类型、运动类型获取运动名称
     * */
    fun getSportTypeName(dataSources: Int, exerciseType: String): String {
        return when (dataSources) {
            0 -> {
                when (exerciseType.toInt()) {
                    0 -> {
                        BaseApplication.mContext.getString(R.string.sport_outdoor_sport)
                    }
                    1 -> {
                        BaseApplication.mContext.getString(R.string.more_sport_6)
                    }
                    2 -> {
                        BaseApplication.mContext.getString(R.string.more_sport_2)
                    }
                    else -> BaseApplication.mContext.getString(R.string.no_data_sign)
                }
            }
            1 -> {
                when (exerciseType.toInt()) {
                    0 -> {
                        BaseApplication.mContext.getString(R.string.sport_outdoor_sport)
                    }
                    1 -> {
                        BaseApplication.mContext.getString(R.string.more_sport_6)
                    }
                    2 -> {
                        BaseApplication.mContext.getString(R.string.more_sport_2)
                    }
                    else -> BaseApplication.mContext.getString(R.string.no_data_sign)
                }
            }
            2 -> {
                val languages: Array<String> =
                    BaseApplication.mContext.resources.getStringArray(R.array.more_sport_str)
                try {
                    if (SportParsing.isData5(exerciseType.toInt())) {
                        if (exerciseType.toInt() == 200) {
                            BaseApplication.mContext.getString(R.string.more_sport_200)
                        } else {
                            BaseApplication.mContext.getString(R.string.more_sport_201)
                        }
                    } else {
                        languages[exerciseType.toInt() - 1]
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    BaseApplication.mContext.getString(R.string.no_data_sign)
                }
            }
            else -> BaseApplication.mContext.getString(R.string.no_data_sign)
        }
    }

    /**
     * 根据数据类型、运动类型获取运动图标
     * */
    fun getSportTypeImg(dataSources: Int, exerciseType: String): Drawable {
        return when (dataSources) {
            0 -> {
                when (exerciseType.toInt()) {
                    0 -> {
                        ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_1)!!
                    }
                    1 -> {
                        ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_6)!!
                    }
                    2 -> {
                        ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_2)!!
                    }
                    else -> ContextCompat.getDrawable(
                        BaseApplication.mContext,
                        R.mipmap.more_sport_1
                    )!!
                }
            }
            1 -> {
                when (exerciseType.toInt()) {
                    0 -> {
                        ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_1)!!
                    }
                    1 -> {
                        ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_6)!!
                    }
                    2 -> {
                        ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_2)!!
                    }
                    else -> ContextCompat.getDrawable(
                        BaseApplication.mContext,
                        R.mipmap.more_sport_1
                    )!!
                }
            }
            2 -> {
                val iconList: TypedArray =
                    BaseApplication.mContext.getResources().obtainTypedArray(R.array.more_sport_img)
                val drawable: Drawable = try {
                    if (SportParsing.isData5(exerciseType.toInt())) {
                        if (exerciseType.toInt() == 200) {
                            ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_200)!!
                        } else {
                            ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_201)!!
                        }
                    } else {
                        ContextCompat.getDrawable(
                            BaseApplication.mContext,
                            iconList.getResourceId(exerciseType.toInt() - 1, 0)
                        )!!
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ContextCompat.getDrawable(
                        BaseApplication.mContext,
                        iconList.getResourceId(0, 0)
                    )!!
                }
                iconList.recycle() //用完后要调用recycle()方法回收
                return drawable
            }
            else -> ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.more_sport_1)!!
        }
    }

    /**
     * 根据数据类型、运动类型获取Strava活动类型
     * */
    fun getStravaActivityType(dataSources: Int, exerciseType: String): String {
        return when (dataSources) {
            0, 1 -> {
                when (exerciseType.toInt()) {
                    0 -> {
                        "Run"
                    }
                    1 -> {
                        "Ride"
                    }
                    2 -> {
                        "Walk"
                    }
                    else -> "Run"
                }
            }
            2 -> {
                when (exerciseType.toInt()) {
                    1 -> "run"              //户外跑步    Outdoor running
                    2 -> "walk"             //户外健走    Outdoor walking
                    4 -> "hike"             //登山    Trekking
                    5 -> "run"              //越野    Trail run
                    6 -> "ride"             //户外骑行    Outdoor cycling
                    13 -> "hike"            //户外徒步    Outdoor hiking
                    14 -> "run"             //小轮车    BMX
                    15 -> "run"             //打猎    Hunting
                    16 -> "sail"            //帆船运动    Sailing
                    17 -> "skateboard"      //滑板    Skateboarding
                    18 -> "inlineSkate"     //轮滑    Roller skating
                    19 -> "iceSkate"        //户外滑冰    Outdoorskating
                    20 -> "ride"            //马术    Equestrian
                    124 -> "ride"           //山地自行车    Mountain cycling
                    201 -> "swim"           //公开水域游泳-H    Open water
                    204 -> "run"            //铁人三项-H    Triathlon
                    else -> "Run"
                }
            }
            else -> "Run"
        }
    }
}