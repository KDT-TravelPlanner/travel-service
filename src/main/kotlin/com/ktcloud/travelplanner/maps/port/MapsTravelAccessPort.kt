package com.ktcloud.travelplanner.maps.port

import java.util.UUID

interface MapsTravelAccessPort {
    fun requireReadableTravel(travelId: UUID, requesterId: UUID): MapsTravelReference
}

data class MapsTravelReference(val travelId: UUID, val travelDays: Int)
