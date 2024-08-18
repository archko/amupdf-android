package cn.archko.pdf.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * Scrollbar selection modes.
 */
enum class ScrollbarSelectionMode {
    /**
     * Enable selection in the whole scrollbar and thumb
     */
    Full,

    /**
     * Enable selection in the thumb
     */
    Thumb,

    /**
     * Disable selection
     */
    Disabled
}

/**
 * Scrollbar selection modes.
 */
enum class ScrollbarSelectionActionable {
    /**
     * Can select scrollbar always (when visible or hidden)
     */
    Always,

    /**
     * Can select scrollbar only when visible
     */
    WhenVisible,
}

/**
 * Scrollbar for LazyColumn
 *
 * @param rightSide true -> right,  false -> left
 */
@Composable
fun LazyColumnScrollbar(
    listState: LazyListState,
    rightSide: Boolean = true,
    alwaysShowScrollBar: Boolean = false,
    thumbMinHeight: Float = 0.1f,
    selectionMode: ScrollbarSelectionMode = ScrollbarSelectionMode.Thumb,
    selectionActionable: ScrollbarSelectionActionable = ScrollbarSelectionActionable.WhenVisible,
    hideDelayMillis: Int = 400,
    enabled: Boolean = true,
    indicatorContent: (@Composable (index: Int, isThumbSelected: Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
    } else {
        var indicator = indicatorContent
        if (indicator == null) {
            indicator = { index, isThumbSelected ->
                DefaultIndicator(index, isThumbSelected)
            }
        }
        Box {
            content()

            InternalLazyColumnScrollbar(
                listState = listState,
                modifier = Modifier,
                rightSide = rightSide,
                alwaysShowScrollBar = alwaysShowScrollBar,
                thumbMinHeight = thumbMinHeight,
                selectionActionable = selectionActionable,
                hideDelayMillis = hideDelayMillis,
                selectionMode = selectionMode,
                indicatorContent = indicator,
            )
        }
    }
}

@Composable
private fun DefaultIndicator(index: Int, isThumbSelected: Boolean) {
    val color = if (isThumbSelected) Color.Gray else Color(0xFFDADFE1)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .background(color, RoundedCornerShape(12.dp, 2.dp, 2.dp, 12.dp))
            .size(48.dp, 38.dp),
        //.clip(RoundedCornerShape(topStart = 12.dp))
        //.clip(CutCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
        //.background(Color.Red)
    ) {
        Text(
            text = "${index + 1}",
            maxLines = 1,
            color = Color.Black,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontSize = 20.sp),
        )
    }
}


/**
 * Scrollbar for LazyColumn
 * Use this variation if you want to place the scrollbar independently of the LazyColumn position
 *
 * @param rightSide true -> right,  false -> left
 * @param thickness Thickness of the scrollbar thumb
 * @param padding Padding of the scrollbar
 * @param thumbMinHeight Thumb minimum height proportional to total scrollbar's height (eg: 0.1 -> 10% of total)
 */
@Composable
fun InternalLazyColumnScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    rightSide: Boolean = true,
    alwaysShowScrollBar: Boolean = false,
    thumbMinHeight: Float = 0.1f,
    selectionMode: ScrollbarSelectionMode = ScrollbarSelectionMode.Thumb,
    selectionActionable: ScrollbarSelectionActionable = ScrollbarSelectionActionable.Always,
    hideDelayMillis: Int = 400,
    indicatorContent: (@Composable (index: Int, isThumbSelected: Boolean) -> Unit),
) {
    var isSelected by remember { mutableStateOf(false) }

    val isInAction = listState.isScrollInProgress || isSelected || alwaysShowScrollBar

    val isInActionSelectable = remember { mutableStateOf(isInAction) }
    val durationAnimationMillis = 500
    LaunchedEffect(isInAction) {
        if (isInAction) {
            isInActionSelectable.value = true
        } else {
            delay(timeMillis = durationAnimationMillis.toLong() + hideDelayMillis.toLong())
            isInActionSelectable.value = false
        }
    }

    if (
        when (selectionActionable) {
            ScrollbarSelectionActionable.Always -> true
            ScrollbarSelectionActionable.WhenVisible -> isInActionSelectable.value
        }
    ) {
        val firstVisibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }

        val coroutineScope = rememberCoroutineScope()

        var dragOffset by remember { mutableFloatStateOf(0f) }

        val reverseLayout by remember { derivedStateOf { listState.layoutInfo.reverseLayout } }

        val realFirstVisibleItem by remember {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo.firstOrNull {
                    it.index == listState.firstVisibleItemIndex
                }
            }
        }

        val isStickyHeaderInAction by remember {
            derivedStateOf {
                val realIndex = realFirstVisibleItem?.index ?: return@derivedStateOf false
                val firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                    ?: return@derivedStateOf false
                realIndex != firstVisibleIndex
            }
        }

        fun LazyListItemInfo.fractionHiddenTop(firstItemOffset: Int) =
            if (size == 0) 0f else firstItemOffset / size.toFloat()

        fun LazyListItemInfo.fractionVisibleBottom(viewportEndOffset: Int) =
            if (size == 0) 0f else (viewportEndOffset - offset).toFloat() / size.toFloat()

        val normalizedThumbSizeReal by remember {
            derivedStateOf {
                listState.layoutInfo.let {
                    if (it.totalItemsCount == 0)
                        return@let 0f

                    val firstItem = realFirstVisibleItem ?: return@let 0f
                    val firstPartial =
                        firstItem.fractionHiddenTop(listState.firstVisibleItemScrollOffset)
                    val lastPartial =
                        1f - it.visibleItemsInfo.last().fractionVisibleBottom(it.viewportEndOffset)

                    val realSize = it.visibleItemsInfo.size - if (isStickyHeaderInAction) 1 else 0
                    val realVisibleSize = realSize.toFloat() - firstPartial - lastPartial
                    realVisibleSize / it.totalItemsCount.toFloat()
                }
            }
        }

        val normalizedThumbSize by remember {
            derivedStateOf {
                normalizedThumbSizeReal.coerceAtLeast(thumbMinHeight)
            }
        }

        fun offsetCorrection(top: Float): Float {
            val topRealMax = (1f - normalizedThumbSizeReal).coerceIn(0f, 1f)
            if (normalizedThumbSizeReal >= thumbMinHeight) {
                return when {
                    reverseLayout -> topRealMax - top
                    else -> top
                }
            }

            val topMax = 1f - thumbMinHeight
            return when {
                reverseLayout -> (topRealMax - top) * topMax / topRealMax
                else -> top * topMax / topRealMax
            }
        }

        fun offsetCorrectionInverse(top: Float): Float {
            if (normalizedThumbSizeReal >= thumbMinHeight)
                return top
            val topRealMax = 1f - normalizedThumbSizeReal
            val topMax = 1f - thumbMinHeight
            return top * topRealMax / topMax
        }

        val normalizedOffsetPosition by remember {
            derivedStateOf {
                listState.layoutInfo.let {
                    if (it.totalItemsCount == 0 || it.visibleItemsInfo.isEmpty())
                        return@let 0f

                    val firstItem = realFirstVisibleItem ?: return@let 0f
                    val top = firstItem
                        .run { index.toFloat() + fractionHiddenTop(listState.firstVisibleItemScrollOffset) } / it.totalItemsCount.toFloat()
                    offsetCorrection(top)
                }
            }
        }

        fun setDragOffset(value: Float) {
            val maxValue = (1f - normalizedThumbSize).coerceAtLeast(0f)
            dragOffset = value.coerceIn(0f, maxValue)
        }

        fun setScrollOffset(newOffset: Float) {
            setDragOffset(newOffset)
            val totalItemsCount = listState.layoutInfo.totalItemsCount.toFloat()
            val exactIndex = offsetCorrectionInverse(totalItemsCount * dragOffset)
            val index: Int = floor(exactIndex).toInt()
            val remainder: Float = exactIndex - floor(exactIndex)

            coroutineScope.launch {
                listState.scrollToItem(index = index, scrollOffset = 0)
                val offset = realFirstVisibleItem
                    ?.size
                    ?.let { it.toFloat() * remainder }
                    ?: 0f
                listState.scrollBy(offset)
            }
        }

        /*val alpha by animateFloatAsState(
            targetValue = if (isInAction) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isInAction) 75 else durationAnimationMillis,
                delayMillis = if (isInAction) 0 else hideDelayMillis
            ),
            label = "scrollbar alpha value"
        )*/

        val displacement by animateFloatAsState(
            targetValue = if (isInAction) 0f else 14f,
            animationSpec = tween(
                durationMillis = if (isInAction) 75 else durationAnimationMillis,
                delayMillis = if (isInAction) 0 else hideDelayMillis
            ),
            label = "scrollbar displacement value"
        )

        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
        ) {
            val maxHeightFloat = constraints.maxHeight.toFloat()
            /*ConstraintLayout(
                modifier = Modifier
                    .align(if (rightSide) Alignment.TopEnd else Alignment.TopStart)
                    .graphicsLayer(
                        translationX = with(LocalDensity.current) { (if (rightSide) displacement.dp else -displacement.dp).toPx() },
                        translationY = maxHeightFloat * normalizedOffsetPosition
                    )
            ) {
                val (box, content) = createRefs()
                *//*Box(
                    modifier = Modifier
                        .padding(
                            start = if (rightSide) 0.dp else padding,
                            end = if (!rightSide) 0.dp else padding,
                        )
                        .clip(thumbShape)
                        .width(thickness)
                        .fillMaxHeight(normalizedThumbSize)
                        .alpha(alpha)
                        .background(if (isSelected) thumbSelectedColor else thumbColor)
                        .constrainAs(box) {
                            if (rightSide) end.linkTo(parent.end)
                            else start.linkTo(parent.start)
                        }
                        .testTag("TestTagsScrollbar.scrollbar")
                )*//*

                Box(
                    modifier = Modifier
                        ///.alpha(alpha)
                        .constrainAs(content) {
                            top.linkTo(box.top)
                            bottom.linkTo(box.bottom)
                            if (rightSide) end.linkTo(box.start)
                            else start.linkTo(box.end)
                        }
                        .testTag("TestTagsScrollbar.scrollbarIndicator")
                        .draggable(
                            state = rememberDraggableState { delta ->
                                val displace = if (reverseLayout) -delta else delta // side effect ?
                                if (isSelected) {
                                    setScrollOffset(dragOffset + displace / maxHeightFloat)
                                }
                            },
                            orientation = Orientation.Vertical,
                            enabled = selectionMode != ScrollbarSelectionMode.Disabled,
                            startDragImmediately = true,
                            onDragStarted = onDragStarted@{ offset ->
                                isSelected = true
                                if (maxHeightFloat <= 0f) {
                                    return@onDragStarted
                                }
                                val newOffset = when {
                                    reverseLayout -> (maxHeightFloat - offset.y) / maxHeightFloat
                                    else -> offset.y / maxHeightFloat
                                }
                                val currentOffset = when {
                                    reverseLayout -> 1f - normalizedOffsetPosition - normalizedThumbSize
                                    else -> normalizedOffsetPosition
                                }

                                when (selectionMode) {
                                    ScrollbarSelectionMode.Full -> {
                                        if (newOffset in currentOffset..(currentOffset + normalizedThumbSize)) {
                                            setDragOffset(currentOffset)
                                        } else {
                                            setScrollOffset(newOffset)
                                        }
                                        //isSelected = true
                                    }

                                    ScrollbarSelectionMode.Thumb -> {
                                        //if (newOffset in currentOffset..(currentOffset + normalizedThumbSize)) {
                                        setDragOffset(currentOffset)
                                        //isSelected = true
                                        //}
                                    }

                                    ScrollbarSelectionMode.Disabled -> Unit
                                }
                            },
                            onDragStopped = {
                                isSelected = false
                            }
                        ),
                ) {
                    val index = firstVisibleItemIndex.value
                    indicatorContent(
                        index,
                        isSelected
                    )
                }
            }*/
        }
    } else {
        return
    }
}