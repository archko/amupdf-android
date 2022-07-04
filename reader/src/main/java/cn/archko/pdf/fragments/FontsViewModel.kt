package cn.archko.pdf.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

    suspend fun loadFonts() = flow {
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
            val fname: String = file.name.lowercase(Locale.getDefault())

            if (fname.endsWith(".ttf", true))
                return@FileFilter true
            if (fname.endsWith(".ttc", true))
                return@FileFilter true
            false
        })?.map {
            list.add(FontBean(PdfOptionRepository.CUSTOM, it.name, it))
        }
        emit(list)
    }.flowOn(Dispatchers.IO)
}