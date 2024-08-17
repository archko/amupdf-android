package cn.archko.pdf.utils

import android.content.Context
import android.widget.ImageView
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.decode.PdfFetcherData
import coil.load

/**
 * @author: archko 2024/8/15 :22:12
 */
object FetcherUtils {
    fun load(path: String, context: Context, imageView: ImageView) {
        imageView.load(
            PdfFetcherData(
                path = path,
                width = Utils.dipToPixel(135f),
                height = Utils.dipToPixel(180f),
            )
        )
        /*val req = ImageRequest.Builder(context)
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
            .target(imageView)
            .build()
        ImageLoader.Builder(context).build().enqueue(req)*/
    }
}
