package com.mebanking.app.domain.util

/**
 * Robust parser for VietQR (EMVCo standard), VietQR payment URLs, JSON,
 * and plaintext banking transfer specifications.
 */
object VietQrParser {

    data class ParsedQrResult(
        val accountNumber: String,
        val bankBin: String? = null,
        val bankName: String? = null,
        val recipientName: String? = null,
        val amount: Long? = null,
        val note: String? = null,
        val rawPayload: String = "",
    )

    private val BANK_BIN_NAMES = mapOf(
        "970436" to "Vietcombank (VCB)",
        "970407" to "Techcombank (TCB)",
        "970422" to "MBBank (MB)",
        "970415" to "VietinBank",
        "970418" to "BIDV",
        "970405" to "Agribank",
        "970416" to "ACB",
        "970432" to "VPBank",
        "970423" to "TPBank",
        "970403" to "Sacombank",
        "970441" to "VIB",
        "970448" to "OCB",
        "970437" to "HDBank",
        "970431" to "Eximbank",
        "970443" to "SHB",
        "970426" to "MSB",
        "970449" to "LPBank",
        "970428" to "Nam A Bank",
        "970440" to "SeABank",
        "970414" to "OceanBank",
        "970419" to "NCB",
        "970438" to "BaoVietBank",
        "970454" to "BVBank",
        "970468" to "Shinhan Bank",
        "970457" to "Woori Bank",
    )

    /**
     * Parses any input text:
     * 1. Standard EMVCo VietQR string (starts with "000201")
     * 2. VietQR quicklink URL (e.g. img.vietqr.io, vietqr.me, etc.)
     * 3. Key-Value formatted text (STK, Số tiền, Nội dung, etc.)
     * 4. Delimited text (account|amount|note)
     * 5. Plain numeric account number (e.g. 100123456789)
     */
    fun parse(input: String): ParsedQrResult? {
        val clean = input.trim()
        if (clean.isEmpty()) return null

        // 1. Try EMVCo VietQR format
        if (clean.startsWith("000201") || (clean.length > 20 && clean.contains("A000000727"))) {
            parseEmvCo(clean)?.let { return it }
        }

        // 2. Try URL format (VietQR quicklink / payment URL)
        if (clean.startsWith("http://", ignoreCase = true) ||
            clean.startsWith("https://", ignoreCase = true) ||
            clean.startsWith("vietqr://", ignoreCase = true) ||
            clean.startsWith("bank://", ignoreCase = true)
        ) {
            parseUrl(clean)?.let { return it }
        }

        // 3. Try JSON format
        if (clean.startsWith("{") && clean.endsWith("}")) {
            parseJson(clean)?.let { return it }
        }

        // 4. Try Key-Value labeled plaintext (e.g. STK: ..., Số tiền: ...)
        parseLabeledText(clean)?.let { return it }

        // 5. Try Delimited text (e.g. 100123456789|50000|Tien an)
        parseDelimited(clean)?.let { return it }

        // 6. Try Plain Account Number (digits only, length 6-24)
        val digitsOnly = clean.filter { it.isDigit() }
        if (digitsOnly.length in 6..24 && clean.all { it.isDigit() || it == ' ' || it == '-' }) {
            return ParsedQrResult(
                accountNumber = digitsOnly,
                rawPayload = clean,
            )
        }

        return null
    }

    /**
     * Parses standard EMVCo Tag-Length-Value (TLV) string.
     */
    private fun parseEmvCo(payload: String): ParsedQrResult? {
        try {
            val rootTlvs = parseTlvs(payload)
            if (rootTlvs.isEmpty()) return null

            // Tag 38 is NAPAS Merchant Account Information
            val tag38Raw = rootTlvs["38"] ?: return null
            val tag38Tlvs = parseTlvs(tag38Raw)

            // Subtag 01 is Beneficiary Organization: sub-subtag 00 = BIN, 01 = Account
            val tag01Raw = tag38Tlvs["01"]
            var bankBin: String? = null
            var accountNumber: String? = null

            if (tag01Raw != null) {
                val sub01Tlvs = parseTlvs(tag01Raw)
                bankBin = sub01Tlvs["00"]
                accountNumber = sub01Tlvs["01"]
            }

            // Fallback: If tag01 was just account or alternate subtag
            if (accountNumber == null) {
                accountNumber = tag38Tlvs["02"] ?: tag38Tlvs["01"]
            }

            if (accountNumber.isNullOrBlank()) return null

            val amountStr = rootTlvs["54"]
            val amount = amountStr?.toDoubleOrNull()?.toLong()

            // Tag 59 is Beneficiary / Merchant Name
            val recipientName = rootTlvs["59"]

            // Tag 62 is Additional Data Field: subtag 08 is Reference Label / Purpose
            var note: String? = null
            val tag62Raw = rootTlvs["62"]
            if (tag62Raw != null) {
                val tag62Tlvs = parseTlvs(tag62Raw)
                note = tag62Tlvs["08"] ?: tag62Tlvs["01"]
            }

            val bankName = bankBin?.let { BANK_BIN_NAMES[it] }

            return ParsedQrResult(
                accountNumber = accountNumber.trim(),
                bankBin = bankBin?.trim(),
                bankName = bankName,
                recipientName = recipientName?.trim(),
                amount = amount,
                note = note?.trim(),
                rawPayload = payload,
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseTlvs(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0
        while (index + 4 <= data.length) {
            val tag = data.substring(index, index + 2)
            val lenStr = data.substring(index + 2, index + 4)
            val len = lenStr.toIntOrNull() ?: break
            index += 4
            if (index + len > data.length) break
            val value = data.substring(index, index + len)
            result[tag] = value
            index += len
        }
        return result
    }

    /**
     * Parses VietQR and banking URLs.
     */
    private fun parseUrl(url: String): ParsedQrResult? {
        try {
            // E.g.: https://img.vietqr.io/image/970436-100123456789-compact2.png?amount=50000&addInfo=An%20trua&accountName=Lan%20Anh
            val pathPart = url.substringBefore("?")
            val queryPart = if (url.contains("?")) url.substringAfter("?") else ""

            var bankBin: String? = null
            var accountNumber: String? = null

            // Check if path has bank-account pattern
            val pathSegments = pathPart.split("/").filter { it.isNotEmpty() }
            for (segment in pathSegments) {
                val cleanSeg = segment.substringBefore(".png").substringBefore(".jpg")
                if (cleanSeg.contains("-")) {
                    val parts = cleanSeg.split("-")
                    if (parts.size >= 2 && parts[0].all { it.isDigit() } && parts[1].all { it.isDigit() }) {
                        bankBin = parts[0]
                        accountNumber = parts[1]
                        break
                    }
                }
            }

            // Extract Query parameters
            val params = queryPart.split("&").mapNotNull {
                val kv = it.split("=")
                if (kv.size == 2) kv[0].lowercase() to java.net.URLDecoder.decode(kv[1], "UTF-8") else null
            }.toMap()

            if (accountNumber == null) {
                accountNumber = params["account"] ?: params["acc"] ?: params["stk"] ?: params["accountno"]
            }
            if (bankBin == null) {
                bankBin = params["bank"] ?: params["bin"] ?: params["bankid"]
            }

            if (accountNumber.isNullOrBlank()) return null

            val amount = params["amount"]?.filter { it.isDigit() }?.toLongOrNull()
            val note = params["addinfo"] ?: params["note"] ?: params["desc"] ?: params["noidung"]
            val recipientName = params["accountname"] ?: params["name"] ?: params["ten"]

            val bankName = bankBin?.let { BANK_BIN_NAMES[it] }

            return ParsedQrResult(
                accountNumber = accountNumber,
                bankBin = bankBin,
                bankName = bankName,
                recipientName = recipientName,
                amount = amount,
                note = note,
                rawPayload = url,
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Parses simple JSON: {"account": "...", "amount": 50000, "note": "...", "name": "..."}
     */
    private fun parseJson(json: String): ParsedQrResult? {
        try {
            val accountRegex = """"account(?:_number|no)?":\s*"?([0-9]{6,24})"?/""".toRegex(RegexOption.IGNORE_CASE)
            val matchAcc = """"account(?:Number|No)?":\s*"?([0-9]{6,24})"?/""".toRegex(RegexOption.IGNORE_CASE).find(json)
                ?: """"stk":\s*"?([0-9]{6,24})"?/""".toRegex(RegexOption.IGNORE_CASE).find(json)
                ?: """"(?:toAccount|account)":\s*"?([0-9]{6,24})"?/""".toRegex(RegexOption.IGNORE_CASE).find(json)
            val acc = matchAcc?.groupValues?.get(1) ?: return null

            val amountMatch = """"amount":\s*([0-9]+)""".toRegex(RegexOption.IGNORE_CASE).find(json)
            val amount = amountMatch?.groupValues?.get(1)?.toLongOrNull()

            val noteMatch = """"note":\s*"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE).find(json)
                ?: """"noidung":\s*"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE).find(json)
            val note = noteMatch?.groupValues?.get(1)

            val nameMatch = """"name":\s*"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE).find(json)
                ?: """"recipientName":\s*"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE).find(json)
            val name = nameMatch?.groupValues?.get(1)

            val bankMatch = """"bank":\s*"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE).find(json)
            val bank = bankMatch?.groupValues?.get(1)

            return ParsedQrResult(
                accountNumber = acc,
                bankName = bank,
                recipientName = name,
                amount = amount,
                note = note,
                rawPayload = json,
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Parses multi-line key-value labeled text:
     * STK: 100123456789
     * Ngân hàng: Vietcombank
     * Số tiền: 50.000
     * Nội dung: Chuyển tiền ăn trưa
     */
    private fun parseLabeledText(text: String): ParsedQrResult? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var acc: String? = null
        var amount: Long? = null
        var note: String? = null
        var name: String? = null
        var bank: String? = null

        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("stk") || lower.startsWith("số tài khoản") || lower.startsWith("tài khoản") || lower.startsWith("so tai khoan") -> {
                    val rawVal = line.substringAfter(":").trim()
                    val digits = rawVal.filter { it.isDigit() }
                    if (digits.length in 6..24) acc = digits
                }
                lower.startsWith("số tiền") || lower.startsWith("so tien") || lower.startsWith("tiền") || lower.startsWith("amount") -> {
                    val rawVal = line.substringAfter(":").trim()
                    val digits = rawVal.filter { it.isDigit() }
                    amount = digits.toLongOrNull()
                }
                lower.startsWith("nội dung") || lower.startsWith("noi dung") || lower.startsWith("ghi chú") || lower.startsWith("note") -> {
                    note = line.substringAfter(":").trim()
                }
                lower.startsWith("người nhận") || lower.startsWith("nguoi nhan") || lower.startsWith("tên") || lower.startsWith("ten") || lower.startsWith("name") -> {
                    name = line.substringAfter(":").trim()
                }
                lower.startsWith("ngân hàng") || lower.startsWith("ngan hang") || lower.startsWith("bank") -> {
                    bank = line.substringAfter(":").trim()
                }
            }
        }

        if (acc != null) {
            return ParsedQrResult(
                accountNumber = acc,
                bankName = bank,
                recipientName = name,
                amount = amount,
                note = note,
                rawPayload = text,
            )
        }
        return null
    }

    /**
     * Parses delimited text:
     * 100123456789|50000|Chuyen tien
     * or: 100123456789, 50000, Chuyen tien
     */
    private fun parseDelimited(text: String): ParsedQrResult? {
        val delimiter = when {
            text.contains("|") -> "|"
            text.contains(";") -> ";"
            text.contains(",") -> ","
            else -> return null
        }
        val parts = text.split(delimiter).map { it.trim() }
        if (parts.size >= 2) {
            val firstDigits = parts[0].filter { it.isDigit() }
            if (firstDigits.length in 6..24) {
                val amount = parts[1].filter { it.isDigit() }.toLongOrNull()
                val note = if (parts.size >= 3) parts[2] else null
                return ParsedQrResult(
                    accountNumber = firstDigits,
                    amount = amount,
                    note = note,
                    rawPayload = text,
                )
            }
        }
        return null
    }
}
