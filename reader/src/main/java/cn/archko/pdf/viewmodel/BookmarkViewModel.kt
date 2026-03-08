package cn.archko.pdf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.core.entity.ReadingStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 书签管理ViewModel
 * @author: archko 2026/3/1
 */
public class BookmarkViewModel : ViewModel() {
    private val _bookmarkList = MutableStateFlow<List<ABookmark>>(emptyList())
    public val bookmarkList: StateFlow<List<ABookmark>> = _bookmarkList

    private val _currentPathBookmarks = MutableStateFlow<List<ABookmark>>(emptyList())
    public val currentPathBookmarks: StateFlow<List<ABookmark>> = _currentPathBookmarks

    /**
     * 从文件全路径中获取文件名（包含扩展名）
     * 例如: "/path/to/file.pdf" -> "file.pdf"
     */
    private fun String.getFileName(): String {
        if (this.isEmpty()) return ""

        val separatorIndex = this.lastIndexOf('/').coerceAtLeast(this.lastIndexOf('\\'))
        return if (separatorIndex >= 0 && separatorIndex < this.length - 1) {
            this.substring(separatorIndex + 1)
        } else {
            this
        }
    }

    /**
     * 加载指定文档的书签
     */
    public fun loadBookmarks(path: String) {
        viewModelScope.launch {
            val name = path.getFileName()
            val bookmarks = Graph.database?.bookmarkDao()?.getBookmarksByPath(name) ?: emptyList()
            _currentPathBookmarks.value = bookmarks
            println("BookmarkViewModel.loadBookmarks: name=$name, count=${bookmarks.size}")
        }
    }

    /**
     * 添加书签
     */
    public fun addBookmark(
        path: String,
        pageIndex: Int,
        title: String? = null,
        note: String? = null,
        color: Long? = null,
        scrollY: Long? = null
    ) {
        viewModelScope.launch {
            val name = path.getFileName()
            val bookmark = ABookmark(
                path = name,
                pageIndex = pageIndex,
                title = title,
                note = note,
                color = color,
                scrollY = scrollY
            )
            Graph.database?.bookmarkDao()?.insertBookmark(bookmark)
            println("BookmarkViewModel.addBookmark: $bookmark")
            
            // 重新加载当前文档的书签
            loadBookmarks(name)
        }
    }

    /**
     * 更新书签
     */
    public fun updateBookmark(bookmark: ABookmark) {
        viewModelScope.launch {
            bookmark.updateAt = System.currentTimeMillis()
            Graph.database?.bookmarkDao()?.updateBookmark(bookmark)
            println("BookmarkViewModel.updateBookmark: $bookmark")
            
            // 重新加载当前文档的书签
            loadBookmarks(bookmark.path)
        }
    }

    /**
     * 删除书签
     */
    public fun deleteBookmark(bookmark: ABookmark) {
        viewModelScope.launch {
            Graph.database?.bookmarkDao()?.deleteBookmark(bookmark)
            println("BookmarkViewModel.deleteBookmark: $bookmark")
            
            // 重新加载当前文档的书签
            loadBookmarks(bookmark.path)
        }
    }

    /**
     * 检查指定页面是否有书签
     */
    public suspend fun hasBookmarkAtPage(path: String, pageIndex: Int): Boolean {
        val name = path.getFileName()
        return Graph.database?.bookmarkDao()?.getBookmarkByPageAndPath(name, pageIndex) != null
    }

    /**
     * 获取指定页面的书签
     */
    public suspend fun getBookmarkAtPage(path: String, pageIndex: Int): ABookmark? {
        val name = path.getFileName()
        return Graph.database?.bookmarkDao()?.getBookmarkByPageAndPath(name, pageIndex)
    }

    /**
     * 加载所有书签
     */
    public fun loadAllBookmarks() {
        viewModelScope.launch {
            val bookmarks = Graph.database?.bookmarkDao()?.getAllBookmarks() ?: emptyList()
            _bookmarkList.value = bookmarks
            println("BookmarkViewModel.loadAllBookmarks: count=${bookmarks.size}")
        }
    }

    // ============================
    private val _currentStats = MutableStateFlow<ReadingStats?>(null)
    public val currentStats: StateFlow<ReadingStats?> = _currentStats

    /**
     * 加载指定文档的统计数据
     */
    public fun loadStats(path: String) {
        viewModelScope.launch {
            val name = path.getFileName()
            val stats = Graph.database?.readingStatsDao()?.getStatsByPath(name)
            _currentStats.value = stats
            println("BookmarkViewModel.loadStats: name=$name, stats=$stats")
        }
    }

    /**
     * 实时更新统计数据（用于在文档打开期间查看统计）
     */
    public fun updateCurrentStats(
        path: String,
        sessionDuration: Long,
        currentPage: Int,
        annotationCount: Int,
        bookmarkCount: Int
    ) {
        viewModelScope.launch {
            val name = path.getFileName()
            val stats = Graph.database?.readingStatsDao()?.getStatsByPath(name)
            if (stats != null) {
                // 临时更新当前显示的统计数据（不保存到数据库）
                val updatedStats = stats.copy().apply {
                    val tempTotalTime = stats.totalReadingTime + sessionDuration
                    val tempSessionCount = stats.sessionCount + 1

                    totalReadingTime = tempTotalTime
                    lastSessionTime = sessionDuration
                    averageSessionTime =
                        if (tempSessionCount > 0) tempTotalTime / tempSessionCount else 0

                    if (currentPage > completedPages) {
                        completedPages = currentPage
                    }

                    this.annotationCount = annotationCount
                    this.bookmarkCount = bookmarkCount
                }
                _currentStats.value = updatedStats
                println("BookmarkViewModel.updateCurrentStats: 临时更新统计 $updatedStats")
            }
        }
    }

    /**
     * 开始新的阅读会话
     */
    public suspend fun startSession(path: String, totalPages: Int) {
        val name = path.getFileName()
        val stats = Graph.database?.readingStatsDao()?.getStatsByPath(name)
        if (stats == null) {
            // 首次阅读，创建新记录
            val newStats = ReadingStats(name, totalPages)
            Graph.database?.readingStatsDao()?.insertStats(newStats)
            _currentStats.value = newStats
            println("BookmarkViewModel.startSession: 创建新统计记录 $newStats")
        } else {
            _currentStats.value = stats
            println("BookmarkViewModel.startSession: 加载已有统计记录 $stats")
        }
    }

    /**
     * 结束阅读会话，更新统计数据
     */
    public fun endSession(
        path: String,
        sessionDuration: Long,  // 本次阅读时长（秒）
        currentPage: Int,
        annotationCount: Int,
        bookmarkCount: Int
    ) {
        viewModelScope.launch {
            val name = path.getFileName()
            val stats = Graph.database?.readingStatsDao()?.getStatsByPath(name) ?: return@launch

            val currentDate = getCurrentDate()
            val isNewDay = stats.lastSessionDate != currentDate

            stats.apply {
                // 更新时长统计
                totalReadingTime += sessionDuration
                lastSessionTime = sessionDuration
                sessionCount += 1
                averageSessionTime = if (sessionCount > 0) totalReadingTime / sessionCount else 0

                // 更新时间
                lastReadAt = System.currentTimeMillis()
                lastSessionDate = currentDate

                // 更新连续阅读天数
                if (isNewDay) {
                    val daysDiff = calculateDaysDifference(stats.lastSessionDate, currentDate)
                    consecutiveDays = if (daysDiff == 1) consecutiveDays + 1 else 1
                }

                // 更新页面进度
                if (currentPage > completedPages) {
                    completedPages = currentPage
                }

                // 更新计数
                this.annotationCount = annotationCount
                this.bookmarkCount = bookmarkCount
            }

            Graph.database?.readingStatsDao()?.updateStats(stats)
            _currentStats.value = stats
            println("BookmarkViewModel.endSession: 更新统计 $stats")
        }
    }

    /**
     * 获取总阅读时长
     */
    public suspend fun getTotalReadingTime(): Long {
        return Graph.database?.readingStatsDao()?.getTotalReadingTime() ?: 0L
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun calculateDaysDifference(date1: String, date2: String): Int {
        try {
            val parts1 = date1.split("-")
            val parts2 = date2.split("-")

            val cal1 = Calendar.getInstance().apply {
                set(parts1[0].toInt(), parts1[1].toInt() - 1, parts1[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val cal2 = Calendar.getInstance().apply {
                set(parts2[0].toInt(), parts2[1].toInt() - 1, parts2[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = cal2.timeInMillis - cal1.timeInMillis
            return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            return 0
        }
    }
}
