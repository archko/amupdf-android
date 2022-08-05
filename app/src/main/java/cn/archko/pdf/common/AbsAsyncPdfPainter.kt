package cn.archko.pdf.common

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
import cn.archko.pdf.AppExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

/**
 * 加载pdf的painter,解码部分空着
 */
abstract class AbsAsyncPdfPainter internal constructor(
    request: ImageWorker.DecodeParam,
) : Painter(), RememberObserver {

    private var rememberScope: CoroutineScope? = null
    private val drawSize = MutableStateFlow(Size.Zero)

    private var painter: Painter? by mutableStateOf(null)
    private var _bitmapState: BitmapState = BitmapState.Empty
        set(value) {
            field = value
            bitmapState = value
        }
    private var _painter: Painter? = null
        set(value) {
            field = value
            painter = value
        }

    internal var transform = DefaultTransform
    internal var onState: ((BitmapState) -> Unit)? = null
    var bitmapState: BitmapState by mutableStateOf(BitmapState.Empty)
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
                decode(request)
            }.flowOn(AppExecutors.instance.diskIO().asCoroutineDispatcher())
                .mapLatest { bitmap ->
                    if (null != bitmap) {
                        BitmapState.Success(
                            BitmapPainter(
                                bitmap.asImageBitmap(),
                            ), bitmap
                        )
                    } else {
                        BitmapState.Error(painter, "null")
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

    private fun updateState(input: BitmapState) {
        val previous = _bitmapState
        val current = transform(input)
        //Logcat.d("updateState.current:${current}, $_state, $_painter")
        _bitmapState = current
        _painter = current.painter

        // Manually forget and remember the old/new painters if we're already remembered.
        if (rememberScope != null && previous.painter !== current.painter) {
            (previous.painter as? RememberObserver)?.onForgotten()
            (current.painter as? RememberObserver)?.onRemembered()
        }

        onState?.invoke(current)
    }

    abstract fun decode(decodeParam: ImageWorker.DecodeParam): Bitmap?

    companion object {
        val DefaultTransform: (BitmapState) -> BitmapState = { it }
    }
}
