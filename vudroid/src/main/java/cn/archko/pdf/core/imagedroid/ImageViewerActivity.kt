package cn.archko.pdf.core.imagedroid;

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import cn.archko.pdf.core.common.SensorHelper
import cn.archko.pdf.core.common.StatusBarHelper
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.decoder.CompatDecoderFactory
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import org.vudroid.R
import org.vudroid.databinding.ImageViewerBinding

class ImageViewerActivity : AppCompatActivity(R.layout.image_viewer) {

    private lateinit var binding: ImageViewerBinding
    private var addToRecent = true
    private var sensorHelper: SensorHelper? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setImmerseBarAppearance(getWindow(), true)
        sensorHelper = SensorHelper(this)

        binding = ImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path: String? = intent.getStringExtra("path")
        if (TextUtils.isEmpty(path)) {
            return
        }

        if (path!!.endsWith("tif") || path.endsWith("tiff")) {
            binding.imageView.setBitmapDecoderFactory(
                CompatDecoderFactory(
                    MupdfImageDecoder::class.java,
                    Bitmap.Config.ARGB_8888
                )
            )
            binding.imageView.setRegionDecoderFactory(
                CompatDecoderFactory(
                    MupdfPooledImageRegionDecoder::class.java,
                    Bitmap.Config.ARGB_8888
                )
            )
        } else {
            binding.imageView.setBitmapDecoderFactory(
                CompatDecoderFactory(
                    SkiaImageDecoder::class.java,
                    Bitmap.Config.ARGB_8888
                )
            )
            binding.imageView.setRegionDecoderFactory(
                CompatDecoderFactory(
                    SkiaPooledImageRegionDecoder::class.java,
                    Bitmap.Config.ARGB_8888
                )
            )
        }

        addToRecent = intent.getBooleanExtra("addToRecent", false)

        binding.imageView.setImage(ImageSource.uri(path!!))
    }

    override fun onResume() {
        super.onResume()
        sensorHelper?.onResume()
    }

    override fun onPause() {
        super.onPause()
        sensorHelper?.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}
