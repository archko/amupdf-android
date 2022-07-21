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
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.BitmapPool
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.entity.APage
import cn.archko.pdf.mupdf.MupdfDocument
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI
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
 * 查看pdf信息时的缩略图
 */
class AsyncPageImagePainter internal constructor(
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
                decode(request)
            }.flowOn(AppExecutors.instance.diskIO().asCoroutineDispatcher())
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

    private fun decode(decodeParam: ImageWorker.DecodeParam): Bitmap? {
        try {
            //long start = SystemClock.uptimeMillis();
            val page: Page = decodeParam.document.loadPage(decodeParam.pageSize.index)
            var leftBound = 0
            var topBound = 0
            val pageSize: APage = decodeParam.pageSize
            var pageW = pageSize.zoomPoint.x
            var pageH = pageSize.zoomPoint.y
            val ctm = Matrix(MupdfDocument.ZOOM)
            val bbox = RectI(page.bounds.transform(ctm))
            val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
            val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
            ctm.scale(xscale, yscale)
            if (pageSize.getTargetWidth() > 0) {
                pageW = pageSize.getTargetWidth()
            }
            if (decodeParam.crop) {
                val arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound)
                leftBound = arr[0].toInt()
                topBound = arr[1].toInt()
                pageH = arr[2].toInt()
                val cropScale = arr[3]
                pageSize.setCropHeight(pageH)
                pageSize.setCropWidth(pageW)
                //RectF cropRectf = new RectF(leftBound, topBound, leftBound + pageW, topBound + pageH);
                //pageSize.setCropBounds(cropRectf, cropScale);
            }
            if (Logcat.loggable) {
                d(
                    String.format(
                        "decode bitmap: %s-%s,page:%s-%s,xOrigin:%s, bound(left-top):%s-%s, page:%s",
                        pageW, pageH, pageSize.zoomPoint.x, pageSize.zoomPoint.y,
                        decodeParam.xOrigin, leftBound, topBound, pageSize
                    )
                )
            }
            val bitmap = BitmapPool.getInstance()
                .acquire(pageW, pageH) //Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
            MupdfDocument.render(page, ctm, bitmap, decodeParam.xOrigin, leftBound, topBound)
            page.destroy()
            //Logcat.d(TAG, "decode:" + (SystemClock.uptimeMillis() - start));
            BitmapCache.getInstance().addBitmap(decodeParam.key, bitmap)
            return bitmap
        } catch (e: Exception) {
            if (Logcat.loggable) {
                d(
                    String.format(
                        "decode bitmap error:countPages-page:%s-%s",
                        decodeParam.document.countPages(),
                        decodeParam
                    )
                )
            }
            e.printStackTrace()
        }
        return null
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
