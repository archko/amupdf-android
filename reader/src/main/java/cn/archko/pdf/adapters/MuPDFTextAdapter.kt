package cn.archko.pdf.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.App
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.ReflowViewCache
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.utils.StreamUtils
import cn.archko.pdf.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader

/**
 * @author: archko 2016/5/13 :11:03
 */
class MuPDFTextAdapter(
    private val mContext: Context,
    private val path: String,
    private var styleHelper: StyleHelper?,
    private var scope: CoroutineScope?,
) : BaseRecyclerAdapter<Any>(mContext) {

    private var screenHeight = 720
    private var screenWidth = 1080
    private val systemScale = Utils.getScale()
    private val reflowCache = ReflowViewCache()
    private var reflowBeans: List<ReflowBean>? = null

    init {
        screenHeight = App.instance!!.screenHeight
        screenWidth = App.instance!!.screenWidth
        scope!!.launch {
            reflowBeans = withContext(Dispatchers.Main) {
                readString(path)
            }
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return reflowBeans?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val pdfView = TextView(mContext)
        val holder = ReflowTextViewHolder(pdfView, styleHelper)
        val lp: RecyclerView.LayoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pdfView.layoutParams = lp

        return holder
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Any>, pos: Int) {
        val result = reflowBeans?.get(pos)
        result?.run {
            (holder as ReflowTextViewHolder).bindAsList(
                result,
                screenHeight,
                screenWidth,
                systemScale,
                reflowCache,
                showBookmark(pos)
            )
        }
    }

    private fun showBookmark(position: Int): Boolean {
        return false
    }

    override fun onViewRecycled(holder: BaseViewHolder<*>) {
        super.onViewRecycled(holder)
        val pdfHolder = holder as ReflowTextViewHolder?

        Logcat.d("onViewRecycled end,exist count::${reflowCache.textViewCount()},${reflowCache.imageViewCount()}")
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

    fun setScope(scope: CoroutineScope?) {
        this.scope = scope
    }

    internal class ReflowTextViewHolder(var pageView: TextView, var styleHelper: StyleHelper?) :
        BaseViewHolder<Any?>(pageView) {

        fun bindAsList(
            text: ReflowBean, screenHeight: Int, screenWidth: Int,
            systemScale: Float, reflowViewCache: ReflowViewCache?, showBookmark: Boolean
        ) {
            applyStyleForText(pageView)
            pageView.setText(text.data)
        }

        private val leftPadding = Utils.dipToPixel(20f)
        private val topPadding = Utils.dipToPixel(8f)

        private fun applyStyleForText(textView: TextView) {
            if (null != styleHelper && null != styleHelper!!.styleBean) {
                textView.textSize = styleHelper!!.styleBean!!.textSize
                textView.setTextColor(styleHelper!!.styleBean!!.fgColor)
                textView.setLineSpacing(0f, styleHelper!!.styleBean!!.lineSpacingMult)
                val typeface: Typeface? = styleHelper!!.fontHelper?.typeface
                textView.setTypeface(typeface)
            }
            textView.setPadding(leftPadding, topPadding, leftPadding, topPadding)
        }
    }

    companion object {

        private const val READ_LINE = 10
        private const val READ_CHAR_COUNT = 400
        private const val TEMP_LINE = "\n"

        private fun readString(path: String): List<ReflowBean> {
            var bufferedReader: BufferedReader? = null
            val reflowBeans = mutableListOf<ReflowBean>()
            reflowBeans.add(ReflowBean(TEMP_LINE, ReflowBean.TYPE_STRING))
            var lineCount = 0
            val sb = StringBuilder()
            try {
                bufferedReader = BufferedReader(FileReader(path))
                var temp: String?
                while (bufferedReader.readLine().also { temp = it } != null) {
                    temp = temp?.trimIndent()
                    if (null != temp && temp!!.length > READ_CHAR_COUNT + 40) {
                        //如果一行大于READ_CHAR_COUNT个字符,就应该把这一行按READ_CHAR_COUNT一个字符换行.
                        addLargeLine(temp!!, reflowBeans)
                    } else {
                        if (lineCount < READ_LINE) {
                            sb.append(temp)
                            lineCount++
                        } else {
                            Logcat.d("======================:$sb")
                            reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING))
                            sb.setLength(0)
                            lineCount = 0
                        }
                    }
                }
                if (sb.isNotEmpty()) {
                    reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING))
                }
                reflowBeans.add(ReflowBean(TEMP_LINE, ReflowBean.TYPE_STRING))
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                StreamUtils.closeStream(bufferedReader)
            }

            return reflowBeans
        }

        private fun addLargeLine(temp: String, reflowBeans: MutableList<ReflowBean>) {
            val length = temp.length
            var start = 0;
            while (start < length) {
                var end = start + READ_CHAR_COUNT
                if (end > length) {
                    end = length
                }
                val line = temp.subSequence(start, end)
                reflowBeans.add(ReflowBean(line.toString(), ReflowBean.TYPE_STRING))
                start = end
            }
        }
    }

}
