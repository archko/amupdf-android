package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.artifex.solib.FileUtils;

import java.util.Arrays;
import java.util.List;

public class NUIDocViewOther extends NUIDocView
{
    public NUIDocViewOther(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocViewOther(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocViewOther(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    private void initialize(Context context)
    {
    }

    @Override
    protected int getLayoutId()
    {
        return R.layout.sodk_editor_other_document;
    }

    @Override
    protected DocView createMainView(Activity activity)
    {
        return new DocView(activity);
    }

    @Override
    protected void createEditButtons() {}

    @Override
    protected void createEditButtons2() {}

    private boolean useFullToolbar()
    {
        //  certain document types will use the full "other" toolbar.
        List<String> exts = Arrays.asList("txt", "csv", "hwp");

        String ext = getFileExtension();
        if (ext==null)
            return false;

        return exts.contains(ext.toLowerCase());
    }

    @Override
    protected void afterFirstLayoutComplete()
    {
        super.afterFirstLayoutComplete();

        if (useFullToolbar())
        {
            hideUnnecessaryDivider2(R.id.other_toolbar);
        }
        else
        {
            //  for all others, hide everything but printing
            findViewById(R.id.search_toolbar).setVisibility(View.GONE);
            findViewById(R.id.first_page_button).setVisibility(View.GONE);
            findViewById(R.id.last_page_button).setVisibility(View.GONE);
            findViewById(R.id.reflow_button).setVisibility(View.GONE);
            findViewById(R.id.divider_1).setVisibility(View.GONE);
            findViewById(R.id.divider_2).setVisibility(View.GONE);
        }
    }

    protected void hideUnnecessaryDivider2(int id)
    {
        //  find the toolbar
        LinearLayout toolbar = (LinearLayout)findViewById(id);
        if (toolbar == null)
            return;

        //  count visible things between the two dividers
        int nBetween = 0;
        int ndiv = 0;
        for (int i = 0; i < toolbar.getChildCount(); i++)
        {
            View child = toolbar.getChildAt(i);
            int childId = child.getId();
            if (childId!=R.id.divider_1 && childId!=R.id.divider_2) {
                //  not divider
                if (child.getVisibility() == View.VISIBLE && ndiv==1)
                    nBetween++;
            }
            else {
                //  divider
                ndiv++;
            }
        }

        //  if no visible buttons between the two dividers,
        //  hide them both
        if (nBetween ==0)
        {
            findViewById(R.id.divider_1).setVisibility(View.GONE);
            findViewById(R.id.divider_2).setVisibility(View.GONE);
        }
    }

    @Override
    protected void createReviewButtons()
    {
    }

    @Override
    protected void createInsertButtons()
    {
    }

    @Override
    protected PageAdapter createAdapter()
    {
        return new PageAdapter(activity(), this, PageAdapter.DOC_KIND);
    }

    @Override
    protected NUIDocView.TabData[] getTabData()
    {
        if (mTabs == null)
        {
            mTabs = new NUIDocView.TabData[2];

            mTabs[0] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_file),     R.id.fileTab,     R.layout.sodk_editor_tab_one, View.VISIBLE);
            mTabs[1] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_pages),    R.id.pagesTab,    R.layout.sodk_editor_tab_right, View.GONE);
        }

        return mTabs;
    }

    @Override
    protected int getTabUnselectedColor()
    {
        return ContextCompat.getColor(getContext(), R.color.sodk_editor_header_other_color);
    }

    @Override
    protected void updateUIAppearance()
    {
    }

    @Override
    public void onReflowButton(View v)
    {
        if (useFullToolbar()) {
            super.onReflowButton(v);
        }
    }

    @Override
    public void onUndoButton(final View v) {
        super.onUndoButton(v);
    }

    @Override
    public void onRedoButton(final View v) {
        super.onRedoButton(v);
    }

    @Override
    public void onClick(View v)
    {
        // Ignore button presses while we are finishing up.
        if (mFinished)
        {
            return;
        }

        super.onClick(v);
    }

    public int getBorderColor()
    {
        return ContextCompat.getColor(getContext(), R.color.sodk_editor_selected_page_border_color);
    }

    @Override
    protected boolean hasSearch()
    {
        String ext = getFileExtension();
        if (ext != null && ext.compareToIgnoreCase("txt")==0)
        {
            //  text files support search
            return true;
        }

        //  other do not
        return false;
    }

    private String getFileExtension()
    {
        if (mStartUri != null)
        {
            return FileUtils.getFileTypeExtension(getContext(), mStartUri);
        }

        String path = "";
        if (mSession!=null)
        {
            path = mSession.getUserPath();
        }
        else if (mState!=null)
        {
            path = mState.getInternalPath();
        }

        return FileUtils.getExtension(path);
    }

    @Override
    public void onPause(final Runnable whenDone)
    {
        onPauseCommon();
        whenDone.run();
    }

    @Override
    public void onResume()
    {
        onResumeCommon();
    }

    @Override
    protected void setupTabs()
    {
        //  "other" files have no tabs
    }

    @Override
    protected void scaleHeader()
    {
        //  scale one toolbar and the back button.
        float factor = 0.65f;
        scaleToolbar(R.id.other_toolbar, factor);
        mBackButton.setScaleX(factor);
        mBackButton.setScaleY(factor);
    }

    @Override
    protected void enforceInitialShowUI(View view)
    {
        boolean bShow = mDocCfgOptions.showUI();

        //  hide the UI if we were told to when we were started
        if (!mShowUI)
            bShow = false;

        findViewById(R.id.other_top).setVisibility(bShow? View.VISIBLE:View.GONE);
        findViewById(R.id.footer).setVisibility(bShow? View.VISIBLE:View.GONE);
        findViewById(R.id.header).setVisibility(bShow? View.VISIBLE:View.GONE);

        mFullscreen = !bShow;
    }

    @Override
    public void showUI(boolean bShow)
    {
        //  If there's a registered handler for exiting full screen, run it
        if (bShow)
        {
            if (mExitFullScreenRunnable != null)
                mExitFullScreenRunnable.run();
            //  set this here in case we were started with no UI
            mFullscreen = false;
        }

        //  if we're started with no UI, keep it that way.
        if (!mShowUI)
            return;

        View top = findViewById(R.id.other_top);

        if (bShow)
        {
            top.setVisibility(View.VISIBLE);
            findViewById(R.id.footer).setVisibility(View.VISIBLE);
            findViewById(R.id.header).setVisibility(View.VISIBLE);
        }
        else
        {
            if (!isSearchVisible())
                top.setVisibility(View.GONE);
            findViewById(R.id.footer).setVisibility(View.GONE);
        }
        layoutNow();

        afterShowUI(bShow);
    }

    protected void onFullScreenHide()
    {
        findViewById(R.id.other_top).setVisibility(View.GONE);
        findViewById(R.id.footer).setVisibility(View.GONE);
        findViewById(R.id.header).setVisibility(View.GONE);
        layoutNow();
    }

    @Override
    protected boolean usePagesView()
    {
        //  "other" files have no page list
        return false;
    }

    @Override
    protected boolean hasUndo() {return false;}

    @Override
    protected boolean hasRedo() {return false;}

    @Override
    protected boolean inputViewHasFocus()
    {
        return false;
    }

    @Override
    protected void createInputView()
    {
        //  this view does not use InputView
    }

    @Override
    protected boolean canSelect() {return false;}
}
