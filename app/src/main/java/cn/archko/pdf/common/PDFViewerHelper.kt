package cn.archko.pdf.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.FileProvider
import cn.archko.pdf.activities.AMuPDFRecyclerViewActivity
import cn.archko.pdf.activities.ComposeTextActivity
import org.vudroid.pdfdroid.PdfViewerActivity
import java.io.File

/**
 * @author: archko 2020/1/4 :2:06 下午
 */
class PDFViewerHelper {

    companion object {

        //private const val FILE_PROVIDER = "cn.archko.mupdf.fileProvider"
        private const val FILE_PROVIDER = "com.radaee.pdfmaster.fileProvider"

        const val deleteContextMenuItem = Menu.FIRST + 100
        const val removeContextMenuItem = Menu.FIRST + 101

        const val mupdfContextMenuItem = Menu.FIRST + 110

        const val vudroidContextMenuItem = Menu.FIRST + 112
        const val otherContextMenuItem = Menu.FIRST + 113
        const val infoContextMenuItem = Menu.FIRST + 114
        const val documentContextMenuItem = Menu.FIRST + 115
        const val addToFavoriteContextMenuItem = Menu.FIRST + 116
        const val removeFromFavoriteContextMenuItem = Menu.FIRST + 117

        fun openWithDefaultViewer(f: File, activity: Context) {
            //val map = HashMap<String, String>()
            //map["type"] = "vudroid"
            //map["name"] = f.name
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
            //Logcat.i(Logcat.TAG, "post intent to open file $f")
            //if (f.absolutePath.endsWith("txt", true)) {
            //    Toast.makeText(activity, "can't load f:${f.absolutePath}", Toast.LENGTH_SHORT).show()
            //    return
            //}
            openWithDefaultViewer(Uri.fromFile(f), activity)
        }

        fun openWithDefaultViewer(uri: Uri, activity: Context) {
            Logcat.i(Logcat.TAG, "post intent to open file $uri")
            val intent = Intent()
            intent.setDataAndType(uri, "application/pdf")
            intent.setClass(activity, ComposeTextActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            activity.startActivity(intent)
        }

        fun openViewer(clickedFile: File, item: MenuItem, activity: Activity) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            when (item.itemId) {
                vudroidContextMenuItem -> {
                    //val map = mapOf("type" to "vudroid", "name" to clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.setClass(activity, PdfViewerActivity::class.java)
                    activity.startActivity(intent)
                }
                mupdfContextMenuItem -> {
                    //val map = mapOf("type" to "AMuPDF", "name" to clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
                    activity.startActivity(intent)
                }
                //bartekscViewContextMenuItem -> {
                //    var map = mapOf("type" to "barteksc", "name" to clickedFile.name)
                //    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
                //    intent.setClass(activity!!, PDFViewActivity::class.java)
                //    startActivity(intent)
                //}
                documentContextMenuItem -> {
                    //val map = mapOf("type" to "Document", "name" to clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.setClass(activity, cn.archko.pdf.activities.DocumentActivity::class.java)
                    // API>=21: intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); /* launch as a new document */
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
                        //val mimeType = activity.contentResolver?.getType(uri)
                        intent.setDataAndType(uri, mimeType)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                }
            }
        }

        fun openViewerMupdf(clickedFile: File, activity: Context) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            //val map = mapOf("type" to "Document", "name" to clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

            intent.setClass(activity, cn.archko.pdf.activities.DocumentActivity::class.java)
            // API>=21: intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); /* launch as a new document */
            activity.startActivity(intent)
        }

        fun openComposeViewerMupdf(clickedFile: File, activity: Context) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            //val map = mapOf("type" to "Document", "name" to clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

            intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
            // API>=21: intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); /* launch as a new document */
            activity.startActivity(intent)
        }

        fun openViewerOther(clickedFile: File, activity: Context) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            //val map = mapOf("type" to "other", "name" to clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
            var mimeType = "application/pdf"
            val name = clickedFile.absolutePath;
            if (name.endsWith("pdf", true)) {
                mimeType = "application/pdf";
            } else if (name.endsWith("epub", true)) {
                mimeType = "application/epub+zip";
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
                //val mimeType = activity.contentResolver?.getType(uri)
                intent.setDataAndType(uri, mimeType)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        }
    }
}