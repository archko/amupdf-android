package cn.archko.pdf.ui.home

import android.content.Context
import android.text.TextUtils
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.archko.mupdf.R
import cn.archko.pdf.bahaviours.CustomFlingBehaviours
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.LengthUtils
import com.google.samples.apps.nowinandroid.core.ui.component.NiaGradientBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val version = packageInfo.versionName
    val name = packageInfo.applicationInfo?.let { stringResource(it.labelRes) }
    val text = name + if (LengthUtils.isNotEmpty(version)) " v$version" else ""

    NiaGradientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = null)
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
                Column {
                    Box(
                        modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Version:$text",
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(fontSize = 18.sp)
                        )
                    }
                    LazyColumn(
                        flingBehavior = CustomFlingBehaviours.smoothScroll(),
                        modifier = modifier
                    ) {
                        itemsIndexed(PARTS) { index, part ->
                            HorizontalDivider(thickness = 1.dp)
                            PartItem(context, part, modifier)
                            HorizontalDivider(thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartItem(
    context: Context,
    part: Part,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val expanded = remember { mutableStateOf(part.expanded) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier
                .clickable {
                    expanded.value = !expanded.value
                    part.expanded = expanded.value
                }
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = stringResource(id = part.labelId),
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
        }
        if (expanded.value) {
            HorizontalDivider(thickness = 1.dp)
            val androidImageView = remember {
                WebView(context).apply {
                    coroutineScope.launch(Dispatchers.IO) {
                        val content = part.getContent(context).toString()
                        withContext(Dispatchers.Main) {
                            loadData(content, "text/html", "UTF-8")
                        }
                    }
                }
            }
            AndroidView(
                { androidImageView },
                modifier = Modifier.fillMaxSize(),
            ) {
            }
        }
    }
}

val PARTS = arrayOf(
    Part(
        R.string.about_commmon_title,
        Format.HTML,
        "about_common.html", false
    ),
    Part(
        R.string.about_3dparty_title,
        Format.HTML,
        "about_3rdparty.html", false
    ),
    Part(
        R.string.about_changelog_title,
        Format.HTML,
        "about_changelog.html", false
    )
)

class Part(
    val labelId: Int,
    val format: Format,
    val fileName: String,
    var expanded: Boolean
) {
    var content: CharSequence? = null
    fun getContent(context: Context?): CharSequence? {
        if (TextUtils.isEmpty(content)) {
            content = try {
                val text = FileUtils.readAssetAsString(fileName)
                format.format(text)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
        return content
    }
}

enum class Format {
    /**
     *
     */
    TEXT,

    /**
     *
     */
    HTML;

    /**
     *
     */
    /*WIKI {
        @Override
        public CharSequence format(final String text) {
            return Wiki.fromWiki(text);
        }
    };*/
    fun format(text: String): CharSequence {
        return text
    }
}