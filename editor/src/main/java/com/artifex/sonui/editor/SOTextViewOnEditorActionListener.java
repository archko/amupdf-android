package com.artifex.sonui.editor;

/*
 * OnEditorActionListener interface to be used with SOTextView objects.
 */

import android.view.KeyEvent;
import android.content.Context;
import android.util.AttributeSet;

public interface SOTextViewOnEditorActionListener
{
    public abstract boolean onEditorAction (SOTextView v,
                                            int        actionId,
                                            KeyEvent   event);
}
