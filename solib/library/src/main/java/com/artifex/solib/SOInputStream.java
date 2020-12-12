package com.artifex.solib;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.SecurityException;
import java.lang.System;

import android.util.Log;

/**
 * This class implements an InputStream that can be used to read from both
 * regular and secure files.
 */

public class SOInputStream extends InputStream
{
    private static final String mDebugTag     = "SOInputStream";
    private final boolean       mDebugLogging = false;

    private SOSecureFS mSecureFs;
    private Object     mInputStream;

    /**
     * Constructor
     *
     * @param path Full path to the 'input' file.
     */
    public SOInputStream(String path)
    {
        if (mDebugLogging)
        {
            Log.d(mDebugTag, "Path: [" + path + "]");
        }

        mSecureFs = ArDkLib.getSecureFS();

        if (mSecureFs != null && mSecureFs.isSecurePath(path))
        {
            /*
             * The input file is in a secure container.
             *
             * Use the registered SOSecureFS implementation to work with it.
             */
            mInputStream = mSecureFs.getFileHandleForReading(path);
        }
        else
        {
            // Open the file as a FileInputStream.
            try
            {
                mInputStream = (Object) new FileInputStream(path);
            }
            catch (FileNotFoundException e)
            {
                mInputStream = null;
                e.printStackTrace();
            }
            catch (SecurityException e)
            {
                mInputStream = null;
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream.
     *
     * @return The estimate. 0 on EOF.
     */
    @Override
    public int available()
    {
        int availableVal = 0;

        if (mInputStream instanceof FileInputStream)
        {
            try
            {
                FileInputStream stream = (FileInputStream)mInputStream;
                availableVal           = stream.available();
            }
            catch (IOException e)
            {
                Log.e(mDebugTag, "Available: Error in FileInputStream");
                return 0;
            }
        }
        else
        {
            // Return the file size.
            availableVal = (int)mSecureFs.getFileLength(mInputStream);

            if (availableVal == -1)
            {
                Log.e(mDebugTag, "Available: Error in SecureFS");
                return 0;
            }
        }

        if (mDebugLogging)
        {
            Log.d(mDebugTag, "Available: [" + String.valueOf(availableVal) +
                             "] bytes");
        }

        return availableVal;
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     */
    @Override
    public void close()
    {
        if (mInputStream instanceof FileInputStream)
        {
            FileInputStream stream = (FileInputStream)mInputStream;

            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                Log.e(mDebugTag, "Error: closing FileInputStream");
            }
        }
        else
        {
            if (! mSecureFs.closeFile(mInputStream))
            {
                Log.e(mDebugTag, "Error: closing SecureFS");
            }
        }
    }

    /**
     * Marks the current position in this input stream.
     *
     * This is not implemented for the SecureFS case. The feature is
     * disabled in markSupported().
     *
     * @param readLimit The maximum limit of bytes that can be read before the
     *                  mark position becomes invalid.
     */
    @Override
    public void mark(int readLimit)
    {
        if (mInputStream instanceof FileInputStream)
        {
            FileInputStream stream = (FileInputStream)mInputStream;

            stream.mark(readLimit);
        }
    }

    /**
     * Tests if this input stream supports the mark and reset methods
     *
     * @return True if it is, False if not.
     */
    @Override
    public boolean markSupported()
    {
        // Supported for FileInputStream
        if (mInputStream instanceof FileInputStream)
        {
            FileInputStream stream = (FileInputStream)mInputStream;

            return stream.markSupported();
        }

        // Not supported for SecureFS.
        return false;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return The byte. -1 on EOF
     */
    @Override
    public int read()
    {
        if (mInputStream instanceof FileInputStream)
        {
            FileInputStream stream = (FileInputStream)mInputStream;

            try
            {
                return stream.read();
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: reading byte from FileInputStream");

                return -1;
            }
        }
        else
        {
            byte[] bArray = new byte[1];
            int    count  = mSecureFs.readFromFile(mInputStream, bArray);

            if (count == -1 || count == 0)
            {
                Log.e(mDebugTag, "Error: reading byte from SecureFS");

                return -1;
            }

            return (int)bArray[0];
        }
    }

    /**
     * Reads up to len bytes of data from the input stream into an array of
     * bytes.
     *
     * @param b   The buffer into which the data is read.
     * @param off The start offset in array b at which the data is written.
     * @param len The maximum number of bytes to read.
     *
     * @return The total number of bytes read into the buffer, or -1 if there
     *         is no more data because the end of the stream has been reached.
     */
    @Override
    public int read (byte[] b, int off, int len)
    {
        if (b == null)
        {
            return -1;
        }

        if (len == 0)
        {
            return 0;
        }

        if (mInputStream instanceof FileInputStream)
        {
            FileInputStream stream = (FileInputStream)mInputStream;

            try
            {
                return stream.read(b, off, len);
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: reading offset byte array from FileInputStream");

                return -1;
            }
        }
        else
        {
            byte[] subArray = new byte[len];
            int    count    = mSecureFs.readFromFile(mInputStream, subArray);

            if (count == -1 || count == 0)
            {
                Log.e(mDebugTag,
                      "Error: reading offset byte array from SecureFS");

                return -1;
            }

            System.arraycopy((Object)subArray, 0, (Object)b, off, count);

            return count;
        }
    }

    /**
     * Reads some number of bytes from the input stream and stores them into
     * the buffer array b
     *
     * @param b   The buffer into which the data is read.
     *
     * @return The total number of bytes read into the buffer, or -1 if there
     *         is no more data because the end of the stream has been reached.
     */
    @Override
    public int read (byte[] b)
    {
        if (b == null)
        {
            return -1;
        }

        if (b.length == 0)
        {
            return 0;
        }

        if (mInputStream instanceof FileInputStream)
        {
            FileInputStream stream = (FileInputStream)mInputStream;

            try
            {
                return stream.read(b);
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: reading byte array from FileInputStream");

                return -1;
            }
            catch (NullPointerException e)
            {
                Log.e(mDebugTag,
                      "Error: reading byte array from FileInputStream - " +
                      "NullPointerException");

                return -1;
            }
        }
        else
        {
            int count = mSecureFs.readFromFile(mInputStream, b);

            if (count == -1 || count == 0)
            {
                Log.e(mDebugTag,
                      "Error: reading byte array from SecureFS");

                return -1;
            }

            return count;
        }
    }

    /**
     * Repositions this stream to the position at the time the mark method
     * was last called on this input stream.
     *
     * This is not implemented for the SecureFS case. The feature is
     * disabled in markSupported().
     */
    @Override
    public void reset()
    {
        if (mInputStream instanceof FileInputStream)
        {
            try
            {
                FileInputStream stream = (FileInputStream)mInputStream;
                stream.reset();
            }
            catch (IOException e)
            {
                Log.e(mDebugTag,
                      "Error: resetting FileInputStream");
            }
        }
    }

    /**
     * Skips over and discards n bytes of data from this input stream
     */
    @Override
    public long skip(long n)
    {
        // Not implemented.
        return 0;
    }
}
