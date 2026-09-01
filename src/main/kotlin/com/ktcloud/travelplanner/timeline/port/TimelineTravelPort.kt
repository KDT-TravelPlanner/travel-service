package com.ktcloud.travelplanner.timeline.port

import java.util.UUID

/** Timeline이 Travel 존재·권한을 확인할 때 사용하는 내부 계약. */
interface TimelineTravelPort {
    fun findTravel(travelId: UUID): TimelineTravelReference?
    fun canAccess(travelId: UUID, userId: UUID): Boolean
}

data class TimelineTravelReference(
    val travelId: UUID,
    val ownerId: UUID,
)
