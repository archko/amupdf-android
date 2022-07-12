package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.components.Divider
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.utils.DateUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import java.math.BigDecimal

@Composable
fun FileInfoDialog(
    showInfoDialog: MutableState<Boolean>,
    fileBean: FileBean?,
    menuOpt: (MenuItemType, FileBean) -> Unit,
) {
    val context = LocalContext.current
    if (showInfoDialog.value && fileBean != null) {
        val file = fileBean.file
        Dialog(
            onDismissRequest = {
                showInfoDialog.value = false
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
                                .padding(horizontal = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(id = R.string.menu_info),
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Divider(thickness = 1.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.label_location),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.Top)
                                .padding(2.dp)
                        )
                        Text(
                            FileUtils.getDir(file),
                            style = TextStyle(fontSize = 14.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Top)
                        )
                    }
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.label_filename),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.Top)
                                .padding(2.dp)
                        )
                        fileBean.file?.name?.let {
                            Text(
                                it,
                                style = TextStyle(fontSize = 14.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.Top)
                            )
                        }
                    }
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.label_filesize),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            Utils.getFileSize(fileBean.fileSize),
                            style = TextStyle(fontSize = 14.sp),
                        )
                    }

                    val progress = fileBean.bookProgress
                    progress?.let {
                        if (progress.pageCount > 0) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(id = R.string.label_pagecount),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                )
                                Text(
                                    "${progress.page}/${progress.pageCount}",
                                    style = TextStyle(fontSize = 14.sp),
                                )
                            }
                        }

                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(id = R.string.label_read_count),
                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            )
                            Text(
                                progress.readTimes.toString(),
                                style = TextStyle(fontSize = 14.sp),
                            )
                        }

                        if (progress.pageCount > 0) {
                            val text = DateUtils.formatTime(
                                progress.firstTimestampe,
                                DateUtils.TIME_FORMAT_TWO
                            )
                            val percent = progress.page * 100f / progress.pageCount
                            val b = BigDecimal(percent.toDouble())
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(id = R.string.label_read),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                )
                                Text(
                                    "$text      ${
                                        b.setScale(2, BigDecimal.ROUND_HALF_UP).toFloat()
                                    }%",
                                    style = TextStyle(fontSize = 14.sp),
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showInfoDialog.value = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text(
                                "Cancel",
                            )
                        }

                        Button(
                            onClick = {
                                showInfoDialog.value = false
                                menuOpt(MenuItemType.ViewBookWithAMupdf, fileBean)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text(
                                "Read",
                            )
                        }
                    }

                    fileBean.file?.path?.let {
                        if (fileBean.file!!.exists()) {
                            /*val androidImageView = remember {
                                ImageView(context).apply {
                                    ImageLoader.getInstance().loadImage(
                                        fileBean.file?.path,
                                        0,
                                        1.0f,
                                        App.instance!!.screenWidth,
                                        this
                                    )
                                }
                            }

                            AndroidView(
                                { androidImageView },
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .width(120.dp)
                                    .height(160.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                            }*/
                            Image(
                                painter = remember {
                                    AsyncPdfImagePainter(
                                        ImageWorker.DecodeParam(
                                            fileBean.file!!.path,
                                            0,
                                            1.0f,
                                            App.instance!!.screenWidth,
                                            null
                                        )
                                    )
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .width(120.dp)
                                    .height(160.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }

}
