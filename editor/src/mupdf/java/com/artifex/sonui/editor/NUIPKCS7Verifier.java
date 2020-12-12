package com.artifex.sonui.editor;

import com.artifex.mupdf.fitz.PDFWidget;
import com.artifex.mupdf.fitz.PKCS7Verifier;

import java.util.HashMap;
import java.util.Map;

public abstract class NUIPKCS7Verifier extends PKCS7Verifier {
    protected int mSignatureValidity = 0;

    public abstract void certificateUpdated();
    public abstract void presentResults( HashMap<String, String> designatedName, int result );
    public abstract void doVerify( final NUIPKCS7VerifierListener listener, int signatureValidity );

    public interface NUIPKCS7VerifierListener
    {
        void onInitComplete();
        void onVerifyResult(Map<String, String> designatedName, int result);
    }
}
