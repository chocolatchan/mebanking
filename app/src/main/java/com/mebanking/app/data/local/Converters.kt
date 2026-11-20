package com.mebanking.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType

/**
 * Room TypeConverters for banking enums and serialized complex types.
 */
class Converters {

    private val gson = Gson()

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
    fun fromStringList(value: List<String>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
