package com.mebanking.app.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.mebanking.app.data.local.AppDatabase
import org.junit.Assert.assertNotNull
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
        AppDatabase::class.java,
    )

    @Test
    fun createDatabase_v1SchemaValidatesSuccessfully() {
        val db = helper.createDatabase(testDbName, 1)
        assertNotNull(db)

        val tables = mutableListOf<String>()
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0))
        }
        cursor.close()

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

        helper.closeWhenFinished(db)
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
}
