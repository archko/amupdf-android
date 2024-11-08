package cn.archko.pdf.core.common

import android.content.Context
import android.content.res.Configuration
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
            var lastBreak = false
            for (s in lists) {
                val ss = s.trim { it <= ' ' }
                if (ss.isNotEmpty()) {
                    if (ss.startsWith(IMAGE_START_MARK)) {
                        isImage = true
                        sb.append("&nbsp;<br>")
                    }
                    if (!isImage) {
                        lastBreak = parseLine(ss, sb, MAX_PAGEINDEX, lastBreak)
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
            //Logcat.d("parse:==>" + content);
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
            //Logcat.d("result=>>" + result);
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
            var lastBreak = true
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
                        lastBreak = parseLine(ss, sb, pageIndex, lastBreak)
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

            if (Logcat.loggable) {
                for (rb in reflowBeans) {
                    Logcat.longLog("result", rb.toString())
                }
            }
            return reflowBeans
        }

        /**
         * process line break
         * @param ss source
         * @param sb parsed string
         * @param pageIndex
         * @param lastBreak wethere last line has a break char.
         */
        private fun parseLine(
            ss: String,
            sb: StringBuilder,
            pageIndex: Int,
            lastBreak: Boolean
        ): Boolean {
            var headBreak = false
            var tailBreak = false
            //1.处理结尾字符,如果是前几页,且一行字符<LINE_LENGTH,有可能是目录.添加尾部换行符.
            val end = ss.substring(ss.length - 1)
            //if (lastBreak && (ss.length < LINE_LENGTH && pageIndex < MAX_PAGEINDEX)) {
            //    Logcat.d("step1.break")
            //    //if (!END_MARK.contains(end)) {
            //    tailBreak = true
            //    //}
            //}

            //2.如果尾部没有换行符,则判断尾部的字符.通常是以标点结束的.或者是程序相关的字符结尾.
            if (!tailBreak) {
                if (END_MARK.contains(end) || PROGRAM_MARK.contains(end)) {
                    Logcat.d("step2.break")
                    tailBreak = true
                }
            }

            //3.从前面开始,如果以START_MARK开头,则可能需要在之前添加换行符.
            var lineLength = ss.length
            if (lineLength > 6) {
                lineLength = 6
            }
            val start = ss.substring(0, lineLength)
            var find = START_MARK.matcher(start).find()
            //Logcat.d("find:$find")
            if (!find) {
                if (ss.startsWith("“|\"|'")) {
                    find = true
                }
            }
            if (find) {
                Logcat.d("step3.break,length:${ss.length}")
                headBreak = true
                //如果是//开头的,一般是注释.需要添加尾部换行符.这里有可能会判断错误.但能保证程序注释会换行.
                if (ss.length < LINE_LENGTH || ss.startsWith("//")) {
                    tailBreak = true
                }
            }
            //4.如果上一次是有换行符的,而这一行的字符数较小,有可能是标题目录.所以需要加换行符.
            //这是针对一些,不是以"第xx"开头的标题.此时头尾都有可能要加,如果之前没有加的话.
            if (ss.length < LINE_LENGTH) {
                if (lastBreak) {
                    Logcat.d("step4.break")
                    if (!headBreak) {
                        headBreak = true
                    }
                    if (!tailBreak) {
                        tailBreak = true
                    }
                } else {
                    //如果上一行没有断句,有可能是没有标点的结束,这时,如果是"2."这样开头的,有可能是要前后都要换行.
                    if (START_MARK2.matcher(start).find()) {
                        Logcat.d("step4.1.break")
                        headBreak = true
                        tailBreak = true
                    }
                }
                //if(ss.substring(0, 1).matches("^[0-9]+$".toRegex())) {
                //    sb.append("<br>")
                //}
            }
            if (headBreak && !lastBreak) {
                sb.append("&nbsp;<br>")
            }
            sb.append(ss)
            if (isLetterDigitOrChinese(end)) {
                Logcat.d("isLetterDigitOrChinese:$end")
                sb.append("&nbsp;")
            }
            if (tailBreak) {
                sb.append("&nbsp;<br>")
            }
            Logcat.d("headBreak:${headBreak},tailBreak:$tailBreak,source:$ss")
            return tailBreak
        }

        fun parseAsList(content: String, pageIndex: Int): List<ReflowBean> {
            //Logcat.d("parse:==>" + content);
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
            //Logcat.d("result=>>" + result);
            return parseList(list, pageIndex)
        }

        fun parseAsTextList(content: String, pageIndex: Int): List<ReflowBean> {
            //Logcat.d("parse:==>" + content);
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
            //Logcat.d("result=>>${list.size}");
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
        Pattern.compile("(第\\w*[^章]章)|总结|小结|●|■|//|var|val|let|fun|public|private|static|abstract|protected|import|export|pack|overri|open|class|void|for|while")
    internal val START_MARK2 = Pattern.compile("\\d+\\.")

    /**
     * 段落的结束字符可能是以下.
     */
    internal const val END_MARK = ".!?．！？。！?:：」？” ——"

    /**
     * 如果遇到的是代码,通常是以这些结尾
     */
    internal const val PROGRAM_MARK = "\\]>){};,'\""

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
        //Logcat.longLog("text", base64Source);
        if (!base64Source.contains(IMAGE_HEADER)) {
            return null
        }
        val index = base64Source.indexOf(IMAGE_HEADER)
        base64Source = base64Source.substring(index + IMAGE_HEADER.length)
        //Logcat.d("base:" + base64Source);
        val bitmap = BitmapUtils.base64ToBitmap(
            base64Source.replace(
                "\"/></p>".toRegex(),
                ""
            ) /*.replaceAll("\\s", "")*/
        )
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
