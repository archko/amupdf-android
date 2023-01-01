package com.baidu.ai.edge.ui.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created by ruanshimin on 2018/5/14.
 */

public class ResultListView extends ListView {
    public ResultListView(Context context) {
        super(context);
    }

    public ResultListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private Handler handler;

    public void setHandler(Handler mHandler) {
        handler = mHandler;
    }

    public void clear() {
        handler.post(() -> {
            removeAllViewsInLayout();
            invalidate();
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
