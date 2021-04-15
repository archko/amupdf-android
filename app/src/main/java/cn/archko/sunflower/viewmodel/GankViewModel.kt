package cn.archko.sunflower.viewmodel

import GankBean
import GankResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cn.archko.sunflower.di.DemoDIGraph
import cn.archko.sunflower.model.ACategory
import cn.archko.sunflower.paging.PageGankResponseSource
import cn.archko.sunflower.repo.GankRepositoryImpl
import cn.archko.sunflower.repo.VideoRepo
import cn.archko.sunflower.paging.ResourceState
import cn.archko.sunflower.ui.utils.VLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 */
class GankViewModel(private val gankRepository: GankRepositoryImpl = DemoDIGraph.gankRepository) :
    ViewModel() {

    private val _dataLoading = MutableStateFlow(ResourceState())
    val dataLoading: StateFlow<ResourceState>
        get() = _dataLoading

    private val _gankResponse =
        MutableStateFlow(GankResponse<MutableList<GankBean>>(0, 0, 0, 0, "", "", mutableListOf()))

    val gankResponse: StateFlow<GankResponse<MutableList<GankBean>>>
        get() = _gankResponse

    init {

    }

    private var _pageIndex: Int = 1

    fun loadMoreGankGirls() {
        loadGankGirls(_pageIndex)
    }

    fun loadGankGirls(pageIndex: Int) {
        VLog.d("loadGankGirls,$pageIndex, old index:$_pageIndex")
        _dataLoading.value = ResourceState(ResourceState.LOADING)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                var response: GankResponse<MutableList<GankBean>>? =
                    VideoRepo.getGankGirls(pageIndex)
                var dataList: MutableList<GankBean> = _gankResponse.value.data
                if (response != null) {
                    dataList.addAll(response.data)
                    response.data = dataList
                }
                response
            }
            if (result != null) {
                _gankResponse.value = result
                if (result.data.size > 0) {
                    _pageIndex++
                }
                _dataLoading.value = ResourceState(ResourceState.FINISHED)
            } else {
                _dataLoading.value = ResourceState(ResourceState.ERROR)
            }
        }
    }

    fun loadGankGirlsPaging(): Flow<PagingData<GankBean>> {
        return Pager(config = PagingConfig(pageSize = 20, initialLoadSize = 20))
        {
            PageGankResponseSource { pageNo, pageSize ->
                gankRepository.getGankResponse(pageNo, pageSize)
            }
        }.flow.cachedIn(viewModelScope)
    }
}