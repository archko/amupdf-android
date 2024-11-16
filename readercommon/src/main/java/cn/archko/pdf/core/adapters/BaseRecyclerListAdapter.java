package cn.archko.pdf.core.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

/**
 * @author: archko 2024/11/16 :09:25
 */
public class BaseRecyclerListAdapter<T> extends ListAdapter<T, BaseViewHolder<T>> {

    protected Context context;
    protected final LayoutInflater inflater;

    protected BaseRecyclerListAdapter(Context context, DiffUtil.ItemCallback diffCallback) {
        super(diffCallback);
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<T> holder, int position) {
        if (getCurrentList().size() > position) {
            T task = getCurrentList().get(position);
            holder.onBind(task, position);
        }
    }
}
