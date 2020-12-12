package com.artifex.sonui.editor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkSelectionLimits;
import com.artifex.solib.FileUtils;
import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFPage;
import com.artifex.solib.SOLinkData;

public class NUIDocViewPdf extends NUIDocView
{
    private ToolbarButton mToggleAnnotButton;
    private ToolbarButton mHighlightButton;
    private ToolbarButton mDeleteButton;
    private ToolbarButton mNoteButton;
    private ToolbarButton mDrawButton;
    private ToolbarButton mLineColorButton;
    private ToolbarButton mLineThicknessButton;
    private ToolbarButton mAuthorButton;
    private ToolbarButton mSignatureButton;

    private ToolbarButton mRedactMarkButton;
    private ToolbarButton mRedactMarkAreaButton;
    private ToolbarButton mRedactRemoveButton;
    private ToolbarButton mRedactApplyButton;

    //  history items
    private ToolbarButton mPreviousLinkButton;
    private ToolbarButton mNextLinkButton;

    private Button mTocButton;
    private boolean tocEnabled = false;

    public NUIDocViewPdf(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocViewPdf(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocViewPdf(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    private void initialize(Context context)
    {
    }

    @Override
    protected int getLayoutId()
    {
        return R.layout.sodk_editor_pdf_document;
    }

    @Override
    protected DocView createMainView(Activity activity)
    {
        return new DocPdfView(activity);
    }

    @Override
    protected void createEditButtons() {}

    @Override
    protected void createEditButtons2() {}

    @Override
    protected void afterFirstLayoutComplete()
    {
        super.afterFirstLayoutComplete();

        if (mDocCfgOptions.isPDFAnnotationEnabled()){
            mToggleAnnotButton = (ToolbarButton) createToolbarButton(R.id.show_annot_button);
            mHighlightButton = (ToolbarButton) createToolbarButton(R.id.highlight_button);
            mNoteButton = (ToolbarButton) createToolbarButton(R.id.note_button);
            mAuthorButton = (ToolbarButton) createToolbarButton(R.id.author_button);
            mDrawButton = (ToolbarButton) createToolbarButton(R.id.draw_button);
            mLineColorButton = (ToolbarButton) createToolbarButton(R.id.line_color_button);
            mLineThicknessButton = (ToolbarButton) createToolbarButton(R.id.line_thickness_button);
            mDeleteButton = (ToolbarButton) createToolbarButton(R.id.delete_button);
            mSignatureButton = (ToolbarButton) createToolbarButton(R.id.signature_button);
        }

        mRedactMarkButton = (ToolbarButton) createToolbarButton(R.id.redact_button_mark);
        mRedactMarkAreaButton = (ToolbarButton) createToolbarButton(R.id.redact_button_mark_area);
        mRedactRemoveButton = (ToolbarButton) createToolbarButton(R.id.redact_button_remove);
        mRedactApplyButton = (ToolbarButton) createToolbarButton(R.id.redact_button_apply);

        if (mDocCfgOptions.featureTracker != null && !mDocCfgOptions.isRedactionsEnabled()) {
            mDocCfgOptions.featureTracker.hasLoadedDisabledFeature(mRedactMarkButton);
            mDocCfgOptions.featureTracker.hasLoadedDisabledFeature(mRedactMarkAreaButton);
            mDocCfgOptions.featureTracker.hasLoadedDisabledFeature(mRedactRemoveButton);
            mDocCfgOptions.featureTracker.hasLoadedDisabledFeature(mRedactApplyButton);
        }

        setupToc();

        hideUnusedButtons();

        //  set up history
        mPreviousLinkButton = (ToolbarButton) createToolbarButton(R.id.previous_link_button);
        mNextLinkButton = (ToolbarButton) createToolbarButton(R.id.next_link_button);
    }

    private void setupToc()
    {
        //  create the button
        mTocButton = (Button) createToolbarButton(R.id.toc_button);
        mTocButton.setEnabled(false);
        tocEnabled = false;
    }

    @Override
    protected void createReviewButtons()
    {
    }

    @Override
    protected void createInsertButtons()
    {
    }

    @Override
    protected PageAdapter createAdapter()
    {
        return new PageAdapter(activity(), this, PageAdapter.PDF_KIND);
    }

    @Override
    protected NUIDocView.TabData[] getTabData()
    {
        if (mTabs == null)
        {
            mTabs = new NUIDocView.TabData[4];

            int redactState = View.GONE;
            if (mDocCfgOptions.isRedactionsEnabled())
                redactState = View.VISIBLE;

            if (mDocCfgOptions.isEditingEnabled() &&  mDocCfgOptions.isPDFAnnotationEnabled())
            {
                mTabs[0] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_file),     R.id.fileTab,     R.layout.sodk_editor_tab_left, View.VISIBLE);
                mTabs[1] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_annotate), R.id.annotateTab, R.layout.sodk_editor_tab, View.VISIBLE);
                //  this tab is visible, but will be "blocked" by IAP until the Pro package is purchased.
                //  Clicking on the tab's buttons will redirect the user to upgrade.
                mTabs[2] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_redact), R.id.redactTab, R.layout.sodk_editor_tab, View.VISIBLE);
                mTabs[3] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_pages),    R.id.pagesTab,    R.layout.sodk_editor_tab_right, View.VISIBLE);
            }
            else
            {
                mTabs[0] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_file),     R.id.fileTab,     R.layout.sodk_editor_tab_left, View.VISIBLE);
                mTabs[1] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_annotate), R.id.annotateTab, R.layout.sodk_editor_tab, View.GONE);
                mTabs[2] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_redact), R.id.redactTab, R.layout.sodk_editor_tab, redactState);
                mTabs[3] = new NUIDocView.TabData(getContext().getString(R.string.sodk_editor_tab_pages),    R.id.pagesTab,    R.layout.sodk_editor_tab_right, View.VISIBLE);
            }
        }

        return mTabs;
    }

    @Override
    protected void setupTabs()
    {
        super.setupTabs();

        //  Notify blocked tabs
        //  note: look for the tab by it's name, not index.  It may not actually be in the tab map.

        if (mDocCfgOptions.featureTracker != null)
        {
            if (!mDocCfgOptions.isRedactionsEnabled())
            {
                View redactTab = tabMap.get(getContext().getString(R.string.sodk_editor_tab_redact));
                if (redactTab!=null)
                    mDocCfgOptions.featureTracker.hasLoadedDisabledFeature(redactTab);
            }
        }

        super.measureTabs();
    }

    @Override
    protected int getTabSelectedColor()
    {
        int colorFromDocType = getResources().getInteger(R.integer.sodk_editor_ui_doc_tab_color_from_doctype);
        int color = 0;

        if (colorFromDocType == 0)
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_color_selected);
        else
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_pdf_color);

        return color;
    }

    @Override
    protected int getTabUnselectedColor()
    {
        int colorFromDocType = getResources().getInteger(R.integer.sodk_editor_ui_doc_tabbar_color_from_doctype);
        int color = 0;

        if (colorFromDocType == 0)
            color = ContextCompat.getColor(activity(), R.color.sodk_editor_header_color);
        else
            color = Utilities.colorForDocExt(getContext(), getDocFileExtension());

        return color;
    }

    @Override
    protected void updateUIAppearance()
    {
        DocPdfView docView = getPdfDocView();

        //  file save
        updateSaveUIAppearance();

        //  undo
        updateUndoUIAppearance();

        boolean areaIsSelected = false;
        ArDkSelectionLimits limits = getDocView().getSelectionLimits();
        if (limits != null) {
            boolean active = limits.getIsActive();
            areaIsSelected = active && !limits.getIsCaret();
        }

        boolean selCanBeDeleted = getDoc().getSelectionCanBeDeleted();

        if (mDocCfgOptions.isPDFAnnotationEnabled()) {

            boolean isAltTextSel = getDoc().getSelectionIsAlterableTextSelection();
            mHighlightButton.setEnabled(isAltTextSel);

            boolean noteMode = docView.getNoteMode();
            mNoteButton.setSelected(noteMode);
            findViewById(R.id.note_holder).setSelected(noteMode);

            boolean signatureMode = docView.getSignatureMode();
            mSignatureButton.setSelected(signatureMode);
            findViewById(R.id.signature_holder).setSelected(signatureMode);

            boolean drawMode = docView.getDrawMode();

            //  always show the delete button in drawing mode (like iOS)
            boolean hasNotSavedInk = ((DocPdfView) getDocView()).hasNotSavedInk();
            mDeleteButton.setEnabled((drawMode && hasNotSavedInk) || selCanBeDeleted);

            //  no notes or highlights while in draw mode
            mNoteButton.setEnabled(!drawMode && !signatureMode);
            mAuthorButton.setEnabled(!drawMode && !signatureMode && !noteMode);
            mHighlightButton.setEnabled(!drawMode && !signatureMode && !noteMode);
            mSignatureButton.setEnabled(!drawMode && !noteMode);

            //  set the button's color to match the current drawing color
            int color = ((DocPdfView) getDocView()).getInkLineColor();
            mLineColorButton.setDrawableColor(color);

            findViewById(R.id.draw_tools_holder).setSelected(drawMode);
        }
        MuPDFDoc pdfDoc = (MuPDFDoc)getDoc();
        boolean markAreaMode = docView.getMarkAreaMode();
        mRedactMarkButton.setEnabled(!markAreaMode);
        mRedactMarkAreaButton.setSelected(markAreaMode);
        mRedactRemoveButton.setEnabled(!markAreaMode && selCanBeDeleted && pdfDoc.selectionIsRedaction());
        mRedactApplyButton.setEnabled(!markAreaMode && pdfDoc.hasRedactionsToApply());

        //  history buttons
        History history = docView.getHistory();
        mPreviousLinkButton.setEnabled(history.canPrevious());
        mNextLinkButton.setEnabled(history.canNext());

        getPdfDocView().onSelectionChanged();

        //  send update UI notification
        doUpdateCustomUI();
    }

    //  no undo/redo
    @Override
    protected boolean hasUndo() {return false;}
    @Override
    protected boolean hasRedo() {return false;}

    @Override
    protected boolean canSelect() {return false;}

    @Override
    protected void updateUndoUIAppearance()
    {
        //  for now, no undo/redo
        mUndoButton.setVisibility(View.GONE);
        mRedoButton.setVisibility(View.GONE);
    }

    @Override
    public void onReflowButton(View v)
    {
        //  TODO: use html to achieve reflow.
    }

    public DocPdfView getPdfDocView()
    {
        return (DocPdfView)getDocView();
    }

    public void onDrawButton(View v)
    {
        getPdfDocView().onDrawMode();
        updateUIAppearance();
    }

    public void onLineColorButton(View v)
    {
        final DocPdfView docView = getPdfDocView();
        if (docView.getDrawMode())
        {
            InkColorDialog dlg = new InkColorDialog(ColorDialog.FG_COLORS,
                    activity(), mLineColorButton, new InkColorDialog.ColorChangedListener()
            {
                @Override
                public void onColorChanged(String color)
                {
                    int icolor = Color.parseColor(color);
                    docView.setInkLineColor(icolor);
                    mLineColorButton.setDrawableColor(icolor);
                }
            }, true);
            dlg.setShowTitle(false);
            dlg.show();
        }
    }

    public void onLineThicknessButton(View v)
    {
        final DocPdfView docView = getPdfDocView();
        if (docView.getDrawMode())
        {
            float val = docView.getInkLineThickness();
            InkLineWidthDialog.show(activity(), mLineThicknessButton, val,
                    new InkLineWidthDialog.WidthChangedListener()
                    {
                        @Override
                        public void onWidthChanged(float value)
                        {
                            docView.setInkLineThickness(value);
                        }
                    });
        }
    }

    public void onNoteButton(View v)
    {
        getPdfDocView().onNoteMode();
        updateUIAppearance();
    }

    public void onSignatureButton(View v)
    {
        getPdfDocView().onSignatureMode();
        updateUIAppearance();
    }

    public void onToggleAnnotationsButton(View v)
    {
    }

    @Override
    protected void onTabChanging(String oldTabId, String newTabId)
    {
        //  save note annotation
        getPdfDocView().saveNoteData();

        //  clear selection if we're leaving the redact tab
        if (oldTabId.equals(getContext().getString(R.string.sodk_editor_tab_redact)))
            getDoc().clearSelection();

        //  clear selection if we're leaving the annotate tab
        if (oldTabId.equals(getContext().getString(R.string.sodk_editor_tab_annotate)))
        {
            //  cancel draw mode
            if (getPdfDocView().getDrawMode())
                getPdfDocView().onDrawMode();

            getDoc().clearSelection();
        }
    }

    @Override
    public void onTabChanged(String tabId)
    {
        super.onTabChanged(tabId);
    }

    @Override
    protected void goBack()
    {
        super.goBack();
    }

    @Override
    protected void onSearch()
    {
        super.onSearch();
    }

    private void onTocButton(View v )
    {
        TocDialog dialog = new TocDialog(getContext(), getDoc(), this,
                new TocDialog.TocDialogListener() {
                    @Override
                    public void onItem(SOLinkData linkData)
                    {
                        //  Add a history entry for the spot we're leaving.
                        DocView dv = getDocView();
                        dv.addHistory(dv.getScrollX(), dv.getScrollY(), dv.getScale(), true);

                        //  add history for where we're going
                        int dy = dv.scrollBoxToTopAmount(linkData.page, linkData.box);
                        dv.addHistory(dv.getScrollX(), dv.getScrollY()-dy, dv.getScale(), false);

                        //  scroll to the new page and box
                        dv.scrollBoxToTop(linkData.page, linkData.box);

                        updateUIAppearance();
                    }
                });
        dialog.show();
    }

    private void onHistoryItem(History.HistoryItem item)
    {
        DocView dv = getDocView();
        dv.onHistoryItem(item);
        updateUIAppearance();
    }

    private void onPreviousLinkButton(View v )
    {
        DocView dv = getDocView();
        History.HistoryItem item = dv.getHistory().previous();
        if (item!=null) {
            onHistoryItem(item);
        }
    }

    private void onNextLinkButton(View v )
    {
        DocView dv = getDocView();
        History.HistoryItem item = dv.getHistory().next();
        if (item!=null) {
            onHistoryItem(item);
        }
    }

    @Override
    public boolean hasHistory() {return true;}

    @Override
    protected void onDeviceSizeChange()
    {
        super.onDeviceSizeChange();

        TocDialog.onRotate();
    }

    public void onDeleteButton(View v)
    {
        DocPdfView docView = getPdfDocView();

        if (getDoc().getSelectionCanBeDeleted())
        {
            getDoc().selectionDelete();
            updateUIAppearance();
        }
        else if (docView.getDrawMode())
        {
            //  delete not-yet-saved ink annotations when in drawing mode (like iOS)
            docView.clearInk();
            updateUIAppearance();
        }
    }

    public void onHighlightButton(View v)
    {
        highlightSelection();
    }

    @Override
    public void onUndoButton(final View v) {
        super.onUndoButton(v);
    }

    @Override
    public void onRedoButton(final View v) {
        super.onRedoButton(v);
    }

    @Override
    public void onClick(View v)
    {
        // Ignore button presses while we are finishing up.
        if (mFinished)
        {
            return;
        }

        super.onClick(v);

        if (v == mToggleAnnotButton)
            onToggleAnnotationsButton(v);
        if (v == mHighlightButton)
            onHighlightButton(v);
        if (v == mDeleteButton)
            onDeleteButton(v);
        if (v == mNoteButton)
            onNoteButton(v);
        if (v == mSignatureButton)
            onSignatureButton(v);
        if (v == mAuthorButton)
            onAuthorButton(v);
        if (v == mDrawButton)
            onDrawButton(v);
        if (v == mLineColorButton)
            onLineColorButton(v);
        if (v == mLineThicknessButton)
            onLineThicknessButton(v);
        if (v==mTocButton)
            onTocButton(v);

        if (v==mRedactMarkButton)
            onRedactMark(v);
        if (v==mRedactMarkAreaButton)
            onRedactMarkArea(v);
        if (v==mRedactRemoveButton)
            onRedactRemove(v);
        if (v==mRedactApplyButton)
            onRedactApply(v);

        if (v==mPreviousLinkButton)
            onPreviousLinkButton(v);
        if (v==mNextLinkButton)
            onNextLinkButton(v);
    }

    public int getBorderColor()
    {
        return ContextCompat.getColor(getContext(), R.color.sodk_editor_header_pdf_color);
    }

    @Override
    protected void prepareToGoBack()
    {
        // if the document open failed, we'll have no doc.
        if (mSession!=null && mSession.getDoc()==null)
            return;

        if (getPdfDocView()!=null)
            getPdfDocView().resetModes();
    }

    private boolean firstSelectionCleared = false;

    @Override
    protected void onDocCompleted()
    {
        //  not if we're finished
        if (mFinished)
            return;

        //  address a bug where PDF files with an annotation open initially
        //  with the annotation selected.
        if (!firstSelectionCleared) {
            mSession.getDoc().clearSelection();
            firstSelectionCleared = true;
        }

        //  get the page count
        mPageCount = mSession.getDoc().getNumPages();

        if (mPageCount<=0) {
            //  no pages is an error
            String message = Utilities.getOpenErrorDescription(getContext(), 17);
            Utilities.showMessage((Activity)getContext(), getContext().getString(R.string.sodk_editor_error), message);
            disableUI();
            return;
        }

        //  we may be called when pages have been added or removed
        //  so update the page count and re-layout
        mAdapter.setCount(mPageCount);
        layoutNow();

        //  disable TOC button
        mTocButton.setEnabled(false);
        tocEnabled = false;
        setButtonColor(mTocButton,
            getResources().getInteger(R.color.sodk_editor_button_disabled));

        //  enumerate the TOC.  If there is at least one entry, enable the button
        ArDkLib.enumeratePdfToc(getDoc(), new ArDkLib.EnumeratePdfTocListener() {
            @Override
            public void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y)
            {
                //  enable the TOC button
                mTocButton.setEnabled(true);
                tocEnabled = true;
                setButtonColor(mTocButton,
                    getResources().getInteger(R.color.sodk_editor_header_button_enabled_tint));
            }
        });

        //  Each time we open a document, set the author for text annotations
        //  default is the application name.
        String author = mSession.getDoc().getAuthor();
        if (author==null)
        {
            String defaultAuthor = Utilities.getApplicationName(activity());
            Object store = Utilities.getPreferencesObject(activity(), Utilities.generalStore);
            author = Utilities.getStringPreference(store, "DocAuthKey", defaultAuthor);
            mSession.getDoc().setAuthor(author);
        }
    }

    @Override
    public void reloadFile()
    {
        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        String path;
        boolean forced = false;
        boolean forcedAtResume;

        forcedAtResume = doc.getForceReloadAtResume();
        forced = doc.getForceReload();
        doc.setForceReloadAtResume(false);
        doc.setForceReload(false);

        if (forcedAtResume)
        {
            //  we were set to force a reload on resume.
            //  this is the save-while-backgrounding case.
            //  see: pauseHandler in DataLeakHandlers
            //  for this case we use the internal path.
            path  = mSession.getFileState().getInternalPath();
        }
        else if (forced)
        {
            path  = mSession.getFileState().getUserPath();
            if (path == null)
                path  = mSession.getFileState().getOpenedPath();
        }
        else
        {
            path  = mSession.getFileState().getOpenedPath();

            //  check for incremental saving.
            //  we don't reload in that case.
            if ( doc.lastSaveWasIncremental() )
                return;

            //  check if the original file has been updated since it was loaded
            //  this will be true when the previous save operation replaced the original
            //  we don't reload unless this is true.
            long   loadTime = doc.getLoadTime();
            long   modTime  = FileUtils.fileLastModified( path );

            //  FileUtils.fileLastModified may return 0, so don't use it for comparison below
            if (modTime == 0)
                return;

            if ( modTime < loadTime )
                return;
        }

        // we're doing a save as - ensure we pick up the new document path and set the MuPDFDoc's
        // openedPath member accordingly
        if (!forcedAtResume)
            doc.setOpenedPath( path );

        final ProgressDialog spinner = Utilities.createAndShowWaitSpinner(getContext());

        //  reload the file and refresh page list
        doc.reloadFile(path, new MuPDFDoc.ReloadListener()
        {
            @Override
            public void onReload()
            {
                //  this part takes place on the main thread

                //  tell the views about it.
                //  they will in turn tell their page views about it.
                if (getDocView() != null)
                    getDocView().onReloadFile();
                if (usePagesView())
                    if (getDocListPagesView() != null)
                        getDocListPagesView().onReloadFile();

                spinner.dismiss();
            }
        }, forced || forcedAtResume);
    }

    @Override
    public InputView getInputView()
    {
        return null;
    }

    @Override
    protected boolean inputViewHasFocus()
    {
        return false;
    }

    protected void checkXFA()
    {
        if (mDocCfgOptions.isFormFillingEnabled())
        {
            //  to see if this is the first call
            boolean first = (mPageCount==0);
            if (first)
            {
                //  when the doc starts loading, see if it has only XFA forms.
                //  if so, display a message. But we still open the document.
                boolean hasXFA = getDoc().hasXFAForm();
                boolean hasAcro = getDoc().hasAcroForm();
                if (hasXFA && !hasAcro)
                {
                    Utilities.showMessage((Activity)getContext(),
                            getContext().getString(R.string.sodk_editor_xfa_title),
                            getContext().getString(R.string.sodk_editor_xfa_body));
                }

                //  if this is a dual form document, set up for showing the warning
                if (hasXFA && hasAcro)
                    ((MuPDFDoc)getDoc()).setShowXFAWarning(true);
            }
        }
    }

    protected void onPageLoaded(int pagesLoaded)
    {
        checkXFA();

        super.onPageLoaded(pagesLoaded);
    }

    @Override
    protected void layoutAfterPageLoad()
    {
    }

    @Override
    protected void createInputView()
    {
        //  PDF does not use InputView
    }

    @Override
    protected boolean shouldConfigureSaveAsPDFButton()
    {
        return false;
    }

    @Override
    protected boolean isRedactionMode()
    {
        String tab = getCurrentTab();
        if (tab!=null && tab.equals("REDACT"))
            return true;
        return false;
    }

    public void onRedactMark(View v)
    {
        int textSelPageNum = MuPDFPage.getTextSelPageNum();
        if (textSelPageNum==-1)
        {
            //  no text selected
            Utilities.showMessage((Activity)getContext(),
                    getContext().getString(R.string.sodk_editor_marknosel_title),
                    getContext().getString(R.string.sodk_editor_marknosel_body));
            return;
        }

        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        doc.addRedactAnnotation();
        doc.clearSelection();
        updateUIAppearance();
    }

    public void onRedactMarkArea(View v)
    {
        getPdfDocView().toggleMarkAreaMode();
        updateUIAppearance();
    }

    public void onRedactRemove(View v)
    {
        if (getDoc().getSelectionCanBeDeleted())
        {
            getDoc().selectionDelete();
            updateUIAppearance();
        }
    }

    public void onRedactApply(View v)
    {
        Utilities.yesNoMessage(((Activity) getContext()),
                "",  //  no title
                getContext().getString(R.string.sodk_editor_redact_confirm_apply_body),
                getContext().getString(R.string.sodk_editor_yes),
                getContext().getString(R.string.sodk_editor_no),
                new Runnable() {
                    @Override
                    public void run() {
                        // yes
                        MuPDFDoc doc = ((MuPDFDoc)getDoc());
                        doc.applyRedactAnnotation();
                        doc.clearSelection();
                        updateUIAppearance();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        //no
                    }
                });
    }

    @Override
    protected void preSaveQuestion(final Runnable yesRunnable, final Runnable noRunnable)
    {
        MuPDFDoc pdfDoc = (MuPDFDoc)getDoc();
        if (!pdfDoc.hasRedactionsToApply())
        {
            if (yesRunnable!=null)
                yesRunnable.run();
            return;
        }

        Utilities.yesNoMessage(((Activity) getContext()), "",
                getContext().getString(R.string.sodk_editor_redact_confirm_save),
                getContext().getString(R.string.sodk_editor_yes),
                getContext().getString(R.string.sodk_editor_no),
                new Runnable() {
                    @Override
                    public void run() {
                        if (yesRunnable!=null)
                            yesRunnable.run();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        if (noRunnable!=null)
                            noRunnable.run();
                    }
                });
    }

    private void hideUnusedButtons()
    {
        //  hide buttons that aren't used in this view
        if (mSavePdfButton!=null)
            mSavePdfButton.setVisibility(View.GONE);
        if (mOpenPdfInButton!=null)
            mOpenPdfInButton.setVisibility(View.GONE);
    }

    @Override
    public void setConfigurableButtons()
    {
        super.setConfigurableButtons();

        hideUnusedButtons();
    }

    @Override
    protected void onFullScreen(View v)
    {
        //  reset modes (draw and note)
        getPdfDocView().resetModes();
        updateUIAppearance();

        super.onFullScreen(v);
    }

    @Override
    protected void onPauseCommon()
    {
        DocPdfView view = getPdfDocView();
        if (view != null)
            view.resetDrawMode();

        super.onPauseCommon();
    }

    //  for no-UI API
    @Override
    public void setDrawModeOn()
    {
        getPdfDocView().onDrawMode();
        updateUIAppearance();
    }

    //  for no-UI API
    @Override
    public void setDrawModeOff()
    {
        getPdfDocView().setDrawModeOff();
        updateUIAppearance();
    }

    //  for no-UI API
    @Override
    public void setNoteModeOn()
    {
        getPdfDocView().setNoteMode(true);
        updateUIAppearance();
    }

    //  for no-UI API
    @Override
    public void setNoteModeOff()
    {
        getPdfDocView().setNoteMode(false);
        updateUIAppearance();
    }

    //  for no-UI API
    @Override
    public void deleteSelection()
    {
        onDeleteButton(null);
    }

    //  for no-UI API
    @Override
    public void deleteSelectedText()
    {
        //  do nothing; we can't select text in PDF files.
    }

    //  for no-UI API
    @Override
    public void setLineColor(int color)
    {
        getPdfDocView().setInkLineColor(color);
    }

    //  for no-UI API
    @Override
    public void setLineThickness(float size)
    {
        getPdfDocView().setInkLineThickness(size);
    }

    //  for no-UI API
    @Override
    public void highlightSelection()
    {
        getDoc().addHighlightAnnotation();
        getDoc().clearSelection();  //  fix 699073
    }

    //  for no-UI API
    @Override
    public boolean isDrawModeOn()
    {
        DocPdfView docView = getPdfDocView();
        return docView.getDrawMode();
    }

    //  for no-UI API
    @Override
    public boolean isNoteModeOn()
    {
        DocPdfView docView = getPdfDocView();
        return docView.getNoteMode();
    }

    //  for no-UI API
    @Override
    public void historyNext()
    {
        onNextLinkButton(null);
    }

    //  for no-UI API
    @Override
    public void historyPrevious()
    {
        onPreviousLinkButton(null);
    }

    //  for no-UI API
    @Override
    public void tableOfContents()
    {
        onTocButton(null);
    }

    //  for no-UI API
    @Override
    public boolean isTOCEnabled()
    {
        return tocEnabled;
    }

    //  for no-UI API
    @Override
    public void redactMarkText()
    {
        onRedactMark(null);
    }

    //  for no-UI API
    @Override
    public void redactMarkArea()
    {
        onRedactMarkArea(null);
    }

    //  for no-UI API
    @Override
    public boolean redactIsMarkingArea()
    {
        DocPdfView docView = getPdfDocView();
        boolean markAreaMode = docView.getMarkAreaMode();
        return markAreaMode;
    }

    //  for no-UI API
    @Override
    public void redactRemove()
    {
        onRedactRemove(null);
    }

    //  for no-UI API
    @Override
    public void redactApply()
    {
        onRedactApply(null);
    }

    //  for no-UI API
    @Override
    public Boolean canMarkRedaction()
    {
        return getDoc().getSelectionIsAlterableTextSelection();
    }

    //  for no-UI API
    @Override
    public Boolean canRemoveRedaction()
    {
        MuPDFDoc pdfDoc = (MuPDFDoc)getDoc();
        boolean selCanBeDeleted = getDoc().getSelectionCanBeDeleted();
        return (selCanBeDeleted && pdfDoc.selectionIsRedaction());
    }

    //  for no-UI API
    @Override
    public Boolean canApplyRedactions()
    {
        MuPDFDoc pdfDoc = (MuPDFDoc)getDoc();
        return pdfDoc.hasRedactionsToApply();
    }

    @Override
    public void onSelectionChanged()
    {
        super.onSelectionChanged();

        getPdfDocView().onSelectionChanged();
    }
}
