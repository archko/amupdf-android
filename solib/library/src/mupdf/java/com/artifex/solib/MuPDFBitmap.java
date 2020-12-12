package com.artifex.solib;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class MuPDFBitmap extends ArDkBitmap
{
    public MuPDFBitmap(int w, int h)
    {
        serialBase++;
        serial = serialBase;
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        this.rect   = new Rect(0, 0, w, h);
    }

    /**
     * Create an MuPDFBitmap that gives access to a subarea of another ArDkBitmap (sharing the same
     * underlying native Bitmap).
     *
     * @param s     MuPDFBitmap that we want to represent a subrectangle of.
     * @param x0    bound of sub rectangle.
     * @param y0    bound of sub rectangle.
     * @param x1    bound of sub rectangle.
     * @param y1    bound of sub rectangle.
     */
    public MuPDFBitmap(ArDkBitmap s, int x0, int y0, int x1, int y1)
    {
        this.serial = s.serial;
        this.bitmap  = s.bitmap;
        this.rect = new Rect(x0, y0, x1, y1);
    }

    /**
     * create a new bitmap representing a portion of this bitmap.
     */
    public ArDkBitmap createBitmap(int x0, int y0, int x1, int y1)
    {
        return new MuPDFBitmap(this, x0, y0, x1, y1);
    }

}
