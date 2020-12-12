package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFWidget;

public class PDFFormCheckboxEditor extends PDFFormEditor
{
    public PDFFormCheckboxEditor(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void start(DocMuPdfPageView pageView, final int pageNumber, final MuPDFDoc doc,
                      final DocView docView, final MuPDFWidget widget, final Rect bounds,
                      final PDFFormTextEditor.EditorListener editorListener)
    {
        //  start the base class
        super.start(pageView, pageNumber, doc, docView, widget, bounds, editorListener);

        //  set up an input filter that allows us to detect the user tapping
        //  Enter, in which case we stop, or Space, in which case we toggle.
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
            {
                if (source.toString().equals("\n"))
                {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            stop();
                            mEditorListener.onStopped();
                        }
                    });
                }
                else if (source.toString().equals(" "))
                {
                    toggle();
                }
                return null;
            }
        };
        mEditText.setFilters(new InputFilter[] { filter });
    }

    @Override
    public boolean stop()
    {
        if (mStopped)
            return true;

        super.stop();

        if (mEditText != null)
        {
            mEditText.setOnKeyListener(null);
            mEditText.setFilters(new InputFilter[] {});
        }

        return true;
    }

    @Override
    protected SOEditText getEditText()
    {
        return findViewById(R.id.pdf_checkbox_editor);
    }

    @Override
    protected void singleTap(float x, float y)
    {
        toggle();
    }

    @Override
    protected void doubleTap(float x, float y)
    {
    }

    protected void toggle()
    {
        mWidget.toggle();
        mDoc.update(mPageNumber);
    }

    private void drawBorder(Canvas canvas)
    {
        //  scaled stroke width
        double factor = mPageView.getFactor();
        int strokeW = (int)(Utilities.convertDpToPixel(1)*factor);
        if (strokeW<2)
            strokeW = 2;

        Paint paint = new Paint();
        paint.setColor(Color.RED);     //set a color
        paint.setStrokeWidth(strokeW);       // set your stroke width
        paint.setStyle(Paint.Style.STROKE);

        Rect outline = new Rect(0, 0, getWidth(), getHeight());
        outline.inset(-strokeW/2, -strokeW/2);
        outline.offset(3,1);  //  ???
        canvas.drawRect(outline, paint);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        if (mStopped)
            return;

        super.onDraw(canvas);

        if (mDocViewAtRest)
        {
            //  we only draw the border when the view is not moving.
            //  Otherwise it seems to swim around.
            drawBorder(canvas);
        }
    }

    @Override
    protected void setupInput()
    {
        mEditText.requestFocus();
    }
}
