package com.artifex.sonui.editor;

import android.graphics.RectF;

import java.util.ArrayList;

public class History
{
    public class HistoryItem
    {
        //  main doc view scroll values and scale
        private int scrollX;
        private int scrollY;
        private float scale;

        //  constructor
        public HistoryItem(int scrollX, int scrollY, float scale)
        {
            this.scrollX = scrollX;
            this.scrollY = scrollY;
            this.scale = scale;
        }

        //  accessors
        public int getScrollX() {return scrollX;}
        public int getScrollY() {return scrollY;}
        public float getScale() {return scale;}
    }

    //  list of history items
    private ArrayList<HistoryItem> mItems;

    //  position in the history list
    private int mItemIndex;

    public History()
    {
        //  create a new history list
        mItems = new ArrayList<>();
        mItemIndex = -1;
    }

    public void add(int scrollX, int scrollY, float scale)
    {
        HistoryItem item = new HistoryItem(scrollX, scrollY, scale);

        //  if we're not positioned at the end of the list,
        //  truncate it so that we are.
        if (mItemIndex+1 != mItems.size())
            mItems = new ArrayList<HistoryItem>(mItems.subList(0, mItemIndex+1));

        //  add the item to the end of the list.
        mItems.add(item);
        mItemIndex = mItems.size()-1;
    }

    public boolean canPrevious()
    {
        //  determine if we can go backwards in the list
        if (mItemIndex > 0)
            return true;
        return false;
    }

    public boolean canNext()
    {
        //  determine if we can go forwards in the list
        if (mItemIndex < mItems.size()-1)
            return true;
        return false;
    }

    public HistoryItem current()
    {
        if (mItemIndex<0)
            return null;
        HistoryItem item = mItems.get(mItemIndex);
        return item;
    }

    public HistoryItem previous()
    {
        //  return the previous history item. Can be null.
        if (canPrevious())
        {
            mItemIndex--;
            HistoryItem item = mItems.get(mItemIndex);
            return item;
        }
        return null;
    }

    public HistoryItem next()
    {
        //  return the next history item. Can be null.
        if (canNext())
        {
            mItemIndex++;
            HistoryItem item = mItems.get(mItemIndex);
            return item;
        }
        return null;
    }
}
