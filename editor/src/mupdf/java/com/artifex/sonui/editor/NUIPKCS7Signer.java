package com.artifex.sonui.editor;

import com.artifex.mupdf.fitz.PKCS7Signer;

public abstract class NUIPKCS7Signer extends PKCS7Signer {

    public abstract void doSign( final NUIPKCS7SignerListener listener );

    public interface NUIPKCS7SignerListener
    {
        void onSignatureReady();
        void onCancel();
    }
}
