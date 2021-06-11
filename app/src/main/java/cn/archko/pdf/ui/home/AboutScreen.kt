package cn.archko.pdf.ui.home

import android.content.Context
import android.text.TextUtils
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.archko.mupdf.R
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.LengthUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val version = packageInfo.versionName
    val name = stringResource(packageInfo.applicationInfo.labelRes)
    val text = name + if (LengthUtils.isNotEmpty(version)) " v$version" else ""

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
                    IconButton(onClick = { onBack() }) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                },
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                LazyColumn(modifier) {
                    itemsIndexed(PARTS) { index, part ->
                        Divider(thickness = 1.dp)
                        PartItem(context, part, modifier)
                        Divider(thickness = 1.dp)
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
    val expanded = remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier
                .clickable { expanded.value = !expanded.value }
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
            Divider(thickness = 1.dp)
            val androidImageView = remember {
                WebView(context).apply {
                    coroutineScope.launch(Dispatchers.IO) {
                        val content = part.getContent(context).toString()
                        withContext(Dispatchers.Main){
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
        "about_common.html"
    ),  //new Part(R.string.about_license_title, Format.HTML, "about_license.html"),
    Part(
        R.string.about_3dparty_title,
        Format.HTML,
        "about_3rdparty.html"
    ),
    Part(
        R.string.about_changelog_title,
        Format.HTML,
        "about_changelog.html"
    )
)

class Part(
    val labelId: Int,
    val format: Format,
    val fileName: String
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