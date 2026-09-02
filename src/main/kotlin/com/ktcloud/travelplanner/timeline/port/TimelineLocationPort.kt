package com.ktcloud.travelplanner.timeline.port

import java.util.UUID

/** Timeline이 Location에 요청하는 도시 확인 계약. */
interface TimelineLocationPort {
    fun findCity(cityId: UUID): TimelineCityReference?
}

data class TimelineCityReference(
    val cityId: UUID,
    val countryId: UUID,
    val name: String,
)
