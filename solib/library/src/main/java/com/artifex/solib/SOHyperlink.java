package com.artifex.solib;

import android.graphics.Rect;

/*
 *  This object is returned by calling SOPage.objectAtPoint.
 */

public class SOHyperlink {

    public String url;      /* url of the hyperlink */
    public int pageNum;     /* page number for the hyperlink */
    public Rect bbox;       /* bounding box of the hyperlink */

}
