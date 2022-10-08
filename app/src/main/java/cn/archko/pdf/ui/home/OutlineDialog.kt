package cn.archko.pdf.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.SliderWithLabel
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.viewmodel.PDFViewModel
import kotlinx.coroutines.launch

@Composable
fun OutlineMenu(
    outlineDialog: MutableState<Boolean>,
    viewModel: PDFViewModel,
    currentPage: MutableState<Int>,
    onSelect: (OutlineItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val outlines by viewModel.outlineFlow.collectAsState()
    val listState = rememberLazyListState(0)

    if (outlineDialog.value) {
        DisposableEffect(viewModel) {
            coroutineScope.launch {
                viewModel.loadOutline()
            }

            onDispose {
            }
        }
        outlines.list?.run {
            var found = -1
            for (i in 0 until this.size) {
                if (this[i].page >= currentPage.value) {
                    found = i
                    break
                }
            }
            Logcat.d("found:${found}, listState:$listState")
            if (found > 0) {
                coroutineScope.launch {
                    listState.scrollToItem(found, 0)
                }
            }
        }

        Logcat.d("currentPage:${currentPage.value}")
        Column(
            modifier = Modifier
                .clickable(
                    onClick = { },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .background(Color.Transparent)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xCc202020))
            ) {
                var count = viewModel.countPages().toFloat()
                if (count == 0f) {
                    count = 1f
                }
                Text(
                    "Outline=>Page:${currentPage.value}/${count.toInt()}",
                    style = TextStyle(
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(0.dp, 4.dp, 0.dp, 0.dp)
                        .align(Alignment.TopCenter)
                )
                var sliderState by remember { mutableStateOf(currentPage.value) }
                SliderWithLabel(
                    value = sliderState.toFloat(),
                    valueRange = 1f..count,
                    onRadiusChange = { newValue ->
                        sliderState = newValue.toInt()
                        Logcat.d("newValue:$newValue, sliderState:$sliderState, count:$count")
                    },
                    onValueChangeFinished = {
                        onSelect(OutlineItem("", sliderState))
                    },
                    modifier = Modifier.padding(20.dp, 2.dp, 20.dp, 0.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(Color(0xCc202020))
                        .weight(1f, true)
                ) {
                    LazyColumn(state = listState) {
                        itemsIndexed(outlines.list!!) { index, outlineItem ->
                            if (index > 0) {
                                Divider(thickness = 0.5.dp)
                            }
                            MenuItem(
                                outlineItem = outlineItem,
                                onSelect = {
                                    currentPage.value = it.page
                                    onSelect(it)
                                },
                            )
                        }
                    }
                }

                val configuration = LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                val screenWidth = configuration.screenWidthDp.dp

                //横屏取宽的1/2,竖屏到1/3作为空白区
                val swidth = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (screenWidth < screenHeight) {
                        screenWidth / 3
                    } else {
                        screenHeight / 3
                    }
                } else {
                    if (screenWidth < screenHeight) {
                        screenHeight / 2
                    } else {
                        screenWidth / 2
                    }
                }
                Box(
                    modifier = Modifier
                        .width(swidth)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                        .clickable(onClick = { outlineDialog.value = false })
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
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
            color = Color.White,
            modifier = Modifier.weight(1f, true)
        )
        Text(
            text = outlineItem.page.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            fontSize = 14.sp,
            modifier = modifier.padding(horizontal = 2.dp)
        )
    }
}
