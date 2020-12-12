package com.artifex.sonui.editor;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import com.artifex.solib.FileUtils;


// We never open a document directly. We first make a copy and open that.
// All operations are performed on the copy before eventually saving
// back to the original. This class aids this mechanism, keeping an
// on disc database of such copies.

public class SOFileDatabase {

    private static final String     mDebugTag     = "SOFileDatabase";
    private static final String     mTmpName      = ".tmpint";
    private static final String     mTmpThumbName = ".thumbs";

    //  there is a singleton database, created by the app's main activity
    //  by calling init()
    //  retrieved by calling getDatabase()
    private static Context        mContext;
    private static Object         mSharedPrefs;
    private static SOFileDatabase mDatabase = null;
    private static String         mTempFolderPath;
    private static String         mTempThumbsPath;

    public static void init(Context context)
    {
        if (mDatabase==null) {
            mContext = context;
            mDatabase = new SOFileDatabase();

            //  get the shared preferences object.
            mSharedPrefs =
                Utilities.getPreferencesObject(mContext, "fileDatabase2");
        }

        String errorBase =
            "init() experienced unexpected exception [%s]";

        mTempFolderPath = FileUtils.getTempPathRoot(mContext) +
                          File.separator + mTmpName + File.separator;

        mTempThumbsPath = FileUtils.getTempPathRoot(mContext) +
                          File.separator + mTmpThumbName + File.separator;

        // Ensure the temporary folder exists
        if (! FileUtils.fileExists(mTempFolderPath))
        {
            FileUtils.createDirectory(mTempFolderPath);
        }

        // Ensure the thumbnail folder exists
        if (! FileUtils.fileExists(mTempThumbsPath))
        {
            FileUtils.createDirectory(mTempThumbsPath);
        }
    }

    public static SOFileDatabase getDatabase()
    {
        return mDatabase;
    }

    private Object getPrefs()
    {
        return mSharedPrefs;
    }

    //  store an SOFileState object.
    public void setValue(String key, SOFileState state)
    {
        Utilities.setStringPreference(getPrefs(),
                                      key,
                                      SOFileState.toString(state));
    }

    //  delete an SOFileState object.
    private void deleteValue(String key)
    {
        Utilities.removePreference(getPrefs(), key);
    }

    //  get an SOFileState object.
    public SOFileState getValue(String key)
    {
        String str = Utilities.getStringPreference(getPrefs(), key, "");
        return SOFileState.fromString(str, this);
    }

    public SOFileState stateForPath(String userPath, boolean template)
    {
        return stateForPath(userPath, template, false);
    }

    //  get or create an SOFileState for the given path.
    public SOFileState stateForPath(String userPath, boolean template, boolean createNew)
    {
        //  look for it
        SOFileState state = null;
        if (!createNew)
            state = getValue(userPath);

        if (state==null || state.getUserPath().isEmpty())
        {
            //  make one

            //  Choose a not-in-use path for our internal copy
            //  TODO: can extension be null or blank?
            String ext = "." + FileUtils.getExtension(userPath);
            String internal = uniqueTempFilePath(ext);
            if (!internal.equals(""))
            {
                // Make a new SOFileState with the original path and the newly chosen unique internal one
                state = new SOFileState(userPath, internal, this, 0);

                // if we're creating the database entry,
                // There shouldn't be a file at internalPath. Delete just in case
                FileUtils.deleteFile(internal);

                // Record the state in the dictionary against the original path
                if (!template)
                    setValue(userPath, state);
            }
        }

        return state;
    }

    //  generate a unique path for a thumbnail.
    public static String uniqueThumbFilePath()
    {
        String filePath = "";

        // Ensure the temporary folder exists
        if (! FileUtils.fileExists(mTempThumbsPath))
        {
            FileUtils.createDirectory(mTempThumbsPath);
        }

        if (FileUtils.fileExists(mTempThumbsPath))
        {
            //  now make the file path using UUID.  This way the path can be
            //  created without the file needing to exist.
            filePath = mTempThumbsPath +
                       java.util.UUID.randomUUID().toString() + ".png";
        }

        return filePath;
    }

    //  generate a unique path for an internal copy.
    private String uniqueTempFilePath(String suffix)
    {
        String filePath = "";

        // Ensure the temporary folder exists
        if (! FileUtils.fileExists(mTempFolderPath))
        {
            FileUtils.createDirectory(mTempFolderPath);
        }

        if (FileUtils.fileExists(mTempFolderPath))
        {
            //  now make the file path using UUID.  This way the path can be
            //  created without the file needing to exist.
            filePath = mTempFolderPath +
                       java.util.UUID.randomUUID().toString() + suffix;
        }

        return filePath;
    }

    //  a class to represent a state and its database key
    public class StateAndKey
    {
        public SOFileState state;
        public String key;
    }

    //  collect up all the database entries in an ArrayList.
    public ArrayList<StateAndKey> getStatesAndKeys()
    {
        ArrayList<StateAndKey> entries = new ArrayList<StateAndKey>();
        Map<String,?> keys = Utilities.getAllStringPreferences(getPrefs());

        if (keys == null)
            return entries;

        for(Map.Entry<String,?> entry : keys.entrySet())
        {
            StateAndKey sak = new StateAndKey();
            sak.state = getValue(entry.getKey());
            sak.key = entry.getKey();
            entries.add(sak);
        }
        return entries;
    }

    //  delete a given entry from the database using its key
    public void deleteEntry(String key)
    {
        deleteValue(key);
    }

    //  delete all database entries, thumbnails, and internal copies.
    //  at some future date we might give this feature, or something
    //  like it, to the user.
    public void clearAll()
    {
        //  WARNING: this function deletes the file database entries,
        //  internal copies and thumbnails.

        Map<String,?> keys = Utilities.getAllStringPreferences(getPrefs());
        for(Map.Entry<String,?> entry : keys.entrySet())
        {
            String key = entry.getKey();
            SOFileState state = getValue(key);
            if (state != null)
            {
                //  delete internal copy
                String internal = state.getInternalPath();
                FileUtils.deleteFile(internal);

                //  delete thumbnail
                String thumb = state.getThumbnail();
                FileUtils.deleteFile(thumb);

                //  delete the entry
                deleteValue(entry.getKey());
            }
        }
    }
}
