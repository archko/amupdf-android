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
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FileFavoritiesList(
    viewModel: FileViewModel,
    navigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result by viewModel.uiFavoritiesModel.collectAsState()
    if (result.state == State.INIT) {
        viewModel.loadFavorities()
    }
    Logcat.d("FileFavoritiesList:${result.list}")

    val loadMore: (Int) -> Unit = { index: Int ->
        Logcat.d("loadMore,$index")
        viewModel.loadFavorities()
    }
    val onClick: (FileBean) -> Unit = { it ->
        it.file?.run {
            PDFViewerHelper.openAMupdf(it.file!!, context)
        }
    }
    val refresh: () -> Unit = { ->
        viewModel.loadFavorities(true)
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

            MenuItemType.AddToFav -> {
                viewModel.favorite(context, fb, 1, true)
            }

            MenuItemType.DeleteFav -> {
                viewModel.favorite(context, fb, 0, true)
            }

            else -> {}
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(loading),
            onRefresh = refresh,
        ) {
            FileList(
                result,
                FileBeanType.Favorite,
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
