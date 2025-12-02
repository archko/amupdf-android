package cn.archko.pdf.viewmodel

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.fragments.FavoriteFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2020/11/16 :11:23
 */
class FavoriteViewModel : ViewModel() {

    var curPage = 0
    var list: MutableList<FileBean> = mutableListOf()

    private val progressDao by lazy { Graph.database.progressDao() }

    private val _uiFileModel = MutableLiveData<Array<Any?>>()
    val uiFileModel: LiveData<Array<Any?>>
        get() = _uiFileModel

    fun reset() {
        curPage = 0
    }

    fun loadFavorities(page: Int, showExtension: Boolean) =
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                var totalCount = 0

                totalCount = progressDao.getFavoriteProgressCount(1)
                val progresses = progressDao.getFavoriteProgresses(
                    FavoriteFragment.PAGE_SIZE * (page),
                    FavoriteFragment.PAGE_SIZE,
                    "1"
                )

                Logcat.d("loadFavorities:$page, $curPage, total:$totalCount, ${progresses?.size}")

                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                var file: File
                val path = Environment.getExternalStorageDirectory().path
                progresses?.map {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.FAVORITE, file, showExtension)
                    entry.bookProgress = it
                    entryList.add(entry)
                }

                val nList = arrayListOf<FileBean>()
                if (page > 0) {
                    nList.addAll(list)
                }
                if ((progresses?.size ?: 0) > 0) {
                    curPage++
                }
                nList.addAll(entryList)

                return@withContext arrayOf<Any?>(totalCount, nList)
            }

            withContext(Dispatchers.Main) {
                list = args[1] as MutableList<FileBean>
                _uiFileModel.value = args
            }
        }
}