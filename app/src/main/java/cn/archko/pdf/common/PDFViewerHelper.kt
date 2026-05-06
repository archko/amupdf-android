package cn.archko.pdf.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Menu
import androidx.core.content.FileProvider
import cn.archko.pdf.activities.AMuPDFRecyclerViewActivity
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.imagedroid.AlbumViewerActivity
import cn.archko.pdf.imagedroid.ImageViewerActivity
import java.io.File
import java.util.Locale

/**
 * @author: archko 2020/1/4 :2:06 下午
 */
class PDFViewerHelper {

    companion object {

        private const val FILE_PROVIDER = "cn.archko.mupdf.fileProvider"

        const val deleteMenuItem = Menu.FIRST + 100
        const val removeMenuItem = Menu.FIRST + 101
        const val removeAndClearMenuItem = Menu.FIRST + 102

        const val otherMenuItem = Menu.FIRST + 103
        const val infoMenuItem = Menu.FIRST + 104
        const val addToFavoriteMenuItem = Menu.FIRST + 105
        const val removeFromFavoriteMenuItem = Menu.FIRST + 106

        const val editMenuItem = Menu.FIRST + 107
        const val albumMenuItem = Menu.FIRST + 108
        const val setHomeMenuItem = Menu.FIRST + 109

        /**
         * 列表项点击直接调用的,所以要判断类型
         */
        fun openAMupdf(clickedFile: File, activity: Context) {
            val fname = clickedFile.name.lowercase(Locale.ROOT)
            if (IntentFile.isImage(fname) || IntentFile.isTiffImage(fname)) {
                openImage(clickedFile, activity)
                return
            }

            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            //val map = mapOf("type" to "Document", "name" to clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

            intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
            activity.startActivity(intent)
        }

        fun openWithOther(clickedFile: File, activity: Context) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            var mimeType = "application/pdf"
            val name = clickedFile.absolutePath
            if (name.endsWith("pdf", true)) {
                mimeType = "application/pdf"
            } else if (name.endsWith("epub", true)) {
                mimeType = "application/epub+zip"
            } else if (name.endsWith("mobi", true)) {
                mimeType = "application/mobi+zip"
            } else if (name.endsWith("cbz", true)) {
                mimeType = "application/x-cbz"
            } else if (name.endsWith("fb2", true)) {
                mimeType = "application/fb2"
            } else if (name.endsWith("txt", true)) {
                mimeType = "text/plain"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setDataAndType(
                    FileProvider.getUriForFile(
                        activity,
                        FILE_PROVIDER,
                        clickedFile
                    ), mimeType
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
            } else {
                intent.setDataAndType(uri, mimeType)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        }

        fun openImage(path: String?, activity: Context) {
            val intent = Intent(Intent.ACTION_VIEW)
            if (IntentFile.isGif(path)) {
                intent.setClass(activity, ImageViewerActivity::class.java)
            } else {
                intent.setClass(activity, AlbumViewerActivity::class.java)
            }

            intent.setData(Uri.parse(path))
            intent.putExtra("path", path)
            activity.startActivity(intent)
        }

        fun openImage(file: File, activity: Context) {
            openImage(file.absolutePath, activity)
        }

        fun openAlbum(file: File, activity: Context) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClass(activity, AlbumViewerActivity::class.java)
            intent.setData(Uri.parse(file.absolutePath))
            intent.putExtra("path", file.absolutePath)
            activity.startActivity(intent)
        }
    }
}
