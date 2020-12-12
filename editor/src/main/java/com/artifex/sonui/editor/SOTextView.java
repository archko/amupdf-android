package com.artifex.sonui.editor;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

//  This class is a wrapper around Android's TextView.

public class SOTextView extends FrameLayout {
    //  constructors

    public SOTextView(Context context) {
        super(context);
        if (mConstructor != null)
            mTextView = mConstructor.construct(getContext());
        else
            mTextView = new TextView(getContext());
        init();
    }

    public SOTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mConstructor != null)
            mTextView = mConstructor.construct(getContext(), attrs);
        else
            mTextView = new TextView(getContext(), attrs);
        init();
    }

    public SOTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (mConstructor != null)
            mTextView = mConstructor.construct(getContext(), attrs, defStyleAttr);
        else
            mTextView = new TextView(getContext(), attrs, defStyleAttr);
        init();
    }

    public SOTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (mConstructor != null)
            mTextView = mConstructor.construct(getContext(), attrs, defStyleAttr, defStyleRes);
        else
            mTextView = new TextView(getContext(), attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        //  we set the Id of the wrapped TextView to this known value.
        mTextView.setId(R.id.sodk_editor_text_view);

        super.addView(mTextView);
        setPadding(0, 0, 0, 0);

        //  if a click listener was set while calling the super's constructor,
        //  apply it to the TextView now.
        //  This happens, I think, when "onClick" is used in XML.
        if (mClickListener!=null) {
            mTextView.setOnClickListener(mClickListener);
        }
    }

    //  The wrapped TextView
    private TextView mTextView;

    //  an interface for apps to provide a different TextView-derived object at
    //  construction time.
    public interface Constructor {
        public TextView construct(Context context);

        public TextView construct(Context context, AttributeSet attrs);

        public TextView construct(Context context, AttributeSet attrs, int defStyleAttr);

        public TextView construct(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes);
    }

    //  value and setter for an alternate constructor
    private static Constructor mConstructor = null;

    public static void setConstructor(Constructor ctor) {
        mConstructor = ctor;
    }

    //  The functions below are pass-throughs to the wrapped TextView, so that
    //  users of SOTextView can use familiar interfaces.

    public void setText(CharSequence text) {
        mTextView.setText(text);
    }

    public void setTextSize(int unit, float size) {
        mTextView.setTextSize(unit, size);
    }

    public void setTextSize(float size) {
        mTextView.setTextSize(size);
    }

    public void setTextColor(int color) {
        mTextView.setTextColor(color);
    }

    public Drawable[] getCompoundDrawables() {
        return mTextView.getCompoundDrawables();
    }

    public CharSequence getText() {
        return mTextView.getText();
    }

    public void setText(CharSequence text, TextView.BufferType type) {
        mTextView.setText(text, type);
    }

    public void setTypeface(Typeface tf) {
        mTextView.setTypeface(tf);
    }

    public final void setText(@StringRes int resid) {
        mTextView.setText(resid);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(@DrawableRes int left, @DrawableRes int top, @DrawableRes int right, @DrawableRes int bottom) {
        mTextView.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
    }

    public void setCompoundDrawablePadding(int pad) {
        mTextView.setCompoundDrawablePadding(pad);
    }

    public void setOnKeyListener(OnKeyListener listener) {
        mTextView.setOnKeyListener(listener);
    }

    public void setSelected(boolean selected) {
        mTextView.setSelected(selected);
    }

    OnClickListener mClickListener = null;

    public void setOnClickListener(OnClickListener l)
    {
        if (mTextView==null) {
            //  we were called during construction. Save for later.
            mClickListener = l;
        }
        else {
            mTextView.setOnClickListener(l);
        }
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mTextView!=null)
            mTextView.setVisibility(visibility);
    }
}
