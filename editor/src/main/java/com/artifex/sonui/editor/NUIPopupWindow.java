package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.PopupWindow;

//  this class extends PopupWindow for the purpose of setting a background
//  drawable.  This allows the popup to be dismissed using a hardware
//  Back button found on some devices.

public class NUIPopupWindow extends PopupWindow
{
    NUIPopupWindow(Context context)
    {
        super(context);
        setup();
    }

    NUIPopupWindow(View view)
    {
        super(view);
        setup();
    }

    NUIPopupWindow(View contentView, int width, int height)
    {
        super(contentView, width, height);
        setup();
    }

    private void setup()
    {
        setBackgroundDrawable(new ColorDrawable());
        setOutsideTouchable(true);
    }

}
