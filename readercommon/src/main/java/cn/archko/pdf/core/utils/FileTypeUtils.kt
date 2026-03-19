package cn.archko.pdf.core.utils

import cn.archko.pdf.core.utils.FileTypeUtils.MAX_SIZE_MB
import java.io.File

/**
 * 文件类型判断工具类
 * @author: archko 2025/1/20
 */
object FileTypeUtils {

    private const val MAX_SIZE_MB = 120 * 1024 * 1024L

    /**
     * 判断是否为图片文件
     * Android支持的图片格式：JPEG, PNG, GIF, BMP, WebP, HEIF, HEIC
     */
    fun isImageFile(path: String): Boolean {
        return isSupportedImageFile(path)
    }

    /**
     * 判断是否为有效的图片文件（包括大小判断）
     * @param file 文件对象
     * @param MAX_SIZE_MB 最大文件大小（MB）
     * @return 是否为有效的图片文件
     */
    fun isValidImageFile(file: File): Boolean {
        return file.exists()
                && file.isFile
                && isImageFile(file.absolutePath)
                && file.length() <= MAX_SIZE_MB
    }

    fun isAccetableImageFile(file: File): Boolean {
        return file.exists()
                && file.isFile
                && isImageFile(file.absolutePath)
    }

    /**
     * 检测文件是否为HEIF格式
     */
    fun isHeifFormat(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension == "heic" || extension == "heif"
    }

    /**
     * 判断是否为文档文件
     */
    fun isDocumentFile(path: String): Boolean {
        return path.lowercase().let { filePath ->
            filePath.endsWith(".pdf") || filePath.endsWith(".epub") ||
                    filePath.endsWith(".mobi") || filePath.endsWith(".xps") ||
                    filePath.endsWith(".fb") || filePath.endsWith(".fb2") ||
                    filePath.endsWith(".pptx") || filePath.endsWith(".docx") ||
                    filePath.endsWith(".djvu") || filePath.endsWith(".djv") ||
                    filePath.endsWith(".svg")
        }
    }

    fun isTiffFile(path: String): Boolean {
        return path.lowercase().let { filePath ->
            filePath.endsWith(".jfif") || filePath.endsWith(".tiff")
                    || filePath.endsWith(".tif")
        }
    }

    fun isDjvuFile(path: String): Boolean {
        return path.lowercase().let { filePath ->
            filePath.endsWith(".djvu") || filePath.endsWith(".djv")
        }
    }

    /**
     * 判断是否应该保存进度
     * 只有单文档文件才保存进度
     */
    fun shouldSaveProgress(paths: List<String>): Boolean {
        return paths.size == 1 && isDocumentFile(paths.first())
    }

    /**
     * 判断是否应该显示大纲功能
     * 只有单文档文件才显示大纲
     */
    fun shouldShowOutline(paths: List<String>): Boolean {
        return paths.size == 1 && isDocumentFile(paths.first())
    }

    /**
     * 过滤文件列表，移除大于指定大小的文件
     * @param files 文件列表
     * @param MAX_SIZE_MB 最大文件大小（MB）
     * @return 过滤后的文件列表
     */
    fun filterFilesBySize(files: List<File>): List<File> {
        return files.filter { file ->
            file.exists()
                    && file.length() <= MAX_SIZE_MB
        }
    }

    fun isReflowable(path: String): Boolean {
        return path.endsWith(".cbz", true)
                || path.endsWith(".epub", true)
                || path.endsWith(".mobi", true)
                || path.endsWith(".pptx", true)
                || path.endsWith(".docx", true)
                || path.endsWith(".xlsx", true)
    }

    fun isSupportedImageForCreater(path: String): Boolean {
        return path.endsWith(".jpg", true)
                || path.endsWith(".jpeg", true)
                || path.endsWith(".gif", true)
    }

    fun isSupportedImageFile(path: String): Boolean {
        return true
    }
}
