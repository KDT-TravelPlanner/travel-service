package com.ktcloud.travelplanner.timeline.port

import com.ktcloud.travelplanner.membership.model.TravelRole
import java.util.UUID

/** Timeline이 Membership에 요청하는 편집 권한 계약. */
interface TimelineMembershipPort {
    fun findRole(travelId: UUID, userId: UUID): TravelRole?
    fun canEdit(travelId: UUID, userId: UUID): Boolean
}
