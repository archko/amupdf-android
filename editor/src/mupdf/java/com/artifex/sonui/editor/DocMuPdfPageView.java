package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.artifex.mupdf.fitz.PKCS7Verifier;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFWidget;
import com.artifex.solib.ArDkDoc;

import com.artifex.solib.MuPDFPage;
import com.artifex.solib.Worker;

import java.util.HashMap;
import java.util.Map;

public class DocMuPdfPageView extends DocPdfPageView
{
    private final String mDebugTag = "DocPdfPageView";

    private android.graphics.Rect  mHighlightingRect = new android.graphics.Rect();
    private Paint mSelectionHighlightPainter=null;
    private Paint mFormFieldPainterPainter=null;

    //  list of form fields on the page
    private MuPDFWidget[] mFormFields;

    //  the bounds of each form field
    private Rect[] mFormFieldBounds;

    //  if a text field is being edited, this will be non-null
    private PDFFormEditor mFormEditor = null;
    //  the page view that holds the text field being edited
    public static DocMuPdfPageView mFormEditorPage = null;

    //  keep track of the currently-being-edited widget.
    private MuPDFWidget mEditingWidget = null;
    private int mEditingWidgetIndex = -1;

    public DocMuPdfPageView(Context context, ArDkDoc theDoc)
    {
        super(context, theDoc);

        mSelectionHighlightPainter = new Paint();
        mSelectionHighlightPainter.setColor(ContextCompat.getColor(context, R.color.sodk_editor_text_highlight_color));
        mSelectionHighlightPainter.setStyle(Paint.Style.FILL);
        mSelectionHighlightPainter.setAlpha(getContext().getResources().getInteger(R.integer.sodk_editor_text_highlight_alpha));

        mFormFieldPainterPainter = new Paint();
        mFormFieldPainterPainter.setColor(ContextCompat.getColor(context, R.color.sodk_editor_form_field_color));
        mFormFieldPainterPainter.setStyle(Paint.Style.FILL);
        mFormFieldPainterPainter.setAlpha(getContext().getResources().getInteger(R.integer.sodk_editor_form_field_alpha));
    }

    @Override
    protected void setOrigin()
    {
        //  get a copy of the pagerect
        Rect originRect = new Rect();
        originRect.set(mPageRect);

        //  adjust based on the location of our window
        View window = ((Activity)getContext()).getWindow().getDecorView();
        int windowLoc[] = new int[2];
        window.getLocationOnScreen(windowLoc);
        originRect.offset(-windowLoc[0], -windowLoc[1]);

        //  set the origin
        mRenderOrigin.set(originRect.left, originRect.top);
    }

    @Override
    protected void changePage(int thePageNum)
    {
        super.changePage(thePageNum);

        collectFormFields();
    }

    @Override
    public void update(RectF area)
    {
        //  we're being asked to update a portion of the page.

        //  not if we're finished.
        if (isFinished())
            return;

        //  not if we're not visible
        if (!isShown())
            return;

        //  right now updates are coming just from selection changes
        //  so this is all we need to do.
        //  later we'll need to re-render the area.
        invalidate();
    }

    @Override
    protected void drawSelection(Canvas canvas)
    {
        if (mPage==null)
            return;

        MuPDFPage page = (MuPDFPage) getPage();

        if (page == null)
            return; //  notional fix for 700568

        Rect r = page.getSelectedAnnotationRect();
        if (r != null)
        {
            //  draw the rect
            RectF rf = new RectF(r.left, r.top, r.right, r.bottom);
            android.graphics.Rect bounds = pageToView(rf);
            mHighlightingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
            canvas.drawRect(mHighlightingRect, mSelectionHighlightPainter);
        }

        Rect[] selectionRects = page.getSelectionRects();
        if (selectionRects!=null && selectionRects.length>0)
        {
            for (Rect sr : selectionRects)
            {
                mHighlightingRect.set(sr);
                pageToView(mHighlightingRect, mHighlightingRect);
                canvas.drawRect(mHighlightingRect, mSelectionHighlightPainter);
            }
        }

        ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();
        if (docCfgOpts!=null && docCfgOpts.isFormFillingEnabled())
        {
            if (!mFullscreen)
            {
                //  highlight the form fields, except for the one
                //  we're currently editing
                if (mFormFields !=null && mFormFields.length>0)
                {
                    for (int i = 0; i< mFormFields.length; i++)
                    {
                        if (!mFormFields[i].equals(mEditingWidget))
                        {
                            Rect wr = mFormFieldBounds[i];
                            mHighlightingRect.set(wr);
                            pageToView(mHighlightingRect, mHighlightingRect);
                            canvas.drawRect(mHighlightingRect, mFormFieldPainterPainter);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void drawSearchHighlight(Canvas canvas)
    {
        MuPDFPage page = (MuPDFPage) getPage();

        if (page == null)
            return; //  notional fix for 700568

        Rect r = page.getSearchHighlight();

        if (r != null)
        {
            //  draw the rect
            RectF rf = new RectF(r.left, r.top, r.right, r.bottom);
            android.graphics.Rect bounds = pageToView(rf);
            mHighlightingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
            canvas.drawRect(mHighlightingRect, mSelectionHighlightPainter);
        }
    }

    public void onReloadFile()
    {
        MuPDFDoc doc = ((MuPDFDoc)getDoc());

        //  after a reload, we refresh our page from the document
        dropPage();
        changePage(getPageNumber());

        //  because we have a new page, the current state of editing form fields
        //  is invalid, so we have to start over.
        ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();
        if (docCfgOpts.isFormFillingEnabled()) {
            stopPreviousEditor();
            collectFormFields();
            invalidate();
        }

        doc.update(getPageNumber());
    }

    public void collectFormFields()
    {
        final MuPDFPage page = (MuPDFPage) getPage();
        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        if (page==null || doc==null)
            return;

        //  collect up the form fields and their bounds.
        mFormFields = page.findFormFields();
        if (mFormFields !=null && mFormFields.length>0)
        {
            mFormFieldBounds = new Rect[mFormFields.length];
            int i=0;
            for (MuPDFWidget mw : mFormFields)
            {
                mFormFieldBounds[i] = mw.getBounds();
                i++;
            }
        }
    }

    public MuPDFWidget getNewestWidget()
    {
        if (mFormFields==null)
            return null;
        if (mFormFields.length==0)
            return null;

        //  we assume that the newest widget is
        //  last on the list.
        return mFormFields[mFormFields.length-1];
    }

    private MuPDFWidget findTappedWidget(int x, int y)
    {
        mEditingWidget = null;
        mEditingWidgetIndex = -1;
        if (mFormFields !=null && mFormFields.length>0)
        {
            for (int i=0; i<mFormFields.length ;i++)
            {
                MuPDFWidget mw = mFormFields[i];
                if (mFormFieldBounds[i].contains(x, y))
                {
                    if (mw.getKind()!=MuPDFWidget.TYPE_PUSHBUTTON)
                    {
                        mEditingWidget = mw;
                        mEditingWidgetIndex = i;
                    }
                    return mw;
                }
            }
        }
        return null;
    }

    protected boolean stopPreviousEditor()
    {
        //  mFormEditor might be on another page view.
        //  so we reference it through that page view.

        if (mFormEditorPage !=null && mFormEditorPage.mFormEditor != null)
        {
            boolean stopped = mFormEditorPage.mFormEditor.stop();
            if (stopped)
            {
                mFormEditorPage.mFormEditor = null;
                mFormEditorPage.mEditingWidgetIndex = -1;
                mFormEditorPage.mEditingWidget = null;
                mFormEditorPage.invalidate();
                mFormEditorPage = null;
            }
            return stopped;
        }
        return true;
    }

    @Override
    protected boolean canDoubleTap(int x, int y)
    {
        //  in PDF documents, a single-tap will select a marked redaction.
        //  We want to prevent a double-tap from also selecting the inderlying text.
        //  so prevent double-=tapping of marked redactions.

        MuPDFPage pdfPage = ((MuPDFPage)mPage);
        Point pPage = screenToPage(new Point(x,y));
        int index = pdfPage.findSelectableAnnotAtPoint(pPage, MuPDFPage.PDF_ANNOT_REDACT);
        if (index != -1)
            return false;

        return true;
    }

    @Override
    public boolean onSingleTap(int x, int y, boolean canEditText, ExternalLinkListener listener)
    {
        ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();

        //  assume nothing selected
        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        doc.setSelectedAnnotation(-1, -1);

        //  NOTE: when double-tapping, a single-tap will also happen first.
        //  so that must be safe to do.

        Point pPage = screenToPage(x, y);

        boolean stopped = true;
        if (docCfgOpts.isFormFillingEnabled() && docCfgOpts.isEditingEnabled()) {
            //  stop previous editor
            stopped = stopPreviousEditor();
            invalidate();
        }

        //  is it a hyperlink?
        if (tryHyperlink(pPage, listener))
            return true;

        //  is it an annotation?
        if (selectAnnotation(x, y))
            return true;

        //  is it a widget?
        if (docCfgOpts.isFormFillingEnabled() && docCfgOpts.isEditingEnabled() && stopped)
        {
            MuPDFWidget widget = findTappedWidget(pPage.x, pPage.y);
            if (widget != null)
            {
                MuPDFDoc mupdfDoc = ((MuPDFDoc)getDoc());
                if (mupdfDoc.getShowXFAWarning())
                {
                    //  only show the warning once
                    mupdfDoc.setShowXFAWarning(false);

                    //  show it
                    String messageFormat = getContext().getString(R.string.sodk_editor_xfa_warning_body);
                    String appName = Utilities.getApplicationName(getContext());
                    String message = String.format(messageFormat, appName);
                    Utilities.showMessage((Activity)getContext(),
                            getContext().getString(R.string.sodk_editor_xfa_warning_title),
                            message);

                    //  un-edit the widget
                    mEditingWidget = null;
                    mEditingWidgetIndex = -1;
                }
                else
                {
                    //  edit the widget
                    editWidget(widget, true, pPage);
                }
                return true;
            }
            else {
                Utilities.hideKeyboard(getContext());
            }
        }

        //  Notify ignored interaction if user tapped on a widget
        //  but only if form filling is currently disabled
        //  bug https://bugs.ghostscript.com/show_bug.cgi?id=702732
        if (!docCfgOpts.isFormFillingEnabled()) {
            if (findTappedWidget(pPage.x, pPage.y) != null) {
                if (docCfgOpts != null && docCfgOpts.featureTracker != null) {
                    docCfgOpts.featureTracker.hasTappedDisabledFeature(pPage);
                }
            }
        }

        return false;
    }

    public boolean selectAnnotation(int x, int y)
    {
        ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();

        Point pPage = screenToPage(x, y);

        if (docCfgOpts.isEditingEnabled())
        {
            MuPDFPage pdfPage = ((MuPDFPage)mPage);
            int index=-1;

            boolean redactMode = NUIDocView.currentNUIDocView().isRedactionMode();
            if (redactMode && docCfgOpts.isRedactionsEnabled())
            {
                //  in redaction mode we only consider selecting redaction marks
                index = pdfPage.findSelectableAnnotAtPoint(pPage, MuPDFPage.PDF_ANNOT_REDACT);
            }
            else
            {
                //  in other modes we consider everything *but* redaction marks
                //  the logic for giving text annotations priority has been moved here
                //  from MuPDFPage, which should still address bug #699865.
                index = pdfPage.findSelectableAnnotAtPoint(pPage, MuPDFPage.PDF_ANNOT_TEXT);
                if (index<0)
                    index = pdfPage.findSelectableAnnotAtPoint(pPage, MuPDFPage.PDF_ANNOT_HIGHLIGHT);
                if (index<0)
                    index = pdfPage.findSelectableAnnotAtPoint(pPage, -1);  //  anything else
            }

            if (index>=0)
            {
                pdfPage.selectAnnot(index);
                return true;
            }
        }

        return false;
    }

    private void editWidget(MuPDFWidget widget, boolean tapped, Point p)
    {
        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        boolean stopped = true;

        int kind = widget.getKind();
        switch (kind)
        {
            case MuPDFWidget.TYPE_TEXT:
                editWidgetAsText(widget);
                break;

            case MuPDFWidget.TYPE_COMBOBOX:
            case MuPDFWidget.TYPE_LISTBOX:
                editWidgetAsCombo(widget);
                break;

            case MuPDFWidget.TYPE_RADIOBUTTON:
            case MuPDFWidget.TYPE_CHECKBOX:
                editWidgetAsCheckbox(widget, tapped);
                break;

            case MuPDFWidget.TYPE_SIGNATURE:
                handleSignatureWidget(widget, tapped);
                break;

            case MuPDFWidget.TYPE_PUSHBUTTON:
                stopped = stopPreviousEditor();
                if (stopped)
                {
                    if (tapped && p!=null)
                        ((MuPDFPage)mPage).selectWidgetAt(p.x, p.y);
                }
                break;

            default:
                //  something unsupported
                stopPreviousEditor();
                Log.i(mDebugTag, "editWidget() unsupported widget type: " + kind);
                break;
        }

        doc.update(getPageNumber());
    }

    private void nextWidget()
    {
        if (mEditingWidgetIndex >=0)
        {
            int start = mEditingWidgetIndex;
            while(true)
            {
                mEditingWidgetIndex++;
                if (mEditingWidgetIndex >= mFormFields.length)
                    mEditingWidgetIndex = 0;

                if (mEditingWidgetIndex==start)
                    return;

                //  advance past push buttons; editing them makes no sense.
                if (mFormFields[mEditingWidgetIndex].getKind()==MuPDFWidget.TYPE_PUSHBUTTON)
                    continue;

                break;
            }

            mEditingWidget = mFormFields[mEditingWidgetIndex];

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    editWidget(mEditingWidget, false, null);
                }
            });
        }
    }

    @Override
    public void endRenderPass()
    {
        super.endRenderPass();

        //  if there's a live form text editor, tell it we're done rendering.
        if (mFormEditor !=null)
            mFormEditor.onRenderComplete();
    }

    private void editWidgetAsCombo(final MuPDFWidget widget)
    {
        final String[] options = widget.getOptions();
        String currentVal = widget.getValue();
        if (options!=null && options.length>0)
        {
            ListWheelDialog.show(getContext(), options, currentVal, new ListWheelDialog.ListWheelDialogListener() {
                @Override
                public void update(String val) {
                    widget.setValue(val);
                    ((MuPDFDoc)getDoc()).update(getPageNumber());
                    invalidate();
                    if (!ListWheelDialog.wasDismissedWithButton())
                    {
                        boolean stopped = stopPreviousEditor();
                        if (stopped)
                            nextWidget();
                    }
                }

                @Override
                public void cancel() {
                    invalidate();
                    if (!ListWheelDialog.wasDismissedWithButton())
                    {
                        boolean stopped = stopPreviousEditor();
                        if (stopped)
                            nextWidget();
                    }
                }
            });
            if (isKeyboardvisible())
                scrollCurrentWidgetIntoView();
            invalidate();
        }
    }

    private void scrollCurrentWidgetIntoView()
    {
        //  ask the current form editor to scroll iteself into view
        if (mFormEditor != null) {
            mFormEditor.scrollIntoView();
        }
    }

    private boolean isKeyboardvisible()
    {
        NUIDocView ndv = NUIDocView.currentNUIDocView();
        return ndv.isKeyboardVisible();
    }

    private void handleSignatureWidget(final MuPDFWidget widget, boolean tapped)
    {
        //  stop previous editor
        boolean stopped = stopPreviousEditor();
        if (!stopped)
            return;

        if (tapped && getDocView().getDocConfigOptions().isFormSigningFeatureEnabled())
        {
            if (!widget.isSigned()) {

                //  the widget's not yet been signed, so

                final DocPdfPageView pageView = this;

                ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.sodk_editor_alert_dialog_style);
                final AlertDialog alert = new AlertDialog.Builder(ctw).create();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View view = inflater.inflate(R.layout.sodk_editor_signature_dialog, null);
                alert.setView(view);

                view.findViewById(R.id.sign_button).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alert.dismiss();
                        doSign(widget);
                    }
                });

                view.findViewById(R.id.reposition_button).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alert.dismiss();
                        DocPdfView dpv = (DocPdfView)getDocView();
                        dpv.doReposition(pageView, widget);
                    }
                });

                view.findViewById(R.id.delete_button).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alert.dismiss();
                        DocPdfView dpv = (DocPdfView)getDocView();
                        dpv.setDeletingPage((DocPdfPageView)DocMuPdfPageView.this);
                        MuPDFDoc doc = (MuPDFDoc) getDoc();
                        doc.deleteWidget(getPageNumber(), widget);
                    }
                });

                view.findViewById(R.id.cancel_button).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                });

                alert.show();

            } else {
                // create a verifier
                final NUIPKCS7Verifier verifier = Utilities.getVerifier((Activity)getContext());
                final MuPDFDoc         doc      = (MuPDFDoc)getDoc();

                if (verifier != null)
                {
                    NUIPKCS7Verifier.NUIPKCS7VerifierListener listener;

                    listener = new NUIPKCS7Verifier.NUIPKCS7VerifierListener() {
                        @Override
                        public void onInitComplete() {
                            /*
                             * Execute the verification via the document
                             * worker thread to ensure data integrity.
                             */
                            Worker worker = doc.getWorker();

                            worker.add(new Worker.Task()
                            {
                                public void work()
                                {
                                    boolean result = widget.verify( verifier );

                                    if (!result)
                                    {
                                        Map<String,String> dn = new HashMap<String, String>();
                                        onVerifyResult(dn, PKCS7Verifier.PKCS7VerifierUnknown);
                                    }
                                }

                                public void run()
                                {
                                }
                            });

                        }
                        @Override
                        public void onVerifyResult(Map<String, String> designatedName, int result) {
                        }
                    };

                    // check if the document has been saved since the widget was signed
                    long lastSaveTime = doc.getLastSaveTime();
                    if(widget.getTimeSigned() > lastSaveTime)
                    {
                        Toast.makeText( getContext(), R.string.sodk_editor_unsaved_signatures, Toast.LENGTH_LONG ).show();
                    }
                    else
                    {
                        int signatureValidity = widget.validate();
                        verifier.doVerify( listener, signatureValidity );
                    }
                }
            }
        }

        scrollCurrentWidgetIntoView();
        invalidate();
    }

    private void doSign(final MuPDFWidget widget)
    {
        // create a signer
        final NUIPKCS7Signer signer = Utilities.getSigner((Activity)getContext());
        if (signer != null)
            signer.doSign(new NUIPKCS7Signer.NUIPKCS7SignerListener(){
                @Override
                public void onSignatureReady() {

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MuPDFDoc doc = (MuPDFDoc)getDoc();

                            doc.setForceReload( true );
                            if (mPage != null)
                            {
                                // now initiate a save as so the user saves the signed doc
                                NUIDocView ndv = NUIDocView.currentNUIDocView();
                                if (ndv != null)
                                    ndv.doSaveAs( false,
                                            new SOSaveAsComplete()
                                            {
                                                @Override
                                                public boolean onFilenameSelected( String path )
                                                {
                                                    return widget.sign(signer);
                                                }

                                                @Override
                                                public void onComplete(int result, String path)
                                                {
                                                }
                                            }
                                    );
                            }
                        }
                    });
                }

                @Override
                public void onCancel() {

                }
            });
    }

    private void editWidgetAsCheckbox(final MuPDFWidget widget, boolean tapped)
    {
        //  stop previous editor
        boolean stopped = stopPreviousEditor();
        if (!stopped)
            return;

        //  starting an editor may raise the keyboard, so let's
        //  find out and scroll the widget into view.
        final DocView docView = getDocView();
        docView.setShowKeyboardListener(new DocView.ShowKeyboardListener() {
            @Override
            public void onShow(boolean bShow) {
                if (bShow)
                    scrollCurrentWidgetIntoView();
            }
        });

        mFormEditorPage = this;
        mFormEditor = ((Activity)getContext()).findViewById(R.id.pdf_form_checkbox_editor_layout);
        mFormEditor.start(this, getPageNumber(), ((MuPDFDoc)getDoc()),
                docView, widget, mFormFieldBounds[mEditingWidgetIndex],
                new PDFFormEditor.EditorListener() {
            @Override
            public void onStopped() {
                docView.setShowKeyboardListener(null);
                boolean stopped = stopPreviousEditor();
                if (stopped)
                    nextWidget();
            }
        });
        if (tapped)
            ((PDFFormCheckboxEditor) mFormEditor).toggle();
        scrollCurrentWidgetIntoView();
        invalidate();
    }

    private void editWidgetAsText(final MuPDFWidget widget)
    {
        //  stop previous editor
        boolean stopped = stopPreviousEditor();
        if (!stopped)
            return;

        //  starting an editor may raise the keyboard, so let's
        //  find out and scroll the widget into view.
        final DocView docView = getDocView();
        docView.setShowKeyboardListener(new DocView.ShowKeyboardListener() {
            @Override
            public void onShow(boolean bShow) {
                if (bShow)
                    scrollCurrentWidgetIntoView();
            }
        });

        mFormEditorPage = this;
        mFormEditor = ((Activity)getContext()).findViewById(R.id.pdf_form_text_editor_layout);
        mFormEditor.start(this, getPageNumber(), ((MuPDFDoc)getDoc()),
                docView, widget, mFormFieldBounds[mEditingWidgetIndex],
                new PDFFormEditor.EditorListener() {
            @Override
            public void onStopped() {
                docView.setShowKeyboardListener(null);
                boolean stopped = stopPreviousEditor();
                if (stopped)
                    nextWidget();
            }
        });
        if (isKeyboardvisible())
            scrollCurrentWidgetIntoView();
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        //  see if the active editor wants to handle this event
        if (mFormEditor !=null) {
            if (mFormEditor.dispatchTouchEvent(event)) {
                return true;
            }
        }

        stopPreviousEditor();
        mEditingWidget = null;
        mEditingWidgetIndex = -1;

        return super.dispatchTouchEvent(event);
    }

    protected boolean mFullscreen = false;

    @Override
    public void onFullscreen(boolean bFull)
    {
        mFullscreen = bFull;

        if (mFullscreen)
        {
            //  get out of editing a form
            boolean stopped = true;
            if (getDocView()!=null)
            {
                ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();
                if (docCfgOpts.isFormFillingEnabled() && docCfgOpts.isEditingEnabled()) {
                    //  stop previous editor
                    stopped = stopPreviousEditor();
                    invalidate();
                }
            }
        }
    }

    public int addRedactAnnotation(Rect r)
    {
        //  how many redactions did we start with?
        MuPDFPage pdfPage = ((MuPDFPage)mPage);
        int count = pdfPage.countAnnotations();

        //  add a new one
        Point p1 = screenToPage(r.left, r.top);
        Point p2 = screenToPage(r.right, r.bottom);
        Rect r2 = new Rect(p1.x, p1.y, p2.x, p2.y);
        ((MuPDFDoc)getDoc()).addRedactAnnotation(getPageNumber(), r2);

        //  assume that the new one is last in the list
        return count;
    }
}
