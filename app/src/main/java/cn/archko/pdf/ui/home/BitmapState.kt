package cn.archko.pdf.ui.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.painter.Painter

sealed class BitmapState {

    abstract val painter: Painter?

    object Empty : BitmapState() {
        override val painter: Painter? get() = null
    }

    data class Loading(
        override val painter: Painter?,
    ) : BitmapState()

    data class Success(
        override val painter: Painter?,
        val result: Bitmap,
    ) : BitmapState()

    data class Error(
        override val painter: Painter?,
        val result: String,
    ) : BitmapState()
}