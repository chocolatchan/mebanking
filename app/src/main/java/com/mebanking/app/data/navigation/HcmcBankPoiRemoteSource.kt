package com.mebanking.app.data.navigation

import com.mebanking.app.data.local.entity.BranchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Queries OpenStreetMap Overpass API for real bank/ATM POIs within Ho Chi Minh City
 * matching "ATM", "Ngân hàng", or "Bank", caching results in memory for the session.
 */
class HcmcBankPoiRemoteSource {

    @Volatile
    private var cachedPois: List<BranchEntity>? = null

    suspend fun fetchHcmcBankAndAtmPois(): List<BranchEntity> = withContext(Dispatchers.IO) {
        cachedPois?.let { return@withContext it }

        val query = """
            [out:json][timeout:10];
            (
              node["amenity"~"^(bank|atm)$"]($HCMC_BBOX);
              node["name"~"ATM|Ngân hàng|Bank",i]($HCMC_BBOX);
            );
            out body 250;
        """.trimIndent()

        for (endpoint in OVERPASS_ENDPOINTS) {
            val parsed = runCatching { queryEndpoint(endpoint, query) }.getOrNull()
            if (!parsed.isNullOrEmpty()) {
                cachedPois = parsed
                return@withContext parsed
            }
        }
        emptyList()
    }

    private fun queryEndpoint(endpoint: String, overpassQuery: String): List<BranchEntity> {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("User-Agent", "m.ebanking/1.0 (Android)")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        return try {
            val body = "data=" + URLEncoder.encode(overpassQuery, "UTF-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                return emptyList()
            }
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            parseOverpassJson(jsonText)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseOverpassJson(jsonText: String): List<BranchEntity> {
        val root = JSONObject(jsonText)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val result = ArrayList<BranchEntity>(elements.length())

        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val lat = el.optDouble("lat", Double.NaN)
            val lon = el.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue

            val tags = el.optJSONObject("tags") ?: continue
            val amenity = tags.optString("amenity")
            val rawName = tags.optString("name").ifBlank {
                tags.optString("operator").ifBlank {
                    tags.optString("brand")
                }
            }
            val displayName = when {
                rawName.isNotBlank() && amenity.equals("atm", ignoreCase = true) &&
                    !rawName.contains("ATM", ignoreCase = true) -> "ATM $rawName"
                rawName.isNotBlank() -> rawName
                amenity.equals("atm", ignoreCase = true) -> "ATM TP.HCM"
                amenity.equals("bank", ignoreCase = true) -> "Ngân hàng TP.HCM"
                else -> continue
            }

            val street = tags.optString("addr:street")
            val house = tags.optString("addr:housenumber")
            val district = tags.optString("addr:district").ifBlank { tags.optString("addr:suburb") }
            val address = listOfNotNull(
                listOf(house, street).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                district.ifBlank { null },
                "TP. Hồ Chí Minh",
            ).joinToString(", ")

            val osmId = el.optLong("id", i.toLong())
            val isAtm = amenity.equals("atm", ignoreCase = true) ||
                displayName.contains("ATM", ignoreCase = true)
            result.add(
                BranchEntity(
                    id = 1_000_000L + (osmId and 0x7FFF_FFFFL),
                    name = displayName,
                    address = address,
                    latitude = lat,
                    longitude = lon,
                    phone = tags.optString("phone").ifBlank { "1900545413" },
                    openingHours = tags.optString("opening_hours").ifBlank {
                        if (isAtm) "24/7" else "08:00 - 17:00 (T2 - T6)"
                    },
                    hasAtm = true,
                ),
            )
        }
        return result
    }

    companion object {
        // Ho Chi Minh City inner + belt bounding box (south, west, north, east)
        private const val HCMC_BBOX = "10.62,106.52,10.92,106.86"
        private val OVERPASS_ENDPOINTS = listOf(
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass-api.de/api/interpreter",
        )
    }
}
