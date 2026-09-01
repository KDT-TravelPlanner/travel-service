package com.ktcloud.travelplanner.travel.port

import java.util.UUID

/** Community 등 외부 서비스가 Travel 권한을 요청할 때 사용하는 계약. */
interface TravelAccessPort {
    fun checkReadAccess(travelId: UUID, requesterId: UUID): TravelReadAccess
}

data class TravelReadAccess(
    val travelId: UUID,
    val exists: Boolean,
    val hasReadAccess: Boolean,
)
