package cn.archko.sunflower.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.coil.CoilImage

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    url: String,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color? = Color(0x1f000000),
    errorColor: Color? = Color(0x1f000000),
) {
    CoilImage(
        data = url,
        modifier = modifier,//aspectRatio(17f / 12f),
        contentDescription = contentDescription,
        contentScale = contentScale,
        loading = {
            if (placeholderColor != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor)
                )
            }
        },
        error = {
            if (errorColor != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(errorColor)
                )
            }
        }
    )
}

@Composable
fun DrawableResImage(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color? = Color(0x1f000000),
    errorColor: Color? = Color(0x1f000000),
) {
    CoilImage(
        data = drawableRes,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        loading = {
            if (placeholderColor != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor)
                )
            }
        },
        error = {
            if (errorColor != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(errorColor)
                )
            }
        }
    )
}