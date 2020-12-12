package com.artifex.sonui.editor;


import com.artifex.sonui.editor.NUICertificate;

public abstract class NUICertificateStore {

    // Perform any required initialisation
    abstract public void initialise();

    // Retrieve list of certificates
    abstract public NUICertificate[] getAllCertificates();

    // Retrieve list of certificates
    abstract public NUICertificate[] getSigningCertificates();

    // Retrieve list of auxiliary certificates
    abstract public NUICertificate[] getAuxCertificates( NUICertificate cert );

}
