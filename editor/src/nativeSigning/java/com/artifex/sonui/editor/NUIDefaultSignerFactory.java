package com.artifex.sonui.editor;

import android.app.Activity;

import com.artifex.sonui.editor.NUIDefaultSigner;
import com.artifex.sonui.editor.NUIDefaultVerifier;

public class NUIDefaultSignerFactory implements Utilities.SigningFactoryListener {
    private static NUIDefaultSignerFactory mInstance;

    // Static Methods ---------------------------------------------------------
    public static NUIDefaultSignerFactory getInstance() {
        if (mInstance == null) {
            synchronized (NUIDefaultSignerFactory.class) {
                mInstance = new NUIDefaultSignerFactory();
            }
        }
        return mInstance;
    }

    public NUIPKCS7Signer getSigner( Activity context )
    {
        NUIDefaultSigner signer = new NUIDefaultSigner( context );
        return signer;
    }

    public NUIPKCS7Verifier getVerifier( Activity context )
    {
        NUIDefaultVerifier verifier = new NUIDefaultVerifier( context, NUICertificateViewer.class );
        return verifier;
    }

}
