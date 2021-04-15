package cn.archko.pdf.entity

data class ReflowBean(var data: String?, var type: Int = TYPE_STRING) {

    override fun toString(): String {
        return "ReflowBean(type=$type, data=$data)"
    }

    companion object {
        @JvmField
        public val TYPE_STRING = 0;

        @JvmField
        public val TYPE_IMAGE = 1;
    }
}
