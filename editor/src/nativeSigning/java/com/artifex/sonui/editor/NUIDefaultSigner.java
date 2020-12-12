package com.artifex.sonui.editor;

import android.app.Activity;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Log;

import com.artifex.mupdf.fitz.FitzInputStream;
import com.artifex.mupdf.fitz.PKCS7DesignatedName;

import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.cert.jcajce.JcaCertStore;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.CMSTypedData;
import org.spongycastle.cms.SignerInfoGenerator;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.util.Store;

import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;


public class NUIDefaultSigner extends NUIPKCS7Signer {

    private Activity mActivity;
    protected String mAlias;
    protected NUICertificate mCert;
    protected PKCS7DesignatedName mDesignatedName;

    private static final int BUF_SIZE = 16384;

    /**
     * NUI default signer implementation
     * @param ctx
     */
    public NUIDefaultSigner( Activity ctx )
    {
        mActivity = ctx;
        Security.addProvider(new BouncyCastleProvider());
    }

    // Get the signers designated name
    @Override
    public PKCS7DesignatedName name()
    {
        return mDesignatedName;
    }

    public static byte[] signData(
            byte[] data,
            X509Certificate signingCertificate,
            PrivateKey signingKey) throws Exception {

        List<X509Certificate> certList      = new ArrayList<>();
        CMSTypedData          cmsData       = new CMSProcessableByteArray(data);
        certList.add(signingCertificate);
        Store certs = new JcaCertStore(certList);

        CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
        ContentSigner contentSigner
                = new JcaContentSignerBuilder("SHA256withRSA").build(signingKey);
        DigestCalculatorProvider digestCalculator = new JcaDigestCalculatorProviderBuilder()
                .build();
        SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(
                digestCalculator )
                .setDirectSignature( true )
                .build(contentSigner, signingCertificate);
        cmsGenerator.addSignerInfoGenerator(signerInfoGenerator);
        cmsGenerator.addCertificates(certs);

        CMSSignedData cms = cmsGenerator.generate(cmsData, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DEROutputStream dos = new DEROutputStream(baos);
        dos.writeObject(cms.toASN1Structure());

        return baos.toByteArray();
    }

    @Override
    public byte[] sign( FitzInputStream stm )
    {
        int nRead = -1;
        byte[] buf = new byte[BUF_SIZE];
        byte[] digitalSignature = null;

        // read blocks from stream into our byte array
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((nRead = stm.read(buf, 0, BUF_SIZE)) > 0)
            output.write(buf, 0, nRead);
        byte[] out = output.toByteArray();

        // sign the stream
        PrivateKey privKey  = mCert.privateKey();
        X509Certificate publicKey  = mCert.publicKey();
        try
        {
            digitalSignature = signData( out, publicKey, privKey );
        } catch ( Exception e )
        {
            e.printStackTrace();
        }

        return digitalSignature;
    }

    @Override
    public int maxDigest()
    {
        return 7 * 1024;
    }

    private void chooseSignature( final NUIPKCS7SignerListener listener )
    {
        KeyChain.choosePrivateKeyAlias(
                mActivity,
                new KeyChainAliasCallback()
                {
                    public void alias(String alias)
                    {
                        // read the DN for the certificate
                        mCert = new NUICertificate();
                        if ( (alias != null) && mCert.fromAlias( mActivity, alias ))
                        {
                            mAlias = alias;
                            mDesignatedName = mCert.pkcs7DesignatedName();
                            listener.onSignatureReady();
                        }
                        else
                        {
                            mCert = null;
                            mAlias = null;
                            listener.onCancel();

                            // notify the user that no certificates are installed and that
                            // they need to install an identity before they can use signing
                            Utilities.showMessage( mActivity,
                                    mActivity.getString(R.string.sodk_editor_certificate_sign),
                                    mActivity.getString(R.string.sodk_editor_certificate_no_identities));
                        }
                    }
                },
                null, // List of acceptable key types. null for any
                null,                        // issuer, null for any
                null,                        // host name of server requesting the cert, null if unavailable
                -1,                          // port of server requesting the cert, -1 if unavailable
                "");                         // alias to preselect, null if unavailable
    }


    // Start the signing process, passing a native bytebuffer if possible
    // to be used for the transfer of data chunks
    public void doSign(final NUIPKCS7SignerListener listener )
    {
        if (mAlias == null)
        {
            chooseSignature( listener );
        }
        else
        {
            // we already selected a certificate - use that.
            listener.onSignatureReady();
        }

        return;
    }
}
