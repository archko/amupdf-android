package cn.archko.pdf.core.entity

/**
 * @author: archko 2024/1/15 :17:42
 */

sealed class AppException : Exception() {
    object Failed : AppException()
    object InvalidParam : AppException()
    object NoNetwork : AppException()
    object UnsolvedHost : AppException()
    object NotFound : AppException()
    object BridgeNotFound : AppException()
    object Unknown : AppException()
    object FirebaseException : AppException()
    object RetryConnectBridge : AppException()
    object ConnectException : AppException()
}