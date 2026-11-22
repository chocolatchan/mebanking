package com.mebanking.app.data.local

import androidx.room.TypeConverter
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType

/**
 * Room TypeConverters for banking enums and serialized complex types.
 *
 * String lists use a small, dependency-free JSON array encoder/decoder so the
 * local persistence layer needs no third-party JSON library and behaves
 * identically in JVM unit tests and on device.
 */
class Converters {

    @TypeConverter
    fun fromRole(value: Role?): String? = value?.name

    @TypeConverter
    fun toRole(value: String?): Role? = value?.let { Role.valueOf(it) }

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? = value?.let { TransactionStatus.valueOf(it) }

    @TypeConverter
    fun fromBillType(value: BillType?): String? = value?.name

    @TypeConverter
    fun toBillType(value: String?): BillType? = value?.let { BillType.valueOf(it) }

    @TypeConverter
    fun fromBillStatus(value: BillStatus?): String? = value?.name

    @TypeConverter
    fun toBillStatus(value: String?): BillStatus? = value?.let { BillStatus.valueOf(it) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return buildString {
            append('[')
            value.forEachIndexed { index, item ->
                if (index > 0) append(',')
                append('"')
                for (character in item) {
                    when (character) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(character)
                    }
                }
                append('"')
            }
            append(']')
        }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val text = value.trim()
        if (!text.startsWith("[") || !text.endsWith("]")) return emptyList()

        val result = mutableListOf<String>()
        val end = text.length - 1
        var index = 1
        while (index < end) {
            while (index < end && text[index].isJsonSeparator()) index++
            if (index >= end) break
            if (text[index] != '"') return emptyList()
            index++

            val builder = StringBuilder()
            var closed = false
            while (index < end) {
                val character = text[index]
                if (character == '\\') {
                    if (index + 1 >= end) return emptyList()
                    index++
                    when (val escaped = text[index]) {
                        '"' -> builder.append('"')
                        '\\' -> builder.append('\\')
                        '/' -> builder.append('/')
                        'n' -> builder.append('\n')
                        'r' -> builder.append('\r')
                        't' -> builder.append('\t')
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000C')
                        'u' -> {
                            if (index + 4 >= end) return emptyList()
                            val hex = text.substring(index + 1, index + 5)
                            val code = hex.toIntOrNull(16) ?: return emptyList()
                            builder.append(code.toChar())
                            index += 4
                        }
                        else -> builder.append(escaped)
                    }
                } else if (character == '"') {
                    closed = true
                    index++
                    break
                } else {
                    builder.append(character)
                }
                index++
            }
            if (!closed) return emptyList()
            result.add(builder.toString())
        }
        return result
    }

    private fun Char.isJsonSeparator(): Boolean =
        this == ',' || this == ' ' || this == '\n' || this == '\r' || this == '\t'
}
