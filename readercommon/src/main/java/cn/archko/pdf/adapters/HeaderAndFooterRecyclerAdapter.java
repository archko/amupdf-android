package cn.archko.pdf.adapters;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.List;

/**
 * @author: archko 2018/12/21 :17:16
 */
public abstract class HeaderAndFooterRecyclerAdapter<T> extends BaseRecyclerAdapter<T> {

    public static final int BASE_ITEM_TYPE_HEADER = 100000;
    public static final int BASE_ITEM_TYPE_FOOTER = 200000;

    private SparseArray<View> mHeaderViews;
    private SparseArray<View> mFootViews;

    public HeaderAndFooterRecyclerAdapter(Context context) {
        super(context);
        mHeaderViews = new SparseArray<>();
        mFootViews = new SparseArray<>();
    }

    public HeaderAndFooterRecyclerAdapter(Context context, List<T> data) {
        super(context, data);
        mHeaderViews = new SparseArray<>();
        mFootViews = new SparseArray<>();
    }

    @Override
    public final BaseViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mHeaderViews.get(viewType) != null) {
            return onCreateHeaderViewHolder(viewType);
        } else if (mFootViews.get(viewType) != null) {
            return onCreateFooterViewHolder(viewType);
        }
        return doCreateViewHolder(parent, viewType);
    }

    @NonNull
    protected BaseViewHolder<T> onCreateHeaderViewHolder(int viewType) {
        return new BaseViewHolder<T>(mHeaderViews.get(viewType));
    }

    @NonNull
    protected BaseViewHolder<T> onCreateFooterViewHolder(int viewType) {
        return new BaseViewHolder<T>(mFootViews.get(viewType));
    }

    /**
     * should be override
     *
     * @param parent
     * @param viewType
     * @return
     */
    public abstract BaseViewHolder<T> doCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public final void onBindViewHolder(BaseViewHolder holder, int position) {

        if (isHeaderViewPos(position)) {
            onBindHeaderViewHolder(holder, position);
            return;
        }
        if (isFooterViewPos(position)) {
            onBindFooterViewHolder(holder, position);
            return;
        }
        onBindNormalViewHolder(holder, position, position - getHeadersCount());

    }

    protected void onBindHeaderViewHolder(BaseViewHolder holder, int position) {
        // 供子类覆盖
    }

    protected void onBindFooterViewHolder(BaseViewHolder holder, int position) {
        // 供子类覆盖
    }

    protected void onBindNormalViewHolder(BaseViewHolder holder, int position, int realPosition) {
        super.onBindViewHolder(holder, realPosition);
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderViewPos(position)) {
            return mHeaderViews.keyAt(position);
        } else if (isFooterViewPos(position)) {
            return mFootViews.keyAt(position - getHeadersCount() - getNormalCount());
        }
        return doGetItemViewType(position - getHeadersCount());
    }

    public int doGetItemViewType(int position) {
        return super.getItemViewType(position - getHeadersCount());
    }

    @Override
    public int getItemCount() {
        return getHeadersCount() + getFootersCount() + getNormalCount();
    }

    public int getRealItemCount() {
        return getItemCount() - getHeadersCount() - getFootersCount();
    }

    /**
     * 处理 GridLayoutManager
     *
     * @param recyclerView
     */
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            final GridLayoutManager.SpanSizeLookup spanSizeLookup = gridLayoutManager.getSpanSizeLookup();

            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int viewType = getItemViewType(position);
                    if (mHeaderViews.get(viewType) != null) {
                        return gridLayoutManager.getSpanCount();
                    } else if (mFootViews.get(viewType) != null) {
                        return gridLayoutManager.getSpanCount();
                    }
                    if (spanSizeLookup != null) {
                        return spanSizeLookup.getSpanSize(position);
                    }
                    return 1;
                }
            });
            gridLayoutManager.setSpanCount(gridLayoutManager.getSpanCount());
        }
    }

    /**
     * 处理 StaggeredGridLayoutManager
     *
     * @param holder
     */
    @Override
    public void onViewAttachedToWindow(BaseViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getLayoutPosition();
        if (isHeaderViewPos(position) || isFooterViewPos(position)) {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }
    }

    public boolean isHeaderViewPos(int position) {
        return position < getHeadersCount();
    }

    public boolean isFooterViewPos(int position) {
        return position >= getHeadersCount() + getNormalCount();
    }

    public void addHeaderView(View view) {
        mHeaderViews.put(mHeaderViews.size() + BASE_ITEM_TYPE_HEADER, view);
    }

    public void addFootView(View view) {
        mFootViews.put(mFootViews.size() + BASE_ITEM_TYPE_FOOTER, view);
    }

    public SparseArray<View> getHeaderViews() {
        return mHeaderViews;
    }

    public SparseArray<View> getFootViews() {
        return mFootViews;
    }

    public int getHeadersCount() {
        return mHeaderViews.size();
    }

    public int getFootersCount() {
        return mFootViews.size();
    }

    public int getNormalCount() {
        return super.getItemCount();
    }
}
