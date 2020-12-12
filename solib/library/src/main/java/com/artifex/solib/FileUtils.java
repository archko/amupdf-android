package com.artifex.solib;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

public class FileUtils
{
    private static final String mDebugTag = "FileUtils";
    private static SOSecureFS mSecureFs   = null;
    private static String     mHomeDir    = null;

    public static void init(Context context)
    {
        String errorBase =
            "init() experienced unexpected exception [%s]";

        try
        {
            //  find a registered instance
            mSecureFs = ArDkLib.getSecureFS();
            if (mSecureFs==null)
                throw new ClassNotFoundException();

        }
        catch (ExceptionInInitializerError e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                             "ExceptionInInitializerError"));
        }
        catch (LinkageError e)
        {
            Log.e(mDebugTag, String.format(errorBase, "LinkageError"));
        }
        catch (SecurityException e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                                           "SecurityException"));
        }
        catch (ClassNotFoundException e)
        {
            Log.i(mDebugTag, "SecureFS implementation unavailable");
        }
    }

    public static long fileSize(String path)
    {
        long numBytes = 0;

        if (path == null)
            return numBytes;

        // Use secure file access primitives if available.
        if (mSecureFs != null)
        {
            SOSecureFS.FileAttributes
                    attribs = mSecureFs.getFileAttributes(path);

            //  fix 701994: some implementations of SOSecureFs.getFileAttributes
            //  may return null, so we return 0;
            if (attribs!=null)
                return attribs.length;
            return numBytes;
        }

        File file = new File(path);
        if( file != null)
            numBytes = file.length();

        return numBytes;
    }

    public static long fileLastModified(String path)
    {
        long lastMod = 0;

        if (path == null)
            return lastMod;

        // Use secure file access primitives if available.
        if (mSecureFs != null)
        {
            SOSecureFS.FileAttributes
                attribs = mSecureFs.getFileAttributes(path);

            //  fix 701994: some implementations of SOSecureFs.getFileAttributes
            //  may return null, so we return 0;
            if (attribs!=null)
                return attribs.lastModified;
            return lastMod;
        }

        File file = new File(path);
        if( file != null)
            lastMod = file.lastModified();

        return lastMod;
    }

    public static boolean fileExists(String path)
    {
        if (path == null)
            return false;

        // Use secure file access primitives if available.
        if (mSecureFs != null && mSecureFs.isSecurePath(path))
        {
            return mSecureFs.fileExists(path);
        }

        File file = new File(path);

        return (file != null) && file.exists();
    }

    public static void setHomeDirectory(String homeDir)
    {
        mHomeDir = homeDir;
    }

    public static File getHomeDirectory(Context context)
    {
        //  the home directory is where "My Documents" are stored.
        if (mHomeDir != null)
        {
            if (! fileExists(mHomeDir))
            {
                createDirectory(mHomeDir);
            }

            return new File(mHomeDir);
        }

        File root = Environment.getExternalStorageDirectory();
        return new File(root, "Documents");
    }

    /*
     * This method creates a directory, and all non-existent directories in
     * the supplied path.
     */
    public static boolean createDirectory(final String path)
    {
        if (path == null)
            return false;

        if (mSecureFs != null && mSecureFs.isSecurePath(path))
        {
            return mSecureFs.createDirectory(path);
        }
        else
        {
            try
            {
                return new File(path).mkdirs();
            }
            catch (SecurityException e)
            {
                return false;
            }
        }
    }

    private static boolean deleteRecursive(final String path)
    {
        File item = new File(path);

        try
        {
            if (item.isDirectory())
            {
                for (File child : item.listFiles())
                {
                    deleteRecursive(child.getPath());
                }
            }

            item.delete();

            return true;
        }
        catch (SecurityException e)
        {
            return false;
        }
        catch (NullPointerException e)
        {
            Log.e(mDebugTag, "deleteRecursive() failed  [" + path +
                             "]: " + "Have storage permissions been granted");

            return false;
        }
    }

    /*
     * This method removes a directory, and all it's sub-directories.
     */
    public static boolean removeDirectory(final String path)
    {
        if (path == null)
            return false;

        if (mSecureFs != null && mSecureFs.isSecurePath(path))
        {
            return mSecureFs.recursivelyRemoveDirectory(path);
        }
        else
        {
            return deleteRecursive(path);
        }
    }

    public static boolean copyFile(String src, String dst, boolean overwrite)
    {
        if (src == null || dst == null)
            return false;

        /*
         * Use secure file access primitives if available and either the
         * source or destination file resides within the secure container.
         */
        if (mSecureFs != null &&
            (mSecureFs.isSecurePath(src) || mSecureFs.isSecurePath(dst)))
        {
            if (mSecureFs.isSecurePath(dst))
            {
                // Handle any destination file in the secure container.
                if (! overwrite && mSecureFs.fileExists(dst))
                {
                    return false;
                }
                else if (overwrite && mSecureFs.fileExists(dst))
                {
                    boolean deleted = mSecureFs.deleteFile(dst);

                    if (! deleted)
                    {
                        return false;
                    }
                }
            }
            else
            {
                // Handle any destination file in the native file system.
                File dstf = new File(dst);

                //  if we can't overwrite, error
                if (!overwrite && dstf.exists())
                    return false;

                //  if we must overwrite but can't delete, error
                if (overwrite && dstf.exists()) {
                    boolean deleted = deleteFile(dst);
                    if (!deleted)
                        return false;
                }
            }

            return mSecureFs.copyFile(src, dst);
        }

        File dstf = new File(dst);
        File srcf = new File(src);

        if (dstf == null || srcf == null)
            return false;

        //  if we can't overwrite, error
        if (!overwrite && dstf.exists())
            return false;

        //  if we must overwrite but can't delete, error
        if (overwrite && dstf.exists()) {
            boolean deleted = deleteFile(dst);
            if (!deleted)
                return false;
        }

        //  now copy
        return copyWithStreams(srcf, dstf);
    }

    public static boolean deleteFile (String path)
    {
        if (path == null)
            return false;

        // Use secure file access primitives for secure files.
        if (mSecureFs != null && mSecureFs.isSecurePath(path))
        {
            return mSecureFs.deleteFile(path);
        }

        try
        {
            File file         = new File(path);
            File fileToDelete = new File(file.getAbsolutePath());
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
        }
        catch(Exception e)
        {
            return false;
        }

        return true;
    }

    public static boolean renameFile (String oldPath, String newPath)
    {
        if (oldPath == null || newPath == null )
            return false;


        // Use secure file access primitives if available.
        if (mSecureFs != null)
        {
            return mSecureFs.renameFile(oldPath, newPath);
        }

        File fOld = new File(oldPath);
        File fNew = new File(newPath);
        return fOld.renameTo(fNew);
    }

    //  this function safely replaces a file by first renaming it
    //  and, if the copy fails, renaming it back.
    public static boolean replaceFile (String srcPath, String dstPath)
    {
        //  source file must exist
        if (! fileExists(srcPath))
        {
            return false;
        }

        //  destination file may or may not exist
        boolean dstExists = fileExists(dstPath);
        String tmp        = dstPath + "xxx";

        //  if tmp exists, error
        if (fileExists(tmp))
        {
            return false;
        }

        //  rename the destination temporarily
        if (dstExists)
        {
            if (!renameFile(dstPath,tmp))
            {
                //  rename error, do nothing else.
                return false;
            }
        }

        //  copy the file
        if (!copyFile(srcPath,dstPath,true)) {
            //  copy failed, put the destination back
            if (dstExists) {
                if (!renameFile(tmp,dstPath)) {
                    //  bad mojo here.  Can't rename back,
                    //  file appears lost.
                }
            }
            return false;
        }

        //  copy succeeded, now delete the tmp file
        deleteFile(tmp);

        return true;
    }

    // This method opens an existing file for reading.
    public static Object getFileHandleForReading(final String path)
    {
        if (path == null)
            return null;

        if (mSecureFs != null)
        {
            return mSecureFs.getFileHandleForReading(path);
        }
        else
        {
            try
            {
                return (Object)new FileInputStream(path);
            }
            catch (FileNotFoundException e)
            {
                return null;
            }
            catch (SecurityException e)
            {
                return null;
            }
        }
    }

    // This method reads data from a file.
    public static int readFromFile(final Object handle, final byte buf[])
    {
        if (handle == null)
            return -1;

        if (mSecureFs != null)
        {
            return mSecureFs.readFromFile(handle, buf);
        }
        else
        {
            try
            {
                return ((FileInputStream)handle).read(buf);
            }
            catch (IOException e)
            {
                return -1;
            }
        }
    }

    // This method closes an open file.
    public static boolean closeFile(final Object handle)
    {
        if (mSecureFs != null)
        {
            return mSecureFs.closeFile(handle);
        }
        else
        {
            try {
                if (handle != null)
                    ((FileInputStream) handle).close();
                return (handle != null);
            }
            catch (IOException e)
            {
                return false;
            }
        }
    }

    // This method returns the root folder to be used for temporary storage.
    public static String getTempPathRoot(Context context)
    {
        if (mSecureFs != null)
        {
            return mSecureFs.getTempPath();
        }
        else
        {
            return context.getFilesDir().toString();
        }
    }

    //  get the extension part of the filename, not including the "."
    public static  String getExtension(String filename)
    {
        if (filename == null)
            return "";

        String filenameArray[] = filename.split("\\.");

        if (filenameArray.length<=1)
        {
            //  no extension
            return "";
        }

        String extension = filenameArray[filenameArray.length-1];
        extension = extension.toLowerCase();
        return extension;
    }

    public static String extractAssetToSecureFile(Context context, String file)
        throws IOException
    {
        String cachePath = getTempPathRoot(context);

        // Ensure the temporary directory exists.
        if (! fileExists(cachePath))
        {
            if (! createDirectory(cachePath))
            {
                throw new IOException();
            }
        }

        String cacheFilePath = cachePath + File.separator + file;

        if (fileExists(cacheFilePath))
        {
            deleteFile(cacheFilePath);
        }

        Object cacheFile     = mSecureFs.getFileHandleForWriting(cacheFilePath);

        if (cacheFile == null)
        {
            throw new IOException();
        }

        try
        {
            InputStream inputStream = context.getAssets().open(file);
            try
            {
                try
                {
                    byte[] buf = new byte[1024];
                    int    len;

                    while ((len = inputStream.read(buf)) > 0)
                    {
                        mSecureFs.writeToFile(cacheFile, Arrays.copyOf(buf, len));
                    }
                } finally
                {
                    mSecureFs.closeFile(cacheFile);
                }
            } finally
            {
                inputStream.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        return cacheFilePath;
    }

    public static String extractAssetToCacheFile(Context context, String file)
    {
        // Copy the document to the seure container in the secure case.
        if (mSecureFs != null)
        {
            try
            {
                return extractAssetToSecureFile(context, file);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        File cacheFile = new File(context.getCacheDir(), file);
        try
        {
            InputStream inputStream = context.getAssets().open(file);
            try
            {
                FileOutputStream outputStream = new FileOutputStream(cacheFile);
                try
                {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0)
                    {
                        outputStream.write(buf, 0, len);
                    }
                } finally
                {
                    outputStream.close();
                }
            } finally
            {
                inputStream.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        return cacheFile.getAbsolutePath();
    }

    public static String extractAssetToString(Context context, String file)
    {
        String json;
        try {
            InputStream is = context.getAssets().open(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static boolean canUseExternalStorage(Context context)
    {
        /*
         *  When SecureFS is use only the secure container is a valid file
         *  store.
         */
        if (mSecureFs != null)
        {
            return false;
        }

        boolean canWrite = (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        boolean canRead  = (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)  == PackageManager.PERMISSION_GRANTED);
        if (!canRead || !canWrite)
            return false;

        String storageState = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(storageState) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState))
            return false;

        return true;
    }

    public static boolean canCreatePath(String path)
    {
        // This is not supported by the secure file system.
        if (mSecureFs != null)
        {
            Log.e(mDebugTag, "canCreatePath() not supported bu secure" +
                             "file system");

            throw new UnsupportedOperationException();
        }

        File f = new File(path);
        try {
            f.getCanonicalPath();
        }
        catch (IOException e) {
            return false;
        }

        if (!f.getParentFile().exists())
            return false;

        return true;
    }

    private static String extensionFromUriFilename(Uri uri)
    {
        String extension = "";

        String filename = new File(uri.getPath()).getName();
        int i = filename.lastIndexOf('.');
        if (i>0)
            extension = filename.substring(i+1);

        return extension;
    }

    public static String getFileTypeExtension(Context context, Uri uri)
    {
        return getFileTypeExtension(context, uri, null);
    }

    public static String getFileTypeExtension(Context context, Uri uri, String mimeFallback)
    {
        String extension = "";

        //  Check uri format to avoid null
        String scheme = uri.getScheme();
        if (scheme!=null &&
            scheme.equalsIgnoreCase(ContentResolver.SCHEME_CONTENT))
        {
            //  If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();

            //  get mime type from the Uri
            String type = context.getContentResolver().getType(uri);

            //  if that's null, use the one we're given as a fallback.
            if (type==null)
                type = mimeFallback;

            if (type.equalsIgnoreCase("application/vnd.ms-xpsdocument") ||
                    type.equalsIgnoreCase("application/oxps"))
            {
                //  Google Drive uses these mime types for XPS
                extension = "xps";
            }
            else if (type.equalsIgnoreCase("application/octet-stream"))
            {
                //  some apps use this generic mime type.
                //  we can try to get the extension a different way
                String name = FileUtils.fileNameFromUri(context, uri);
                if (name!=null)
                    extension = FileUtils.getExtension(name);
            }
            else
            {
                //  get the extension from the mime type
                extension = mime.getExtensionFromMimeType(type);
            }

            //  getExtensionFromMimeType can fail in some cases, so get the
            //  extension from end of the filename
            if (extension==null)
                extension = extensionFromUriFilename(uri);
        }
        else
        {
            /*
             *  If scheme is a File or other.
             *
             *  We used to use MimeTypeMap.getFileExtensionFromUrl here,
             *  but that fails when the filename or path contains "~".
             *
             *  So instead, we just search backwards in the filename for a "."
             *  this fixes https://bugs.ghostscript.com/show_bug.cgi?id=699910
             */
            extension = extensionFromUriFilename(uri);
        }

        return extension;
    }

    private static String fileNameFromUri(Context context, Uri uri)
    {
        String scheme    = uri.getScheme();
        String path      = uri.getPath();
        String fullPath  = uri.toString();
        String fileName  = "file";;

        if (scheme != null &&
            scheme.equalsIgnoreCase(ContentResolver.SCHEME_CONTENT))
        {
            ContentResolver cr          = context.getContentResolver();
            String          displayName = null;

            try
            {
                String[] projections = {OpenableColumns.DISPLAY_NAME};
                Cursor   cursor      = cr.query(uri, projections, null,
                                                null, null);

                if (cursor != null)
                {
                    if (cursor.moveToFirst())
                    {
                        displayName = cursor.getString(
                           cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        cursor.close();
                    }
                }
            }
            catch (Exception e)
            {
            }

            /*If we can't get a display name, then make one up*/
            if (displayName == null)
            {
                if(fullPath.contains("attachment") || fullPath.contains("mail"))
                {
                    displayName = "attachment";
                }
                else
                {
                    displayName = "content";
                }

                String extension = getFileTypeExtension(context, uri);
                if (extension != null && extension.length() > 0)
                    displayName += "." + extension;
            }

            fileName = displayName;
        }
        else
        {
            if (path != null)
            {
                fileName = new File(path).getName();
            }
        }

        return fileName;
    }

    public static String exportContentUri(Context context, Uri uri)
    {
        String path = "";

        //  get the file's name
        String name = fileNameFromUri(context, uri);

        if (getExtension(name).equals(""))
        {
            //  if the name has no extension, add it.
            String ext = getFileTypeExtension(context, uri);
            name += "." + ext;
        }

        //  create input stream for the uri
        InputStream inStream;
        try {
            inStream = context.getContentResolver().openInputStream(uri);
        }
        catch (Exception e) {
            //  failed - return special value for this.
            return "---fileOpen";
        }

        try
        {
            //  create directory and file name for the output
            String directory = getTempPathRoot(context) + "/shared/" + UUID.randomUUID();
            createDirectory(directory);
            String tempfile = directory + "/" + name;

            //  delete any existing output file
            if (fileExists(tempfile))
                deleteFile(tempfile);

            //  create the output file
            Object handle = null;
            if (mSecureFs != null && mSecureFs.isSecurePath(tempfile))
                handle = mSecureFs.getFileHandleForWriting(tempfile);
            else
                handle = new FileOutputStream(tempfile);

            //  if the temp file cannot be created, let's just issue a message
            //  instead of crashing.
            //  although we're also now catching all exceptions (see below).
            if (handle == null)
            {
                return "---";  // just tells the caller we failed
            }

            //  write the data
            byte[] buffer = new byte[4096]; // To hold file contents
            int bytes_read; // How many bytes in buffer

            // Read a chunk of bytes into the buffer, then write them out,
            // looping until we reach the end of the file (when read() returns
            // -1). Note the combination of assignment and comparison in this
            // while loop. This is a common I/O programming idiom.

            while ((bytes_read = inStream.read(buffer)) != -1)
            {
                if (handle instanceof FileOutputStream)
                    ((FileOutputStream)handle).write(buffer, 0, bytes_read);
                else
                    mSecureFs.writeToFile(handle, Arrays.copyOf(buffer, bytes_read));
            }

            //  close the output
            if (handle != null) {
                if (handle instanceof FileOutputStream)
                    ((FileOutputStream)handle).close();
                else
                    mSecureFs.closeFile(handle);
            }

            //  success
            path = tempfile;

        } catch (IOException e) {
            e.printStackTrace();
            path = "---IOException " + e.getMessage();
        } catch (SecurityException e) {
            e.printStackTrace();
            path = "---SecurityException " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            path = "---Exception " + e.getMessage();
        }

        return path;
    }

    public static boolean copyWithStreams(File aSourceFile, File aTargetFile)
    {
        InputStream inStream = null;
        OutputStream outStream = null;

        boolean result = true;

        try
        {
            try
            {
                byte[] bucket = new byte[32*1024];
                inStream = new BufferedInputStream(new FileInputStream(aSourceFile));
                outStream = new BufferedOutputStream(new FileOutputStream(aTargetFile, false));
                int bytesRead = 0;
                while(bytesRead != -1)
                {
                    bytesRead = inStream.read(bucket); //-1, 0, or more
                    if(bytesRead > 0){
                        outStream.write(bucket, 0, bytesRead);
                    }
                }
            }
            catch (Exception e)
            {
                result = false;
            }
            finally
            {
                if (inStream != null)
                    inStream.close();
                if (outStream != null)
                    outStream.close();
            }
        }
        catch (FileNotFoundException ex){
            result = false;
        }
        catch (IOException ex){
            result = false;
        }

        return result;
    }

    //  test to see if the filename has the specified extension.
    public static boolean hasExtension(String filename, String extension)
    {
        return filename.toLowerCase().endsWith("."+extension);
    }

    //  see if a file extension is in a liat of extensions.
    public static boolean matchFileExtension(String ext, String[] types)
    {
        for (String type: types) {
            if (ext.toLowerCase().equals(type.toLowerCase()))
                return true;
        }
        return false;
    }

    //  test if the muPDF library will support the given filename
    //  by checking the file extension.
    public static boolean isDocSupportedByMupdf(String filename)
    {
        String ext = FileUtils.getExtension(filename);
        return matchFileExtension(ext, ArDkUtils.MUPDF_TYPES) || matchFileExtension(ext, ArDkUtils.IMG_TYPES);
    }

    static public boolean nameContainsUUID(String name)
    {
        //  this function determines whether the given name string contains within it
        //  a valid UUID.

        //  length of a valid string
        int uuidLen = 36;

        if (name==null)
            return false;  //  no string

        if (name.length()<uuidLen)
            return false;  //  too short

        //  regex patten for matching
        Pattern pattern = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

        for (int i=0; i<name.length()-uuidLen; i++)
        {
            //  extract a candidate uuid at this position
            String uuidMaybe = name.substring(i, uuidLen+i);

            //  does it match?
            if(pattern.matcher(uuidMaybe).matches())
                return true;
        }

        //  no match.
        return false;
    }

    public static boolean isDocTypeDoc(String path)
    {
        String ext = getExtension(path);
        return matchFileExtension(ext, ArDkUtils.DOC_TYPES) || matchFileExtension(ext, ArDkUtils.DOCX_TYPES);
    }

    public static boolean isDocTypeExcel(String path) {
        String ext = getExtension(path);
        return matchFileExtension(ext, ArDkUtils.XLS_TYPES) || matchFileExtension(ext, ArDkUtils.XLSX_TYPES);
    }

    public static boolean isDocTypePowerPoint(String path) {
        String ext = getExtension(path);
        return matchFileExtension(ext, ArDkUtils.PPT_TYPES) || matchFileExtension(ext, ArDkUtils.PPTX_TYPES);
    }
}
