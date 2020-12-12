package com.artifex.sonui.editor;

import android.graphics.Rect;

//  this interface handles notifications for interesting document events.
public interface DocumentListener {

    //  called when another page is loaded from the document.
    void onPageLoaded(int pagesLoaded);

    //  called when the document is done loading.
    void onDocCompleted();

    //  called when a password is required.
    void onPasswordRequired();

    //  called when the scale, scroll, or selection in the document changes.
    void onViewChanged(float scale, int scrollX, int scrollY, Rect selectionRect);
}
