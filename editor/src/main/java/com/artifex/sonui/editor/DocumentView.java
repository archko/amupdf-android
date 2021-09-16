/**
 *  This class implements a new no-UI API that allows a developer to
 *  embed a DocumentView into their app and have total control of the UI.
 *
 * Copyright (C) Artifex, 2020. All Rights Reserved.
 *
 * @author Artifex
 */

package com.artifex.sonui.editor;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.SODoc;
import com.artifex.solib.SODocSaveListener;

public class DocumentView extends NUIView {

    public DocumentView(Context context) {
        super(context);
    }

    public DocumentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * start the view with a given file path
     *
     * @param uri       the file path
     * @param page      start at a given page number (zero-based)
     * @param showUI    controls whether the built-in UI is used, true = yes
     */
    public void start(Uri uri, int page, boolean showUI)
    {
        makeNUIView(uri, null);
        addView(mDocView);
        mDocView.setDocumentListener(mDocumentListener);
        if (page<0)
            page = 0;
        ViewingState vstate = new ViewingState(page);
        mDocView.start(uri, false, vstate, null, mDoneListener, showUI);
    }

    /**
     *  Show the list of page thumbnails.
     */
    public void showPageList()
    {
        if (mDocView != null)
            mDocView.showPages();
    }

    /**
     *  Hide the list of page thumbnails.
     */
    public void hidePageList()
    {
        if (mDocView != null)
            mDocView.hidePages();
    }

    /**
     *  Determine if the page list is visible
     *
     * @return  true if it's visible
     */
    public boolean isPageListVisible()
    {
        if (mDocView!=null)
            return mDocView.isPageListVisible();
        return false;
    }

    /**
     *  enable PDF ink annotation drawing mode
     */
    public void setDrawModeOn()
    {
        if (mDocView != null)
            mDocView.setDrawModeOn();
    }

    /**
     *  disable PDF ink annotation drawing mode
     */
    public void setDrawModeOff()
    {
        if (mDocView != null)
            mDocView.setDrawModeOff();
    }

    /**
     *  delete what's currently selected.
     *  for PDF documents, this applies to selected annotations and redactions.
     */
    public void deleteSelection()
    {
        if (mDocView != null)
            mDocView.deleteSelection();
    }

    /**
     *  create a highlight annotation from the currently selected text.
     */
    public void highlightSelection()
    {
        if (mDocView != null)
            mDocView.highlightSelection();
    }

    /**
     *  set the color for ink annotations.
     *
     * @param color   the color, in ARGB format
     */
    public void setLineColor(int color)
    {
        if (mDocView != null)
            mDocView.setLineColor(color);
    }

    /**
     *  set the line thickness for ink annotations.
     *
      * @param size   the thickness in (units ???)
     */
    public void setLineThickness(float size)
    {
        if (mDocView != null)
            mDocView.setLineThickness(size);
    }

    /**
     *  Enter full screen mode.
     *  When we leave full screen mode, the supplied Runnable is run.
     *
     * @param onExitFullScreen  the Runnable
     */
    public void enterFullScreen(Runnable onExitFullScreen)
    {
        if (mDocView != null)
            mDocView.enterFullScreen(onExitFullScreen);
    }

    /**
     * specify a Runnable that should run when the selection changes.
     *
     * @param updateUIRunnable - the Runnable
     */
    public void setOnUpdateUI(Runnable updateUIRunnable)
    {
        if (mDocView != null)
            mDocView.setOnUpdateUI(updateUIRunnable);
    }

    /**
     * find out if the current selection can be deleted.
     *
     * @return - true if it can, false if not.
     */
    public boolean canDeleteSelection()
    {
        if (mDocView != null)
            return mDocView.canDeleteSelection();
        return false;
    }

    /**
     * check to see if ink annotation draw mode is enabled.
     *
     * @return - true if it is, false if not.
     */
    public boolean isDrawModeOn()
    {
        if (mDocView != null)
            return mDocView.isDrawModeOn();
        return false;
    }

    /**
     * check to see if note annotation mode is enabled.
     *
     * @return - true if it is, false if not.
     */
    public boolean isNoteModeOn()
    {
        if (mDocView != null)
            return mDocView.isNoteModeOn();
        return false;
    }

    /**
     * get the current page count.
     *
     * @return - the page count
     */
    public int getPageCount()
    {
        if (mDocView != null)
            return mDocView.getPageCount();
        return 0;
    }

    /**
     * if a password is required, DocumentListener.onPasswordRequired() will
     * be called. At that time, you should collect a password from the user and then call
     * providePassword. If the password is verified, the document will load.
     * If not, DocumentListener.onPasswordRequired() will be called again.
     *
     * @param password - the password to provide for verification
     */
    public void providePassword(String password)
    {
        if (mDocView != null)
            mDocView.providePassword(password);
    }

    /**
     * used to find out if selected text in a PDF file can have a highlight
     * annotation created for it.
     *
     * @return
     */
    public boolean isAlterableTextSelection()
    {
        if (mDocView != null)
            return mDocView.isAlterableTextSelection();
        return false;
    }

    /**
     * search forward for text
     *
     * @param s - the text to search for
     */
    public void searchForward(String s)
    {
        if (mDocView != null)
            mDocView.searchForward(s);
    }

    /**
     * search backwards for text
     *
     * @param s - the text to search for
     */
    public void searchBackward(String s)
    {
        if (mDocView != null)
            mDocView.searchBackward(s);
    }

    /**
     * save the document
     */
    public void save()
    {
        if (mDocView != null)
            mDocView.save();
    }

    /**
     * save the document to a new path
     *
     * @param path      the new path
     * @param listener  a listener to use when the save is complete
     */
    public void saveTo(String path, SODocSaveListener listener)
    {
        if (mDocView != null)
            mDocView.saveTo(path, listener);
    }

    /**
     * invoke Save As for the document.
     *
     * currently requires implementing DataLeakHandlers to handle the UI.
     */
    public void saveAs()
    {
        if (mDocView != null)
            mDocView.saveAs();
    }

    /**
     * find out if the document has been changed since it was last opened or saved.
     *
     * @return - true if it's been modified, false if not.
     */
    public boolean isDocumentModified()
    {
        if (mDocView != null)
            return mDocView.isDocumentModified();
        return false;
    }

    /**
     * print the document.
     */
    public void print()
    {
        if (mDocView != null)
            mDocView.print();
    }

    /**
     * invoke Share for the document.
     * currently requires implementing DataLeakHandlers
     */
    public void share()
    {
        if (mDocView != null)
            mDocView.share();
    }

    /**
     * invoke Open In for the document.
     * currently requires implementing DataLeakHandlers
     */
    public void openIn()
    {
        if (mDocView != null)
            mDocView.openIn();
    }

    /**
     * enable note annotation mode
     */
    public void setNoteModeOn()
    {
        if (mDocView != null)
            mDocView.setNoteModeOn();
    }

    /**
     * disable note annotation mode
     */
    public void setNoteModeOff()
    {
        if (mDocView != null)
            mDocView.setNoteModeOff();
    }

    /**
     * display a dialog for setting note annotation author.
     */
    public void author()
    {
        //  TODO: this should be done externally.
        if (mDocView != null)
            mDocView.author();
    }

    /**
     * go to the first page
     */
    public void firstPage()
    {
        if (mDocView != null)
            mDocView.firstPage();
    }

    /**
     * go to the last page
     */
    public void lastPage()
    {
        if (mDocView != null)
            mDocView.lastPage();
    }

    /**
     * get the current page number
     *
     * @return - the page number
     */
    public int getPageNumber()
    {
        if (mDocView != null)
            return mDocView.getPageNumber();
        return 0;
    }

    /**
     * go to the next position in the History
     */
    public void historyNext()
    {
        if (mDocView != null)
            mDocView.historyNext();
    }

    /**
     * go to the previous position in the History
     */
    public void historyPrevious()
    {
        if (mDocView != null)
            mDocView.historyPrevious();
    }

    /**
     * returns whether the document has a previous entry
     * in the History
     *
     * @return - true or false
     */
    public boolean hasPreviousHistory()
    {
        if (mDocView != null)
            return mDocView.hasPreviousHistory();
        return false;
    }

    /**
     * returns whether the document has a next entry
     * in the History
     *
     * @return - true or false
     */
    public boolean hasNextHistory()
    {
        if (mDocView != null)
            return mDocView.hasNextHistory();
        return false;
    }

    /**
     * use the built-in UI to display the table of contents
     */
    public void tableOfContents()
    {
        if (mDocView != null)
            mDocView.tableOfContents();
    }

    /**
     * return whether the document has a table of contents
     *
     * @return
     */
    public boolean isTOCEnabled()
    {
        if (mDocView != null)
            return mDocView.isTOCEnabled();
        return false;
    }

    /**
     * interface for enumerating the table of contents
     */
    public interface EnumeratePdfTocListener {
        /**
         * nextTocEntry is called once for each entry in the Table of Contents
         * handle and parentHandle can be used to navigate the TOC hierarchy
         * if page>=0 it's an internal link, so use x and y
         * if page<0 and url in not null, url is an external link
         *
         * @param handle        my ID number in the hierarchy
         * @param parentHandle  my parent's ID number in the hierarchy
         * @param page          target page number
         * @param label         user-friendly label
         * @param url           url for an external link
         * @param x             x - coordinate on the page.
         * @param y             y - coordinate on the page.
         */
        void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y);
    }

    /**
     * enumerate the document's TOC. The listener's nextTocEntry() function
     * will be called for each TOC entry.
     *
     * @param listener
     */
    public void enumeratePdfToc(final EnumeratePdfTocListener listener)
    {
        SODoc doc = (SODoc)mDocView.getDoc();
        ArDkLib.enumeratePdfToc(doc, new ArDkLib.EnumeratePdfTocListener() {
            @Override
            public void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y)
            {
                listener.nextTocEntry(handle, parentHandle, page, label, url, x, y);
            }
        });
    }

    /**
     * given a page number and a rectangle, scroll so that location in the document is
     * visible.
     *
     * @param page      the page
     * @param box       the rectangle
     */
    public void gotoInternalLocation(int page, RectF box)
    {
        if (mDocView != null)
            mDocView.gotoInternalLocation(page, box);
    }

    /**
     * mark the selected text for redaction.
     */
    public void redactMarkText()
    {
        if (mDocView != null)
            mDocView.redactMarkText();
    }

    /**
     * turn on "mark area" redaction mode.
     * This allows the user to then draw a rectangle which is
     * marked for redaction.
     */
    public void redactMarkArea()
    {
        if (mDocView != null)
            mDocView.redactMarkArea();
    }

    /**
     * returns whether the document is in "mark area" redaction mode
     *
     * @return - true or false
     */
    public boolean redactIsMarkingArea()
    {
        if (mDocView != null)
            return mDocView.redactIsMarkingArea();
        return false;
    }

    /**
     * un-redact a marked and selected, but not yet applied, redaction
     */
    public void redactRemove()
    {
        if (mDocView != null)
            mDocView.redactRemove();
    }

    /**
     * apply all marked and not-yet-applied redactions.
     * this operation is not reversible.
     */
    public void redactApply()
    {
        if (mDocView != null)
            mDocView.redactApply();
    }

    /**
     * returns whether currently selected text can be redacted.
     *
     * @return - true or false
     */
    public boolean canMarkTextRedaction()
    {
        if (mDocView != null)
            return mDocView.canMarkRedaction();
        return false;
    }

    /**
     * returns whether currently selected but not yet applied
     * redaction can be removed (un-redacted)
     *
     * @return - true or false
     */
    public boolean canRemoveRedaction()
    {
        if (mDocView != null)
            return mDocView.canRemoveRedaction();
        return false;
    }

    /**
     * returns whether there are redactions that can be applied.
     *
     * @return
     */
    public boolean canApplyRedactions()
    {
        if (mDocView != null)
            return mDocView.canApplyRedactions();
        return false;
    }

    /**
     * scroll to the given page number.
     *
     * @param pageNum
     */
    public void goToPage(int pageNum)
    {
        if (mDocView != null) {

            //  enforce range
            if (pageNum<0)
                pageNum = 0;
            if (pageNum>getPageCount()-1)
                pageNum = getPageCount()-1;

            //  put the keyboard away
            Utilities.hideKeyboard(getContext());

            //  go
            mDocView.goToPage(pageNum, true);
        }
    }

    /**
     * interface for monitoring changes in which page is being displayed
     */
    public interface ChangePageListener {
        void onPage(int pageNumber);
    }
    private ChangePageListener mChangePageListener = null;

    /**
     * set a ChangePageListener on the document.
     *
     * @param listener - the listener
     */
    public void setPageChangeListener(ChangePageListener listener) {
        mChangePageListener = listener;
        if (mDocView != null) {
            mDocView.setPageChangeListener(new NUIDocView.ChangePageListener() {
                @Override
                public void onPage(int pageNumber) {
                    if (mChangePageListener!=null) {
                        mChangePageListener.onPage(pageNumber);
                    }
                }
            });
        }
    }

    /**
     * call this to set a DocumentListener on this view.
     */
    protected DocumentListener mDocumentListener = null;
    public void setDocumentListener(DocumentListener listener)
    {
        mDocumentListener = listener;
    }

    /**
     * returns whether undo is possible
     *
     * @return - true or false
     */
    public boolean canUndo() {
        if (mDocView != null)
            return mDocView.canUndo();
        return false;
    }

    /**
     * returns whether redo is possible
     *
     * @return - true or false
     */
    public boolean canRedo() {
        if (mDocView != null)
            return mDocView.canRedo();
        return false;
    }

    /**
     * perform an undo operation.
     */
    public void undo() {
        if (!canUndo())
            return;

        if (mDocView != null)
            mDocView.doUndo();
    }

    /**
     * perform a redo operation.
     */
    public void redo() {
        if (!canRedo())
            return;

        if (mDocView != null)
            mDocView.doRedo();
    }

    /**
     * set the document's flow mode.
     *
     * @param mode - mode
     *             1 = normal
     *             2 = reflow
     *             3 = resize
     */
    public void setFlowMode(int mode) {
        if (mDocView != null)
            mDocView.setFlowMode(mode);
    }

    /**
     * set the document's flow mode to Reflow.
     */
    public void setFlowModeReflow() {
        if (mDocView != null)
            mDocView.setFlowMode(SODoc.FLOW_MODE_REFLOW);
    }

    /**
     * set the document's flow mode to Normal.
     */
    public void setFlowModeNormal() {
        if (mDocView != null)
            mDocView.setFlowMode(SODoc.FLOW_MODE_NORMAL);
    }

    /**
     * set the document's flow mode to Resize.
     */
    public void setFlowModeResize() {
        if (mDocView != null)
            mDocView.setFlowMode(SODoc.FLOW_MODE_RESIZE);
    }

    /**
     * get document's flow mode
     */
    public int getFlowMode()
    {
        if (mDocView != null)
            return mDocView.getFlowMode();
        return SODoc.FLOW_MODE_NORMAL;
    }

    /**
     * delete selected text
     */
    public void deleteSelectedText() {
        if (mDocView != null)
            mDocView.deleteSelectedText();
    }

    /**
     * set selected text
     *
     * if the current selection is caret, the text is inserted
     * otherwise the currently selected text is replaced.
     */
    public void setSelectionText(String text) {
        if (mDocView != null)
            mDocView.setSelectionText(text);
    }

    /**
     * get currently selected text
     *
     * returns NULL if there is no selection
     */
    public String getSelectedText()
    {
        if (mDocView != null)
            return mDocView.getSelectedText();
        return "";
    }

    /**
     * select text based on one point, in view coordinates
     * this will place the caret in the text near the given point
     */
    public boolean select (Point p)
    {
        if (mDocView != null)
            return mDocView.select(p);
        return false;
    }

    /**
     * select text based on two given points, in view coordinates
     */
    public boolean select (Point topLeft, Point bottomRight)
    {
        if (mDocView != null)
            return mDocView.select(topLeft, bottomRight);
        return false;
    }

    /**
     * return true if the view supports searching
     */
    public boolean hasSearch()
    {
        if (mDocView!=null)
            return mDocView.hasSearch();
        return false;
    }

    /**
     * return true if the view supports undo
     */
    public boolean hasUndo()
    {
        if (mDocView!=null)
            return mDocView.hasUndo();
        return false;
    }

    /**
     * return true if the view supports redo
     */
    public boolean hasRedo()
    {
        if (mDocView!=null)
            return mDocView.hasRedo();
        return false;
    }

    /**
     * return true if the view supports selection
     */
    public boolean canSelect()
    {
        if (mDocView!=null)
            return mDocView.canSelect();
        return false;
    }

    /**
     * return true if the view supports history
     */
    public boolean hasHistory()
    {
        if (mDocView!=null)
            return mDocView.hasHistory();
        return false;
    }

    /**
     * return true if the view supports reflow
     */
    public boolean hasReflow()
    {
        if (mDocView!=null)
            return mDocView.hasReflow();
        return false;
    }

    /**
     * return the view's current x-scroll position
     * a return value of -1 indicates an error
     */
    public int getScrollPositionX()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionX();
        return -1;
    }

    /**
     * return the view's current y-scroll position
     * a return value of -1 indicates an error
     */
    public int getScrollPositionY()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionY();
        return -1;
    }

    /**
     * return the view's current scale factor
     * a return value of -1 indicates an error
     */
    public float getScaleFactor()
    {
        if (mDocView != null)
            return mDocView.getScaleFactor();
        return -1;
    }

    /**
     * set new scale factor, x- and y-scroll values for the view.
     */
    public void setScaleAndScroll (float newScale, int newX, int newY)
    {
        if (mDocView != null)
            mDocView.setScaleAndScroll(newScale, newX, newY);
    }

    //  get the LifecycleOwner for our view, by walking up the hierarchy
    //  until we find the LifecycleOwner.
    //  typically this will be an AppCompatActivity,
    //  but could be a FragmentActivity.
    private LifecycleOwner getLifecycleOwner() {
        //  should be called after we're attached to the window.
        if (!isAttachedToWindow())
            return null;  //  maybe this should throw instead?
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof LifecycleOwner) {
                return (LifecycleOwner) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    //  this is an observer class for intercepting
    //  specific lifecycle events.
    private class MyLifecycleObserver implements LifecycleObserver {
        public MyLifecycleObserver(DocumentView v) {
            mDocumentView = v;
        }
        private DocumentView mDocumentView;

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        void onDestroy() {
            mDocumentView.onDestroy();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void onPause() {
            mDocumentView.onPause(null);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        void onResume() {
            mDocumentView.onResume();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //  get the LifecycleOwner
        LifecycleOwner owner = getLifecycleOwner();

        if (owner != null) {

            //  who are we?
            final DocumentView documentView = this;

            //  add the lifecycle observer
            owner.getLifecycle().addObserver(new MyLifecycleObserver(this));

            //  register to get component callbacks.
            //  we'll use this to find out about configuration changes
            ((Context) owner).registerComponentCallbacks(new ComponentCallbacks2() {
                @Override
                public void onTrimMemory(int level) {
                }

                @Override
                public void onConfigurationChanged(@NonNull Configuration newConfig) {
                    documentView.onConfigurationChange(newConfig);
                }

                @Override
                public void onLowMemory() {
                }
            });

            //  register a callback for OnBackPressed
            if (owner instanceof FragmentActivity) {
                OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        documentView.onBackPressed();
                    }
                };
                ((AppCompatActivity) owner).getOnBackPressedDispatcher().addCallback(
                        owner, // LifecycleOwner
                        callback);
            }

        }
    }
}
