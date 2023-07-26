package com.jwei.xzfit.ui.region;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jwei.xzfit.R;

import java.util.ArrayList;
import java.util.List;

/**
 * author : ym
 * package_name : com.transsion.oraimohealth.widget.sortview
 * class_name : SortAdapter
 * description : 排序Adapter
 * time : 2021-12-09 18:20
 */
public class SortAdapter extends RecyclerView.Adapter<SortAdapter.SortViewHolder> {

    private LayoutInflater mInflater;
    private List<RegionBean> mDataList;
    private Context mContext;
    private RegionBean mSelectedItem;

    public SortAdapter(Context context, List<RegionBean> list) {
        mInflater = LayoutInflater.from(context);
        mDataList = list == null ? new ArrayList<>() : list;
        this.mContext = context;
    }

    @NonNull
    @Override
    public SortViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_sort, parent, false);
        SortViewHolder viewHolder = new SortViewHolder(view);
        viewHolder.tvLetter = view.findViewById(R.id.tv_letter);
        viewHolder.tvName = view.findViewById(R.id.name);
        viewHolder.ivChecked = view.findViewById(R.id.iv_check);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final SortViewHolder holder, final int position) {
        int section = getSectionForPosition(position);
        //如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
        if (position == getPositionForSection(section)) {
            holder.tvLetter.setVisibility(View.VISIBLE);
            holder.tvLetter.setText(mDataList.get(position).letters);
        } else {
            holder.tvLetter.setVisibility(View.GONE);
        }
        holder.ivChecked.setVisibility(
                mSelectedItem != null && TextUtils.equals(mSelectedItem.text, mDataList.get(position).text) ?
                        View.VISIBLE : View.GONE);
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(v ->
                    mOnItemClickListener.onItemClick(mDataList.get(position), position));
        }
        holder.tvName.setText(this.mDataList.get(position).text);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    /**
     * 点击监听
     */
    public interface OnItemClickListener<RegionBean> {
        void onItemClick(RegionBean t, int position);
    }

    private OnItemClickListener<RegionBean> mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener<RegionBean> mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    /**
     * 提供给Activity刷新数据
     *
     * @param list
     */
    public void updateList(List<RegionBean> list) {
        this.mDataList = list;
        notifyDataSetChanged();
    }

    public RegionBean getItem(int position) {
        return mDataList.get(position);
    }

    /**
     * 根据ListView的当前位置获取分类的首字母的char ascii值
     */
    public int getSectionForPosition(int position) {
        return mDataList.get(position).letters.charAt(0);
    }

    /**
     * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
     */
    public int getPositionForSection(int section) {
        for (int i = 0; i < getItemCount(); i++) {
            String sortStr = mDataList.get(i).letters;
            char firstChar = sortStr.toUpperCase().charAt(0);
            if (firstChar == section) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 设置选中的item
     *
     * @param t
     */
    public void setSelectedItem(RegionBean t) {
        mSelectedItem = t;
    }

    /**
     * author : ym
     * package_name : com.transsion.oraimohealth.widget.sortview
     * class_name : SortViewHolder
     * description : 排序ViewHolder
     * time : 2021-12-09 18:23
     */
    public class SortViewHolder extends RecyclerView.ViewHolder {

        TextView tvLetter, tvName;
        ImageView ivChecked;

        public SortViewHolder(View itemView) {
            super(itemView);
        }
    }
}
