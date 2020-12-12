package cn.archko.pdf.common

import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import java.util.*

/**
 * @author: archko 2018/2/24 :14:58
 */
class ProgressScaner {

    fun startScan(fileListEntries: List<FileBean>, currentPath: String): Array<Any?> {
        val entries: MutableList<FileBean> = ArrayList()
        val recent = RecentManager.instance
        for (entry in fileListEntries) {
            val listEntry = entry.clone()
            entries.add(listEntry)
            if (!listEntry.isDirectory && entry.file != null) {
                val progress = recent.readRecentFromDb(entry.file!!.absolutePath, BookProgress.ALL)
                if (null != progress) {
                    listEntry.bookProgress = progress
                }
            }
        }
        return arrayOf(currentPath, entries)
    }
}