package cn.archko.pdf.paging

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * @author: archko 2021/4/22 :8:14 上午
 */
/*class AKPager<Data : Any, Value : Any>() {

    private val _resourceConfig =
        MutableStateFlow(ResourceConfig<Data, Value>(State.INIT, LoadResult()))
    val resourceConfig: StateFlow<ResourceConfig<Data, Value>>
        get() = _resourceConfig

    suspend fun doLoad(
        params: Map<String, String>,
        loadPage: suspend (params: Map<String, String>) -> LoadResult<Data, List<Value>>
    ) {
        flow {
            val result: LoadResult<Data, List<Value>> = loadPage(params)
            emit(result)
        }.catch { e ->
            Logcat.d("Exception:$e")
            emit(ResourceResult())
        }.flowOn(Dispatchers.IO)
            .collect { result ->
                _resourceConfig.value = ResourceConfig(State.FINISHED, result)
            }
    }
}*/

fun <T : Any> LazyListScope.itemsIndexed(
    list: List<T>,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(index: Int, value: T?) -> Unit,
) {
    items(list.size, key) { index ->
        val item = list[index]
        itemContent(index, item)
    }
}