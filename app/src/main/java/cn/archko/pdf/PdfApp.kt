package cn.archko.pdf

import cn.archko.pdf.core.App
import cn.archko.pdf.decode.DjvuFetcher
import cn.archko.pdf.decode.PdfFetcher
import coil.ComponentRegistry
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * @author: archko 2024/8/15 :22:05
 */
class PdfApp : App(), ImageLoaderFactory {

    private val MAX_MEMORY_CACHE_SIZE_PERCENTAGE = 0.3
    private val MAX_DISK_CACHE_SIZE_PERCENTAGE = 0.2

    override fun newImageLoader(): ImageLoader {
        val imageLoader = ImageLoader.Builder(this)
            .components(fun ComponentRegistry.Builder.() {
                add(PdfFetcher.Factory())
                add(DjvuFetcher.Factory())
                //add(MupdfFetcher.Factory())
            })
            .allowRgb565(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MAX_MEMORY_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(MAX_DISK_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .build()
        return imageLoader
    }
}