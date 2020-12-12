package com.artifex.solib;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

public class SODoc extends ArDkDoc {

    @Override
    public boolean setAuthor(String author) {
        return false;
    }

    @Override
    public boolean canPrint() {
        return false;
    }

    @Override
    public void clearSelection() { }

    @Override
    public void processKeyCommand(int command) { }

    @Override
    public void setSearchListener(SOSearchListener listener) { }

    @Override
    public void setSearchMatchCase(boolean matchCase) { }

    @Override
    public void setOpenedPath(String path) { }

    @Override
    public void setForceReload(boolean force) { }

    @Override
    public void setForceReloadAtResume(boolean force) { }

    @Override
    public ArDkPage getPage(int pageNumber, SOPageListener listener) {
        return null;
    }

    @Override
    public void saveTo(String path, SODocSaveListener listener) { }

    @Override
    public void saveToPDF(String path, boolean imagePerPage, SODocSaveListener listener) { }

    @Override
    public boolean getHasBeenModified() {
        return false;
    }

    @Override
    public void abortLoad() { }

    @Override
    public void destroyDoc() { }

    @Override
    public boolean getSelectionIsAlterableTextSelection() {
        return false;
    }

    @Override
    public boolean getSelectionHasAssociatedPopup() {
        return false;
    }

    @Override
    public boolean getSelectionCanCreateAnnotation() {
        return false;
    }

    @Override
    public boolean getSelectionCanBeDeleted() {
        return false;
    }

    @Override
    public boolean getSelectionCanBeResized() {
        return false;
    }

    @Override
    public boolean getSelectionCanBeAbsolutelyPositioned() {
        return false;
    }

    @Override
    public boolean getSelectionCanBeRotated() {
        return false;
    }

    @Override
    public void selectionDelete() { }

    @Override
    public void cancelSearch() { }

    @Override
    public void closeSearch() { }

    @Override
    public boolean isSearchRunning() {
        return false;
    }

    @Override
    public void setSearchStart(int page, PointF xy) { }

    @Override
    public void setSearchStart(int page, float x, float y) { }

    @Override
    public void setSearchString(String text) { }

    @Override
    public void setSearchBackwards(boolean backwards) { }

    @Override
    public int search() {
        return 0;
    }

    @Override
    public void createInkAnnotation(int pageNum, SOPoint[] points, float width, int color) { }

    @Override
    public void addHighlightAnnotation() { }

    @Override
    public void deleteHighlightAnnotation() { }

    @Override
    public void createTextAnnotationAt(PointF point, int pageNum) {
    }

    @Override
    public String getSelectionAnnotationAuthor() {
        return null;
    }

    @Override
    public String getSelectionAnnotationDate() {
        return null;
    }

    @Override
    public String getSelectionAnnotationComment() {
        return null;
    }

    @Override
    public void setSelectionAnnotationComment(String comment) { }

    @Override
    public void setSelectedObjectBounds(RectF bounds) { }

    @Override
    public boolean providePassword(String password) {
        return false;
    }

    @Override
    public boolean hasXFAForm() {
        return false;
    }

    @Override
    public boolean hasAcroForm() {
        return false;
    }

    @Override
    public String getDateFormatPattern() {
        return null;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    public int getCurrentEdit() {return 0;}
    public int getNumEdits() {return 0;}
    public void setCurrentEdit(int n) {}

    //  Reflow mode
    public static final int FLOW_MODE_NORMAL = 1;
    public static final int FLOW_MODE_REFLOW = 2;
    public static final int FLOW_MODE_RESIZE = 3;
    private int mFlowMode = FLOW_MODE_NORMAL;
    public int getFlowMode() {return mFlowMode;}
    public void setFlowMode(int mode, float width, float height) {
        mFlowMode = mode;  //  keep track of the mode
    }

    public boolean getSelectionCanBeCopied() {return false;}

    public void onDeleteKey() {}
    public void onForwardDeleteKey() {}
    public void setSelectionText(String s) {}

    public static final int SmartOfficeTrackedChangeType_NoChange = 0;
    public static final int SmartOfficeTrackedChangeType_DeletedText = 5;
    public static final int SmartOfficeTrackedChangeType_InsertedText = 15;
    public static final int SmartOfficeTrackedChangeType_InsertedParagraph = 16;
    public static final int SmartOfficeTrackedChangeType_InsertedTableCell = 17;
    public static final int SmartOfficeTrackedChangeType_InsertedTableRow = 18;
    public static final int SmartOfficeTrackedChangeType_ChangedParagraphProperties = 24;
    public static final int SmartOfficeTrackedChangeType_ChangedRunProperties = 26;
    public static final int SmartOfficeTrackedChangeType_ChangedSectionProperties = 28;
    public static final int SmartOfficeTrackedChangeType_ChangedTableRowProperties = 36;
    public static final int SmartOfficeTrackedChangeType_ChangedTableCellProperties = 31;
    public static final int SmartOfficeTrackedChangeType_ChangedTableProperties = 33;
    public static final int SmartOfficeTrackedChangeType_ChangedTableGrid = 32;

    public boolean docSupportsReview() {return false;}

    public boolean selectionIsReviewable(){return false;}

    public String getSelectedTrackedChangeAuthor() {return null;}
    public String getSelectedTrackedChangeDate() {return null;}
    public String getSelectedTrackedChangeComment() {return null;}
    public int getSelectedTrackedChangeType() {return 0;}

    public boolean selectionIsAutoshapeOrImage() {return false;}

    public ArDkBitmap getSelectionAsBitmap() {return null;}
    public PointF getSelectionNaturalDimensions() {return null;}

    //  rotation
    public float getSelectionRotation() {return 0;}
    public void setSelectionRotation(float angle) {}

    //  line width and type
    public float getSelectionLineWidth() {return 0;}
    public void setSelectionLineWidth(float width) {}
    public int getSelectionLineType() {return 0;}
    public void setSelectionLineType(int type) {}

    //  pages
    public void addBlankPage(int pageNumber) {}
    public void deletePage(int pageNumber) {}
    public void movePage(int pageNumber, int newNumber) {}
    public void duplicatePage(int pageNumber) {}

    /**
     * Selection context data.
     */
    public class SOSelectionContext
    {
        public String text;     // Contextual text
        public int    start;    // Selection index
        public int    length;   // Selection length
    }
    public SOSelectionContext getSelectionContext() {return null;}

    public void adjustSelection(int startOffset,
                                       int endOffset,
                                       int updateHighlight) {}

    public String[] getBgColorList() {return null;}

    public static final int SOTextAlign_Left = 0;
    public static final int SOTextAlign_Center = 1;
    public static final int SOTextAlign_Right = 2;
    public static final int SOTextAlign_Justify = 3;

    public static final int SOTextAlignV_Top = 0;
    public static final int SOTextAlignV_Center = 1;
    public static final int SOTextAlignV_Bottom = 2;

    public void setSelectionAlignment(int alignment) {}

    public String getSelectionFontName() {return null;}

    public String getSelectionAsText() {return null;}

    public void createSignatureAt(PointF point, int pageNum) {}

}
