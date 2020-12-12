package com.artifex.solib;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class MuPDFSelectionLimits extends ArDkSelectionLimits
{
    public MuPDFSelectionLimits(Rect box)
    {
        mBox = new Rect(box);
    }

    public MuPDFSelectionLimits(PointF start, PointF end)
    {
        mBox = new Rect((int)start.x, (int)start.y, (int)end.x, (int)end.y);
    }

    private Rect mBox;
    public Rect getRect() {return mBox;}

    @Override
    public PointF getStart()
    {
        return new PointF(mBox.left, mBox.top);
    }

    @Override
    public PointF getEnd()
    {
        return new PointF(mBox.right, mBox.bottom);
    }

    @Override
    public RectF getBox()
    {
        return new RectF(mBox.left, mBox.top, mBox.right, mBox.bottom);
    }

    @Override
    public boolean getHasSelectionStart()
    {
        return true;
    }

    @Override
    public boolean getHasSelectionEnd()
    {
        return true;
    }

    @Override
    public boolean getIsActive()
    {
        return true;
    }

    @Override
    public boolean getIsCaret()
    {
        return false;
    }

    @Override
    public void combine(ArDkSelectionLimits limits)
    {
        MuPDFSelectionLimits that = (MuPDFSelectionLimits)limits;

        mBox.left   = (int)Math.min(mBox.left,   that.getRect().left);
        mBox.right  = (int)Math.max(mBox.right,  that.getRect().right);
        mBox.top    = (int)Math.min(mBox.top,    that.getRect().top);
        mBox.bottom = (int)Math.max(mBox.bottom, that.getRect().bottom);
    }
}
