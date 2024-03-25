package cn.archko.pdf.fragments

import androidx.lifecycle.ViewModel
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.FileFilter
import java.util.Locale

/**
 * @author: archko 2020/11/16 :11:23
 */
class FontsViewModel : ViewModel() {

    var selectedFontName: String? = null
    init {
        selectedFontName = PdfOptionRepository.getFontName()
    }

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