//package cn.archko.pdf.core.imagedroid
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Point
//import android.graphics.Rect
//import android.net.Uri
//import android.util.Log
//import androidx.annotation.Keep
//import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
//import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
//
///**
// *
// */
//class TiffPooledImageRegionDecoder @Keep constructor(bitmapConfig: Bitmap.Config?) : ImageRegionDecoder {
//    private val bitmapConfig: Bitmap.Config?
//
//    private var context: Context? = null
//    private var uri: Uri? = null
//
//    private val imageDimensions = Point(0, 0)
//    private var decoder: TiffNewImageRegionDecoder? = null
//
//    @Keep
//    constructor() : this(null)
//
//    init {
//        val globalBitmapConfig = SubsamplingScaleImageView.getPreferredBitmapConfig()
//        if (bitmapConfig != null) {
//            this.bitmapConfig = bitmapConfig
//        } else if (globalBitmapConfig != null) {
//            this.bitmapConfig = globalBitmapConfig
//        } else {
//            this.bitmapConfig = Bitmap.Config.RGB_565
//        }
//    }
//
//    @Throws(Exception::class)
//    override fun init(context: Context?, uri: Uri): Point {
//        this.context = context
//        this.uri = uri
//        initialiseDecoder()
//        return this.imageDimensions
//    }
//
//    private fun lazyInit() {
//        debug("Starting lazy init of additional decoders")
//        try {
//            if (null == decoder) {
//                val start = System.currentTimeMillis()
//                debug("Starting decoder")
//                initialiseDecoder()
//                val end = System.currentTimeMillis()
//                debug("Started decoder, took " + (end - start) + "ms")
//            }
//        } catch (e: Exception) {
//            debug("Failed to start decoder: " + e.message)
//        }
//    }
//
//    @Throws(Exception::class)
//    private fun initialiseDecoder() {
//        decoder = TiffNewImageRegionDecoder()
//        decoder!!.init(context, uri!!)
//
//        //this.fileLength = decoder.getFileLength();
//        this.imageDimensions.set(decoder!!.width, decoder!!.height)
//    }
//
//    /**
//     * Acquire a read lock to prevent decoding overlapping with recycling, then check the pool still
//     * exists and acquire a decoder to load the requested region. There is no check whether the pool
//     * currently has decoders, because it's guaranteed to have one decoder after [.init]
//     * is called and be null once [.recycle] is called. In practice the view can't call this
//     * method until after [.init], so there will be no blocking on an empty pool.
//     */
//    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap? {
//        debug("Decode region $sRect")
//        if (sRect.width() < imageDimensions.x || sRect.height() < imageDimensions.y) {
//            lazyInit()
//        }
//        try {
//            if (decoder != null) {
//                val bitmap = decoder!!.decodeRegion(sRect, sampleSize)
//                if (bitmap == null) {
//                    debug("Tiff image decoder returned null bitmap - image format may not be supported")
//                }
//                return bitmap
//            }
//        } catch (e: Exception) {
//            debug(String.format("Failed to decode:%s-%s, %s ", sRect, sampleSize, e.message))
//        }
//        return null
//    }
//
//    @Synchronized
//    override fun isReady(): Boolean {
//        return decoder != null
//    }
//
//    @Synchronized
//    override fun recycle() {
//        if (decoder != null) {
//            decoder!!.recycle()
//            decoder = null
//            context = null
//            uri = null
//        }
//    }
//
//    private fun debug(message: String?) {
//        if (debug) {
//            Log.d(TAG, message!!)
//        }
//    }
//
//    companion object {
//        private val TAG: String = TiffPooledImageRegionDecoder::class.java.getSimpleName()
//
//        private var debug = false
//
//        @Keep
//        @Suppress("unused")
//        fun setDebug(debug: Boolean) {
//            Companion.debug = debug
//        }
//    }
//}
