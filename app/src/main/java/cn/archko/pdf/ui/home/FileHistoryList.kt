package cn.archko.pdf.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.BackPressHandler
import cn.archko.pdf.paging.ResourceState
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.JetsnackSurface
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
        viewModel.loadFileBeanfromJson(null)
    }

    if (!viewModel.isTop()) {
        BackPressHandler(onBackPressed = {
            if (!viewModel.isTop()) {
                viewModel.stack.pop()
            }
            val path = viewModel.stack.peek()
            viewModel.loadFileBeanfromJson(path)
        })
    }

    val response by viewModel.uiFileHistoryModel.collectAsState()
    val onClick: (FileBean) -> Unit = { it ->
        if (it.file != null) {
            viewModel.loadFileBeanfromJson(it.file!!.path)
        }
    }
    //val refresh: () -> Unit = { ->
    //}
    VLog.d("FileList,${response} $navigateTo,")
    val menuOpt: (MenuItemType, FileBean) -> Unit = { _, fb ->
        viewModel.update(fb)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        FileList(
            response,
            resourceState,
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
                ItemList(list, modifier, menuOpt, onClick, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    list: MutableList<FileBean>,
    modifier: Modifier = Modifier,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel
) {
    val showUserDialog = remember { mutableStateOf(false) }
    var fileIndex = remember { mutableStateOf(0) }
    val onOptClick: (Int) -> Unit = { it ->
        fileIndex.value = it
        if (it > list.size) {
            fileIndex.value = 0
        }
        showUserDialog.value = true
    }
    VLog.d("item:${showUserDialog.value}, file:${fileIndex.value}")
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
        }
    }
}