import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.paging.LoadResult
import cn.archko.pdf.paging.itemsIndexed

@Composable
fun TextViewer(
    result: LoadResult<Any, ReflowBean>,
    onClick: (pos: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = result.list!!
    val listState = rememberLazyListState(0)

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        item {
            HeaderFooterItem(
            )
        }
        itemsIndexed(list) { index, reflowBean ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            reflowBean?.let {
                TextItem(
                    reflowBean = reflowBean,
                    onClick = onClick,
                    index = index,
                )
            }
        }
        item {
            HeaderFooterItem(
            )
        }
    }
}

@Composable
fun TextItem(
    reflowBean: ReflowBean,
    onClick: (pos: Int) -> Unit,
    index: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .clickable(
                onClick = { onClick(index) },
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                })
    ) {
        reflowBean.data?.let {
            Text(
                text = it,
                overflow = TextOverflow.Visible,
                fontSize = 17.sp,
            )
        }
    }
}

@Composable
private fun HeaderFooterItem(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp)
    ) {

    }
}