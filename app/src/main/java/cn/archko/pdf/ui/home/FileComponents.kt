package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.archko.mupdf.R
import cn.archko.pdf.components.Divider
import cn.archko.pdf.components.JetsnackSurface
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.theme.Typography

@Composable
fun EmptyView(modifier: Modifier) {
    JetsnackSurface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .absolutePadding(top = 80.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.loading),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .absolutePadding(bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = "Waiting...",
                style = Typography.h5,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Loading...",
                style = Typography.subtitle2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        /*Box(modifier = modifier.fillMaxSize()) {
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
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 5.dp)
                )
            }
        }*/
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
    if (showUserDialog.value && (fileIndex.value < fileBeans.size)) {
        val fileBean = fileBeans[fileIndex.value]
        Dialog(
            onDismissRequest = {
                showUserDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Row {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            "File Operation",
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Divider(thickness = 1.dp)
                    DialogItem(
                        txt = stringResource(id = R.string.menu_mupdf),
                        onClick = { menuOpt(MenuItemType.ViewBookWithAMupdf, fileBean) }
                    )
                    Divider(thickness = 0.5.dp)
                    DialogItem(
                        txt = "Mupdf new Viewer",
                        onClick = { menuOpt(MenuItemType.ViewBookWithMupdf, fileBean) }
                    )
                    Divider(thickness = 0.5.dp)
                    DialogItem(
                        txt = stringResource(id = R.string.menu_other),
                        onClick = { menuOpt(MenuItemType.OpenWithOther, fileBean) }
                    )
                    if (fileBeanType == FileBeanType.SysFile) {
                        DialogItem(
                            txt = stringResource(id = R.string.menu_delete),
                            onClick = { menuOpt(MenuItemType.DeleteFile, fileBean) }
                        )
                        Divider(thickness = 0.5.dp)
                    }
                    if (fileBeanType == FileBeanType.History) {
                        DialogItem(
                            txt = stringResource(id = R.string.menu_remove_from_recent),
                            onClick = { menuOpt(MenuItemType.DeleteHistory, fileBean) }
                        )
                        Divider(thickness = 0.5.dp)
                    }
                    if (fileBeanType == FileBeanType.Favorite) {
                        if (fileBean.bookProgress!!.isFavorited == 0) {
                            DialogItem(
                                txt = stringResource(id = R.string.menu_add_to_fav),
                                onClick = { menuOpt(MenuItemType.AddToFav, fileBean) }
                            )
                            Divider(thickness = 0.5.dp)
                        } else {
                            DialogItem(
                                txt = stringResource(id = R.string.menu_remove_from_fav),
                                onClick = { menuOpt(MenuItemType.DeleteFav, fileBean) }
                            )
                            Divider(thickness = 0.5.dp)
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
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text,
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(8.dp)
                    )
                    /*CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(20.dp)
                    )*/
                    Spacer(modifier = Modifier.height(40.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .align(alignment = Alignment.CenterHorizontally)
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
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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