package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.artifex.solib.ArDkSelectionLimits;
import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFWidget;

import java.util.ArrayList;

public class DocPdfView extends DocView
{
    private static final String  TAG           = "DocPdfView";

    public DocPdfView(Context context) {
        super(context);
    }

    public DocPdfView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DocPdfView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //  drag handles
    private DragHandle mDragHandleTopLeft = null;
    private DragHandle mDragHandleBottomRight = null;

    //  resizing details
    private View mResizingView;                 //  transparent View that's shown while resizing
    private DocPageView mResizingPage = null;   //  page on which the resizing is happening
    private Rect mResizingRect = new Rect();    //  current rect of the resize
    private Point mResizingFixedPoint;          //  corner that's not moving
    private Point mResizingMovingPoint;         //  corner that's moving
    private Point mDragHandlePointAtStart;      //  where the handle that's being drug was at the start
    private Point mResizingMovingPointAtStart;  //  where the moving point was at the start of resizing
    private boolean dragging = false;           //  true if we're dragging a handle

    //  page on which we just created a signature.
    //  this is set when the signature is created,
    //  and used in onSelectionChanged()
    private DocPdfPageView sigCreatedPage = null;
    private DocPdfPageView sigDeletingPage = null;

    //  this is set while a signature is being edited
    private MuPDFWidget sigEditingWidget = null;
    private DocPdfPageView sigEditingPage = null;

    //  true if we're in "mark area" mode.
    private boolean mMarkAreaMode = false;
    public boolean getMarkAreaMode() {return mMarkAreaMode;}
    public void toggleMarkAreaMode()
    {
        clearAreaSelection();
        clearSignatureEditing();
        mMarkAreaMode = !mMarkAreaMode;
        onSelectionChanged();
    }

    //  instance of the Note Editor
    private NoteEditor mNoteEditor = null;

    public void saveNoteData()
    {
        if (mNoteEditor!=null)
            mNoteEditor.saveData();
    }

    @Override
    public void setup(RelativeLayout layout)
    {
        super.setup(layout);

        //  create the note editor
        mNoteEditor = new NoteEditor((Activity)getContext(), this, mHostActivity, new NoteEditor.NoteDataHandler() {
            @Override
            public void setComment(String comment) {
                getDoc().setSelectionAnnotationComment(comment);
            }

            @Override
            public String getAuthor() {
                return getDoc().getSelectionAnnotationAuthor();
            }

            @Override
            public String getDate() {
                return getDoc().getSelectionAnnotationDate();
            }

            @Override
            public String getComment() {
                return getDoc().getSelectionAnnotationComment();
            }
        });
    }

    private DragHandle setupHandle(RelativeLayout layout, int kind)
    {
        //  create the handle
        DragHandle dh = new DragHandle(getContext(), R.layout.sodk_editor_resize_handle, kind);

        //  add to the layout, initially hidden
        layout.addView(dh);
        dh.show(false);

        //  establish the listener
        dh.setDragHandleListener(dragListener);

        return dh;
    }

    private DragHandleListener dragListener = new DragHandleListener()
    {
        //  This is the DragHandleListener for the two DragHandles
        //  for this view.  It's used when resizing an existing redaction,
        //  but not for drawing a new one.
        //  It's also used for resizing signature widgets.

        @Override
        public void onStartDrag(DragHandle handle)
        {
            dragging = true;

            if (sigEditingWidget !=null)
            {
                //  match the resizing rect to the widget being edited
                Rect bounds = sigEditingWidget.getBounds();
                mResizingRect = new Rect(mSelectionStartPage.pageToView((int) bounds.left),
                        mSelectionStartPage.pageToView((int) bounds.top),
                        mSelectionStartPage.pageToView((int) bounds.right),
                        mSelectionStartPage.pageToView((int) bounds.bottom));
                mResizingRect.offset(mSelectionStartPage.getLeft(), mSelectionStartPage.getTop());
                mResizingRect.offset(-getScrollX(), -getScrollY());
            }
            else {
                //  initial value for the resizing rect should match the selected object
                mResizingPage = mSelectionStartPage;
                RectF bounds = mSelectionStartPage.getSelectionLimits().getBox();
                mResizingRect = new Rect(mSelectionStartPage.pageToView((int) bounds.left),
                        mSelectionStartPage.pageToView((int) bounds.top),
                        mSelectionStartPage.pageToView((int) bounds.right),
                        mSelectionStartPage.pageToView((int) bounds.bottom));
                mResizingRect.offset(mSelectionStartPage.getLeft(), mSelectionStartPage.getTop());
                mResizingRect.offset(-getScrollX(), -getScrollY());
            }

            //  figure out the fixed and moving points based on which handle we're dragging
            if (handle== mDragHandleTopLeft) {
                mResizingFixedPoint = new Point(mResizingRect.right, mResizingRect.bottom);
                mResizingMovingPoint = new Point(mResizingRect.left, mResizingRect.top);
            }
            else {
                mResizingMovingPoint = new Point(mResizingRect.right, mResizingRect.bottom);
                mResizingFixedPoint = new Point(mResizingRect.left, mResizingRect.top);
            }

            //  remember where the handle and the moving point are.
            mDragHandlePointAtStart = new Point(handle.getPosition());
            mResizingMovingPointAtStart = new Point(mResizingMovingPoint);

            //  now position (and show) the resizing view.
            moveResizingView(mResizingFixedPoint, mResizingMovingPoint);
            mResizingView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onDrag(DragHandle handle)
        {
            //  how far has the handle been dragged?
            Point p = new Point(handle.getPosition());
            int dx = p.x - mDragHandlePointAtStart.x;
            int dy = p.y - mDragHandlePointAtStart.y;

            //  adjust the moving point accordingly
            mResizingMovingPoint.x = mResizingMovingPointAtStart.x + dx;
            mResizingMovingPoint.y = mResizingMovingPointAtStart.y + dy;

            //  position the resizing view.
            moveResizingView(mResizingFixedPoint, mResizingMovingPoint);
        }

        @Override
        public void onEndDrag(DragHandle handle)
        {
            if (sigEditingWidget !=null)
            {
                //  we've just finished resizing a signature. Set the bounds of the widget
                Rect r = mResizingPage.screenToPage(mResizingRect);
                sigEditingWidget.setBounds(r);
                MuPDFDoc mdoc = (MuPDFDoc) getDoc();
                mdoc.update(mResizingPage.getPageNumber());
                //  ... but keep the resizing view visible.
            }
            else
            {
                //  finalize the redaction's new position
                Rect r = mResizingPage.screenToPage(mResizingRect);
                MuPDFDoc mdoc = (MuPDFDoc) getDoc();
                mdoc.setSelectedObjectBounds(new RectF(r));

                //  remove the resizing view.
                mResizingView.setVisibility(View.GONE);
                mResizingPage = null;
            }

            dragging = false;
        }
    };

    @Override
    public void setupHandles(RelativeLayout layout)
    {
        super.setupHandles(layout);

        //  selection handles
        mDragHandleTopLeft = setupHandle(layout, DragHandle.RESIZE_TOP_LEFT);
        mDragHandleBottomRight = setupHandle(layout, DragHandle.RESIZE_BOTTOM_RIGHT);

        //  area view
        mResizingView = new View(getContext());
        mResizingView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.sodk_editor_redact_resize_color));
        layout.addView(mResizingView);
        mResizingView.setVisibility(View.GONE);
    }

    @Override
    protected boolean handleFullscreenTap(float fx, float fy)
    {
        Point p = eventToScreen(fx,fy);
        final DocPageView dpv = findPageViewContainingPoint(p.x, p.y, false);
        if (dpv==null)
            return false;
        p = dpv.screenToPage(p);
        return dpv.handleFullscreenTap(p.x, p.y);
    }

    //  for PDF files, we're not entertaining single-tap, since the text can't be edited.
    protected boolean canEditText() {return false;}

    protected boolean onSingleTap(float x, float y, DocPageView dpv)
    {
        DocPdfPageView dppv = (DocPdfPageView)dpv;

        if (mNoteMode && dppv!=null)
        {
            //  create a note
            dppv.createNote(x, y);

            //  ...  and then leave note mode.
            mNoteMode = false;
            return true;
        }

        if (mSignatureMode && dppv!=null && sigEditingWidget ==null)
        {
            //  create a digital signature field
            sigCreatedPage = dppv;
            dppv.createSignatureAt(x, y);

            //  ...  and then leave the mode.
            mSignatureMode = false;
            return true;
        }

        if (sigEditingWidget !=null)
        {
            //  we were resizing a signature.
            //  This tap will cancel that.
            clearSignatureEditing();
            mSignatureMode = false;
            return true;
        }

        return false;
    }

    protected void setDeletingPage(DocPdfPageView dpv)
    {
        sigDeletingPage = dpv;
    }

    protected void doReposition(DocPdfPageView pageView, final MuPDFWidget widget)
    {
        if (widget==null || pageView==null)
            return;

        //  set the widget and page that are being repositioned
        sigEditingWidget = widget;
        sigEditingPage = pageView;

        //  find the widget's rect
        Rect pageRect = sigEditingWidget.getBounds();
        sigEditingPage.pageToView(pageRect, mResizingRect);
        mResizingRect.offset(-getScrollX(), -getScrollY());

        //  set up and show the resizing view and handles
        mSelectionStartPage = mSelectionEndPage = mResizingPage = pageView;
        mResizingView.setVisibility(View.VISIBLE);
        moveResizingView(mResizingRect.left, mResizingRect.top, mResizingRect.width(), mResizingRect.height());
        showHandles();
    }

    @Override
    protected void doDoubleTap(float fx, float fy)
    {
        //  note: DocView.doDoubleTap() restricts double-tapping to editable docs.
        //  here we don't, we use it to select text.

        //  but if editing is configured off, we don't allow this.
        if (!mDocCfgOptions.isEditingEnabled())
            return;

        //  not if we're in full-screen
        if (((NUIDocView)mHostActivity).isFullScreen())
            return;

        doDoubleTap2(fx, fy);
    }

    @Override
    protected void showKeyboardAfterDoubleTap(Point p)
    {
    }

    @Override
    protected void updateReview()
    {
        if (getDoc() == null)
        {
            Log.e(TAG, "getDoc() returned NULL in updateReview");
            return;
        }

        if (getDoc().getSelectionHasAssociatedPopup())
        {
            //  show the note editor
            ArDkSelectionLimits limits = getSelectionLimits();
            mNoteEditor.show(limits, mSelectionStartPage);
            mNoteEditor.move();
            mNoteEditor.setCommentEditable(true);
            requestLayout();
        }
        else
        {
            //  hide the note editor and keyboard
            if (mNoteEditor !=null && mNoteEditor.isVisible())
            {
                Utilities.hideKeyboard(getContext());
                mNoteEditor.hide();
            }
        }
    }

    @Override
    protected boolean canSelectionSpanPages()
    {
        return false;
    }

    @Override
    public void onSelectionChanged()
    {
        super.onSelectionChanged();

        if (sigEditingWidget !=null) {
            //  we've just resized a signature.
            //  refresh its list of form fields to get new sizes.
            sigEditingPage.collectFormFields();
        }

        if (sigDeletingPage !=null) {
            //  we've just deleted a signature.
            //  refresh the list of form fields.
            sigDeletingPage.collectFormFields();
            sigDeletingPage = null;
        }

        if (sigCreatedPage !=null && sigEditingWidget ==null)
        {
            //  we've just created a signature. Re-evaluate the
            //  list of form fields to pick up the new one.
            sigCreatedPage.collectFormFields();

            //  get the latest field, which should be the one we just added.
            sigEditingWidget = sigCreatedPage.getNewestWidget();
            if (sigEditingWidget !=null)
            {
                sigEditingPage = sigCreatedPage;

                //  find the rect
                Rect pageRect = sigEditingWidget.getBounds();
                sigCreatedPage.pageToView(pageRect, mResizingRect);
                mResizingRect.offset(-getScrollX(), -getScrollY());

                //  move and show the resizing view
                mSelectionStartPage = mSelectionEndPage = mResizingPage = sigCreatedPage;
                mResizingView.setVisibility(View.VISIBLE);
                moveResizingView(mResizingRect.left, mResizingRect.top, mResizingRect.width(), mResizingRect.height());
                showHandles();
            }

            //  do all of the above just once.
            sigCreatedPage = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        if (finished())
            return;

        //  move the note editor to follow the note it's showing
        mNoteEditor.move();

        if (sigEditingWidget !=null && !dragging)
        {
            //  this layout come from scrolling.
            //  move the resizing view to match the signature widsget.
            Rect pageRect = sigEditingWidget.getBounds();
            mResizingPage.pageToView(pageRect, mResizingRect);
            mResizingRect.offset(-getScrollX(), -getScrollY());
            moveResizingView(mResizingRect.left, mResizingRect.top, mResizingRect.width(), mResizingRect.height());
        }
    }

    private boolean mNoteMode = false;
    public boolean getNoteMode() {return mNoteMode;}
    public void onNoteMode()
    {
        //  toggle the mode
        setNoteMode(!mNoteMode);
    }

    public void setNoteMode(boolean val)
    {
        mNoteMode = val;
        mDrawMode = false;
        mSignatureMode = false;
        clearAreaSelection();
        clearSignatureEditing();
        onSelectionChanged();
    }


    private boolean mSignatureMode = false;
    public boolean getSignatureMode() {return mSignatureMode;}
    public void onSignatureMode()
    {
        //  toggle the mode
        setSignatureMode(!mSignatureMode);
    }

    public void setSignatureMode(boolean val)
    {
        mSignatureMode = val;
        mNoteMode = false;
        mDrawMode = false;
        clearAreaSelection();
        clearSignatureEditing();
        onSelectionChanged();
    }

    private boolean mDrawMode = false;
    public boolean getDrawMode() {return mDrawMode;}
    public void onDrawMode()
    {
        mDrawMode = !mDrawMode;
        mNoteMode = false;
        mSignatureMode = false;

        if (!mDrawMode)
            saveInk();

        getDoc().clearSelection();
        onSelectionChanged();
    }

    public void setDrawModeOff()
    {
        mDrawMode = false;
        saveInk();
        clearAreaSelection();
        clearSignatureEditing();
        onSelectionChanged();
    }

    public void resetDrawMode()
    {
        //  cancel drawing mode, and save any drawing that's underway.
        mDrawMode = false;
        saveInk();
    }

    public void resetModes()
    {
        resetDrawMode();

        //  cancel note mode and save the note data.
        mNoteMode = false;
        if (mNoteEditor !=null && mNoteEditor.isVisible())
        {
            mNoteEditor.saveData();
            Utilities.hideKeyboard(getContext());
            mNoteEditor.hide();
        }

        clearAreaSelection();

        clearSignatureEditing();
        mSignatureMode = false;

        onSelectionChanged();
    }

    private void clearSignatureEditing()
    {
        sigEditingWidget = null;
        sigEditingPage = null;
        mResizingView.setVisibility(View.GONE);
        hideHandles();
    }

    private void moveResizingView(Point p1, Point p2)
    {
        //  this sets the position and size of the resizing view from two points that
        //  are assumed to be at opposite corners.

        int w = Math.abs(p1.x-p2.x);
        int h = Math.abs(p1.y-p2.y);
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        moveResizingView(x, y, w, h);
    }

    private void moveResizingView(int x, int y, int w, int h)
    {
        //  constrain the location to stay within the page.
        Rect newRect = new Rect(x, y, x + w, y + h);
        int loc[] = new int[2];
        getLocationOnScreen(loc);
        newRect.offset(loc[0], loc[1]);
        Rect pageRect = mResizingPage.screenRect();

        if (newRect.left < pageRect.left) {
            x = x + (pageRect.left - newRect.left);
        }
        if (newRect.right > pageRect.right) {
            x = x - (newRect.right - pageRect.right);
        }
        if (newRect.top < pageRect.top) {
            y = y + (pageRect.top - newRect.top);
        }
        if (newRect.bottom > pageRect.bottom) {
            y = y - (newRect.bottom - pageRect.bottom);
        }

        //  keep track of the size, used when dragging or resizing ends
        mResizingRect.set(newRect);

        //  move and size the view by adjusting it's layout parameters
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)mResizingView.getLayoutParams();
        int bottomMargin = 0;
        int diff = y+h - getHeight();
        if (diff>0)
            bottomMargin = -diff;
        int rightMargin = 0;
        diff = x+w - getWidth();
        if (diff>0)
            rightMargin = -diff;
        layoutParams.setMargins(x, y, rightMargin, bottomMargin);
        layoutParams.width = w;
        layoutParams.height = h;
        mResizingView.setLayoutParams(layoutParams);
        mResizingView.invalidate();

        //  be sure the view is showing
        mResizingView.setVisibility(View.VISIBLE);
    }

    private void moveHandlesToResizingRect()
    {
        Point topLeft = mResizingPage.screenToPage(new Point(mResizingRect.left, mResizingRect.top));
        Point bottomRight = mResizingPage.screenToPage(new Point(mResizingRect.right, mResizingRect.bottom));
        positionHandle(mDragHandleTopLeft, mResizingPage, topLeft.x, topLeft.y);
        positionHandle(mDragHandleBottomRight, mResizingPage, bottomRight.x, bottomRight.y);
    }

    public boolean onTouchEventMarkArea(MotionEvent event)
    {
        //  this function is used when we're drawing a new redaction area.

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:

                //  what page?
                Point p = eventToScreen(event.getX(), event.getY());
                mResizingPage = findPageViewContainingPoint(p.x, p.y, false);

                //  start with the corners the same.
                mResizingFixedPoint = new Point((int)event.getX(), (int)event.getY());
                mResizingMovingPoint = new Point((int)event.getX(), (int)event.getY());

                //  move and show the resizing view
                moveResizingView(mResizingFixedPoint, mResizingMovingPoint);
                mResizingView.setVisibility(View.VISIBLE);

                break;

            case MotionEvent.ACTION_MOVE:

                //  change the moving point and resize the view
                mResizingMovingPoint = new Point((int)event.getX(), (int)event.getY());
                moveResizingView(mResizingFixedPoint, mResizingMovingPoint);

                break;

            case MotionEvent.ACTION_UP:

                //  create the redaction and select it.
                int index = ((DocMuPdfPageView)mResizingPage).addRedactAnnotation(mResizingRect);
                if (index!=-1) {
                    MuPDFDoc mupdfDoc = (MuPDFDoc)getDoc();
                    mupdfDoc.setSelectedAnnotation(mResizingPage.getPageNumber(), index);
                    mupdfDoc.setSelectionStartPage(mResizingPage.getPageNumber());
                    mupdfDoc.setSelectionEndPage(mResizingPage.getPageNumber());

                }

                //  hide the resizing view, set marking mode off
                mResizingView.setVisibility(View.GONE);
                mMarkAreaMode = false;
                mResizingPage = null;

                break;
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //  prevent further touch event if this view is done.
        if (finished())
            return true;

        if (getMarkAreaMode()) {
            //  if we're in area marking mode, use the alternative method, above.
            return onTouchEventMarkArea(event);
        }

        if (getDrawMode())
        {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    break;
            }

            return true;
        }

        return super.onTouchEvent(event);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 2;
    DocPdfPageView mDrawingPage = null;
    ArrayList<DocPdfPageView> mDrawingPageList = new ArrayList<>();

    private int mCurrentInkLineColor = 0;
    public int getInkLineColor()
    {
        if (mCurrentInkLineColor == 0) {
            if (mDocCfgOptions!=null) {
                mCurrentInkLineColor = mDocCfgOptions.getDefaultPdfInkAnnotationDefaultLineColor();
            }
        }
        if (mCurrentInkLineColor == 0)
            mCurrentInkLineColor = 0xffff0000;  //  red

        return mCurrentInkLineColor;
    }
    public void setInkLineColor(int val)
    {
        mCurrentInkLineColor=val;

        //  also set for not-yet-committed annotations
        for (DocPdfPageView page:mDrawingPageList)
            page.setInkLineColor(val);
    }

    private float mCurrentInkLineThickness = 0;
    public float getInkLineThickness()
    {
        if (mCurrentInkLineThickness == 0) {
            if (mDocCfgOptions!=null) {
                mCurrentInkLineThickness = mDocCfgOptions.getDefaultPdfInkAnnotationDefaultLineThickness();
            }
        }
        if (mCurrentInkLineThickness == 0)
            mCurrentInkLineThickness = 4.5f;

        return mCurrentInkLineThickness;
    }
    public void setInkLineThickness(float val)
    {
        mCurrentInkLineThickness=val;

        //  also set for not-yet-committed annotations
        for (DocPdfPageView page:mDrawingPageList)
            page.setInkLineThickness(val);
    }

    private void touch_start(float x, float y)
    {
        Point p = eventToScreen(x, y);
        mDrawingPage = (DocPdfPageView)findPageViewContainingPoint(p.x, p.y, false);
        if (mDrawingPage != null)
        {
            mDrawingPage.startDrawInk(p.x, p.y, getInkLineColor(), getInkLineThickness());
            if (!mDrawingPageList.contains(mDrawingPage))
                mDrawingPageList.add(mDrawingPage);

            //  update ui
            mHostActivity.selectionupdated();
        }

        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {

        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
        {
            Point p = eventToScreen(x, y);
            if (mDrawingPage != null)
            {
                mDrawingPage.continueDrawInk(p.x, p.y);
            }
            mX = x;
            mY = y;
        }
    }

    private void touch_up()
    {
        if (mDrawingPage != null)
        {
            mDrawingPage.endDrawInk();
        }
    }

    public void saveInk()
    {
        for (DocPdfPageView page:mDrawingPageList)
            page.saveInk();

        mDrawingPageList.clear();
        mDrawingPage = null;
    }

    //  clear any not-yet-saved ink annotations
    public void clearInk()
    {
        for (DocPdfPageView page:mDrawingPageList)
            page.clearInk();
        mDrawingPageList.clear();
    }

    //  tell if there are not-yet-saved ink annotations
    public boolean hasNotSavedInk()
    {
        if (mDrawingPageList!=null && mDrawingPageList.size()>0)
            return true;
        return false;
    }

    @Override
    public void onReloadFile()
    {
        for (int i=0; i<getPageCount(); i++)
        {
            DocMuPdfPageView cv = (DocMuPdfPageView)getOrCreateChild(i);
            cv.onReloadFile();
        }
    }

    @Override
    protected void hideHandles()
    {
        super.hideHandles();

        showHandle(mDragHandleTopLeft, false);
        showHandle(mDragHandleBottomRight, false);
    }

    @Override
    protected void showHandles()
    {
        hideHandles();

        MuPDFDoc mdoc = (MuPDFDoc)getDoc();
        boolean isRedaction = mdoc.selectionIsRedaction();

        if (isRedaction || sigEditingWidget !=null)
        {
            boolean show = true;
            showHandle(mDragHandleTopLeft, show);
            showHandle(mDragHandleBottomRight, show);
            moveHandlesToCorners();
        }
        else
        {
            super.showHandles();
        }
    }

    @Override
    protected void moveHandlesToCorners()
    {
        super.moveHandlesToCorners();

        MuPDFDoc mdoc = (MuPDFDoc)getDoc();
        boolean isRedaction = mdoc.selectionIsRedaction();

        if (isRedaction)
        {
            //  move handles to match the selection
            ArDkSelectionLimits limits = getSelectionLimits();
            if (limits!=null)
            {
                positionHandle(mDragHandleTopLeft,     mSelectionStartPage, (int)limits.getStart().x, (int)limits.getStart().y);
                positionHandle(mDragHandleBottomRight, mSelectionEndPage,   (int)limits.getEnd().x,   (int)limits.getEnd().y);
            }
        }

        if (sigEditingWidget !=null)
        {
            //  move handles to match the signature widget
            Rect r = sigEditingWidget.getBounds();
            positionHandle(mDragHandleTopLeft,     mSelectionStartPage, r.left, r.top);
            positionHandle(mDragHandleBottomRight, mSelectionEndPage,   r.right, r.bottom);
        }
    }
}
