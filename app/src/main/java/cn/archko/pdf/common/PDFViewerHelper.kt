package cn.archko.pdf.common

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.FileProvider
import cn.archko.pdf.activities.AMuPDFRecyclerViewActivity
import cn.archko.pdf.fragments.BrowserFragment
import com.umeng.analytics.MobclickAgent
import org.vudroid.pdfdroid.PdfViewerActivity
import java.io.File

/**
 * @author: archko 2020/1/4 :2:06 下午
 */
class PDFViewerHelper {

    companion object {

        fun openWithDefaultViewer(f: File, activity: Activity) {
            Logcat.i(Logcat.TAG, "post intent to open file $f")
            if (f.absolutePath.endsWith("txt", true)) {
                Toast.makeText(activity, "can't load f:${f.absolutePath}", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            openWithDefaultViewer(Uri.fromFile(f), activity)
        }

        fun openWithDefaultViewer(uri: Uri, activity: Activity) {
            Logcat.i(Logcat.TAG, "post intent to open file $uri")
            val intent = Intent()
            intent.setDataAndType(uri, "application/pdf")
            intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            activity.startActivity(intent)
        }

        fun openViewer(clickedFile: File, item: MenuItem, activity: Activity) {
            val uri = Uri.fromFile(clickedFile)
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = uri

            when (item.itemId) {
                BrowserFragment.vudroidContextMenuItem -> {
                    val map = mapOf("type" to "vudroid", "name" to clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.setClass(activity, PdfViewerActivity::class.java)
                    activity.startActivity(intent)
                }
                BrowserFragment.mupdfContextMenuItem -> {
                    val map = mapOf("type" to "AMuPDF", "name" to clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.setClass(activity, AMuPDFRecyclerViewActivity::class.java)
                    activity.startActivity(intent)
                }
                //bartekscViewContextMenuItem -> {
                //    var map = mapOf("type" to "barteksc", "name" to clickedFile.name)
                //    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
                //    intent.setClass(activity!!, PDFViewActivity::class.java)
                //    startActivity(intent)
                //}
                BrowserFragment.documentContextMenuItem -> {
                    val map = mapOf("type" to "Document", "name" to clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    intent.setClass(activity, cn.archko.pdf.activities.DocumentActivity::class.java)
                    // API>=21: intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); /* launch as a new document */
                    activity.startActivity(intent)
                }
                BrowserFragment.otherContextMenuItem -> {
                    val map = mapOf("type" to "other", "name" to clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)
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
                                "cn.archko.mupdf.fileProvider",
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
    }
}