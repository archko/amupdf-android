package cn.archko.pdf.utils

import android.content.Context
import android.widget.ImageView
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.decode.PdfFetcherData
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * @author: archko 2024/8/15 :22:12
 */
object FetcherUtils {
    fun load(path: String, context: Context, imageView: ImageView) {
        val fetcherData = PdfFetcherData(
            path = path,
            width = Utils.dipToPixel(135f),
            height = Utils.dipToPixel(180f),
        )
        imageView.load(fetcherData)
        /*val req = ImageRequest.Builder(context)
            .data(fetcherData)
            .memoryCacheKey("page_$path")
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .target(imageView)
            .build()
        ImageLoader.Builder(context).build().enqueue(req)*/
    }
}
