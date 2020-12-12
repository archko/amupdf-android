package cn.archko.pdf.adapters;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView公共的适配器
 *
 * @author: archko 2016/12/2 :18:19
 */

public abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<BaseViewHolder> {

    protected final LayoutInflater mInflater;
    protected List<T> mData;

    public BaseRecyclerAdapter(Context context) {
        this(context, null);
    }

    public BaseRecyclerAdapter(Context context, List<T> data) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mData = data;
        if (null == mData) {
            mData = new ArrayList<>();
        }
    }

    public LayoutInflater getInflater() {
        return mInflater;
    }

    public void setData(List<T> data) {
        this.mData = data;
        if (mData == null) {
            mData = new ArrayList<>();
        }
    }

    public void addData(List<T> arrayList) {
        if (mData == null) {
            mData = new ArrayList();
        }
        if (null != arrayList) {
            mData.addAll(arrayList);
        }
    }

    public List<T> getData() {
        return mData;
    }

    /*@Override
    public GJBaseRecyclerViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
        GJBaseRecyclerViewHolder holder = null;
        View view = mInflater.inflate(R.layout.item_simple_text, null);

        holder = new GJBaseRecyclerViewHolder(view);

        return holder;
    }*/

    @Override
    public void onBindViewHolder(BaseViewHolder viewHolder, final int position) {
        T task = mData.get(position);
        viewHolder.onBind(task, position);
    }

    @Override
    public int getItemCount() {
        int size = mData == null ? 0 : mData.size();
        return size;
    }
}
