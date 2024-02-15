package org.vudroid.core.link;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;

import org.vudroid.core.Page;

public class Hyperlink {

    public static int LINKTYPE_PAGE = 0;
    public static int LINKTYPE_URL = 1;

    public static Hyperlink mapPointToPage(Page page, float atX, float atY) {
        if (null == page.links) {
            return null;
        }
        for (Hyperlink hyper : page.links) {
            if (null != hyper.bbox && hyper.bbox.contains((int) atX, (int) atY)) {
                return hyper;
            }
        }
        return null;
    }

    public static void openSystemBrowser(Context context, String url) {
        if (context != null && !TextUtils.isEmpty(url)) {
            try {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.parse(url);
                intent.setData(uri);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int linkType = LINKTYPE_PAGE;
    public String url;
    public int page = 0;
    public Rect bbox = null;

    @Override
    public String toString() {
        return "Hyperlink{" +
                "linkType=" + linkType +
                ", page=" + page +
                ", bbox=" + bbox +
                ", url='" + url + '\'' +
                '}';
    }
}