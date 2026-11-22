package com.mebanking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mebanking.app.domain.model.Branch

/**
 * Room entity representing an offline bank branch or ATM location.
 */
@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "opening_hours")
    val openingHours: String,

    @ColumnInfo(name = "has_atm")
    val hasAtm: Boolean = true,
) {
    val type: String
        get() = if (name.contains("ATM", ignoreCase = true)) "ATM" else "BRANCH"

    fun toDomainModel(): Branch {
        return Branch(
            id = id,
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            phone = phone,
            openingHours = openingHours,
            hasAtm = hasAtm,
        )
    }

    companion object {
        fun fromDomainModel(branch: Branch): BranchEntity {
            return BranchEntity(
                id = branch.id,
                name = branch.name,
                address = branch.address,
                latitude = branch.latitude,
                longitude = branch.longitude,
                phone = branch.phone,
                openingHours = branch.openingHours,
                hasAtm = branch.hasAtm,
            )
        }
    }
}
