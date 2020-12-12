package com.artifex.sonui.editor;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //  all activities derive from BaseActivity, so we can change the locale
        //  dynamically as activities are created, but before they load
        //  any resources.
        //
        //  for example,
        //
        //  setLocale("de");
        //
        //  setLocale(Locale.SIMPLIFIED_CHINESE);
        //
        //  setLocale(new Locale("ar", "AR"));
    }

    private void setLocale(String languageId)
    {
        Locale locale = new Locale(languageId);
        setLocale(locale);
    }


    private void setLocale(Locale locale)
    {
        //  this function uses a deprecated feature,
        //  but it's just for testing.

        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    public boolean isSlideShow()
    {
        return false;
    }

    //  interface for a result handler
    public interface ResultHandler {
        boolean handle(int request, int result, Intent data);
    }

    //  the current result handler.  May be null
    private static ResultHandler mResultHandler = null;
    public static void setResultHandler(ResultHandler handler) {mResultHandler=handler;}

    @Override
    protected void onActivityResult(int request, int result, Intent data)
    {
        //  invoke a result handler if one is set.
        if (mResultHandler != null)
        {
            boolean handled = mResultHandler.handle(request, result, data);
            if (handled)
                return;
        }

        super.onActivityResult(request, result, data);
    }

    //  interface for a permissions result handler
    public interface PermissionResultHandler {
        boolean handle(int requestCode, String permissions[], int[] grantResults);
    }

    //  the current permission result handler.  May be null
    private static PermissionResultHandler mPermissionResultHandler = null;
    public static void setPermissionResultHandler(PermissionResultHandler handler) {mPermissionResultHandler=handler;}

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        //  invoke a result handler if one is set.
        if (mPermissionResultHandler != null)
        {
            boolean handled = mPermissionResultHandler.handle(requestCode, permissions, grantResults);
            if (handled)
                return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //  the current activity.
    //  set by onResume, cleared by onPause.
    private static BaseActivity mCurrentActivity = null;
    public static BaseActivity getCurrentActivity() {return mCurrentActivity;}

    @Override
    protected void onResume()
    {
        mCurrentActivity = this;
        super.onResume();

        if (mResumeHandler != null)
        {
            mResumeHandler.handle();
        }
    }

    @Override
    protected void onPause()
    {
        mCurrentActivity = null;
        super.onPause();
    }

    //  interface for a result handler
    public interface ResumeHandler {
        void handle();
    }

    //  the current result handler.  May be null
    private static ResumeHandler mResumeHandler = null;
    public static void setResumeHandler(ResumeHandler handler) {mResumeHandler=handler;}

}
