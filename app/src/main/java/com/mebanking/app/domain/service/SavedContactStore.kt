package com.mebanking.app.domain.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SavedContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val accountNumber: String,
    val bank: String = "MEbanking",
    val phone: String = "",
) {
    val initials: String
        get() = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .takeLast(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "KH" }
}

/**
 * Lightweight persisted directory of frequent transfer contacts, seeded with
 * default quick-transfer shortcuts on first launch.
 */
class SavedContactStore(
    private val prefs: SharedPreferences? = null,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    private val _contacts = MutableStateFlow(loadOrSeedContacts())
    val contacts: StateFlow<List<SavedContact>> = _contacts.asStateFlow()

    fun addContact(
        name: String,
        accountNumber: String,
        bank: String = "MEbanking",
        phone: String = "",
    ): SavedContact {
        val cleanName = name.trim()
        val cleanAcc = accountNumber.trim()
        require(cleanName.isNotBlank()) { "Vui lòng nhập tên liên hệ" }
        require(cleanAcc.isNotBlank()) { "Vui lòng nhập số tài khoản" }

        val created = SavedContact(
            name = cleanName,
            accountNumber = cleanAcc,
            bank = bank.trim().ifBlank { "MEbanking" },
            phone = phone.trim(),
        )
        val updated = listOf(created) + _contacts.value.filterNot {
            it.accountNumber == cleanAcc && it.bank.equals(created.bank, ignoreCase = true)
        }
        persist(updated)
        _contacts.value = updated
        return created
    }

    fun deleteContact(contactId: String) {
        val updated = _contacts.value.filterNot { it.id == contactId }
        persist(updated)
        _contacts.value = updated
    }

    private fun loadOrSeedContacts(): List<SavedContact> {
        val serialized = prefs?.getString(KEY_CONTACTS, null)
        if (serialized == null) {
            persist(DefaultContactsSeed)
            return DefaultContactsSeed
        }
        return decode(serialized)
    }

    private fun persist(list: List<SavedContact>) {
        prefs?.edit()?.putString(KEY_CONTACTS, encode(list))?.apply()
    }

    private fun encode(list: List<SavedContact>): String =
        list.joinToString(RECORD_SEP) { contact ->
            listOf(
                sanitize(contact.id),
                sanitize(contact.name),
                sanitize(contact.accountNumber),
                sanitize(contact.bank),
                sanitize(contact.phone),
            ).joinToString(FIELD_SEP)
        }

    private fun decode(raw: String): List<SavedContact> {
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { record ->
            val parts = record.split(FIELD_SEP)
            if (parts.size < 4) return@mapNotNull null
            SavedContact(
                id = parts[0],
                name = parts[1],
                accountNumber = parts[2],
                bank = parts[3],
                phone = parts.getOrElse(4) { "" },
            )
        }
    }

    private fun sanitize(value: String): String =
        value.replace(RECORD_SEP, " ").replace(FIELD_SEP, " ").trim()

    companion object {
        private const val PREFS_NAME = "mebanking_saved_contacts_v1"
        private const val KEY_CONTACTS = "contacts_serialized"
        private const val RECORD_SEP = "\n;;\n"
        private const val FIELD_SEP = "|~|"

        val DefaultContactsSeed = listOf(
            SavedContact(id = "seed-1", name = "Nguyễn Lan Anh", accountNumber = "100555666777", bank = "VCB", phone = "0901234567"),
            SavedContact(id = "seed-2", name = "Trần Văn Bình", accountNumber = "200987654321", bank = "TCB", phone = "0912345678"),
            SavedContact(id = "seed-3", name = "Tài khoản tiết kiệm", accountNumber = "200123456789", bank = "MEbanking"),
            SavedContact(id = "seed-4", name = "Lê Hoàng Yến", accountNumber = "100888999111", bank = "MB", phone = "0934567890"),
            SavedContact(id = "seed-5", name = "Phạm Quang Huy", accountNumber = "100666777222", bank = "ACB", phone = "0987654321"),
        )
    }
}
