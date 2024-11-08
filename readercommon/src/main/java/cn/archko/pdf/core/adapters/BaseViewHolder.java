package cn.archko.pdf.core.adapters;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView和ListView通用ViewHolder
 *
 * @author: archko 2016/12/2 :18:19
 */
public class BaseViewHolder<T> extends RecyclerView.ViewHolder {

    public BaseViewHolder(View itemView) {
        super(itemView);
    }

    public void onBind(final T data, int position) {
    }
}
