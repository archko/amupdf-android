package cn.archko.sunflower.ui.home

import GankBean
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import cn.archko.sunflower.Destination
import cn.archko.sunflower.ui.components.JetsnackSurface
import cn.archko.sunflower.ui.components.NetworkImage
import cn.archko.sunflower.ui.theme.JetsnackTheme
import cn.archko.sunflower.ui.utils.JsonUtils

@Composable
fun GankItem(
    gankBean: GankBean,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    index: Int = 0,
    scroll: ScrollState
) {
    GankItem(
        gankBean,
        navController,
        index,
        scroll.value,
        modifier = modifier
    )
}

@Composable
private fun GankItem(
    gankBean: GankBean,
    navController: NavHostController,
    index: Int,
    scroll: Int,
    modifier: Modifier = Modifier
) {
    Log.d("", "$index, $scroll")

    Box(
        modifier = modifier
            .height(480.dp)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        ItemSnackImage(
            imageUrl = gankBean.images[0],
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .clickable(onClick = {
                    navController.navigate(
                        /*Screen.GankDetail(
                            JsonUtils.toJson(
                                gankBean
                            )
                        )*/
                        Destination.GANKDETAIL + "/" + JsonUtils.toJson(gankBean)
                    )
                })
                .fillMaxSize()
                .align(Alignment.BottomStart)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row() {
                Text(
                    text = gankBean.author,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.h6,
                    color = Color.Magenta,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = gankBean.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.h6,
                    color = JetsnackTheme.colors.textInteractive,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = gankBean.desc,
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
        NetworkImage(
            url = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

