package cn.archko.pdf.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import cn.archko.pdf.common.R;

/**
 * @author: archko 2024/3/12 :19:18
 */
public class ColorItemDecoration extends RecyclerView.ItemDecoration {

    private int dividerHeight = 1;
    private Paint dividerPaint;

    private int color = Color.parseColor("#33000000");

    public void setColor(int color) {
        this.color = color;
        dividerPaint.setColor(color);
    }

    public void setDividerHeight(int dividerHeight) {
        this.dividerHeight = dividerHeight;
    }

    public ColorItemDecoration(Context context) {
        dividerPaint = new Paint();
        dividerPaint.setColor(color);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.bottom = dividerHeight;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getChildCount();
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        for (int i = 0; i < childCount - 1; i++) {
            View view = parent.getChildAt(i);
            float top = view.getBottom();
            float bottom = view.getBottom() + dividerHeight;
            c.drawRect(left, top, right, bottom, dividerPaint);
        }
    }
}