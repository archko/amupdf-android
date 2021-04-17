package cn.archko.pdf.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
fun BookProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.secondary.copy(alpha = 0.3f),
) {
    Canvas(
        modifier.focusable()
    ) {
        drawLinearIndicator(color, progress)
    }
}

private fun DrawScope.drawLinearIndicator(
    color: Color,
    progress: Float,
) {
    val width = size.width
    val height = size.height
    val w = width * progress / 100F
    drawRect(color, Offset.Zero, Size(width = w, height = height))
}
