package cn.archko.pdf.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.utils.DateUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun FileInfoDialog(
    showInfoDialog: MutableState<Boolean>,
    fileBean: FileBean?,
    menuOpt: (MenuItemType, FileBean) -> Unit,
) {
    val context = LocalContext.current
    if (showInfoDialog.value && fileBean != null) {
        val file = fileBean.file
        Dialog(
            onDismissRequest = {
                showInfoDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier,
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Row {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(id = R.string.menu_info),
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Divider(thickness = 1.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.label_location),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.Top)
                                .padding(2.dp)
                        )
                        Text(
                            FileUtils.getDir(file),
                            style = TextStyle(fontSize = 14.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Top)
                        )
                    }
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.label_filename),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.Top)
                                .padding(2.dp)
                        )
                        fileBean.file?.name?.let {
                            Text(
                                it,
                                style = TextStyle(fontSize = 14.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.Top)
                            )
                        }
                    }
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.label_filesize),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            Utils.getFileSize(fileBean.fileSize),
                            style = TextStyle(fontSize = 14.sp),
                        )
                    }

                    val progress = fileBean.bookProgress
                    progress?.let {
                        if (progress.pageCount > 0) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(id = R.string.label_pagecount),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                )
                                Text(
                                    "${progress.page}/${progress.pageCount}",
                                    style = TextStyle(fontSize = 14.sp),
                                )
                            }
                        }

                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(id = R.string.label_read_count),
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            )
                            Text(
                                progress.readTimes.toString(),
                                style = TextStyle(fontSize = 14.sp),
                            )
                        }

                        if (progress.pageCount > 0) {
                            val text = DateUtils.formatTime(
                                progress.firstTimestampe,
                                DateUtils.TIME_FORMAT_TWO
                            )
                            val percent = progress.page * 100f / progress.pageCount
                            val b = BigDecimal(percent.toDouble())
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(id = R.string.label_read),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                )
                                Text(
                                    "$text      ${
                                        b.setScale(2, BigDecimal.ROUND_HALF_UP).toFloat()
                                    }%",
                                    style = TextStyle(fontSize = 14.sp),
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showInfoDialog.value = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text(
                                "Cancel",
                            )
                        }

                        Button(
                            onClick = {
                                showInfoDialog.value = false
                                menuOpt(MenuItemType.ViewBookWithAMupdf, fileBean)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text(
                                "Read",
                            )
                        }
                    }

                    fileBean.file?.path?.let {
                        if (fileBean.file!!.exists()) {
                            /*val androidImageView = remember {
                                ImageView(context).apply {
                                    ImageLoader.getInstance().loadImage(
                                        fileBean.file?.path,
                                        0,
                                        1.0f,
                                        App.instance!!.screenWidth,
                                        this
                                    )
                                }
                            }

                            AndroidView(
                                { androidImageView },
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .width(120.dp)
                                    .height(160.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                            }*/
                            Image(
                                painter = remember {
                                    AsyncImagePainter(
                                        ImageWorker.DecodeParam(
                                            fileBean.file!!.path,
                                            0,
                                            1.0f,
                                            App.instance!!.screenWidth,
                                            null
                                        )
                                    )
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .width(120.dp)
                                    .height(160.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }

}

class AsyncImagePainter internal constructor(
    request: ImageWorker.DecodeParam,
) : Painter(), RememberObserver {

    private var rememberScope: CoroutineScope? = null
    private val drawSize = MutableStateFlow(Size.Zero)

    private var painter: Painter? by mutableStateOf(null)
    private var _state: State = State.Empty
        set(value) {
            field = value
            state = value
        }
    private var _painter: Painter? = null
        set(value) {
            field = value
            painter = value
        }

    internal var transform = DefaultTransform
    internal var onState: ((State) -> Unit)? = null
    var state: State by mutableStateOf(State.Empty)
        private set

    var request: ImageWorker.DecodeParam by mutableStateOf(request)
        internal set

    override val intrinsicSize: Size
        get() = painter?.intrinsicSize ?: Size.Unspecified

    override fun DrawScope.onDraw() {
        drawSize.value = size

        painter?.apply { draw(size) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onRemembered() {
        if (rememberScope != null) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        rememberScope = scope

        (_painter as? RememberObserver)?.onRemembered()

        scope.launch {
            snapshotFlow {
                val bitmap: Bitmap? = ImageLoader.decodeFromPDF(
                    request.key,
                    request.pageNum,
                    request.zoom,
                    request.screenWidth
                )
                bitmap
            }.flowOn(Dispatchers.IO)
                .mapLatest { bitmap ->
                    if (null != bitmap) {
                        State.Success(
                            BitmapPainter(
                                bitmap.asImageBitmap(),
                            ), bitmap
                        )
                    } else {
                        State.Error(painter, "null")
                    }
                }
                .collect(::updateState)
        }
    }

    override fun onForgotten() {
        clear()
        (_painter as? RememberObserver)?.onForgotten()
    }

    override fun onAbandoned() {
        clear()
        (_painter as? RememberObserver)?.onAbandoned()
    }

    private fun clear() {
        rememberScope?.cancel()
        rememberScope = null
    }

    private fun updateState(input: State) {
        val previous = _state
        val current = transform(input)
        //Logcat.d("updateState.current:${current}, $_state, $_painter")
        _state = current
        _painter = current.painter

        // Manually forget and remember the old/new painters if we're already remembered.
        if (rememberScope != null && previous.painter !== current.painter) {
            (previous.painter as? RememberObserver)?.onForgotten()
            (current.painter as? RememberObserver)?.onRemembered()
        }

        onState?.invoke(current)
    }

    sealed class State {

        abstract val painter: Painter?

        object Empty : State() {
            override val painter: Painter? get() = null
        }

        data class Loading(
            override val painter: Painter?,
        ) : State()

        data class Success(
            override val painter: Painter?,
            val result: Bitmap,
        ) : State()

        data class Error(
            override val painter: Painter?,
            val result: String,
        ) : State()
    }

    companion object {
        val DefaultTransform: (State) -> State = { it }
    }
}
