package com.artifex.sonui.editor;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ArDkSelectionLimits;

public class NoteEditor
{
    private View mEditor;

    private SOEditText mCommentView;
    private SOTextView mDateView;
    private SOTextView mAuthorView;
    private View mCover;
    private DocView mScrollView;
    private int mEditorScrollDiff = 0;
    private ArDkSelectionLimits mSelLimits;
    private DocPageView mPageView;
    private NoteDataHandler mDataHandler = null;

    public NoteEditor(final Activity activity, DocView scrollView, final DocViewHost host, NoteDataHandler handler)
    {
        mScrollView = scrollView;
        mDataHandler = handler;

        mEditor = activity.findViewById(R.id.doc_note_editor);
        mCover = activity.findViewById(R.id.doc_cover);
        ViewGroup mParent = (ViewGroup)mEditor.getParent();
        mCommentView = (SOEditText)activity.findViewById(R.id.doc_note_editor_text);
        mDateView = (SOTextView)activity.findViewById(R.id.doc_note_editor_date);
        mAuthorView = (SOTextView)activity.findViewById(R.id.doc_note_editor_author);

        //  comment editing is initially disabled
        mCommentView.setEnabled(false);

        mCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Utilities.hideKeyboard(activity);
                mCommentView.clearFocus();
                mCover.setVisibility(View.GONE);
            }
        });

        mCommentView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    mCover.setVisibility(View.VISIBLE);

                    //  when the editor gains focus, scroll it to the top of the viewport
                    Rect viewport = new Rect();
                    mScrollView.getGlobalVisibleRect(viewport);
                    viewport.offset(0,-viewport.top);

                    //  where is the editor now?
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditor.getLayoutParams();

                    //  move it into view
                    mEditorScrollDiff = viewport.top - params.topMargin;
                    mScrollView.smoothScrollBy(0,mEditorScrollDiff);
                }
                else
                {
                    mCover.setVisibility(View.GONE);

                    //  when editor loses focus, save the data.
                    saveData();

                    //  when editor loses focus, scroll back to where we began.
                    mScrollView.smoothScrollBy(0,-mEditorScrollDiff);
                    mEditorScrollDiff = 0;
                }
            }
        });
    }

    public void preMoving()
    {
        //  this is called before re-using the editor for something else.
        //  if the comment is editable, save the data
        if (mCommentView.isEnabled())
        {
            saveData();
            mCommentView.setEnabled(false);
        }
    }

    public void saveComment()
    {
        saveData();
    }

    public void setCommentEditable(boolean editable)
    {
        mCommentView.setEnabled(editable);
    }

    public void saveData()
    {
        String comment = mCommentView.getText().toString();
        mDataHandler.setComment(comment);
    }

    public void show(ArDkSelectionLimits limits, DocPageView pageView)
    {
        mSelLimits = limits;
        mPageView = pageView;
        ArDkDoc doc = mScrollView.getDoc();

        String author  = mDataHandler.getAuthor();
        String date    = mDataHandler.getDate();
        String comment = mDataHandler.getComment();

        if (author!=null && !author.isEmpty()) {
            mAuthorView.setVisibility(View.VISIBLE);
            mAuthorView.setText(author);
        }
        else {
            mAuthorView.setVisibility(View.GONE);
        }

        if (date!=null && !date.isEmpty()) {
            mDateView.setVisibility(View.VISIBLE);
            //  reformat date for the current locale
            date = Utilities.formatDateForLocale(mScrollView.getContext(), date, doc.getDateFormatPattern());
            mDateView.setText(date);
        }
        else {
            mDateView.setVisibility(View.GONE);
        }

        mCommentView.setText(comment);

        mEditor.setVisibility(View.VISIBLE);
    }

    public void hide()
    {
        mEditor.setVisibility(View.GONE);
        mCover.setVisibility(View.GONE);
    }

    public boolean isVisible()
    {
        return (mEditor.getVisibility() == View.VISIBLE);
    }

    public void move()
    {
        if (mEditor!=null && mSelLimits!=null && mEditor.getVisibility()==View.VISIBLE && mPageView!=null)
        {
            RectF box = mSelLimits.getBox();
            //  convert to DocPageView-based coords
            Point p = mPageView.pageToView((int)box.left, (int)box.bottom);
            //  offset to 0,0
            p.offset(mPageView.getLeft(), mPageView.getTop());
            //  offset to position in the scrolling view (this)
            p.offset(-mScrollView.getScrollX(), -mScrollView.getScrollY());
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditor.getLayoutParams();

            //  move this to the left if it's on the right side of the page.
            //  this will serve to keep the editor within the page boundaries.
            Point ppage = mPageView.getPage().sizeAtZoom(1);
            if (box.left>ppage.x/2)
                p.x -= params.width;

            params.leftMargin = p.x;
            params.topMargin = p.y;
            mEditor.setLayoutParams(params);
        }
    }

    public Rect getRect()
    {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditor.getLayoutParams();
        Rect r = new Rect();
        r.left = params.leftMargin;
        r.top = params.topMargin;
        r.right = r.left + mEditor.getMeasuredWidth();
        r.bottom = r.top + mEditor.getMeasuredHeight();

        return r;
    }

    public void focus()
    {
        //  give the text field focus
        mCommentView.requestFocus();

    }

    public interface NoteDataHandler
    {
        void setComment(String comment);
        String getAuthor();
        String getDate();
        String getComment();
    }
}
