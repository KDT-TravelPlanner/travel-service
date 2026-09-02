package com.ktcloud.travelplanner.travel.port

import java.util.UUID

/** Travel이 Timeline 구현체에 요구하는 최소 계약. */
interface TravelTimelinePort {
    fun countItems(travelId: UUID): Long
    fun deleteItems(travelId: UUID)
}
