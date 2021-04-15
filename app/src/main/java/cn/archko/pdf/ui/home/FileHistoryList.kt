package cn.archko.pdf.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.JetsnackSurface
import cn.archko.pdf.components.LoadingFooter
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.paging.ResourceState
import cn.archko.pdf.viewmodel.FileViewModel

@Composable
fun FileHistoryList(
    viewModel: FileViewModel,
    navigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resourceState by viewModel.dataLoading.collectAsState()
    if (viewModel.uiFileModel.value.isEmpty()
        && resourceState.value != ResourceState.ERROR
        && resourceState.value != ResourceState.LOADING
    ) {
        viewModel.loadFileBeanFromDB(0)
    }

    val response by viewModel.uiFileHistoryModel.collectAsState()
    val loadMore: (Int) -> Unit = { index: Int ->
        Logcat.d("loadMore,$index")
    }
    val onClick: (FileBean) -> Unit = { it ->
        if (it.file != null) {
            viewModel.loadFileBeanFromDB(0)
        }
    }
    //val refresh: () -> Unit = { ->
    //}
    Logcat.d("FileList,${response} $navigateTo,")
    val menuOpt: (MenuItemType, FileBean) -> Unit = { _, fb ->
    }
    Box(modifier = Modifier.fillMaxSize()) {
        FileList(
            response,
            resourceState,
            loadMore,
            modifier,
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
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel
) {
    if (resourceState.value == ResourceState.INIT || list.isEmpty()) {
        EmptyView(modifier)
    } else {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box {
                ItemList(list, resourceState, loadMore, modifier, menuOpt, onClick, viewModel)
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
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel
) {
    val scroll = rememberScrollState(0)
    val size = list.size
    val showUserDialog = remember { mutableStateOf(false) }
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
                //if (!scroll.isScrollInProgress) {
                //    if (resourceState.value == ResourceState.LOADING || resourceState.value == ResourceState.ERROR) {
                //    } else {
                //        loadMore(size)
                //    }
                //}
            }
        }
    }
}