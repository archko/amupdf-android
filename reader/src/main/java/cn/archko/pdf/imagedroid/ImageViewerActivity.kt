package cn.archko.pdf.imagedroid;

import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.SensorHelper
import cn.archko.pdf.core.common.StatusBarHelper
import com.github.penfeizhou.animation.loader.FileLoader
import com.github.penfeizhou.animation.webp.WebPDrawable
import org.vudroid.R
import org.vudroid.databinding.ImageViewerBinding
import pl.droidsonroids.gif.GifDrawable

class ImageViewerActivity : AppCompatActivity(R.layout.image_viewer) {

    private lateinit var binding: ImageViewerBinding
    private var sensorHelper: SensorHelper? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setImmerseBarAppearance(getWindow(), true)
        sensorHelper = SensorHelper(this)

        binding = ImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path: String? = IntentFile.processIntentAction(intent, this)
        if (TextUtils.isEmpty(path)) {
            return
        }

        if (path!!.endsWith("gif") || path.endsWith("webp")) {
            if (path.endsWith("gif")) {
                val drawable = GifDrawable(path)
                binding.gifView.setImageDrawable(drawable)
            } else if (path.endsWith("webp")
            ) {
                val fileLoader = FileLoader(path)
                val drawable = WebPDrawable(fileLoader)
                binding.gifView.setImageDrawable(drawable)
            }
            return
        }
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
