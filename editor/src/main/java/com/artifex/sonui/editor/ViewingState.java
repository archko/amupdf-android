package com.artifex.sonui.editor;

//  this is a class to hold information about a document's current viewing state.
//
//  pageNumber = 0-based page number to start at
//  scale - initial scale factor
//  scrollX - initial X scroll position
//  scrollY - initial Y scroll position
//  pageListVisible - whether to show the page list initially

import android.os.Parcel;
import android.os.Parcelable;

public class ViewingState
{
    public int pageNumber = 0;
    public float scale = 1.0f;
    public int scrollX = 0;
    public int scrollY = 0;
    public boolean pageListVisible = false;

    public ViewingState()
    {
    }

    public ViewingState(int pageNumber)
    {
        this.pageNumber = pageNumber;
    }

    public ViewingState(SOFileState state)
    {
        this.pageNumber = state.getPageNumber();
        this.scale = state.getScale();
        this.scrollX = state.getScrollX();
        this.scrollY = state.getScrollY();
        this.pageListVisible = state.getPageListVisible();
    }

    protected ViewingState(Parcel in) {
        pageNumber = in.readInt();
        scale = in.readFloat();
        scrollX = in.readInt();
        scrollY = in.readInt();
        pageListVisible = in.readByte() != 0;
    }
}
