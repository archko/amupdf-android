package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.archko.mupdf.R
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.JetsnackSurface
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.theme.JetsnackTheme
import cn.archko.pdf.theme.Neutral8

@Composable
fun EmptyView(modifier: Modifier) {
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
            ) {
                Image(
                    painter = painterResource(id = R.drawable.loading),
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
}

sealed class MenuItemType {
    object ViewBookWithAMupdf : MenuItemType()
    object ViewBookWithMupdf : MenuItemType()
    object ViewBookInfo : MenuItemType()
    object DeleteHistory : MenuItemType()
    object DeleteFile : MenuItemType()
    object AddToFav : MenuItemType()
    object DeleteFav : MenuItemType()
    object OpenWithOther : MenuItemType()
}

sealed class FileBeanType {
    object SysFile : FileBeanType()
    object History : FileBeanType()
    object Favorite : FileBeanType()
}

@Composable
fun UserOptDialog(
    showUserDialog: MutableState<Boolean>,
    fileBeans: MutableList<FileBean>,
    fileIndex: MutableState<Int>,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    fileBeanType: FileBeanType = FileBeanType.SysFile
) {
    val background = Color.White
    Logcat.d("$menuOpt,$background")

    if (showUserDialog.value && (fileIndex.value < fileBeans.size)) {
        val fileBean = fileBeans[fileIndex.value]
        Dialog(
            onDismissRequest = {
                showUserDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier,
                shape = MaterialTheme.shapes.large,
                color = background,
                contentColor = MaterialTheme.colors.onSurface
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Text(
                        "File Operation",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(8.dp)
                    )
                    Divider(thickness = 1.dp)
                    DialogItem(
                        txt = stringResource(id = R.string.menu_mupdf),
                        onClick = { menuOpt(MenuItemType.ViewBookWithAMupdf, fileBean) }
                    )
                    DialogItem(
                        txt = "Mupdf new Viewer",
                        onClick = { menuOpt(MenuItemType.ViewBookWithMupdf, fileBean) }
                    )
                    DialogItem(
                        txt = stringResource(id = R.string.menu_other),
                        onClick = { menuOpt(MenuItemType.OpenWithOther, fileBean) }
                    )
                    if (fileBeanType == FileBeanType.SysFile) {
                        DialogItem(
                            txt = stringResource(id = R.string.menu_delete),
                            onClick = { menuOpt(MenuItemType.DeleteFile, fileBean) }
                        )
                    }
                    if (fileBeanType == FileBeanType.History) {
                        DialogItem(
                            txt = stringResource(id = R.string.menu_remove_from_recent),
                            onClick = { menuOpt(MenuItemType.DeleteHistory, fileBean) }
                        )
                    }
                    if (fileBeanType == FileBeanType.Favorite) {
                        if (fileBean.bookProgress!!.isFavorited == 0) {
                            DialogItem(
                                txt = stringResource(id = R.string.menu_add_to_fav),
                                onClick = { menuOpt(MenuItemType.AddToFav, fileBean) }
                            )
                        } else {
                            DialogItem(
                                txt = stringResource(id = R.string.menu_remove_from_fav),
                                onClick = { menuOpt(MenuItemType.DeleteFav, fileBean) }
                            )
                        }
                    }
                    DialogItem(
                        txt = stringResource(id = R.string.menu_info),
                        onClick = { menuOpt(MenuItemType.ViewBookInfo, fileBean) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingDialog(
    showLoadingDialog: MutableState<Boolean>,
    text: String = "Please Waiting"
) {
    val background = Color.White
    Logcat.d("$background")

    if (showLoadingDialog.value) {
        Dialog(
            onDismissRequest = {
                showLoadingDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = MaterialTheme.shapes.large,
                color = background,
                contentColor = MaterialTheme.colors.onSurface
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Text(
                        text,
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(8.dp)
                    )
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogItem(
    txt: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
        )
        Box(
            modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp)
        ) {
            if (txt != null) {
                Text(
                    text = txt,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier
                )
            }
        }
    }
}