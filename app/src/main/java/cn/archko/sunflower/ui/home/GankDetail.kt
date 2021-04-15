package cn.archko.sunflower.ui.home

import GankBean
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.archko.sunflower.ui.components.JetsnackSurface
import cn.archko.sunflower.ui.components.NetworkImage
import cn.archko.sunflower.ui.utils.VLog
import cn.archko.sunflower.R
import cn.archko.sunflower.ui.components.Divider
import cn.archko.sunflower.ui.theme.JetsnackTheme
import cn.archko.sunflower.ui.theme.Neutral8
import cn.archko.sunflower.ui.utils.JsonUtils
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding

private val BottomBarHeight = 56.dp
private val HzPadding = Modifier.padding(horizontal = 24.dp)

@Composable
fun GankDetail(
    gankStr: String,
    upPress: () -> Unit
) {
    VLog.d("$gankStr")
    val gank: GankBean = JsonUtils.parseGankBean(gankStr)

    Box(Modifier.fillMaxSize()) {
        val scroll = rememberScrollState(0)
        Column(
            modifier = Modifier.verticalScroll(scroll)
        ) {
            Header(gank)

            Spacer(Modifier.height(16.dp))
            Text(
                text = gank.author,
                style = MaterialTheme.typography.h5,
                color = JetsnackTheme.colors.textHelp,
                modifier = HzPadding
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = gank.title,
                style = MaterialTheme.typography.body1,
                color = JetsnackTheme.colors.textHelp,
                modifier = HzPadding
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = gank.createdAt,
                style = MaterialTheme.typography.overline,
                color = JetsnackTheme.colors.textHelp,
                modifier = HzPadding
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = gank.desc,
                style = MaterialTheme.typography.body1,
                color = JetsnackTheme.colors.textHelp,
                modifier = HzPadding
            )

            Spacer(Modifier.height(16.dp))
            Divider()

            Spacer(
                modifier = Modifier
                    .padding(bottom = BottomBarHeight)
                    .navigationBarsPadding(left = false, right = false)
                    .height(8.dp)
            )
        }
        Up(upPress)
    }

    Up(upPress)
}

@Composable
private fun Header(
    gank: GankBean,
) {
    Box(Modifier.fillMaxSize()) {
        var url = gank.images[0];
        if (gank.images.size == 2) {
            url = gank.images[1]
        }
        BGImage(url, contentDescription = null)
    }
}

@Composable
private fun BGImage(
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
        NetworkImage(
            url = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
private fun Up(upPress: () -> Unit) {
    IconButton(
        onClick = upPress,
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .size(36.dp)
            .background(
                color = Neutral8.copy(alpha = 0.32f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            tint = JetsnackTheme.colors.iconInteractive,
            contentDescription = stringResource(R.string.label_back)
        )
    }
}
