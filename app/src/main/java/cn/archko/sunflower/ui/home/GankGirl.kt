package cn.archko.sunflower.ui.home

import GankBean
import GankResponse
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cn.archko.sunflower.R
import cn.archko.sunflower.paging.ResourceState
import cn.archko.sunflower.ui.components.Divider
import cn.archko.sunflower.ui.components.JetsnackSurface
import cn.archko.sunflower.ui.components.LoadingFooter
import cn.archko.sunflower.ui.theme.JetsnackTheme
import cn.archko.sunflower.ui.theme.Neutral8
import cn.archko.sunflower.ui.utils.VLog
import cn.archko.sunflower.viewmodel.GankViewModel

@Composable
fun GankGirl(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    //这里会导致每次都加载，只要重新组合。所以加载更多是正常，但会重复加载第一页
    val viewModel: GankViewModel = viewModel()
    val gankResponse by viewModel.gankResponse.collectAsState()
    val resourceState by viewModel.dataLoading.collectAsState()
    if (viewModel.gankResponse.value.data.isEmpty()
        && resourceState.value != ResourceState.ERROR
        && resourceState.value != ResourceState.LOADING
    ) {
        viewModel.loadGankGirls(1)
    }
    val loadMore: (Int) -> Unit = { index: Int ->
        VLog.d("loadMore:$index")
        viewModel.loadMoreGankGirls()
    }
    val refresh: () -> Unit = { ->
        viewModel.loadGankGirls(1)
    }
    VLog.d("GankGirl")
    GankGirl(
        gankResponse = gankResponse,
        resourceState = resourceState,
        navController = navController,
        refresh = refresh,
        loadMore = loadMore,
        modifier = modifier
    )
}

@Composable
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
            }
        }
    }
}

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
        //item {
        //    Spacer(Modifier.statusBarsHeight(additional = 56.dp))
        //}
        itemsIndexed(gankResponse.data) { index, gankBean ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            GankItem(
                gankBean = gankBean,
                navController = navController,
                index = index,
                scroll = scroll
            )
            //VLog.d("$index")
            if (index == size - 1 || size == 0) {
                LoadingFooter(
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

