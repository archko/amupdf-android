package cn.archko.pdf.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 招聘所有列表的适配器,用于替换原来的absadapter,list取代了vector
 * 现在作为超类.absadapter不再使用.
 *
 * @author archko 2016-1-1
 */
public abstract class ListBaseAdapter<T> extends BaseAdapter {

    protected Context mContext;
    protected LayoutInflater mInflater;
    private List<T> mContent;

    public ListBaseAdapter(Context context) {
        this(context, null);
    }

    public ListBaseAdapter(Context context, List<T> arrayList) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        setData(arrayList);
    }

    /**
     * 设置数据
     */
    public void setData(List<T> arrayList) {
        mContent = arrayList;
        if (null == mContent) {
            mContent = new ArrayList<T>();
        }
        notifyDataSetChanged();
    }

    public void addData(List arrayList) {
        if (mContent == null) {
            mContent = new ArrayList();
        }
        if (null != arrayList) {
            mContent.addAll(arrayList);
        }
        notifyDataSetChanged();
    }

    /**
     * 获得数据
     */
    public List<T> getData() {
        if (null == mContent) {
            mContent = new ArrayList<>();
        }
        return mContent;
    }

    @Override
    public int getCount() {
        return mContent.size();
    }

    @Override
    public Object getItem(int position) {
        return mContent.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BaseViewHolder vh;
        if (convertView == null) {
            vh = onCreateViewHolder(parent, getItemViewType(position));
            convertView = vh.itemView;
            convertView.setTag(vh);
        } else {
            vh = (BaseViewHolder) convertView.getTag();
        }
        onBindViewHolder(vh, position);

        return convertView;
    }

    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    public void onBindViewHolder(BaseViewHolder holder, int position) {
        T task = mContent.get(position);
        holder.onBind(task, position);
    }
}
