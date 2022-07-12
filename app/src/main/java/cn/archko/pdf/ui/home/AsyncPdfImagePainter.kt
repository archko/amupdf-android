package cn.archko.pdf.ui.home

import android.graphics.Bitmap
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import cn.archko.pdf.App.Companion.instance
import cn.archko.pdf.common.ImageLoader
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.utils.BitmapUtils
import cn.archko.pdf.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.io.File

/**
 * 查看pdf信息时的缩略图
 */
class AsyncPdfImagePainter internal constructor(
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
                var bitmap: Bitmap? = null
                try {
                    val thumb =
                        FileUtils.getDiskCacheDir(instance, FileUtils.getRealPath(request.key))
                    bitmap = ImageLoader.decodeFromFile(thumb)
                    if (null == bitmap) {
                        val file = File(request.key)
                        if (file.exists()) {
                            bitmap = ImageLoader.decodeFromPDF(
                                request.key,
                                request.pageNum,
                                request.zoom,
                                request.screenWidth
                            )
                            if (bitmap != null) {
                                BitmapUtils.saveBitmapToFile(bitmap, thumb)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

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
