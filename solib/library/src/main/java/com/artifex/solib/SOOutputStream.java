package com.artifex.solib;

import android.util.Log;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.SecurityException;
import java.util.Arrays;

/**
 * This class implements an OutputStream that can be used to write to both
 * regular and secure files.
 */

public class SOOutputStream extends OutputStream
{
    private static final String mDebugTag     = "SOOutputStream";
    private final boolean       mDebugLogging = false;

    private SOSecureFS mSecureFs;
    private Object     mOutputStream;

    /**
     * Constructor
     *
     * @param path Full path to the 'input' file.
     */
    public SOOutputStream(String path)
    {
        if (mDebugLogging)
        {
            Log.d(mDebugTag, "Path: [" + path + "]");
        }

        mSecureFs = ArDkLib.getSecureFS();

        if (mSecureFs != null && mSecureFs.isSecurePath(path))
        {
            /*
             * The output file is in a secure container.
             *
             * Use the registered SOSecureFS implementation to work with it.
             */
            mOutputStream = mSecureFs.getFileHandleForWriting(path);
        }
        else
        {
            // Open the file as a FileOutputStream.
            try
            {
                mOutputStream = (Object) new FileOutputStream(path);
            }
            catch (FileNotFoundException e)
            {
                mOutputStream = null;
                e.printStackTrace();
            }
            catch (SecurityException e)
            {
                mOutputStream = null;
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     */
    @Override
    public void close()
    {
        if (mOutputStream!=null && mOutputStream instanceof FileOutputStream)
        {
            FileOutputStream stream = (FileOutputStream)mOutputStream;

            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                Log.e(mDebugTag, "Error: closing FileOutputStream");
            }
        }
        else
        {
            if (mSecureFs!=null && mOutputStream!=null)
                mSecureFs.closeFile(mOutputStream);
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be
     * written out.
     */
    @Override
    public void flush()
    {
        if (mOutputStream instanceof FileOutputStream)
        {
            FileOutputStream stream = (FileOutputStream)mOutputStream;

            try
            {
                stream.flush();
            }
            catch (IOException e)
            {
                Log.e(mDebugTag, "Error: flushing FileOutputStream");
            }
        }
        else
        {
            if (! mSecureFs.syncFile(mOutputStream))
            {
                Log.e(mDebugTag, "Error: flushing SecureFS");
            }
        }
    }

    /**
     * Writes b.length bytes from the specified byte array to this output
     * stream.
     */
    @Override
    public void write (byte[] b)
    {
        if (mOutputStream instanceof FileOutputStream)
        {
            FileOutputStream stream = (FileOutputStream)mOutputStream;

            try
            {
                stream.write(b);
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: writing byte array to FileOutputStream");
            }
        }
        else
        {
            int count = mSecureFs.writeToFile(mOutputStream, b);

            if (count == -1)
            {
                Log.e(mDebugTag,
                      "Error: writing byte array to SecureFS");
            }
        }
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off
     * to this output stream.
     *
     * @param b   The data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     */
    @Override
    public void write (byte[] b, int off, int len)
    {
        if (mOutputStream instanceof FileOutputStream)
        {
            FileOutputStream stream = (FileOutputStream)mOutputStream;

            try
            {
                stream.write(b, off, len);
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: writing offset byte array to FileOutputStream");
            }
        }
        else
        {
            byte[] subArray = Arrays.copyOfRange(b, off, off + len);
            int count       = mSecureFs.writeToFile(mOutputStream, subArray);

            if (count == -1)
            {
                Log.e(mDebugTag,
                      "Error: writing offset byte array to SecureFS");
            }
        }
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param b The byte
     */
    @Override
    public void write (int b)
    {
        if (mOutputStream instanceof FileOutputStream)
        {
            FileOutputStream stream = (FileOutputStream)mOutputStream;

            try
            {
                stream.write(b);
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: writing byte to FileOutputStream");
            }
        }
        else
        {

            byte[] bArray = { (byte)(b & 0xFF) };
            int    count = mSecureFs.writeToFile(mOutputStream, bArray);

            if (count == -1)
            {
                Log.e(mDebugTag,
                      "Error: writing byte to SecureFS");
            }
        }
    }
}
