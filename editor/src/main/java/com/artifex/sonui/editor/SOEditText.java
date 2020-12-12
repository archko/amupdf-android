package com.artifex.sonui.editor;

import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

//  This class is a wrapper around Android's EditText.

public class SOEditText extends FrameLayout
{
    //  constructors

    public SOEditText(Context context) {
        super(context);
        if (mConstructor != null)
            mEditText = mConstructor.construct(getContext());
        else
            mEditText = new EditText(getContext());
        init();
    }

    public SOEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mConstructor != null)
            mEditText = mConstructor.construct(getContext(), attrs);
        else
            mEditText = new EditText(getContext(), attrs);
        init();
    }

    public SOEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (mConstructor != null)
            mEditText = mConstructor.construct(getContext(), attrs, defStyleAttr);
        else
            mEditText = new EditText(getContext(), attrs, defStyleAttr);
        init();
    }

    public SOEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (mConstructor != null)
            mEditText = mConstructor.construct(getContext(), attrs, defStyleAttr, defStyleRes);
        else
            mEditText = new EditText(getContext(), attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init()
    {
        super.addView(mEditText);
        setFocusable(false);
        setPadding(0, 0, 0, 0);
    }

    //  the wrapped EditText.
    private EditText mEditText;

    //  an interface for apps to provide a different EditText-derived object at
    //  construction time.
    public interface Constructor {
        public EditText construct(Context context);
        public EditText construct(Context context, AttributeSet attrs);
        public EditText construct(Context context, AttributeSet attrs, int defStyleAttr);
        public EditText construct(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes);
    }

    //  value and setter for an alternate constructor
    private static Constructor mConstructor = null;
    public static void setConstructor(Constructor ctor) {
        mConstructor = ctor;
    }

    //  The functions below are pass-throughs to the wrapped EditText, so that
    //  users of SOEditText can use familiar interfaces.

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener)
    {
        mEditText.setOnEditorActionListener(listener);
    }

    public int getSelectionStart() {
        return mEditText.getSelectionStart();
    }

    public int getSelectionEnd() {
        return mEditText.getSelectionEnd();
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback callback) {
        mEditText.setCustomSelectionActionModeCallback(callback);
    }

    public void setCustomInsertionActionModeCallback(ActionMode.Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) // api 23
            mEditText.setCustomInsertionActionModeCallback(callback);
    }

    public void setText(String text) {
        mEditText.setText(text);
    }

    public void setTextSize(int unit, float size) {
        mEditText.setTextSize(unit, size);
    }

    public Editable getText() {
        return mEditText.getText();
    }

    public void selectAll() {
        mEditText.selectAll();
    }

    public void setImeActionLabel(CharSequence label, int actionId) {
        mEditText.setImeActionLabel(label, actionId);
    }

    public void setFilters(InputFilter[] filters) {
        mEditText.setFilters(filters);
    }

    public void setSelection(int start, int stop) {
        mEditText.setSelection(start, stop);
    }

    public void setSelection(int index) {
        mEditText.setSelection(index);
    }

    public void setInputType(int type) {
        mEditText.setInputType(type);
    }

    public void setSingleLine(boolean singleLine) {
        mEditText.setSingleLine(singleLine);
    }

    public void setSingleLine() {
        mEditText.setSingleLine();
    }

    public void setImeOptions(int imeOptions) {
        mEditText.setImeOptions(imeOptions);
    }

    public void setSelectAllOnFocus(boolean selectAllOnFocus) {
        mEditText.setSelectAllOnFocus(selectAllOnFocus);
    }

    public void addTextChangedListener(TextWatcher watcher) {
        mEditText.addTextChangedListener(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        mEditText.removeTextChangedListener(watcher);
    }

    public void setOnKeyListener(OnKeyListener listener) {
        mEditText.setOnKeyListener(listener);
    }

    public void setOnClickListener(OnClickListener listener) {
        mEditText.setOnClickListener(listener);
    }

    public void setEnabled(boolean enabled) {
        mEditText.setEnabled(enabled);
    }

    public void clearFocus() {
        mEditText.clearFocus();
    }

    public boolean isEnabled() {
        return mEditText.isEnabled();
    }

    public void setOnFocusChangeListener(OnFocusChangeListener listener) {
        mEditText.setOnFocusChangeListener(listener);
    }

    public void setOnTouchListener(OnTouchListener listener) {
        mEditText.setOnTouchListener(listener);
    }

    public void setFocusableInTouchMode(boolean focusable) {
        mEditText.setFocusableInTouchMode(focusable);
    }

    public void setLayoutParams(LayoutParams params) {
        mEditText.setLayoutParams(params);
    }

    public boolean callOnClick() {
        return mEditText.callOnClick();
    }

    public void setTag(int key, final Object tag) {
        mEditText.setTag(key, tag);
    }

    public Object getTag(int key) {
        return mEditText.getTag(key);
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mEditText!=null)
            mEditText.setVisibility(visibility);
    }
}
