package com.ktcloud.travelplanner.location.port

import java.util.UUID

/** Travel/Timeline이 Location 도메인에 요청하는 내부 조회 계약. */
interface LocationCatalogPort {
    fun existsCountry(countryId: UUID): Boolean
    fun existsCity(cityId: UUID): Boolean
    fun cityBelongsToCountry(cityId: UUID, countryId: UUID): Boolean
    fun findCity(cityId: UUID): LocationCityReference?
}

data class LocationCityReference(
    val cityId: UUID,
    val countryId: UUID,
    val name: String,
)
