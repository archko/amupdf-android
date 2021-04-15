package cn.archko.pdf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.App
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.paging.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

/**
 * @author: archko 2021/4/11 :8:14 上午
 */
class FileViewModel() : ViewModel() {

    private val _dataLoading = MutableStateFlow(ResourceState())
    val dataLoading: StateFlow<ResourceState>
        get() = _dataLoading

    private val _uiFileModel = MutableStateFlow<MutableList<FileBean>>(mutableListOf())
    val uiFileModel: StateFlow<MutableList<FileBean>>
        get() = _uiFileModel

    private val _uiFileHistoryModel = MutableStateFlow<MutableList<FileBean>>(mutableListOf())
    val uiFileHistoryModel: StateFlow<MutableList<FileBean>>
        get() = _uiFileHistoryModel

    var home: String = "/sdcard/"
    var selectionIndex = 0

    init {
        var externalFileRootDir: File? = App.instance!!.getExternalFilesDir(null)
        do {
            externalFileRootDir = Objects.requireNonNull(externalFileRootDir)?.parentFile
        } while (Objects.requireNonNull(externalFileRootDir)?.absolutePath?.contains("/Android") == true
        )
        home = externalFileRootDir?.path!!
        Logcat.d("home:$home")
    }

    fun isHome(path: String): Boolean {
        return path == home
    }

    var mCurrentPath: String? = null

    var stack: Stack<String> = Stack<String>()
    fun isTop(): Boolean {
        if (stack.isEmpty()) {
            return true
        }
        val top = stack.peek()
        return top == home
    }

    fun loadFiles(currentPath: String?, dirsFirst: Boolean = true, showExtension: Boolean = true) {
        val path = currentPath ?: home
        val f = File(path)
        if (!f.exists() || !f.isDirectory) {
            return
        }
        mCurrentPath = path
        if (!stack.contains(mCurrentPath)) {
            stack.push(mCurrentPath)
        }
        Logcat.d("loadFiles, path:$mCurrentPath")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val fileList: ArrayList<FileBean> = ArrayList()
                var entry: FileBean

                entry = FileBean(FileBean.HOME, home)
                fileList.add(entry)
                if (mCurrentPath != "/") {
                    val upFolder = File(mCurrentPath!!).parentFile
                    entry = FileBean(FileBean.NORMAL, upFolder!!, "..")
                    fileList.add(entry)
                }
                val files = File(mCurrentPath!!).listFiles()
                Logcat.d("$dirsFirst, path:$mCurrentPath, files:$files")
                if (files != null) {
                    try {
                        Arrays.sort(files, Comparator<File> { f1, f2 ->
                            if (f1 == null) throw RuntimeException("f1 is null inside sort")
                            if (f2 == null) throw RuntimeException("f2 is null inside sort")
                            try {
                                if (dirsFirst && f1.isDirectory != f2.isDirectory) {
                                    if (f1.isDirectory)
                                        return@Comparator -1
                                    else
                                        return@Comparator 1
                                }
                                return@Comparator f2.lastModified().compareTo(f1.lastModified())
                            } catch (e: NullPointerException) {
                                throw RuntimeException("failed to compare $f1 and $f2", e)
                            }
                        })
                    } catch (e: NullPointerException) {
                        throw RuntimeException(
                            "failed to sort file list $files for path $mCurrentPath",
                            e
                        )
                    }

                    for (file in files) {
                        entry = FileBean(FileBean.NORMAL, file, showExtension)
                        fileList.add(entry)
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiFileModel.value = fileList
                }
            }
        }
    }

    fun loadFileBeanfromJson(currentPath: String?) {
        Logcat.d("$currentPath")
        viewModelScope.launch {
            flow {
                val fileBeans = loadFileBeans()
                emit(fileBeans)
            }.catch { e ->
                Logcat.d("Exception:$e")
                emit(mutableListOf())
            }.flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiFileHistoryModel.value = list
                }
        }
    }

    private suspend fun loadFileBeans(): MutableList<FileBean> {
        val path = "$home/amupdf/mupdf_2021-04-12-07-30-02"
        val content = readStringFromInputStream(FileInputStream(path))
        //return JsonUtils.parseFileBeanFromJson(content)
        return null
    }

    private fun readStringFromInputStream(input: InputStream?): String {
        if (input == null) {
            return ""
        }
        var bos: ByteArrayOutputStream? = null
        try {
            bos = ByteArrayOutputStream()
            val buffer = ByteArray(512)
            var len: Int
            while (input.read(buffer).also { len = it } != -1) {
                bos.write(buffer, 0, len)
            }
            return String(bos.toByteArray(), Charset.forName("UTF-8"))
        } catch (e: Exception) {
        } finally {
            if (null != bos) {
                bos.close()
            }
        }
        return ""
    }

    fun update(fb: FileBean) {
        Logcat.d("$fb")
        val fileList: ArrayList<FileBean> = ArrayList()
        fileList.add(fb)
        _uiFileModel.value = fileList
    }
}