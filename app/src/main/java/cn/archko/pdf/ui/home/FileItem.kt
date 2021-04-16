package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.common.Logcat
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
            )
            Box(
                modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        paddingValues = PaddingValues(
                            top = 2.dp,
                            start = 4.dp,
                            end = 4.dp,
                        )
                    )
                    .height(48.dp)
            ) {
                val progress = fileBean.getProgress()
                BookProgressBar(
                    progress = progress,
                    color = Color(0xFFC9E9CE),
                    modifier = Modifier.fillMaxSize()
                )
                fileBean.label?.let {
                    Text(
                        text = it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier
                    )
                }
            }
            Text(
                text = fileBean.getSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = Color(0xFF444444),
                modifier = Modifier
                    .padding(
                        paddingValues = PaddingValues(
                            start = 4.dp,
                        )
                    )
                    .align(Alignment.CenterVertically)
            )
            Icon(
                imageVector = Icons.Default.MoreVert, contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable(onClick = {
                        onOptClick(index)
                    })
                    .padding(paddingValues = PaddingValues(start = 2.dp))
            )
        }
    }
}
