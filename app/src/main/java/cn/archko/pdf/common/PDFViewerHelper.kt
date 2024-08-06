package cn.archko.pdf.common

//import cn.archko.pdf.activities.ComposeTextActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.FileProvider
import cn.archko.pdf.activities.AMuPDFRecyclerViewActivity
import cn.archko.pdf.imagedroid.AlbumViewerActivity
import org.vudroid.pdfdroid.PdfViewerActivity
import java.io.File

/**
 * @author: archko 2020/1/4 :2:06 下午
 */
class PDFViewerHelper {

    companion object {

        private const val FILE_PROVIDER = "cn.archko.mupdf.fileProvider"

        const val deleteContextMenuItem = Menu.FIRST + 100
        const val removeContextMenuItem = Menu.FIRST + 101

        const val mupdfContextMenuItem = Menu.FIRST + 110

        //protected const val apvContextMenuItem = Menu.FIRST + 111
        const val vudroidContextMenuItem = Menu.FIRST + 112
        const val otherContextMenuItem = Menu.FIRST + 113
        const val infoContextMenuItem = Menu.FIRST + 114
        const val documentContextMenuItem = Menu.FIRST + 115
        const val addToFavoriteContextMenuItem = Menu.FIRST + 116
        const val removeFromFavoriteContextMenuItem = Menu.FIRST + 117

        //protected const val bartekscViewContextMenuItem = Menu.FIRST + 118
        const val editContextMenuItem = Menu.FIRST + 119
        const val albumContextMenuItem = Menu.FIRST + 120

        fun openViewer(clickedFile: File, item: MenuItem, activity: Activity) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            when (item.itemId) {
                vudroidContextMenuItem -> {
                    openVudroid(clickedFile, activity)
                }

                mupdfContextMenuItem -> {
                    //val map = mapOf("type" to "AMuPDF", "name" to clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.putExtra("forceCropParam", 0)
                    intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
                    activity.startActivity(intent)
                }

                otherContextMenuItem -> {
                    //val map = mapOf("type" to "other", "name" to clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
                    var mimeType = "application/pdf"
                    val name = clickedFile.absolutePath;
                    if (name.endsWith("pdf", true)) {
                        mimeType = "application/pdf";
                    } else if (name.endsWith("epub", true)) {
                        mimeType = "application/epub+zip";
                    } else if (name.endsWith("mobi", true)) {
                        mimeType = "application/mobi+zip";
                    } else if (name.endsWith("cbz", true)) {
                        mimeType = "application/x-cbz";
                    } else if (name.endsWith("fb2", true)) {
                        mimeType = "application/fb2";
                    } else if (name.endsWith("txt", true)) {
                        mimeType = "text/plain";
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //val mimeType = this@BrowserFragment.activity?.contentResolver?.getType(FileProvider.getUriForFile(getContext()!!, "cn.archko.mupdf.fileProvider", clickedFile))
                        intent.setDataAndType(
                            FileProvider.getUriForFile(
                                activity,
                                FILE_PROVIDER,
                                clickedFile
                            ), mimeType
                        );
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                    } else {
                        intent.setDataAndType(uri, mimeType)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                }
            }
        }

        fun openVudroid(clickedFile: File, activity: Context) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            //val map = mapOf("type" to "Document", "name" to clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

            intent.setClass(activity, PdfViewerActivity::class.java)
            activity.startActivity(intent)
        }

        fun openAMupdf(clickedFile: File, activity: Context) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            //val map = mapOf("type" to "Document", "name" to clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

            intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
            activity.startActivity(intent)
        }

        fun openAlbum(file: File, activity: Context) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClass(activity, AlbumViewerActivity::class.java)
            intent.setData(Uri.parse(file.absolutePath))
            intent.putExtra("path", file.absoluteFile)
            activity.startActivity(intent)
        }
    }
}