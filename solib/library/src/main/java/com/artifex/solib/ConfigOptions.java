package com.artifex.solib;

import android.util.Log;
import android.view.View;
import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

public class ConfigOptions implements Cloneable, Parcelable
{
    /** Notify when the document viewer has loaded a disabled feature **/
    public interface FeatureTracker{
        void hasLoadedDisabledFeature(View featureView);
        void hasTappedDisabledFeature(Point point);
    }
    public FeatureTracker featureTracker;

    private String mDebugTag = "ConfigOptions";

    private boolean           mShowUI;
    private boolean           mUsePersistentFileState;
    private boolean           mAllowAutoOpen;
    private boolean           mEditingEnabled;
    private boolean           mSaveAsEnabled;
    private boolean           mSaveAsPdfEnabled;
    private boolean           mOpenInEnabled;
    private boolean           mOpenPdfInEnabled;
    private boolean           mShareEnabled;
    private boolean           mExtClipboardInEnabled;
    private boolean           mExtClipboardOutEnabled;
    private boolean           mImageInsertEnabled;
    private boolean           mPhotoInsertEnabled;
    private boolean           mPrintingEnabled;
    private boolean           mSecurePrintingEnabled;
    private boolean           mNonRepudiationCertsOnly;
    private boolean           mLaunchUrlEnabled;
    private boolean           mDocAuthEntryEnabled;
    private boolean           mAppAuthEnabled;
    private int               mAppAuthTimeout;

    private boolean           mSaveEnabled;
    private boolean           mCustomSaveEnabled;

    private boolean           mTrackChangesFeatureEnabled;

    private boolean           mFormFillingEnabled;
    private boolean           mFormSigningFeatureEnabled;

    private boolean           mRedactionsEnabled;
    private boolean           mFullscreenEnabled;

    private boolean           mAnimationFeatureEnabled;

    private float             mDefaultInkAnnotationLineThickness;
    private int               mDefaultInkAnnotationLineColor;

    private boolean           mInvertContentInDarkMode;

    private boolean           mPDFAnnotationEnabled;

    public ConfigOptions()
    {
        mShowUI                  = true;
        mEditingEnabled          = true;
        mSaveAsEnabled           = true;
        mSaveAsPdfEnabled        = true;
        mOpenInEnabled           = true;
        mOpenPdfInEnabled        = true;
        mShareEnabled            = true;
        mExtClipboardInEnabled   = true;
        mExtClipboardOutEnabled  = true;
        mImageInsertEnabled      = true;
        mPhotoInsertEnabled      = true;
        mPrintingEnabled         = true;
        mSecurePrintingEnabled   = false;
        mNonRepudiationCertsOnly = false;
        mLaunchUrlEnabled        = true;
        mUsePersistentFileState  = true;
        mAllowAutoOpen           = true;
        mDocAuthEntryEnabled     = true;
        mAppAuthEnabled          = false;
        mAppAuthTimeout          = 30;

        mSaveEnabled            = true;
        mCustomSaveEnabled      = false;

        mTrackChangesFeatureEnabled = false;

        mFormFillingEnabled        = false;
        mFormSigningFeatureEnabled = false;

        mRedactionsEnabled      = false;

        mFullscreenEnabled = false;
        mAnimationFeatureEnabled = false;

        mInvertContentInDarkMode = false;

        mPDFAnnotationEnabled = true;
    }

    public ConfigOptions clone() throws
                   CloneNotSupportedException
    {
        // shallow copy should be enough, as we have only primitives and immutables.
        // note: featureTracker is copied shallow, but it is ok as its configuration
        // is updated whenever it is created. see iAPBinder constructor.
        return (ConfigOptions)super.clone();
    }

    protected ConfigOptions(Parcel in)
    {
        // featureTracker is not transfered.
        // But it is ok as its concrete class iAPBinder is
        // created whenever it is needed. see onCreate@AppNUIActivity.java
        featureTracker = null;

        // parcel primitives.
        mDebugTag = in.readString();
        mShowUI = in.readByte() != 0x00;
        mUsePersistentFileState = in.readByte() != 0x00;
        mAllowAutoOpen = in.readByte() != 0x00;
        mEditingEnabled = in.readByte() != 0x00;
        mSaveAsEnabled = in.readByte() != 0x00;
        mSaveAsPdfEnabled = in.readByte() != 0x00;
        mOpenInEnabled = in.readByte() != 0x00;
        mOpenPdfInEnabled = in.readByte() != 0x00;
        mShareEnabled = in.readByte() != 0x00;
        mExtClipboardInEnabled = in.readByte() != 0x00;
        mExtClipboardOutEnabled = in.readByte() != 0x00;
        mImageInsertEnabled = in.readByte() != 0x00;
        mPhotoInsertEnabled = in.readByte() != 0x00;
        mPrintingEnabled = in.readByte() != 0x00;
        mSecurePrintingEnabled = in.readByte() != 0x00;
        mNonRepudiationCertsOnly = in.readByte() != 0x00;
        mLaunchUrlEnabled = in.readByte() != 0x00;
        mDocAuthEntryEnabled = in.readByte() != 0x00;
        mAppAuthEnabled = in.readByte() != 0x00;
        mAppAuthTimeout = in.readInt();
        mSaveEnabled = in.readByte() != 0x00;
        mCustomSaveEnabled = in.readByte() != 0x00;
        mTrackChangesFeatureEnabled = in.readByte() != 0x00;
        mFormFillingEnabled = in.readByte() != 0x00;
        mFormSigningFeatureEnabled = in.readByte() != 0x00;
        mRedactionsEnabled = in.readByte() != 0x00;
        mFullscreenEnabled = in.readByte() != 0x00;
        mAnimationFeatureEnabled = in.readByte() != 0x00;
        mDefaultInkAnnotationLineThickness = in.readFloat();
        mDefaultInkAnnotationLineColor = in.readInt();
        mInvertContentInDarkMode = in.readByte() != 0x00;
        mPDFAnnotationEnabled = in.readByte() != 0x00;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        // featureTracker is ignored.

        // parcels
        dest.writeString(mDebugTag);
        dest.writeByte((byte) (mShowUI ? 0x01 : 0x00));
        dest.writeByte((byte) (mUsePersistentFileState ? 0x01 : 0x00));
        dest.writeByte((byte) (mAllowAutoOpen ? 0x01 : 0x00));
        dest.writeByte((byte) (mEditingEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mSaveAsEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mSaveAsPdfEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mOpenInEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mOpenPdfInEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mShareEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mExtClipboardInEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mExtClipboardOutEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mImageInsertEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mPhotoInsertEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mPrintingEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mSecurePrintingEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mNonRepudiationCertsOnly ? 0x01 : 0x00));
        dest.writeByte((byte) (mLaunchUrlEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mDocAuthEntryEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mAppAuthEnabled ? 0x01 : 0x00));
        dest.writeInt(mAppAuthTimeout);
        dest.writeByte((byte) (mSaveEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mCustomSaveEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mTrackChangesFeatureEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mFormFillingEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mFormSigningFeatureEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mRedactionsEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mFullscreenEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (mAnimationFeatureEnabled ? 0x01 : 0x00));
        dest.writeFloat(mDefaultInkAnnotationLineThickness);
        dest.writeInt(mDefaultInkAnnotationLineColor);
        dest.writeByte((byte) (mInvertContentInDarkMode ? 0x01 : 0x00));
        dest.writeByte((byte) (mPDFAnnotationEnabled ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ConfigOptions> CREATOR = new Parcelable.Creator<ConfigOptions>() {
        @Override
        public ConfigOptions createFromParcel(Parcel in) {
            return new ConfigOptions(in);
        }

        @Override
        public ConfigOptions[] newArray(int size) {
            return new ConfigOptions[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean showUI()
    {
        return mShowUI;
    }

    public boolean isEditingEnabled()
    {
        return mEditingEnabled;
    }

    public boolean isSaveAsEnabled()
    {
        return mSaveAsEnabled;
    }

    public boolean isSaveAsPdfEnabled()
    {
        return mSaveAsPdfEnabled;
    }

    public boolean isOpenInEnabled()
    {
        return mOpenInEnabled;
    }

    public boolean isOpenPdfInEnabled()
    {
        return mOpenPdfInEnabled;
    }

    public boolean isShareEnabled()
    {
        return mShareEnabled;
    }

    public boolean isExtClipboardInEnabled()
    {
        return mExtClipboardInEnabled;
    }

    public boolean isExtClipboardOutEnabled()
    {
        return mExtClipboardOutEnabled;
    }

    public boolean isImageInsertEnabled()
    {
        return mImageInsertEnabled;
    }

    public boolean isPhotoInsertEnabled()
    {
        return mPhotoInsertEnabled;
    }

    public boolean isPrintingEnabled()
    {
        return mPrintingEnabled;
    }

    public boolean isSecurePrintingEnabled()
    {
        return mSecurePrintingEnabled;
    }

    public boolean isNonRepudiationCertFilterEnabled()
    {
        return mNonRepudiationCertsOnly;
    }

    public boolean isLaunchUrlEnabled()
    {
        return mLaunchUrlEnabled;
    }

    public boolean isSaveEnabled()
    {
        return mSaveEnabled;
    }

    public boolean isCustomSaveEnabled()
    {
        return mCustomSaveEnabled;
    }

    public boolean usePersistentFileState()
    {
        return mUsePersistentFileState;
    }

    public boolean allowAutoOpen()
    {
        return mAllowAutoOpen;
    }

    public boolean isDocAuthEntryEnabled()
    {
        return mDocAuthEntryEnabled;
    }

    public boolean isAppAuthEnabled()
    {
        return mAppAuthEnabled;
    }

    public int getAppAuthTimeout()
    {
        return mAppAuthTimeout;
    }

    public boolean isTrackChangesFeatureEnabled()
    {
        return mTrackChangesFeatureEnabled;
    }

    public boolean isFormFillingEnabled()
    {
        return mFormFillingEnabled;
    }

    public boolean isFormSigningFeatureEnabled()
    {
        return mFormSigningFeatureEnabled;
    }

    public boolean isRedactionsEnabled()
    {
        return mRedactionsEnabled;
    }

    public boolean isFullscreenEnabled()
    {
        return mFullscreenEnabled;
    }

    public boolean isAnimationFeatureEnabled()
    {
        return mAnimationFeatureEnabled;
    }

    public boolean isDocExpired()
    {
        return false;
    }

    public boolean isDocSavable()
    {
        return true;
    }

    public int getDefaultPdfInkAnnotationDefaultLineColor() { return mDefaultInkAnnotationLineColor; }

    public float getDefaultPdfInkAnnotationDefaultLineThickness() { return mDefaultInkAnnotationLineThickness; }

    public boolean isInvertContentInDarkModeEnabled()
    {
        return mInvertContentInDarkMode;
    }

    public boolean isPDFAnnotationEnabled() {return mPDFAnnotationEnabled;}

    public void setShowUI(boolean showUI)
    {
        mShowUI = showUI;

        Log.i(mDebugTag, "mShowUI set to " +
                         String.valueOf(mShowUI));
    }

    public void setEditingEnabled(boolean isEnabled)
    {
        mEditingEnabled = isEnabled;

        Log.i(mDebugTag, "mEditingEnabled set to " +
                         String.valueOf(mEditingEnabled));
    }

    public void setSaveAsEnabled(boolean isEnabled)
    {
        mSaveAsEnabled = isEnabled;

        Log.i(mDebugTag, "mSaveAsEnabled set to " +
                         String.valueOf(mSaveAsEnabled));
    }

    public void setSaveAsPdfEnabled(boolean isEnabled)
    {
        mSaveAsPdfEnabled = isEnabled;

        Log.i(mDebugTag, "mSaveAsPdfEnabled set to " +
                         String.valueOf(mSaveAsPdfEnabled));
    }

    public void setOpenInEnabled(boolean isEnabled)
    {
        mOpenInEnabled = isEnabled;

        Log.i(mDebugTag, "mOpenInEnabled set to " +
                         String.valueOf(mOpenInEnabled));

    }
    public void setOpenPdfInEnabled(boolean isEnabled)
    {
        mOpenPdfInEnabled = isEnabled;

        Log.i(mDebugTag, "OpenPdfInEnabled set to " +
                         String.valueOf(mOpenPdfInEnabled));
    }

    public void setShareEnabled(boolean isEnabled)
    {
        mShareEnabled = isEnabled;

        Log.i(mDebugTag, "mShareEnabled set to " +
                         String.valueOf(mShareEnabled));
    }

    public void setExtClipboardInEnabled(boolean isEnabled)
    {
        mExtClipboardInEnabled = isEnabled;

        Log.i(mDebugTag, "mExtClipboardInEnabled set to " +
                         String.valueOf(mExtClipboardInEnabled));
    }

    public void setExtClipboardOutEnabled(boolean isEnabled)
    {
        mExtClipboardOutEnabled = isEnabled;

        Log.i(mDebugTag, "mExtClipboardOutEnabled set to " +
                         String.valueOf(mExtClipboardOutEnabled));
    }

    public void setImageInsertEnabled(boolean isEnabled)
    {
        mImageInsertEnabled = isEnabled;

        Log.i(mDebugTag, "mImageInsertEnabled set to " +
                         String.valueOf(mImageInsertEnabled));
    }

    public void setPhotoInsertEnabled(boolean isEnabled)
    {
        mPhotoInsertEnabled = isEnabled;

        Log.i(mDebugTag, "mPhotoInsertEnabled set to " +
                         String.valueOf(mPhotoInsertEnabled));
    }

    public void setPrintingEnabled(boolean isEnabled)
    {
        mPrintingEnabled = isEnabled;

        Log.i(mDebugTag, "mPrintingEnabled set to " +
                         String.valueOf(mPrintingEnabled));
    }

    public void setSecurePrintingEnabled(boolean isEnabled)
    {
        mSecurePrintingEnabled = isEnabled;

        Log.i(mDebugTag, "mSecurePrintingEnabled set to " +
                         String.valueOf(mSecurePrintingEnabled));
    }

    public void setNonRepudiationCertOnlyFilterEnabled(boolean isEnabled)
    {
        mNonRepudiationCertsOnly = isEnabled;

        Log.i(mDebugTag, "mNonRepudiationCertsOnly set to " +
                         String.valueOf(mNonRepudiationCertsOnly));
    }

    public void setLaunchUrlEnabled(boolean isEnabled)
    {
        mLaunchUrlEnabled = isEnabled;

        Log.i(mDebugTag, "mLaunchUrlEnabled set to " +
                         String.valueOf(mLaunchUrlEnabled));
    }

    public void setSaveEnabled(boolean isEnabled)
    {
        mSaveEnabled = isEnabled;

        Log.i(mDebugTag, "mSaveEnabled set to " +
                         String.valueOf(mSaveEnabled));
    }

    public void setCustomSaveEnabled(boolean isEnabled)
    {
        mCustomSaveEnabled = isEnabled;

        Log.i(mDebugTag, "mCustomSaveEnabled set to " +
                         String.valueOf(mCustomSaveEnabled));
    }

    public void setUsePersistentFileState(boolean isEnabled)
    {
        mUsePersistentFileState = isEnabled;

        Log.i(mDebugTag, "mUsePersistentFileState set to " +
                         String.valueOf(mUsePersistentFileState));
    }

    public void setAllowAutoOpen(boolean isEnabled)
    {
        mAllowAutoOpen = isEnabled;

        Log.i(mDebugTag, "mAllowAutoOpen set to " +
                         String.valueOf(mAllowAutoOpen));
    }

    public void setDocAuthEntryEnabled(boolean isEnabled)
    {
        mDocAuthEntryEnabled = isEnabled;

        Log.i(mDebugTag, "mDocAuthEntryEnabled set to " +
                         String.valueOf(mDocAuthEntryEnabled));
    }

    public void setAppAuthEnabled(boolean isEnabled)
    {
        mAppAuthEnabled = isEnabled;

        Log.i(mDebugTag, "mAppAuthEnabled set to " +
                         String.valueOf(mAppAuthEnabled));
    }

    public void setAppAuthTimeout(int timeout)
    {
        mAppAuthTimeout = timeout;

        Log.i(mDebugTag, "mAppAuthTimeout set to " +
                         String.valueOf(mAppAuthTimeout));
    }

    public void setTrackChangesFeatureEnabled(boolean isEnabled)
    {
        mTrackChangesFeatureEnabled = isEnabled;

        Log.i(mDebugTag, "mTrackChangesFeatureEnabled set to " +
                         String.valueOf(mTrackChangesFeatureEnabled));
    }

    public void setFeatureTracker(FeatureTracker featureTracker) {
        this.featureTracker = featureTracker;
    }

    public void setFormFillingEnabled(boolean isEnabled)
    {
        mFormFillingEnabled = isEnabled;

        Log.i(mDebugTag, "mFormFillingEnabled set to " +
                String.valueOf(mFormFillingEnabled));
    }

    public void setFormSigningFeatureEnabled(boolean isEnabled)
    {
        mFormSigningFeatureEnabled = isEnabled;

        Log.i(mDebugTag, "mFormSigningFeatureEnabled set to " +
                String.valueOf(mFormSigningFeatureEnabled));
	}

    public void setRedactionsEnabled(boolean isEnabled)
    {
        mRedactionsEnabled = isEnabled;

        Log.i(mDebugTag, "mRedactionsEnabled set to " +
                String.valueOf(mRedactionsEnabled));
    }

    public void setFullscreenEnabled(boolean isEnabled)
    {
        mFullscreenEnabled = isEnabled;

        Log.i(mDebugTag, "mFullscreenEnabled set to " +
                String.valueOf(mFullscreenEnabled));
    }

    public void setAnimationFeatureEnabled(boolean isEnabled)
    {
        mAnimationFeatureEnabled = isEnabled;

        Log.i(mDebugTag, "mAnimationFeatureEnabled set to " +
                         String.valueOf(mAnimationFeatureEnabled));
    }

    public void setDefaultPdfInkAnnotationDefaultLineColor(int color)
    {
        mDefaultInkAnnotationLineColor = color;

        Log.i(mDebugTag, "mDefaultInkAnnotationLineColor set to " +
                String.valueOf(mDefaultInkAnnotationLineColor));
    }

    public void setDefaultPdfInkAnnotationDefaultLineThickness(float thickness)
    {
        mDefaultInkAnnotationLineThickness = thickness;

        Log.i(mDebugTag, "mDefaultInkAnnotationLineThickness set to " +
                String.valueOf(mDefaultInkAnnotationLineThickness));
    }

    public void setInvertContentInDarkModeEnabled(boolean isEnabled)
    {
        mInvertContentInDarkMode = isEnabled;

        Log.i(mDebugTag, "mInvertContentInDarkMode set to " +
                String.valueOf(mInvertContentInDarkMode));
    }

    public void setPDFAnnotationEnabled(boolean enable)
    {
        mPDFAnnotationEnabled = enable;

        Log.i(mDebugTag, "mPDFAnnotationEnabled set to " +
                String.valueOf(mPDFAnnotationEnabled));
    }
}
