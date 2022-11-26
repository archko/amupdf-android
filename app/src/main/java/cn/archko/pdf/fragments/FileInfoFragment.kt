package cn.archko.pdf.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentFileInfoBinding
import cn.archko.pdf.App
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.DateUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Document
import com.thuypham.ptithcm.editvideo.base.BaseDialogFragment
import com.umeng.analytics.MobclickAgent
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * @author: archko 2016/1/16 :14:34
 */
class FileInfoFragment : BaseDialogFragment<FragmentFileInfoBinding>(R.layout.fragment_file_info) {

    var mEntry: FileBean? = null
    var bookProgress: BookProgress? = null
    var mDataListener: DataListener? = null

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Material_Dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeId = android.R.style.Theme_Material_Light_Dialog
        }
        setStyle(DialogFragment.STYLE_NORMAL, themeId)
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(TAG)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (null != args) {
            mEntry = args.getSerializable(FILE_LIST_ENTRY, FileBean::class.java)
            bookProgress = mEntry!!.bookProgress
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.btnCancel.setOnClickListener { this@FileInfoFragment.dismiss() }
        binding.btnCancel.setOnClickListener {
            read()
        }

        dialog?.setTitle(R.string.menu_info)

        return view
    }

    private fun read() {
        this@FileInfoFragment.dismiss()
        mDataListener?.onSuccess(mEntry)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (null == mEntry || mEntry!!.file == null) {
            Toast.makeText(activity, "file is null.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        val file = mEntry!!.file
        binding.location.text = FileUtils.getDir(file)
        binding.fileName.text = file?.name
        binding.fileSize.text = Utils.getFileSize(mEntry!!.fileSize)

        if (null == bookProgress || bookProgress?.pageCount == 0) {
            try {
                bookProgress =
                    Graph.database.progressDao().getProgress(file!!.name, BookProgress.ALL)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (null == bookProgress) {
                bookProgress = BookProgress(FileUtils.getRealPath(file!!.absolutePath))
            }
        }

        showProgress(bookProgress!!)

        showIcon(file!!.path)

        //mLastModified.text = DateUtil.formatTime(file.lastModified(), DateUtil.TIME_FORMAT_TWO)
    }

    private fun showIcon(path: String) {
        ImageLoader.getInstance().loadImage(path, 0, 1.0f, App.instance!!.screenWidth, binding.icon)
    }

    private fun updatePageCount() {
        binding.pageCount.setText(
            String.format(
                "%s/%s",
                bookProgress!!.page,
                bookProgress!!.pageCount
            )
        )
    }

    private fun loadBook() {
        try {
            val core: Document? = Document.openDocument(mEntry!!.file!!.path)
            bookProgress?.pageCount = core!!.countPages()
            updatePageCount()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showProgress(progress: BookProgress) {
        if (null != bookProgress && bookProgress!!.pageCount > 0) {
            binding.layoutLastRead.visibility = View.VISIBLE

            var text = DateUtils.formatTime(progress.firstTimestampe, DateUtils.TIME_FORMAT_TWO)
            val percent = progress.page * 100f / progress.pageCount
            val b = BigDecimal(percent.toDouble())
            text += "       " + b.setScale(2, RoundingMode.HALF_UP).toFloat() + "%"
            binding.lastRead.text = text
            binding.progressbar.max = progress.pageCount
            binding.progressbar.progress = progress.page

            binding.readCount.text = progress.readTimes.toString()
            updatePageCount()
        } else {
            //mLastReadLayout.visibility = View.GONE
            if (bookProgress?.pageCount == 0) {
                loadBook()
            }
        }
    }

    companion object {

        const val TAG = "FileInfoFragment"
        const val FILE_LIST_ENTRY = "FILE_LIST_ENTRY"

        fun showInfoDialog(
            activity: FragmentActivity?,
            entry: FileBean,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("diaLog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val fileInfoFragment = FileInfoFragment()
            val bundle = Bundle()
            bundle.putSerializable(FileInfoFragment.FILE_LIST_ENTRY, entry)
            fileInfoFragment.arguments = bundle
            fileInfoFragment.setListener(dataListener)
            fileInfoFragment.show(ft!!, "diaLog")
        }
    }
}
