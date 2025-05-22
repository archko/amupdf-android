package cn.archko.pdf.core.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.text.Html
import android.text.TextUtils
import cn.archko.pdf.core.entity.BitmapBean
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.utils.BitmapUtils
import java.util.regex.Pattern

/**
 * @author: archko 2019/2/18 :15:57
 */
object ParseTextMain {

    private val txtParser: TxtParser = TxtParser()

    fun parseAsText(bytes: ByteArray): String {
        val content = String(bytes)
        return txtParser.parseAsText(content)
    }

    fun parseAsList(bytes: ByteArray, pageIndex: Int): List<ReflowBean> {
        val content = String(bytes)
        return txtParser.parseAsList(content, pageIndex)
    }

    /**
     * 解析为spanned,可以直接使用
     */
    fun parseAsHtmlList(bytes: ByteArray, pageIndex: Int): List<ReflowBean> {
        val content = String(bytes)
        val list = txtParser.parseAsList(content, pageIndex)
        for (reflowBean in list) {
            if (ReflowBean.TYPE_STRING == reflowBean.type) {
                reflowBean.data = Html.fromHtml(reflowBean.data).toString()
            }
        }
        return list
    }

    //解析为普通文件,原数据就是普通文本+图片,没有html标签
    fun parseAsTextList(bytes: ByteArray, pageIndex: Int): List<ReflowBean> {
        val content = String(bytes)
        val list = txtParser.parseAsTextList(content, pageIndex)
        return list
    }

    class TxtParser {
        lateinit var path: String
        internal var joinLine = true
        internal var deleteEmptyLine = true

        constructor() {}

        constructor(path: String) {
            this.path = path
        }

        internal fun parse(lists: List<String>): String {
            val sb = StringBuilder()
            var isImage = false
            var maxNumberCharOfLine = 20
            for (s in lists) {
                if (s.length > maxNumberCharOfLine) {
                    maxNumberCharOfLine = s.length
                }
            }

            var lastLine: Line? = null
            for (s in lists) {
                val ss = s.trim { it <= ' ' }
                if (ss.isNotEmpty()) {
                    if (ss.startsWith(IMAGE_START_MARK)) {
                        isImage = true
                        sb.append(LINE_END)
                    }
                    if (!isImage) {
                        lastLine = parseLine(ss, sb, MAX_PAGEINDEX, lastLine, maxNumberCharOfLine)
                    } else {
                        sb.append(ss)
                    }

                    if (ss.endsWith("</p>")) {
                        isImage = false
                    }
                }
            }
            return sb.toString()
        }

        fun parseAsText(content: String): String {
            //Logcat.d("parse:==>" + content)
            val sb = StringBuilder()
            val list = ArrayList<String>()
            var aChar: Char
            for (element in content) {
                aChar = element
                if (aChar == '\n') {
                    list.add(sb.toString())
                    sb.setLength(0)
                } else {
                    sb.append(aChar)
                }
            }
            //Logcat.d("result=>>" + result)
            return parse(list)
        }

        /**
         * parse text as List<ReflowBean>
         */
        private fun parseList(lists: List<String>, pageIndex: Int): List<ReflowBean> {
            val sb = StringBuilder()
            var isImage = false
            val reflowBeans = ArrayList<ReflowBean>()
            var reflowBean: ReflowBean? = null
            var maxNumberCharOfLine = 20
            var hasImage = false
            for (s in lists) {  //图片第一行会有很多字符
                if (s.startsWith(IMAGE_START_MARK)) {
                    hasImage = true
                }
                if (s.length > maxNumberCharOfLine && !hasImage) {
                    maxNumberCharOfLine = s.length
                }
            }

            var lastLine: Line? = null
            for (s in lists) {
                val ss = s.trim()
                if (!TextUtils.isEmpty(ss)) {
                    //if (Logcat.loggable) {
                    //    Logcat.longLog("text", ss)
                    //}
                    if (ss.startsWith(IMAGE_START_MARK)) {
                        isImage = true
                        sb.setLength(0)
                        reflowBean = ReflowBean(null, ReflowBean.TYPE_STRING, pageIndex.toString())
                        reflowBean.type = ReflowBean.TYPE_IMAGE
                        reflowBeans.add(reflowBean)
                    }
                    if (!isImage) {
                        if (null == reflowBean) {
                            reflowBean =
                                ReflowBean(null, ReflowBean.TYPE_STRING, pageIndex.toString())
                            reflowBeans.add(reflowBean)
                        }
                        lastLine = parseLine(ss, sb, pageIndex, lastLine, maxNumberCharOfLine - 5)
                        reflowBean.data = sb.toString()
                    } else {
                        sb.append(ss)
                    }

                    if (ss.endsWith(IMAGE_END_MARK)) {
                        isImage = false
                        reflowBean?.data = sb.toString()
                        reflowBean = null
                        sb.setLength(0)
                    }
                }
            }

            //if (Logcat.loggable) {
            //    for (rb in reflowBeans) {
            //        Logcat.longLog("result", rb.toString())
            //    }
            //}
            return reflowBeans
        }

        /**
         * 重排的数据是按行获取的,只有纯文本,要把行合并起来.合并需要区分是否这一行就是结束.
         * 如果这行是开始标志
         *     则判断上一行是否有结束.没有则添加结束标志.
         *     追加本行
         * 如果这行有结束标志
         *     上行没有结束符
         *         行字数小于标准字数
         *             加结束符
         *     追加本行内容,加结束符
         * 如果这行没有结束标志
         *     上行有结束符
         *         追加本行内容
         *     上行没有结束符
         *         上行小于标准字数
         *             本行字数小于标准字数
         *                 上行添加结束符
         *                 追加本行内容,加结束符
         *             本行字数大于标准字数
         *                 上行添加结束符
         *                 追加本行内容
         *         上行大于标准字数
         *             本行字数小于标准字数
         *                 追加本行内容,加结束符
         *             本行字数大于标准字数
         *                 追加本行内容
         * @param ss source
         * @param sb parsed string
         * @param pageIndex
         * @param lastBreak wethere last line has a break char.
         */
        private fun parseLine(
            ss: String,
            sb: StringBuilder,
            pageIndex: Int,
            lastLine: Line?,
            maxNumberCharOfLine: Int
        ): Line {
            val line = StringBuilder()
            val thisLine = Line(ss.length < maxNumberCharOfLine)
            //1.处理结尾字符
            val end = ss.substring(ss.length - 1)

            //2.判断尾部的字符是否是结束符.通常是以标点结束的.或者是程序相关的字符结尾.
            val isEnd = if (END_MARK.contains(end) || PROGRAM_MARK.contains(end)) {
                Logcat.d("step2.line.end.break:$ss")
                true
            } else {
                false
            }

            //3.判断是否是新一行开始
            var lineLength = ss.length
            if (lineLength > 6) {
                lineLength = 6
            }
            val start = ss.substring(0, lineLength)
            var isStartLine = START_MARK.matcher(start).find()
            //Logcat.d("find:$find")
            if (!isStartLine) {
                if (ss.startsWith("“|\"|'")) {
                    isStartLine = true
                }
            }
            if (!isStartLine) {
                if (START_MARK2.matcher(start).find()) {
                    isStartLine = true
                }
            }
            if (isStartLine) {
                Logcat.d("step3.line break,length:${ss.length}")
                //如果是开始行,上行如果没有结束符,则添加上.
                lastLine?.run {
                    if (!this.isEnd) {
                        line.append(LINE_END)
                    }
                }
                line.append(ss)
                if (isEnd) {
                    line.append(LINE_END)
                }
                thisLine.isEnd = isEnd
                thisLine.text = line.toString()
                sb.append(line)
                if (Logcat.loggable) {
                    Logcat.d("count:${maxNumberCharOfLine} :$line")
                }
                return thisLine
            }

            //4.如果这行有结束标志
            if (isEnd) {
                lastLine?.run {
                    //上行没有结束符,行字数小于标准字数,加结束符
                    if (!this.isEnd && lastLine.isNotALine) {
                        line.append(LINE_END)
                    }
                }
                line.append(ss)
                line.append(LINE_END)
                thisLine.isEnd = true
                thisLine.text = line.toString()
                sb.append(line)
                if (Logcat.loggable) {
                    Logcat.d("count1:${maxNumberCharOfLine} :$line")
                }
                return thisLine
            } else {
                //5.如果这行没有结束标志
                val lastLineIsEnd = (lastLine == null || lastLine.isEnd)
                //上行有结束符
                if (lastLineIsEnd) {
                    line.append(ss)
                    thisLine.isEnd = false
                } else { //上行没有结束符
                    if (lastLine.isNotALine) { //上行小于标准字数
                        if (ss.length < maxNumberCharOfLine) {//本行字数小于标准字数
                            line.append(LINE_END)
                            line.append(ss)
                            line.append(LINE_END)
                            thisLine.isEnd = true
                        } else {  //本行字数大于标准字数
                            line.append(LINE_END)
                            line.append(ss)
                        }
                    } else {    //上行大于标准字数
                        if (ss.length < maxNumberCharOfLine) {//本行字数小于标准字数
                            //追加本行内容,加结束符
                            line.append(ss)
                            line.append(LINE_END)
                            thisLine.isEnd = true
                        } else {  //本行字数大于标准字数
                            line.append(ss)
                        }
                    }
                }
            }
            if (isLetterDigitOrChinese(end)) {
                Logcat.d("isLetterDigitOrChinese:$end")
                line.append(LINE_END)
            }
            thisLine.text = line.toString()
            sb.append(line)
            if (Logcat.loggable) {
                Logcat.d("count2:${maxNumberCharOfLine} :$line")
            }
            return thisLine
        }

        fun parseAsList(content: String, pageIndex: Int): List<ReflowBean> {
            //Logcat.d("parse:==>" + content)
            val sb = StringBuilder()
            val list = ArrayList<String>()
            var aChar: Char
            val rs = content.replace(SINGLE_WORD_FIX_REGEX, "")
            for (i in 0 until rs.length) {
                aChar = rs[i]
                if (aChar == '\n') {
                    list.add(sb.toString())
                    sb.setLength(0)
                } else {
                    sb.append(aChar)
                }
            }
            //Logcat.d("result=>>" + result)
            return parseList(list, pageIndex)
        }

        fun parseAsTextList(content: String, pageIndex: Int): List<ReflowBean> {
            //Logcat.d("parse:==>" + content)
            val sb = StringBuilder()
            val list = ArrayList<String>()
            var aChar: Char
            for (element in content) {
                aChar = element
                if (aChar == '\n') {
                    list.add(sb.toString())
                    sb.setLength(0)
                } else {
                    sb.append(aChar)
                }
            }
            //Logcat.d("result=>>${list.size}")
            return parseTextList(list, pageIndex)
        }

        private fun parseTextList(lists: List<String>, pageIndex: Int): List<ReflowBean> {
            val sb = StringBuilder()
            val reflowBeans = ArrayList<ReflowBean>()
            val reflowBean = ReflowBean(null, ReflowBean.TYPE_STRING, pageIndex.toString())
            reflowBeans.add(reflowBean)
            for (s in lists) {
                val ss = s.trim()
                if (!TextUtils.isEmpty(ss)) {
                    sb.append(ss)
                    reflowBean.data = sb.toString()
                }
            }

            if (Logcat.loggable) {
                for (rb in reflowBeans) {
                    Logcat.longLog("result", rb.toString())
                }
            }
            return reflowBeans
        }

        fun isLetterDigitOrChinese(str: String): Boolean {
            val regex = "^[a-z0-9A-Z]+$"//其他需要，直接修改正则表达式就好
            return str.matches(regex.toRegex())
        }
    }

    private class Line(var isNotALine: Boolean) {
        var text: String? = ""
        var isEnd: Boolean = false

        override fun toString(): String {
            return "Line(isNotALine=$isNotALine, isEnd=$isEnd, text=$text)"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
    }

    //对于一些文档,会有一个换行符,导致每一个字符都换行,目前测试中发现,有两个换行符的才是换行.
    val SINGLE_WORD_FIX_REGEX = Regex("( \\n)")

    /**
     * 段落的开始字符可能是以下的:
     * 第1章,第四章.
     * 总结,小结,●,■,（2）,（3）
     * //|var|val|let|这是程序的注释.需要换行,或者是程序的开头.
     */
    internal val START_MARK =
        Pattern.compile("(第\\w*[^章]章)|总结|小结|○|●|■|—|//|var|val|let|fun|public|private|static|abstract|protected|import|export|pack|overri|open|class|void|for|while")
    internal val START_MARK2 = Pattern.compile("\\d+\\.")

    /**
     * 段落的结束字符可能是以下.
     */
    internal const val END_MARK = ".!?．！？。！?:：」？” ——"

    /**
     * 如果遇到的是代码,通常是以这些结尾
     */
    internal const val PROGRAM_MARK = ";,]>){}"

    /**
     * 解析pdf得到的文本,取出其中的图片
     */
    internal const val IMAGE_START_MARK = "<p><img"

    /**
     * 图片结束,jni中的特定结束符.
     */
    internal const val IMAGE_END_MARK = "</p>"

    /**
     * 一行如果不到20个字符,有可能是目录或是标题.
     */
    internal const val LINE_LENGTH = 20

    /**
     * 最大的页面是30页,如果是30页前的,一行小于25字,认为可能是目录.在这之后的,文本重排时不认为是目录.合并为一行.
     */
    internal const val MAX_PAGEINDEX = 20
    private const val LINE_END = "&nbsp;<br>"

    //========================== decode image ==========================
    private const val IMAGE_HEADER = "base64,"

    var minImgHeight = 32f
        set
        get

    fun decodeBitmap(
        base64Source: String,
        systemScale: Float,
        screenHeight: Int,
        screenWidth: Int,
        context: Context
    ): BitmapBean? {
        var base64Source = base64Source
        if (TextUtils.isEmpty(base64Source)) {
            return null
        }
        //Logcat.longLog("text", base64Source)
        if (!base64Source.contains(IMAGE_HEADER)) {
            return null
        }
        val index = base64Source.indexOf(IMAGE_HEADER)
        base64Source = base64Source.substring(index + IMAGE_HEADER.length)
        //Logcat.d("base:$base64Source")
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapUtils.base64ToBitmap(
                base64Source.replace(
                    "\"/></p>".toRegex(),
                    ""
                ) /*.replaceAll("\\s", "")*/
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (null == bitmap
            || (bitmap.width < minImgHeight
                    && bitmap.height < minImgHeight)
        ) {
            Logcat.i("text", "bitmap decode failed.")
            return null
        }
        var width = bitmap.width * systemScale
        var height = bitmap.height * systemScale
        if (Logcat.loggable) {
            Logcat.d(
                String.format(
                    "width:%s, height:%s systemScale:%s",
                    bitmap.width,
                    bitmap.height,
                    systemScale
                )
            )
        }
        var sw = screenHeight
        if (isScreenPortrait(context)) {
            sw = screenWidth
        }
        if (width > sw) {
            val ratio = sw / width
            height = ratio * height
            width = sw.toFloat()
        }
        return BitmapBean(bitmap, width, height)
    }

    fun isScreenPortrait(context: Context): Boolean {
        val mConfiguration = context.resources.configuration //获取设置的配置信息
        val ori = mConfiguration.orientation //获取屏幕方向
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏
            return false
        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏
        }
        return true
    }
}
