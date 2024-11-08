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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.components.BookProgressBar
import cn.archko.pdf.core.entity.FileBean
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
                    .size(44.dp)
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
                    .height(52.dp)
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
        val current = viewModel.getCurrentItem()
        current?.let {
            Row(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val path = stringResource(id = cn.archko.pdf.R.string.title_path) + current.label
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