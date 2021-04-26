package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cn.archko.mupdf.R
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.paging.State
import cn.archko.pdf.theme.AppThemeState
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun HomePager(
    appThemeState: MutableState<AppThemeState>,
    navController: NavHostController
) {
    val context = LocalContext.current
    val showLoadingDialog = remember { mutableStateOf(false) }
    val showRestoreDialog = remember { mutableStateOf(false) }

    val viewModel: FileViewModel = viewModel()
    val uiBackup by viewModel.uiBackupModel.collectAsState()
    val showMenu = remember { mutableStateOf(false) }

    showLoadingDialog.value = (uiBackup.state == State.LOADING)

    val navItems =
        listOf(
            stringResource(id = R.string.tab_search),
            stringResource(id = R.string.tab_history),
            stringResource(id = R.string.tab_browser),
            stringResource(id = R.string.tab_favorite),
        )

    val (currentSection, setCurrentSection) = rememberSaveable {
        mutableStateOf(0)
    }

    LiveEventBus
        .get(Event.ACTION_STOPPED, String::class.java)
        .observe(LocalLifecycleOwner.current, object : Observer<String> {
            override fun onChanged(t: String?) {
                viewModel.onReadBook(t, currentSection)
            }
        })

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
                1 -> {
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
                2 -> {
                    MenuItem(stringResource(id = R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                    MenuItem(stringResource(id = R.string.menu_set_as_home)) {
                        onPalletChange()
                        viewModel.setAsHome(context)
                    }
                }
                0, 3 -> {
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
                        IconButton(onClick = {
                            appThemeState.value = appThemeState
                                .value.copy(darkTheme = !appThemeState.value.darkTheme)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sleep),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { PdfOptionsActivity.start(context) }) {
                            Icon(
                                imageVector = Icons.Default.Settings, contentDescription = null,
                            )
                        }
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
    val pagerState = rememberPagerState(initialPage = 1, pageCount = navItems.size)
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
            offscreenLimit = 2,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            content = { page ->
                setCurrentSection(pagerState.currentPage)
                when (page) {
                    0 -> FileSearchList(
                        viewModel,
                    )
                    1 -> FileHistoryList(
                        viewModel,
                        navigateTo = { Logcat.d("file.navigateTo") },
                    )
                    2 -> FileBrowserList(
                        viewModel,
                        navigateTo = { Logcat.d("file.navigateTo") },
                    )
                    3 -> FileFavoritiesList(
                        viewModel,
                        navigateTo = { Logcat.d("file.navigateTo") },
                    )
                }
            }
        )
    }
}
