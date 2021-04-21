import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.LoadingFooter
import cn.archko.pdf.components.Surface
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.paging.ResourceState
import cn.archko.pdf.ui.home.EmptyView
import cn.archko.pdf.ui.home.FileBeanType
import cn.archko.pdf.ui.home.FileInfoDialog
import cn.archko.pdf.ui.home.FileItem
import cn.archko.pdf.ui.home.MenuItemType
import cn.archko.pdf.ui.home.UserOptDialog
import cn.archko.pdf.viewmodel.FileViewModel

@Composable
fun FileList(
    list: MutableList<FileBean>,
    totalCount: Int,
    resourceState: ResourceState,
    fileBeanType: FileBeanType = FileBeanType.SysFile,
    loadMore: (Int) -> Unit,
    showUserDialog: MutableState<Boolean>,
    showInfoDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier,
) {
    if (resourceState.value == ResourceState.INIT || list.isEmpty()) {
        EmptyView(modifier)
    } else {
        Surface(modifier = modifier.fillMaxSize()) {
            Box {
                ItemList(
                    list,
                    totalCount,
                    resourceState,
                    fileBeanType,
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    list: MutableList<FileBean>,
    totalCount: Int,
    resourceState: ResourceState,
    fileBeanType: FileBeanType,
    loadMore: (Int) -> Unit,
    showUserDialog: MutableState<Boolean>,
    showInfoDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState(0)
    val size = list.size
    val fileIndex = remember { mutableStateOf(0) }
    val onOptClick: (Int) -> Unit = { it ->
        fileIndex.value = it
        if (it > list.size) {
            fileIndex.value = 0
        }
        showUserDialog.value = true
    }
    Logcat.d("showUserDialog:${showUserDialog.value}, file.fileIndex:${fileIndex.value}")
    if (fileIndex.value < size) {
        showUserDialog.value = false
    }
    val fileBean = if (fileIndex.value >= size) null else list[fileIndex.value]
    UserOptDialog(showUserDialog, fileBean, menuOpt, fileBeanType)
    FileInfoDialog(showInfoDialog, fileBean, menuOpt)
    LazyColumn(modifier) {
        itemsIndexed(list) { index, fileBean ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            FileItem(
                fileBean = fileBean,
                index = index,
                onOptClick = onOptClick,
                onClick = onClick,
                viewModel = viewModel
            )
            //Log.d("ItemList", "$index")
            if (index == size - 1) {
                val hasMore = totalCount > size
                LoadingFooter(
                    resourceState = resourceState,
                    onClick = { loadMore(size) },
                    hasMore = hasMore,
                    visible = true
                )
                Logcat.d("scroll.totalCount:$totalCount.index:$index, ${scroll.isScrollInProgress}, resourceState:${resourceState.value}")
                if (!scroll.isScrollInProgress && hasMore) {
                    if (resourceState.value == ResourceState.LOADING || resourceState.value == ResourceState.ERROR) {
                    } else {
                        loadMore(size)
                    }
                }
            }
        }
    }
}
