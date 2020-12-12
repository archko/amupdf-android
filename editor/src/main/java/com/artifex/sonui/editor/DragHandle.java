package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class DragHandle extends FrameLayout implements View.OnTouchListener
{
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

    //  DragHandleListener
    private DragHandleListener mDragHandleListener;

    //  kinds for selection
    public static final int SELECTION_TOP_LEFT = 1;
    public static final int SELECTION_BOTTOM_RIGHT = 2;

    //  kinds for resizing
    public static final int RESIZE_TOP_LEFT = 3;
    public static final int RESIZE_TOP_RIGHT = 4;
    public static final int RESIZE_BOTTOM_LEFT = 5;
    public static final int RESIZE_BOTTOM_RIGHT = 6;

    //  kind for dragging and rotating
    public static final int DRAG = 7;
    public static final int ROTATE = 8;

    private int mKind = 0;

    public DragHandle(Context context, int resource, int kind) {
        super(context);

        mDragHandleListener = null;
        mKind = kind;

        //  inflate with the given resource
        View.inflate(context, resource, this);

        //  set our touch listener
        setOnTouchListener(this);

        //  get the drag slop
        dragSlop = (int) context.getResources().getDimension(R.dimen.sodk_editor_drag_slop);
    }

    public int getKind() {return mKind;}

    //  test to see if this handle is a selection handle
    public boolean isSelectionKind()
    {
        return (mKind==SELECTION_TOP_LEFT || mKind==SELECTION_BOTTOM_RIGHT);
    }

    //  test to see if this handle is a resize handle
    public boolean isResizeKind()
    {
        return (mKind==RESIZE_TOP_LEFT    ||
                mKind==RESIZE_TOP_RIGHT   ||
                mKind==RESIZE_BOTTOM_LEFT ||
                mKind==RESIZE_BOTTOM_RIGHT  );
    }

    //  test to see if this handle is a drag handle
    public boolean isDragKind()
    {
        return (mKind==DRAG);
    }

    //  test to see if this handle is a rotate handle
    public boolean isRotateKind()
    {
        return (mKind==ROTATE);
    }

    public void setDragHandleListener(DragHandleListener listener)
    {
        mDragHandleListener = listener;
    }

    //  this view is shown at the corners of a selection.
    //  We use a touch listener to drag it within its parent.
    //  It's parent is a RelativeLayout, so we effect moving by adjusting
    //  offsets.  The actual top and left are always 0,0.

    @Override
    public boolean onTouch(View view, MotionEvent event)
    {
        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();

        final DragHandle theHandle = this;

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

                //  tell the caller we're done dragging
                if (mIsDragging && mDragHandleListener!=null)
                    mDragHandleListener.onEndDrag(theHandle);

                mIsDragging = false;

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

    public void show(boolean bShow)
    {
        int oldVis = getVisibility();
        int newVis;
        if (bShow)
            newVis = View.VISIBLE;
        else
            newVis = View.GONE;
        setVisibility(newVis);
        if (newVis!=oldVis)
            requestLayout();
    }

    public Point getPosition()
    {
        return new Point(mPositionX, mPositionY);
    }

    public void moveTo(int x, int y)
    {
        offsetLeftAndRight(x-mPositionX);
        offsetTopAndBottom(y-mPositionY);

        mPositionX = x;
        mPositionY = y;

        invalidate();
    }

    public Point offsetCircleToEdge()
    {
        //  depending on what type of handle this is, we offset an additional amount
        //  that puts the border of the circle at the corner of the selection box.
        int dx = 0;
        int dy = 0;
        ImageView iv = (ImageView)findViewById(R.id.handle_image);
        if (iv!=null)
        {
            //  we use just the height, assuming that the handle is a circle.
            //  the number 0.707 is the approximate value of cos(45).
            int height = iv.getDrawable().getIntrinsicHeight();
            int delta = (int)(((double)height) * iv.getScaleY() * 0.707 / 2);

            dx = delta;
            if (mKind==SELECTION_TOP_LEFT || mKind==RESIZE_TOP_LEFT  || mKind==RESIZE_BOTTOM_LEFT )
                dx = -delta;

            dy = delta;
            if (mKind==SELECTION_TOP_LEFT || mKind==RESIZE_TOP_LEFT  || mKind==RESIZE_TOP_RIGHT )
                dy = -delta;
        }

        return new Point(dx, dy);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        //  we control the position by setting offsets
        //  The actual position is always 0,0.
        //  Because of this, we need to reapply the offsets here.

        offsetLeftAndRight(mPositionX);
        offsetTopAndBottom(mPositionY);
    }

}
