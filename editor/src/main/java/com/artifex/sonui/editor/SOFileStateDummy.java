package com.artifex.sonui.editor;

import android.content.Context;

/*
 * This subclassing of the SOFileState class is intended to allow the existing
 * use of SOFileState to run without modification, while avoiding any:
 *
 *  - Copying of the original source file to a temporary location to be
 *    worked on.
 *  - The use of a file state database.
 *  - Thumbnail generation
 *
 * Additionally no SOFilaDatabase is used.
 */
public class SOFileStateDummy extends SOFileState
{
    //  public constructor
    public SOFileStateDummy(String userPath)
    {
        /*
         * Set user and internal paths to be identical
         * and register null SOFileDatabase.
         */
        super(userPath, userPath, null, 0);
    }

    @Override
    public void openFile(boolean template)
    {
    }

    @Override
    public void saveFile()
    {
    }

    @Override
    public void closeFile()
    {
    }

    @Override
    public void setThumbnail(String path)
    {
    }

    @Override
    public String getThumbnail()
    {
        return null;
    }

    @Override
    public void deleteThumbnailFile()
    {
    }
}
