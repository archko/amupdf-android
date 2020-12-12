package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFWidget;

public class PDFFormEditor extends RelativeLayout
{
    protected MuPDFDoc mDoc = null;
    protected DocView mDocView = null;
    protected SOEditText mEditText = null;
    protected MuPDFWidget mWidget = null;
    protected DocMuPdfPageView mPageView = null;
    protected EditorListener mEditorListener = null;
    protected Rect mWidgetBounds = null;
    protected boolean mDocViewAtRest = true;
    protected int mPageNumber;
    protected boolean mStopped = false;

    private ViewTreeObserver mDocViewTreeObserver;
    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;

    public void onRenderComplete()
    {
        matchWidgetSizeAndPosition();
        invalidate();
    }

    protected void scrollCaretIntoView()
    {
        //  do nothing in the base class
    }

    //  an interface for notifying when we've been stopped
    public interface EditorListener {
        void onStopped();
    }

    public PDFFormEditor(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setWillNotDraw(false);  //  so OnDraw() will be called
    }

    public void start(DocMuPdfPageView pageView, final int pageNumber, final MuPDFDoc doc,
                      final DocView docView, final MuPDFWidget widget, final Rect bounds,
                      final PDFFormTextEditor.EditorListener editorListener)
    {
        mDoc = doc;
        mWidget = widget;
        mPageView = pageView;
        mDocView = docView;
        mEditText = getEditText();
        mEditorListener = editorListener;
        mPageNumber = pageNumber;
        mStopped = false;
        mWidgetBounds = new Rect(bounds);

        setupInput();

        show();

        setInitialValue();

        //  watch for layout action on our DocView.
        final PDFFormEditor editor = this;
        mDocViewTreeObserver = mDocView.getViewTreeObserver();
        mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                editor.onGlobalLayout();
            }
        };
        mDocViewTreeObserver.addOnGlobalLayoutListener(mLayoutListener);

        //  watch for single and double tap on our SOEditText
        //  we will consume every touch event here, so the SOEditText never
        //  sees them.
        final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            public boolean onDoubleTap(MotionEvent e) {
                doubleTap(e.getX(), e.getY());
                return true;
            }
            public  boolean onSingleTapConfirmed(MotionEvent e) {
                singleTap(e.getX(), e.getY());
                return true;
            }
        });
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

    }

    protected void onGlobalLayout()
    {
        //  this allows us to find out if our DocView is idle.
        mDocViewAtRest = mDocView.isAtRest();
        invalidate();
    }

    protected void setupInput()
    {
    }

    protected void show()
    {
        //  show ourselves, and give the EditText focus
        setVisibility(View.VISIBLE);
        mEditText.requestFocus();

        //  size/position it over the widget
        matchWidgetSizeAndPosition();
    }

    protected void setInitialValue()
    {
    }

    private void matchWidgetSizeAndPosition()
    {
        //  this function changes our size and position to match that of the
        //  widget we're editing.

        if (getVisibility() != View.VISIBLE)
            return;

        //  get the widget's size and position relative to the DocView
        Rect wr = new Rect();
        wr.set(mWidgetBounds);
        mPageView.pageToView(wr, wr);
        wr.offset(mPageView.getLeft(), mPageView.getTop());
        wr.offset(-mDocView.getScrollX(), -mDocView.getScrollY());

        //  move/size the outer layout
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
        params.leftMargin = wr.left;
        params.topMargin = wr.top;
        setLayoutParams(params);

        //  size the EditText to match us
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(wr.width(), wr.height());
        mEditText.setLayoutParams(lparams);

        invalidate();
    }

    public boolean stop()
    {
        mStopped = true;
        mPageView.invalidate();

        if (mEditText != null)
        {
            mEditText.clearFocus();
            mEditText.setOnTouchListener(null);
        }

        if (mDoc != null)
        {
            mDoc.setJsEventListener(null);
        }

        if (mDocView != null)
        {
            mDocView.setShowKeyboardListener(null);
        }

        if (mDocViewTreeObserver!=null)
        {
            mDocViewTreeObserver.removeOnGlobalLayoutListener(mLayoutListener);
            mDocViewTreeObserver = null;
            mLayoutListener = null;
        }

        setVisibility(View.GONE);

        return true;
    }

    protected void doubleTap(float x, float y)
    {
    }
    protected void singleTap(float x, float y)
    {
    }

    protected SOEditText getEditText()
    {
        return null;
    }

    protected void scrollIntoView()
    {
        //  scroll this editor into view
        RectF box = new RectF(mWidgetBounds);
        mDocView.scrollBoxIntoView(mPageNumber, box, true);
    }
}
