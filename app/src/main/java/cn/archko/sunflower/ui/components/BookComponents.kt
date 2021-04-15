package cn.archko.sunflower.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.archko.pdf.entity.FileBean
import cn.archko.sunflower.ui.utils.JsonUtils
import cn.archko.sunflower.R
import cn.archko.sunflower.ui.utils.Screen
import com.google.accompanist.insets.statusBarsPadding
import com.google.gson.reflect.TypeToken

@Composable
fun BookComponents(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
) {
    Canvas(
        modifier.focusable()
    ) {
        drawLinearIndicator(color, progress)
    }
}

private fun DrawScope.drawLinearIndicator(
    color: Color,
    progress: Float,
) {
    val width = size.width
    val height = size.height
    val w = width * progress / 100F
    drawRect(color, Offset.Zero, Size(width = w, height = height))
}

@ExperimentalMaterialApi
@Composable
fun BookSheet(
    fileBean: String,
    navigateTo: (Screen) -> Unit,
) {
    val fBean: FileBean = JsonUtils.fromJson(fileBean, object : TypeToken<FileBean?>() {}.type)
    BookSheet(fBean, navigateTo)
}

@ExperimentalMaterialApi
@Composable
fun BookSheet(
    fileBean: FileBean,
    navigateTo: (Screen) -> Unit,
) {
    val bottomSheetScaffold = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )

    BottomSheetScaffold(
        sheetPeekHeight = 252.dp,
        scaffoldState = bottomSheetScaffold,
        sheetContent = {
            Box(
                modifier = Modifier
                    .padding(0.dp)
                    .statusBarsPadding()
                    .fillMaxSize()
                    .background(Color.White)

            ) {
                Column(modifier = Modifier.padding(2.dp)) {
                    SheetItem(txt = fileBean.label, navigateTo = navigateTo)
                    SheetItem(txt = fileBean.isDirectory.toString(), navigateTo = navigateTo)
                    SheetItem(txt = fileBean.label, navigateTo = navigateTo)
                    SheetItem(txt = fileBean.label, navigateTo = navigateTo)
                    SheetItem(txt = fileBean.label, navigateTo = navigateTo)
                }
            }
        }) {}
}

@Composable
private fun SheetItem(
    txt: String?,
    navigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clickable(onClick = { navigateTo(Screen.FileList) })
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.empty_state_search),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
        )
        Box(
            modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    paddingValues = PaddingValues(
                        top = 2.dp,
                        start = 4.dp,
                        end = 8.dp,
                    )
                )
                .height(36.dp)
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
