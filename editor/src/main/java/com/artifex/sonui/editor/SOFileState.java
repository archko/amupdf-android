package com.artifex.sonui.editor;

import android.content.Context;
import android.util.Base64;

import com.artifex.solib.FileUtils;

import java.io.UnsupportedEncodingException;

// We never open a document directly. We first make a copy and open that.
// All operations are performed on the copy before eventually saving
// back to the original. This class aids this mechanism, keeping an
// on disc database of such copies.

public class SOFileState
{
    //  constants
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    private static final String DELIMITER = "|";

    //  tokens we use in the encoded string to represent
    //  null or empty strings.
    private static final String NULL = "--null--";
    private static final String EMPTY = "--empty--";

    //  original path of the file
    private String mUserPath;
    private String mOpenedPath;

    //  foreign data.
    private String mForeignData;
    public void setForeignData(String str) {mForeignData=str; persist();}
    public String getForeignData() {return mForeignData;}

    //  path to the internal copy, if there is one
    private final String mInternalPath;

    //  tracking whether the file has been changed
    private boolean mHasChanges;

    //  last access date/time
    private long mLastAccess;
    public long getLastAccess() {return mLastAccess;}

    //  path to the thumbnail, if there is one, for documents.  Or,
    //  name of a thumbnail in the Assets folder, for templates.
    private String mThumbPath;

    //  the file database
    private final SOFileDatabase mDatabase;

    //  page number, zero-based
    private int mPageNumber=0;

    //  scale factor
    private float mScale = 1.0f;

    //  scroll position
    private int mScrollX = 0;
    private int mScrollY = 0;

    //  is page list showing
    private boolean mPagesListVisible = false;

    //  private constructor
    private SOFileState(String userPath, String internalPath, String openedPath, long lastAccess, boolean hasChanges,
                        String thumbPath, SOFileDatabase database, int pageNumber,
                        float scale, int scrollX, int scrollY, boolean pageListVisible)
    {
        mUserPath = userPath;
        mInternalPath = internalPath;
        mLastAccess = lastAccess;
        mHasChanges = hasChanges;
        mDatabase = database;
        mThumbPath = thumbPath;
        mOpenedPath = openedPath;
        mForeignData = null;
        mPageNumber = pageNumber;
        mScale = scale;
        mScrollX = scrollX;
        mScrollY = scrollY;
        mPagesListVisible = pageListVisible;
    }

    //  public constructor
    public SOFileState(String userPath, String internalPath, SOFileDatabase database, int pageNumber)
    {
        this(userPath, internalPath, userPath, 0, false, "", database, pageNumber, 1.0f, 0, 0, false);
    }

    //  "copy" constructor
    public SOFileState(SOFileState state)
    {
        mUserPath         = state.mUserPath;
        mInternalPath     = state.mInternalPath;
        mLastAccess       = state.mLastAccess;
        mHasChanges       = state.mHasChanges;
        mDatabase         = state.mDatabase;
        mThumbPath        = state.mThumbPath;
        mOpenedPath       = state.mOpenedPath;
        mForeignData      = state.getForeignData();
        mPageNumber       = state.mPageNumber;
        mScale = state.mScale;
        mScrollX          = state.mScrollX;
        mScrollY          = state.mScrollY;
        mPagesListVisible = state.mPagesListVisible;
    }

    //  setters/getters
    public String  getInternalPath() {return mInternalPath;}

    public String  getOpenedPath()  {return mOpenedPath;}
    public void    setOpenedPath(String path) {mOpenedPath = path;}

    public String  getUserPath() {return mUserPath;}
    public void    setUserPath(String path) {mUserPath = path;}

    public boolean hasChanges() {return mHasChanges;}
    public void    setHasChanges(boolean val) {mHasChanges=val;}

    public void    setPageNumber(int val) {mPageNumber=val;}
    public int     getPageNumber() {return mPageNumber;}

    public void    setScale(float val) {mScale =val;}
    public float   getScale() {return mScale;}

    public void    setScrollX(int val) {mScrollX=val;}
    public int     getScrollX() {return mScrollX;}

    public void    setScrollY(int val) {mScrollY=val;}
    public int     getScrollY() {return mScrollY;}

    public void    setPageListVisible(boolean val) {mPagesListVisible=val;}
    public boolean getPageListVisible() {return mPagesListVisible;}

    public boolean isTemplate()
    {
        //  this state repreents an open template if has no user path.
        if (mUserPath==null)
            return true;
        if (mUserPath.isEmpty())
            return true;
        return false;
    }

    //  convert an SOFileState to an encoded string
    public static String toString(SOFileState state)
    {
        String str = "";

        //  encode and combine
        str += encode(state.mUserPath) + DELIMITER;
        str += encode(state.mInternalPath) + DELIMITER;
        str += String.valueOf(state.mLastAccess) + DELIMITER;
        str += (state.mHasChanges?TRUE:FALSE) + DELIMITER;
        str += encode(state.mThumbPath) + DELIMITER;
        str += encode(state.mOpenedPath) + DELIMITER;
        str += encode(state.mForeignData) + DELIMITER;
        str += String.valueOf(state.mPageNumber) + DELIMITER;
        str += String.valueOf(state.mScale) + DELIMITER;
        str += String.valueOf(state.mScrollX) + DELIMITER;
        str += String.valueOf(state.mScrollY) + DELIMITER;
        str += String.valueOf(state.mPagesListVisible) + DELIMITER;

        return str;
    }

    public SOFileState copy()
    {
        String str = SOFileState.toString(this);
        SOFileState state = SOFileState.fromString(str, SOFileDatabase.getDatabase());
        return state;
    }

    //  convert an encoded string to an SOFileState
    public static SOFileState fromString (String s, SOFileDatabase database)
    {
        //  must have a string.
        if (s==null)
            return null;
        if (s.isEmpty())
            return null;

        //  split into array and decode values
        String[] values = s.split("\\|");

        //  sanity check.  The string should have at least six values.
        if (values.length<6)
            return null;

        //  decode the values

        String userPath = "";
        if (values.length>=1)
            userPath = decode(values[0]);

        String internalPath = "";
        if (values.length>=2)
            internalPath = decode(values[1]);

        long lastAccess = 0;
        if (values.length>=3)
            lastAccess = Long.parseLong(values[2], 10);

        boolean hasChanges = false;
        if (values.length>=4)
            hasChanges = values[3].equals(TRUE);

        String thumbPath = "";
        if (values.length>=5)
            thumbPath = decode(values[4]);

        String openedPath = "";
        if (values.length>=6)
            openedPath = decode(values[5]);

        String foreign = "";
        if (values.length>=7)
            foreign = decode(values[6]);

        int pageNumber = 0;
        if (values.length>=8)
            pageNumber = Integer.parseInt(values[7], 10);

        float scale = 1.0f;
        if (values.length>=9)
            scale = Float.parseFloat(values[8]);

        int scrollX = 0;
        if (values.length>=10)
            scrollX = Integer.parseInt(values[9], 10);

        int scrollY = 0;
        if (values.length>=11)
            scrollY = Integer.parseInt(values[10], 10);

        boolean pageListVisible = false;
        if (values.length>=12)
            pageListVisible = Boolean.parseBoolean(values[11]);

        //  create new object with these values
        SOFileState state = new SOFileState(userPath, internalPath, openedPath, lastAccess, hasChanges,
                thumbPath, database, pageNumber, scale, scrollX, scrollY, pageListVisible);
        state.setForeignData(foreign);
        return state;
    }

    //  base64 encoding
    private static String encode(String string)
    {
        //  handle null or empty.
        if (string==null)
            return NULL;
        if (string.isEmpty())
            return EMPTY;

        try {
            byte[] data = string.getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    //  base64 decoding
    private static String decode(String string)
    {
        //  handle null or empty.
        if (string.equals(NULL))
            return null;
        if (string.equals(EMPTY))
            return "";

        try {
            byte[] data = Base64.decode(string, Base64.DEFAULT);
            return new String(data,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public void updateAccess()
    {
        //  update the access time
        mLastAccess = System.currentTimeMillis();
    }

    private void persist()
    {
        // FIXME To fix annoying crash on startup
        if (mDatabase == null)
        {
            return;
        }

        //  update the database
        if (mUserPath!=null)
            mDatabase.setValue(mUserPath, this);
    }

    public void openFile(boolean template)
    {
        if (!FileUtils.fileExists(mInternalPath))
        {
            // Make the internal file a copy of the original
            FileUtils.copyFile(mUserPath, mInternalPath, true);
        }
        else
        {
            //  get a path to the file.
            String path = mUserPath;  //  will be null in the template case.
            if (path==null)
                path = mOpenedPath;

            //  if the original is newer than the last access time, make a new copy
            long lastMod = FileUtils.fileLastModified(path);

            //  FileUtils.fileLastModified may return 0, but the following time check
            //  is still valid in that case.
            if (lastMod > mLastAccess) {
                FileUtils.copyFile(mUserPath, mInternalPath, true);
            }
        }

        //  if this was a template, remove the user path (there is none yet)
        if (template) {
            mUserPath = null;
        }
        else
        {
            updateAccess();
            persist();
        }
    }

    //  called when a file is saved
    public void saveFile()
    {
        if (FileUtils.fileExists(mInternalPath))
        {
            //  Replace the original with our internal copy
            FileUtils.replaceFile(mInternalPath, mUserPath);
            mOpenedPath = mUserPath;
            mHasChanges = false;
            updateAccess();
            persist();
        }
    }

    //  called when a file is closed.
    public void closeFile()
    {
        //  We can now get rid of the internal copy
        FileUtils.deleteFile(mInternalPath);
        updateAccess();
        persist();
    }

    //  set the thumbnail path.
    public void setThumbnail(String path)
    {
        mThumbPath=path;
        persist();
    }

    //  get the thumbnail path.
    public String getThumbnail()
    {
        return mThumbPath;
    }

    //  delete the thumbnail.
    public void deleteThumbnailFile()
    {
        if (FileUtils.fileExists(mThumbPath))
        {
            FileUtils.deleteFile(mThumbPath);
        }
    }

    //  Preferences key for saving the state.
    private static final String AUTO_OPEN_KEY = "autoOpen";

    public static boolean mDontAutoOpen = false;

    //  set the saved auto-open state to the given state.
    public static void setAutoOpen(Context context, SOFileState state)
    {
        if (!mDontAutoOpen)
        {
            String val = SOFileState.toString(state);

            Object store =
                Utilities.getPreferencesObject(context,
                                               Utilities.generalStore);

            Utilities.setStringPreference(store, AUTO_OPEN_KEY, val);
        }
    }

    //  get the currently-saved auto-open state.
    public static SOFileState getAutoOpen(Context context)
    {
        Object store = Utilities.getPreferencesObject(context,
                                                      Utilities.generalStore);

        String val = Utilities.getStringPreference(store, AUTO_OPEN_KEY, "");
        if (val==null || val.isEmpty())
            return null;
        return SOFileState.fromString(val, SOFileDatabase.getDatabase());
    }

    //  remove any currently-saved auto-open state.
    public static void clearAutoOpen(Context context)
    {
        Object store = Utilities.getPreferencesObject(context,
                                                      Utilities.generalStore);

        Utilities.setStringPreference(store, AUTO_OPEN_KEY, "");
    }

}
