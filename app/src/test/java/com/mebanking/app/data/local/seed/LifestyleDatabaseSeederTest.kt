package com.mebanking.app.data.local.seed

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifestyleDatabaseSeederTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seed_populatesAllLifestyleCatalogs() = runTest {
        LifestyleDatabaseSeeder.seed(database)

        val flights = database.flightDao().getAllFlights().first()
        assertEquals(3, flights.size)
        assertEquals("VN123", flights[0].flightNumber)
        assertEquals(1_850_000L, flights[0].priceVnd)

        val hotels = database.hotelDao().getAllHotels().first()
        assertEquals(3, hotels.size)
        assertEquals("The Reverie Saigon", hotels[0].name)

        val movies = database.cinemaDao().getAllMovies().first()
        assertEquals(3, movies.size)

        val showtimes = database.cinemaDao().getShowtimesForMovie(1L).first()
        assertEquals(1, showtimes.size)
        assertEquals("CGV Vincom Đồng Khởi", showtimes[0].cinemaName)

        val products = database.shopDao().getAllProducts().first()
        assertEquals(3, products.size)
        assertEquals(6_990_000L, products[0].priceVnd)
    }

    @Test
    fun seed_multipleCallsAreIdempotent() = runTest {
        LifestyleDatabaseSeeder.seed(database)
        LifestyleDatabaseSeeder.seed(database)

        val flights = database.flightDao().getAllFlights().first()
        assertEquals(3, flights.size)

        val products = database.shopDao().getAllProducts().first()
        assertEquals(3, products.size)
    }
}
