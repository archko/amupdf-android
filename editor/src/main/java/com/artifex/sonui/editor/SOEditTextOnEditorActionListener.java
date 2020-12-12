package com.artifex.sonui.editor;

/*
 * OnEditorActionListener interface to be used with SOEditText objects.
 */

import android.view.KeyEvent;
import android.content.Context;
import android.util.AttributeSet;

public interface SOEditTextOnEditorActionListener
{
    public abstract boolean onEditorAction (SOEditText v,
                                            int        actionId,
                                            KeyEvent   event);
}
