package cn.archko.sunflower.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.sunflower.model.ACategory
import cn.archko.sunflower.repo.VideoRepo
import cn.archko.sunflower.ui.utils.VLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 */
class VideoViewModel : ViewModel() {

    private val _categories = MutableStateFlow(CategoryReponse<ACategory>())

    val categoryResponse: StateFlow<CategoryReponse<ACategory>>
        get() = _categories

    init {
    }

    fun loadCategories(pageIndex: Int) {
        VLog.d("loadCategorie,$pageIndex")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val list: List<ACategory>? = VideoRepo.getACategory()
                if (null != list) {
                    CategoryReponse(
                        pageIndex,
                        categories = list
                    )
                } else {
                    null
                }
            }
            if (null != result) {
                _categories.value = result
            }
        }
    }
}

data class CategoryReponse<T>(
    val page: Int = 0,
    val categories: List<T> = emptyList(),
)
