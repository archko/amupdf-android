package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.artifex.solib.ArDkBitmap;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkSelectionLimits;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.SOHyperlink;
import com.artifex.solib.ArDkPage;
import com.artifex.solib.SOPageListener;
import com.artifex.solib.ArDkRender;
import com.artifex.solib.SORenderListener;

public class DocPageView extends View implements SOPageListener
{
    private ArDkDoc mDoc;
    private static final String  TAG           = "DocPageView";
    private int mPageNum = -1;
    protected ArDkPage mPage;
    private ArDkRender mRender=null;
    protected boolean mFinished = false;
    private SODataLeakHandlers mDataLeakHandlers;

    protected float mScale = 1.0f;
    protected double mZoom = 1.0;

    //  rendering layer
    protected int mLayer = ArDkPage.SOLayer_All;

    //  rendering geometry
    private Rect mRenderToRect = new Rect();
    private Rect mDrawToRect = new Rect();
    protected Rect mPageRect = new Rect();
    protected PointF mRenderOrigin = new PointF();
    private int screenLoc[] = new int[2];

    //  rendering bitmap
    private ArDkBitmap mBitmapRender = null;
    private final Rect renderRect = new Rect();
    private float renderScale;

    //  holding bitmap
    private ArDkBitmap mBitmapDrawHold = null;
    private final Rect drawRectHold = new Rect();
    private float drawScaleHold;
    private int mBackgroundColorHold = ContextCompat.getColor(getContext(), R.color.sodk_editor_page_default_bg_color);

    //  drawing bitmap
    private ArDkBitmap mBitmapDraw = null;
    private final Rect drawRect = new Rect();
    private float drawScale;
    private int mBackgroundColor = ContextCompat.getColor(getContext(), R.color.sodk_editor_page_default_bg_color);

    //  current size of this view
    protected Point mSize;

    //  for drawing
    private final Paint mPainter;
    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();
    private final Paint mBlankPainter;
    private final Paint mSelectedBorderPainter;
    private final Paint mBorderPainter;
    private final Rect mBorderRect = new Rect();

    private static final boolean DEBUG_PAGE_RENDERING = false;

    //  use this to control whether the selected border is drawn.
    private boolean isCurrent = false;

    //  track whether this page is valid for drawing its content
    //  false when the doc is in the background.
    private boolean valid = true;
    protected boolean isValid() {return valid;}

    //  this view draws it's content from this bitmap if the page is not valid.
    private Bitmap lowResBitmap = null;

    //  size of the screen when the lowres bitmap was created
    private Point lowResScreenSize = null;

    //  a painter for creating the low res bitmap
    private static Paint lowResPainter = null;

    protected ArDkDoc getDoc() {return mDoc;}

    //  we keep a reference to the DocView to which we belong.
    private DocView mDocView = null;
    public void setDocView(DocView dv)
    {
        mDocView=dv;

        // Good place to check dataLeakHandler.
        getDataLeakHandlers();
    }
    protected DocView getDocView() {return mDocView;}

    public DocPageView(Context context, ArDkDoc theDoc)
    {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        resetBackground();

        mDoc = theDoc;
        mPainter = new Paint();

        mBlankPainter = new Paint();
        mBlankPainter.setStyle(Paint.Style.FILL);
        mBlankPainter.setColor(mBackgroundColor);

        mBorderPainter = new Paint();
        mBorderPainter.setColor(ContextCompat.getColor(getContext(), R.color.sodk_editor_page_border_color));
        mBorderPainter.setStyle(Paint.Style.STROKE);
        mBorderPainter.setStrokeWidth(Utilities.convertDpToPixel(2));

        mSelectedBorderPainter = new Paint();
        setSelectedBorderColor(ContextCompat.getColor(getContext(), R.color.sodk_editor_selected_page_border_color));
        mSelectedBorderPainter.setStyle(Paint.Style.STROKE);
        mSelectedBorderPainter.setStrokeWidth(Utilities.convertDpToPixel(context.getResources().getInteger(R.integer.sodk_editor_selected_page_border_width)));

        //  create the low res bitmap painter
        if (lowResPainter==null) {
            lowResPainter = new Paint();
            lowResPainter.setAntiAlias(true);
            lowResPainter.setFilterBitmap(true);
            lowResPainter.setDither(true);
        }
    }

    public void setLayer(int layer) {
        mLayer = layer;
    }

    public void setSelectedBorderColor(int color)
    {
        mSelectedBorderPainter.setColor(color);
    }

    public double getZoomScale() {return mZoom*mScale;}

    //  this function is used by reflow mode to handle cases where
    //  a change in scale causes new pages to appear in the document,
    //  and those pages need their zoom, scale and size set.

    public void onReflowScale(DocPageView that)
    {
        mZoom = that.mZoom;
        mScale = that.mScale;
        mSize.x = that.mSize.x;
        mSize.y = that.mSize.y;
        requestLayout();
    }

    private Rect scaleRectF(RectF rectf, float factor)
    {
        Rect rect = new Rect();
        rect.left   = (int)(rectf.left*factor);
        rect.top    = (int)(rectf.top*factor);
        rect.right  = (int)(rectf.right*factor);
        rect.bottom = (int)(rectf.bottom*factor);
        return rect;
    }

    public int getPageNumber() {return mPageNum;}

    //  set the validity of the page view
    public void setValid(boolean val)
    {
        if (val == valid)
            return;

        valid = val;

        if (!valid)
        {
            //  when we are set invalid, create a low res bitmap of our contents, which will be drawn
            //  during the time we're invalid.

            if (isShown() && mBitmapDraw!=null && !mBitmapDraw.getBitmap().isRecycled())
            {
                lowResScreenSize = Utilities.getScreenSize(getContext());

                int width = mBitmapDraw.getWidth()/2;
                int height = mBitmapDraw.getHeight()/2;
                Rect lowResBitmapRect = new Rect(0, 0, width, height);
                lowResBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas newCanvas = new Canvas(lowResBitmap);
                newCanvas.drawBitmap(mBitmapDraw.getBitmap(), mBitmapDraw.getRect(), lowResBitmapRect, lowResPainter);
            }

            //  de-reference bitmaps.
            mBitmapDraw = null;
            mBitmapDrawHold = null;
            mBitmapRender = null;
        }
        else
        {
            //  we're valid now, so lose the low-res bitmap
            if (lowResBitmap !=null)
                lowResBitmap.recycle();
            lowResBitmap = null;
        }

        invalidate();
    }

    public ArDkSelectionLimits selectionLimits()
    {
        if (mFinished)
            return null;
        return mPage.selectionLimits();
    }

    protected boolean isFinished() {return mFinished;}

    @Override
    public void update(RectF area)
    {
        //  we're being asked to update a portion of the page.

        //  not if we're finished.
        if (mFinished)
            return;

        //  not if we're not visible
        if (!isShown())
            return;

        //  the previous implementation, which tried to cause a redraw of just the updated portion,
        //  was flawed.  And in practice, a lot of the update requests, especially
        //  while typing, are for the whole page.  So, ...

        //  ... just trigger another render cycle
        //  this needs to be done on the UI thread.

        ((Activity)getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                NUIDocView ndv = NUIDocView.currentNUIDocView();
                if (ndv != null)
                    ndv.triggerRender();
            }
        });
    }

    public void setupPage(final int thePageNum, int w, int h)
    {
        if (isFinished())
            return;
        if (!valid)
            return;

        changePage(thePageNum);

        //  calculate zoom factor
        resize(w, 1);
    }

    protected void changePage(int thePageNum)
    {
        if (isFinished())
            return;
        if (!valid)
            return;
        if (mDoc==null)  //  notional fix for 699827
            return;

        //  if the page number has not yet been set, or has changed,
        //  make a new page object.
        if (thePageNum != mPageNum || mPage==null)
        {
            mPageNum = thePageNum;

            //  destroy the current page before making a new one.
            dropPage();

            mPage = mDoc.getPage(mPageNum, this);
            mDoc.addPage(mPage);
        }
    }

    protected void dropPage()
    {
        //  destroy the current page
        //  used during reloading.
        if (mPage!=null) {
            mDoc.removePage(mPage);
            mPage.releasePage();
        }
        mPage = null;
    }

    public void resize(int w, int h)
    {
        if (mPage==null)
            return;  //  notional fix for 699527

        PointF zoomFit = mPage.zoomToFitRect(w, h);
        mZoom = Math.max(zoomFit.x, zoomFit.y);
        mSize = mPage.sizeAtZoom(mZoom);
    }

    public boolean sizeViewToPage()
    {
        // Take care on shutdown.
        if (mPage == null)
        {
            return false;
        }

        //  prevent Bug 698544
        if (mSize==null)
            return false;

        //  previous size
        int x = mSize.x;
        int y = mSize.y;

        //  set new size
        mSize = mPage.sizeAtZoom(mZoom);

        //  prevent Bug 698544
        if (mSize==null)
            return false;

        //  did it change?
        return (mSize.x!=x || mSize.y!=y);
    }

    public Point getSize() {return mSize;}

    public ArDkPage getPage() {return mPage;}

    public void setNewScale(float scale) {
        mScale = scale;
    }

    private boolean isReflowMode()
    {
        DocView dv = getDocView();
        if (dv!=null && dv.getReflowMode())
            return true;
        return false;
    }

    private boolean pagesShowing()
    {
        DocView dv = getDocView();
        if (dv!=null && dv.pagesShowing())
            return true;

        return false;
    }

    public int getUnscaledWidth()
    {
        if (isReflowMode())
        {
            Point p = mPage.sizeAtZoom(mZoom);
            return p.x;
        }

        return mSize.x;
    }
    public int getUnscaledHeight()
    {
        if (isReflowMode())
        {
            Point p = mPage.sizeAtZoom(mZoom);
            return p.y;
        }

        return mSize.y;
    }

    public int getReflowWidth()
    {
        //  reflow width is just the width of the page at zoom=1.
        Point p = mPage.sizeAtZoom(1.0f);
        return p.x;
    }

    public void render(ArDkBitmap bitmap, final SORenderListener listener)
    {
        if (mFinished)
            return;

        if (mPage==null)
            return;  //  notional fix for 702514

        //  get local visible rect
        Rect localVisRect = new Rect();
        if (!getLocalVisibleRect(localVisRect)) {
            listener.progress(0);
            return;  //  not visible
        }

        //  get global visible rect
        Rect globalVisRect = new Rect();
        if (!getGlobalVisibleRect(globalVisRect)) {
            listener.progress(0);
            return;  //  not visible
        }

        //  do the render.
        renderPage(bitmap, listener, localVisRect, globalVisRect);
    }

    //  called at the beginning of a render pass.
    public void startRenderPass()
    {
    }

    public void resetBackground()
    {
        mBackgroundColor = ContextCompat.getColor(getContext(), R.color.sodk_editor_page_default_bg_color);
        mBackgroundColorHold = ContextCompat.getColor(getContext(), R.color.sodk_editor_page_default_bg_color);
    }

    //  called when a render pass is complete.
    public void endRenderPass()
    {
        //  set the new draw bitmap rect and scale.
        mBitmapDraw = mBitmapDrawHold;
        drawRect.set(drawRectHold);
        drawScale = drawScaleHold;
        mBackgroundColor = mBackgroundColorHold;
    }

    protected void setPageRect()
    {
        //  our page rect , also shifted to include bitmap margins
        getLocationOnScreen(screenLoc);
        mPageRect.set(screenLoc[0], screenLoc[1], screenLoc[0]+getChildRect().width(), screenLoc[1]+getChildRect().height());
        mPageRect.offset(NUIDocView.OVERSIZE_MARGIN, NUIDocView.OVERSIZE_MARGIN);
    }

    //  This function renders the document's page.
    private void renderPage(ArDkBitmap bitmap, final SORenderListener listener, Rect localVisRect, Rect globalVisRect)
    {
        //  our global rect, shifted to include bitmap margins
        mRenderToRect.set(globalVisRect);
        mRenderToRect.offset(NUIDocView.OVERSIZE_MARGIN, NUIDocView.OVERSIZE_MARGIN);

        //  our drawing rect
        mDrawToRect.set(localVisRect);

        setPageRect();

        //  enlarge rendering and display rects to account for the margins
        int topMargin    = Math.min(Math.max(mRenderToRect.top  - mPageRect.top,        0), NUIDocView.OVERSIZE_MARGIN);
        int bottomMargin = Math.min(Math.max(mPageRect.bottom   - mRenderToRect.bottom, 0), NUIDocView.OVERSIZE_MARGIN);
        int leftMargin   = Math.min(Math.max(mRenderToRect.left - mPageRect.left,       0), NUIDocView.OVERSIZE_MARGIN);
        int rightMargin  = Math.min(Math.max(mPageRect.right    - mRenderToRect.right,  0), NUIDocView.OVERSIZE_MARGIN);

        //  limit the right or left edge is the page list is showing.
        if (pagesShowing())
        {
            boolean isThumb = (getParent() instanceof DocListPagesView);
            if (isThumb)
                leftMargin = Math.min(Math.max(mRenderToRect.left - mPageRect.left, 0), 0);
            else
                rightMargin = Math.min(Math.max(mPageRect.right - mRenderToRect.right, 0), 0);
        }

        mRenderToRect.top    -= topMargin;
        mRenderToRect.bottom += bottomMargin;
        mRenderToRect.left   -= leftMargin;
        mRenderToRect.right  += rightMargin;

        mDrawToRect.top      -= topMargin;
        mDrawToRect.bottom   += bottomMargin;
        mDrawToRect.left     -= leftMargin;
        mDrawToRect.right    += rightMargin;

        //  clip to the bitmap

        if (mRenderToRect.left<0) {
            int dx = -mRenderToRect.left;
            mRenderToRect.left += dx;
            mDrawToRect.left += dx;
        }
        if (mRenderToRect.right>bitmap.getWidth()) {
            int dx = mRenderToRect.right-bitmap.getWidth();
            mRenderToRect.right -= dx;
            mDrawToRect.right -= dx;
        }
        if (mRenderToRect.top<0) {
            int dy = -mRenderToRect.top;
            mRenderToRect.top += dy;
            mDrawToRect.top += dy;
        }
        if (mRenderToRect.bottom>bitmap.getHeight()) {
            int dy = mRenderToRect.bottom-bitmap.getHeight();
            mRenderToRect.bottom -= dy;
            mDrawToRect.bottom -= dy;
        }

        //  figure the rendering rect, scale and make the ArDkBitmap
        renderRect.set(mDrawToRect);
        renderScale = mScale;
        mBitmapRender = bitmap.createBitmap(mRenderToRect.left, mRenderToRect.top, mRenderToRect.right, mRenderToRect.bottom);

        if (DEBUG_PAGE_RENDERING)
        {
            //  simulate a render by drawing

            Paint p = new Paint();
            Canvas c = new Canvas(mBitmapRender.getBitmap());
            p.setColor(Color.CYAN);
            p.setStyle(Paint.Style.FILL);
            c.drawRect(mBitmapRender.getRect(),p);

            String s = "" + (mPageNum+1);
            p.setColor(Color.RED);
            p.setTextSize(200.0f*mScale);
            c.drawText(s, mBitmapRender.getRect().left+(50*mScale), mBitmapRender.getRect().top+(250*mScale), p);

            //  simulate render delay
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mBitmapDrawHold = mBitmapRender;
                    drawRectHold.set(renderRect);
                    drawScaleHold = renderScale;
                    mBackgroundColorHold = averageCornerColors(mBitmapDrawHold);

                    listener.progress(0);
                }
            }, 10);
        }
        else
        {
            //  render a document page
            //  inverted if we're in night mode if ConfigOptions allows
            boolean appInvert = ArDkLib.getAppConfigOptions().isInvertContentInDarkModeEnabled();
            boolean invert = appInvert && ArDkLib.isNightMode(getContext());

            setOrigin();
            mRender = mPage.renderLayerAtZoomWithAlpha(mLayer, mScale * mZoom, mRenderOrigin.x, mRenderOrigin.y, mBitmapRender, null, new SORenderListener()
            {
                @Override
                public void progress(int error)
                {
                    if (mFinished)
                        return;

                    stopRender();

                    if (error == 0)
                    {
                        //  when a page render is done, we hold the results until the end of the rendering pass.
                        //  these values are then made active when endRenderPass() is called.
                        mBitmapDrawHold = mBitmapRender;
                        drawRectHold.set(renderRect);
                        drawScaleHold = renderScale;
                        mBackgroundColorHold = averageCornerColors(mBitmapDrawHold);
                    }
                    else
                    {
                        System.out.printf("render error %d for page %d  %n", error, mPageNum);
                    }

                    //  this render is done, notify caller
                    listener.progress(error);
                }
            }, true,invert);
        }
    }

    protected void setOrigin()
    {
        mRenderOrigin.set(-mDrawToRect.left, -mDrawToRect.top);
    }

    private int averageCornerColors(ArDkBitmap bitmap)
    {
        if (!valid)
            return 0;

        //  notional fix for #700588
        if (bitmap==null)
            return Color.WHITE;
        if (bitmap.getRect()==null || bitmap.getBitmap()==null)
            return Color.WHITE;

        //  coordinates of the four corners
        int left   = bitmap.getRect().left+5;
        int top    = bitmap.getRect().top+5;
        int right  = bitmap.getRect().right-5;
        int bottom = bitmap.getRect().bottom-5;

        //  colors at those corners
        int c1 = bitmap.getBitmap().getPixel(left,  top);
        int c2 = bitmap.getBitmap().getPixel(right, top);
        int c3 = bitmap.getBitmap().getPixel(left,  bottom);
        int c4 = bitmap.getBitmap().getPixel(right, bottom);

        //  average
        return averageColors(new int[]{c1, c2, c3, c4});
    }

    private int averageColors(int [] colors)
    {
        int redBucket   = 0;
        int greenBucket = 0;
        int blueBucket  = 0;
        int alphaBucket = 0;

        for (int i=0; i<colors.length; i++)
        {
            int color = colors[i];
            redBucket   += (color >> 16) & 0xFF; // Color.red
            greenBucket += (color >> 8) & 0xFF; // Color.greed
            blueBucket  += (color & 0xFF); // Color.blue
            alphaBucket += (color >>> 24); // Color.alpha
        }

        return Color.argb(
                alphaBucket / colors.length,
                redBucket   / colors.length,
                greenBucket / colors.length,
                blueBucket  / colors.length);
    }

    //  set or clear the clipping path
    Path clipPath = null;
    public void setClipPath(Path path)
    {
        clipPath = path;
    }
    public Path getClipPath() {return clipPath;}

    //  clear previously rendered content
    public void clearContent()
    {
        mBitmapDraw = null;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        if (mFinished)
            return;

        if (!isShown())
            return;

        if (mPage==null)
            return;  //  notional fix for 700568

        // draw the low res bitmap if we're not valid
        if (lowResBitmap != null)
        {
            Rect srcRect = new Rect(0, 0, lowResBitmap.getWidth(), lowResBitmap.getHeight());

            //  use a different output rect if the screen size has changed since making the
            //  low res bitmap.
            Rect dstRect;
            Point currentScreenSize = Utilities.getScreenSize(getContext());
            if (lowResScreenSize!=null && currentScreenSize!=null && (lowResScreenSize.x!=currentScreenSize.x || lowResScreenSize.y!=currentScreenSize.y)) {
                dstRect = new Rect();
                getLocalVisibleRect(dstRect);
            }
            else {
                dstRect = drawRect;
            }

            canvas.drawBitmap(lowResBitmap, srcRect, dstRect, mPainter);
            return;
        }

        //  don't draw if we're not valid
        if (!valid)
            return;

        //  always start with the calculated background colorbackground
        mBlankPainter.setColor(mBackgroundColor);
        Rect rBlank = new Rect();
        getLocalVisibleRect(rBlank);
        canvas.drawRect(rBlank, mBlankPainter);

        if (null != clipPath)
            canvas.save();

        //  get bitmap to draw
        ArDkBitmap bitmap = mBitmapDraw;
        if (bitmap==null || bitmap.getBitmap().isRecycled())
            return;  //  not yet rendered, or recycled

        //  set rectangles for drawing
        mSrcRect.set(bitmap.getRect());
        mDstRect.set(drawRect);

        //  if the scale has changed, adjust the destination
        if (drawScale != mScale)
        {
            mDstRect.left   *= (mScale/drawScale);
            mDstRect.top    *= (mScale/drawScale);
            mDstRect.right  *= (mScale/drawScale);
            mDstRect.bottom *= (mScale/drawScale);
        }

        //  use a clipping path if we have one
        if (null != clipPath)
            canvas.clipPath(clipPath);

        //  draw
        canvas.drawBitmap(bitmap.getBitmap(), mSrcRect, mDstRect, mPainter);

        //  draw a border
        mBorderRect.set(0, 0, getWidth(), getHeight());
        if (isCurrent)
        {
            //  selected
            canvas.drawRect(mBorderRect, mSelectedBorderPainter);
        }
        else
        {
            //  regular
            canvas.drawRect(mBorderRect, mBorderPainter);
        }

        if (null != clipPath)
            canvas.restore();
    }

    protected void launchHyperLink(String url)
    {
        // Use the custom data leakage handlers if available.
        if (mDataLeakHandlers != null)
        {
            try
            {
                mDataLeakHandlers.launchUrlHandler(url);
            }
            catch(UnsupportedOperationException e)
            {
            }
        }
        else
        {
            //  the editor no longer has this capability built-in.
            throw new UnsupportedOperationException();
        }
    }

    protected boolean tryHyperlink(Point pPage, ExternalLinkListener listener)
    {
        SOHyperlink link = mPage.objectAtPoint(pPage.x, pPage.y);
        if (link != null)
        {
            if (link.url!=null)
            {
                //  external link
                ConfigOptions docCfgOpts = mDocView.getDocConfigOptions();

                if (docCfgOpts.isLaunchUrlEnabled())
                {
                    launchHyperLink(link.url);
                }

                return true;
            }
            else if (link.pageNum != -1)
            {
                //  internal link
                if (listener != null)
                {
                    listener.handleExternalLink(link.pageNum, link.bbox);
                }

                return true;
            }
        }

        return false;
    }

    protected boolean handleFullscreenTap(int x, int y)
    {
        Point p = new Point(x, y);
        if (tryHyperlink(p, null))
            return true;

        return false;
    }

    public boolean onSingleTap(int x, int y, boolean canEditText, ExternalLinkListener listener)
    {
        //  NOTE: when double-tapping, a single-tap will also happen first.
        //  so that must be safe to do.

        Point pPage = screenToPage(x, y);

        if (tryHyperlink(pPage, listener))
            return true;

        if (canEditText)
        {
            getDoc().clearSelection();
            mPage.select(ArDkPage.SOSelectMode_Caret, pPage.x, pPage.y);
        }

        return false;
    }

    public void setCaret(int x, int y)
    {
        //  don't select in the middle of a link
        Point p = screenToPage(x, y);
        SOHyperlink link = mPage.objectAtPoint(p.x, p.y);
        if (link != null && link.url!=null || link.pageNum != -1)
            return;

        //  do it
        mPage.select(ArDkPage.SOSelectMode_Caret, p.x, p.y);
    }

    protected boolean canDoubleTap(int x, int y)
    {
        //  double-tapping is normally allowed.
        return true;
    }

    public void onDoubleTap(int x, int y)
    {
        Point p = screenToPage(x, y);
        mPage.select(ArDkPage.SOSelectMode_DefaultUnit, p.x, p.y);
        NUIDocView.currentNUIDocView().showUI(true);
    }

    public void selectTopLeft()
    {
        mPage.select(ArDkPage.SOSelectMode_DefaultUnit, 0, 0);
    }

    protected Point screenToPage(Point p)
    {
        return screenToPage(p.x, p.y);
    }

    protected PointF screenToPage(PointF p)
    {
        Point p2 = screenToPage(p.x, p.y);
        return new PointF(p2);
    }

    private Point screenToPage(float screenX, float screenY)
    {
        return screenToPage((int)screenX, (int)screenY);
    }

    protected Point screenToPage(int screenX, int screenY)
    {
        //  convert to view-relative
        int viewX = screenX;
        int viewY = screenY;
        int loc[] = new int[2];
        getLocationOnScreen(loc);

        //  adjust screen location to window
        loc = Utilities.screenToWindow(loc, getContext());

        viewX -= loc[0];
        viewY -= loc[1];

        //  convert to page-relative
        double factor = getFactor();
        int pageX = (int)(((double)viewX)/factor);
        int pageY = (int)(((double)viewY)/factor);

        return new Point(pageX,pageY);
    }

    //  convert a single value from page to view coordinates
    public int pageToView(int val)
    {
        double factor = getFactor();
        int newVal = (int)(((double)val)*factor);
        return newVal;
    }

    //  convert a point from page to view coordinates.
    public Point pageToView(int pageX, int pageY)
    {
        return new Point(pageToView(pageX), pageToView(pageY));
    }

    public Point viewToPage(int viewX, int viewY)
    {
        double factor = getFactor();

        int pageX = (int)(((double)viewX)/factor);
        int pageY = (int)(((double)viewY)/factor);

        return new Point(pageX, pageY);
    }

    public int viewToPage(int val)
    {
        double factor = getFactor();
        int newVal = (int)(((double)val)/factor);
        return newVal;
    }

    public double getFactor()
    {
        return mZoom * mScale;
    }

    public double getZoom()
    {
        return mZoom;
    }

    protected void pageToView(PointF pageP, PointF viewP)
    {
        double factor = getFactor();

        float x = (pageP.x * (float)factor);
        float y = (pageP.y * (float)factor);

        viewP.set(x, y);
    }

    public void pageToView(Rect pageR, Rect viewR)
    {
        double factor = getFactor();

        int left = (int) (((double) pageR.left) * factor);
        int top = (int) (((double) pageR.top) * factor);
        int right = (int) (((double) pageR.right) * factor);
        int bottom = (int) (((double) pageR.bottom) * factor);

        viewR.set(left, top, right, bottom);
    }

    protected Rect pageToView(RectF rectf)
    {
        double factor = getFactor();

        Rect rect = new Rect();

        rect.left   = (int)Math.round(rectf.left   * factor);
        rect.top    = (int)Math.round(rectf.top    * factor);
        rect.right  = (int)Math.round(rectf.right  * factor);
        rect.bottom = (int)Math.round(rectf.bottom * factor);

        return rect;
    }

    public void finish()
    {
        mFinished = true;

        //  destroy the render
        stopRender();

        //  destroy the page
        if (mPage!=null) {
            mPage.destroyPage();
            mPage = null;
        }

        //  de-reference bitmaps
        mBitmapDraw = null;
        mBitmapDrawHold = null;
        mBitmapRender = null;

        mDoc = null;
    }

    public void stopRender()
    {
        //  destroy the render
        if (mRender!=null) {
            mRender.abort();
            mRender.destroy();
            mRender = null;
        }
    }

    //  return the selection limits data for this page.
    ArDkSelectionLimits getSelectionLimits()
    {
        if (mPage == null)
            return null;

        return mPage.selectionLimits();
    }

    //  set the start of the selection to a point in this page.
    public void setSelectionStart(Point p)
    {
        //  input is in screen coordinates, convert to page
        p = screenToPage(p);
        PointF pf = new PointF(p.x, p.y);

        mPage.select(ArDkPage.SOSelectMode_Start, pf.x, pf.y);
    }

    //  set the end of the selection to a point in this page.
    public void setSelectionEnd(Point p)
    {
        //  input is in screen coordinates, convert to page
        p = screenToPage(p);
        PointF pf = new PointF(p.x, p.y);

        mPage.select(ArDkPage.SOSelectMode_End, pf.x, pf.y);
    }

    public void setCurrent(boolean val)
    {
        if (val != isCurrent)
        {
            isCurrent = val;
            invalidate();
        }
    }

    public boolean isCurrent() {return isCurrent;}

    //  during layout, a DocView-relative rect is calculated and stashed here.
    private final Rect mChildRect = new Rect();
    public void setChildRect(Rect r) {mChildRect.set(r);}
    public Rect getChildRect() {return mChildRect;}

    public interface ExternalLinkListener
    {
        public void handleExternalLink(int pageNum, Rect bbox);
    }

    public Point pageToScreen(Point pPage)
    {
        //  page to view
        double factor = getFactor();
        int x = (int)(((double)pPage.x)*factor);
        int y = (int)(((double)pPage.y)*factor);

        //  view to screen
        int loc[] = new int[2];
        getLocationOnScreen(loc);
        x += loc[0];
        y += loc[1];

        return new Point(x, y);
    }

    //  calculate a Rect in screen coordinates
    //  from a RectF, in page coordinates.
    public Rect pageToScreen(RectF box)
    {
        double factor = getFactor();

        int left   = (int)(((double)box.left  )*factor);
        int top    = (int)(((double)box.top   )*factor);
        int right  = (int)(((double)box.right )*factor);
        int bottom = (int)(((double)box.bottom)*factor);

        int loc[] = new int[2];
        getLocationOnScreen(loc);
        left   += loc[0];
        top    += loc[1];
        right  += loc[0];
        bottom += loc[1];

        return new Rect(left, top, right, bottom);
    }

    /*
     * Obtain an instance of the application data leakage handler class
     * if available.
     */
    private void getDataLeakHandlers()
    {
        String errorBase =
            "getDataLeakHandlers() experienced unexpected exception [%s]";

        try
        {
            //  find a registered instance.
            mDataLeakHandlers = mDocView.getDataLeakHandlers();
            if (mDataLeakHandlers==null)
                throw new ClassNotFoundException();

        }
        catch (ExceptionInInitializerError e)
        {
            Log.e(TAG, String.format(errorBase,
                       "ExceptionInInitializerError"));
        }
        catch (LinkageError e)
        {
            Log.e(TAG, String.format(errorBase, "LinkageError"));
        }
        catch (SecurityException e)
        {
            Log.e(TAG, String.format(errorBase,
                       "SecurityException"));
        }
        catch (ClassNotFoundException e)
        {
            Log.i(TAG, "DataLeakHandlers implementation unavailable");
        }
    }

    public Rect screenRect()
    {
        int loc[] = new int[2];
        getLocationOnScreen(loc);
        Rect r = new Rect();
        r.set(loc[0], loc[1], loc[0]+getChildRect().width(), loc[1]+getChildRect().height());
        return r;
    }

    public void onFullscreen(boolean bFull)
    {
    }

    protected Rect screenToPage(Rect r)
    {
        Point p1 = screenToPage(r.left, r.top);
        Point p2 = screenToPage(r.right, r.bottom);
        return new Rect(p1.x, p1.y, p2.x, p2.y);
    }
}
