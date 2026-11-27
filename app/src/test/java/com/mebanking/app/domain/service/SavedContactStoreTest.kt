package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.domain.util.FuzzySearchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedContactStoreTest {

    @Test
    fun seedsDefaultContactsAndSupportsAddSearchAndDelete() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("test_contacts_store", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val store = SavedContactStore(prefs)
        assertEquals(SavedContactStore.DefaultContactsSeed.size, store.contacts.value.size)

        val added = store.addContact(
            name = "Nguyễn Minh Quân",
            accountNumber = "100999888777",
            bank = "TPB",
            phone = "0909112233",
        )
        assertEquals("MQ", added.initials)
        assertTrue(store.contacts.value.any { it.accountNumber == "100999888777" })

        // Fuzzy search supports accentless + slight typo
        val matched = store.contacts.value.filter { contact ->
            FuzzySearchHelper.matches("minh quna tpb", listOf(contact.name, contact.accountNumber, contact.bank, contact.phone))
        }
        assertEquals(1, matched.size)
        assertEquals(added.id, matched.first().id)

        store.deleteContact(added.id)
        assertFalse(store.contacts.value.any { it.id == added.id })
    }
}
