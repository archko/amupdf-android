package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ArDkPage;
import com.artifex.solib.MuPDFWidget;
import com.artifex.solib.SODoc;
import com.artifex.solib.SOPoint;

import java.util.ArrayList;
import java.util.Iterator;

public class DocPdfPageView extends DocPageView
{
    private DisplayMetrics metrics;

    public DocPdfPageView(Context context, ArDkDoc theDoc)
    {
        super(context, theDoc);

        metrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
    }

    private int getMinInkSize()
    {
        //   calculate an appropriate minimum ink annotation size - 1/10 inch
        float pixels = ((float)metrics.densityDpi)*0.10f;
        return viewToPage((int)pixels);
    }

    private ArrayList<InkAnnotation> mInkAnnots;

    public void startDrawInk(float x, float y, int color, float thickness)
    {
        //  create annotation list
        if (mInkAnnots == null)
            mInkAnnots = new ArrayList<>();

        //  add a new annotation to the list
        InkAnnotation annot = new InkAnnotation(color, thickness);
        mInkAnnots.add(annot);

        //  add first point to the new annot, in page coords
        PointF pScreen = new PointF(x, y);
        PointF pPage = screenToPage(pScreen);
        annot.add(pPage);

        invalidate();
    }

    public void continueDrawInk(float x, float y)
    {
        if (mInkAnnots!=null && mInkAnnots.size()>0)
        {
            //  get the most recent annotation
            InkAnnotation annot = mInkAnnots.get(mInkAnnots.size()-1);

            //  add the point, in page coords
            PointF pScreen = new PointF(x, y);
            PointF pPage = screenToPage(pScreen);
            annot.add(pPage);

            invalidate();
        }
    }

    public void endDrawInk()
    {
        if (mInkAnnots!=null && mInkAnnots.size()>0)
        {
            //  get the most recent annotation
            InkAnnotation annot = mInkAnnots.get(mInkAnnots.size()-1);

            //  delete it if it's too small
            int minInkSize = getMinInkSize();
            Rect r = annot.getRect();
            if (r.width()<minInkSize && r.height()<minInkSize)
            {
                mInkAnnots.remove(mInkAnnots.size()-1);
                invalidate();
            }
        }
    }

    //   clear any not-yet-saved ink annotations
    public void clearInk()
    {
        if (mInkAnnots != null)
        {
            invalidate();
            mInkAnnots.clear();
        }
    }

    public void saveInk()
    {
        //  save the current ink annotations to the document
        if (mInkAnnots != null)
        {
            // TODO: this saves each stroke as a separate annotation; this may not be what is
            //       desired. If not, amalgamate all the SOPoints from all of the annotations
            //       into a single points array and create an annotation with the set of strokes.
            Iterator<InkAnnotation> it = mInkAnnots.iterator();
            while (it.hasNext())
            {
                InkAnnotation annot = it.next();
                getDoc().createInkAnnotation(getPageNumber(), annot.points(), annot.getLineThickness(), annot.getLineColor());
            }
        }

        //  now get rid of them
        if (mInkAnnots != null)
            mInkAnnots.clear();

        invalidate();
    }

    public void setInkLineColor(int val)
    {
        if (mInkAnnots != null)
        {
            Iterator<InkAnnotation> it = mInkAnnots.iterator();
            while (it.hasNext())
            {
                InkAnnotation annot = it.next();
                annot.setLineColor(val);
            }
            invalidate();
        }
    }

    public void setInkLineThickness(float val)
    {
        if (mInkAnnots != null)
        {
            Iterator<InkAnnotation> it = mInkAnnots.iterator();
            while (it.hasNext())
            {
                InkAnnotation annot = it.next();
                annot.setLineThickness(val);
            }
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        //  draw the base page
        super.onDraw(canvas);

        //  if we're not valid, do no more.
        if (!isValid())
            return;

        if (mPage==null)
            return;

        //  draw ink annotations
        drawInk(canvas);

        //  draw selection
        drawSelection(canvas);

        //  draw search highlight
        drawSearchHighlight(canvas);
    }

    private void drawInk(Canvas canvas)
    {
        //  draw ink annotations
        if (mInkAnnots != null)
        {
            int minInkSize = getMinInkSize();
            Iterator<InkAnnotation> it = mInkAnnots.iterator();
            while (it.hasNext())
            {
                InkAnnotation annot = it.next();
                Rect r = annot.getRect();
                if (r.width()>=minInkSize || r.height()>=minInkSize)
                    annot.draw(canvas);
            }
        }
    }

    protected void drawSelection(Canvas canvas)
    {
    }

    protected void drawSearchHighlight(Canvas canvas)
    {
    }

    public void createNote(float x, float y)
    {
        //  where, in page coordinates
        PointF pScreen = new PointF(x, y);
        PointF pPage = screenToPage(pScreen);

        //  create it
        getDoc().createTextAnnotationAt(pPage, getPageNumber());

        invalidate();
    }

    public void createSignatureAt(float x, float y)
    {
        //  where, in page coordinates
        PointF pScreen = new PointF(x, y);
        PointF pPage = screenToPage(pScreen);

        //  create it
        getDoc().createSignatureAt(pPage, getPageNumber());
    }

    public void collectFormFields()
    {
    }

    public MuPDFWidget getNewestWidget()
    {
        return null;
    }

    //-----------------------------------------------------

    public class InkAnnotation
    {
        private float mLineThickness;
        public float getLineThickness() {return mLineThickness;}
        public void setLineThickness(float val) {mLineThickness=val;}

        private int mLineColor;
        public int getLineColor() {return mLineColor;}
        public void setLineColor(int val) {mLineColor=val;}

        private ArrayList<PointF> mArc;
        public SOPoint[] points() {return mArc.toArray(new SOPoint[0]);}

        public InkAnnotation(int lineColor, float lineThickness)
        {
            mLineColor = lineColor;
            mLineThickness = lineThickness;
            mArc = new ArrayList<>();
        }

        public void add(PointF p)
        {
            int type = SOPoint.LineTo;

            if(mArc.size() == 0)
                type = SOPoint.MoveTo;

            SOPoint pt = new SOPoint(p, type);
            mArc.add(pt);
        }

        public Rect getRect()
        {
            //  make a Rect that contains all the points in the arc.
            Rect r = null;
            Iterator<PointF> iit = mArc.iterator();
            int i = 0;
            while (iit.hasNext()) {
                PointF pPage = iit.next();
                i++;
                if (i==1)
                    r = new Rect((int)pPage.x,(int)pPage.y,(int)pPage.x,(int)pPage.y);
                else
                    r.union((int)pPage.x,(int)pPage.y);
            }
            return r;
        }

        public void draw(Canvas canvas)
        {
            Path path = new Path();
            PointF pPage;
            PointF pView = new PointF();

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(mLineThickness * (float)getFactor());
            paint.setColor(mLineColor);

            if (mArc.size() >= 2)
            {
                Iterator<PointF> iit = mArc.iterator();
                pPage = iit.next();
                pageToView(pPage, pView);
                float mX = pView.x;
                float mY = pView.y;
                path.moveTo(mX, mY);
                while (iit.hasNext())
                {
                    pPage = iit.next();
                    pageToView(pPage, pView);
                    float x = pView.x;
                    float y = pView.y;
                    path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                    mX = x;
                    mY = y;
                }
                path.lineTo(mX, mY);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(path, paint);
            }
            else
            {
                pPage = mArc.get(0);
                pageToView(pPage, pView);
                canvas.drawCircle(pView.x, pView.y, mLineThickness * mScale / 2, paint);
            }
        }
    }

    @Override
    protected void launchHyperLink(final String url)
    {
        //  ask first

        final DocPdfPageView dppv = this;
        final Context context = getContext();
        Utilities.yesNoMessage((Activity)context,
                context.getString(com.artifex.sonui.editor.R.string.sodk_editor_open_link),
                "\n"+url+"\n",
                context.getString(com.artifex.sonui.editor.R.string.sodk_editor_OK),
                context.getString(com.artifex.sonui.editor.R.string.sodk_editor_cancel),
                new Runnable() {
                    @Override
                    public void run() {
                        //  OK, so do it
                        DocPdfPageView.super.launchHyperLink(url);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        //  cancelled
                    }
                });

    }

    @Override
    public void onDoubleTap(int x, int y)
    {
        Point p = screenToPage(x, y);
        mPage.select(ArDkPage.SOSelectMode_DefaultUnit, p.x, p.y);
        NUIDocView.currentNUIDocView().showUI(true);
    }
}
