package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cn.archko.mupdf.R
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.State
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.google.samples.apps.nowinandroid.core.ui.component.NiaGradientBackground
import com.google.samples.apps.nowinandroid.core.ui.component.NiaTab
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun HomePager(
    changeTheme: (Boolean) -> Unit,
    darkTheme: Boolean,
    up: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
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
            stringResource(id = cn.archko.pdf.R.string.tab_search),
            stringResource(id = cn.archko.pdf.R.string.tab_history),
            stringResource(id = cn.archko.pdf.R.string.tab_browser),
            stringResource(id = cn.archko.pdf.R.string.tab_favorite),
        )

    val (currentSection, setCurrentSection) = rememberSaveable {
        mutableStateOf(0)
    }

    LiveEventBus
        .get(Event.ACTION_STOPPED, String::class.java)
        .observe(
            LocalLifecycleOwner.current
        ) { t -> viewModel.onReadBook(t, currentSection) }

    val onPalletChange: () -> Unit = { ->
        showMenu.value = false
    }

    val onSelectFile: (File) -> Unit = { it ->
        showRestoreDialog.value = false
        viewModel.restoreToDb(it)
    }

    val menuContent: @Composable ColumnScope.() -> Unit = {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            when (currentSection) {
                1 -> {
                    MenuItem(stringResource(id = cn.archko.pdf.R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                    MenuItem(stringResource(id = cn.archko.pdf.R.string.menu_backup)) {
                        onPalletChange()
                        backup(showLoadingDialog, viewModel)
                    }
                    MenuItem(stringResource(id = cn.archko.pdf.R.string.menu_restore)) {
                        onPalletChange()
                        restore(showRestoreDialog)
                    }
                }

                2 -> {
                    MenuItem(stringResource(id = cn.archko.pdf.R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                    MenuItem(stringResource(id = cn.archko.pdf.R.string.menu_set_as_home)) {
                        onPalletChange()
                        viewModel.setAsHome(context)
                    }
                }

                0, 3 -> {
                    MenuItem(stringResource(id = cn.archko.pdf.R.string.options)) {
                        onPalletChange()
                        PdfOptionsActivity.start(context)
                    }
                }
            }
            MenuItem(stringResource(id = cn.archko.pdf.R.string.menu_about)) {
                onPalletChange()
                AboutActivity.start(context)
            }
            MenuItem(stringResource(id = cn.archko.pdf.R.string.menu_tools)) {
                onPalletChange()
                //PDFToolActivity.start(context)
            }
        }
    }

    NiaGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            changeTheme(darkTheme)
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
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = modifier
                    .padding(innerPadding)
            ) {
                TabContent(
                    viewModel, up, navController, navItems, setCurrentSection
                )
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
    up: () -> Unit,
    navController: NavHostController,
    navItems: List<String>,
    setCurrentSection: (Int) -> Unit,
) {
    Logcat.d("$navController")
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1)
    Column {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.navigationBarsPadding(),
            edgePadding = 2.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .pagerTabIndicatorOffset(pagerState, tabPositions)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            navItems.forEachIndexed { index, title ->
                NiaTab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        // Animate to the selected page when clicked
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title) },
                )
            }
        }

        HorizontalPager(
            count = 4,
            state = pagerState,
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
                        up,
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
