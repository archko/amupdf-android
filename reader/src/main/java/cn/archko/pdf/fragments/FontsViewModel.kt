package cn.archko.pdf.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileFilter
import java.util.*

/**
 * @author: archko 2020/11/16 :11:23
 */
class FontsViewModel(private var optionRepository: PdfOptionRepository) : ViewModel() {
    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = optionRepository.pdfOptionFlow.first()

                selectedFontName = data.fontName
            }
        }
    }

    var selectedFontName: String? = null

    private val _uiFontsModel = MutableLiveData<List<FontBean>>()
    val uiFontModel: LiveData<List<FontBean>>
        get() = _uiFontsModel

    fun loadFonts() =
        viewModelScope.launch {

            val list: MutableList<FontBean> = withContext(Dispatchers.IO) {
                val fontDir = FileUtils.getStorageDir(PdfOptionRepository.FONT_DIR)
                val list = ArrayList<FontBean>()

                list.add(
                    FontBean(
                        PdfOptionRepository.DEFAULT,
                        PdfOptionRepository.SYSTEM_FONT,
                        null
                    )
                )
                list.add(
                    FontBean(
                        PdfOptionRepository.SANS_SERIF,
                        PdfOptionRepository.SYSTEM_FONT_SAN,
                        null
                    )
                )
                list.add(
                    FontBean(
                        PdfOptionRepository.SERIF,
                        PdfOptionRepository.SYSTEM_FONT_SERIF,
                        null
                    )
                )
                list.add(
                    FontBean(
                        PdfOptionRepository.MONOSPACE,
                        PdfOptionRepository.SYSTEM_FONT_MONO,
                        null
                    )
                )
                fontDir.listFiles(FileFilter { file ->
                    if (file.isDirectory)
                        return@FileFilter false
                    val fname: String = file.name.toLowerCase(Locale.ROOT)

                    if (fname.endsWith(".ttf", true))
                        return@FileFilter true
                    if (fname.endsWith(".ttc", true))
                        return@FileFilter true
                    false
                })?.map {
                    list.add(FontBean(PdfOptionRepository.CUSTOM, it.name, it))
                }
                return@withContext list
            }

            withContext(Dispatchers.Main) {
                _uiFontsModel.value = list
            }
        }
}