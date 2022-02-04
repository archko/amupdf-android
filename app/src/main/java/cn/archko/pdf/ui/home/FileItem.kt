package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.mupdf.R
import cn.archko.pdf.components.BookProgressBar
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.utils.getIcon
import cn.archko.pdf.utils.getProgress
import cn.archko.pdf.utils.getSize
import cn.archko.pdf.viewmodel.FileViewModel

@Composable
fun FileItem(
    fileBean: FileBean,
    index: Int,
    onOptClick: (Int) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    onClick(fileBean)
                })
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Image(
                painter = painterResource(id = fileBean.getIcon()),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterVertically)
            )
            Box(
                modifier
                    .padding(
                        top = 1.dp,
                        start = 4.dp,
                        end = 72.dp,
                        bottom = 1.dp,
                    )
                    .height(48.dp)
            ) {
                val progress = fileBean.getProgress()
                if (progress > 0) {
                    BookProgressBar(
                        progress = progress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                fileBean.label?.let {
                    Text(
                        text = it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 17.sp,
                    )
                }
            }
        }

        if (!fileBean.isDirectory) {
            Row(
                modifier = Modifier
                    .clickable(onClick = {
                        onOptClick(index)
                    })
                    .height(44.dp)
                    .align(alignment = Alignment.CenterEnd)
                    .padding(end = 2.dp)
            ) {
                Text(
                    text = fileBean.getSize(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .align(Alignment.CenterVertically)
                )
                val file = fileBean.file
                if (file != null) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, contentDescription = null,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
fun FileHeaderItem(
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        /*val home = viewModel.getHomeItem()
        Row(
            modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    onClick(home)
                })
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val path = stringResource(id = R.string.title_home) + home.label
            Text(
                text = path,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
            )
        }*/

        val current = viewModel.getCurrentItem()
        current?.let {
            Row(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val path = stringResource(id = R.string.title_path) + current.label
                Text(
                    text = path,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

/*
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemBeanList(
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
                    onClick = onClick,
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
                            onClick = { */
/*result.retry()*//*
 },
                            hasMore = hasMore
                        )
                    }
                }
            }
        }
    }
}*/
