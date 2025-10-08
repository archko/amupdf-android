package cn.archko.pdf.core.common

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Nullable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.*

object IntentFile {
    const val PRIMARY_VOLUME_NAME: String = "primary"

    @JvmStatic
    fun processIntentAction(intent: Intent, context: Context): String? {
        var path: String? = null
        if (Intent.ACTION_VIEW == intent.action) {
            path = getPath(context, intent.data)
            if (path == null && intent.data != null) {
                path = intent.data.toString()
            }
        } else {
            if (!TextUtils.isEmpty(intent.getStringExtra("path"))) {
                path = intent.getStringExtra("path")
            }
        }
        return path
    }

    @JvmStatic
    fun getPath(context: Context, data: Uri?): String? {
        if (null == data) {
            return null
        }
        Log.d("uri", data.toString())
        val path: String? = getFilePathByUri(context, data)
        Log.d("path", "path:$path")
        return path
    }

    @JvmStatic
    fun getFilePathByUri(context: Context, uri: Uri): String? {
        var path: String? = getRealPathFromURI(context, uri)
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            path = getRealPathFromURI(context, uri)
            if (path == null) {
                path = getFullPathFromTreeUri(uri, context)
            }
        }
        return path
    }

    fun getRealPathFromURI(context: Context?, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), id.toLong()
                )

                return getDataColumn(context!!, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf<String?>(
                    split[1]
                )

                return getDataColumn(context!!, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) return uri.lastPathSegment

            // Check if it's a FileProvider URI
            if (isFileProviderUri(uri)) {
                return getFileProviderPath(uri)
            }

            return getDataColumn(context!!, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return getPathFromOther(uri)
    }

    @Suppress("deprecation")
    fun getFullPathFromTreeUri(treeUri: Uri?, context: Context): String? {
        if (treeUri == null) {
            return null
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (isDownloadsDocument(treeUri)) {
                val docId = DocumentsContract.getDocumentId(treeUri)
                val extPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .path
                if (docId == "downloads") {
                    return extPath
                } else if (docId.matches("^ms[df]\\:.*".toRegex())) {
                    val fileName: String? = getFileName(treeUri, context)
                    return "$extPath/$fileName"
                } else if (docId.startsWith("raw:")) {
                    val rawPath: String? =
                        docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    return rawPath
                }
                return null
            }
        }

        var volumePath: String? = getVolumePath(getVolumeIdFromTreeUri(treeUri), context)

        if (volumePath == null) {
            return File.separator
        }

        if (volumePath.endsWith(File.separator)) volumePath =
            volumePath.substring(0, volumePath.length - 1)

        var documentPath: String? = getDocumentPathFromTreeUri(treeUri)

        if (null != documentPath && documentPath.endsWith(File.separator)) documentPath =
            documentPath.substring(0, documentPath.length - 1)

        if (null != documentPath && documentPath.isNotEmpty()) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath
            } else {
                return volumePath + File.separator + documentPath
            }
        } else {
            return volumePath
        }
    }

    fun getFileName(uri: Uri, context: Context?): String? {
        var result: String? = null

        try {
            if (uri.scheme == "content") {
                val cursor = context?.contentResolver
                    ?.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result =
                            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                } finally {
                    cursor!!.close()
                }
            }
            if (result == null) {
                result = uri.getPath()
                val cut = result!!.lastIndexOf('/')
                if (cut != -1) {
                    result = result.substring(cut + 1)
                }
            }
        } catch (ex: Exception) {
            Log.e("TAG", "Failed to handle file name: $ex")
        }

        return result
    }

    @Nullable
    private fun getDirectoryPath(
        storageVolumeClazz: Class<*>,
        storageVolumeElement: Any?
    ): String? {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val getPath: Method = storageVolumeClazz.getMethod("getPath")
                return getPath.invoke(storageVolumeElement) as String?
            }

            val getDirectory: Method = storageVolumeClazz.getMethod("getDirectory")
            val f = getDirectory.invoke(storageVolumeElement) as File?
            if (f != null) return f.getPath()
        } catch (_: java.lang.Exception) {
            return null
        }
        return null
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun getVolumePath(volumeId: String?, context: Context): String? {
        try {
            val mStorageManager =
                context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList: Method = mStorageManager.javaClass.getMethod("getVolumeList")
            val getUuid: Method = storageVolumeClazz.getMethod("getUuid")
            val isPrimary: Method = storageVolumeClazz.getMethod("isPrimary")
            val result: Any? = getVolumeList.invoke(mStorageManager)
            if (result == null) return null

            val length: Int = java.lang.reflect.Array.getLength(result)
            for (i in 0..<length) {
                val storageVolumeElement: Any? = java.lang.reflect.Array.get(result, i)
                val uuid = getUuid.invoke(storageVolumeElement) as String?
                val primary = isPrimary.invoke(storageVolumeElement) as Boolean?

                // primary volume?
                if (primary != null && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return getDirectoryPath(storageVolumeClazz, storageVolumeElement)
                }

                // other volumes?
                if (uuid != null && uuid == volumeId) {
                    return getDirectoryPath(storageVolumeClazz, storageVolumeElement)
                }
            }
            // not found.
            return null
        } catch (_: java.lang.Exception) {
            return null
        }
    }

    private fun getVolumeIdFromTreeUri(treeUri: Uri?): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split: Array<String?> =
            docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.isNotEmpty()) return split[0]
        else return null
    }

    private fun getDocumentPathFromTreeUri(treeUri: Uri?): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split: Array<String?> =
            docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if ((split.size >= 2) && (split[1] != null)) return split[1]
        else return File.separator
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.getAuthority()
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri is a FileProvider URI.
     */
    public fun isFileProviderUri(uri: Uri): Boolean {
        return uri.authority?.contains("fileProvider") == true
    }

    /**
     * Extract file path from FileProvider URI by parsing the path segments.
     * @param uri The FileProvider Uri
     * @return The file path or null if cannot be extracted
     */
    public fun getFileProviderPath(uri: Uri): String? {
        try {
            val pathSegments = uri.pathSegments
            if (pathSegments != null && pathSegments.isNotEmpty()) {
                // For FileProvider URIs like content://authority/external_files/path/to/file
                // We need to reconstruct the actual file path
                val sb = StringBuilder()

                // Skip the first segment if it's "external_files" or similar provider path
                val startIndex = if (pathSegments.size > 1 &&
                    (pathSegments[0] == "external_files" || pathSegments[0] == "files")
                ) 1 else 0

                for (i in startIndex until pathSegments.size) {
                    if (sb.isNotEmpty()) sb.append("/")
                    sb.append(pathSegments[i])
                }

                // If it starts with external_files, prepend external storage path
                if (pathSegments.size > 0 && pathSegments[0] == "external_files") {
                    return Environment.getExternalStorageDirectory().path + "/" + sb.toString()
                }

                return sb.toString()
            }
        } catch (e: Exception) {
            Log.e("IntentFile", "Error extracting FileProvider path: ${e.message}")
        }
        return null
    }

    private fun getPathFromOther(contentUri: Uri): String? {
        val pathSegments = contentUri.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val sb = StringBuilder()
            for (index in 1 until pathSegments.size) {
                sb.append("/").append(pathSegments[index])
            }
            if (contentUri.toString().contains("external_files")) {
                sb.insert(0, Environment.getExternalStorageDirectory().path)
            }
            return sb.toString()
        }
        return null
    }

    fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String?>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf<String?>(
            column
        )

        try {
            cursor = uri?.let {
                context.contentResolver.query(
                    it, projection, selection, selectionArgs,
                    null
                )
            }
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(column)
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            Log.e("IntentFile", "Error querying data column: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    @JvmStatic
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    @JvmStatic
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    @JvmStatic
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * 递归删除文件夹
     *
     * @param dirPath
     * @return
     */
    @JvmStatic
    fun deleteDir(dirPath: String?): Boolean {
        var success = false
        if (dirPath.isNullOrEmpty()) {
            return success
        }
        val dir = File(dirPath)
        return if (dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    if (files[i].isDirectory) {
                        deleteDir(files[i].absolutePath)
                    } else {
                        success = success and files[i].delete()
                    }
                }
            }
            success = success and dir.delete()
            success
        } else {
            success
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createTempFile(uri: Uri, context: Context): File {
        val ext = uri.lastPathSegment
        val extension = getExtensionName(ext)
        val tempfile = File.createTempFile("temp", "content.$extension")
        tempfile.deleteOnExit()
        val source = context.contentResolver.openInputStream(uri)
        copy(source, tempfile)
        return tempfile
    }

    fun getExtensionName(filename: String?): String? {
        if (filename != null && filename.length > 0) {
            val dot = filename.lastIndexOf('.')
            if (dot > -1 && dot < filename.length - 1) {
                return filename.substring(dot + 1)
            }
        }
        return filename
    }

    @Throws(IOException::class)
    fun copy(inputStream: InputStream?, output: File?) {
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(output)
            var read = 0
            val bytes = ByteArray(1024)
            while (inputStream!!.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        } finally {
            try {
                inputStream?.close()
            } finally {
                outputStream?.close()
            }
        }
    }

    fun isText(path: String): Boolean {
        return path.endsWith(".txt", true)
                || path.endsWith(".log", true)
                || path.endsWith(".xml", true)
                || path.endsWith(".html", true)
                || path.endsWith(".xhtml", true)
                || path.endsWith(".js", true)
                || path.endsWith(".json", true)
    }

    fun isImage(path: String): Boolean {
        return path.endsWith(".png", true)
                || path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".webp", true)
                || path.endsWith(".heic", true)
                || path.endsWith(".bmp", true)
                || path.endsWith(".gif", true)
    }

    fun isGif(path: String?): Boolean {
        return !TextUtils.isEmpty(path) &&
                (path!!.endsWith(".webp", true)
                        || path.endsWith(".gif", true))
    }

    fun isTiffImage(path: String?): Boolean {
        return !TextUtils.isEmpty(path) &&
                (path!!.endsWith(".jfif", true)
                        || path.endsWith(".jfif-tbnl", true)
                        || path.endsWith(".tif", true)
                        || path.endsWith(".tiff", true))
    }

    fun isPdf(path: String): Boolean {
        return path.endsWith(".pdf", true)
    }

    fun isMuPdf(path: String): Boolean {
        return path.endsWith(".pdf", true)
                || path.endsWith(".xps", true)
                || path.endsWith(".cbz", true)
                || path.endsWith(".epub", true)
                || path.endsWith(".mobi", true)
                || path.endsWith(".pptx", true)
                || path.endsWith(".docx", true)
                || path.endsWith(".xlsx", true)
    }

    fun isReflowable(path: String): Boolean {
        return path.endsWith(".cbz", true)
                || path.endsWith(".epub", true)
                || path.endsWith(".mobi", true)
                || path.endsWith(".pptx", true)
                || path.endsWith(".docx", true)
                || path.endsWith(".xlsx", true)
    }

    fun isEpub(path: String): Boolean {
        return path.endsWith(".epub", true)
    }

    fun isMobi(path: String): Boolean {
        return path.endsWith(".mobi", true)
                || path.endsWith(".azw", true)
                || path.endsWith(".azw2", true)
                || path.endsWith(".azw3", true)
                || path.endsWith(".azw4", true)
    }

    fun isAzw(path: String): Boolean {
        return path.endsWith(".azw", true)
                || path.endsWith(".azw2", true)
                || path.endsWith(".azw3", true)
                || path.endsWith(".azw4", true)
    }

    fun isDocx(path: String): Boolean {
        return path.endsWith(".docx", true)
    }

    fun isDjvu(name: String): Boolean {
        val fname = name.lowercase(Locale.getDefault())
        return fname.endsWith("djvu") || fname.endsWith("djv")
    }

    fun isSupportedImageForCreater(path: String): Boolean {
        return path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".gif", true)
    }
}

