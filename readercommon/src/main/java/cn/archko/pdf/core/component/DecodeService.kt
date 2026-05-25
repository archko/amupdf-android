package cn.archko.pdf.core.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于三队列优先级的解码服务
 * 参考Android DecodeServiceBase的设计，使用协程实现
 *
 * 优先级顺序：pageTask -> nodeTask -> cropTask
 * 只有当前优先级队列为空时，才处理下一优先级队列
 *
 * @author: archko 2025/1/10
 */
class DecodeService(
    private val decoder: Decoder
) {
    // 多平台通用的锁和队列
    private val pageQueue = mutableListOf<DecodeTask>()
    private val nodeQueue = mutableListOf<DecodeTask>()
    private val cropQueue = mutableListOf<DecodeTask>()
    private val pendingJobs = mutableMapOf<String, DecodeTask>()

    private val isShutdown = AtomicBoolean(false)
    private val lock = Any()

    private val singleThreadDispatcher = newSingleThreadContext("Decode-Worker-Thread")
    private val scope = CoroutineScope(SupervisorJob() + singleThreadDispatcher)

    init {
        // 启动一个常驻循环来监听队列
        scope.launch {
            while (!isShutdown.get()) {
                val task = synchronized(lock) {
                    val next = when {
                        pageQueue.isNotEmpty() -> pageQueue.removeAt(0)
                        nodeQueue.isNotEmpty() -> nodeQueue.removeAt(0)
                        cropQueue.isNotEmpty() -> cropQueue.removeAt(0)
                        else -> null
                    }
                    if (next == null) {
                        (lock as Object).wait(1000)
                        null
                    } else next
                }

                task?.let { executeTask(it) }
            }
        }
    }

    public fun submitTask(task: DecodeTask) {
        synchronized(lock) {
            pendingJobs[task.key] = task
            when (task.type) {
                TaskType.PAGE -> pageQueue.add(task)
                TaskType.NODE -> nodeQueue.add(task)
                TaskType.CROP -> cropQueue.add(task)
            }
            (lock as Object).notifyAll()
        }
    }

    public fun submitCropTasks(tasks: List<DecodeTask>) {
        synchronized(lock) {
            tasks.forEach { task ->
                if (!pendingJobs.containsKey(task.key)) {
                    pendingJobs[task.key] = task
                    cropQueue.add(task)
                }
            }
            (lock as Object).notifyAll()
        }
    }

    private suspend fun executeTask(task: DecodeTask) {
        synchronized(lock) {
            if (pendingJobs[task.key] != task) return
        }

        if (task.callback?.shouldRender(task.pageIndex, task.type == TaskType.PAGE) != true) {
            println("[DecodeService.shouldRender] page=${task.pageIndex}, $task")
            task.callback?.onFinish(task.pageIndex)
            return
        }

        try {
            val bitmap = when (task.type) {
                TaskType.PAGE -> decoder.decodePage(task)
                TaskType.NODE -> decoder.decodeNode(task)
                TaskType.CROP -> {
                    decoder.processCrop(task)
                    null
                }
            }
            if (bitmap != null) {
                task.callback?.onDecodeComplete(bitmap, task.type == TaskType.PAGE, null)
            }
        } catch (e: Exception) {
            task.callback?.onDecodeComplete(null, false, e)
        } finally {
            synchronized(lock) { pendingJobs.remove(task.key) }
            task.callback?.onFinish(task.pageIndex)
        }
    }

    public fun shutdown() {
        isShutdown.set(true)
        scope.cancel()
        singleThreadDispatcher.close()
        synchronized(lock) {
            pageQueue.clear()
            nodeQueue.clear()
            cropQueue.clear()
            pendingJobs.clear()
            (lock as Object).notifyAll()
        }
    }
}