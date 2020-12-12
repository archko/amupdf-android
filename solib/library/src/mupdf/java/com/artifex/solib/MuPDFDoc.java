package com.artifex.solib;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Location;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFDocument;
import com.artifex.mupdf.fitz.PDFObject;
import com.artifex.mupdf.fitz.PDFPage;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.SeekableInputStream;
import com.artifex.mupdf.fitz.SeekableInputOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MuPDFDoc extends ArDkDoc
{
    private Document mDocument = null;
    private ArrayList<MuPDFPage> mPages = new ArrayList<>();
    private Worker mWorker = null;
    private int mPageCount = 0;
    private int mPageNumber = 0;
    private boolean mLoadAborted = false;
    private boolean mDestroyed = false;
    private ConfigOptions mDocCfgOpts = null;
    private SODocLoadListener mListener = null;
    private Context mContext;

    //  an interface for receiving javascript alerts
    public interface JsEventListener {
        void onAlert(String message);
    }

    //  an optional listener set by the holder of this doc
    private JsEventListener jsEventListener2 = null;
    public void setJsEventListener(JsEventListener listener) {jsEventListener2=listener;}

    //  a listener passed to mupdf, which in turn calls the holder's listener.
    public PDFDocument.JsEventListener jsEventListener = new PDFDocument.JsEventListener() {
        @Override
        public void onAlert(String message)
        {
            if (jsEventListener2!=null)
                jsEventListener2.onAlert(message);
        }
    };

    MuPDFDoc(Looper looper, SODocLoadListener listener, Context context, ConfigOptions cfg)
    {
        mListener = listener;
        mContext = context;
        mDocCfgOpts = cfg;

        mPageCount = 0;
        mPageNumber = 0;

        mLoadTime=System.currentTimeMillis();
        // initialise last save time to be our load time
        mLastSaveTime = mLoadTime;

        mWorker = new Worker(looper);
    }

    public void setDocument(Document document)
    {
        mDocument = document;
    }

    private long mLoadTime;
    public long getLoadTime() {return mLoadTime;}

    private String mOpenedPath = null;
    public void setOpenedPath(String path) {mOpenedPath = path;}
    public String getOpenedPath() {return mOpenedPath;}

    public void startWorker ()
    {
        mWorker.start();
    }

    public Worker getWorker() {return mWorker;}

    private void addPage(Page page)
    {
        MuPDFPage mpage = new MuPDFPage(this, page, mNumPages);
        mPages.add(mpage);
        mNumPages = mPages.size();
    }

    public Document getDocument() {
        return mDocument;
    }

    public static PDFDocument getPDFDocument(Document doc)
    {
        try {
            PDFDocument pdfDoc = (PDFDocument)doc;
            return pdfDoc;
        }
        catch (Exception e)
        {
        }

        return null;
    }

    @Override
    public ArDkPage getPage(int pageNumber, SOPageListener listener)
    {
        MuPDFPage page = mPages.get(pageNumber);
        page.addPageListener(listener);

        return page;
    }

    @Override
    public void saveTo(final String path, final SODocSaveListener listener)
    {
        if (!canSave())
        {
            //  mupdf can't save the file, so we'll just copy the original document

            if (path.compareToIgnoreCase(getOpenedPath())==0)
            {
                //  asking to copy the file to itself.
                //  do nothing, but return success.
                ArDkLib.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        listener.onComplete(SODocSaveListener.SODocSave_Succeeded, 0);
                        mLastSaveTime = System.currentTimeMillis();
                    }
                });
            }
            else if (FileUtils.copyFile(getOpenedPath(), path, true))
            {
                //  file was copied
                ArDkLib.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        listener.onComplete(SODocSaveListener.SODocSave_Succeeded, 0);
                        mLastSaveTime = System.currentTimeMillis();
                    }
                });
            }
            else
            {
                //  file copy error
                //  795 = file copy error
                ArDkLib.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        listener.onComplete(SODocSaveListener.SODocSave_Error, 795);
                    }
                });
            }
            return;
        }

        //  mupdf will save the file.
        saveToInternal(path, listener);
    }

    private boolean mLastSaveWasIncremental;
    public boolean lastSaveWasIncremental() {return mLastSaveWasIncremental;}

    private long mLastSaveTime = 0;
    public long getLastSaveTime() {return mLastSaveTime;}

    private boolean mForceReload;
    public boolean getForceReload() {return mForceReload;}
    @Override
    public void setForceReload(boolean force) {mForceReload = force;}

    private boolean mForceReloadAtResume;
    public boolean getForceReloadAtResume() {return mForceReloadAtResume;}
    @Override
    public void setForceReloadAtResume(boolean force) {
        mForceReloadAtResume = force;
    }

    //  this function tells us whether mupdf can save the file.
    //  It would be good if this were part of the mupdf API instead.
    public boolean canSave()
    {
        //  this doc can't be saved unless the underlying mupdf document is a PDF doc.
        PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
        if (pdfDoc==null)
            return false;
        return true;
    }

    public boolean canPrint()
    {
        //  if this doc can't be saved, it also can't be printed.
        //  that's because for printing we require a PDF file, and one can't
        //  be produced by mupdf for non-PDF formats.
        return canSave();
    }

    //  this is the path of the last-saved version of this doc
    private String lastSavedPath = null;

    public void saveToInternal(final String path, final SODocSaveListener listener)
    {
        //  we can only save PDF files
        final PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
        if (pdfDoc==null){
            listener.onComplete(SODocSaveListener.SODocSave_Error, 0);
            return;
        }

        //  NOTE: we now save by first writing to a temp file, and copying
        //  the result back to the destination. This solves a potential issue
        //  with non-incremental saving over an original.

        //  we save on the background thread so as not to block the UI

        getWorker().add(new Worker.Task()
        {
            private int saveResult;

            public void work()
            {
                //  check for incremental saving only if we're told to.
                //  that's the default, but when printing we don't.
                mLastSaveWasIncremental = false;
                String opts = "";
                if (pdfDoc.canBeSavedIncrementally()) {
                    mLastSaveWasIncremental = true;
                    opts = "incremental";
                }

                //  create a temp file path
                String tmp = FileUtils.getTempPathRoot(mContext) + File.separator + UUID.randomUUID() + ".pdf";

                //  if we're doing an incremental save, first copy the original
                //  to the temp path.  mupdf will update it.
                if (opts.equals("incremental")) {
                    //  the original might be the one the user opened,
                    //  or the last one we saved.
                    String original;
                    if (lastSavedPath==null)
                        original = getOpenedPath();
                    else
                        original = lastSavedPath;
                    FileUtils.copyFile(original, tmp, true);
                }

                if (!getHasBeenModified() && opts.equals("incremental")) {
                    //  there are no changes, so no saving is required,
                    //  the copy we made above is up to date.
                    saveResult=0;
                }
                else
                    {
                    //  do the save.
                    SOSecureFS mSecureFs = ArDkLib.getSecureFS();
                    if (mSecureFs != null && mSecureFs.isSecurePath(path)) {
                        saveResult = saveSecure(pdfDoc, tmp, opts, mSecureFs);
                    }
                    else {
                        try {
                            pdfDoc.save(tmp, opts);
                            saveResult = 0;
                        }
                        catch(Exception e) {
                            saveResult = 1;  //  anything that's not 0 for now
                        }
                    }
                }

                //  do the swap
                if (saveResult==0) {
                    setModified(false);
                    if(FileUtils.copyFile(tmp, path, true)) {
                        FileUtils.deleteFile(tmp);
                        lastSavedPath = path;
                    }
                }
            }

            public void run()
            {
                //  notify
                if (listener != null)
                {
                    if (saveResult==0)
                    {
                        listener.onComplete(SODocSaveListener.SODocSave_Succeeded, saveResult);
                        mLastSaveTime = System.currentTimeMillis();
                    }
                    else
                    {
                        listener.onComplete( SODocSaveListener.SODocSave_Error, saveResult );
                    }
                }
            }
        });
    }

    static int saveSecure(PDFDocument doc, String path, String opts, final SOSecureFS secureFS)
    {
        try
        {
            final Object handle = secureFS.getFileHandleForWriting(path);

            SeekableInputOutputStream stream = new SeekableInputOutputStream()
            {
                public void close() throws IOException
                {
                    //  we should get called back here when mupdf is done saving.
                    //  but instead we do it below, because it's safer.
                    //secureFS.closeFile(handle);
                }

                public int read(byte[] b) throws IOException
                {
                    int numBytes = secureFS.readFromFile(handle, b);
                    return (numBytes == 0) ? -1 : numBytes;
                }

                public void write(byte[] b, int off, int len) throws IOException
                {
                    if (off == 0 && len == b.length)
                        secureFS.writeToFile(handle, b);
                    else
                        secureFS.writeToFile(handle, Arrays.copyOfRange(b, off, len));
                }

                public long seek(long offset, int whence) throws IOException
                {
                    long current = secureFS.getFileOffset(handle);
                    long length = secureFS.getFileLength(handle);
                    long pos = 0;

                    switch (whence)
                    {
                        case SEEK_SET:
                            pos = offset;
                            break;

                        case SEEK_CUR:
                            pos = current+offset;
                            break;

                        case SEEK_END:
                            pos = length+offset;
                            break;
                    }
                    secureFS.seekToFileOffset(handle, pos);
                    return pos;
                }

                public long position() throws IOException
                {
                    long current = secureFS.getFileOffset(handle);
                    return current;
                }
            };

            //  OK, save.
            doc.save(stream, opts);
            secureFS.closeFile(handle);

            return 0;
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    @Override
    public void saveToPDF(String path, boolean imagePerPage, final SODocSaveListener listener)
    {
        //  this is the same as saveTo.
        //  we're ignoring the 'imagePerPage' parameter.

        //  this is used by printing, so let's not check for incremental
        //  saving.  We know we're making a brand new copy.

        final boolean wasModified = getHasBeenModified();
        saveToInternal(path, new SODocSaveListener() {
            @Override
            public void onComplete(int result, int err) {
                //  saving may cause the 'modified' state to change, so
                //  restore it.
                setModified(wasModified);
                listener.onComplete(result, err);
            }
        });
    }

    //  this tracks whether the document has been modified by the user.
    private boolean mIsModified = false;
    void setModified(boolean val) {mIsModified = val;}

    @Override
    public boolean getHasBeenModified() {

        //return mDocument.hasUnsavedChanges();

        //  The issue with using mDocument.hasUnsavedChanges() here is that when mupdf
        //  repairs a document, hasUnsavedChanges() returns true.
        //  we'll instead use setModified() to track when the user has made
        //  changes.  Which at this writing is only when adding annotations.

        return mIsModified;
    }

    @Override
    public void abortLoad() {
        destroyDoc();
    }

    @Override
    public void destroyDoc()
    {
        if (mDestroyed)
            return;
        mDestroyed = true;

        mLoadAborted = true;

        //  disable javascript, which should be done on the doc's worker thread,
        //  When that's complete, handle the rest of the destruction.
        Worker worker = getWorker();
        worker.add(new Worker.Task()
        {
            public void work()
            {
                if (mDocument != null)
                {
                    PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
                    if (pdfDoc!=null) {
                        //  disable javascript
                        pdfDoc.setJsEventListener(null);
                        pdfDoc.disableJs();
                    }
                }
            }

            public void run()
            {
                //  stop the worker
                if (mWorker!=null) {
                    mWorker.stop();
                    mWorker = null;
                }

                //  get rid of the pages
                if (mPages!=null)
                {
                    for (MuPDFPage page : mPages) {
                        page.releasePage();
                    }
                    mPages.clear();
                    mPages = null;
                }

                mPageCount = 0;
                mPageNumber = 0;

                if (mDocument!=null)
                    mDocument.destroy();
                mDocument = null;
            }
        });

    }

    @Override
    public void clearSelection()
    {
        int oldPage = selectedAnnotPagenum;
        selectedAnnotPagenum = -1;
        selectedAnnotIndex = -1;

        if (oldPage != -1) {
            mPages.get(oldPage).clearSelection();
        }

        int textSelPage = MuPDFPage.getTextSelPageNum();
        if (textSelPage != -1)
        {
            mPages.get(textSelPage).clearSelectedText();
        }
    }

    private int selectedAnnotIndex = -1;
    private int selectedAnnotPagenum = -1;
    public int getSelectedAnnotationPagenum() {return selectedAnnotPagenum;}
    public void setSelectedAnnotation(int pagenum, int index)
    {
        selectedAnnotPagenum = pagenum;
        selectedAnnotIndex = index;
    }
    public PDFAnnotation getSelectedAnnotation()
    {
        if (selectedAnnotPagenum!=-1 && selectedAnnotIndex!=-1 && selectedAnnotPagenum<mPages.size())
        {
            MuPDFPage page = mPages.get(selectedAnnotPagenum);
            PDFAnnotation annot = page.getAnnotation(selectedAnnotIndex);
            return annot;
        }
        return null;
    }

    @Override
    public boolean getSelectionIsAlterableTextSelection() {
        int textSelPage = MuPDFPage.getTextSelPageNum();
        return (textSelPage != -1);
    }

    @Override
    public boolean getSelectionHasAssociatedPopup()
    {
        PDFAnnotation annot = (PDFAnnotation)getSelectedAnnotation();

        if (annot != null && annot.getType()==PDFAnnotation.TYPE_TEXT)
            return true;
        if (annot != null && annot.getType()==PDFAnnotation.TYPE_HIGHLIGHT)
            return true;

        return false;
    }

    @Override
    public boolean getSelectionCanCreateAnnotation() {
        return false;  //  TODO
    }

    @Override
    public boolean getSelectionCanBeDeleted()
    {
        if (selectedAnnotPagenum!=-1 && selectedAnnotIndex!=-1)
            return true;
        return false;
    }

    public boolean selectionIsRedaction()
    {
        if (selectedAnnotPagenum!=-1)
        {
            MuPDFPage page = mPages.get(selectedAnnotPagenum);
            PDFAnnotation annot = page.getAnnotation(selectedAnnotIndex);
            if (null != annot && annot.getType() == PDFAnnotation.TYPE_REDACT)
                return true;
        }

        return false;
    }

    @Override
    public boolean getSelectionCanBeResized()
    {
        return false;
    }

    @Override
    public boolean getSelectionCanBeAbsolutelyPositioned()
    {
        return false;
    }

    @Override
    public boolean getSelectionCanBeRotated()
    {
        return false;
    }

    @Override
    public void selectionDelete()
    {
        if (selectedAnnotPagenum!=-1 && selectedAnnotIndex!=-1)
        {
            MuPDFPage page = mPages.get(selectedAnnotPagenum);
            PDFAnnotation annot = page.getAnnotation(selectedAnnotIndex);
            page.deleteAnnotation(annot);
            update(selectedAnnotPagenum);

            //  keep track of pages with un-applied redaction marks
            int numRedact = mPages.get(selectedAnnotPagenum).countAnnotations(PDFAnnotation.TYPE_REDACT);
            if (numRedact==0)
                removePageWithRedactions(selectedAnnotPagenum);

            clearSelection();
        }
    }

    @Override
    public void cancelSearch() {
        searchRunning = false;
    }

    @Override
    public void closeSearch()
    {
        searchRunning = false;
    }

    @Override
    public boolean isSearchRunning() {
        return searchRunning;
    }

    @Override
    public void setSearchStart(int page, PointF xy) {
        setSearchStart(page, xy.x, xy.y);
    }

    @Override
    public void setSearchStart(int page, float x, float y) {
        //  no one is calling this
        if (searchRunning)
            throw new IllegalArgumentException("Search already in progess");
        //  TODO
    }

    @Override
    public void setSearchString(String text)
    {
        if (searchRunning)
            throw new IllegalArgumentException("Search already in progess");

        if (!text.equalsIgnoreCase(searchText))
        {
            searchIndex = 0;
            searchPage = 0;
            searchNewPage = true;
        }

        searchText = text;
    }

    @Override
    public void setSearchMatchCase(boolean matchCase)
    {
        if (searchRunning)
            throw new IllegalArgumentException("Search already in progess");
        this.searchMatchCase = matchCase;
    }

    @Override
    public void setSearchBackwards(boolean backwards) {
        if (searchRunning)
            throw new IllegalArgumentException("Search already in progess");
        searchBackwards = backwards;
    }

    /* true, iff we have a search running. */
    private boolean searchRunning;

    /* The search text to use for the next search operation. */
    private String searchText;

    /* Whether the next search operation should match case sensitively or not */
    private boolean searchMatchCase;

    /* Whether the next search operation should search backwards or not */
    private boolean searchBackwards;

    /* The SOSearchListener to use in the next search operation */
    private SOSearchListener searchListener;

    @Override
    public void setSearchListener(SOSearchListener listener)
    {
        if (listener==null)
        {
            cancelSearch();
            this.searchListener = listener;
            return;
        }

        if (searchRunning)
            throw new IllegalArgumentException("Search already in progess");
        this.searchListener = listener;
    }

    private int searchPage = 0;
    private int searchIndex = 0;
    private boolean searchNewPage = true;
    private boolean searchCancelled;
    private boolean searchMatchFound;

    private void nextSearchPage()
    {
        //  cancel highlight for the current page
        mPages.get(searchPage).setSearchIndex(-1);

        if (searchBackwards)
            searchPage--;
        else
            searchPage++;

        //  wrap around
        if (searchPage < 0)
            searchPage = getNumPages() - 1;
        if (searchPage >= getNumPages())
            searchPage = 0;

        searchNewPage = true;

        ((Activity)mContext).runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (searchListener != null)
                    searchListener.progressing(searchPage);
            }
        });
    }

    @Override
    public int search()
    {
        searchRunning    = true;
        searchCancelled  = false;
        searchMatchFound = false;

        Worker worker = getWorker();
        worker.add(new Worker.Task()
        {
            public void work()
            {
                int numFailedPages = 0;

                while (true)
                {
                    //  we were cancelled?
                    if (!searchRunning)
                    {
                        searchCancelled = true;
                        break;
                    }

                    //  what's on the current page?
                    int numHits =
                        mPages.get(searchPage).setSearchString(searchText);

                    if (numHits == 0)
                    {
                        numFailedPages++;
                        if (numFailedPages==getNumPages())
                        {
                            //  nothing found at all
                            break;
                        }

                        //  none found, go to next page
                        nextSearchPage();
                    }
                    else
                    {
                        //  some found on this page.
                        //  set a sensible index

                        if (searchNewPage)
                        {
                            //  searching a new page
                            if (searchBackwards)
                                searchIndex = numHits - 1;
                            else
                                searchIndex = 0;
                            searchNewPage = false;
                        }
                        else
                        {
                            //  searching again same page
                            if (searchBackwards)
                                searchIndex--;
                            else
                                searchIndex++;

                            if (searchIndex < 0 || searchIndex >= numHits)
                            {
                                //  out of bounds

                                if (searchBackwards)
                                {
                                    if (searchPage==0)
                                    {
                                        //  start of doc
                                        nextSearchPage();
                                        break;
                                    }
                                }
                                else
                                {
                                    if (searchPage==getNumPages()-1)
                                    {
                                        //  end of doc
                                        nextSearchPage();
                                        break;
                                    }
                                }

                                // go to next page
                                nextSearchPage();
                                continue;
                            }
                        }

                        searchMatchFound = true;
                        break;
                    }
                }
            }

            public void run()
            {
                if (searchCancelled)
                {
                    if (searchListener != null)
                        searchListener.cancelled();
                }

                else if (! searchMatchFound)
                {
                    if (searchListener != null)
                        searchListener.notFound();
                }

                else {
                    //  choose this one.
                    mPages.get(searchPage).setSearchIndex(searchIndex);
                    android.graphics.Rect r =
                            mPages.get(searchPage).getSearchHighlight();

                    if (searchListener != null)
                    {
                        RectF r2 = mPages.get(searchPage).toRectF(r);
                        if (searchListener!=null)
                            searchListener.found(searchPage, r2);
                    }
                }

                searchRunning = false;
            }
        });

        return 0;
    }

    @Override
    public int getNumPages() {
        return mNumPages;
    }

    @Override
    public void createInkAnnotation(int pageNum, SOPoint[] points, float width, int color)
    {
        mPages.get(pageNum).createInkAnnotation(points, width, color);
        update(pageNum);
    }

    @Override
    public void addHighlightAnnotation() {
        int pageNum = MuPDFPage.getTextSelPageNum();
        if (pageNum != -1)
        {
            mPages.get(pageNum).addHighlightAnnotation();
            update(pageNum);
        }
    }

    //  a list of page numbers that have unapplied redaction marks
    private List<Integer> pagesWithRedactions = new ArrayList<Integer>();

    private void addPageWithRedactions(int pageNum) {
        Integer thePage = new Integer(pageNum);
        if (!pagesWithRedactions.contains(thePage))
            pagesWithRedactions.add(thePage);
    }

    private void removePageWithRedactions(int pageNum) {
        Integer thePage = new Integer(pageNum);
        if (pagesWithRedactions.contains(thePage))
            pagesWithRedactions.remove(thePage);
    }

    public void addRedactAnnotation()
    {
        int pageNum = MuPDFPage.getTextSelPageNum();
        if (pageNum != -1)
        {
            mPages.get(pageNum).addRedactAnnotation();
            update(pageNum);
            addPageWithRedactions(pageNum);
        }
    }

    public void addRedactAnnotation(int pageNum, Rect r)
    {
        if (pageNum != -1)
        {
            mPages.get(pageNum).addRedactAnnotation(r);
            update(pageNum);
            addPageWithRedactions(pageNum);
        }
    }

    public boolean hasRedactionsToApply()
    {
        return !pagesWithRedactions.isEmpty();
    }

    public void applyRedactAnnotation()
    {
        int count = mPages.size();
        for (int i=0; i<count; i++)
        {
            if (mPages.get(i).countAnnotations(PDFAnnotation.TYPE_REDACT)>0)
            {
                mPages.get(i).applyRedactAnnotation();
                update(i);
            }
        }
        pagesWithRedactions.clear();
    }

    @Override
    public void deleteHighlightAnnotation() {
        //  TODO
    }

    private String mAuthor=null;
    public boolean setAuthor(String author)
    {
        mAuthor = author;
        return true;
    }

    public String getAuthor()
    {
        return mAuthor;
    }

    @Override
    public void createTextAnnotationAt(PointF point, int pageNum)
    {
        String author = getAuthor();
        mPages.get(pageNum).createTextAnnotationAt(point, author);
        update(pageNum);
    }

    @Override
    public void createSignatureAt(PointF point, final int pageNum)
    {
        mPages.get(pageNum).createSignatureAt(point);
        update(pageNum);
    }

    public void deleteWidget(int pageNum, MuPDFWidget widget)
    {
        mPages.get(pageNum).deleteWidget(widget);
        update(pageNum);
    }

    @Override
    public String getSelectionAnnotationAuthor()
    {
        PDFAnnotation annot = (PDFAnnotation)getSelectedAnnotation();
        if (annot != null)
        {
            return annot.getAuthor();
        }
        return null;
    }

    private static String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    @Override
    public String getSelectionAnnotationDate()
    {
        PDFAnnotation annot = (PDFAnnotation)getSelectedAnnotation();
        if (annot != null)
        {
            //  get the date

            try {
                Date date = annot.getModificationDate();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                return sdf.format(date);
            }
            catch (Exception e) {
            }
        }
        return null;
    }

    @Override
    public String getSelectionAnnotationComment()
    {
        PDFAnnotation annot = (PDFAnnotation)getSelectedAnnotation();
        if (annot != null)
        {
            return annot.getContents();
        }
        return null;
    }

    @Override
    public void setSelectionAnnotationComment(String comment)
    {
        PDFAnnotation annot = (PDFAnnotation)getSelectedAnnotation();
        if (annot != null)
        {
            //  if the comment is changing, also update the date.
            if (comment != null)
            {
                String previous = annot.getContents();
                if (previous != null && previous.compareTo(comment)!=0)
                {
                    annot.setContents(comment);
                    Date now = new Date();
                    annot.setModificationDate(now);
                }
                update(selectedAnnotIndex);
            }
        }
    }

    @Override
    public void setSelectedObjectBounds(RectF bounds)
    {
        PDFAnnotation annot = getSelectedAnnotation();
        if (annot != null)
        {
            com.artifex.mupdf.fitz.Rect rf = new com.artifex.mupdf.fitz.Rect(bounds.left, bounds.top, bounds.right, bounds.bottom);
            annot.setRect(rf);

            com.artifex.mupdf.fitz.Rect[] rfs = {rf};
            annot.setQuadPoints(MuPDFPage.rectsToQuads(rfs));

            Date now = new Date();
            annot.setModificationDate(now);

            update(selectedAnnotPagenum);
        }
    }

    public void update(final int pageNumber)
    {
        Worker worker = getWorker();
        worker.add(new Worker.Task()
        {
            public void work()
            {
                MuPDFPage page = mPages.get(pageNumber);
                if (page != null) {
                    page.update();
                }

            }
            public void run()
            {
                if (mListener!=null)
                {
                    //  this should cause the lib client to re-render pages.
                    mListener.onDocComplete();
                    //  this should cause the lib client to update its UI.
                    mListener.onSelectionChanged(pageNumber, pageNumber);
                }
            }
        });
    }

    public void onSelectionUpdate(final int pageNumber)
    {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                setSelectionStartPage(pageNumber);
                setSelectionEndPage(pageNumber);
                if (mListener != null)
                    mListener.onSelectionChanged(pageNumber, pageNumber);
            }
        });
    }

    @Override
    public boolean providePassword(String password)
    {
        boolean val =  mDocument.authenticatePassword(password);
        if (!val) {

            //  arrange for another password request
            if (mListener!=null)
                mListener.onError(ArDkLib.SmartOfficeDocErrorType_PasswordRequest, 0);
            return false;
        }

        //  things to do after the password is validated
        afterValidation();

        //  start loading pages
        loadNextPage();
        return true;
    }

    public void afterValidation()
    {
        //  set the page count
        mPageCount = mDocument.countPages();

        //  enable javascript
        if (mDocCfgOpts.isFormFillingEnabled())
        {
            PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
            if (pdfDoc!=null) {

                //  enable javascript and establish a listener
                pdfDoc.enableJs();
                pdfDoc.setJsEventListener(jsEventListener);
            }
        }
    }

    @Override
    public void processKeyCommand(int command) {
        //  TODO
    }

    public interface MuPDFEnumerateTocListener {
        void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y);
    }

    public int enumerateToc(MuPDFEnumerateTocListener listener)
    {
        if (listener !=null)
        {
            try
            {
                handleCounter = 0;
                Outline[] outlines = mDocument.loadOutline();
                processOutline(outlines, handleCounter, listener);
            }
            catch (Exception e)
            {
            }
        }

        return 0;
    }

    int handleCounter = 0;

    private void processOutline(Outline[] outlines, int parent, MuPDFEnumerateTocListener listener)
    {
        if (outlines!=null && outlines.length>0)
        {
            for (int i=0; i<outlines.length; i++)
            {
                Outline  outline = outlines[i];
                Location loc     = mDocument.resolveLink(outline);
                int      page    = mDocument.pageNumberFromLocation(loc);

                if (page>=0)
                {
                    MuPDFPage mppage = mPages.get(page);
                }

                handleCounter++;
                listener.nextTocEntry(handleCounter, parent, page, outline.title, outline.uri, loc.x, loc.y);

                Outline[] down = outline.down;
                processOutline(down, handleCounter, listener);
            }
        }
    }

    public void loadNextPage()
    {
        if (mLoadAborted)
        {
            if (mListener!=null)
                mListener.onDocComplete();  //  TODO: error?
            return;
        }

        getWorker().add(new Worker.Task()
        {
            private boolean done = false;
            public void work()
            {
                //  this part takes place on the background thread

                if (mPageNumber < mPageCount)
                {
                    Page page = mDocument.loadPage(mPageNumber);
                    addPage(page);

                    //  keep track of pages with un-applied redaction marks
                    int numRedact = mPages.get(mPageNumber).countAnnotations(PDFAnnotation.TYPE_REDACT);
                    if (numRedact>0)
                        addPageWithRedactions(mPageNumber);
                }
                else
                {
                    //  no more pages
                    done = true;
                }
            }

            public void run()
            {
                //  this part takes place on the main thread

                if (done)
                {
                    if (mListener!=null)
                        mListener.onDocComplete();
                }
                else
                {
                    mPageNumber++;
                    if (mListener!=null)
                        mListener.onPageLoad(mPageNumber);

                    loadNextPage();
                }
            }
        });

    }

    public interface ReloadListener {
        void onReload();
    }

    public void reloadFile(final String path, final ReloadListener listener, final boolean forced)
    {
        final MuPDFDoc thisMupdfDoc = this;
        getWorker().add(new Worker.Task()
        {
            private Document newDoc;
            private ArrayList<MuPDFPage> newPages = new ArrayList<>();

            public void work()
            {
                //  this part takes place on the background thread
                //  here we load the file and make a new page list.
                //  we'll make them active on the foreground thread

                //  load the file
                newDoc = openFile(path);

                //  build a new page list
                int count = newDoc.countPages();
                for (int i=0; i<count; i++)
                {
                    PDFPage page = (PDFPage)newDoc.loadPage(i);
                    MuPDFPage mpage;
                    if (forced)
                    {
                        //  in this case we know that the population of pages is the same,
                        //  so we can just copy the old ones and give each one a new PDFPage.
                        mpage = mPages.get(i);
                        mpage.setPage(page);
                    }
                    else
                    {
                        mpage = new MuPDFPage(thisMupdfDoc, page, i);
                    }
                    newPages.add(mpage);
                }
            }

            public void run()
            {
                //  this part takes place on the main thread

                //  replace the page list
                ArrayList<MuPDFPage> oldPages = mPages;
                mPages = newPages;

                //  replace the Document
                Document oldDoc = mDocument;
                mDocument = newDoc;

                //  clear the now-unreferenced page list
                if (!forced) {
                    for (int i=0; i<oldPages.size(); i++) {
                        MuPDFPage page = oldPages.get(i);
                        if (page!=null)
                            page.releasePage();
                    }
                }
                oldPages.clear();

                //  destroy the now-unreferenced Document
                oldDoc.destroy();

                //  tell someone
                listener.onReload();
            }
        });
    }

    public static Document openFile(String path)
    {
        Document doc = null;

        try {

            SOSecureFS mSecureFs = ArDkLib.getSecureFS();
            if (mSecureFs != null && mSecureFs.isSecurePath(path))
            {
                doc = openSecure(path, mSecureFs);
            }
            else
            {
                doc = Document.openDocument(path);
            }
        }
        catch (Exception e)
        {
            //  exception while trying to open the file
            doc = null;
        }

        return doc;
    }

    private static Document openSecure(String path, final SOSecureFS secureFS)
    {
        try
        {
            final Object handle = secureFS.getFileHandleForReading(path);

            SeekableInputStream stream = new SeekableInputStream()
            {
                public void close() throws IOException
                {
                    secureFS.closeFile(handle);
                }

                public int read(byte[] b) throws IOException
                {
                    int numBytes = secureFS.readFromFile(handle, b);
                    return (numBytes == 0) ? -1 : numBytes;
                }

                public long seek(long offset, int whence) throws IOException
                {
                    long current = secureFS.getFileOffset(handle);
                    long length = secureFS.getFileLength(handle);
                    long pos = 0;

                    switch (whence)
                    {
                        case SEEK_SET:
                            pos = offset;
                            break;

                        case SEEK_CUR:
                            pos = current+offset;
                            break;

                        case SEEK_END:
                            pos = length+offset;
                            break;
                    }
                    secureFS.seekToFileOffset(handle, pos);
                    return pos;
                }

                public long position() throws IOException
                {
                    long current = secureFS.getFileOffset(handle);
                    return current;
                }
            };

            String extension = FileUtils.getExtension(path);
            Document doc = Document.openDocument(stream, extension);

            return doc;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private boolean isNull(PDFObject obj)
    {
        return obj==null || obj.equals(PDFObject.Null);
    }

    @Override
    public boolean hasXFAForm()
    {
        //  only valid for PDF docs
        if (!(getDocument() instanceof PDFDocument))
            return false;

        PDFObject obj = ((PDFDocument)getDocument()).getTrailer();

        if (!isNull(obj))
            obj = obj.get("Root");
        if (!isNull(obj))
            obj = obj.get("AcroForm");
        if (!isNull(obj))
            obj = obj.get("XFA");

        if (isNull(obj))
            return false;

        return true;
    }

    @Override
    public boolean hasAcroForm()
    {
        //  only valid for PDF docs
        if (!(getDocument() instanceof PDFDocument))
            return false;

        PDFObject obj = ((PDFDocument)getDocument()).getTrailer();

        if (!isNull(obj))
            obj = obj.get("Root");
        if (!isNull(obj))
            obj = obj.get("AcroForm");
        if (!isNull(obj))
            obj = obj.get("Fields");

        if (isNull(obj))
            return false;

        return obj.size() > 0;
    }

    @Override
    public String getDateFormatPattern()
    {
        //  this is the date format for date strings returned from muPDF docs.
        //  its used to convert date strings to Date objects.
        return "yyyy-MM-dd HH:mm";
    }

    //  keep track of whether we should show ther XFA warning
    private boolean mShowXFAWarning = false;
    public void setShowXFAWarning(boolean val) {mShowXFAWarning = val;}
    public boolean getShowXFAWarning() {return mShowXFAWarning;}

}
