package cn.archko.pdf.core.cache

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val CANDIDATE_TIMEOUT = 60_000L
private var MAX_MEMORY_BYTES = 256 * 1024 * 1024L
private var MAX_CANDIDATE_MEMORY_BYTES = MAX_MEMORY_BYTES / 4

private var PAGE_CACHE_MEMORY_BYTES = 32 * 1024 * 1024L
private var PAGE_CANDIDATE_MEMORY_BYTES = PAGE_CACHE_MEMORY_BYTES / 4

/**
 * Bitmap状态管理器，解决并发访问和生命周期问题
 * 使用引用计数确保正在使用的bitmap不会被回收
 *
 * 用一个 AtomicInteger 合并编码 refCount（低31位）和 recycled 标志（第32位），
 * 通过 CAS 循环实现所有方法完全无锁。
 */
public class BitmapState(
    public val bitmap: Bitmap,
    public val key: String,
    public val byteSize: Long
) {
    private val state = AtomicInteger(0)

    public fun acquire(): Boolean {
        while (true) {
            val current = state.get()
            if (current < 0) return false
            if (state.compareAndSet(current, current + 1)) return true
        }
    }

    public fun release(): Unit {
        while (true) {
            val current = state.get()
            val refCount = current and Int.MAX_VALUE
            if (refCount == 0) break
            if (state.compareAndSet(current, current - 1)) break
        }
    }

    public fun markRecycled(): Boolean {
        while (true) {
            val current = state.get()
            if (current < 0) return false
            val refCount = current and Int.MAX_VALUE
            if (refCount != 0) return false
            if (state.compareAndSet(current, current or Int.MIN_VALUE)) return true
        }
    }

    public fun canRecycle(): Boolean = (state.get() and Int.MAX_VALUE) == 0
    public fun isRecycled(): Boolean = state.get() < 0
}

/**
 * 线程安全的图片缓存，使用 ConcurrentHashMap + 独立的 LRU 列表，
 * 将内存记账 eviction 与 查询/插入 分离，主线程读写完全无锁。
 */
private class InnerImageCacheImpl(
    private var maxMemoryBytes: Long,
    private var maxCandidateMemoryBytes: Long = maxMemoryBytes / 4
) {
    // 主缓存：ConcurrentHashMap 保证并发的 get/put/remove 无锁
    private val cache = ConcurrentHashMap<String, BitmapState>()

    // LRU 顺序链表：只用于 trimToSize() eviction 决策，不在主线程热路径上
    private val lruList = LinkedHashMap<String, BitmapState>(0, 0.75f, true)

    // 候选池：用 ConcurrentHashMap 避免与主线程锁争用
    private val candidatePool = ConcurrentHashMap<String, CandidateEntry>()

    private val currentMemoryBytes = AtomicLong(0)
    private val candidateMemoryBytes = AtomicLong(0)

    private val evictionLock = Any()

    private class CandidateEntry(
        val state: BitmapState,
        val timestamp: Long = System.currentTimeMillis()
    )

    public fun setMaxMemory(maxMemoryBytes: Long) {
        this.maxMemoryBytes = maxMemoryBytes
        this.maxCandidateMemoryBytes = maxMemoryBytes / 3
        val toRecycle = mutableListOf<Bitmap>()
        synchronized(evictionLock) { trimToSize(toRecycle) }
        toRecycle.forEach { recycleImageBitmap(it) }
    }

    public fun acquire(key: String): BitmapState? {
        // 1. 检查主缓存（ConcurrentHashMap 无锁读）
        cache[key]?.let { state ->
            if (state.acquire()) {
                return state
            }
        }

        // 2. 检查候选池（ConcurrentHashMap 无锁读）
        candidatePool.remove(key)?.let { entry ->
            if (System.currentTimeMillis() - entry.timestamp < CANDIDATE_TIMEOUT) {
                if (entry.state.acquire()) {
                    candidateMemoryBytes.addAndGet(-entry.state.byteSize)
                    cache[key] = entry.state
                    currentMemoryBytes.addAndGet(entry.state.byteSize)
                    return entry.state
                }
            } else {
                if (entry.state.markRecycled()) {
                    candidateMemoryBytes.addAndGet(-entry.state.byteSize)
                    recycleImageBitmap(entry.state.bitmap)
                }
            }
        }
        return null
    }

    public fun release(state: BitmapState) {
        state.release()
    }

    public fun put(key: String, bitmap: Bitmap): BitmapState {
        val imageSize = calculateImageSize(bitmap)
        val state = BitmapState(bitmap, key, imageSize)

        val oldState = cache.put(key, state)
        if (oldState != null) {
            currentMemoryBytes.addAndGet(-oldState.byteSize)
            addToCandidatePool(key, oldState)
        }
        currentMemoryBytes.addAndGet(imageSize)

        // trimToSize 把需要回收的 bitmap 收集到列表，锁外统一回收
        val toRecycle = mutableListOf<Bitmap>()
        synchronized(evictionLock) {
            lruList[key] = state
            trimToSize(toRecycle)
        }
        toRecycle.forEach { recycleImageBitmap(it) }

        return state
    }

    private fun trimToSize(toRecycle: MutableList<Bitmap>) {
        if (currentMemoryBytes.get() <= maxMemoryBytes) return

        val iterator = lruList.entries.iterator()
        while (iterator.hasNext() && currentMemoryBytes.get() > maxMemoryBytes) {
            val entry = iterator.next()
            if (entry.value.canRecycle()) {
                val evicted = cache.remove(entry.key)
                if (evicted != null) {
                    currentMemoryBytes.addAndGet(-evicted.byteSize)
                    addToCandidatePool(entry.key, evicted, toRecycle)
                }
                iterator.remove()
            }
        }
    }

    private fun addToCandidatePool(key: String, state: BitmapState, toRecycle: MutableList<Bitmap>? = null) {
        val entry = CandidateEntry(state)
        candidatePool[key] = entry
        candidateMemoryBytes.addAndGet(state.byteSize)

        // 候选池超限时回收最老的
        while (candidateMemoryBytes.get() > maxCandidateMemoryBytes) {
            val oldest = candidatePool.entries.minByOrNull { it.value.timestamp } ?: break
            val oldestKey = oldest.key
            val oldestEntry = oldest.value
            if (candidatePool.remove(oldestKey) != null) {
                if (oldestEntry.state.markRecycled()) {
                    candidateMemoryBytes.addAndGet(-oldestEntry.state.byteSize)
                    if (toRecycle != null) {
                        toRecycle.add(oldestEntry.state.bitmap)
                    } else {
                        recycleImageBitmap(oldestEntry.state.bitmap)
                    }
                }
            }
        }
    }

    public fun remove(key: String) {
        cache.remove(key)?.let { state ->
            currentMemoryBytes.addAndGet(-state.byteSize)
            addToCandidatePool(key, state)
        }
        val toRecycle = mutableListOf<Bitmap>()
        synchronized(evictionLock) {
            lruList.remove(key)
            cleanCandidatePool(toRecycle)
        }
        toRecycle.forEach { recycleImageBitmap(it) }
    }

    public fun hasNode(key: String): Boolean {
        return cache.containsKey(key) || candidatePool.containsKey(key)
    }

    public fun clear() {
        synchronized(evictionLock) {
            cache.values.forEach { if (it.markRecycled()) recycleImageBitmap(it.bitmap) }
            cache.clear()
            currentMemoryBytes.set(0)

            val candValues = candidatePool.values.toList()
            candidatePool.clear()
            candidateMemoryBytes.set(0)
            candValues.forEach { entry ->
                if (entry.state.markRecycled()) recycleImageBitmap(entry.state.bitmap)
            }

            lruList.clear()
        }
    }

    public fun size(): Int = cache.size

    private fun cleanCandidatePool(toRecycle: MutableList<Bitmap>) {
        val now = System.currentTimeMillis()
        val iterator = candidatePool.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val candidate = entry.value
            if (now - candidate.timestamp > CANDIDATE_TIMEOUT) {
                if (candidate.state.markRecycled()) {
                    toRecycle.add(candidate.state.bitmap)
                    candidateMemoryBytes.addAndGet(-candidate.state.byteSize)
                    iterator.remove()
                }
            }
        }
    }

    private fun calculateImageSize(bitmap: Bitmap): Long {
        return if (bitmap.isRecycled) 0L else bitmap.byteCount.toLong()
    }

    private fun recycleImageBitmap(bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            BitmapPool.getInstance().release(bitmap)
        }
    }
}

/**
 * 全局缓存实例
 */
public object ImageCache {
    // Node 高清图缓存（动态大小）
    private var nodeCache = InnerImageCacheImpl(MAX_MEMORY_BYTES, MAX_CANDIDATE_MEMORY_BYTES)

    // Page 缩略图缓存（固定 32MB）
    private val pageCache = InnerImageCacheImpl(PAGE_CACHE_MEMORY_BYTES, PAGE_CANDIDATE_MEMORY_BYTES)

    /**
     * 设置最大内存限制（只影响 Node 缓存）
     */
    public fun setMaxMemory(maxMemoryBytes: Long) {
        nodeCache.setMaxMemory(maxMemoryBytes)
        pageCache.setMaxMemory(maxMemoryBytes / 4)
    }

    /**
     * Node 高清图缓存操作
     */
    public fun acquireNode(key: String): BitmapState? = nodeCache.acquire(key)
    public fun releaseNode(state: BitmapState): Unit = nodeCache.release(state)
    public fun putNode(key: String, bitmap: Bitmap): BitmapState = nodeCache.put(key, bitmap)
    public fun removeNode(key: String): Unit = nodeCache.remove(key)
    public fun hasNode(key: String): Boolean = nodeCache.hasNode(key)
    public fun clearNodes(): Unit = nodeCache.clear()

    /**
     * Page 缩略图缓存操作
     */
    public fun acquirePage(key: String): BitmapState? = pageCache.acquire(key)
    public fun releasePage(state: BitmapState): Unit = pageCache.release(state)
    public fun putPage(key: String, bitmap: Bitmap): BitmapState = pageCache.put(key, bitmap)
    public fun removePage(key: String): Unit = pageCache.remove(key)
    public fun hasPage(key: String): Boolean = pageCache.hasNode(key)
    public fun clearPages(): Unit = pageCache.clear()
    public fun pageCount(): Int = pageCache.size()

    /**
     * 清空所有缓存
     */
    public fun clear() {
        nodeCache.clear()
        pageCache.clear()
    }
}
