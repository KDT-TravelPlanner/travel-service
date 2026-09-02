package com.ktcloud.travelplanner.membership.port

import com.ktcloud.travelplanner.membership.model.TravelRole
import java.util.UUID

/** Travel/Timeline이 멤버십 권한을 확인할 때 사용하는 내부 계약. */
interface MembershipAuthorizationPort {
    fun findAcceptedRole(travelId: UUID, userId: UUID): TravelRole?
    fun canRead(travelId: UUID, userId: UUID): Boolean
    fun canEdit(travelId: UUID, userId: UUID): Boolean
}
