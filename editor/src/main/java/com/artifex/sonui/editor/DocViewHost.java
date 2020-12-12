package com.artifex.sonui.editor;

public interface DocViewHost
{
    boolean showKeyboard();
    void setCurrentPage(int pageNumber);
    DocView getDocView();
    void prefinish();
    void clickSheetButton(int index, boolean searching);
    void onShowKeyboard(boolean bShow);
    int getBorderColor();
    int getKeyboardHeight();
    void layoutNow();
    void selectionupdated();
    void updateUI();
    void reportViewChanges();
}
