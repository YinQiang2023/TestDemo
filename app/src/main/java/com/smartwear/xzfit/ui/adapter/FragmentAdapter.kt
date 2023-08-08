package com.smartwear.xzfit.ui.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

@SuppressLint("WrongConstant")
open class FragmentAdapter(
    fm: FragmentManager,
    private val list: List<Fragment>
) : FragmentPagerAdapter(fm, FragmentPagerAdapter.BEHAVIOR_SET_USER_VISIBLE_HINT) {
    override fun getItem(arg0: Int): Fragment {
        // TODO Auto-generated method stub
        return list[arg0]
    }

    override fun getCount(): Int {
        // TODO Auto-generated method stub
        return if (list.isEmpty()) 0 else list.size
    }

}