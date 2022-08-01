import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PointF
import android.text.Html
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.adapters.ReflowTextViewHolder
import cn.archko.pdf.common.ImageWorker.DecodeParam
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.ParseTextMain
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.paging.itemsIndexed
import cn.archko.pdf.ui.home.PdfImageDecoder
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.BaseMenu
import cn.archko.pdf.widgets.CakeView
import io.iamjosephmj.flinger.bahaviours.StockFlingBehaviours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }
    val screenWidth = with(LocalDensity.current) {
        configuration.screenWidthDp.dp.toPx()
    }

    val showMenu = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (showMenu.value) {
                            showMenu.value = false
                        }
                    },
                    onDoubleTap = {
                        showMenu.value = !showMenu.value
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
        Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                flingBehavior = StockFlingBehaviours.smoothScroll(),
                modifier = modifier
            ) {
                itemsIndexed(list) { index, aPage ->
                    if (index > 0) {
                        Divider(thickness = 1.dp)
                    }
                    if (pdfViewModel.bookProgress?.reflow == 0) {
                        aPage?.let {
                            ImageItem(
                                mupdfDocument = mupdfDocument,
                                width = width,
                                height = height,
                                aPage = aPage
                            )
                        }
                    } else {
                        aPage?.let {
                            ReflowItem(
                                mupdfDocument = mupdfDocument,
                                width = width,
                                height = height,
                                aPage = aPage
                            )
                        }
                    }
                }
            }

            if (showMenu.value) {
                val menus = arrayListOf<BaseMenu>()

                val color = Color(0xff1d84fb).toArgb()
                addMenu("设置", color, menus)
                addMenu("重排", color, menus)
                addMenu("切割", color, menus)
                addMenu("字体", color, menus)
                addMenu("大纲", color, menus)
                addMenu("退出", color, menus)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                ) {
                    //draw(menus)
                    val cakeView = remember {
                        CakeView(context)
                    }
                    //cakeView.setTextColor(Color(0xffffffff).toArgb())
                    cakeView.setCakeData(menus)
                    cakeView.viewOnclickListener = object : CakeView.ViewOnclickListener {
                        override fun onViewClick(v: View?, position: Int) {

                        }

                        override fun onViewCenterClick() {

                        }
                    }
                    AndroidView(
                        { cakeView },
                        modifier = Modifier
                            .padding(28.dp)
                            .width(220.dp)
                            .height(220.dp)
                    ) {
                    }
                }
            }
        }
    }
}

@Composable
fun draw(menus: ArrayList<BaseMenu>) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(100.dp, 100.dp, 0.dp, 0.dp)
    ) {
        val state = remember {
            mutableStateOf(0f)
        }
        /*LaunchedEffect(state.value) {
            state.value += 60f
            if (state.value >= 360f) {
                state.value = 360f
            }
            withContext(Dispatchers.IO) {
                Thread.sleep(50)
            }
        }*/
        var angle = state.value
        val textPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 40f
            color = Color.White.toArgb()
        }
        /*Canvas(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp)
                .background(Color.Yellow)
        ) {
            val canvasWidth = size.width  // 画布的宽
            val canvasHeight = size.height  // 画布的高
            val stroke = 50f
            val radius = canvasWidth / 2
            drawCircle(
                color = Color.Red, // 颜色
                center = Offset(x = canvasWidth / 2, y = canvasHeight / 2), // 圆点
                radius = radius  // 半径
            )
            drawLine(
                start = Offset(x = 0f, y = canvasHeight), // 起点
                end = Offset(x = canvasWidth, y = 0f), // 终点
                color = Color.Blue,  // 颜色
                strokeWidth = 2f
            )
            drawLine(
                start = Offset(x = canvasWidth/2, y = canvasHeight), // 起点
                end = Offset(x = canvasWidth/2, y = canvasHeight/2), // 终点
                color = Color.Yellow,  // 颜色
                strokeWidth = 2f
            )
            
        }*/
        Canvas(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp)
                //.background(Color.LightGray)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                        },
                        onDoubleTap = {
                        },
                        onTap = {
                            Logcat.d("onTap:$it")
                        }
                    )
                }
        ) {
            val canvasWidth = size.width  // 画布的宽
            val canvasHeight = size.height  // 画布的高
            val stroke = 130f
            val radius = canvasWidth - stroke
            drawArc(
                Color.Red,
                0f, // 开始度数
                360f, // 结束度数
                useCenter = false, // 指示圆弧是否闭合边界中心的标志
                // 偏移量
                topLeft = Offset(stroke / 2, stroke / 2),
                // 大小
                size = Size(radius, radius),
                // 样式
                style = Stroke(width = stroke),
            )

            for (i in 0 until 6) {
                val px =
                    ((canvasWidth - stroke / 2) / 2 + radius / 2 * cos(Math.toRadians(angle.toDouble()))).toFloat()
                val py =
                    (canvasHeight / 2 + radius / 2 * sin(Math.toRadians(angle.toDouble()))).toFloat()

                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        "${menus[i].content}",
                        px,
                        py,
                        textPaint
                    )
                }
                angle += 60f
            }

            /*angle = 100f
            val startX =
                ((radius) / 2 + (radius - stroke / 2) * cos(Math.toRadians(angle.toDouble()))).toFloat()
            val startY =
                ((radius) / 2 + (radius - stroke / 2) * sin(Math.toRadians(angle.toDouble()))).toFloat()
            val stopX =
                ((radius) / 2 + (radius + stroke / 2) * cos(Math.toRadians(angle.toDouble()))).toFloat()
            val stopY =
                ((radius) / 2 + (radius + stroke / 2) * sin(Math.toRadians(angle.toDouble()))).toFloat()
            drawLine(
                start = Offset(x = radius / 2 + stroke / 2, y = radius / 2 + stroke / 2), // 起点
                end = Offset(x = stopX, y = stopY), // 终点
                color = Color.Green,  // 颜色
                strokeWidth = 10f
            )*/
        }
    }
}

private fun getLinePointFs(
    angle: Float,
    size: Size,
    radius: Float,
    strokeWidth: Float
): Array<PointF> {
    val stopX =
        (size.center.x / 2 + (radius + strokeWidth / 2) * cos(Math.toRadians(angle.toDouble()))).toFloat()
    val stopY =
        (size.center.y / 2 + (radius + strokeWidth / 2) * sin(Math.toRadians(angle.toDouble()))).toFloat()
    val startX =
        (size.center.x / 2 + (radius - strokeWidth / 2) * cos(Math.toRadians(angle.toDouble()))).toFloat()
    val startY =
        (size.center.y / 2 + (radius - strokeWidth / 2) * sin(Math.toRadians(angle.toDouble()))).toFloat()
    val startPoint = PointF(startX, startY)
    val stopPoint = PointF(stopX, stopY)
    return arrayOf(startPoint, stopPoint)
}

fun addMenu(text: String, color: Int, list: ArrayList<BaseMenu>) {
    val menu = BaseMenu()
    menu.content = text
    menu.color = color
    menu.percent = 60f
    list.add(menu)
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
    text: String = "Decoding",
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(40.dp))
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

@Composable
fun ReflowItem(
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
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp

        //在DisposableEffect中使用flow异步加载
        val reflowState: MutableState<List<ReflowBean>?> = remember { mutableStateOf(null) }
        AsyncDecodeTextPage(aPage, mupdfDocument, reflowState)

        val reflowBeans = reflowState.value
        if (reflowBeans != null) {
            Column {
                for (reflowBean in reflowBeans) {
                    reflowBean.data?.let {
                        if (reflowBean.type == ReflowBean.TYPE_STRING) {
                            Text(
                                Html.fromHtml(it).toString(),
                                style = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp, 10.dp, 24.dp, 10.dp)
                            )
                        } else {
                            val bean = ReflowTextViewHolder.decodeBitmap(
                                reflowBean.data,
                                Utils.getScale(),
                                height,
                                width,
                                context
                            )
                            val bh = with(LocalDensity.current) {
                                bean.height.toDp()
                            }
                            Image(
                                bitmap = bean.bitmap.asImageBitmap(),
                                contentDescription = "",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(bh)
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                LoadingView("Decoding Page:${aPage.index + 1}")
            }
        }
    }
}

@Composable
private fun AsyncDecodeTextPage(
    aPage: APage,
    mupdfDocument: MupdfDocument,
    imageState: MutableState<List<ReflowBean>?>
) {
    DisposableEffect(aPage.index) {
        val scope =
            CoroutineScope(SupervisorJob() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
        scope.launch {
            snapshotFlow {
                val result = mupdfDocument.loadPage(aPage.index)
                    ?.textAsText("preserve-whitespace,inhibit-spaces,preserve-images")
                if (null != result) {
                    ParseTextMain.instance.parseAsList(result, aPage.index)
                } else {
                    return@snapshotFlow null
                }
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