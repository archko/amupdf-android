package cn.archko.sunflower.ui.home

import GankBean
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import cn.archko.sunflower.paging.ResourceState
import cn.archko.sunflower.ui.components.JetsnackSurface
import cn.archko.sunflower.ui.components.LoadingFooter
import cn.archko.sunflower.ui.utils.VLog
import cn.archko.sunflower.viewmodel.GankViewModel
import cn.archko.sunflower.ui.components.Divider

@Composable
fun GankGirlPaging(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    //这里会导致每次都加载，只要重新组合。所以加载更多是正常，但会重复加载第一页
    val viewModel: GankViewModel = viewModel()

    val pagingItems = viewModel.loadGankGirlsPaging().collectAsLazyPagingItems()
    VLog.d("pagingItems:${pagingItems.itemCount}")
    val listScrollState = rememberLazyListState()
    GankGirlList(
        pagingItems = pagingItems,
        listScrollState = listScrollState,
        navController = navController,
        modifier = modifier
    )
}

@Composable
private fun GankGirlList(
    pagingItems: LazyPagingItems<GankBean>,
    listScrollState: LazyListState,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    JetsnackSurface(modifier = modifier.fillMaxSize()) {
        Box {
            //val context = LocalContext.current
            val scroll = rememberScrollState(0)

            LazyColumn(state = listScrollState) {
                //item {
                //    Spacer(Modifier.statusBarsHeight(additional = 56.dp))
                //}
                itemsIndexed(pagingItems) { index, gankBean ->
                    if (index > 0) {
                        Divider(thickness = 1.dp)
                    }
                    //val animatedModifier = when (index % 2) {
                    //    0 -> {
                    //        val animatedProgress = remember { Animatable(initialValue = 0f) }
                    //        LaunchedEffect(Unit) {
                    //            animatedProgress.animateTo(
                    //                targetValue = 1f,
                    //                animationSpec = tween(600)
                    //            )
                    //        }
                    //        Modifier
                    //            .alpha(animatedProgress.value)
                    //    }
                    //    else -> {
                    //        val animatedProgress = remember { Animatable(initialValue = 0f) }
                    //        LaunchedEffect(Unit) {
                    //            animatedProgress.animateTo(
                    //                targetValue = 1f,
                    //                animationSpec = tween(600)
                    //            )
                    //        }
                    //        Modifier
                    //            .alpha(animatedProgress.value)
                    //    }
                    //}
                    gankBean?.let {
                        GankItem(
                            gankBean = gankBean,
                            navController = navController,
                            //modifier = animatedModifier,
                            index = index,
                            scroll = scroll,
                        )
                    }
                }

                pagingItems.apply {
                    VLog.d("pagingItems:${loadState.refresh}, ${loadState.append}")
                    when {
                        loadState.refresh is LoadState.Loading -> item {
                            LoadingFooter(
                                resourceState = ResourceState(initial = ResourceState.LOADING),
                                onClick = { },
                                total = 0,
                            )
                        }
                        //loadState.refresh is LoadState.NotLoading -> item {
                        //    Text(
                        //        text = "IDLE",
                        //        style = MaterialTheme.typography.h5,
                        //        color = JetsnackTheme.colors.textSecondary,
                        //        modifier = modifier
                        //    )
                        //}
                        loadState.refresh is LoadState.Error -> item {
                            LoadingFooter(
                                resourceState = ResourceState(initial = ResourceState.ERROR),
                                onClick = { pagingItems.retry() },
                                total = 0,
                            )
                        }
                        loadState.append is LoadState.Loading -> {
                            item {
                                LoadingFooter(
                                    resourceState = ResourceState(initial = ResourceState.FINISHED),
                                    onClick = { },
                                    total = 0,
                                )
                            }
                        }
                        loadState.append is LoadState.Error -> {
                            item {
                                LoadingFooter(
                                    resourceState = ResourceState(initial = ResourceState.ERROR),
                                    onClick = { pagingItems.retry() },
                                    total = 0,
                                )
                            }
                        }
                    }
                }
            }
            //DestinationBar()
        }
    }
}

/*@Composable
private fun GankGirl(
    gankResponse: GankResponse<MutableList<GankBean>>,
    resourceState: ResourceState,
    navController: NavHostController,
    loadMore: (Int) -> Unit,
    refresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    VLog.d("$refresh")
    if (resourceState.value == ResourceState.INIT || gankResponse.data.isEmpty()) {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box(modifier = modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .background(
                            color = Neutral8.copy(alpha = 0.32f),
                            shape = RectangleShape
                        )
                        .align(Alignment.Center)
                        .clickable { refresh() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.empty_state_search),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(horizontal = 5.dp)
                    )
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.h5,
                        color = JetsnackTheme.colors.textSecondary,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(horizontal = 5.dp)
                    )
                }
            }
        }
    } else {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box {
                ItemList(gankResponse, resourceState, navController, loadMore)
                DestinationBar()
            }
        }
    }
}*/

/*
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    gankResponse: GankResponse<MutableList<GankBean>>,
    resourceState: ResourceState,
    navController: NavHostController,
    loadMore: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState(0)
    val size = gankResponse.data.size
    LazyColumn(modifier) {
        item {
            Spacer(Modifier.statusBarsHeight(additional = 56.dp))
        }
        itemsIndexed(gankResponse.data) { index, gankBean ->
            if (index > 0) {
                JetsnackDivider(thickness = 1.dp)
            }
            GankItem(
                gankBean = gankBean,
                navController = navController,
                index = index,
                scroll = scroll
            )
            //VLog.d("$index")
            if (index == size - 1 || size == 0) {
                LoadintFooter(
                    resourceState = resourceState,
                    onClick = { loadMore(size) },
                    total = size,
                )
                VLog.d("scroll.index:$index, ${scroll.isScrollInProgress}, resourceState:${resourceState.value}")
                if (!scroll.isScrollInProgress) {
                    if (resourceState.value == ResourceState.LOADING || resourceState.value == ResourceState.ERROR) {
                    } else {
                        LaunchedEffect(size) {
                            loadMore(size)
                        }
                    }
                }
            }
        }
    }
}
*/

