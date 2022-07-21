import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.paging.itemsIndexed
import cn.archko.pdf.ui.home.AsyncPageImagePainter
import kotlinx.coroutines.launch

@Composable
fun ImageViewer(
    result: LoadResult<Any, APage>,
    mupdfDocument: MupdfDocument,
    onClick: (pos: Int) -> Unit,
    width: Int,
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
            itemsIndexed(list) { index, aPage ->
                if (index > 0) {
                    Divider(thickness = 1.dp)
                }
                aPage?.let {
                    ImageItem(
                        mupdfDocument = mupdfDocument,
                        width = width,
                        height = height,
                        aPage = aPage
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageItem(
    mupdfDocument: MupdfDocument,
    width: Int,
    height: Int,
    aPage: APage,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Image(
            painter = remember {
                AsyncPageImagePainter(
                    ImageWorker.DecodeParam(
                        aPage.toString(),
                        null,
                        false,
                        0,
                        aPage,
                        mupdfDocument.document,
                        null
                    )
                )
            },
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
 