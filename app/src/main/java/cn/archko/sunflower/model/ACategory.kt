package cn.archko.sunflower.model

data class ACategory(
    val id: Long,
    val tagId: Int,
    val defaultAuthorId: Int,
    val name: String,
    val description: String,
    val bgPicture: String,
    val headerImage: String,
) {
}
