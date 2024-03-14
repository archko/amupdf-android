package cn.archko.pdf.common

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import java.io.File

object IntentFile {

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
        val path: String? = getFilePathByUri(context, data)
        Log.d("path", "path:$path")
        return path
    }

    @JvmStatic
    fun getFilePathByUri(context: Context, uri: Uri): String? {
        var path: String? = null
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            path = uri.path
            return path
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT == uri.scheme && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATA, MediaStore.Video.Media.DURATION),
                null,
                null,
                null
            )
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    var columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex)
                    }
                    columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                }
                cursor.close()
            }
            return path
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT == uri.scheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        //这种的是拿文件,没有DURATION
                        path = Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        return path
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    path = getDataColumn(context, contentUri, null, null)
                    return path
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    } else if ("document" == type) {
                        contentUri = MediaStore.Files.getContentUri("external")
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    path = getDataColumn(context, contentUri, selection, selectionArgs)
                    return path
                }
            } else {
                path = getRealPathFromURI(context, uri)
            }
        }
        return path
    }

    @JvmStatic
    // 由系统文件管理器选择的是,content://com.android.fileexplorer.myprovider/external_files/DCIM/Camera/VID_20211029_091852.mp4 这种的DURATION是没有的
    // 其它管理器是content://com.speedsoftware.rootexplorer.fileprovider/root/storage/emulated/0/book/
    fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
            val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val path = cursor.getString(column_index)
            return path
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return getPathFromOther(context, contentUri)
    }

    private fun getPathFromOther(context: Context, contentUri: Uri): String? {
        val pathSegments = contentUri.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val sb = StringBuilder()
            for (index in 1 until pathSegments.size) {
                sb.append("/").append(pathSegments[index])
            }
            return sb.toString()
        }
        return null
    }

    @JvmStatic
    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
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
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    deleteDir(files[i].absolutePath)
                } else {
                    success = success and files[i].delete()
                }
            }
            success = success and dir.delete()
            success
        } else {
            success
        }
    }

    fun isText(path: String?): Boolean {
        return path!!.endsWith(".txt", true)
                || path.endsWith(".log", true)
                || path.endsWith(".xml", true)
                || path.endsWith(".html", true)
                || path.endsWith(".xhtml", true)
                || path.endsWith(".js", true)
                || path.endsWith(".json", true)
    }

    fun isImage(path: String?): Boolean {
        return path!!.endsWith(".png", true)
                || path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".webp", true)
                || path.endsWith(".bmp", true)
    }

    fun isPdf(path: String?): Boolean {
        return path!!.endsWith(".pdf", true)
                || path.endsWith(".xps", true)
                || path.endsWith(".cbz", true)
                || path.endsWith(".png", true)
                || path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".jfif", true)
                || path.endsWith(".jfif-tbnl", true)
                || path.endsWith(".tif", true)
                || path.endsWith(".tiff", true)
                || path.endsWith(".epub", true)
                || path.endsWith(".mobi", true)
                || path.endsWith(".ppt", true)
                || path.endsWith(".pptx", true)
                || path.endsWith(".doc", true)
                || path.endsWith(".docx", true)
                || path.endsWith(".xls", true)
                || path.endsWith(".xlsx", true)
    }
}

