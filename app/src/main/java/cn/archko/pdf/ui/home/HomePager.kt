package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cn.archko.mupdf.R
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.paging.ResourceState
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun HomePager(
    navController: NavHostController
) {
    val context = LocalContext.current
    val showLoadingDialog = remember { mutableStateOf(false) }
    val showRestoreDialog = remember { mutableStateOf(false) }

    val viewModel: FileViewModel = viewModel()
    val uiBackup by viewModel.uiBackupModel.collectAsState()
    val showMenu = remember { mutableStateOf(false) }

    showLoadingDialog.value = (uiBackup.value == ResourceState.LOADING)

    val navItems =
        listOf(
            stringResource(id = R.string.tab_history),
            stringResource(id = R.string.tab_browser),
            stringResource(id = R.string.tab_favorite)
        )

    val (currentSection, setCurrentSection) = rememberSaveable {
        mutableStateOf(0)
    }

    val onPalletChange: () -> Unit = { ->
        showMenu.value = false
    }

    val onSelectFile: (File) -> Unit = { it ->
        showRestoreDialog.value = false
        viewModel.restoreToDb(it)
    }

    val menuContent: @Composable BoxScope.() -> Unit = {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            when (currentSection) {
                0 -> {
                    MenuItem(stringResource(id = R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                    MenuItem(stringResource(id = R.string.menu_backup)) {
                        onPalletChange()
                        backup(showLoadingDialog, viewModel)
                    }
                    MenuItem(stringResource(id = R.string.menu_restore)) {
                        onPalletChange()
                        restore(showRestoreDialog)
                    }
                }
                1 -> {
                    MenuItem(stringResource(id = R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                    MenuItem(stringResource(id = R.string.menu_set_as_home)) {
                        onPalletChange()
                        viewModel.setAsHome(context)
                    }
                }
                else -> {
                    MenuItem(stringResource(id = R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                }
            }
            MenuItem(stringResource(id = R.string.menu_about)) {
                onPalletChange()
                AboutActivity.start(context)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    elevation = 0.dp,
                    navigationIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null
                        )
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
            Box(modifier = Modifier.fillMaxSize()) {
                TabContent(viewModel, navController, navItems, setCurrentSection)
                PopupMenu(
                    modifier = Modifier.align(Alignment.TopEnd),
                    showMenu,
                    menuContent
                )
            }
        }
        RestoreDialog(showRestoreDialog, viewModel, onSelectFile)
        LoadingDialog(showLoadingDialog)
    }
}

private fun backup(showLoadingDialog: MutableState<Boolean>, viewModel: FileViewModel) {
    showLoadingDialog.value = true
    viewModel.backupFromDb()
}

private fun restore(showRestoreDialog: MutableState<Boolean>) {
    showRestoreDialog.value = true
}

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
private fun TabContent(
    viewModel: FileViewModel,
    navController: NavHostController,
    navItems: List<String>,
    setCurrentSection: (Int) -> Unit,
) {
    Logcat.d("$navController")
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = navItems.size)
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.navigationBarsPadding(bottom = false),
            edgePadding = 2.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .pagerTabIndicatorOffset(pagerState, tabPositions)
                        .height(4.dp),
                    color = MaterialTheme.colors.secondary,
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
                        )
                    },
                    unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
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
                    2 -> FileFavoritiesList(
                        viewModel,
                        navigateTo = { Logcat.d("file.navigateTo") },
                    )
                }
            }
        )
    }
}
