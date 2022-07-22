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
import cn.archko.pdf.BackPressHandler
import cn.archko.pdf.common.AnalysticsHelper
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.entity.State
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.umeng.analytics.MobclickAgent
import java.io.File

@Composable
fun FileBrowserList(
    viewModel: FileViewModel,
    up: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result by viewModel.uiFileModel.collectAsState()
    if (result.state == State.INIT) {
        viewModel.loadFiles(null)
    }
    Logcat.d("FileBrowserList：${result.list}")

    if (!viewModel.isTop()) {
        BackPressHandler(onBackPressed = {
            if (!viewModel.isTop()) {
                viewModel.stack.pop()
            }
            if (!viewModel.stack.isEmpty()) {
                val path = viewModel.stack.peek()
                viewModel.loadFiles(path)
            } else {
                up()
            }
        })
    }

    val onClick: (FileBean) -> Unit = { it ->
        val clickedFile: File?

        if (it.type == FileBean.HOME) {
            clickedFile = File(viewModel.sdcardRoot)
        } else {
            clickedFile = it.file
        }
        if (it.isDirectory) {
            if (it.file != null) {
                viewModel.setCurrentPos(it.file!!.path, 0)
                viewModel.loadFiles(it.file!!.path)
            }
            val map = HashMap<String, String>()
            map["type"] = "dir"
            map["name"] = clickedFile!!.name
            MobclickAgent.onEvent(context, AnalysticsHelper.A_FILE, map)
        } else {
            if (it.file != null) {
                PDFViewerHelper.openWithDefaultViewer(it.file!!, context)
            }
        }
    }
    val refresh: () -> Unit = { ->
        viewModel.loadFiles(null, true)
    }
    val loading = result.state == State.LOADING
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
            MenuItemType.ViewBookWithComposeMupdf -> {
                PDFViewerHelper.openComposeViewerMupdf(fb.file!!, context)
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
            MenuItemType.DeleteFile -> {
                viewModel.deleteFile(fb)
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
        SwipeRefresh(
            state = rememberSwipeRefreshState(loading),
            onRefresh = refresh,
        ) {
            FileList(
                result,
                FileBeanType.SysFile,
                loadMore = { },
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
