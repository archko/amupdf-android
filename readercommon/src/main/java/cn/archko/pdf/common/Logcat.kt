package cn.archko.pdf.common

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object Logcat {
    const val TAG = "Logcat"

    /**
     * 是否允许输出日志
     */
    @JvmField
    var loggable = true
    fun v(tag: String?, msg: String?) {
        if (loggable) {
            Log.v(tag, msg)
        }
    }

    @JvmStatic
    fun d(msg: String?) {
        if (loggable) {
            Log.d(TAG, msg)
        }
    }

    @JvmStatic
    fun d(tag: String?, msg: String?) {
        if (loggable) {
            Log.d(tag, msg)
        }
    }

    @JvmStatic
    fun i(tag: String?, msg: String?) {
        if (loggable) {
            Log.i(tag, msg)
        }
    }

    @JvmStatic
    fun w(tag: String?, msg: String?) {
        if (loggable) {
            Log.w(tag, msg)
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String?) {
        if (loggable) {
            Log.e(tag, msg)
        }
    }

    @JvmStatic
    fun e(tag: String?, throwable: Throwable) {
        if (loggable) {
            Log.e(tag, throwable.message, throwable)
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String?, throwable: Throwable?) {
        if (loggable) {
            Log.e(tag, msg, throwable)
        }
    }

    /**
     * 长日志，以便显示全部的信息
     *
     * @param tag
     * @param tempData 日志内容
     */
    @JvmStatic
    fun longLog(tag: String?, tempData: String) {
        var tempData = tempData
        if (!loggable) {
            return
        }
        tempData = tempData
        val len = tempData.length
        val div = 2000
        val count = len / div
        if (count > 0) {
            for (i in 0 until count) {
                Log.d(tag, tempData.substring(i * div, (i + 1) * div))
            }
            val mode = len % div
            if (mode > 0) {
                Log.d(tag, tempData.substring(div * count, len))
            }
        } else {
            Log.d(tag, tempData)
        }
    }

    /**
     * Writes the current app logcat to a file.
     *
     * @param filename The filename to save it as
     * @throws java.io.IOException
     */
    @JvmStatic
    @Throws(IOException::class)
    fun writeLogcat(filename: String?) {
        val args = arrayOf("logcat", "-v", "time", "-d")
        val process = Runtime.getRuntime().exec(args)
        val input = InputStreamReader(process.inputStream)
        val fileStream: FileOutputStream
        fileStream = try {
            FileOutputStream(filename)
        } catch (e: FileNotFoundException) {
            return
        }
        val output = OutputStreamWriter(fileStream)
        val br = BufferedReader(input)
        val bw = BufferedWriter(output)
        try {
            var line: String?
            while (br.readLine().also { line = it } != null) {
                bw.write(line)
                bw.newLine()
            }
        } catch (e: Exception) {
        } finally {
            bw.close()
            output.close()
            br.close()
            input.close()
        }
    }

    /**
     * Get the last 500 lines of the application logcat.
     *
     * @return the log string.
     * @throws java.io.IOException
     */
    @get:Throws(IOException::class)
    val logcat: String
        get() {
            val args = arrayOf("logcat", "-v", "time", "-d", "-t", "500")
            val process = Runtime.getRuntime().exec(args)
            val input = InputStreamReader(
                process.inputStream
            )
            val br = BufferedReader(input)
            val log = StringBuilder()
            var line: String
            while (br.readLine().also { line = it } != null) log.append(
                """
    $line
    
    """.trimIndent()
            )
            br.close()
            input.close()
            return log.toString()
        }
}