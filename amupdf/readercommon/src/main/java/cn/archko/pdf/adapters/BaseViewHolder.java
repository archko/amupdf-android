package cn.archko.pdf.adapters;

import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

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
