package cn.archko.pdf.core.imagedroid;

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cn.archko.pdf.core.common.SensorHelper
import cn.archko.pdf.core.common.StatusBarHelper
import cn.archko.pdf.core.common.ViewerPrefs
import cn.archko.pdf.core.entity.Recent
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.decoder.CompatDecoderFactory
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import org.vudroid.R
import org.vudroid.databinding.ImageViewerBinding

class ImageViewerActivity : AppCompatActivity(R.layout.image_viewer) {

    private lateinit var binding: ImageViewerBinding
    private var addToRecent = true
    private var recent: Recent? = null
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
        recent = ViewerPrefs.instance.getRecent(path)

        binding.imageView.setImage(ImageSource.uri(path!!))


        /*val descriptor = ParcelFileDescriptor.open(
            File("/storage/emulated/0/DCIM/院本清明上河图.清.陈枚等合绘.67704X2036像素.台湾故宫博物院藏[Dujin.org].tif"),
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        val opt = TiffBitmapFactory.Options()
        opt.inSampleSize = 16
        opt.inDecodeArea = DecodeArea(0, 0, 67704, 2036)
        val bitmap = TiffBitmapFactory.decodeFileDescriptor(descriptor.fd, opt)*
        val tiffImage = TiffImage()
        val imageInfo = TiffImage.getImageInfo(path)
        println(String.format("width=%s,height=%s", imageInfo.width, imageInfo.height))
        val bitmap: Bitmap = tiffImage.decode(path) //path为绝对路径
        binding.imageView.setImageBitmap(bitmap)
        tiffImage.release() //释放资源*/


        /*val descriptor =
            ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        TiffBitmapFactory.setup(descriptor.fd)
        var bitmap = TiffBitmapFactory.decodeFileDescriptor(descriptor.fd)
        binding.imageView.setImageBitmap(bitmap)
        bitmap = TiffBitmapFactory.decodeFileDescriptor(descriptor.fd)
        binding.imageView2.setImageBitmap(bitmap)*/

    }

    override fun onResume() {
        super.onResume()
        sensorHelper?.onResume()
    }

    override fun onPause() {
        super.onPause()
        sensorHelper?.onPause()
        if (recent != null && !TextUtils.isEmpty(recent!!.path) && addToRecent) {
            val savePage: Int = 0
            val lastPage: Int = 0
            recent!!.zoom = 1000F
            recent!!.page = savePage
            recent!!.pageCount = 1
            recent!!.scrollX = 0
            recent!!.scrollY = 0
            Log.d("TAG", java.lang.String.format("add recent:%s", recent))
            ViewerPrefs.instance.addRecent(recent)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}
