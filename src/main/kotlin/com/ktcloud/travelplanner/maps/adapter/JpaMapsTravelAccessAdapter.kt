package com.ktcloud.travelplanner.maps.adapter

import com.ktcloud.travelplanner.maps.port.MapsTravelAccessPort
import com.ktcloud.travelplanner.maps.port.MapsTravelReference
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.place.service.MapPointAccessDeniedException
import com.ktcloud.travelplanner.place.service.MapPointTravelNotFoundException
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaMapsTravelAccessAdapter(
    private val travelRepository: TravelRepository,
    private val travelMemberRepository: TravelMemberRepository,
) : MapsTravelAccessPort {
    override fun requireReadableTravel(travelId: UUID, requesterId: UUID): MapsTravelReference {
        val travel = travelRepository.findById(travelId).orElseThrow(::MapPointTravelNotFoundException)
        if (travel.owner.id != requesterId &&
            travelMemberRepository.findAcceptedRole(travelId, requesterId) == null
        ) throw MapPointAccessDeniedException()
        return MapsTravelReference(travel.id, travel.travelDays)
    }
}
