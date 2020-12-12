package com.artifex.sonui.editor;

import android.content.Context;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;

import com.artifex.mupdf.fitz.PKCS7DesignatedName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class NUICertificate {

    public String alias;
    public String issuer;
    public String subject;
    public String subjectAlt;
    public String serial;
    public boolean isValid;
    public String notAfter;
    public String usage;
    public String extendedUsage;

    private HashMap<String, String> details = null;
    private HashMap<String, String> subjDict = null;
    private HashMap<String, String> subjAltDict = null;
    private boolean[] keyUsage = null;
    private List<String> extKeyUsage = null;
    private PrivateKey privKey = null;
    private X509Certificate publicCert = null;

    private final static String TAG = "NUICertificate";

    public final static int digitalSignature    = 0;
    public final static int nonRepudiation      = 1;
    public final static int keyEncipherment     = 2;
    public final static int dataEncipherment    = 3;
    public final static int keyAgreement        = 4;
    public final static int keyCertSign         = 5;
    public final static int cRLSign             = 6;
    public final static int encipherOnly        = 7;
    public final static int decipherOnly        = 8;

    public NUICertificate()
    {
        init();
    }

    PrivateKey privateKey() { return privKey; }
    X509Certificate publicKey() { return publicCert; }

    void init()
    {
        alias = null;
        issuer = null;
        subject = null;
        subjectAlt = null;
        serial = null;
        isValid = false;
        notAfter = null;
        usage = null;
        extendedUsage = null;

        details = null;
        subjDict = null;
        subjAltDict = null;
    }

    boolean fromASN1( byte[] bytes )
    {
        CertificateFactory certFactory = null;
        try
        {
            certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(bytes);
            X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);
            return fromCertificate( cert );

        } catch ( CertificateException e )
        {
            e.printStackTrace();
        }
        return false;
    }

    boolean fromCertificate(X509Certificate certificate)
    {
        boolean parsed = false;

        try
        {

            // get the designated name
            subject = certificate.getSubjectDN().getName();
            Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
            subjectAlt = (subjectAltNames == null) ? "" : subjectAltNames.toString();
            keyUsage = certificate.getKeyUsage();
            extKeyUsage = certificate.getExtendedKeyUsage();

            publicCert = certificate;
            parsed = true;
        } catch ( CertificateException e ) {
            Log.e(TAG, "Failed to parse certificate");
            init();
        }

        return parsed;
    }

    boolean fromAlias( Context ctx, String alias )
    {
        boolean parsed = false;

        init();

        X509Certificate[] chain = null;
        try
        {
            chain = KeyChain.getCertificateChain(ctx, alias);
        } catch ( InterruptedException e ) {
            // do nothing - we'll fall through and exit
        } catch ( KeyChainException e ) {
            // do nothing - we'll fall through and exit
        }

        if (chain != null && chain.length != 0)
        {
            X509Certificate certificate = chain[0];
            parsed = fromCertificate( certificate );
        }

        // now try to get a private key for the alias
        try
        {
            privKey = KeyChain.getPrivateKey(ctx, alias);
        } catch ( InterruptedException e )
        {
            // do nothing - we'll fall through and exit
        } catch ( KeyChainException e )
        {
            // do nothing - we'll fall through and exit
        }

        return parsed;
    }


    public static HashMap<String, String> defaultDetails()
    {
        HashMap<String, String> details = new HashMap<>();
        String value = " - ";

        // get the designated name from the signing cert
        details.put("EMAIL", value);
        details.put("CN", value);
        details.put("O", value);
        details.put("OU", value);
        details.put("C", value);
        details.put("EMAIL", value);

        // get the X509v3 extensions fromn the signing cert
        details.put("KEYUSAGE", value);
        details.put("EXTENDEDKEYUSAGE", value);

        // get the expiry date of the certificate
        details.put("NOTAFTER", value);

        return details;
    }

    HashMap<String, String> parseX509field( String subject, String separator)
    {
        HashMap<String, String> dict = new HashMap<>();

        if( subject != null)
        {
            String[] pairs = subject.split(",");

            for( String entry : pairs )
            {
                String pair = entry.replaceAll("[\\n\\t ]", "");

                if (pair.length() != 0)
                {
                    String[] elements = pair.split(separator);
                    if (elements.length > 1)
                    {
                        String key = elements[ 0 ].trim();
                        String val = elements[ 1 ].trim();

                        dict.put( key, val );
                    }
                }
            }

        }

        return dict;
    }

    HashMap<String, String> parseLDAPfield( String ldapSubject, String separator)
    {
        HashMap<String, String> dict = new HashMap<>();

        if( ldapSubject != null)
        {
            // handle LDAP multi-value RDNs in the ldapSubject string
            String regex = "([^\\\\])([+])";
            String x509subject = ldapSubject.replaceAll(regex, "$1,");
            String[] pairs = x509subject.split(",");

            for( String entry : pairs )
            {
                String pair = entry.replaceAll("[\\n\\t ]", "");

                if (pair.length() != 0)
                {
                    String[] elements = pair.split(separator);
                    if (elements.length > 1)
                    {
                        String key = elements[ 0 ].trim();
                        String val = elements[ 1 ].trim();

                        dict.put( key, val );
                    }
                }
            }

        }

        return dict;
    }


    public PKCS7DesignatedName pkcs7DesignatedName()
    {
        PKCS7DesignatedName name = new PKCS7DesignatedName();

        HashMap<String, String> detailsMap = designatedName();

        name.c = detailsMap.get("C");
        name.cn = detailsMap.get("CN");
        name.o = detailsMap.get("O");
        name.ou = detailsMap.get("OU");
        name.email = detailsMap.get("EMAIL");

        return name;
    }

    // Get the certificate's designated name
    // returns a map of the certificate details, in the form:
    //   { {"O","Artifex"}, {"OU","Engineering"}, ... }

    public HashMap<String, String> designatedName()
    {
        if (details != null )
            return details;

        String value;
        HashMap<String, String> subjDict = parseLDAPfield (subject, "=");
        HashMap<String, String> subjAltDict = parseLDAPfield (subjectAlt, ":");

        details = new HashMap<>();

        // get the designated name from the signing cert
        if(subjAltDict != null )
        {
            value = subjAltDict.get("email");
            if (value != null)
                details.put("EMAIL", value);
        }

        if(subjDict != null)
        {
            value = subjDict.get("CN");
            if (value != null)
                details.put("CN", value);

            value = subjDict.get("O");
            if (value != null)
                details.put("O", value);

            value = subjDict.get("OU");
            if (value != null)
                details.put("OU", value);

            value = subjDict.get("C");
            if (value != null)
                details.put("C", value);

            value = subjDict.get("emailAddress");
            if (value != null)
                details.put("EMAIL", value);
        }

        return details;
    }

    public HashMap<String, String> v3Extensions()
    {
        HashMap<String, String> v3ExtensionsDetails = new HashMap<>();

        if (usage != null)
        {
            v3ExtensionsDetails.put("KEYUSAGE", usage);
        }

        if (extendedUsage != null)
        {
            v3ExtensionsDetails.put("EXTENDEDKEYUSAGE", extendedUsage);
        }

        return v3ExtensionsDetails;
    }

    public HashMap<String, String> validity()
    {
        HashMap<String, String> validityDetails = new HashMap<>();

        if (notAfter != null)
        {
            validityDetails.put("NOTAFTER", notAfter);
        }

        return validityDetails;
    }
}
