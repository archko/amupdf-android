package cn.archko.pdf

import android.graphics.Bitmap
import cn.archko.pdf.core.App
import cn.archko.pdf.decode.PdfFetcher
import cn.archko.pdf.decode.PdfFetcherKeyer
import cn.archko.pdf.decode.PdfMapper
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

/**
 * @author: archko 2024/8/15 :22:05
 */
class PdfApp : App(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        var directory = externalCacheDir?.resolve("image_cache")
        if (directory == null) {
            directory = cacheDir.resolve("image_cache")
        }
        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowRgb565(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, MAX_MEMORY_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(directory.toOkioPath())
                    .maxSizePercent(MAX_DISK_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .components {
                add(PdfMapper())
                add(PdfFetcherKeyer())
                add(PdfFetcher.Factory())
            }
            .build()
    }

    public companion object {
        public var app: PdfApp? = null
            private set

        //一张图片4-5mb,200mb大概缓存50张
        public const val MAX_CACHE: Int = 300 * 1024 * 1024
        private val MAX_MEMORY_CACHE_SIZE_PERCENTAGE = 0.3
        private val MAX_DISK_CACHE_SIZE_PERCENTAGE = 0.2
    }
}