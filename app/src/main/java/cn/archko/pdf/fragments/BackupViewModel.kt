package cn.archko.pdf.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Arrays

/**
 * @author: archko 2020/11/16 :11:23
 */
class BackupViewModel : ViewModel() {

    private val _uiFileModel = MutableLiveData<List<File>>()
    val uiFileModel: LiveData<List<File>>
        get() = _uiFileModel

    fun backupFiles() =
        viewModelScope.launch {
            val files: List<File> = withContext(Dispatchers.IO) {
                var files: Array<File>? = null
                val dir = FileUtils.getStorageDir("amupdf")
                if (dir.exists()) {
                    files = dir.listFiles { pathname: File -> pathname.name.startsWith("mupdf_") }
                    if (files != null) {
                        Arrays.sort(files) { f1: File?, f2: File? ->
                            if (f1 == null) throw RuntimeException("f1 is null inside sort")
                            if (f2 == null) throw RuntimeException("f2 is null inside sort")
                            return@sort f2.lastModified().compareTo(f1.lastModified())
                        }
                    }
                }
                val list = ArrayList<File>()
                if (files != null) {
                    for (f in files) {
                        list.add(f)
                    }
                }
                return@withContext list
            }

            withContext(Dispatchers.Main) {
                _uiFileModel.value = files
            }
        }
}