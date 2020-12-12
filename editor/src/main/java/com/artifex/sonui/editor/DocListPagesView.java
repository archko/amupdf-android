package com.artifex.sonui.editor;

import android.animation.LayoutTransition;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.artifex.solib.SODoc;

public class DocListPagesView extends DocView
{
    private static final float AUTOSCROLL_MARGIN = 0.15f;
    private static final int AUTOSCROLL_AMOUNT_DP = 8;
    private int AUTOSCROLL_AMOUNT=0;  //  calculated at runtime
    private static final int AUTOSCROLL_TIME = 5;
    private static final int LAYOUT_TRANSITION_DURATION = 200;
    private static final int OFFSET_DP = 10;

    private DocView mMainView;
    int mBorderColor;

    private ImageView mMovingPageView;
    private Bitmap mMovingPageBitmap = null;
    boolean mIsMovingPage = false;
    private int mMovingPageNumber = -1;
    private MotionEvent lastEvent;
    private DocPageView mMovingPage = null;
    private boolean mIsMoving = false;
    private boolean mMoved = false;

    float startPressX, startPressY;
    float startPageX, startPageY;

    public DocListPagesView(Context context) {
        super(context);
        init();
    }

    public DocListPagesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DocListPagesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        AUTOSCROLL_AMOUNT = Utilities.convertDpToPixel(AUTOSCROLL_AMOUNT_DP);
    }

    public void setMainView(DocView v) {mMainView = v;}

    public void setBorderColor(int color) {mBorderColor = color;}

    @Override
    protected void doSingleTap(float fx, float fy)
    {
        Point p = eventToScreen(fx,fy);
        DocPageView v = findPageViewContainingPoint(p.x, p.y, false);
        if (v != null)
        {
            //  add history for where we're leaving (here)
            mMainView.addHistory(mMainView.getScrollX(), mMainView.getScrollY(), mMainView.getScale(), true);

            //  add history for where we're going
            //  unless the distance is zero.
            int newPage = v.getPageNumber();
            Point pnew = mMainView.scrollToPageAmounts(newPage);
            if (pnew.y != 0)
                mMainView.addHistory(mMainView.getScrollX(), mMainView.getScrollY()-pnew.y, mMainView.getScale(), false);

            //  change the current page
            setCurrentPage(newPage);

            //  tell the host about it too
            mHostActivity.setCurrentPage(newPage);

            //  tell the main view to scroll to the top of that page
            mMainView.scrollToPage(newPage, false);
        }
    }

    public void setCurrentPage(int p)
    {
        //  run through the pages and set one of them to be "current"
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView cv = (DocPageView) getOrCreateChild(i);
            if (p==-1)
                cv.setCurrent(false);
            else
                cv.setCurrent(i==p);
            cv.setSelectedBorderColor(getBorderColor());
        }
    }

    public int getCurrentPage()
    {
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView cv = (DocPageView) getOrCreateChild(i);
            if (cv.isCurrent())
                return i;
        }
        return -1;
    }

    public void fitToColumns()
    {
        mLastLayoutColumns = 1;

        //  get current viewport
        final Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);

        //  find the widest page.  This value can be different than it was at the last layout,
        //  so calculate it again.
        int usmaxW = 0;
        for (int i=0; i<getPageCount(); i++)
        {
            DocPageView page = (DocPageView)getOrCreateChild(i);
            usmaxW = Math.max(usmaxW, page.getUnscaledWidth());
        }

        //  calculate a new scale factor and re-scale the pages
        int unscaledTotal = usmaxW*mLastLayoutColumns + UNSCALED_GAP*(mLastLayoutColumns-1);
        mScale = (float)viewport.width()/(float)unscaledTotal;
        scaleChildren();

        requestLayout();
    }

    @Override
    public void onOrientationChange()
    {
        //  get measurements
        final Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        int vpw = viewport.width();
        final int vph = viewport.height();
        int apw = mAllPagesRect.width();
        final int sy = getScrollY();

        //  we rotated before any pages were laid out,
        //  do don't do this now.
        if (apw <= 0)
            return;

        //  fit to width factor
        final float factor = ((float)vpw)/((float)apw);

        //  scale the children and do a layout
        mScale *= factor;
        scaleChildren();
        requestLayout();

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);

                //  after the layout, scroll to the current page
                final int page = getCurrentPage();
                if (page >= 0)
                {
                    //  we need to wait until the resulting layout above is finished, but
                    //  this global layout listener is not cutting it.
                    //  but this small delay does the trick.
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollToPage(page, false);
                        }
                    }, 100);
                }

                //  done forcing the column count
                mForceColumnCount = -1;
            }
        });
    }

    @Override
    protected void setMostVisiblePage()
    {
    }

    @Override
    protected void showHandles()
    {
    }

    @Override
    protected void onEndFling()
    {
    }

    @Override
    protected void doDoubleTap(float fx, float fy)
    {
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector)
    {
        return true;
    }

    @Override
    protected Point constrainScrollBy(int dx, int dy)
    {
        //  don't scroll sideways
        dx = 0;

        Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        int vph = viewport.height();
        int aph = mAllPagesRect.height();
        int sy = getScrollY();

        if (aph <= vph)
        {
            //  all the pages can fit vertically
            //  align at the top.
            dy = -sy;
        }
        else
        {
            //  not too far down
            if (sy+dy < 0)
                dy = -sy;

            //  not too far up
            if (!isMovingPage())
            {
                if (-sy+aph-dy < vph)
                    dy = -(vph-(-sy+aph));
            }
            else
            {
                int mph = mMovingPageView.getHeight();
                if (-sy+aph-dy < vph-mph)
                    dy = -(vph-mph-(-sy+aph));
            }
        }

        return new Point(dx, dy);
    }

    @Override
    public void onShowPages()
    {
    }

    @Override
    public void onHidePages()
    {
    }

    @Override
    public void onShowKeyboard(boolean bShow)
    {
        //  redraw the page list when the keyboard is hidden.
        if (isShown() && !bShow)
            forceLayout();
    }

    public void setup(RelativeLayout layout)
    {
        //  create the moving page view, initially hidden.
        if (mMovingPageView == null)
        {
            mMovingPageView = new ImageView(getContext());
            layout.addView(mMovingPageView);
            mMovingPageView.setVisibility(View.GONE);
        }
    }

    private boolean mCanManipulatePages = false;
    public void setCanManipulatePages(boolean val) {mCanManipulatePages = val;}

    @Override
    public void onLongPress(MotionEvent e)
    {
        if (!mCanManipulatePages)
            return;

        mMoved = false;

        //  use a layout transition
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(LAYOUT_TRANSITION_DURATION);
        transition.enableTransitionType(LayoutTransition.CHANGING);
        setLayoutTransition(transition);

        //  select the page
        Point p = eventToScreen(e.getX(), e.getY());
        mMovingPage = findPageViewContainingPoint(p.x, p.y, false);
        if (mMovingPage != null)
        {
            setCurrentPage(-1);
            mMovingPage.setCurrent(true);

            mPressing = true;
            mIsMovingPage = true;
            mMovingPageNumber = mMovingPage.getPageNumber();

            //  save starting locations
            startPressX = e.getX();
            startPressY = e.getY();
            startPageX = mMovingPage.getX() - getScrollX();
            startPageY = mMovingPage.getY() - getScrollY();

            //  don't render the moving page view util movement is detected.

            startMovingPage(mMovingPageNumber);

            lastEvent = e;
        }
    }

    //  create a bitmap from a given view.
    private Bitmap createBitmapFromView(View v)
    {
        Bitmap b = Bitmap.createBitmap( v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    //  while long-pressing, the pressed-upon page is removed from the list
    //  which may result in no children, but we still want to process
    //  touch events.

    @Override
    protected boolean allowTouchWithoutChildren() {
        return true;
    }

    @Override
    public void onLongPressRelease()
    {
        if (!mCanManipulatePages)
            return;

        setLayoutTransition(null);

        //  hide the moving page view
        if (mMovingPageView != null)
        {
            mMovingPageView.setVisibility(View.GONE);
            mMovingPageView.setImageBitmap(null);
            if (mMovingPageBitmap !=null)
            {
                mMovingPageBitmap.recycle();
                mMovingPageBitmap = null;
            }
        }

        //  drop it.
        if (isEventInside(lastEvent))
            dropMovingPage(mMoved);
        else
            finishDrop();

        mIsMoving = false;
        mPressing = false;
        mIsMovingPage = false;
        mMovingPageNumber = -1;
        mMovingPage = null;
    }

    @Override
    protected void finishDrop()
    {
        mMovingPageView.setVisibility(View.GONE);
        super.finishDrop();
    }

    @Override
    protected void onPageMoved(int newPage)
    {
        setCurrentPage(newPage);
        mMainView.scrollToPage(newPage, false);
    }

    @Override
    public void onLongPressMoving(MotionEvent e)
    {
        if (!mCanManipulatePages)
            return;

        //  move the moving page view
        if (mMovingPageView != null)
        {
            //  calculate new position relative to the outer container
            float dx = e.getX() - startPressX;
            float dy = e.getY() - startPressY;

            int slop = (int) getContext().getResources().getDimension(R.dimen.sodk_editor_drag_slop);
            if (Math.abs(dx)>slop || Math.abs(dy)>slop)
                mMoved = true;

            mIsMoving = true;

            if (mIsMoving)
            {
                float newPageX = startPageX + dx;
                float newPageY = startPageY + dy;

                //  offset a little
                int offset = Utilities.convertDpToPixel(OFFSET_DP);
                newPageX += offset;
                newPageY -= offset;

                //  render the page to the moving view, and show it
                if (mMovingPageView.getVisibility()==View.GONE) {
                    mMovingPageBitmap = createBitmapFromView(mMovingPage);
                    mMovingPageView.setImageBitmap(mMovingPageBitmap);
                    mMovingPageView.setVisibility(View.VISIBLE);
                }

                //  move it
                mMovingPageView.setX(newPageX);
                mMovingPageView.setY(newPageY);
                mMovingPageView.invalidate();

                if (isEventInside(e))
                {
                    //  handle auto-scrolling
                    View parent = (View)getParent();
                    float parentHeight = parent.getHeight();
                    float screenY = e.getY();
                    float parentY = screenY - parent.getTop();

                    //  determine the autoscroll amount.
                    int amount = 0;

                    if (parentY/parentHeight <= AUTOSCROLL_MARGIN && getScrollY()>0) {
                        //  scrolling at the top;
                        amount = -AUTOSCROLL_AMOUNT;
                    }
                    else if (parentY/parentHeight >= 1- AUTOSCROLL_MARGIN) {
                        //  scrolling at the bottom
                        amount = AUTOSCROLL_AMOUNT;
                    }
                    if (amount!=0)
                    {
                        final Handler handler = new Handler();
                        final int scrollBy = amount;
                        super.forceLayout();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scrollBy(0, scrollBy);
                            }
                        }, AUTOSCROLL_TIME);
                    }
                    else
                    {
                        onMovePage(mMovingPageNumber, (int)newPageY-offset + mMovingPageView.getHeight()/2);
                    }
                }

                lastEvent = e;

            }
        }
    }

    private boolean isEventInside(MotionEvent e)
    {
        Rect r = new Rect();
        getHitRect(r);
        int x = (int)e.getX();
        int y = (int)e.getY();
        return r.contains(x, y);
    }

    @Override
    public boolean isMovingPage() {return mIsMovingPage;}

    @Override
    protected int getMovingPageNumber() {return mMovingPageNumber;}

    @Override
    protected boolean centerPagesHorizontally() {return true;}

//    @Override
//    protected boolean clearOnColumnChange() {return false;}

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed,  left,  top,  right,  bottom);
    }

    @Override
    public void onConfigurationChange()
    {
        //  config was changed, probably orientation.
    }

    //  this spinner is shown when a page delete is underway
    private ProgressDialog spinner = null;
    //  this is the page number we're deleting
    private int deletingPageNum = -1;

    @Override
    protected void doPageMenu(final int pageNumber)
    {
        DocPageView cv = (DocPageView) getOrCreateChild(pageNumber);
        PageMenu menu = new PageMenu(getContext(), cv, getPageCount()>1, new PageMenu.ActionListener() {
            @Override
            public void onDelete() {

                //  start a spinner
                spinner = Utilities.createAndShowWaitSpinner(getContext());

                //  delete the page a little later (so the spinner can be shown)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((SODoc)getDoc()).deletePage(pageNumber);
                        deletingPageNum = pageNumber;
                    }
                }, 50);
            }

            @Override
            public void onDuplicate() {

                //  start a spinner
                spinner = Utilities.createAndShowWaitSpinner(getContext());

                //  duplicate the page a little later (so the spinner can be shown)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((SODoc)getDoc()).duplicatePage(pageNumber);
                        onPageMoved(pageNumber);
                    }
                }, 50);
            }
        });
        menu.show();
    }

    @Override
    public void onSelectionChanged()
    {
        if (deletingPageNum!=-1)
        {
            //  we come here after a page has been deleted.

            //  set a new 'current page'
            int count = getPageCount();
            int newPage=0;
            if (deletingPageNum<0) {
                newPage=0;
            }
            else if (deletingPageNum>=count) {
                newPage=count-1;
            }
            else {
                newPage=deletingPageNum;
            }

            //  scroll to the new page and set it as current.
            mMainView.scrollToPage(newPage, true);
            setCurrentPage(newPage);

            //  tell the host about  it too
            mHostActivity.setCurrentPage(newPage);

            //  don't do this twice.
            deletingPageNum = -1;
        }

        forceLayout();

        if (spinner !=null)
        {
            //  dismiss the spinner if it was created.
            spinner.dismiss();
            spinner = null;
        }

    }

    @Override
    protected boolean allowXScroll()
    {
        return false;
    }

    @Override
    protected void reportViewChanges()
    {
    }

}
