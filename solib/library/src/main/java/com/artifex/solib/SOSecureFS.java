package com.artifex.solib;

/**
 * This interface specifies the basis for implementing a class allowing
 * proprietary encrypted files, stored in a secure container, to be
 * accessed by SOLib.
 */

public interface SOSecureFS
{
    /**
     * This class defines the attributes associated with the decrypted  file.
     */
    public class FileAttributes
    {
        /**
         * The length of the file in bytes.
         */
        public long    length;

        /**
         * The time of last file modification in seconds since the epoch.
         */
        public long    lastModified;

        /**
         * True if the file is hidden.
         */
        public boolean isHidden;

        /**
         * True if the file represents a directory.
         */
        public boolean isDirectory;

        /**
         * True if the file is writeable.
         */
        public boolean isWriteable;

        /**
         * True if the file represents a system file.
         */
        public boolean isSystem;
    }

    /**
     * This method determines whether the supplied file path resides
     * within the secure container.<br><br>
     *
     * The path may be a pseudo path on which a mapping can be performed
     * to access the actual file. For example "/SECURE/filename".
     *
     * @param path The file path to be analysed.
     *
     * @return True if the file resides within the secure container.<br>
     *         False otherwise.
     */
    public boolean isSecurePath(final String path);

    /**
     * This method returns the directory to be used to store temporary
     * files created during file translation/saving.<br><br>
     *
     * This directory must reside within the secure container as identified
     * in {@link #isSecurePath(String)}.
     *
     * @return The (pseudo) path to the temporary directory.
     */
    public String getTempPath();

    /**
     * This method returns the relevant attributes of the file located
     * at the supplied path. The path will reference a file within the
     * secure container.<br><br>
     *
     * The attributes should refer to the properties of the decrypted file.
     * <br><br>
     *
     * The (pseudo) path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to obtain attributes for.
     *
     * @return A reference to a {@link FileAttributes} object.<br>
     *         null on error.
     */
    public FileAttributes getFileAttributes(final String path);

    /**
     * This method renames a file within the secure container.<br><br>
     *
     * Both source and destination file paths will reside within the
     * secure container and be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param src The path to the file to be renamed.
     * @param dst The path to the destination file.
     *
     * @return True on success. False on failure.
     */
    public boolean renameFile(final String src, final String dst);

    /**
     * This method copies a file to a new location within the secure
     * container..<br><br>
     *
     * Both source and destination file paths will reside within the
     * secure container and be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param src The path to the file to be copied.
     * @param dst The path to the destination file.
     *
     * @return True on success. False on failure.
     */
    public boolean copyFile(final String src, final String dst);

    /**
     * This method deletes a file from within the secure container..<br><br>
     *
     * The file path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to be deleted.
     *
     * @return True on success. False on failure.
     */
    public boolean deleteFile(final String path);

    /**
     * This method tests for the existence of a file within the secure
     * container.<br><br>
     *
     * The file path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to be checked.
     *
     * @return True if the file exists. False otherwise.
     */
    public boolean fileExists(final String path);

    /**
     * This method recursively deletes the supplied directory, and it's
     * sub-directories, located within the secure container.<br><br>
     *
     * The path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the directory to be deleted.
     *
     * @return True on success. False on failure.
     */
    public boolean recursivelyRemoveDirectory(final String path);

    /**
     * This method creates a directory, and all non-existent directories in
     * the supplied path, within the secure container.<br><br>
     *
     * The path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the directory to be created.
     *
     * @return True on success. False on failure.
     */
    public boolean createDirectory(final String path);

    /**
     * This method creates a file within the secure container.<br><br>
     *
     * The file path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to be created.
     *
     * @return True on success. False on failure.
     */
    public boolean createFile(final String path);

    /**
     * This method opens an existing file, in the secure container, for
     * reading.<br><br>
     *
     * The file path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to be opened.
     *
     * @return The file handle object on success.<br>
     *         null on error.
     */
    public Object getFileHandleForReading(final String path);

    /**
     * This method opens an existing file, in the secure container, for
     * writing.<br><br>
     *
     * The file path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to be opened.
     *
     * @return The file handle object on success.<br>
     *         null on error.
     */
    public Object getFileHandleForWriting(final String path);

    /**
     * This method opens an existing file, in the secure container, for
     * updating.<br><br>
     *
     * The file path will be constructed using the secure container
     * identifier used in {@link #isSecurePath(String)}.
     *
     * @param path The path to the file to be opened.
     *
     * @return The file handle object on success.<br>
     *         null on error.
     */
    public Object getFileHandleForUpdating(final String path);

    /**
     * This method closes an open file within the secure container.
     *
     * @param handle The file handle object to be closed.
     *
     * @return True on success. False on failure.
     */
    public boolean closeFile(final Object handle);

    /**
     * This method sets the file length of a file located within the
     * secure container.
     *
     * @param handle The file handle object to be used..
     *
     * @return True on success. False on failure.
     */
    public boolean setFileLength(final Object handle, final long length);

    /**
     * This method reads data from a file located within the secure container.
     *
     * @param handle The file handle object to be used..
     * @param buf    The buffer to put the read data in.
     *
     * @return The amount of data placed in the buffer.<br>
     *          0 on EOF<br>
     *         -1 on error.
     */
    public int readFromFile(final Object handle,
                            final byte   buf[]);

    /**
     * This method writes data to a file located within the secure container.
     *
     * @param handle The file handle object to be used.
     * @param buf    The buffer containing the data.
     *
     * @return The amount of data written.
     *         -1 on error.
     */
    public int writeToFile(final Object handle,
                           final byte   buf[]);

    /**
     * This method forces buffered data to be written to the underlying
     * device.
     *
     * @param handle The file handle object to be used.
     *
     * @return True on success. False on failure.
     */
    public boolean syncFile(final Object handle);

    /**
     * This method obtains the length of a file within the secure container.
     * <br><br>
     *
     * The length returned is that of the decrypted file.
     *
     * @param handle The file handle object to be used.
     *
     * @return The file length. -1 on error.
     */
    public long getFileLength(final Object handle);

    /**
     * This method obtains the offset, from the start of the file , of the
     * file pointer. <br><br>
     *
     * The offset returned relates to the decrypted file.
     *
     * @param handle The file handle object to be used.
     *
     * @return The file pointer offset. -1 on error.
     */
    public long getFileOffset(final Object handle);

    /**
     * This method moves the file pointer to the requested offset from 0.
     * <br><br>
     *
     * The offset relates to the decrypted file.
     *
     * @param handle The file handle object to be used.
     *
     * @return True on success. False on failure.
     */
    public boolean seekToFileOffset(final Object handle, final long ofset);

    /**
     * Return the physical root of the secure container.
     * <br><br>
     *
     * @return The physical path to the secure container.
     */
    public String getSecurePath();

    /**
     * Return the 'tag' to indicate the file is suitable for decryption.
     * <br><br>
     *
     * @return The secure 'tag' string.
     */
    public String getSecurePrefix();

}
