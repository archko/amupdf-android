import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.paging.itemsIndexed
import cn.archko.pdf.viewmodel.PDFViewModel
import io.iamjosephmj.flinger.bahaviours.StockFlingBehaviours
import kotlinx.coroutines.launch

@Composable
fun TextViewer(
    result: LoadResult<Any, ReflowBean>,
    pdfViewModel: PDFViewModel,
    styleHelper: StyleHelper?,
    onClick: (pos: Int) -> Unit,
    height: Int,
    margin: Int,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    modifier: Modifier = Modifier,
) {
    val list = result.list!!
    val listState = rememberLazyListState(0)
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }
    val screenWidth = with(LocalDensity.current) {
        configuration.screenWidthDp.dp.toPx()
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                    },
                    onDoubleTap = {
                    },
                    onTap = {
                        scrollOnTab(
                            coroutineScope,
                            listState,
                            it,
                            configuration,
                            screenHeight,
                            screenWidth,
                            margin,
                            onClick
                        )
                    }
                )
            }) {
        DisposableEffect(result) {
            coroutineScope.launch {
                listState.scrollToItem(pdfViewModel.getCurrentPage())
            }
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    pdfViewModel.bookProgress?.run {
                        autoCrop = 0
                        val position = listState.firstVisibleItemIndex
                        pdfViewModel.saveBookProgress(
                            pdfViewModel.pdfPath,
                            pdfViewModel.countPages(),
                            position,
                            pdfViewModel.bookProgress!!.zoomLevel,
                            -1,
                            listState.firstVisibleItemScrollOffset
                        )
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        LazyColumn(
            state = listState,
            flingBehavior = StockFlingBehaviours.smoothScroll(),
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
                    TextItem(aPage = reflowBean, styleHelper = styleHelper)
                }
            }
            item {
                HeaderFooterItem()
            }
        }
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
private fun TextItem(
    aPage: ReflowBean,
    styleHelper: StyleHelper?,
    modifier: Modifier = Modifier
) {
    val fSize = TextUnit(styleHelper!!.styleBean!!.textSize, TextUnitType.Sp)
    var fontSize by remember { mutableStateOf(fSize) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        offset += offsetChange
        fontSize *= zoomChange
        if (fontSize > 24.sp) {
            fontSize = 24.sp
        }
        styleHelper.styleBean!!.textSize = fontSize.value
        Logcat.d("scale:$scale, offset:$offset, zoom:$zoomChange, fontSize:$fontSize, value:$fontSize.value")
    }

    aPage.data?.let {
        Text(
            text = it,
            overflow = TextOverflow.Visible,
            fontSize = fontSize,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .transformable(state = state, lockRotationOnZoomPan = false)
        )
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