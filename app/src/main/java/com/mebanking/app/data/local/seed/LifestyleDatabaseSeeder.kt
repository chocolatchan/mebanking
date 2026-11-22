package com.mebanking.app.data.local.seed

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.lifestyle.CinemaShowtimeEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
import com.mebanking.app.data.local.entity.lifestyle.MovieEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity

/**
 * Seed fixtures for Roadmap B Lifestyle Simulators (Flights, Hotels, Cinema, Shop).
 * Independent from core banking fixtures to preserve DatabaseSeederTest counts.
 */
object LifestyleDatabaseSeeder {

    fun initialFlights(): List<FlightEntity> = listOf(
        FlightEntity(
            id = 1L,
            flightNumber = "VN123",
            airline = "Vietnam Airlines",
            departureAirport = "SGN",
            arrivalAirport = "HAN",
            departureTime = 1735000000000L,
            arrivalTime = 1735007200000L,
            priceVnd = 1_850_000L,
            availableSeats = 42,
        ),
        FlightEntity(
            id = 2L,
            flightNumber = "VJ456",
            airline = "Vietjet Air",
            departureAirport = "SGN",
            arrivalAirport = "DAD",
            departureTime = 1735010000000L,
            arrivalTime = 1735014800000L,
            priceVnd = 980_000L,
            availableSeats = 28,
        ),
        FlightEntity(
            id = 3L,
            flightNumber = "QH789",
            airline = "Bamboo Airways",
            departureAirport = "HAN",
            arrivalAirport = "PQC",
            departureTime = 1735020000000L,
            arrivalTime = 1735028000000L,
            priceVnd = 2_150_000L,
            availableSeats = 15,
        ),
    )

    fun initialHotels(): List<HotelEntity> = listOf(
        HotelEntity(
            id = 1L,
            name = "The Reverie Saigon",
            city = "TP. Hồ Chí Minh",
            address = "22-36 Nguyễn Huệ, Bến Nghé, Quận 1",
            roomType = "Deluxe King Room",
            pricePerNightVnd = 3_500_000L,
            rating = 4.9f,
            availableRooms = 8,
        ),
        HotelEntity(
            id = 2L,
            name = "Dalat Palace Heritage",
            city = "Đà Lạt",
            address = "02 Trần Phú, Phường 3, TP. Đà Lạt",
            roomType = "Superior Pine View",
            pricePerNightVnd = 2_200_000L,
            rating = 4.7f,
            availableRooms = 5,
        ),
        HotelEntity(
            id = 3L,
            name = "InterContinental Danang Sun Peninsula",
            city = "Đà Nẵng",
            address = "Bãi Bắc, Bán đảo Sơn Trà, Đà Nẵng",
            roomType = "Classic Ocean View",
            pricePerNightVnd = 5_800_000L,
            rating = 4.9f,
            availableRooms = 4,
        ),
    )

    fun initialMovies(): List<MovieEntity> = listOf(
        MovieEntity(
            id = 1L,
            title = "Đất Rừng Phương Nam",
            genre = "Phiêu lưu, Lịch sử",
            durationMinutes = 134,
            ticketPriceVnd = 110_000L,
        ),
        MovieEntity(
            id = 2L,
            title = "Mai",
            genre = "Tâm lý, Tình cảm",
            durationMinutes = 131,
            ticketPriceVnd = 120_000L,
        ),
        MovieEntity(
            id = 3L,
            title = "Godzilla x Kong: The New Empire",
            genre = "Hành động, Viễn tưởng",
            durationMinutes = 115,
            ticketPriceVnd = 135_000L,
        ),
    )

    fun initialShowtimes(): List<CinemaShowtimeEntity> = listOf(
        CinemaShowtimeEntity(
            id = 1L,
            movieId = 1L,
            cinemaName = "CGV Vincom Đồng Khởi",
            hallName = "Cinema 01 (Starium)",
            showTime = "18:30",
        ),
        CinemaShowtimeEntity(
            id = 2L,
            movieId = 2L,
            cinemaName = "CGV Landmark 81",
            hallName = "Cinema 03 (IMAX)",
            showTime = "20:00",
        ),
        CinemaShowtimeEntity(
            id = 3L,
            movieId = 3L,
            cinemaName = "Lotte Cinema Nowzone",
            hallName = "Cinema 02",
            showTime = "19:15",
        ),
    )

    fun initialProducts(): List<ProductEntity> = listOf(
        ProductEntity(
            id = 1L,
            name = "Tai nghe không dây chống ồn Sony WH-1000XM5",
            category = "Thiết bị điện tử",
            priceVnd = 6_990_000L,
            description = "Chống ồn đỉnh cao, chất âm trung thực, pin 30 giờ liên tục.",
            stockQuantity = 25,
        ),
        ProductEntity(
            id = 2L,
            name = "Sạc nhanh Anker 65W GaNPrime 3 cổng",
            category = "Phụ kiện công nghệ",
            priceVnd = 850_000L,
            description = "Công nghệ sạc GaN nhỏ gọn, hỗ trợ Power Delivery 65W cho laptop và điện thoại.",
            stockQuantity = 60,
        ),
        ProductEntity(
            id = 3L,
            name = "Bình giữ nhiệt Lock&Lock 500ml Inox 316",
            category = "Đời sống gia đình",
            priceVnd = 320_000L,
            description = "Thép không gỉ 316 an toàn sức khỏe, giữ nhiệt nóng lạnh 12 tiếng.",
            stockQuantity = 100,
        ),
    )

    suspend fun seed(db: AppDatabase) {
        db.flightDao().insertFlights(initialFlights())
        db.hotelDao().insertHotels(initialHotels())
        db.cinemaDao().insertMovies(initialMovies())
        db.cinemaDao().insertShowtimes(initialShowtimes())
        db.shopDao().insertProducts(initialProducts())
    }
}
