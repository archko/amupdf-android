package cn.archko.pdf.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.paging.ResourceState

@Composable
fun LoadingFooter(
    resourceState: ResourceState = ResourceState(initial = ResourceState.LOADING),
    text: String? = "Loading",
    hasMore: Boolean,
    onClick: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) {
    } else {
        var txt = text
        txt?.let {
            txt = if (resourceState.value == ResourceState.ERROR) {
                "Error"
            } else if (resourceState.value == ResourceState.FINISHED || resourceState.value == ResourceState.IDLE) {
                if (!hasMore) {
                    "NO MORE"
                } else {
                    "IDLE"
                }
            } else {
                "Loading"
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
            if (resourceState.value == ResourceState.FINISHED) {
                CircularProgressIndicator(
                    modifier = modifier
                        .size(36.dp)
                        .padding(paddingValues = PaddingValues(end = 8.dp))
                        .align(Alignment.CenterVertically)
                )
            } else {
                Spacer(
                    modifier = modifier
                        .size(36.dp)
                        .padding(paddingValues = PaddingValues(end = 8.dp))
                        .align(Alignment.CenterVertically)
                )
            }
            txt?.let {
                Text(
                    text = it,
                    style = TextStyle(fontSize = 18.sp),
                    color = MaterialTheme.colors.onSecondary,
                    modifier = modifier
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
