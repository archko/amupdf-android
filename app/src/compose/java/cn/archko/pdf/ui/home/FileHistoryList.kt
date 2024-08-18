package cn.archko.pdf.ui.home

import FileList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.components.FileBeanType
import cn.archko.pdf.components.MenuItemType
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FileHistoryList(
    viewModel: FileViewModel,
    navigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result by viewModel.historyFileModel.collectAsState()
    if (result.state == State.INIT) {
        viewModel.loadHistories()
    }
    Logcat.d("FileHistoryList:${result.list}")

    val loadMore: (Int) -> Unit = { index: Int ->
        Logcat.d("loadMore,$index")
        viewModel.loadHistories()
    }
    val onClick: (FileBean) -> Unit = { it ->
        it.file?.run {
            PDFViewerHelper.openAMupdf(it.file!!, context)
        }
    }
    val refresh: () -> Unit = { ->
        viewModel.loadHistories(true)
    }
    val loading = result.state == State.LOADING
    val showUserDialog = remember { mutableStateOf(false) }
    val showInfoDialog = remember { mutableStateOf(false) }
    val menuOpt: (MenuItemType, FileBean) -> Unit = { menuType, fb ->
        showUserDialog.value = false
        when (menuType) {
            MenuItemType.ViewBookWithAMupdf -> {
                fb.file?.run {
                    PDFViewerHelper.openAMupdfNoCrop(this, context)
                }
            }

            MenuItemType.OpenWithOther -> {
                PDFViewerHelper.openWithOther(fb.file!!, context)
            }

            MenuItemType.ViewBookInfo -> {
                //val map = HashMap<String, String>()
                //map["type"] = "info"
                //map["name"] = fb.file!!.name
                //MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
                showInfoDialog.value = true
            }

            MenuItemType.DeleteHistory -> {
                //MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, "remove")
                viewModel.deleteHistory(fb.file!!)
            }

            MenuItemType.DeleteHistoryAndClear -> {
                //MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, "removeRecentAndClearCache")
                viewModel.removeRecentAndClearCache(fb.file!!)
            }

            MenuItemType.AddToFav -> {
                viewModel.favorite(context, fb, 1)
            }

            MenuItemType.DeleteFav -> {
                viewModel.favorite(context, fb, 0)
            }

            else -> {}
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(loading),
            onRefresh = refresh,
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    // Pass the SwipeRefreshState + trigger through
                    state = state,
                    refreshTriggerDistance = trigger,
                    // Enable the scale animation
                    scale = true,
                    // Change the color and shape
                    //backgroundColor = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            FileList(
                result,
                FileBeanType.History,
                loadMore,
                showUserDialog,
                showInfoDialog,
                menuOpt,
                onClick,
                viewModel,
                modifier,
            )
        }
    }
}
