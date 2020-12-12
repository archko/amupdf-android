package com.artifex.sonui.editor;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.artifex.mupdf.fitz.PKCS7DesignatedName;
import com.artifex.mupdf.fitz.PKCS7Verifier;
import com.artifex.sonui.editor.SOTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


public class NUICertificateViewer extends AppCompatActivity {

    protected Button mSignButton;
    protected HashMap<String, String> mDetails;
    protected int mResult = -1;
    protected int mUpdatedSinceSigning = 0;
    protected PKCS7DesignatedName mPKCS7DesignatedName;

    protected static NUICertificateViewerListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the result to null initially, to indicate no signature selected
        setContentView(R.layout.sodk_editor_certificate_view);

        Intent intent = getIntent();

        // set up mDetails from intent data
        mDetails = (HashMap<String, String>)(intent.getSerializableExtra("certificateDetails"));
        mResult = intent.getIntExtra("verifyResult", -1);
        mUpdatedSinceSigning = intent.getIntExtra("updatedSinceSigning", 0);

        mSignButton = findViewById(R.id.sodk_choose_signature);
        mSignButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        populate(mDetails);
    }

    protected void populate(HashMap<String, String> name) {
        SOTextView result = findViewById(R.id.sodk_editor_certificate_result);
        SOTextView resultDetail = findViewById(R.id.sodk_editor_certificate_result_detail);

        if (name != null)
        {
            SOTextView CN = findViewById(R.id.certificate_cn);
            SOTextView C = findViewById(R.id.certificate_c);
            SOTextView OU = findViewById(R.id.certificate_ou);
            SOTextView O = findViewById(R.id.certificate_o);
            SOTextView EMAIL = findViewById(R.id.certificate_email);

            if (name != null) {
                CN.setText(name.get("CN"));
                C.setText(name.get("C"));
                OU.setText(name.get("OU"));
                O.setText(name.get("O"));
                EMAIL.setText(name.get("EMAIL"));
            }

            SOTextView KeyUsage = findViewById(R.id.certificate_keyusage);
            SOTextView ExtendedKeyUsage =
                findViewById(R.id.certificate_extended_keyusage);
            SOTextView Expiry = findViewById(R.id.certificate_expiry);

            KeyUsage.setText(name.get("KEYUSAGE"));
            ExtendedKeyUsage.setText(name.get("EXTENDEDKEYUSAGE"));

            if (name.containsKey( "NOTAFTER" ))
            {
                int              seconds    = Integer.valueOf(name.get("NOTAFTER"));
                SimpleDateFormat formatter  = new SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm");
                String           dateString = formatter.format(new Date(seconds * 1000L));

                Expiry.setText(dateString);
            }
        }

        // check the result
        switch (mResult)
        {
            case PKCS7Verifier.PKCS7VerifierOK:
            {
                if (mUpdatedSinceSigning > 0)
                {
                    result.setText( R.string.sodk_editor_certificate_verify_warning );
                    resultDetail.setText( R.string.sodk_editor_certificate_verify_has_changes );
                }
                else
                {
                    result.setText( R.string.sodk_editor_certificate_verify_ok );
                    resultDetail.setText( R.string.sodk_editor_certificate_verify_permitted_changes );
                }
            }
            break;

            case PKCS7Verifier.PKCS7VerifierDigestFailure:
            {
                result.setText( R.string.sodk_editor_certificate_verify_failed );
                resultDetail.setText( R.string.sodk_editor_certificate_verify_digest_failure );
            }
            break;

            default:
            {
                result.setText( R.string.sodk_editor_certificate_verify_failed );
                resultDetail.setText( R.string.sodk_editor_certificate_verify_not_trusted );
            }
        }

        LinearLayout layoutView = findViewById(R.id.sodk_editor_certificate_list);
        if (layoutView != null) {
            layoutView.invalidate();
            layoutView.requestLayout();
        }
    }

    @Override
    public void onBackPressed() {
        if (mListener != null)
            mListener.onCancel();

        super.onBackPressed();
    }

    public interface NUICertificateViewerListener
    {
        void onCancel();
    }
}
