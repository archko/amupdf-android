package cn.archko.pdf.utils

import android.content.Context
import android.widget.ImageView
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.decode.DjvuFetcherData
import cn.archko.pdf.decode.PdfFetcherData
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * @author: archko 2024/8/15 :22:12
 */
object FetcherUtils {
    fun load(path: String, context: Context, imageView: ImageView) {
        if (IntentFile.isDjvu(path)) {
            val req = ImageRequest.Builder(context)
                .data(
                    DjvuFetcherData(
                        path = path,
                        width = 270,
                        height = 360,
                    )
                )
                .memoryCacheKey("page_$path")
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .target(imageView)
                .build()
            ImageLoader.Builder(context).build().enqueue(req)
        } else {
            val req = ImageRequest.Builder(context)
                .data(
                    PdfFetcherData(
                        path = path,
                        width = 270,
                        height = 360,
                    )
                )
                .memoryCacheKey("page_$path")
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            ImageLoader.Builder(context).build().enqueue(req)
        }
    }
}
