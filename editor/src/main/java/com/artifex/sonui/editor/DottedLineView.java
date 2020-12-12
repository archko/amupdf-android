package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class DottedLineView extends View
{
    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();
    private int lineWidthPx;
    private float mPattern[];

    public DottedLineView(Context context) {
        super(context);
        init();
    }

    public DottedLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DottedLineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setPattern(float pattern[])
    {
        mPattern = new float[pattern.length];
        for (int i=0; i<pattern.length; i++)
            mPattern[i] = pattern[i]*lineWidthPx;

        mPaint.setPathEffect(new DashPathEffect(mPattern, 0));
        invalidate();
    }

    private void init()
    {
        setWillNotDraw(false);

        mPaint.setStyle(Paint.Style.STROKE);

        //  stroke width
        int dp = 6;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        lineWidthPx = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        mPaint.setStrokeWidth(lineWidthPx);

        mPaint.setColor(Color.BLACK);
        mPaint.setARGB(255, 0, 0,0);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        int measuredWidth = getMeasuredWidth();

        //  reduce the path length to only show whole patterns
        float sum = 0;
        for (int i=0; i<mPattern.length; i++)
            sum += mPattern[i];
        int n = measuredWidth / ((int)sum);
        int w = ((int)sum) * n;

        mPath.moveTo(0, 0);
        mPath.lineTo(w, 0);
        canvas.drawPath(mPath, mPaint);
    }


}
