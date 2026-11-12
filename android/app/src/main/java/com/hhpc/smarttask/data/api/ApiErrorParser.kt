package com.hhpc.smarttask.data.api

import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Bộ xử lý và chuẩn hóa lỗi API cho toàn bộ ứng dụng Android.
 * Đảm bảo mọi mã trạng thái HTTP hoặc ngoại lệ kết nối mạng đều được chuyển đổi thành
 * thông điệp tiếng Việt rõ ràng, thân thiện, không in ra các con số mã lỗi kỹ thuật khó hiểu (400, 401, 500...).
 */
object ApiErrorParser {

    /**
     * Bóc tách thông báo lỗi từ Retrofit Response.
     */
    fun <T> parseError(response: Response<T>, defaultMessage: String = "Thao tác không thành công"): String {
        val rawBody = try {
            response.errorBody()?.string()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }

        // 1. Phân tích nội dung phản hồi từ máy chủ nếu có JSON message
        if (rawBody.isNotBlank()) {
            try {
                if (rawBody.startsWith("{") && rawBody.endsWith("}")) {
                    val json = JsonParser.parseString(rawBody).asJsonObject
                    val msg = json.get("message")?.takeIf { !it.isJsonNull }?.asString
                        ?: json.get("error")?.takeIf { !it.isJsonNull }?.asString

                    if (!msg.isNullOrBlank() && !isTechnicalError(msg)) {
                        return sanitizeMessage(msg)
                    }
                } else if (!rawBody.startsWith("<") && rawBody.length < 200 && !isTechnicalError(rawBody)) {
                    return sanitizeMessage(rawBody)
                }
            } catch (ignored: Exception) {
            }
        }

        // 2. Phân loại theo mã trạng thái HTTP chuẩn sang tiếng Việt thân thiện
        return when (response.code()) {
            400 -> "Thông tin yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu đã nhập."
            401 -> "Phiên làm việc đã hết hạn hoặc thông tin đăng nhập không chính xác. Vui lòng thử lại."
            403 -> "Bạn không có quyền thực hiện thao tác này hoặc tài khoản đang bị hạn chế."
            404 -> "Không tìm thấy dữ liệu yêu cầu hoặc nội dung đã bị xóa."
            409 -> "Dữ liệu này đã tồn tại trong hệ thống. Vui lòng sử dụng thông tin khác."
            422 -> "Dữ liệu cung cấp chưa đầy đủ hoặc không đúng quy định xử lý."
            429 -> "Hệ thống đang nhận nhiều lượt truy cập cùng lúc. Vui lòng đợi trong giây lát."
            500, 502, 503, 504 -> "Hệ thống máy chủ đang gặp sự cố tạm thời. Vui lòng thử lại sau ít phút."
            else -> defaultMessage
        }
    }

    /**
     * Chuyển đổi ngoại lệ mạng hoặc thiết bị sang câu tiếng Việt tự nhiên.
     */
    fun parseException(throwable: Throwable, defaultMessage: String = "Đã xảy ra lỗi không mong muốn"): String {
        return when (throwable) {
            is UnknownHostException, is ConnectException ->
                "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng của bạn."
            is SocketTimeoutException ->
                "Thời gian phản hồi từ máy chủ quá lâu. Vui lòng thử lại sau ít phút."
            is IOException ->
                "Lỗi kết nối đường truyền mạng. Vui lòng kiểm tra lại mạng thiết bị."
            else -> {
                val msg = throwable.localizedMessage ?: throwable.message ?: ""
                if (msg.isNotBlank() && !isTechnicalError(msg)) {
                    sanitizeMessage(msg)
                } else {
                    defaultMessage
                }
            }
        }
    }

    /**
     * Bóc tách lỗi đăng nhập Google Credential Manager trên Android thành thông báo tự nhiên.
     */
    fun parseGoogleAuthException(throwable: Throwable): String {
        val className = throwable.javaClass.simpleName
        return when {
            className.contains("NoCredentialException") ->
                "Không tìm thấy tài khoản Google khả dụng trên thiết bị. Vui lòng đăng nhập tài khoản Google vào máy trước."
            className.contains("GetCredentialCustomException") ->
                "Dịch vụ Google Play chưa sẵn sàng hoặc cần được cập nhật trên thiết bị này."
            className.contains("GetCredentialProviderConfigurationException") ->
                "Dịch vụ đăng nhập Google chưa được cấu hình hoàn tất trên máy."
            else -> {
                val msg = throwable.localizedMessage ?: throwable.message ?: ""
                if (msg.isNotBlank() && !isTechnicalError(msg)) {
                    sanitizeMessage(msg)
                } else {
                    "Đăng nhập bằng tài khoản Google không thành công. Vui lòng thử lại sau ít phút."
                }
            }
        }
    }

    private fun isTechnicalError(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("internal server error") ||
                lower.contains("bad request") ||
                lower.contains("unauthorized") ||
                lower.contains("forbidden") ||
                lower.contains("not found") ||
                lower.contains("status code") ||
                lower.contains("exception") ||
                lower.contains("failed to connect") ||
                lower.contains("failed_reply") ||
                lower.contains("nullpointer") ||
                lower.contains("sql") ||
                lower.contains("org.springframework") ||
                lower.contains("java.")
    }

    private fun sanitizeMessage(msg: String): String {
        return msg.replace(Regex("\\(?[Mm]ã\\s*lỗi\\s*[:=]?\\s*\\d+\\)?"), "")
            .replace(Regex("\\(?[Ll]ỗi\\s*\\d+\\)?"), "")
            .replace(Regex("[Ss]tatus\\s*[Cc]ode\\s*[:=]?\\s*\\d+"), "")
            .replace(Regex("\\bHTTP\\s*\\d{3}\\b", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { "Thao tác không thành công. Vui lòng thử lại sau ít phút." }
    }
}
