package com.mebanking.app.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.model.navigation.RouteResult
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import com.mebanking.app.domain.navigation.PathFinder
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream

/**
 * Filter criteria for branch and ATM querying.
 */
enum class BranchFilter {
    ALL,
    BRANCH_ONLY,
    ATM_ONLY,
}

/**
 * Common test fixtures, factory utilities, and offline navigation helper for E2E tests.
 */
object E2ETestHelper {

    // Known geographic coordinates for tests
    val COORD_D1_HOI_SO = GeoPoint(10.7769, 106.7009)
    val COORD_D1_BEN_THANH = GeoPoint(10.7725, 106.6980)
    val COORD_D7_TAN_PHONG = GeoPoint(10.7326, 106.6992)
    val COORD_OUT_OF_BOUNDS = GeoPoint(21.0285, 105.8542) // Hanoi (far from HCMC coverage)
    val COORD_NORTH_POLE = GeoPoint(90.0, 0.0)

    /**
     * Builds an in-memory SQLite database instance populated with invariant triggers.
     */
    fun createInMemoryDatabase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return AppDatabase.buildInMemoryDatabase(context)
    }

    /**
     * Seeds the given [AppDatabase] with initial test fixtures and returns it.
     */
    suspend fun createAndSeedDatabase(): AppDatabase {
        val db = createInMemoryDatabase()
        DatabaseSeeder.seed(db)
        return db
    }

    /**
     * Loads the bundled HCMC road network graph asset resiliently.
     * Tries Robolectric Context asset manager first, falling back to local file streams.
     */
    fun loadBundledRoadGraph(): RoadGraph {
        return try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            RoadGraphLoader(context).loadFromAssets("navigation/hcmc_road_network.json")
        } catch (_: Exception) {
            val fileA = File("src/main/assets/navigation/hcmc_road_network.json")
            val fileB = File("app/src/main/assets/navigation/hcmc_road_network.json")
            val target = if (fileA.exists()) fileA else fileB
            FileInputStream(target).use { RoadGraphLoader().loadFromStream(it) }
        }
    }

    /**
     * Filters a list of branches by [BranchFilter].
     */
    fun filterBranches(branches: List<BranchEntity>, filter: BranchFilter): List<BranchEntity> {
        return when (filter) {
            BranchFilter.ALL -> branches
            BranchFilter.BRANCH_ONLY -> branches.filter { !it.name.contains("ATM", ignoreCase = true) }
            BranchFilter.ATM_ONLY -> branches.filter { it.hasAtm }
        }
    }

    /**
     * Searches branches by keyword in name or address.
     */
    fun searchBranches(branches: List<BranchEntity>, query: String): List<BranchEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return branches
        return branches.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.address.contains(trimmed, ignoreCase = true)
        }
    }

    /**
     * Finds the nearest branch or ATM to the given user location matching the filter.
     */
    fun findNearestBranch(
        branches: List<BranchEntity>,
        userLocation: GeoPoint,
        filter: BranchFilter = BranchFilter.ALL,
        graph: RoadGraph? = null,
    ): BranchDistance? {
        val filtered = filterBranches(branches, filter)
        if (filtered.isEmpty()) return null

        var bestBranch: BranchEntity? = null
        var minDistance = Double.MAX_VALUE

        for (b in filtered) {
            val dist = GeoDistanceCalculator.haversineDistanceMeters(
                userLocation.latitude, userLocation.longitude,
                b.latitude, b.longitude,
            )
            if (dist < minDistance) {
                minDistance = dist
                bestBranch = b
            }
        }

        val nearest = bestBranch ?: return null
        val nearestNode = graph?.let {
            GeoDistanceCalculator.findNearestNode(it, GeoPoint(nearest.latitude, nearest.longitude))
        }
        val estimatedMinutes = PathFinder.calculateEstimatedMinutes(minDistance)

        return BranchDistance(
            branch = nearest,
            distanceMeters = minDistance,
            estimatedMinutes = estimatedMinutes,
            nearestNode = nearestNode,
        )
    }

    /**
     * Computes the walking route to a target branch from the user location on the road graph.
     */
    fun calculateRouteToBranch(
        branch: BranchEntity,
        userLocation: GeoPoint,
        graph: RoadGraph,
        pathFinder: PathFinder = AStarPathFinder(),
    ): RouteResult? {
        val startNode = GeoDistanceCalculator.findNearestNode(graph, userLocation) ?: return null
        val branchPoint = GeoPoint(branch.latitude, branch.longitude)
        val destNode = GeoDistanceCalculator.findNearestNode(graph, branchPoint) ?: return null

        return pathFinder.findShortestPath(graph, startNode.id, destNode.id)
    }

    /**
     * Factory method for creating test UserEntity.
     */
    fun createUserEntity(
        id: Long = 0L,
        username: String = "test_user_${System.currentTimeMillis()}",
        plaintextPassword: String = "TestPass@123",
        fullName: String = "Nguyen Van Test",
        email: String = "test@example.com",
        phone: String = "0900000000",
        role: Role = Role.CUSTOMER,
    ): UserEntity {
        return UserEntity(
            id = id,
            username = username,
            passwordHash = PasswordHasher.hashPassword(plaintextPassword),
            fullName = fullName,
            email = email,
            phone = phone,
            role = role,
            kycVerified = true,
        )
    }

    /**
     * Factory method for creating test AccountEntity.
     */
    fun createAccountEntity(
        id: Long = 0L,
        userId: Long,
        accountNumber: String = "ACC${System.currentTimeMillis() % 100000000}",
        accountType: AccountType = AccountType.CHECKING,
        balance: Long = 10_000_000L,
        currency: String = "VND",
    ): AccountEntity {
        return AccountEntity(
            id = id,
            userId = userId,
            accountNumber = accountNumber,
            accountType = accountType,
            balance = balance,
            currency = currency,
        )
    }
}
