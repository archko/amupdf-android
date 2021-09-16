package com.artifex.solib;

import android.graphics.Rect;

import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PKCS7Signer;
import com.artifex.mupdf.fitz.PKCS7Verifier;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.PDFWidget;


public class MuPDFWidget
{
    //  these must match mupdf
    public static final int TYPE_NOT_WIDGET    = PDFWidget.TYPE_UNKNOWN;
    public static final int TYPE_PUSHBUTTON    = PDFWidget.TYPE_BUTTON;
    public static final int TYPE_CHECKBOX      = PDFWidget.TYPE_CHECKBOX;
    public static final int TYPE_RADIOBUTTON   = PDFWidget.TYPE_RADIOBUTTON;
    public static final int TYPE_TEXT          = PDFWidget.TYPE_TEXT;
    public static final int TYPE_LISTBOX       = PDFWidget.TYPE_LISTBOX;
    public static final int TYPE_COMBOBOX      = PDFWidget.TYPE_COMBOBOX;
    public static final int TYPE_SIGNATURE     = PDFWidget.TYPE_SIGNATURE;

    //  these must match mupdf
    public static final int CONTENT_UNRESTRAINED = 0;   //  TX_FORMAT_NONE
    public static final int CONTENT_NUMBER = 1;         //  TX_FORMAT_NUMBER
    public static final int CONTENT_SPECIAL = 2;        //  TX_FORMAT_SPECIAL
    public static final int CONTENT_DATE = 3;           //  TX_FORMAT_DATE
    public static final int CONTENT_TIME = 4;           //  TX_FORMAT_TIME

    protected PDFWidget mWidget;
    protected long mTimeSigned = -1;

    //  if a widget is created during this session, it will be marked as such
    //  by calling setCreatedInThisSession().
    private boolean mCreatedInThisSession = false;
    public boolean getCreatedInThisSession() {return mCreatedInThisSession;}
    public void setCreatedInThisSession(boolean val) {mCreatedInThisSession=val;}

    MuPDFWidget(PDFWidget widget)
    {
        mWidget = widget;
    }

    public boolean setValue(String val)
    {
        //  we should allow setting a blank field as final
        if (!mWidget.isEditing() && val!=null && val.equals(""))
        {
            mWidget.setEditing(true);
            mWidget.setTextValue("");
            mWidget.setEditing(false);
            return true;
        }

        if (mWidget!=null)
        {
            boolean accepted;
            if (mWidget.getTextFormat() == CONTENT_NUMBER)
                accepted = mWidget.setValue(val);
            else
                accepted = mWidget.setTextValue(val);

            mWidget.update();
            return accepted;
        }

        return false;
    }

    public int getKind()
    {
        if (mWidget!=null)
            return mWidget.getFieldType();

        return TYPE_NOT_WIDGET;
    }

    public String getValue()
    {
        if (mWidget!=null)
            return mWidget.getValue();
        return null;
    }

    public Rect getBounds()
    {
        com.artifex.mupdf.fitz.Rect r = mWidget.getBounds();
        if (r !=null)
        {
            Rect r2 = new Rect((int)r.x0, (int)r.y0, (int)r.x1, (int)r.y1);
            return r2;
        }
        return null;
    }

    public void setBounds(Rect r)
    {
        mWidget.setRect(new com.artifex.mupdf.fitz.Rect(r.left, r.top, r.right, r.bottom));
        mWidget.update();
    }

    public String[] getOptions()
    {
        if (mWidget!=null)
            return mWidget.getOptions();
        return null;
    }

    public Rect[] textRects()
    {
        if (mWidget!=null)
        {
            Quad[] quads = mWidget.textQuads();
            Rect[] rects = new Rect[quads.length];
            for (int i=0; i<quads.length; i++)
            {
                com.artifex.mupdf.fitz.Rect r = quads[i].toRect();
                rects[i] = new Rect((int)r.x0, (int)r.y0, (int)r.x1, (int)r.y1);
            }
            return rects;
        }
        return null;
    }

    public int getMaxChars()
    {
        if (mWidget!=null)
            return mWidget.getMaxLen();

        return 0;
    }

    public int getTextFormat()
    {
        if (mWidget!=null)
            return mWidget.getTextFormat();

        return CONTENT_UNRESTRAINED;
    }

    public boolean isMultiline()
    {
        if (mWidget!=null)
        {
            int flags = mWidget.getFieldFlags();
            return (flags & PDFWidget.PDF_TX_FIELD_IS_MULTILINE) != 0;
        }
        return false;
    }

    public boolean equals (MuPDFWidget w)
    {
        if (w != null)
            return mWidget.equals(w.mWidget);
        return false;
    }

    public void setEditingState(boolean state)
    {
        if (mWidget!=null)
        {
            mWidget.setEditing(state);
        }
    }

    public boolean toggle()
    {
        if (mWidget!=null)
            return mWidget.toggle();
        return false;
    }

    public boolean isSigned()
    {
        if (mWidget!=null)
            return mWidget.isSigned();
        return false;
    }

    public int validate()
    {
        int result = 0;

        if (mWidget!=null)
            result = mWidget.validateSignature();

        return result;
    }

    public boolean sign(PKCS7Signer signer)
    {
        boolean result = false;

        if (mWidget!=null)
            result = mWidget.sign( signer );

        if (result)
            mTimeSigned = System.currentTimeMillis();

        return result;
    }

    public boolean verify(PKCS7Verifier verifier)
    {
        boolean result = false;

        if (mWidget!=null)
            result = mWidget.verify( verifier );
        return result;
    }

    public long getTimeSigned()
    {
        return mTimeSigned;
    }

    PDFAnnotation asAnnotation()
    {
        return (PDFAnnotation)mWidget;
    }

    public void focus()
    {
        //  give focus to the underlying PDFWidget
        if (mWidget!=null) {
            mWidget.eventFocus();
            mWidget.eventDown();
            mWidget.eventUp();
        }
    }
}
