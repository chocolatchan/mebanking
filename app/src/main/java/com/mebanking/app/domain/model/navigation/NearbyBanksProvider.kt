package com.mebanking.app.domain.model.navigation

import com.mebanking.app.data.local.entity.BranchEntity

/**
 * Provider supplying pre-marked nearby banks and ATMs in central Ho Chi Minh City
 * for Google Maps and interactive locator rendering.
 */
object NearbyBanksProvider {
    val NEARBY_BANKS: List<BranchEntity> = listOf(
        BranchEntity(
            id = 101L,
            name = "Vietcombank - CN Sài Gòn",
            address = "29 Bến Chương Dương, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7688,
            longitude = 106.7052,
            phone = "02838297245",
            openingHours = "08:00 - 16:30 (T2 - T6)",
            hasAtm = true,
        ),
        BranchEntity(
            id = 102L,
            name = "Techcombank - CN Sài Gòn",
            address = "9-11 Tôn Đức Thắng, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7812,
            longitude = 106.7058,
            phone = "02839100345",
            openingHours = "08:00 - 17:00 (T2 - T6)",
            hasAtm = true,
        ),
        BranchEntity(
            id = 103L,
            name = "BIDV - CN Nam Kỳ Khởi Nghĩa",
            address = "66 Nam Kỳ Khởi Nghĩa, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7695,
            longitude = 106.6970,
            phone = "02838218812",
            openingHours = "08:00 - 17:00 (T2 - T6)",
            hasAtm = true,
        ),
        BranchEntity(
            id = 104L,
            name = "Agribank - CN Trung Tâm Sài Gòn",
            address = "2 Võ Văn Kiệt, Phường Nguyễn Thái Bình, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7680,
            longitude = 106.7025,
            phone = "02838214152",
            openingHours = "08:00 - 17:00 (T2 - T6)",
            hasAtm = true,
        ),
        BranchEntity(
            id = 105L,
            name = "MBBank - CN Hai Bà Trưng",
            address = "168 Hai Bà Trưng, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7845,
            longitude = 106.6965,
            phone = "02838226066",
            openingHours = "08:00 - 17:00 (T2 - T6)",
            hasAtm = true,
        ),
        BranchEntity(
            id = 106L,
            name = "ACB - CN Bến Thành",
            address = "135 Nguyễn Thái Học, Phường Cầu Ông Lãnh, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7665,
            longitude = 106.6935,
            phone = "02838367878",
            openingHours = "07:30 - 16:30 (T2 - T6)",
            hasAtm = true,
        ),
        BranchEntity(
            id = 107L,
            name = "ATM Vietcombank - Bitexco Tower",
            address = "2 Hải Triều, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7715,
            longitude = 106.7045,
            phone = "1900545413",
            openingHours = "24/7",
            hasAtm = true,
        ),
        BranchEntity(
            id = 108L,
            name = "ATM Techcombank - Phố đi bộ",
            address = "88 Đồng Khởi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
            latitude = 10.7758,
            longitude = 106.7032,
            phone = "1800588822",
            openingHours = "24/7",
            hasAtm = true,
        ),
    )

    fun searchNearby(query: String, filter: BranchFilter): List<BranchEntity> {
        val trimmed = query.trim()
        return NEARBY_BANKS.filter { branch ->
            val matchesFilter = when (filter) {
                BranchFilter.ALL -> true
                BranchFilter.BRANCH_ONLY -> branch.type.equals("BRANCH", ignoreCase = true)
                BranchFilter.ATM_ONLY -> branch.type.equals("ATM", ignoreCase = true)
            }
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                branch.name.contains(trimmed, ignoreCase = true) ||
                    branch.address.contains(trimmed, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }

    fun getById(id: Long): BranchEntity? = NEARBY_BANKS.firstOrNull { it.id == id }
}
