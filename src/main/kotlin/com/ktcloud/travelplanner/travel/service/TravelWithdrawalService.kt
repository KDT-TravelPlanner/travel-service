package com.ktcloud.travelplanner.travel.service

import com.ktcloud.travelplanner.global.exception.DomainException
import com.ktcloud.travelplanner.global.exception.ErrorCode
import com.ktcloud.travelplanner.membership.repository.TravelMemberRepository
import com.ktcloud.travelplanner.travel.repository.TravelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Identity의 회원 탈퇴가 POST /api/v1/internal/users/{userId}/withdrawal 로 호출한다.
// Identity는 자기 DB의 user soft-delete와 refresh token 폐기를 담당하고, Travel이 소유한
// 소유권/멤버십 정리는 여기서 한다(SERVICE_COMMUNICATION_BOUNDARIES.md 5).
//
// 규칙(모놀리식 이슈 #150과 동일): 이 유저가 오너인 활성 travel마다, 이전 후보(ACCEPTED 멤버)가
// 있으면 소유권을 넘기고 그 멤버 행을 지운다. 후보가 없으면 그대로 둔다(owner_id는 탈퇴 유저를
// 계속 가리킨다 — 표시 단에서 "탈퇴한 사용자"로 처리).
//
// 멱등: Identity 저장 실패로 같은 요청이 재시도될 수 있다. 이미 이전된 travel은 다음 호출의
// findAllByOwnerId 결과에서 빠지므로 자연히 no-op이 된다.
@Service
class TravelWithdrawalService(
	private val travelRepository: TravelRepository,
	private val travelMemberRepository: TravelMemberRepository,
) {
	@Transactional
	fun cleanUpOwnership(userId: UUID) {
		travelRepository.findAllByOwnerId(userId).forEach { travel ->
			val candidate = travelMemberRepository
				.findOwnershipTransferCandidates(travel.id)
				.firstOrNull()
				?: return@forEach
			travel.transferOwnership(candidate.user)
			travelMemberRepository.delete(candidate)
		}
	}
}

// path의 userId와 forward된 JWT의 주체가 다를 때. 탈퇴는 본인 요청만 허용한다.
class WithdrawalSubjectMismatchException :
	DomainException(ErrorCode.ACCESS_DENIED, "본인 계정만 탈퇴 처리할 수 있습니다.")
