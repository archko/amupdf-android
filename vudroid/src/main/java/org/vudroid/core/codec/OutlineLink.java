package org.vudroid.core.codec;

import android.graphics.RectF;

import java.io.Serializable;

public class OutlineLink implements CharSequence, Serializable {

    public final String title;
    public final int level;

    public String targetUrl;
    public int targetPage = -1;
    public RectF targetRect;

    public OutlineLink(final String title, final int page, final int level) {
        this.title = title;
        this.targetPage = page;
        this.level = level;
    }

    public OutlineLink(final String title, final String link, final int level) {
        this.title = title;
        this.level = level;

        if (link != null) {
            if (link.startsWith("#")) {
                try {   //djvu
                    targetPage = Integer.parseInt(link.substring(1).replace(" ", ""));
                } catch (final Exception e) {
                    //mupdf
                    targetUrl = link;
                }
            } else if (link.startsWith("http:")) {
                targetUrl = link;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see CharSequence#charAt(int)
     */
    @Override
    public char charAt(final int index) {
        return title.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @see CharSequence#length()
     */
    @Override
    public int length() {
        return title.length();
    }

    /**
     * {@inheritDoc}
     *
     * @see CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return title.subSequence(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return title;
    }
}
