package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;

public class ToolbarButton extends Button
{
    public ToolbarButton(Context context) {
        super(context);
        init();
    }

    public ToolbarButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToolbarButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    //  let's remember the size of the text when we're created.
    //  then we can "hide" the text by setting its size to zero,
    //  and "show" it by restoring the original size.

    private float mTextSize;
    private void init() {
        //  note: this size is pixels
        mTextSize = getTextSize();
    }

    public void hideText() {
        setTextSize(0);
    }

    public void showText() {
        //  make sure to set the value in pixels.
        setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
    }

    public void setImageResource(int id)
    {
        //  get the drawable from the ID
        Drawable drawable = ContextCompat.getDrawable(getContext(), id);

        //  all buttons of this class specify their image using android:drawbleTop.
        setCompoundDrawablesWithIntrinsicBounds(null, drawable , null, null);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        //  call super
        super.setEnabled(enabled);

        //  use different alpha value for "disabled"
        if (enabled)
            setAlpha(1.0f);
        else
            setAlpha(0.4f);
    }

    public void setDrawableColor(int color)
    {
        Drawable[] drawables = getCompoundDrawables();
        for (int i=0;i< drawables.length;i++) {
            if (drawables[i]!=null)
                DrawableCompat.setTint(drawables[i], color);
        }
    }

    public static void setAllSameSize(int width, ToolbarButton[] buttons)
    {
        int nButtons = buttons.length;

        for (int i=0; i<nButtons; i++)
        {
            if (buttons[i]!=null)
            {
                buttons[i].setWidth(width);
            }
        }
    }

    //  this function is only to be used by the Explorer activity.
    public static int getMaxWidth(ToolbarButton[] buttons)
    {
        int nButtons = buttons.length;

        int maxw = 0;
        for (int i=0; i<nButtons; i++)
        {
            if (buttons[i]!=null)
            {
                buttons[i].measure(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                int w = buttons[i].getMeasuredWidth();
                if (w>maxw)
                    maxw = w;
            }
        }

        return maxw;
    }

    public static void setAllSameSize(ToolbarButton[] buttons)
    {
        int nButtons = buttons.length;

        //  first break words
        int nbroken = 0;
        int i;
        for (i=0; i<nButtons; i++)
        {
            if (buttons[i]!=null)
            {
                String text = buttons[i].getText().toString();
                String[] parts = text.split(" ");
                if (parts.length==2)
                {
                    //  if the text is two words, wrap
                    text = parts[0] + "\n" + parts[1];
                    buttons[i].setText(text);
                    nbroken++;
                }
                else if (parts.length==3)
                {
                    //  if the text is three words, wrap after the 2nd word
                    //  this could be improved
                    text = parts[0] + " " + parts[1] + "\n" + parts[2];
                    buttons[i].setText(text);
                    nbroken++;
                }
            }
        }

        //  if any were broken, make sure they all have two lines.
        //  this will give them all the same height.
        if (nbroken>0)
        {
            for (i=0; i<nButtons; i++)
            {
                if (buttons[i]!=null)
                {
                    String text = buttons[i].getText().toString();
                    if (!text.contains("\n"))
                    {
                        text = text + "\n" ;
                        buttons[i].setText(text);
                    }
                }
            }
        }

        //  now calculate the new width
        int maxw = 0;
        for (i=0; i<nButtons; i++)
        {
            if (buttons[i]!=null)
            {
                int w = buttons[i].getMeasuredWidth();
                if (w>maxw)
                    maxw = w;
            }
        }

        //  now resize all the buttons
        setAllSameSize(maxw, buttons);
    }

}
