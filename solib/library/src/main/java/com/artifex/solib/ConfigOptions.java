package com.artifex.solib;

import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ConfigOptions
{
    // Enable/Disable Logging.
    protected static final boolean mDebugLog = false;
    private String mDebugTag = "ConfigOptions";

    /** Notify when the document viewer has loaded a disabled feature **/
    public interface FeatureTracker{
        void hasLoadedDisabledFeature(View featureView);
        void hasTappedDisabledFeature(Point point);
    }
    public FeatureTracker featureTracker;

    // Config bundle keys
    public static final String ClassNameKey =                 "ClassNameKey";

    private static final String ShowUIKey =                      "ShowUIKey";
    private static final String UsePersistentFileStateKey =
                                                 "UsePersistentFileStateKey";
    private static final String AllowAutoOpenKey =        "AllowAutoOpenKey";
    private static final String EditingEnabledKey =      "EditingEnabledKey";
    private static final String SaveAsEnabledKey =        "SaveAsEnabledKey";
    private static final String SaveAsPdfEnabledKey =  "SaveAsPdfEnabledKey";
    private static final String OpenInEnabledKey =        "OpenInEnabledKey";
    private static final String OpenPdfInEnabledKey =  "OpenPdfInEnabledKey";
    private static final String ShareEnabledKey =          "ShareEnabledKey";
    private static final String ExtClipboardInEnabledKey =
                                                  "ExtClipboardInEnabledKey";
    private static final String ExtClipboardOutEnabledKey =
                                                 "ExtClipboardOutEnabledKey";
    private static final String ImageInsertEnabledKey =
                                                     "ImageInsertEnabledKey";
    private static final String PhotoInsertEnabledKey =
                                                     "PhotoInsertEnabledKey";
    private static final String PrintingEnabledKey =    "PrintingEnabledKey";
    private static final String SecurePrintingEnabledKey =
                                                  "SecurePrintingEnabledKey";
    private static final String NonRepudiationCertsOnlyKey =
                                                "NonRepudiationCertsOnlyKey";
    private static final String LaunchUrlEnabledKey =  "LaunchUrlEnabledKey";
    private static final String DocAuthEntryEnabledKey =
                                                    "DocAuthEntryEnabledKey";
    private static final String AppAuthEnabledKey =      "AppAuthEnabledKey";
    private static final String AppAuthTimeoutKey =      "AppAuthTimeoutKey";

    private static final String SaveEnabledKey =            "SaveEnabledKey";
    private static final String CustomSaveEnabledKey =
                                                      "CustomSaveEnabledKey";

    private static final String TrackChangesFeatureEnabledKey =
                                             "TrackChangesFeatureEnabledKey";

    private static final String FormFillingEnabledKey =
                                                     "FormFillingEnabledKey";
    private static final String FormSigningFeatureEnabledKey =
                                              "FormSigningFeatureEnabledKey";

    private static final String RedactionsEnabledKey =
                                                      "RedactionsEnabledKey";
    private static final String FullscreenEnabledKey =
                                                      "FullscreenEnabledKey";

    private static final String AnimationFeatureEnabledKey =
                                                "AnimationFeatureEnabledKey";

    private static final String DefaultInkAnnotationLineThicknessKey =
                                      "DefaultInkAnnotationLineThicknessKey";
    private static final String DefaultInkAnnotationLineColorKey =
                                          "DefaultInkAnnotationLineColorKey";

    private static final String InvertContentInDarkModeKey =
                                                "InvertContentInDarkModeKey";

    private static final String PDFAnnotationEnabledKey =
                                                   "PDFAnnotationEnabledKey";

    protected Bundle mSettingsBundle;

    public ConfigOptions()
    {
        /*
         * Create the config bundle.
         *
         * Entries are added in the set* functions,
         * Defaults are defined in the get* functions,
         */
        mSettingsBundle = new Bundle();

        /*
         * Store the, fully qualified, class name.
         *
         * This will be used when loading an instance of this class
         * received via intent.
         */
        mSettingsBundle.putString("ClassNameKey", this.getClass().getName());
    }

    // Copy constructor
    public ConfigOptions(ConfigOptions co)
    {
        /*
         * Create the config bundle.
         *
         * Entries are added in the set* functions,
         * Defaults are defined in the get* functions,
         */
        mSettingsBundle = new Bundle(co.mSettingsBundle);

        /*
         * Store the, fully qualified, class name.
         *
         * This will be used when loading an instance of this class
         * received via intent.
         */
        mSettingsBundle.putString("ClassNameKey", this.getClass().getName());
    }

    // Construct from a bundle.
    public ConfigOptions(Bundle bundle)
    {
        /*
         * Create the config bundle.
         *
         * Entries are added in the set* functions,
         * Defaults are defined in the get* functions,
         */
        mSettingsBundle = new Bundle(bundle);

        /*
         * Store the, fully qualified, class name.
         *
         * This will be used when loading an instance of this class
         * received via intent.
         */
        mSettingsBundle.putString("ClassNameKey", this.getClass().getName());
    }

    /*
     * Copy the mappings contained in 'bundle' into the settings
     * bundle, overwriting any existing matching mappings.
     */
    public void applyBundle(Bundle bundle)
    {
        mSettingsBundle.putAll(bundle);
    }

    public Bundle getSettingsBundle()
    {
        return mSettingsBundle;
    }

    // Getters
    public boolean showUI()
    {
        return mSettingsBundle.getBoolean(ShowUIKey, true);
    }

    public boolean isEditingEnabled()
    {
        return mSettingsBundle.getBoolean(EditingEnabledKey, true);
    }

    public boolean isSaveAsEnabled()
    {
        return mSettingsBundle.getBoolean(SaveAsEnabledKey, true);
    }

    public boolean isSaveAsPdfEnabled()
    {
        return mSettingsBundle.getBoolean(SaveAsPdfEnabledKey, true);
    }

    public boolean isOpenInEnabled()
    {
        return mSettingsBundle.getBoolean(OpenInEnabledKey, true);
    }

    public boolean isOpenPdfInEnabled()
    {
        return mSettingsBundle.getBoolean(OpenPdfInEnabledKey, true);
    }

    public boolean isShareEnabled()
    {
        return mSettingsBundle.getBoolean(ShareEnabledKey, true);
    }

    public boolean isExtClipboardInEnabled()
    {
        return mSettingsBundle.getBoolean(ExtClipboardInEnabledKey, true);
    }

    public boolean isExtClipboardOutEnabled()
    {
        return mSettingsBundle.getBoolean(ExtClipboardOutEnabledKey, true);
    }

    public boolean isImageInsertEnabled()
    {
        return mSettingsBundle.getBoolean(ImageInsertEnabledKey, true);
    }

    public boolean isPhotoInsertEnabled()
    {
        return mSettingsBundle.getBoolean(PhotoInsertEnabledKey, true);
    }

    public boolean isPrintingEnabled()
    {
        return mSettingsBundle.getBoolean(PrintingEnabledKey, true);
    }

    public boolean isSecurePrintingEnabled()
    {
        return mSettingsBundle.getBoolean(SecurePrintingEnabledKey, false);
    }

    public boolean isNonRepudiationCertFilterEnabled()
    {
        return mSettingsBundle.getBoolean(NonRepudiationCertsOnlyKey, false);
    }

    public boolean isLaunchUrlEnabled()
    {
        return mSettingsBundle.getBoolean(LaunchUrlEnabledKey, true);
    }

    public boolean isSaveEnabled()
    {
        return mSettingsBundle.getBoolean(SaveEnabledKey, true);
    }

    public boolean isCustomSaveEnabled()
    {
        return mSettingsBundle.getBoolean(CustomSaveEnabledKey, false);
    }

    public boolean usePersistentFileState()
    {
        return mSettingsBundle.getBoolean(UsePersistentFileStateKey, true);
    }

    public boolean allowAutoOpen()
    {
        return mSettingsBundle.getBoolean(AllowAutoOpenKey, true);
    }

    public boolean isDocAuthEntryEnabled()
    {
        return mSettingsBundle.getBoolean(DocAuthEntryEnabledKey, true);
    }

    public boolean isAppAuthEnabled()
    {
        return mSettingsBundle.getBoolean(AppAuthEnabledKey, false);
    }

    public int getAppAuthTimeout()
    {
        return mSettingsBundle.getInt(AppAuthTimeoutKey, 30);
    }

    public boolean isTrackChangesFeatureEnabled()
    {
        return mSettingsBundle.getBoolean(TrackChangesFeatureEnabledKey,
                                          false);
    }

    public boolean isFormFillingEnabled()
    {
        return mSettingsBundle.getBoolean(FormFillingEnabledKey, false);
    }

    public boolean isFormSigningFeatureEnabled()
    {
        return mSettingsBundle.getBoolean(FormSigningFeatureEnabledKey, false);
    }

    public boolean isRedactionsEnabled()
    {
        return mSettingsBundle.getBoolean(RedactionsEnabledKey, false);
    }

    public boolean isFullscreenEnabled()
    {
        return mSettingsBundle.getBoolean(FullscreenEnabledKey, false);
    }

    public boolean isAnimationFeatureEnabled()
    {
        return mSettingsBundle.getBoolean(AnimationFeatureEnabledKey, false);
    }

    public boolean isDocExpired()
    {
        return false;
    }

    public boolean isDocSavable()
    {
        return true;
    }

    public int getDefaultPdfInkAnnotationDefaultLineColor()
    {
        return mSettingsBundle.getInt(DefaultInkAnnotationLineColorKey, 0);
    }

    public float getDefaultPdfInkAnnotationDefaultLineThickness()
    {
        return mSettingsBundle.getFloat(DefaultInkAnnotationLineThicknessKey,
                                        0.0f);
    }

    public boolean isInvertContentInDarkModeEnabled()
    {
        return mSettingsBundle.getBoolean(InvertContentInDarkModeKey, false);
    }

    public boolean isPDFAnnotationEnabled()
    {
        return mSettingsBundle.getBoolean(PDFAnnotationEnabledKey, true);
    }

    // Setters
    public void setShowUI(boolean showUI)
    {
        mSettingsBundle.putBoolean(ShowUIKey, showUI);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "ShowUI set to " + String.valueOf(showUI));
        }
    }

    public void setEditingEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(EditingEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "EditingEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setSaveAsEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(SaveAsEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "SaveAsEnabled set to " +
                              String.valueOf(isEnabled));
        }
    }

    public void setSaveAsPdfEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(SaveAsPdfEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "SaveAsPdfEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setOpenInEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(OpenInEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "OpenInEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setOpenPdfInEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(OpenPdfInEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "OpenPdfInEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setShareEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(ShareEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "ShareEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setExtClipboardInEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(ExtClipboardInEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "ExtClipboardInEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setExtClipboardOutEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(ExtClipboardOutEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "ExtClipboardOutEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setImageInsertEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(ImageInsertEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "ImageInsertEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setPhotoInsertEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(PhotoInsertEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "PhotoInsertEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setPrintingEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(PrintingEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "PrintingEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setSecurePrintingEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(SecurePrintingEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "SecurePrintingEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setNonRepudiationCertOnlyFilterEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(NonRepudiationCertsOnlyKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "NonRepudiationCertsOnly set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setLaunchUrlEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(LaunchUrlEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "LaunchUrlEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setSaveEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(SaveEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "SaveEnabled set to " + String.valueOf(isEnabled));
        }
    }

    public void setCustomSaveEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(CustomSaveEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "CustomSaveEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setUsePersistentFileState(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(UsePersistentFileStateKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "UsePersistentFileState set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setAllowAutoOpen(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(AllowAutoOpenKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "AllowAutoOpen set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setDocAuthEntryEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(DocAuthEntryEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "DocAuthEntryEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setAppAuthEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(AppAuthEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "AppAuthEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setAppAuthTimeout(int timeout)
    {
        mSettingsBundle.putInt(AppAuthTimeoutKey, timeout);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "AppAuthTimeout set to " +
                             String.valueOf(timeout));
        }
    }

    public void setTrackChangesFeatureEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(TrackChangesFeatureEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "TrackChangesFeatureEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setFeatureTracker(FeatureTracker featureTracker) {
        this.featureTracker = featureTracker;
    }

    public void setFormFillingEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(FormFillingEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "FormFillingEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setFormSigningFeatureEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(FormSigningFeatureEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "FormSigningFeatureEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setRedactionsEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(RedactionsEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "RedactionsEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setFullscreenEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(FullscreenEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "FullscreenEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setAnimationFeatureEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(AnimationFeatureEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "AnimationFeatureEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setDefaultPdfInkAnnotationDefaultLineColor(int color)
    {
        mSettingsBundle.putInt(DefaultInkAnnotationLineColorKey, color);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "DefaultInkAnnotationLineColor set to " +
                             String.valueOf(color));
        }
    }

    public void setDefaultPdfInkAnnotationDefaultLineThickness(float thickness)
    {
        mSettingsBundle.putFloat(DefaultInkAnnotationLineThicknessKey,
                                 thickness);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "DefaultInkAnnotationLineThickness set to " +
                             String.valueOf(thickness));
        }
    }

    public void setInvertContentInDarkModeEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(InvertContentInDarkModeKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "InvertContentInDarkMode set to " +
                             String.valueOf(isEnabled));
        }
    }

    public void setPDFAnnotationEnabled(boolean isEnabled)
    {
        mSettingsBundle.putBoolean(PDFAnnotationEnabledKey, isEnabled);

        if (mDebugLog)
        {
            Log.i(mDebugTag, "PDFAnnotationEnabled set to " +
                             String.valueOf(isEnabled));
        }
    }

    // For debugging purposed only.
    public void dumpOptionsBundle()
    {
        if (mDebugLog)
        {
            Log.d(mDebugTag, "Dumping Option Bundle");
            Log.d(mDebugTag, mSettingsBundle.toString());
        }
    }
}
