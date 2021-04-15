package cn.archko.sunflower.ui.home

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.archko.sunflower.model.ACategory
import cn.archko.sunflower.ui.components.JetsnackSurface
import cn.archko.sunflower.ui.theme.JetsnackTheme
import cn.archko.sunflower.ui.utils.Screen
import com.google.accompanist.coil.CoilImage

@Composable
fun VideoItem(
    aCategory: ACategory,
    navigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    scroll: ScrollState
) {
    VideoItem(
        aCategory,
        navigateTo,
        index,
        scroll.value,
        modifier = modifier
    )
}

@Composable
private fun VideoItem(
    aCategory: ACategory,
    navigateTo: (Screen) -> Unit,
    index: Int,
    scroll: Int,
    modifier: Modifier = Modifier
) {
    Log.d("", "$index, $scroll")

    Box(
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
    ) {
        ItemSnackImage(
            imageUrl = aCategory.headerImage,
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .clickable(onClick = { navigateTo(Screen.VideoDetail(aCategory.id.toString())) })
                .fillMaxSize()
                .align(Alignment.BottomStart)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = aCategory.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6,
                color = JetsnackTheme.colors.textInteractive,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = aCategory.description,
                style = MaterialTheme.typography.body1,
                color = JetsnackTheme.colors.textInteractive,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ItemSnackImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp
) {
    JetsnackSurface(
        color = Color.Transparent,
        elevation = elevation,
        modifier = modifier
    ) {
        CoilImage(
            data = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

