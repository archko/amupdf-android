package com.artifex.solib;

import android.graphics.Bitmap;
import android.graphics.Rect;
import java.lang.IllegalArgumentException;

public class ArDkBitmap implements Comparable<ArDkBitmap>
{
    public ArDkBitmap() {}

    /**
     * Pixel formats supported for output by SmartOffice.
     *
     * Note that the naming scheme is not consistent with Bitmap.Config.
     */
    public enum Type {
        A8,
        RGB555,
        RGB565,
        RGBA8888
    }

    protected void allocateBitmap(int w, int h, ArDkBitmap.Type type)
    {
        serialBase++;
        serial = serialBase;
        Bitmap.Config config;
        switch (type) {
            default:
            case RGB555:
            case RGB565:   config = Bitmap.Config.RGB_565;   break;
            case A8:       config = Bitmap.Config.ALPHA_8;   break;
            case RGBA8888: config = Bitmap.Config.ARGB_8888; break;
        }
        this.bitmap = Bitmap.createBitmap(w, h, config);
        this.rect   = new Rect(0, 0, w, h);
    }

    public static Type defaultType()
    {
        if (BuildConfig.SCREENS_ARE.equals("R8G8B8X8"))
            return Type.RGBA8888;
        else
            return Type.RGB565;
    }

    public static Bitmap.Config defaultConfig()
    {
        if (BuildConfig.SCREENS_ARE.equals("R8G8B8X8"))
            return Bitmap.Config.ARGB_8888;
        else
            return Bitmap.Config.RGB_565;
    }

    public ArDkBitmap(Bitmap bitmap)
    {
        //  create an ArDkBitmap from an Android Bitmap
        serialBase++;
        serial = serialBase;
        this.bitmap = bitmap;
        this.rect   = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    /**
     * Get the Android bitmap that underlies this ArDkBitmap.
     *
     * @return the android bitmap that underlies this ArDkBitmap.
     */
    public Bitmap getBitmap()
    {
        return bitmap;
    }

    /**
     * Get the subrectangle of the underlying Android Bitmap that corresponds to this ArDkBitmap.
     *
     * @return subrectangle of the underlying Android Bitmap that corresponds to this ArDkBitmap.
     */
    public Rect getRect()
    {
        return rect;
    }

    /**
     * Read the current width of this bitmap.
     *
     * @note If we ever move to an implementation using adjustAspectForSize, then this value
     * may change. Best to code for this case initially.
     *
     * @return The width of the ArDkBitmap.
     */
    public int getWidth()
    {
        return this.rect.right - this.rect.left;
    }

    /**
     * Read the current height of this bitmap.
     *
     * @note If we ever move to an implementation using adjustAspectForSize, then this value
     * may change. Best to code for this case initially.
     *
     * @return The height of the ArDkBitmap.
     */
    public int getHeight()
    {
        return this.rect.bottom - this.rect.top;
    }

    /**
     * Used to order ArDkBitmap objects in ordered Collections
     */
    public int compareTo (ArDkBitmap o)
    {
        int size = (bitmap == null)? 0:bitmap.getByteCount();
        int compareSize = (o.bitmap== null)? 0:o.bitmap.getByteCount();
        if (size > compareSize)
            return 1;
        if (size < compareSize)
            return -1;
        if (serial > o.serial)
            return 1;
        if (serial < o.serial)
            return -1;
        return 0;
    }

    /** Recycle the bitmap within the ArDkBitmap
     *  Intended to be called just before the ArDkBitmap reference is set to null
     */
    public void dispose()
    {
        if (!bitmap.isRecycled())
            bitmap.recycle();
    }

    /**
     *  return the serial number
     */
    public int getSerial()
    {
        return serial;
    }

    /* Private implementation and internal data. */
    protected static int serialBase = 0;
    protected int      serial;
    protected Bitmap   bitmap;
    protected Rect     rect;

    /**
     * create a new bitmap representing a portion of this bitmap.
     * real implementations are found in SOBitmap and MuPdfBitmap
     */
    public ArDkBitmap createBitmap(int x0, int y0, int x1, int y1) {return null;}
}
