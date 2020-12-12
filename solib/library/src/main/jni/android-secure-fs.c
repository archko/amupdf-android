/**
 * Android Wrapper for the SmartOffice Library secure fs API
 *
 * Maps the library's securefs API into a JNI API.
 *
 * Copyright (C) Artifex, 2012-2016. All Rights Reserved.
 *
 * @author Artifex
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>

#include "android-jni.h"
#include "sol-secure-fs.h"

static jobject jSecureFSRef;
static jclass  jSecureFSClassRef;
static jclass  jFileAttributesClassRef;

static jmethodID SOSecureFS_closeFile_mid;
static jmethodID SOSecureFS_copyFile_mid;
static jmethodID SOSecureFS_createFile_mid;
static jmethodID SOSecureFS_deleteFile_mid;
static jmethodID SOSecureFS_fileExists_mid;
static jmethodID SOSecureFS_getFileAttributes_mid;
static jmethodID SOSecureFS_getFileHandleForReading_mid;
static jmethodID SOSecureFS_getFileHandleForWriting_mid;
static jmethodID SOSecureFS_getFileHandleForUpdating_mid;
static jmethodID SOSecureFS_getFileLength_mid;
static jmethodID SOSecureFS_getFileOffset_mid;
static jmethodID SOSecureFS_isSecurePath_mid;
static jmethodID SOSecureFS_readFromFile_mid;
static jmethodID SOSecureFS_renameFile_mid;
static jmethodID SOSecureFS_seekToFileOffset_mid;
static jmethodID SOSecureFS_setFileLength_mid;
static jmethodID SOSecureFS_syncFile_mid;
static jmethodID SOSecureFS_writeToFile_mid;

static jfieldID FileAttributes_lastModified_fid;
static jfieldID FileAttributes_length_fid;
static jfieldID FileAttributes_isDirectory_fid;
static jfieldID FileAttributes_isHidden_fid;
static jfieldID FileAttributes_isSystem_fid;
static jfieldID FileAttributes_isWriteable_fid;

/* Structures internal to this file */

/**
 * Structure containing information about our file handle
 */
typedef struct SecureFs_FileHandle
{
    char *path;     /**< The path to the file (used for debug only) */
    int   fd;       /**< The actual file descriptor                 */
} SecureFs_FileHandle;

/* Cache JNI method and field ID's for future use. */
JNIEXPORT void JNICALL
JNI_FN(SOLib_initSecureFS)(JNIEnv *env, jobject thiz, jobject impl)
{
    jclass  clazz;

    /* Return if no implementation object is available. */
    if (impl == NULL)
    {
        return;
    }

    /* Take a global reference to the SOSecureFS object. */
    jSecureFSRef = (jobject)(*env)->NewGlobalRef(env, impl);

    if (jSecureFSRef == NULL)
    {
        LOGE("SOLib_initSecureFS: Failed to obtain "
             "SOSecureFS global reference");

        goto error;
    }

    /* Take a local reference to the SOSecureFS class implementation. */
    clazz = (*env)->GetObjectClass(env, jSecureFSRef);

    if (clazz == NULL)
    {
        LOGE("SOLib_initSecureFS: Failed to obtain "
             "SOSecureFS class");

        goto error;
    }

    /* Take a global reference to the class to keep the method ids valid. */
    jSecureFSClassRef = (jobject)(*env)->NewGlobalRef(env, clazz);

    if (jSecureFSClassRef == NULL)
    {
        LOGE("SOLib_initSecureFS: Failed to obtain "
             "SOSecureFS class global reference");

        goto error;
    }

    /* Cache the method id for the "closeFile" method. */
    SOSecureFS_closeFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "closeFile",
                            "(Ljava/lang/Object;)Z");

    if (SOSecureFS_closeFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::closeFile");

        goto error;
    }

    /* Cache the method id for the "copyFile" method. */
    SOSecureFS_copyFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "copyFile",
                            "(Ljava/lang/String;Ljava/lang/String;)Z");

    if (SOSecureFS_copyFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::copyFile");

        goto error;
    }

    /* Cache the method id for the "createFile" method. */
    SOSecureFS_createFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "createFile",
                            "(Ljava/lang/String;)Z");

    if (SOSecureFS_createFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::createFile");

        goto error;
    }

    /* Cache the method id for the "deleteFile" method. */
    SOSecureFS_deleteFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "deleteFile",
                            "(Ljava/lang/String;)Z");

    if (SOSecureFS_deleteFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::deleteFile");

        goto error;
    }

    /* Cache the method id for the "fileExists" method. */
    SOSecureFS_fileExists_mid =
        (*env)->GetMethodID(env, clazz,
                            "fileExists",
                            "(Ljava/lang/String;)Z");

    if (SOSecureFS_fileExists_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::fileExists");

        goto error;
    }

    /* Cache the method id for the "getFileAttributes" method. */
    SOSecureFS_getFileAttributes_mid =
        (*env)->GetMethodID(env, clazz, "getFileAttributes",
           "(Ljava/lang/String;)Lcom/artifex/solib/SOSecureFS$FileAttributes;");

    if (SOSecureFS_getFileAttributes_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::getFileAttributes");

        goto error;
    }

    /* Cache the method id for the "getFileHandleForReading" method. */
    SOSecureFS_getFileHandleForReading_mid =
        (*env)->GetMethodID(env, clazz,
                            "getFileHandleForReading",
                            "(Ljava/lang/String;)Ljava/lang/Object;");

    if (SOSecureFS_getFileHandleForReading_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::getFileHandleForReading");

        goto error;
    }

    /* Cache the method id for the "getFileHandleForWriting" method. */
    SOSecureFS_getFileHandleForWriting_mid =
        (*env)->GetMethodID(env, clazz,
                            "getFileHandleForWriting",
                            "(Ljava/lang/String;)Ljava/lang/Object;");

    if (SOSecureFS_getFileHandleForWriting_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::getFileHandleForWriting");

        goto error;
    }

    /* Cache the method id for the "getFileHandleForUpdating" method. */
    SOSecureFS_getFileHandleForUpdating_mid =
        (*env)->GetMethodID(env, clazz,
                            "getFileHandleForUpdating",
                            "(Ljava/lang/String;)Ljava/lang/Object;");

    if (SOSecureFS_getFileHandleForUpdating_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::getFileHandleForUpdating");

        goto error;
    }

    /* Cache the method id for the "getFileLength" method. */
    SOSecureFS_getFileLength_mid =
        (*env)->GetMethodID(env, clazz,
                            "getFileLength",
                            "(Ljava/lang/Object;)J");

    if (SOSecureFS_getFileLength_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::getFileLength");

        goto error;
    }

    /* Cache the method id for the "getFileOffset" method. */
    SOSecureFS_getFileOffset_mid =
        (*env)->GetMethodID(env, clazz,
                            "getFileOffset",
                            "(Ljava/lang/Object;)J");

    if (SOSecureFS_getFileOffset_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::getFileOffset");

        goto error;
    }

    /* Cache the method id for the "isSecurePath" method. */
    SOSecureFS_isSecurePath_mid =
        (*env)->GetMethodID(env, clazz,
                            "isSecurePath", "(Ljava/lang/String;)Z");

    if (SOSecureFS_isSecurePath_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::isSecurePath");

        goto error;
    }

    /* Cache the method id for the "readFromFile" method. */
    SOSecureFS_readFromFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "readFromFile",
                            "(Ljava/lang/Object;[B)I");

    if (SOSecureFS_readFromFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::readFromFile");

        goto error;
    }

    /* Cache the method id for the "renameFile" method. */
    SOSecureFS_renameFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "renameFile",
                            "(Ljava/lang/String;Ljava/lang/String;)Z");

    if (SOSecureFS_renameFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::renameFile");

        goto error;
    }

    /* Cache the method id for the "seekToFileOffset" method. */
    SOSecureFS_seekToFileOffset_mid =
        (*env)->GetMethodID(env, clazz,
                            "seekToFileOffset",
                            "(Ljava/lang/Object;J)Z");

    if (SOSecureFS_seekToFileOffset_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::seekToFileOffset");

        goto error;
    }

    /* Cache the method id for the "setFileLength" method. */
    SOSecureFS_setFileLength_mid =
        (*env)->GetMethodID(env, clazz,
                            "setFileLength",
                            "(Ljava/lang/Object;J)Z");

    if (SOSecureFS_setFileLength_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::setFileLength");

        goto error;
    }

    /* Cache the method id for the "syncFile" method. */
    SOSecureFS_syncFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "syncFile",
                            "(Ljava/lang/Object;)Z");

    if (SOSecureFS_syncFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::syncFile");

        goto error;
    }

    /* Cache the method id for the "writeToFile" method. */
    SOSecureFS_writeToFile_mid =
        (*env)->GetMethodID(env, clazz,
                            "writeToFile",
                            "(Ljava/lang/Object;[B)I");

    if (SOSecureFS_writeToFile_mid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain method id for "
             "SecureFS::writeToFile");

        goto error;
    }

    /* Cache the field ids for FileAttribute class members. */
    clazz = (*env)->FindClass(env,
                              "com/artifex/solib/SOSecureFS$FileAttributes");

    if ((*env)->ExceptionCheck(env))
    {
        (*env)->ExceptionClear(env);

        LOGE("SOLib_initSecureFS: Cannot Locate SOSecureFS$FileAttributes "
             "class");

        goto error;
    }

    /* Take a global reference to the class to keep the method id valid. */
    jFileAttributesClassRef = (jobject)(*env)->NewGlobalRef(env, clazz);

    /* Cache the field ids for the FileAttributes class */
    FileAttributes_isDirectory_fid =
        (*env)->GetFieldID(env, clazz, "isDirectory", "Z");

    if (FileAttributes_isDirectory_fid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain field id for "
             "FileAttributes::isDirectory");

        goto error;
    }

    FileAttributes_isHidden_fid =
        (*env)->GetFieldID(env, clazz, "isHidden", "Z");

    if (FileAttributes_isHidden_fid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain field id for "
             "FileAttributes::isHidden");

        goto error;
    }

    FileAttributes_isSystem_fid =
        (*env)->GetFieldID(env, clazz, "isSystem", "Z");

    if (FileAttributes_isSystem_fid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain field id for "
             "FileAttributes::isSystem");

        goto error;
    }

    FileAttributes_isWriteable_fid =
        (*env)->GetFieldID(env, clazz, "isWriteable", "Z");

    if (FileAttributes_isWriteable_fid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain field id for "
             "FileAttributes::isWriteable");

        goto error;
    }

    FileAttributes_lastModified_fid =
        (*env)->GetFieldID(env, clazz, "lastModified", "J");

    if (FileAttributes_lastModified_fid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain field id for "
             "FileAttributes::lastModified");

        goto error;
    }

    FileAttributes_length_fid =
        (*env)->GetFieldID(env, clazz, "length", "J");

    if (FileAttributes_length_fid == NULL)
    {
        LOGE("SOLib_initSecureFS: Cannot obtain field id for "
             "FileAttributes::length");

        goto error;
    }

    return;

error:
    (*env)->DeleteGlobalRef(env, jSecureFSRef);
    jSecureFSRef = NULL;

    (*env)->DeleteGlobalRef(env, jSecureFSClassRef);
    jSecureFSClassRef = NULL;

    (*env)->DeleteGlobalRef(env, jFileAttributesClassRef);
    jFileAttributesClassRef = NULL;

    return;
}

/* Tidy Up */
JNIEXPORT void JNICALL
JNI_FN(SOLib_finSecureFS)(JNIEnv *env, jobject thiz)
{
    /* Delete Global References. */
    (*env)->DeleteGlobalRef(env, jSecureFSRef);
    jSecureFSRef = NULL;

    (*env)->DeleteGlobalRef(env, jSecureFSClassRef);
    jSecureFSClassRef = NULL;

    (*env)->DeleteGlobalRef(env, jFileAttributesClassRef);
    jFileAttributesClassRef = NULL;

    return;
}

int SecureFs_getFileProperties(const char              *path,
                               SecureFs_FileProperties *properties)
{
    int      retVal = -1;
    JNIEnv  *env    = ensureJniAttached();
    jstring  fname  = NULL;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (path == NULL || properties == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
       fname = (*env)->NewStringUTF(env, (const char *)path);
    }

    if (fname != NULL)
    {
        jobject  fileAttributes;
        jlong    longVal;
        jboolean boolVal;

        /* Initialise the properties structure before use. */
        *properties = (SecureFs_FileProperties){0};

        /* Obtain a reference to the FileAttributes class for this file */
        fileAttributes =
            (*env)->CallObjectMethod(env,
                                     jSecureFSRef,
                                     SOSecureFS_getFileAttributes_mid,
                                     fname);

        if (fileAttributes == NULL)
        {
            LOGE("Could not obtain the FileAttributesL");
            goto finish;
        }

        /* Read the class 'isDirectory' field */
        boolVal = (*env)->GetBooleanField(env,
                                          fileAttributes,
                                          FileAttributes_isDirectory_fid);

        if (boolVal)
        {
            properties->attrib |= SecureFs_FileAttrib_Dir;
        }

        /* Read the class 'isHidden' field */
        boolVal = (*env)->GetBooleanField(env,
                                          fileAttributes,
                                          FileAttributes_isHidden_fid);

        if (boolVal)
        {
            properties->attrib |= SecureFs_FileAttrib_Hidden;
        }

        /* Read the class 'isSystem' field */
        boolVal = (*env)->GetBooleanField(env,
                                          fileAttributes,
                                          FileAttributes_isSystem_fid);

        if (boolVal)
        {
            properties->attrib |= SecureFs_FileAttrib_System;
        }

        /* Read the class 'isWriteable' field */
        boolVal = (*env)->GetBooleanField(env,
                                          fileAttributes,
                                          FileAttributes_isWriteable_fid);

        if (! boolVal)
        {
            properties->attrib |= SecureFs_FileAttrib_ReadOnly;
        }

        /* Read the class 'lastModified' field */
        longVal = (*env)->GetLongField(env,
                                       fileAttributes,
                                       FileAttributes_lastModified_fid);

        properties->modificationDate = (uint64_t)longVal;

        /* Read the class 'length' field */
        longVal = (*env)->GetLongField(env,
                                       fileAttributes,
                                       FileAttributes_length_fid);

        properties->size = (uint64_t)longVal;

        (*env)->DeleteLocalRef(env, fname);

        /* Success */
        retVal = 0;
    }

finish:

    return retVal;
}

int SecureFs_fileClose(SecureFs_FileHandle *fileHandle)
{
    JNIEnv  *env    = ensureJniAttached();
    int      retVal = -1;
        ;
    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL)
    {
        goto finish;
    }

    /* Call the java method. */
    if ((*env)->CallBooleanMethod(env,
                                  jSecureFSRef,
                                  SOSecureFS_closeFile_mid,
                                  (jobject)fileHandle))
    {
        /* Release our reference to the file handle. */
        (*env)->DeleteGlobalRef(env, (jobject)fileHandle);

        retVal = 0;
    }


finish:

    return retVal;
    return 0;
}

int SecureFs_fileCopy(const char *src, const char *dst)
{
    JNIEnv  *env     = ensureJniAttached();
    jstring  srcFile = NULL;
    jstring  dstFile = NULL;
    int      retVal  = -1;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (src == NULL || dst == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
       srcFile = (*env)->NewStringUTF(env, (const char *)src);
       dstFile = (*env)->NewStringUTF(env, (const char *)dst);
    }

    if (srcFile != NULL && dstFile != NULL)
    {
        /* Call the java method. */
        if ((*env)->CallBooleanMethod(env,
                                      jSecureFSRef,
                                      SOSecureFS_copyFile_mid,
                                      srcFile,
                                      dstFile))
        {
            /* Success */
            retVal = 0;
        }
    }

    if (srcFile != NULL)
    {
       (*env)->DeleteLocalRef(env, srcFile);
    }

    if (dstFile != NULL)
    {
       (*env)->DeleteLocalRef(env, dstFile);
    }

finish:

    return retVal;
}

int64_t SecureFs_fileRead(SecureFs_FileHandle *fileHandle,
                          void                *buffer,
                          uint64_t             count)
{
    JNIEnv     *env       = ensureJniAttached();
    jbyteArray  byteArray = NULL;
    jint        retVal    = -1;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL || buffer == NULL || count == 0)
    {
        goto finish;
    }

    byteArray = (*env)->NewByteArray(env,(jint)count);

    if (byteArray == NULL)
    {
        goto finish;
    }

    /* Call the java method. */
    retVal = (*env)->CallIntMethod(env,
                                   jSecureFSRef,
                                   SOSecureFS_readFromFile_mid,
                                   (jobject)fileHandle,
                                   byteArray);

    if (retVal > 0)
    {
        (*env)->GetByteArrayRegion(env, byteArray, 0, retVal, (jbyte *)buffer);
    }

    (*env)->DeleteLocalRef(env, byteArray);

finish:

    return retVal;
}

int SecureFs_fileDelete(const char *path)
{
    JNIEnv  *env    = ensureJniAttached();
    jstring  fname  = NULL;
    int      retVal = -1;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (path == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
        fname = (*env)->NewStringUTF(env, (const char *)path);
    }

    if (fname)
    {
        /* Call the java method. */
        if ((*env)->CallBooleanMethod(env,
                                      jSecureFSRef,
                                      SOSecureFS_deleteFile_mid,
                                      fname))
        {
            /* Success */
            retVal = 0;
        }

       (*env)->DeleteLocalRef(env, fname);
    }

finish:

    return retVal;
}

int SecureFs_fileExists(const char *path)
{
    JNIEnv  *env    = ensureJniAttached();
    jstring  fname  = NULL;
    int      retVal = 0;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (path == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
        fname = (*env)->NewStringUTF(env, (const char *)path);
    }

    if (fname)
    {
        /* Call the java method. */
        retVal = (*env)->CallBooleanMethod(env,
                                      jSecureFSRef,
                                      SOSecureFS_fileExists_mid,
                                      fname);

       (*env)->DeleteLocalRef(env, fname);
    }

finish:

    return retVal;
}

int SecureFs_fileFlush(SecureFs_FileHandle *fileHandle)
{
    JNIEnv  *env    = ensureJniAttached();
    int      retVal = -1;
        ;
    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL)
    {
        goto finish;
    }

    /* Call the java method. */
    if ((*env)->CallBooleanMethod(env,
                                  jSecureFSRef,
                                  SOSecureFS_syncFile_mid,
                                  (jobject)fileHandle))
    {
        /* Success */
        retVal = 0;
    }

finish:

    return retVal;
}

SecureFs_FileHandle *SecureFs_fileOpen(const char        *path,
                                       SecureFs_FileMode  mode)
{
    JNIEnv  *env                = ensureJniAttached();
    jstring  fname              = NULL;
    jobject  handle             = NULL;
    SecureFs_FileHandle *retVal = NULL;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (path == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
        fname = (*env)->NewStringUTF(env, (const char *)path);
    }

    if (fname)
    {
        /* Create the file if if doesn't already exist. */
        if (! (*env)->CallBooleanMethod(env,
                                        jSecureFSRef,
                                        SOSecureFS_fileExists_mid,
                                        fname))
        {
            if ((mode & SecureFs_FileMode_Create) == 0)
            {
                goto finish;
            }

            if (! (*env)->CallBooleanMethod(env,
                                          jSecureFSRef,
                                          SOSecureFS_createFile_mid,
                                          fname))
            {
                goto finish;
            }
        }

        /* Open the file */
        if ((mode & SecureFs_FileMode_ReadOnly) != 0)
        {
            /* Obtain a handle to the file. */
            handle = (*env)->CallObjectMethod(
                                      env,
                                      jSecureFSRef,
                                      SOSecureFS_getFileHandleForReading_mid,
                                      fname);

            if (handle == NULL)
            {
                goto finish;
            }

            /* Take a global reference to keep the handle alive. */
            retVal = (SecureFs_FileHandle *)(*env)->NewGlobalRef(env, handle);
        }
        else if ((mode & SecureFs_FileMode_WriteOnly) != 0)
        {
            /* Obtain a handle to the file. */
            handle = (*env)->CallObjectMethod(
                                      env,
                                      jSecureFSRef,
                                      SOSecureFS_getFileHandleForWriting_mid,
                                      fname);

            if (handle == NULL)
            {
                goto finish;
            }

            /* Take a global reference to keep the handle alive. */
            retVal = (SecureFs_FileHandle *)(*env)->NewGlobalRef(env, handle);
        }
        else if ((mode & SecureFs_FileMode_ReadWrite) != 0)
        {
            /* Obtain a handle to the file. */
            handle = (*env)->CallObjectMethod(
                                     env,
                                     jSecureFSRef,
                                     SOSecureFS_getFileHandleForUpdating_mid,
                                     fname);

            if (handle == NULL)
            {
                goto finish;
            }

            /* Take a global reference to keep the handle alive. */
            retVal = (SecureFs_FileHandle *)(*env)->NewGlobalRef(env, handle);
        }

        if (handle && (mode & SecureFs_FileMode_Truncate))
        {
            (*env)->CallBooleanMethod(env,
                                      jSecureFSRef,
                                      SOSecureFS_setFileLength_mid,
                                      (jobject)retVal,
                                      (jlong)0);

        }
    }

finish:
    if (fname != NULL)
    {
        (*env)->DeleteLocalRef(env, fname);
    }
    if (handle != NULL)
    {
        (*env)->DeleteLocalRef(env, handle);
    }

    return retVal;
}

int SecureFs_fileRename(const char *src,
                        const char *dst)
{
    JNIEnv  *env     = ensureJniAttached();
    jstring  srcFile = NULL;
    jstring  dstFile = NULL;
    int      retVal  = -1;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (src == NULL || dst == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
       srcFile = (*env)->NewStringUTF(env, (const char *)src);
       dstFile = (*env)->NewStringUTF(env, (const char *)dst);
    }

    if (srcFile != NULL && dstFile != NULL)
    {
        /* Call the java method. */
        if ((*env)->CallBooleanMethod(env,
                                      jSecureFSRef,
                                      SOSecureFS_renameFile_mid,
                                      srcFile,
                                      dstFile))
        {
            /* Success */
            retVal = 0;
        }
    }

    if (srcFile != NULL)
    {
       (*env)->DeleteLocalRef(env, srcFile);
    }

    if (dstFile != NULL)
    {
       (*env)->DeleteLocalRef(env, dstFile);
    }

finish:

    return retVal;
}

int64_t SecureFs_fileSeek(SecureFs_FileHandle *fileHandle,
                          int64_t              offset,
                          SecureFs_SeekOrigin  origin)
{
    JNIEnv  *env    = ensureJniAttached();
    int      retVal = -1;
    long     newOffset;
        ;
    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL)
    {
        goto finish;
    }

    switch (origin)
    {
        case SecureFs_SeekOrigin_Set:
        {
            newOffset = (long)offset;
            break;
        }

        case SecureFs_SeekOrigin_Cur:
        {
            long current = (*env)->CallLongMethod(env,
                                                  jSecureFSRef,
                                                  SOSecureFS_getFileOffset_mid,
                                                  (jobject)fileHandle);

            newOffset = current + offset;
            break;
        }

        case SecureFs_SeekOrigin_End:
        {
            long size = (*env)->CallLongMethod(env,
                                               jSecureFSRef,
                                               SOSecureFS_getFileLength_mid,
                                               (jobject)fileHandle);

            newOffset = size + offset;
            break;
        }

        default:
        {
            goto finish;
        }
    }

    /* Call the java method. */
    if ((*env)->CallBooleanMethod(env,
                                  jSecureFSRef,
                                  SOSecureFS_seekToFileOffset_mid,
                                  (jobject)fileHandle,
                                  (jlong)newOffset))
    {
        /* Success */
        retVal = newOffset;
    }

finish:

    return retVal;
}

uint64_t SecureFs_fileSize(SecureFs_FileHandle *fileHandle)
{
    JNIEnv  *env     = ensureJniAttached();
    long     retVal  = -1L;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL)
    {
        goto finish;
    }

    /* Call the java method. */
    retVal = (*env)->CallLongMethod(env,
                                    jSecureFSRef,
                                    SOSecureFS_getFileLength_mid,
                                    (jobject)fileHandle);

finish:

    return (uint64_t)retVal;
}
int SecureFs_fileTruncate(SecureFs_FileHandle *fileHandle,
                          uint64_t             size)
{
    JNIEnv  *env    = ensureJniAttached();
    int      retVal = -1;
        ;
    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL)
    {
        goto finish;
    }

    /* Call the java method. */
    if ((*env)->CallBooleanMethod(env,
                                  jSecureFSRef,
                                  SOSecureFS_setFileLength_mid,
                                  (jobject)fileHandle,
                                  (jlong)size))
    {
        /* Success */
        retVal = 0;
    }

finish:

    return retVal;
}

int64_t SecureFs_fileWrite(SecureFs_FileHandle *fileHandle,
                           const void          *buffer,
                           uint64_t             count)
{
    JNIEnv     *env       = ensureJniAttached();
    jbyteArray  byteArray = NULL;
    int         retVal    = -1;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (fileHandle == NULL || buffer == NULL || count == 0)
    {
        goto finish;
    }

    byteArray  = (*env)->NewByteArray(env,(int)count);

    if (byteArray == NULL)
    {
        goto finish;
    }

    /* Copy data to the java buffer */
    (*env)->SetByteArrayRegion(env, byteArray, 0, count, (jbyte *)buffer);

    /* Call the java method. */
    retVal = (*env)->CallIntMethod(env,
                                   jSecureFSRef,
                                   SOSecureFS_writeToFile_mid,
                                   (jobject)fileHandle,
                                   byteArray,
                                   (jint)count);

    (*env)->DeleteLocalRef(env, byteArray);

finish:

    return (int64_t)retVal;
}

int SecureFs_isSecurePath(const char *path)
{
    JNIEnv  *env    = ensureJniAttached();
    jstring  fname  = NULL;
    int      retVal = 0;

    /* If no SOSecureFS implementation is available, do nothing. */
    if (jSecureFSRef == NULL || jSecureFSClassRef == NULL ||
        jFileAttributesClassRef == NULL)
    {
        goto finish;
    }

    if (path == NULL)
    {
        goto finish;
    }

    if (env != NULL)
    {
       fname = (*env)->NewStringUTF(env, (const char *)path);
    }

    if (fname)
    {
        /* Call the java method. */
        retVal = (int)(*env)->CallBooleanMethod(env,
                                                jSecureFSRef,
                                                SOSecureFS_isSecurePath_mid,
                                                fname);

       (*env)->DeleteLocalRef(env, fname);
    }

finish:
    return retVal;
}

