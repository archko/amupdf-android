package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.artifex.solib.ArDkDoc;

public class PageAdapter extends BaseAdapter {

    public static final int DOC_KIND = 1;
    public static final int PDF_KIND = 2;

    private final Context mContext;
    private ArDkDoc mDoc;
    private int mPageCount;
    private int mKind=0;
    DocViewHost mHost = null;

    public PageAdapter(Context context, DocViewHost host, int kind)
    {
        mHost = host;
        mContext = context;
        mKind = kind;
    }

    public void setDoc(ArDkDoc doc)
    {
        mDoc = doc;
    }

    @Override
    public int getCount() {return mPageCount;}
    public void setCount(int count) {mPageCount=count;}

    public Object getItem(int position) {
        return null;  //  not used
    }

    public long getItemId(int position) {
        return 0;  //  not used
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        //  make or reuse a view
        DocPageView pageView=null;

        final Activity activity = (Activity)mContext;
        if (convertView == null)
        {
            //  make a new one
            if (mKind == DOC_KIND)
                pageView = new DocPageView(activity, mDoc);
            else if (mKind == PDF_KIND)
            {
                pageView = new DocMuPdfPageView(activity, mDoc);
            }
            else {
                //  bad
            }
        }
        else
        {
            //  reuse an existing one
            pageView = (DocPageView) convertView;
        }

        if (pageView == null) {
            //  bad
        }

        //  set up the page

        //  be sure that the pages all have the same size when they sre set up.
        //  this fixes  698177 - "Weird thumbnail margin when rotated back"
        if (setupWidth==-1)
            setupWidth = mHost.getDocView().getWidth();

        pageView.setupPage(position, setupWidth, 1);

        return pageView;
    }

    private int setupWidth = -1;

}
