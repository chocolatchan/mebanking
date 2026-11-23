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
import com.mebanking.app.data.local.dao.lifestyle.CinemaDao
import com.mebanking.app.data.local.dao.lifestyle.FlightDao
import com.mebanking.app.data.local.dao.lifestyle.HotelDao
import com.mebanking.app.data.local.dao.lifestyle.ShopDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.BillEntity
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.local.entity.ChatIntentEntity
import com.mebanking.app.data.local.entity.ExternalBankEntity
import com.mebanking.app.data.local.entity.OtpCodeEntity
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.entity.lifestyle.CinemaBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.CinemaShowtimeEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
import com.mebanking.app.data.local.entity.lifestyle.MovieEntity
import com.mebanking.app.data.local.entity.lifestyle.OrderEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity

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
        // Lifestyle Simulators (Roadmap B)
        FlightEntity::class,
        FlightBookingEntity::class,
        HotelEntity::class,
        HotelBookingEntity::class,
        MovieEntity::class,
        CinemaShowtimeEntity::class,
        CinemaBookingEntity::class,
        ProductEntity::class,
        OrderEntity::class,
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

    // Lifestyle DAOs (Roadmap B)
    abstract fun flightDao(): FlightDao
    abstract fun hotelDao(): HotelDao
    abstract fun cinemaDao(): CinemaDao
    abstract fun shopDao(): ShopDao

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

                // Lifestyle Simulators tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `flights` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `flight_number` TEXT NOT NULL, `airline` TEXT NOT NULL, `departure_airport` TEXT NOT NULL, `arrival_airport` TEXT NOT NULL, `departure_time` INTEGER NOT NULL, `arrival_time` INTEGER NOT NULL, `price_vnd` INTEGER NOT NULL, `available_seats` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `flight_bookings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `flight_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `passenger_name` TEXT NOT NULL, `seat_number` TEXT NOT NULL, `booking_reference` TEXT NOT NULL, `total_amount_vnd` INTEGER NOT NULL, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`flight_id`) REFERENCES `flights`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flight_bookings_flight_id` ON `flight_bookings` (`flight_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_flight_bookings_booking_reference` ON `flight_bookings` (`booking_reference`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `hotels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `city` TEXT NOT NULL, `address` TEXT NOT NULL, `room_type` TEXT NOT NULL, `price_per_night_vnd` INTEGER NOT NULL, `rating` REAL NOT NULL, `available_rooms` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `hotel_bookings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `hotel_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `guest_name` TEXT NOT NULL, `check_in_date` TEXT NOT NULL, `check_out_date` TEXT NOT NULL, `nights` INTEGER NOT NULL, `total_amount_vnd` INTEGER NOT NULL, `booking_reference` TEXT NOT NULL, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`hotel_id`) REFERENCES `hotels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hotel_bookings_hotel_id` ON `hotel_bookings` (`hotel_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hotel_bookings_booking_reference` ON `hotel_bookings` (`booking_reference`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `movies` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `genre` TEXT NOT NULL, `duration_minutes` INTEGER NOT NULL, `poster_url` TEXT NOT NULL, `ticket_price_vnd` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `cinema_showtimes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `movie_id` INTEGER NOT NULL, `cinema_name` TEXT NOT NULL, `hall_name` TEXT NOT NULL, `show_time` TEXT NOT NULL, FOREIGN KEY(`movie_id`) REFERENCES `movies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cinema_showtimes_movie_id` ON `cinema_showtimes` (`movie_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `cinema_bookings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `showtime_id` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `seat_number` TEXT NOT NULL, `ticket_code` TEXT NOT NULL, `amount_vnd` INTEGER NOT NULL, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`showtime_id`) REFERENCES `cinema_showtimes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cinema_bookings_showtime_id_seat_number` ON `cinema_bookings` (`showtime_id`, `seat_number`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cinema_bookings_ticket_code` ON `cinema_bookings` (`ticket_code`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `price_vnd` INTEGER NOT NULL, `description` TEXT NOT NULL, `stock_quantity` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `order_code` TEXT NOT NULL, `total_amount_vnd` INTEGER NOT NULL, `items_summary` TEXT NOT NULL, `delivery_address` TEXT NOT NULL, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_orders_order_code` ON `orders` (`order_code`)")
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
