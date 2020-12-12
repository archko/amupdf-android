package com.artifex.sonui.editor;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

//  This class implements a ProgressDialog that appears after a given delay.
//  It's useful if you want it to appear before long file load times,
//  but not for short ones.

public class ProgressDialogDelayed extends ProgressDialog
{
    private boolean dismissed = false;
    private int delay = 0;

    public ProgressDialogDelayed(Context context, int delay)
    {
        super(context, R.style.sodk_editor_alert_dialog_style);

        this.delay = delay;
    }

    @Override
    public void show()
    {
        //  don't show immediately, but after a delay.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                //  don't show if we're already dismissed
                if (dismissed)
                    return;

                //  OK, show it.
                ProgressDialogDelayed.super.show();
            }
        }, delay);
    }

    @Override
    public void dismiss()
    {
        //  dismiss it if we're actually showing
        if (super.isShowing())
            super.dismiss();

        dismissed = true;
    }
}
