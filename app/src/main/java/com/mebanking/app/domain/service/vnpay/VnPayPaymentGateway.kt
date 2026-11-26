package com.mebanking.app.domain.service.vnpay

import android.net.Uri
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class VnPayConfig(
    val tmnCode: String = DEFAULT_DEMO_TMN_CODE,
    val hashSecret: String = DEFAULT_DEMO_HASH_SECRET,
    val paymentBaseUrl: String = SANDBOX_PAYMENT_URL,
    val returnUrl: String = DEFAULT_RETURN_URL,
) {
    companion object {
        const val SANDBOX_PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        const val DEFAULT_RETURN_URL = "https://mebanking.local/vnpay_return"
        const val DEFAULT_DEMO_TMN_CODE = "MEBANK01"
        const val DEFAULT_DEMO_HASH_SECRET = "MEBANKING_VNPAY_SANDBOX_SECRET_2026"
    }
}

sealed interface VnPayReturnResult {
    data class Success(
        val txnRef: String,
        val amountVnd: Long,
        val bankCode: String,
        val transactionNo: String,
        val orderInfo: String,
    ) : VnPayReturnResult

    data class Failed(
        val responseCode: String,
        val message: String,
    ) : VnPayReturnResult

    data class InvalidSignature(
        val reason: String,
    ) : VnPayReturnResult
}

/**
 * VNPay 2.1.0 Payment URL builder & HMAC-SHA512 return-URL verifier.
 */
object VnPayPaymentGateway {

    fun buildPaymentUrl(
        config: VnPayConfig,
        txnRef: String,
        amountVnd: Long,
        orderInfo: String,
        bankCode: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).apply {
            timeInMillis = nowMillis
        }
        val createDate = formatter.format(cal.time)
        cal.add(Calendar.MINUTE, 15)
        val expireDate = formatter.format(cal.time)

        val params = sortedMapOf(
            "vnp_Version" to "2.1.0",
            "vnp_Command" to "pay",
            "vnp_TmnCode" to config.tmnCode.trim(),
            "vnp_Amount" to (amountVnd * 100L).toString(),
            "vnp_CurrCode" to "VND",
            "vnp_TxnRef" to txnRef,
            "vnp_OrderInfo" to orderInfo.ifBlank { "Thanh toan MEbanking $txnRef" },
            "vnp_OrderType" to "other",
            "vnp_Locale" to "vn",
            "vnp_ReturnUrl" to config.returnUrl,
            "vnp_IpAddr" to "127.0.0.1",
            "vnp_CreateDate" to createDate,
            "vnp_ExpireDate" to expireDate,
        )
        if (!bankCode.isNullOrBlank()) {
            params["vnp_BankCode"] = bankCode.trim()
        }

        val queryBuilder = StringBuilder()
        val hashDataBuilder = StringBuilder()
        val entries = params.entries.toList()
        entries.forEachIndexed { index, (k, v) ->
            val encKey = urlEncode(k)
            val encVal = urlEncode(v)
            hashDataBuilder.append(k).append('=').append(encVal)
            queryBuilder.append(encKey).append('=').append(encVal)
            if (index < entries.lastIndex) {
                hashDataBuilder.append('&')
                queryBuilder.append('&')
            }
        }

        val secureHash = hmacSha512(config.hashSecret.trim(), hashDataBuilder.toString())
        queryBuilder.append("&vnp_SecureHash=").append(secureHash)
        return "${config.paymentBaseUrl}?$queryBuilder"
    }

    /**
     * Builds a signed simulated return URL using the configured [hashSecret]
     * for offline / sandbox testing without a live VNPay Merchant account.
     */
    fun buildSimulatedReturnUrl(
        config: VnPayConfig,
        txnRef: String,
        amountVnd: Long,
        bankCode: String,
        orderInfo: String,
        responseCode: String = "00",
        transactionNo: String = (System.currentTimeMillis() % 100_000_000L).toString(),
    ): String {
        val params = sortedMapOf(
            "vnp_Amount" to (amountVnd * 100L).toString(),
            "vnp_BankCode" to bankCode.ifBlank { "NCB" },
            "vnp_OrderInfo" to orderInfo,
            "vnp_ResponseCode" to responseCode,
            "vnp_TmnCode" to config.tmnCode.trim(),
            "vnp_TransactionNo" to transactionNo,
            "vnp_TransactionStatus" to responseCode,
            "vnp_TxnRef" to txnRef,
        )

        val hashData = StringBuilder()
        val query = StringBuilder()
        val entries = params.entries.toList()
        entries.forEachIndexed { index, (k, v) ->
            val encVal = urlEncode(v)
            hashData.append(k).append('=').append(encVal)
            query.append(urlEncode(k)).append('=').append(encVal)
            if (index < entries.lastIndex) {
                hashData.append('&')
                query.append('&')
            }
        }
        val secureHash = hmacSha512(config.hashSecret.trim(), hashData.toString())
        return "${config.returnUrl}?$query&vnp_SecureHash=$secureHash"
    }

    fun verifyReturnUrl(
        returnUrl: String,
        hashSecret: String,
    ): VnPayReturnResult {
        val uri = try {
            Uri.parse(returnUrl)
        } catch (t: Throwable) {
            return VnPayReturnResult.InvalidSignature("URL phản hồi không hợp lệ")
        }

        val paramMap = sortedMapOf<String, String>()
        var receivedHash = ""
        for (name in uri.queryParameterNames) {
            val value = uri.getQueryParameter(name).orEmpty()
            when (name) {
                "vnp_SecureHash" -> receivedHash = value
                "vnp_SecureHashType" -> Unit
                else -> if (value.isNotEmpty()) paramMap[name] = value
            }
        }

        if (receivedHash.isBlank()) {
            return VnPayReturnResult.InvalidSignature("Thiếu chữ ký bảo mật vnp_SecureHash")
        }

        val hashData = StringBuilder()
        val entries = paramMap.entries.toList()
        entries.forEachIndexed { index, (k, v) ->
            hashData.append(k).append('=').append(urlEncode(v))
            if (index < entries.lastIndex) hashData.append('&')
        }
        val expectedHash = hmacSha512(hashSecret.trim(), hashData.toString())
        if (!expectedHash.equals(receivedHash, ignoreCase = true)) {
            return VnPayReturnResult.InvalidSignature("Chữ ký VNPay không hợp lệ (sai HashSecret hoặc dữ liệu bị thay đổi)")
        }

        val responseCode = paramMap["vnp_ResponseCode"].orEmpty()
        val txStatus = paramMap["vnp_TransactionStatus"].orEmpty()
        if (responseCode != "00" || (txStatus.isNotEmpty() && txStatus != "00")) {
            return VnPayReturnResult.Failed(
                responseCode = responseCode.ifBlank { "99" },
                message = mapResponseCodeMessage(responseCode),
            )
        }

        val amountVnd = (paramMap["vnp_Amount"]?.toLongOrNull() ?: 0L) / 100L
        return VnPayReturnResult.Success(
            txnRef = paramMap["vnp_TxnRef"].orEmpty(),
            amountVnd = amountVnd,
            bankCode = paramMap["vnp_BankCode"].orEmpty().ifBlank { "VNPAY" },
            transactionNo = paramMap["vnp_TransactionNo"].orEmpty(),
            orderInfo = paramMap["vnp_OrderInfo"].orEmpty(),
        )
    }

    fun hmacSha512(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA512")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA512")
        mac.init(secretKey)
        val raw = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "US-ASCII")

    private fun mapResponseCodeMessage(code: String): String = when (code) {
        "07" -> "Trừ tiền thành công nhưng giao dịch bị nghi ngờ"
        "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking"
        "10" -> "Xác thực thông tin thẻ/tài khoản quá 3 lần"
        "11" -> "Đã hết hạn chờ thanh toán"
        "12" -> "Thẻ/Tài khoản bị khóa"
        "13" -> "Sai mật khẩu xác thực giao dịch (OTP)"
        "24" -> "Khách hàng đã hủy giao dịch"
        "51" -> "Tài khoản không đủ số dư"
        "65" -> "Tài khoản vượt quá hạn mức giao dịch trong ngày"
        "75" -> "Ngân hàng thanh toán đang bảo trì"
        "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định"
        else -> "Giao dịch VNPay thất bại (mã lỗi: $code)"
    }
}
