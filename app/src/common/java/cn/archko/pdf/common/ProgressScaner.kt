package cn.archko.pdf.common

import cn.archko.pdf.core.common.ProgressDao
import cn.archko.pdf.core.entity.FileBean

/**
 * @author: archko 2018/2/24 :14:58
 */
class ProgressScaner {

    //fun startScan(fileListEntries: List<FileBean>, currentPath: String): Array<Any?> {
    //    val entries: MutableList<FileBean> = ArrayList()
    //    val recent = RecentManager.instance
    //    for (entry in fileListEntries) {
    //        val listEntry = entry.clone()
    //        entries.add(listEntry)
    //        if (!listEntry.isDirectory && entry.file != null) {
    //            val progress = recent.readRecentFromDb(entry.file!!.absolutePath, BookProgress.ALL)
    //            if (null != progress) {
    //                listEntry.bookProgress = progress
    //            }
    //        }
    //    }
    //    return arrayOf(currentPath, entries)
    //}

    fun startScan(fileListEntries: List<FileBean>?, progressDao: ProgressDao) {
        if (null != fileListEntries) {
            for (entry in fileListEntries) {
                if (!entry.isDirectory && entry.file != null) {
                    val progress = progressDao.getProgress(entry.file!!.name)
                    if (null != progress) {
                        val size = entry.bookProgress?.size
                        entry.bookProgress = progress
                        size?.let {
                            entry.bookProgress!!.size = size
                        }
                    }
                }
            }
        }
    }
}