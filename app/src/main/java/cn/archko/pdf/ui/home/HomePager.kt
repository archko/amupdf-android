package cn.archko.pdf.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cn.archko.pdf.common.Logcat
import cn.archko.mupdf.R
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.theme.JetsnackTheme
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun HomePager(
    navController: NavHostController
) {
    val showMenu = remember { mutableStateOf(false) }

    val showLoadingDialog = remember { mutableStateOf(false) }
    val navItems = listOf("History", "File")
    val (currentSection, setCurrentSection) = rememberSaveable {
        mutableStateOf(0)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        overflow = TextOverflow.Ellipsis,
                        color = JetsnackTheme.colors.uiFloated,
                    )
                },
                elevation = 0.dp,
                backgroundColor = JetsnackTheme.colors.uiBorder,
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    Text(
                        text = stringResource(id = R.string.menu_search),
                        modifier = Modifier
                            .clickable(onClick = { })
                            .padding(10.dp)
                    )
                    IconButton(onClick = { showMenu.value = !showMenu.value }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        }
    ) {
        val onPalletChange: () -> Unit = { ->
            showMenu.value = false
        }
        Box(modifier = Modifier.fillMaxSize()) {
            TabContent(navController, navItems, setCurrentSection)
            PalletMenu(
                currentSection,
                showLoadingDialog,
                modifier = Modifier.align(Alignment.TopEnd),
                showMenu.value,
                onPalletChange
            )
        }
        LoadingDialog(showLoadingDialog)
    }
}

fun backup(showLoadingDialog: MutableState<Boolean>) {
    showLoadingDialog.value = true
}

fun restore(showLoadingDialog: MutableState<Boolean>) {
    showLoadingDialog.value = true
}

fun setAsHome() {

}

@Composable
fun PalletMenu(
    currentSection: Int,
    showLoadingDialog: MutableState<Boolean>,
    modifier: Modifier,
    showMenu: Boolean,
    onPalletChange: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .width(120.dp)
            .padding(8.dp)
            .animateContentSize(),
        elevation = 8.dp,
        shape = MaterialTheme.shapes.large,
        backgroundColor = JetsnackTheme.colors.uiBackground,
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            if (showMenu) {
                when (currentSection) {
                    0 -> {
                        MenuItem(stringResource(id = R.string.options)) {
                            onPalletChange()
                            PdfOptionsActivity.start(
                                context
                            )
                        }
                        MenuItem(stringResource(id = R.string.menu_backup)) {
                            onPalletChange()
                            backup(showLoadingDialog)
                        }
                        MenuItem(stringResource(id = R.string.menu_restore)) {
                            onPalletChange()
                            restore(showLoadingDialog)
                        }
                    }
                    1 -> {
                        MenuItem(stringResource(id = R.string.options)) {
                            onPalletChange()
                            PdfOptionsActivity.start(
                                context
                            )
                        }
                        MenuItem(stringResource(id = R.string.menu_set_as_home)) {
                            onPalletChange()
                            setAsHome()
                        }
                    }
                    else -> {
                        MenuItem(stringResource(id = R.string.options)) {
                            onPalletChange()
                            PdfOptionsActivity.start(
                                context
                            )
                        }
                    }
                }
                MenuItem(stringResource(id = R.string.menu_about)) {
                    onPalletChange()
                    AboutActivity.start(context)
                }
            }
        }
    }
}

@Composable
fun MenuItem(name: String, onPalletChange: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onPalletChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Icon(imageVector = Icons.Filled.FiberManualRecord, tint = color, contentDescription = null)
        Text(text = name, color = Color.Black, modifier = Modifier.padding(4.dp))
    }
}

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
private fun TabContent(
    navController: NavHostController,
    navItems: List<String>,
    setCurrentSection: (Int) -> Unit,
) {
    Logcat.d("$navController")
    val viewModel: FileViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = navItems.size)
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.navigationBarsPadding(bottom = false),
            backgroundColor = MaterialTheme.colors.background,
            edgePadding = 2.dp,
            contentColor = MaterialTheme.colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .pagerTabIndicatorOffset(pagerState, tabPositions)
                        .height(4.dp),
                    color = Color.Blue
                )
            }
        ) {
            navItems.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        // Animate to the selected page when clicked
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title.toUpperCase(Locale.getDefault()),
                            color = if (pagerState.currentPage == index) Color.White else Color(
                                0x61000000
                            ),
                        )
                    },
                    //unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            content = { page ->
                setCurrentSection(pagerState.currentPage)
                when (page) {
                    0 -> FileHistoryList(
                        viewModel,
                        navigateTo = { Logcat.d("file.navigateTo") },
                    )
                    1 -> FileList(
                        viewModel,
                        navigateTo = { Logcat.d("file.navigateTo") },
                    )
                }
            }
        )
    }
}
