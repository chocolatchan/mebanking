package com.mebanking.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.BillDao
import com.mebanking.app.data.local.dao.BranchDao
import com.mebanking.app.data.local.dao.ChatIntentDao
import com.mebanking.app.data.local.dao.ExternalBankDao
import com.mebanking.app.data.local.dao.OtpCodeDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.BillEntity
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.local.entity.ChatIntentEntity
import com.mebanking.app.data.local.entity.ExternalBankEntity
import com.mebanking.app.data.local.entity.OtpCodeEntity
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        OtpCodeEntity::class,
        BillEntity::class,
        ExternalBankEntity::class,
        BranchEntity::class,
        ChatIntentEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun otpCodeDao(): OtpCodeDao
    abstract fun billDao(): BillDao
    abstract fun externalBankDao(): ExternalBankDao
    abstract fun branchDao(): BranchDao
    abstract fun chatIntentDao(): ChatIntentDao

    companion object {
        const val DATABASE_NAME = "mebanking.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN idempotency_key TEXT DEFAULT NULL")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_idempotency_key ON transactions(idempotency_key)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN national_id_cipher TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE users ADD COLUMN national_id_hash TEXT DEFAULT NULL")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_national_id_hash ON users(national_id_hash)")
            }
        }

        val DATABASE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Enforce SQLite-level invariant triggers for non-negative balances and positive amounts
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trigger_account_balance_non_negative_insert
                    BEFORE INSERT ON accounts
                    FOR EACH ROW
                    WHEN NEW.balance < 0
                    BEGIN
                        SELECT RAISE(ABORT, 'Account balance cannot be negative');
                    END;
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trigger_account_balance_non_negative_update
                    BEFORE UPDATE OF balance ON accounts
                    FOR EACH ROW
                    WHEN NEW.balance < 0
                    BEGIN
                        SELECT RAISE(ABORT, 'Account balance cannot be negative');
                    END;
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trigger_transaction_amount_positive_insert
                    BEFORE INSERT ON transactions
                    FOR EACH ROW
                    WHEN NEW.amount <= 0
                    BEGIN
                        SELECT RAISE(ABORT, 'Transaction amount must be strictly positive');
                    END;
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trigger_bill_amount_positive_insert
                    BEFORE INSERT ON bills
                    FOR EACH ROW
                    WHEN NEW.amount <= 0
                    BEGIN
                        SELECT RAISE(ABORT, 'Bill amount must be strictly positive');
                    END;
                    """.trimIndent()
                )
            }
        }

        fun buildDatabase(context: Context, databaseName: String = DATABASE_NAME): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                databaseName,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(DATABASE_CALLBACK)
                .build()
        }

        fun buildInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
            )
                .addCallback(DATABASE_CALLBACK)
                .allowMainThreadQueries()
                .build()
        }
    }
}
