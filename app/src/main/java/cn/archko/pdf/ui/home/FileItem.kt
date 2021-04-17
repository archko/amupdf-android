package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        top = 1.dp,
                        start = 4.dp,
                        end = 4.dp,
                        bottom = 1.dp,
                    )
                    .height(48.dp)
            ) {
                val progress = fileBean.getProgress()
                BookProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxSize()
                )
                fileBean.label?.let {
                    Text(
                        text = it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                    )
                }
            }
            Text(
                text = fileBean.getSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .align(Alignment.CenterVertically)
            )
            if (!fileBean.isDirectory) {
                val file = fileBean.file
                if (file != null) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clickable(onClick = {
                                onOptClick(index)
                            })
                            .padding(start = 2.dp)
                    )
                }
            }
        }
    }
}
