package cn.archko.pdf.viewmodel

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.filesystem.FileExtensionFilter
import cn.archko.pdf.core.filesystem.FileSystemScanner
import cn.archko.pdf.core.utils.CompareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONException
import java.io.File
import java.util.Collections
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
    private var comparator: Comparator<FileBean> = CompareUtils.NAME_ASC
    private val fileMap = mutableMapOf<String, MutableList<FileBean>>()

    private val _uiFileModel = MutableLiveData<MutableList<FileBean>>()
    val uiFileModel: LiveData<MutableList<FileBean>>
        get() = _uiFileModel

    private val listener = object : FileSystemScanner.Listener {
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

    private val progressListener = FileSystemScanner.ProgressListener { show ->
        if (!show) {
            AppExecutors.instance.networkIO().execute {
                comparator = CompareUtils.getSortor(PdfOptionRepository.getSort())
                Logcat.d(Logcat.TAG, "onFileScaned: ${fileMap.size}")
                val fileList = mutableListOf<FileBean>()
                for (entry in fileMap.entries) {
                    fileList.addAll(entry.value)
                }
                Collections.sort(fileList, comparator);
                Logcat.d(Logcat.TAG, "onFileScaned: ${fileMap.size}, ${fileList.size}")
                AppExecutors.instance.mainThread().execute {
                    _uiFileModel.value = fileList
                    list = fileList
                }
            }
        }
    }

    private fun doOnFileScan(parent: File?, files: Array<out File>?) {
        if (files != null && parent != null) {
            val fileList = mutableListOf<FileBean>()
            Logcat.d(Logcat.TAG, "onFileScan:$parent, ${files.size}")
            for (file in files) {
                if (file.isFile && !file.isHidden) {
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

    fun shutdown() {
        fileSystemScanner.shutdown()
    }

    fun search(text: String) = flow {
        try {
            if (TextUtils.isEmpty(text)) {
                emit(ResponseHandler.Success(list))
                return@flow
            }
            val result: MutableList<FileBean> = mutableListOf()
            for (fb in list) {
                if (null != fb.label && fb.label!!.contains(text)) {
                    result.add(fb)
                }
            }

            emit(ResponseHandler.Success(result))
        } catch (e: JSONException) {
            emit(ResponseHandler.Failure())
            e.printStackTrace()
        } catch (e: Exception) {
            emit(ResponseHandler.Failure())
            Logcat.e(Logcat.TAG, e.message)
        }
    }

    fun sort(cmp: Comparator<FileBean>) = flow {
        try {
            val result: MutableList<FileBean> = mutableListOf()
            result.addAll(list)
            if (cmp == comparator) {
                emit(ResponseHandler.Success(result))
                return@flow
            }

            comparator = cmp
            Collections.sort(result, comparator);
            emit(ResponseHandler.Success(result))
        } catch (e: JSONException) {
            emit(ResponseHandler.Failure())
            e.printStackTrace()
        } catch (e: Exception) {
            emit(ResponseHandler.Failure())
            Logcat.e(Logcat.TAG, e.message)
        }
    }
}