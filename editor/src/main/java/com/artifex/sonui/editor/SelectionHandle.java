package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

public class SelectionHandle extends View implements View.OnTouchListener
{
    //  two kinds
    public static final int KIND_START = 1;
    public static final int KIND_END = 2;
    private int mKind = 0;
    public int getKind() {return mKind;}

    //  for tracking movement
    private int mDragDeltaX;
    private int mDragDeltaY;
    private boolean mIsDragging = false;
    private int downX;
    private int downY;
    private int dragSlop = 0;

    //  the actual current position
    private int mPositionX = 0;
    private int mPositionY = 0;

    //  set whether the handle may draw itself.
    //  we don't draw while the page is being scrolled or scaled.
    private boolean mMayDraw = false;
    protected void setMayDraw(boolean val) {
        if (val != mMayDraw) {
            mMayDraw = val;
            invalidate();
        }
    }

    //  DragHandleListener
    public interface SelectionHandleListener {
        void onStartDrag(SelectionHandle handle);
        void onDrag(SelectionHandle handle);
        void onEndDrag(SelectionHandle handle);
    }
    private SelectionHandleListener mDragHandleListener;
    public void setSelectionHandleListener(SelectionHandleListener listener){mDragHandleListener=listener;}

    public SelectionHandle(Context context) {
        super(context);
        init(context);
    }

    public SelectionHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SelectionHandle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        //  set our touch listener
        setOnTouchListener(this);

        //  get the drag slop
        dragSlop = (int) context.getResources().getDimension(R.dimen.sodk_editor_drag_slop);

        setWillNotDraw(false);

        mKind = Integer.parseInt((String)getTag());

        //  scrolling causes new layout pass4s, which resets our offset values.
        //  so we need to set them back.
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                offsetLeftAndRight(mPositionX);
                offsetTopAndBottom(mPositionY);
                invalidate();
            }
        });

        moveTo(0, 0);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    public Point getPosition()
    {
        return new Point(mPositionX, mPositionY);
    }

    public void moveTo(int x, int y)
    {
        offsetLeftAndRight(x - mPositionX);
        offsetTopAndBottom(y - mPositionY);

        mPositionX = x;
        mPositionY = y;

        invalidate();
    }

    public void setPoint(int x, int y)
    {
        int w = getLayoutParams().width;
        int h = getLayoutParams().height;

        if (mKind==KIND_START)
        {
            moveTo(x-w/2, y-h);
        }
        else if (mKind==KIND_END)
        {
            moveTo(x-w/2, y);
        }
    }

    public Point getPoint()
    {
        Point p = new Point();
        int w = getLayoutParams().width;
        int h = getLayoutParams().height;

        if (mKind==KIND_START)
        {
            p.set(mPositionX+w/2, mPositionY+h);
        }
        else if (mKind==KIND_END)
        {
            p.set(mPositionX+w/2, mPositionY);
        }

        return p;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event)
    {
        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();

        //  handle this event if it's inside, or if we're dragging.
        Rect rect = new Rect();
        this.getGlobalVisibleRect(rect);
        if (!rect.contains(X, Y) && !mIsDragging)
            return false;

        final SelectionHandle theHandle = this;

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:
                Point position = getPosition();
                mDragDeltaX = X - position.x;
                mDragDeltaY = Y - position.y;
                downX = X;
                downY = Y;

                break;

            case MotionEvent.ACTION_UP:
                mIsDragging = false;

                //  tell the caller we're done dragging
                if (mDragHandleListener!=null)
                    mDragHandleListener.onEndDrag(theHandle);

                break;

            case MotionEvent.ACTION_MOVE:

                //  see if we've exceeded the drag slop
                if (!mIsDragging)
                {
                    int dx = Math.abs(X-downX);
                    int dy = Math.abs(Y-downY);
                    if (dx> dragSlop || dy> dragSlop)
                    {
                        //  slop exceeded.
                        mIsDragging = true;

                        //  tell the caller we've started dragging.
                        if (mDragHandleListener!=null)
                            mDragHandleListener.onStartDrag(theHandle);
                    }
                }

                //  move the handle in any case
                moveTo(X - mDragDeltaX, Y - mDragDeltaY);

                if (mIsDragging)
                {
                    //  tell the caller we're still dragging
                    if (mDragHandleListener!=null)
                        mDragHandleListener.onDrag(theHandle);
                }

                break;
        }
        return true;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (mMayDraw)
        {
            Paint paint = new Paint();

            paint.setColor(0xff8888ff);

            int w = getWidth();
            int h = getHeight();

            Path path = new Path();

            if (mKind == KIND_START)
            {
                path.moveTo(w/4, 0);
                path.lineTo(w/4, 2*h/3);
                path.lineTo(w/2, h);
                path.lineTo(3*w/4, 2*h/3);
                path.lineTo(3*w/4, 0);
                path.lineTo(w/4, 0);
                path.close();
            }
            else if (mKind == KIND_END)
            {
                path.moveTo(w/4, h);
                path.lineTo(w/4, h/3);
                path.lineTo(w/2, 0);
                path.lineTo(3*w/4, h/3);
                path.lineTo(3*w/4, h);
                path.lineTo(w/4, h);
                path.close();
            }

            canvas.drawPath(path, paint);
        }
    }
}
