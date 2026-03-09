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
import java.io.File
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

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
    
    // 阅读统计视图
    private lateinit var mDividerStats: View
    private lateinit var mLabelStatsTitle: TextView
    private lateinit var mTotalReadingTime: TextView
    private lateinit var mSessionCount: TextView
    private lateinit var mAverageSessionTime: TextView
    private lateinit var mCompletedPages: TextView
    private lateinit var mConsecutiveDays: TextView
    private lateinit var mAnnotationCount: TextView
    private lateinit var mBookmarkCount: TextView
    private lateinit var mFirstReadAt: TextView
    private lateinit var mLastReadAt: TextView

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
        
        // 初始化阅读统计视图
        mDividerStats = view.findViewById(R.id.divider_stats)
        mLabelStatsTitle = view.findViewById(R.id.label_stats_title)
        mTotalReadingTime = view.findViewById(R.id.totalReadingTime)
        mSessionCount = view.findViewById(R.id.sessionCount)
        mAverageSessionTime = view.findViewById(R.id.averageSessionTime)
        mCompletedPages = view.findViewById(R.id.completedPages)
        mConsecutiveDays = view.findViewById(R.id.consecutiveDays)
        mAnnotationCount = view.findViewById(R.id.annotationCount)
        mBookmarkCount = view.findViewById(R.id.bookmarkCount)
        mFirstReadAt = view.findViewById(R.id.firstReadAt)
        mLastReadAt = view.findViewById(R.id.lastReadAt)
        
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
            
            // 加载阅读统计数据
            loadReadingStats()
        } else {
            //mLastReadLayout.visibility = View.GONE
            if (bookProgress?.pageCount == 0) {
                loadBook()
            }
        }
    }
    
    private fun loadReadingStats() {
        mEntry?.file?.path?.let { path ->
            lifecycleScope.launch {
                try {
                    val fileName = getFileName(path)
                    val stats = Graph.database?.readingStatsDao()?.getStatsByPath(fileName)
                    
                    withContext(Dispatchers.Main) {
                        if (stats != null) {
                            showReadingStats(stats)
                        } else {
                            hideReadingStats()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        hideReadingStats()
                    }
                }
            }
        } ?: run {
            hideReadingStats()
        }
    }
    
    private fun getFileName(path: String): String {
        return File(path).name
    }
    
    private fun showReadingStats(stats: cn.archko.pdf.core.entity.ReadingStats) {
        mTotalReadingTime.text = formatDuration(stats.totalReadingTime)
        mSessionCount.text = stats.sessionCount.toString()
        mAverageSessionTime.text = formatDuration(stats.averageSessionTime)
        mCompletedPages.text = "${stats.completedPages}/${stats.totalPages}"
        mConsecutiveDays.text = "${stats.consecutiveDays}天"
        mAnnotationCount.text = stats.annotationCount.toString()
        mBookmarkCount.text = stats.bookmarkCount.toString()
        
        mFirstReadAt.text = if (stats.firstReadAt > 0) {
            DateUtils.formatTime(stats.firstReadAt, DateUtils.TIME_FORMAT_TWO)
        } else {
            ""
        }
        
        mLastReadAt.text = if (stats.lastReadAt > 0) {
            DateUtils.formatTime(stats.lastReadAt, DateUtils.TIME_FORMAT_TWO)
        } else {
            ""
        }
        
        showReadingStatsViews()
    }
    
    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            "${hours}小时${minutes}分钟"
        } else {
            "${minutes}分钟"
        }
    }
    
    private fun showReadingStatsViews() {
        mDividerStats.visibility = View.VISIBLE
        mLabelStatsTitle.visibility = View.VISIBLE
        mTotalReadingTime.visibility = View.VISIBLE
        mSessionCount.visibility = View.VISIBLE
        mAverageSessionTime.visibility = View.VISIBLE
        mCompletedPages.visibility = View.VISIBLE
        mConsecutiveDays.visibility = View.VISIBLE
        mAnnotationCount.visibility = View.VISIBLE
        mBookmarkCount.visibility = View.VISIBLE
        mFirstReadAt.visibility = View.VISIBLE
        mLastReadAt.visibility = View.VISIBLE
    }
    
    private fun hideReadingStats() {
        mDividerStats.visibility = View.GONE
        mLabelStatsTitle.visibility = View.GONE
        mTotalReadingTime.visibility = View.GONE
        mSessionCount.visibility = View.GONE
        mAverageSessionTime.visibility = View.GONE
        mCompletedPages.visibility = View.GONE
        mConsecutiveDays.visibility = View.GONE
        mAnnotationCount.visibility = View.GONE
        mBookmarkCount.visibility = View.GONE
        mFirstReadAt.visibility = View.GONE
        mLastReadAt.visibility = View.GONE
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
