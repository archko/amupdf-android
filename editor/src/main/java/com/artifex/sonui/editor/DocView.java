package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import com.artifex.solib.ArDkBitmap;
import com.artifex.solib.ArDkPage;
import com.artifex.solib.ArDkSelectionLimits;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SODoc;
import com.artifex.solib.SORenderListener;

import java.lang.Boolean;
import java.util.ArrayList;

public class DocView
        extends AdapterView<Adapter>
        implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, Runnable,
        DragHandleListener
{
    private static final String TAG          = "DocView";
    protected static final int    UNSCALED_GAP = 20;

    protected float minScale() {return .15f;}
    protected float maxScale() {return 5.0f;}

    private PageAdapter mAdapter;
    private boolean mFinished = false;

    private final SparseArray<View> mChildViews = new SparseArray<View>(3);

    protected boolean         mScaling;    // Whether the user is currently pinch zooming
    protected float           mScale     = 1.0f;
    public float getScale() {return mScale;}
    private int               mXScroll;    // Scroll amounts recorded from events.
    private int               mYScroll;    // and then accounted for in onLayout
    private boolean           mTouching = false;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    //  bitmaps for rendering
    //  these are created by the activity and set using setBitmaps()
    private ArDkBitmap[] bitmaps;

    private int bitmapIndex = 0;
    private boolean renderRequested = false;
    private int renderCount = 0;

    //  used during layout
    private final Rect mChildRect = new Rect();
    private final Rect mViewport = new Rect();
    private final Point mViewportOrigin = new Point();
    protected final Rect mAllPagesRect = new Rect();
    private final Rect mLastAllPagesRect = new Rect();
    protected int mLastLayoutColumns = 1;
    protected int lastMostVisibleChild = -1;

    private static   final int SMOOTH_SCROLL_TIME = 400;

    private Scroller          mScroller;
    private int               mScrollerLastX;
    private int               mScrollerLastY;
    private Smoother          mSmoother;

    //  for single- and double-tapping
    private long mLastTapTime = 0;
    private float lastTapX;
    private float lastTapY;
    private int mTapStatus = 0;

    private static final int DOUBLE_TAP_TIME = 300;
    private static final int SHOW_KEYBOARD_TIME = 500;

    //  the document.
    private ArDkDoc mDoc;

    //  selection handles
    private DragHandle mSelectionHandleTopLeft = null;
    private DragHandle mSelectionHandleBottomRight = null;

    //  resize handles
    private DragHandle mResizeHandleTopLeft = null;
    private DragHandle mResizeHandleTopRight = null;
    private DragHandle mResizeHandleBottomLeft = null;
    private DragHandle mResizeHandleBottomRight = null;

    // true if the last selection was (alterable) text.
    private Boolean mLastSelIsAltText = null;

    //  drag handle
    private DragHandle mDragHandle = null;

    //  rotate handle
    private DragHandle mRotateHandle = null;

    //  resizing view
    private ImageView mResizingView;
    //  bounds of the view as it's being resized
    private Rect mResizeRect = new Rect();
    private Rect mResizeOrigRect = new Rect();
    //  bitmap collected when resizing begins
    private ArDkBitmap mResizingBitmap = null;
    //  top/left of selection when resizing begins
    private Point mResizeOrigTopLeft;
    //  bottom/right of selection when resizing begins
    private Point mResizeOrigBottomRight;
    //  aspect ratio when resizing begins
    private float mResizeAspect;
    //  location of drag handle when dragging begins
    private Point mDragOrigLocation = new Point();
    //  bounds in page coordinates of an object when dragging it begins
    private RectF mDraggingObjectPageBounds;

    private float mRotateAngle = 0;
    private float mNAdditionalAngle = 0;

    private PointF mNatDim;
    //  min resizing dimension
    private final static int RESIZE_MIN_DIMENSION = 100;

    //  pages containing the start and end of the current selection.
    protected DocPageView mSelectionStartPage;
    public DocPageView getSelectionStartPage() {return mSelectionStartPage;}
    protected DocPageView mSelectionEndPage;
    public DocPageView getSelectionEndPage() {return mSelectionEndPage;}

    //  on each layout, compute the page that has the most visible height.
    //  That's considered to be the "current" page for purposes of drawing the blue dot
    //  on the pages list.
    private int mostVisibleChild = -1;
    private final Rect mostVisibleRect = new Rect();

    //  instance of the Note Editor
    private NoteEditor mNoteEditor = null;

    //  use these to reduce unnecessary layout calls.
    private boolean mForceLayout = false;
    private float mLastScale = 0;
    private int mLastScrollX = 0;
    private int mLastScrollY = 0;

    //  data to handle press-zooming
    protected boolean mPressing = false;
    private boolean mMoving = false;
    private boolean mPressZooming = false;
    private boolean mDonePressZooming = false;
    private float mPressStartScale;
    private int mPressStartX;
    private int mPressStartY;
    private int mPressStartViewX;
    private int mPressStartViewY;
    private DocPageView mPressPage;

    private static int ZOOM_PERIOD = 10;
    private static int ZOOM_DURATION = 250;
    private static float ZOOM_FACTOR = 2.5f;
    private static float MOVE_THRESHOLD = 20f;   //  this value is dp

    //  indicates whether scrolling is currentlty being constrained
    private boolean mConstrained = true;

    //  widest page width (unscaled)
    private int unscaledMaxw = 0;

    //  for page rearranging
    private int mDropPageAbove = -1;
    private int mDropPageBelow = -1;
    private int dropY = -1;

    // configuration options
    protected ConfigOptions mDocCfgOptions = null;

    // dataleak handlers
    protected SODataLeakHandlers mDataLeakHandlers = null;

    //  set this to temporarily force layout to use a number of columns.
    protected int mForceColumnCount = -1;

    //  keep track of the last reflow width
    public float mLastReflowWidth = 0;

    //  history
    private History mHistory;
    public History getHistory() {return mHistory;}

    public void setDoc(ArDkDoc doc)
    {
        mDoc=doc;
    }
    protected ArDkDoc getDoc() {return mDoc;}

    public DocView(Context context) {
        super(context);
        initialize(context);
    }

    public DocView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public DocView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    protected DocViewHost mHostActivity = null;

    protected void initialize(Context context)
    {
        //  set the view background color
        int bgColor = ContextCompat.getColor(getContext(), R.color.sodk_editor_doc_background);
        setBackgroundColor(bgColor);

        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mScroller = new Scroller(context);
        mSmoother = new Smoother(3);

        //  create the history object
        mHistory = new History();

        //  this doesn't prevent scrolling, but it may prevent
        //  the entire activity from being pushed out of place when the Note Editor gains focus.
        setScrollContainer(false);
    }

    public void setHost(DocViewHost base) {mHostActivity = base;}

    public void setDocSpecifics(ConfigOptions configOptions, SODataLeakHandlers leakHandlers)
    {
        mDocCfgOptions = configOptions;
        mDataLeakHandlers = leakHandlers;
    }

    public SODataLeakHandlers getDataLeakHandlers()
    {
        return mDataLeakHandlers;
    }

    public ConfigOptions getDocConfigOptions()
    {
        return mDocCfgOptions;
    }

    public void setBitmaps(ArDkBitmap[] bm)
    {
        //  if we were given null bitmaps, that means we're not valid for drawing,
        if (bm[0]==null)
            setValid(false);
        else
            setValid(true);

        //  just keep a reference to the caller's array.
        bitmaps = bm;
    }

    public void releaseBitmaps()
    {
        //  release our reference.
        bitmaps = null;
    }

    public void setValid(boolean valid)
    {
        //  set the validity of each page
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView page = (DocPageView)getOrCreateChild(i);
            page.setValid(valid);
        }
    }

    private void onScaleChild(View v, Float scale)
    {
        ((DocPageView)v).setNewScale(scale);
    }

    public void onConfigurationChange()
    {
        //  config was changed, probably orientation.

        //  reset background color of visible pages
        for (int i = 0; i < getPageCount(); i++)
        {
            DocPageView cv = (DocPageView) getOrCreateChild(i);
            if (cv.getParent()!=null && cv.isShown()) {
                cv.resetBackground();
            }
        }

        //  this is the earliest point in the rotation scenario where we can
        //  collect the desired number of columns.
        //  It is used in onLayout() until we set it back to -1

        mForceColumnCount = mLastLayoutColumns;

        //  trigger an orientation change at a later time
        ((NUIDocView)mHostActivity).triggerOrientationChange();
    }

    private InputView inputView()
    {
        return ((NUIDocView)mHostActivity).getInputView();
    }

    private int mLastViewPortWidth = 0;

    public void onOrientationChange()
    {
        //  get measurements
        final Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        int viewportWidth = viewport.width();
        int contentWidth = mAllPagesRect.width();

        if (contentWidth <= 0 || viewportWidth <=0)
        {
            //  We were apparently called before any pages were laid out.
            //  Try this again after another layout
            requestLayout();
            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);
                    onOrientationChange();
                }
            });

            return;
        }

        /*
         * There appear to be spurious calls to this function, generated
         * from onGlobalLayout() above.
         *
         * In these spurious calls the viewportWidth is the same as for
         * the valid call. This results in the document content being
         * scaled multiple times.
         *
         * To avoid this incorrect scaling we note the viewportWidth
         * on each call and take no action if it has not changed.
         */
        if (mLastViewPortWidth == viewportWidth) {
            return;
        }
        mLastViewPortWidth = viewportWidth;

        //  if we're at one column and wider than the viewport, leave it alone.
        if (!mReflowMode && mLastLayoutColumns==0 && contentWidth>=viewportWidth)
        {
            //  done forcing the column count
            mForceColumnCount = -1;
            requestLayout();  //  fix 702512
            return;
        }

        //  fit to width factor
        final float factor = ((float) viewportWidth)/((float) contentWidth);
        final int scrollY = getScrollY();

        //  scale the children and do a layout
        mScale *= factor;
        scaleChildren();
        requestLayout();

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);

                //  after the layout, scroll to a new y-location
                int dy = scrollY - (int)(factor* scrollY);
                scrollBy(0, -dy);  //  this should constrain

                //  done forcing the column count
                mForceColumnCount = -1;

                layoutNow();
            }
        });

    }

    //  track whether the page list is showing
    private boolean mPagesShowing = false;
    public boolean pagesShowing() {return mPagesShowing;}

    public void onShowPages()
    {
        int page_width_percentage = getContext().getResources().getInteger(R.integer.sodk_editor_page_width_percentage);
        int pagelist_width_percentage = getContext().getResources().getInteger(R.integer.sodk_editor_pagelist_width_percentage);

        onSizeChange((float)(page_width_percentage)/(float)(page_width_percentage+pagelist_width_percentage));

        mPagesShowing = true;
    }

    public void onHidePages()
    {
        int page_width_percentage = getContext().getResources().getInteger(R.integer.sodk_editor_page_width_percentage);
        int pagelist_width_percentage = getContext().getResources().getInteger(R.integer.sodk_editor_pagelist_width_percentage);

        onSizeChange((float)(page_width_percentage+pagelist_width_percentage)/(float)(page_width_percentage));

        mPagesShowing = false;
    }

    private void onSizeChange(float factor)
    {
        //  scale the children
        mScale *= factor;
        scaleChildren();

        //  scale the Y location
        int y = getScrollY();
        y *= factor;
        scrollTo(getScrollX(), y);

        requestLayout();
    }

    //  set to true if a tap was used to stop scrolling
    private boolean mScrollingStopped = false;

    public boolean onDown(MotionEvent arg0)
    {
        if (!mScroller.isFinished())
        {
            //  we were scrolling, so stop it
            //  this will get set back to false in onSingleTapUp()
            mScrollingStopped = true;
            mScroller.forceFinished(true);
        }

        return true;
    }

    private boolean flinging = false;
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        //  not while we're scaling
        if (mScaling)
            return true;

        //  not while a previous fling is underway
        if (!mScroller.isFinished())
            return true;

        flinging = true;

        int velx = (int)velocityX;
        int vely = (int)velocityY;

        //  x-scrolling may not be allowed.
        if (!allowXScroll())
            velx = 0;

        mSmoother.clear();
        mScroller.forceFinished(true);
        mScrollerLastX = mScrollerLastY = 0;
        mScroller.fling(0, 0, velx, vely, -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.post(this);

        return true;
    }

    protected boolean allowXScroll()
    {
        if (mReflowMode)
            return false;
        return true;
    }

    public void onLongPress(MotionEvent e)
    {
        //  see if there is a page
        Point pScreen = viewToScreen(new Point((int) e.getX(), (int) e.getY()));
        DocPageView page = findPageViewContainingPoint(pScreen.x, pScreen.y, false);
        if (page == null)
            return;

        //  start
        Utilities.hideKeyboard(getContext());
        mPressing = true;
        mPressStartScale = mScale;
        mPressStartX = (int) e.getX();
        mPressStartY = (int) e.getY();
        mPressStartViewX = mPressStartX + getScrollX();
        mPressStartViewY = mPressStartY + getScrollY();
        zoomWhilePressing(mScale, mScale * ZOOM_FACTOR, false);
    }

    public void onLongPressMoving(MotionEvent e)
    {
        //  not if we can't edit text
        if (!canEditText())
            return;

        //  not while we're doing the zooming
        if (mPressZooming)
            return;

        //  find the page this event is in
        Point p = eventToScreen((int)e.getX(), (int)e.getY());
        mPressPage = findPageViewContainingPoint(p.x, p.y, false);

        //  not if there's no page
        if (mPressPage == null)
            return;

        //  test a moving threshold
        if (!mMoving) {
            float moveThreshold = Utilities.convertDpToPixel(MOVE_THRESHOLD);
            if (Math.abs((e.getX()-mPressStartX))>= moveThreshold || Math.abs((e.getY()-mPressStartY))>= moveThreshold) {
                mMoving = true;
            }
        }

        if (mMoving)
        {
            //  do the same thing as if the page were single-tapped.
            //  this has the effect of placing the caret.

            //  do this slightly above the finger position.  The value
            //  is the approximate size of a finger pad in dp.
            int above = Utilities.convertDpToPixel(35);

            mPressPage.setCaret(p.x, p.y-above);

            //  seems to be necessary in the 2nd split (but not the first)
            forceLayout();

            focusInputView();
            updateInputView();
        }
    }

    public void onLongPressRelease()
    {
        //  we're done with the long press, so zoom back.
        zoomWhilePressing(mScale, mPressStartScale, true);
    }

    public void onLongPressReleaseDone()
    {
        ArDkSelectionLimits sel = getSelectionLimits();
        if (sel == null )
        {
            Log.e(TAG, "NULL selection limits in onLongPressReleaseDone");
        }
        else if (sel.getIsActive() && mPressPage!=null)
        {
            //  scroll to selection after the keyboard shows
            scrollToSelection = mHostActivity.showKeyboard();
        }
    }

    protected void zoomWhilePressing(final float startScale, final float endScale, final boolean release)
    {
        //  we are zooming
        mPressZooming = true;
        mDonePressZooming = false;

        final long startTime = System.currentTimeMillis();

        final DocView thisDocView = this;

        final Handler handler = new Handler();
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                final Runnable thisRunnable = this;

                long now = System.currentTimeMillis();
                if (!mDonePressZooming && now>startTime+ZOOM_DURATION)
                {
                    //  make sure we finish at exactly the right time.
                    now = startTime+ZOOM_DURATION;
                    mDonePressZooming = true;
                }

                if (now <= startTime+ZOOM_DURATION)
                {
                    //  new scale value
                    float ratio = ((float)(now-startTime))/((float)ZOOM_DURATION);
                    mScale = startScale + (endScale-startScale)*ratio;
                    scaleChildren();

                    //  keep the pressed point at the same place
                    int viewFocusX = (int)(mPressStartViewX * mScale / mPressStartScale);
                    int viewFocusY = (int)(mPressStartViewY * mScale / mPressStartScale);
                    int newScrollX =  viewFocusX - mPressStartX;
                    int newScrollY =  viewFocusY - mPressStartY;

                    int dx = newScrollX - getScrollX();
                    int dy = newScrollY - getScrollY();
                    thisDocView.mXScroll -= dx;
                    thisDocView.mYScroll -= dy;

                    //  request a layout and wait for it
                    requestLayout();
                    final ViewTreeObserver observer = getViewTreeObserver();
                    observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            observer.removeOnGlobalLayoutListener(this);
                            //  do another pass immediately
                            handler.post(thisRunnable);
                        }
                    });
                }
                else
                {
                    //  transition is done
                    mPressZooming = false;
                    if (release) {
                        mPressing = false;
                        mTouching = false;
                        onLongPressReleaseDone();
                    }
                }
            }
        };
        //  transition is starting
        handler.postDelayed(runnable, ZOOM_PERIOD);
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        //  not if we're scaling
        if (mScaling)
            return true;

        //  not if we're long-pressing
        if (mPressing)
            return true;

        //  not if we're dragging
        if (mDragging)
            return true;

        //  not if a previous fling is underway
        if (!mScroller.isFinished())
            return true;

        //  in reflow mode we are x-constrained.
        if (mReflowMode)
            distanceX = 0;

        //  accumulate scrolling amount.
        mXScroll -= distanceX;
        mYScroll -= distanceY;

        requestLayout();

        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    protected DocPageView findPageContainingSelection()
    {
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView child = (DocPageView)getOrCreateChild(i);
            if (child != null)
            {
                ArDkSelectionLimits limits = child.selectionLimits();
                if (limits != null && limits.getIsActive() && limits.getHasSelectionStart())
                {
                    return child;
                }
            }
        }

        return null;
    }

    protected DocPageView findPageViewContainingPoint(int x, int y, boolean includeMargin)
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            //  get the rect for the page
            View child = getChildAt(i);
            Rect childRect = new Rect();
            child.getGlobalVisibleRect(childRect);

            //  add in the margin
            if (includeMargin)
            {
                childRect.left   -= UNSCALED_GAP*mScale/2;
                childRect.right  += UNSCALED_GAP*mScale/2;
                childRect.top    -= UNSCALED_GAP*mScale/2;
                childRect.bottom += UNSCALED_GAP*mScale/2;
            }

            //  see if the rect contains the point
            if (childRect.contains(x,y))
                return (DocPageView)child;
        }

        return null;
    }

    protected Point eventToScreen(float fx, float fy)
    {
        int x = Math.round(fx);
        int y = Math.round(fy);
        Rect docRect = new Rect();
        getGlobalVisibleRect(docRect);
        x += docRect.left;
        y += docRect.top;

        return new Point(x,y);
    }

    protected boolean clearAreaSelection()
    {
        boolean cleared = false;

        ArDkSelectionLimits limits = getSelectionLimits();
        if (limits!=null && !limits.getIsCaret() && (limits.getHasSelectionStart() || limits.getHasSelectionEnd()))
        {
            mDoc.clearSelection();
            cleared = true;
        }

        return cleared;
    }

    protected boolean canEditText()
    {
        if (mDocCfgOptions != null)
        {
            return mDocCfgOptions.isEditingEnabled();
        }

        return true;
    }

    public void selectTopLeft()
    {
        DocPageView dpv = (DocPageView)getChildAt(0);
        dpv.selectTopLeft();
    }

    protected boolean shouldPreclearSelection() {return true;}

    protected boolean onSingleTap(float x, float y, DocPageView dpv)
    {
        return false;
    }

    protected boolean handleFullscreenTap(float fx, float fy)
    {
        return false;
    }

    public void onFullscreen(boolean bFull)
    {
        if (bFull)
            getDoc().clearSelection();

        //  tell the children
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView cv = (DocPageView)getOrCreateChild(i);
            cv.onFullscreen(bFull);
        }
    }

    protected void doSingleTap(float fx, float fy)
    {
        //  see if we should exit full screen mode
        if (((NUIDocView)mHostActivity).isFullScreen())
        {
            if (!handleFullscreenTap(fx, fy))
            {
                //  not otherwise handled, so exit full-screen
                //  if config options allows
                if (mDocCfgOptions.showUI())
                    ((NUIDocView)mHostActivity).showUI(true);
                return;
            }
        }

        //  no single-tapping in non-office docs
        if (mHostActivity instanceof NUIDocViewOther)
            return;

        //  NOTE: when double-tapping, a single-tap will also happen first.
        //  so that must be safe to do.

        //  if we select, then stop any outstanding search, otherwise we get crashing.
        //  I think searching affects selection in some way, so let's keep them
        //  separate.
        getDoc().closeSearch();

        //  if the note editor is showing, dismiss it
        if (mNoteEditor!=null && mNoteEditor.isVisible())
        {
            Utilities.hideKeyboard(getContext());
            mNoteEditor.hide();
            return;
        }

        //  first clear the area selection
        //  if there was something to clear, do nothing more.
        if (shouldPreclearSelection()) {
            boolean cleared = clearAreaSelection();
            if (cleared)
            {
                Utilities.hideKeyboard(getContext());
                return;
            }
        }

        //  find the page view that was tapped.
        Point p = eventToScreen(fx,fy);
        final DocPageView dpv = findPageViewContainingPoint(p.x, p.y, false);
        if (dpv==null)
            return;

        //  do we want to handle it?
        if (onSingleTap(p.x, p.y, dpv))
        {
            return;
        }

        //  give setFocus to the input view (this will create the view's input connection)
        if (canEditText() && tapToFocus())
            focusInputView();

        //  see if the page wants to handle the single tap
        boolean handled = dpv.onSingleTap(p.x, p.y, canEditText(), new DocPageView.ExternalLinkListener() {
            @Override
            public void handleExternalLink(int pageNum, Rect bbox)
            {
                //  Add a history entry for the spot we're leaving.
                addHistory(getScrollX(), getScrollY(), mScale, true);

                //  add a history entry for the destination.
                int dy = scrollBoxToTopAmount(pageNum, new RectF(bbox.left, bbox.top, bbox.right, bbox.bottom));
                addHistory(getScrollX(), getScrollY()-dy, mScale, false);

                //  go there
                RectF bboxf = new RectF(bbox.left, bbox.top, bbox.right, bbox.bottom);
                scrollBoxToTop(pageNum, bboxf);
            }
        });

        //  if not, ...
        if (!handled)
        {
            // no keyboard is required, if not editable.
            if (!canEditText())
                return;

            //  schedule a task in the near future to check if we're still a single-tap.
            final Handler handler = new Handler();
            final Point tappedPoint = p;
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    //  in case this runs after the doc is closed.
                    if (finished())
                        return;

                    if (mTapStatus==1 && canEditText())
                    {
                        //  still single, so show keyboard
                        ArDkSelectionLimits sel = dpv.selectionLimits();
                        if (sel.getIsActive())
                        {
                            //  scroll to selection after the keyboard shows
                            scrollToSelection = mHostActivity.showKeyboard();
                        }
                        else
                        {
                            //  show keyboard and scroll so tapped point is visible
                            showKeyboardAndScroll(tappedPoint);
                        }

                        /*
                         * Update the InputView with the text around the
                         * current caret.
                         */
                        updateInputView();
                    }
                    else
                    {
                        if (Utilities.isLandscapePhone(getContext()))
                            Utilities.hideKeyboard(getContext());
                    }
                    mTapStatus = 0;
                }
            }, SHOW_KEYBOARD_TIME);
        }
    }

    protected void addHistory(int scrollX, int scrollY, float scale, boolean check)
    {
        boolean add = true;
        if (check && !shouldAddHistory(scrollX, scrollY, scale))
            add = false;

        if (add)
            getHistory().add(scrollX, scrollY, scale);

        //  get the outer view to update its UI
        mHostActivity.updateUI();
    }

    protected boolean shouldAddHistory(int scrollX, int scrollY, float scale)
    {
        //  get the current item
        //  if there is none, it's ok to add
        History.HistoryItem item = getHistory().current();
        if (item==null)
            return true;

        Rect r1=new Rect();
        getLocalVisibleRect(r1);
        r1.offset(item.getScrollX(), item.getScrollY());
        Rect r2=new Rect();
        getLocalVisibleRect(r2);
        r2.offset(scrollX, scrollY);

        //  do they overlap? If so, we don't add history
        //  use a copy of 'r1' because intersect() will change it
        //  TODO: scale?
        if (new Rect(r1).intersect(r2))
            return false;

        return true;
    }

    protected boolean tapToFocus() {return true;}

    protected void doDoubleTap(float fx, float fy)
    {
        //  note : Just like DocPdfView.doDoubleTap() does,
        //  we don't restrict double-tapping to editable docs.
        ////if (!canEditText())
        ////    return;

        //  not while we're in full-screen
        if (((NUIDocView)mHostActivity).isFullScreen())
            return;

        doDoubleTap2(fx, fy);
    }

    protected void doDoubleTap2(float fx, float fy)
    {
        //  no double-tapping in non-office docs
        if (mHostActivity instanceof NUIDocViewOther)
            return;

        Point p = eventToScreen(fx,fy);
        DocPageView v = findPageViewContainingPoint(p.x, p.y, false);
        if (v != null && v.canDoubleTap(p.x, p.y))
        {
            v.onDoubleTap(p.x, p.y);

            if (!canEditText())
            {
                // don't care about input/keyboard stuff
                // we reached here only to make selection.
                return;
            }

            focusInputView();

            // Ensure the IME is updated to match the new selection.
            updateInputView();

            showKeyboardAfterDoubleTap(p);
        }
    }

    protected void showKeyboardAfterDoubleTap(Point p)
    {
        if (!Utilities.isLandscapePhone(getContext()))
        {
            showKeyboardAndScroll(p);
        }
    }

    public boolean onSingleTapUp(final MotionEvent e)
    {
        long now = System.currentTimeMillis();
        if (mLastTapTime!=0 && ((now-mLastTapTime)<DOUBLE_TAP_TIME))
        {
            mTapStatus = 2;
            if (!mScrollingStopped)
                doDoubleTap(lastTapX,lastTapY);
            mLastTapTime = 0;
        }
        else
        {
            mLastTapTime = now;
            lastTapX = e.getX();
            lastTapY = e.getY();
            if (!mScrollingStopped)
                doSingleTap(lastTapX, lastTapY);
            mTapStatus = 1;
        }

        mScrollingStopped = false;

        return false;
    }

    private Point scrollToHere;
    private DocPageView scrollToHerePage;
    private boolean scrollToSelection = false;

    protected void showKeyboardAndScroll(final Point p)
    {
        //  find page under point
        DocPageView dpv = findPageViewContainingPoint(p.x, p.y, false);
        if (dpv == null)
            return;

        //  show the keyboard and remember where we must scroll to when it finally appears.
        scrollToHerePage = null;
        scrollToHere = null;
        if (mHostActivity.showKeyboard())
        {
            scrollToHerePage = dpv;
            scrollToHere = dpv.screenToPage(p);
        }
    }

    //  interface: listen for keyboard appearance changes
    interface ShowKeyboardListener {
        void onShow(boolean bShow);
    }
    private ShowKeyboardListener showKeyboardListener = null;
    public void setShowKeyboardListener(ShowKeyboardListener listener) {showKeyboardListener = listener;}

    public void onShowKeyboard(boolean bShow)
    {
        if (bShow)
        {
            if (scrollToHere!=null && scrollToHerePage!=null)
            {
                RectF box = new RectF(scrollToHere.x, scrollToHere.y, scrollToHere.x+1, scrollToHere.y+1);
                int page = scrollToHerePage.getPageNumber();
                scrollBoxIntoView(page, box);

                scrollToHere = null;
                scrollToHerePage = null;
            }

            if (scrollToSelection)
            {
                scrollSelectionIntoView();
                scrollToSelection = false;
            }
        }
        else
        {
            //  have the note editor do a save
            if (mNoteEditor != null)
                mNoteEditor.preMoving();

            //  keyboard got hidden.  force a layout.
            mForceLayout = true;
            requestLayout();
        }

        //  call the registered keyboard appearance listener
        if (showKeyboardListener != null)
            showKeyboardListener.onShow(bShow);
    }

    public void layoutNow()
    {
        mForceLayout = true;
        requestLayout();
    }

    public void scrollPointVisible(Point point)
    {
        //  scroll so that the point is inside the visible rectangle.

        Rect r = new Rect();
        getGlobalVisibleRect(r);
        if (point.y<r.top || point.y>(r.top + r.bottom)/2)
        {
            int diff = (r.top + r.bottom)/2 - point.y;
            smoothScrollBy(0,diff);
        }
    }

    protected void scaleChildren()
    {
        //  scale children
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView cv = (DocPageView)getOrCreateChild(i);
            cv.setNewScale(mScale);
        }
    }

    public void setScale(float val)
    {
        mScale = val;
    }

    public boolean onScale(ScaleGestureDetector detector)
    {
        //  not while we're long-pressing
        if (mPressing)
            return true;

        //  new scale factor
        float previousScale = mScale;
        mScale = Math.min(Math.max(mScale * detector.getScaleFactor(), minScale()), maxScale());

        //  did we really scale?
        if (mScale == previousScale)
            return true;

        //  scale children
        scaleChildren();

        //  maintain focus while scaling
        float currentFocusX = detector.getFocusX();
        float currentFocusY = detector.getFocusY();
        int viewFocusX = (int)currentFocusX + getScrollX();
        int viewFocusY = (int)currentFocusY + getScrollY();
        mXScroll += viewFocusX - viewFocusX * detector.getScaleFactor();
        mYScroll += viewFocusY - viewFocusY * detector.getScaleFactor();

        requestLayout();

        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {

        mScaling = true;
        hideHandles();

        if (mReflowWidth == -1)
            mReflowWidth = getReflowWidth();

        //  Ignore any scroll amounts yet to be accounted for: the
        //  screen is not showing the effect of them, so they can
        //  only confuse the user
        mXScroll = mYScroll = 0;

        return true;
    }

    //  vieing state
    protected ViewingState mViewingState;
    public void setViewingState(ViewingState state) {
        mViewingState = state;}

    //  make sure there is always a reflow width set
    public void setReflowWidth() {
        mReflowWidth = getReflowWidth();
    }

    public void onScaleEnd(ScaleGestureDetector detector)
    {
        showHandles();

        //  When a pinch-scale is done, we want to get n-across
        //  to fit properly.

        //  nothing to do
        if (mAllPagesRect==null || mAllPagesRect.width()==0 || mAllPagesRect.height()==0)
            return;

        //  get current viewport
        final Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);

        //  if we're at one column and wider than the viewport, leave it alone.
        if (!mReflowMode && mLastLayoutColumns==0 && mAllPagesRect.width()>=viewport.width())
        {
            mScaling = false;
            return;
        }

        //  the doc, and page count.
        ArDkDoc doc = getDoc();
        int numPages = getPageCount();
        if (mReflowMode)
            numPages = mDoc.getNumPages();

        //  ratios for the viewport
        final float ratiox = ((float)(viewport.width())) /((float)(mAllPagesRect.width()));
        float ratioy = ((float)(viewport.height()))/((float)(mAllPagesRect.height()));

        if (mReflowMode)
        {
            //  handle possible appearance of new pages
            NUIDocView.currentNUIDocView().onReflowScale();

            //  remember current scroll locations
            final int sx = getScrollX();
            final int sy = getScrollY();

            //  change the flow mode width and height
            float pct = 1.0f;
            if (NUIDocView.currentNUIDocView().isPageListVisible())
                pct = ((float)getContext().getResources().getInteger(R.integer.sodk_editor_page_width_percentage))/100;
            final float newWidth = pct * mReflowWidth / mScale;
            final float newHeight = getReflowHeight();
            int currentMode = ((SODoc)doc).getFlowMode();
            ((SODoc)doc).setFlowMode(currentMode, newWidth, newHeight);

            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);

                    //  after the layout, scroll to a new y-location
                    float factor = mLastReflowWidth/newWidth;
                    int dy = sy - (int)(factor*sy);
                    scrollBy(-sx, -dy);  //  this should constrain

                    mLastReflowWidth = newWidth;
                }
            });

            mScaling = false;
            return;
        }

        float ratio = Math.min(ratiox, ratioy);

        //  NOTE: we removed a section of code that was causing
        //  https://bugs.ghostscript.com/show_bug.cgi?id=701594
        //  that seems now unnecessary.

        //  set a new scale factor that will result in the same number of columns
        int unscaledTotal = unscaledMaxw*mLastLayoutColumns + UNSCALED_GAP*(mLastLayoutColumns-1);
        mScale = (float)viewport.width()/(float)unscaledTotal;
        scaleChildren();
        mXScroll = 0;
        mYScroll = 0;

        //  scroll so that the vertical center remains the center (more or less)
        //  but if the content is shorter than the viewport, move it to the top.
        int newY = (int)((getScrollY() + viewport.height()/2)*ratiox - viewport.height()/2);
        if ((int)(ratio*mLastAllPagesRect.height())<viewport.height())
            newY = 0;

        //  scroll the correct amount after the next layout, because it was
        //  calculated based on re-scaled children.
        final int scrollAmount = newY;
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                scrollAfterScaleEnd(0, scrollAmount);
                requestLayout();
            }
        });

        requestLayout();

        mScaling = false;
    }

    public void onReflowScale()
    {
        if (mReflowMode)
        {
            //  the number of pages might have changed while zooming in reflow mode.
            //  Make sure they all have the same zoom, scale and size (based on the first page)

            DocPageView page0 = (DocPageView)getOrCreateChild(0);
            int numPages = mDoc.getNumPages();
            for (int i=1; i<numPages; i++)
            {
                DocPageView page = (DocPageView)getOrCreateChild(i);
                page.onReflowScale(page0);
            }
        }
    }

    protected void scrollAfterScaleEnd(int x, int y)
    {
        scrollTo(x, y);
    }

    //  this function specifies whether we should process touch events if there
    //  are no children.
    protected boolean allowTouchWithoutChildren() {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //  prevent further touch event if this view is done.
        if (finished())
            return true;

        //  not if there are no children yet
        if (!allowTouchWithoutChildren() && this.getChildCount()<=0)
            return true;

        //  is the user touches, restore constraining
        mConstrained = true;

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            //  user interaction begins
            mTouching = true;
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            //  user interaction ends
            if (mPressing)
                onLongPressRelease();
            else
                mTouching = false;
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE)
        {
            if (mPressing)
            {
                onLongPressMoving(event);
            }
        }

        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        return true;
    }

    //  create a single DragHandle of a particular kind
    private DragHandle setupHandle(RelativeLayout layout, int kind)
    {
        //  create
        DragHandle dh;
        if (kind==DragHandle.DRAG)
            dh = new DragHandle(getContext(), R.layout.sodk_editor_drag_handle, kind);
        else if (kind==DragHandle.ROTATE)
            dh = new DragHandle(getContext(), R.layout.sodk_editor_rotate_handle, kind);
        else
            dh = new DragHandle(getContext(), R.layout.sodk_editor_resize_handle, kind);

        //  add to the layout, initially hidden
        layout.addView(dh);
        dh.show(false);

        //  establish the listener
        dh.setDragHandleListener(this);

        return dh;
    }

    protected void setupNoteEditor()
    {
        //  create the note editor
        mNoteEditor = new NoteEditor((Activity)getContext(), this, mHostActivity, new NoteEditor.NoteDataHandler() {
            @Override
            public void setComment(String comment) {
                ArDkDoc doc = getDoc();

                if (doc != null)
                {
                    doc.setSelectionAnnotationComment(comment);
                }
            }

            @Override
            public String getAuthor() {
                return ((SODoc)getDoc()).getSelectedTrackedChangeAuthor();
            }

            @Override
            public String getDate() {
                return ((SODoc)getDoc()).getSelectedTrackedChangeDate();
            }

            @Override
            public String getComment() {

                boolean editable = false;

                String comment = ((SODoc)getDoc()).getSelectedTrackedChangeComment();
                int type = ((SODoc)getDoc()).getSelectedTrackedChangeType();
                switch (type)
                {
                    case SODoc.SmartOfficeTrackedChangeType_DeletedText:
                        comment = getContext().getString(R.string.sodk_editor_deleted_text);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_InsertedText:
                        comment = getContext().getString(R.string.sodk_editor_inserted_text);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_InsertedTableRow:
                        comment = getContext().getString(R.string.sodk_editor_inserted_table_row);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedTableGrid:
                        comment = getContext().getString(R.string.sodk_editor_changed_table_grid);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_InsertedParagraph:
                        comment = getContext().getString(R.string.sodk_editor_inserted_paragraph);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_InsertedTableCell:
                        comment = getContext().getString(R.string.sodk_editor_inserted_table_cell);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedRunProperties:
                        comment = getContext().getString(R.string.sodk_editor_changed_run_properties);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedTableProperties:
                        comment = getContext().getString(R.string.sodk_editor_changed_table_properties);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedSectionProperties:
                        comment = getContext().getString(R.string.sodk_editor_changed_section_properties);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedTableRowProperties:
                        comment = getContext().getString(R.string.sodk_editor_changed_table_row_properties);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedParagraphProperties:
                        comment = getContext().getString(R.string.sodk_editor_changed_paragraph_properties);
                        break;
                    case SODoc.SmartOfficeTrackedChangeType_ChangedTableCellProperties:
                        comment = getContext().getString(R.string.sodk_editor_changed_table_cell_properties);
                        break;
                    default:
                        // make comment editable only if doc is editable.
                        editable = mDocCfgOptions.isEditingEnabled();
                        break;
                }

                //  tell the editor whether the comment can be edited
                mNoteEditor.setCommentEditable(editable);

                return comment;
            }
        });
    }

    public void setup(RelativeLayout layout)
    {
        setupHandles(layout);
        setupNoteEditor();
    }

    //  create the selection and resizing handles
    //  and the resizing view
    public void setupHandles(RelativeLayout layout)
    {
        //  selection handles
        mSelectionHandleTopLeft     = setupHandle(layout, DragHandle.SELECTION_TOP_LEFT);
        mSelectionHandleBottomRight = setupHandle(layout, DragHandle.SELECTION_BOTTOM_RIGHT);

        //  resizing view
        mResizingView = new ImageView(getContext());
        mResizingView.setAlpha(0.5f);
        layout.addView(mResizingView);
        mResizingView.setVisibility(View.GONE);

        //  The resizing view should not maintain the aspect ratio.
        //  we'll handle that in our own code.
        mResizingView.setAdjustViewBounds(false);
        mResizingView.setScaleType(ImageView.ScaleType.FIT_XY);

        //  resize handles
        mResizeHandleTopLeft     = setupHandle(layout, DragHandle.RESIZE_TOP_LEFT);
        mResizeHandleTopRight    = setupHandle(layout, DragHandle.RESIZE_TOP_RIGHT);
        mResizeHandleBottomLeft  = setupHandle(layout, DragHandle.RESIZE_BOTTOM_LEFT);
        mResizeHandleBottomRight = setupHandle(layout, DragHandle.RESIZE_BOTTOM_RIGHT);

        //  drag handle
        mDragHandle = setupHandle(layout, DragHandle.DRAG);

        //  rotate handle
        mRotateHandle = setupHandle(layout, DragHandle.ROTATE);
    }

    protected int getPageCount()
    {
        if (getAdapter()==null)
            return 0;
        return getAdapter().getCount();
    }

    protected boolean shouldLayout()
    {
        //  reduce unnecessary repeated layouts
        boolean layout = false;

        //  if reflow mode was changed, do it
        if (mPreviousReflowMode != mReflowMode)
            layout = true;

        //  if scale or position was changed, do it
        if (mScale!=mLastScale || mLastScrollX!=getScrollX() || mLastScrollY!=getScrollY())
            layout = true;

        if (mForceLayout)
        {
            layout = true;
            mForceLayout = false;
        }

        if (layout)
        {
            mLastScale = mScale;
            mLastScrollX = getScrollX();
            mLastScrollY = getScrollY();
        }

        return layout;
    }

    public void forceLayout()
    {
        if (finished())
            return;

        mForceLayout = true;
        requestLayout();
    }

    protected boolean centerPagesHorizontally() {return false;}

    private float mLastLayoutScale = 0.0f;

    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        boolean columnsChanged = false;

        //  not if we've been finished
        if (finished())
            return;

        //  not if we're hiding
        if (!isShown())
            return;

        //  count our pages
        int numDocPages = getPageCount();
        if (mReflowMode)
            numDocPages = mDoc.getNumPages();

        //  do nothing if there are no pages
        if (getPageCount()==0)
            return;

        //  perform any pending scrolling
        scrollBy(-mXScroll, -mYScroll);
        mXScroll = mYScroll = 0;

        //  check to see if we really should
        if (!shouldLayout())
            return;

        //  get current viewport
        mViewportOrigin.set(getScrollX(), getScrollY());
        getGlobalVisibleRect(mViewport);
        mViewport.offsetTo(mViewportOrigin.x, mViewportOrigin.y);

        //  find the widest page
        unscaledMaxw = 0;
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView page = (DocPageView)getOrCreateChild(i);
            unscaledMaxw = Math.max(unscaledMaxw, page.getUnscaledWidth());
        }
        int maxw = (int)(unscaledMaxw*mScale);

        //  how many columns?
        int columns;
        if (mPressing)
        {
            columns = mLastLayoutColumns;
        }
        else if (mForceColumnCount != -1)
        {
            columns = mForceColumnCount;
        }
        else
        {
            /*
             * If the scale hasn't changed no need to recalculate the columns.
             *
             * This prevents miscalculations seen on rotation when the
             * viewport width has changed, but onOrientationChange() has
             * not yet been called to calculate the scale.
             */
            columns = mLastLayoutColumns;
            if (mLastLayoutScale != mScale)
            {
                mLastLayoutScale = mScale;
                int scaledGap = (int)(UNSCALED_GAP*mScale);
                double dcol = (double)(mViewport.width()+scaledGap)/(double)(maxw+scaledGap);
                columns = (int) Math.round(dcol);
            }

            if (mReflowMode)
                columns = 1;
        }

        //  limit the number of columns for docs with a small page count
        if (columns>numDocPages)
            columns = numDocPages;

        //  on each layout, we compute the "most visible" page.
        //  This may be considered to be the "current" page for purposes of highlighting it
        //  in the pages list.
        mostVisibleChild = -1;
        int mostVisibleChildHeight = -1;

        //  lay them out

        mAllPagesRect.setEmpty();
        int column = 0;
        int childTop = 0;
        int childWidth, childHeight, childLeft, childRight, childBottom;

        //  get the height of the moving page if any
        int unscaledMovingPageHeight = 0;
        if (isMovingPage())
        {
            DocPageView page = (DocPageView)getOrCreateChild(getMovingPageNumber());
            unscaledMovingPageHeight = page.getUnscaledHeight();
            mDropPageAbove = -1;
            mDropPageBelow = -1;
        }

        int unscaledRowHeight = 0;
        int topPage = -1;
        int nChildren = Math.max(getPageCount(), getChildCount());
        for (int i=0; i<nChildren; i++)
        {
            //  the number of document pages
            //  may change between layouts, resulting in
            //  views that no longer have a matching page.
            DocPageView page = (DocPageView)getOrCreateChild(i);
            if (page == null)
            {
                removeViewAt(i);
                continue;
            }

            //  tell the DocPageView that we're its DocView
            page.setDocView(this);

            //  if we're moving a page, remove it from this layout.
            if (isMovingPage() && page.getPageNumber() == getMovingPageNumber())
            {
                removeViewInLayout(page);
                continue;
            }

            //  calculate the page dimensions using integer math.
            //  this actually produces the same result each time, since the page size is fixed.
            childWidth = page.getUnscaledWidth();
            childHeight = page.getUnscaledHeight();
            childLeft = column * (unscaledMaxw + UNSCALED_GAP);
            childRight = childLeft + childWidth;
            childBottom = childTop + childHeight;

            //  this row is only as tall as its tallest page.
            unscaledRowHeight = Math.max(unscaledRowHeight, childHeight);

            if (isMovingPage())
            {
                int sct = (int)(childTop   *mScale);
                int scb = (int)(childBottom*mScale);

                boolean drop = false;
                if (topPage==-1)
                {
                    if (dropY<=sct)
                        drop = true;
                    topPage = i;
                }
                if (dropY>=sct && dropY<=(sct+scb)/2)
                    drop = true;

                if (drop)
                {
                    // drop should go above this page, so add a gap
                    mDropPageAbove = i;
                    childTop += unscaledMovingPageHeight;
                    childBottom += unscaledMovingPageHeight;
                }
            }

            //  scale those results to get the actual page location.
            int scaledChildLeft   = (int)(childLeft  *mScale);
            int scaledChildTop    = (int)(childTop   *mScale);
            int scaledChildRight  = (int)(childRight *mScale);
            int scaledChildBottom = (int)(childBottom*mScale);

            //  stash the rect in the page view for later use.
            mChildRect.set(scaledChildLeft, scaledChildTop, scaledChildRight, scaledChildBottom);
            page.setChildRect(mChildRect);

            //  at each layout, we remember the entire width and height of the laid-out
            //  pages.  This is used in applying constraints to scrolling amounts.
            if (mAllPagesRect.isEmpty())
                mAllPagesRect.set(mChildRect);
            else
                mAllPagesRect.union(mChildRect);

            if (mChildRect.intersect(mViewport) && i<numDocPages)
            {
                //  visible, so include in layout
                if (page.getParent()==null)
                {
                    //  was previously not visible.  Clear any
                    //  previously rendered contents
                    page.clearContent();

                    //  add to our layout.
                    addChildToLayout(page);
                }

                //  move the page to the right if we're centering them.
                int dx = 0;
                if (centerPagesHorizontally())
                    dx = (getWidth()-mChildRect.width())/2;

                page.layout(scaledChildLeft+dx, scaledChildTop, scaledChildRight+dx, scaledChildBottom);
                page.invalidate();

                //  determine the "most visible" child.
                if (page.getGlobalVisibleRect(mostVisibleRect))
                {
                    int h = mostVisibleRect.height();
                    if (h > mostVisibleChildHeight)
                    {
                        mostVisibleChildHeight = h;
                        mostVisibleChild = i;
                    }
                }
            }
            else
            {
                //  not visible, so remove from layout
                removeViewInLayout(page);
            }

            column++;
            if (column >= columns)
            {
                column = 0;

                if (getReflowMode())
                    childTop += childHeight;
                else
                    childTop += unscaledRowHeight;
                childTop += UNSCALED_GAP;

                unscaledRowHeight = 0;
            }

            if (isMovingPage())
            {
                int g = UNSCALED_GAP;
                g *= mScale;
                if (dropY>=(scaledChildTop+scaledChildBottom)/2 && dropY<=scaledChildBottom+g)
                {
                    // drop should go below this page, so add a gap
                    mDropPageBelow = i;
                    childTop += unscaledMovingPageHeight;
                }

            }

        }

        setMostVisiblePage();

        //  if the number of columns has changed, do some scrolling to adjust
        if (mScaling && columns>=1 && mLastLayoutColumns>=1 && mLastLayoutColumns!=columns)
        {
            columnsChanged = true;

            //  x - center in the viewport
            int dx = mAllPagesRect.centerX() - mViewport.centerX();
            scrollBy(dx,0);

            //  y - keep the 'most visible child' in view
            if (lastMostVisibleChild != -1)
                scrollToPage(lastMostVisibleChild, true);
        }
        mLastLayoutColumns = columns;
        mLastAllPagesRect.set(mAllPagesRect);
        lastMostVisibleChild = mostVisibleChild;

        moveHandlesToCorners();

        //  move the note editor to follow the note it's showing
        if (mNoteEditor != null)
            mNoteEditor.move();

        //  assume first page should get focus.
        //  this insures that hardware keyboard commands work
        if (once)
        {
            DocPageView page = (DocPageView) getOrCreateChild(0);
            //  TODO: this is needed if we ever reinstate control-A
            //page.requestFocus();
            once = false;
        }

        //  see if we're handling a start page
        handleStartPage();

        triggerRender();

        if (columnsChanged)
        {
            //  scheduling another layout when the column count has changed fixes
            //  https://bugs.ghostscript.com/show_bug.cgi?id=701315
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    layoutNow();
                }
            });
        }

        //  see if we should go to a page
        if (goToThisPage !=-1)
        {
            //  yes, but do it in a handler.
            final int page = goToThisPage;
            goToThisPage = -1;
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    scrollToPage(page, page==mDoc.getNumPages()-1, false);
                }
            });
        }

        reportViewChanges();
    }

    private boolean once = true;

    public int getMostVisiblePage()
    {
        return mostVisibleChild;
    }

    protected void setMostVisiblePage()
    {
        //  tell the main view about a new current page
        //  if we're scrolling by hand.
        //  the end of a fling is a special case.
        if (mTouching && mostVisibleChild >= 0 && !mScaling)
        {
            mHostActivity.setCurrentPage(mostVisibleChild);
        }
    }

    protected void onEndFling()
    {
        if (finished())
            return;

        //  a fling just ended.
        //  tell the main view about a new current page
        if ( flinging && mostVisibleChild >= 0)
        {
            mHostActivity.setCurrentPage(mostVisibleChild);
        }
    }

    //  start page, get and set.
    private int mStartPage = -1;
    public void setStartPage(int page) {
        mStartPage = page;
    }
    protected int getStartPage() {return mStartPage;}

    //  handle start page
    public void handleStartPage()
    {
        //  if we've been given a start page, go there.
        final int start = getStartPage();
        if (start>=0)
        {
            setStartPage(-1);  //  but just once

            if (mViewingState !=null)
            {
                //  scale us, and our children.
                setScale(mViewingState.scale);
                scaleChildren();

                //  post all of this so that we get an additional layout request
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //  scroll by the given amount.
                        //  that won't necessarily trigger a layout, so let's force one
                        //  and wait for the layout to settle
                        scrollTo(mViewingState.scrollX, mViewingState.scrollY);
                        forceLayout();
                        final ViewTreeObserver observer = getViewTreeObserver();
                        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                observer.removeOnGlobalLayoutListener(this);
                                invalidate();
                                //  with a new layout, mostVisibleChild will be set and so
                                //  calling onEndFling tells the parent the new value.
                                //  the parent will in turn scroll the page list to that value.
                                onEndFling();
                            }
                        });
                    }
                });
            }
        }
    }

    //  override the view's scrollBy() function so we can
    //  take the opportunity to apply some constraints

    @Override
    public void scrollBy(int dx, int dy)
    {
        Point p;
        if (mConstrained)
            p = constrainScrollBy(dx, dy);
        else
            p = new Point(dx, dy);
        super.scrollBy(p.x, p.y);
    }

    // apply constraints to every scroll request.

    protected Point constrainScrollBy(int dx, int dy)
    {
        int vph;
        int vpw;
        {
            Rect viewport = new Rect();
            getGlobalVisibleRect(viewport);

            //  subtract the keyboard height
//            viewport.bottom -= mHostActivity.getKeyboardHeight();

            //  fix https://bugs.ghostscript.com/show_bug.cgi?id=700061
            //  because getGlobalVisibleRect is smaller when the keyboard is
            //  visible.

            vph = viewport.height();
            vpw = viewport.width();
        }
        int sx = getScrollX();
        int sy = getScrollY();

        if (mAllPagesRect.width() <= vpw)
        {
            //  not too far to the right
            if (mAllPagesRect.width()-sx-dx > vpw)
                dx = 0;

            //  not too far to the left
            if (sx+dx>0)
                dx = -sx;
        }
        else
        {
            //  not too far to the right
            if (mAllPagesRect.width() < sx+vpw+dx)
                dx = 0;

            //  not too far to the left
            if (sx+dx < 0)
                dx = -sx;

            //  keep the right edge from moving left of the viewport's right edge.
            int aprw = mAllPagesRect.width();
            if (aprw-sx+dx<vpw)
                dx = aprw-sx+dx - vpw;
        }

        if (mAllPagesRect.height() <= vph)
        {
            // not too far down
            if (mAllPagesRect.height()-sy-dy > vph)
                dy = 0;

            //  not too far up
            if (sy+dy>0)
                dy = -sy;
        }
        else
        {
            //  not too far down
            if (sy+dy < 0)
                dy = -sy;

            //  not too far up.
            if (-sy+mAllPagesRect.height()-dy < vph)
                dy = -(vph-(-sy+mAllPagesRect.height()));

        }

        return new Point(dx, dy);
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mAdapter = (PageAdapter)adapter;
        requestLayout();
    }

    @Override
    public void setSelection(int arg0) {
        throw new UnsupportedOperationException(getContext().getString(R.string.sodk_editor_not_supported));
    }

    private View getCached() {
        return null;
    }

    protected View getOrCreateChild(int i) {
        View v = mChildViews.get(i);
        if (v == null) {
            v = getViewFromAdapter(i);
            mChildViews.append(i, v); // Record the view against it's adapter index
            onScaleChild(v, mScale);
        }

        return v;
    }

    protected void clearChildViews()
    {
        mChildViews.clear();
    }

    protected View getViewFromAdapter(int index)
    {
        return getAdapter().getView(index, getCached(), this);
    }

    private void addChildToLayout(View v) {
        LayoutParams params = v.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        addViewInLayout(v, 0, params, true);
    }

    public void triggerRender()
    {
        //  we might not have bitmaps
        if (bitmaps[0]==null)
            return;

        // Note that a render is needed
        renderRequested = true;

        // If the previous render has completed, start a new one. Otherwise
        // a new one will start as soon as the previous one completes.
        if (renderCount == 0)
            renderPages();
    }

    private boolean isValid()
    {
        if (mFinished)
            return false;
        if (mDoc==null)
            return false;

        if (bitmaps==null)
            return false;
        for (int i=0; i<bitmaps.length; i++)
        {
            if (bitmaps[i]==null)
                return false;
            if (bitmaps[i].getBitmap()==null)
                return false;
            if (bitmaps[i].getBitmap().isRecycled())
                return false;
        }

        return true;
    }

    private void renderPages()
    {
        renderRequested = false;

        if (!isValid())
            return;

        //  Rotate to the next bitmap
        bitmapIndex++;
        if (bitmapIndex>=bitmaps.length)
            bitmapIndex = 0;

        //  make a list of visible pages to render for this pass,
        //  and tell them we're starting
        final ArrayList<DocPageView> pages = new ArrayList<>();
        for (int i = 0; i < getPageCount(); i++)
        {
            DocPageView cv = (DocPageView) getOrCreateChild(i);
            if (cv.getParent()!=null && cv.isShown()) {
                pages.add(cv);
                cv.startRenderPass();
            }
        }

        //  time at start of the render pass
        final long startTime = System.currentTimeMillis();

        //  begin the render pass
        for (final DocPageView cv:pages)
        {
            if (!isValid())
                return;

            // Count up as we kick off rendering of each page
            renderCount++;

            cv.render(bitmaps[bitmapIndex], new SORenderListener()
            {
                @Override
                public void progress(int error)
                {
                    // Count down as they complete
                    renderCount--;

                    if (renderCount==0)
                    {
                        if (BuildConfig.DEBUG) {
                            //  report the render times
                            long endTime = System.currentTimeMillis();
                            System.out.println(String.format("DocView rendered %d pages in %d ms", pages.size(), (endTime - startTime)));
                        }

                        //  tell each one we're done
                        for ( DocPageView cv2:pages) {
                            cv2.endRenderPass();
                            cv2.invalidate();
                        }

                        //  if another render pass was requested while this was underway, start it
                        if (renderRequested)
                            renderPages();
                    }
                }
            });
        }
    }

    @Override
    public void run()
    {
        if (!mScroller.isFinished())
        {
            //  calculate the scrolling increments
            mScroller.computeScrollOffset();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            int dx = x - mScrollerLastX;
            int dy = y - mScrollerLastY;

            //  smooth out the dy value
            //  but only if the user did a fling.
            int smoothDy = dy;
            if (flinging)
            {
                mSmoother.addValue(dy);
                smoothDy = mSmoother.getAverage();
            }

            //  scroll at the next layout
            mXScroll += dx;
            mYScroll += smoothDy;
            requestLayout();

            //  remember where we left off
            mScrollerLastX += dx;
            mScrollerLastY += dy;

            //  repeat.
            this.post(this);
        }
        else
        {
            //  one more
            requestLayout();
            onEndFling();
            flinging = false;
        }
    }

    public void finish()
    {
        //  we're done with this view.
        mFinished = true;

        //  "finish" all the children
        for (int i = 0; i < getChildCount(); i++)
        {
            DocPageView cv = (DocPageView)getChildAt(i);
            cv.finish();
        }
        clearChildViews();

        mDoc = null;
    }

    public boolean finished() {return mFinished;}

    protected void smoothScrollBy(int dx, int dy)
    {
        smoothScrollBy(dx, dy, SMOOTH_SCROLL_TIME);
    }

    protected void smoothScrollBy(int dx, int dy, int ms)
    {
        //  let's not invoke the scroller if we don't need to
        if (dx==0 && dy==0)
            return;

        mScrollerLastX = mScrollerLastY = 0;
        mScroller.startScroll(0, 0, dx, dy, ms);
        this.post(this);
    }

    protected void addPageHistory(int pageNumber)
    {
        //  add history for where we're leaving (here)
        addHistory(getScrollX(), getScrollY(), getScale(), true);

        //  add history for where we're going
        //  unless the distance is zero.
        int newPage = pageNumber;
        Point pnew = scrollToPageAmounts(newPage);
        if (pnew.y != 0)
            addHistory(getScrollX(), getScrollY()-pnew.y, getScale(), false);
    }

    //  holds the go-to page number, used at the end of onLayout.
    private int goToThisPage = -1;

    public void goToLastPage()
    {
        //  set the last page number and get a new layout
        goToThisPage = getPageCount()-1;
        forceLayout();
    }

    public void goToFirstPage()
    {
        //  set the first page number and get a new layout
        goToThisPage = 0;
        forceLayout();
    }

    public void scrollToPage(int pageNumber, boolean fast)
    {
        if (isValidPage(pageNumber))
        {
            int pages = getPageCount();
            scrollToPage(pageNumber, pageNumber==pages-1, fast);
        }
    }

    public Point scrollToPageAmounts(int pageNumber)
    {
        int pages = getPageCount();
        return scrollToPageAmounts(pageNumber, pageNumber==pages-1);
    }

    public Point scrollToPageAmounts(int pageNumber, boolean last)
    {
        //  get current viewport
        Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);

        //  offset it based on current scroll position
        Point viewportOrigin = new Point();
        viewportOrigin.set(getScrollX(), getScrollY());
        viewport.offsetTo(viewportOrigin.x, viewportOrigin.y);

        //  get page rect from last layout
        DocPageView cv = (DocPageView)getOrCreateChild(pageNumber);
        Rect childRect = cv.getChildRect();

        int dy = 0;

        //  calculate scroll amount
        if ((childRect.height()) > viewport.height())
        {
            if (last)
            {
                //  put the bottom of the page at the bottom
                dy = getScrollY()-childRect.top-(childRect.height()-viewport.height());
            }
            else
            {
                //  put the top of the page at the top
                dy = getScrollY()-childRect.top;
            }
        }
        else
        {
            //  if the whole page is not visible, move the center of the page at the center
            if (childRect.top < viewport.top || childRect.bottom > viewport.bottom)
            {
                if (childRect.top==0)
                    dy = getScrollY();
                else
                    dy = getScrollY() + viewport.height() / 2 - (childRect.bottom + childRect.top) / 2;
            }
        }

        return new Point(0, dy);
    }

    public void scrollToPage(int pageNumber, boolean last, boolean fast)
    {
        //  scroll to bring the page into view

        //  how much?
        Point p = scrollToPageAmounts(pageNumber, last);
        int dy = p.y;

        //  now scroll
        if (dy != 0)
        {
            if (fast) {
                //  using scrollBy doesn't work here, but
                //  smoothScrollBy with a time of zero does.
                smoothScrollBy(0, dy, 0);
            }
            else
                smoothScrollBy(0, dy);
        }
        else
        {
            //  in some cases, like bug 701424, scrolling would result in a desired re-layout of the
            //  view. But when dy=0, no scrolling takes place, but we still need a layout.
            //  This fixes that.
            forceLayout();
        }
    }

    //  compute the selection limits data for the document
    private ArDkSelectionLimits mSelectionLimits = null;
    private void computeSelectionLimits()
    {
        mSelectionStartPage = null;
        mSelectionEndPage = null;
        mSelectionLimits = null;

        int start = getDoc().getSelectionStartPage();
        int end = getDoc().getSelectionEndPage();

        if (start>end)
            return;  //  this means there is no selection

        int numPages = getDoc().getNumPages();
        if (numPages<=0)
            return;  //  this also means there is no selection

        mSelectionStartPage = (DocPageView)getOrCreateChild(start);
        mSelectionEndPage = (DocPageView)getOrCreateChild(end);

        for (int i=start; i<end+1; i++)
        {
            DocPageView cv = (DocPageView)getOrCreateChild(i);
            ArDkSelectionLimits pageLimits = cv.getSelectionLimits();
            if (pageLimits != null)
            {
                if (mSelectionLimits==null)
                    mSelectionLimits = pageLimits;
                else
                    mSelectionLimits.combine(pageLimits);
            }
        }
    }

    //  return the selection limits data for the document
    public ArDkSelectionLimits getSelectionLimits()
    {
        return mSelectionLimits;
    }

    public void onLayoutChanged()
    {
        //  called when a core layout is done
    }

    public void onSelectionChanged()
    {
        // Assume this event to be spurious if there is no valid doc.
        if (getDoc() == null)
        {
            return;
        }

        computeSelectionLimits();

        //  don't scroll here if the editor is visible.
        //  the editor will scroll itself into view instead.
        boolean bScroll = true;
        if (mNoteEditor!=null && mNoteEditor.isVisible())
            bScroll = false;
        if (bScroll)
            scrollSelectionIntoView();

        boolean layout = false;
        for (int i=0; i<getPageCount(); i++)
        {
            //  get the view
            DocPageView pageView = (DocPageView)getOrCreateChild(i);

            //  The last doc change might have changed the size of one of the pages.
            //  Re-evaluate the size of the view
            if(pageView.sizeViewToPage())
                layout = true;

            //  This page might be new (added while typing), so re-assert the common scale factor.
            pageView.setNewScale(mScale);
        }

        //  The size of one of the pages has changed, so do a new layout.
        if (layout)
            forceLayout();

        selectionTypeChangeCheck();
        updateDragHandles();
        updateReview();
    }

    private boolean mScrollRequested = false;
    private boolean mAddComment = false;

    //  called right before moving to the next or previous tracked change
    public void preNextPrevTrackedChange()
    {
        if (mNoteEditor != null)
            mNoteEditor.preMoving();
    }

    public void saveComment()
    {
        if (mNoteEditor != null)
            mNoteEditor.saveComment();
    }

    public void onNextPrevTrackedChange()
    {
        mScrollRequested=true;
        Utilities.hideKeyboard(getContext());
    }

    public void addComment()
    {
        mAddComment = true;
        getDoc().addHighlightAnnotation();
    }

    protected void updateReview()
    {
        boolean shouldScroll = mScrollRequested;
        mScrollRequested = false;

        boolean addComment = mAddComment;
        mAddComment = false;

        if (((SODoc)getDoc()).getSelectionHasAssociatedPopup() || ((SODoc)getDoc()).selectionIsReviewable())
        {
            //  prevent keyboard from also showing because of a single-tap
            mTapStatus = 0;

            //  show the note editor
            ArDkSelectionLimits limits = getSelectionLimits();
            mNoteEditor.show(limits, mSelectionStartPage);
            mNoteEditor.move();
            if (shouldScroll)
            {
                scrollEditorIntoView();
            }
            if (addComment)
            {
                //  give the note editor focus
                mNoteEditor.focus();
                mHostActivity.showKeyboard();
            }
        }
        else
        {
            //  hide the note editor and keyboard
            if (mNoteEditor!=null && mNoteEditor.isVisible())
            {
                Utilities.hideKeyboard(getContext());
                mNoteEditor.hide();
                if (shouldScroll)
                    scrollSelectionIntoView();
            }
        }
    }

    protected void showHandle(DragHandle handle, boolean show)
    {
        if (handle != null)
            handle.show(show);
    }

    //  show or hide the selection handles
    private void showSelectionHandles(boolean show)
    {
        showHandle(mSelectionHandleTopLeft, show);
        showHandle(mSelectionHandleBottomRight, show);
    }

    //  show or hide the resizing handles
    private void showResizeHandles(boolean show)
    {
        if (!canEditText())
            return;
        showHandle(mResizeHandleTopLeft, show);
        showHandle(mResizeHandleTopRight, show);
        showHandle(mResizeHandleBottomLeft, show);
        showHandle(mResizeHandleBottomRight, show);
    }

    private void showDragHandle(boolean show)
    {
        if (!canEditText())
            return;
        showHandle(mDragHandle, show);
    }

    private void showRotateHandle(boolean show)
    {
        if (!canEditText())
            return;
        showHandle(mRotateHandle, show);
    }

    protected void updateDragHandles()
    {
        //  hide the resizing view and destroy the bitmap.
        mResizingView.setImageBitmap(null);
        mResizingView.setVisibility(View.GONE);
        if (mResizingBitmap!=null && mResizingBitmap.getBitmap()!=null)
        {
            mResizingBitmap.getBitmap().recycle();
            mResizingBitmap = null;
        }

        //  show and move handles
        showHandles();
        moveHandlesToCorners();
    }

    protected void moveHandlesToCorners()
    {
        if (finished())
            return;

        //  get selection limits
        ArDkSelectionLimits limits = getSelectionLimits();
        if (limits!=null)
        {
            boolean enableSelectionHandles = (limits.getIsActive() && !limits.getIsCaret());
            boolean enableResizeHandles = (limits.getIsActive() && getDoc().getSelectionCanBeResized() && !limits.getIsCaret());
            boolean enableDragHandle = (limits.getIsActive() && getDoc().getSelectionCanBeAbsolutelyPositioned());
            boolean enableRotateHandle = (limits.getIsActive() && getDoc().getSelectionCanBeRotated());

            if (enableResizeHandles)
            {
                positionHandle(mResizeHandleTopLeft,     mSelectionStartPage, (int) limits.getBox().left,  (int)limits.getBox().top);
                positionHandle(mResizeHandleTopRight,    mSelectionStartPage, (int) limits.getBox().right, (int)limits.getBox().top);
                positionHandle(mResizeHandleBottomLeft,  mSelectionStartPage, (int) limits.getBox().left,  (int)limits.getBox().bottom);
                positionHandle(mResizeHandleBottomRight, mSelectionStartPage, (int) limits.getBox().right, (int)limits.getBox().bottom);
            }

            if (enableSelectionHandles)
            {
                positionHandle(mSelectionHandleTopLeft,     mSelectionStartPage, (int)limits.getStart().x, (int)limits.getStart().y);
                positionHandle(mSelectionHandleBottomRight, mSelectionEndPage,   (int)limits.getEnd().x,   (int)limits.getEnd().y);
            }

            if (enableDragHandle) {
                positionHandle(mDragHandle, mSelectionStartPage, (int) (limits.getBox().left+limits.getBox().right)/2, (int)limits.getBox().bottom);
            }

            if (enableRotateHandle) {
                positionHandle(mRotateHandle, mSelectionStartPage, (int) (limits.getBox().left+limits.getBox().right)/2, (int)limits.getBox().top);
            }
        }
    }

    //  position a handle, given page coordinates
    protected void positionHandle(DragHandle handle, DocPageView dpv, int pageX, int pageY)
    {
        if (handle != null)
        {
            //  convert to DocPageView-based coords
            Point p = dpv.pageToView(pageX, pageY);

            //  offset to 0,0,
            //  based on where layout would have put the page, were it visible
            p.offset(dpv.getChildRect().left, dpv.getChildRect().top);

            //  offset to position in the scrolling view (this)
            p.offset(-getScrollX(), -getScrollY());

            //  offset by half the handle size
            handle.measure(0, 0);
            p.offset(-handle.getMeasuredWidth()/2, -handle.getMeasuredHeight()/2);

            //  put the border of the circle at the corner of the selection box
            Point offset = handle.offsetCircleToEdge();
            p.offset(offset.x, offset.y);

            //  move it
            handle.moveTo(p.x, p.y);
        }
    }

    private Point viewToScreen(Point p)
    {
        Point newPoint = new Point(p);

        Rect r = new Rect();
        this.getGlobalVisibleRect(r);

        newPoint.offset(r.left, r.top);

        return newPoint;
    }

    private boolean mDragging = false;

    @Override
    public void onStartDrag(DragHandle handle)
    {
        mDragging = true;

        if (handle.isSelectionKind())
        {
            //  do nothing
        }
        else
        {
            //  fix 700142 - Application Crashes On Image Resize
            if (mSelectionStartPage==null || mSelectionStartPage.getSelectionLimits()==null) {
                mDragging = false;
                return;
            }

            //  resizing / dragging / rotating a shape has begun.

            //  get object's current bounds
            mDraggingObjectPageBounds = mSelectionStartPage.getSelectionLimits().getBox();

            //  get the bitmap and stash it in the resizing view.
            //  the view will draw it a different sizes.
            mResizingBitmap = ((SODoc)getDoc()).getSelectionAsBitmap();
            mResizingView.setImageBitmap(mResizingBitmap!=null ? mResizingBitmap.getBitmap() : null);

            //  remember the selection corners
            ArDkSelectionLimits limits = getSelectionLimits();
            mResizeOrigTopLeft = mSelectionStartPage.pageToView((int) limits.getBox().left, (int) limits.getBox().top);
            mResizeOrigTopLeft.offset(mSelectionStartPage.getLeft(), mSelectionStartPage.getTop());
            mResizeOrigTopLeft.offset(-getScrollX(), -getScrollY());
            mResizeOrigBottomRight = mSelectionStartPage.pageToView((int) limits.getBox().right, (int) limits.getBox().bottom);
            mResizeOrigBottomRight.offset(mSelectionStartPage.getLeft(), mSelectionStartPage.getTop());
            mResizeOrigBottomRight.offset(-getScrollX(), -getScrollY());

            //  remember the aspect ratio
            mNatDim = ((SODoc)getDoc()).getSelectionNaturalDimensions();
            mResizeAspect = ((float)(mResizeOrigBottomRight.y-mResizeOrigTopLeft.y))/((float)(mResizeOrigBottomRight.x-mResizeOrigTopLeft.x));

            //  remember the drag handle location
            mDragOrigLocation.set(handle.getPosition().x,handle.getPosition().y);

            //  remember initial rotation value
            mRotateAngle = ((SODoc)getDoc()).getSelectionRotation();
            mNAdditionalAngle = 0;

            mResizingView.setRotation(mNAdditionalAngle);

            //  init the size and location of the dragging view
            moveResizingView(mResizeOrigTopLeft.x, mResizeOrigTopLeft.y,
                    (mResizeOrigBottomRight.x-mResizeOrigTopLeft.x),
                    (mResizeOrigBottomRight.y-mResizeOrigTopLeft.y));

            mResizeOrigRect.set(mResizeRect);

            //  hide the handles
            hideHandles();
        }
    }

    private void onDragObject(DragHandle handle)
    {
        //  location
        int x = mResizeOrigTopLeft.x + (handle.getPosition().x - mDragOrigLocation.x);
        int y = mResizeOrigTopLeft.y + (handle.getPosition().y - mDragOrigLocation.y);

        //  dimensions
        int width  = mResizeOrigBottomRight.x-mResizeOrigTopLeft.x;
        int height = mResizeOrigBottomRight.y-mResizeOrigTopLeft.y;

        //  move and size the view
        moveResizingView(x, y, width, height);
    }

    private void onResizeObject(DragHandle handle)
    {
        //  what are the four corners?
        int top    = mResizeOrigTopLeft.y;
        int left   = mResizeOrigTopLeft.x;
        int bottom = mResizeOrigBottomRight.y;
        int right  = mResizeOrigBottomRight.x;

        switch (handle.getKind()) {
            case DragHandle.RESIZE_TOP_LEFT:
                top  = handle.getPosition().y+handle.getMeasuredHeight()/2;
                left = handle.getPosition().x+handle.getMeasuredWidth()/2;
                break;
            case DragHandle.RESIZE_TOP_RIGHT:
                top   = handle.getPosition().y+handle.getMeasuredHeight()/2;
                right = handle.getPosition().x+handle.getMeasuredWidth()/2;
                break;
            case DragHandle.RESIZE_BOTTOM_LEFT:
                bottom = handle.getPosition().y+handle.getMeasuredHeight()/2;
                left   = handle.getPosition().x+handle.getMeasuredWidth()/2;
                break;
            case DragHandle.RESIZE_BOTTOM_RIGHT:
                bottom = handle.getPosition().y+handle.getMeasuredHeight()/2;
                right  = handle.getPosition().x+handle.getMeasuredWidth()/2;
                break;
        }

        //  calculate new dimensions while imposing a minimum, and maintaining the aspect ratio.
        int newWidth = right-left;
        int newHeight = bottom-top;

        if (mNatDim.x>0 && mNatDim.y>0)
        {
            int origWidth  = mResizeOrigBottomRight.x - mResizeOrigTopLeft.x;
            int origHeight = mResizeOrigBottomRight.y - mResizeOrigTopLeft.y;

            double origDiag = Math.sqrt(origWidth*origWidth + origHeight*origHeight);
            double newDiag  = Math.sqrt(newWidth*newWidth   + newHeight*newHeight);
            double ratio = newDiag / origDiag;
            newWidth  = (int)(origWidth  * ratio);
            newHeight = (int)(origHeight * ratio);

            if (newWidth<RESIZE_MIN_DIMENSION || newHeight<RESIZE_MIN_DIMENSION)
                return;
        }
        else
        {
            //  don't
            if (newWidth< RESIZE_MIN_DIMENSION)
                newWidth = RESIZE_MIN_DIMENSION;
            if (newHeight< RESIZE_MIN_DIMENSION)
                newHeight = RESIZE_MIN_DIMENSION;
        }

        //  figure out where the new location should be.
        int newX=0;
        int newY=0;
        switch (handle.getKind()) {
            case DragHandle.RESIZE_TOP_LEFT:
                newX = mResizeOrigBottomRight.x-newWidth;
                newY = mResizeOrigBottomRight.y-newHeight;
                break;
            case DragHandle.RESIZE_TOP_RIGHT:
                newX = mResizeOrigTopLeft.x;
                newY = mResizeOrigBottomRight.y-newHeight;
                break;
            case DragHandle.RESIZE_BOTTOM_LEFT:
                newX = mResizeOrigBottomRight.x-newWidth;
                newY = mResizeOrigTopLeft.y;
                break;
            case DragHandle.RESIZE_BOTTOM_RIGHT:
                newX = mResizeOrigTopLeft.x;
                newY = mResizeOrigTopLeft.y;
                break;
        }

        //  move and size the view
        moveResizingView(newX, newY, newWidth, newHeight);
    }

    private void onRotateObject(DragHandle handle)
    {
        int x = handle.getPosition().x;
        int y = handle.getPosition().y;
        int centerX = (mResizeOrigTopLeft.x + mResizeOrigBottomRight.x)/2;
        int centerY = (mResizeOrigTopLeft.y + mResizeOrigBottomRight.y)/2;

        float newAngle = getAngle(x, y, centerX, centerY);
        if (Math.abs(newAngle-mNAdditionalAngle)>2)
        {
            mNAdditionalAngle = newAngle;
            mResizingView.setRotation(mNAdditionalAngle);
            mResizingView.setLayoutParams(mResizingView.getLayoutParams()); // needed to cause a redraw
        }
    }

    public float getAngle(int x1, int y1, int x2, int y2)
    {
        float angle = (float) Math.toDegrees(Math.atan2(y2-y1, x2-x1));
        angle -= 90;

        if(angle < 0){
            angle += 360;
        }

        return angle;
    }

    private void moveResizingView(int x, int y, int w, int h)
    {
        //  constrain the location to stay within the page.
        Rect pageRect = mSelectionStartPage.screenRect();
        Rect newRect = new Rect(x, y, x + w, y + h);
        int loc[] = new int[2];
        getLocationOnScreen(loc);
        newRect.offset(loc[0], loc[1]);

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
        mResizeRect.set(x, y, x+w, y+h);

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

    protected Point adjustDragHandle(DragHandle handle, Point p)
    {
        return new Point(p);
    }

    protected boolean canSelectionSpanPages()
    {
        return true;
    }

    @Override
    public void onDrag(DragHandle handle)
    {
        //  what page, based on the handle's position?
        Point p = handle.getPosition();
        p = viewToScreen(p);
        DocPageView pageView  = findPageViewContainingPoint(p.x, p.y, false);
        if (pageView == null)
            return;

        if (handle.isSelectionKind())
        {
            if (!canSelectionSpanPages())
            {
                //  disallow page-spanning selections by checking the page number
                //  of the page we're dragging in, versus the current selection's
                //  page numbers
                int draggingPage = pageView.getPageNumber();
                if (draggingPage != mSelectionStartPage.getPageNumber())
                    return;
                if (draggingPage != mSelectionEndPage.getPageNumber())
                    return;
            }

            p = adjustDragHandle(handle, p);

            //  now modify the selection in that page
            if (handle.getKind() == DragHandle.SELECTION_TOP_LEFT)
            {
                //  we use the drag handle's lower right corner
                p.x += handle.getWidth();
                p.y += handle.getHeight();
                pageView.setSelectionStart(p);
            }

            else if (handle.getKind() == DragHandle.SELECTION_BOTTOM_RIGHT)
            {
                //  we use the drag handle's upper left corner
                pageView.setSelectionEnd(p);
            }
        }

        else if (handle.isResizeKind())
        {
            onResizeObject(handle);
        }

        else if (handle.isDragKind())
        {
            onDragObject(handle);
        }

        else if (handle.isRotateKind())
        {
            onRotateObject(handle);
        }

    }

    protected void setSelectionBoxBounds(RectF bounds)
    {
    }

    @Override
    public void onEndDrag(DragHandle handle)
    {
        if (handle.isResizeKind() && mDraggingObjectPageBounds!=null)
        {
            //  how far did the corners move?
            int dLeft   = mResizeRect.left   - mResizeOrigRect.left;
            int dTop    = mResizeRect.top    - mResizeOrigRect.top;
            int dRight  = mResizeRect.right  - mResizeOrigRect.right;
            int dBottom = mResizeRect.bottom - mResizeOrigRect.bottom;

            //  how far in page coordinates?
            Point dTopLeft     = mSelectionStartPage.viewToPage(dLeft, dTop);
            Point dBottomRight = mSelectionStartPage.viewToPage(dRight, dBottom);

            //  set the new bounds.
            mDraggingObjectPageBounds.left   += dTopLeft.x;
            mDraggingObjectPageBounds.top    += dTopLeft.y;
            mDraggingObjectPageBounds.right  += dBottomRight.x;
            mDraggingObjectPageBounds.bottom += dBottomRight.y;
            setSelectionBoxBounds(mDraggingObjectPageBounds);
        }

        else if (handle.isDragKind() && mDraggingObjectPageBounds!=null && mDragging)
        {
            //  how far was the object dragged, in page coordinates
            int dx = mResizeRect.left - mResizeOrigRect.left;
            int dy = mResizeRect.top - mResizeOrigRect.top;
            Point pageDistance = mSelectionStartPage.viewToPage(dx, dy);

            //  set the new bounds.
            mDraggingObjectPageBounds.offset(pageDistance.x, pageDistance.y);
            setSelectionBoxBounds(mDraggingObjectPageBounds);
        }

        else if (handle.isRotateKind())
        {
            ((SODoc)getDoc()).setSelectionRotation(mRotateAngle+mNAdditionalAngle);
        }

        else if (handle.isSelectionKind())
        {
            moveHandlesToCorners();

            // The selection has changed. Tell the InputView.
            updateInputView();
        }

        mDraggingObjectPageBounds = null;
        mDragging = false;
    }

    /*
     * The IME only knows about the 'text' around the selection point.
     *
     * There may be other selectable objects present.
     *
     * To keep the IME in sync with the document we update it
     * as we move to/from a text selection to a selection of
     * another type.
     */
    private void selectionTypeChangeCheck()
    {
        ArDkSelectionLimits limits = getSelectionLimits();
        if (limits != null)
        {
            boolean isAltText = getDoc().getSelectionIsAlterableTextSelection();

            if (mLastSelIsAltText == null)
            {
                mLastSelIsAltText = new Boolean(isAltText);
            }
            else
            {
                if (isAltText != mLastSelIsAltText.booleanValue())
                {
                    mLastSelIsAltText = new Boolean(isAltText);
                    updateInputView();
                }
            }
        }
        else
        {
            mLastSelIsAltText = null;
        }
    }

    protected void showHandles()
    {
        hideHandles();

        ArDkSelectionLimits limits = getSelectionLimits();
        if (limits != null)
        {
            boolean enableSelectionHandles = (limits.getIsActive() && !limits.getIsCaret() && ! NUIDocView.currentNUIDocView().getIsComposing());
            boolean enableResizeHandles = (limits.getIsActive() && getDoc().getSelectionCanBeResized() && !limits.getIsCaret());
            boolean enableDragHandle = (limits.getIsActive() && getDoc().getSelectionCanBeAbsolutelyPositioned());
            boolean enableRotateHandle = (limits.getIsActive() && getDoc().getSelectionCanBeRotated());

            showSelectionHandles(enableSelectionHandles);
            showResizeHandles(enableResizeHandles);
            showDragHandle(enableDragHandle);
            showRotateHandle(enableRotateHandle);
        }
    }

    protected void hideHandles()
    {
        showSelectionHandles(false);
        showResizeHandles(false);
        showDragHandle(false);
        showRotateHandle(false);
    }

    private Point viewToPage(int x, int y)
    {
        Point p = new Point(x, y);
        p.offset(getScrollX(), getScrollY());
        p.offset(-mSelectionStartPage.getLeft(),-mSelectionStartPage.getTop());
        p = mSelectionStartPage.viewToPage(p.x, p.y);
        return p;
    }

    public void scrollSelectionIntoView()
    {
        if (mSelectionLimits==null)
            return;
        if (mSelectionStartPage==null)
            return;
        ArDkSelectionLimits limits = mSelectionStartPage.getSelectionLimits();
        if (limits==null)
            return;
        RectF box = limits.getBox();
        if (box==null || box.isEmpty())
            return;

        int pageNum = mSelectionStartPage.getPageNumber();
        if (((NUIDocView)mHostActivity).wasTyping())
        {
            //  while typing, we don't want the whole selection box, which represents what's being
            //  composed.  Instead, let's make a box that represents the right edge,
            //  (or left edge if we're RTL) where the text is being extended.
            RectF box2;
            if (Utilities.isRTL(getContext()))
                box2 = new RectF(box.left, box.top, box.left+1, box.bottom);
            else
                box2 = new RectF(box.right-1, box.top, box.right, box.bottom);

            scrollBoxIntoView(pageNum, box2, true);
        }
        else
        {
            scrollBoxIntoView(pageNum, box);
        }
    }

    public void scrollEditorIntoView()
    {
        if (mNoteEditor !=null && mNoteEditor.isVisible())
        {
            //  get our viewport
            Rect viewport = new Rect();
            getGlobalVisibleRect(viewport);
            //  offset in both directions
            viewport.offset(-viewport.left,-viewport.top);

            //  get the editor rect
            Rect r = mNoteEditor.getRect();

            //  calculate the y distance
            int dy = 0;
            if (r.top<viewport.top)
                dy = viewport.top - r.top;
            if (r.bottom>viewport.bottom)
                dy = viewport.bottom - r.bottom;

            //  calculate the x distance
            int dx = 0;
            if (r.left<viewport.left)
                dx = viewport.left - r.left;
            if (r.right>viewport.right)
                dx = viewport.right - r.right;

            //  go!
            smoothScrollBy(dx, dy);
        }
    }

    public void scrollBoxIntoView (int pageNum, RectF box)
    {
        scrollBoxIntoView(pageNum, box, false);
    }

    public void scrollBoxIntoView (int pageNum, RectF box, boolean alsoWidth)
    {
        scrollBoxIntoView(pageNum, box, alsoWidth, 0);
    }

    private Point scrollBoxIntoViewAmounts(int pageNum, RectF box, boolean alsoWidth, int viewportMargin)
    {
        //  get our viewport
        Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        viewport.offset(0,-viewport.top);

        //  adjust the viewport
        viewport.inset(viewportMargin, viewportMargin);

        //  get the location of the box's lower left corner,
        //  relative to the viewport
        DocPageView cv = (DocPageView)getOrCreateChild(pageNum);
        Point point = cv.pageToView((int)box.left,(int)box.bottom);
        Rect childRect = cv.getChildRect();
        point.y += childRect.top;
        point.y -= getScrollY();
        point.x += childRect.left;
        point.x -= getScrollX();

        //  if the point is outside the viewport, scroll so it is.
        int dy = 0;
        if (point.y<viewport.top || point.y>viewport.bottom)
        {
            dy = (viewport.top + viewport.bottom)/2 - point.y;
        }

        int dx = 0;
        if (alsoWidth)
        {
            if (point.x < viewport.left || point.x > viewport.right)
            {
                dx = (viewport.left + viewport.right) / 2 - point.x;

            }
        }

        return new Point(dx, dy);
    }

    public void scrollBoxIntoView (int pageNum, RectF box, boolean alsoWidth, int viewportMargin)
    {
        Point p = scrollBoxIntoViewAmounts(pageNum, box, alsoWidth, viewportMargin);

        smoothScrollBy(p.x, p.y);
    }

    public int scrollBoxToTopAmount(int pageNum, RectF box)
    {
        //  get our viewport
        Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        viewport.offset(0,-viewport.top);

        //  get the location of the box's upper left corner, relative to the viewport
        DocPageView cv = (DocPageView)getOrCreateChild(pageNum);
        Point point = cv.pageToView((int)box.left,(int)box.top);
        Rect childRect = cv.getChildRect();
        point.y += childRect.top;
        point.y -= getScrollY();

        int diff = viewport.top - point.y;

        return diff;
    }

    public void scrollBoxToTop(int pageNum, RectF box)
    {
        int diff = scrollBoxToTopAmount(pageNum, box);
        smoothScrollBy(0, diff);
    }

    public int getReflowWidth()
    {
        //  for now, base this on the first page.
        DocPageView cv = (DocPageView)getOrCreateChild(0);
        return cv.getReflowWidth();
    }

    public int getReflowHeight()
    {
        final Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);

        DocPageView cv = (DocPageView)getOrCreateChild(0);
        double factor = cv.getFactor();

        return (int)(viewport.height()/factor);
    }

    private boolean mPreviousReflowMode = false;
    private boolean mReflowMode = false;
    private int mReflowWidth=-1;
    public void setReflowMode(boolean val)
    {
        mPreviousReflowMode = mReflowMode;
        mReflowMode = val;
        mReflowWidth = -1;
    }
    public boolean getReflowMode() { return mReflowMode; }

    public int getBorderColor()
    {
        return mHostActivity.getBorderColor();
    }

    protected boolean isMovingPage() {return false;}

    protected int getMovingPageNumber() {return -1;}

    protected void startMovingPage(int pageNum)
    {
        //  this used to set an initial value for dropY.
        //  instead, that doesn't happen until after moving is detected.
    }

    protected void onPageMoved(int newPage)
    {
    }

    private int getNewPos(int movingPage)
    {
        //  This function calculates the new position for an about-to-be-dropped
        //  page during slide sorting.

        if (mDropPageAbove == -1)
        {
            if (mDropPageBelow == -1) {
                //  drop at end
                return getDoc().getNumPages()-1;
            }
            else {
                if (movingPage<=mDropPageBelow)
                    return mDropPageBelow;
                else
                    return mDropPageBelow+1;
            }
        }
        else
        {
            if (movingPage > mDropPageAbove) {
                //  up
                return mDropPageAbove;
            }
            else {
                //  down
                return mDropPageAbove-1;
            }
        }
    }

    protected void dropMovingPage(boolean moved)
    {
        final int movingPage = getMovingPageNumber();
        if (!isValidPage(movingPage))
            return;

        //  If its new position is the same, it hasn't really moved.
        int newPos = getNewPos(movingPage);
        if (newPos == movingPage)
            moved = false;

        if (!moved)
        {
            if (canEditText())
            {
                //  do this after the layout is done, so the page is visible
                final ViewTreeObserver observer = getViewTreeObserver();
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        observer.removeOnGlobalLayoutListener(this);
                        doPageMenu(movingPage);
                        mHostActivity.setCurrentPage(movingPage);
                    }
                });
            }

            finishDrop();
            return;
        }

        //  Move it now.
        newPos = getNewPos(movingPage);
        movePage (movingPage, newPos);

        finishDrop();
    }

    public void movePage(int oldPos, int newPos)
    {
        if (!isValidPage(oldPos))
            return;
        if (!isValidPage(newPos))
            return;

        ((SODoc)getDoc()).movePage(oldPos, newPos);
        onPageMoved(newPos);
    }

    public boolean isValidPage(int pageNumber)
    {
        int numPages = getPageCount();
        if (pageNumber>=0 && pageNumber<numPages)
            return true;

        return false;
    }

    protected void finishDrop()
    {
        dropY = -1;
        mForceLayout = true;
        requestLayout();
    }

    protected void doPageMenu(int pageNumber)
    {
    }

    protected void onMovePage(int pageNum, int positionY)
    {
        if (!isMovingPage())
            return;

        dropY = positionY + getScrollY();
        mForceLayout = true;
        requestLayout();
    }

    protected void onPreLayout()
    {
    }

    @Override
    public void requestLayout()
    {
        if (finished())
            return;

        onPreLayout();
        super.requestLayout();
    }

    public class Smoother
    {
        //  this class keeps a running average of a set of values

        private int MAX;
        ArrayList<Integer> values;

        //  ctor
        Smoother(int max) {
            values = new ArrayList<>();
            MAX = max;
        }

        //  remove all values
        public void clear() {
            values.clear();
        }

        //  add a value
        public void addValue(int val) {
            //  if we're at max, remove the oldest one
            if (values.size()==MAX)
                values.remove(0);
            //  add the new one
            values.add(new Integer(val));
        }

        // get the average
        public int getAverage() {
            // if there are no values, return 0
            if (values.size()==0)
                return 0;

            //  calulate the total
            int total = 0;
            for (int i=0; i<values.size(); i++)
            {
                Integer a = values.get(i);
                total += a.intValue();
            }

            //  return the average.
            int avg = total / values.size();
            return avg;
        }
    }

    public boolean isDocModified()
    {
        if (mDoc!=null)
            return mDoc.getHasBeenModified();
        return false;
    }

    protected void resetInputView()
    {
        if (inputView() != null)
            inputView().resetEditable();
    }

    protected void updateInputView()
    {
        if (inputView() != null)
            inputView().updateEditable();
    }

    protected void focusInputView()
    {
        if (inputView() != null)
            inputView().setFocus();
    }

    public void onFoundText(int pageNum, RectF box)
    {
        scrollBoxIntoView(pageNum, box, true);
    }

    public void onReloadFile()
    {
    }

    public boolean isAtRest()
    {
        //  this is a simple function to tell if the user is panning/zooming.

        if (mTouching)
            return false;
        if (mScaling)
            return false;
        if (!mScroller.isFinished())
            return false;

        return true;
    }

    //  this function polls ever 100 msec to see if the view is "at rest", as defined
    //  by the function isAtRest() (above)
    //  It calls the runnable when the view is at rest.
    public void waitForRest(final Runnable runnable)
    {
        if (isAtRest()) {
            runnable.run();
        }
        else
        {
            //  do this again shortly
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    waitForRest(runnable);
                }
            }, 100);
        }
    }

    public void onHistoryItem(History.HistoryItem item)
    {
        //  set the new scroll/scale
        mXScroll = 0;
        mYScroll = 0;
        setScrollX(item.getScrollX());
        setScrollY(item.getScrollY());
        mScale = item.getScale();

        //  layout
        forceLayout();

        //  when the layout is done, update the current page
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if ( mostVisibleChild >= 0)
                {
                    mHostActivity.setCurrentPage(mostVisibleChild);
                }
            }
        });
    }

    //  for no-UI API
    public boolean select(Point p1, Point p2)
    {
        //  no selection in non-office docs
        if (mHostActivity instanceof NUIDocViewOther)
            return false;

        //  find the page view
        final DocPageView dpv = findPageViewContainingPoint(p1.x, p1.y, false);
        if (dpv==null)
            return false;

        //  stop search and clear current selection.
        getDoc().closeSearch();
        getDoc().clearSelection();

        int err;

        if (p2==null)
        {
            //  one point provided. Place the cursor
            Point pPage = dpv.screenToPage(p1.x, p1.y);
            err = dpv.getPage().select(ArDkPage.SOSelectMode_Caret, pPage.x, pPage.y);
            if (err!=1)
                return false;
        }
        else
        {
            //  two points provided. Select a range.
            Point p1Page = dpv.screenToPage(p1.x, p1.y);
            Point p2Page = dpv.screenToPage(p2.x, p2.y);

            //  first create a selection
            err = dpv.getPage().select(ArDkPage.SOSelectMode_DefaultUnit, p1Page.x, p1Page.y);
            if (err!=1)
                return false;

            //  move the end of the selection
            err = dpv.getPage().select(ArDkPage.SOSelectMode_End, p2Page.x, p2Page.y);
            if (err!=1)
                return false;

            //  move the start of the selection
            err = dpv.getPage().select(ArDkPage.SOSelectMode_Start, p1Page.x, p1Page.y);
            if (err!=1)
                return false;
        }

        return true;
    }

    //  for no-UI API
    public int getScrollPositionX()
    {
        return getScrollX();
    }

    //  for no-UI API
    public int getScrollPositionY()
    {
        return getScrollY();
    }

    //  for no-UI API
    public float getScaleFactor()
    {
        return mScale;
    }

    //  for no-UI API
    public void setScaleAndScroll (float newScale, final int newX, final int newY)
    {
        //  set the new scale value
        mScale = newScale;

        //  scale children
        scaleChildren();

        //  layout
        requestLayout();

        if (mReflowMode)
        {
            //  handle possible appearance of new pages
            NUIDocView.currentNUIDocView().onReflowScale();

            //  change the flow mode width and height
            float pct = 1.0f;
            if (NUIDocView.currentNUIDocView().isPageListVisible())
                pct = ((float)getContext().getResources().getInteger(R.integer.sodk_editor_page_width_percentage))/100;
            final float newWidth = pct * mReflowWidth / mScale;
            final float newHeight = getReflowHeight();
            int currentMode = ((SODoc)getDoc()).getFlowMode();
            ((SODoc)getDoc()).setFlowMode(currentMode, newWidth, newHeight);
        }

        //  set scroll values
        setScrollX(newX);
        setScrollY(newY);

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);

                //  report the new page
                mHostActivity.setCurrentPage(mostVisibleChild);
            }
        });
    }

    protected void reportViewChanges()
    {
        mHostActivity.reportViewChanges();
    }
}
