package cn.archko.pdf.core.cache

import android.graphics.Bitmap

private const val CANDIDATE_TIMEOUT = 60_000L
private var MAX_MEMORY_BYTES = 256 * 1024 * 1024L
private var MAX_CANDIDATE_MEMORY_BYTES = MAX_MEMORY_BYTES / 4

private var PAGE_CACHE_MEMORY_BYTES = 32 * 1024 * 1024L
private var PAGE_CANDIDATE_MEMORY_BYTES = PAGE_CACHE_MEMORY_BYTES / 4

/**
 * Bitmap状态管理器，解决并发访问和生命周期问题
 * 使用引用计数确保正在使用的bitmap不会被回收
 */
public class BitmapState(
    bitmap: Bitmap,
    key: String,
    byteSize: Long // 缓存尺寸，避免重复计算
) {
    @JvmField
    public val bitmap: Bitmap = bitmap
    @JvmField
    public val key: String = key
    @JvmField
    public val byteSize: Long = byteSize

    private var referenceCount = 0
    private var isRecycled = false

    public fun acquire(): Boolean = synchronized(this) {
        if (isRecycled) return false
        referenceCount++
        return true
    }

    public fun release(): Unit = synchronized(this) {
        if (referenceCount > 0) referenceCount--
    }

    public fun markRecycled(): Boolean = synchronized(this) {
        if (referenceCount == 0 && !isRecycled) {
            isRecycled = true
            return true
        }
        return false
    }

    public fun canRecycle(): Boolean = synchronized(this) { referenceCount == 0 }
    public fun isRecycled(): Boolean = synchronized(this) { isRecycled }
}

/**
 * 线程安全的图片缓存，使用引用计数防止正在使用的bitmap被回收
 */
private class InnerImageCacheImpl(
    private var maxMemoryBytes: Long,
    private var maxCandidateMemoryBytes: Long = maxMemoryBytes / 4
) {
    // 使用 LinkedHashMap (accessOrder=true) 实现真正的 LRU
    private val cache = LinkedHashMap<String, BitmapState>(0, 0.75f, true)

    // 候选池：暂时不用的 Bitmap 停留区
    private val candidatePool = mutableMapOf<String, Pair<BitmapState, Long>>()

    private var currentMemoryBytes = 0L
    private var candidateMemoryBytes = 0L

    /**
     * 设置最大内存限制
     */
    public fun setMaxMemory(maxMemoryBytes: Long) {
        synchronized(this) {
            this.maxMemoryBytes = maxMemoryBytes
            this.maxCandidateMemoryBytes = maxMemoryBytes / 3
            trimToSize()
        }
    }

    public fun acquire(key: String): BitmapState? = synchronized(this) {
        // 1. 检查主缓存 (LinkedHashMap 会自动更新访问顺序)
        cache[key]?.let { state ->
            if (state.acquire()) return state
        }

        // 2. 检查候选池
        candidatePool[key]?.let { (state, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CANDIDATE_TIMEOUT) {
                if (state.acquire()) {
                    candidatePool.remove(key)
                    candidateMemoryBytes -= state.byteSize

                    cache[key] = state
                    currentMemoryBytes += state.byteSize
                    return state
                }
            } else {
                // 超时自动清理
                internalRecycle(key, state, fromCandidate = true)
            }
        }
        return null
    }

    public fun release(state: BitmapState) {
        state.release()
    }

    public fun put(key: String, bitmap: Bitmap): BitmapState = synchronized(this) {
        val imageSize = calculateImageSize(bitmap)

        // 如果已存在旧的，先移入候选池
        cache.remove(key)?.let { oldState ->
            currentMemoryBytes -= oldState.byteSize
            addToCandidatePool(key, oldState)
        }

        val state = BitmapState(bitmap, key, imageSize)
        cache[key] = state
        currentMemoryBytes += imageSize

        trimToSize()
        return state
    }

    /**
     * 核心逻辑：根据 LRU 顺序和引用计数清理内存
     */
    private fun trimToSize() {
        if (currentMemoryBytes <= maxMemoryBytes) return

        val iterator = cache.entries.iterator()
        while (iterator.hasNext() && currentMemoryBytes > maxMemoryBytes) {
            val entry = iterator.next()
            // 只有没有引用的才能被踢出主缓存
            if (entry.value.canRecycle()) {
                val state = entry.value
                iterator.remove()
                currentMemoryBytes -= state.byteSize
                addToCandidatePool(entry.key, state)
            }
        }
    }

    private fun addToCandidatePool(key: String, state: BitmapState) {
        // 如果候选池超限，先清理最老的
        while (candidateMemoryBytes + state.byteSize > maxCandidateMemoryBytes && candidatePool.isNotEmpty()) {
            val oldestKey = candidatePool.entries.minByOrNull { it.value.second }?.key
            oldestKey?.let { k ->
                candidatePool.remove(k)?.let { (s, _) ->
                    internalRecycle(k, s, fromCandidate = true)
                }
            }
        }

        candidatePool[key] = Pair(state, System.currentTimeMillis())
        candidateMemoryBytes += state.byteSize
    }

    private fun internalRecycle(key: String, state: BitmapState, fromCandidate: Boolean) {
        if (state.markRecycled()) {
            if (fromCandidate) candidateMemoryBytes -= state.byteSize
            else currentMemoryBytes -= state.byteSize

            recycleImageBitmap(state.bitmap)
        }
    }

    public fun remove(key: String) = synchronized(this) {
        cache.remove(key)?.let { state ->
            currentMemoryBytes -= state.byteSize
            addToCandidatePool(key, state)
        }
        cleanCandidatePool()
    }

    public fun hasNode(key: String): Boolean = synchronized(this) {
        return cache.containsKey(key) || candidatePool.containsKey(key)
    }

    public fun clear() = synchronized(this) {
        cache.values.forEach { if (it.markRecycled()) recycleImageBitmap(it.bitmap) }
        cache.clear()
        currentMemoryBytes = 0L

        candidatePool.values.forEach { (state, _) ->
            if (state.markRecycled()) recycleImageBitmap(state.bitmap)
        }
        candidatePool.clear()
        candidateMemoryBytes = 0L
    }

    public fun size(): Int = synchronized(this) { cache.size }

    private fun cleanCandidatePool() {
        val now = System.currentTimeMillis()
        val iterator = candidatePool.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val (state, timestamp) = entry.value
            if (now - timestamp > CANDIDATE_TIMEOUT) {
                if (state.markRecycled()) {
                    recycleImageBitmap(state.bitmap)
                    candidateMemoryBytes -= state.byteSize
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
