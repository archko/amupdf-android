package cn.archko.pdf.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.filesystem.FileExtensionFilter
import cn.archko.pdf.core.filesystem.FileSystemScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONException
import java.io.File
import java.util.Locale

/**
 * @author: archko 2024/11/16 :11:23
 */
class LibraryViewModel : ViewModel() {
    companion object {

    }

    private val progressDao by lazy { Graph.database.progressDao() }

    var list: MutableList<FileBean> = mutableListOf()
    private val fileSystemScanner = FileSystemScanner()
    private val fileMap = mutableMapOf<String, MutableList<FileBean>>()

    private val _uiFileModel = MutableLiveData<MutableList<FileBean>>()
    val uiFileModel: LiveData<MutableList<FileBean>>
        get() = _uiFileModel

    val listener = object : FileSystemScanner.Listener {
        override fun onFileScan(parent: File?, files: Array<out File>?) {
            doOnFileScan(parent, files)
        }

        override fun onFileAdded(parent: File?, f: File?) {
            Logcat.d(Logcat.TAG, "onFileAdded:$f")
        }

        override fun onFileDeleted(parent: File?, f: File?) {
            Logcat.d(Logcat.TAG, "onFileDeleted:$f")
        }

        override fun onDirAdded(parent: File?, f: File?) {
            Logcat.d(Logcat.TAG, "onDirAdded:$f")
        }

        override fun onDirDeleted(parent: File?, f: File?) {
            Logcat.d(Logcat.TAG, "onDirDeleted:$f")
        }
    }
    val progressListener = object : FileSystemScanner.ProgressListener {
        override fun showProgress(show: Boolean) {
            if (!show) {
                Logcat.d(Logcat.TAG, "onFileScaned: ${fileMap.size}")
                val fileList = mutableListOf<FileBean>()
                for (entry in fileMap.entries) {
                    fileList.addAll(entry.value)
                }
                Logcat.d(Logcat.TAG, "onFileScaned: ${fileMap.size}, ${fileList.size}")
                _uiFileModel.value = fileList
                list = fileList
            }
        }
    }

    private fun doOnFileScan(parent: File?, files: Array<out File>?) {
        if (files != null && parent != null) {
            val fileList = mutableListOf<FileBean>()
            Logcat.d(Logcat.TAG, "onFileScan:$parent, ${files.size}")
            for (file in files) {
                if (file.isFile) {
                    val entry = FileBean(FileBean.NORMAL, file, true)
                    fileList.add(entry)
                }
            }
            fileMap[parent.path] = fileList
        }
    }

    suspend fun scan(path: String?) = flow {
        try {
            fileSystemScanner.addListener(listener)
            fileSystemScanner.addListener(progressListener)

            fileSystemScanner.startScan(object : FileExtensionFilter() {
                override fun accept(file: File?, name: String?): Boolean {
                    if (null == file) {
                        return false
                    }
                    val fname = file.name.lowercase(Locale.ROOT)
                    if (fname.startsWith(".")) {
                        return false
                    }
                    if (file.isDirectory || file.isHidden) {
                        return true
                    }

                    return IntentFile.isMuPdf(fname)
                            || IntentFile.isImage(fname)
                            || IntentFile.isText(fname)
                            || IntentFile.isDjvu(fname)
                            || IntentFile.isMobi(fname)
                }
            }, path)
            emit(ResponseHandler.Success(""))
        } catch (e: JSONException) {
            emit(ResponseHandler.Failure())
            e.printStackTrace()
        } catch (e: Exception) {
            emit(ResponseHandler.Failure())
            Logcat.e(Logcat.TAG, e.message)
        }
    }.flowOn(Dispatchers.IO)
        .collectLatest {
            Logcat.d(Logcat.TAG, "success")
        }
}