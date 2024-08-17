package cn.archko.pdf.decode

/**
 * @author: archko 2024/8/16 :08:35
 */
data class PdfFetcherData(
    val path: String,
    val width: Int,
    val height: Int,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PdfFetcherData

        if (path != other.path) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        return "PdfFetcherData(path='$path', width=$width, height=$height)"
    }

}