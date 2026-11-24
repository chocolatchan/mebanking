package com.mebanking.app.domain.service

import android.content.Context

data class RememberedCustomer(
    val userId: Long,
    val fullName: String,
    val phone: String,
    val username: String,
)

/**
 * Persists last successfully logged-in customer info on device.
 * Used to provide seamless biometrics login and greeting when a user logs out
 * while keeping their profile remembered, or clean fresh login when wiped.
 */
class RememberedCustomerStore(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("mebanking_remembered_customer_v1", Context.MODE_PRIVATE)
    }

    fun getRememberedCustomer(): RememberedCustomer? {
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        if (userId <= 0L) return null
        val fullName = prefs.getString(KEY_FULL_NAME, null) ?: return null
        val phone = prefs.getString(KEY_PHONE, null) ?: ""
        val username = prefs.getString(KEY_USERNAME, null) ?: phone
        return RememberedCustomer(
            userId = userId,
            fullName = fullName,
            phone = phone,
            username = username,
        )
    }

    fun saveCustomer(userId: Long, fullName: String, phone: String, username: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_PHONE, phone)
            .putString(KEY_USERNAME, username)
            .commit()
    }

    fun saveCustomer(customer: RememberedCustomer) {
        saveCustomer(
            userId = customer.userId,
            fullName = customer.fullName,
            phone = customer.phone,
            username = customer.username,
        )
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val KEY_USER_ID = "remembered_user_id"
        const val KEY_FULL_NAME = "remembered_full_name"
        const val KEY_PHONE = "remembered_phone"
        const val KEY_USERNAME = "remembered_username"
    }
}
