//package cn.archko.pdf.common
//
//import android.graphics.Bitmap
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.RememberObserver
//import androidx.compose.runtime.snapshotFlow
//import cn.archko.pdf.core.common.AppExecutors
//import cn.archko.pdf.core.decode.DecodeParam
//import cn.archko.pdf.core.decode.MupdfDocument
//import cn.archko.pdf.core.entity.APage
//import cn.archko.pdf.core.entity.ReflowBean
//import cn.archko.pdf.core.utils.BitmapUtils
//import cn.archko.pdf.core.utils.FileUtils
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.asCoroutineDispatcher
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import java.io.File
//
///**
// * 查看pdf信息时的缩略图
// */
//class AsyncPdfImagePainter internal constructor(
//    request: DecodeParam,
//) : AbsAsyncPdfPainter(request), RememberObserver {
//
//    override fun decode(decodeParam: DecodeParam): Bitmap? {
//        var bitmap: Bitmap? = null
//        try {
//            val thumb =
//                FileUtils.getDiskCacheDir(AppExecutors.instance, FileUtils.getRealPath(decodeParam.key))
//            bitmap = ImageLoader.decodeFromFile(thumb)
//            if (null == bitmap) {
//                val file = File(decodeParam.key)
//                if (file.exists()) {
//                    bitmap = ImageLoader.decodeFromPDF(
//                        decodeParam.key,
//                        decodeParam.pageNum,
//                        decodeParam.zoom,
//                        decodeParam.screenWidth
//                    )
//                    if (bitmap != null) {
//                        BitmapUtils.saveBitmapToFile(bitmap, thumb)
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        return bitmap
//    }
//}
//
//@Composable
//fun AsyncDecodePage(
//    aPage: APage,
//    crop: Boolean = true,
//    mupdfDocument: MupdfDocument,
//    imageState: MutableState<Bitmap?>
//) {
//    DisposableEffect(aPage.getTargetWidth()) {
//        val decodeParam = ImageWorker.DecodeParam(
//            aPage.toString(),
//            crop,
//            0,
//            aPage,
//            mupdfDocument.document,
//        )
//        val scope =
//            CoroutineScope(SupervisorJob() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
//        scope.launch {
//            snapshotFlow {
//                if (isActive) {
//                    PdfImageDecoder.decode(decodeParam)
//                } else {
//                    return@snapshotFlow null
//                }
//            }.flowOn(AppExecutors.instance.diskIO().asCoroutineDispatcher())
//                .collectLatest {
//                    imageState.value = it
//                }
//        }
//        onDispose {
//            scope.cancel()
//        }
//    }
//}
//
//@Composable
//fun AsyncDecodeTextPage(
//    aPage: APage,
//    mupdfDocument: MupdfDocument,
//    imageState: MutableState<List<ReflowBean>?>
//) {
//    DisposableEffect(aPage.index) {
//        val scope =
//            CoroutineScope(SupervisorJob() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
//        scope.launch {
//            snapshotFlow {
//                return@snapshotFlow mupdfDocument.decodeReflow(aPage.index)
//            }.flowOn(AppExecutors.instance.diskIO().asCoroutineDispatcher())
//                .collectLatest {
//                    imageState.value = it
//                }
//        }
//        onDispose {
//            scope.cancel()
//        }
//    }
//}