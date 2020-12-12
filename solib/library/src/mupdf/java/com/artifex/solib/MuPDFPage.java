package com.artifex.solib;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.DisplayListDevice;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Location;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFPage;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.mupdf.fitz.PDFWidget;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class MuPDFPage extends ArDkPage
{
    private Page mPage;
    private int mPageNumber;
    private MuPDFDoc mDoc;
    private Rect pageBounds = new Rect();
    private Worker worker;

    //  cached display lists
    private DisplayList pageContents = null;
    private DisplayList annotContents = null;
    private DisplayList widgetContents = null;

    private Quad[] searchResults;
    private int searchIndex;
    private String lastSearch = "";

    public static final int PDF_ANNOT_TEXT = PDFAnnotation.TYPE_TEXT;
    public static final int PDF_ANNOT_HIGHLIGHT = PDFAnnotation.TYPE_HIGHLIGHT;
    public static final int PDF_ANNOT_REDACT = PDFAnnotation.TYPE_REDACT;

    MuPDFPage(MuPDFDoc doc, Page page, int pageNumber)
    {
        mPage = page;
        mPageNumber = pageNumber;
        mDoc = doc;

        //  set our bounds
        pageBounds = mPage.getBounds();

        //  set our worker
        worker = mDoc.getWorker();

        //  update the annotations, widgets and text for this page.
        refreshPageElements();
    }

    public void setPage(Page page)
    {
        mPage = page;

        //  update the annotations, widgets and text for this page.
        refreshPageElements();
    }

    private void refreshPageElements()
    {
        //  update the annotations, widgets and text for this page.
        //  this should be done on the background thread, so we check for that.
        if (!worker.isWorkerThread())
            throw new RuntimeException("MuPDFPage.refreshPageElements should be run on the worker thread.");

        updateAnnotations();
        updateWidgets();
        updateText();
    }

    private void updatePageRect(Rect r)
    {
        for (SOPageListener listener: mPageListeners)
        {
            if (r!=null)
                listener.update(toRectF(r));
            else
                listener.update(null);
        }
    }

    public int setSearchString(String s)
    {
        //  the search may throw if the document is corrupt
        //  so we catch that and move on.
        try
        {
            //  generate new results when the string changes
            if (!s.equalsIgnoreCase(lastSearch))
                searchResults = mPage.search(s);
            lastSearch = s;

            //  tell the caller how many
            if (searchResults != null)
                return searchResults.length;
        }
        catch (Exception e)
        {
        }

        return 0;
    }

    public void setSearchIndex(int i)
    {
        searchIndex = i;
        updatePageRect(pageBounds);
    }

    public android.graphics.Rect getSearchHighlight()
    {
        if (searchResults!=null && searchResults.length>0 && searchIndex>=0 && searchIndex<searchResults.length)
            return toRect(searchResults[searchIndex].toRect());
        return null;
    }

    private ArrayList<SOPageListener> mPageListeners = new ArrayList<>();
    public void addPageListener(SOPageListener listener)
    {
        if (!mPageListeners.contains(listener))
            mPageListeners.add(listener);
    }

    @Override
    public PointF zoomToFitRect(int w, int h)
    {
        Rect r = pageBounds;
        float pw = r.x1 - r.x0;
        float ph = r.y1 - r.y0;

        return new PointF(((float)w)/pw, ((float)h)/ph);
    }

    @Override
    public Point sizeAtZoom(double zoom)
    {
        Rect r = pageBounds;
        double pw = zoom*(r.x1 - r.x0);
        double ph = zoom*(r.y1 - r.y0);

        return new Point ((int)pw, (int)ph);
    }

    private void cachePage(Cookie cookie)
    {
        PDFPage pdfPage = getPDFPage(mPage);

        if (pageContents == null)
        {
            pageContents = new DisplayList(null);
            DisplayListDevice dispDev = new DisplayListDevice(pageContents);
            try
            {
                mPage.runPageContents(dispDev, new Matrix(1, 0, 0, 1, 0, 0), cookie);
            }
            catch (RuntimeException e)
            {
                pageContents.destroy();
                pageContents = null;
            }
            finally
            {
                dispDev.destroy();
            }
        }

        if (annotContents == null && pdfPage!=null)
        {
            //  run the annotation list
            annotContents = new DisplayList(null);
            DisplayListDevice annotDev = new DisplayListDevice(annotContents);
            try
            {
                PDFAnnotation annotations[] = pdfPage.getAnnotations();
                if (annotations != null)
                {
                    for (PDFAnnotation annot : annotations)
                    {
                        annot.run(annotDev, new Matrix(1, 0, 0, 1, 0, 0), cookie);
                    }
                }
            }
            catch (RuntimeException e)
            {
                annotContents.destroy();
                annotContents = null;
            }
            finally
            {
                annotDev.destroy();
            }
        }

        if (widgetContents == null && pdfPage!=null)
        {
            //  run the annotation list
            widgetContents = new DisplayList(null);
            DisplayListDevice widgetDev = new DisplayListDevice(widgetContents);
            try
            {
                PDFWidget widgets[] = pdfPage.getWidgets();
                if (widgets != null)
                {
                    for (PDFWidget widget : widgets)
                    {
                        widget.run(widgetDev, new Matrix(1, 0, 0, 1, 0, 0), cookie);
                    }
                }
            }
            catch (RuntimeException e)
            {
                widgetContents.destroy();
                widgetContents = null;
            }
            finally
            {
                widgetDev.destroy();
            }
        }
    }

    @Override
    public ArDkRender renderLayerAtZoomWithAlpha (
            final int layer,
            final double zoom,
            final double originX,
            final double originY,
            final ArDkBitmap bitmap,
            final ArDkBitmap alpha,
            final SORenderListener listener,
            final boolean uiThread,
            final boolean inverted)
    {
        //  page
        final int pageX0 = (int)(originX);
        final int pageY0 = (int)(originY);
        final int pageX1 = pageX0 + bitmap.getWidth();
        final int pageY1 = pageY0 + bitmap.getHeight();

        //  patch
        final int patchX0 = bitmap.getRect().left;
        final int patchY0 = bitmap.getRect().top;
        final int patchX1 = bitmap.getRect().right;
        final int patchY1 = bitmap.getRect().bottom;

        //  set up a matrix for scaling
        final Matrix ctm = Matrix.Identity();
        ctm.scale((float)zoom);

        //  render using a worker
        worker = mDoc.getWorker();
        final MuPDFRender render = new MuPDFRender();
        worker.addFirst(new Worker.Task()
        {
            private boolean failed = false;

            public void work() {

                if (mDestroyed)
                    return;

                if (!worker.isRunning())
                    return;

                //  make a cookie.  This gets put in the MuPDFRender object,
                //  and used in the future to abort a page run.
                Cookie cookie = new Cookie();
                render.setCookie(cookie);

                //  construct page lists
                cachePage(cookie);

                AndroidDrawDevice dev = null;

                try
                {
                    if (!bitmap.getBitmap().isRecycled())
                    {
                        dev = new AndroidDrawDevice(bitmap.getBitmap(), -pageX0, -pageY0,
                                patchX0, patchY0, patchX1, patchY1);

                        if (pageContents != null)
                        {
                            pageContents.run(dev, ctm, cookie);
                        }
                        if (annotContents != null )
                        {
                            annotContents.run(dev, ctm, cookie);
                        }
                        if (widgetContents != null )
                        {
                            widgetContents.run(dev, ctm, cookie);
                        }

                        //  invert the result if asked.
                        if (inverted)
                            dev.invertLuminance();
                    }
                }
                catch (Exception e)
                {
                    failed = true;
                    Log.e("mupdf", e.getMessage());
                }
                finally
                {
                    //  we may arrive here after we've been destroyed and the
                    //  bitmap may no longer be valid
                    if (dev!=null && !mDestroyed && !bitmap.getBitmap().isRecycled())
                    {
                        try {
                            //  close and destroy the draw device
                            dev.close();
                            dev.destroy();

                            //  destroy the cookie and remove it from the render
                            render.setCookie(null);
                            cookie.destroy();
                        }
                        catch (Exception e)
                        {
                            failed = true;
                            Log.e("mupdf", e.getMessage());
                        }
                        finally {}
                    }
                }
            }
            public void run()
            {
                if (listener != null)
                {
                    //  we may arrive here after we've been destroyed and the
                    //  bitmap may no longer be valid
                    if (!mDestroyed && !bitmap.getBitmap().isRecycled()) {
                        if (failed)
                            listener.progress(1);
                        else
                            listener.progress(0);
                    }
                    else
                        listener.progress(1);
                }
            }
        });

        return render;
    }

    void createTextAnnotationAt(PointF point, String author)
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        PDFAnnotation annot = pdfPage.createAnnotation(PDFAnnotation.TYPE_TEXT);

        //  this is where the user tapped
        float x = point.x;
        float y = point.y;

        //  this is a rect where the tapped point is the lower left
        int size = 24;
        Rect r = new Rect(x, y-size, x+size, y);

        //  make it
        annot.setRect(r);

        //  set the author
        annot.setAuthor(author);

        //  set dates
        Date now = new Date();
        annot.setModificationDate(now);

        annot.update();
    }

    void createSignatureAt(final PointF point)
    {
        final PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        //  this is where the user tapped
        float x = point.x;
        float y = point.y;

        PDFWidget widget = pdfPage.createSignature();
        if (widget!=null)
        {
            //  get the rect
            Rect r = widget.getRect();

            //  shift to the tapped location
            float dx = x - r.x0;
            float dy = y - r.y0;
            r.x0 += dx;
            r.x1 += dx;
            r.y0 += dy;
            r.y1 += dy;

            //  set the new rect
            widget.setRect(r);
            widget.update();

            //  mark the doc as modified
            mDoc.setModified(true);
        }
    }

    void createInkAnnotation(SOPoint[] points, float width, int color)
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        PDFAnnotation annot = pdfPage.createAnnotation(PDFAnnotation.TYPE_INK);
        annot.setBorder(width);
        annot.setColor(colorToArray(color));

        //  each stroke is a 1xN array.
        int nPoints = points.length;
        com.artifex.mupdf.fitz.Point[][] inkList = new com.artifex.mupdf.fitz.Point[1][nPoints];
        for (int i=0; i<nPoints; i++)
        {
            inkList[0][i] = new com.artifex.mupdf.fitz.Point(points[i].x, points[i].y);
        }
        annot.setInkList(inkList);

        annot.update();

        //  mark the doc as modified
        mDoc.setModified(true);
    }

    public void update()
    {
        if (mDoc==null)
            return;
        Document pdoc = mDoc.getDocument();
        if (pdoc==null)
            return;

        //  recreate the display lists at the next render
        destroyDisplayLists();

        //  mark the doc as modified
        mDoc.setModified(true);

        //  update the annotations and widgets
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null)
        {
            pdfPage.update();

            //  update the annotations, widgets and text for this page.
            refreshPageElements();
        }
    }

    //  copy of the annotations, widgets and text for the page.
    //  these are kept updated on the background thread and
    //  can be used on the UI thread.
    //  CopyOnWriteArrayList is used for thread-safety.
    private CopyOnWriteArrayList<PDFAnnotation> mAnnotations = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<PDFWidget> mWidgets = new CopyOnWriteArrayList<>();
    private StructuredText mStructuredText;

    public void updateWidgets()
    {
        mWidgets.clear();
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null)
        {
            PDFWidget[] widgets = pdfPage.getWidgets();
            if (widgets != null && widgets.length > 0)
            {
                for (int i = 0; i < widgets.length; i++)
                {
                    PDFWidget widget = widgets[i];
                    if (widget != null)
                    {
                        widget.update();
                        mWidgets.add(widget);
                    }
                }
            }
        }
    }

    public void updateAnnotations()
    {
        mAnnotations.clear();
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null)
        {
            PDFAnnotation[] annots = pdfPage.getAnnotations();
            if (annots != null && annots.length > 0)
            {
                for (int i = 0; i < annots.length; i++)
                {
                    PDFAnnotation annot = (PDFAnnotation) annots[i];
                    if (annot != null)
                    {
                        annot.update();
                        mAnnotations.add(annot);
                    }
                }
            }
        }
    }

    public void updateText()
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null)
        {
            mStructuredText = pdfPage.toStructuredText();
        }
        else
        {
            mStructuredText = null;
        }
    }

    private boolean canSelectAnnotation(PDFAnnotation pdfAnnot)
    {
        //  determine if a tapped annotation can be selected.
        //  we disallow widgets so as to fix bug 698967
        if (pdfAnnot != null && pdfAnnot.getType()==PDFAnnotation.TYPE_WIDGET)
            return false;

        return true;
    }

    public int findSelectableAnnotAtPoint(Point p, int type)
    {
        //  find an annotation that contains the given point, and is also
        //  of the given type.  if the given type is -1, consider all annotations.
        //  return -1 if no matching annotation is found.

        //  if this is the wrong kind of page
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return -1;

        //  if there are no annotations to consider
        if (mAnnotations ==null || mAnnotations.size()==0)
            return -1;

        //  loop through the annotations
        for (int i = 0; i < mAnnotations.size(); i++)
        {
            PDFAnnotation annot = mAnnotations.get(i);
            if (annot!=null && canSelectAnnotation(annot))  //  must be selectable
            {
                if (annot.getType()==type || type==-1)
                {
                    Rect r = annot.getRect();
                    if (r.contains(p.x, p.y))
                    {
                        //  got one
                        return i;
                    }
                }
            }
        }

        //  got none
        return -1;
    }

    public void selectAnnot(int index)
    {
        //  if this is the wrong kind of page
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        //  if there are no annotations
        if (mAnnotations ==null || mAnnotations.size()==0)
            return;

        PDFAnnotation annot = mAnnotations.get(index);
        if (annot!=null)
        {
            //  select it
            mDoc.setSelectedAnnotation(mPageNumber, index);

            //  redraw the page
            Rect r = annot.getRect();
            for (SOPageListener listener: mPageListeners)
                listener.update(toRectF(r));
            updatePageRect(r);
            mDoc.onSelectionUpdate(mPageNumber);
        }
    }

    @Override
    public int select(int mode, double atX, double atY)
    {
        Point p = new Point((int) atX, (int) atY);

        //  assume nothing selected
        mDoc.setSelectedAnnotation(-1, -1);

        if (mode == ArDkPage.SOSelectMode_Start)
        {
            //  dragging the selection
            selectionStart = p;
            updateSelectionRects();
            mDoc.onSelectionUpdate(mPageNumber);
            updatePageRect(pageBounds);
            return 1;
        }

        if (mode == ArDkPage.SOSelectMode_End)
        {
            //  dragging the selection
            selectionEnd = p;
            updateSelectionRects();
            mDoc.onSelectionUpdate(mPageNumber);
            updatePageRect(pageBounds);
            return 1;
        }

        //  try to select a word
        android.graphics.Rect wordRect = selectWord(p);
        if (wordRect != null)
        {
            selectionStart = new Point(wordRect.left, wordRect.top);
            selectionEnd   = new Point(wordRect.right, wordRect.bottom);
            mSelectionRects = new Rect[1];
            mSelectionRects[0] = toRect(wordRect);
            mTextSelPageNum = mPageNumber;
            updatePageRect(toRect(wordRect));
            mDoc.onSelectionUpdate(mPageNumber);
        }
        else
        {
            mSelectionRects = null;
            updatePageRect(pageBounds);
            mDoc.onSelectionUpdate(mPageNumber);
            mTextSelPageNum = -1;
        }

        return 1;
    }

    private android.graphics.Rect toRect(Rect r)
    {
        android.graphics.Rect rNew = new android.graphics.Rect((int)r.x0, (int)r.y0, (int)r.x1, (int)r.y1);
        return rNew;
    }

    private android.graphics.RectF toRectF(Rect r)
    {
        android.graphics.RectF rNew = new android.graphics.RectF(r.x0, r.y0, r.x1, r.y1);
        return rNew;
    }
    public android.graphics.RectF toRectF(android.graphics.Rect r)
    {
        android.graphics.RectF rNew = new android.graphics.RectF(r.left, r.top, r.right, r.bottom);
        return rNew;
    }

    private Rect toRect(android.graphics.Rect r)
    {
        Rect rNew = new Rect(r.left, r.top, r.right, r.bottom);
        return rNew;
    }

    @Override
    public ArDkSelectionLimits selectionLimits()
    {
        //  is an annotation selected?
        PDFAnnotation annot = (PDFAnnotation)mDoc.getSelectedAnnotation();
        if (annot != null)
        {
            //  is it on our page?
            int pageNum = mDoc.getSelectedAnnotationPagenum();
            if (pageNum == this.mPageNumber)
            {
                Rect r =  annot.getRect();
                MuPDFSelectionLimits limits = new MuPDFSelectionLimits(toRect(r));
                return limits;
            }
        }

        //  is some text selected?
        if (mSelectionRects!=null && mSelectionRects.length>0)
        {
            PointF p1 = new PointF(mSelectionRects[0].x0, mSelectionRects[0].y0);
            PointF p2 = new PointF(mSelectionRects[mSelectionRects.length-1].x1, mSelectionRects[mSelectionRects.length-1].y1);
            MuPDFSelectionLimits limits = new MuPDFSelectionLimits(p1, p2);

            return limits;
        }

        return null;
    }

    public PDFAnnotation getAnnotation(int index)
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null)
        {
            if (mAnnotations != null && mAnnotations.size() > index)
                return mAnnotations.get(index);
        }

        return null;
    }

    public android.graphics.Rect getSelectedAnnotationRect()
    {
        //  is one selected?
        PDFAnnotation annot = (PDFAnnotation)mDoc.getSelectedAnnotation();
        if (annot != null)
        {
            //  is it on our page?
            int pageNum = mDoc.getSelectedAnnotationPagenum();
            if (pageNum == this.mPageNumber)
            {
                Rect r = annot.getRect();
                return toRect(r);
            }
        }

        return null;
    }

    public void deleteAnnotation(PDFAnnotation annot)
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null) {
            pdfPage.deleteAnnotation(annot);
        }
    }

    public void deleteWidget(MuPDFWidget widget)
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage!=null) {

            PDFAnnotation annot = widget.asAnnotation();
            pdfPage.deleteAnnotation(annot);

            //  mark the doc as modified
            mDoc.setModified(true);
        }
    }

    //===============================

    //  current selection
    Rect[] mSelectionRects = null;
    private Point selectionStart = null;
    private Point selectionEnd = null;

    public android.graphics.Rect[] getSelectionRects()
    {
        if (mSelectionRects==null)
            return null;

        android.graphics.Rect rects[] = new android.graphics.Rect[mSelectionRects.length];
        for (int i=0; i<mSelectionRects.length; i++) {
            rects[i] = toRect(mSelectionRects[i]);
        }

        return rects;
    }

    float[] colorToArray(int color)
    {
        //  color
        //  Using 3 colors means RGB.
        //  Values are in the range 0-1, not 0-255.

        float[] colors = new float[3];
        colors[0] = ((float)Color.red(color))/255f;
        colors[1] = ((float)Color.green(color))/255f;
        colors[2] = ((float)Color.blue(color))/255f;

        return colors;
    }

    //  convert a rect into a mupdf fitz Quad.
    public static com.artifex.mupdf.fitz.Quad rectToQuad(Rect r)
    {
        com.artifex.mupdf.fitz.Quad quad =
                new com.artifex.mupdf.fitz.Quad(
                        r.x0, r.y0,
                        r.x1, r.y0,
                        r.x0, r.y1,
                        r.x1, r.y1
                        );

        return quad;
    }

    //  convert an array of rects to an array of mupdf fitz Quads
    public static com.artifex.mupdf.fitz.Quad [] rectsToQuads(Rect[] rects)
    {
        com.artifex.mupdf.fitz.Quad[] quads = new com.artifex.mupdf.fitz.Quad[rects.length];
        int i = 0;
        for (Rect r:rects)
        {
            quads[i] = rectToQuad(r);
            i++;
        }

        return quads;
    }

    private void addAnnotation(int type)
    {
        //  must have a page
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        //  must be the page that has the selection
        if (mTextSelPageNum != mPageNumber)
            return;

        //  create the annotation
        PDFAnnotation annot = pdfPage.createAnnotation(type);

        //  color
        if (type == PDFAnnotation.TYPE_HIGHLIGHT)
            annot.setColor(colorToArray(0xffff00));

        //  array of quads
        annot.setQuadPoints(rectsToQuads(mSelectionRects));

        //  set the author
        annot.setAuthor("SmartOffice");

        //  set mod date
        Date now = new Date();
        annot.setModificationDate(now);

        annot.update();
    }

    public void addRedactAnnotation(android.graphics.Rect r)
    {
        //  must have a page
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        //  create the annotation
        PDFAnnotation annot = pdfPage.createAnnotation(PDFAnnotation.TYPE_REDACT);

        //  array of quads
        Rect rf = new com.artifex.mupdf.fitz.Rect(r.left, r.top, r.right, r.bottom);
        Rect[] rfs = {rf};
        annot.setQuadPoints(rectsToQuads(rfs));

        //  set the author
        annot.setAuthor("SmartOffice");

        //  set mod date
        Date now = new Date();
        annot.setModificationDate(now);

        annot.update();
    }

    public void addRedactAnnotation()
    {
        addAnnotation(PDFAnnotation.TYPE_REDACT);
    }

    public void applyRedactAnnotation()
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return;

        pdfPage.applyRedactions();
    }

    public void addHighlightAnnotation()
    {
        addAnnotation(PDFAnnotation.TYPE_HIGHLIGHT);
    }

    public void clearSelection()
    {
        mSelectionRects = null;
        selectionStart = null;
        selectionEnd = null;
        mTextSelPageNum = -1;
        updatePageRect(null);
        mDoc.onSelectionUpdate(mPageNumber);
    }

    private static int mTextSelPageNum = -1;
    public static int getTextSelPageNum() {return mTextSelPageNum;}
    public void clearSelectedText()
    {
        if (mPageNumber == mTextSelPageNum)
        {
            clearSelection();
        }
    }

    private void updateSelectionRects()
    {
        mSelectionRects = null;
        mTextSelPageNum = -1;
        if (selectionStart!=null && selectionEnd!=null && mStructuredText !=null)
        {
            Rect r = new Rect(selectionStart.x, selectionStart.y, selectionEnd.x, selectionEnd.y);

            com.artifex.mupdf.fitz.Point p1 = new com.artifex.mupdf.fitz.Point((int)r.x0, (int)r.y0);
            com.artifex.mupdf.fitz.Point p2 = new com.artifex.mupdf.fitz.Point((int)r.x1, (int)r.y1);

            Quad[] quads = mStructuredText.highlight(p1, p2);
            if (quads!=null && quads.length>0)
            {
                mSelectionRects = new Rect[quads.length];
                mTextSelPageNum = mPageNumber;
                for (int i=0; i<quads.length; i++)
                {
                    mSelectionRects[i] = quads[i].toRect();
                }
            }
        }
    }

    private StructuredText.TextBlock blockContainingPoint(StructuredText.TextBlock blocks[], Point p)
    {
        if (blocks != null)
        {
            for (StructuredText.TextBlock block : blocks)
            {
                if (block !=null && block.bbox.contains(p.x, p.y))
                    return block;
            }
        }

        return null;
    }

    private StructuredText.TextLine lineContainingPoint(StructuredText.TextLine lines[], Point p)
    {
        if (lines != null)
        {
            for (StructuredText.TextLine line : lines)
            {
                if (line !=null && line.bbox.contains(p.x, p.y))
                    return line;
            }
        }

        return null;
    }

    private android.graphics.Rect selectWord(Point pPage)
    {
        if (mStructuredText ==null)
            return null;

        //  get structured text and the block structure
        StructuredText.TextBlock textBlocks[] = mStructuredText.getBlocks();

        StructuredText.TextBlock block = blockContainingPoint(textBlocks, pPage);
        if (block == null)
            return null;

        StructuredText.TextLine line = lineContainingPoint(block.lines, pPage);
        if (line == null)
            return null;

        //  find the char containing my point
        int n = -1;
        int i;
        for (i = 0; i < line.chars.length; i++)
        {
            if (line.chars[i].quad.toRect().contains(pPage.x, pPage.y))
            {
                n = i;
                break;
            }
        }
        //  not found
        if (n == -1)
            return null;
        //  must be non-blank
        if (line.chars[n].isWhitespace())
            return null;

        //  look forward for a space, or the end
        int nEnd = n;
        while (nEnd + 1 < line.chars.length && !line.chars[nEnd + 1].isWhitespace())
            nEnd++;

        //  look backward for a space, or the beginning
        int nStart = n;
        while (nStart - 1 >= 0 && !line.chars[nStart - 1].isWhitespace())
            nStart--;

        ArrayList<StructuredText.TextChar> mSelection = new ArrayList<>();
        com.artifex.mupdf.fitz.Rect rWord = new com.artifex.mupdf.fitz.Rect();
        for (i = nStart; i <= nEnd; i++)
        {
            mSelection.add(line.chars[i]);
            rWord.union(line.chars[i].quad.toRect());
        }

        return new android.graphics.Rect((int) rWord.x0, (int) rWord.y0, (int) rWord.x1, (int) rWord.y1);
    }

    @Override
    public void releasePage()
    {
    }

    private boolean mDestroyed = false;
    private boolean mPDFPageDestroyed = false;

    private void destroyPdfPage()
    {
        mDestroyed = true;

        if (!mPDFPageDestroyed)
        {
            mPage.destroy();
            mPage = null;
            mPDFPageDestroyed = true;
        }
    }

    private void destroyDisplayLists()
    {
        if (pageContents!=null) {
            pageContents.destroy();
            pageContents = null;
        }
        if (annotContents!=null) {
            annotContents.destroy();
            annotContents = null;
        }
        if (widgetContents!=null) {
            widgetContents.destroy();
            widgetContents = null;
        }
    }

    @Override
    public void destroyPage()
    {
        mDestroyed = true;
        destroyDisplayLists();
        mPageListeners.clear();
        mDoc = null;
        destroyPdfPage();
    }

    protected void finalize() throws Throwable
    {
        try
        {
            mPageListeners.clear();
            destroyDisplayLists();
            mDoc = null;
            destroyPdfPage();
        }
        finally
        {
//            super.finalize();
        }
    }

    @Override
    public SOHyperlink objectAtPoint(float atX, float atY)
    {
        Document doc = mDoc.getDocument();
        if (doc==null)
            return null;

        Link[] links = mPage.getLinks();
        if (links==null || links.length==0)
            return null;

        for (Link link: links)
        {
            if (link.bounds.contains(atX, atY))
            {
                SOHyperlink hyper = new SOHyperlink();
                Location    loc   = doc.resolveLink(link);
                int         page  = doc.pageNumberFromLocation(loc);

                hyper.pageNum = page;
                if (page >= 0) {
                    hyper.bbox = new android.graphics.Rect((int) loc.x, (int) loc.y, (int) loc.x, (int) loc.y);
                    hyper.url = null;
                } else {
                    hyper.bbox = null;
                    hyper.url = link.uri;
                }

                return hyper;
            }
        }

        return null;
    }

    public MuPDFWidget[] findFormFields()
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage==null)
            return null;

        //  do we have widgets?
        if (mWidgets == null)
            return null;

        //  screen out those that are not currently editable
        ArrayList<MuPDFWidget> mwidgets = new ArrayList<>();
        for (int i = 0; i < mWidgets.size(); i++)
        {
            int flags = mWidgets.get(i).getFlags();
            int fflags = mWidgets.get(i).getFieldFlags();
            int ftype = mWidgets.get(i).getFieldType();

            if ((flags & PDFWidget.IS_INVISIBLE) !=0)
                continue;
            if ((flags & PDFWidget.IS_HIDDEN) !=0)
                continue;
            if ((flags & PDFWidget.IS_NO_VIEW) !=0)
                continue;
            if ((flags & PDFWidget.IS_READ_ONLY) !=0)
                continue;
            if (    ((fflags & PDFWidget.PDF_FIELD_IS_READ_ONLY) !=0)
                &&  !((ftype == PDFWidget.TYPE_SIGNATURE) && (mWidgets.get(i).isSigned())) )
                continue;

            mwidgets.add(new MuPDFWidget(mWidgets.get(i)));
        }

        return mwidgets.toArray(new MuPDFWidget[mwidgets.size()]);
    }

    public MuPDFWidget selectWidgetAt(int pageX, int pageY)
    {
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage != null)
        {
            PDFWidget widget = pdfPage.activateWidgetAt(pageX, pageY);
            if (widget != null)
                return new MuPDFWidget(widget);
        }

        return null;
    }

    public static PDFPage getPDFPage(Page page)
    {
        try {
            PDFPage pdfPage = (PDFPage)page;
            return pdfPage;
        }
        catch (Exception e)
        {
        }

        return null;
    }

    //  cuont annotations of all types
    public int countAnnotations()
    {
        if (mAnnotations !=null)
            return mAnnotations.size();
        return 0;
    }

    //  count annotations of the given type
    public int countAnnotations(int type)
    {
        int count = 0;
        PDFPage pdfPage = getPDFPage(mPage);
        if (pdfPage != null)
        {
            if (mAnnotations != null)
            {
                for (PDFAnnotation annot : mAnnotations)
                {
                    if (annot.getType() == type)
                        count++;
                }
            }
        }
        return count;
    }
}
