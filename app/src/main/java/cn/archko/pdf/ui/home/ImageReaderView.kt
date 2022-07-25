import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.ImageWorker.DecodeParam
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.paging.itemsIndexed
import cn.archko.pdf.ui.home.PdfImageDecoder
import cn.archko.pdf.viewmodel.PDFViewModel
import io.iamjosephmj.flinger.bahaviours.StockFlingBehaviours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@Composable
fun ImageViewer(
    result: LoadResult<Any, APage>,
    pdfViewModel: PDFViewModel,
    mupdfDocument: MupdfDocument,
    onClick: (pos: Int) -> Unit,
    width: Int,
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

fun scrollOnTab(
    coroutineScope: CoroutineScope,
    listState: LazyListState,
    offset: Offset,
    configuration: Configuration,
    screenHeight: Float,
    screenWidth: Float,
    margin: Int,
    onClick: (pos: Int) -> Unit
) {
    coroutineScope.launch {
        val h = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (screenWidth < screenHeight) {
                screenHeight
            } else {
                screenWidth
            }
        } else {
            if (screenWidth < screenHeight) {
                screenWidth
            } else {
                screenHeight
            }
        }
        val top = h / 4
        val bottom = h * 3 / 4
        val y = offset.y   //点击的位置
        var scrollY = 0
        if (y < top) {
            scrollY -= h.toInt()
            listState.scrollBy((scrollY - margin).toFloat())
        } else if (y > bottom) {
            scrollY += h.toInt()
            listState.scrollBy((scrollY + margin).toFloat())
        } else {
            onClick(listState.firstVisibleItemIndex)
        }
        Logcat.d("scroll:$top, bottom:$bottom, y:$y,h:$h, screenHeight:$screenHeight, margin:$margin, scrollY:$scrollY, firstVisibleItemIndex:${listState.firstVisibleItemIndex}")
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
        modifier
            .fillMaxWidth()
    ) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp

        val swidth = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (screenWidth < screenHeight) {
                screenWidth
            } else {
                screenHeight
            }
        } else {
            if (screenWidth < screenHeight) {
                screenHeight
            } else {
                screenWidth
            }
        }
        val h: Float =
            aPage.effectivePagesHeight.toFloat() * swidth.value / aPage.effectivePagesWidth
        val theDp = h.dp
        val w = with(LocalDensity.current) {
            swidth.toPx()
        }
        if (aPage.getTargetWidth() != w.toInt()) {
            aPage.setTargetWidth(w.toInt())
        }

        Logcat.d("h:$h, theDp:$theDp,w:$w, width:$width, height:$height, swidth:$swidth screenHeight:$screenHeight, screenWidth:$screenWidth, aPage.effectivePagesWidth:${aPage.effectivePagesWidth}, aPage:$aPage")

        /*
        使用painter
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
        val painter = remember {
            AsyncPageImagePainter(
                DecodeParam(
                    aPage.toString(),
                    false,
                    0,
                    aPage,
                    mupdfDocument.document,
                )
            )
        }

        Image(
            painter = painter,
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(theDp)
        )

        when (painter.bitmapState) {
            is BitmapState.Loading -> {
            }
            is BitmapState.Error -> {
            }
            else -> {

            }
        }*/

        //使用同步加载
        //val imageState = loadPage(aPage, mupdfDocument)

        //在DisposableEffect中使用flow异步加载
        val imageState: MutableState<Bitmap?> = remember { mutableStateOf(null) }
        AsyncDecodePage(aPage, mupdfDocument, imageState)

        if (imageState.value != null) {
            val bitmap = imageState.value
            val bh = with(LocalDensity.current) {
                bitmap!!.height.toDp()
            }
            //Logcat.d("bitmap:${bitmap!!.width}, h:${bitmap.height},bh:$bh, width:$width, height:$height, screenHeight:$screenHeight, screenWidth:$screenWidth, aPage.effectivePagesWidth:${aPage.effectivePagesWidth}, aPage:$aPage")
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bh)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(theDp)
            ) {
                LoadingView("Decoding Page:${aPage.index + 1}")
            }
        }
    }
}

@Composable
private fun LoadingView(
    text: String = "Decoding"
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(12.dp)
        )
        /*CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(20.dp)
        )*/
        Spacer(modifier = Modifier.height(20.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .height(6.dp)
                .align(alignment = Alignment.CenterHorizontally)
        )
    }
}

fun loadPage(
    aPage: APage,
    mupdfDocument: MupdfDocument,
): MutableState<Bitmap?> {
    val decodeParam = DecodeParam(
        aPage.toString(),
        true,
        0,
        aPage,
        mupdfDocument.document,
    )
    val bitmapState: MutableState<Bitmap?> = mutableStateOf(null)
    bitmapState.value = PdfImageDecoder.decode(decodeParam)
    return bitmapState
}

@Composable
private fun AsyncDecodePage(
    aPage: APage,
    mupdfDocument: MupdfDocument,
    imageState: MutableState<Bitmap?>
) {
    DisposableEffect(aPage.getTargetWidth()) {
        val decodeParam = DecodeParam(
            aPage.toString(),
            true,
            0,
            aPage,
            mupdfDocument.document,
        )
        val scope =
            CoroutineScope(SupervisorJob() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
        scope.launch {
            snapshotFlow {
                PdfImageDecoder.decode(decodeParam)
            }.flowOn(AppExecutors.instance.diskIO().asCoroutineDispatcher())
                .collectLatest {
                    imageState.value = it
                }
        }
        onDispose {
            scope.cancel()
        }
    }
}