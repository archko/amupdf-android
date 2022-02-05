import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.LoadingFooter
import cn.archko.pdf.components.Surface
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.paging.LoadResult
import cn.archko.pdf.paging.State
import cn.archko.pdf.paging.itemsIndexed
import cn.archko.pdf.ui.home.EmptyView
import cn.archko.pdf.ui.home.FileBeanType
import cn.archko.pdf.ui.home.FileHeaderItem
import cn.archko.pdf.ui.home.FileInfoDialog
import cn.archko.pdf.ui.home.FileItem
import cn.archko.pdf.ui.home.MenuItemType
import cn.archko.pdf.ui.home.UserOptDialog
import cn.archko.pdf.viewmodel.FileViewModel
import io.iamjosephmj.flinger.bahaviours.StockFlingBehaviours
import kotlinx.coroutines.launch

@Composable
fun FileList(
    result: LoadResult<Any, FileBean>,
    fileBeanType: FileBeanType = FileBeanType.SysFile,
    loadMore: (Int) -> Unit,
    showUserDialog: MutableState<Boolean>,
    showInfoDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier,
) {
    if (result.state == State.INIT || result.list == null) {
        EmptyView(modifier)
    } else {
        Surface(modifier = modifier.fillMaxSize()) {
            Box {
                ItemList(
                    result,
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

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    result: LoadResult<Any, FileBean>,
    fileBeanType: FileBeanType,
    loadMore: (Int) -> Unit,
    showUserDialog: MutableState<Boolean>,
    showInfoDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier,
) {
    val list = result.list!!
    val listState = rememberLazyListState(0)
    val coroutineScope = rememberCoroutineScope()

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

    val fileBean = if (fileIndex.value >= size) null else list[fileIndex.value]
    UserOptDialog(showUserDialog, fileBean, menuOpt, fileBeanType)
    FileInfoDialog(showInfoDialog, fileBean, menuOpt)
    LazyColumn(
        state = listState,
        flingBehavior = StockFlingBehaviours.smoothScroll(),
        modifier = modifier
    ) {
        if (fileBeanType == FileBeanType.SysFile) {
            stickyHeader {
                FileHeaderItem(
                    onClick = onClick,
                    viewModel = viewModel
                )
            }
        }
        val hasMore = result.nextKey != null
        itemsIndexed(list) { index, fileBean ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            fileBean?.let {
                FileItem(
                    fileBean = fileBean,
                    index = index,
                    onOptClick = onOptClick,
                    onClick = {
                        viewModel.setCurrentPos(listState.firstVisibleItemIndex)
                        onClick(it)
                    },
                    viewModel = viewModel
                )
            }
            //Logcat.d("hasMore:$hasMore,prevKey:${result.prevKey},nextKey:${result.nextKey}")
            if (hasMore && index == size - 1 && result.state != State.LOADING) {
                loadMore(size)
            }
        }

        Logcat.d("hasMore:$hasMore，state:${result.state}")
        result.apply {
            when {
                result.state is State.LOADING -> {
                    item {
                        LoadingFooter(
                            state = State.LOADING,
                            onClick = { },
                            hasMore = hasMore
                        )
                    }
                }
                result.state is State.FINISHED -> {
                    item {
                        LoadingFooter(
                            state = State.FINISHED,
                            onClick = { loadMore(0) },
                            hasMore = hasMore
                        )
                    }
                }
                result.state is State.ERROR -> {
                    item {
                        LoadingFooter(
                            state = State.ERROR,
                            onClick = { /*result.retry()*/ },
                            hasMore = hasMore
                        )
                    }
                }
            }
        }
    }
    val firstVisibleIndex = viewModel.getCurrentPos()
    if (firstVisibleIndex >= 0) {
        coroutineScope.launch {
            listState.scrollToItem(firstVisibleIndex)
            viewModel.setCurrentPos(-1)
        }
    }
}
