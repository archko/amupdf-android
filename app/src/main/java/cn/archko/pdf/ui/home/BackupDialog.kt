package cn.archko.pdf.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.archko.mupdf.R
import cn.archko.pdf.components.Divider
import cn.archko.pdf.paging.State
import cn.archko.pdf.viewmodel.FileViewModel
import java.io.File

@Composable
fun RestoreDialog(
    showRestoreDialog: MutableState<Boolean>,
    viewModel: FileViewModel,
    onSelectFile: (File) -> Unit,
) {
    val uiBackupFiles by viewModel.uiBackupFileModel.collectAsState()

    if (uiBackupFiles.state == State.INIT) {
        viewModel.loadBackupFiles()
    }

    if (showRestoreDialog.value) {
        Dialog(
            onDismissRequest = {
                showRestoreDialog.value = false
            }
        ) {
            Surface(
                modifier = Modifier,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Row {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            "History Backups",
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Divider(thickness = 1.dp)
                    LazyColumn() {
                        itemsIndexed(uiBackupFiles.list!!) { index, file ->
                            if (index > 0) {
                                Divider(thickness = 0.5.dp)
                            }
                            DialogItem(
                                file = file,
                                onSelectFile = onSelectFile,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogItem(
    file: File,
    onSelectFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clickable(onClick = { onSelectFile(file) })
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp)
        ) {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                modifier = Modifier
            )
        }
    }
}