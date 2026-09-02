package com.ktcloud.travelplanner.travel.port

import com.ktcloud.travelplanner.membership.model.TravelRole
import java.util.UUID

/** Travel/Timeline이 멤버십 권한 구현체에 요구하는 계약. */
interface TravelMembershipPort {
    fun findAcceptedRole(travelId: UUID, userId: UUID): TravelRole?
    fun canRead(travelId: UUID, userId: UUID): Boolean
}
