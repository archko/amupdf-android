package com.artifex.solib;

import com.artifex.mupdf.fitz.Cookie;

public class MuPDFRender extends ArDkRender
{
    private Cookie mCookie;

    //  set a cookie
    public void setCookie(Cookie cookie)
    {
        mCookie = cookie;
    }

    public void abort()
    {
        //  abort a page run that may be in progress.
        if (mCookie != null) {
            mCookie.abort();
        }
    }

    public void destroy()
    {
        mCookie = null;
    }
}
