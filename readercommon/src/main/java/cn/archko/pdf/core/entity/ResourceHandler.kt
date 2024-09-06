package cn.archko.pdf.core.entity

/**
 * @author: archko 2024/9/6 :09:35
 */
sealed class ResponseHandler<out T> {
    /* Success response with data */
    data class Success<out T>(val data: T) : ResponseHandler<T>()

    /* Failed Response with an exception and message */
    data class Failure(val error: Throwable? = AppException.Unknown, val extra: String? = "") :
        ResponseHandler<Nothing>()

    /** Function had already called before and the previous one is executing */
    object Loading : ResponseHandler<Nothing>()
    object DoNothing : ResponseHandler<Nothing>()
}