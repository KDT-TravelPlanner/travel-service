package com.ktcloud.travelplanner.membership.port

import java.util.UUID

/** Membership가 Travel 존재·소유자 정보를 확인할 때 사용하는 내부 계약. */
interface TravelReferencePort {
    fun findTravel(travelId: UUID): TravelReference?
}

data class TravelReference(
    val travelId: UUID,
    val ownerId: UUID,
)
