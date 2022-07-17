package cn.archko.pdf.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
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
) : BaseRecyclerAdapter<ReflowBean>(mContext) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReflowBean> {
        val pdfView = AKTextView(mContext, styleHelper)
        val holder = AKTextViewHolder(pdfView)
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
            var position = "$pos"
            if (pos == 0) {
                position = ""
            }
            if (null != reflowBeans) {
                if (pos == reflowBeans!!.size - 1) {
                    position = ""
                }
            }
            (holder as AKTextViewHolder).bindAsList(
                result,
                screenHeight,
                screenWidth,
                systemScale,
                reflowCache,
                showBookmark(pos),
                position
            )
        }
    }

    private fun showBookmark(position: Int): Boolean {
        return false
    }

    override fun onViewRecycled(holder: BaseViewHolder<*>) {
        super.onViewRecycled(holder)
        val pdfHolder = holder as AKTextViewHolder?

        Logcat.d("onViewRecycled end,exist count::${reflowCache.textViewCount()},${reflowCache.imageViewCount()}")
        pdfHolder?.recycleViews(reflowCache)
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

    fun setScope(scope: CoroutineScope?) {
        this.scope = scope
    }

    internal class AKTextViewHolder(var pageView: AKTextView) :
        BaseViewHolder<ReflowBean>(pageView) {

        fun recycleViews(reflowViewCache: ReflowViewCache?) {
            if (null != reflowViewCache) {
                for (i in 0 until pageView.getChildCount()) {
                    val child: View = pageView.getChildAt(i)
                    if (child is TextView) {
                        reflowViewCache.addTextView(child)
                    } else if (child is ImageView) {
                        reflowViewCache.addImageView(child)
                    }
                }
                pageView.removeAllViews()
            }
        }

        fun bindAsList(
            text: ReflowBean, screenHeight: Int, screenWidth: Int,
            systemScale: Float, reflowViewCache: ReflowViewCache?,
            showBookmark: Boolean, pos: String
        ) {
            recycleViews(reflowViewCache)
            pageView.update(text, reflowViewCache, showBookmark, pos)
        }
    }

    class AKTextView(context: Context?, private val styleHelper: StyleHelper?) :
        RelativeLayout(context) {

        private val leftPadding = Utils.dipToPixel(20f)
        private val rightPadding = Utils.dipToPixel(20f)
        private val topPadding = Utils.dipToPixel(8f)
        private val bottomPadding = Utils.dipToPixel(12f)

        init {
            /*setPadding(
                styleHelper?.styleBean!!.leftPadding, styleHelper.styleBean!!.topPadding,
                styleHelper.styleBean!!.rightPadding, styleHelper.styleBean!!.bottomPadding
            )*/
        }

        fun update(
            text: ReflowBean,
            reflowViewCache: ReflowViewCache?,
            showBookmark: Boolean,
            pos: String
        ) {
            applyStyle()
            addTextView(text.data, reflowViewCache, showBookmark)
            addPageNumberView(reflowViewCache, pos)
        }

        fun applyStyle() {
            setBackgroundColor(styleHelper?.styleBean!!.bgColor)
        }

        fun addTextView(text: String?, cacheViews: ReflowViewCache?, showBookmark: Boolean) {
            if (TextUtils.isEmpty(text)) {
                return
            }
            var textView: TextView? = null
            if (null != cacheViews && cacheViews.textViewCount() > 0) {
                textView = cacheViews.getTextView(0)
            } else {
                textView = TextView(context)
                textView.setTextIsSelectable(false)
                if (minImgHeight == 32f) {
                    minImgHeight = textView.paint.measureText("我") + 5
                }
            }
            textView.id = 100
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.addRule(Gravity.CENTER_HORIZONTAL)
            addView(textView, lp)
            applyStyleForText(textView)
            textView.text = Html.fromHtml(text)
        }

        /**
         * html:行间距要调小,会导致图文重排图片上移,段间距偏大,行间距也偏大,
         * xhtml:默认不修改,文本行间距偏小,段间距偏大.
         * text:所有文本不换行,显示不对.剩下与xhtml相同.如果不使用Html.from设置,有换行,但显示不了图片.
         * textView.setLineSpacing(0, 0.8f);
         *
         * @param context
         * @param textView
         */
        fun applyStyleForText(textView: TextView) {
            if (null != styleHelper && null != styleHelper.styleBean) {
                textView.textSize = styleHelper!!.styleBean!!.textSize
                textView.setTextColor(styleHelper!!.styleBean!!.fgColor)
                textView.setLineSpacing(0f, styleHelper!!.styleBean!!.lineSpacingMult)
                val typeface: Typeface? = styleHelper!!.fontHelper?.typeface
                textView.setTypeface(typeface)
            }

            textView.setPadding(leftPadding, topPadding, rightPadding, bottomPadding)
        }

        fun addPageNumberView(cacheViews: ReflowViewCache?, pos: String) {
            var textView: TextView?
            if (null != cacheViews && cacheViews.textViewCount() > 0) {
                textView = cacheViews.getTextView(0)
            } else {
                textView = TextView(context)
                textView.setTextIsSelectable(false)
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.addRule(ALIGN_PARENT_LEFT)
            lp.addRule(ALIGN_PARENT_BOTTOM)
            lp.addRule(BELOW, 100)
            lp.topMargin = topPadding / 4
            addView(textView, lp)
            textView.textSize = 13f
            textView.text = "$pos"
            textView.setPadding(leftPadding, 0, rightPadding, topPadding / 4)
        }

        companion object {
            var minImgHeight = 32f
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
