package cn.archko.pdf.core.sardine

/**
 * @author: archko 2024/9/6 :10:06
 */
data class WebdavUser(
    val name: String,
    val pass: String,
    val host: String,
    val path: String
) {
    override fun toString(): String {
        return "WebdavUser(name='$name', pass='$pass', host='$host', path='$path')"
    }
}
