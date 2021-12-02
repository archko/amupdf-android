package cn.archko.pdf.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.paging.State

@Composable
fun LoadingFooter(
    state: State = State.LOADING,
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
            txt = if (state == State.ERROR) {
                "Error"
            } else if (state == State.FINISHED || state == State.INIT) {
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
            if (state == State.LOADING) {
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
                    style = TextStyle(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
                    modifier = modifier
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}
