package cn.archko.pdf.common

import android.os.Build
import android.os.Environment
import android.text.format.DateFormat
import cn.archko.pdf.common.Logcat.e
import cn.archko.pdf.utils.FileUtils
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

class CrashHandler : Thread.UncaughtExceptionHandler {
    private val defaultUEH: Thread.UncaughtExceptionHandler
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val result: Writer = StringWriter()
        val printWriter = PrintWriter(result)

        // Inject some info about android version and the device, since google can't provide them in the developer console
        val trace = ex.stackTrace
        val trace2 = arrayOfNulls<StackTraceElement>(trace.size + 3)
        System.arraycopy(trace, 0, trace2, 0, trace.size)
        trace2[trace.size + 0] = StackTraceElement("Android", "MODEL", Build.MODEL, -1)
        trace2[trace.size + 1] = StackTraceElement("Android", "VERSION", Build.VERSION.RELEASE, -1)
        trace2[trace.size + 2] = StackTraceElement("Android", "FINGERPRINT", Build.FINGERPRINT, -1)
        ex.stackTrace = trace2
        ex.printStackTrace(printWriter)
        val stacktrace = result.toString()
        printWriter.close()
        e(TAG, stacktrace)

        // Save the log on SD card if available
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val dir = FileUtils.getStorageDir("amupdf")
            var sdcardPath = Environment.getExternalStorageDirectory().path
            if (dir != null && dir.exists()) {
                sdcardPath = dir.absolutePath
            }
            writeLog(stacktrace, "$sdcardPath/m_crash")
            writeLogcat("$sdcardPath/m_logcat")
        }
        defaultUEH.uncaughtException(thread, ex)
    }

    private fun writeLog(log: String, name: String) {
        val timestamp = DateFormat.format("yyyyMMdd_kkmmss", System.currentTimeMillis())
        val filename = name + "_" + timestamp + ".log"
        val stream: FileOutputStream
        stream = try {
            FileOutputStream(filename)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return
        }
        val output = OutputStreamWriter(stream)
        val bw = BufferedWriter(output)
        try {
            bw.write(log)
            bw.newLine()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                bw.close()
                output.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun writeLogcat(name: String) {
        val timestamp = DateFormat.format("yyyyMMdd_kkmmss", System.currentTimeMillis())
        val filename = name + "_" + timestamp + ".log"
        try {
            Logcat.writeLogcat(filename)
        } catch (e: IOException) {
            e(TAG, "Cannot write logcat to disk")
        }
    }

    companion object {
        private const val TAG = "CrashHandler"
    }

    init {
        defaultUEH = Thread.getDefaultUncaughtExceptionHandler()
    }
}