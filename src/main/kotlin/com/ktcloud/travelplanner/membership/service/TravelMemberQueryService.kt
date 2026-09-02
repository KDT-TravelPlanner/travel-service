package com.ktcloud.travelplanner.membership.service
import com.ktcloud.travelplanner.global.exception.DomainException
import com.ktcloud.travelplanner.global.exception.ErrorCode
import com.ktcloud.travelplanner.membership.dto.TravelMemberResponse
import com.ktcloud.travelplanner.membership.port.MembershipUserPort
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
@Service
class TravelMemberQueryService(
        private val travelRepository: TravelRepository,
        private val travelMemberRepository: TravelMemberRepository,
        private val membershipUserPort: MembershipUserPort,
) {
        @Transactional(readOnly = true)
        fun getTravelMembers(
                travelId: UUID,
                requesterId: UUID,
        ): List<TravelMemberResponse> {
                val travel = travelRepository.findById(travelId).orElseThrow(::MemberTravelNotFoundException)
                if (travel.ownerId != requesterId && travelMemberRepository.findAcceptedRole(travelId, requesterId) == null) {
                        throw TravelMemberAccessDeniedException()
                }
                // travel은 User 테이블을 갖지 않으므로 오너/멤버 표시 정보(닉네임·프로필)는 Identity에서
                // 가져온다. 탈퇴한 사용자면 deleted=true로 오고, Identity 장애면 findDisplay가 503을 던진다.
                val owner = TravelMemberResponse.fromOwner(membershipUserPort.findDisplay(travel.ownerId))
                val members = travelMemberRepository.findVisibleMembers(travelId).map { member ->
                        TravelMemberResponse.fromMember(member, membershipUserPort.findDisplay(member.userId))
                }
                return listOf(owner) + members
        }
}
class MemberTravelNotFoundException : DomainException(ErrorCode.RESOURCE_NOT_FOUND)
class TravelMemberAccessDeniedException : DomainException(ErrorCode.ACCESS_DENIED)
