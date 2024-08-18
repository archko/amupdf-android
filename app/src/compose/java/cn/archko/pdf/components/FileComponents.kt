package cn.archko.pdf.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.utils.FileUtils

@Composable
fun EmptyView(modifier: Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .absolutePadding(top = 80.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .absolutePadding(bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = stringResource(id = R.string.empty_title),
                style = androidx.compose.material3.Typography().headlineLarge,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.empty_subtitle),
                style = androidx.compose.material3.Typography().titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

sealed class MenuItemType {
    object ViewBookWithAMupdf : MenuItemType()
    //object ViewBookWithVudroid : MenuItemType()
    //object ViewBookWithNewViewer : MenuItemType()
    object ViewBookInfo : MenuItemType()
    object DeleteHistory : MenuItemType()
    object DeleteFile : MenuItemType()
    object AddToFav : MenuItemType()
    object DeleteFav : MenuItemType()
    object OpenWithOther : MenuItemType()
    //object Compress : MenuItemType()
    //object EncryptPDF : MenuItemType()
    //object DecryptPDF : MenuItemType()
    //object ConvertToPDF : MenuItemType()
}

sealed class FileBeanType {
    object SysFile : FileBeanType()
    object History : FileBeanType()
    object Favorite : FileBeanType()
}

val searchTypeFile = 0
val searchTypeHistory = 1
val searchTypeFavorite = 2

@Composable
fun UserOptDialog(
    showUserDialog: MutableState<Boolean>,
    fileBean: FileBean?,
    menuOpt: (MenuItemType, FileBean) -> Unit,
    fileBeanType: FileBeanType = FileBeanType.SysFile
) {
    if (showUserDialog.value && fileBean != null) {
        Dialog(
            onDismissRequest = {
                showUserDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier,
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
                    HorizontalDivider(thickness = 1.dp)
                    DialogItem(
                        txt = stringResource(id = R.string.menu_mupdf),
                        onClick = { menuOpt(MenuItemType.ViewBookWithAMupdf, fileBean) }
                    )
                    HorizontalDivider(thickness = 1.dp)
                    /*DialogItem(
                        txt = stringResource(id = R.string.menu_new_mupdf),
                        onClick = { menuOpt(MenuItemType.ViewBookWithNewViewer, fileBean) }
                    )*/
                    HorizontalDivider(thickness = 0.5.dp)
                    DialogItem(
                        txt = stringResource(id = R.string.menu_other),
                        onClick = { menuOpt(MenuItemType.OpenWithOther, fileBean) }
                    )
                    if (fileBeanType == FileBeanType.SysFile) {
                        DialogItem(
                            txt = stringResource(id = R.string.menu_delete),
                            onClick = { menuOpt(MenuItemType.DeleteFile, fileBean) }
                        )
                        /*Divider(thickness = 0.5.dp)
                        DialogItem(
                            txt = stringResource(id = com.radaee.viewlib.R.string.compress_pdf_label),
                            onClick = { menuOpt(MenuItemType.Compress, fileBean) }
                        )
                        Divider(thickness = 0.5.dp)
                        if (fileBean.isImage()) {
                            DialogItem(
                                txt = stringResource(id = com.radaee.viewlib.R.string.convert_pdf_label),
                                onClick = { menuOpt(MenuItemType.ConvertToPDF, fileBean) }
                            )
                            Divider(thickness = 0.5.dp)
                        }*/
                    }
                    if (fileBeanType == FileBeanType.History) {
                        DialogItem(
                            txt = stringResource(id = R.string.menu_remove_from_recent),
                            onClick = { menuOpt(MenuItemType.DeleteHistory, fileBean) }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                    if (null != fileBean.file) {
                        var bookProgress = fileBean.bookProgress
                        if (null == bookProgress) {
                            bookProgress =
                                BookProgress(FileUtils.getRealPath(fileBean.file!!.absolutePath))
                        }
                        if (bookProgress.isFavorited == 0) {
                            DialogItem(
                                txt = stringResource(id = R.string.menu_add_to_fav),
                                onClick = { menuOpt(MenuItemType.AddToFav, fileBean) }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        } else {
                            DialogItem(
                                txt = stringResource(id = R.string.menu_remove_from_fav),
                                onClick = { menuOpt(MenuItemType.DeleteFav, fileBean) }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
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
fun LoadingView(
    showLoading: MutableState<Boolean>,
    text: String = "Please Waiting"
) {
    if (showLoading.value) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
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
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(20.dp)
                )
                /*Spacer(modifier = Modifier.height(40.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .height(18.dp)
                        .align(alignment = Alignment.CenterHorizontally)
                )*/
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
                    modifier = Modifier
                )
            }
        }
    }
}