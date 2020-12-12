package cn.archko.pdf.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.RecentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2020/11/16 :11:23
 */
class BackupViewModel : ViewModel() {

    private val _uiFileModel = MutableLiveData<List<File>>()
    val uiFileModel: LiveData<List<File>>
        get() = _uiFileModel

    fun backupFiles() =
        viewModelScope.launch {
            val files: List<File>? = withContext(Dispatchers.IO) {
                return@withContext RecentManager.instance.backupFiles
            }

            withContext(Dispatchers.Main) {
                _uiFileModel.value = files
            }
        }
}