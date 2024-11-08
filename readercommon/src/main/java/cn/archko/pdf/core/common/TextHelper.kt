package cn.archko.pdf.core.common

import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.utils.StreamUtils
import org.mozilla.universalchardet.UniversalDetector
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

object TextHelper {
    private const val READ_LINE = 10
    private const val READ_CHAR_COUNT = 400
    private const val TEMP_LINE = "\n"
    const val HEADER_HEIGHT = 60f

    @JvmStatic
    fun readString(path: String): List<ReflowBean> {
        var bufferedReader: BufferedReader? = null
        val reflowBeans = mutableListOf<ReflowBean>()
        var countPerLine = 0
        var line = 0
        val sb = StringBuilder()
        try {
            val stream = FileInputStream(File(path))
            val encoding = UniversalDetector.detectCharset(stream)
            val isr = InputStreamReader(FileInputStream(path), encoding)
            bufferedReader = BufferedReader(isr)
            var temp: String?
            while (bufferedReader.readLine().also { temp = it } != null) {
                temp = temp?.trimIndent()
                if (null != temp && temp!!.length > READ_CHAR_COUNT + 40) {
                    //如果一行大于READ_CHAR_COUNT个字符,就应该把这一行按READ_CHAR_COUNT一个字符换行.
                    line = addLargeLine(temp!!, reflowBeans, line)
                } else {
                    if (countPerLine < READ_LINE) {
                        sb.append(temp)
                        countPerLine++
                    } else {
                        Logcat.d("======================:$sb")
                        reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING, "$line"))
                        sb.setLength(0)
                        countPerLine = 0
                        line++
                    }
                }
            }
            if (sb.isNotEmpty()) {
                reflowBeans.add(ReflowBean(sb.toString(), ReflowBean.TYPE_STRING, "$line"))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            StreamUtils.closeStream(bufferedReader)
        }

        return reflowBeans
    }

    private fun addLargeLine(temp: String, reflowBeans: MutableList<ReflowBean>, line: Int): Int {
        val length = temp.length
        var start = 0
        var count = line
        while (start < length) {
            var end = start + READ_CHAR_COUNT
            if (end > length) {
                end = length
            }
            val sequence = temp.subSequence(start, end)
            reflowBeans.add(ReflowBean(sequence.toString(), ReflowBean.TYPE_STRING, "$count"))
            start = end
            count++
        }
        return count
    }
}