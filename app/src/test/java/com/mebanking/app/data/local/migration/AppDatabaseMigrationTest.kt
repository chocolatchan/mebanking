package com.mebanking.app.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseFile = File(
        instrumentation.targetContext.cacheDir,
        "migration-test-${UUID.randomUUID()}.db",
    )
    private val sqliteDriver = AndroidSQLiteDriver()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation,
        databaseFile,
        sqliteDriver,
        AppDatabase::class,
    )

    @Test
    fun createDatabase_v1SchemaValidatesSuccessfully() {
        val db = helper.createDatabase(1)
        assertNotNull(db)

        val tables = mutableListOf<String>()
        val statement = db.prepare("SELECT name FROM sqlite_master WHERE type='table'")
        try {
            while (statement.step()) {
                tables.add(statement.getText(0))
            }
        } finally {
            statement.close()
        }

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
