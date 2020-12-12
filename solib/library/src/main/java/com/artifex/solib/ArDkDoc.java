package com.artifex.solib;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;

public abstract class ArDkDoc
{
    //  processing key commands
    public static final int SmartOfficeCmd_Left = 0;
    public static final int SmartOfficeCmd_Right = 1;
    public static final int SmartOfficeCmd_Up = 2;
    public static final int SmartOfficeCmd_Down = 3;
    public static final int SmartOfficeCmd_LineStart = 4;
    public static final int SmartOfficeCmd_LineEnd = 5;
    public static final int SmartOfficeCmd_DocStart = 6;
    public static final int SmartOfficeCmd_DocEnd = 7;
    public static final int SmartOfficeCmd_SelectionCharBack = 8;
    public static final int SmartOfficeCmd_SelectionCharForward = 9;
    public static final int SmartOfficeCmd_SelectionLineStart = 10;
    public static final int SmartOfficeCmd_SelectionLineEnd = 11;
    public static final int SmartOfficeCmd_SelectionLineUp = 12;
    public static final int SmartOfficeCmd_SelectionLineDown = 13;
    public static final int SmartOfficeCmd_SelectionDocStart = 14;
    public static final int SmartOfficeCmd_SelectionDocEnd = 15;



    //  keep a counter of the number of pages, which is updated by
    //  an SODocLoadListener in SOLib.
    protected int mNumPages = 0;
    //  setNumPages must be package-private (no qualifier)
    void setNumPages(int val) {mNumPages=val;};
    public int getNumPages() {return mNumPages;}

    //  keep track of the beginning and ending pages of the
    //  current selection.
    private int mSelectionStartPage = 0;
    public void setSelectionStartPage(int n) {mSelectionStartPage=n;}
    public int getSelectionStartPage() {return mSelectionStartPage;}

    private int mSelectionEndPage = 0;
    public void setSelectionEndPage(int n) {mSelectionEndPage=n;}
    public int getSelectionEndPage() {return mSelectionEndPage;}

    //  Each SODoc keeps track of SOPages that have been created.
    //  When we're finished, destroyPages() is called to destroy them.
    //  This was moved here from DocPageView because that implementation
    //  did not handle multiple open documents.

    //  array of pages, initially empty
    private ArrayList<ArDkPage> mPages = new ArrayList<>();
    public void addPage(ArDkPage page) { mPages.add(page); }
    public void removePage(ArDkPage page) { mPages.remove(page); }

    //  destroy pages and clear the list.
    public void destroyPages() {
        //  destroy any created SOPage objects
        for (int i=0; i<mPages.size(); i++) {
            mPages.get(i).releasePage();
        }
        //  empty the list.
        mPages.clear();
    }

    public abstract boolean setAuthor(String author);
    public abstract boolean canPrint();
    public abstract void clearSelection();
    public abstract void processKeyCommand(int command);
    public abstract void setSearchListener(SOSearchListener listener);
    public abstract void setSearchMatchCase(boolean matchCase);
    public abstract void setOpenedPath(String path);
    public abstract void setForceReload(boolean force);
    public abstract void setForceReloadAtResume(boolean force);
    public abstract ArDkPage getPage(int pageNumber, SOPageListener listener);
    public abstract void saveTo(final String path, final SODocSaveListener listener);
    public abstract void saveToPDF(String path, boolean imagePerPage, SODocSaveListener listener);
    public abstract boolean getHasBeenModified();
    public abstract void abortLoad();
    public abstract void destroyDoc();
    public abstract boolean getSelectionIsAlterableTextSelection();
    public abstract boolean getSelectionHasAssociatedPopup();
    public abstract boolean getSelectionCanCreateAnnotation();
    public abstract boolean getSelectionCanBeDeleted();
    public abstract boolean getSelectionCanBeResized();
    public abstract boolean getSelectionCanBeAbsolutelyPositioned();
    public abstract boolean getSelectionCanBeRotated();
    public abstract void selectionDelete();
    public abstract void cancelSearch();
    public abstract void closeSearch();
    public abstract boolean isSearchRunning();
    public abstract void setSearchStart(int page, PointF xy);
    public abstract void setSearchStart(int page, float x, float y);
    public abstract void setSearchString(String text);
    public abstract void setSearchBackwards(boolean backwards);
    public abstract int search();
    public abstract void createInkAnnotation(int pageNum, SOPoint[] points, float width, int color);
    public abstract void addHighlightAnnotation();
    public abstract void deleteHighlightAnnotation();
    public abstract void createTextAnnotationAt(PointF point, int pageNum);
    public abstract void createSignatureAt(PointF point, int pageNum);
    public abstract String getSelectionAnnotationAuthor();
    public abstract String getSelectionAnnotationDate();
    public abstract String getSelectionAnnotationComment();
    public abstract void setSelectionAnnotationComment(String comment);
    public abstract void setSelectedObjectBounds(RectF bounds);
    public abstract boolean providePassword(String password);
    public abstract boolean hasXFAForm();
    public abstract boolean hasAcroForm();
    public abstract String getDateFormatPattern();
    public abstract String getAuthor();
    public abstract boolean canSave();
}
