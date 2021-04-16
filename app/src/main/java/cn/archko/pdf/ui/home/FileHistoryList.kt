package cn.archko.pdf.ui.home

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.archko.pdf.common.AnalysticsHelper
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.JetsnackSurface
import cn.archko.pdf.components.LoadingFooter
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.paging.ResourceState
import cn.archko.pdf.viewmodel.FileViewModel
import com.umeng.analytics.MobclickAgent
import java.io.File
import java.util.*

@Composable
fun FileHistoryList(
    viewModel: FileViewModel,
    navigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resourceState by viewModel.dataLoading.collectAsState()
    if (viewModel.uiFileModel.value.isEmpty()
        && resourceState.value != ResourceState.ERROR
        && resourceState.value != ResourceState.LOADING
    ) {
        viewModel.loadHistoryFileBean(0)
    }

    val response by viewModel.uiFileHistoryModel.collectAsState()
    val loadMore: (Int) -> Unit = { index: Int ->
        Logcat.d("loadMore,$index")
        viewModel.loadMoreFileBeanFromDB(index)
    }
    val onClick: (FileBean) -> Unit = { it ->
        val clickedFile: File? = it.file

        val map = HashMap<String, String>()
        map.put("type", "file")
        map.put("name", clickedFile!!.name)
        MobclickAgent.onEvent(context, AnalysticsHelper.A_FILE, map)

        PDFViewerHelper.openWithDefaultViewer(Uri.fromFile(clickedFile), context)
    }
    //val refresh: () -> Unit = { ->
    //}
    Logcat.d("FileList,${response.isEmpty()} $navigateTo,")
    val showUserDialog = remember { mutableStateOf(false) }
    val menuOpt: (MenuItemType, FileBean) -> Unit = { menuType, fb ->
        showUserDialog.value = false
        when (menuType) {
            MenuItemType.ViewBookWithAMupdf -> {
                PDFViewerHelper.openWithDefaultViewer(fb.file!!, context)
            }
            MenuItemType.ViewBookWithMupdf -> {
                PDFViewerHelper.openViewerMupdf(fb.file!!, context)
            }
            MenuItemType.OpenWithOther -> {
                PDFViewerHelper.openViewerOther(fb.file!!, context)
            }
            MenuItemType.ViewBookInfo -> {
            }
            MenuItemType.DeleteHistory -> {
                MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, "remove")
                RecentManager.instance.removeRecentFromDb(fb.file!!.absolutePath)
                viewModel.loadFileBeanFromDB(0)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        FileList(
            response,
            resourceState,
            loadMore,
            modifier,
            showUserDialog,
            menuOpt,
            onClick,
            viewModel
        )
    }
}

@Composable
private fun FileList(
    list: MutableList<FileBean>,
    resourceState: ResourceState,
    loadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showUserDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel
) {
    if (resourceState.value == ResourceState.INIT || list.isEmpty()) {
        EmptyView(modifier)
    } else {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box {
                ItemList(
                    list,
                    resourceState,
                    loadMore,
                    modifier,
                    showUserDialog,
                    menuOpt,
                    onClick,
                    viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    list: MutableList<FileBean>,
    resourceState: ResourceState,
    loadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showUserDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel
) {
    val scroll = rememberScrollState(0)
    val size = list.size
    var fileIndex = remember { mutableStateOf(0) }
    val onOptClick: (Int) -> Unit = { it ->
        fileIndex.value = it
        if (it > list.size) {
            fileIndex.value = 0
        }
        showUserDialog.value = true
    }
    Logcat.d("item:${showUserDialog.value}, file:${fileIndex.value}")
    UserOptDialog(showUserDialog, list, fileIndex, menuOpt, FileBeanType.History)
    LazyColumn(modifier) {
        //item {
        //    Spacer(Modifier.statusBarsHeight(additional = 56.dp))
        //}
        itemsIndexed(list) { index, gankBean ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            FileItem(
                fileBean = gankBean,
                index = index,
                onOptClick = onOptClick,
                onClick = onClick,
                viewModel = viewModel
            )
            //Log.d("ItemList", "$index")
            if (index == size - 1) {
                LoadingFooter(
                    resourceState = resourceState,
                    onClick = { loadMore(size) },
                    total = size, visible = false
                )
                Logcat.d("scroll.index:$index, ${scroll.isScrollInProgress}, resourceState:${resourceState.value}")
                if (!scroll.isScrollInProgress) {
                    if (resourceState.value == ResourceState.LOADING || resourceState.value == ResourceState.ERROR) {
                    } else {
                        loadMore(size)
                    }
                }
            }
        }
    }
}