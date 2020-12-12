package com.artifex.sonui.editor;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.artifex.mupdf.fitz.PKCS7DesignatedName;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class NUICertificatePicker extends AppCompatActivity implements NUICertificateAdapter.ItemClickListener {

    private ConfigOptions mAppCfgOptions = ArDkLib.getAppConfigOptions();

    protected NUICertificateAdapter   adapter;
    protected Button                  mSignButton;
    protected HashMap<String, String> mDetails;
    protected HashMap<String, String> mV3ExtensionsDetails;
    protected HashMap<String, String> mValidityDetails;
    protected PKCS7DesignatedName     mPKCS7DesignatedName;

    protected static NUICertificatePickerListener mListener;

    public final static String CERTIFICATE_SERIAL = "cert_serial";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the result to null initially, to indicate no signature selected
        setContentView(R.layout.sodk_editor_certificate_pick);

        mDetails             = null;
        mV3ExtensionsDetails = null;
        mValidityDetails     = null;

        mSignButton = findViewById(R.id.sodk_editor_choose_signature);
        mSignButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int pos = adapter.getSelectedPos();

                if (pos != RecyclerView.NO_POSITION && pos < adapter.getItemCount()) {
                    Intent         result = new Intent();
                    NUICertificate cert   = adapter.getItem(pos);

                    if (cert != null) {
                        if (mListener != null) {
                            mListener.onOK(cert.serial, mPKCS7DesignatedName);
                            finish();
                        }
                    }
                }
            }
        });
    }

    private ArrayList<NUICertificate> filterCertificates(NUICertificate[] certs)
    {
        ArrayList<NUICertificate> list = new ArrayList<NUICertificate>();

        for (NUICertificate cert : certs)
        {
            if (mAppCfgOptions.isNonRepudiationCertFilterEnabled())
            {
                /*
                 * Filter out certificates that do not have 'non-repudiation"
                 * set.
                 */
                HashMap<String, String> v3ExtensionsDetails =
                    cert.v3Extensions();

                String keyUsage = v3ExtensionsDetails.get("KEYUSAGE");

                if (keyUsage != null && keyUsage.contains("Non Repudiation"))
                {
                    list.add(cert);
                }

                continue;
            }

            // No filtering. Retain all certificates.
            list.add(cert);
        }

        return list;
    }

    protected void populate( NUICertificateStore store) {

        // data to populate the RecyclerView with
        ArrayList<NUICertificate> certificateList =
            filterCertificates(store.getSigningCertificates());

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.certificate_view);
        LinearLayoutManager horizontalLayoutManager
                = new LinearLayoutManager(NUICertificatePicker.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(horizontalLayoutManager);
        adapter = new NUICertificateAdapter(this, certificateList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        // if we have certificates, hide the 'no certificates' item,
        // otherwise hide the certificate details
        LinearLayout layoutView;
        if(adapter.getItemCount() == 0)
            layoutView = findViewById(R.id.sodk_editor_certificate_list);
        else
            layoutView = findViewById(R.id.sodk_editor_certificate_none);

        if (layoutView != null)
            layoutView.setVisibility(View.GONE);

        if (adapter.getItemCount() > 0)
            refreshDetails(0);
    }

    @Override
    public void onItemClick(View view, int position) {
        // get the certificate info for the 'selected' cert
        refreshDetails(position);
    }

    private void refreshDetails(int position) {
        // get the certificate info for the 'selected' cert
        NUICertificate cert = adapter.getItem(position);
        if (cert != null)
        {
            SOTextView CN = findViewById(R.id.certificate_cn);
            SOTextView C = findViewById(R.id.certificate_c);
            SOTextView OU = findViewById(R.id.certificate_ou);
            SOTextView O = findViewById(R.id.certificate_o);
            SOTextView EMAIL = findViewById(R.id.certificate_email);

            mDetails = cert.designatedName();
            if (mDetails != null) {
                CN.setText(mDetails.get("CN"));
                C.setText(mDetails.get("C"));
                OU.setText(mDetails.get("OU"));
                O.setText(mDetails.get("O"));
                EMAIL.setText(mDetails.get("EMAIL"));
            }

            SOTextView KeyUsage = findViewById(R.id.certificate_keyusage);
            SOTextView ExtendedKeyUsage = findViewById(R.id.certificate_extended_keyusage);
            mV3ExtensionsDetails = cert.v3Extensions();
            if (mV3ExtensionsDetails != null) {
                if (mV3ExtensionsDetails.containsKey("KEYUSAGE"))
                {
                    KeyUsage.setText(mV3ExtensionsDetails.get("KEYUSAGE"));
                }

                if (mV3ExtensionsDetails.containsKey("EXTENDEDKEYUSAGE"))
                {
                    ExtendedKeyUsage.setText(mV3ExtensionsDetails.get(
                                                          "EXTENDEDKEYUSAGE"));
                }
            }

            SOTextView Expiry = findViewById(R.id.certificate_expiry);
            mValidityDetails = cert.validity();
            if (mValidityDetails != null)
            {
                if (mValidityDetails.containsKey("NOTAFTER"))
                {
                    int seconds = Integer.valueOf(mValidityDetails.get("NOTAFTER"));
                    SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm");
                    String dateString = formatter.format(new Date(seconds * 1000L));

                    Expiry.setText(dateString);
                }
            }

            mPKCS7DesignatedName = cert.pkcs7DesignatedName();
        }

        if (adapter.getItemCount() > 0) {
            LinearLayout layoutView = findViewById(R.id.sodk_editor_certificate_list);
            if (layoutView != null) {
                adapter.selectItem(position);
                layoutView.invalidate();
                layoutView.requestLayout();
            }
        }

        // enable the sign button only if there's a valid selection
        mSignButton.setEnabled((adapter.getItemCount() > 0) && (position >= 0));
    }

    @Override
    public void onBackPressed() {
        if (mListener != null)
            mListener.onCancel();

        super.onBackPressed();
    }

    public interface NUICertificatePickerListener
    {
        void onOK(String serial, PKCS7DesignatedName designatedName);
        void onCancel();
    }
}
