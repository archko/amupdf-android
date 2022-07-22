import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.paging.itemsIndexed
import cn.archko.pdf.ui.home.AsyncPageImagePainter
import io.iamjosephmj.flinger.bahaviours.StockFlingBehaviours
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

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                    },
                    onDoubleTap = {
                    },
                    onTap = {
                        coroutineScope.launch {
                            val h = screenHeight.toPx()
                            val top = h / 4
                            val bottom = h * 3 / 4
                            val y = it.y   //点击的位置
                            var scrollY = 0
                            if (y < top) {
                                scrollY -= h.toInt()
                                listState.scrollBy((scrollY + margin).toFloat())
                            } else if (y > bottom) {
                                scrollY += h.toInt()
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
            flingBehavior = StockFlingBehaviours.smoothScroll(),
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
        modifier = modifier.fillMaxWidth()
    ) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp
        val h: Float =
            aPage.effectivePagesHeight.toFloat() * screenWidth.value / aPage.effectivePagesWidth
        val theDp = h.dp
        val w = with(LocalDensity.current) {
            screenWidth.toPx()
        }
        if (aPage.getTargetWidth() != w.toInt()) {
            aPage.setTargetWidth(w.toInt())
        }

        Logcat.d("h:$h, theDp:$theDp,w:$w, width:$width, height:$height, screenHeight:$screenHeight, screenWidth:$screenWidth, aPage.effectivePagesWidth:${aPage.effectivePagesWidth}, aPage:$aPage")
        Image(
            painter = remember {
                AsyncPageImagePainter(
                    ImageWorker.DecodeParam(
                        aPage.toString(),
                        false,
                        0,
                        aPage,
                        mupdfDocument.document,
                    )
                )
            },
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(theDp)
        )
    }
}
 