package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cn.archko.mupdf.R
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.DateUtils
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.utils.FetcherUtils
import com.artifex.mupdf.fitz.Document
import java.math.BigDecimal

/**
 * @author: archko 2016/1/16 :14:34
 */
class FileInfoFragment : DialogFragment() {

    private val progressDao by lazy { Graph.database.progressDao() }
    private var mEntry: FileBean? = null
    private lateinit var mLocation: TextView
    private lateinit var mFileName: TextView
    private lateinit var mFileSize: TextView

    //lateinit var mLastModified: TextView
    private lateinit var mLastReadLayout: View
    private lateinit var mLastRead: TextView
    private lateinit var mReadCount: TextView
    private lateinit var mPageCount: TextView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mIcon: ImageView
    var bookProgress: BookProgress? = null
    private var mDataListener: DataListener? = null

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = android.R.style.Theme_Material_Light_Dialog
        setStyle(STYLE_NORMAL, themeId)
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG);
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG);
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (null != args) {
            mEntry = args.getSerializable(FILE_LIST_ENTRY) as FileBean
            bookProgress = mEntry!!.bookProgress
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.detail_book_info, container, false)
        mLocation = view.findViewById(R.id.location)
        mFileName = view.findViewById(R.id.fileName)
        mFileSize = view.findViewById(R.id.fileSize)
        mLastReadLayout = view.findViewById(R.id.lay_last_read)
        mLastRead = view.findViewById(R.id.lastRead)
        mReadCount = view.findViewById(R.id.readCount)
        mProgressBar = view.findViewById(R.id.progressbar)
        //mLastModified = view.findViewById<TextView>(R.id.lastModified)
        mPageCount = view.findViewById(R.id.pageCount)
        mIcon = view.findViewById(R.id.icon)
        var button = view.findViewById<Button>(R.id.btn_cancel)
        button.setOnClickListener { this@FileInfoFragment.dismiss() }
        button = view.findViewById(R.id.btn_ok)
        button.setOnClickListener {
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
        mLocation.text = FileUtils.getDir(file)
        mFileName.text = file?.name
        mFileSize.text = Utils.getFileSize(mEntry!!.fileSize)

        if (null == bookProgress || bookProgress?.pageCount == 0) {
            try {
                bookProgress = progressDao.getProgress(file!!.name, BookProgress.ALL)
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
        FetcherUtils.load(path, requireContext(), mIcon)
    }

    private fun updatePageCount() {
        mPageCount.text = String.format("%s/%s", bookProgress!!.page, bookProgress!!.pageCount)
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
            mLastReadLayout.visibility = View.VISIBLE

            var text = DateUtils.formatTime(progress.firstTimestampe, DateUtils.TIME_FORMAT_TWO)
            val percent = progress.page * 100f / progress.pageCount
            val b = BigDecimal(percent.toDouble())
            text += "       " + b.setScale(2, BigDecimal.ROUND_HALF_UP).toFloat() + "%"
            mLastRead.text = text
            mProgressBar.max = progress.pageCount
            mProgressBar.progress = progress.page

            mReadCount.text = progress.readTimes.toString()
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
            bundle.putSerializable(FILE_LIST_ENTRY, entry)
            fileInfoFragment.arguments = bundle
            fileInfoFragment.setListener(dataListener)
            fileInfoFragment.show(ft!!, "diaLog")
        }
    }
}
