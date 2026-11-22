package com.mebanking.app.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.mebanking.app.data.local.AppDatabase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(testDbName),
        AndroidSQLiteDriver(),
        AppDatabase::class,
    )

    @Test
    fun createDatabase_v1SchemaValidatesSuccessfully() {
        val db = helper.createDatabase(1)
        assertNotNull(db)

        val tables = mutableListOf<String>()
        val query = db.prepare("SELECT name FROM sqlite_master WHERE type='table'")
        while (query.step()) {
            tables.add(query.getText(0))
        }
        query.close()

        val expectedTables = listOf(
            "users",
            "accounts",
            "transactions",
            "otp_codes",
            "bills",
            "external_banks",
            "branches",
            "chat_intents",
        )

        for (expected in expectedTables) {
            assertTrue(
                "Expected table '$expected' to exist in created database. Found tables: $tables",
                tables.contains(expected),
            )
        }

        db.close()
    }

    @Test
    fun schemaJsonFile_existsAndIsValid() {
        val schemaFromAppDir = File("schemas/com.mebanking.app.data.local.AppDatabase/1.json")
        val schemaFromRootDir = File("app/schemas/com.mebanking.app.data.local.AppDatabase/1.json")
        val exists = schemaFromAppDir.exists() || schemaFromRootDir.exists()
        assertTrue("Schema JSON 1.json must exist in configured schema directory", exists)

        val file = if (schemaFromAppDir.exists()) schemaFromAppDir else schemaFromRootDir
        assertTrue("Schema JSON file should not be empty", file.length() > 0)
    }

    @Test
    fun migration1To2_addsNullableUniqueIdempotencyKeyAndKeepsRows() {
        val db = helper.createDatabase(1)
        db.execute("INSERT INTO users (id, username, password_hash, full_name, email, phone, role, kyc_verified, face_image_path, created_at) VALUES (1, 'testuser', 'hash', 'Test User', 'test@example.com', '0900000000', 'CUSTOMER', 0, NULL, 1)")
        db.execute("INSERT INTO accounts (id, user_id, account_number, account_type, balance, currency, interest_rate, status, created_at) VALUES (1, 1, '10000001', 'CHECKING', 1000, 'VND', 0, 'ACTIVE', 1)")
        db.execute("INSERT INTO transactions (id, from_account_id, to_account_id, external_bank_id, to_account_no, amount, type, status, note, timestamp) VALUES (1, NULL, 1, NULL, NULL, 100, 'DEPOSIT', 'SUCCESS', 'legacy', 1)")
        db.close()

        val migrated = helper.runMigrationsAndValidate(2, listOf(AppDatabase.MIGRATION_1_2))
        val query = migrated.prepare("SELECT idempotency_key, note FROM transactions WHERE id = 1")
        assertTrue(query.step())
        assertTrue(query.isNull(0))
        assertEquals("legacy", query.getText(1))
        query.close()
        migrated.close()
    }

    @Test
    fun migration2To3_addsNationalIdWithoutLosingExistingCustomer() {
        val db = helper.createDatabase(2)
        db.execute("INSERT INTO users (id, username, password_hash, full_name, email, phone, role, kyc_verified, face_image_path, created_at) VALUES (1, 'testuser', 'hash', 'Test User', 'test@example.com', '0900000000', 'CUSTOMER', 0, NULL, 1)")
        db.close()

        val migrated = helper.runMigrationsAndValidate(3, listOf(AppDatabase.MIGRATION_2_3))
        val query = migrated.prepare("SELECT national_id_cipher, national_id_hash, full_name FROM users WHERE id = 1")
        assertTrue(query.step())
        assertTrue(query.isNull(0))
        assertTrue(query.isNull(1))
        assertEquals("Test User", query.getText(2))
        query.close()
        migrated.close()
    }

    private fun SQLiteConnection.execute(sql: String) {
        prepare(sql).use { it.step() }
    }
}
