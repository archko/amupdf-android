package cn.archko.pdf.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.archko.mupdf.R
import cn.archko.pdf.common.AnalysticsHelper
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.Surface
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.model.SearchSuggestionGroup
import cn.archko.pdf.theme.Typography
import cn.archko.pdf.viewmodel.FileViewModel
import com.google.accompanist.insets.statusBarsPadding
import com.umeng.analytics.MobclickAgent
import io.iamjosephmj.flinger.bahaviours.StockFlingBehaviours
import java.util.*

@Composable
fun FileSearchList(
    viewModel: FileViewModel,
    state: SearchState = rememberSearchState(viewModel),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onClick: (FileBean) -> Unit = { it ->
        if (it.isDirectory) {
        } else {
            PDFViewerHelper.openWithDefaultViewer(it.file!!, context)
        }
    }
    val showUserDialog = remember { mutableStateOf(false) }
    val showInfoDialog = remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(searchTypeFile) }

    val menuOpt: (MenuItemType, FileBean) -> Unit = { menuType, fb ->
        showUserDialog.value = false
        when (menuType) {
            MenuItemType.ViewBookWithAMupdf -> {
                PDFViewerHelper.openWithDefaultViewer(fb.file!!, context)
            }
            MenuItemType.ViewBookWithMupdf -> {
                PDFViewerHelper.openViewerMupdf(fb.file!!, context)
            }
            MenuItemType.OpenWithOther -> {
                PDFViewerHelper.openViewerOther(fb.file!!, context)
            }
            MenuItemType.ViewBookInfo -> {
                val map = HashMap<String, String>()
                map["type"] = "info"
                map["name"] = fb.file!!.name
                MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
                showInfoDialog.value = true
            }
            MenuItemType.DeleteFile -> {
                viewModel.deleteFile(fb)
            }
            MenuItemType.AddToFav -> {
                viewModel.favorite(context, fb, 1)
            }
            MenuItemType.DeleteFav -> {
                viewModel.favorite(context, fb, 0)
            }
        }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Box {
            Column {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(alignment = CenterHorizontally)
                ) {
                    RadioButton(
                        selected = selected == searchTypeFile,
                        onClick = { selected = searchTypeFile })
                    Text(
                        text = stringResource(id = R.string.search_file),
                        modifier = Modifier
                            .clickable(onClick = { selected = searchTypeFile })
                            .padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    RadioButton(
                        selected = selected == searchTypeHistory,
                        onClick = { selected = searchTypeHistory })
                    Text(
                        text = stringResource(id = R.string.search_history),
                        modifier = Modifier
                            .clickable(onClick = { selected = searchTypeHistory })
                            .padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    RadioButton(
                        selected = selected == searchTypeFavorite,
                        onClick = { selected = searchTypeFavorite })
                    Text(
                        text = stringResource(id = R.string.search_favorite),
                        modifier = Modifier
                            .clickable(onClick = { selected = searchTypeFavorite })
                            .padding(start = 4.dp)
                    )
                }
                SearchBar(
                    query = state.query,
                    onQueryChange = { state.query = it },
                    searchFocused = state.focused,
                    onSearchFocusChange = { state.focused = it },
                    onClearQuery = { state.query = TextFieldValue("") },
                    searching = state.searching
                )
                Divider()

                LaunchedEffect(state.query.text) {
                    state.searching = true
                    state.searchResults = viewModel.search(state.query.text, selected)
                    state.searching = false
                }
                when (state.searchDisplay) {
                    SearchDisplay.Categories -> {
                        Text(
                            text = "",
                            style = Typography.subtitle1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SearchDisplay.Suggestions -> SearchSuggestions(
                        suggestions = state.suggestions,
                        onSuggestionSelect = { suggestion ->
                            state.query = TextFieldValue(suggestion)
                        }
                    )
                    SearchDisplay.NoResults -> Text("no results")
                    SearchDisplay.Results -> ItemList(
                        state.searchResults,
                        showUserDialog,
                        showInfoDialog,
                        menuOpt,
                        onClick,
                        viewModel
                    )
                }
            }
        }
    }
}

enum class SearchDisplay {
    Categories, Suggestions, Results, NoResults
}

@Composable
private fun rememberSearchState(
    viewModel: FileViewModel,
    query: TextFieldValue = TextFieldValue(""),
    focused: Boolean = false,
    searching: Boolean = false,
    suggestions: List<SearchSuggestionGroup> = viewModel.getSuggestions(),
    searchResults: MutableList<FileBean> = mutableListOf()
): SearchState {
    return remember {
        SearchState(
            query = query,
            focused = focused,
            searching = searching,
            suggestions = suggestions,
            searchResults = searchResults
        )
    }
}

@Stable
class SearchState(
    query: TextFieldValue,
    focused: Boolean,
    searching: Boolean = false,
    suggestions: List<SearchSuggestionGroup>,
    searchResults: MutableList<FileBean>
) {
    var query by mutableStateOf(query)
    var focused by mutableStateOf(focused)
    var searching by mutableStateOf(searching)
    var suggestions by mutableStateOf(suggestions)
    var searchResults by mutableStateOf(searchResults)
    val searchDisplay: SearchDisplay
        get() = when {
            !focused && query.text.isEmpty() -> SearchDisplay.Categories
            focused && query.text.isEmpty() -> SearchDisplay.Suggestions
            searchResults.isEmpty() -> SearchDisplay.NoResults
            else -> SearchDisplay.Results
        }
}


@Composable
private fun SearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchFocused: Boolean,
    onSearchFocusChange: (Boolean) -> Unit,
    onClearQuery: () -> Unit,
    searching: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colors.secondary.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (query.text.isEmpty()) {
                SearchHint()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight()
            ) {
                if (searchFocused) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged {
                            onSearchFocusChange(it.isFocused)
                        }
                )
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(36.dp)
                    )
                } else {
                    Spacer(Modifier.width(36.dp)) // balance arrow icon
                }
            }
        }
    }
}

@Composable
private fun SearchHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize()
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Search",
            color = MaterialTheme.colors.primary.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemList(
    list: MutableList<FileBean>,
    showUserDialog: MutableState<Boolean>,
    showInfoDialog: MutableState<Boolean>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    onClick: (FileBean) -> Unit,
    viewModel: FileViewModel,
    modifier: Modifier = Modifier
) {
    val fileIndex = remember { mutableStateOf(0) }
    val onOptClick: (Int) -> Unit = { it ->
        fileIndex.value = it
        if (it > list.size) {
            fileIndex.value = 0
        }
        showUserDialog.value = true
    }
    //Logcat.d("showUserDialog:${showUserDialog.value}, file.fileIndex:${fileIndex.value}")
    //if (fileIndex.value < list.size) {
    //    showUserDialog.value = false
    //}
    val fileBean = if (fileIndex.value >= list.size) null else list[fileIndex.value]
    UserOptDialog(showUserDialog, fileBean, menuOpt)
    FileInfoDialog(showInfoDialog, fileBean, menuOpt)
    LazyColumn(
        flingBehavior = StockFlingBehaviours.smoothScroll(),
        modifier=modifier
    ) {
        itemsIndexed(list) { index, fileBean ->
            if (index > 0) {
                Divider(thickness = 1.dp)
            }
            FileItem(
                fileBean = fileBean,
                index = index,
                onOptClick = onOptClick,
                onClick = onClick,
                viewModel = viewModel
            )
        }
    }
}
