package cn.archko.sunflower.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cn.archko.sunflower.paging.ResourceState
import cn.archko.sunflower.R
import cn.archko.sunflower.ui.theme.JetsnackTheme

@Composable
fun LoadingFooter(
    resourceState: ResourceState = ResourceState(initial = ResourceState.LOADING),
    text: String? = "Loading",
    total: Int = 0,
    onClick: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) {
    } else {
        var txt = text
        txt?.let {
            if (resourceState.value == ResourceState.ERROR) {
                txt = "Error"
            } else if (resourceState.value == ResourceState.FINISHED || resourceState.value == ResourceState.IDLE) {
                if (total == 0) {
                    txt = "NO MORE"
                } else {
                    txt = "IDLE"
                }
            }
        }
        Row(
            modifier = modifier
                .fillMaxSize()
                .clickable { onClick() }
                .padding(
                    paddingValues = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 10.dp
                    )
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.empty_state_search),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(48.dp)
                    .padding(paddingValues = PaddingValues(end = 8.dp))
                    .align(Alignment.CenterVertically)
            )
            txt?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.h5,
                    color = JetsnackTheme.colors.textSecondary,
                    modifier = modifier
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
