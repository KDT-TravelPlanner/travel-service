package com.ktcloud.travelplanner.travel.port

import java.util.UUID

/** Travel/Timeline이 Location 구현체에 요구하는 최소 계약. */
interface TravelLocationPort {
    fun existsCountry(countryId: UUID): Boolean
    fun existsCity(cityId: UUID): Boolean
    fun cityBelongsToCountry(cityId: UUID, countryId: UUID): Boolean
}
