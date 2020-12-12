package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Intent;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;

import com.artifex.mupdf.fitz.FitzInputStream;


import org.spongycastle.asn1.x509.Certificate;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cms.CMSProcessable;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationVerifier;
import org.spongycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.Store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;


public class NUIDefaultVerifier extends NUIPKCS7Verifier {
    private Activity mActivity;
    protected int mResult;  // a PKCS7VerifyResult_xxx value
    protected NUICertificate mCertificate;
    protected NUIPKCS7VerifierListener mListener;
    protected Class mResultViewerClass;

    private static final int BUF_SIZE = 16384;

    public NUIDefaultVerifier( Activity ctx, Class resultClass )
    {
        mActivity = ctx;
        mResult = PKCS7VerifierUnknown;
        mResultViewerClass = resultClass;
        Security.addProvider(new BouncyCastleProvider());
    }

    public boolean verifySignedData(byte[]data, byte[] sig)
            throws Exception
    {
        CMSProcessable                cmsProcessableInputStream = new CMSProcessableByteArray(data);
        CMSSignedData                 cmsSignedData             = new CMSSignedData(cmsProcessableInputStream, sig);
        Store                         certificatesStore         = cmsSignedData.getCertificates();
        Collection<SignerInformation> signers                   = cmsSignedData.getSignerInfos().getSigners();
        SignerInformation             signerInformation         = signers.iterator().next();
        Collection                    matches                   = certificatesStore.getMatches(signerInformation.getSID());
        X509CertificateHolder         certificateHolder         = (X509CertificateHolder) matches.iterator().next();
        X509Certificate signerCert = new JcaX509CertificateConverter().getCertificate(certificateHolder);

        mCertificate.fromCertificate( signerCert );

        SignerInformationVerifier signerInformationVerifier = new JcaSimpleSignerInfoVerifierBuilder()
                .build(certificateHolder);

        return signerInformation.verify(signerInformationVerifier) ;
    }


    public int checkDigest(FitzInputStream stream, byte[] sig)
    {
        int nRead;
        byte[] buf = new byte[BUF_SIZE];

        mCertificate = new NUICertificate();

        // read blocks from stream into our byte array
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((nRead = stream.read(buf, 0, BUF_SIZE)) > 0)
            output.write(buf, 0, nRead);
        byte[] out = output.toByteArray();

        // check if signature matches data
        try
        {
            mResult = PKCS7VerifierDigestFailure;

            if (verifySignedData( out, sig ))
                mResult = PKCS7VerifierOK;
        } catch ( Exception e )
        {
            e.printStackTrace();
        }

        // check signer certificate validity
        if (mResult == PKCS7VerifierOK)
            mResult = checkCertificate( sig );

        HashMap<String, String> details;

        // Get the designated name from the certificate.
        if (mCertificate != null)
            details = mCertificate.designatedName();
        else
            details = NUICertificate.defaultDetails();

        // Get the X509v3 extensions from the certificate and append.
        HashMap<String, String> mV3ExtensionsDetails;

        if (mCertificate != null)
        {
            mV3ExtensionsDetails = mCertificate.v3Extensions();

            if (mV3ExtensionsDetails != null)
            {
                details.putAll(mV3ExtensionsDetails);
            }
        }

        // Get the validity from the certificate and append.
        HashMap<String, String> validityDetails;

        if (mCertificate != null)
        {
            validityDetails = mCertificate.validity();

            if (validityDetails != null)
            {
                details.putAll(validityDetails);
            }
        }
        if (mListener != null)
            mListener.onVerifyResult(details, mResult );

        presentResults(details, mResult);

        return mResult;
    }

    private static boolean selfSigned(X509Certificate cert) throws CertificateException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch ( SignatureException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException sigEx) {
            return false;
        }
    }


    public int checkCertificate( byte[] signature )
    {
        if ( (mCertificate == null) || (mCertificate.publicKey() == null) )
            return PKCS7VerifierNoCertificate;

        try
        {
            KeyStore keyStore = KeyStore.getInstance("AndroidCAStore");
            keyStore.load(null, null); // Load default system keystore
            Enumeration<String> keyAliases = keyStore.aliases();

            while ( keyAliases.hasMoreElements() )
            {
                String          alias = keyAliases.nextElement();
                X509Certificate certificate  = ( X509Certificate ) keyStore.getCertificate( alias );

                try
                {
                    // check that the certificate is valid
                    certificate.checkValidity();

                    Log.v( "sign", "Issuer: " + certificate.getIssuerDN().getName() );
                    Log.v( "sign", "Cert: " + certificate.toString() );

                    // check our signer against this certificate's public key
                    // verify() will throw if not successful
                    mCertificate.publicKey().verify( certificate.getPublicKey() );
                    return PKCS7VerifierOK;
                }
                catch ( CertificateException | InvalidKeyException | NoSuchProviderException e )
                {
                    Log.v( "sign", "No certificate chain found for: " + alias );
                }
                catch ( SignatureException e )
                {
                    Log.v( "sign", "Invalid signature: " + alias );
                }

            }
        } catch ( CertificateException | KeyStoreException e ) {
            e.printStackTrace();
        } catch ( IOException | NoSuchAlgorithmException e ) {
            e.printStackTrace();
        }

        return PKCS7VerifierNotTrusted;
    }

    @Override
    public void certificateUpdated()
    {

    }

    public void presentResults( HashMap<String, String> details, int result )
    {
        // kick off a certificate viewer activity
        Intent intent= new Intent(mActivity, mResultViewerClass);
        intent.putExtra("certificateDetails", details);
        intent.putExtra("verifyResult", result);
        intent.putExtra("updatedSinceSigning", mSignatureValidity);
        mActivity.startActivity(intent);
    }

    // Start the verification process, passing a native bytebuffer if possible
    // to be used for the transfer of data chunks
    public void doVerify(final NUIPKCS7VerifierListener listener, int signatureValidity ) {

        mListener = listener;
        mSignatureValidity = signatureValidity;

        listener.onInitComplete();
    }

}
