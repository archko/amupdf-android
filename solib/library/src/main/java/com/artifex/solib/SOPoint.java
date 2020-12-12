package com.artifex.solib;

import android.graphics.PointF;

public class SOPoint extends PointF
{
    public static final int     MoveTo = 0;
    public static final int     LineTo = 1;

    /**
     * Allocate an SOPoint at the specified location
     *
     * @param x         horizontal position.
     * @param y         vertical position.
     * @param pointType type of point (SOPointType_xxx)
     */
    public SOPoint(int x, int y, int pointType)
    {
        this.x = x;
        this.y = y;

        if( pointType >= 0 && pointType <= LineTo)
            type = pointType;
    }

    /**
     * Allocate an SOPoint at the specified location
     *
     * @param x         horizontal position (float).
     * @param y         vertical position (float).
     * @param pointType type of point (SOPointType_xxx)
     */
    public SOPoint(float x, float y, int pointType)
    {
        this.x = x;
        this.y = y;

        if( pointType >= 0 && pointType <= LineTo)
            type = pointType;
    }

    /**
     * Allocate an SOPoint for the specified location
     *
     * @param position  x,y position.
     * @param pointType type of point (SOPointType_xxx)
     */
    public SOPoint(PointF position, int pointType)
    {
        x = position.x;
        y = position.y;

        if( pointType >= 0 && pointType <= LineTo)
            type = pointType;
    }

    /* public implementation */
    public int     type = MoveTo;
}
