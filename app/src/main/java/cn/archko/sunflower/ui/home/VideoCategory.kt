package cn.archko.sunflower.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.archko.sunflower.model.ACategory
import cn.archko.sunflower.ui.components.JetsnackSurface
import cn.archko.sunflower.viewmodel.VideoViewModel
import cn.archko.sunflower.ui.components.Divider
import cn.archko.sunflower.ui.theme.JetsnackTheme
import cn.archko.sunflower.ui.theme.Neutral8
import cn.archko.sunflower.ui.utils.Screen
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding

@Composable
fun VideoCategory(
    navigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: VideoViewModel = viewModel()
    if (viewModel.categoryResponse.value.categories.isEmpty()) {
        viewModel.loadCategories(0)
    }
    val categoryResponse by viewModel.categoryResponse.collectAsState()
    val aCategories = categoryResponse.categories

    val refresh: () -> Unit = { ->
        viewModel.loadCategories(0)
    }
    VideoCategory(
        aCategories = aCategories,
        navigateTo = navigateTo,
        refresh = refresh,
        modifier = modifier
    )
}

@Composable
private fun VideoCategory(
    aCategories: List<ACategory>,
    navigateTo: (Screen) -> Unit,
    refresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (aCategories.isNotEmpty()) {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box {
                ItemList(aCategories, navigateTo)
            }
        }
    } else {
        JetsnackSurface(modifier = modifier.fillMaxSize()) {
            Box(modifier = modifier.fillMaxSize()) {
                Button(
                    onClick = refresh,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        //.size(36.dp)
                        .background(
                            color = Neutral8.copy(alpha = 0.32f),
                            shape = CircleShape
                        )
                        .align(Alignment.Center)
                ) {
                    /*Icon(
                        imageVector = Icons.Outlined.Refresh,
                        tint = JetsnackTheme.colors.iconInteractive,
                        contentDescription = stringResource(R.string.label_back)
                    )*/
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.h5,
                        color = JetsnackTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    aCategories: List<ACategory>,
    navigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState(0)
    LazyColumn(modifier) {
        //item {
        //    Spacer(Modifier.statusBarsHeight(additional = 56.dp))
        //}
        itemsIndexed(aCategories) { index, aCategory ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            VideoItem(
                aCategory = aCategory,
                navigateTo = navigateTo,
                index = index,
                scroll = scroll
            )
        }
    }
    /*LazyVerticalGrid(cells = GridCells.Fixed(2), modifier) {
        item {
            Spacer(Modifier.statusBarsHeight(additional = 56.dp))
        }
        itemsIndexed(aCategories) { index, aCategory ->
            if (index > 0) {
                JetsnackDivider(thickness = 1.dp)
            }
            VideoItem(
                aCategory = aCategory,
                onSnackClick = onSnackClick,
                index = index,
                scroll = scroll
            )
        }
    }*/
}

