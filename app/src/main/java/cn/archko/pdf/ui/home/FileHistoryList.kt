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
import cn.archko.pdf.common.AnalysticsHelper
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.paging.State
import cn.archko.pdf.viewmodel.FileViewModel
import com.umeng.analytics.MobclickAgent
import java.util.*

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
        PDFViewerHelper.openWithDefaultViewer(it.file!!, context)
    }
    //val refresh: () -> Unit = { ->
    //}
    val showUserDialog = remember { mutableStateOf(false) }
    val showInfoDialog = remember { mutableStateOf(false) }
    val menuOpt: (MenuItemType, FileBean) -> Unit = { menuType, fb ->
        showUserDialog.value = false
        when (menuType) {
            MenuItemType.ViewBookWithAMupdf -> {
                if (fb.file != null) {
                    PDFViewerHelper.openWithDefaultViewer(fb.file!!, context)
                }
            }
            MenuItemType.ViewBookWithMupdf -> {
                PDFViewerHelper.openViewerMupdf(fb.file!!, context)
            }
            MenuItemType.OpenWithOther -> {
                PDFViewerHelper.openViewerOther(fb.file!!, context)
            }
            MenuItemType.ViewBookInfo -> {
                val map = HashMap<String, String>()
                map["type"] = "info"
                map["name"] = fb.file!!.name
                MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
                showInfoDialog.value = true
            }
            MenuItemType.DeleteHistory -> {
                MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, "remove")
                viewModel.deleteHistory(fb.file!!)
            }
            MenuItemType.AddToFav -> {
                viewModel.favorite(context, fb, 1)
            }
            MenuItemType.DeleteFav -> {
                viewModel.favorite(context, fb, 0)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
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
