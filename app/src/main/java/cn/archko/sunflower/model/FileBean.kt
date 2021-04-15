package cn.archko.pdf.entity

import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.Serializable

class FileBean : Serializable, Cloneable {
    @SerializedName(value = "path")
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

    constructor(type: Int, file: File, label: String?) {
        this.file = file
        this.label = file.name
        isDirectory = file.isDirectory
        this.type = type
        this.label = label
        if (!isDirectory) {
        }
    }

    constructor(type: Int, file: File, showPDFExtension: Boolean) : this(
        type,
        file,
        getLabel(file, showPDFExtension)
    ) {
    }

    constructor(type: Int, label: String?) {
        this.type = type
        this.label = label
    }

    val fileSize: Long
        get() {
            return if (file != null) file!!.length() else 0
        }
    val isUpFolder: Boolean
        get() = isDirectory && label == ".."

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
                ", label=" + label +
                ", isDirectory=" + isDirectory +
                ", type=" + type +
                '}'
    }

    companion object {
        const val NORMAL = 0
        const val HOME = 1
        const val RECENT = 2
        const val FAVORITE = 3
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