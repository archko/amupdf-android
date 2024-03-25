package cn.archko.pdf.core.entity

import android.text.TextUtils
import cn.archko.pdf.core.utils.FileUtils
import java.io.File
import java.io.Serializable
import java.util.Locale

class FileBean : Serializable, Cloneable {
    var label: String? = null
        private set
    var file: File? = null
        private set
    var isDirectory = false
        private set

    /**
     * bean type.
     */
    var type = NORMAL
        private set
    var bookProgress: BookProgress? = null

    constructor(type: Int, file: File, label: String?) {
        this.file = file
        this.label = file.name
        isDirectory = file.isDirectory
        this.type = type
        this.label = label
        if (!isDirectory) {
            if (null == bookProgress) {
                bookProgress = BookProgress(FileUtils.getRealPath(file.absolutePath))
            }

            bookProgress!!.size = file.length()
        }
    }

    constructor(type: Int, file: File, showPDFExtension: Boolean) : this(
        type,
        file,
        getLabel(file, showPDFExtension)
    )

    constructor(bookProgress: BookProgress, type: Int) {
        this.bookProgress = bookProgress
        this.type = type
        isDirectory = false
        label = bookProgress.name
    }

    constructor(bookProgress: BookProgress, type: Int, path: String) {
        this.bookProgress = bookProgress
        this.type = type
        isDirectory = false
        label = bookProgress.name
        if (!TextUtils.isEmpty(path)) {
            file = File(path)
            bookProgress.size = file!!.length()
        }
    }

    constructor(type: Int, label: String?) {
        this.type = type
        this.label = label
    }

    val fileSize: Long
        get() {
            if (null != bookProgress) {
                return bookProgress!!.size
            }
            return if (file != null) file!!.length() else 0
        }
    val isUpFolder: Boolean
        get() = isDirectory && label == ".."

    fun isImage(): Boolean {
        if (null != file) {
            val ext: String = bookProgress!!.ext!!.lowercase(Locale.ROOT)
            if (ext.contains("jpg") || ext.contains("png") || ext.contains("jpeg")) {
                return true
            }
        }
        return false
    }

    public override fun clone(): FileBean {
        try {
            return super.clone() as FileBean
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }
        return FileBean(NORMAL, null)
    }

    override fun toString(): String {
        return "FileBean{" +
                ", isDirectory=" + isDirectory +
                ", type=" + type +
                ", mBookProgress=" + bookProgress +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileBean

        if (label != other.label) return false
        if (file != other.file) return false
        if (isDirectory != other.isDirectory) return false
        if (type != other.type) return false
        if (bookProgress != other.bookProgress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + isDirectory.hashCode()
        result = 31 * result + type
        result = 31 * result + (bookProgress?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val NORMAL = 0
        const val HOME = 1
        const val RECENT = 2
        const val FAVORITE = 3
        const val CURRENT = 4
        private fun getLabel(file: File, showPDFExtension: Boolean): String {
            val label = file.name
            return if (!showPDFExtension && label.length > 4 && !file.isDirectory /*&& label.substring(label.length()-4, label.length()).equalsIgnoreCase(".pdf")*/) {
                label.substring(0, label.length - 4)
            } else {
                label
            }
        }
    }
}