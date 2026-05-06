package cn.archko.pdf.core.common

import cn.archko.pdf.core.entity.AnnotationPath
import cn.archko.pdf.core.entity.DrawType
import cn.archko.pdf.core.entity.Offset
import cn.archko.pdf.core.entity.PathConfig
import cn.archko.pdf.core.utils.FileUtils
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * @author: archko 2026/2/3 :08:47
 */
class AnnotationManager(val path: String?) {
    val decodeScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default.limitedParallelism(1))

    // 使用普通MutableMap，在View系统中使用
    val annotations: MutableMap<Int, MutableList<AnnotationPath>> =
        mutableMapOf()

    private val _annotationsFlow = MutableStateFlow<Map<Int, List<AnnotationPath>>>(emptyMap())
    val annotationsFlow: StateFlow<Map<Int, List<AnnotationPath>>> =
        _annotationsFlow.asStateFlow()

    // 撤销/重做栈：记录的是"操作指令"
    private val undoStack = mutableListOf<UndoAction>()
    private val redoStack = mutableListOf<UndoAction>()

    // 用于 UI 判断按钮是否可用 - 使用StateFlow来管理状态
    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        decodeScope.launch {
            loadFromFile()
            updateAnnotationsFlow()
        }
    }

    sealed class UndoAction {
        data class Add(val pageIndex: Int, val path: AnnotationPath) : UndoAction()
        // 未来可以扩展 Delete, Clear 等
    }

    fun addPath(pageIndex: Int, path: AnnotationPath) {
        val list = annotations.getOrPut(pageIndex) { mutableListOf() }
        list.add(path)
        undoStack.add(UndoAction.Add(pageIndex, path))
        redoStack.clear() // 新操作会清空重做栈

        updateAnnotationsFlow()

        decodeScope.launch {
            saveToFile()
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        when (val action = undoStack.removeAt(undoStack.size - 1)) {
            is UndoAction.Add -> {
                val list = annotations[action.pageIndex]
                if (list != null) {
                    list.remove(action.path)
                    // 强制触发 Compose 重组：先移除再重新添加
                    if (list.isEmpty()) {
                        annotations.remove(action.pageIndex)
                    } else {
                        val pageIndex = action.pageIndex
                        val newList = list.toMutableList()
                        annotations.remove(pageIndex)
                        annotations[pageIndex] = newList
                    }
                }
                redoStack.add(action)

                updateAnnotationsFlow()

                decodeScope.launch {
                    saveToFile()
                }
            }
        }
    }

    fun deletePaths(pageIndex: Int) {
        annotations.remove(pageIndex)
        undoStack.clear()
        redoStack.clear()

        updateAnnotationsFlow()

        decodeScope.launch {
            saveToFile()
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        when (val action = redoStack.removeAt(redoStack.size - 1)) {
            is UndoAction.Add -> {
                val pageIndex = action.pageIndex
                val list = annotations.getOrPut(pageIndex) { mutableListOf() }
                list.add(action.path)
                // 强制触发 Compose 重组：先移除再重新添加
                val newList = list.toMutableList()
                annotations.remove(pageIndex)
                annotations[pageIndex] = newList
                undoStack.add(action)

                updateAnnotationsFlow()

                decodeScope.launch {
                    saveToFile()
                }
            }
        }
    }

    fun toJson(): String {
        val file = File(path)
        val size = file.length()
        val fileName = file.name
        return try {
            val root = JSONObject()
            root.put("fileName", fileName)
            root.put("size", size)

            val annoArray = JSONArray()
            annotations.forEach { (pageIndex, paths) ->
                if (paths.isNotEmpty()) {
                    val pageObj = JSONObject()
                    pageObj.put("page", pageIndex)

                    val pathsArray = JSONArray()
                    paths.forEach { path ->
                        val pathObj = JSONObject()

                        val pointsArray = JSONArray()
                        path.points.forEach { offset ->
                            val pointObj = JSONObject()
                            pointObj.put("x", offset.x)
                            pointObj.put("y", offset.y)
                            pointsArray.put(pointObj)
                        }
                        pathObj.put("points", pointsArray)

                        val configObj = JSONObject()
                        configObj.put("c", Integer.toHexString(path.config.color))
                        configObj.put("s", path.config.strokeWidth)
                        configObj.put("d", path.config.drawType.name)
                        pathObj.put("config", configObj)

                        pathsArray.put(pathObj)
                    }
                    pageObj.put("paths", pathsArray)
                    annoArray.put(pageObj)
                }
            }
            root.put("anno", annoArray)

            root.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }

    fun saveToFile() {
        try {
            val saveFile = getAnnotationCacheFile()
            val content = toJson()
            println("save.${saveFile.absolutePath}")
            saveFile.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFromFile(): Boolean {
        return try {
            val saveFile = getAnnotationCacheFile()
            if (!saveFile.exists()) {
                return false
            }
            val content = saveFile.readText(Charsets.UTF_8)
            println("load.${saveFile.absolutePath}")
            if (content.isNotEmpty()) {
                fromJson(content)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun fromJson(json: String) {
        try {
            annotations.clear()
            val file = File(path)
            val size = file.length()
            val fileName = file.name
            val jsonObj = JSONObject(json)
            if (fileName != jsonObj.optString("fileName")) {
                println("new fileName:$fileName")
                getAnnotationCacheFile().delete()
                return
            }
            if (size != jsonObj.optLong("size")) {
                println("new filesize:$size")
                getAnnotationCacheFile().delete()
                return
            }
            val annoArray = jsonObj.optJSONArray("anno")

            if (annoArray != null) {
                for (i in 0 until annoArray.length()) {
                    val pageObj = annoArray.optJSONObject(i) ?: continue
                    val pageIndex = pageObj.optInt("page", -1)
                    if (pageIndex == -1) continue

                    val pathsArray = pageObj.optJSONArray("paths")
                    if (pathsArray != null) {
                        for (j in 0 until pathsArray.length()) {
                            val pathObj = pathsArray.optJSONObject(j) ?: continue
                            val pointsArray = pathObj.optJSONArray("points")
                            val configObj = pathObj.optJSONObject("config")

                            if (pointsArray != null && configObj != null) {
                                val points = mutableListOf<Offset>()
                                for (k in 0 until pointsArray.length()) {
                                    val pointObj = pointsArray.optJSONObject(k) ?: continue
                                    val x = pointObj.optDouble("x").toFloat()
                                    val y = pointObj.optDouble("y").toFloat()
                                    points.add(Offset(x, y))
                                }

                                val colorStr = configObj.optString("c")
                                val color = try {
                                    colorStr.toLongOrNull(16)?.toInt() ?: Color.RED
                                } catch (e: Exception) {
                                    Color.RED
                                }
                                val strokeWidth = configObj.optDouble("s").toFloat()
                                val drawTypeStr = configObj.optString("d", "CURVE")
                                val drawType = try {
                                    DrawType.valueOf(drawTypeStr)
                                } catch (e: IllegalArgumentException) {
                                    DrawType.CURVE
                                }

                                val config = PathConfig(
                                    color = color,
                                    strokeWidth = strokeWidth,
                                    drawType = drawType
                                )

                                val annotationPath = AnnotationPath(points, config)
                                annotations.getOrPut(pageIndex) { mutableListOf() }
                                    .add(annotationPath)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAnnotationCacheFile(): File {
        val file = File(path)
        val fileName = file.nameWithoutExtension
        val dir = FileUtils.getStorageDir("amupdf")
        return File(dir, "anno/$fileName.json")
    }

    private fun updateAnnotationsFlow() {
        val immutableMap = annotations.mapValues { (_, paths) ->
            paths.toList()
        }
        _annotationsFlow.value = immutableMap
        // 同时更新撤销/重做按钮状态
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}