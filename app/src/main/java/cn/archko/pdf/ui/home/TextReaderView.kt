import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.paging.itemsIndexed
import kotlinx.coroutines.launch

@Composable
fun TextViewer(
    result: LoadResult<Any, ReflowBean>,
    onClick: (pos: Int) -> Unit,
    height: Int,
    margin: Int,
    modifier: Modifier = Modifier,
) {
    val list = result.list!!
    val listState = rememberLazyListState(0)
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                },
                onDoubleTap = {
                },
                onTap = {
                    coroutineScope.launch {
                        val top = height / 4
                        val bottom = height * 3 / 4
                        val y = it.y   //点击的位置
                        var scrollY = 0
                        if (y < top) {
                            scrollY -= height
                            listState.scrollBy((scrollY + margin).toFloat())
                        } else if (y > bottom) {
                            scrollY += height
                            listState.scrollBy((scrollY - margin).toFloat())
                        } else {
                            onClick(listState.firstVisibleItemIndex)
                        }
                        //Logcat.d("scroll:$top, y:$y, margin:$margin, scrollY:$scrollY, firstVisibleItemIndex:${listState.firstVisibleItemIndex}")
                    }
                }
            )
        }) {
        LazyColumn(
            state = listState,
            modifier = modifier
        ) {
            item {
                HeaderFooterItem()
            }
            itemsIndexed(list) { index, reflowBean ->
                if (index > 0) {
                    Divider(thickness = 1.dp)
                }
                reflowBean?.let {
                    TextItem(aPage = reflowBean)
                }
            }
            item {
                HeaderFooterItem()
            }
        }
    }
}

@Composable
private fun TextItem(
    aPage: ReflowBean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        aPage.data?.let {
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