package cn.archko.pdf.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.viewmodel.PDFViewModel
import kotlinx.coroutines.launch

@Composable
fun OutlineDialog(
    outlineDialog: MutableState<Boolean>,
    viewModel: PDFViewModel,
    currentPage: MutableState<Int>,
    onSelect: (OutlineItem) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val outlines by viewModel.outlineFlow.collectAsState()
    val listState = rememberLazyListState(0)
    Logcat.d("OutlineDialog list:${outlineDialog.value}, size:${outlines.list?.size}, currentPage:${currentPage.value}")

    if (outlines.list?.size == 0) {
        coroutineScope.launch {
            viewModel.loadOutline()
        }
    }

    if (outlineDialog.value) {
        Dialog(
            onDismissRequest = {
                outlineDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier,
            ) {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Row {
                        Text(
                            "Outline, Page:${currentPage.value}",
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Divider(thickness = 1.dp)
                    LazyColumn {
                        itemsIndexed(outlines.list!!) { index, outlineItem ->
                            if (index > 0) {
                                Divider(thickness = 0.5.dp)
                            }
                            DialogItem(
                                outlineItem = outlineItem,
                                onSelect = onSelect,
                            )
                        }
                    }
                }
            }
        }
        DisposableEffect(currentPage.value) {
            if (null != outlines.list) {
                var found = -1
                for (i in outlines.list!!.indices) {
                    val item = outlines.list!![i]
                    if (found < 0 && item.page >= currentPage.value) {
                        found = i
                    }
                }
                Logcat.d("found:${found}, listState:$listState")
                if (found >= 0) {
                    coroutineScope.launch {
                        listState.scrollToItem(found, 0)
                    }
                }
            }
            onDispose {
            }
        }
    }
}

@Composable
private fun DialogItem(
    outlineItem: OutlineItem,
    onSelect: (OutlineItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clickable(onClick = { onSelect(outlineItem) })
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = outlineItem.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f, true)
        )
        Text(
            text = outlineItem.page.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
        )
    }
}
