package cn.archko.pdf.utils

import android.content.Context
import android.widget.ImageView
import cn.archko.pdf.decode.PdfFetcherData
import coil3.load

/**
 * @author: archko 2024/8/15 :22:12
 */
object FetcherUtils {
    fun load(path: String, context: Context, imageView: ImageView) {
        val fetcherData = PdfFetcherData(
            path = path,
            width = 135,
            height = 180,
        )
        imageView.load(fetcherData) {
            //error(cn.archko.mupdf.R.drawable.ic_book)
        }
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
