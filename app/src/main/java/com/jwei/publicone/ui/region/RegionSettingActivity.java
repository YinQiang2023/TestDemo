package com.jwei.publicone.ui.region;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jwei.publicone.R;
import com.jwei.publicone.base.BaseApplication;
import com.jwei.publicone.ui.view.SideBar;
import com.jwei.publicone.utils.PinyinUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * author : ym
 * package_name : com.transsion.oraimohealth.module.account.activity
 * class_name : RegionSettingActivity
 * description :
 * time : 2021-11-04 16:05
 */
public class RegionSettingActivity extends AppCompatActivity {

    public static int KEY_TAG = 101;
    public static final String KEY_REGION = "key_region";
    public static final String KEY_IS_SETTING_CODE = "key_is_setting_code";
    private TextView tvTitle;
    private LinearLayout mLayoutCurrent;
    private TextView mTvCenter;
    private RecyclerView mRvRegion;
    private RegionBean mRegion;
    private SortAdapter mAdapter;
    private boolean mIsSettingCode;
    private SideBar mSideBar;
    private EditText mEtSearch;
    private List<RegionBean> mRegionList;
    private LetterComparator mLetterComparator;
    private AppCompatImageView mIvDelete;
    private TextWatcherImpl mTextWatcher;

    private ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_region_setting);
        initContentViews();
        initData();
        initEvent();
    }


    protected void initContentViews() {
        tvTitle = findViewById(R.id.tvTitle2);
        tvTitle.setVisibility(VISIBLE);
        tvTitle.setText(getString(R.string.select_region_title));
        mLayoutCurrent = findViewById(R.id.layout_current);
        mRvRegion = findViewById(R.id.rv_region);
        mSideBar = findViewById(R.id.side_bar);
        mTvCenter = findViewById(R.id.tv_center);
        mEtSearch = findViewById(R.id.et_search);
        mIvDelete = findViewById(R.id.iv_delete);
        constraintLayout = findViewById(R.id.noData);
        findViewById(R.id.tvTitle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    protected void initData() {
        Intent intent = getIntent();
        mRegion = (RegionBean) intent.getSerializableExtra(KEY_REGION);
        mIsSettingCode = intent.getBooleanExtra(KEY_IS_SETTING_CODE, false);
        mRegionList = getRegionList();
    }

    private List<RegionBean> mRegionBeans;

    /**
     * 获取区域列表
     *
     * @return
     */
    public List<RegionBean> getRegionList() {
        if (mRegionBeans == null || mRegionBeans.isEmpty()) {
            mRegionBeans = formatArray2RegionList(BaseApplication.getMContext().getResources().getStringArray(R.array.region_array));
        }
        return mRegionBeans;
    }

    public static String getRegionName(String countryCode, String areaCode) {
        String result = "";
        List<RegionBean> mRegionBeans = formatArray2RegionList(BaseApplication.getMContext().getResources().getStringArray(R.array.region_array));
        if (mRegionBeans.size() > 0) {
            for (int i = 0; i < mRegionBeans.size(); i++) {
                RegionBean mRegionBean = mRegionBeans.get(i);

                if (mRegionBean.countryIsoCode.equals(countryCode) && mRegionBean.areaCode.equals(areaCode)) {
                    result = mRegionBean.name;
                    break;

                }
            }
        }
        return result;
    }

    public static boolean isChinaServiceUrl(String countryCode, String areaCode) {
        boolean result = false;

        String countryCode1 = "86";
        String countryCode2 = "852";
        String countryCode3 = "853";
        String countryCode4 = "886";

        String areaCode1 = "cn";
        String areaCode2 = "hk";
        String areaCode3 = "mo";
        String areaCode4 = "tw";

        if ((countryCode1.equals(areaCode) && areaCode1.equals(countryCode))
                || (countryCode2.equals(areaCode) && areaCode2.equals(countryCode))
                || (countryCode3.equals(areaCode) && areaCode3.equals(countryCode))
                || (countryCode4.equals(areaCode) && areaCode4.equals(countryCode))
        ) {
            result = true;
        }

        return result;
    }

    /**
     * 将数组转为区域列表
     *
     * @param array
     * @return
     */
    public static List<RegionBean> formatArray2RegionList(String[] array) {
        List<RegionBean> mSortList = new ArrayList<>();
        for (String s : array) {
            if (TextUtils.isEmpty(s)) {
                continue;
            }
            String[] tempArr = s.split("_");
            if (tempArr.length != 3) {
                continue;
            }
            RegionBean bean = new RegionBean(tempArr[0], tempArr[1], tempArr[2]);
            bean.text = bean.name;
            //汉字转换成拼音
            String pinyin = PinyinUtils.getPingYin(bean.text);
            String sortString = pinyin.substring(0, 1).toUpperCase();
            // 正则表达式，判断首字母是否是英文字母
            if (sortString.matches("[A-Z]")) {
                bean.letters = sortString.toUpperCase();
            } else {
                bean.letters = "#";
            }
            mSortList.add(bean);
        }
        return mSortList;
    }

    protected void initEvent() {
        if (mLetterComparator == null) {
            mLetterComparator = new LetterComparator();
        }
        //initTitle(getString(R.string.account_country_region));
        mLayoutCurrent.setVisibility(mIsSettingCode ? GONE : VISIBLE);
        initRecyclerView();
        mEtSearch.setFilters(new InputFilter[]{new EmojiFilter(), new EnterFilter()});
        mTextWatcher = new TextWatcherImpl() {
            @Override
            public void afterTextChanged(String text) {
                super.afterTextChanged(text);
                mIvDelete.setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
                filterData(text);
            }
        };
        mEtSearch.addTextChangedListener(mTextWatcher);
        mSideBar.setVisibility(isZhOrEn() ? VISIBLE : GONE);
        mIvDelete.setOnClickListener(v -> {
            mEtSearch.setText("");
            mIvDelete.setVisibility(GONE);
        });
    }

    /**
     * 是否为中文或英文
     *
     * @return
     */
    public boolean isZhOrEn() {
        String language = Locale.getDefault().getLanguage();
        return TextUtils.equals(language, Locale.CHINA.getLanguage()) || TextUtils.equals(language, Locale.ENGLISH.getLanguage());
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRvRegion.setLayoutManager(layoutManager);
        // 根据a-z进行排序源数据
        Collections.sort(mRegionList, mLetterComparator);
        mAdapter = new SortAdapter(this, mRegionList);
        mAdapter.setSelectedItem(mRegion);
        mRvRegion.setAdapter(mAdapter);
        mSideBar.setTextView(mTvCenter);
        mSideBar.setOnTouchingLetterChangedListener(s -> {
            //该字母首次出现的位置
            int position = mAdapter.getPositionForSection(s.charAt(0));
            if (position >= 0) {
                layoutManager.scrollToPositionWithOffset(position, 0);
            }
        });
        mAdapter.setOnItemClickListener((regionBean, position) -> {
            setResult(RESULT_OK, new Intent().putExtra(KEY_REGION, regionBean));
            finishAfterTransition();
        });
    }

    /**
     * 根据输入框中的值来过滤数据并更新RecyclerView
     *
     * @param filterStr
     */
    private void filterData(String filterStr) {
        List<RegionBean> filterDateList = new ArrayList<>();
        if (TextUtils.isEmpty(filterStr)) {
            filterDateList = mRegionList;
        } else {
            for (RegionBean bean : mRegionList) {
                String name = bean.text;
                if (name.contains(filterStr) || PinyinUtils.getFirstSpell(name).startsWith(filterStr) ||
                        PinyinUtils.getFirstSpell(name).toLowerCase().startsWith(filterStr.toLowerCase()) ||
                        bean.areaCode.contains(filterStr)) {
                    filterDateList.add(bean);
                }
            }
        }
        // 根据a-z进行排序
        Collections.sort(filterDateList, mLetterComparator);
        mAdapter.updateList(filterDateList);
        //更新滑轮
        if (!filterDateList.isEmpty()) {
            ArrayList<String> sideData = new ArrayList<>();
            for (RegionBean item : filterDateList) {
                if (!sideData.contains(item.letters)) {
                    sideData.add(item.letters);
                }
            }
            String[] data = new String[sideData.size()];
            for (int i = 0; i < sideData.size(); i++) {
                data[i] = sideData.get(i);
            }
            mSideBar.setSideData(data);
        }
        constraintLayout.setVisibility(filterDateList.isEmpty() ? VISIBLE : GONE);
        mSideBar.setVisibility(filterDateList.isEmpty() ? GONE : VISIBLE);
    }

    @Override
    protected void onDestroy() {
        if (mTextWatcher != null) {
            mEtSearch.removeTextChangedListener(mTextWatcher);
            mTextWatcher = null;
        }
        super.onDestroy();
    }
}
