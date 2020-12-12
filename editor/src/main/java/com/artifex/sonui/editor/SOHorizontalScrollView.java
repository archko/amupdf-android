package com.artifex.sonui.editor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.HorizontalScrollView;

public class SOHorizontalScrollView extends HorizontalScrollView implements View.OnTouchListener
{
    //  keep track of whether we should show the indicators at either end.
    //  these get updated at appropriate times.
    private boolean mShowLeftIndicator = false;
    private boolean mShowRightIndicator = false;

    //  width of the indicator in inches or pixels.
    private int mIndicatorWidthPixels;
    private float mIndicatorWidthInches = 0.2f;

    //  use this to test how close we are to either end.
    private int mIndicatorMargin = Utilities.convertDpToPixel(5);

    //  drawables of the indicators.
    private Drawable mLeftIndicator;
    private Drawable mRightIndicator;

    //  stuff for animation
    private int mAnimateDistance;
    private ViewPropertyAnimator mAnimator;
    private static final int DURATION  = 1000;
    private int mAnimationAmount;

    public SOHorizontalScrollView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        //  watch scrolling
        setOnTouchListener(this);

        //  set up the indicators
        mLeftIndicator = ContextCompat.getDrawable(context, R.drawable.sodk_editor_scrollind_left);
        mRightIndicator = ContextCompat.getDrawable(context, R.drawable.sodk_editor_scrollind_right);

        //  calculate the indicator width
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        mIndicatorWidthPixels = (int)(metrics.densityDpi * mIndicatorWidthInches);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        //  this function is called while the user is scrolling
        //  so we use it to revisit whether we should show the indicators
        updateIndicatorVisibility();

        //  we're not handling touch, just watching.
        return false;
    }

    private int getChildWidth()
    {
        //  get some measurements.
        //  de-scale the child width if we're on a phone
        View child = getChildAt(0);
        int childWidth = child.getWidth();
        if (Utilities.isPhoneDevice(getContext())) {
            float scale = child.getScaleX();
            childWidth = (int)(childWidth*scale);
        }
        return childWidth;
    }

    private int getParentWidth()
    {
        View parent = (View)getParent();
        int parentWidth = parent.getWidth();
        return parentWidth;
    }

    private void updateIndicatorVisibility()
    {
        //  get some measurements.
        int childWidth = getChildWidth();
        int x = getScrollX();
        int parentWidth = getParentWidth();

        //  now determine visibility
        mShowLeftIndicator = (x >= mIndicatorMargin);
        mShowRightIndicator = ((parentWidth+x+ mIndicatorMargin) < childWidth);
    }

    @Override
    public void onDrawForeground(Canvas canvas)
    {
        //  this function is called to draw on top of our view's contents
        //  we use it to draw the indicators

        //  don't draw the indicators while we're animating.
        if (mAnimator != null)
            return;

        updateIndicatorVisibility();

        int height = getHeight();
        View parent = (View)getParent();
        int parentWidth = parent.getWidth();
        int x = getScrollX();

        if (mShowLeftIndicator)
        {
            Rect rectangle = new Rect(x, 0, x+ mIndicatorWidthPixels, height);
            mLeftIndicator.setBounds(rectangle);
            mLeftIndicator.draw(canvas);
        }

        if (mShowRightIndicator)
        {
            Rect rectangle = new Rect(parentWidth+x- mIndicatorWidthPixels, 0, parentWidth+x, height);
            mRightIndicator.setBounds(rectangle);
            mRightIndicator.draw(canvas);
        }
    }

    public boolean mayAnimate()
    {
        //  we're a candidate for animation if scrolling is necessary to see everything.
        return (getChildWidth() > getParentWidth());
    }

    public void startAnimate()
    {
        //  this is the amount we'll scroll back and forth.
        mAnimationAmount = getChildWidth() - getParentWidth();
        mAnimationAmount = Math.min(mAnimationAmount, 500);

        //  create the animator
        mAnimator = animate();

        //  set initial conditions, and then go.
        mAnimateDistance = -mAnimationAmount;
        setTranslationX(0);
        doAnimate();
    }

    private void doAnimate()
    {
        if (mAnimator!=null)  //  check if we're still animating
        {
            // do a new animation
            mAnimator.translationX(mAnimateDistance)
                    .setDuration(DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            //  animation is done, reset the distance and do it again.
                            mAnimateDistance = -mAnimationAmount - mAnimateDistance;
                            doAnimate();
                        }
                    });
        }
    }

    public void stopAnimate()
    {
        //  kill existing animation
        if (mAnimator != null)
            mAnimator.cancel();
        mAnimator = null;

        //  restore the position.
        setTranslationX(0);
    }
}
