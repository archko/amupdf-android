package org.vudroid.core.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.util.List;

public class PathFromUri {

    public static String retrieve(final Context context, final Uri uri) {
        //if ("file".equals(uri.getScheme())) {
        //    return uri.getPath();
        //}
        //final Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
        //if ((cursor != null) && cursor.moveToFirst()) {
        //    return cursor.getString(0);
        //}
        //throw new RuntimeException("Can't retrieve path from uri: " + uri);
        return getFilePathByUri(context, uri);
    }

    public static String getFilePathByUri(Context context, Uri uri) {
        Log.d("PDFSDK", "getFilePathByUri:" + uri);
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{MediaStore.Images.Media.DATA, MediaStore.Video.Media.DURATION},
                    null,
                    null,
                    null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                    columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    String docId = DocumentsContract.getDocumentId(uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        //这种的是拿文件,没有DURATION
                        path = Environment.getExternalStorageDirectory().toString() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    String id = DocumentsContract.getDocumentId(uri);
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            Long.parseLong(id)
                    );
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    String docId = DocumentsContract.getDocumentId(uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    } else if ("document".equals(type)) {
                        contentUri = MediaStore.Files.getContentUri("external");
                    }
                    String selection = "_id=?";
                    String[] selectionArgs = {split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            } else {
                path = getRealPathFromURI(context, uri);
            }
        }
        return path;
    }

    // 由系统文件管理器选择的是,content://com.android.fileexplorer.myprovider/external_files/DCIM/Camera/VID_20211029_091852.mp4 这种的DURATION是没有的
    // 其它管理器是content://com.speedsoftware.rootexplorer.fileprovider/root/storage/emulated/0/book/
    // 小米,amaze:content://com.amaze.filemanager.ak/storage_root/storage/emulated/0/book/
    static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = new String[]{MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return getPathFromOther(context, contentUri);
    }

    private static String getPathFromOther(Context context, Uri contentUri) {
        List<String> pathSegments = contentUri.getPathSegments();
        if (pathSegments != null && pathSegments.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int index = 1; index < pathSegments.size(); index++) {
                sb.append("/").append(pathSegments.get(index));
            }
            if ("external_files".equals(pathSegments.get(0))) {
                sb.insert(0, "/sdcard");
            }
            return sb.toString();
        }
        return null;
    }

    private static String getDataColumn(
            Context context,
            Uri uri,
            String selection,
            String[] selectionArgs
    ) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equalsIgnoreCase(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equalsIgnoreCase(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equalsIgnoreCase(uri.getAuthority());
    }
}
