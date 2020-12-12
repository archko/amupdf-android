package com.artifex.sonui.editor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class Slider extends View implements View.OnTouchListener
{
    public Slider(Context context) {
        super(context);
        init(null);
    }

    public Slider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Slider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public interface OnSliderEventListener {
        void onSlide(View v, float value);
        void onFinished(View v, float value);
    }

    private OnSliderEventListener mListener=null;
    public void setSliderEventListener(OnSliderEventListener eventListener) {
        mListener = eventListener;
    }

    private float min = 0f;
    private float max = 1f;
    private float current = 0.5f;

    private final Paint paint = new Paint();

    private int trackColor  = 0;
    private int trackWidth  = 1;
    private int thumbColor  = 0xffffffff;
    private int thumbRadius = 10;

    private boolean logarithmic = false;
    public void setLogarithmic(boolean val) {logarithmic=val;}

    private void init(AttributeSet attrs)
    {
        if (attrs!=null)
        {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.sodk_editor_Slider, 0, 0);

            try {
                trackColor = ta.getColor(R.styleable.sodk_editor_Slider_trackColor, 0);
                thumbColor = ta.getColor(R.styleable.sodk_editor_Slider_thumbColor, 0xffffffff);
                trackWidth = (int)ta.getDimension(R.styleable.sodk_editor_Slider_trackWidth, 1);
                thumbRadius = (int)ta.getDimension(R.styleable.sodk_editor_Slider_thumbRadius, 10);
            }
            finally {}
            ta.recycle();
        }

        setupLogScaling();

        setOnTouchListener(this);
    }

    public void setCurrent(float val)
    {
        current=val2pos(val);
        constrainValues();
        invalidate();
    }

    public void setParameters(float newMin, float newMax, float newCurrent)
    {
        min = newMin;
        max = newMax;
        current = val2pos(newCurrent);
        constrainValues();

        setupLogScaling();
    }

    private float minp;
    private float minv;
    private float scale;

    private void setupLogScaling()
    {
        minp = min;
        float maxp = max;

        // The result should be between 100 an 10000000
        minv = (float)Math.log(min);
        float maxv = (float) Math.log(max);

        // calculate adjustment factor
        scale = (maxv -minv) / (maxp -minp);
    }

    private void constrainValues()
    {
        if (current<min)
            current=min;
        if (current>max)
            current=max;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        int sliderWidth = getMeasuredWidth();
        int sliderHeight = getMeasuredHeight();

        int drawTrackColor = trackColor;
        int drawThumbColor = thumbColor;

        if (!isEnabled()) {
            drawTrackColor = Color.parseColor("#aaaaaa");
            drawThumbColor = Color.parseColor("#aaaaaa");
        }

        //  draw the line
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(trackWidth);
        paint.setColor(drawTrackColor);
        int y = sliderHeight/2;
        canvas.drawLine(0,y,sliderWidth,y,paint);

        //  draw the thumb
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(drawThumbColor);

        int x = thumbRadius + (int)((float)(sliderWidth-2*thumbRadius) * (current/(max-min)));
        canvas.drawCircle(x,y,thumbRadius,paint);
    }

    public boolean onTouch(View view, MotionEvent event)
    {
        if (!isEnabled())
            return true;

        int action = event.getAction();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            {
                float pct = (event.getX())/((float)getMeasuredWidth());
                current = min + pct*(max-min);
                constrainValues();
                invalidate();

                if (mListener!=null)
                    mListener.onSlide(this, pos2val(current));

                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            {
                if (mListener!=null)
                    mListener.onFinished(this, pos2val(current));
                break;
            }
        }

        //  we're the only thing handling touch events.
        return true;
    }

    private float pos2val(float p)
    {
        if (!logarithmic)
            return p;

        return (float)Math.exp(minv + scale*(current-minp));
    }

    private float val2pos(float v)
    {
        if (!logarithmic)
            return v;

        return ((float)Math.log(v)-minv) / scale + minp;
    }

}
