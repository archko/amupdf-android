package cn.archko.pdf.core.entity

data class ReflowBean(
    var data: String?,
    var type: Int = TYPE_STRING,
    var page: String? = null
) {

    override fun toString(): String {
        return "ReflowBean(page=$page, type=$type, data=$data)"
    }

    companion object {
        @JvmField
        public val TYPE_STRING = 0;

        @JvmField
        public val TYPE_IMAGE = 1;
    }
}
