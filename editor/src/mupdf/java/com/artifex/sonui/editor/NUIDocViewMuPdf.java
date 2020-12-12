package com.artifex.sonui.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class NUIDocViewMuPdf extends NUIDocViewPdf
{
    public NUIDocViewMuPdf(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocViewMuPdf(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocViewMuPdf(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    private void initialize(Context context)
    {
    }

    @Override
    protected NUIDocView.TabData[] getTabData()
    {
        if (mTabs == null) {
            mTabs = new NUIDocView.TabData[4];

            mTabs[0] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_file),     R.id.fileTab,     R.layout.sodk_editor_tab_left, View.VISIBLE);
            mTabs[1] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_annotate), R.id.annotateTab, R.layout.sodk_editor_tab, View.GONE);
            mTabs[2] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_redact), R.id.redactTab, R.layout.sodk_editor_tab, View.GONE);
            mTabs[3] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_pages),    R.id.pagesTab,    R.layout.sodk_editor_tab_right, View.VISIBLE);
        }
        return mTabs;
    }

    @Override
    public void setConfigurableButtons()
    {
        super.setConfigurableButtons();

        //  for non-PDF file (where this view is used),
        //  remove buttons that won't work.

        if (mSaveButton!=null)
            mSaveButton.setVisibility(View.GONE);

        if (mSaveAsButton!=null)
            mSaveAsButton.setVisibility(View.GONE);

        if (mOpenPdfInButton!=null)
            mOpenPdfInButton.setVisibility(View.GONE);

        if (mSavePdfButton!=null)
            mSavePdfButton.setVisibility(View.GONE);

        if (mPrintButton!=null)
            mPrintButton.setVisibility(View.GONE);

        if (mOpenInButton!=null)
            mOpenInButton.setVisibility(View.GONE);
    }

    @Override
    protected void checkXFA()
    {
        //  do nothing
    }
}
