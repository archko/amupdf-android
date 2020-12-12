package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SOLinkData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class TocDialog implements PopupWindow.OnDismissListener
{
    //  these are set in the constructor.
    private final Context mContext;
    private final View mAnchor;
    private final ArDkDoc mDoc;
    private final TocDialogListener mListener;
    private Button mCancelButton;

    //  only one dialog at a time please.
    private static TocDialog singleton = null;

    //  a popup window for us.
    private NUIPopupWindow popupWindow;

    //  constructor
    TocDialog(Context context, ArDkDoc doc, View anchor, TocDialogListener listener)
    {
        mContext = context;
        mAnchor = anchor;
        mDoc = doc;
        mListener = listener;
    }

    //  time to show the dialog
    public void show()
    {
        //  one at a time please.
        //  calling dismiss() here fixes https://bugs.ghostscript.com/show_bug.cgi?id=701968
        //  previously we would returfn, leaving a stray static singleton
        if (singleton != null)
            singleton.dismiss();
        singleton = this;

        //  get the layout
        View popupView = LayoutInflater.from(mContext).inflate(R.layout.sodk_editor_table_of_contents, null);

        //  create the list and adapter
        final ListView list = (ListView)popupView.findViewById(R.id.List);
        final  TocListViewAdapter adapter = new TocListViewAdapter(mContext);
        list.setAdapter(adapter);

        //  populate the list
        ArDkLib.enumeratePdfToc(mDoc, new ArDkLib.EnumeratePdfTocListener() {
            @Override
            public void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y)
            {
                //  add to the list
                adapter.addItem(new TocData(handle, parentHandle, page, label, url, x, y));
            }
        });

        //  establish a handler for when an item is chosen
        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //  get the page and box
                TocData data = (TocData)adapter.getItem(position);

                if (data.page>=0)
                {
                    RectF r = new RectF(data.x, data.y, data.x+1, data.y+1);
                    SOLinkData linkData = new SOLinkData(data.page, r);
                    if (Utilities.isPhoneDevice(mContext))
                        dismiss();
                    mListener.onItem(linkData);
                }
            }
        });

        //  dismiss the popup with the cancel button
        mCancelButton = (Button)popupView.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //  put everything in a popup window and show it
        popupWindow = new NUIPopupWindow(popupView);
        popupWindow.setFocusable(true);
        popupWindow.setClippingEnabled(false);
        popupWindow.setOnDismissListener(this);
        showOrResize();
    }

    public static void onRotate()
    {
        try
        {
            //  https://bugs.ghostscript.com/show_bug.cgi?id=701923
            //  the popup window may become disconnected from the window manager
            //  which will cause an exception here.
            if (singleton != null) {
                singleton.showOrResize();
            }
        }
        catch (Exception e)
        {
            singleton = null;
        }
    }

    void showOrResize()
    {
        int w, h;
        int offsetx, offsety;
        Point size = Utilities.getScreenSize(mContext);
        if (Utilities.isPhoneDevice(mContext))
        {
            w = size.x;
            h = size.y;
            offsetx = 0;
            offsety = 0;
            mCancelButton.setVisibility(View.VISIBLE);

            popupWindow.setBackgroundDrawable(null);
        }
        else
        {
            offsetx = (int) mContext.getResources().getDimension(R.dimen.sodk_editor_toc_offsetx);
            offsety = (int) mContext.getResources().getDimension(R.dimen.sodk_editor_toc_offsety);
            w = size.x/2 - offsetx;
            h = size.y/2;
            mCancelButton.setVisibility(View.GONE);
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(mContext, R.drawable.sodk_editor_table_of_contents));
        }

        if (popupWindow.isShowing())
        {
            popupWindow.update(offsetx, offsety, w, h);
        }
        else
        {
            popupWindow.setWidth(w);
            popupWindow.setHeight(h);
            popupWindow.showAtLocation(mAnchor, Gravity.NO_GRAVITY, offsetx, offsety);
        }
    }

    //  internal function to dismiss the popup.
    private void dismiss()
    {
        popupWindow.dismiss();
        singleton = null;
    }

    //  this function is called when the user taps outside the popup.
    //  we make sure to dismiss it properly.
    @Override
    public void onDismiss()
    {
        dismiss();
    }

    //  a class for returning data to the caller.

    private class TocData {
        int handle;
        int parentHandle;
        String label;
        String url;
        int level;
        float x;
        float y;
        int page;
        private TocData(int handle, int parentHandle, int page, String label, String url, float x, float y) {
            this.handle = handle;
            this.parentHandle = parentHandle;
            this.label = label;
            this.url = url;
            this.x = x;
            this.y = y;
            this.page = page;
        }
    }

    //  an interface for retutning data to the caller.

    interface TocDialogListener {
        void onItem(SOLinkData linkData);
    }

    //  this is the adapter class for TOC.

    private class TocListViewAdapter extends BaseAdapter
    {
        private ArrayList<TocData> listEntries = new ArrayList<>();
        private Map<Integer,TocData> mapEntries = new HashMap<>();

        void addItem(TocData item)
        {
            //  map it
            mapEntries.put(item.handle, item);

            //  calculate this item's level
            int level = 0;
            TocData it = item;
            while (it != null && it.parentHandle != 0)
            {
                level ++;
                it = mapEntries.get(it.parentHandle);
            }
            item.level = level;

            //  list it
            listEntries.add(item);
        }

        public void clear()
        {
            //  clear both the list and the map
            mapEntries.clear();
            listEntries.clear();
        }

        private Context mContext;
        TocListViewAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return listEntries.size();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            //  get the entry
            TocData entry = listEntries.get(position);

            //  create the UI
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            View row = inflater.inflate(R.layout.sodk_editor_toc_list_item, parent, false);

            //  set the text label
            SOTextView tv = (SOTextView) row.findViewById(R.id.toc_item);
            tv.setText(entry.label);

            //  indent based on the level
            int indent = Utilities.convertDpToPixel(40) * (entry.level);
            tv.setPadding(tv.getPaddingLeft()+indent, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());

            return row;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return listEntries.get(position);
        }
    }
}
