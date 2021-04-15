data class GankResponse<T>(
    var page: Int,
    var page_count: Int,
    var status: Int,
    var total_counts: Int,
    var hot: String,
    var category: String,
    var data: T,
) {
}
